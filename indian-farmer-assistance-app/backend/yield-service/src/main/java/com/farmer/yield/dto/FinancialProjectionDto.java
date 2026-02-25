package com.farmer.yield.dto;

import java.math.BigDecimal;
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
    private BigDecimal estimatedYieldQuintals;
    private BigDecimal yieldConfidencePercent;
    
    // Price assumptions
    private BigDecimal currentPricePerQuintal;
    private BigDecimal minPricePerQuintal;
    private BigDecimal maxPricePerQuintal;
    private String priceSource;
    private LocalDateTime priceFetchedAt;
    
    // Revenue projections
    private BigDecimal estimatedRevenueMin;
    private BigDecimal estimatedRevenueExpected;
    private BigDecimal estimatedRevenueMax;
    
    // Cost estimates (optional - can be provided or estimated)
    private BigDecimal estimatedInputCosts;
    private BigDecimal estimatedLaborCosts;
    private BigDecimal estimatedOtherCosts;
    private BigDecimal totalEstimatedCosts;
    
    // Profit projections
    private BigDecimal estimatedProfitMin;
    private BigDecimal estimatedProfitExpected;
    private BigDecimal estimatedProfitMax;
    
    // ROI
    private BigDecimal estimatedRoiPercent;
    
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

    public BigDecimal getEstimatedYieldQuintals() {
        return estimatedYieldQuintals;
    }

    public void setEstimatedYieldQuintals(BigDecimal estimatedYieldQuintals) {
        this.estimatedYieldQuintals = estimatedYieldQuintals;
    }

    public BigDecimal getYieldConfidencePercent() {
        return yieldConfidencePercent;
    }

    public void setYieldConfidencePercent(BigDecimal yieldConfidencePercent) {
        this.yieldConfidencePercent = yieldConfidencePercent;
    }

    public BigDecimal getCurrentPricePerQuintal() {
        return currentPricePerQuintal;
    }

    public void setCurrentPricePerQuintal(BigDecimal currentPricePerQuintal) {
        this.currentPricePerQuintal = currentPricePerQuintal;
    }

    public BigDecimal getMinPricePerQuintal() {
        return minPricePerQuintal;
    }

    public void setMinPricePerQuintal(BigDecimal minPricePerQuintal) {
        this.minPricePerQuintal = minPricePerQuintal;
    }

    public BigDecimal getMaxPricePerQuintal() {
        return maxPricePerQuintal;
    }

    public void setMaxPricePerQuintal(BigDecimal maxPricePerQuintal) {
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

    public BigDecimal getEstimatedRevenueMin() {
        return estimatedRevenueMin;
    }

    public void setEstimatedRevenueMin(BigDecimal estimatedRevenueMin) {
        this.estimatedRevenueMin = estimatedRevenueMin;
    }

    public BigDecimal getEstimatedRevenueExpected() {
        return estimatedRevenueExpected;
    }

    public void setEstimatedRevenueExpected(BigDecimal estimatedRevenueExpected) {
        this.estimatedRevenueExpected = estimatedRevenueExpected;
    }

    public BigDecimal getEstimatedRevenueMax() {
        return estimatedRevenueMax;
    }

    public void setEstimatedRevenueMax(BigDecimal estimatedRevenueMax) {
        this.estimatedRevenueMax = estimatedRevenueMax;
    }

    public BigDecimal getEstimatedInputCosts() {
        return estimatedInputCosts;
    }

    public void setEstimatedInputCosts(BigDecimal estimatedInputCosts) {
        this.estimatedInputCosts = estimatedInputCosts;
    }

    public BigDecimal getEstimatedLaborCosts() {
        return estimatedLaborCosts;
    }

    public void setEstimatedLaborCosts(BigDecimal estimatedLaborCosts) {
        this.estimatedLaborCosts = estimatedLaborCosts;
    }

    public BigDecimal getEstimatedOtherCosts() {
        return estimatedOtherCosts;
    }

    public void setEstimatedOtherCosts(BigDecimal estimatedOtherCosts) {
        this.estimatedOtherCosts = estimatedOtherCosts;
    }

    public BigDecimal getTotalEstimatedCosts() {
        return totalEstimatedCosts;
    }

    public void setTotalEstimatedCosts(BigDecimal totalEstimatedCosts) {
        this.totalEstimatedCosts = totalEstimatedCosts;
    }

    public BigDecimal getEstimatedProfitMin() {
        return estimatedProfitMin;
    }

    public void setEstimatedProfitMin(BigDecimal estimatedProfitMin) {
        this.estimatedProfitMin = estimatedProfitMin;
    }

    public BigDecimal getEstimatedProfitExpected() {
        return estimatedProfitExpected;
    }

    public void setEstimatedProfitExpected(BigDecimal estimatedProfitExpected) {
        this.estimatedProfitExpected = estimatedProfitExpected;
    }

    public BigDecimal getEstimatedProfitMax() {
        return estimatedProfitMax;
    }

    public void setEstimatedProfitMax(BigDecimal estimatedProfitMax) {
        this.estimatedProfitMax = estimatedProfitMax;
    }

    public BigDecimal getEstimatedRoiPercent() {
        return estimatedRoiPercent;
    }

    public void setEstimatedRoiPercent(BigDecimal estimatedRoiPercent) {
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
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final FinancialProjectionDto projection = new FinancialProjectionDto();

        public Builder commodityName(String commodityName) {
            projection.setCommodityName(commodityName);
            return this;
        }

        public Builder variety(String variety) {
            projection.setVariety(variety);
            return this;
        }

        public Builder estimatedYieldQuintals(BigDecimal yield) {
            projection.setEstimatedYieldQuintals(yield);
            return this;
        }

        public Builder currentPricePerQuintal(BigDecimal price) {
            projection.setCurrentPricePerQuintal(price);
            return this;
        }

        public Builder minPricePerQuintal(BigDecimal price) {
            projection.setMinPricePerQuintal(price);
            return this;
        }

        public Builder maxPricePerQuintal(BigDecimal price) {
            projection.setMaxPricePerQuintal(price);
            return this;
        }

        public Builder estimatedRevenueMin(BigDecimal revenue) {
            projection.setEstimatedRevenueMin(revenue);
            return this;
        }

        public Builder estimatedRevenueExpected(BigDecimal revenue) {
            projection.setEstimatedRevenueExpected(revenue);
            return this;
        }

        public Builder estimatedRevenueMax(BigDecimal revenue) {
            projection.setEstimatedRevenueMax(revenue);
            return this;
        }

        public Builder totalEstimatedCosts(BigDecimal costs) {
            projection.setTotalEstimatedCosts(costs);
            return this;
        }

        public Builder estimatedProfitMin(BigDecimal profit) {
            projection.setEstimatedProfitMin(profit);
            return this;
        }

        public Builder estimatedProfitExpected(BigDecimal profit) {
            projection.setEstimatedProfitExpected(profit);
            return this;
        }

        public Builder estimatedProfitMax(BigDecimal profit) {
            projection.setEstimatedProfitMax(profit);
            return this;
        }

        public Builder estimatedRoiPercent(BigDecimal roi) {
            projection.setEstimatedRoiPercent(roi);
            return this;
        }

        public Builder marketAdvisory(String advisory) {
            projection.setMarketAdvisory(advisory);
            return this;
        }

        public Builder advisoryReason(String reason) {
            projection.setAdvisoryReason(reason);
            return this;
        }

        public FinancialProjectionDto build() {
            return projection;
        }
    }
}