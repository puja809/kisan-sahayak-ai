package com.farmer.user.repository;

import com.farmer.user.entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Crop entity with custom query methods.
 * Requirements: 11A.4, 11A.5, 11A.6
 */
@Repository
public interface CropRepository extends JpaRepository<Crop, Long> {

    /**
     * Find all crops for a farm.
     * Requirements: 11A.4
     */
    List<Crop> findByFarmId(Long farmId);

    /**
     * Find all crops for a user through their farms.
     * Requirements: 11A.4
     */
    @Query("SELECT c FROM Crop c WHERE c.farm.user.id = :userId ORDER BY c.sowingDate DESC")
    List<Crop> findByUserId(@Param("userId") Long userId);

    /**
     * Find current active crops for a user (not harvested or failed).
     * Requirements: 11A.4, 11A.8
     */
    @Query("SELECT c FROM Crop c WHERE c.farm.user.id = :userId AND c.status IN ('SOWN', 'GROWING') ORDER BY c.sowingDate DESC")
    List<Crop> findCurrentCropsByUserId(@Param("userId") Long userId);

    /**
     * Find crops by status.
     * Requirements: 11A.4
     */
    List<Crop> findByFarmIdAndStatus(Long farmId, Crop.CropStatus status);

    /**
     * Find crops by name.
     * Requirements: 11A.4
     */
    List<Crop> findByFarmUserIdAndCropName(Long userId, String cropName);

    /**
     * Find crops by season.
     * Requirements: 11A.4
     */
    List<Crop> findByFarmUserIdAndSeason(Long userId, Crop.Season season);

    /**
     * Find crops by status and user.
     * Requirements: 11A.4
     */
    List<Crop> findByFarmUserIdAndStatus(Long userId, Crop.CropStatus status);

    /**
     * Find crops within a date range.
     * Requirements: 11A.6
     */
    @Query("SELECT c FROM Crop c WHERE c.farm.user.id = :userId AND c.sowingDate BETWEEN :startDate AND :endDate ORDER BY c.sowingDate DESC")
    List<Crop> findByUserIdAndSowingDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find crops for harvest within a date range.
     * Requirements: 11A.8
     */
    @Query("SELECT c FROM Crop c WHERE c.farm.user.id = :userId AND c.expectedHarvestDate BETWEEN :startDate AND :endDate AND c.status = 'GROWING'")
    List<Crop> findUpcomingHarvests(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find crop by ID and user ID.
     */
    Optional<Crop> findByIdAndFarmUserId(Long id, Long userId);

    /**
     * Count crops by status for a user.
     */
    long countByFarmUserIdAndStatus(Long userId, Crop.CropStatus status);

    /**
     * Find harvested crops for a user within a date range.
     * Requirements: 11A.5
     */
    @Query("SELECT c FROM Crop c WHERE c.farm.user.id = :userId AND c.status = 'HARVESTED' AND c.actualHarvestDate BETWEEN :startDate AND :endDate ORDER BY c.actualHarvestDate DESC")
    List<Crop> findHarvestedCropsByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get crop history for the past N years.
     * Requirements: 11A.6
     */
    @Query("SELECT c FROM Crop c WHERE c.farm.user.id = :userId AND c.sowingDate >= :cutoffDate ORDER BY c.sowingDate DESC")
    List<Crop> findCropHistory(@Param("userId") Long userId, @Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Calculate total yield for a user in a date range.
     * Requirements: 11A.5
     */
    @Query("SELECT COALESCE(SUM(c.totalYieldQuintals), 0) FROM Crop c WHERE c.farm.user.id = :userId AND c.status = 'HARVESTED' AND c.actualHarvestDate BETWEEN :startDate AND :endDate")
    Double calculateTotalYield(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total revenue for a user in a date range.
     * Requirements: 11A.5
     */
    @Query("SELECT COALESCE(SUM(c.totalRevenue), 0) FROM Crop c WHERE c.farm.user.id = :userId AND c.status = 'HARVESTED' AND c.actualHarvestDate BETWEEN :startDate AND :endDate")
    Double calculateTotalRevenue(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Calculate total input cost for a user in a date range.
     * Requirements: 11A.4
     */
    @Query("SELECT COALESCE(SUM(c.totalInputCost), 0) FROM Crop c WHERE c.farm.user.id = :userId AND c.sowingDate BETWEEN :startDate AND :endDate")
    Double calculateTotalInputCost(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}