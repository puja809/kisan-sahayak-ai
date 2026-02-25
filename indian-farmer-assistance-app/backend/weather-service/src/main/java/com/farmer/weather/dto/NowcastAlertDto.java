package com.farmer.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for individual nowcast alert.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NowcastAlertDto {
    private String alertType; // THUNDERSTORM, SQUALL, HEAVY_RAIN, LIGHT_RAIN
    private String severity; // LIGHT, MODERATE, HEAVY, VERY_HEAVY
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double expectedRainfallMm;
    private Double expectedWindSpeedKmph;
    private String direction;
}