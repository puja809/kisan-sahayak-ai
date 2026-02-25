package com.farmer.mandi.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for price trend analysis response.
 * 
 * Requirements:
 * - 6.5: Display 30-day price history with graphical visualization
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceTrendDto {

    private String commodityName;
    private String variety;
    private String state;
    private String district;
    private List<PricePointDto> priceHistory;
    private TrendAnalysisDto trendAnalysis;
    private MspComparisonDto mspComparison;
    private StorageAdvisoryDto storageAdvisory;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDataPoints;

    /**
     * Individual price point for time-series data.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PricePointDto {
        private LocalDate date;
        private BigDecimal modalPrice;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private BigDecimal arrivalQuantity;
        private String mandiName;
    }

    /**
     * Trend analysis summary.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendAnalysisDto {
        private String trendDirection; // INCREASING, DECREASING, STABLE
        private BigDecimal priceChangePercent;
        private BigDecimal averagePrice;
        private BigDecimal highestPrice;
        private BigDecimal lowestPrice;
        private BigDecimal priceVolatility;
    }

    /**
     * MSP vs market price comparison.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MspComparisonDto {
        private BigDecimal msp;
        private BigDecimal currentMarketPrice;
        private BigDecimal difference;
        private String comparisonResult; // ABOVE_MSP, BELOW_MSP, AT_MSP
        private String recommendation;
    }

    /**
     * Post-harvest storage advisory.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageAdvisoryDto {
        private String recommendation; // HOLD, SELL
        private String reasoning;
        private Double confidenceLevel;
        private String expectedPriceChange;
        private Integer suggestedHoldingDays;
    }
}