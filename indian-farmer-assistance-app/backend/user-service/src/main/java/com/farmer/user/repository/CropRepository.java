package com.farmer.user.repository;

import com.farmer.user.entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CropRepository extends JpaRepository<Crop, Long> {

        List<Crop> findByUserId(Long userId);

        @Query("SELECT c FROM Crop c WHERE c.user.id = :userId ORDER BY c.sowingDate DESC")
        List<Crop> findByUserIdOrderBySowingDateDesc(@Param("userId") Long userId);

        @Query("SELECT c FROM Crop c WHERE c.user.id = :userId AND c.status IN ('SOWN', 'GROWING') ORDER BY c.sowingDate DESC")
        List<Crop> findCurrentCropsByUserId(@Param("userId") Long userId);

        List<Crop> findByUserIdAndStatus(Long userId, Crop.CropStatus status);

        List<Crop> findByUserIdAndCropName(Long userId, String cropName);

        List<Crop> findByUserIdAndSeason(Long userId, Crop.Season season);

        @Query("SELECT c FROM Crop c WHERE c.user.id = :userId AND c.sowingDate BETWEEN :startDate AND :endDate ORDER BY c.sowingDate DESC")
        List<Crop> findByUserIdAndSowingDateBetween(
                        @Param("userId") Long userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT c FROM Crop c WHERE c.user.id = :userId AND c.expectedHarvestDate BETWEEN :startDate AND :endDate AND c.status = 'GROWING'")
        List<Crop> findUpcomingHarvests(
                        @Param("userId") Long userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        Optional<Crop> findByIdAndUserId(Long id, Long userId);

        long countByUserIdAndStatus(Long userId, Crop.CropStatus status);

        @Query("SELECT COALESCE(SUM(c.totalYieldQuintals), 0) FROM Crop c WHERE c.user.id = :userId AND c.status = 'HARVESTED' AND c.actualHarvestDate BETWEEN :startDate AND :endDate")
        Double calculateTotalYield(
                        @Param("userId") Long userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT COALESCE(SUM(c.totalRevenue), 0) FROM Crop c WHERE c.user.id = :userId AND c.status = 'HARVESTED' AND c.actualHarvestDate BETWEEN :startDate AND :endDate")
        Double calculateTotalRevenue(
                        @Param("userId") Long userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT COALESCE(SUM(c.totalInputCost), 0) FROM Crop c WHERE c.user.id = :userId AND c.sowingDate BETWEEN :startDate AND :endDate")
        Double calculateTotalInputCost(
                        @Param("userId") Long userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}