package com.farmer.crop.controller;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.FertilizerApplication;
import com.farmer.crop.service.FertilizerRecommendationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for fertilizer recommendation and tracking.
 * 
 * Endpoints:
 * - GET /api/v1/crops/fertilizer/recommend - Get fertilizer recommendations
 * - POST /api/v1/crops/fertilizer/calculate - Calculate nutrient requirements
 * - GET /api/v1/crops/fertilizer/history/{cropId} - Get fertilizer history for a crop
 * - POST /api/v1/crops/fertilizer/application - Record a fertilizer application
 * 
 * Validates: Requirements 11C.1, 11C.3, 11C.6, 11C.7
 */
@RestController
@RequestMapping("/api/v1/crops/fertilizer")
public class FertilizerController {

    private static final Logger logger = LoggerFactory.getLogger(FertilizerController.class);

    private final FertilizerRecommendationService fertilizerRecommendationService;

    public FertilizerController(FertilizerRecommendationService fertilizerRecommendationService) {
        this.fertilizerRecommendationService = fertilizerRecommendationService;
    }

    /**
     * Get fertilizer recommendations for a crop.
     * 
     * @param request Recommendation request with crop details and optional soil health data
     * @return Fertilizer recommendation response
     * 
     * Validates: Requirements 11C.1, 11C.2, 11C.3, 11C.4, 11C.5
     */
    @PostMapping("/recommend")
    public ResponseEntity<FertilizerRecommendationResponseDto> getRecommendations(
            @Valid @RequestBody FertilizerRecommendationRequestDto request) {
        
        logger.info("Received fertilizer recommendation request for farmer: {}, crop: {}", 
                request.getFarmerId(), request.getCropName());
        
        FertilizerRecommendationResponseDto response = 
                fertilizerRecommendationService.generateRecommendations(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Calculate nutrient requirements for a crop.
     * 
     * @param request Calculation request with crop details
     * @return Nutrient requirements response
     * 
     * Validates: Requirement 11C.3
     */
    @PostMapping("/calculate")
    public ResponseEntity<FertilizerRecommendationResponseDto> calculateNutrients(
            @Valid @RequestBody FertilizerRecommendationRequestDto request) {
        
        logger.info("Received nutrient calculation request for farmer: {}, crop: {}", 
                request.getFarmerId(), request.getCropName());
        
        // Set default options for calculation
        request.setIncludeOrganicAlternatives(false);
        request.setIncludeSplitApplication(true);
        
        FertilizerRecommendationResponseDto response = 
                fertilizerRecommendationService.generateRecommendations(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get fertilizer application history for a crop.
     * 
     * @param cropId Crop ID
     * @return Fertilizer tracking response with application history and nutrient calculations
     * 
     * Validates: Requirements 11C.6, 11C.7, 11C.8, 11C.11
     */
    @GetMapping("/history/{cropId}")
    public ResponseEntity<FertilizerTrackingResponseDto> getFertilizerHistory(
            @PathVariable Long cropId) {
        
        logger.info("Received fertilizer history request for crop: {}", cropId);
        
        FertilizerTrackingResponseDto response = 
                fertilizerRecommendationService.getFertilizerTracking(cropId);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Record a fertilizer application.
     * 
     * @param request Application details
     * @return Saved application confirmation
     * 
     * Validates: Requirement 11C.6
     */
    @PostMapping("/application")
    public ResponseEntity<FertilizerApplication> recordApplication(
            @Valid @RequestBody FertilizerApplicationRequestDto request) {
        
        logger.info("Recording fertilizer application for crop: {}, type: {}", 
                request.getCropId(), request.getFertilizerType());
        
        FertilizerApplication application = 
                fertilizerRecommendationService.recordApplication(request);
        
        return ResponseEntity.ok(application);
    }
}