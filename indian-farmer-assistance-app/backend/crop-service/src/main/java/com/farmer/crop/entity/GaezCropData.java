package com.farmer.crop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for storing GAEZ v4 crop suitability data.
 * 
 * The GAEZ (Global Agro-Ecological Zones) framework provides comprehensive
 * crop suitability assessments based on climate, soil, terrain, and water availability.
 * 
 * Validates: Requirements 2.2, 2.3
 */
@Entity
@Table(name = "gaez_crop_data", indexes = {
    @Index(name = "idx_gaez_crop_code", columnList = "cropCode"),
    @Index(name = "idx_gaez_zone_code", columnList = "zoneCode"),
    @Index(name = "idx_gaez_climate_suitability", columnList = "climateSuitabilityScore"),
    @Index(name = "idx_gaez_overall_suitability", columnList = "overallSuitabilityScore")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GaezCropData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * GAEZ crop code (e.g., "RICE", "WHEAT", "COTTON")
     */
    @Column(name = "crop_code", nullable = false, length = 50)
    private String cropCode;

    /**
     * Crop name in English
     */
    @Column(name = "crop_name", nullable = false, length = 100)
    private String cropName;

    /**
     * Crop name in Hindi
     */
    @Column(name = "crop_name_hindi", length = 100)
    private String cropNameHindi;

    /**
     * GAEZ zone code (e.g., "AEZ-05")
     */
    @Column(name = "zone_code", nullable = false, length = 50)
    private String zoneCode;

    /**
     * Overall suitability score (0-100)
     */
    @Column(name = "overall_suitability_score", precision = 5, scale = 2)
    private BigDecimal overallSuitabilityScore;

    /**
     * Suitability classification
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "suitability_classification", length = 30)
    private SuitabilityClassification suitabilityClassification;

    /**
     * Climate suitability score (0-100)
     */
    @Column(name = "climate_suitability_score", precision = 5, scale = 2)
    private BigDecimal climateSuitabilityScore;

    /**
     * Soil suitability score (0-100)
     */
    @Column(name = "soil_suitability_score", precision = 5, scale = 2)
    private BigDecimal soilSuitabilityScore;

    /**
     * Terrain suitability score (0-100)
     */
    @Column(name = "terrain_suitability_score", precision = 5, scale = 2)
    private BigDecimal terrainSuitabilityScore;

    /**
     * Water availability suitability score (0-100)
     */
    @Column(name = "water_suitability_score", precision = 5, scale = 2)
    private BigDecimal waterSuitabilityScore;

    /**
     * Potential yield under rain-fed conditions (kg/ha)
     */
    @Column(name = "rainfed_potential_yield", precision = 10, scale = 2)
    private BigDecimal rainfedPotentialYield;

    /**
     * Potential yield under irrigated conditions (kg/ha)
     */
    @Column(name = "irrigated_potential_yield", precision = 10, scale = 2)
    private BigDecimal irrigatedPotentialYield;

    /**
     * Water requirements (mm per growing season)
     */
    @Column(name = "water_requirements_mm", precision = 10, scale = 2)
    private BigDecimal waterRequirementsMm;

    /**
     * Growing season duration (days)
     */
    @Column(name = "growing_season_days")
    private Integer growingSeasonDays;

    /**
     * Kharif season suitability
     */
    @Column(name = "kharif_suitable")
    private Boolean kharifSuitable;

    /**
     * Rabi season suitability
     */
    @Column(name = "rabi_suitable")
    private Boolean rabiSuitable;

    /**
     * Zaid season suitability
     */
    @Column(name = "zaid_suitable")
    private Boolean zaidSuitable;

    /**
     * Climate risk level
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "climate_risk_level", length = 20)
    private ClimateRiskLevel climateRiskLevel;

    /**
     * Minimum temperature tolerance (°C)
     */
    @Column(name = "min_temp_tolerance", precision = 5, scale = 2)
    private BigDecimal minTempTolerance;

    /**
     * Maximum temperature tolerance (°C)
     */
    @Column(name = "max_temp_tolerance", precision = 5, scale = 2)
    private BigDecimal maxTempTolerance;

    /**
     * Minimum rainfall requirement (mm)
     */
    @Column(name = "min_rainfall_mm", precision = 10, scale = 2)
    private BigDecimal minRainfallMm;

    /**
     * Maximum rainfall tolerance (mm)
     */
    @Column(name = "max_rainfall_mm", precision = 10, scale = 2)
    private BigDecimal maxRainfallMm;

    /**
     * Soil pH minimum
     */
    @Column(name = "soil_ph_min", precision = 4, scale = 2)
    private BigDecimal soilPhMin;

    /**
     * Soil pH maximum
     */
    @Column(name = "soil_ph_max", precision = 4, scale = 2)
    private BigDecimal soilPhMax;

    /**
     * Maximum slope tolerance (%)
     */
    @Column(name = "max_slope_percent", precision = 5, scale = 2)
    private BigDecimal maxSlopePercent;

    /**
     * GAEZ data version
     */
    @Column(name = "data_version", length = 20)
    private String dataVersion;

    /**
     * Data resolution (e.g., "5 arc-min", "district")
     */
    @Column(name = "data_resolution", length = 50)
    private String dataResolution;

    /**
     * Whether this record is active
     */
    @Column(name = "is_active")
    private Boolean isActive;

    /**
     * Record creation timestamp
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Record last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Suitability classification enum
     */
    public enum SuitabilityClassification {
        HIGHLY_SUITABLE,
        SUITABLE,
        MARGINALLY_SUITABLE,
        NOT_SUITABLE
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