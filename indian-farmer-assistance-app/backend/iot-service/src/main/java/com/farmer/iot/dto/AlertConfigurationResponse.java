package com.farmer.iot.dto;

import com.farmer.iot.entity.AlertConfiguration;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for alert configuration response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertConfigurationResponse {

    private Long id;
    private Long deviceId;
    private String farmerId;
    private String sensorType;
    private Double minThreshold;
    private Double maxThreshold;
    private String thresholdUnit;
    private Boolean alertEnabled;
    private String notificationMethod;
    private String alertSeverity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AlertConfigurationResponse fromEntity(AlertConfiguration config) {
        return AlertConfigurationResponse.builder()
                .id(config.getId())
                .deviceId(config.getDeviceId())
                .farmerId(config.getFarmerId())
                .sensorType(config.getSensorType())
                .minThreshold(config.getMinThreshold())
                .maxThreshold(config.getMaxThreshold())
                .thresholdUnit(config.getThresholdUnit())
                .alertEnabled(config.getAlertEnabled())
                .notificationMethod(config.getNotificationMethod())
                .alertSeverity(config.getAlertSeverity() != null ? config.getAlertSeverity().name() : null)
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}