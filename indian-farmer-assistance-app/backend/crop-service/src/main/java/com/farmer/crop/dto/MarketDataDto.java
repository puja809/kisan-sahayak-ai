package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Double currentPrice;
    
    /**
     * Minimum price (INR per quintal)
     */
    private Double minPrice;
    
    /**
     * Maximum price (INR per quintal)
     */
    private Double maxPrice;
    
    /**
     * Price 30 days ago (for trend calculation)
     */
    private Double price30DaysAgo;
    
    /**
     * Price 7 days ago (for trend calculation)
     */
    private Double price7DaysAgo;
    
    /**
     * Price change percentage over last 30 days
     */
    private Double priceChange30Days;
    
    /**
     * Price change percentage over last 7 days
     */
    private Double priceChange7Days;
    
    /**
     * Price trend direction (UP, DOWN, STABLE)
     */
    private PriceTrend trend;
    
    /**
     * Arrival quantity in current period (quintals)
     */
    private Double arrivalQuantity;
    
    /**
     * Arrival quantity change percentage
     */
    private Double arrivalChangePercent;
    
    /**
     * Nearest mandi name
     */
    private String nearestMandi;
    
    /**
     * Distance to nearest mandi (km)
     */
    private Double distanceToMandi;
    
    /**
     * Minimum Support Price (MSP) if applicable
     */
    private Double msp;
    
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