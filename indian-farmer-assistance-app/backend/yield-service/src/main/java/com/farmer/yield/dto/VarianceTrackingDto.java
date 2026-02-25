package com.farmer.yield.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for variance tracking and model improvement feedback.
 * 
 * Contains:
 * - Prediction vs actual comparison
 * - Variance calculations
 * - Model improvement suggestions
 * - Historical variance trends
 * 
 * Validates: Requirement 11B.9
 */
public class VarianceTrackingDto {

    private Long predictionId;
    private Long cropId;
    private String farmerId;
    private String cropName;
    
    // Prediction details
    private LocalDate predictionDate;
    private BigDecimal predictedYieldExpectedQuintals;
    private BigDecimal predictedYieldMinQuintals;
    private BigDecimal predictedYieldMaxQuintals;
    private String modelVersion;
    
    // Actual results
    private LocalDate harvestDate;
    private BigDecimal actualYieldQuintals;
    private String qualityGrade;
    
    // Variance calculations
    private BigDecimal varianceQuintals;
    private BigDecimal variancePercent;
    private String varianceCategory; // "positive", "negative", "neutral"
    
    // Model improvement data
    private BigDecimal modelAccuracyPercent;
    private String improvementSuggestion;
    private boolean usedForModelTraining;
    private LocalDateTime processedAt;
    
    // Historical context
    private BigDecimal averageVarianceForCrop;
    private Integer totalPredictionsForCrop;
    private Integer predictionsWithActualData;
    
    // Getters and Setters
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

    public LocalDate getPredictionDate() {
        return predictionDate;
    }

    public void setPredictionDate(LocalDate predictionDate) {
        this.predictionDate = predictionDate;
    }

    public BigDecimal getPredictedYieldExpectedQuintals() {
        return predictedYieldExpectedQuintals;
    }

    public void setPredictedYieldExpectedQuintals(BigDecimal predictedYieldExpectedQuintals) {
        this.predictedYieldExpectedQuintals = predictedYieldExpectedQuintals;
    }

    public BigDecimal getPredictedYieldMinQuintals() {
        return predictedYieldMinQuintals;
    }

    public void setPredictedYieldMinQuintals(BigDecimal predictedYieldMinQuintals) {
        this.predictedYieldMinQuintals = predictedYieldMinQuintals;
    }

    public BigDecimal getPredictedYieldMaxQuintals() {
        return predictedYieldMaxQuintals;
    }

    public void setPredictedYieldMaxQuintals(BigDecimal predictedYieldMaxQuintals) {
        this.predictedYieldMaxQuintals = predictedYieldMaxQuintals;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public LocalDate getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(LocalDate harvestDate) {
        this.harvestDate = harvestDate;
    }

    public BigDecimal getActualYieldQuintals() {
        return actualYieldQuintals;
    }

    public void setActualYieldQuintals(BigDecimal actualYieldQuintals) {
        this.actualYieldQuintals = actualYieldQuintals;
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(String qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public BigDecimal getVarianceQuintals() {
        return varianceQuintals;
    }

    public void setVarianceQuintals(BigDecimal varianceQuintals) {
        this.varianceQuintals = varianceQuintals;
    }

    public BigDecimal getVariancePercent() {
        return variancePercent;
    }

    public void setVariancePercent(BigDecimal variancePercent) {
        this.variancePercent = variancePercent;
    }

    public String getVarianceCategory() {
        return varianceCategory;
    }

    public void setVarianceCategory(String varianceCategory) {
        this.varianceCategory = varianceCategory;
    }

    public BigDecimal getModelAccuracyPercent() {
        return modelAccuracyPercent;
    }

    public void setModelAccuracyPercent(BigDecimal modelAccuracyPercent) {
        this.modelAccuracyPercent = modelAccuracyPercent;
    }

    public String getImprovementSuggestion() {
        return improvementSuggestion;
    }

    public void setImprovementSuggestion(String improvementSuggestion) {
        this.improvementSuggestion = improvementSuggestion;
    }

    public boolean isUsedForModelTraining() {
        return usedForModelTraining;
    }

    public void setUsedForModelTraining(boolean usedForModelTraining) {
        this.usedForModelTraining = usedForModelTraining;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public BigDecimal getAverageVarianceForCrop() {
        return averageVarianceForCrop;
    }

    public void setAverageVarianceForCrop(BigDecimal averageVarianceForCrop) {
        this.averageVarianceForCrop = averageVarianceForCrop;
    }

    public Integer getTotalPredictionsForCrop() {
        return totalPredictionsForCrop;
    }

    public void setTotalPredictionsForCrop(Integer totalPredictionsForCrop) {
        this.totalPredictionsForCrop = totalPredictionsForCrop;
    }

    public Integer getPredictionsWithActualData() {
        return predictionsWithActualData;
    }

    public void setPredictionsWithActualData(Integer predictionsWithActualData) {
        this.predictionsWithActualData = predictionsWithActualData;
    }

    /**
     * Builder pattern for creating variance tracking DTOs.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final VarianceTrackingDto dto = new VarianceTrackingDto();

        public Builder predictionId(Long predictionId) {
            dto.setPredictionId(predictionId);
            return this;
        }

        public Builder cropId(Long cropId) {
            dto.setCropId(cropId);
            return this;
        }

        public Builder farmerId(String farmerId) {
            dto.setFarmerId(farmerId);
            return this;
        }

        public Builder cropName(String cropName) {
            dto.setCropName(cropName);
            return this;
        }

        public Builder predictedYieldExpectedQuintals(BigDecimal predicted) {
            dto.setPredictedYieldExpectedQuintals(predicted);
            return this;
        }

        public Builder actualYieldQuintals(BigDecimal actual) {
            dto.setActualYieldQuintals(actual);
            return this;
        }

        public Builder varianceQuintals(BigDecimal variance) {
            dto.setVarianceQuintals(variance);
            return this;
        }

        public Builder variancePercent(BigDecimal variancePercent) {
            dto.setVariancePercent(variancePercent);
            return this;
        }

        public Builder varianceCategory(String category) {
            dto.setVarianceCategory(category);
            return this;
        }

        public Builder modelVersion(String modelVersion) {
            dto.setModelVersion(modelVersion);
            return this;
        }

        public Builder averageVarianceForCrop(BigDecimal avgVariance) {
            dto.setAverageVarianceForCrop(avgVariance);
            return this;
        }

        public VarianceTrackingDto build() {
            return dto;
        }
    }
}