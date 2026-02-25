package com.farmer.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
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

    private BigDecimal totalYieldQuintals;

    private String qualityGrade;

    private BigDecimal sellingPricePerQuintal;

    private String mandiName;
}