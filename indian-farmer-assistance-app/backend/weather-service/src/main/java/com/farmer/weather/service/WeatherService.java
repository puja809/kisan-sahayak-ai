package com.farmer.weather.service;

import com.farmer.weather.client.WeatherApiClient;
import com.farmer.weather.dto.*;
import com.farmer.weather.exception.ImdApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);

    private final WeatherApiClient weatherApiClient;

    public WeatherService(WeatherApiClient weatherApiClient) {
        this.weatherApiClient = weatherApiClient;
    }

    public Mono<SevenDayForecastDto> getSevenDayForecast(String district, String state) {
        logger.info("Fetching 7-day forecast for district: {}, state: {}", district, state);

        return weatherApiClient.getSevenDayForecast(district, state)
                .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            logger.warn(
                                    "Retry attempt {} for 7-day forecast operation on district: {}, waiting {} seconds",
                                    retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                        }))
                .onErrorResume(error -> {
                    logger.error("Error fetching 7-day forecast: {}", error.getMessage());
                    return Mono.error(new ImdApiException(
                            "Failed to fetch 7-day forecast", error, (int) MAX_RETRIES));
                });
    }

    public Mono<CurrentWeatherDto> getCurrentWeather(String district, String state) {
        logger.info("Fetching current weather for district: {}, state: {}", district, state);

        return weatherApiClient.getCurrentWeather(district, state)
                .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            logger.warn(
                                    "Retry attempt {} for current weather operation on district: {}, waiting {} seconds",
                                    retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                        }))
                .onErrorResume(error -> {
                    logger.error("Error fetching current weather: {}", error.getMessage());
                    return Mono.error(new ImdApiException(
                            "Failed to fetch current weather", error, (int) MAX_RETRIES));
                });
    }

    public Mono<NowcastDto> getNowcast(String district, String state) {
        logger.info("Fetching nowcast for district: {}, state: {}", district, state);

        return weatherApiClient.getNowcast(district, state)
                .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            logger.warn("Retry attempt {} for nowcast operation on district: {}, waiting {} seconds",
                                    retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                        }))
                .onErrorResume(error -> {
                    logger.error("Error fetching nowcast: {}", error.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<WeatherAlertDto> getWeatherAlerts(String district, String state) {
        logger.info("Fetching weather alerts for district: {}, state: {}", district, state);

        return weatherApiClient.getWeatherAlerts(district, state)
                .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            logger.warn(
                                    "Retry attempt {} for weather alerts operation on district: {}, waiting {} seconds",
                                    retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                        }))
                .onErrorResume(error -> {
                    logger.error("Error fetching weather alerts: {}", error.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<RainfallStatsDto> getRainfallStats(String district, String state) {
        logger.info("Fetching rainfall stats for district: {}, state: {}", district, state);

        return weatherApiClient.getRainfallStats(district, state)
                .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            logger.warn(
                                    "Retry attempt {} for rainfall stats operation on district: {}, waiting {} seconds",
                                    retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                        }))
                .onErrorResume(error -> {
                    logger.error("Error fetching rainfall stats: {}", error.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<AgrometAdvisoryDto> getAgrometAdvisories(String district, String state) {
        logger.info("Fetching agromet advisories for district: {}, state: {}", district, state);

        return weatherApiClient.getAgrometAdvisories(district, state)
                .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            logger.warn(
                                    "Retry attempt {} for agromet advisories operation on district: {}, waiting {} seconds",
                                    retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                        }))
                .onErrorResume(error -> {
                    logger.error("Error fetching agromet advisories: {}", error.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<List<AwsArgDataDto>> getAwsArgData(String district, String state) {
        logger.info("Fetching AWS/ARG data for district: {}, state: {}", district, state);

        return weatherApiClient.getAwsArgData(district, state)
                .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            logger.warn(
                                    "Retry attempt {} for AWS/ARG data operation on district: {}, waiting {} seconds",
                                    retrySignal.totalRetries() + 1, district, Math.pow(2, retrySignal.totalRetries()));
                        }))
                .onErrorResume(error -> {
                    logger.error("Error fetching AWS/ARG data: {}", error.getMessage());
                    return Mono.empty();
                });
    }

    private boolean isRetryableError(Throwable error) {
        String message = error.getMessage();
        if (message == null)
            return false;
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
}