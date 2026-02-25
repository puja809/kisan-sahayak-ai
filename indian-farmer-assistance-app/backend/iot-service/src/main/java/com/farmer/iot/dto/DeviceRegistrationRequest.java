package com.farmer.iot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO for device registration/provisioning request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceRegistrationRequest {

    @NotBlank(message = "Device name is required")
    @Size(max = 100, message = "Device name must be at most 100 characters")
    private String deviceName;

    @Size(max = 50, message = "Device type must be at most 50 characters")
    private String deviceType;

    @Size(max = 100, message = "Manufacturer must be at most 100 characters")
    private String manufacturer;

    @Size(max = 100, message = "Model must be at most 100 characters")
    private String model;

    private Long farmId;

    private Boolean dataOwnershipConfirmed;
}