package com.farmer.iot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for alert configuration request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertConfigurationRequest {

    @NotNull(message = "Device ID is required")
    private Long deviceId;

    @NotBlank(message = "Sensor type is required")
    private String sensorType;

    private Double minThreshold;

    private Double maxThreshold;

    private String thresholdUnit;

    private Boolean alertEnabled;

    private String notificationMethod;

    private String alertSeverity;
}