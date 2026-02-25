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
 * Entity for recording fertilizer applications.
 * 
 * This entity stores:
 * - Fertilizer type (urea, DAP, MOP, organic compost, etc.)
 * - Quantity applied
 * - Application date and growth stage
 * - Cost and nutrient content (N, P, K percentages)
 * 
 * Validates: Requirements 11C.6, 11C.7, 11C.8
 */
@Entity
@Table(name = "fertilizer_applications", indexes = {
    @Index(name = "idx_fert_app_crop_id", columnList = "crop_id"),
    @Index(name = "idx_fert_app_application_date", columnList = "application_date"),
    @Index(name = "idx_fert_app_fertilizer_type", columnList = "fertilizer_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FertilizerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Crop ID this application is associated with
     */
    @Column(name = "crop_id", nullable = false)
    private Long cropId;

    /**
     * Farmer ID for tracking
     */
    @Column(name = "farmer_id", nullable = false, length = 50)
    private String farmerId;

    /**
     * Type of fertilizer (urea, DAP, MOP, organic compost, vermicompost, etc.)
     */
    @Column(name = "fertilizer_type", nullable = false, length = 100)
    private String fertilizerType;

    /**
     * Category of fertilizer
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fertilizer_category", nullable = false, length = 20)
    private FertilizerCategory fertilizerCategory;

    /**
     * Quantity of fertilizer applied in kg
     */
    @Column(name = "quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityKg;

    /**
     * Area in acres where fertilizer was applied
     */
    @Column(name = "area_acres", precision = 10, scale = 2)
    private BigDecimal areaAcres;

    /**
     * Date of application
     */
    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    /**
     * Growth stage at time of application (e.g., basal, tillering, flowering)
     */
    @Column(name = "application_stage", length = 50)
    private String applicationStage;

    /**
     * Cost of fertilizer in INR
     */
    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    /**
     * Nitrogen content percentage (0-100)
     */
    @Column(name = "nitrogen_percent", precision = 5, scale = 2)
    private BigDecimal nitrogenPercent;

    /**
     * Phosphorus content percentage (0-100)
     */
    @Column(name = "phosphorus_percent", precision = 5, scale = 2)
    private BigDecimal phosphorusPercent;

    /**
     * Potassium content percentage (0-100)
     */
    @Column(name = "potassium_percent", precision = 5, scale = 2)
    private BigDecimal potassiumPercent;

    /**
     * Sulfur content percentage (0-100) - secondary nutrient
     */
    @Column(name = "sulfur_percent", precision = 5, scale = 2)
    private BigDecimal sulfurPercent;

    /**
     * Zinc content percentage (0-100) - micronutrient
     */
    @Column(name = "zinc_percent", precision = 5, scale = 2)
    private BigDecimal zincPercent;

    /**
     * Source of recommendation (soil_test, default, ai)
     */
    @Column(name = "recommendation_source", length = 20)
    private String recommendationSource;

    /**
     * Notes about the application
     */
    @Column(name = "notes", length = 500)
    private String notes;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate total nitrogen applied in kg
     */
    public BigDecimal getNitrogenKg() {
        if (quantityKg == null || nitrogenPercent == null) {
            return BigDecimal.ZERO;
        }
        return quantityKg.multiply(nitrogenPercent)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate total phosphorus applied in kg
     */
    public BigDecimal getPhosphorusKg() {
        if (quantityKg == null || phosphorusPercent == null) {
            return BigDecimal.ZERO;
        }
        return quantityKg.multiply(phosphorusPercent)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate total potassium applied in kg
     */
    public BigDecimal getPotassiumKg() {
        if (quantityKg == null || potassiumPercent == null) {
            return BigDecimal.ZERO;
        }
        return quantityKg.multiply(potassiumPercent)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Fertilizer category enum
     */
    public enum FertilizerCategory {
        CHEMICAL,
        ORGANIC,
        BIO
    }
}