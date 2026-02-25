package com.farmer.mandi.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for mandi price data response.
 * 
 * Requirements:
 * - 6.2: Display modal price, min price, max price, arrival quantity, variety
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MandiPriceDto {

    private Long id;
    private String commodityName;
    private String variety;
    private String mandiName;
    private String mandiCode;
    private String state;
    private String district;
    private LocalDate priceDate;
    private BigDecimal modalPrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal arrivalQuantityQuintals;
    private String unit;
    private String source;
    private LocalDateTime fetchedAt;
    private BigDecimal distanceKm; // Distance from farmer's location
    private Boolean isCached; // Indicates if data is from cache
}