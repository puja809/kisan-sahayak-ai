package com.farmer.crop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DistrictZoneMapping entity.
 * 
 * Validates: Requirement 2.1
 */
@Repository
public interface DistrictZoneMappingRepository extends JpaRepository<DistrictZoneMapping, Long> {

    /**
     * Find district mapping by district name and state.
     * 
     * @param districtName District name
     * @param state State name
     * @return Optional containing the mapping if found
     */
    @Query("SELECT d FROM DistrictZoneMapping d WHERE d.isActive = true " +
           "AND LOWER(d.districtName) = LOWER(:districtName) AND LOWER(d.state) = LOWER(:state)")
    Optional<DistrictZoneMapping> findByDistrictAndState(
            @Param("districtName") String districtName, 
            @Param("state") String state);

    /**
     * Find all districts in a state.
     * 
     * @param state State name
     * @return List of district mappings for the state
     */
    @Query("SELECT d FROM DistrictZoneMapping d WHERE d.isActive = true AND LOWER(d.state) = LOWER(:state)")
    List<DistrictZoneMapping> findByState(@Param("state") String state);

    /**
     * Find district mapping by zone ID.
     * 
     * @param zoneId Zone ID
     * @return List of district mappings for the zone
     */
    List<DistrictZoneMapping> findByZoneIdAndIsActiveTrue(Long zoneId);

    /**
     * Find district mapping by district name (case-insensitive).
     * 
     * @param districtName District name
     * @return List of matching district mappings
     */
    @Query("SELECT d FROM DistrictZoneMapping d WHERE d.isActive = true AND LOWER(d.districtName) = LOWER(:districtName)")
    List<DistrictZoneMapping> findByDistrictName(@Param("districtName") String districtName);

    /**
     * Find district mapping by alternative names.
     * 
     * @param alternativeName Alternative district name
     * @return List of matching district mappings
     */
    @Query("SELECT d FROM DistrictZoneMapping d WHERE d.isActive = true AND d.alternativeNames LIKE %:alternativeName%")
    List<DistrictZoneMapping> findByAlternativeName(@Param("alternativeName") String alternativeName);

    /**
     * Find verified district mappings.
     * 
     * @return List of verified district mappings
     */
    List<DistrictZoneMapping> findByIsVerifiedAndIsActiveTrue(Boolean isVerified);

    /**
     * Find district mapping by region.
     * 
     * @param region Region name
     * @return List of district mappings in the region
     */
    List<DistrictZoneMapping> findByRegionAndIsActiveTrue(String region);

    /**
     * Find district mapping by approximate coordinates.
     * Uses a bounding box search to find districts near the given coordinates.
     * 
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @return Optional containing the nearest district mapping
     */
    @Query(value = """
        SELECT d.* FROM district_zone_mappings d 
        WHERE d.is_active = true 
        AND d.latitude BETWEEN :minLat AND :maxLat
        AND d.longitude BETWEEN :minLon AND :maxLon
        ORDER BY ABS(d.latitude - :latitude) + ABS(d.longitude - :longitude)
        LIMIT 1
        """, nativeQuery = true)
    Optional<DistrictZoneMapping> findNearestByCoordinates(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon);

    /**
     * Count districts in a state.
     * 
     * @param state State name
     * @return Count of districts
     */
    @Query("SELECT COUNT(d) FROM DistrictZoneMapping d WHERE d.isActive = true AND LOWER(d.state) = LOWER(:state)")
    long countByState(@Param("state") String state);
}