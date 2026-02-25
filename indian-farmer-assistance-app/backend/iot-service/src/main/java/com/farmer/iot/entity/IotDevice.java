package com.farmer.iot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing an IoT device on the farm.
 * Stores device metadata, status, and configuration.
 */
@Entity
@Table(name = "iot_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IotDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", unique = true, nullable = false, length = 100)
    private String deviceId;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "configuration", columnDefinition = "JSON")
    private String configuration;

    @Column(name = "capabilities", columnDefinition = "JSON")
    private String capabilities;

    @Column(name = "owner_farmer_id", nullable = false, length = 50)
    private String ownerFarmerId;

    @Column(name = "farm_id")
    private Long farmId;

    @Column(name = "data_ownership_confirmed")
    @Builder.Default
    private Boolean dataOwnershipConfirmed = false;

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

    public enum DeviceStatus {
        ACTIVE,
        INACTIVE,
        MAINTENANCE,
        OFFLINE
    }
}