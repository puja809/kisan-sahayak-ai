package com.farmer.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for AWS/ARG real-time data from IMD.
 * Contains observations from Automated Weather Stations and Rain Gauges for hyper-local precision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsArgDataDto {
    private String stationId;
    private String stationName;
    private String stationType; // AWS (Automated Weather Station), ARG (Automatic Rain Gauge)
    private String district;
    private String state;
    private Double latitude;
    private Double longitude;
    private LocalDateTime observationTime;
    private Double temperatureCelsius;
    private Double relativeHumidity;
    private Double windSpeedKmph;
    private String windDirection;
    private Double pressureHpa;
    private Double solarRadiationWM2;
    private Double soilTemperatureCelsius;
    private Double soilMoisturePercent;
    private RainfallData rainfall;
    private String dataQuality;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RainfallData {
        private Double lastHourMm;
        private Double last3HoursMm;
        private Double last6HoursMm;
        private Double last12HoursMm;
        private Double last24HoursMm;
        private Double cumulativeTodayMm;
    }
}