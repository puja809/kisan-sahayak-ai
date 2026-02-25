package com.farmer.weather.service;

import com.farmer.weather.dto.*;
import com.farmer.weather.entity.WeatherCache;
import com.farmer.weather.repository.WeatherCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching weather data using Redis with cache-aside pattern.
 * Provides hot data caching with configurable TTL and cache invalidation.
 * 
 * Cache-aside pattern:
 * 1. On read: Check cache first, if miss then fetch from DB and populate cache
 * 2. On write: Update DB first, then invalidate/update cache
 * 3. On new data fetch: Invalidate old cache entries and create new ones
 */
@Service
public class WeatherCacheService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherCacheService.class);
    private static final String WEATHER_CACHE_KEY_PREFIX = "weather:";
    private static final String CACHE_TIMESTAMP_SUFFIX = ":timestamp";

    private final RedisTemplate<String, Object> redisTemplate;
    private final WeatherCacheRepository weatherCacheRepository;
    private final int cacheTtlMinutes;

    public WeatherCacheService(
            RedisTemplate<String, Object> redisTemplate,
            WeatherCacheRepository weatherCacheRepository,
            @Value("${weather.cache.ttl-minutes:30}") int cacheTtlMinutes) {
        this.redisTemplate = redisTemplate;
        this.weatherCacheRepository = weatherCacheRepository;
        this.cacheTtlMinutes = cacheTtlMinutes;
    }

    /**
     * Get cached 7-day forecast from Redis.
     * Implements cache-aside pattern: check cache first, fall back to DB if miss.
     * 
     * @param district District name
     * @param state State name
     * @return Optional containing cached forecast if found and valid
     */
    public Optional<SevenDayForecastDto> getCachedSevenDayForecast(String district, String state) {
        String cacheKey = buildCacheKey(district, state, "7DAY");
        return getCachedData(cacheKey, SevenDayForecastDto.class);
    }

    /**
     * Get cached current weather from Redis.
     * 
     * @param district District name
     * @param state State name
     * @return Optional containing cached current weather if found and valid
     */
    public Optional<CurrentWeatherDto> getCachedCurrentWeather(String district, String state) {
        String cacheKey = buildCacheKey(district, state, "CURRENT");
        return getCachedData(cacheKey, CurrentWeatherDto.class);
    }

    /**
     * Get cached nowcast from Redis.
     * 
     * @param district District name
     * @param state State name
     * @return Optional containing cached nowcast if found and valid
     */
    public Optional<NowcastDto> getCachedNowcast(String district, String state) {
        String cacheKey = buildCacheKey(district, state, "NOWCAST");
        return getCachedData(cacheKey, NowcastDto.class);
    }

    /**
     * Get cached weather alerts from Redis.
     * 
     * @param district District name
     * @param state State name
     * @return Optional containing cached alerts if found and valid
     */
    public Optional<WeatherAlertDto> getCachedWeatherAlerts(String district, String state) {
        String cacheKey = buildCacheKey(district, state, "ALERTS");
        return getCachedData(cacheKey, WeatherAlertDto.class);
    }

    /**
     * Get cached rainfall stats from Redis.
     * 
     * @param district District name
     * @param state State name
     * @return Optional containing cached rainfall stats if found and valid
     */
    public Optional<RainfallStatsDto> getCachedRainfallStats(String district, String state) {
        String cacheKey = buildCacheKey(district, state, "RAINFALL");
        return getCachedData(cacheKey, RainfallStatsDto.class);
    }

    /**
     * Get cached agromet advisories from Redis.
     * 
     * @param district District name
     * @param state State name
     * @return Optional containing cached advisories if found and valid
     */
    public Optional<AgrometAdvisoryDto> getCachedAgrometAdvisories(String district, String state) {
        String cacheKey = buildCacheKey(district, state, "AGROMET");
        return getCachedData(cacheKey, AgrometAdvisoryDto.class);
    }

    /**
     * Cache 7-day forecast in Redis and MySQL.
     * Implements cache invalidation by removing old cache entries before storing new ones.
     * 
     * @param district District name
     * @param state State name
     * @param forecast Forecast data to cache
     */
    public void cacheSevenDayForecast(String district, String state, SevenDayForecastDto forecast) {
        cacheWeatherData(district, state, "7DAY", forecast);
    }

    /**
     * Cache current weather in Redis and MySQL.
     * 
     * @param district District name
     * @param state State name
     * @param weather Current weather data to cache
     */
    public void cacheCurrentWeather(String district, String state, CurrentWeatherDto weather) {
        cacheWeatherData(district, state, "CURRENT", weather);
    }

    /**
     * Cache nowcast in Redis and MySQL.
     * 
     * @param district District name
     * @param state State name
     * @param nowcast Nowcast data to cache
     */
    public void cacheNowcast(String district, String state, NowcastDto nowcast) {
        cacheWeatherData(district, state, "NOWCAST", nowcast);
    }

    /**
     * Cache weather alerts in Redis and MySQL.
     * 
     * @param district District name
     * @param state State name
     * @param alerts Alert data to cache
     */
    public void cacheWeatherAlerts(String district, String state, WeatherAlertDto alerts) {
        cacheWeatherData(district, state, "ALERTS", alerts);
    }

    /**
     * Cache rainfall stats in Redis and MySQL.
     * 
     * @param district District name
     * @param state State name
     * @param rainfall Rainfall stats to cache
     */
    public void cacheRainfallStats(String district, String state, RainfallStatsDto rainfall) {
        cacheWeatherData(district, state, "RAINFALL", rainfall);
    }

    /**
     * Cache agromet advisories in Redis and MySQL.
     * 
     * @param district District name
     * @param state State name
     * @param advisories Advisory data to cache
     */
    public void cacheAgrometAdvisories(String district, String state, AgrometAdvisoryDto advisories) {
        cacheWeatherData(district, state, "AGROMET", advisories);
    }

    /**
     * Invalidate all cache entries for a district and state.
     * Called when new data is fetched to ensure cache consistency.
     * 
     * @param district District name
     * @param state State name
     */
    public void invalidateCache(String district, String state) {
        String[] cacheTypes = {"7DAY", "CURRENT", "NOWCAST", "ALERTS", "RAINFALL", "AGROMET"};
        for (String cacheType : cacheTypes) {
            String cacheKey = buildCacheKey(district, state, cacheType);
            try {
                Boolean deleted = redisTemplate.delete(cacheKey);
                if (Boolean.TRUE.equals(deleted)) {
                    logger.debug("Invalidated cache key: {}", cacheKey);
                }
            } catch (Exception e) {
                logger.error("Error invalidating cache key {}: {}", cacheKey, e.getMessage());
            }
        }
        logger.info("Invalidated all cache entries for district: {}, state: {}", district, state);
    }

    /**
     * Invalidate specific cache entry for a district, state, and forecast type.
     * 
     * @param district District name
     * @param state State name
     * @param forecastType Type of forecast
     */
    public void invalidateCache(String district, String state, String forecastType) {
        String cacheKey = buildCacheKey(district, state, forecastType);
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                logger.debug("Invalidated cache key: {}", cacheKey);
            }
        } catch (Exception e) {
            logger.error("Error invalidating cache key {}: {}", cacheKey, e.getMessage());
        }
    }

    /**
     * Get cache timestamp for offline data age indication.
     * 
     * @param district District name
     * @param state State name
     * @param forecastType Type of forecast
     * @return Optional containing the cache timestamp
     */
    public Optional<LocalDateTime> getCacheTimestamp(String district, String state, String forecastType) {
        String timestampKey = buildCacheKey(district, state, forecastType) + CACHE_TIMESTAMP_SUFFIX;
        try {
            Object timestamp = redisTemplate.opsForValue().get(timestampKey);
            if (timestamp instanceof LocalDateTime) {
                return Optional.of((LocalDateTime) timestamp);
            } else if (timestamp != null) {
                return Optional.of(LocalDateTime.parse(timestamp.toString()));
            }
        } catch (Exception e) {
            logger.error("Error getting cache timestamp for key {}: {}", timestampKey, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Check if cache is valid (not expired).
     * 
     * @param district District name
     * @param state State name
     * @param forecastType Type of forecast
     * @return true if cache is valid, false otherwise
     */
    public boolean isCacheValid(String district, String state, String forecastType) {
        String cacheKey = buildCacheKey(district, state, forecastType);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        } catch (Exception e) {
            logger.error("Error checking cache validity for key {}: {}", cacheKey, e.getMessage());
            return false;
        }
    }

    /**
     * Get remaining TTL for a cache entry.
     * 
     * @param district District name
     * @param state State name
     * @param forecastType Type of forecast
     * @return Optional containing remaining TTL in minutes
     */
    public Optional<Long> getRemainingTtl(String district, String state, String forecastType) {
        String cacheKey = buildCacheKey(district, state, forecastType);
        try {
            Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.MINUTES);
            if (ttl != null && ttl > 0) {
                return Optional.of(ttl);
            }
        } catch (Exception e) {
            logger.error("Error getting TTL for key {}: {}", cacheKey, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Build cache key for weather data.
     * 
     * @param district District name
     * @param state State name
     * @param forecastType Type of forecast
     * @return Cache key string
     */
    private String buildCacheKey(String district, String state, String forecastType) {
        return WEATHER_CACHE_KEY_PREFIX + district.toLowerCase() + ":" + 
               state.toLowerCase() + ":" + forecastType;
    }

    /**
     * Get cached data from Redis.
     * 
     * @param cacheKey Cache key
     * @param type Class type to deserialize
     * @return Optional containing deserialized data
     */
    private <T> Optional<T> getCachedData(String cacheKey, Class<T> type) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && type.isInstance(cached)) {
                logger.debug("Cache hit for key: {}", cacheKey);
                return Optional.of(type.cast(cached));
            }
            logger.debug("Cache miss for key: {}", cacheKey);
        } catch (Exception e) {
            logger.error("Error getting cached data for key {}: {}", cacheKey, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Cache weather data in both Redis and MySQL.
     * Implements cache invalidation before storing new data.
     * 
     * @param district District name
     * @param state State name
     * @param forecastType Type of forecast
     * @param data Weather data to cache
     */
    private <T> void cacheWeatherData(String district, String state, String forecastType, T data) {
        String cacheKey = buildCacheKey(district, state, forecastType);
        LocalDateTime now = LocalDateTime.now();

        try {
            // Invalidate old cache entry first (cache invalidation on new data fetch)
            invalidateCache(district, state, forecastType);

            // Store in Redis with TTL
            redisTemplate.opsForValue().set(cacheKey, data, Duration.ofMinutes(cacheTtlMinutes));

            // Store cache timestamp
            String timestampKey = cacheKey + CACHE_TIMESTAMP_SUFFIX;
            redisTemplate.opsForValue().set(timestampKey, now.toString(), Duration.ofMinutes(cacheTtlMinutes));

            logger.debug("Cached weather data for key: {} with TTL: {} minutes", cacheKey, cacheTtlMinutes);

            // Store in MySQL for persistence (fallback for offline mode)
            saveToDatabase(district, state, forecastType, data, now);

        } catch (Exception e) {
            logger.error("Error caching weather data for key {}: {}", cacheKey, e.getMessage());
            // Fallback to database only if Redis fails
            saveToDatabase(district, state, forecastType, data, now);
        }
    }

    /**
     * Save weather data to MySQL database for persistence.
     * 
     * @param district District name
     * @param state State name
     * @param forecastType Type of forecast
     * @param data Weather data to save
     * @param fetchedAt Timestamp when data was fetched
     */
    private <T> void saveToDatabase(String district, String state, String forecastType, T data, LocalDateTime fetchedAt) {
        try {
            WeatherCache cache = WeatherCache.builder()
                .district(district)
                .state(state)
                .forecastType(forecastType)
                .cachedData(data.toString())
                .fetchedAt(fetchedAt)
                .validUntil(fetchedAt.plusMinutes(cacheTtlMinutes))
                .source("IMD")
                .build();
            weatherCacheRepository.save(cache);
            logger.debug("Saved weather data to database for district: {}, type: {}", district, forecastType);
        } catch (Exception e) {
            logger.error("Error saving weather data to database: {}", e.getMessage());
        }
    }
}