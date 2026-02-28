package com.farmer.crop.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class WeatherApiClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${weatherapi.api-key}")
    private String apiKey;
    
    private static final String BASE_URL = "https://api.weatherapi.com/v1";
    
    public WeatherApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public WeatherData getCurrentWeather(Double latitude, Double longitude) {
        try {
            String url = String.format("%s/current.json?key=%s&q=%s,%s&aqi=no",
                BASE_URL, apiKey, latitude, longitude);
            
            WeatherApiResponse response = restTemplate.getForObject(url, WeatherApiResponse.class);
            
            if (response != null && response.getCurrent() != null) {
                return mapToWeatherData(response.getCurrent());
            }
            
            throw new RuntimeException("Invalid weather API response");
        } catch (Exception e) {
            log.error("Error fetching weather data for coordinates: {}, {}", latitude, longitude, e);
            throw new RuntimeException("Failed to fetch weather data", e);
        }
    }
    
    private WeatherData mapToWeatherData(Current current) {
        WeatherData data = new WeatherData();
        data.setTemperature(current.getTempC());
        data.setHumidity(Double.valueOf(current.getHumidity()));
        data.setRainfall(current.getPrecipMm());
        return data;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherData {
        private Double temperature;
        private Double humidity;
        private Double rainfall;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherApiResponse {
        private Current current;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Current {
        @JsonProperty("temp_c")
        private Double tempC;
        
        @JsonProperty("humidity")
        private Integer humidity;
        
        @JsonProperty("precip_mm")
        private Double precipMm;
    }
}
