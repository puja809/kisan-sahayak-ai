package com.farmer.weather.client;

import com.farmer.weather.dto.*;
import com.farmer.weather.exception.ImdApiException;
import com.farmer.weather.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IMD (India Meteorological Department) API Client.
 * Provides methods for accessing weather data from IMD APIs including:
 * - 7-day forecast
 * - Current weather
 * - Nowcast
 * - Weather alerts
 * - Rainfall statistics
 * - Agromet advisories
 * - AWS/ARG real-time data
 * 
 * Implements retry logic with exponential backoff (1s, 2s, 4s, max 3 retries)
 * and handles API timeouts and rate limiting.
 */
@Component
public class ImdApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ImdApiClient.class);

    // Retry configuration: exponential backoff with max 3 retries
    private static final Duration[] RETRY_DELAYS = {
        Duration.ofSeconds(1),
        Duration.ofSeconds(2),
        Duration.ofSeconds(4)
    };
    private static final int MAX_RETRIES = 3;

    private final WebClient webClient;
    private final int timeoutSeconds;
    private final int rateLimitRequestsPerMinute;
    
    // Rate limiting: track request timestamps
    private final ConcurrentLinkedQueue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();
    private final AtomicLong rateLimitErrorCount = new AtomicLong(0);

    public ImdApiClient(
            WebClient imdWebClient,
            @Value("${imd.api.timeout-seconds:30}") int timeoutSeconds,
            @Value("${imd.api.rate-limit-requests-per-minute:60}") int rateLimitRequestsPerMinute) {
        this.webClient = imdWebClient;
        this.timeoutSeconds = timeoutSeconds;
        this.rateLimitRequestsPerMinute = rateLimitRequestsPerMinute;
    }

    /**
     * Get 7-day weather forecast for a district.
     * Validates: Requirement 1.1
     * 
     * @param district District name
     * @param state State name
     * @return 7-day forecast data
     */
    public Mono<SevenDayForecastDto> getSevenDayForecast(String district, String state) {
        return executeWithRateLimiting(() -> 
            webClient.get()
                .uri("/weather/forecast/7day/{district}", district)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> 
                    handleClientError(response, "7-day forecast", district))
                .onStatus(HttpStatusCode::is5xxServerError, response -> 
                    handleServerError(response, "7-day forecast", district))
                .bodyToMono(SevenDayForecastDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(createRetrySpec("7-day forecast", district))
                .doOnSuccess(result -> logger.info("Retrieved 7-day forecast for district: {}", district))
                .doOnError(error -> logger.error("Error fetching 7-day forecast for {}: {}", district, error.getMessage()))
        );
    }

    /**
     * Get current weather data for a district.
     * Validates: Requirement 1.3
     * 
     * @param district District name
     * @param state State name
     * @return Current weather data including cloud coverage, 24-hour rainfall, wind vectors
     */
    public Mono<CurrentWeatherDto> getCurrentWeather(String district, String state) {
        return executeWithRateLimiting(() -> 
            webClient.get()
                .uri("/weather/current/{district}", district)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> 
                    handleClientError(response, "current weather", district))
                .onStatus(HttpStatusCode::is5xxServerError, response -> 
                    handleServerError(response, "current weather", district))
                .bodyToMono(CurrentWeatherDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(createRetrySpec("current weather", district))
                .doOnSuccess(result -> logger.info("Retrieved current weather for district: {}", district))
                .doOnError(error -> logger.error("Error fetching current weather for {}: {}", district, error.getMessage()))
        );
    }

    /**
     * Get nowcast data (0-3 hour forecasts) for a district.
     * Validates: Requirement 1.5
     * 
     * @param district District name
     * @param state State name
     * @return Nowcast data including thunderstorms, squalls, and localized precipitation
     */
    public Mono<NowcastDto> getNowcast(String district, String state) {
        return executeWithRateLimiting(() -> 
            webClient.get()
                .uri("/weather/nowcast/{district}", district)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> 
                    handleClientError(response, "nowcast", district))
                .onStatus(HttpStatusCode::is5xxServerError, response -> 
                    handleServerError(response, "nowcast", district))
                .bodyToMono(NowcastDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(createRetrySpec("nowcast", district))
                .doOnSuccess(result -> logger.info("Retrieved nowcast for district: {}", district))
                .doOnError(error -> logger.error("Error fetching nowcast for {}: {}", district, error.getMessage()))
        );
    }

    /**
     * Get weather alerts for a district.
     * Validates: Requirement 1.4
     * 
     * @param district District name
     * @param state State name
     * @return Weather alerts with severity indicators
     */
    public Mono<WeatherAlertDto> getWeatherAlerts(String district, String state) {
        return executeWithRateLimiting(() -> 
            webClient.get()
                .uri("/weather/alerts/{district}", district)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> 
                    handleClientError(response, "weather alerts", district))
                .onStatus(HttpStatusCode::is5xxServerError, response -> 
                    handleServerError(response, "weather alerts", district))
                .bodyToMono(WeatherAlertDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(createRetrySpec("weather alerts", district))
                .doOnSuccess(result -> logger.info("Retrieved weather alerts for district: {}", district))
                .doOnError(error -> logger.error("Error fetching weather alerts for {}: {}", district, error.getMessage()))
        );
    }

    /**
     * Get rainfall statistics for a district.
     * Validates: Requirement 1.6
     * 
     * @param district District name
     * @param state State name
     * @return Rainfall statistics including actual vs normal comparison and percentage departure
     */
    public Mono<RainfallStatsDto> getRainfallStats(String district, String state) {
        return executeWithRateLimiting(() -> 
            webClient.get()
                .uri("/weather/rainfall/{district}", district)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> 
                    handleClientError(response, "rainfall stats", district))
                .onStatus(HttpStatusCode::is5xxServerError, response -> 
                    handleServerError(response, "rainfall stats", district))
                .bodyToMono(RainfallStatsDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(createRetrySpec("rainfall stats", district))
                .doOnSuccess(result -> logger.info("Retrieved rainfall stats for district: {}", district))
                .doOnError(error -> logger.error("Error fetching rainfall stats for {}: {}", district, error.getMessage()))
        );
    }

    /**
     * Get agromet advisories for a district.
     * Validates: Requirement 1.8
     * 
     * @param district District name
     * @param state State name
     * @return Agromet advisories including crop-stage-based weather advisories, heat stress indices, and ET₀
     */
    public Mono<AgrometAdvisoryDto> getAgrometAdvisories(String district, String state) {
        return executeWithRateLimiting(() -> 
            webClient.get()
                .uri("/weather/agromet/{district}", district)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> 
                    handleClientError(response, "agromet advisories", district))
                .onStatus(HttpStatusCode::is5xxServerError, response -> 
                    handleServerError(response, "agromet advisories", district))
                .bodyToMono(AgrometAdvisoryDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(createRetrySpec("agromet advisories", district))
                .doOnSuccess(result -> logger.info("Retrieved agromet advisories for district: {}", district))
                .doOnError(error -> logger.error("Error fetching agromet advisories for {}: {}", district, error.getMessage()))
        );
    }

    /**
     * Get AWS/ARG real-time data for a district.
     * Validates: Requirement 1.7
     * 
     * @param district District name
     * @param state State name
     * @return AWS/ARG data from Automated Weather Stations and Rain Gauges
     */
    @SuppressWarnings("unchecked")
    public Mono<List<AwsArgDataDto>> getAwsArgData(String district, String state) {
        return executeWithRateLimiting(() -> 
            (Mono<List<AwsArgDataDto>>) (Mono<?>) webClient.get()
                .uri("/weather/aws-arg/{district}", district)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> 
                    handleClientError(response, "AWS/ARG data", district))
                .onStatus(HttpStatusCode::is5xxServerError, response -> 
                    handleServerError(response, "AWS/ARG data", district))
                .bodyToMono(List.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(createRetrySpec("AWS/ARG data", district))
                .doOnSuccess(result -> logger.info("Retrieved AWS/ARG data for district: {}", district))
                .doOnError(error -> logger.error("Error fetching AWS/ARG data for {}: {}", district, error.getMessage()))
        );
    }

    /**
     * Execute a request with rate limiting.
     * Checks if the rate limit has been exceeded and waits if necessary.
     * 
     * @param request The request to execute
     * @return The result of the request
     */
    private <T> Mono<T> executeWithRateLimiting(java.util.function.Supplier<Mono<T>> request) {
        return Mono.defer(() -> {
            if (isRateLimitExceeded()) {
                logger.warn("Rate limit exceeded, waiting before retry");
                return Mono.delay(Duration.ofSeconds(60))
                    .flatMap(ignored -> executeWithRateLimiting(request));
            }
            recordRequest();
            return request.get();
        });
    }

    /**
     * Check if the rate limit has been exceeded.
     * 
     * @return true if rate limit is exceeded, false otherwise
     */
    private boolean isRateLimitExceeded() {
        long now = System.currentTimeMillis();
        long windowStart = now - 60000; // 1 minute window

        // Remove old timestamps
        while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < windowStart) {
            requestTimestamps.poll();
        }

        return requestTimestamps.size() >= rateLimitRequestsPerMinute;
    }

    /**
     * Record a request timestamp for rate limiting.
     */
    private void recordRequest() {
        requestTimestamps.add(System.currentTimeMillis());
    }

    /**
     * Create a retry specification with exponential backoff.
     * 
     * @param operationName Name of the operation for logging
     * @param district District name for logging
     * @return Retry specification
     */
    private Retry createRetrySpec(String operationName, String district) {
        return Retry.from(companion -> 
            companion.flatMap(retrySignal -> {
                long attempt = retrySignal.totalRetries() + 1;
                
                if (attempt > MAX_RETRIES) {
                    logger.error("Max retries exceeded for {} operation on district: {}", operationName, district);
                    return Mono.error(new ImdApiException(
                        "Failed to fetch " + operationName + " for district " + district + " after " + MAX_RETRIES + " retries",
                        (int) attempt));
                }

                Duration delay = RETRY_DELAYS[(int) (attempt - 1)];
                logger.warn("Retry attempt {} for {} operation on district {}, waiting {} seconds", 
                    attempt, operationName, district, delay.getSeconds());
                
                return Mono.delay(delay);
            })
        );
    }

    /**
     * Handle 4xx client errors.
     * 
     * @param response The error response
     * @param operationName Name of the operation
     * @param district District name
     * @return Mono error
     */
    private Mono<? extends Throwable> handleClientError(
            org.springframework.web.reactive.function.client.ClientResponse response,
            String operationName, String district) {
        return response.bodyToMono(String.class)
            .flatMap(body -> {
                HttpStatusCode status = response.statusCode();
                if (status.value() == 429) {
                    rateLimitErrorCount.incrementAndGet();
                    logger.warn("Rate limit exceeded for {} operation on district: {}", operationName, district);
                    return Mono.error(new RateLimitExceededException(
                        "Rate limit exceeded for " + operationName + " on district " + district, body));
                }
                logger.error("Client error for {} operation on district {}: {} - {}", 
                    operationName, district, status.value(), body);
                return Mono.error(new ImdApiException(
                    "Client error for " + operationName + " on district " + district + ": " + status.value()));
            });
    }

    /**
     * Handle 5xx server errors.
     * 
     * @param response The error response
     * @param operationName Name of the operation
     * @param district District name
     * @return Mono error
     */
    private Mono<? extends Throwable> handleServerError(
            org.springframework.web.reactive.function.client.ClientResponse response,
            String operationName, String district) {
        return response.bodyToMono(String.class)
            .flatMap(body -> {
                HttpStatusCode status = response.statusCode();
                logger.error("Server error for {} operation on district {}: {} - {}", 
                    operationName, district, status.value(), body);
                return Mono.error(new ImdApiException(
                    "Server error for " + operationName + " on district " + district + ": " + status.value()));
            });
    }

    /**
     * Get the rate limit error count for monitoring.
     * 
     * @return Number of rate limit errors
     */
    public long getRateLimitErrorCount() {
        return rateLimitErrorCount.get();
    }

    /**
     * Reset the rate limit error count.
     */
    public void resetRateLimitErrorCount() {
        rateLimitErrorCount.set(0);
    }
}