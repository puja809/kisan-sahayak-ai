package com.farmer.yield.service;

import com.farmer.yield.dto.YieldCalculationRequest;
import com.farmer.yield.dto.YieldCalculationResponse;
import com.farmer.yield.entity.YieldCalculator;
import com.farmer.yield.repository.YieldCalculatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for yield calculation based on commodity, farm size, and investment
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YieldCalculatorService {

    private final YieldCalculatorRepository yieldCalculatorRepository;

    /**
     * Calculate yield based on commodity, farm size, and investment amount
     * 
     * @param request Yield calculation request with commodity, farm size, and investment
     * @return Yield calculation response with estimates
     */
    public YieldCalculationResponse calculateYield(YieldCalculationRequest request) {
        log.info("Calculating yield for commodity: {}, farm size: {} hectares, investment: {}",
                request.getCommodity(), request.getFarmSizeHectares(), request.getInvestmentAmount());

        YieldCalculator calculator = yieldCalculatorRepository
                .findByCommodityAndIsActiveTrue(request.getCommodity())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Commodity not found or inactive: " + request.getCommodity()));

        return buildYieldResponse(request, calculator);
    }

    /**
     * Build yield calculation response with estimates
     */
    private YieldCalculationResponse buildYieldResponse(YieldCalculationRequest request, YieldCalculator calculator) {
        Double farmSize = request.getFarmSizeHectares();
        Double investment = request.getInvestmentAmount();
        Double baseYield = calculator.getBaseYieldPerHectare();
        Double variance = calculator.getYieldVariancePercent() != null ? calculator.getYieldVariancePercent() : 15.0;

        // Calculate yields based on farm size
        Double expectedYield = baseYield * farmSize;
        Double minYield = expectedYield * (1 - variance / 100);
        Double maxYield = expectedYield * (1 + variance / 100);

        // Calculate revenues
        Double minRevenue = minYield * calculator.getMinPricePerKg();
        Double expectedRevenue = expectedYield * calculator.getAvgPricePerKg();
        Double maxRevenue = maxYield * calculator.getMaxPricePerKg();

        // Calculate profit margin
        Double profitMargin = investment > 0 ? ((expectedRevenue - investment) / investment) * 100 : 0;

        return YieldCalculationResponse.builder()
                .commodity(request.getCommodity())
                .farmSizeHectares(farmSize)
                .investmentAmount(investment)
                .minPricePerKg(calculator.getMinPricePerKg())
                .avgPricePerKg(calculator.getAvgPricePerKg())
                .maxPricePerKg(calculator.getMaxPricePerKg())
                .baseYieldPerHectare(baseYield)
                .estimatedMinYield(Math.round(minYield * 100.0) / 100.0)
                .estimatedExpectedYield(Math.round(expectedYield * 100.0) / 100.0)
                .estimatedMaxYield(Math.round(maxYield * 100.0) / 100.0)
                .estimatedMinRevenue(Math.round(minRevenue * 100.0) / 100.0)
                .estimatedExpectedRevenue(Math.round(expectedRevenue * 100.0) / 100.0)
                .estimatedMaxRevenue(Math.round(maxRevenue * 100.0) / 100.0)
                .profitMarginPercent(Math.round(profitMargin * 100.0) / 100.0)
                .message("Yield calculation completed successfully")
                .success(true)
                .build();
    }
}
