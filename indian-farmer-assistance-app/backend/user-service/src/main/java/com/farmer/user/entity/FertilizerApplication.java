package com.farmer.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FertilizerApplication entity for tracking fertilizer applications on crops.
 * Maps to the fertilizer_applications table with session-specific table prefix.
 * 
 * Requirements: 11A.4, 11C.6, 11C.7
 */
@Entity
@Table(name = "sess_c05a946fe_fertilizer_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @Column(name = "fertilizer_type", nullable = false, length = 100)
    private String fertilizerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fertilizer_category")
    private FertilizerCategory fertilizerCategory;

    @Column(name = "quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityKg;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "application_stage", length = 50)
    private String applicationStage;

    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "nitrogen_content_percent", precision = 5, scale = 2)
    private BigDecimal nitrogenContentPercent;

    @Column(name = "phosphorus_content_percent", precision = 5, scale = 2)
    private BigDecimal phosphorusContentPercent;

    @Column(name = "potassium_content_percent", precision = 5, scale = 2)
    private BigDecimal potassiumContentPercent;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Fertilizer categories for classification.
     * Requirements: 11C.1
     */
    public enum FertilizerCategory {
        CHEMICAL,
        ORGANIC,
        BIO
    }

    /**
     * Calculate nitrogen content in kg.
     */
    public BigDecimal getNitrogenKg() {
        if (quantityKg == null || nitrogenContentPercent == null) {
            return BigDecimal.ZERO;
        }
        return quantityKg.multiply(nitrogenContentPercent).divide(BigDecimal.valueOf(100));
    }

    /**
     * Calculate phosphorus content in kg.
     */
    public BigDecimal getPhosphorusKg() {
        if (quantityKg == null || phosphorusContentPercent == null) {
            return BigDecimal.ZERO;
        }
        return quantityKg.multiply(phosphorusContentPercent).divide(BigDecimal.valueOf(100));
    }

    /**
     * Calculate potassium content in kg.
     */
    public BigDecimal getPotassiumKg() {
        if (quantityKg == null || potassiumContentPercent == null) {
            return BigDecimal.ZERO;
        }
        return quantityKg.multiply(potassiumContentPercent).divide(BigDecimal.valueOf(100));
    }
}