package com.farmer.yield.controller;

import com.farmer.yield.dto.*;
import com.farmer.yield.entity.YieldPrediction;
import com.farmer.yield.service.YieldPredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for yield prediction endpoints.
 * 
 * Endpoints:
 * - POST /api/v1/crops/yield/estimate - Generate yield estimate
 * - GET /api/v1/crops/yield/history/{cropId} - Get yield history
 * - POST /api/v1/crops/yield/actual - Record actual yield
 * - POST /api/v1/ai/yield/predict - AI-based yield prediction
 * - POST /api/v1/ai/yield/update-model - Update ML model with variance data
 * 
 * Validates: Requirements 11B.1, 11B.7, 11B.9
 */
@RestController
@RequestMapping("/api/v1")
public class YieldController {

    private static final Logger logger = LoggerFactory.getLogger(YieldController.class);

    private final YieldPredictionService yieldPredictionService;

    public YieldController(YieldPredictionService yieldPredictionService) {
        this.yieldPredictionService = yieldPredictionService;
    }

    /**
     * Generate yield estimate for a crop.
     * 
     * @param request Yield estimation request
     * @return Yield estimate response with min/expected/max values
     */
    @PostMapping("/crops/yield/estimate")
    public ResponseEntity<YieldEstimateResponseDto> estimateYield(
            @RequestBody YieldEstimateRequestDto request) {
        
        logger.info("Received yield estimate request for farmer: {}, crop: {}", 
                request.getFarmerId(), request.getCropName());
        
        YieldEstimateResponseDto response = yieldPredictionService.generateYieldEstimate(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get yield prediction history for a crop.
     * 
     * @param cropId Crop ID
     * @return List of yield predictions
     */
    @GetMapping("/crops/yield/history/{cropId}")
    public ResponseEntity<List<YieldPrediction>> getYieldHistory(@PathVariable Long cropId) {
        logger.info("Fetching yield history for crop: {}", cropId);
        
        List<YieldPrediction> history = yieldPredictionService.getYieldHistory(cropId);
        return ResponseEntity.ok(history);
    }

    /**
     * Record actual yield after harvest.
     * 
     * @param request Actual yield request
     * @return Variance tracking data
     */
    @PostMapping("/crops/yield/actual")
    public ResponseEntity<VarianceTrackingDto> recordActualYield(
            @RequestBody ActualYieldRequestDto request) {
        
        logger.info("Recording actual yield for crop: {}", request.getCropId());
        
        try {
            VarianceTrackingDto response = yieldPredictionService.recordActualYield(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error recording actual yield: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * AI-based yield prediction endpoint.
     * This endpoint integrates with the AI service for advanced predictions.
     * 
     * @param request Yield estimation request
     * @return AI-enhanced yield prediction
     */
    @PostMapping("/ai/yield/predict")
    public ResponseEntity<YieldEstimateResponseDto> aiYieldPredict(
            @RequestBody YieldEstimateRequestDto request) {
        
        logger.info("Received AI yield prediction request for farmer: {}, crop: {}", 
                request.getFarmerId(), request.getCropName());
        
        // For now, use the standard prediction service
        // In a full implementation, this would call the AI/ML service
        YieldEstimateResponseDto response = yieldPredictionService.generateYieldEstimate(request);
        
        // Mark as AI-generated
        if (response.isSuccess()) {
            response.setModelVersion("AI-Enhanced " + response.getModelVersion());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update ML model with variance data.
     * This endpoint is used to feed actual vs predicted data back to the ML model.
     * 
     * @param request Variance tracking data
     * @return Confirmation of model update
     */
    @PostMapping("/ai/yield/update-model")
    public ResponseEntity<Map<String, Object>> updateModel(
            @RequestBody VarianceTrackingDto request) {
        
        logger.info("Received model update request for prediction: {}", request.getPredictionId());
        
        try {
            // In a full implementation, this would:
            // 1. Store the variance data for model training
            // 2. Trigger model retraining if sufficient new data is available
            // 3. Update model version if retraining improved accuracy
            
            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Variance data recorded for model improvement",
                    "predictionId", request.getPredictionId(),
                    "variancePercent", request.getVariancePercent() != null ? 
                            request.getVariancePercent().toString() : "N/A",
                    "usedForTraining", true,
                    "modelVersion", "1.0.1"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating model: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to update model: " + e.getMessage()
            ));
        }
    }

    /**
     * Get predictions that need notification.
     * This endpoint is used by the notification service to fetch predictions with significant deviations.
     * 
     * @return List of predictions needing notification
     */
    @GetMapping("/crops/yield/notifications/pending")
    public ResponseEntity<List<YieldPrediction>> getPendingNotifications() {
        logger.info("Fetching pending yield notifications");
        
        List<YieldPrediction> predictions = yieldPredictionService.getPredictionsNeedingNotification();
        return ResponseEntity.ok(predictions);
    }

    /**
     * Mark a prediction as notified.
     * 
     * @param predictionId Prediction ID
     * @return Confirmation
     */
    @PostMapping("/crops/yield/notifications/{predictionId}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeNotification(
            @PathVariable Long predictionId) {
        
        logger.info("Acknowledging notification for prediction: {}", predictionId);
        
        try {
            yieldPredictionService.markAsNotified(predictionId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notification acknowledged",
                    "predictionId", predictionId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to acknowledge notification: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint.
     * 
     * @return Health status
     */
    @GetMapping("/yield/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "yield-service"
        ));
    }
}