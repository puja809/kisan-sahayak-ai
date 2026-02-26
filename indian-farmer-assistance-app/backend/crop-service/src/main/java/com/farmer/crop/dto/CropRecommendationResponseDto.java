package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for crop recommendation response.
 * 
 * Contains ranked crop recommendations with detailed suitability information,
 * yield estimates, and recommendations for the farmer.
 * 
 * Validates: Requirements 2.5, 2.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropRecommendationResponseDto {
    
    /**
     * Whether the recommendation was successful
     */
    private boolean success;
    
    /**
     * Error message if recommendation failed
     */
    private String errorMessage;
    
    /**
     * Timestamp when recommendations were generated
     */
    private LocalDateTime generatedAt;
    
    /**
     * Farmer ID for which recommendations were generated
     */
    private String farmerId;
    
    /**
     * Location used for recommendations
     */
    private String location;
    
    /**
     * Agro-ecological zone used for recommendations
     */
    private String agroEcologicalZone;
    
    /**
     * Season for which recommendations were generated
     */
    private CropRecommendationRequestDto.Season season;
    
    /**
     * List of recommended crops, ranked by suitability score
     */
    private List<RecommendedCropDto> recommendations;
    
    /**
     * Number of recommendations returned
     */
    private int recommendationCount;
    
    /**
     * Whether soil health card data was used
     */
    private Boolean soilHealthCardUsed;
    
    /**
     * Climate risk summary
     */
    private ClimateRiskSummary climateRiskSummary;
    
    /**
     * Market data integration status
     */
    private MarketDataStatus marketDataStatus;
    
    /**
     * DTO for individual recommended crop
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedCropDto {
        
        /**
         * Rank of the recommendation (1 = best)
         */
        private int rank;
        
        /**
         * GAEZ crop suitability data
         */
        private GaezCropSuitabilityDto gaezSuitability;
        
        /**
         * Overall suitability score (0-100), combining all factors
         */
        private Double overallSuitabilityScore;
        
        /**
         * Suitability score adjusted for irrigation type
         */
        private Double irrigationAdjustedScore;
        
        /**
         * Suitability score adjusted for soil health (if available)
         */
        private Double soilHealthAdjustedScore;
        
        /**
         * Expected yield per acre (quintals)
         */
        private Double expectedYieldPerAcre;
        
        /**
         * Expected revenue per acre (INR)
         */
        private Double expectedRevenuePerAcre;
        
        /**
         * Water requirement per acre (liters)
         */
        private Double waterRequirementPerAcre;
        
        /**
         * Growing duration (days)
         */
        private Integer growingDurationDays;
        
        /**
         * Climate risk level
         */
        private GaezCropSuitabilityDto.ClimateRiskLevel climateRiskLevel;
        
        /**
         * Whether this crop is suitable for the season
         */
        private Boolean seasonSuitable;
        
        /**
         * Recommended varieties for this location
         */
        private List<String> recommendedVarieties;
        
        /**
         * Specific recommendations based on soil health
         */
        private List<String> soilHealthRecommendations;
        
        /**
         * Potential yield gap (expected vs potential)
         */
        private Double potentialYieldGap;
        
        /**
         * Input cost estimate per acre (INR)
         */
        private Double estimatedInputCost;
        
        /**
         * Net profit estimate per acre (INR)
         */
        private Double estimatedNetProfit;
        
        /**
         * Risk factors to consider
         */
        private List<String> riskFactors;
        
        /**
         * Additional notes
         */
        private String notes;
    }
    
    /**
     * Climate risk summary for the location
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClimateRiskSummary {
        
        /**
         * Overall climate risk level for the location
         */
        private GaezCropSuitabilityDto.ClimateRiskLevel overallRiskLevel;
        
        /**
         * Number of high-risk crops
         */
        private int highRiskCropCount;
        
        /**
         * Number of medium-risk crops
         */
        private int mediumRiskCropCount;
        
        /**
         * Number of low-risk crops
         */
        private int lowRiskCropCount;
        
        /**
         * Key climate risks to consider
         */
        private List<String> keyClimateRisks;
        
        /**
         * Recommended risk mitigation strategies
         */
        private List<String> mitigationStrategies;
    }
    
    /**
     * Market data integration status
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketDataStatus {
        
        /**
         * Whether market data was successfully integrated
         */
        private boolean integrated;
        
        /**
         * Number of crops with market data
         */
        private int cropsWithMarketData;
        
        /**
         * Price trend summary
         */
        private String priceTrendSummary;
        
        /**
         * Market recommendations
         */
        private List<String> marketRecommendations;
    }
}