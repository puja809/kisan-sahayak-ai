package com.farmer.weather.service;

import com.farmer.weather.client.WeatherApiClient;
import com.farmer.weather.dto.*;
import com.farmer.weather.entity.WeatherCache;
import com.farmer.weather.exception.ImdApiException;
import com.farmer.weather.repository.WeatherCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);

    private final WeatherApiClient weatherApiClient;
    private final WeatherCacheService weatherCacheService;
    private final WeatherCacheRepository weatherCacheRepository;

    public WeatherService(
            WeatherApiClient weatherApiClient,
            WeatherCacheService weatherCacheService,
            WeatherCacheRepository weatherCacheRepository) {
        this.weatherApiClient = weatherApiClient;
        this.weatherCacheService = weatherCacheService;
        this.weatherCacheRepository = weatherCacheRepository;
    }

    public Mono<SevenDayForecastDto> getSevenDayForecast(String district, String state) {
        logger.info("Fetching 7-day forecast for district: {}, state: {}", district, state);
        
        Optional<SevenDayForecastDto> cached = weatherCacheService.getCachedSevenDayForecast(district, state);
        if (cached.isPresent()) {
            SevenDayForecastDto forecast = cached.get();
            forecast.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached 7-day forecast for district: {}", district);
            return Mono.just(forecast);
        }

        return weatherApiClient.getSevenDayForecast(district, state)
            .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                .filter(throwable -> isRetryableError(throwable))
                .doBeforeRetry(retrySignal -> {
                    logger.warn("Retry attempt {} for 7-day forecast operation on district: {}, waiting {} seconds",
                        retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                }))
            .doOnNext(forecast -> {
                weatherCacheService.cacheSevenDayForecast(district, state, forecast);
                logger.info("Cached new 7-day forecast for district: {}", district);
            })
            .onErrorResume(error -> {
                logger.error("Error fetching 7-day forecast: {}", error.getMessage());
                Optional<WeatherCache> staleCache = weatherCacheRepository
                    .findByDistrictAndStateAndForecastType(district, state, "7DAY");
                if (staleCache.isPresent()) {
                    logger.info("Returning stale MySQL-cached 7-day forecast for district: {}", district);
                    SevenDayForecastDto forecast = parseCachedForecast(staleCache.get());
                    forecast.setCacheTimestamp(staleCache.get().getFetchedAt());
                    return Mono.just(forecast);
                }
                return Mono.error(new ImdApiException(
                    "Failed to fetch 7-day forecast and no cached data available", error, (int) MAX_RETRIES));
            });
    }

    public Mono<CurrentWeatherDto> getCurrentWeather(String district, String state) {
        logger.info("Fetching current weather for district: {}, state: {}", district, state);
        
        Optional<CurrentWeatherDto> cached = weatherCacheService.getCachedCurrentWeather(district, state);
        if (cached.isPresent()) {
            CurrentWeatherDto weather = cached.get();
            weather.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached current weather for district: {}", district);
            return Mono.just(weather);
        }

        return weatherApiClient.getCurrentWeather(district, state)
            .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                .filter(throwable -> isRetryableError(throwable))
                .doBeforeRetry(retrySignal -> {
                    logger.warn("Retry attempt {} for current weather operation on district: {}, waiting {} seconds",
                        retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                }))
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
                    "Failed to fetch current weather and no cached data available", error, (int) MAX_RETRIES));
            });
    }

    public Mono<NowcastDto> getNowcast(String district, String state) {
        logger.info("Fetching nowcast for district: {}, state: {}", district, state);
        
        Optional<NowcastDto> cached = weatherCacheService.getCachedNowcast(district, state);
        if (cached.isPresent()) {
            NowcastDto nowcast = cached.get();
            nowcast.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached nowcast for district: {}", district);
            return Mono.just(nowcast);
        }

        return weatherApiClient.getNowcast(district, state)
            .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                .filter(throwable -> isRetryableError(throwable))
                .doBeforeRetry(retrySignal -> {
                    logger.warn("Retry attempt {} for nowcast operation on district: {}, waiting {} seconds",
                        retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                }))
            .doOnNext(nowcast -> {
                weatherCacheService.cacheNowcast(district, state, nowcast);
                logger.info("Cached new nowcast for district: {}", district);
            })
            .onErrorResume(error -> {
                logger.error("Error fetching nowcast: {}", error.getMessage());
                return Mono.empty();
            });
    }

    public Mono<WeatherAlertDto> getWeatherAlerts(String district, String state) {
        logger.info("Fetching weather alerts for district: {}, state: {}", district, state);
        
        Optional<WeatherAlertDto> cached = weatherCacheService.getCachedWeatherAlerts(district, state);
        if (cached.isPresent()) {
            WeatherAlertDto alerts = cached.get();
            alerts.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached weather alerts for district: {}", district);
            return Mono.just(alerts);
        }

        return weatherApiClient.getWeatherAlerts(district, state)
            .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                .filter(throwable -> isRetryableError(throwable))
                .doBeforeRetry(retrySignal -> {
                    logger.warn("Retry attempt {} for weather alerts operation on district: {}, waiting {} seconds",
                        retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                }))
            .doOnNext(alerts -> {
                weatherCacheService.cacheWeatherAlerts(district, state, alerts);
                logger.info("Cached new weather alerts for district: {}", district);
            })
            .onErrorResume(error -> {
                logger.error("Error fetching weather alerts: {}", error.getMessage());
                return Mono.empty();
            });
    }

    public Mono<RainfallStatsDto> getRainfallStats(String district, String state) {
        logger.info("Fetching rainfall stats for district: {}, state: {}", district, state);
        
        Optional<RainfallStatsDto> cached = weatherCacheService.getCachedRainfallStats(district, state);
        if (cached.isPresent()) {
            RainfallStatsDto stats = cached.get();
            stats.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached rainfall stats for district: {}", district);
            return Mono.just(stats);
        }

        return weatherApiClient.getRainfallStats(district, state)
            .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                .filter(throwable -> isRetryableError(throwable))
                .doBeforeRetry(retrySignal -> {
                    logger.warn("Retry attempt {} for rainfall stats operation on district: {}, waiting {} seconds",
                        retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                }))
            .doOnNext(stats -> {
                weatherCacheService.cacheRainfallStats(district, state, stats);
                logger.info("Cached new rainfall stats for district: {}", district);
            })
            .onErrorResume(error -> {
                logger.error("Error fetching rainfall stats: {}", error.getMessage());
                return Mono.empty();
            });
    }

    public Mono<AgrometAdvisoryDto> getAgrometAdvisories(String district, String state) {
        logger.info("Fetching agromet advisories for district: {}, state: {}", district, state);
        
        Optional<AgrometAdvisoryDto> cached = weatherCacheService.getCachedAgrometAdvisories(district, state);
        if (cached.isPresent()) {
            AgrometAdvisoryDto advisories = cached.get();
            advisories.setCacheTimestamp(LocalDateTime.now());
            logger.info("Returning Redis-cached agromet advisories for district: {}", district);
            return Mono.just(advisories);
        }

        return weatherApiClient.getAgrometAdvisories(district, state)
            .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                .filter(throwable -> isRetryableError(throwable))
                .doBeforeRetry(retrySignal -> {
                    logger.warn("Retry attempt {} for agromet advisories operation on district: {}, waiting {} seconds",
                        retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                }))
            .doOnNext(advisories -> {
                weatherCacheService.cacheAgrometAdvisories(district, state, advisories);
                logger.info("Cached new agromet advisories for district: {}", district);
            })
            .onErrorResume(error -> {
                logger.error("Error fetching agromet advisories: {}", error.getMessage());
                return Mono.empty();
            });
    }

    public Mono<List<AwsArgDataDto>> getAwsArgData(String district, String state) {
        logger.info("Fetching AWS/ARG data for district: {}, state: {}", district, state);
        
        return weatherApiClient.getAwsArgData(district, state)
            .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                .filter(throwable -> isRetryableError(throwable))
                .doBeforeRetry(retrySignal -> {
                    logger.warn("Retry attempt {} for AWS/ARG data operation on district: {}, waiting {} seconds",
                        retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                }))
            .onErrorResume(error -> {
                logger.error("Error fetching AWS/ARG data: {}", error.getMessage());
                return Mono.empty();
            });
    }

    private boolean isRetryableError(Throwable error) {
        String message = error.getMessage();
        if (message == null) return false;
        message = message.toLowerCase();
        // Retry on server errors, timeouts, connection issues
        return message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("unavailable") ||
               message.contains("server") ||
               message.contains("rate limit") ||
               message.contains("eof") ||
               message.contains("socket");
    }

    private SevenDayForecastDto parseCachedForecast(WeatherCache cache) {
        // In a real implementation, this would parse the cached JSON data
        // For now, return a basic structure with cache metadata
        return SevenDayForecastDto.builder()
            .district(cache.getDistrict())
            .state(cache.getState())
            .fetchedAt(cache.getFetchedAt())
            .build();
    }

    private CurrentWeatherDto parseCachedCurrentWeather(WeatherCache cache) {
        // In a real implementation, this would parse the cached JSON data
        return CurrentWeatherDto.builder()
            .district(cache.getDistrict())
            .state(cache.getState())
            .observationTime(cache.getFetchedAt())
            .build();
    }
}