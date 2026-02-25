package com.farmer.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for individual weather alert.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {
    private String type; // HEAT_WAVE, COLD_WAVE, HEAVY_RAIN, FLOOD, THUNDERSTORM, etc.
    private String severity; // YELLOW, ORANGE, RED
    private String description;
    private String instructions; // Safety instructions for farmers
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String affectedAreas;
}