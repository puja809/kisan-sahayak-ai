package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for GAEZ v4 crop suitability data.
 * 
 * The GAEZ (Global Agro-Ecological Zones) framework provides crop suitability
 * assessments based on climate, soil, terrain, and water availability.
 * 
 * Validates: Requirements 2.2, 2.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GaezCropSuitabilityDto {
    
    /**
     * GAEZ crop code (e.g., "RICE", "WHEAT", "COTTON")
     */
    private String cropCode;
    
    /**
     * Crop name in English
     */
    private String cropName;
    
    /**
     * Crop name in Hindi/local language
     */
    private String cropNameLocal;
    
    /**
     * Overall suitability score (0-100)
     * Combines all suitability factors
     */
    private Double overallSuitabilityScore;
    
    /**
     * Suitability classification
     * HIGHLY_SUITABLE, SUITABLE, MARGINALLY_SUITABLE, NOT_SUITABLE
     */
    private SuitabilityClassification suitabilityClassification;
    
    /**
     * Climate suitability score (0-100)
     * Based on temperature, precipitation, growing season
     */
    private Double climateSuitabilityScore;
    
    /**
     * Soil suitability score (0-100)
     * Based on soil type, pH, organic matter, fertility
     */
    private Double soilSuitabilityScore;
    
    /**
     * Terrain suitability score (0-100)
     * Based on slope, elevation, landform
     */
    private Double terrainSuitabilityScore;
    
    /**
     * Water availability suitability score (0-100)
     * Based on irrigation potential, water stress
     */
    private Double waterSuitabilityScore;
    
    /**
     * Potential yield under rain-fed conditions (kg/ha)
     */
    private Double rainfedPotentialYield;
    
    /**
     * Potential yield under irrigated conditions (kg/ha)
     */
    private Double irrigatedPotentialYield;
    
    /**
     * Expected yield range (kg/ha) - minimum
     */
    private Double expectedYieldMin;
    
    /**
     * Expected yield range (kg/ha) - expected
     */
    private Double expectedYieldExpected;
    
    /**
     * Expected yield range (kg/ha) - maximum
     */
    private Double expectedYieldMax;
    
    /**
     * Water requirements (mm per growing season)
     */
    private Double waterRequirementsMm;
    
    /**
     * Growing season duration (days)
     */
    private Integer growingSeasonDays;
    
    /**
     * Kharif season suitability
     */
    private Boolean kharifSuitable;
    
    /**
     * Rabi season suitability
     */
    private Boolean rabiSuitable;
    
    /**
     * Zaid season suitability
     */
    private Boolean zaidSuitable;
    
    /**
     * Climate risk level (LOW, MEDIUM, HIGH)
     */
    private ClimateRiskLevel climateRiskLevel;
    
    /**
     * GAEZ data version
     */
    private String dataVersion;
    
    /**
     * Data resolution (e.g., "5 arc-min", "district")
     */
    private String dataResolution;
    
    /**
     * Suitability classification enum
     */
    public enum SuitabilityClassification {
        HIGHLY_SUITABLE,    // Score >= 80
        SUITABLE,           // Score 60-79
        MARGINALLY_SUITABLE, // Score 40-59
        NOT_SUITABLE        // Score < 40
    }
    
    /**
     * Climate risk level enum
     */
    public enum ClimateRiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}