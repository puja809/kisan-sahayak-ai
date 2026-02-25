package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for market price data.
 * 
 * Contains price information from AGMARKNET for commodities
 * to enable market-linked crop recommendations.
 * 
 * Validates: Requirement 2.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataDto {
    
    /**
     * Commodity/crop code
     */
    private String cropCode;
    
    /**
     * Commodity name
     */
    private String cropName;
    
    /**
     * Variety (e.g., "Hybrid", "Desi")
     */
    private String variety;
    
    /**
     * Current modal price (INR per quintal)
     */
    private BigDecimal currentPrice;
    
    /**
     * Minimum price (INR per quintal)
     */
    private BigDecimal minPrice;
    
    /**
     * Maximum price (INR per quintal)
     */
    private BigDecimal maxPrice;
    
    /**
     * Price 30 days ago (for trend calculation)
     */
    private BigDecimal price30DaysAgo;
    
    /**
     * Price 7 days ago (for trend calculation)
     */
    private BigDecimal price7DaysAgo;
    
    /**
     * Price change percentage over last 30 days
     */
    private BigDecimal priceChange30Days;
    
    /**
     * Price change percentage over last 7 days
     */
    private BigDecimal priceChange7Days;
    
    /**
     * Price trend direction (UP, DOWN, STABLE)
     */
    private PriceTrend trend;
    
    /**
     * Arrival quantity in current period (quintals)
     */
    private BigDecimal arrivalQuantity;
    
    /**
     * Arrival quantity change percentage
     */
    private BigDecimal arrivalChangePercent;
    
    /**
     * Nearest mandi name
     */
    private String nearestMandi;
    
    /**
     * Distance to nearest mandi (km)
     */
    private BigDecimal distanceToMandi;
    
    /**
     * Minimum Support Price (MSP) if applicable
     */
    private BigDecimal msp;
    
    /**
     * Whether current price is above MSP
     */
    private Boolean aboveMsp;
    
    /**
     * Recommended action based on price trends
     */
    private PriceRecommendation recommendation;
    
    /**
     * Data as of date
     */
    private LocalDate dataDate;
    
    /**
     * Price trend direction enum
     */
    public enum PriceTrend {
        UP,        // Price increasing
        DOWN,      // Price decreasing
        STABLE     // Price relatively stable
    }
    
    /**
     * Price recommendation enum
     */
    public enum PriceRecommendation {
        SELL_NOW,      // Good time to sell
        HOLD,          // Wait for better prices
        MONITOR,       // Continue monitoring
        CONSIDER_STORAGE // Consider storage if facilities available
    }
}