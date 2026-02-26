package com.farmer.crop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for recording fertilizer application.
 * 
 * Validates: Requirement 11C.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FertilizerApplicationRequestDto {

    /**
     * Crop ID
     */
    @NotNull(message = "Crop ID is required")
    private Long cropId;

    /**
     * Farmer ID
     */
    @NotNull(message = "Farmer ID is required")
    private String farmerId;

    /**
     * Type of fertilizer
     */
    @NotNull(message = "Fertilizer type is required")
    private String fertilizerType;

    /**
     * Category of fertilizer (CHEMICAL, ORGANIC, BIO)
     */
    @NotNull(message = "Fertilizer category is required")
    private String fertilizerCategory;

    /**
     * Quantity applied in kg
     */
    @NotNull(message = "Quantity is required")
    private Double quantityKg;

    /**
     * Area in acres
     */
    private Double areaAcres;

    /**
     * Date of application
     */
    @NotNull(message = "Application date is required")
    private LocalDate applicationDate;

    /**
     * Growth stage at time of application
     */
    private String applicationStage;

    /**
     * Cost in INR
     */
    private Double cost;

    /**
     * Nitrogen content percentage
     */
    private Double nitrogenPercent;

    /**
     * Phosphorus content percentage
     */
    private Double phosphorusPercent;

    /**
     * Potassium content percentage
     */
    private Double potassiumPercent;

    /**
     * Sulfur content percentage
     */
    private Double sulfurPercent;

    /**
     * Zinc content percentage
     */
    private Double zincPercent;

    /**
     * Source of recommendation
     */
    private String recommendationSource;

    /**
     * Notes
     */
    private String notes;
}