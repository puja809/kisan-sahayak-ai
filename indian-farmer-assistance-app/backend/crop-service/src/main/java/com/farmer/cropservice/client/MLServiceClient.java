package com.farmer.cropservice.client;

import com.farmer.cropservice.dto.FertilizerRecommendationRequest;
import com.farmer.cropservice.dto.FertilizerRecommendationResponse;
import com.farmer.cropservice.dto.CropRotationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MLServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${ml-service.url:http://localhost:8001}")
    private String mlServiceUrl;
    
    public Map<String, Object> predictCrop(Double nitrogen, Double phosphorus, Double potassium,
                                           Double temperature, Double humidity, Double pH, Double rainfall) {
        try {
            String url = mlServiceUrl + "/api/ml/predict-crop";
            log.info("Calling ML service for crop prediction: {}", url);
            
            Map<String, Object> request = Map.of(
                "nitrogen", nitrogen,
                "phosphorus", phosphorus,
                "potassium", potassium,
                "temperature", temperature,
                "humidity", humidity,
                "soilPH", pH,
                "rainfall", rainfall,
                "latitude", 0.0,
                "longitude", 0.0
            );
            
            log.debug("Crop prediction request: {}", request);
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            log.info("Crop prediction response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error calling ML service for crop prediction", e);
            return null;
        }
    }
    
    public Map<String, Object> predictCropRotation(String previousCrop, Double soilPH, String soilType,
                                                   Double temperature, Double humidity, Double rainfall, String season) {
        try {
            String url = mlServiceUrl + "/api/ml/predict-rotation";
            log.info("Calling ML service for crop rotation: {}", url);
            
            CropRotationRequest request = new CropRotationRequest();
            request.setPreviousCrop(previousCrop);
            request.setSoilPH(soilPH);
            request.setSoilType(soilType);
            request.setTemperature(temperature);
            request.setHumidity(humidity);
            request.setRainfall(rainfall);
            request.setSeason(season);
            
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            log.info("Crop rotation response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error calling ML service for crop rotation", e);
            return null;
        }
    }
    
    public FertilizerRecommendationResponse predictFertilizer(FertilizerRecommendationRequest request) {
        try {
            String url = mlServiceUrl + "/api/ml/predict-fertilizer";
            log.info("Calling ML service for fertilizer prediction: {}", url);
            
            FertilizerRecommendationResponse response = restTemplate.postForObject(
                    url,
                    request,
                    FertilizerRecommendationResponse.class
            );
            
            log.info("Fertilizer prediction response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error calling ML service for fertilizer prediction", e);
            return null;
        }
    }
}
