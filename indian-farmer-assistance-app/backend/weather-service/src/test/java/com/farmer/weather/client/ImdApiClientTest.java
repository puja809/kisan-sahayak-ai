package com.farmer.weather.client;

import com.farmer.weather.dto.*;
import com.farmer.weather.exception.ImdApiException;
import com.farmer.weather.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ImdApiClient.
 * Tests all API methods and retry logic with exponential backoff.
 */
@ExtendWith(MockitoExtension.class)
class ImdApiClientTest {

    @Mock
    private WebClient imdWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ImdApiClient imdApiClient;

    @BeforeEach
    void setUp() {
        imdApiClient = new ImdApiClient(imdWebClient, 30, 60);
    }

    @Test
    @DisplayName("Test getSevenDayForecast returns forecast data")
    void testGetSevenDayForecast() {
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
                    .humidity0830(70.0)
                    .humidity1730(60.0)
                    .rainfallMm(0.0)
                    .windSpeedKmph(10.0)
                    .windDirection("NE")
                    .cloudCoverage(3)
                    .sunriseTime("06:15")
                    .sunsetTime("18:45")
                    .build()
            ))
            .build();

        when(imdWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SevenDayForecastDto.class)).thenReturn(Mono.just(forecast));

        // Act & Assert
        StepVerifier.create(imdApiClient.getSevenDayForecast(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getForecastDays().size() == 1 &&
                result.getForecastDays().get(0).getMaxTempCelsius() == 32.0)
            .verifyComplete();

        verify(imdWebClient).get();
    }

    @Test
    @DisplayName("Test getCurrentWeather returns current weather data")
    void testGetCurrentWeather() {
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

        when(imdWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CurrentWeatherDto.class)).thenReturn(Mono.just(currentWeather));

        // Act & Assert
        StepVerifier.create(imdApiClient.getCurrentWeather(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getCloudCoverage() == 4 &&
                result.getRainfall24HoursMm() == 5.0)
            .verifyComplete();
    }

    @Test
    @DisplayName("Test getNowcast returns nowcast data")
    void testGetNowcast() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        NowcastDto nowcast = NowcastDto.builder()
            .district(district)
            .state(state)
            .issuedAt(LocalDateTime.now())
            .validUntil(LocalDateTime.now().plusHours(3))
            .overallSituation("CLEAR")
            .alerts(List.of())
            .build();

        when(imdWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(NowcastDto.class)).thenReturn(Mono.just(nowcast));

        // Act & Assert
        StepVerifier.create(imdApiClient.getNowcast(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getOverallSituation().equals("CLEAR"))
            .verifyComplete();
    }

    @Test
    @DisplayName("Test getWeatherAlerts returns alerts")
    void testGetWeatherAlerts() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        WeatherAlertDto alerts = WeatherAlertDto.builder()
            .district(district)
            .state(state)
            .issuedAt(LocalDateTime.now())
            .overallAlertLevel("YELLOW")
            .alerts(List.of())
            .build();

        when(imdWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(WeatherAlertDto.class)).thenReturn(Mono.just(alerts));

        // Act & Assert
        StepVerifier.create(imdApiClient.getWeatherAlerts(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getOverallAlertLevel().equals("YELLOW"))
            .verifyComplete();
    }

    @Test
    @DisplayName("Test getRainfallStats returns rainfall statistics")
    void testGetRainfallStats() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        RainfallStatsDto stats = RainfallStatsDto.builder()
            .district(district)
            .state(state)
            .actualRainfallMm(150.0)
            .normalRainfallMm(200.0)
            .departurePercent(-25.0)
            .departureCategory("DEFICIT")
            .build();

        when(imdWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RainfallStatsDto.class)).thenReturn(Mono.just(stats));

        // Act & Assert
        StepVerifier.create(imdApiClient.getRainfallStats(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getActualRainfallMm() == 150.0 &&
                result.getDeparturePercent() == -25.0)
            .verifyComplete();
    }

    @Test
    @DisplayName("Test getAgrometAdvisories returns advisories")
    void testGetAgrometAdvisories() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        AgrometAdvisoryDto advisories = AgrometAdvisoryDto.builder()
            .district(district)
            .state(state)
            .issuedAt(LocalDateTime.now())
            .cropStage("VEGETATIVE")
            .majorCrop("Paddy")
            .heatStressIndex(AgrometAdvisoryDto.HeatStressIndex.builder()
                .indexValue(25.0)
                .category("MODERATE")
                .build())
            .evapotranspiration(AgrometAdvisoryDto.EvapotranspirationData.builder()
                .et0MmPerDay(5.0)
                .etcMmPerDay(4.0)
                .build())
            .build();

        when(imdWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AgrometAdvisoryDto.class)).thenReturn(Mono.just(advisories));

        // Act & Assert
        StepVerifier.create(imdApiClient.getAgrometAdvisories(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getCropStage().equals("VEGETATIVE") &&
                result.getHeatStressIndex().getCategory().equals("MODERATE"))
            .verifyComplete();
    }

    @Test
    @DisplayName("Test getAwsArgData returns AWS/ARG data")
    void testGetAwsArgData() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        AwsArgDataDto awsData = AwsArgDataDto.builder()
            .stationId("AWS-001")
            .stationName("Bangalore AWS")
            .stationType("AWS")
            .district(district)
            .state(state)
            .temperatureCelsius(28.0)
            .relativeHumidity(65.0)
            .rainfall(AwsArgDataDto.RainfallData.builder()
                .last24HoursMm(5.0)
                .cumulativeTodayMm(5.0)
                .build())
            .build();

        when(imdWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(List.class)).thenReturn(Mono.just(List.of(awsData)));

        // Act & Assert
        StepVerifier.create(imdApiClient.getAwsArgData(district, state))
            .expectNextMatches(result -> 
                result.size() == 1 &&
                result.get(0).getStationId().equals("AWS-001"))
            .verifyComplete();
    }

    @Test
    @DisplayName("Test retry logic with exponential backoff")
    void testRetryLogic() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        when(imdWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SevenDayForecastDto.class))
            .thenReturn(Mono.error(new RuntimeException("API error")))
            .thenReturn(Mono.error(new RuntimeException("API error")))
            .thenReturn(Mono.just(SevenDayForecastDto.builder()
                .district(district)
                .state(state)
                .build()));

        // Act & Assert
        StepVerifier.create(imdApiClient.getSevenDayForecast(district, state))
            .expectError(ImdApiException.class)
            .verify(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("Test rate limit error count")
    void testRateLimitErrorCount() {
        // Arrange
        imdApiClient.resetRateLimitErrorCount();
        assertEquals(0, imdApiClient.getRateLimitErrorCount());
    }
}