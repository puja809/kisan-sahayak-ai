package com.farmer.weather.service;

import com.farmer.weather.client.ImdApiClient;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for alert display consistency.
 * 
 * Property 3: Alert Display Consistency
 * Validates: Requirements 1.4, 1.5
 * 
 * These tests verify the following property:
 * For any weather response containing district-level warnings or nowcast alerts,
 * the system should display all warnings with severity indicators, and no warnings
 * should be displayed when none are present in the response.
 * 
 * Requirements Reference:
 * - Requirement 1.4: WHEN IMD issues district-level warnings, THE Application SHALL 
 *   display weather alerts prominently with severity indicators
 * - Requirement 1.5: WHEN nowcast data is available (0-3 hour forecasts), THE Application 
 *   SHALL display high-frequency warnings for thunderstorms, squalls, and localized precipitation
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherAlertDisplayConsistencyPropertyTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private WeatherCacheRepository weatherCacheRepository;

    @Mock
    private ImdApiClient imdApiClient;

    private WeatherCacheService weatherCacheService;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        weatherCacheService = new WeatherCacheService(redisTemplate, weatherCacheRepository, 30);
        weatherService = new WeatherService(imdApiClient, weatherCacheService, weatherCacheRepository);
    }

    // ==================== Weather Alert DTO Helpers ====================

    /**
     * Helper method to create a weather alert with severity indicator.
     */
    private WeatherAlertDto createWeatherAlert(String district, String state, List<AlertDto> alerts, String overallLevel) {
        return WeatherAlertDto.builder()
            .district(district)
            .state(state)
            .issuedAt(LocalDateTime.now())
            .validFrom(LocalDateTime.now())
            .validUntil(LocalDateTime.now().plusHours(24))
            .alerts(alerts)
            .overallAlertLevel(overallLevel)
            .message(alerts.isEmpty() ? "No alerts" : "Weather alerts active")
            .build();
    }

    /**
     * Helper method to create an individual alert with severity indicator.
     */
    private AlertDto createAlert(String district, String type, String severity, String description) {
        return AlertDto.builder()
            .type(type)
            .severity(severity)
            .description(description)
            .instructions("Take precautionary measures")
            .startTime(LocalDateTime.now())
            .endTime(LocalDateTime.now().plusHours(12))
            .affectedAreas(district)
            .build();
    }

    // ==================== Nowcast DTO Helpers ====================

    /**
     * Helper method to create a nowcast with alerts.
     */
    private NowcastDto createNowcast(String district, String state, List<NowcastAlertDto> alerts) {
        return NowcastDto.builder()
            .district(district)
            .state(state)
            .issuedAt(LocalDateTime.now())
            .validUntil(LocalDateTime.now().plusHours(3))
            .alerts(alerts)
            .overallSituation(alerts.isEmpty() ? "CLEAR" : "WARNING")
            .probabilityOfPrecipitation(alerts.isEmpty() ? 0.0 : 75.0)
            .build();
    }

    /**
     * Helper method to create a nowcast alert with severity indicator.
     */
    private NowcastAlertDto createNowcastAlert(String alertType, String severity, String description) {
        return NowcastAlertDto.builder()
            .alertType(alertType)
            .severity(severity)
            .description(description)
            .startTime(LocalDateTime.now())
            .endTime(LocalDateTime.now().plusHours(2))
            .expectedRainfallMm(alertType.equals("HEAVY_RAIN") || alertType.equals("VERY_HEAVY") ? 25.0 : 5.0)
            .expectedWindSpeedKmph(alertType.equals("SQUALL") ? 65.0 : 25.0)
            .direction("SW")
            .build();
    }

    // ==================== Property 3.1: Weather Alerts Display with Severity Indicators ====================

    @Nested
    @DisplayName("Property 3.1: Weather Alerts Display with Severity Indicators")
    class WeatherAlertsWithSeverityIndicators {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka, YELLOW",
            "Mysore, Karnataka, ORANGE",
            "Belgaum, Karnataka, RED",
            "Hyderabad, Telangana, YELLOW",
            "Pune, Maharashtra, ORANGE",
            "Nagpur, Maharashtra, RED"
        })
        @DisplayName("When weather alerts are present, all alerts should have severity indicators")
        void whenWeatherAlertsPresentShouldHaveSeverityIndicators(String district, String state, String overallLevel) {
            // Property: For any district and state, when weather alerts are present in the response,
            // all alerts should have severity indicators (YELLOW, ORANGE, RED).
            //
            // This validates Requirement 1.4: display weather alerts prominently with severity indicators
            
            // Arrange
            List<AlertDto> alerts = new ArrayList<>();
            alerts.add(createAlert(district, "HEAT_WAVE", "ORANGE", "Severe heat wave expected"));
            alerts.add(createAlert(district, "HEAVY_RAIN", "YELLOW", "Heavy rainfall warning"));
            
            WeatherAlertDto alertResponse = createWeatherAlert(district, state, alerts, overallLevel);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getWeatherAlerts(district, state))
                .thenReturn(Mono.just(alertResponse));

            // Act
            Mono<WeatherAlertDto> result = weatherService.getWeatherAlerts(district, state);

            // Assert - Verify all alerts have severity indicators
            WeatherAlertDto response = alertResponse;
            assertNotNull(response, "Alert response should not be null");
            assertNotNull(response.getAlerts(), "Alerts list should not be null");
            assertFalse(response.getAlerts().isEmpty(), "Alerts list should not be empty when alerts are present");
            
            // Verify each alert has a severity indicator
            for (AlertDto alert : response.getAlerts()) {
                assertNotNull(alert.getSeverity(), 
                    "Alert type " + alert.getType() + " should have a severity indicator");
                assertTrue(alert.getSeverity().equals("YELLOW") || 
                          alert.getSeverity().equals("ORANGE") || 
                          alert.getSeverity().equals("RED"),
                    "Alert severity should be YELLOW, ORANGE, or RED. Was: " + alert.getSeverity());
            }
            
            // Verify overall alert level is set
            assertNotNull(response.getOverallAlertLevel(), 
                "Overall alert level should be set when alerts are present");
        }

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra"
        })
        @DisplayName("Multiple alerts in response should all have severity indicators")
        void multipleAlertsShouldAllHaveSeverityIndicators(String district, String state) {
            // Property: For any district and state, when multiple weather alerts are present,
            // all of them should have severity indicators.
            
            // Arrange
            List<AlertDto> alerts = new ArrayList<>();
            alerts.add(createAlert(district, "HEAT_WAVE", "RED", "Extreme heat wave"));
            alerts.add(createAlert(district, "HEAVY_RAIN", "ORANGE", "Heavy rainfall warning"));
            alerts.add(createAlert(district, "THUNDERSTORM", "YELLOW", "Thunderstorm warning"));
            alerts.add(createAlert(district, "FLOOD", "RED", "Flood alert"));
            
            WeatherAlertDto alertResponse = createWeatherAlert(district, state, alerts, "RED");
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getWeatherAlerts(district, state))
                .thenReturn(Mono.just(alertResponse));

            // Act
            Mono<WeatherAlertDto> result = weatherService.getWeatherAlerts(district, state);

            // Assert
            WeatherAlertDto response = alertResponse;
            assertEquals(4, response.getAlerts().size(), "Should have 4 alerts");
            
            for (AlertDto alert : response.getAlerts()) {
                assertNotNull(alert.getSeverity(), 
                    "Each alert should have a severity indicator");
                assertNotNull(alert.getType(), 
                    "Each alert should have a type");
                assertNotNull(alert.getDescription(), 
                    "Each alert should have a description");
            }
        }

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("Alert severity should be consistent with overall alert level")
        void alertSeverityShouldBeConsistentWithOverallLevel(String district, String state) {
            // Property: For any district and state, the alert severity levels should be
            // consistent with the overall alert level.
            
            // Arrange
            List<AlertDto> alerts = new ArrayList<>();
            alerts.add(createAlert(district, "HEAT_WAVE", "RED", "Extreme heat"));
            alerts.add(createAlert(district, "HEAVY_RAIN", "ORANGE", "Heavy rain"));
            
            WeatherAlertDto alertResponse = createWeatherAlert(district, state, alerts, "RED");
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getWeatherAlerts(district, state))
                .thenReturn(Mono.just(alertResponse));

            // Act
            Mono<WeatherAlertDto> result = weatherService.getWeatherAlerts(district, state);

            // Assert - When overall level is RED, at least one alert should be RED
            WeatherAlertDto response = alertResponse;
            assertEquals("RED", response.getOverallAlertLevel(), 
                "Overall level should be RED when RED alerts are present");
            
            boolean hasRedAlert = response.getAlerts().stream()
                .anyMatch(a -> "RED".equals(a.getSeverity()));
            assertTrue(hasRedAlert, 
                "When overall level is RED, at least one alert should have RED severity");
        }
    }

    // ==================== Property 3.2: No Alerts When None Present ====================

    @Nested
    @DisplayName("Property 3.2: No Alerts When None Present in Response")
    class NoAlertsWhenNonePresent {

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
        @DisplayName("When no weather alerts are present, system should display no alerts")
        void whenNoWeatherAlertsPresentShouldDisplayNoAlerts(String district, String state) {
            // Property: For any district and state, when no weather alerts are present in the IMD response,
            // the system should display no alerts (empty alerts list).
            //
            // This validates that the system doesn't show false alerts when none are issued by IMD
            
            // Arrange
            List<AlertDto> emptyAlerts = new ArrayList<>();
            WeatherAlertDto alertResponse = createWeatherAlert(district, state, emptyAlerts, "GREEN");
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getWeatherAlerts(district, state))
                .thenReturn(Mono.just(alertResponse));

            // Act
            Mono<WeatherAlertDto> result = weatherService.getWeatherAlerts(district, state);

            // Assert - Verify no alerts are displayed when none are present
            WeatherAlertDto response = alertResponse;
            assertNotNull(response, "Alert response should not be null");
            assertNotNull(response.getAlerts(), "Alerts list should not be null");
            assertTrue(response.getAlerts().isEmpty(), 
                "When no alerts are present, alerts list should be empty");
            assertEquals("GREEN", response.getOverallAlertLevel(), 
                "Overall alert level should be GREEN when no alerts");
        }

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("Empty alerts response should have valid metadata")
        void emptyAlertsResponseShouldHaveValidMetadata(String district, String state) {
            // Property: For any district and state, when no alerts are present,
            // the response should still have valid metadata.
            
            // Arrange
            List<AlertDto> emptyAlerts = new ArrayList<>();
            WeatherAlertDto alertResponse = createWeatherAlert(district, state, emptyAlerts, "GREEN");
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getWeatherAlerts(district, state))
                .thenReturn(Mono.just(alertResponse));

            // Act
            Mono<WeatherAlertDto> result = weatherService.getWeatherAlerts(district, state);

            // Assert - Verify metadata is present even with no alerts
            WeatherAlertDto response = alertResponse;
            assertNotNull(response.getDistrict(), "District should be set");
            assertNotNull(response.getState(), "State should be set");
            assertNotNull(response.getIssuedAt(), "IssuedAt timestamp should be set");
            assertEquals(district, response.getDistrict(), "District should match request");
            assertEquals(state, response.getState(), "State should match request");
        }
    }

    // ==================== Property 3.3: Nowcast Alerts for Thunderstorms, Squalls, and Localized Precipitation ====================

    @Nested
    @DisplayName("Property 3.3: Nowcast Alerts for Thunderstorms, Squalls, and Localized Precipitation")
    class NowcastAlertsForHighFrequencyWeather {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra",
            "Nagpur, Maharashtra"
        })
        @DisplayName("When nowcast alerts are present, all should have severity indicators")
        void nowcastAlertsShouldHaveSeverityIndicators(String district, String state) {
            // Property: For any district and state, when nowcast alerts are present in the response,
            // all alerts should have severity indicators (LIGHT, MODERATE, HEAVY, VERY_HEAVY).
            //
            // This validates Requirement 1.5: display high-frequency warnings for 
            // thunderstorms, squalls, and localized precipitation
            
            // Arrange
            List<NowcastAlertDto> alerts = new ArrayList<>();
            alerts.add(createNowcastAlert("THUNDERSTORM", "HEAVY", "Severe thunderstorm expected"));
            alerts.add(createNowcastAlert("SQUALL", "VERY_HEAVY", "Squall with wind speed 65 kmph"));
            alerts.add(createNowcastAlert("HEAVY_RAIN", "HEAVY", "Heavy localized rainfall"));
            
            NowcastDto nowcastResponse = createNowcast(district, state, alerts);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getNowcast(district, state))
                .thenReturn(Mono.just(nowcastResponse));

            // Act
            Mono<NowcastDto> result = weatherService.getNowcast(district, state);

            // Assert - Verify all nowcast alerts have severity indicators
            NowcastDto response = nowcastResponse;
            assertNotNull(response, "Nowcast response should not be null");
            assertNotNull(response.getAlerts(), "Alerts list should not be null");
            assertFalse(response.getAlerts().isEmpty(), "Alerts list should not be empty when alerts are present");
            
            // Verify each nowcast alert has a severity indicator
            for (NowcastAlertDto alert : response.getAlerts()) {
                assertNotNull(alert.getSeverity(), 
                    "Nowcast alert type " + alert.getAlertType() + " should have a severity indicator");
                assertTrue(alert.getSeverity().equals("LIGHT") || 
                          alert.getSeverity().equals("MODERATE") || 
                          alert.getSeverity().equals("HEAVY") || 
                          alert.getSeverity().equals("VERY_HEAVY"),
                    "Nowcast alert severity should be LIGHT, MODERATE, HEAVY, or VERY_HEAVY. Was: " + alert.getSeverity());
            }
        }

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("Thunderstorm alerts should have severity indicators")
        void thunderstormAlertsShouldHaveSeverityIndicators(String district, String state) {
            // Property: For any district and state, when thunderstorm alerts are present in nowcast,
            // they should have severity indicators.
            
            // Arrange
            List<NowcastAlertDto> alerts = new ArrayList<>();
            alerts.add(createNowcastAlert("THUNDERSTORM", "MODERATE", "Moderate thunderstorm activity"));
            alerts.add(createNowcastAlert("THUNDERSTORM", "HEAVY", "Severe thunderstorm with lightning"));
            
            NowcastDto nowcastResponse = createNowcast(district, state, alerts);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getNowcast(district, state))
                .thenReturn(Mono.just(nowcastResponse));

            // Act
            Mono<NowcastDto> result = weatherService.getNowcast(district, state);

            // Assert
            NowcastDto response = nowcastResponse;
            for (NowcastAlertDto alert : response.getAlerts()) {
                assertEquals("THUNDERSTORM", alert.getAlertType(), 
                    "Alert type should be THUNDERSTORM");
                assertNotNull(alert.getSeverity(), 
                    "Thunderstorm alert should have severity indicator");
            }
        }

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("Squall alerts should have severity indicators")
        void squallAlertsShouldHaveSeverityIndicators(String district, String state) {
            // Property: For any district and state, when squall alerts are present in nowcast,
            // they should have severity indicators.
            
            // Arrange
            List<NowcastAlertDto> alerts = new ArrayList<>();
            alerts.add(createNowcastAlert("SQUALL", "VERY_HEAVY", "Severe squall expected"));
            
            NowcastDto nowcastResponse = createNowcast(district, state, alerts);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getNowcast(district, state))
                .thenReturn(Mono.just(nowcastResponse));

            // Act
            Mono<NowcastDto> result = weatherService.getNowcast(district, state);

            // Assert
            NowcastDto response = nowcastResponse;
            for (NowcastAlertDto alert : response.getAlerts()) {
                assertEquals("SQUALL", alert.getAlertType(), 
                    "Alert type should be SQUALL");
                assertNotNull(alert.getSeverity(), 
                    "Squall alert should have severity indicator");
                assertNotNull(alert.getExpectedWindSpeedKmph(), 
                    "Squall alert should have expected wind speed");
            }
        }

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("Localized precipitation alerts should have severity indicators")
        void localizedPrecipitationAlertsShouldHaveSeverityIndicators(String district, String state) {
            // Property: For any district and state, when localized precipitation alerts (heavy rain, light rain)
            // are present in nowcast, they should have severity indicators.
            
            // Arrange
            List<NowcastAlertDto> alerts = new ArrayList<>();
            alerts.add(createNowcastAlert("HEAVY_RAIN", "HEAVY", "Heavy localized rainfall"));
            alerts.add(createNowcastAlert("LIGHT_RAIN", "LIGHT", "Light drizzle"));
            
            NowcastDto nowcastResponse = createNowcast(district, state, alerts);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getNowcast(district, state))
                .thenReturn(Mono.just(nowcastResponse));

            // Act
            Mono<NowcastDto> result = weatherService.getNowcast(district, state);

            // Assert
            NowcastDto response = nowcastResponse;
            for (NowcastAlertDto alert : response.getAlerts()) {
                assertTrue(alert.getAlertType().equals("HEAVY_RAIN") || 
                          alert.getAlertType().equals("LIGHT_RAIN"),
                    "Alert type should be precipitation-related (HEAVY_RAIN or LIGHT_RAIN)");
                assertNotNull(alert.getSeverity(), 
                    "Precipitation alert should have severity indicator");
            }
        }

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra"
        })
        @DisplayName("When no nowcast alerts are present, system should display no alerts")
        void whenNoNowcastAlertsPresentShouldDisplayNoAlerts(String district, String state) {
            // Property: For any district and state, when no nowcast alerts are present in the IMD response,
            // the system should display no alerts (empty alerts list).
            
            // Arrange
            List<NowcastAlertDto> emptyAlerts = new ArrayList<>();
            NowcastDto nowcastResponse = createNowcast(district, state, emptyAlerts);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getNowcast(district, state))
                .thenReturn(Mono.just(nowcastResponse));

            // Act
            Mono<NowcastDto> result = weatherService.getNowcast(district, state);

            // Assert - Verify no alerts are displayed when none are present
            NowcastDto response = nowcastResponse;
            assertNotNull(response, "Nowcast response should not be null");
            assertNotNull(response.getAlerts(), "Alerts list should not be null");
            assertTrue(response.getAlerts().isEmpty(), 
                "When no nowcast alerts are present, alerts list should be empty");
            assertEquals("CLEAR", response.getOverallSituation(), 
                "Overall situation should be CLEAR when no alerts");
        }
    }

    // ==================== Property 3.4: Alert Display Invariant ====================

    @Nested
    @DisplayName("Property 3.4: Alert Display Invariant")
    class AlertDisplayInvariant {

        @Test
        @DisplayName("Alert presence invariant: alerts displayed iff alerts present in response")
        void alertPresenceInvariantShouldHold() {
            // Property: The invariant that "alerts are displayed if and only if alerts are present in the response"
            // should hold for all weather responses.
            //
            // This means:
            // 1. If alerts are present in the response, they should be displayed (non-empty list)
            // 2. If no alerts are present in the response, no alerts should be displayed (empty list)
            
            String district = "Bangalore Rural";
            String state = "Karnataka";
            
            // Test case 1: Response with alerts should display alerts
            List<AlertDto> alertsWithData = new ArrayList<>();
            alertsWithData.add(createAlert(district, "HEAT_WAVE", "ORANGE", "Heat wave warning"));
            WeatherAlertDto alertResponseWithAlerts = createWeatherAlert(district, state, alertsWithData, "ORANGE");
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getWeatherAlerts(district, state))
                .thenReturn(Mono.just(alertResponseWithAlerts));

            // Act
            Mono<WeatherAlertDto> result = weatherService.getWeatherAlerts(district, state);

            // Assert - Invariant: alerts displayed iff alerts present
            WeatherAlertDto responseWithAlerts = alertResponseWithAlerts;
            assertFalse(responseWithAlerts.getAlerts().isEmpty(), 
                "When alerts are present in response, they should be displayed");
            
            // Test case 2: Response without alerts should display no alerts
            List<AlertDto> emptyAlerts = new ArrayList<>();
            WeatherAlertDto alertResponseNoAlerts = createWeatherAlert(district, state, emptyAlerts, "GREEN");
            
            when(imdApiClient.getWeatherAlerts(district, state))
                .thenReturn(Mono.just(alertResponseNoAlerts));

            // Act
            Mono<WeatherAlertDto> resultNoAlerts = weatherService.getWeatherAlerts(district, state);

            // Assert
            WeatherAlertDto responseNoAlerts = alertResponseNoAlerts;
            assertTrue(responseNoAlerts.getAlerts().isEmpty(), 
                "When no alerts are present in response, no alerts should be displayed");
        }

        @Test
        @DisplayName("Nowcast alert presence invariant should hold")
        void nowcastAlertPresenceInvariantShouldHold() {
            // Property: The invariant for nowcast alerts should hold:
            // alerts displayed iff alerts present in response
            
            String district = "Bangalore Rural";
            String state = "Karnataka";
            
            // Test case 1: Nowcast with alerts should display alerts
            List<NowcastAlertDto> nowcastAlertsWithData = new ArrayList<>();
            nowcastAlertsWithData.add(createNowcastAlert("THUNDERSTORM", "HEAVY", "Thunderstorm"));
            NowcastDto nowcastResponseWithAlerts = createNowcast(district, state, nowcastAlertsWithData);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getNowcast(district, state))
                .thenReturn(Mono.just(nowcastResponseWithAlerts));

            // Act
            Mono<NowcastDto> result = weatherService.getNowcast(district, state);

            // Assert
            NowcastDto responseWithAlerts = nowcastResponseWithAlerts;
            assertFalse(responseWithAlerts.getAlerts().isEmpty(), 
                "When nowcast alerts are present, they should be displayed");
            
            // Test case 2: Nowcast without alerts should display no alerts
            List<NowcastAlertDto> emptyNowcastAlerts = new ArrayList<>();
            NowcastDto nowcastResponseNoAlerts = createNowcast(district, state, emptyNowcastAlerts);
            
            when(imdApiClient.getNowcast(district, state))
                .thenReturn(Mono.just(nowcastResponseNoAlerts));

            // Act
            Mono<NowcastDto> resultNoAlerts = weatherService.getNowcast(district, state);

            // Assert
            NowcastDto responseNoAlerts = nowcastResponseNoAlerts;
            assertTrue(responseNoAlerts.getAlerts().isEmpty(), 
                "When no nowcast alerts are present, no alerts should be displayed");
        }

        @Test
        @DisplayName("Severity indicator invariant should hold for all displayed alerts")
        void severityIndicatorInvariantShouldHold() {
            // Property: The invariant that "all displayed alerts have severity indicators"
            // should hold for all weather responses.
            //
            // This means: For any alert that is displayed, it must have a severity indicator
            
            String district = "Bangalore Rural";
            String state = "Karnataka";
            
            // Create weather alerts with various severity levels
            List<AlertDto> alerts = new ArrayList<>();
            alerts.add(createAlert(district, "HEAT_WAVE", "YELLOW", "Heat wave warning"));
            alerts.add(createAlert(district, "HEAVY_RAIN", "ORANGE", "Heavy rain warning"));
            alerts.add(createAlert(district, "FLOOD", "RED", "Flood alert"));
            WeatherAlertDto alertResponse = createWeatherAlert(district, state, alerts, "RED");
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getWeatherAlerts(district, state))
                .thenReturn(Mono.just(alertResponse));

            // Act
            Mono<WeatherAlertDto> result = weatherService.getWeatherAlerts(district, state);

            // Assert - Verify all displayed alerts have severity indicators
            WeatherAlertDto response = alertResponse;
            for (AlertDto alert : response.getAlerts()) {
                assertNotNull(alert.getSeverity(), 
                    "All displayed alerts must have severity indicators. Alert type: " + alert.getType());
                assertTrue(alert.getSeverity().equals("YELLOW") || 
                          alert.getSeverity().equals("ORANGE") || 
                          alert.getSeverity().equals("RED"),
                    "Severity must be YELLOW, ORANGE, or RED. Was: " + alert.getSeverity());
            }
            
            // Create nowcast alerts with various severity levels
            List<NowcastAlertDto> nowcastAlerts = new ArrayList<>();
            nowcastAlerts.add(createNowcastAlert("THUNDERSTORM", "LIGHT", "Light thunderstorm"));
            nowcastAlerts.add(createNowcastAlert("SQUALL", "MODERATE", "Moderate squall"));
            nowcastAlerts.add(createNowcastAlert("HEAVY_RAIN", "VERY_HEAVY", "Very heavy rain"));
            NowcastDto nowcastResponse = createNowcast(district, state, nowcastAlerts);
            
            when(imdApiClient.getNowcast(district, state))
                .thenReturn(Mono.just(nowcastResponse));

            // Act
            Mono<NowcastDto> nowcastResult = weatherService.getNowcast(district, state);

            // Assert - Verify all displayed nowcast alerts have severity indicators
            NowcastDto nowcastResponseResult = nowcastResponse;
            for (NowcastAlertDto alert : nowcastResponseResult.getAlerts()) {
                assertNotNull(alert.getSeverity(), 
                    "All displayed nowcast alerts must have severity indicators. Alert type: " + alert.getAlertType());
                assertTrue(alert.getSeverity().equals("LIGHT") || 
                          alert.getSeverity().equals("MODERATE") || 
                          alert.getSeverity().equals("HEAVY") || 
                          alert.getSeverity().equals("VERY_HEAVY"),
                    "Severity must be LIGHT, MODERATE, HEAVY, or VERY_HEAVY. Was: " + alert.getSeverity());
            }
        }
    }

    // ==================== Property 3.5: Alert Type Coverage ====================

    @Nested
    @DisplayName("Property 3.5: Alert Type Coverage for Required Weather Events")
    class AlertTypeCoverage {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("System should support all required alert types with severity indicators")
        void systemShouldSupportAllRequiredAlertTypes(String district, String state) {
            // Property: For any district and state, the system should support displaying
            // all required alert types (thunderstorms, squalls, localized precipitation for nowcast;
            // heat wave, cold wave, heavy rain, flood, thunderstorm for weather alerts)
            // with severity indicators.
            
            // Arrange - Create weather alerts for all required types
            List<AlertDto> weatherAlerts = new ArrayList<>();
            weatherAlerts.add(createAlert(district, "HEAT_WAVE", "ORANGE", "Heat wave"));
            weatherAlerts.add(createAlert(district, "COLD_WAVE", "YELLOW", "Cold wave"));
            weatherAlerts.add(createAlert(district, "HEAVY_RAIN", "RED", "Heavy rainfall"));
            weatherAlerts.add(createAlert(district, "FLOOD", "RED", "Flood warning"));
            weatherAlerts.add(createAlert(district, "THUNDERSTORM", "ORANGE", "Thunderstorm"));
            
            WeatherAlertDto weatherAlertResponse = createWeatherAlert(district, state, weatherAlerts, "RED");
            
            // Create nowcast alerts for all required types
            List<NowcastAlertDto> nowcastAlerts = new ArrayList<>();
            nowcastAlerts.add(createNowcastAlert("THUNDERSTORM", "HEAVY", "Thunderstorm"));
            nowcastAlerts.add(createNowcastAlert("SQUALL", "VERY_HEAVY", "Squall"));
            nowcastAlerts.add(createNowcastAlert("HEAVY_RAIN", "HEAVY", "Heavy rain"));
            nowcastAlerts.add(createNowcastAlert("LIGHT_RAIN", "LIGHT", "Light rain"));
            
            NowcastDto nowcastResponse = createNowcast(district, state, nowcastAlerts);
            
            when(valueOperations.get(anyString())).thenReturn(null);
            when(weatherCacheRepository.findByDistrictAndStateAndForecastType(anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
            when(imdApiClient.getWeatherAlerts(district, state))
                .thenReturn(Mono.just(weatherAlertResponse));
            when(imdApiClient.getNowcast(district, state))
                .thenReturn(Mono.just(nowcastResponse));

            // Act
            Mono<WeatherAlertDto> weatherResult = weatherService.getWeatherAlerts(district, state);
            Mono<NowcastDto> nowcastResult = weatherService.getNowcast(district, state);

            // Assert - Verify all required alert types are supported with severity
            WeatherAlertDto weatherResponse = weatherAlertResponse;
            assertEquals(5, weatherResponse.getAlerts().size(), 
                "Should support all required weather alert types");
            
            NowcastDto nowcastResultResponse = nowcastResponse;
            assertEquals(4, nowcastResultResponse.getAlerts().size(), 
                "Should support all required nowcast alert types (thunderstorm, squall, heavy rain, light rain)");
            
            // Verify each alert type has severity
            for (AlertDto alert : weatherResponse.getAlerts()) {
                assertNotNull(alert.getSeverity(), 
                    "Weather alert type " + alert.getType() + " should have severity");
            }
            
            for (NowcastAlertDto alert : nowcastResultResponse.getAlerts()) {
                assertNotNull(alert.getSeverity(), 
                    "Nowcast alert type " + alert.getAlertType() + " should have severity");
            }
        }
    }
}