package com.farmer.crop.client;

import com.farmer.crop.dto.CropRecommendationMLRequestDto;
import com.farmer.crop.dto.CropRotationMLRequestDto;
import com.farmer.crop.dto.MLPredictionResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Slf4j
@Component
public class MLModelClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${ml.service.url:http://localhost:8001}")
    private String mlServiceUrl;
    
    public MLModelClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public MLPredictionResponseDto predictCrop(CropRecommendationMLRequestDto request) {
        try {
            String url = mlServiceUrl + "/api/ml/predict-crop";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<CropRecommendationMLRequestDto> entity = new HttpEntity<>(request, headers);
            
            MLPredictionResponseDto response = restTemplate.postForObject(
                url, 
                entity, 
                MLPredictionResponseDto.class
            );
            
            log.info("Crop prediction successful: {}", response.getPrediction());
            return response;
        } catch (Exception e) {
            log.error("Error calling ML service for crop prediction", e);
            throw new RuntimeException("ML service unavailable", e);
        }
    }
    
    public MLPredictionResponseDto predictCropRotation(CropRotationMLRequestDto request) {
        try {
            String url = mlServiceUrl + "/api/ml/predict-rotation";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<CropRotationMLRequestDto> entity = new HttpEntity<>(request, headers);
            
            MLPredictionResponseDto response = restTemplate.postForObject(
                url, 
                entity, 
                MLPredictionResponseDto.class
            );
            
            log.info("Crop rotation prediction successful: {}", response.getPrediction());
            return response;
        } catch (Exception e) {
            log.error("Error calling ML service for crop rotation prediction", e);
            throw new RuntimeException("ML service unavailable", e);
        }
    }
}
