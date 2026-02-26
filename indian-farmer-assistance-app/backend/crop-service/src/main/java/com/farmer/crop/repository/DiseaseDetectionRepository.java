package com.farmer.crop.repository;

import com.farmer.crop.entity.DiseaseDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DiseaseDetection entity.
 * 
 * Provides database operations for disease detection records.
 */
@Repository
public interface DiseaseDetectionRepository extends JpaRepository<DiseaseDetection, Long> {

    /**
     * Find all disease detections for a user.
     * 
     * @param userId The user ID
     * @return List of disease detections
     */
    List<DiseaseDetection> findByUserId(Long userId);

    /**
     * Find all disease detections for a crop.
     * 
     * @param cropId The crop ID
     * @return List of disease detections
     */
    List<DiseaseDetection> findByCropId(Long cropId);

    /**
     * Find disease detections for a user within a date range.
     * 
     * @param userId The user ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of disease detections
     */
    @Query("SELECT d FROM DiseaseDetection d WHERE d.userId = :userId " +
           "AND d.detectionTimestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY d.detectionTimestamp DESC")
    List<DiseaseDetection> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find disease detections by disease name.
     * 
     * @param diseaseName The disease name
     * @return List of disease detections
     */
    List<DiseaseDetection> findByDiseaseName(String diseaseName);

    /**
     * Find disease detections by severity level.
     * 
     * @param severityLevel The severity level
     * @return List of disease detections
     */
    List<DiseaseDetection> findBySeverityLevel(DiseaseDetection.SeverityLevel severityLevel);

    /**
     * Find the most recent disease detection for a crop.
     * 
     * @param cropId The crop ID
     * @return Optional containing the most recent detection
     */
    @Query("SELECT d FROM DiseaseDetection d WHERE d.cropId = :cropId " +
           "ORDER BY d.detectionTimestamp DESC LIMIT 1")
    Optional<DiseaseDetection> findMostRecentByCropId(@Param("cropId") Long cropId);

    /**
     * Count disease detections for a user.
     * 
     * @param userId The user ID
     * @return Count of disease detections
     */
    long countByUserId(Long userId);

    /**
     * Count disease detections for a crop.
     * 
     * @param cropId The crop ID
     * @return Count of disease detections
     */
    long countByCropId(Long cropId);
}
