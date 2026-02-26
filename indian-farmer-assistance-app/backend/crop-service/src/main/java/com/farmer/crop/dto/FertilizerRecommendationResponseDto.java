package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for fertilizer recommendation.
 * 
 * Contains complete fertilizer recommendations including:
 * - Fertilizer type, quantity per acre, timing
 * - Split application schedules (basal dose, top dressing)
 * - Organic alternatives
 * 
 * Validates: Requirements 11C.1, 11C.2, 11C.3, 11C.4, 11C.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FertilizerRecommendationResponseDto {

    /**
     * Whether the recommendation was successful
     */
    private boolean success;

    /**
     * Timestamp when recommendation was generated
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
     * Area in acres
     */
    private Double areaAcres;

    /**
     * Whether soil health card data was used
     */
    private boolean soilHealthCardUsed;

    /**
     * Nutrient deficiencies detected from soil test
     */
    private List<NutrientDeficiencyDto> nutrientDeficiencies;

    /**
     * Total nutrient requirements
     */
    private NutrientRequirementsDto nutrientRequirements;

    /**
     * Recommended fertilizer applications
     */
    private List<RecommendedFertilizerDto> recommendations;

    /**
     * Split application schedule
     */
    private List<ApplicationScheduleDto> applicationSchedule;

    /**
     * Organic alternatives
     */
    private List<OrganicAlternativeDto> organicAlternatives;

    /**
     * Cost estimate
     */
    private Double estimatedCost;

    /**
     * Error message if unsuccessful
     */
    private String errorMessage;

    /**
     * Nutrient deficiency DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutrientDeficiencyDto {
        private String nutrient;
        private String currentLevel;
        private String requiredLevel;
        private String deficiency;
        private String recommendation;
    }

    /**
     * Nutrient requirements DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutrientRequirementsDto {
        private Double nitrogenKgPerAcre;
        private Double phosphorusKgPerAcre;
        private Double potassiumKgPerAcre;
        private Double sulfurKgPerAcre;
        private Double zincKgPerAcre;
    }

    /**
     * Recommended fertilizer DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedFertilizerDto {
        private String fertilizerType;
        private String fertilizerCategory;
        private Double quantityKgPerAcre;
        private String applicationTiming;
        private String applicationStage;
        private Double nitrogenContent;
        private Double phosphorusContent;
        private Double potassiumContent;
        private Double costPerAcre;
        private String notes;
        private String source;
    }

    /**
     * Application schedule DTO for split applications
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationScheduleDto {
        private String applicationName;
        private String applicationStage;
        private LocalDate suggestedDate;
        private String description;
        private List<RecommendedFertilizerDto> fertilizers;
        private Double totalCost;
    }

    /**
     * Organic alternative DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganicAlternativeDto {
        private String alternativeType;
        private String name;
        private Double quantityKgPerAcre;
        private String benefits;
        private String applicationMethod;
        private Double costPerAcre;
        private String notes;
    }
}