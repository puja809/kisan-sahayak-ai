package com.farmer.yield.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for fetching weather data from weather service.
 * 
 * Used for:
 * - Rainfall data
 * - Temperature data
 * - Extreme weather events
 * 
 * Validates: Requirements 11B.3
 */
@Component
public class WeatherClient {

    private final WebClient weatherServiceClient;

    public WeatherClient(WebClient.Builder webClientBuilder) {
        this.weatherServiceClient = webClientBuilder
                .baseUrl("http://weather-service:8080")
                .build();
    }

    /**
     * Get weather data for a location.
     * 
     * @param district District name
     * @param state State name
     * @return Weather data including rainfall, temperature
     */
    public Map<String, Object> getWeatherData(String district, String state) {
        try {
            return weatherServiceClient.get()
                    .uri("/api/v1/weather/current/{district}", district)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            // Return empty data on failure - service will use defaults
            return new HashMap<>();
        }
    }

    /**
     * Get rainfall data for a location.
     * 
     * @param district District name
     * @return Rainfall data
     */
    public Map<String, Object> getRainfallData(String district) {
        try {
            return weatherServiceClient.get()
                    .uri("/api/v1/weather/rainfall/{district}", district)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get weather alerts for a location.
     * 
     * @param district District name
     * @return List of weather alerts
     */
    public Map<String, Object> getWeatherAlerts(String district) {
        try {
            return weatherServiceClient.get()
                    .uri("/api/v1/weather/alerts/{district}", district)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get forecast data for planning.
     * 
     * @param district District name
     * @return 7-day forecast
     */
    public Map<String, Object> getForecast(String district) {
        try {
            return weatherServiceClient.get()
                    .uri("/api/v1/weather/forecast/{district}", district)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}