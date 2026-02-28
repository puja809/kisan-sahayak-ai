package com.farmer.mandi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CropRecommendationDashboardResponse {
    private CropPrediction cropRecommendation;
    private CropPrediction cropRotation;
    private FertilizerRecommendationResponse fertilizerRecommendation;
    private SoilDataResponse soilData;
    private WeatherDataResponse weatherData;
    private String location;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CropPrediction {
        private String prediction;
        private Double confidence;
        private java.util.Map<String, Double> probabilities;
        private String modelVersion;
    }
}
