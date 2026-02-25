package com.farmer.yield.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing yield predictions for crops.
 * 
 * Stores:
 * - min/expected/max predictions with confidence intervals
 * - factors considered in the prediction
 * - model version used
 * - actual yield and variance after harvest
 * 
 * Validates: Requirements 11B.1, 11B.7, 11B.9
 */
@Entity
@Table(name = "yield_predictions", indexes = {
    @Index(name = "idx_yield_pred_crop_id", columnList = "crop_id"),
    @Index(name = "idx_yield_pred_prediction_date", columnList = "prediction_date"),
    @Index(name = "idx_yield_pred_farmer_id", columnList = "farmer_id")
})
public class YieldPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "crop_id", nullable = false)
    private Long cropId;

    @Column(name = "farmer_id", nullable = false)
    private String farmerId;

    @Column(name = "prediction_date", nullable = false)
    private LocalDate predictionDate;

    @Column(name = "predicted_yield_min_quintals", nullable = false, precision = 10, scale = 2)
    private BigDecimal predictedYieldMinQuintals;

    @Column(name = "predicted_yield_expected_quintals", nullable = false, precision = 10, scale = 2)
    private BigDecimal predictedYieldExpectedQuintals;

    @Column(name = "predicted_yield_max_quintals", nullable = false, precision = 10, scale = 2)
    private BigDecimal predictedYieldMaxQuintals;

    @Column(name = "confidence_interval_percent", precision = 5, scale = 2)
    private BigDecimal confidenceIntervalPercent;

    @Column(name = "factors_considered", columnDefinition = "JSON")
    private String factorsConsidered;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "actual_yield_quintals", precision = 10, scale = 2)
    private BigDecimal actualYieldQuintals;

    @Column(name = "variance_quintals", precision = 10, scale = 2)
    private BigDecimal varianceQuintals;

    @Column(name = "variance_percent", precision = 5, scale = 2)
    private BigDecimal variancePercent;

    @Column(name = "previous_prediction_id")
    private Long previousPredictionId;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDate getPredictionDate() {
        return predictionDate;
    }

    public void setPredictionDate(LocalDate predictionDate) {
        this.predictionDate = predictionDate;
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

    public String getFactorsConsidered() {
        return factorsConsidered;
    }

    public void setFactorsConsidered(String factorsConsidered) {
        this.factorsConsidered = factorsConsidered;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public BigDecimal getActualYieldQuintals() {
        return actualYieldQuintals;
    }

    public void setActualYieldQuintals(BigDecimal actualYieldQuintals) {
        this.actualYieldQuintals = actualYieldQuintals;
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

    public Long getPreviousPredictionId() {
        return previousPredictionId;
    }

    public void setPreviousPredictionId(Long previousPredictionId) {
        this.previousPredictionId = previousPredictionId;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public LocalDateTime getNotificationSentAt() {
        return notificationSentAt;
    }

    public void setNotificationSentAt(LocalDateTime notificationSentAt) {
        this.notificationSentAt = notificationSentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Calculate variance between actual and predicted yield.
     * 
     * @param actualYield The actual harvested yield
     */
    public void calculateVariance(BigDecimal actualYield) {
        this.actualYieldQuintals = actualYield;
        if (actualYield != null && predictedYieldExpectedQuintals != null) {
            this.varianceQuintals = actualYield.subtract(predictedYieldExpectedQuintals);
            if (predictedYieldExpectedQuintals.compareTo(BigDecimal.ZERO) > 0) {
                this.variancePercent = this.varianceQuintals
                        .multiply(new BigDecimal("100"))
                        .divide(predictedYieldExpectedQuintals, 2, java.math.RoundingMode.HALF_UP);
            }
        }
    }

    /**
     * Check if there's a significant deviation from previous prediction.
     * 
     * @param previousPrediction The previous prediction to compare against
     * @return true if deviation exceeds 10%
     */
    public boolean hasSignificantDeviation(YieldPrediction previousPrediction) {
        if (previousPrediction == null || previousPrediction.getPredictedYieldExpectedQuintals() == null) {
            return false;
        }
        
        BigDecimal previousExpected = previousPrediction.getPredictedYieldExpectedQuintals();
        BigDecimal currentExpected = this.predictedYieldExpectedQuintals;
        
        if (previousExpected.compareTo(BigDecimal.ZERO) == 0) {
            return currentExpected.compareTo(BigDecimal.ZERO) != 0;
        }
        
        BigDecimal deviation = currentExpected.subtract(previousExpected)
                .abs()
                .multiply(new BigDecimal("100"))
                .divide(previousExpected, 2, java.math.RoundingMode.HALF_UP);
        
        return deviation.compareTo(new BigDecimal("10")) >= 0;
    }

    /**
     * Builder pattern for creating YieldPrediction instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final YieldPrediction prediction = new YieldPrediction();

        public Builder cropId(Long cropId) {
            prediction.setCropId(cropId);
            return this;
        }

        public Builder farmerId(String farmerId) {
            prediction.setFarmerId(farmerId);
            return this;
        }

        public Builder predictionDate(LocalDate predictionDate) {
            prediction.setPredictionDate(predictionDate);
            return this;
        }

        public Builder predictedYieldMinQuintals(BigDecimal predictedYieldMinQuintals) {
            prediction.setPredictedYieldMinQuintals(predictedYieldMinQuintals);
            return this;
        }

        public Builder predictedYieldExpectedQuintals(BigDecimal predictedYieldExpectedQuintals) {
            prediction.setPredictedYieldExpectedQuintals(predictedYieldExpectedQuintals);
            return this;
        }

        public Builder predictedYieldMaxQuintals(BigDecimal predictedYieldMaxQuintals) {
            prediction.setPredictedYieldMaxQuintals(predictedYieldMaxQuintals);
            return this;
        }

        public Builder confidenceIntervalPercent(BigDecimal confidenceIntervalPercent) {
            prediction.setConfidenceIntervalPercent(confidenceIntervalPercent);
            return this;
        }

        public Builder factorsConsidered(String factorsConsidered) {
            prediction.setFactorsConsidered(factorsConsidered);
            return this;
        }

        public Builder modelVersion(String modelVersion) {
            prediction.setModelVersion(modelVersion);
            return this;
        }

        public Builder previousPredictionId(Long previousPredictionId) {
            prediction.setPreviousPredictionId(previousPredictionId);
            return this;
        }

        public YieldPrediction build() {
            return prediction;
        }
    }
}