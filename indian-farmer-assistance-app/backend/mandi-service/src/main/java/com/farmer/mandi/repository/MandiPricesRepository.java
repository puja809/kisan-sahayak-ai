package com.farmer.mandi.repository;

import com.farmer.mandi.entity.MandiPrices;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for MandiPrices entity.
 * 
 * Requirements:
 * - 6.1: Retrieve current prices from AGMARKNET API
 * - 6.2: Display modal price, min price, max price, arrival quantity, variety
 * - 6.11: Display cached prices with timestamp when AGMARKNET unavailable
 */
@Repository
public interface MandiPricesRepository extends JpaRepository<MandiPrices, Long> {

    /**
     * Find prices by commodity name.
     */
    List<MandiPrices> findByCommodityNameOrderByPriceDateDesc(String commodityName);

    /**
     * Find prices by commodity name and variety.
     */
    List<MandiPrices> findByCommodityNameAndVarietyOrderByPriceDateDesc(String commodityName, String variety);

    /**
     * Find prices by commodity and date.
     */
    List<MandiPrices> findByCommodityNameAndPriceDate(String commodityName, LocalDate priceDate);

    /**
     * Find prices by commodity, variety, and date.
     */
    List<MandiPrices> findByCommodityNameAndVarietyAndPriceDate(
            String commodityName, String variety, LocalDate priceDate);

    /**
     * Find prices by mandi name.
     */
    List<MandiPrices> findByMandiNameOrderByPriceDateDesc(String mandiName);

    /**
     * Find prices by state.
     */
    List<MandiPrices> findByStateOrderByPriceDateDesc(String state);

    /**
     * Find prices by state and district.
     */
    List<MandiPrices> findByStateAndDistrictOrderByPriceDateDesc(String state, String district);

    /**
     * Find prices by commodity and state.
     */
    List<MandiPrices> findByCommodityNameAndStateOrderByPriceDateDesc(String commodityName, String state);

    /**
     * Find prices by commodity and district.
     */
    List<MandiPrices> findByCommodityNameAndDistrictOrderByPriceDateDesc(String commodityName, String district);

    /**
     * Find historical prices for trend analysis.
     */
    @Query("SELECT m FROM MandiPrices m WHERE m.commodityName = :commodity " +
           "AND m.priceDate >= :startDate AND m.priceDate <= :endDate " +
           "ORDER BY m.priceDate ASC")
    List<MandiPrices> findHistoricalPrices(
            @Param("commodity") String commodityName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find historical prices for a specific variety.
     */
    @Query("SELECT m FROM MandiPrices m WHERE m.commodityName = :commodity " +
           "AND m.variety = :variety " +
           "AND m.priceDate >= :startDate AND m.priceDate <= :endDate " +
           "ORDER BY m.priceDate ASC")
    List<MandiPrices> findHistoricalPricesByVariety(
            @Param("commodity") String commodityName,
            @Param("variety") String variety,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find latest prices for a commodity.
     */
    @Query("SELECT m FROM MandiPrices m WHERE m.commodityName = :commodity " +
           "AND m.priceDate = (SELECT MAX(m2.priceDate) FROM MandiPrices m2 WHERE m2.commodityName = :commodity)")
    List<MandiPrices> findLatestPricesByCommodity(@Param("commodity") String commodityName);

    /**
     * Find latest prices for a commodity in a specific state.
     */
    @Query("SELECT m FROM MandiPrices m WHERE m.commodityName = :commodity " +
           "AND m.state = :state " +
           "AND m.priceDate = (SELECT MAX(m2.priceDate) FROM MandiPrices m2 " +
           "WHERE m2.commodityName = :commodity AND m2.state = :state)")
    List<MandiPrices> findLatestPricesByCommodityAndState(
            @Param("commodity") String commodityName,
            @Param("state") String state);

    /**
     * Find latest prices for a commodity in neighboring districts.
     */
    @Query("SELECT m FROM MandiPrices m WHERE m.commodityName = :commodity " +
           "AND m.district IN :districts " +
           "AND m.priceDate = (SELECT MAX(m2.priceDate) FROM MandiPrices m2 " +
           "WHERE m2.commodityName = :commodity AND m2.district IN :districts)")
    List<MandiPrices> findLatestPricesInNeighboringDistricts(
            @Param("commodity") String commodityName,
            @Param("districts") List<String> districts);

    /**
     * Find price by commodity, variety, mandi code, and date.
     */
    Optional<MandiPrices> findByCommodityNameAndVarietyAndMandiCodeAndPriceDate(
            String commodityName, String variety, String mandiCode, LocalDate priceDate);

    /**
     * Find distinct commodities.
     */
    @Query("SELECT DISTINCT m.commodityName FROM MandiPrices m ORDER BY m.commodityName")
    List<String> findDistinctCommodities();

    /**
     * Find distinct states.
     */
    @Query("SELECT DISTINCT m.state FROM MandiPrices m ORDER BY m.state")
    List<String> findDistinctStates();

    /**
     * Find distinct districts.
     */
    @Query("SELECT DISTINCT m.district FROM MandiPrices m WHERE m.state = :state ORDER BY m.district")
    List<String> findDistinctDistrictsByState(@Param("state") String state);

    /**
     * Find distinct mandis by state.
     */
    @Query("SELECT DISTINCT m.mandiName FROM MandiPrices m WHERE m.state = :state ORDER BY m.mandiName")
    List<String> findDistinctMandiNamesByState(@Param("state") String state);

    /**
     * Count prices by commodity and date.
     */
    long countByCommodityNameAndPriceDate(String commodityName, LocalDate priceDate);
}