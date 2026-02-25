package com.farmer.crop.repository;

import com.farmer.crop.entity.AgroEcologicalZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AgroEcologicalZone entity.
 * 
 * Validates: Requirement 2.1
 */
@Repository
public interface AgroEcologicalZoneRepository extends JpaRepository<AgroEcologicalZone, Long> {

    /**
     * Find zone by ICAR zone code.
     * 
     * @param zoneCode ICAR zone code (e.g., "AEZ-01")
     * @return Optional containing the zone if found
     */
    Optional<AgroEcologicalZone> findByZoneCode(String zoneCode);

    /**
     * Find all active zones.
     * 
     * @return List of active zones
     */
    List<AgroEcologicalZone> findByIsActiveTrue();

    /**
     * Find zones by climate type.
     * 
     * @param climateType Climate type (e.g., "Tropical", "Subtropical")
     * @return List of zones with the specified climate type
     */
    List<AgroEcologicalZone> findByClimateTypeAndIsActiveTrue(String climateType);

    /**
     * Find zone by name.
     * 
     * @param zoneName Zone name
     * @return Optional containing the zone if found
     */
    Optional<AgroEcologicalZone> findByZoneNameAndIsActiveTrue(String zoneName);

    /**
     * Find zones covering a specific state.
     * 
     * @param state State name
     * @return List of zones covering the state
     */
    @Query("SELECT z FROM AgroEcologicalZone z WHERE z.isActive = true AND z.statesCovered LIKE %:state%")
    List<AgroEcologicalZone> findByStateCoverage(@Param("state") String state);

    /**
     * Find zone by approximate latitude and longitude.
     * This is a simplified lookup that checks if the coordinates fall within
     * the zone's bounding box.
     * 
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @return Optional containing the zone if found
     */
    @Query(value = """
        SELECT z.* FROM agro_ecological_zones z 
        WHERE z.is_active = true 
        AND CAST(SUBSTRING_INDEX(z.latitude_range, '-', 1) AS DECIMAL(10,2)) <= :latitude 
        AND CAST(SUBSTRING_INDEX(z.latitude_range, '-', -1) AS DECIMAL(10,2)) >= :latitude
        AND CAST(SUBSTRING_INDEX(z.longitude_range, '-', 1) AS DECIMAL(10,2)) <= :longitude 
        AND CAST(SUBSTRING_INDEX(z.longitude_range, '-', -1) AS DECIMAL(10,2)) >= :longitude
        LIMIT 1
        """, nativeQuery = true)
    Optional<AgroEcologicalZone> findByCoordinates(
            @Param("latitude") Double latitude, 
            @Param("longitude") Double longitude);
}