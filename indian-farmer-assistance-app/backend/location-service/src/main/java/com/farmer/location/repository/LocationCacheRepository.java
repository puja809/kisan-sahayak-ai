package com.farmer.location.repository;

import com.farmer.location.entity.LocationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LocationCache entity.
 * 
 * Validates: Requirement 14.3 (reverse geocoding with caching)
 */
@Repository
public interface LocationCacheRepository extends JpaRepository<LocationCache, Long> {

    /**
     * Find cached location by exact coordinates
     */
    @Query("SELECT lc FROM LocationCache lc WHERE " +
           "ABS(lc.latitude - :latitude) < 0.0001 AND " +
           "ABS(lc.longitude - :longitude) < 0.0001")
    Optional<LocationCache> findByCoordinates(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude);

    /**
     * Find cached location by district and state
     */
    Optional<LocationCache> findByDistrictAndState(String district, String state);

    /**
     * Find all cached locations in a state
     */
    List<LocationCache> findByState(String state);

    /**
     * Find all cached locations in a district
     */
    List<LocationCache> findByDistrict(String district);

    /**
     * Find cached locations by agro-ecological zone
     */
    List<LocationCache> findByAgroEcologicalZone(String zone);

    /**
     * Find the most recent cached location for a district
     */
    @Query("SELECT lc FROM LocationCache lc WHERE lc.district = :district " +
           "ORDER BY lc.cacheTimestamp DESC LIMIT 1")
    Optional<LocationCache> findMostRecentByDistrict(@Param("district") String district);

    /**
     * Find the most recent cached location for a state
     */
    @Query("SELECT lc FROM LocationCache lc WHERE lc.state = :state " +
           "ORDER BY lc.cacheTimestamp DESC LIMIT 1")
    Optional<LocationCache> findMostRecentByState(@Param("state") String state);

    /**
     * Delete old cache entries (for cache cleanup)
     */
    void deleteByCacheTimestampBefore(java.time.LocalDateTime timestamp);

    /**
     * Count cached entries
     */
    long count();

    /**
     * Check if coordinates are cached
     */
    @Query("SELECT COUNT(lc) > 0 FROM LocationCache lc WHERE " +
           "ABS(lc.latitude - :latitude) < 0.0001 AND " +
           "ABS(lc.longitude - :longitude) < 0.0001")
    boolean existsByCoordinates(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude);
}