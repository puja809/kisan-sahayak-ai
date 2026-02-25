package com.farmer.crop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity for storing Soil Health Card data.
 * 
 * Soil Health Cards contain nutrient analysis results including:
 * - Primary nutrients: Nitrogen (N), Phosphorus (P), Potassium (K)
 * - Secondary nutrients: Sulfur (S)
 * - Micronutrients: Zinc (Zn), Iron (Fe), Copper (Cu), Manganese (Mn), Boron (B)
 * 
 * Validates: Requirement 2.4
 */
@Entity
@Table(name = "soil_health_cards", indexes = {
    @Index(name = "idx_shc_card_id", columnList = "cardId"),
    @Index(name = "idx_shc_farmer_id", columnList = "farmerId"),
    @Index(name = "idx_shc_survey_number", columnList = "surveyNumber"),
    @Index(name = "idx_shc_location", columnList = "latitude, longitude")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoilHealthCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique Soil Health Card identifier
     */
    @Column(name = "card_id", nullable = false, unique = true, length = 50)
    private String cardId;

    /**
     * Farmer ID associated with this card
     */
    @Column(name = "farmer_id", nullable = false, length = 50)
    private String farmerId;

    /**
     * Survey number of the land parcel
     */
    @Column(name = "survey_number", length = 50)
    private String surveyNumber;

    /**
     * GPS latitude of sample collection point
     */
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    /**
     * GPS longitude of sample collection point
     */
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    /**
     * District name
     */
    @Column(name = "district", length = 50)
    private String district;

    /**
     * State name
     */
    @Column(name = "state", length = 50)
    private String state;

    /**
     * Village name
     */
    @Column(name = "village", length = 100)
    private String village;

    /**
     * Date of soil sample collection
     */
    @Column(name = "sample_date")
    private LocalDate sampleDate;

    /**
     * Date of laboratory analysis
     */
    @Column(name = "analysis_date")
    private LocalDate analysisDate;

    /**
     * Soil pH value (0-14)
     */
    @Column(name = "ph", precision = 4, scale = 2)
    private BigDecimal ph;

    /**
     * Electrical conductivity (dS/m) - indicates salinity
     */
    @Column(name = "electrical_conductivity", precision = 6, scale = 3)
    private BigDecimal electricalConductivity;

    /**
     * Organic carbon content (%)
     */
    @Column(name = "organic_carbon", precision = 5, scale = 2)
    private BigDecimal organicCarbon;

    // Primary Nutrients

    /**
     * Available Nitrogen (kg/ha)
     */
    @Column(name = "nitrogen_kg_ha", precision = 8, scale = 2)
    private BigDecimal nitrogenKgHa;

    /**
     * Available Phosphorus (kg/ha)
     */
    @Column(name = "phosphorus_kg_ha", precision = 8, scale = 2)
    private BigDecimal phosphorusKgHa;

    /**
     * Available Potassium (kg/ha)
     */
    @Column(name = "potassium_kg_ha", precision = 8, scale = 2)
    private BigDecimal potassiumKgHa;

    // Secondary Nutrients

    /**
     * Available Sulfur (ppm)
     */
    @Column(name = "sulfur_ppm", precision = 6, scale = 2)
    private BigDecimal sulfurPpm;

    // Micronutrients

    /**
     * Available Zinc (ppm)
     */
    @Column(name = "zinc_ppm", precision = 6, scale = 2)
    private BigDecimal zincPpm;

    /**
     * Available Iron (ppm)
     */
    @Column(name = "iron_ppm", precision = 6, scale = 2)
    private BigDecimal ironPpm;

    /**
     * Available Copper (ppm)
     */
    @Column(name = "copper_ppm", precision = 6, scale = 2)
    private BigDecimal copperPpm;

    /**
     * Available Manganese (ppm)
     */
    @Column(name = "manganese_ppm", precision = 6, scale = 2)
    private BigDecimal manganesePpm;

    /**
     * Available Boron (ppm)
     */
    @Column(name = "boron_ppm", precision = 6, scale = 2)
    private BigDecimal boronPpm;

    /**
     * Soil texture classification
     */
    @Column(name = "soil_texture", length = 50)
    private String soilTexture;

    /**
     * Soil color (for reference)
     */
    @Column(name = "soil_color", length = 50)
    private String soilColor;

    /**
     * Previous crop grown
     */
    @Column(name = "previous_crop", length = 100)
    private String previousCrop;

    /**
     * Overall soil health status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", length = 20)
    private SoilHealthStatus overallStatus;

    /**
     * Laboratory name
     */
    @Column(name = "testing_laboratory", length = 200)
    private String testingLaboratory;

    /**
     * Whether this data is from an official Soil Health Card
     */
    @Column(name = "is_official_card")
    private Boolean isOfficialCard;

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
        if (isOfficialCard == null) {
            isOfficialCard = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Soil health status enum
     */
    public enum SoilHealthStatus {
        GOOD,
        MODERATE,
        POOR
    }
}