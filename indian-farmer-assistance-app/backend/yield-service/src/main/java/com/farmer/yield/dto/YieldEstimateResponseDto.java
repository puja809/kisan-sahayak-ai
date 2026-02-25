package com.farmer.yield.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for yield estimation.
 * 
 * Contains:
 * - Yield prediction (min, expected, max)
 * - Confidence interval
 * - Factors considered in the prediction
 * - Model version used
 * - Financial projections (if requested)
 * - Historical comparison
 * 
 * Validates: Requirements 11B.1, 11B.7, 11B.10
 */
public class YieldEstimateResponseDto {

    private boolean success;
    private String message;
    private LocalDateTime generatedAt;
    
    // Prediction results
    private Long predictionId;
    private Long cropId;
    private String farmerId;
    private String cropName;
    private String cropVariety;
    private BigDecimal areaAcres;
    
    // Yield estimates (per acre)
    private BigDecimal predictedYieldMinQuintalsPerAcre;
    private BigDecimal predictedYieldExpectedQuintalsPerAcre;
    private BigDecimal predictedYieldMaxQuintalsPerAcre;
    
    // Total yield estimates (for the entire area)
    private BigDecimal predictedYieldMinQuintals;
    private BigDecimal predictedYieldExpectedQuintals;
    private BigDecimal predictedYieldMaxQuintals;
    
    // Confidence and factors
    private BigDecimal confidenceIntervalPercent;
    private List<String> factorsConsidered;
    private List<String> factorAdjustments; // Positive/negative adjustments made
    private String modelVersion;
    
    // Growth stage info
    private String currentGrowthStage;
    private Integer daysSinceSowing;
    private Integer estimatedDaysToHarvest;
    
    // Historical comparison
    private BigDecimal historicalAverageYieldQuintalsPerAcre;
    private BigDecimal yieldVarianceFromHistoricalPercent;
    private String historicalComparisonNote;
    
    // Financial projections
    private FinancialProjectionDto financialProjection;
    
    // Notification flag
    private boolean significantDeviationFromPrevious;
    private BigDecimal deviationFromPreviousPercent;
    private String deviationNote;
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Long getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(Long predictionId) {
        this.predictionId = predictionId;
    }

    public Long getCropId() {
        return cropId;
    }

    public void setCropId(Long cropId) {
        this.cropId = cropId;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getCropVariety() {
        return cropVariety;
    }

    public void setCropVariety(String cropVariety) {
        this.cropVariety = cropVariety;
    }

    public BigDecimal getAreaAcres() {
        return areaAcres;
    }

    public void setAreaAcres(BigDecimal areaAcres) {
        this.areaAcres = areaAcres;
    }

    public BigDecimal getPredictedYieldMinQuintalsPerAcre() {
        return predictedYieldMinQuintalsPerAcre;
    }

    public void setPredictedYieldMinQuintalsPerAcre(BigDecimal predictedYieldMinQuintalsPerAcre) {
        this.predictedYieldMinQuintalsPerAcre = predictedYieldMinQuintalsPerAcre;
    }

    public BigDecimal getPredictedYieldExpectedQuintalsPerAcre() {
        return predictedYieldExpectedQuintalsPerAcre;
    }

    public void setPredictedYieldExpectedQuintalsPerAcre(BigDecimal predictedYieldExpectedQuintalsPerAcre) {
        this.predictedYieldExpectedQuintalsPerAcre = predictedYieldExpectedQuintalsPerAcre;
    }

    public BigDecimal getPredictedYieldMaxQuintalsPerAcre() {
        return predictedYieldMaxQuintalsPerAcre;
    }

    public void setPredictedYieldMaxQuintalsPerAcre(BigDecimal predictedYieldMaxQuintalsPerAcre) {
        this.predictedYieldMaxQuintalsPerAcre = predictedYieldMaxQuintalsPerAcre;
    }

    public BigDecimal getPredictedYieldMinQuintals() {
        return predictedYieldMinQuintals;
    }

    public void setPredictedYieldMinQuintals(BigDecimal predictedYieldMinQuintals) {
        this.predictedYieldMinQuintals = predictedYieldMinQuintals;
    }

    public BigDecimal getPredictedYieldExpectedQuintals() {
        return predictedYieldExpectedQuintals;
    }

    public void setPredictedYieldExpectedQuintals(BigDecimal predictedYieldExpectedQuintals) {
        this.predictedYieldExpectedQuintals = predictedYieldExpectedQuintals;
    }

    public BigDecimal getPredictedYieldMaxQuintals() {
        return predictedYieldMaxQuintals;
    }

    public void setPredictedYieldMaxQuintals(BigDecimal predictedYieldMaxQuintals) {
        this.predictedYieldMaxQuintals = predictedYieldMaxQuintals;
    }

    public BigDecimal getConfidenceIntervalPercent() {
        return confidenceIntervalPercent;
    }

    public void setConfidenceIntervalPercent(BigDecimal confidenceIntervalPercent) {
        this.confidenceIntervalPercent = confidenceIntervalPercent;
    }

    public List<String> getFactorsConsidered() {
        return factorsConsidered;
    }

    public void setFactorsConsidered(List<String> factorsConsidered) {
        this.factorsConsidered = factorsConsidered;
    }

    public List<String> getFactorAdjustments() {
        return factorAdjustments;
    }

    public void setFactorAdjustments(List<String> factorAdjustments) {
        this.factorAdjustments = factorAdjustments;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getCurrentGrowthStage() {
        return currentGrowthStage;
    }

    public void setCurrentGrowthStage(String currentGrowthStage) {
        this.currentGrowthStage = currentGrowthStage;
    }

    public Integer getDaysSinceSowing() {
        return daysSinceSowing;
    }

    public void setDaysSinceSowing(Integer daysSinceSowing) {
        this.daysSinceSowing = daysSinceSowing;
    }

    public Integer getEstimatedDaysToHarvest() {
        return estimatedDaysToHarvest;
    }

    public void setEstimatedDaysToHarvest(Integer estimatedDaysToHarvest) {
        this.estimatedDaysToHarvest = estimatedDaysToHarvest;
    }

    public BigDecimal getHistoricalAverageYieldQuintalsPerAcre() {
        return historicalAverageYieldQuintalsPerAcre;
    }

    public void setHistoricalAverageYieldQuintalsPerAcre(BigDecimal historicalAverageYieldQuintalsPerAcre) {
        this.historicalAverageYieldQuintalsPerAcre = historicalAverageYieldQuintalsPerAcre;
    }

    public BigDecimal getYieldVarianceFromHistoricalPercent() {
        return yieldVarianceFromHistoricalPercent;
    }

    public void setYieldVarianceFromHistoricalPercent(BigDecimal yieldVarianceFromHistoricalPercent) {
        this.yieldVarianceFromHistoricalPercent = yieldVarianceFromHistoricalPercent;
    }

    public String getHistoricalComparisonNote() {
        return historicalComparisonNote;
    }

    public void setHistoricalComparisonNote(String historicalComparisonNote) {
        this.historicalComparisonNote = historicalComparisonNote;
    }

    public FinancialProjectionDto getFinancialProjection() {
        return financialProjection;
    }

    public void setFinancialProjection(FinancialProjectionDto financialProjection) {
        this.financialProjection = financialProjection;
    }

    public boolean isSignificantDeviationFromPrevious() {
        return significantDeviationFromPrevious;
    }

    public void setSignificantDeviationFromPrevious(boolean significantDeviationFromPrevious) {
        this.significantDeviationFromPrevious = significantDeviationFromPrevious;
    }

    public BigDecimal getDeviationFromPreviousPercent() {
        return deviationFromPreviousPercent;
    }

    public void setDeviationFromPreviousPercent(BigDecimal deviationFromPreviousPercent) {
        this.deviationFromPreviousPercent = deviationFromPreviousPercent;
    }

    public String getDeviationNote() {
        return deviationNote;
    }

    public void setDeviationNote(String deviationNote) {
        this.deviationNote = deviationNote;
    }

    /**
     * Builder pattern for creating response DTOs.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final YieldEstimateResponseDto response = new YieldEstimateResponseDto();

        public Builder success(boolean success) {
            response.setSuccess(success);
            return this;
        }

        public Builder message(String message) {
            response.setMessage(message);
            return this;
        }

        public Builder generatedAt(LocalDateTime generatedAt) {
            response.setGeneratedAt(generatedAt);
            return this;
        }

        public Builder predictionId(Long predictionId) {
            response.setPredictionId(predictionId);
            return this;
        }

        public Builder cropId(Long cropId) {
            response.setCropId(cropId);
            return this;
        }

        public Builder farmerId(String farmerId) {
            response.setFarmerId(farmerId);
            return this;
        }

        public Builder cropName(String cropName) {
            response.setCropName(cropName);
            return this;
        }

        public Builder cropVariety(String cropVariety) {
            response.setCropVariety(cropVariety);
            return this;
        }

        public Builder areaAcres(BigDecimal areaAcres) {
            response.setAreaAcres(areaAcres);
            return this;
        }

        public Builder predictedYieldMinQuintalsPerAcre(BigDecimal min) {
            response.setPredictedYieldMinQuintalsPerAcre(min);
            return this;
        }

        public Builder predictedYieldExpectedQuintalsPerAcre(BigDecimal expected) {
            response.setPredictedYieldExpectedQuintalsPerAcre(expected);
            return this;
        }

        public Builder predictedYieldMaxQuintalsPerAcre(BigDecimal max) {
            response.setPredictedYieldMaxQuintalsPerAcre(max);
            return this;
        }

        public Builder confidenceIntervalPercent(BigDecimal confidence) {
            response.setConfidenceIntervalPercent(confidence);
            return this;
        }

        public Builder factorsConsidered(List<String> factors) {
            response.setFactorsConsidered(factors);
            return this;
        }

        public Builder modelVersion(String modelVersion) {
            response.setModelVersion(modelVersion);
            return this;
        }

        public Builder significantDeviationFromPrevious(boolean significant) {
            response.setSignificantDeviationFromPrevious(significant);
            return this;
        }

        public Builder deviationFromPreviousPercent(BigDecimal deviation) {
            response.setDeviationFromPreviousPercent(deviation);
            return this;
        }

        public Builder financialProjection(FinancialProjectionDto financialProjection) {
            response.setFinancialProjection(financialProjection);
            return this;
        }

        public YieldEstimateResponseDto build() {
            return response;
        }
    }
}