package com.farmer.user.dto;

import com.farmer.user.entity.Crop;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropRequest {

    @NotBlank(message = "Crop name is required")
    private String cropName;

    private String cropVariety;

    @NotNull(message = "Sowing date is required")
    private LocalDate sowingDate;

    private LocalDate expectedHarvestDate;

    @NotNull(message = "Area is required")
    @Positive(message = "Area must be positive")
    private Double areaAcres;

    @NotNull(message = "Season is required")
    private Crop.Season season;

    private Crop.CropStatus status;

    private Double seedCost;
    private Double fertilizerCost;
    private Double pesticideCost;
    private Double laborCost;
    private Double otherCost;
    private Double totalYieldQuintals;
    private String qualityGrade;
    private Double sellingPricePerQuintal;
    private String mandiName;
    private Double totalRevenue;
    private LocalDate actualHarvestDate;
    private String notes;
}