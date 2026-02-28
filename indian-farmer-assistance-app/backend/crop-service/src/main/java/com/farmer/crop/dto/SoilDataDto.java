package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for soil data from Kaegro API.
 * Contains comprehensive soil information including texture, physical, chemical, and water properties.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoilDataDto {
    
    /**
     * Soil texture class (e.g., "Sandy Clay Loam")
     */
    private String textureClass;
    
    /**
     * FAO soil classification (e.g., "Lixisols")
     */
    private String faoClassification;
    
    /**
     * Sand percentage
     */
    private Double sandPct;
    
    /**
     * Silt percentage
     */
    private Double siltPct;
    
    /**
     * Clay percentage
     */
    private Double clayPct;
    
    /**
     * Bulk density (g/cm³)
     */
    private Double bulkDensityGCm3;
    
    /**
     * Soil pH (H2O)
     */
    private Double phH2o;
    
    /**
     * Organic matter percentage
     */
    private Double organicMatterPct;
    
    /**
     * Nitrogen content (g/kg)
     */
    private Double nitrogenGKg;
    
    /**
     * Cation exchange capacity (cmol/kg)
     */
    private Double cecCmolKg;
    
    /**
     * Field capacity (volumetric %)
     */
    private Double capacityFieldVolPct;
    
    /**
     * Wilting point (volumetric %)
     */
    private Double capacityWiltVolPct;
    
    /**
     * API response latency in seconds
     */
    private Double latencySeconds;
    
    /**
     * Get soil fertility rating based on organic matter and nitrogen
     */
    public String getFertilityRating() {
        if (organicMatterPct == null || nitrogenGKg == null) {
            return "UNKNOWN";
        }
        
        if (organicMatterPct >= 3.0 && nitrogenGKg >= 2.0) {
            return "HIGH";
        } else if (organicMatterPct >= 1.5 && nitrogenGKg >= 1.0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Get soil pH rating
     */
    public String getPhRating() {
        if (phH2o == null) {
            return "UNKNOWN";
        }
        
        if (phH2o < 5.5) {
            return "ACIDIC";
        } else if (phH2o <= 7.5) {
            return "NEUTRAL";
        } else {
            return "ALKALINE";
        }
    }
    
    /**
     * Get water holding capacity rating
     */
    public String getWaterHoldingCapacity() {
        if (capacityFieldVolPct == null) {
            return "UNKNOWN";
        }
        
        if (capacityFieldVolPct >= 30) {
            return "HIGH";
        } else if (capacityFieldVolPct >= 20) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
