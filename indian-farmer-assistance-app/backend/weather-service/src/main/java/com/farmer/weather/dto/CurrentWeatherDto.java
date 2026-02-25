package com.farmer.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for current weather data from IMD API.
 * Contains real-time cloud coverage, 24-hour cumulative rainfall, and wind vectors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentWeatherDto {
    private String district;
    private String state;
    private LocalDateTime observationTime;
    private LocalDateTime cacheTimestamp; // Timestamp for offline data age indication
    private Integer cloudCoverage; // 0-8 scale
    private Double rainfall24HoursMm;
    private Double windSpeedKmph;
    private String windDirection;
    private Double temperatureCelsius;
    private Double humidity;
    private Double pressureHpa;
    private Double visibilityKm;
    private String weatherDescription;
}