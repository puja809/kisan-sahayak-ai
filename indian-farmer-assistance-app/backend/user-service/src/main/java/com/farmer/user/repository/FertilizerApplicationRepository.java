package com.farmer.user.repository;

import com.farmer.user.entity.FertilizerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository for FertilizerApplication entity with custom query methods.
 * Requirements: 11A.4, 11C.6, 11C.7
 */
@Repository
public interface FertilizerApplicationRepository extends JpaRepository<FertilizerApplication, Long> {

    /**
     * Find all fertilizer applications for a crop.
     * Requirements: 11A.4
     */
    List<FertilizerApplication> findByCropId(Long cropId);

    /**
     * Find fertilizer applications by crop and date range.
     * Requirements: 11A.4
     */
    List<FertilizerApplication> findByCropIdAndApplicationDateBetween(
            Long cropId, LocalDate startDate, LocalDate endDate);

    /**
     * Find fertilizer applications by type.
     * Requirements: 11C.6
     */
    List<FertilizerApplication> findByCropFarmUserIdAndFertilizerType(Long userId, String fertilizerType);

    /**
     * Find fertilizer applications by category.
     * Requirements: 11C.6
     */
    List<FertilizerApplication> findByCropFarmUserIdAndFertilizerCategory(
            Long userId, FertilizerApplication.FertilizerCategory category);

    /**
     * Calculate total nitrogen applied to a crop.
     * Requirements: 11C.7
     */
    @Query("SELECT COALESCE(SUM(f.quantityKg * f.nitrogenContentPercent / 100), 0) FROM FertilizerApplication f WHERE f.crop.id = :cropId")
    BigDecimal calculateTotalNitrogenForCrop(@Param("cropId") Long cropId);

    /**
     * Calculate total phosphorus applied to a crop.
     * Requirements: 11C.7
     */
    @Query("SELECT COALESCE(SUM(f.quantityKg * f.phosphorusContentPercent / 100), 0) FROM FertilizerApplication f WHERE f.crop.id = :cropId")
    BigDecimal calculateTotalPhosphorusForCrop(@Param("cropId") Long cropId);

    /**
     * Calculate total potassium applied to a crop.
     * Requirements: 11C.7
     */
    @Query("SELECT COALESCE(SUM(f.quantityKg * f.potassiumContentPercent / 100), 0) FROM FertilizerApplication f WHERE f.crop.id = :cropId")
    BigDecimal calculateTotalPotassiumForCrop(@Param("cropId") Long cropId);

    /**
     * Calculate total cost of fertilizer applications for a crop.
     * Requirements: 11C.6
     */
    @Query("SELECT COALESCE(SUM(f.cost), 0) FROM FertilizerApplication f WHERE f.crop.id = :cropId")
    BigDecimal calculateTotalCostForCrop(@Param("cropId") Long cropId);

    /**
     * Find recent fertilizer applications for a user.
     * Requirements: 11C.6
     */
    @Query("SELECT f FROM FertilizerApplication f WHERE f.crop.farm.user.id = :userId ORDER BY f.applicationDate DESC")
    List<FertilizerApplication> findRecentByUserId(@Param("userId") Long userId);

    /**
     * Count fertilizer applications for a crop.
     */
    long countByCropId(Long cropId);
}