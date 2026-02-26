package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for crop recommendation request.
 * 
 * Combines location data, soil health card data, and farmer preferences
 * to generate personalized crop recommendations.
 * 
 * Validates: Requirements 2.2, 2.3, 2.4, 2.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropRecommendationRequestDto {
    
    /**
     * Farmer's unique identifier
     */
    private String farmerId;
    
    /**
     * District name (optional if GPS coordinates provided)
     */
    private String district;
    
    /**
     * State name (optional if GPS coordinates provided)
     */
    private String state;
    
    /**
     * GPS latitude of farm location
     */
    private Double latitude;
    
    /**
     * GPS longitude of farm location
     */
    private Double longitude;
    
    /**
     * Total farm area in acres
     */
    private Double farmAreaAcres;
    
    /**
     * Soil type (if known, otherwise will use GAEZ data)
     */
    private String soilType;
    
    /**
     * Irrigation type
     * (RAINFED, DRIP, SPRINKLER, CANAL, BOREWELL, MIXED)
     */
    private IrrigationType irrigationType;
    
    /**
     * Whether soil health card data is available
     */
    private Boolean hasSoilHealthCard;
    
    /**
     * Soil health card data (if available)
     */
    private SoilHealthCardDto soilHealthCard;
    
    /**
     * Season for which to generate recommendations
     * (KHARIF, RABI, ZAID, ALL)
     */
    private Season season;
    
    /**
     * Farm's agro-ecological zone code (if known)
     */
    private String agroEcologicalZoneCode;
    
    /**
     * Previous crop grown (for rotation planning)
     */
    private String previousCrop;
    
    /**
     * Minimum expected yield (quintals per acre) - for filtering
     */
    private Double minExpectedYield;
    
    /**
     * Maximum water stress tolerance
     * (LOW, MEDIUM, HIGH) - higher means more tolerant to water stress
     */
    private WaterStressTolerance waterStressTolerance;
    
    /**
     * Whether to include climate risk assessment
     */
    private Boolean includeClimateRiskAssessment;
    
    /**
     * Whether to include market data integration
     */
    private Boolean includeMarketData;
    
    /**
     * Farmer's preferred crops (to prioritize)
     */
    private List<String> preferredCrops;
    
    /**
     * Crops to exclude from recommendations
     */
    private List<String> excludeCrops;
    
    /**
     * Minimum suitability score threshold (0-100)
     */
    private Double minSuitabilityScore;
    
    /**
     * Irrigation type enum
     */
    public enum IrrigationType {
        RAINFED,
        DRIP,
        SPRINKLER,
        CANAL,
        BOREWELL,
        MIXED
    }
    
    /**
     * Season enum for Indian agricultural calendar
     */
    public enum Season {
        KHARIF,     // Monsoon season (June-October)
        RABI,       // Winter season (October-March)
        ZAID,       // Summer season (March-June)
        ALL         // All seasons
    }
    
    /**
     * Water stress tolerance enum
     */
    public enum WaterStressTolerance {
        LOW,        // Sensitive to water stress
        MEDIUM,     // Moderate tolerance
        HIGH        // High tolerance to water stress
    }
    
    /**
     * Check if location data is sufficient
     */
    public boolean hasLocationData() {
        boolean hasDistrictState = district != null && state != null;
        boolean hasCoordinates = latitude != null && longitude != null;
        return hasDistrictState || hasCoordinates;
    }
    
    /**
     * Check if soil health data is available
     */
    public boolean hasSoilHealthData() {
        return Boolean.TRUE.equals(hasSoilHealthCard) && soilHealthCard != null;
    }
}