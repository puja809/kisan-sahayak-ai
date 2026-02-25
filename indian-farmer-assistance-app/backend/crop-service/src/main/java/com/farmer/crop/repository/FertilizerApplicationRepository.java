package com.farmer.crop.repository;

import com.farmer.crop.entity.FertilizerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository for FertilizerApplication entity.
 * 
 * Provides methods for:
 * - Finding applications by crop ID
 * - Finding applications by farmer ID
 * - Calculating total nutrient input
 * - Getting application history
 * 
 * Validates: Requirements 11C.6, 11C.7
 */
@Repository
public interface FertilizerApplicationRepository extends JpaRepository<FertilizerApplication, Long> {

    /**
     * Find all fertilizer applications for a crop
     */
    List<FertilizerApplication> findByCropIdOrderByApplicationDateAsc(Long cropId);

    /**
     * Find all fertilizer applications for a farmer
     */
    List<FertilizerApplication> findByFarmerIdOrderByApplicationDateDesc(String farmerId);

    /**
     * Find fertilizer applications by crop ID and date range
     */
    List<FertilizerApplication> findByCropIdAndApplicationDateBetween(
            Long cropId, LocalDate startDate, LocalDate endDate);

    /**
     * Find fertilizer applications by farmer and date range
     */
    List<FertilizerApplication> findByFarmerIdAndApplicationDateBetween(
            String farmerId, LocalDate startDate, LocalDate endDate);

    /**
     * Find fertilizer applications by type
     */
    List<FertilizerApplication> findByFarmerIdAndFertilizerTypeContainingIgnoreCase(
            String farmerId, String fertilizerType);

    /**
     * Calculate total nitrogen applied for a crop
     */
    @Query("SELECT COALESCE(SUM(f.nitrogenPercent * f.quantityKg / 100), 0) " +
           "FROM FertilizerApplication f WHERE f.cropId = :cropId")
    BigDecimal calculateTotalNitrogenForCrop(@Param("cropId") Long cropId);

    /**
     * Calculate total phosphorus applied for a crop
     */
    @Query("SELECT COALESCE(SUM(f.phosphorusPercent * f.quantityKg / 100), 0) " +
           "FROM FertilizerApplication f WHERE f.cropId = :cropId")
    BigDecimal calculateTotalPhosphorusForCrop(@Param("cropId") Long cropId);

    /**
     * Calculate total potassium applied for a crop
     */
    @Query("SELECT COALESCE(SUM(f.potassiumPercent * f.quantityKg / 100), 0) " +
           "FROM FertilizerApplication f WHERE f.cropId = :cropId")
    BigDecimal calculateTotalPotassiumForCrop(@Param("cropId") Long cropId);

    /**
     * Calculate total cost for a crop
     */
    @Query("SELECT COALESCE(SUM(f.cost), 0) FROM FertilizerApplication f WHERE f.cropId = :cropId")
    BigDecimal calculateTotalCostForCrop(@Param("cropId") Long cropId);

    /**
     * Calculate total quantity for a crop
     */
    @Query("SELECT COALESCE(SUM(f.quantityKg), 0) FROM FertilizerApplication f WHERE f.cropId = :cropId")
    BigDecimal calculateTotalQuantityForCrop(@Param("cropId") Long cropId);

    /**
     * Get nutrient summary for a crop
     */
    @Query("SELECT " +
           "COALESCE(SUM(f.nitrogenPercent * f.quantityKg / 100), 0), " +
           "COALESCE(SUM(f.phosphorusPercent * f.quantityKg / 100), 0), " +
           "COALESCE(SUM(f.potassiumPercent * f.quantityKg / 100), 0), " +
           "COALESCE(SUM(f.sulfurPercent * f.quantityKg / 100), 0), " +
           "COALESCE(SUM(f.zincPercent * f.quantityKg / 100), 0), " +
           "COALESCE(SUM(f.quantityKg), 0), " +
           "COALESCE(SUM(f.cost), 0) " +
           "FROM FertilizerApplication f WHERE f.cropId = :cropId")
    Object[] getNutrientSummaryForCrop(@Param("cropId") Long cropId);

    /**
     * Count applications for a crop
     */
    long countByCropId(Long cropId);

    /**
     * Find latest applications for a farmer
     */
    List<FertilizerApplication> findTop10ByFarmerIdOrderByApplicationDateDesc(String farmerId);

    /**
     * Delete all applications for a crop
     */
    void deleteByCropId(Long cropId);
}