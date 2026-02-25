package com.farmer.weather.service;

import com.farmer.weather.dto.*;
import com.farmer.weather.entity.WeatherCache;
import com.farmer.weather.repository.WeatherCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for cached data timestamp display.
 * 
 * Property 4: Cached Data Timestamp Display
 * Validates: Requirements 1.9, 5.12, 6.11, 12.5
 * 
 * These tests verify the following property:
 * For any cached data (weather, schemes, prices) displayed in offline mode, 
 * the system should include a timestamp indicating when the data was last fetched, 
 * and the timestamp should accurately reflect the cache time.
 * 
 * Requirements Reference:
 * - Requirement 1.9: WHEN the farmer is offline, THE Application SHALL display 
 *   the most recently cached weather data with a timestamp indicating data age
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherCacheTimestampPropertyTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private WeatherCacheRepository weatherCacheRepository;

    private WeatherCacheService weatherCacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        weatherCacheService = new WeatherCacheService(redisTemplate, weatherCacheRepository, 30);
    }

    @Nested
    @DisplayName("Property 4.1: Seven Day Forecast Cache Timestamp")
    class SevenDayForecastTimestamp {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Belgaum, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra",
            "Nagpur, Maharashtra",
            "Ludhiana, Punjab",
            "Jaipur, Rajasthan",
            "Ahmedabad, Gujarat",
            "Bhubaneswar, Odisha"
        })
        @DisplayName("Cached seven day forecast should include cache timestamp")
        void cachedSevenDayForecastShouldHaveTimestamp(String district, String state) {
            // Property: For any district and state, when weather data is cached,
            // the system should store a timestamp that accurately reflects when the data was cached.
            //
            // This test verifies that the cache timestamp is stored correctly
            // and can be retrieved for offline data age indication.
            
            // Arrange
            String expectedKey = "weather:" + district.toLowerCase() + ":" + state.toLowerCase() + ":7DAY";
            String timestampKey = expectedKey + ":timestamp";

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

            when(redisTemplate.delete(expectedKey)).thenReturn(true);
            when(weatherCacheRepository.save(any(WeatherCache.class))).thenAnswer(invocation -> {
                WeatherCache savedCache = invocation.getArgument(0);
                // Verify the cache has the correct timestamp
                assertNotNull(savedCache.getFetchedAt(), 
                    "Cached data should have a timestamp");
                return savedCache;
            });

            // Act
            weatherCacheService.cacheSevenDayForecast(district, state, forecast);

            // Assert - Verify timestamp was stored with correct TTL
            verify(redisTemplate.opsForValue()).set(
                eq(timestampKey),
                anyString(), // Any timestamp string is acceptable
                eq(Duration.ofMinutes(30))
            );
        }

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("Cache timestamp should be retrievable for offline data age display")
        void cacheTimestampShouldBeRetrievable(String district, String state) {
            // Property: For any cached weather data, the system should be able to retrieve
            // the cache timestamp to display data age in offline mode.
            
            // Arrange
            String expectedKey = "weather:" + district.toLowerCase() + ":" + state.toLowerCase() + ":7DAY";
            String timestampKey = expectedKey + ":timestamp";
            LocalDateTime cacheTime = LocalDateTime.now();

            when(valueOperations.get(timestampKey)).thenReturn(cacheTime.toString());

            // Act
            Optional<LocalDateTime> result = weatherCacheService.getCacheTimestamp(district, state, "7DAY");

            // Assert
            assertTrue(result.isPresent(), 
                "Cache timestamp should be retrievable for offline data age display");
            // The timestamp should be parseable and represent the cache time
            assertNotNull(result.get(), "Retrieved timestamp should not be null");
        }
    }

    @Nested
    @DisplayName("Property 4.2: Current Weather Cache Timestamp")
    class CurrentWeatherTimestamp {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Pune, Maharashtra",
            "Hyderabad, Telangana"
        })
        @DisplayName("Cached current weather should include cache timestamp")
        void cachedCurrentWeatherShouldHaveTimestamp(String district, String state) {
            // Property: For any district and state, when current weather is cached,
            // the system should store a timestamp that accurately reflects when the data was cached.
            
            // Arrange
            String expectedKey = "weather:" + district.toLowerCase() + ":" + state.toLowerCase() + ":CURRENT";
            String timestampKey = expectedKey + ":timestamp";

            CurrentWeatherDto currentWeather = CurrentWeatherDto.builder()
                .district(district)
                .state(state)
                .observationTime(LocalDateTime.now())
                .cloudCoverage(4)
                .temperatureCelsius(28.0)
                .build();

            when(redisTemplate.delete(expectedKey)).thenReturn(true);
            when(weatherCacheRepository.save(any(WeatherCache.class))).thenAnswer(invocation -> {
                WeatherCache savedCache = invocation.getArgument(0);
                assertNotNull(savedCache.getFetchedAt(), 
                    "Cached current weather should have a timestamp");
                return savedCache;
            });

            // Act
            weatherCacheService.cacheCurrentWeather(district, state, currentWeather);

            // Assert
            verify(redisTemplate.opsForValue()).set(
                eq(timestampKey),
                anyString(), // Any timestamp string is acceptable
                eq(Duration.ofMinutes(30))
            );
        }
    }

    @Nested
    @DisplayName("Property 4.3: Cache Timestamp Consistency Across Weather Data Types")
    class CacheTimestampConsistency {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("All weather data types should have consistent cache timestamp behavior")
        void allWeatherDataTypesShouldHaveConsistentTimestamp(String district, String state) {
            // Property: For any district and state, all types of weather data
            // (forecast, current, alerts, etc.) should have consistent cache timestamp behavior.
            
            // Arrange
            String[] forecastTypes = {"7DAY", "CURRENT", "NOWCAST", "ALERTS", "RAINFALL", "AGROMET"};

            // Create different types of weather data
            SevenDayForecastDto forecast = SevenDayForecastDto.builder()
                .district(district)
                .state(state)
                .build();

            CurrentWeatherDto currentWeather = CurrentWeatherDto.builder()
                .district(district)
                .state(state)
                .build();

            NowcastDto nowcast = NowcastDto.builder()
                .district(district)
                .state(state)
                .build();

            WeatherAlertDto alerts = WeatherAlertDto.builder()
                .district(district)
                .state(state)
                .build();

            RainfallStatsDto rainfall = RainfallStatsDto.builder()
                .district(district)
                .state(state)
                .build();

            AgrometAdvisoryDto agromet = AgrometAdvisoryDto.builder()
                .district(district)
                .state(state)
                .build();

            when(redisTemplate.delete(anyString())).thenReturn(true);
            when(weatherCacheRepository.save(any(WeatherCache.class))).thenReturn(mock(WeatherCache.class));

            // Act - Cache all types of weather data
            weatherCacheService.cacheSevenDayForecast(district, state, forecast);
            weatherCacheService.cacheCurrentWeather(district, state, currentWeather);
            weatherCacheService.cacheNowcast(district, state, nowcast);
            weatherCacheService.cacheWeatherAlerts(district, state, alerts);
            weatherCacheService.cacheRainfallStats(district, state, rainfall);
            weatherCacheService.cacheAgrometAdvisories(district, state, agromet);

            // Assert - Verify all types stored timestamps with correct TTL
            for (String forecastType : forecastTypes) {
                String expectedKey = "weather:" + district.toLowerCase() + ":" + state.toLowerCase() + ":" + forecastType;
                String timestampKey = expectedKey + ":timestamp";
                
                verify(redisTemplate.opsForValue()).set(
                    eq(timestampKey),
                    anyString(),
                    eq(Duration.ofMinutes(30))
                );
            }
        }
    }

    @Nested
    @DisplayName("Property 4.4: Cache TTL Consistency")
    class CacheTtlConsistency {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra"
        })
        @DisplayName("Cached weather data should have consistent 30-minute TTL")
        void cachedWeatherDataShouldHaveConsistentTtl(String district, String state) {
            // Property: For any district and state, cached weather data
            // should have the same TTL (30 minutes) for consistency.
            
            // Arrange
            String expectedKey = "weather:" + district.toLowerCase() + ":" + state.toLowerCase() + ":7DAY";
            String timestampKey = expectedKey + ":timestamp";

            SevenDayForecastDto forecast = SevenDayForecastDto.builder()
                .district(district)
                .state(state)
                .build();

            when(redisTemplate.delete(expectedKey)).thenReturn(true);
            when(weatherCacheRepository.save(any(WeatherCache.class))).thenReturn(mock(WeatherCache.class));

            // Act - Cache forecast data
            weatherCacheService.cacheSevenDayForecast(district, state, forecast);

            // Assert - Verify TTL is 30 minutes for the cached data
            verify(redisTemplate.opsForValue()).set(
                eq(expectedKey),
                any(),
                eq(Duration.ofMinutes(30))
            );
            
            // Verify timestamp also has correct TTL
            verify(redisTemplate.opsForValue()).set(
                eq(timestampKey),
                anyString(),
                eq(Duration.ofMinutes(30))
            );
        }
    }

    @Nested
    @DisplayName("Property 4.5: Offline Data Age Calculation")
    class OfflineDataAgeCalculation {

        @Test
        @DisplayName("Cache timestamp should enable accurate data age calculation")
        void cacheTimestampShouldEnableDataAgeCalculation() {
            // Property: The cache timestamp should enable accurate calculation of data age
            // for offline data display.
            
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";
            String expectedKey = "weather:" + district.toLowerCase() + ":" + state.toLowerCase() + ":7DAY";
            String timestampKey = expectedKey + ":timestamp";
            
            // Simulate cache was stored 15 minutes ago
            LocalDateTime cacheTime = LocalDateTime.now().minusMinutes(15);
            
            when(valueOperations.get(timestampKey)).thenReturn(cacheTime.toString());

            // Act
            Optional<LocalDateTime> result = weatherCacheService.getCacheTimestamp(district, state, "7DAY");

            // Assert
            assertTrue(result.isPresent(), "Cache timestamp should be retrievable");
            
            // Calculate data age
            LocalDateTime now = LocalDateTime.now();
            long dataAgeMinutes = java.time.Duration.between(result.get(), now).toMinutes();
            
            // Data age should be approximately 15 minutes (with some tolerance)
            assertTrue(dataAgeMinutes >= 14 && dataAgeMinutes <= 16,
                "Data age should be approximately 15 minutes, was: " + dataAgeMinutes);
        }

        @Test
        @DisplayName("Cache validity check should work correctly for expired cache")
        void cacheValidityCheckForExpiredCache() {
            // Property: The cache validity check should correctly identify expired cache
            // for proper offline data handling.
            
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";
            String expectedKey = "weather:" + district.toLowerCase() + ":" + state.toLowerCase() + ":7DAY";
            
            // Cache expired (more than 30 minutes ago)
            when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

            // Act
            boolean isValid = weatherCacheService.isCacheValid(district, state, "7DAY");

            // Assert
            assertFalse(isValid, "Expired cache should be marked as invalid");
        }

        @Test
        @DisplayName("Cache validity check should work correctly for valid cache")
        void cacheValidityCheckForValidCache() {
            // Property: The cache validity check should correctly identify valid cache
            // for proper offline data handling.
            
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";
            String expectedKey = "weather:" + district.toLowerCase() + ":" + state.toLowerCase() + ":7DAY";
            
            // Cache is valid (exists in Redis)
            when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

            // Act
            boolean isValid = weatherCacheService.isCacheValid(district, state, "7DAY");

            // Assert
            assertTrue(isValid, "Valid cache should be marked as valid");
        }
    }

    @Nested
    @DisplayName("Property 4.6: Cache Invalidation Timestamp Update")
    class CacheInvalidationTimestampUpdate {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("Cache invalidation should update timestamp for new data")
        void cacheInvalidationShouldUpdateTimestamp(String district, String state) {
            // Property: When new data is fetched and cached, the old cache should be invalidated
            // and a new timestamp should be stored for the new data.
            
            // Arrange
            String expectedKey = "weather:" + district.toLowerCase() + ":" + state.toLowerCase() + ":7DAY";
            String timestampKey = expectedKey + ":timestamp";
            
            SevenDayForecastDto forecast1 = SevenDayForecastDto.builder()
                .district(district)
                .state(state)
                .build();

            SevenDayForecastDto forecast2 = SevenDayForecastDto.builder()
                .district(district)
                .state(state)
                .build();

            when(redisTemplate.delete(expectedKey)).thenReturn(true);
            when(weatherCacheRepository.save(any(WeatherCache.class))).thenReturn(mock(WeatherCache.class));

            // Act - Cache data twice (simulating new data fetch)
            weatherCacheService.cacheSevenDayForecast(district, state, forecast1);
            weatherCacheService.cacheSevenDayForecast(district, state, forecast2);

            // Assert - Verify old cache was invalidated and new timestamp was stored
            verify(redisTemplate, times(2)).delete(expectedKey);
            verify(redisTemplate.opsForValue(), times(2)).set(
                eq(timestampKey),
                anyString(),
                eq(Duration.ofMinutes(30))
            );
        }
    }
}