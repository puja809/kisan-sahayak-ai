package com.farmer.iot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing alerts generated when sensor readings exceed thresholds.
 */
@Entity
@Table(name = "iot_alerts", indexes = {
    @Index(name = "idx_device_id", columnList = "device_id"),
    @Index(name = "idx_farmer_id", columnList = "farmer_id"),
    @Index(name = "idx_timestamp", columnList = "alert_timestamp"),
    @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IotAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "farmer_id", nullable = false, length = 50)
    private String farmerId;

    @Column(name = "sensor_type", nullable = false, length = 50)
    private String sensorType;

    @Column(name = "reading_value", precision = 10, scale = 2)
    private Double readingValue;

    @Column(name = "threshold_type", length = 10)
    private String thresholdType;

    @Column(name = "threshold_value", precision = 10, scale = 2)
    private Double thresholdValue;

    @Column(name = "alert_message", length = 500)
    private String alertMessage;

    @Column(name = "alert_severity", length = 20)
    @Enumerated(EnumType.STRING)
    private AlertSeverity alertSeverity;

    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "notification_method", length = 50)
    private String notificationMethod;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AlertStatus status = AlertStatus.NEW;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "alert_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime alertTimestamp = LocalDateTime.now();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum AlertStatus {
        NEW,
        ACKNOWLEDGED,
        RESOLVED,
        DISMISSED
    }
}