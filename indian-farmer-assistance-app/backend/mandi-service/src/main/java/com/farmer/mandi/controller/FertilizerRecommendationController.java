package com.farmer.mandi.controller;

import com.farmer.mandi.dto.FertilizerRecommendationRequest;
import com.farmer.mandi.dto.FertilizerRecommendationResponse;
import com.farmer.mandi.service.FertilizerRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fertilizer/recommendation")
@RequiredArgsConstructor
@Slf4j
public class FertilizerRecommendationController {
    
    private final FertilizerRecommendationService fertilizerRecommendationService;
    
    @PostMapping("/predict")
    public ResponseEntity<FertilizerRecommendationResponse> predictFertilizer(
            @RequestBody FertilizerRecommendationRequest request) {
        log.info("Predicting fertilizer for crop: {}", request.getCrop());
        
        FertilizerRecommendationResponse response = fertilizerRecommendationService.recommendFertilizer(request);
        return ResponseEntity.ok(response);
    }
}
