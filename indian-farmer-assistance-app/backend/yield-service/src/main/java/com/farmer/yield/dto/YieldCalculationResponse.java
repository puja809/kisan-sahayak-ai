package com.farmer.yield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for yield calculation
 * Returns calculated yield estimates based on commodity, farm size, and investment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YieldCalculationResponse {

    private String commodity;
    private Double farmSizeHectares;
    private Double investmentAmount;
    private Double minPricePerKg;
    private Double avgPricePerKg;
    private Double maxPricePerKg;
    private Double baseYieldPerHectare;
    private Double estimatedMinYield;
    private Double estimatedExpectedYield;
    private Double estimatedMaxYield;
    private Double estimatedMinRevenue;
    private Double estimatedExpectedRevenue;
    private Double estimatedMaxRevenue;
    private Double profitMarginPercent;
    private String message;
    private Boolean success;
}
