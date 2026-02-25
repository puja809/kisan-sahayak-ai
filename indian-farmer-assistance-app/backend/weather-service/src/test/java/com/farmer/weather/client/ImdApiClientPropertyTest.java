package com.farmer.weather.client;

import com.farmer.weather.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for IMD API Client parameter correctness.
 * 
 * Property 1: API Parameter Correctness
 * Validates: Requirements 1.1
 * 
 * These tests verify the following property:
 * For any farmer location (district, state), when requesting weather data from IMD API,
 * the system should call the API with the correct district parameter matching the farmer's location.
 * 
 * Requirements Reference:
 * - Requirement 1.1: WHEN a farmer requests weather information, THE Application SHALL 
 *   retrieve forecast data from IMD API for the farmer's district
 */
@ExtendWith(MockitoExtension.class)
class ImdApiClientPropertyTest {

    @Mock
    private WebClient imdWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ImdApiClient imdApiClient;

    @BeforeEach
    void setUp() {
        imdApiClient = new ImdApiClient(imdWebClient, 30, 60);
    }

    /**
     * Helper method to set up common mock behavior for all API methods.
     */
    private void setupCommonMocks() {
        when(imdWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    /**
     * Helper method to create a mock response DTO with the expected district.
     */
    @SuppressWarnings("unchecked")
    private <T> T createMockResponse(String district, String state, Class<T> dtoClass) {
        try {
            if (dtoClass == SevenDayForecastDto.class) {
                return dtoClass.cast(SevenDayForecastDto.builder()
                    .district(district)
                    .state(state)
                    .fetchedAt(LocalDateTime.now())
                    .forecastDays(List.of())
                    .build());
            } else if (dtoClass == CurrentWeatherDto.class) {
                return dtoClass.cast(CurrentWeatherDto.builder()
                    .district(district)
                    .state(state)
                    .observationTime(LocalDateTime.now())
                    .build());
            } else if (dtoClass == NowcastDto.class) {
                return dtoClass.cast(NowcastDto.builder()
                    .district(district)
                    .state(state)
                    .issuedAt(LocalDateTime.now())
                    .validUntil(LocalDateTime.now().plusHours(3))
                    .overallSituation("CLEAR")
                    .alerts(List.of())
                    .build());
            } else if (dtoClass == WeatherAlertDto.class) {
                return dtoClass.cast(WeatherAlertDto.builder()
                    .district(district)
                    .state(state)
                    .issuedAt(LocalDateTime.now())
                    .overallAlertLevel("GREEN")
                    .alerts(List.of())
                    .build());
            } else if (dtoClass == RainfallStatsDto.class) {
                return dtoClass.cast(RainfallStatsDto.builder()
                    .district(district)
                    .state(state)
                    .actualRainfallMm(100.0)
                    .normalRainfallMm(150.0)
                    .departurePercent(-33.3)
                    .departureCategory("DEFICIT")
                    .build());
            } else if (dtoClass == AgrometAdvisoryDto.class) {
                return dtoClass.cast(AgrometAdvisoryDto.builder()
                    .district(district)
                    .state(state)
                    .issuedAt(LocalDateTime.now())
                    .cropStage("SOWING")
                    .majorCrop("Paddy")
                    .build());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Nested
    @DisplayName("Property 1.1: Seven Day Forecast API District Parameter")
    class SevenDayForecastDistrictParameter {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Belgaum, Karnataka",
            "Dharwad, Karnataka",
            "Uttar Kannada, Karnataka",
            "Hyderabad, Telangana",
            "Warangal, Telangana",
            "Khammam, Telangana",
            "Pune, Maharashtra",
            "Nagpur, Maharashtra",
            "Ahmednagar, Maharashtra",
            "Nashik, Maharashtra",
            "Ludhiana, Punjab",
            "Amritsar, Punjab",
            "Bathinda, Punjab",
            "Jaipur, Rajasthan",
            "Jodhpur, Rajasthan",
            "Udaipur, Rajasthan",
            "Bikaner, Rajasthan",
            "Allahabad, Uttar Pradesh",
            "Lucknow, Uttar Pradesh",
            "Kanpur, Uttar Pradesh",
            "Varanasi, Uttar Pradesh",
            "Gwalior, Madhya Pradesh",
            "Indore, Madhya Pradesh",
            "Bhopal, Madhya Pradesh",
            "Jabalpur, Madhya Pradesh",
            "Chennai, Tamil Nadu",
            "Coimbatore, Tamil Nadu",
            "Madurai, Tamil Nadu",
            "Tiruchirappalli, Tamil Nadu",
            "Kolkata, West Bengal",
            "Howrah, West Bengal",
            "Darjeeling, West Bengal",
            "Murshidabad, West Bengal",
            "Ahmedabad, Gujarat",
            "Surat, Gujarat",
            "Vadodara, Gujarat",
            "Rajkot, Gujarat",
            "Bhubaneswar, Odisha",
            "Cuttack, Odisha",
            "Rourkela, Odisha",
            "Puri, Odisha",
            "Ranchi, Jharkhand",
            "Jamshedpur, Jharkhand",
            "Dhanbad, Jharkhand",
            "Hazaribagh, Jharkhand",
            "Patna, Bihar",
            "Gaya, Bihar",
            "Muzaffarpur, Bihar",
            "Bhagalpur, Bihar"
        })
        @DisplayName("IMD API should return forecast data with correct district matching farmer's location")
        void imdApiShouldReturnCorrectDistrictForForecast(String district, String state) {
            // Property: For any farmer location (district, state), when requesting weather data 
            // from IMD API, the system should call the API with the correct district parameter 
            // matching the farmer's location.
            // 
            // This test verifies that the IMD API client correctly passes the district parameter
            // by checking that the response contains the same district that was requested.
            
            // Arrange
            SevenDayForecastDto mockResponse = createMockResponse(district, state, SevenDayForecastDto.class);
            assertNotNull(mockResponse, "Mock response should be created");

            setupCommonMocks();
            when(responseSpec.bodyToMono(SevenDayForecastDto.class)).thenReturn(Mono.just(mockResponse));

            // Act
            Mono<SevenDayForecastDto> result = imdApiClient.getSevenDayForecast(district, state);

            // Assert - Verify the API is called with correct district parameter
            // by checking that the response district matches the requested district
            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    // The key property: response district must match the requested district
                    assertEquals(district, response.getDistrict(), 
                        "Response district should match requested district. " +
                        "This verifies the API was called with the correct district parameter.");
                    assertEquals(state, response.getState(), 
                        "Response state should match requested state.");
                    return true;
                })
                .verifyComplete();

            // Verify the WebClient was called with the district parameter
            verify(requestHeadersUriSpec).uri(contains("/{district}"), eq(district));
        }

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("Multiple consecutive API calls should all return correct district")
        void multipleConsecutiveApiCallsShouldReturnCorrectDistrict(String district, String state) {
            // Property: For any farmer location, all API calls should use the correct district parameter
            
            // Arrange
            SevenDayForecastDto mockResponse = createMockResponse(district, state, SevenDayForecastDto.class);
            assertNotNull(mockResponse, "Mock response should be created");

            setupCommonMocks();
            when(responseSpec.bodyToMono(SevenDayForecastDto.class)).thenReturn(Mono.just(mockResponse));

            // Act - Make multiple consecutive calls
            Mono<SevenDayForecastDto> result1 = imdApiClient.getSevenDayForecast(district, state);
            Mono<SevenDayForecastDto> result2 = imdApiClient.getSevenDayForecast(district, state);
            Mono<SevenDayForecastDto> result3 = imdApiClient.getSevenDayForecast(district, state);

            // Assert - All calls should complete successfully with correct district
            StepVerifier.create(result1)
                .expectNextMatches(response -> district.equals(response.getDistrict()))
                .verifyComplete();

            StepVerifier.create(result2)
                .expectNextMatches(response -> district.equals(response.getDistrict()))
                .verifyComplete();

            StepVerifier.create(result3)
                .expectNextMatches(response -> district.equals(response.getDistrict()))
                .verifyComplete();

            // Verify all three calls used the correct district
            verify(requestHeadersUriSpec, times(3)).uri(contains("/{district}"), eq(district));
        }
    }

    @Nested
    @DisplayName("Property 1.2: Current Weather API District Parameter")
    class CurrentWeatherDistrictParameter {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Belgaum, Karnataka",
            "Pune, Maharashtra",
            "Nagpur, Maharashtra"
        })
        @DisplayName("IMD API should return current weather with correct district")
        void imdApiShouldReturnCorrectDistrictForCurrentWeather(String district, String state) {
            // Property: For any farmer location, the current weather API should be called 
            // with the correct district parameter.
            
            // Arrange
            CurrentWeatherDto mockResponse = createMockResponse(district, state, CurrentWeatherDto.class);
            assertNotNull(mockResponse, "Mock response should be created");

            setupCommonMocks();
            when(responseSpec.bodyToMono(CurrentWeatherDto.class)).thenReturn(Mono.just(mockResponse));

            // Act
            Mono<CurrentWeatherDto> result = imdApiClient.getCurrentWeather(district, state);

            // Assert
            StepVerifier.create(result)
                .expectNextMatches(response -> 
                    district.equals(response.getDistrict()))
                .verifyComplete();

            // Verify the WebClient was called with the district parameter
            verify(requestHeadersUriSpec).uri(contains("/{district}"), eq(district));
        }
    }

    @Nested
    @DisplayName("Property 1.3: Nowcast API District Parameter")
    class NowcastDistrictParameter {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra"
        })
        @DisplayName("IMD API should return nowcast with correct district")
        void imdApiShouldReturnCorrectDistrictForNowcast(String district, String state) {
            // Property: For any farmer location, the nowcast API should be called 
            // with the correct district parameter.
            
            // Arrange
            NowcastDto mockResponse = createMockResponse(district, state, NowcastDto.class);
            assertNotNull(mockResponse, "Mock response should be created");

            setupCommonMocks();
            when(responseSpec.bodyToMono(NowcastDto.class)).thenReturn(Mono.just(mockResponse));

            // Act
            Mono<NowcastDto> result = imdApiClient.getNowcast(district, state);

            // Assert
            StepVerifier.create(result)
                .expectNextMatches(response -> 
                    district.equals(response.getDistrict()))
                .verifyComplete();

            // Verify the WebClient was called with the district parameter
            verify(requestHeadersUriSpec).uri(contains("/{district}"), eq(district));
        }
    }

    @Nested
    @DisplayName("Property 1.4: Weather Alerts API District Parameter")
    class WeatherAlertsDistrictParameter {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Belgaum, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra"
        })
        @DisplayName("IMD API should return weather alerts with correct district")
        void imdApiShouldReturnCorrectDistrictForWeatherAlerts(String district, String state) {
            // Property: For any farmer location, the weather alerts API should be called 
            // with the correct district parameter.
            
            // Arrange
            WeatherAlertDto mockResponse = createMockResponse(district, state, WeatherAlertDto.class);
            assertNotNull(mockResponse, "Mock response should be created");

            setupCommonMocks();
            when(responseSpec.bodyToMono(WeatherAlertDto.class)).thenReturn(Mono.just(mockResponse));

            // Act
            Mono<WeatherAlertDto> result = imdApiClient.getWeatherAlerts(district, state);

            // Assert
            StepVerifier.create(result)
                .expectNextMatches(response -> 
                    district.equals(response.getDistrict()))
                .verifyComplete();

            // Verify the WebClient was called with the district parameter
            verify(requestHeadersUriSpec).uri(contains("/{district}"), eq(district));
        }
    }

    @Nested
    @DisplayName("Property 1.5: Rainfall Stats API District Parameter")
    class RainfallStatsDistrictParameter {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Belgaum, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra"
        })
        @DisplayName("IMD API should return rainfall stats with correct district")
        void imdApiShouldReturnCorrectDistrictForRainfallStats(String district, String state) {
            // Property: For any farmer location, the rainfall stats API should be called 
            // with the correct district parameter.
            
            // Arrange
            RainfallStatsDto mockResponse = createMockResponse(district, state, RainfallStatsDto.class);
            assertNotNull(mockResponse, "Mock response should be created");

            setupCommonMocks();
            when(responseSpec.bodyToMono(RainfallStatsDto.class)).thenReturn(Mono.just(mockResponse));

            // Act
            Mono<RainfallStatsDto> result = imdApiClient.getRainfallStats(district, state);

            // Assert
            StepVerifier.create(result)
                .expectNextMatches(response -> 
                    district.equals(response.getDistrict()))
                .verifyComplete();

            // Verify the WebClient was called with the district parameter
            verify(requestHeadersUriSpec).uri(contains("/{district}"), eq(district));
        }
    }

    @Nested
    @DisplayName("Property 1.6: Agromet Advisories API District Parameter")
    class AgrometAdvisoriesDistrictParameter {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Belgaum, Karnataka",
            "Hyderabad, Telangana",
            "Pune, Maharashtra"
        })
        @DisplayName("IMD API should return agromet advisories with correct district")
        void imdApiShouldReturnCorrectDistrictForAgrometAdvisories(String district, String state) {
            // Property: For any farmer location, the agromet advisories API should be called 
            // with the correct district parameter.
            
            // Arrange
            AgrometAdvisoryDto mockResponse = createMockResponse(district, state, AgrometAdvisoryDto.class);
            assertNotNull(mockResponse, "Mock response should be created");

            setupCommonMocks();
            when(responseSpec.bodyToMono(AgrometAdvisoryDto.class)).thenReturn(Mono.just(mockResponse));

            // Act
            Mono<AgrometAdvisoryDto> result = imdApiClient.getAgrometAdvisories(district, state);

            // Assert
            StepVerifier.create(result)
                .expectNextMatches(response -> 
                    district.equals(response.getDistrict()))
                .verifyComplete();

            // Verify the WebClient was called with the district parameter
            verify(requestHeadersUriSpec).uri(contains("/{district}"), eq(district));
        }
    }

    @Nested
    @DisplayName("Property 1.7: District Parameter Consistency Across All API Methods")
    class DistrictParameterConsistency {

        @ParameterizedTest
        @CsvSource({
            "Bangalore Rural, Karnataka",
            "Mysore, Karnataka",
            "Hyderabad, Telangana"
        })
        @DisplayName("All IMD API methods should return the same district parameter for a given location")
        void allApiMethodsShouldReturnSameDistrictParameter(String district, String state) {
            // Property: For any farmer location, all API methods should use the same 
            // district parameter consistently.
            
            // Arrange - Set up mocks for all API methods
            when(imdWebClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

            // Create mock responses for all API methods
            SevenDayForecastDto forecastResponse = createMockResponse(district, state, SevenDayForecastDto.class);
            CurrentWeatherDto currentWeatherResponse = createMockResponse(district, state, CurrentWeatherDto.class);
            NowcastDto nowcastResponse = createMockResponse(district, state, NowcastDto.class);
            WeatherAlertDto alertsResponse = createMockResponse(district, state, WeatherAlertDto.class);
            RainfallStatsDto rainfallResponse = createMockResponse(district, state, RainfallStatsDto.class);
            AgrometAdvisoryDto agrometResponse = createMockResponse(district, state, AgrometAdvisoryDto.class);

            // Act - Call all API methods with the same district
            Mono<SevenDayForecastDto> forecastResult = imdApiClient.getSevenDayForecast(district, state);
            Mono<CurrentWeatherDto> currentWeatherResult = imdApiClient.getCurrentWeather(district, state);
            Mono<NowcastDto> nowcastResult = imdApiClient.getNowcast(district, state);
            Mono<WeatherAlertDto> alertsResult = imdApiClient.getWeatherAlerts(district, state);
            Mono<RainfallStatsDto> rainfallResult = imdApiClient.getRainfallStats(district, state);
            Mono<AgrometAdvisoryDto> agrometResult = imdApiClient.getAgrometAdvisories(district, state);

            // Configure responses
            when(responseSpec.bodyToMono(SevenDayForecastDto.class)).thenReturn(Mono.just(forecastResponse));
            when(responseSpec.bodyToMono(CurrentWeatherDto.class)).thenReturn(Mono.just(currentWeatherResponse));
            when(responseSpec.bodyToMono(NowcastDto.class)).thenReturn(Mono.just(nowcastResponse));
            when(responseSpec.bodyToMono(WeatherAlertDto.class)).thenReturn(Mono.just(alertsResponse));
            when(responseSpec.bodyToMono(RainfallStatsDto.class)).thenReturn(Mono.just(rainfallResponse));
            when(responseSpec.bodyToMono(AgrometAdvisoryDto.class)).thenReturn(Mono.just(agrometResponse));

            // Assert - All responses should contain the same district
            StepVerifier.create(forecastResult)
                .expectNextMatches(response -> district.equals(response.getDistrict()))
                .verifyComplete();

            StepVerifier.create(currentWeatherResult)
                .expectNextMatches(response -> district.equals(response.getDistrict()))
                .verifyComplete();

            StepVerifier.create(nowcastResult)
                .expectNextMatches(response -> district.equals(response.getDistrict()))
                .verifyComplete();

            StepVerifier.create(alertsResult)
                .expectNextMatches(response -> district.equals(response.getDistrict()))
                .verifyComplete();

            StepVerifier.create(rainfallResult)
                .expectNextMatches(response -> district.equals(response.getDistrict()))
                .verifyComplete();

            StepVerifier.create(agrometResult)
                .expectNextMatches(response -> district.equals(response.getDistrict()))
                .verifyComplete();

            // Verify all API calls used the correct district parameter
            verify(requestHeadersUriSpec, times(6)).uri(contains("/{district}"), eq(district));
        }
    }

    @Nested
    @DisplayName("Property 1.8: District Parameter Not Mixed Between Different Locations")
    class DistrictParameterIsolation {

        @Test
        @DisplayName("API calls for different districts should not mix up district parameters")
        void apiCallsForDifferentDistrictsShouldNotMixParameters() {
            // Property: For different farmer locations, the API should be called with 
            // the correct district for each location, not mixing them up.
            
            String district1 = "Bangalore Rural";
            String district2 = "Mysore";
            String state = "Karnataka";

            // Arrange - Set up mocks
            when(imdWebClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

            // Create responses for each district
            SevenDayForecastDto response1 = createMockResponse(district1, state, SevenDayForecastDto.class);
            SevenDayForecastDto response2 = createMockResponse(district2, state, SevenDayForecastDto.class);

            // Act - Call API for district1, then district2
            when(responseSpec.bodyToMono(SevenDayForecastDto.class))
                .thenReturn(Mono.just(response1))
                .thenReturn(Mono.just(response2));

            Mono<SevenDayForecastDto> result1 = imdApiClient.getSevenDayForecast(district1, state);
            Mono<SevenDayForecastDto> result2 = imdApiClient.getSevenDayForecast(district2, state);

            // Assert - Each call should return data for the correct district
            StepVerifier.create(result1)
                .expectNextMatches(response -> 
                    district1.equals(response.getDistrict()))
                .verifyComplete();

            StepVerifier.create(result2)
                .expectNextMatches(response -> 
                    district2.equals(response.getDistrict()))
                .verifyComplete();

            // Verify each district was used in its respective call
            verify(requestHeadersUriSpec).uri(contains("/{district}"), eq(district1));
            verify(requestHeadersUriSpec).uri(contains("/{district}"), eq(district2));
        }
    }
}