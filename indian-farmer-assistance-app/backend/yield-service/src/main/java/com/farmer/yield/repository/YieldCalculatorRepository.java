package com.farmer.yield.repository;

import com.farmer.yield.entity.YieldCalculator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface YieldCalculatorRepository extends JpaRepository<YieldCalculator, Long> {
    Optional<YieldCalculator> findByCommodityAndIsActiveTrue(String commodity);
    Optional<YieldCalculator> findByCommodity(String commodity);
}
