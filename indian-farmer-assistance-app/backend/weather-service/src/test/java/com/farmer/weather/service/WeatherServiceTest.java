package com.farmer.weather.service;

import com.farmer.weather.client.WeatherApiClient;
import com.farmer.weather.dto.*;
import com.farmer.weather.entity.WeatherCache;
import com.farmer.weather.repository.WeatherCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WeatherService.
 * Tests service methods and caching behavior with Redis cache-aside pattern.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherServiceTest {

    @Mock
    private WeatherApiClient weatherApiClient;

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
        weatherService = new WeatherService(weatherApiClient, weatherCacheService, weatherCacheRepository);
    }

    @Test
    @DisplayName("Test getSevenDayForecast returns fresh data from API when cache miss")
    void testGetSevenDayForecastFromApi() {
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
        when(weatherApiClient.getSevenDayForecast(anyString(), anyString()))
            .thenReturn(Mono.just(forecast));

        // Act & Assert
        StepVerifier.create(weatherService.getSevenDayForecast(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getForecastDays().size() == 1)
            .verifyComplete();

        verify(weatherApiClient).getSevenDayForecast(district, state);
        verify(weatherCacheService).cacheSevenDayForecast(eq(district), eq(state), any(SevenDayForecastDto.class));
    }

    @Test
    @DisplayName("Test getSevenDayForecast returns cached data when available in Redis")
    void testGetSevenDayForecastFromCache() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        SevenDayForecastDto cachedForecast = SevenDayForecastDto.builder()
            .district(district)
            .state(state)
            .fetchedAt(LocalDateTime.now().minusMinutes(10))
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
                result.getCacheTimestamp() != null)
            .verifyComplete();

        verify(weatherApiClient, never()).getSevenDayForecast(anyString(), anyString());
    }

    @Test
    @DisplayName("Test getCurrentWeather returns current weather data from API when cache miss")
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
            .temperatureCelsius(28.0)
            .build();

        when(weatherCacheService.getCachedCurrentWeather(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(weatherApiClient.getCurrentWeather(anyString(), anyString()))
            .thenReturn(Mono.just(currentWeather));

        // Act & Assert
        StepVerifier.create(weatherService.getCurrentWeather(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getCloudCoverage() == 4 &&
                result.getRainfall24HoursMm() == 5.0)
            .verifyComplete();
    }

    @Test
    @DisplayName("Test getCurrentWeather returns cached data when available in Redis")
    void testGetCurrentWeatherFromCache() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        CurrentWeatherDto cachedWeather = CurrentWeatherDto.builder()
            .district(district)
            .state(state)
            .observationTime(LocalDateTime.now().minusMinutes(10))
            .cloudCoverage(4)
            .rainfall24HoursMm(5.0)
            .temperatureCelsius(28.0)
            .build();

        when(weatherCacheService.getCachedCurrentWeather(anyString(), anyString()))
            .thenReturn(Optional.of(cachedWeather));

        // Act & Assert
        StepVerifier.create(weatherService.getCurrentWeather(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getCacheTimestamp() != null)
            .verifyComplete();

        verify(weatherApiClient, never()).getCurrentWeather(anyString(), anyString());
    }

    @Test
    @DisplayName("Test getNowcast returns nowcast data from API when cache miss")
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

        when(weatherCacheService.getCachedNowcast(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(weatherApiClient.getNowcast(anyString(), anyString()))
            .thenReturn(Mono.just(nowcast));

        // Act & Assert
        StepVerifier.create(weatherService.getNowcast(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getOverallSituation().equals("CLEAR"))
            .verifyComplete();
    }

    @Test
    @DisplayName("Test getNowcast returns cached data when available in Redis")
    void testGetNowcastFromCache() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        NowcastDto cachedNowcast = NowcastDto.builder()
            .district(district)
            .state(state)
            .issuedAt(LocalDateTime.now().minusMinutes(10))
            .validUntil(LocalDateTime.now().plusHours(3))
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

        verify(weatherApiClient, never()).getNowcast(anyString(), anyString());
    }

    @Test
    @DisplayName("Test getWeatherAlerts returns alerts from API when cache miss")
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

        when(weatherCacheService.getCachedWeatherAlerts(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(weatherApiClient.getWeatherAlerts(anyString(), anyString()))
            .thenReturn(Mono.just(alerts));

        // Act & Assert
        StepVerifier.create(weatherService.getWeatherAlerts(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getOverallAlertLevel().equals("YELLOW"))
            .verifyComplete();
    }

    @Test
    @DisplayName("Test getWeatherAlerts returns cached data when available in Redis")
    void testGetWeatherAlertsFromCache() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        WeatherAlertDto cachedAlerts = WeatherAlertDto.builder()
            .district(district)
            .state(state)
            .issuedAt(LocalDateTime.now().minusMinutes(10))
            .overallAlertLevel("YELLOW")
            .alerts(List.of())
            .build();

        when(weatherCacheService.getCachedWeatherAlerts(anyString(), anyString()))
            .thenReturn(Optional.of(cachedAlerts));

        // Act & Assert
        StepVerifier.create(weatherService.getWeatherAlerts(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getCacheTimestamp() != null)
            .verifyComplete();

        verify(weatherApiClient, never()).getWeatherAlerts(anyString(), anyString());
    }

    @Test
    @DisplayName("Test getRainfallStats returns rainfall statistics from API when cache miss")
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

        when(weatherCacheService.getCachedRainfallStats(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(weatherApiClient.getRainfallStats(anyString(), anyString()))
            .thenReturn(Mono.just(stats));

        // Act & Assert
        StepVerifier.create(weatherService.getRainfallStats(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getActualRainfallMm() == 150.0 &&
                result.getDeparturePercent() == -25.0)
            .verifyComplete();
    }

    @Test
    @DisplayName("Test getRainfallStats returns cached data when available in Redis")
    void testGetRainfallStatsFromCache() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        RainfallStatsDto cachedStats = RainfallStatsDto.builder()
            .district(district)
            .state(state)
            .actualRainfallMm(150.0)
            .normalRainfallMm(200.0)
            .departurePercent(-25.0)
            .departureCategory("DEFICIT")
            .build();

        when(weatherCacheService.getCachedRainfallStats(anyString(), anyString()))
            .thenReturn(Optional.of(cachedStats));

        // Act & Assert
        StepVerifier.create(weatherService.getRainfallStats(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getCacheTimestamp() != null)
            .verifyComplete();

        verify(weatherApiClient, never()).getRainfallStats(anyString(), anyString());
    }

    @Test
    @DisplayName("Test getAgrometAdvisories returns advisories from API when cache miss")
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

        when(weatherCacheService.getCachedAgrometAdvisories(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(weatherApiClient.getAgrometAdvisories(anyString(), anyString()))
            .thenReturn(Mono.just(advisories));

        // Act & Assert
        StepVerifier.create(weatherService.getAgrometAdvisories(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getCropStage().equals("VEGETATIVE") &&
                result.getHeatStressIndex().getCategory().equals("MODERATE"))
            .verifyComplete();
    }

    @Test
    @DisplayName("Test getAgrometAdvisories returns cached data when available in Redis")
    void testGetAgrometAdvisoriesFromCache() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        AgrometAdvisoryDto cachedAdvisories = AgrometAdvisoryDto.builder()
            .district(district)
            .state(state)
            .issuedAt(LocalDateTime.now().minusMinutes(10))
            .cropStage("VEGETATIVE")
            .majorCrop("Paddy")
            .build();

        when(weatherCacheService.getCachedAgrometAdvisories(anyString(), anyString()))
            .thenReturn(Optional.of(cachedAdvisories));

        // Act & Assert
        StepVerifier.create(weatherService.getAgrometAdvisories(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getCacheTimestamp() != null)
            .verifyComplete();

        verify(weatherApiClient, never()).getAgrometAdvisories(anyString(), anyString());
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
            .rainfall(AwsArgDataDto.RainfallData.builder()
                .last24HoursMm(5.0)
                .build())
            .build();

        when(weatherApiClient.getAwsArgData(anyString(), anyString()))
            .thenReturn(Mono.just(List.of(awsData)));

        // Act & Assert
        StepVerifier.create(weatherService.getAwsArgData(district, state))
            .expectNextMatches(result -> 
                result.size() == 1 &&
                result.get(0).getStationId().equals("AWS-001"))
            .verifyComplete();
    }

    @Test
    @DisplayName("Test API error returns stale cache from MySQL")
    void testApiErrorReturnsStaleCache() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        WeatherCache cache = WeatherCache.builder()
            .district(district)
            .state(state)
            .forecastType("7DAY")
            .cachedData("cached forecast data")
            .fetchedAt(LocalDateTime.now().minusHours(2))
            .build();

        when(weatherCacheService.getCachedSevenDayForecast(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(weatherApiClient.getSevenDayForecast(anyString(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("API error")));
        when(weatherCacheRepository.findByDistrictAndStateAndForecastType(
            anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(cache));

        // Act & Assert
        StepVerifier.create(weatherService.getSevenDayForecast(district, state))
            .expectNextMatches(result -> 
                result.getDistrict().equals(district) &&
                result.getCacheTimestamp() != null)
            .verifyComplete();
    }
}