package com.farmer.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for weather alerts from IMD API.
 * Contains district-level warnings with severity indicators.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherAlertDto {
    private String district;
    private String state;
    private LocalDateTime issuedAt;
    private LocalDateTime cacheTimestamp; // Timestamp for offline data age indication
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private List<AlertDto> alerts;
    private String overallAlertLevel; // YELLOW, ORANGE, RED
    private String message;
}