package com.farmer.user.dto;

import com.farmer.user.entity.Crop;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating or updating a crop.
 * Requirements: 11A.4, 11A.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropRequest {

    @NotNull(message = "Farm ID is required")
    private Long farmId;

    @NotBlank(message = "Crop name is required")
    private String cropName;

    private String cropVariety;

    @NotNull(message = "Sowing date is required")
    private LocalDate sowingDate;

    private LocalDate expectedHarvestDate;

    private LocalDate actualHarvestDate;

    @NotNull(message = "Area is required")
    @Positive(message = "Area must be positive")
    private Double areaAcres;

    private Crop.Season season;

    private Crop.CropStatus status;

    private BigDecimal seedCost;

    private BigDecimal fertilizerCost;

    private BigDecimal pesticideCost;

    private BigDecimal laborCost;

    private BigDecimal otherCost;

    private BigDecimal totalYieldQuintals;

    private String qualityGrade;

    private BigDecimal sellingPricePerQuintal;

    private String mandiName;

    private String notes;
}