package com.farmer.weather.controller;

import com.farmer.weather.dto.*;
import com.farmer.weather.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST Controller for Weather Service.
 * Provides endpoints for weather data including forecasts, current weather,
 * alerts, nowcast, rainfall statistics, and agromet advisories.
 */
@RestController
@RequestMapping("/api/v1/weather")
public class WeatherController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherController.class);

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Get 7-day weather forecast for a district.
     * Validates: Requirements 1.1, 1.2
     * 
     * @param district District name
     * @param state State name
     * @return 7-day forecast data
     */
    @GetMapping("/forecast/{district}")
    public Mono<ResponseEntity<SevenDayForecastDto>> getSevenDayForecast(
            @PathVariable String district,
            @RequestParam(required = false, defaultValue = "") String state) {
        logger.info("Request for 7-day forecast: district={}, state={}", district, state);
        return weatherService.getSevenDayForecast(district, state)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get current weather data for a district.
     * Validates: Requirement 1.3
     * 
     * @param district District name
     * @param state State name
     * @return Current weather data
     */
    @GetMapping("/current/{district}")
    public Mono<ResponseEntity<CurrentWeatherDto>> getCurrentWeather(
            @PathVariable String district,
            @RequestParam(required = false, defaultValue = "") String state) {
        logger.info("Request for current weather: district={}, state={}", district, state);
        return weatherService.getCurrentWeather(district, state)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get nowcast data for a district.
     * Validates: Requirement 1.5
     * 
     * @param district District name
     * @param state State name
     * @return Nowcast data
     */
    @GetMapping("/nowcast/{district}")
    public Mono<ResponseEntity<NowcastDto>> getNowcast(
            @PathVariable String district,
            @RequestParam(required = false, defaultValue = "") String state) {
        logger.info("Request for nowcast: district={}, state={}", district, state);
        return weatherService.getNowcast(district, state)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get weather alerts for a district.
     * Validates: Requirement 1.4
     * 
     * @param district District name
     * @param state State name
     * @return Weather alerts
     */
    @GetMapping("/alerts/{district}")
    public Mono<ResponseEntity<WeatherAlertDto>> getWeatherAlerts(
            @PathVariable String district,
            @RequestParam(required = false, defaultValue = "") String state) {
        logger.info("Request for weather alerts: district={}, state={}", district, state);
        return weatherService.getWeatherAlerts(district, state)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get rainfall statistics for a district.
     * Validates: Requirement 1.6
     * 
     * @param district District name
     * @param state State name
     * @return Rainfall statistics
     */
    @GetMapping("/rainfall/{district}")
    public Mono<ResponseEntity<RainfallStatsDto>> getRainfallStats(
            @PathVariable String district,
            @RequestParam(required = false, defaultValue = "") String state) {
        logger.info("Request for rainfall stats: district={}, state={}", district, state);
        return weatherService.getRainfallStats(district, state)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get agromet advisories for a district.
     * Validates: Requirement 1.8
     * 
     * @param district District name
     * @param state State name
     * @return Agromet advisories
     */
    @GetMapping("/agromet/{district}")
    public Mono<ResponseEntity<AgrometAdvisoryDto>> getAgrometAdvisories(
            @PathVariable String district,
            @RequestParam(required = false, defaultValue = "") String state) {
        logger.info("Request for agromet advisories: district={}, state={}", district, state);
        return weatherService.getAgrometAdvisories(district, state)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get AWS/ARG real-time data for a district.
     * Validates: Requirement 1.7
     * 
     * @param district District name
     * @param state State name
     * @return AWS/ARG data
     */
    @GetMapping("/aws-arg/{district}")
    public Mono<ResponseEntity<List<AwsArgDataDto>>> getAwsArgData(
            @PathVariable String district,
            @RequestParam(required = false, defaultValue = "") String state) {
        logger.info("Request for AWS/ARG data: district={}, state={}", district, state);
        return weatherService.getAwsArgData(district, state)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}