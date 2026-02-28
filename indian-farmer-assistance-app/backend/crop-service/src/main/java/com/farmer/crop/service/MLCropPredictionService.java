package com.farmer.crop.service;

import com.farmer.crop.client.KaegroCropSoilApiClient;
import com.farmer.crop.client.MLModelClient;
import com.farmer.crop.client.WeatherApiClient;
import com.farmer.crop.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for ML-based crop predictions.
 * Integrates with ML models for crop recommendation and rotation.
 */
@Slf4j
@Service
public class MLCropPredictionService {
    
    private final MLModelClient mlModelClient;
    private final WeatherApiClient weatherApiClient;
    private final KaegroCropSoilApiClient soilApiClient;
    private final CropRecommendationService cropRecommendationService;
    private final CropRotationService cropRotationService;
    
    public MLCropPredictionService(
            MLModelClient mlModelClient,
            WeatherApiClient weatherApiClient,
            KaegroCropSoilApiClient soilApiClient,
            CropRecommendationService cropRecommendationService,
            CropRotationService cropRotationService) {
        this.mlModelClient = mlModelClient;
        this.weatherApiClient = weatherApiClient;
        this.soilApiClient = soilApiClient;
        this.cropRecommendationService = cropRecommendationService;
        this.cropRotationService = cropRotationService;
    }
    
    /**
     * Get ML-based crop recommendation for a location.
     * Complements the existing CropRecommendationService with ML predictions.
     */
    public MLPredictionResponseDto recommendCropByLocation(
            Double latitude, 
            Double longitude,
            Double nitrogen,
            Double phosphorus,
            Double potassium) {
        
        log.info("ML crop recommendation for location: {}, {}", latitude, longitude);
        
        try {
            // Fetch weather data
            WeatherApiClient.WeatherData weatherData = weatherApiClient.getCurrentWeather(latitude, longitude);
            
            // Fetch soil data
            KaegroCropSoilApiClient.SoilDataResponse soilData = soilApiClient.getSoilData(latitude, longitude);
            
            // Build ML request
            CropRecommendationMLRequestDto request = new CropRecommendationMLRequestDto();
            request.setLatitude(latitude);
            request.setLongitude(longitude);
            request.setTemperature(weatherData.getTemperature());
            request.setHumidity(weatherData.getHumidity());
            request.setRainfall(weatherData.getRainfall());
            request.setSoilPH(soilData.getChemicalProperties().getPhH2o());
            request.setNitrogen(nitrogen);
            request.setPhosphorus(phosphorus);
            request.setPotassium(potassium);
            
            // Get prediction from ML model
            MLPredictionResponseDto mlPrediction = mlModelClient.predictCrop(request);
            log.info("ML prediction: {} with confidence: {}", 
                    mlPrediction.getPrediction(), mlPrediction.getConfidence());
            
            return mlPrediction;
            
        } catch (Exception e) {
            log.error("Error in ML crop recommendation", e);
            throw new RuntimeException("Failed to get ML crop recommendation", e);
        }
    }
    
    /**
     * Get ML-based crop rotation recommendation.
     * Complements the existing CropRotationService with ML predictions.
     */
    public MLPredictionResponseDto recommendCropRotation(
            String previousCrop,
            Double latitude,
            Double longitude,
            String season) {
        
        log.info("ML crop rotation recommendation for previous crop: {}, season: {}", previousCrop, season);
        
        try {
            // Fetch weather data
            WeatherApiClient.WeatherData weatherData = weatherApiClient.getCurrentWeather(latitude, longitude);
            
            // Fetch soil data
            KaegroCropSoilApiClient.SoilDataResponse soilData = soilApiClient.getSoilData(latitude, longitude);
            
            // Build ML request
            CropRotationMLRequestDto request = new CropRotationMLRequestDto();
            request.setPreviousCrop(previousCrop);
            request.setSoilPH(soilData.getChemicalProperties().getPhH2o());
            request.setSoilType(soilData.getSoilType().getTextureClass());
            request.setTemperature(weatherData.getTemperature());
            request.setHumidity(weatherData.getHumidity());
            request.setRainfall(weatherData.getRainfall());
            request.setSeason(season);
            
            // Get prediction from ML model
            MLPredictionResponseDto mlPrediction = mlModelClient.predictCropRotation(request);
            log.info("ML rotation prediction: {} with confidence: {}", 
                    mlPrediction.getPrediction(), mlPrediction.getConfidence());
            
            return mlPrediction;
            
        } catch (Exception e) {
            log.error("Error in ML crop rotation recommendation", e);
            throw new RuntimeException("Failed to get ML crop rotation recommendation", e);
        }
    }
}
