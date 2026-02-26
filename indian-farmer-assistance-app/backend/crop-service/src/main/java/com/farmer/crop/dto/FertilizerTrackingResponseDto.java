package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Double areaAcres;

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
        private Double quantityKg;
        private LocalDate applicationDate;
        private String applicationStage;
        private Double cost;
        private Double nitrogenKg;
        private Double phosphorusKg;
        private Double potassiumKg;
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
        private Double totalNitrogenKg;
        private Double totalPhosphorusKg;
        private Double totalPotassiumKg;
        private Double totalSulfurKg;
        private Double totalZincKg;
        private Double totalQuantityKg;
        private Double totalCost;
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
        private Double totalCost;
        private Double costPerAcre;
        private Double costPerKgNutrient;
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
        private Double cost;
    }

    /**
     * Nutrient efficiency DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutrientEfficiencyDto {
        private Double yieldPerKgNitrogen;
        private Double yieldPerKgPhosphorus;
        private Double yieldPerKgPotassium;
        private String efficiencyRating;
        private List<String> improvementSuggestions;
    }
}