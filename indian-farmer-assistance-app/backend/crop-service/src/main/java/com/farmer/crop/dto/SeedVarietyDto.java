package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for state-released seed varieties.
 * 
 * Contains information about seed varieties released by state agricultural
 * universities and institutes, suitable for specific locations.
 * 
 * Validates: Requirement 2.9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeedVarietyDto {
    
    /**
     * Unique variety identifier
     */
    private String varietyId;
    
    /**
     * Crop code
     */
    private String cropCode;
    
    /**
     * Crop name
     */
    private String cropName;
    
    /**
     * Variety name
     */
    private String varietyName;
    
    /**
     * Variety name in local language
     */
    private String varietyNameLocal;
    
    /**
     * Releasing organization
     */
    private String releasingInstitute;
    
    /**
     * Release year
     */
    private Integer releaseYear;
    
    /**
     * State(s) where this variety is recommended
     */
    private List<String> recommendedStates;
    
    /**
     * Agro-ecological zones where this variety is suitable
     */
    private List<String> suitableZones;
    
    /**
     * Season suitability
     */
    private SeasonSuitability seasonSuitability;
    
    /**
     * Maturity duration (days)
     */
    private Integer maturityDays;
    
    /**
     * Average yield (quintals per hectare)
     */
    private Double averageYieldQtlHa;
    
    /**
     * Potential yield (quintals per hectare)
     */
    private Double potentialYieldQtlHa;
    
    /**
     * Special characteristics
     */
    private List<String> characteristics;
    
    /**
     * Disease resistance
     */
    private List<String> diseaseResistance;
    
    /**
     * Climate resilience features
     */
    private List<String> climateResilience;
    
    /**
     * Water requirement (mm)
     */
    private Double waterRequirementMm;
    
    /**
     * Whether this is a drought-tolerant variety
     */
    private Boolean droughtTolerant;
    
    /**
     * Whether this is a flood-tolerant variety
     */
    private Boolean floodTolerant;
    
    /**
     * Whether this is a heat-tolerant variety
     */
    private Boolean heatTolerant;
    
    /**
     * Seed rate (kg per hectare)
     */
    private Double seedRateKgHa;
    
    /**
     * Recommended spacing (cm)
     */
    private String spacing;
    
    /**
     * Any special cultivation notes
     */
    private String cultivationNotes;
    
    /**
     * Source/reference URL
     */
    private String sourceUrl;
    
    /**
     * Whether this variety is currently available
     */
    private Boolean isAvailable;
    
    /**
     * Approximate seed cost (INR per kg)
     */
    private Double seedCostPerKg;
    
    /**
     * Season suitability
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonSuitability {
        private Boolean kharif;
        private Boolean rabi;
        private Boolean zaid;
    }
}