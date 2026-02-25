package com.farmer.mandi.repository;

import com.farmer.mandi.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PriceAlert entity.
 * 
 * Requirements:
 * - 6.10: Create price alert subscription endpoints
 */
@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    /**
     * Find all alerts for a farmer.
     */
    List<PriceAlert> findByFarmerIdAndIsActiveTrue(String farmerId);

    /**
     * Find all alerts for a farmer including inactive.
     */
    List<PriceAlert> findByFarmerIdOrderByCreatedAtDesc(String farmerId);

    /**
     * Find alerts by commodity.
     */
    List<PriceAlert> findByCommodityAndIsActiveTrue(String commodity);

    /**
     * Find alerts by commodity and farmer.
     */
    List<PriceAlert> findByCommodityAndFarmerIdAndIsActiveTrue(String commodity, String farmerId);

    /**
     * Find alerts for neighboring districts only.
     */
    List<PriceAlert> findByNeighboringDistrictsOnlyAndIsActiveTrue(Boolean neighboringDistrictsOnly);

    /**
     * Find active alerts that haven't been notified recently.
     */
    @Query("SELECT p FROM PriceAlert p WHERE p.isActive = true " +
           "AND (p.lastNotificationAt IS NULL OR p.lastNotificationAt < :threshold)")
    List<PriceAlert> findActiveAlertsNeedingNotification(@Param("threshold") java.time.LocalDateTime threshold);

    /**
     * Count active alerts for a farmer.
     */
    long countByFarmerIdAndIsActiveTrue(String farmerId);

    /**
     * Find alert by farmer, commodity, and variety.
     */
    List<PriceAlert> findByFarmerIdAndCommodityAndVarietyAndIsActiveTrue(
            String farmerId, String commodity, String variety);
}