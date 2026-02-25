package com.farmer.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for nowcast data from IMD API.
 * Contains 0-3 hour forecasts for thunderstorms, squalls, and localized precipitation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NowcastDto {
    private String district;
    private String state;
    private LocalDateTime issuedAt;
    private LocalDateTime cacheTimestamp; // Timestamp for offline data age indication
    private LocalDateTime validUntil;
    private List<NowcastAlertDto> alerts;
    private String overallSituation;
    private Double probabilityOfPrecipitation;
}