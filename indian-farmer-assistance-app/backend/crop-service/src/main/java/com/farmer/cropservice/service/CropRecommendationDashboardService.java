package com.farmer.cropservice.service;

import com.farmer.cropservice.client.KaegroCropSoilApiClient;
import com.farmer.cropservice.client.MLServiceClient;
import com.farmer.cropservice.client.WeatherApiClient;
import com.farmer.cropservice.dto.*;
import com.farmer.cropservice.util.SoilTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CropRecommendationDashboardService {
    
    private final KaegroCropSoilApiClient kaegroCropSoilApiClient;
    private final WeatherApiClient weatherApiClient;
    private final MLServiceClient mlServiceClient;
    
    public CropRecommendationDashboardResponse getDashboardData(CropRecommendationDashboardRequest request) {
        log.info("Getting dashboard data for location: {}, {}", request.getLatitude(), request.getLongitude());
        
        // Fetch soil data
        SoilDataResponse soilData = kaegroCropSoilApiClient.getSoilData(
            request.getLatitude(),
            request.getLongitude()
        );
        
        // Fetch weather data
        WeatherDataResponse weatherData = weatherApiClient.getForecastWeather(
            request.getLatitude(),
            request.getLongitude(),
            14
        );
        
        // Extract weather parameters for ML models
        Double temperature = extractTemperature(weatherData);
        Double humidity = extractHumidity(weatherData);
        Double rainfall = extractRainfall(weatherData);
        
        // Extract soil parameters with null checks
        Double soilPH = extractSoilPH(soilData);
        String soilType = extractSoilType(soilData);
        
        // Get crop recommendation
        CropRecommendationDashboardResponse.CropPrediction cropRecommendation = 
            getCropRecommendation(soilData, temperature, humidity, rainfall, soilPH);
        
        // Get crop rotation recommendation if previous crop is provided
        CropRecommendationDashboardResponse.CropPrediction cropRotation = null;
        if (request.getPreviousCrop() != null && !request.getPreviousCrop().isEmpty()) {
            cropRotation = getCropRotation(
                request.getPreviousCrop(),
                soilPH,
                soilType,
                temperature,
                humidity,
                rainfall,
                request.getSeason()
            );
        }
        
        // Get fertilizer recommendation
        FertilizerRecommendationResponse fertilizerRecommendation = null;
        if (cropRecommendation != null) {
            fertilizerRecommendation = getFertilizerRecommendation(
                cropRecommendation.getPrediction(),
                soilType,
                soilPH,
                temperature,
                humidity,
                rainfall,
                request.getSeason()
            );
        }
        
        // Build response
        CropRecommendationDashboardResponse response = new CropRecommendationDashboardResponse();
        response.setCropRecommendation(cropRecommendation);
        response.setCropRotation(cropRotation);
        response.setFertilizerRecommendation(fertilizerRecommendation);
        response.setSoilData(soilData);
        response.setWeatherData(weatherData);
        response.setLocation(String.format("%.4f, %.4f", request.getLatitude(), request.getLongitude()));
        
        return response;
    }
    
    private CropRecommendationDashboardResponse.CropPrediction getCropRecommendation(
            SoilDataResponse soilData,
            Double temperature,
            Double humidity,
            Double rainfall,
            Double soilPH) {
        try {
            // Extract NPK values from soil data with null checks
            Double nitrogen = 50.0; // Default value
            if (soilData != null && soilData.getChemicalProperties() != null && 
                soilData.getChemicalProperties().getNitrogenGKg() != null) {
                nitrogen = soilData.getChemicalProperties().getNitrogenGKg() * 100;
            }
            Double phosphorus = 50.0; // Default value
            Double potassium = 50.0; // Default value
            
            log.info("Calling ML service for crop prediction with N={}, P={}, K={}, temp={}, humidity={}, pH={}, rainfall={}", 
                nitrogen, phosphorus, potassium, temperature, humidity, soilPH, rainfall);
            
            // Call ML service for crop recommendation
            Map<String, Object> mlResponse = mlServiceClient.predictCrop(
                nitrogen, phosphorus, potassium,
                temperature, humidity, soilPH, rainfall
            );
            
            if (mlResponse != null) {
                CropRecommendationDashboardResponse.CropPrediction prediction = 
                    new CropRecommendationDashboardResponse.CropPrediction();
                prediction.setPrediction((String) mlResponse.get("prediction"));
                prediction.setConfidence(((Number) mlResponse.get("confidence")).doubleValue());
                prediction.setModelVersion((String) mlResponse.getOrDefault("modelVersion", "1.0.0"));
                prediction.setProbabilities((Map<String, Double>) mlResponse.get("probabilities"));
                log.info("Crop prediction successful: {}", prediction.getPrediction());
                return prediction;
            }
            
            log.warn("ML service returned null response for crop prediction");
            return null;
        } catch (Exception e) {
            log.error("Error getting crop recommendation: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private CropRecommendationDashboardResponse.CropPrediction getCropRotation(
            String previousCrop,
            Double soilPH,
            String soilType,
            Double temperature,
            Double humidity,
            Double rainfall,
            String season) {
        try {
            log.info("Calling ML service for crop rotation with previousCrop={}, soilPH={}, soilType={}, temp={}, humidity={}, rainfall={}, season={}", 
                previousCrop, soilPH, soilType, temperature, humidity, rainfall, season);
            
            // Call ML service for crop rotation prediction
            Map<String, Object> mlResponse = mlServiceClient.predictCropRotation(
                previousCrop, soilPH, soilType,
                temperature, humidity, rainfall, season
            );
            
            if (mlResponse != null) {
                CropRecommendationDashboardResponse.CropPrediction prediction = 
                    new CropRecommendationDashboardResponse.CropPrediction();
                prediction.setPrediction((String) mlResponse.get("prediction"));
                prediction.setConfidence(((Number) mlResponse.get("confidence")).doubleValue());
                prediction.setModelVersion((String) mlResponse.getOrDefault("modelVersion", "1.0.0"));
                prediction.setProbabilities((Map<String, Double>) mlResponse.get("probabilities"));
                log.info("Crop rotation prediction successful: {}", prediction.getPrediction());
                return prediction;
            }
            
            log.warn("ML service returned null response for crop rotation");
            return null;
        } catch (Exception e) {
            log.error("Error getting crop rotation: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private FertilizerRecommendationResponse getFertilizerRecommendation(
            String crop,
            String soilType,
            Double soilPH,
            Double temperature,
            Double humidity,
            Double rainfall,
            String season) {
        try {
            FertilizerRecommendationRequest request = new FertilizerRecommendationRequest();
            request.setCrop(crop);
            request.setSoilType(soilType);
            request.setSoilPH(soilPH);
            request.setTemperature(temperature);
            request.setHumidity(humidity);
            request.setRainfall(rainfall);
            request.setSeason(season != null ? season : "Kharif");
            
            log.info("Calling ML service for fertilizer prediction with crop={}, soilType={}, soilPH={}, temp={}, humidity={}, rainfall={}, season={}", 
                crop, soilType, soilPH, temperature, humidity, rainfall, request.getSeason());
            
            FertilizerRecommendationResponse response = mlServiceClient.predictFertilizer(request);
            if (response != null) {
                log.info("Fertilizer prediction successful: N={}, P={}, K={}", response.getN_dosage(), response.getP_dosage(), response.getK_dosage());
            } else {
                log.warn("ML service returned null response for fertilizer prediction");
            }
            return response;
        } catch (Exception e) {
            log.error("Error getting fertilizer recommendation: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private Double extractTemperature(WeatherDataResponse weatherData) {
        if (weatherData == null || weatherData.getCurrent() == null) {
            return 26.5; // Default
        }
        return weatherData.getCurrent().getTempC() != null ? 
            weatherData.getCurrent().getTempC() : 26.5;
    }
    
    private Double extractHumidity(WeatherDataResponse weatherData) {
        if (weatherData == null || weatherData.getCurrent() == null) {
            return 78.0; // Default
        }
        return weatherData.getCurrent().getHumidity() != null ? 
            weatherData.getCurrent().getHumidity() : 78.0;
    }
    
    private Double extractRainfall(WeatherDataResponse weatherData) {
        if (weatherData == null || weatherData.getForecast() == null || 
            weatherData.getForecast().getForecastday() == null) {
            return 210.0; // Default
        }
        
        return weatherData.getForecast().getForecastday().stream()
            .mapToDouble(fd -> fd.getDay().getTotalPrecipMm() != null ? 
                fd.getDay().getTotalPrecipMm() : 0.0)
            .sum();
    }

    private Double extractSoilPH(SoilDataResponse soilData) {
        if (soilData == null || soilData.getChemicalProperties() == null) {
            return 7.0; // Default neutral pH
        }
        Double ph = soilData.getChemicalProperties().getPhH2o();
        return ph != null ? ph : 7.0;
    }

    private String extractSoilType(SoilDataResponse soilData) {
        if (soilData == null || soilData.getSoilType() == null) {
            return "loamy"; // Default soil type
        }
        String kaegrType = soilData.getSoilType().getTextureClass();
        if (kaegrType == null || kaegrType.trim().isEmpty()) {
            return "loamy";
        }
        // Map Kaegro soil type to ML model soil type
        return SoilTypeMapper.mapKaegrToML(kaegrType);
    }
}
