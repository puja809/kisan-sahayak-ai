package com.farmer.iot.service;

import com.farmer.iot.dto.AlertConfigurationRequest;
import com.farmer.iot.dto.AlertConfigurationResponse;
import com.farmer.iot.dto.AlertResponse;
import com.farmer.iot.entity.AlertConfiguration;
import com.farmer.iot.entity.IotAlert;
import com.farmer.iot.entity.IotDevice;
import com.farmer.iot.entity.SensorReading;
import com.farmer.iot.repository.AlertConfigurationRepository;
import com.farmer.iot.repository.IotAlertRepository;
import com.farmer.iot.repository.IotDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing IoT alerts and monitoring sensor thresholds.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final IotAlertRepository alertRepository;
    private final AlertConfigurationRepository configRepository;
    private final IotDeviceRepository deviceRepository;
    private final NotificationService notificationService;

    /**
     * Monitor sensor readings and generate alerts if thresholds are exceeded.
     * Validates: Requirements 10.4
     */
    @Transactional
    public void monitorThresholds(SensorReading reading) {
        List<AlertConfiguration> configs = configRepository.findByDeviceId(reading.getDeviceId());

        for (AlertConfiguration config : configs) {
            if (!config.getAlertEnabled()) continue;

            Double readingValue = getReadingValue(reading, config.getSensorType());
            if (readingValue == null) continue;

            if (config.getMaxThreshold() != null && readingValue > config.getMaxThreshold()) {
                createAlert(reading, config, readingValue, "MAX", config.getMaxThreshold());
            } else if (config.getMinThreshold() != null && readingValue < config.getMinThreshold()) {
                createAlert(reading, config, readingValue, "MIN", config.getMinThreshold());
            }
        }
    }

    /**
     * Get all alerts for a farmer.
     */
    public List<IotAlert> getFarmerAlerts(String farmerId) {
        return alertRepository.findByFarmerIdOrderByAlertTimestampDesc(farmerId);
    }

    /**
     * Get active alerts for a device.
     */
    public List<IotAlert> getActiveDeviceAlerts(Long deviceId) {
        return alertRepository.findByDeviceIdAndStatus(deviceId, IotAlert.AlertStatus.NEW);
    }

    /**
     * Acknowledge an alert.
     */
    @Transactional
    public AlertResponse acknowledgeAlert(Long alertId) {
        IotAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
        alert.setStatus(IotAlert.AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(LocalDateTime.now());
        IotAlert saved = alertRepository.save(alert);
        return AlertResponse.fromEntity(saved);
    }

    /**
     * Resolve an alert.
     */
    @Transactional
    public AlertResponse resolveAlert(Long alertId) {
        IotAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
        alert.setStatus(IotAlert.AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        IotAlert saved = alertRepository.save(alert);
        return AlertResponse.fromEntity(saved);
    }

    /**
     * Create and save an alert.
     */
    private void createAlert(SensorReading reading, AlertConfiguration config,
                             Double readingValue, String thresholdType, Double thresholdValue) {
        
        IotDevice device = deviceRepository.findById(reading.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));

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
                .alertSeverity(mapSeverity(config.getAlertSeverity()))
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

    private IotAlert.AlertSeverity mapSeverity(AlertConfiguration.AlertSeverity severity) {
        if (severity == null) return IotAlert.AlertSeverity.MEDIUM;
        return IotAlert.AlertSeverity.valueOf(severity.name());
    }

    /**
     * Get the reading value based on sensor type.
     */
    private Double getReadingValue(SensorReading reading, String sensorType) {
        return switch (sensorType.toUpperCase()) {
            case "SOIL_MOISTURE" -> reading.getSoilMoisturePercent();
            case "TEMPERATURE" -> reading.getTemperatureCelsius();
            case "HUMIDITY" -> reading.getHumidityPercent();
            case "PH_LEVEL" -> reading.getPhLevel();
            case "NPK_NITROGEN" -> reading.getNpkNitrogen();
            case "NPK_PHOSPHORUS" -> reading.getNpkPhosphorus();
            case "NPK_POTASSIUM" -> reading.getNpkPotassium();
            default -> null;
        };
    }

    /**
     * Configure alert thresholds for a device sensor.
     */
    @Transactional
    public AlertConfigurationResponse configureAlert(String farmerId, AlertConfigurationRequest request) {
        AlertConfiguration config = AlertConfiguration.builder()
                .deviceId(request.getDeviceId())
                .sensorType(request.getSensorType())
                .minThreshold(request.getMinThreshold())
                .maxThreshold(request.getMaxThreshold())
                .thresholdUnit(request.getThresholdUnit())
                .alertEnabled(request.getAlertEnabled() != null ? request.getAlertEnabled() : true)
                .alertSeverity(AlertConfiguration.AlertSeverity.valueOf(request.getAlertSeverity()))
                .notificationMethod(request.getNotificationMethod())
                .createdAt(LocalDateTime.now())
                .build();
        AlertConfiguration saved = configRepository.save(config);
        return AlertConfigurationResponse.fromEntity(saved);
    }

    /**
     * Get alert configurations for a device.
     */
    public List<AlertConfigurationResponse> getAlertConfigurations(Long deviceId) {
        return configRepository.findByDeviceId(deviceId)
                .stream()
                .map(AlertConfigurationResponse::fromEntity)
                .toList();
    }

    /**
     * Get alerts for a specific device.
     */
    public List<AlertResponse> getAlertsForDevice(Long deviceId) {
        return alertRepository.findByDeviceIdOrderByAlertTimestampDesc(deviceId)
                .stream()
                .map(AlertResponse::fromEntity)
                .toList();
    }

    /**
     * Get alerts for a farmer.
     */
    public List<AlertResponse> getAlertsForFarmer(String farmerId) {
        return alertRepository.findByFarmerIdOrderByAlertTimestampDesc(farmerId)
                .stream()
                .map(AlertResponse::fromEntity)
                .toList();
    }

}