package com.farmer.iot.dto;

import com.farmer.iot.entity.IotAlert;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for IoT alert response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {

    private Long id;
    private Long deviceId;
    private String farmerId;
    private String sensorType;
    private Double readingValue;
    private String thresholdType;
    private Double thresholdValue;
    private String alertMessage;
    private String alertSeverity;
    private Boolean notificationSent;
    private String notificationMethod;
    private LocalDateTime notificationSentAt;
    private String status;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime alertTimestamp;
    private LocalDateTime createdAt;

    public static AlertResponse fromEntity(IotAlert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .deviceId(alert.getDeviceId())
                .farmerId(alert.getFarmerId())
                .sensorType(alert.getSensorType())
                .readingValue(alert.getReadingValue())
                .thresholdType(alert.getThresholdType())
                .thresholdValue(alert.getThresholdValue())
                .alertMessage(alert.getAlertMessage())
                .alertSeverity(alert.getAlertSeverity() != null ? alert.getAlertSeverity().name() : null)
                .notificationSent(alert.getNotificationSent())
                .notificationMethod(alert.getNotificationMethod())
                .notificationSentAt(alert.getNotificationSentAt())
                .status(alert.getStatus().name())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .resolvedAt(alert.getResolvedAt())
                .alertTimestamp(alert.getAlertTimestamp())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}