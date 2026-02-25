package com.farmer.iot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for sensor reading data from IoT devices.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReadingRequest {

    @NotNull(message = "Device ID is required")
    private Long deviceId;

    private LocalDateTime readingTimestamp;

    private Double soilMoisturePercent;

    private Double temperatureCelsius;

    private Double humidityPercent;

    private Double phLevel;

    private Double ecValue;

    private Double npkNitrogen;

    private Double npkPhosphorus;

    private Double npkPotassium;
}