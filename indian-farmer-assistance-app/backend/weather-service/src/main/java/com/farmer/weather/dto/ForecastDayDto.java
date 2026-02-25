package com.farmer.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for a single day forecast data from IMD API.
 * Contains all weather parameters for a 24-hour period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDayDto {
    private LocalDate date;
    private Double maxTempCelsius;
    private Double minTempCelsius;
    private Double humidity0830;
    private Double humidity1730;
    private Double rainfallMm;
    private Double windSpeedKmph;
    private String windDirection;
    private Integer cloudCoverage; // 0-8 scale (nebulosity)
    private String sunriseTime;
    private String sunsetTime;
    private String moonriseTime;
    private String moonsetTime;
}