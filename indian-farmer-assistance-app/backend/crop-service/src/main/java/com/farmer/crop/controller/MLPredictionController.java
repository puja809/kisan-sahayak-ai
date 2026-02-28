package com.farmer.crop.controller;

import com.farmer.crop.dto.MLPredictionResponseDto;
import com.farmer.crop.service.MLCropPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for ML-based crop predictions.
 * 
 * Provides ML model endpoints that complement the existing crop recommendation
 * and rotation services with machine learning predictions.
 * 
 * These endpoints use trained Random Forest models to provide:
 * - Quick ML-based crop recommendations
 * - ML-based crop rotation suggestions
 * 
 * Note: These are complementary to the existing CropRecommendationController
 * and CropRotationController which provide more comprehensive analysis.
 */
@Slf4j
@RestController
@RequestMapping("/api/crop/ml-predictions")
@Tag(name = "ML Crop Predictions", description = "ML-based crop recommendation and rotation predictions")
public class MLPredictionController {
    
    private final MLCropPredictionService mlCropPredictionService;
    
    public MLPredictionController(MLCropPredictionService mlCropPredictionService) {
        this.mlCropPredictionService = mlCropPredictionService;
    }
    
    /**
     * Get ML-based crop recommendation for a location.
     * 
     * This endpoint uses a trained Random Forest model to quickly predict
     * suitable crops based on soil nutrients and weather conditions.
     * 
     * For more comprehensive recommendations including market data and climate risk,
     * use: POST /api/v1/crops/recommendations/calculate
     * 
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @param nitrogen Soil nitrogen (mg/kg), default 100
     * @param phosphorus Soil phosphorus (mg/kg), default 50
     * @param potassium Soil potassium (mg/kg), default 50
     * @return ML prediction with confidence score and alternatives
     */
    @PostMapping("/recommend-crop")
    @Operation(summary = "Get ML-based crop recommendation", 
               description = "Quick ML prediction for suitable crops using Random Forest model")
    public ResponseEntity<MLPredictionResponseDto> recommendCrop(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "100") Double nitrogen,
            @RequestParam(defaultValue = "50") Double phosphorus,
            @RequestParam(defaultValue = "50") Double potassium) {
        
        log.info("ML crop recommendation request: lat={}, lon={}", latitude, longitude);
        
        MLPredictionResponseDto response = mlCropPredictionService.recommendCropByLocation(
            latitude, longitude, nitrogen, phosphorus, potassium
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get ML-based crop rotation recommendation.
     * 
     * This endpoint uses a trained Random Forest model to predict the best
     * next crop for rotation based on the previous crop and current conditions.
     * 
     * For more comprehensive rotation analysis including history and patterns,
     * use: POST /api/v1/crops/rotation/generate
     * 
     * @param previousCrop The crop grown in the previous season
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @param season Current season (Kharif, Rabi, Summer)
     * @return ML prediction for next crop with confidence score
     */
    @PostMapping("/recommend-rotation")
    @Operation(summary = "Get ML-based crop rotation recommendation",
               description = "Quick ML prediction for next crop in rotation using Random Forest model")
    public ResponseEntity<MLPredictionResponseDto> recommendRotation(
            @RequestParam String previousCrop,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam String season) {
        
        log.info("ML crop rotation recommendation request: previousCrop={}, season={}", previousCrop, season);
        
        MLPredictionResponseDto response = mlCropPredictionService.recommendCropRotation(
            previousCrop, latitude, longitude, season
        );
        
        return ResponseEntity.ok(response);
    }
}
