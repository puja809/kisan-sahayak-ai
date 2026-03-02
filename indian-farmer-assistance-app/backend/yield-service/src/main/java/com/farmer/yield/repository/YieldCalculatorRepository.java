package com.farmer.yield.repository;

import com.farmer.yield.entity.YieldCalculator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YieldCalculatorRepository extends JpaRepository<YieldCalculator, Long> {
    Optional<YieldCalculator> findByCommodityAndIsActiveTrue(String commodity);

    Optional<YieldCalculator> findByCommodity(String commodity);

    @Query("SELECT y.commodity FROM YieldCalculator y WHERE y.isActive = true ORDER BY y.commodity")
    List<String> findAllActiveCommodityNames();
}
