package com.farmer.weather.service;

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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WeatherCacheService.
 * Tests Redis caching behavior with cache-aside pattern.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherCacheServiceTest {

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

    @Test
    @DisplayName("Test getCachedSevenDayForecast returns cached data when available")
    void testGetCachedSevenDayForecastCacheHit() {
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

        String expectedKey = "weather:bangalore rural:karnataka:7DAY";
        when(valueOperations.get(expectedKey)).thenReturn(forecast);

        // Act
        Optional<SevenDayForecastDto> result = weatherCacheService.getCachedSevenDayForecast(district, state);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(district, result.get().getDistrict());
        assertEquals(1, result.get().getForecastDays().size());
    }

    @Test
    @DisplayName("Test getCachedSevenDayForecast returns empty when cache miss")
    void testGetCachedSevenDayForecastCacheMiss() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        String expectedKey = "weather:bangalore rural:karnataka:7DAY";
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // Act
        Optional<SevenDayForecastDto> result = weatherCacheService.getCachedSevenDayForecast(district, state);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test getCachedCurrentWeather returns cached data when available")
    void testGetCachedCurrentWeatherCacheHit() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        CurrentWeatherDto currentWeather = CurrentWeatherDto.builder()
            .district(district)
            .state(state)
            .observationTime(LocalDateTime.now())
            .cloudCoverage(4)
            .rainfall24HoursMm(5.0)
            .temperatureCelsius(28.0)
            .build();

        String expectedKey = "weather:bangalore rural:karnataka:CURRENT";
        when(valueOperations.get(expectedKey)).thenReturn(currentWeather);

        // Act
        Optional<CurrentWeatherDto> result = weatherCacheService.getCachedCurrentWeather(district, state);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(district, result.get().getDistrict());
        assertEquals(4, result.get().getCloudCoverage());
    }

    @Test
    @DisplayName("Test cacheSevenDayForecast stores data in Redis and MySQL")
    void testCacheSevenDayForecast() {
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

        String expectedKey = "weather:bangalore rural:karnataka:7DAY";
        when(redisTemplate.delete(expectedKey)).thenReturn(true);
        when(weatherCacheRepository.save(any(WeatherCache.class))).thenReturn(mock(WeatherCache.class));

        // Act
        weatherCacheService.cacheSevenDayForecast(district, state, forecast);

        // Assert
        verify(redisTemplate).delete(expectedKey);
        verify(redisTemplate.opsForValue()).set(eq(expectedKey), eq(forecast), eq(Duration.ofMinutes(30)));
        verify(weatherCacheRepository).save(any(WeatherCache.class));
    }

    @Test
    @DisplayName("Test invalidateCache removes all cache entries for district")
    void testInvalidateCache() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Act
        weatherCacheService.invalidateCache(district, state);

        // Assert
        verify(redisTemplate, times(6)).delete(anyString());
    }

    @Test
    @DisplayName("Test invalidateCache removes specific cache entry")
    void testInvalidateCacheSpecific() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";
        String forecastType = "7DAY";

        String expectedKey = "weather:bangalore rural:karnataka:7DAY";
        when(redisTemplate.delete(expectedKey)).thenReturn(true);

        // Act
        weatherCacheService.invalidateCache(district, state, forecastType);

        // Assert
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("Test isCacheValid returns true when cache exists")
    void testIsCacheValidTrue() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";
        String forecastType = "7DAY";

        String expectedKey = "weather:bangalore rural:karnataka:7DAY";
        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

        // Act
        boolean result = weatherCacheService.isCacheValid(district, state, forecastType);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Test isCacheValid returns false when cache does not exist")
    void testIsCacheValidFalse() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";
        String forecastType = "7DAY";

        String expectedKey = "weather:bangalore rural:karnataka:7DAY";
        when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

        // Act
        boolean result = weatherCacheService.isCacheValid(district, state, forecastType);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Test getRemainingTtl returns TTL when cache exists")
    void testGetRemainingTtl() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";
        String forecastType = "7DAY";

        String expectedKey = "weather:bangalore rural:karnataka:7DAY";
        when(redisTemplate.getExpire(expectedKey, TimeUnit.MINUTES)).thenReturn(25L);

        // Act
        Optional<Long> result = weatherCacheService.getRemainingTtl(district, state, forecastType);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(25L, result.get());
    }

    @Test
    @DisplayName("Test getRemainingTtl returns empty when cache does not exist")
    void testGetRemainingTtlEmpty() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";
        String forecastType = "7DAY";

        String expectedKey = "weather:bangalore rural:karnataka:7DAY";
        when(redisTemplate.getExpire(expectedKey, TimeUnit.MINUTES)).thenReturn(-2L);

        // Act
        Optional<Long> result = weatherCacheService.getRemainingTtl(district, state, forecastType);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test getCacheTimestamp returns timestamp when available")
    void testGetCacheTimestamp() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";
        String forecastType = "7DAY";

        String expectedKey = "weather:bangalore rural:karnataka:7DAY:timestamp";
        LocalDateTime timestamp = LocalDateTime.now();
        when(valueOperations.get(expectedKey)).thenReturn(timestamp.toString());

        // Act
        Optional<LocalDateTime> result = weatherCacheService.getCacheTimestamp(district, state, forecastType);

        // Assert
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Test cacheNowcast stores data in Redis and MySQL")
    void testCacheNowcast() {
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

        String expectedKey = "weather:bangalore rural:karnataka:NOWCAST";
        when(redisTemplate.delete(expectedKey)).thenReturn(true);
        when(weatherCacheRepository.save(any(WeatherCache.class))).thenReturn(mock(WeatherCache.class));

        // Act
        weatherCacheService.cacheNowcast(district, state, nowcast);

        // Assert
        verify(redisTemplate).delete(expectedKey);
        verify(redisTemplate.opsForValue()).set(eq(expectedKey), eq(nowcast), eq(Duration.ofMinutes(30)));
        verify(weatherCacheRepository).save(any(WeatherCache.class));
    }

    @Test
    @DisplayName("Test cacheWeatherAlerts stores data in Redis and MySQL")
    void testCacheWeatherAlerts() {
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

        String expectedKey = "weather:bangalore rural:karnataka:ALERTS";
        when(redisTemplate.delete(expectedKey)).thenReturn(true);
        when(weatherCacheRepository.save(any(WeatherCache.class))).thenReturn(mock(WeatherCache.class));

        // Act
        weatherCacheService.cacheWeatherAlerts(district, state, alerts);

        // Assert
        verify(redisTemplate).delete(expectedKey);
        verify(redisTemplate.opsForValue()).set(eq(expectedKey), eq(alerts), eq(Duration.ofMinutes(30)));
        verify(weatherCacheRepository).save(any(WeatherCache.class));
    }

    @Test
    @DisplayName("Test cacheRainfallStats stores data in Redis and MySQL")
    void testCacheRainfallStats() {
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

        String expectedKey = "weather:bangalore rural:karnataka:RAINFALL";
        when(redisTemplate.delete(expectedKey)).thenReturn(true);
        when(weatherCacheRepository.save(any(WeatherCache.class))).thenReturn(mock(WeatherCache.class));

        // Act
        weatherCacheService.cacheRainfallStats(district, state, stats);

        // Assert
        verify(redisTemplate).delete(expectedKey);
        verify(redisTemplate.opsForValue()).set(eq(expectedKey), eq(stats), eq(Duration.ofMinutes(30)));
        verify(weatherCacheRepository).save(any(WeatherCache.class));
    }

    @Test
    @DisplayName("Test cacheAgrometAdvisories stores data in Redis and MySQL")
    void testCacheAgrometAdvisories() {
        // Arrange
        String district = "Bangalore Rural";
        String state = "Karnataka";

        AgrometAdvisoryDto advisories = AgrometAdvisoryDto.builder()
            .district(district)
            .state(state)
            .issuedAt(LocalDateTime.now())
            .cropStage("VEGETATIVE")
            .majorCrop("Paddy")
            .build();

        String expectedKey = "weather:bangalore rural:karnataka:AGROMET";
        when(redisTemplate.delete(expectedKey)).thenReturn(true);
        when(weatherCacheRepository.save(any(WeatherCache.class))).thenReturn(mock(WeatherCache.class));

        // Act
        weatherCacheService.cacheAgrometAdvisories(district, state, advisories);

        // Assert
        verify(redisTemplate).delete(expectedKey);
        verify(redisTemplate.opsForValue()).set(eq(expectedKey), eq(advisories), eq(Duration.ofMinutes(30)));
        verify(weatherCacheRepository).save(any(WeatherCache.class));
    }
}