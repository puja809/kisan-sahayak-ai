package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for fertilizer tracking and nutrient calculation.
 * 
 * Contains:
 * - Fertilizer application history
 * - Total nutrient input (N, P, K)
 * - Over/under-application highlights
 * - Cost trends and nutrient efficiency
 * 
 * Validates: Requirements 11C.6, 11C.7, 11C.8, 11C.11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FertilizerTrackingResponseDto {

    /**
     * Whether the request was successful
     */
    private boolean success;

    /**
     * Timestamp of the response
     */
    private LocalDateTime generatedAt;

    /**
     * Farmer ID
     */
    private String farmerId;

    /**
     * Crop ID
     */
    private Long cropId;

    /**
     * Crop name
     */
    private String cropName;

    /**
     * Total area in acres
     */
    private BigDecimal areaAcres;

    /**
     * Fertilizer application history
     */
    private List<FertilizerApplicationDto> applications;

    /**
     * Total nutrient input across all applications
     */
    private TotalNutrientInputDto totalNutrientInput;

    /**
     * Recommended nutrient requirements for comparison
     */
    private FertilizerRecommendationResponseDto.NutrientRequirementsDto recommendedRequirements;

    /**
     * Application status (under, optimal, over)
     */
    private NutrientApplicationStatusDto applicationStatus;

    /**
     * Cost summary
     */
    private CostSummaryDto costSummary;

    /**
     * Nutrient efficiency metrics
     */
    private NutrientEfficiencyDto nutrientEfficiency;

    /**
     * Error message if unsuccessful
     */
    private String errorMessage;

    /**
     * Individual fertilizer application DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FertilizerApplicationDto {
        private Long id;
        private String fertilizerType;
        private String fertilizerCategory;
        private BigDecimal quantityKg;
        private LocalDate applicationDate;
        private String applicationStage;
        private BigDecimal cost;
        private BigDecimal nitrogenKg;
        private BigDecimal phosphorusKg;
        private BigDecimal potassiumKg;
        private String notes;
        private LocalDateTime createdAt;
    }

    /**
     * Total nutrient input DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalNutrientInputDto {
        private BigDecimal totalNitrogenKg;
        private BigDecimal totalPhosphorusKg;
        private BigDecimal totalPotassiumKg;
        private BigDecimal totalSulfurKg;
        private BigDecimal totalZincKg;
        private BigDecimal totalQuantityKg;
        private BigDecimal totalCost;
    }

    /**
     * Nutrient application status DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutrientApplicationStatusDto {
        private String nitrogenStatus;
        private String phosphorusStatus;
        private String potassiumStatus;
        private List<String> warnings;
        private List<String> recommendations;
    }

    /**
     * Cost summary DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostSummaryDto {
        private BigDecimal totalCost;
        private BigDecimal costPerAcre;
        private BigDecimal costPerKgNutrient;
        private String costTrend;
        private List<CostTrendPointDto> costTrendHistory;
    }

    /**
     * Cost trend point DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostTrendPointDto {
        private LocalDate date;
        private BigDecimal cost;
    }

    /**
     * Nutrient efficiency DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutrientEfficiencyDto {
        private BigDecimal yieldPerKgNitrogen;
        private BigDecimal yieldPerKgPhosphorus;
        private BigDecimal yieldPerKgPotassium;
        private String efficiencyRating;
        private List<String> improvementSuggestions;
    }
}