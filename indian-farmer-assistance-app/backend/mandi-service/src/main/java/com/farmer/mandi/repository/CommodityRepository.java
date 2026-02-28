package com.farmer.mandi.repository;

import com.farmer.mandi.entity.Commodity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Commodity entity.
 */
@Repository
public interface CommodityRepository extends JpaRepository<Commodity, Long> {

    /**
     * Find commodity by name.
     */
    Optional<Commodity> findByCommodityName(String commodityName);

    /**
     * Find all active commodities.
     */
    List<Commodity> findByIsActiveTrueOrderByCommodityName();

    /**
     * Find all distinct commodity names.
     */
    @Query("SELECT DISTINCT c.commodityName FROM Commodity c WHERE c.isActive = true ORDER BY c.commodityName")
    List<String> findAllDistinctCommodityNames();
}
