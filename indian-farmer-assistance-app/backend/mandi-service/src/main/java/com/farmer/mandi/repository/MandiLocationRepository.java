package com.farmer.mandi.repository;

import com.farmer.mandi.entity.MandiLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

/**
 * Repository for MandiLocation entity.
 * 
 * Requirements:
 * - 6.4: Sort mandis by distance from farmer's location
 */
@Repository
public interface MandiLocationRepository extends JpaRepository<MandiLocation, Long> {

    /**
     * Find location by mandi code.
     */
    Optional<MandiLocation> findByMandiCode(String mandiCode);

    /**
     * Find all active locations by state.
     */
    List<MandiLocation> findByStateAndIsActiveTrueOrderByMandiName(String state);

    /**
     * Find all active locations by district.
     */
    List<MandiLocation> findByDistrictAndIsActiveTrueOrderByMandiName(String district);

    /**
     * Find all active locations.
     */
    List<MandiLocation> findByIsActiveTrueOrderByMandiName();

    /**
     * Find locations within a bounding box for distance calculation.
     */
    @Query("SELECT m FROM MandiLocation m WHERE m.isActive = true " +
           "AND m.latitude BETWEEN :minLat AND :maxLat " +
           "AND m.longitude BETWEEN :minLon AND :maxLon")
    List<MandiLocation> findLocationsInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon);

    /**
     * Find locations by state.
     */
    List<MandiLocation> findByState(String state);

    /**
     * Find distinct states.
     */
    @Query("SELECT DISTINCT m.state FROM MandiLocation m WHERE m.isActive = true ORDER BY m.state")
    List<String> findDistinctStates();

    /**
     * Find distinct districts by state.
     */
    @Query("SELECT DISTINCT m.district FROM MandiLocation m WHERE m.state = :state AND m.isActive = true ORDER BY m.district")
    List<String> findDistinctDistrictsByState(@Param("state") String state);
}