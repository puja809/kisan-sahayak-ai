package com.farmer.iot.service;

import com.farmer.iot.dto.*;
import com.farmer.iot.entity.*;
import com.farmer.iot.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing alert configurations and generating alerts.
 * Handles threshold monitoring and notification dispatch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertConfigurationRepository configRepository;
    private final IotAlertRepository alertRepository;
    private final IotDeviceRepository deviceRepository;
    private final NotificationService notificationService;

    /**
     * Configure alert thresholds for a device sensor.
     */
    @Transactional
    public AlertConfigurationResponse configureAlert(String farmerId, AlertConfigurationRequest request) {
        log.info("Configuring alert for device: {}, sensor: {}", request.getDeviceId(), request.getSensorType());

        // Verify device exists and belongs to farmer
        IotDevice device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found: " + request.getDeviceId()));

        if (!device.getOwnerFarmerId().equals(farmerId)) {
            throw new RuntimeException("Device does not belong to farmer: " + farmerId);
        }

        // Check if config already exists
        AlertConfiguration config = configRepository.findByDeviceIdAndSensorType(
                request.getDeviceId(), request.getSensorType())
                .orElse(new AlertConfiguration());

        // Update or create configuration
        config.setDeviceId(request.getDeviceId());
        config.setFarmerId(farmerId);
        config.setSensorType(request.getSensorType());
        config.setMinThreshold(request.getMinThreshold());
        config.setMaxThreshold(request.getMaxThreshold());
        config.setThresholdUnit(request.getThresholdUnit());
        config.setAlertEnabled(request.getAlertEnabled() != null ? request.getAlertEnabled() : true);
        config.setNotificationMethod(request.getNotificationMethod() != null ?
                request.getNotificationMethod() : "PUSH");

        if (request.getAlertSeverity() != null) {
            config.setAlertSeverity(AlertConfiguration.AlertSeverity.valueOf(request.getAlertSeverity()));
        }

        AlertConfiguration savedConfig = configRepository.save(config);
        log.info("Alert configuration saved for device: {}, sensor: {}", request.getDeviceId(), request.getSensorType());

        return AlertConfigurationResponse.fromEntity(savedConfig);
    }

    /**
     * Get alert configurations for a device.
     */
    public List<AlertConfigurationResponse> getAlertConfigurations(Long deviceId) {
        log.debug("Fetching alert configurations for device: {}", deviceId);
        return configRepository.findByDeviceId(deviceId)
                .stream()
                .map(AlertConfigurationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Check sensor reading against thresholds and generate alerts if needed.
     */
    @Transactional
    public void checkThresholdsAndGenerateAlerts(SensorReading reading, IotDevice device) {
        log.debug("Checking thresholds for device: {}", device.getDeviceId());

        List<AlertConfiguration> configs = configRepository.findByDeviceIdAndAlertEnabled(
                reading.getDeviceId(), true);

        for (AlertConfiguration config : configs) {
            Double readingValue = getReadingValue(reading, config.getSensorType());

            if (readingValue == null) {
                continue;
            }

            // Check minimum threshold
            if (config.getMinThreshold() != null && readingValue < config.getMinThreshold()) {
                generateAlert(reading, device, config, "MIN", config.getMinThreshold(), readingValue);
            }

            // Check maximum threshold
            if (config.getMaxThreshold() != null && readingValue > config.getMaxThreshold()) {
                generateAlert(reading, device, config, "MAX", config.getMaxThreshold(), readingValue);
            }
        }
    }

    /**
     * Generate an alert when threshold is exceeded.
     */
    private void generateAlert(SensorReading reading, IotDevice device,
                               AlertConfiguration config, String thresholdType,
                               Double thresholdValue, Double readingValue) {
        log.info("Generating alert for device: {}, sensor: {}, value: {} {} threshold: {}",
                device.getDeviceId(), config.getSensorType(), readingValue,
                config.getThresholdUnit(), thresholdValue);

        String message = String.format("Alert: %s reading (%.2f %s) %s threshold (%.2f %s). " +
                        "Farmer retains full ownership of all IoT-generated data.",
                config.getSensorType(), readingValue, config.getThresholdUnit(),
                thresholdType.equals("MIN") ? "below" : "exceeds",
                thresholdValue, config.getThresholdUnit());

        IotAlert alert = IotAlert.builder()
                .deviceId(reading.getDeviceId())
                .farmerId(device.getOwnerFarmerId())
                .sensorType(config.getSensorType())
                .readingValue(readingValue)
                .thresholdType(thresholdType)
                .thresholdValue(thresholdValue)
                .alertMessage(message)
                .alertSeverity(config.getAlertSeverity())
                .notificationMethod(config.getNotificationMethod())
                .status(IotAlert.AlertStatus.NEW)
                .alertTimestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        IotAlert savedAlert = alertRepository.save(alert);

        // Send notification
        if (config.getAlertEnabled()) {
            notificationService.sendAlertNotification(savedAlert);
        }
    }

    /**
     * Get the reading value based on sensor type.
     */
    private Double getReadingValue(SensorReading reading, String sensorType) {
        return switch (sensorType.toUpperCase()) {
            case "SOIL_MOISTURE" -> reading.getSoilMoisturePercent();
            case "TEMPERATURE" -> reading.getTemperatureCelsius();
            case "HUMIDITY" -> reading.getHumidityPercent();
            case "PH" -> reading.getPhLevel();
            case "EC" -> reading.getEcValue();
            case "NPK_NITROGEN" -> reading.getNpkNitrogen();
            case "NPK_PHOSPHORUS" -> reading.getNpkPhosphorus();
            case "NPK_POTASSIUM" -> reading.getNpkPotassium();
            default -> null;
        };
    }

    /**
     * Get alerts for a device.
     */
    public List<AlertResponse> getAlertsForDevice(Long deviceId) {
        log.debug("Fetching alerts for device: {}", deviceId);
        return alertRepository.findByDeviceIdOrderByAlertTimestampDesc(deviceId)
                .stream()
                .map(AlertResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get alerts for a farmer.
     */
    public List<AlertResponse> getAlertsForFarmer(String farmerId) {
        log.debug("Fetching alerts for farmer: {}", farmerId);
        return alertRepository.findByFarmerIdOrderByAlertTimestampDesc(farmerId)
                .stream()
                .map(AlertResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get new alerts for a farmer.
     */
    public List<AlertResponse> getNewAlertsForFarmer(String farmerId) {
        log.debug("Fetching new alerts for farmer: {}", farmerId);
        return alertRepository.findNewAlertsByFarmerId(farmerId)
                .stream()
                .map(AlertResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Acknowledge an alert.
     */
    @Transactional
    public AlertResponse acknowledgeAlert(Long alertId) {
        log.info("Acknowledging alert: {}", alertId);
        IotAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));

        alert.setStatus(IotAlert.AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(LocalDateTime.now());
        IotAlert savedAlert = alertRepository.save(alert);

        return AlertResponse.fromEntity(savedAlert);
    }

    /**
     * Resolve an alert.
     */
    @Transactional
    public AlertResponse resolveAlert(Long alertId) {
        log.info("Resolving alert: {}", alertId);
        IotAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));

        alert.setStatus(IotAlert.AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        IotAlert savedAlert = alertRepository.save(alert);

        return AlertResponse.fromEntity(savedAlert);
    }

    /**
     * Get count of new alerts for a farmer.
     */
    public long getNewAlertCount(String farmerId) {
        return alertRepository.countNewAlertsByFarmerId(farmerId);
    }
}