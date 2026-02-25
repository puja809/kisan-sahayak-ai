package com.farmer.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for 7-day weather forecast from IMD API.
 * Contains forecast data for the next 7 days for a district.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SevenDayForecastDto {
    private String district;
    private String state;
    private LocalDateTime fetchedAt;
    private LocalDateTime cacheTimestamp; // Timestamp for offline data age indication
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private List<ForecastDayDto> forecastDays;
}