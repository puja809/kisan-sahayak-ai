package com.farmer.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;


import java.time.LocalDate;

/**
 * DTO for recording harvest data.
 * Requirements: 11A.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HarvestRequest {

    @NotNull(message = "Crop ID is required")
    private Long cropId;

    private LocalDate actualHarvestDate;

    private Double totalYieldQuintals;

    private String qualityGrade;

    private Double sellingPricePerQuintal;

    private String mandiName;
}