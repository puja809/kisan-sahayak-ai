package com.farmer.crop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Soil Health Card data.
 * 
 * Provides methods to query soil health card data by farmer, location, and card ID.
 * 
 * Validates: Requirement 2.4
 */
@Repository
public interface SoilHealthCardRepository extends JpaRepository<SoilHealthCard, Long> {

    /**
     * Find soil health card by card ID.
     * 
     * @param cardId Unique card identifier
     * @return Optional containing the card if found
     */
    Optional<SoilHealthCard> findByCardIdAndIsActiveTrue(String cardId);

    /**
     * Find all soil health cards for a farmer.
     * 
     * @param farmerId Farmer identifier
     * @return List of soil health cards
     */
    List<SoilHealthCard> findByFarmerIdAndIsActiveTrue(String farmerId);

    /**
     * Find the most recent soil health card for a farmer.
     * 
     * @param farmerId Farmer identifier
     * @return Optional containing the most recent card
     */
    @Query("SELECT s FROM SoilHealthCard s WHERE s.farmerId = :farmerId " +
           "AND s.isActive = true ORDER BY s.analysisDate DESC LIMIT 1")
    Optional<SoilHealthCard> findMostRecentByFarmerId(@Param("farmerId") String farmerId);

    /**
     * Find soil health card by survey number and farmer ID.
     * 
     * @param surveyNumber Land parcel survey number
     * @param farmerId Farmer identifier
     * @return Optional containing the card if found
     */
    Optional<SoilHealthCard> findBySurveyNumberAndFarmerIdAndIsActiveTrue(
            String surveyNumber, String farmerId);

    /**
     * Find soil health cards by district and state.
     * 
     * @param district District name
     * @param state State name
     * @return List of soil health cards
     */
    List<SoilHealthCard> findByDistrictAndStateAndIsActiveTrue(String district, String state);

    /**
     * Find soil health cards within a geographic bounding box.
     * 
     * @param minLat Minimum latitude
     * @param maxLat Maximum latitude
     * @param minLon Minimum longitude
     * @param maxLon Maximum longitude
     * @return List of soil health cards within the bounding box
     */
    @Query("SELECT s FROM SoilHealthCard s WHERE s.latitude BETWEEN :minLat AND :maxLat " +
           "AND s.longitude BETWEEN :minLon AND :maxLon AND s.isActive = true")
    List<SoilHealthCard> findWithinBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon);

    /**
     * Find soil health cards with nutrient deficiency data.
     * 
     * @param farmerId Farmer identifier
     * @return List of cards with nutrient data
     */
    @Query("SELECT s FROM SoilHealthCard s WHERE s.farmerId = :farmerId " +
           "AND s.isActive = true AND s.nitrogenKgHa IS NOT NULL")
    List<SoilHealthCard> findWithNutrientDataByFarmerId(@Param("farmerId") String farmerId);

    /**
     * Count active cards for a farmer.
     * 
     * @param farmerId Farmer identifier
     * @return Count of active cards
     */
    long countByFarmerIdAndIsActiveTrue(String farmerId);
}