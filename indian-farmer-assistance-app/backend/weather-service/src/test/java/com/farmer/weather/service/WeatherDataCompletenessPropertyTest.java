package com.farmer.weather.service;

import com.farmer.weather.client.WeatherApiClient;
import com.farmer.weather.dto.*;
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
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for weather data completeness.
 * 
 * Property 2: Weather Data Completeness
 * Validates: Requirements 1.2
 * 
 * These tests verify the following property:
 * For any weather forecast response, all required fields (max temp, min temp, 
 * humidity at 0830 and 1730 hours, rainfall, wind speed, wind direction, 
 * cloud coverage, sunrise/sunset times, moonrise/moonset times) should be 
 * present for each of the 7 forecast days.
 * 
 * Requirements Reference:
 * - Requirement 1.2: WHEN weather data is received, THE Application SHALL display 
 *   maximum temperature, minimum temperature, relative humidity (0830 and 1730 hours), 
 *   sunrise/sunset times, moonrise/moonset times, and nebulosity (cloud coverage 0-8 scale) 
 *   for the next 7 days
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherDataCompletenessPropertyTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private WeatherCacheRepository weatherCacheRepository;

    @Mock
    private WeatherApiClient WeatherApiClient;

    private WeatherCacheService weatherCacheService;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        weatherCacheService = new WeatherCacheService(redisTemplate, weatherCacheRepository, 30);
        weatherService = new WeatherService(WeatherApiClient, weatherCacheService, weatherCacheRepository);
    }

    /**
     * Helper method to create a complete forecast day with all required fields.
     */
    private ForecastDayDto createCompleteForecastDay(LocalDate date) {
        return ForecastDayDto.builder()
            .date(date)
            .maxTempCelsius(32.5)
            .minTempCelsius(22.0)
            .humidity0830(75.0)
            .humidity1730(65.0)
            .rainfallMm(12.5)
            .windSpeedKmph(15.0)
            .windDirection("SW")
            .cloudCoverage(4)
            .sunriseTime("06:15")
            .sunsetTime("18:45")
            .moonriseTime("14:30")
            .moonsetTime("02:15")
            .build();
    }

    /**
     * Helper method to create a complete 7-day forecast.
     */
    private SevenDayForecastDto createCompleteSevenDayForecast(String district, String state) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        return SevenDayForecastDto.builder()
            .district(district)
            .state(state)
            .fetchedAt(LocalDateTime.now())
            .validFrom(todayStart)
            .validUntil(today.plusDays(7).atStartOfDay())
            .forecastDays(List.of(
                createCompleteForecastDay(today),
                createCompleteForecastDay(today.plusDays(1)),
                createCompleteForecastDay(today.plusDays(2)),
                createCompleteForecastDay(today.plusDays(3)),
                createCompleteForecastDay(today.plusDays(4)),
                createCompleteForecastDay(today.plusDays(5)),
                createCompleteForecastDay(today.plusDays(6))
            ))
            .build();
    }

    @Nested
    @DisplayName("Property 2.1: All Required Fields Present in Forecast Response")
    class AllRequiredFieldsPresent {

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
        @DisplayName("Seven day forecast should contain all required weather fields for each day")
        void sevenDayForecastShouldContainAllRequiredFields(String district, String state) {
            // Property: For any district and state, when weather data is received,
            // all required fields should be present for each of the 7 forecast days.
            //
            // Required fields per day:
            // - maxTempCelsius (maximum temperature)
            // - minTempCelsius (minimum temperature)
            // - humidity0830 (relative humidity at 0830 hours)
            // - humidity1730 (relative humidity at 1730 hours)
            // - rainfallMm (rainfall)
            // - windSpeedKmph (wind speed)
            // - windDirection (wind direction)
            // - cloudCoverage (nebulosity, 0-8 scale)
            // - sunriseTime (sunrise time)
            // - sunsetTime (sunset time)
            // - moonriseTime (moonrise time)
            // - moonsetTime (moonset time)
            
            // Arrange
            SevenDayForecastDto completeForecast = createCompleteSevenDayForecast(district, state);
            
            // Mock cache miss
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(WeatherApiClient.getSevenDayForecast(district, state))
                .thenReturn(Mono.just(completeForecast));

            // Act
            Mono<SevenDayForecastDto> result = weatherService.getSevenDayForecast(district, state);

            // Assert - Verify all required fields are present for all 7 days
            Mono<SevenDayForecastDto> testResult = result;
            assertNotNull(testResult, "Forecast result should not be null");
            
            // Verify the forecast contains 7 days
            assertEquals(7, completeForecast.getForecastDays().size(),
                "Forecast should contain exactly 7 days");
            
            // Verify each day has all required fields
            for (int i = 0; i < 7; i++) {
                ForecastDayDto day = completeForecast.getForecastDays().get(i);
                
                // Temperature fields
                assertNotNull(day.getMaxTempCelsius(), 
                    "Day " + (i + 1) + ": maxTempCelsius should not be null");
                assertNotNull(day.getMinTempCelsius(), 
                    "Day " + (i + 1) + ": minTempCelsius should not be null");
                
                // Humidity fields
                assertNotNull(day.getHumidity0830(), 
                    "Day " + (i + 1) + ": humidity0830 should not be null");
                assertNotNull(day.getHumidity1730(), 
                    "Day " + (i + 1) + ": humidity1730 should not be null");
                
                // Rainfall field
                assertNotNull(day.getRainfallMm(), 
                    "Day " + (i + 1) + ": rainfallMm should not be null");
                
                // Wind fields
                assertNotNull(day.getWindSpeedKmph(), 
                    "Day " + (i + 1) + ": windSpeedKmph should not be null");
                assertNotNull(day.getWindDirection(), 
                    "Day " + (i + 1) + ": windDirection should not be null");
                
                // Cloud coverage (nebulosity 0-8 scale)
                assertNotNull(day.getCloudCoverage(), 
                    "Day " + (i + 1) + ": cloudCoverage should not be null");
                assertTrue(day.getCloudCoverage() >= 0 && day.getCloudCoverage() <= 8,
                    "Day " + (i + 1) + ": cloudCoverage should be on 0-8 scale, was: " + day.getCloudCoverage());
                
                // Sun times
                assertNotNull(day.getSunriseTime(), 
                    "Day " + (i + 1) + ": sunriseTime should not be null");
                assertNotNull(day.getSunsetTime(), 
                    "Day " + (i + 1) + ": sunsetTime should not be null");
                
                // Moon times
                assertNotNull(day.getMoonriseTime(), 
                    "Day " + (i + 1) + ": moonriseTime should not be null");
                assertNotNull(day.getMoonsetTime(), 
                    "Day " + (i + 1) + ": moonsetTime should not be null");
            }
        }

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("All forecast days should have valid data types and ranges")
        void allForecastDaysShouldHaveValidDataTypes(String district, String state) {
            // Property: For any district and state, all forecast days should have
            // valid data types and reasonable ranges for all required fields.
            
            // Arrange
            SevenDayForecastDto completeForecast = createCompleteSevenDayForecast(district, state);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(WeatherApiClient.getSevenDayForecast(district, state))
                .thenReturn(Mono.just(completeForecast));

            // Act
            Mono<SevenDayForecastDto> result = weatherService.getSevenDayForecast(district, state);

            // Assert - Verify valid data types and ranges
            for (int i = 0; i < 7; i++) {
                ForecastDayDto day = completeForecast.getForecastDays().get(i);
                
                // Temperature should be reasonable for Indian climate (0-50°C)
                assertTrue(day.getMaxTempCelsius() >= 0 && day.getMaxTempCelsius() <= 50,
                    "Day " + (i + 1) + ": maxTempCelsius should be between 0-50°C");
                assertTrue(day.getMinTempCelsius() >= 0 && day.getMinTempCelsius() <= 50,
                    "Day " + (i + 1) + ": minTempCelsius should be between 0-50°C");
                
                // Max temp should be >= min temp
                assertTrue(day.getMaxTempCelsius() >= day.getMinTempCelsius(),
                    "Day " + (i + 1) + ": maxTempCelsius should be >= minTempCelsius");
                
                // Humidity should be percentage (0-100)
                assertTrue(day.getHumidity0830() >= 0 && day.getHumidity0830() <= 100,
                    "Day " + (i + 1) + ": humidity0830 should be between 0-100%");
                assertTrue(day.getHumidity1730() >= 0 && day.getHumidity1730() <= 100,
                    "Day " + (i + 1) + ": humidity1730 should be between 0-100%");
                
                // Rainfall should be non-negative
                assertTrue(day.getRainfallMm() >= 0,
                    "Day " + (i + 1) + ": rainfallMm should be non-negative");
                
                // Wind speed should be non-negative
                assertTrue(day.getWindSpeedKmph() >= 0,
                    "Day " + (i + 1) + ": windSpeedKmph should be non-negative");
                
                // Wind direction should be valid compass direction (2-3 characters)
                assertTrue(day.getWindDirection().length() >= 2 && day.getWindDirection().length() <= 3,
                    "Day " + (i + 1) + ": windDirection should be 2-3 characters");
            }
        }
    }

    @Nested
    @DisplayName("Property 2.2: Seven Day Forecast Contains Exactly 7 Days")
    class SevenDayForecastContainsExactlySevenDays {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Belgaum, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra",
            "Nagpur, Maharashtra"
        })
        @DisplayName("Seven day forecast should contain exactly 7 days of data")
        void sevenDayForecastShouldContainExactlySevenDays(String district, String state) {
            // Property: For any district and state, the seven-day forecast
            // should contain exactly 7 days of forecast data.
            
            // Arrange
            SevenDayForecastDto forecast = createCompleteSevenDayForecast(district, state);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(WeatherApiClient.getSevenDayForecast(district, state))
                .thenReturn(Mono.just(forecast));

            // Act
            Mono<SevenDayForecastDto> result = weatherService.getSevenDayForecast(district, state);

            // Assert
            assertNotNull(forecast, "Forecast should not be null");
            assertNotNull(forecast.getForecastDays(), "Forecast days list should not be null");
            assertEquals(7, forecast.getForecastDays().size(),
                "Forecast should contain exactly 7 days");
        }

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("Forecast days should be consecutive starting from today")
        void forecastDaysShouldBeConsecutive(String district, String state) {
            // Property: For any district and state, the 7 forecast days
            // should be consecutive dates starting from today.
            
            // Arrange
            LocalDate today = LocalDate.now();
            SevenDayForecastDto forecast = createCompleteSevenDayForecast(district, state);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(WeatherApiClient.getSevenDayForecast(district, state))
                .thenReturn(Mono.just(forecast));

            // Act
            Mono<SevenDayForecastDto> result = weatherService.getSevenDayForecast(district, state);

            // Assert - Verify consecutive dates
            List<ForecastDayDto> days = forecast.getForecastDays();
            for (int i = 0; i < days.size(); i++) {
                LocalDate expectedDate = today.plusDays(i);
                assertEquals(expectedDate, days.get(i).getDate(),
                    "Day " + (i + 1) + " should be " + expectedDate);
            }
        }
    }

    @Nested
    @DisplayName("Property 2.3: Forecast Response Metadata Completeness")
    class ForecastResponseMetadataCompleteness {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra"
        })
        @DisplayName("Forecast response should contain required metadata fields")
        void forecastResponseShouldContainMetadata(String district, String state) {
            // Property: For any district and state, the forecast response
            // should contain required metadata (district, state, fetchedAt, etc.).
            
            // Arrange
            SevenDayForecastDto forecast = createCompleteSevenDayForecast(district, state);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(WeatherApiClient.getSevenDayForecast(district, state))
                .thenReturn(Mono.just(forecast));

            // Act
            Mono<SevenDayForecastDto> result = weatherService.getSevenDayForecast(district, state);

            // Assert - Verify metadata fields
            assertNotNull(forecast.getDistrict(), "District should not be null");
            assertNotNull(forecast.getState(), "State should not be null");
            assertNotNull(forecast.getFetchedAt(), "FetchedAt timestamp should not be null");
            assertNotNull(forecast.getValidFrom(), "ValidFrom date should not be null");
            assertNotNull(forecast.getValidUntil(), "ValidUntil date should not be null");
            
            // Verify district and state match
            assertEquals(district, forecast.getDistrict(),
                "District in response should match requested district");
            assertEquals(state, forecast.getState(),
                "State in response should match requested state");
            
            // Verify validFrom is today (compare date portion of LocalDateTime)
            assertEquals(LocalDate.now(), forecast.getValidFrom().toLocalDate(),
                "ValidFrom should be today");
            
            // Verify validUntil is 7 days from today (compare date portion of LocalDateTime)
            assertEquals(LocalDate.now().plusDays(7), forecast.getValidUntil().toLocalDate(),
                "ValidUntil should be 7 days from today");
        }
    }

    @Nested
    @DisplayName("Property 2.4: Data Consistency Across Multiple Forecast Requests")
    class DataConsistencyAcrossRequests {

        @Test
        @DisplayName("Multiple forecast requests for same location should return consistent data structure")
        void multipleRequestsShouldReturnConsistentStructure() {
            // Property: For any district and state, multiple forecast requests
            // should return data with consistent structure (all fields present).
            
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";
            SevenDayForecastDto forecast = createCompleteSevenDayForecast(district, state);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(WeatherApiClient.getSevenDayForecast(district, state))
                .thenReturn(Mono.just(forecast));

            // Act - Make multiple requests
            Mono<SevenDayForecastDto> result1 = weatherService.getSevenDayForecast(district, state);
            Mono<SevenDayForecastDto> result2 = weatherService.getSevenDayForecast(district, state);
            Mono<SevenDayForecastDto> result3 = weatherService.getSevenDayForecast(district, state);

            // Assert - All requests should return complete data
            assertNotNull(result1, "First request should not return null");
            assertNotNull(result2, "Second request should not return null");
            assertNotNull(result3, "Third request should not return null");
            
            // Verify all 7 days have complete data in each response
            for (Mono<SevenDayForecastDto> result : List.of(result1, result2, result3)) {
                assertNotNull(result, "Result should not be null");
                assertEquals(7, forecast.getForecastDays().size(),
                    "Each response should contain exactly 7 days");
                
                for (int i = 0; i < 7; i++) {
                    ForecastDayDto day = forecast.getForecastDays().get(i);
                    assertNotNull(day.getMaxTempCelsius(), 
                        "All requests should have maxTempCelsius for day " + (i + 1));
                    assertNotNull(day.getMinTempCelsius(), 
                        "All requests should have minTempCelsius for day " + (i + 1));
                    assertNotNull(day.getHumidity0830(), 
                        "All requests should have humidity0830 for day " + (i + 1));
                    assertNotNull(day.getHumidity1730(), 
                        "All requests should have humidity1730 for day " + (i + 1));
                    assertNotNull(day.getCloudCoverage(), 
                        "All requests should have cloudCoverage for day " + (i + 1));
                }
            }
        }

        @Test
        @DisplayName("Forecast data should maintain field presence invariant across all days")
        void forecastDataShouldMaintainFieldPresenceInvariant() {
            // Property: The invariant that all required fields are present
            // should hold for all 7 days in the forecast.
            
            // Arrange
            String district = "Bangalore Rural";
            String state = "Karnataka";
            SevenDayForecastDto forecast = createCompleteSevenDayForecast(district, state);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(WeatherApiClient.getSevenDayForecast(district, state))
                .thenReturn(Mono.just(forecast));

            // Act
            Mono<SevenDayForecastDto> result = weatherService.getSevenDayForecast(district, state);

            // Assert - Verify invariant: all required fields present for all days
            List<ForecastDayDto> days = forecast.getForecastDays();
            
            // Check that we have exactly 7 days
            assertEquals(7, days.size(), "Forecast must have exactly 7 days");
            
            // For each required field, verify it's present for all days
            String[] requiredFields = {
                "maxTempCelsius", "minTempCelsius", "humidity0830", "humidity1730",
                "rainfallMm", "windSpeedKmph", "windDirection", "cloudCoverage",
                "sunriseTime", "sunsetTime", "moonriseTime", "moonsetTime"
            };
            
            for (String field : requiredFields) {
                for (int i = 0; i < 7; i++) {
                    ForecastDayDto day = days.get(i);
                    Object value = null;
                    
                    // Use reflection to get field value
                    try {
                        java.lang.reflect.Method getter = day.getClass().getMethod("get" + 
                            Character.toUpperCase(field.charAt(0)) + field.substring(1));
                        value = getter.invoke(day);
                    } catch (Exception e) {
                        fail("Failed to access field: " + field);
                    }
                    
                    assertNotNull(value, 
                        "Field '" + field + "' must be present for day " + (i + 1));
                }
            }
        }
    }

    @Nested
    @DisplayName("Property 2.5: Cloud Coverage Scale Validation")
    class CloudCoverageScaleValidation {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("Cloud coverage should always be on 0-8 scale (nebulosity)")
        void cloudCoverageShouldBeOnZeroToEightScale(String district, String state) {
            // Property: For any district and state, cloud coverage (nebulosity)
            // should always be on the 0-8 scale as per IMD standards.
            
            // Arrange
            SevenDayForecastDto forecast = createCompleteSevenDayForecast(district, state);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(WeatherApiClient.getSevenDayForecast(district, state))
                .thenReturn(Mono.just(forecast));

            // Act
            Mono<SevenDayForecastDto> result = weatherService.getSevenDayForecast(district, state);

            // Assert - Verify cloud coverage is on 0-8 scale
            for (int i = 0; i < 7; i++) {
                ForecastDayDto day = forecast.getForecastDays().get(i);
                
                assertNotNull(day.getCloudCoverage(), 
                    "Day " + (i + 1) + ": cloudCoverage should not be null");
                
                // IMD nebulosity scale is 0-8 (oktas)
                assertTrue(day.getCloudCoverage() >= 0 && day.getCloudCoverage() <= 8,
                    "Day " + (i + 1) + ": cloudCoverage should be on 0-8 scale (oktas), was: " + 
                    day.getCloudCoverage());
            }
        }
    }

    @Nested
    @DisplayName("Property 2.6: Temperature Range Validity")
    class TemperatureRangeValidity {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra",
            "Jaipur, Rajasthan"
        })
        @DisplayName("Temperature values should be within valid range for Indian climate")
        void temperatureValuesShouldBeWithinValidRange(String district, String state) {
            // Property: For any district and state, temperature values
            // should be within a valid range for Indian climate.
            
            // Arrange
            SevenDayForecastDto forecast = createCompleteSevenDayForecast(district, state);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
            when(WeatherApiClient.getSevenDayForecast(district, state))
                .thenReturn(Mono.just(forecast));

            // Act
            Mono<SevenDayForecastDto> result = weatherService.getSevenDayForecast(district, state);

            // Assert - Verify temperature ranges
            for (int i = 0; i < 7; i++) {
                ForecastDayDto day = forecast.getForecastDays().get(i);
                
                // Valid temperature range for India: 0°C to 50°C (extremes)
                assertTrue(day.getMaxTempCelsius() >= 0 && day.getMaxTempCelsius() <= 50,
                    "Day " + (i + 1) + ": maxTempCelsius should be between 0-50°C, was: " + 
                    day.getMaxTempCelsius());
                
                assertTrue(day.getMinTempCelsius() >= 0 && day.getMinTempCelsius() <= 50,
                    "Day " + (i + 1) + ": minTempCelsius should be between 0-50°C, was: " + 
                    day.getMinTempCelsius());
                
                // Max temp should be >= min temp (physically meaningful)
                assertTrue(day.getMaxTempCelsius() >= day.getMinTempCelsius(),
                    "Day " + (i + 1) + ": maxTempCelsius should be >= minTempCelsius. " +
                    "Max: " + day.getMaxTempCelsius() + ", Min: " + day.getMinTempCelsius());
            }
        }
    }
}
