package com.farmer.iot.dto;

import com.farmer.iot.entity.IotDevice;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for device information response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {

    private Long id;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String manufacturer;
    private String model;
    private String firmwareVersion;
    private String status;
    private LocalDateTime lastSeen;
    private String configuration;
    private String capabilities;
    private Long farmId;
    private Boolean dataOwnershipConfirmed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DeviceResponse fromEntity(IotDevice device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .manufacturer(device.getManufacturer())
                .model(device.getModel())
                .firmwareVersion(device.getFirmwareVersion())
                .status(device.getStatus().name())
                .lastSeen(device.getLastSeen())
                .configuration(device.getConfiguration())
                .capabilities(device.getCapabilities())
                .farmId(device.getFarmId())
                .dataOwnershipConfirmed(device.getDataOwnershipConfirmed())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }
}