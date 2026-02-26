package com.farmer.mandi.repository;

import com.farmer.mandi.entity.MandiMarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for MandiMarketData
 */
@Repository
public interface MandiMarketDataRepository extends JpaRepository<MandiMarketData, Long> {
    
    /**
     * Find all unique states
     */
    @Query("SELECT DISTINCT m.state FROM MandiMarketData m ORDER BY m.state")
    List<String> findAllStates();
    
    /**
     * Find all districts by state
     */
    @Query("SELECT DISTINCT m.district FROM MandiMarketData m WHERE m.state = :state ORDER BY m.district")
    List<String> findDistrictsByState(@Param("state") String state);
    
    /**
     * Find all markets by state and district
     */
    @Query("SELECT DISTINCT m.market FROM MandiMarketData m WHERE m.state = :state AND m.district = :district ORDER BY m.market")
    List<String> findMarketsByStateAndDistrict(@Param("state") String state, @Param("district") String district);
    
    /**
     * Find all commodities by market
     */
    @Query("SELECT DISTINCT m.commodity FROM MandiMarketData m WHERE m.market = :market ORDER BY m.commodity")
    List<String> findCommoditiesByMarket(@Param("market") String market);
    
    /**
     * Find all varieties by commodity
     */
    @Query("SELECT DISTINCT m.variety FROM MandiMarketData m WHERE m.commodity = :commodity ORDER BY m.variety")
    List<String> findVarietiesByCommodity(@Param("commodity") String commodity);
    
    /**
     * Find all grades by commodity and variety
     */
    @Query("SELECT DISTINCT m.grade FROM MandiMarketData m WHERE m.commodity = :commodity AND m.variety = :variety ORDER BY m.grade")
    List<String> findGradesByCommodityAndVariety(@Param("commodity") String commodity, @Param("variety") String variety);
    
    /**
     * Find all commodities
     */
    @Query("SELECT DISTINCT m.commodity FROM MandiMarketData m ORDER BY m.commodity")
    List<String> findAllCommodities();
    
    /**
     * Find all varieties
     */
    @Query("SELECT DISTINCT m.variety FROM MandiMarketData m ORDER BY m.variety")
    List<String> findAllVarieties();
    
    /**
     * Find all grades
     */
    @Query("SELECT DISTINCT m.grade FROM MandiMarketData m ORDER BY m.grade")
    List<String> findAllGrades();
    
    /**
     * Find market data by filters
     */
    @Query("SELECT m FROM MandiMarketData m WHERE " +
           "(:state IS NULL OR m.state = :state) AND " +
           "(:district IS NULL OR m.district = :district) AND " +
           "(:market IS NULL OR m.market = :market) AND " +
           "(:commodity IS NULL OR m.commodity = :commodity) AND " +
           "(:variety IS NULL OR m.variety = :variety) AND " +
           "(:grade IS NULL OR m.grade = :grade)")
    List<MandiMarketData> findByFilters(
        @Param("state") String state,
        @Param("district") String district,
        @Param("market") String market,
        @Param("commodity") String commodity,
        @Param("variety") String variety,
        @Param("grade") String grade
    );
    
    /**
     * Check if data exists
     */
    boolean existsByStateAndDistrictAndMarketAndCommodityAndVarietyAndGrade(
        String state, String district, String market, String commodity, String variety, String grade
    );
    
    /**
     * Delete all market data
     */
    void deleteAll();
    
    /**
     * Count total records
     */
    long count();
}
