package com.farmer.crop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GAEZ crop suitability data.
 * 
 * Provides methods to query GAEZ v4 crop suitability data by zone, crop, and suitability criteria.
 * 
 * Validates: Requirements 2.2, 2.3
 */
@Repository
public interface GaezCropDataRepository extends JpaRepository<GaezCropData, Long> {

    /**
     * Find GAEZ crop data by zone code and crop code.
     * 
     * @param zoneCode GAEZ zone code
     * @param cropCode GAEZ crop code
     * @return Optional containing the crop data if found
     */
    Optional<GaezCropData> findByZoneCodeAndCropCodeAndIsActiveTrue(
            String zoneCode, String cropCode);

    /**
     * Find all active crop data for a zone.
     * 
     * @param zoneCode GAEZ zone code
     * @return List of crop data for the zone
     */
    List<GaezCropData> findByZoneCodeAndIsActiveTrue(String zoneCode);

    /**
     * Find all active crop data for a crop.
     * 
     * @param cropCode GAEZ crop code
     * @return List of crop data for the crop across all zones
     */
    List<GaezCropData> findByCropCodeAndIsActiveTrue(String cropCode);

    /**
     * Find highly suitable crops for a zone (score >= 80).
     * 
     * @param zoneCode GAEZ zone code
     * @return List of highly suitable crops
     */
    @Query("SELECT g FROM GaezCropData g WHERE g.zoneCode = :zoneCode " +
           "AND g.overallSuitabilityScore >= 80 AND g.isActive = true " +
           "ORDER BY g.overallSuitabilityScore DESC")
    List<GaezCropData> findHighlySuitableCrops(@Param("zoneCode") String zoneCode);

    /**
     * Find suitable crops for a zone with minimum score.
     * 
     * @param zoneCode GAEZ zone code
     * @param minScore Minimum suitability score
     * @return List of suitable crops
     */
    @Query("SELECT g FROM GaezCropData g WHERE g.zoneCode = :zoneCode " +
           "AND g.overallSuitabilityScore >= :minScore AND g.isActive = true " +
           "ORDER BY g.overallSuitabilityScore DESC")
    List<GaezCropData> findSuitableCropsByMinScore(
            @Param("zoneCode") String zoneCode,
            @Param("minScore") Double minScore);

    /**
     * Find kharif-suitable crops for a zone.
     * 
     * @param zoneCode GAEZ zone code
     * @return List of kharif-suitable crops
     */
    @Query("SELECT g FROM GaezCropData g WHERE g.zoneCode = :zoneCode " +
           "AND g.kharifSuitable = true AND g.isActive = true " +
           "ORDER BY g.overallSuitabilityScore DESC")
    List<GaezCropData> findKharifSuitableCrops(@Param("zoneCode") String zoneCode);

    /**
     * Find rabi-suitable crops for a zone.
     * 
     * @param zoneCode GAEZ zone code
     * @return List of rabi-suitable crops
     */
    @Query("SELECT g FROM GaezCropData g WHERE g.zoneCode = :zoneCode " +
           "AND g.rabiSuitable = true AND g.isActive = true " +
           "ORDER BY g.overallSuitabilityScore DESC")
    List<GaezCropData> findRabiSuitableCrops(@Param("zoneCode") String zoneCode);

    /**
     * Find zaid-suitable crops for a zone.
     * 
     * @param zoneCode GAEZ zone code
     * @return List of zaid-suitable crops
     */
    @Query("SELECT g FROM GaezCropData g WHERE g.zoneCode = :zoneCode " +
           "AND g.zaidSuitable = true AND g.isActive = true " +
           "ORDER BY g.overallSuitabilityScore DESC")
    List<GaezCropData> findZaidSuitableCrops(@Param("zoneCode") String zoneCode);

    /**
     * Find low climate risk crops for a zone.
     * 
     * @param zoneCode GAEZ zone code
     * @return List of low climate risk crops
     */
    @Query("SELECT g FROM GaezCropData g WHERE g.zoneCode = :zoneCode " +
           "AND g.climateRiskLevel = 'LOW' AND g.isActive = true " +
           "ORDER BY g.overallSuitabilityScore DESC")
    List<GaezCropData> findLowClimateRiskCrops(@Param("zoneCode") String zoneCode);

    /**
     * Find crops suitable for rain-fed conditions.
     * 
     * @param zoneCode GAEZ zone code
     * @return List of rain-fed suitable crops
     */
    @Query("SELECT g FROM GaezCropData g WHERE g.zoneCode = :zoneCode " +
           "AND g.waterSuitabilityScore >= 60 AND g.isActive = true " +
           "ORDER BY g.overallSuitabilityScore DESC")
    List<GaezCropData> findRainfedSuitableCrops(@Param("zoneCode") String zoneCode);

    /**
     * Find crops suitable for irrigated conditions.
     * 
     * @param zoneCode GAEZ zone code
     * @return List of irrigated suitable crops
     */
    @Query("SELECT g FROM GaezCropData g WHERE g.zoneCode = :zoneCode " +
           "AND g.irrigatedPotentialYield IS NOT NULL AND g.isActive = true " +
           "ORDER BY g.overallSuitabilityScore DESC")
    List<GaezCropData> findIrrigatedSuitableCrops(@Param("zoneCode") String zoneCode);

    /**
     * Find crops by multiple crop codes.
     * 
     * @param zoneCode GAEZ zone code
     * @param cropCodes List of crop codes
     * @return List of matching crop data
     */
    @Query("SELECT g FROM GaezCropData g WHERE g.zoneCode = :zoneCode " +
           "AND g.cropCode IN :cropCodes AND g.isActive = true")
    List<GaezCropData> findByZoneCodeAndCropCodes(
            @Param("zoneCode") String zoneCode,
            @Param("cropCodes") List<String> cropCodes);

    /**
     * Find top N crops for a zone by overall suitability.
     * 
     * @param zoneCode GAEZ zone code
     * @param limit Maximum number of results
     * @return List of top crops
     */
    @Query("SELECT g FROM GaezCropData g WHERE g.zoneCode = :zoneCode " +
           "AND g.isActive = true ORDER BY g.overallSuitabilityScore DESC")
    List<GaezCropData> findTopCropsByZone(@Param("zoneCode") String zoneCode);

    /**
     * Count active records for a zone.
     * 
     * @param zoneCode GAEZ zone code
     * @return Count of active records
     */
    long countByZoneCodeAndIsActiveTrue(String zoneCode);

    /**
     * Check if zone has any crop data.
     * 
     * @param zoneCode GAEZ zone code
     * @return true if data exists
     */
    boolean existsByZoneCodeAndIsActiveTrue(String zoneCode);
}