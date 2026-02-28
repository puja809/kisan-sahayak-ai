package com.farmer.mandi.client;

import com.farmer.mandi.dto.WeatherDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherApiClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${weather-api.key:}")
    private String weatherApiKey;
    
    @Value("${weather-api.url:https://api.weatherapi.com/v1}")
    private String weatherApiUrl;
    
    public WeatherDataResponse getCurrentWeather(Double latitude, Double longitude) {
        try {
            if (weatherApiKey == null || weatherApiKey.isEmpty()) {
                log.warn("Weather API key not configured, returning mock data");
                return createMockWeatherData();
            }
            
            String url = String.format(
                "%s/current.json?key=%s&q=%f,%f",
                weatherApiUrl, weatherApiKey, latitude, longitude
            );
            log.info("Fetching weather data from WeatherAPI");
            
            WeatherDataResponse response = restTemplate.getForObject(url, WeatherDataResponse.class);
            log.info("Weather data fetched successfully");
            return response;
        } catch (Exception e) {
            log.error("Error fetching weather data, returning mock data", e);
            return createMockWeatherData();
        }
    }
    
    public WeatherDataResponse getForecastWeather(Double latitude, Double longitude, int days) {
        try {
            if (weatherApiKey == null || weatherApiKey.isEmpty()) {
                log.warn("Weather API key not configured, returning mock data");
                return createMockWeatherData();
            }
            
            String url = String.format(
                "%s/forecast.json?key=%s&q=%f,%f&days=%d",
                weatherApiUrl, weatherApiKey, latitude, longitude, days
            );
            log.info("Fetching forecast weather data from WeatherAPI");
            
            WeatherDataResponse response = restTemplate.getForObject(url, WeatherDataResponse.class);
            log.info("Forecast weather data fetched successfully");
            return response;
        } catch (Exception e) {
            log.error("Error fetching forecast weather data, returning mock data", e);
            return createMockWeatherData();
        }
    }
    
    private WeatherDataResponse createMockWeatherData() {
        WeatherDataResponse response = new WeatherDataResponse();
        
        WeatherDataResponse.CurrentWeather current = new WeatherDataResponse.CurrentWeather();
        current.setTempC(26.5);
        current.setHumidity(78.0);
        current.setPrecipMm(0.0);
        response.setCurrent(current);
        
        return response;
    }
}
