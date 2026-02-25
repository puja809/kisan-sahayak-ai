package com.farmer.user.repository;

import com.farmer.user.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Farm entity with custom query methods.
 * Requirements: 11A.2, 11A.3, 11A.10
 */
@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {

    /**
     * Find all farms for a user.
     * Requirements: 11A.10
     */
    List<Farm> findByUserId(Long userId);

    /**
     * Find all active farms for a user.
     * Requirements: 11A.10
     */
    List<Farm> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find farm by ID and user ID.
     * Requirements: 11A.10
     */
    Optional<Farm> findByIdAndUserId(Long id, Long userId);

    /**
     * Find farms by state.
     */
    List<Farm> findByState(String state);

    /**
     * Find farms by district.
     */
    List<Farm> findByDistrict(String district);

    /**
     * Find farms by irrigation type.
     * Requirements: 11A.3
     */
    List<Farm> findByUserIdAndIrrigationType(Long userId, Farm.IrrigationType irrigationType);

    /**
     * Find farms by soil type.
     * Requirements: 11A.3
     */
    List<Farm> findByUserIdAndSoilType(Long userId, String soilType);

    /**
     * Count active farms for a user.
     * Requirements: 11A.10
     */
    long countByUserIdAndIsActiveTrue(Long userId);

    /**
     * Calculate total land area for a user.
     * Requirements: 11A.3
     */
    @Query("SELECT COALESCE(SUM(f.totalAreaAcres), 0) FROM Farm f WHERE f.user.id = :userId AND f.isActive = true")
    Double calculateTotalLandArea(@Param("userId") Long userId);

    /**
     * Find farms within a GPS bounding box.
     */
    @Query("SELECT f FROM Farm f WHERE f.user.id = :userId AND " +
           "f.gpsLatitude BETWEEN :minLat AND :maxLat AND " +
           "f.gpsLongitude BETWEEN :minLon AND :maxLon")
    List<Farm> findFarmsWithinBounds(
            @Param("userId") Long userId,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon);
}