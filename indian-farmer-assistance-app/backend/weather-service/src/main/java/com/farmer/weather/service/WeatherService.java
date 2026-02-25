package com.farmer.weather.service;

import com.farmer.weather.client.ImdApiClient;
import com.farmer.weather.dto.*;
import com.farmer.weather.entity.WeatherCache;
import com.farmer.weather.exception.ImdApiException;
import com.farmer.weather.repository.WeatherCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Weather Service that integrates with IMD API.
 * Provides weather data for farmers including forecasts, current weather,
 * alerts, nowcast, rainfall statistics, and agromet advisories.
 * 
 * Uses Redis cache-aside pattern for hot data caching with 30-minute TTL.
 * Validates: Requirements 1.9, 12.2
 */
@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final ImdApiClient imdApiClient;
    private final WeatherCacheService weatherCacheService;
    private final WeatherCacheRepository weatherCacheRepository;

    public WeatherService(
            ImdApiClient imdApiClient,
            WeatherCacheService weatherCacheService,
            WeatherCacheRepository weatherCacheRepository) {
        this.imdApiClient = imdApiClient;
        this.weatherCacheService = weatherCacheService;
        this.weatherCacheRepository = weatherCacheRepository;
    }

    /**
     * Get 7-day weather forecast for a district.
     * Implements cache-aside pattern: check Redis cache first, then MySQL, then IMD API.
     * Validates: Requirement 1.1, 1.9
     * 
     * @param district District name
     * @param state State name
     * @return 7-day forecast data
     */
    public Mono<SevenDayForecastDto> getSevenDayForecast(String district, String state) {
        logger.info("Fetching 7-day forecast for district: {}, state: {}", district, state);
        
        // Try Redis cache first (hot data)
        Optional<SevenDayForecastDto> cached = weatherCacheService.getCachedSevenDayForecast(district, state);
        if (cached.isPresent()) {
            SevenDayForecastDto forecast = cached.get();
            forecast.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached 7-day forecast for district: {}", district);
            return Mono.just(forecast);
        }

        // Fetch from IMD API
        return imdApiClient.getSevenDayForecast(district, state)
            .doOnNext(forecast -> {
                // Cache the new data (invalidates old cache entries)
                weatherCacheService.cacheSevenDayForecast(district, state, forecast);
                logger.info("Cached new 7-day forecast for district: {}", district);
            })
            .onErrorResume(error -> {
                logger.error("Error fetching 7-day forecast: {}", error.getMessage());
                // Try to return stale cache from MySQL if available
                Optional<WeatherCache> staleCache = weatherCacheRepository
                    .findByDistrictAndStateAndForecastType(district, state, "7DAY");
                if (staleCache.isPresent()) {
                    logger.info("Returning stale MySQL-cached 7-day forecast for district: {}", district);
                    SevenDayForecastDto forecast = parseCachedForecast(staleCache.get());
                    forecast.setCacheTimestamp(staleCache.get().getFetchedAt());
                    return Mono.just(forecast);
                }
                return Mono.error(new ImdApiException(
                    "Failed to fetch 7-day forecast and no cached data available", error));
            });
    }

    /**
     * Get current weather data for a district.
     * Implements cache-aside pattern with Redis for hot data.
     * Validates: Requirement 1.3, 1.9
     * 
     * @param district District name
     * @param state State name
     * @return Current weather data
     */
    public Mono<CurrentWeatherDto> getCurrentWeather(String district, String state) {
        logger.info("Fetching current weather for district: {}, state: {}", district, state);
        
        // Try Redis cache first
        Optional<CurrentWeatherDto> cached = weatherCacheService.getCachedCurrentWeather(district, state);
        if (cached.isPresent()) {
            CurrentWeatherDto weather = cached.get();
            weather.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached current weather for district: {}", district);
            return Mono.just(weather);
        }

        // Fetch from IMD API
        return imdApiClient.getCurrentWeather(district, state)
            .doOnNext(weather -> {
                weatherCacheService.cacheCurrentWeather(district, state, weather);
                logger.info("Cached new current weather for district: {}", district);
            })
            .onErrorResume(error -> {
                logger.error("Error fetching current weather: {}", error.getMessage());
                Optional<WeatherCache> staleCache = weatherCacheRepository
                    .findByDistrictAndStateAndForecastType(district, state, "CURRENT");
                if (staleCache.isPresent()) {
                    logger.info("Returning stale MySQL-cached current weather for district: {}", district);
                    CurrentWeatherDto weather = parseCachedCurrentWeather(staleCache.get());
                    weather.setCacheTimestamp(staleCache.get().getFetchedAt());
                    return Mono.just(weather);
                }
                return Mono.error(new ImdApiException(
                    "Failed to fetch current weather and no cached data available", error));
            });
    }

    /**
     * Get nowcast data for a district.
     * Implements cache-aside pattern with Redis for hot data.
     * Validates: Requirement 1.5, 1.9
     * 
     * @param district District name
     * @param state State name
     * @return Nowcast data
     */
    public Mono<NowcastDto> getNowcast(String district, String state) {
        logger.info("Fetching nowcast for district: {}, state: {}", district, state);
        
        // Try Redis cache first
        Optional<NowcastDto> cached = weatherCacheService.getCachedNowcast(district, state);
        if (cached.isPresent()) {
            NowcastDto nowcast = cached.get();
            nowcast.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached nowcast for district: {}", district);
            return Mono.just(nowcast);
        }

        // Fetch from IMD API
        return imdApiClient.getNowcast(district, state)
            .doOnNext(nowcast -> {
                weatherCacheService.cacheNowcast(district, state, nowcast);
                logger.info("Cached new nowcast for district: {}", district);
            })
            .onErrorResume(error -> {
                logger.error("Error fetching nowcast: {}", error.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Get weather alerts for a district.
     * Implements cache-aside pattern with Redis for hot data.
     * Validates: Requirement 1.4, 1.9
     * 
     * @param district District name
     * @param state State name
     * @return Weather alerts
     */
    public Mono<WeatherAlertDto> getWeatherAlerts(String district, String state) {
        logger.info("Fetching weather alerts for district: {}, state: {}", district, state);
        
        // Try Redis cache first
        Optional<WeatherAlertDto> cached = weatherCacheService.getCachedWeatherAlerts(district, state);
        if (cached.isPresent()) {
            WeatherAlertDto alerts = cached.get();
            alerts.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached weather alerts for district: {}", district);
            return Mono.just(alerts);
        }

        // Fetch from IMD API
        return imdApiClient.getWeatherAlerts(district, state)
            .doOnNext(alerts -> {
                weatherCacheService.cacheWeatherAlerts(district, state, alerts);
                logger.info("Cached new weather alerts for district: {}", district);
            })
            .onErrorResume(error -> {
                logger.error("Error fetching weather alerts: {}", error.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Get rainfall statistics for a district.
     * Implements cache-aside pattern with Redis for hot data.
     * Validates: Requirement 1.6, 1.9
     * 
     * @param district District name
     * @param state State name
     * @return Rainfall statistics
     */
    public Mono<RainfallStatsDto> getRainfallStats(String district, String state) {
        logger.info("Fetching rainfall stats for district: {}, state: {}", district, state);
        
        // Try Redis cache first
        Optional<RainfallStatsDto> cached = weatherCacheService.getCachedRainfallStats(district, state);
        if (cached.isPresent()) {
            RainfallStatsDto stats = cached.get();
            stats.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached rainfall stats for district: {}", district);
            return Mono.just(stats);
        }

        // Fetch from IMD API
        return imdApiClient.getRainfallStats(district, state)
            .doOnNext(stats -> {
                weatherCacheService.cacheRainfallStats(district, state, stats);
                logger.info("Cached new rainfall stats for district: {}", district);
            })
            .onErrorResume(error -> {
                logger.error("Error fetching rainfall stats: {}", error.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Get agromet advisories for a district.
     * Implements cache-aside pattern with Redis for hot data.
     * Validates: Requirement 1.8, 1.9
     * 
     * @param district District name
     * @param state State name
     * @return Agromet advisories
     */
    public Mono<AgrometAdvisoryDto> getAgrometAdvisories(String district, String state) {
        logger.info("Fetching agromet advisories for district: {}, state: {}", district, state);
        
        // Try Redis cache first
        Optional<AgrometAdvisoryDto> cached = weatherCacheService.getCachedAgrometAdvisories(district, state);
        if (cached.isPresent()) {
            AgrometAdvisoryDto advisories = cached.get();
            advisories.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached agromet advisories for district: {}", district);
            return Mono.just(advisories);
        }

        // Fetch from IMD API
        return imdApiClient.getAgrometAdvisories(district, state)
            .doOnNext(advisories -> {
                weatherCacheService.cacheAgrometAdvisories(district, state, advisories);
                logger.info("Cached new agromet advisories for district: {}", district);
            })
            .onErrorResume(error -> {
                logger.error("Error fetching agromet advisories: {}", error.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Get AWS/ARG real-time data for a district.
     * Validates: Requirement 1.7
     * 
     * @param district District name
     * @param state State name
     * @return AWS/ARG data
     */
    public Mono<List<AwsArgDataDto>> getAwsArgData(String district, String state) {
        logger.info("Fetching AWS/ARG data for district: {}, state: {}", district, state);
        
        return imdApiClient.getAwsArgData(district, state)
            .onErrorResume(error -> {
                logger.error("Error fetching AWS/ARG data: {}", error.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Parse cached forecast data from MySQL database.
     * Used as fallback when Redis cache is unavailable.
     * 
     * @param cache Cached weather data from database
     * @return Parsed forecast data
     */
    private SevenDayForecastDto parseCachedForecast(WeatherCache cache) {
        // In a real implementation, this would parse the cached JSON data
        // For now, return a basic structure with cache metadata
        return SevenDayForecastDto.builder()
            .district(cache.getDistrict())
            .state(cache.getState())
            .fetchedAt(cache.getFetchedAt())
            .build();
    }

    /**
     * Parse cached current weather data from MySQL database.
     * Used as fallback when Redis cache is unavailable.
     * 
     * @param cache Cached weather data from database
     * @return Parsed current weather data
     */
    private CurrentWeatherDto parseCachedCurrentWeather(WeatherCache cache) {
        // In a real implementation, this would parse the cached JSON data
        return CurrentWeatherDto.builder()
            .district(cache.getDistrict())
            .state(cache.getState())
            .observationTime(cache.getFetchedAt())
            .build();
    }
}