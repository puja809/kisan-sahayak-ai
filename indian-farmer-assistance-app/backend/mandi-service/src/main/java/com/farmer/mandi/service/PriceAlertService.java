package com.farmer.mandi.service;

import com.farmer.mandi.dto.PriceAlertDto;
import com.farmer.mandi.dto.PriceAlertRequest;
import com.farmer.mandi.entity.PriceAlert;
import com.farmer.mandi.repository.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing price alerts.
 * 
 * Requirements:
 * - 6.10: Create price alert subscription endpoints
 * - 6.10: Implement scheduled job to check price peaks in neighboring districts
 * - 6.10: Send push notifications for crop price alerts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final MandiPriceService mandiPriceService;
    private final NotificationService notificationService;

    /**
     * Creates a new price alert subscription.
     * 
     * @param request The alert subscription request
     * @return The created alert DTO
     */
    public PriceAlertDto createAlert(PriceAlertRequest request) {
        log.info("Creating price alert for farmer: {}, commodity: {}", 
                request.getFarmerId(), request.getCommodity());
        
        // Check if alert already exists for this farmer/commodity/variety
        List<PriceAlert> existingAlerts = priceAlertRepository.findByFarmerIdAndCommodityAndVarietyAndIsActiveTrue(
                request.getFarmerId(), request.getCommodity(), request.getVariety());
        
        if (!existingAlerts.isEmpty()) {
            log.info("Alert already exists for farmer: {}, commodity: {}", 
                    request.getFarmerId(), request.getCommodity());
            return mapToDto(existingAlerts.get(0));
        }
        
        PriceAlert alert = PriceAlert.builder()
                .farmerId(request.getFarmerId())
                .commodity(request.getCommodity())
                .variety(request.getVariety())
                .targetPrice(request.getTargetPrice())
                .alertType(request.getAlertType() != null ? request.getAlertType() : "PRICE_ABOVE")
                .neighboringDistrictsOnly(request.getNeighboringDistrictsOnly() != null 
                        ? request.getNeighboringDistrictsOnly() : false)
                .isActive(true)
                .notificationSent(false)
                .build();
        
        PriceAlert savedAlert = priceAlertRepository.save(alert);
        log.info("Created price alert with ID: {}", savedAlert.getId());
        
        return mapToDto(savedAlert);
    }

    /**
     * Gets all active alerts for a farmer.
     * 
     * @param farmerId The farmer ID
     * @return List of alert DTOs
     */
    public List<PriceAlertDto> getAlertsForFarmer(String farmerId) {
        log.info("Getting alerts for farmer: {}", farmerId);
        
        return priceAlertRepository.findByFarmerIdAndIsActiveTrue(farmerId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets all alerts for a farmer including inactive.
     * 
     * @param farmerId The farmer ID
     * @return List of alert DTOs
     */
    public List<PriceAlertDto> getAllAlertsForFarmer(String farmerId) {
        log.info("Getting all alerts for farmer: {}", farmerId);
        
        return priceAlertRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Deactivates a price alert.
     * 
     * @param alertId The alert ID
     * @return true if deactivated, false if not found
     */
    public boolean deactivateAlert(Long alertId) {
        log.info("Deactivating alert: {}", alertId);
        
        return priceAlertRepository.findById(alertId)
                .map(alert -> {
                    alert.setIsActive(false);
                    priceAlertRepository.save(alert);
                    log.info("Deactivated alert: {}", alertId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Gets alerts that need to be checked for price peaks.
     * 
     * @return List of active alerts
     */
    public List<PriceAlert> getAlertsNeedingCheck() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(6);
        return priceAlertRepository.findActiveAlertsNeedingNotification(threshold);
    }

    /**
     * Scheduled job to check price alerts and send notifications.
     * Runs every hour.
     * 
     * Requirements:
     * - 6.10: Implement scheduled job to check price peaks in neighboring districts
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void checkPriceAlerts() {
        log.info("Running scheduled price alert check");
        
        List<PriceAlert> alerts = getAlertsNeedingCheck();
        log.info("Found {} alerts to check", alerts.size());
        
        for (PriceAlert alert : alerts) {
            try {
                checkAndNotifyAlert(alert);
            } catch (Exception e) {
                log.error("Error checking alert {}: {}", alert.getId(), e.getMessage());
            }
        }
    }

    /**
     * Checks a single alert and sends notification if conditions are met.
     * 
     * @param alert The price alert to check
     */
    private void checkAndNotifyAlert(PriceAlert alert) {
        log.info("Checking alert {} for commodity: {}", alert.getId(), alert.getCommodity());
        
        // Get latest prices
        List<com.farmer.mandi.dto.MandiPriceDto> prices = 
                mandiPriceService.getLatestPricesFromDatabase(alert.getCommodity());
        
        if (prices.isEmpty()) {
            log.info("No prices found for commodity: {}", alert.getCommodity());
            return;
        }
        
        boolean shouldNotify = false;
        String notificationMessage = "";
        
        for (com.farmer.mandi.dto.MandiPriceDto price : prices) {
            if (price.getModalPrice() == null) {
                continue;
            }
            
            if ("PRICE_ABOVE".equals(alert.getAlertType())) {
                if (alert.getTargetPrice() != null && 
                        price.getModalPrice().compareTo(alert.getTargetPrice()) >= 0) {
                    shouldNotify = true;
                    notificationMessage = String.format("Price for %s has reached %s (target: %s) at %s mandi",
                            alert.getCommodity(), price.getModalPrice(), alert.getTargetPrice(), price.getMandiName());
                    break;
                }
            } else if ("PRICE_BELOW".equals(alert.getAlertType())) {
                if (alert.getTargetPrice() != null && 
                        price.getModalPrice().compareTo(alert.getTargetPrice()) <= 0) {
                    shouldNotify = true;
                    notificationMessage = String.format("Price for %s has dropped to %s (alert: %s) at %s mandi",
                            alert.getCommodity(), price.getModalPrice(), alert.getTargetPrice(), price.getMandiName());
                    break;
                }
            } else if ("PRICE_PEAK".equals(alert.getAlertType())) {
                // Check if this is a peak price (highest in recent history)
                if (isPricePeak(price, alert.getCommodity())) {
                    shouldNotify = true;
                    notificationMessage = String.format("Price peak detected for %s at %s (price: %s)",
                            alert.getCommodity(), price.getMandiName(), price.getModalPrice());
                    break;
                }
            }
        }
        
        if (shouldNotify) {
            sendNotification(alert, notificationMessage);
        } else {
            // Update last checked time
            alert.setLastNotificationAt(LocalDateTime.now());
            priceAlertRepository.save(alert);
        }
    }

    /**
     * Checks if a price is a peak (highest in recent history).
     * 
     * @param price The current price
     * @param commodity The commodity name
     * @return true if it's a peak price
     */
    private boolean isPricePeak(com.farmer.mandi.dto.MandiPriceDto price, String commodity) {
        // Get historical prices for comparison
        List<com.farmer.mandi.dto.MandiPriceDto> history = 
                mandiPriceService.getHistoricalPricesFromDatabase(commodity, 30);
        
        if (history.isEmpty()) {
            return false;
        }
        
        // Find the highest price in history
        BigDecimal highestHistorical = history.stream()
                .map(com.farmer.mandi.dto.MandiPriceDto::getModalPrice)
                .filter(p -> p != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        // Check if current price is higher than historical max
        return price.getModalPrice() != null && 
                price.getModalPrice().compareTo(highestHistorical) > 0;
    }

    /**
     * Sends a push notification for a price alert.
     * 
     * @param alert The price alert
     * @param message The notification message
     */
    private void sendNotification(PriceAlert alert, String message) {
        log.info("Sending notification for alert {}: {}", alert.getId(), message);
        
        try {
            notificationService.sendPushNotification(alert.getFarmerId(), 
                    "Price Alert: " + alert.getCommodity(), message);
            
            // Update alert status
            alert.setNotificationSent(true);
            alert.setLastNotificationAt(LocalDateTime.now());
            priceAlertRepository.save(alert);
            
            log.info("Notification sent successfully for alert: {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to send notification for alert {}: {}", alert.getId(), e.getMessage());
        }
    }

    /**
     * Maps entity to DTO.
     */
    private PriceAlertDto mapToDto(PriceAlert entity) {
        return PriceAlertDto.builder()
                .id(entity.getId())
                .farmerId(entity.getFarmerId())
                .commodity(entity.getCommodity())
                .variety(entity.getVariety())
                .targetPrice(entity.getTargetPrice())
                .alertType(entity.getAlertType())
                .neighboringDistrictsOnly(entity.getNeighboringDistrictsOnly())
                .isActive(entity.getIsActive())
                .notificationSent(entity.getNotificationSent())
                .lastNotificationAt(entity.getLastNotificationAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}