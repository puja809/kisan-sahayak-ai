package com.farmer.mandi.repository;

import com.farmer.mandi.entity.Commodity;
import com.farmer.mandi.entity.Variety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Variety entity.
 */
@Repository
public interface VarietyRepository extends JpaRepository<Variety, Long> {

    /**
     * Find varieties by commodity.
     */
    List<Variety> findByCommodityAndIsActiveTrueOrderByVarietyName(Commodity commodity);

    /**
     * Find varieties by commodity ID.
     */
    @Query("SELECT v FROM Variety v WHERE v.commodity.id = :commodityId AND v.isActive = true ORDER BY v.varietyName")
    List<Variety> findByCommodityId(@Param("commodityId") Long commodityId);

    /**
     * Find variety by commodity and variety name.
     */
    Optional<Variety> findByCommodityAndVarietyName(Commodity commodity, String varietyName);

    /**
     * Find all distinct variety names.
     */
    @Query("SELECT DISTINCT v.varietyName FROM Variety v WHERE v.isActive = true ORDER BY v.varietyName")
    List<String> findAllDistinctVarietyNames();
}
