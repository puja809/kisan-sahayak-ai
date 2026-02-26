package com.farmer.yield.repository;

import com.farmer.yield.entity.YieldPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for YieldPrediction entity.
 * 
 * Provides methods for:
 * - Finding predictions by crop ID
 * - Finding predictions by farmer ID
 * - Finding the latest prediction for a crop
 * - Finding predictions with significant variance
 * - Calculating average variance for model improvement
 * 
 * Validates: Requirements 11B.1, 11B.7, 11B.9
 */
@Repository
public interface YieldPredictionRepository extends JpaRepository<YieldPrediction, Long> {

    /**
     * Find all predictions for a specific crop, ordered by prediction date descending.
     */
    List<YieldPrediction> findByCropIdOrderByPredictionDateDesc(Long cropId);

    /**
     * Find all predictions for a specific farmer, ordered by prediction date descending.
     */
    List<YieldPrediction> findByFarmerIdOrderByPredictionDateDesc(String farmerId);

    /**
     * Find the latest prediction for a specific crop.
     */
    Optional<YieldPrediction> findFirstByCropIdOrderByPredictionDateDesc(Long cropId);

    /**
     * Find the latest prediction for a specific farmer.
     */
    Optional<YieldPrediction> findFirstByFarmerIdOrderByPredictionDateDesc(String farmerId);

    /**
     * Find predictions by crop ID and date range.
     */
    List<YieldPrediction> findByCropIdAndPredictionDateBetweenOrderByPredictionDateDesc(
            Long cropId, LocalDate startDate, LocalDate endDate);

    /**
     * Find predictions that have actual yield recorded but no variance calculated.
     */
    @Query("SELECT y FROM YieldPrediction y WHERE y.actualYieldQuintals IS NOT NULL AND y.varianceQuintals IS NULL")
    List<YieldPrediction> findPredictionsNeedingVarianceCalculation();

    /**
     * Find predictions with significant variance (>20% deviation).
     */
    @Query("SELECT y FROM YieldPrediction y WHERE y.variancePercent IS NOT NULL AND ABS(y.variancePercent) > :threshold")
    List<YieldPrediction> findPredictionsWithSignificantVariance(@Param("threshold") Double threshold);

    /**
     * Calculate average variance for a specific crop ID.
     */
    @Query("SELECT AVG(ABS(y.variancePercent)) FROM YieldPrediction y WHERE y.cropId = :cropId AND y.variancePercent IS NOT NULL")
    Double calculateAverageVarianceForCrop(@Param("cropId") Long cropId);

    /**
     * Find predictions that need notification (significant deviation from previous).
     */
    @Query("SELECT y FROM YieldPrediction y WHERE y.notificationSent = false AND y.previousPredictionId IS NOT NULL " +
           "AND ABS((y.predictedYieldExpectedQuintals - (SELECT y2.predictedYieldExpectedQuintals FROM YieldPrediction y2 WHERE y2.id = y.previousPredictionId)) * 100 / " +
           "(SELECT y2.predictedYieldExpectedQuintals FROM YieldPrediction y2 WHERE y2.id = y.previousPredictionId)) >= 10")
    List<YieldPrediction> findPredictionsNeedingNotification();

    /**
     * Count predictions by model version.
     */
    @Query("SELECT y.modelVersion, COUNT(y) FROM YieldPrediction y WHERE y.modelVersion IS NOT NULL GROUP BY y.modelVersion")
    List<Object[]> countByModelVersion();

    /**
     * Find all predictions for a crop with actual yield recorded.
     */
    @Query("SELECT y FROM YieldPrediction y WHERE y.cropId = :cropId AND y.actualYieldQuintals IS NOT NULL ORDER BY y.predictionDate DESC")
    List<YieldPrediction> findByCropIdWithActualYield(@Param("cropId") Long cropId);

    /**
     * Calculate average actual yield for a farmer's specific crop.
     */
    @Query("SELECT AVG(y.actualYieldQuintals) FROM YieldPrediction y WHERE y.farmerId = :farmerId " +
           "AND y.cropId = :cropId AND y.actualYieldQuintals IS NOT NULL")
    Double calculateAverageActualYieldForFarmerAndCrop(
            @Param("farmerId") String farmerId, 
            @Param("cropId") Long cropId);

    /**
     * Find recent predictions for a farmer's crops.
     */
    @Query("SELECT y FROM YieldPrediction y WHERE y.farmerId = :farmerId AND y.predictionDate >= :since ORDER BY y.predictionDate DESC")
    List<YieldPrediction> findRecentPredictionsForFarmer(
            @Param("farmerId") String farmerId, 
            @Param("since") LocalDate since);
}