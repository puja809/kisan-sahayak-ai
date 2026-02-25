package com.farmer.weather.repository;

import com.farmer.weather.entity.WeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WeatherCache entity.
 * Provides methods for caching and retrieving weather data.
 */
@Repository
public interface WeatherCacheRepository extends JpaRepository<WeatherCache, Long> {

    /**
     * Find cached weather data by district, state, and forecast type.
     * 
     * @param district District name
     * @param state State name
     * @param forecastType Type of forecast (7DAY, NOWCAST, CURRENT)
     * @return Optional containing cached data if found
     */
    Optional<WeatherCache> findByDistrictAndStateAndForecastType(
            String district, String state, String forecastType);

    /**
     * Find cached weather data by district, state, forecast type, and freshness.
     * 
     * @param district District name
     * @param state State name
     * @param forecastType Type of forecast
     * @param fetchedAfter Only return data fetched after this time
     * @return Optional containing fresh cached data if found
     */
    @Query("SELECT w FROM WeatherCache w WHERE w.district = :district AND w.state = :state " +
           "AND w.forecastType = :forecastType AND w.fetchedAt > :fetchedAfter " +
           "ORDER BY w.fetchedAt DESC")
    Optional<WeatherCache> findByDistrictAndStateAndForecastTypeAndFetchedAtAfter(
            @Param("district") String district,
            @Param("state") String state,
            @Param("forecastType") String forecastType,
            @Param("fetchedAfter") LocalDateTime fetchedAfter);

    /**
     * Find all cached weather data for a district.
     * 
     * @param district District name
     * @param state State name
     * @return List of cached weather data
     */
    List<WeatherCache> findByDistrictAndStateOrderByFetchedAtDesc(String district, String state);

    /**
     * Delete old cached weather data.
     * 
     * @param fetchedBefore Delete data fetched before this time
     * @return Number of deleted records
     */
    long deleteByFetchedAtBefore(LocalDateTime fetchedBefore);

    /**
     * Find the most recent cached data for a district and forecast type.
     * 
     * @param district District name
     * @param state State name
     * @param forecastType Type of forecast
     * @return Optional containing the most recent cached data
     */
    @Query("SELECT w FROM WeatherCache w WHERE w.district = :district AND w.state = :state " +
           "AND w.forecastType = :forecastType ORDER BY w.fetchedAt DESC LIMIT 1")
    Optional<WeatherCache> findMostRecentByDistrictAndStateAndForecastType(
            @Param("district") String district,
            @Param("state") String state,
            @Param("forecastType") String forecastType);
}