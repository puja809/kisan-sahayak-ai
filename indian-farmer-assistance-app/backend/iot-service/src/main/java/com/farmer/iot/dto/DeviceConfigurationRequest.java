package com.farmer.iot.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO for device configuration update request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceConfigurationRequest {

    @Size(max = 100, message = "Device name must be at most 100 characters")
    private String deviceName;

    private String configuration;

    private String status;
}