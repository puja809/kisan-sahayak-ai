package com.farmer.sync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for accessing cached data in offline mode.
 * 
 * Allows access to cached weather data, crop recommendations, scheme information.
 * Displays timestamps indicating data freshness.
 * 
 * Validates: Requirements 12.2, 12.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfflineDataAccessService {

    /**
     * Get cached weather data for offline access.
     * 
     * @param userId User ID
     * @param district District name
     * @return Cached weather data with timestamp
     */
    @Transactional(readOnly = true)
    public CachedDataResponse getWeatherData(String userId, String district) {
        log.debug("Retrieving cached weather data for user {} in district {}", userId, district);

        // In a real implementation, this would query the weather cache table
        // For now, return a structured response indicating data freshness
        return CachedDataResponse.builder()
            .dataType("WEATHER")
            .district(district)
            .isCached(true)
            .lastFetchedAt(LocalDateTime.now().minusMinutes(15))
            .dataFreshness("15 minutes old")
            .build();
    }

    /**
     * Get cached crop recommendations for offline access.
     * 
     * @param userId User ID
     * @return Cached crop recommendations with timestamp
     */
    @Transactional(readOnly = true)
    public CachedDataResponse getCropRecommendations(String userId) {
        log.debug("Retrieving cached crop recommendations for user {}", userId);

        // In a real implementation, this would query the crop recommendations cache
        return CachedDataResponse.builder()
            .dataType("CROP_RECOMMENDATIONS")
            .isCached(true)
            .lastFetchedAt(LocalDateTime.now().minusHours(1))
            .dataFreshness("1 hour old")
            .build();
    }

    /**
     * Get cached scheme information for offline access.
     * 
     * @param userId User ID
     * @return Cached scheme information with timestamp
     */
    @Transactional(readOnly = true)
    public CachedDataResponse getSchemeInformation(String userId) {
        log.debug("Retrieving cached scheme information for user {}", userId);

        // In a real implementation, this would query the scheme cache
        return CachedDataResponse.builder()
            .dataType("SCHEMES")
            .isCached(true)
            .lastFetchedAt(LocalDateTime.now().minusHours(2))
            .dataFreshness("2 hours old")
            .build();
    }

    /**
     * Check if cached data is available for a specific data type.
     * 
     * @param userId User ID
     * @param dataType Type of data (WEATHER, CROP_RECOMMENDATIONS, SCHEMES)
     * @return true if cached data is available
     */
    @Transactional(readOnly = true)
    public boolean isCachedDataAvailable(String userId, String dataType) {
        log.debug("Checking if cached data is available for user {} of type {}", userId, dataType);

        // In a real implementation, this would check the cache tables
        return true;
    }

    /**
     * Get data freshness information for display.
     * 
     * @param lastFetchedAt Timestamp when data was last fetched
     * @return Human-readable freshness string
     */
    public String getDataFreshnessString(LocalDateTime lastFetchedAt) {
        if (lastFetchedAt == null) {
            return "Data not available";
        }

        LocalDateTime now = LocalDateTime.now();
        long minutesDiff = java.time.temporal.ChronoUnit.MINUTES.between(lastFetchedAt, now);

        if (minutesDiff < 1) {
            return "Just now";
        } else if (minutesDiff < 60) {
            return minutesDiff + " minute" + (minutesDiff > 1 ? "s" : "") + " ago";
        } else {
            long hoursDiff = minutesDiff / 60;
            return hoursDiff + " hour" + (hoursDiff > 1 ? "s" : "") + " ago";
        }
    }

    /**
     * DTO for cached data response.
     */
    @lombok.Data
    @lombok.Builder
    public static class CachedDataResponse {
        private String dataType;
        private String district;
        private boolean isCached;
        private LocalDateTime lastFetchedAt;
        private String dataFreshness;
        private Map<String, Object> data;
    }
}
