package com.farmer.yield.dto;


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
    private Double predictedYieldExpectedQuintals;
    private Double predictedYieldMinQuintals;
    private Double predictedYieldMaxQuintals;
    private String modelVersion;
    
    // Actual results
    private LocalDate harvestDate;
    private Double actualYieldQuintals;
    private String qualityGrade;
    
    // Variance calculations
    private Double varianceQuintals;
    private Double variancePercent;
    private String varianceCategory; // "positive", "negative", "neutral"
    
    // Model improvement data
    private Double modelAccuracyPercent;
    private String improvementSuggestion;
    private boolean usedForModelTraining;
    private LocalDateTime processedAt;
    
    // Historical context
    private Double averageVarianceForCrop;
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

    public Double getPredictedYieldExpectedQuintals() {
        return predictedYieldExpectedQuintals;
    }

    public void setPredictedYieldExpectedQuintals(Double predictedYieldExpectedQuintals) {
        this.predictedYieldExpectedQuintals = predictedYieldExpectedQuintals;
    }

    public Double getPredictedYieldMinQuintals() {
        return predictedYieldMinQuintals;
    }

    public void setPredictedYieldMinQuintals(Double predictedYieldMinQuintals) {
        this.predictedYieldMinQuintals = predictedYieldMinQuintals;
    }

    public Double getPredictedYieldMaxQuintals() {
        return predictedYieldMaxQuintals;
    }

    public void setPredictedYieldMaxQuintals(Double predictedYieldMaxQuintals) {
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

    public Double getActualYieldQuintals() {
        return actualYieldQuintals;
    }

    public void setActualYieldQuintals(Double actualYieldQuintals) {
        this.actualYieldQuintals = actualYieldQuintals;
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(String qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public Double getVarianceQuintals() {
        return varianceQuintals;
    }

    public void setVarianceQuintals(Double varianceQuintals) {
        this.varianceQuintals = varianceQuintals;
    }

    public Double getVariancePercent() {
        return variancePercent;
    }

    public void setVariancePercent(Double variancePercent) {
        this.variancePercent = variancePercent;
    }

    public String getVarianceCategory() {
        return varianceCategory;
    }

    public void setVarianceCategory(String varianceCategory) {
        this.varianceCategory = varianceCategory;
    }

    public Double getModelAccuracyPercent() {
        return modelAccuracyPercent;
    }

    public void setModelAccuracyPercent(Double modelAccuracyPercent) {
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

    public Double getAverageVarianceForCrop() {
        return averageVarianceForCrop;
    }

    public void setAverageVarianceForCrop(Double averageVarianceForCrop) {
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
    public static VarianceTrackingDtoBuilder builder() {
        return new VarianceTrackingDtoBuilder();
    }

    public static class VarianceTrackingDtoBuilder {
        private final VarianceTrackingDto dto = new VarianceTrackingDto();

        public VarianceTrackingDtoBuilder predictionId(Long predictionId) {
            dto.setPredictionId(predictionId);
            return this;
        }

        public VarianceTrackingDtoBuilder cropId(Long cropId) {
            dto.setCropId(cropId);
            return this;
        }

        public VarianceTrackingDtoBuilder farmerId(String farmerId) {
            dto.setFarmerId(farmerId);
            return this;
        }

        public VarianceTrackingDtoBuilder cropName(String cropName) {
            dto.setCropName(cropName);
            return this;
        }

        public VarianceTrackingDtoBuilder predictedYieldExpectedQuintals(Double predicted) {
            dto.setPredictedYieldExpectedQuintals(predicted);
            return this;
        }

        public VarianceTrackingDtoBuilder actualYieldQuintals(Double actual) {
            dto.setActualYieldQuintals(actual);
            return this;
        }

        public VarianceTrackingDtoBuilder varianceQuintals(Double variance) {
            dto.setVarianceQuintals(variance);
            return this;
        }

        public VarianceTrackingDtoBuilder variancePercent(Double variancePercent) {
            dto.setVariancePercent(variancePercent);
            return this;
        }

        public VarianceTrackingDtoBuilder varianceCategory(String category) {
            dto.setVarianceCategory(category);
            return this;
        }

        public VarianceTrackingDtoBuilder modelVersion(String modelVersion) {
            dto.setModelVersion(modelVersion);
            return this;
        }

        public VarianceTrackingDtoBuilder averageVarianceForCrop(Double avgVariance) {
            dto.setAverageVarianceForCrop(avgVariance);
            return this;
        }

        public VarianceTrackingDto build() {
            return dto;
        }
    }
}