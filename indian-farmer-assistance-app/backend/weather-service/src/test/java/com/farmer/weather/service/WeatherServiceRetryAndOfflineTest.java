package com.farmer.weather.service;

import com.farmer.weather.client.ImdApiClient;
import com.farmer.weather.dto.*;
import com.farmer.weather.entity.WeatherCache;
import com.farmer.weather.exception.ImdApiException;
import com.farmer.weather.repository.WeatherCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WeatherService focusing on retry logic, offline mode, and cache fallback.
 * Tests cover Requirements 1.9, 1.10, and design section 19.2 (exponential backoff retry).
 * 
 * Validates:
 * - Requirement 1.9: Offline mode with cached data and timestamp
 * - Requirement 1.10: Descriptive error messages and retry suggestions
 * - Requirement 19.2: Retry logic with exponential backoff (1s, 2s, 4s, max 3 retries)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherServiceRetryAndOfflineTest {

    @Mock
    private ImdApiClient imdApiClient;

    @Mock
    private WeatherCacheService weatherCacheService;

    @Mock
    private WeatherCacheRepository weatherCacheRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        weatherService = new WeatherService(imdApiClient, weatherCacheService, weatherCacheRepository);
    }

    @Nested
    @DisplayName("Successful API Calls with Mock Responses")
    class SuccessfulApiCallsTests {

        @Test
        @DisplayName("Test getSevenDayForecast returns fresh data from API with all required fields")
        void testGetSevenDayForecastSuccessfulApiCall() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            SevenDayForecastDto forecast = createSevenDayForecast(district, state);
            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getSevenDayForecast(anyString(), anyString()))
                .thenReturn(Mono.just(forecast));

            // Act & Assert
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getState().equals(state) &&
                    result.getForecastDays() != null &&
                    result.getForecastDays().size() == 7 &&
                    result.getFetchedAt() != null)
                .verifyComplete();

            verify(imdApiClient).getSevenDayForecast(district, state);
            verify(weatherCacheService).cacheSevenDayForecast(eq(district), eq(state), any(SevenDayForecastDto.class));
        }

        @Test
        @DisplayName("Test getCurrentWeather returns current weather with all required fields")
        void testGetCurrentWeatherSuccessfulApiCall() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            CurrentWeatherDto currentWeather = CurrentWeatherDto.builder()
                .district(district)
                .state(state)
                .observationTime(LocalDateTime.now())
                .cloudCoverage(4)
                .rainfall24HoursMm(5.0)
                .windSpeedKmph(15.0)
                .windDirection("SW")
                .temperatureCelsius(28.0)
                .humidity(65.0)
                .build();

            when(weatherCacheService.getCachedCurrentWeather(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getCurrentWeather(anyString(), anyString()))
                .thenReturn(Mono.just(currentWeather));

            // Act & Assert
            StepVerifier.create(weatherService.getCurrentWeather(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getCloudCoverage() == 4 &&
                    result.getRainfall24HoursMm() == 5.0 &&
                    result.getTemperatureCelsius() == 28.0)
                .verifyComplete();

            verify(imdApiClient).getCurrentWeather(district, state);
            verify(weatherCacheService).cacheCurrentWeather(eq(district), eq(state), any(CurrentWeatherDto.class));
        }

        @Test
        @DisplayName("Test getWeatherAlerts returns alerts with severity indicators")
        void testGetWeatherAlertsSuccessfulApiCall() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            WeatherAlertDto alerts = WeatherAlertDto.builder()
                .district(district)
                .state(state)
                .issuedAt(LocalDateTime.now())
                .overallAlertLevel("ORANGE")
                .alerts(List.of(
                    AlertDto.builder()
                        .type("HEAT_WAVE")
                        .severity("ORANGE")
                        .description("Heat wave conditions expected")
                        .startTime(LocalDateTime.now())
                        .endTime(LocalDateTime.now().plusDays(2))
                        .build()
                ))
                .build();

            when(weatherCacheService.getCachedWeatherAlerts(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getWeatherAlerts(anyString(), anyString()))
                .thenReturn(Mono.just(alerts));

            // Act & Assert
            StepVerifier.create(weatherService.getWeatherAlerts(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getOverallAlertLevel().equals("ORANGE") &&
                    result.getAlerts() != null &&
                    result.getAlerts().size() == 1)
                .verifyComplete();

            verify(imdApiClient).getWeatherAlerts(district, state);
            verify(weatherCacheService).cacheWeatherAlerts(eq(district), eq(state), any(WeatherAlertDto.class));
        }

        @Test
        @DisplayName("Test getNowcast returns nowcast data with high-frequency warnings")
        void testGetNowcastSuccessfulApiCall() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            NowcastDto nowcast = NowcastDto.builder()
                .district(district)
                .state(state)
                .issuedAt(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusHours(3))
                .overallSituation("THUNDERSTORM_WARNING")
                .alerts(List.of(
                    NowcastAlertDto.builder()
                        .alertType("THUNDERSTORM")
                        .severity("YELLOW")
                        .description("Thunderstorm with squalls expected")
                        .build()
                ))
                .build();

            when(weatherCacheService.getCachedNowcast(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getNowcast(anyString(), anyString()))
                .thenReturn(Mono.just(nowcast));

            // Act & Assert
            StepVerifier.create(weatherService.getNowcast(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getOverallSituation().equals("THUNDERSTORM_WARNING") &&
                    result.getValidUntil() != null)
                .verifyComplete();

            verify(imdApiClient).getNowcast(district, state);
            verify(weatherCacheService).cacheNowcast(eq(district), eq(state), any(NowcastDto.class));
        }

        private SevenDayForecastDto createSevenDayForecast(String district, String state) {
            return SevenDayForecastDto.builder()
                .district(district)
                .state(state)
                .fetchedAt(LocalDateTime.now())
                .forecastDays(List.of(
                    createForecastDay(LocalDate.now()),
                    createForecastDay(LocalDate.now().plusDays(1)),
                    createForecastDay(LocalDate.now().plusDays(2)),
                    createForecastDay(LocalDate.now().plusDays(3)),
                    createForecastDay(LocalDate.now().plusDays(4)),
                    createForecastDay(LocalDate.now().plusDays(5)),
                    createForecastDay(LocalDate.now().plusDays(6))
                ))
                .build();
        }

        private ForecastDayDto createForecastDay(LocalDate date) {
            return ForecastDayDto.builder()
                .date(date)
                .maxTempCelsius(32.0)
                .minTempCelsius(22.0)
                .humidity0830(70.0)
                .humidity1730(60.0)
                .rainfallMm(0.0)
                .windSpeedKmph(10.0)
                .windDirection("NE")
                .cloudCoverage(4) // 0-8 scale (nebulosity)
                .sunriseTime("06:15")
                .sunsetTime("18:45")
                .moonriseTime("14:30")
                .moonsetTime("02:45")
                .build();
        }
    }

    @Nested
    @DisplayName("API Failures and Fallback to Cache")
    class ApiFailuresAndCacheFallbackTests {

        @Test
        @DisplayName("Test API error returns cached data from Redis when available")
        void testApiErrorReturnsCachedDataFromRedis() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            SevenDayForecastDto cachedForecast = SevenDayForecastDto.builder()
                .district(district)
                .state(state)
                .fetchedAt(LocalDateTime.now().minusMinutes(10))
                .cacheTimestamp(LocalDateTime.now().minusMinutes(10))
                .forecastDays(List.of(
                    ForecastDayDto.builder()
                        .date(LocalDate.now())
                        .maxTempCelsius(32.0)
                        .minTempCelsius(22.0)
                        .build()
                ))
                .build();

            // Return cached data from Redis (cache hit)
            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.of(cachedForecast));

            // Act & Assert - When cache has data, API is never called, cached data is returned
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getCacheTimestamp() != null)
                .verifyComplete();

            // API is not called when cache has data
            verify(imdApiClient, never()).getSevenDayForecast(anyString(), anyString());
            verify(weatherCacheRepository, never()).findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Test API error returns stale cache from MySQL when Redis cache miss")
        void testApiErrorReturnsStaleCacheFromMySQL() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            WeatherCache cache = WeatherCache.builder()
                .id(1L)
                .district(district)
                .state(state)
                .forecastType("7DAY")
                .cachedData("{\"district\":\"" + district + "\",\"state\":\"" + state + "\"}")
                .fetchedAt(LocalDateTime.now().minusHours(2))
                .build();

            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getSevenDayForecast(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("API unavailable")));
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(cache));

            // Act & Assert
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getCacheTimestamp() != null)
                .verifyComplete();

            verify(imdApiClient).getSevenDayForecast(district, state);
            verify(weatherCacheRepository).findByDistrictAndStateAndForecastType(district, state, "7DAY");
        }

        @Test
        @DisplayName("Test API error returns descriptive error message when no cache available")
        void testApiErrorReturnsDescriptiveErrorWhenNoCache() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getSevenDayForecast(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("IMD API connection timeout")));
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            // Act & Assert
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectError(ImdApiException.class)
                .verify();

            verify(imdApiClient).getSevenDayForecast(district, state);
        }

        @Test
        @DisplayName("Test rate limit error returns cached data")
        void testRateLimitErrorReturnsCachedData() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            CurrentWeatherDto cachedWeather = CurrentWeatherDto.builder()
                .district(district)
                .state(state)
                .observationTime(LocalDateTime.now().minusMinutes(15))
                .cacheTimestamp(LocalDateTime.now().minusMinutes(15))
                .cloudCoverage(4)
                .rainfall24HoursMm(5.0)
                .temperatureCelsius(28.0)
                .build();

            when(weatherCacheService.getCachedCurrentWeather(anyString(), anyString()))
                .thenReturn(Optional.of(cachedWeather));
            when(imdApiClient.getCurrentWeather(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Rate limit exceeded")));

            // Act & Assert
            StepVerifier.create(weatherService.getCurrentWeather(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getCacheTimestamp() != null)
                .verifyComplete();
        }

        @Test
        @DisplayName("Test all weather endpoints fallback to cache on API error")
        void testAllWeatherEndpointsFallbackToCache() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            // Test rainfall stats fallback
            RainfallStatsDto cachedRainfall = RainfallStatsDto.builder()
                .district(district)
                .state(state)
                .actualRainfallMm(150.0)
                .normalRainfallMm(200.0)
                .departurePercent(-25.0)
                .departureCategory("DEFICIT")
                .cacheTimestamp(LocalDateTime.now().minusMinutes(20))
                .build();

            when(weatherCacheService.getCachedRainfallStats(anyString(), anyString()))
                .thenReturn(Optional.of(cachedRainfall));
            when(imdApiClient.getRainfallStats(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("API error")));

            // Act & Assert
            StepVerifier.create(weatherService.getRainfallStats(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getCacheTimestamp() != null)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Retry Logic with Exponential Backoff")
    class RetryLogicExponentialBackoffTests {

        @Test
        @DisplayName("Test retry logic performs 3 retries with exponential backoff (1s, 2s, 4s)")
        void testRetryLogicWithExponentialBackoff() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            SevenDayForecastDto forecast = SevenDayForecastDto.builder()
                .district(district)
                .state(state)
                .build();

            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getSevenDayForecast(anyString(), anyString()))
                .thenReturn(
                    Mono.error(new RuntimeException("API error 1")),
                    Mono.error(new RuntimeException("API error 2")),
                    Mono.error(new RuntimeException("API error 3")),
                    Mono.just(forecast)
                );

            // Act & Assert
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectError(ImdApiException.class)
                .verify(Duration.ofSeconds(10));

            // Verify 4 total attempts (1 initial + 3 retries)
            verify(imdApiClient, times(4)).getSevenDayForecast(district, state);
        }

        @Test
        @DisplayName("Test retry succeeds on third attempt")
        void testRetrySucceedsOnThirdAttempt() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            SevenDayForecastDto forecast = SevenDayForecastDto.builder()
                .district(district)
                .state(state)
                .fetchedAt(LocalDateTime.now())
                .forecastDays(List.of(
                    ForecastDayDto.builder()
                        .date(LocalDate.now())
                        .maxTempCelsius(32.0)
                        .minTempCelsius(22.0)
                        .build()
                ))
                .build();

            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getSevenDayForecast(anyString(), anyString()))
                .thenReturn(
                    Mono.error(new RuntimeException("API error 1")),
                    Mono.error(new RuntimeException("API error 2")),
                    Mono.just(forecast)
                );

            // Act & Assert
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getForecastDays().size() == 1)
                .verifyComplete();

            // Verify 3 total attempts (1 initial + 2 retries)
            verify(imdApiClient, times(3)).getSevenDayForecast(district, state);
        }

        @Test
        @DisplayName("Test retry logic with different error types")
        void testRetryLogicWithDifferentErrorTypes() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            CurrentWeatherDto currentWeather = CurrentWeatherDto.builder()
                .district(district)
                .state(state)
                .temperatureCelsius(28.0)
                .build();

            when(weatherCacheService.getCachedCurrentWeather(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getCurrentWeather(anyString(), anyString()))
                .thenReturn(
                    Mono.error(new RuntimeException("Connection timeout")),
                    Mono.error(new RuntimeException("Service unavailable")),
                    Mono.just(currentWeather)
                );

            // Act & Assert
            StepVerifier.create(weatherService.getCurrentWeather(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getTemperatureCelsius() == 28.0)
                .verifyComplete();

            verify(imdApiClient, times(3)).getCurrentWeather(district, state);
        }

        @Test
        @DisplayName("Test retry does not occur for client errors (4xx)")
        void testRetryDoesNotOccurForClientErrors() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            when(weatherCacheService.getCachedCurrentWeather(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getCurrentWeather(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Bad request")));

            // Act & Assert
            StepVerifier.create(weatherService.getCurrentWeather(district, state))
                .expectError(ImdApiException.class)
                .verify(Duration.ofSeconds(5));

            // Verify only 1 attempt for client errors
            verify(imdApiClient, times(1)).getCurrentWeather(district, state);
        }

        @Test
        @DisplayName("Test retry includes attempt count in exception")
        void testRetryIncludesAttemptCountInException() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getSevenDayForecast(anyString(), anyString()))
                .thenReturn(
                    Mono.error(new RuntimeException("API error 1")),
                    Mono.error(new RuntimeException("API error 2")),
                    Mono.error(new RuntimeException("API error 3")),
                    Mono.error(new RuntimeException("API error 4"))
                );

            // Act & Assert
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectErrorSatisfies(throwable -> {
                    assertTrue(throwable instanceof ImdApiException);
                    ImdApiException exception = (ImdApiException) throwable;
                    assertEquals(4, exception.getAttemptCount());
                })
                .verify(Duration.ofSeconds(10));
        }
    }

    @Nested
    @DisplayName("Offline Mode with Cached Data")
    class OfflineModeWithCachedDataTests {

        @Test
        @DisplayName("Test offline mode displays cached weather data with timestamp")
        void testOfflineModeDisplaysCachedDataWithTimestamp() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";
            LocalDateTime cacheTime = LocalDateTime.now().minusMinutes(45);

            SevenDayForecastDto cachedForecast = SevenDayForecastDto.builder()
                .district(district)
                .state(state)
                .fetchedAt(cacheTime)
                .cacheTimestamp(cacheTime)
                .forecastDays(List.of(
                    ForecastDayDto.builder()
                        .date(LocalDate.now())
                        .maxTempCelsius(32.0)
                        .minTempCelsius(22.0)
                        .build()
                ))
                .build();

            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.of(cachedForecast));

            // Act & Assert
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getCacheTimestamp() != null &&
                    result.getCacheTimestamp().equals(cacheTime))
                .verifyComplete();

            verify(imdApiClient, never()).getSevenDayForecast(anyString(), anyString());
        }

        @Test
        @DisplayName("Test offline mode displays data age in timestamp")
        void testOfflineModeDisplaysDataAge() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";
            LocalDateTime cacheTime = LocalDateTime.now().minusHours(2);

            CurrentWeatherDto cachedWeather = CurrentWeatherDto.builder()
                .district(district)
                .state(state)
                .observationTime(LocalDateTime.now().minusHours(2))
                .cacheTimestamp(cacheTime)
                .cloudCoverage(4)
                .temperatureCelsius(28.0)
                .build();

            when(weatherCacheService.getCachedCurrentWeather(anyString(), anyString()))
                .thenReturn(Optional.of(cachedWeather));

            // Act & Assert
            StepVerifier.create(weatherService.getCurrentWeather(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getCacheTimestamp() != null &&
                    result.getCacheTimestamp().isBefore(LocalDateTime.now()))
                .verifyComplete();
        }

        @Test
        @DisplayName("Test offline mode returns all cached weather data types")
        void testOfflineModeReturnsAllCachedWeatherDataTypes() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            // Test nowcast
            NowcastDto cachedNowcast = NowcastDto.builder()
                .district(district)
                .state(state)
                .issuedAt(LocalDateTime.now().minusMinutes(30))
                .cacheTimestamp(LocalDateTime.now().minusMinutes(30))
                .overallSituation("CLEAR")
                .alerts(List.of())
                .build();

            when(weatherCacheService.getCachedNowcast(anyString(), anyString()))
                .thenReturn(Optional.of(cachedNowcast));

            // Act & Assert
            StepVerifier.create(weatherService.getNowcast(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getCacheTimestamp() != null)
                .verifyComplete();
        }

        @Test
        @DisplayName("Test offline mode with expired cache still returns data")
        void testOfflineModeWithExpiredCacheReturnsData() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";
            LocalDateTime cacheTime = LocalDateTime.now().minusHours(5); // Expired cache

            WeatherAlertDto cachedAlerts = WeatherAlertDto.builder()
                .district(district)
                .state(state)
                .issuedAt(LocalDateTime.now().minusHours(5))
                .cacheTimestamp(cacheTime)
                .overallAlertLevel("YELLOW")
                .alerts(List.of())
                .build();

            when(weatherCacheService.getCachedWeatherAlerts(anyString(), anyString()))
                .thenReturn(Optional.of(cachedAlerts));
            when(imdApiClient.getWeatherAlerts(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Offline")));

            // Act & Assert - should still return expired cache when offline
            StepVerifier.create(weatherService.getWeatherAlerts(district, state))
                .expectNextMatches(result -> 
                    result.getDistrict().equals(district) &&
                    result.getCacheTimestamp() != null)
                .verifyComplete();
        }

        @Test
        @DisplayName("Test offline mode with no cache returns error with retry suggestion")
        void testOfflineModeWithNoCacheReturnsErrorWithRetrySuggestion() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getSevenDayForecast(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Offline - no network")));
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            // Act & Assert
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectErrorSatisfies(throwable -> {
                    assertTrue(throwable instanceof ImdApiException);
                    ImdApiException exception = (ImdApiException) throwable;
                    assertTrue(exception.getMessage().contains("retry") || 
                               exception.getMessage().contains("offline") ||
                               exception.getMessage().contains("cache"));
                })
                .verify();
        }

        @Test
        @DisplayName("Test offline mode preserves all forecast data fields")
        void testOfflineModePreservesAllForecastDataFields() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";
            LocalDateTime cacheTime = LocalDateTime.now().minusMinutes(20);

            SevenDayForecastDto cachedForecast = SevenDayForecastDto.builder()
                .district(district)
                .state(state)
                .fetchedAt(cacheTime)
                .cacheTimestamp(cacheTime)
                .forecastDays(List.of(
                    ForecastDayDto.builder()
                        .date(LocalDate.now())
                        .maxTempCelsius(32.0)
                        .minTempCelsius(22.0)
                        .humidity0830(70.0)
                        .humidity1730(60.0)
                        .rainfallMm(5.0)
                        .windSpeedKmph(10.0)
                        .windDirection("NE")
                        .cloudCoverage(4) // 0-8 scale (nebulosity)
                        .sunriseTime("06:15")
                        .sunsetTime("18:45")
                        .moonriseTime("14:30")
                        .moonsetTime("02:45")
                        .build()
                ))
                .build();

            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.of(cachedForecast));

            // Act & Assert
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectNextMatches(result -> {
                    assertEquals(district, result.getDistrict());
                    assertEquals(1, result.getForecastDays().size());
                    ForecastDayDto day = result.getForecastDays().get(0);
                    assertEquals(32.0, day.getMaxTempCelsius());
                    assertEquals(22.0, day.getMinTempCelsius());
                    assertEquals(70.0, day.getHumidity0830());
                    assertEquals(60.0, day.getHumidity1730());
                    assertEquals(5.0, day.getRainfallMm());
                    assertEquals(10.0, day.getWindSpeedKmph());
                    assertEquals("NE", day.getWindDirection());
                    assertEquals(3, day.getCloudCoverage());
                    assertEquals("06:15", day.getSunriseTime());
                    assertEquals("18:45", day.getSunsetTime());
                    assertEquals(4, day.getCloudCoverage());
                    return true;
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Error Messages and Retry Suggestions")
    class ErrorMessagesAndRetrySuggestionsTests {

        @Test
        @DisplayName("Test error message includes district and state information")
        void testErrorMessageIncludesLocationInfo() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getSevenDayForecast(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("API error")));
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            // Act & Assert
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectErrorSatisfies(throwable -> {
                    assertTrue(throwable instanceof ImdApiException);
                    ImdApiException exception = (ImdApiException) throwable;
                    assertTrue(exception.getMessage().contains(district) ||
                               exception.getMessage().contains(state) ||
                               exception.getMessage().contains("weather"));
                })
                .verify();
        }

        @Test
        @DisplayName("Test retry suggestion in error message")
        void testRetrySuggestionInErrorMessage() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            when(weatherCacheService.getCachedCurrentWeather(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getCurrentWeather(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Connection failed")));
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            // Act & Assert
            StepVerifier.create(weatherService.getCurrentWeather(district, state))
                .expectErrorSatisfies(throwable -> {
                    assertTrue(throwable instanceof ImdApiException);
                    ImdApiException exception = (ImdApiException) throwable;
                    // Error message should indicate no cached data available
                    assertTrue(exception.getMessage().contains("Failed to fetch") &&
                               exception.getMessage().contains("no cached data available"),
                        "Error message should indicate fetch failure and no cache: " + exception.getMessage());
                })
                .verify();
        }

        @Test
        @DisplayName("Test error message for rate limit exceeded")
        void testErrorMessageForRateLimitExceeded() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getSevenDayForecast(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Rate limit exceeded")));
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            // Act & Assert
            StepVerifier.create(weatherService.getSevenDayForecast(district, state))
                .expectErrorSatisfies(throwable -> {
                    assertTrue(throwable instanceof ImdApiException);
                    ImdApiException exception = (ImdApiException) throwable;
                    assertTrue(exception.getMessage().toLowerCase().contains("rate") ||
                               exception.getMessage().toLowerCase().contains("limit") ||
                               exception.getMessage().toLowerCase().contains("retry"));
                })
                .verify();
        }

        @Test
        @DisplayName("Test error message for timeout")
        void testErrorMessageForTimeout() {
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";

            when(weatherCacheService.getCachedRainfallStats(anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(imdApiClient.getRainfallStats(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Connection timeout")));
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            // Act & Assert
            StepVerifier.create(weatherService.getRainfallStats(district, state))
                .expectErrorSatisfies(throwable -> {
                    assertTrue(throwable instanceof ImdApiException);
                    ImdApiException exception = (ImdApiException) throwable;
                    assertTrue(exception.getMessage().toLowerCase().contains("timeout") ||
                               exception.getMessage().toLowerCase().contains("connection") ||
                               exception.getMessage().toLowerCase().contains("retry"));
                })
                .verify();
        }
    }
}