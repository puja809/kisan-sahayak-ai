package com.farmer.yield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for yield calculation
 * Takes commodity, farm size, and investment amount as input
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YieldCalculationRequest {

    @NotBlank(message = "Commodity is required")
    private String commodity;

    @NotNull(message = "Farm size is required")
    @Positive(message = "Farm size must be positive")
    private Double farmSizeHectares;

    @NotNull(message = "Investment amount is required")
    @Positive(message = "Investment amount must be positive")
    private Double investmentAmount;
}
