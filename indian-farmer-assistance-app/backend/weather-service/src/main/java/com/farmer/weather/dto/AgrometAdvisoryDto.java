package com.farmer.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for agromet advisories from IMD API.
 * Contains crop-stage-based weather advisories including heat stress indices and evapotranspiration (ET₀).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgrometAdvisoryDto {
    private String district;
    private String state;
    private LocalDateTime issuedAt;
    private LocalDateTime cacheTimestamp; // Timestamp for offline data age indication
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String cropStage; // e.g., SOWING, VEGETATIVE, FLOWERING, MATURITY, HARVEST
    private String majorCrop;
    private List<AdvisoryDto> advisories;
    private HeatStressIndex heatStressIndex;
    private EvapotranspirationData evapotranspiration;
    private String generalAdvice;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvisoryDto {
        private String type; // IRRIGATION, PEST, DISEASE, FERTILIZER, HARVEST, PROTECTION
        private String title;
        private String description;
        private String priority; // HIGH, MEDIUM, LOW
        private List<String> recommendations;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatStressIndex {
        private Double indexValue;
        private String category; // LOW, MODERATE, HIGH, EXTREME
        private String description;
        private List<String> precautions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvapotranspirationData {
        private Double et0MmPerDay; // Reference evapotranspiration
        private Double etcMmPerDay; // Crop evapotranspiration
        private String irrigationAdvice;
        private Double suggestedIrrigationMm;
        private String bestIrrigationTime;
    }
}