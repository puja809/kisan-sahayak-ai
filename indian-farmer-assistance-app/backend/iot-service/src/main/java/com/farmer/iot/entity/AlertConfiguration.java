package com.farmer.iot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing alert threshold configurations for IoT devices.
 * Stores threshold values for each sensor type and notification preferences.
 */
@Entity
@Table(name = "alert_configurations", indexes = {
    @Index(name = "idx_device_id", columnList = "device_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "farmer_id", nullable = false, length = 50)
    private String farmerId;

    @Column(name = "sensor_type", nullable = false, length = 50)
    private String sensorType;

    @Column(name = "min_threshold", precision = 10, scale = 2)
    private Double minThreshold;

    @Column(name = "max_threshold", precision = 10, scale = 2)
    private Double maxThreshold;

    @Column(name = "threshold_unit", length = 20)
    private String thresholdUnit;

    @Column(name = "alert_enabled")
    @Builder.Default
    private Boolean alertEnabled = true;

    @Column(name = "notification_method", length = 50)
    @Builder.Default
    private String notificationMethod = "PUSH";

    @Column(name = "alert_severity", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AlertSeverity alertSeverity = AlertSeverity.MEDIUM;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}