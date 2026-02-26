package com.farmer.yield.dto;


import java.time.LocalDateTime;

/**
 * DTO for financial projections based on yield estimates and current mandi prices.
 * 
 * Contains:
 * - Estimated revenue range based on yield and prices
 * - Cost estimates
 * - Profit projections
 * - Price assumptions used
 * 
 * Validates: Requirement 11B.10
 */
public class FinancialProjectionDto {

    private String commodityName;
    private String variety;
    
    // Yield assumptions
    private Double estimatedYieldQuintals;
    private Double yieldConfidencePercent;
    
    // Price assumptions
    private Double currentPricePerQuintal;
    private Double minPricePerQuintal;
    private Double maxPricePerQuintal;
    private String priceSource;
    private LocalDateTime priceFetchedAt;
    
    // Revenue projections
    private Double estimatedRevenueMin;
    private Double estimatedRevenueExpected;
    private Double estimatedRevenueMax;
    
    // Cost estimates (optional - can be provided or estimated)
    private Double estimatedInputCosts;
    private Double estimatedLaborCosts;
    private Double estimatedOtherCosts;
    private Double totalEstimatedCosts;
    
    // Profit projections
    private Double estimatedProfitMin;
    private Double estimatedProfitExpected;
    private Double estimatedProfitMax;
    
    // ROI
    private Double estimatedRoiPercent;
    
    // Market advisory
    private String marketAdvisory; // "hold", "sell now", "monitor"
    private String advisoryReason;
    
    // Getters and Setters
    public String getCommodityName() {
        return commodityName;
    }

    public void setCommodityName(String commodityName) {
        this.commodityName = commodityName;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public Double getEstimatedYieldQuintals() {
        return estimatedYieldQuintals;
    }

    public void setEstimatedYieldQuintals(Double estimatedYieldQuintals) {
        this.estimatedYieldQuintals = estimatedYieldQuintals;
    }

    public Double getYieldConfidencePercent() {
        return yieldConfidencePercent;
    }

    public void setYieldConfidencePercent(Double yieldConfidencePercent) {
        this.yieldConfidencePercent = yieldConfidencePercent;
    }

    public Double getCurrentPricePerQuintal() {
        return currentPricePerQuintal;
    }

    public void setCurrentPricePerQuintal(Double currentPricePerQuintal) {
        this.currentPricePerQuintal = currentPricePerQuintal;
    }

    public Double getMinPricePerQuintal() {
        return minPricePerQuintal;
    }

    public void setMinPricePerQuintal(Double minPricePerQuintal) {
        this.minPricePerQuintal = minPricePerQuintal;
    }

    public Double getMaxPricePerQuintal() {
        return maxPricePerQuintal;
    }

    public void setMaxPricePerQuintal(Double maxPricePerQuintal) {
        this.maxPricePerQuintal = maxPricePerQuintal;
    }

    public String getPriceSource() {
        return priceSource;
    }

    public void setPriceSource(String priceSource) {
        this.priceSource = priceSource;
    }

    public LocalDateTime getPriceFetchedAt() {
        return priceFetchedAt;
    }

    public void setPriceFetchedAt(LocalDateTime priceFetchedAt) {
        this.priceFetchedAt = priceFetchedAt;
    }

    public Double getEstimatedRevenueMin() {
        return estimatedRevenueMin;
    }

    public void setEstimatedRevenueMin(Double estimatedRevenueMin) {
        this.estimatedRevenueMin = estimatedRevenueMin;
    }

    public Double getEstimatedRevenueExpected() {
        return estimatedRevenueExpected;
    }

    public void setEstimatedRevenueExpected(Double estimatedRevenueExpected) {
        this.estimatedRevenueExpected = estimatedRevenueExpected;
    }

    public Double getEstimatedRevenueMax() {
        return estimatedRevenueMax;
    }

    public void setEstimatedRevenueMax(Double estimatedRevenueMax) {
        this.estimatedRevenueMax = estimatedRevenueMax;
    }

    public Double getEstimatedInputCosts() {
        return estimatedInputCosts;
    }

    public void setEstimatedInputCosts(Double estimatedInputCosts) {
        this.estimatedInputCosts = estimatedInputCosts;
    }

    public Double getEstimatedLaborCosts() {
        return estimatedLaborCosts;
    }

    public void setEstimatedLaborCosts(Double estimatedLaborCosts) {
        this.estimatedLaborCosts = estimatedLaborCosts;
    }

    public Double getEstimatedOtherCosts() {
        return estimatedOtherCosts;
    }

    public void setEstimatedOtherCosts(Double estimatedOtherCosts) {
        this.estimatedOtherCosts = estimatedOtherCosts;
    }

    public Double getTotalEstimatedCosts() {
        return totalEstimatedCosts;
    }

    public void setTotalEstimatedCosts(Double totalEstimatedCosts) {
        this.totalEstimatedCosts = totalEstimatedCosts;
    }

    public Double getEstimatedProfitMin() {
        return estimatedProfitMin;
    }

    public void setEstimatedProfitMin(Double estimatedProfitMin) {
        this.estimatedProfitMin = estimatedProfitMin;
    }

    public Double getEstimatedProfitExpected() {
        return estimatedProfitExpected;
    }

    public void setEstimatedProfitExpected(Double estimatedProfitExpected) {
        this.estimatedProfitExpected = estimatedProfitExpected;
    }

    public Double getEstimatedProfitMax() {
        return estimatedProfitMax;
    }

    public void setEstimatedProfitMax(Double estimatedProfitMax) {
        this.estimatedProfitMax = estimatedProfitMax;
    }

    public Double getEstimatedRoiPercent() {
        return estimatedRoiPercent;
    }

    public void setEstimatedRoiPercent(Double estimatedRoiPercent) {
        this.estimatedRoiPercent = estimatedRoiPercent;
    }

    public String getMarketAdvisory() {
        return marketAdvisory;
    }

    public void setMarketAdvisory(String marketAdvisory) {
        this.marketAdvisory = marketAdvisory;
    }

    public String getAdvisoryReason() {
        return advisoryReason;
    }

    public void setAdvisoryReason(String advisoryReason) {
        this.advisoryReason = advisoryReason;
    }

    /**
     * Builder pattern for creating financial projections.
     */
    public static FinancialProjectionBuilder builder() {
        return new FinancialProjectionBuilder();
    }

    public static class FinancialProjectionBuilder {
        private final FinancialProjectionDto projection = new FinancialProjectionDto();

        public FinancialProjectionBuilder commodityName(String commodityName) {
            projection.setCommodityName(commodityName);
            return this;
        }

        public FinancialProjectionBuilder variety(String variety) {
            projection.setVariety(variety);
            return this;
        }

        public FinancialProjectionBuilder estimatedYieldQuintals(Double yield) {
            projection.setEstimatedYieldQuintals(yield);
            return this;
        }

        public FinancialProjectionBuilder currentPricePerQuintal(Double price) {
            projection.setCurrentPricePerQuintal(price);
            return this;
        }

        public FinancialProjectionBuilder minPricePerQuintal(Double price) {
            projection.setMinPricePerQuintal(price);
            return this;
        }

        public FinancialProjectionBuilder maxPricePerQuintal(Double price) {
            projection.setMaxPricePerQuintal(price);
            return this;
        }

        public FinancialProjectionBuilder priceSource(String priceSource) {
            projection.setPriceSource(priceSource);
            return this;
        }

        public FinancialProjectionBuilder estimatedRevenueMin(Double revenue) {
            projection.setEstimatedRevenueMin(revenue);
            return this;
        }

        public FinancialProjectionBuilder estimatedRevenueExpected(Double revenue) {
            projection.setEstimatedRevenueExpected(revenue);
            return this;
        }

        public FinancialProjectionBuilder estimatedRevenueMax(Double revenue) {
            projection.setEstimatedRevenueMax(revenue);
            return this;
        }

        public FinancialProjectionBuilder totalEstimatedCosts(Double costs) {
            projection.setTotalEstimatedCosts(costs);
            return this;
        }

        public FinancialProjectionBuilder estimatedProfitMin(Double profit) {
            projection.setEstimatedProfitMin(profit);
            return this;
        }

        public FinancialProjectionBuilder estimatedProfitExpected(Double profit) {
            projection.setEstimatedProfitExpected(profit);
            return this;
        }

        public FinancialProjectionBuilder estimatedProfitMax(Double profit) {
            projection.setEstimatedProfitMax(profit);
            return this;
        }

        public FinancialProjectionBuilder estimatedRoiPercent(Double roi) {
            projection.setEstimatedRoiPercent(roi);
            return this;
        }

        public FinancialProjectionBuilder marketAdvisory(String advisory) {
            projection.setMarketAdvisory(advisory);
            return this;
        }

        public FinancialProjectionBuilder advisoryReason(String reason) {
            projection.setAdvisoryReason(reason);
            return this;
        }

        public FinancialProjectionDto build() {
            return projection;
        }
    }
}