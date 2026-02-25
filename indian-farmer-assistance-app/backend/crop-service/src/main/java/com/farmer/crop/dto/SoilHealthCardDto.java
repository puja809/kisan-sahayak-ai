package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for Soil Health Card data.
 * 
 * Soil Health Cards contain nutrient analysis results including:
 * - Primary nutrients: Nitrogen (N), Phosphorus (P), Potassium (K)
 * - Secondary nutrients: Sulfur (S)
 * - Micronutrients: Zinc (Zn), Iron (Fe), Copper (Cu), Manganese (Mn), Boron (B)
 * 
 * Validates: Requirement 2.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoilHealthCardDto {
    
    /**
     * Unique Soil Health Card identifier
     */
    private String cardId;
    
    /**
     * Farmer ID associated with this card
     */
    private String farmerId;
    
    /**
     * Survey number of the land parcel
     */
    private String surveyNumber;
    
    /**
     * GPS latitude of sample collection point
     */
    private BigDecimal latitude;
    
    /**
     * GPS longitude of sample collection point
     */
    private BigDecimal longitude;
    
    /**
     * District name
     */
    private String district;
    
    /**
     * State name
     */
    private String state;
    
    /**
     * Village name
     */
    private String village;
    
    /**
     * Date of soil sample collection
     */
    private LocalDate sampleDate;
    
    /**
     * Date of laboratory analysis
     */
    private LocalDate analysisDate;
    
    /**
     * Soil pH value (0-14)
     */
    private BigDecimal ph;
    
    /**
     * Electrical conductivity (dS/m) - indicates salinity
     */
    private BigDecimal electricalConductivity;
    
    /**
     * Organic carbon content (%)
     */
    private BigDecimal organicCarbon;
    
    // Primary Nutrients
    
    /**
     * Available Nitrogen (kg/ha)
     */
    private BigDecimal nitrogenKgHa;
    
    /**
     * Available Phosphorus (kg/ha)
     */
    private BigDecimal phosphorusKgHa;
    
    /**
     * Available Potassium (kg/ha)
     */
    private BigDecimal potassiumKgHa;
    
    // Secondary Nutrients
    
    /**
     * Available Sulfur (ppm)
     */
    private BigDecimal sulfurPpm;
    
    // Micronutrients
    
    /**
     * Available Zinc (ppm)
     */
    private BigDecimal zincPpm;
    
    /**
     * Available Iron (ppm)
     */
    private BigDecimal ironPpm;
    
    /**
     * Available Copper (ppm)
     */
    private BigDecimal copperPpm;
    
    /**
     * Available Manganese (ppm)
     */
    private BigDecimal manganesePpm;
    
    /**
     * Available Boron (ppm)
     */
    private BigDecimal boronPpm;
    
    /**
     * Soil texture classification
     * (Sandy, Loamy, Clayey, Sandy Loam, Clay Loam, etc.)
     */
    private String soilTexture;
    
    /**
     * Soil color (for reference)
     */
    private String soilColor;
    
    /**
     * Previous crop grown
     */
    private String previousCrop;
    
    /**
     * Recommended amendments
     */
    private List<String> recommendedAmendments;
    
    /**
     * Overall soil health status
     * (GOOD, MODERATE, POOR)
     */
    private SoilHealthStatus overallStatus;
    
    /**
     * Laboratory name
     */
    private String testingLaboratory;
    
    /**
     * Whether this data is from an official Soil Health Card
     */
    private Boolean isOfficialCard;
    
    /**
     * Soil health status enum
     */
    public enum SoilHealthStatus {
        GOOD,
        MODERATE,
        POOR
    }
    
    /**
     * Check if micronutrient data is available
     */
    public boolean hasMicronutrientData() {
        return zincPpm != null || ironPpm != null || copperPpm != null 
            || manganesePpm != null || boronPpm != null;
    }
    
    /**
     * Check if secondary nutrient data is available
     */
    public boolean hasSecondaryNutrientData() {
        return sulfurPpm != null;
    }
    
    /**
     * Check if primary nutrient data is available
     */
    public boolean hasPrimaryNutrientData() {
        return nitrogenKgHa != null || phosphorusKgHa != null || potassiumKgHa != null;
    }
    
    /**
     * Get nutrient deficiency status for a specific nutrient
     */
    public NutrientDeficiencyStatus getNutrientStatus(NutrientType nutrient) {
        if (nutrient == null) return null;
        
        return switch (nutrient) {
            case NITROGEN -> getNitrogenStatus();
            case PHOSPHORUS -> getPhosphorusStatus();
            case POTASSIUM -> getPotassiumStatus();
            case SULFUR -> getSulfurStatus();
            case ZINC -> getZincStatus();
            case IRON -> getIronStatus();
            case COPPER -> getCopperStatus();
            case MANGANESE -> getManganeseStatus();
            case BORON -> getBoronStatus();
        };
    }
    
    private NutrientDeficiencyStatus getNitrogenStatus() {
        if (nitrogenKgHa == null) return NutrientDeficiencyStatus.UNKNOWN;
        if (nitrogenKgHa.compareTo(new BigDecimal("280")) < 0) return NutrientDeficiencyStatus.DEFICIENT;
        if (nitrogenKgHa.compareTo(new BigDecimal("560")) < 0) return NutrientDeficiencyStatus.LOW;
        return NutrientDeficiencyStatus.ADEQUATE;
    }
    
    private NutrientDeficiencyStatus getPhosphorusStatus() {
        if (phosphorusKgHa == null) return NutrientDeficiencyStatus.UNKNOWN;
        if (phosphorusKgHa.compareTo(new BigDecimal("10")) < 0) return NutrientDeficiencyStatus.DEFICIENT;
        if (phosphorusKgHa.compareTo(new BigDecimal("25")) < 0) return NutrientDeficiencyStatus.LOW;
        return NutrientDeficiencyStatus.ADEQUATE;
    }
    
    private NutrientDeficiencyStatus getPotassiumStatus() {
        if (potassiumKgHa == null) return NutrientDeficiencyStatus.UNKNOWN;
        if (potassiumKgHa.compareTo(new BigDecimal("108")) < 0) return NutrientDeficiencyStatus.DEFICIENT;
        if (potassiumKgHa.compareTo(new BigDecimal("280")) < 0) return NutrientDeficiencyStatus.LOW;
        return NutrientDeficiencyStatus.ADEQUATE;
    }
    
    private NutrientDeficiencyStatus getSulfurStatus() {
        if (sulfurPpm == null) return NutrientDeficiencyStatus.UNKNOWN;
        if (sulfurPpm.compareTo(new BigDecimal("10")) < 0) return NutrientDeficiencyStatus.DEFICIENT;
        if (sulfurPpm.compareTo(new BigDecimal("20")) < 0) return NutrientDeficiencyStatus.LOW;
        return NutrientDeficiencyStatus.ADEQUATE;
    }
    
    private NutrientDeficiencyStatus getZincStatus() {
        if (zincPpm == null) return NutrientDeficiencyStatus.UNKNOWN;
        if (zincPpm.compareTo(new BigDecimal("0.6")) < 0) return NutrientDeficiencyStatus.DEFICIENT;
        if (zincPpm.compareTo(new BigDecimal("1.2")) < 0) return NutrientDeficiencyStatus.LOW;
        return NutrientDeficiencyStatus.ADEQUATE;
    }
    
    private NutrientDeficiencyStatus getIronStatus() {
        if (ironPpm == null) return NutrientDeficiencyStatus.UNKNOWN;
        if (ironPpm.compareTo(new BigDecimal("4.5")) < 0) return NutrientDeficiencyStatus.DEFICIENT;
        if (ironPpm.compareTo(new BigDecimal("9.0")) < 0) return NutrientDeficiencyStatus.LOW;
        return NutrientDeficiencyStatus.ADEQUATE;
    }
    
    private NutrientDeficiencyStatus getCopperStatus() {
        if (copperPpm == null) return NutrientDeficiencyStatus.UNKNOWN;
        if (copperPpm.compareTo(new BigDecimal("0.2")) < 0) return NutrientDeficiencyStatus.DEFICIENT;
        if (copperPpm.compareTo(new BigDecimal("0.4")) < 0) return NutrientDeficiencyStatus.LOW;
        return NutrientDeficiencyStatus.ADEQUATE;
    }
    
    private NutrientDeficiencyStatus getManganeseStatus() {
        if (manganesePpm == null) return NutrientDeficiencyStatus.UNKNOWN;
        if (manganesePpm.compareTo(new BigDecimal("2.0")) < 0) return NutrientDeficiencyStatus.DEFICIENT;
        if (manganesePpm.compareTo(new BigDecimal("4.0")) < 0) return NutrientDeficiencyStatus.LOW;
        return NutrientDeficiencyStatus.ADEQUATE;
    }
    
    private NutrientDeficiencyStatus getBoronStatus() {
        if (boronPpm == null) return NutrientDeficiencyStatus.UNKNOWN;
        if (boronPpm.compareTo(new BigDecimal("0.4")) < 0) return NutrientDeficiencyStatus.DEFICIENT;
        if (boronPpm.compareTo(new BigDecimal("0.8")) < 0) return NutrientDeficiencyStatus.LOW;
        return NutrientDeficiencyStatus.ADEQUATE;
    }
    
    /**
     * Nutrient type enum
     */
    public enum NutrientType {
        NITROGEN,
        PHOSPHORUS,
        POTASSIUM,
        SULFUR,
        ZINC,
        IRON,
        COPPER,
        MANGANESE,
        BORON
    }
    
    /**
     * Nutrient deficiency status enum
     */
    public enum NutrientDeficiencyStatus {
        ADEQUATE,
        LOW,
        DEFICIENT,
        UNKNOWN
    }
}