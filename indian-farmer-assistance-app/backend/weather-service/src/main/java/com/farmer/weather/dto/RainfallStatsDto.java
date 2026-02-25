package com.farmer.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * DTO for rainfall statistics from IMD API.
 * Contains actual vs normal rainfall comparison and percentage departure analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RainfallStatsDto {
    private String district;
    private String state;
    private YearMonth month;
    private LocalDateTime fetchedAt;
    private LocalDateTime cacheTimestamp; // Timestamp for offline data age indication
    private Double actualRainfallMm;
    private Double normalRainfallMm;
    private Double departurePercent;
    private String departureCategory; // DEFICIT, NORMAL, EXCESS, LARGE_EXCESS, LARGE_DEFICIT
    private Double seasonalActualMm;
    private Double seasonalNormalMm;
    private Double seasonalDeparturePercent;
    private String seasonalStatus;
    private MonthlyRainfallData[] monthlyData;
    private WeeklyRainfallData[] weeklyData;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRainfallData {
        private YearMonth month;
        private Double actualMm;
        private Double normalMm;
        private Double departurePercent;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyRainfallData {
        private int weekNumber;
        private Double actualMm;
        private Double normalMm;
        private Double departurePercent;
    }
}