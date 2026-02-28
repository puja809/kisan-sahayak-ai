package com.farmer.mandi.service;

import com.farmer.mandi.client.MLServiceClient;
import com.farmer.mandi.dto.FertilizerRecommendationRequest;
import com.farmer.mandi.dto.FertilizerRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FertilizerRecommendationService {
    
    private final MLServiceClient mlServiceClient;
    
    public FertilizerRecommendationResponse recommendFertilizer(FertilizerRecommendationRequest request) {
        log.info("Getting fertilizer recommendation for crop: {}", request.getCrop());
        return mlServiceClient.predictFertilizer(request);
    }
}
