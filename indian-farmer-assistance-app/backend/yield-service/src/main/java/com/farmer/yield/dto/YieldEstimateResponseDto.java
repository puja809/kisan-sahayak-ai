package com.farmer.yield.dto;

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
    private Double areaAcres;
    
    // Yield estimates (per acre)
    private Double predictedYieldMinQuintalsPerAcre;
    private Double predictedYieldExpectedQuintalsPerAcre;
    private Double predictedYieldMaxQuintalsPerAcre;
    
    // Total yield estimates (for the entire area)
    private Double predictedYieldMinQuintals;
    private Double predictedYieldExpectedQuintals;
    private Double predictedYieldMaxQuintals;
    
    // Confidence and factors
    private Double confidenceIntervalPercent;
    private List<String> factorsConsidered;
    private List<String> factorAdjustments; // Positive/negative adjustments made
    private String modelVersion;
    
    // Growth stage info
    private String currentGrowthStage;
    private Integer daysSinceSowing;
    private Integer estimatedDaysToHarvest;
    
    // Historical comparison
    private Double historicalAverageYieldQuintalsPerAcre;
    private Double yieldVarianceFromHistoricalPercent;
    private String historicalComparisonNote;
    
    // Financial projections
    private FinancialProjectionDto financialProjection;
    
    // Notification flag
    private boolean significantDeviationFromPrevious;
    private Double deviationFromPreviousPercent;
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

    public Double getAreaAcres() {
        return areaAcres;
    }

    public void setAreaAcres(Double areaAcres) {
        this.areaAcres = areaAcres;
    }

    public Double getPredictedYieldMinQuintalsPerAcre() {
        return predictedYieldMinQuintalsPerAcre;
    }

    public void setPredictedYieldMinQuintalsPerAcre(Double predictedYieldMinQuintalsPerAcre) {
        this.predictedYieldMinQuintalsPerAcre = predictedYieldMinQuintalsPerAcre;
    }

    public Double getPredictedYieldExpectedQuintalsPerAcre() {
        return predictedYieldExpectedQuintalsPerAcre;
    }

    public void setPredictedYieldExpectedQuintalsPerAcre(Double predictedYieldExpectedQuintalsPerAcre) {
        this.predictedYieldExpectedQuintalsPerAcre = predictedYieldExpectedQuintalsPerAcre;
    }

    public Double getPredictedYieldMaxQuintalsPerAcre() {
        return predictedYieldMaxQuintalsPerAcre;
    }

    public void setPredictedYieldMaxQuintalsPerAcre(Double predictedYieldMaxQuintalsPerAcre) {
        this.predictedYieldMaxQuintalsPerAcre = predictedYieldMaxQuintalsPerAcre;
    }

    public Double getPredictedYieldMinQuintals() {
        return predictedYieldMinQuintals;
    }

    public void setPredictedYieldMinQuintals(Double predictedYieldMinQuintals) {
        this.predictedYieldMinQuintals = predictedYieldMinQuintals;
    }

    public Double getPredictedYieldExpectedQuintals() {
        return predictedYieldExpectedQuintals;
    }

    public void setPredictedYieldExpectedQuintals(Double predictedYieldExpectedQuintals) {
        this.predictedYieldExpectedQuintals = predictedYieldExpectedQuintals;
    }

    public Double getPredictedYieldMaxQuintals() {
        return predictedYieldMaxQuintals;
    }

    public void setPredictedYieldMaxQuintals(Double predictedYieldMaxQuintals) {
        this.predictedYieldMaxQuintals = predictedYieldMaxQuintals;
    }

    public Double getConfidenceIntervalPercent() {
        return confidenceIntervalPercent;
    }

    public void setConfidenceIntervalPercent(Double confidenceIntervalPercent) {
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

    public Double getHistoricalAverageYieldQuintalsPerAcre() {
        return historicalAverageYieldQuintalsPerAcre;
    }

    public void setHistoricalAverageYieldQuintalsPerAcre(Double historicalAverageYieldQuintalsPerAcre) {
        this.historicalAverageYieldQuintalsPerAcre = historicalAverageYieldQuintalsPerAcre;
    }

    public Double getYieldVarianceFromHistoricalPercent() {
        return yieldVarianceFromHistoricalPercent;
    }

    public void setYieldVarianceFromHistoricalPercent(Double yieldVarianceFromHistoricalPercent) {
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

    public Double getDeviationFromPreviousPercent() {
        return deviationFromPreviousPercent;
    }

    public void setDeviationFromPreviousPercent(Double deviationFromPreviousPercent) {
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
    public static YieldEstimateResponseBuilder builder() {
        return new YieldEstimateResponseBuilder();
    }

    public static class YieldEstimateResponseBuilder {
        private final YieldEstimateResponseDto response = new YieldEstimateResponseDto();

        public YieldEstimateResponseBuilder success(boolean success) {
            response.setSuccess(success);
            return this;
        }

        public YieldEstimateResponseBuilder message(String message) {
            response.setMessage(message);
            return this;
        }

        public YieldEstimateResponseBuilder generatedAt(LocalDateTime generatedAt) {
            response.setGeneratedAt(generatedAt);
            return this;
        }

        public YieldEstimateResponseBuilder predictionId(Long predictionId) {
            response.setPredictionId(predictionId);
            return this;
        }

        public YieldEstimateResponseBuilder cropId(Long cropId) {
            response.setCropId(cropId);
            return this;
        }

        public YieldEstimateResponseBuilder farmerId(String farmerId) {
            response.setFarmerId(farmerId);
            return this;
        }

        public YieldEstimateResponseBuilder cropName(String cropName) {
            response.setCropName(cropName);
            return this;
        }

        public YieldEstimateResponseBuilder cropVariety(String cropVariety) {
            response.setCropVariety(cropVariety);
            return this;
        }

        public YieldEstimateResponseBuilder areaAcres(Double areaAcres) {
            response.setAreaAcres(areaAcres);
            return this;
        }

        public YieldEstimateResponseBuilder predictedYieldMinQuintalsPerAcre(Double min) {
            response.setPredictedYieldMinQuintalsPerAcre(min);
            return this;
        }

        public YieldEstimateResponseBuilder predictedYieldExpectedQuintalsPerAcre(Double expected) {
            response.setPredictedYieldExpectedQuintalsPerAcre(expected);
            return this;
        }

        public YieldEstimateResponseBuilder predictedYieldMaxQuintalsPerAcre(Double max) {
            response.setPredictedYieldMaxQuintalsPerAcre(max);
            return this;
        }

        public YieldEstimateResponseBuilder predictedYieldMinQuintals(Double min) {
            response.setPredictedYieldMinQuintals(min);
            return this;
        }

        public YieldEstimateResponseBuilder predictedYieldExpectedQuintals(Double expected) {
            response.setPredictedYieldExpectedQuintals(expected);
            return this;
        }

        public YieldEstimateResponseBuilder predictedYieldMaxQuintals(Double max) {
            response.setPredictedYieldMaxQuintals(max);
            return this;
        }

        public YieldEstimateResponseBuilder confidenceIntervalPercent(Double confidence) {
            response.setConfidenceIntervalPercent(confidence);
            return this;
        }

        public YieldEstimateResponseBuilder factorsConsidered(List<String> factors) {
            response.setFactorsConsidered(factors);
            return this;
        }

        public YieldEstimateResponseBuilder factorAdjustments(List<String> adjustments) {
            response.setFactorAdjustments(adjustments);
            return this;
        }

        public YieldEstimateResponseBuilder modelVersion(String modelVersion) {
            response.setModelVersion(modelVersion);
            return this;
        }

        public YieldEstimateResponseBuilder currentGrowthStage(String stage) {
            response.setCurrentGrowthStage(stage);
            return this;
        }

        public YieldEstimateResponseBuilder significantDeviationFromPrevious(boolean significant) {
            response.setSignificantDeviationFromPrevious(significant);
            return this;
        }

        public YieldEstimateResponseBuilder deviationFromPreviousPercent(Double deviation) {
            response.setDeviationFromPreviousPercent(deviation);
            return this;
        }

        public YieldEstimateResponseBuilder financialProjection(FinancialProjectionDto financialProjection) {
            response.setFinancialProjection(financialProjection);
            return this;
        }

        public YieldEstimateResponseBuilder historicalAverageYieldQuintalsPerAcre(Double avg) {
            response.setHistoricalAverageYieldQuintalsPerAcre(avg);
            return this;
        }

        public YieldEstimateResponseBuilder yieldVarianceFromHistoricalPercent(Double variance) {
            response.setYieldVarianceFromHistoricalPercent(variance);
            return this;
        }

        public YieldEstimateResponseBuilder historicalComparisonNote(String note) {
            response.setHistoricalComparisonNote(note);
            return this;
        }

        public YieldEstimateResponseDto build() {
            return response;
        }
    }
}