package com.farmer.mandi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


/**
 * DTO for price alert subscription request.
 * 
 * Requirements:
 * - 6.10: Create price alert subscription endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlertRequest {

    @NotBlank(message = "Farmer ID is required")
    private String farmerId;

    @NotBlank(message = "Commodity is required")
    private String commodity;

    private String variety;

    private Double targetPrice;

    private String alertType; // PRICE_ABOVE, PRICE_BELOW, PRICE_PEAK

    private Boolean neighboringDistrictsOnly;
}