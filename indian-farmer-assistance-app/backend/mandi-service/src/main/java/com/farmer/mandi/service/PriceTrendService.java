package com.farmer.mandi.service;

import com.farmer.mandi.dto.MandiPriceDto;
import com.farmer.mandi.dto.PriceTrendDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for price trend analysis and visualization.
 * 
 * Requirements:
 * - 6.5: Display 30-day price history with graphical visualization
 * - 6.6: MSP vs market price comparison
 * - 6.7: Post-harvest storage advisory (hold vs sell)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceTrendService {

    private final MandiPriceService mandiPriceService;

    // MSP data for common commodities (would typically come from a database or API)
    private static final Map<String, BigDecimal> MSP_DATA = new HashMap<>();
    
    static {
        // Common MSP prices per quintal (2024-2025)
        MSP_DATA.put("Paddy", BigDecimal.valueOf(2300));
        MSP_DATA.put("Wheat", BigDecimal.valueOf(2650));
        MSP_DATA.put("Cotton", BigDecimal.valueOf(6620));
        MSP_DATA.put("Groundnut", BigDecimal.valueOf(6375));
        MSP_DATA.put("Soybean", BigDecimal.valueOf(4892));
        MSP_DATA.put("Sunflower", BigDecimal.valueOf(6760));
        MSP_DATA.put("Mustard", BigDecimal.valueOf(5950));
        MSP_DATA.put("Gram", BigDecimal.valueOf(5440));
        MSP_DATA.put("Masoor", BigDecimal.valueOf(6000));
        MSP_DATA.put("Moong", BigDecimal.valueOf(8559));
        MSP_DATA.put("Urad", BigDecimal.valueOf(8600));
        MSP_DATA.put("Rice", BigDecimal.valueOf(2900));
        MSP_DATA.put("Bajra", BigDecimal.valueOf(2500));
        MSP_DATA.put("Jowar", BigDecimal.valueOf(3180));
        MSP_DATA.put("Ragi", BigDecimal.valueOf(4290));
        MSP_DATA.put("Maize", BigDecimal.valueOf(2250));
        MSP_DATA.put("Sugarcane", BigDecimal.valueOf(315)); // per quintal
    }

    /**
     * Gets price trend analysis for a commodity.
     * 
     * @param commodity The commodity name
     * @param days Number of days of historical data
     * @return PriceTrendDto with trend analysis
     */
    public Mono<PriceTrendDto> getPriceTrend(String commodity, int days) {
        log.info("Getting price trend for commodity: {} over {} days", commodity, days);
        
        return mandiPriceService.getHistoricalPrices(commodity, days)
                .map(priceDtos -> {
                    if (priceDtos.isEmpty()) {
                        return buildEmptyTrend(commodity, days);
                    }
                    return analyzeTrend(priceDtos, commodity, days);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    List<MandiPriceDto> dbPrices = mandiPriceService.getHistoricalPricesFromDatabase(commodity, days);
                    if (!dbPrices.isEmpty()) {
                        dbPrices.forEach(p -> p.setIsCached(true));
                        return Mono.just(analyzeTrend(dbPrices, commodity, days));
                    }
                    return Mono.just(buildEmptyTrend(commodity, days));
                }));
    }

    /**
     * Gets price trend for a commodity with default 30 days.
     * 
     * @param commodity The commodity name
     * @return PriceTrendDto with trend analysis
     */
    public Mono<PriceTrendDto> getPriceTrend(String commodity) {
        return getPriceTrend(commodity, 30);
    }

    /**
     * Gets MSP comparison for a commodity.
     * 
     * @param commodity The commodity name
     * @return MspComparisonDto with MSP vs market price comparison
     */
    public PriceTrendDto.MspComparisonDto getMspComparison(String commodity) {
        log.info("Getting MSP comparison for commodity: {}", commodity);
        
        BigDecimal msp = MSP_DATA.getOrDefault(commodity, BigDecimal.ZERO);
        
        // Get latest market price
        List<MandiPriceDto> latestPrices = mandiPriceService.getLatestPricesFromDatabase(commodity);
        BigDecimal currentMarketPrice = BigDecimal.ZERO;
        
        if (!latestPrices.isEmpty()) {
            currentMarketPrice = latestPrices.stream()
                    .map(MandiPriceDto::getModalPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(latestPrices.size()), 2, RoundingMode.HALF_UP);
        }
        
        BigDecimal difference = currentMarketPrice.subtract(msp);
        String comparisonResult;
        String recommendation;
        
        if (currentMarketPrice.compareTo(msp) > 0) {
            comparisonResult = "ABOVE_MSP";
            recommendation = "Current market prices are above MSP. Consider selling if prices meet your expectations.";
        } else if (currentMarketPrice.compareTo(msp) < 0) {
            comparisonResult = "BELOW_MSP";
            recommendation = "Current market prices are below MSP. Consider holding if storage is available.";
        } else {
            comparisonResult = "AT_MSP";
            recommendation = "Current market prices are at MSP level.";
        }
        
        return PriceTrendDto.MspComparisonDto.builder()
                .msp(msp)
                .currentMarketPrice(currentMarketPrice)
                .difference(difference)
                .comparisonResult(comparisonResult)
                .recommendation(recommendation)
                .build();
    }

    /**
     * Gets storage advisory for a commodity based on price trends.
     * 
     * @param commodity The commodity name
     * @return StorageAdvisoryDto with hold/sell recommendation
     */
    public PriceTrendDto.StorageAdvisoryDto getStorageAdvisory(String commodity) {
        log.info("Getting storage advisory for commodity: {}", commodity);
        
        // Get recent price trend
        List<MandiPriceDto> recentPrices = mandiPriceService.getHistoricalPricesFromDatabase(commodity, 7);
        
        if (recentPrices.size() < 2) {
            return PriceTrendDto.StorageAdvisoryDto.builder()
                    .recommendation("HOLD")
                    .reasoning("Insufficient price data to make a recommendation.")
                    .confidenceLevel(0.5)
                    .expectedPriceChange("Unknown")
                    .suggestedHoldingDays(7)
                    .build();
        }
        
        // Calculate price trend
        MandiPriceDto latest = recentPrices.get(0);
        MandiPriceDto oldest = recentPrices.get(recentPrices.size() - 1);
        
        if (latest.getModalPrice() == null || oldest.getModalPrice() == null) {
            return PriceTrendDto.StorageAdvisoryDto.builder()
                    .recommendation("HOLD")
                    .reasoning("Insufficient price data to make a recommendation.")
                    .confidenceLevel(0.5)
                    .expectedPriceChange("Unknown")
                    .suggestedHoldingDays(7)
                    .build();
        }
        
        BigDecimal priceChange = latest.getModalPrice().subtract(oldest.getModalPrice());
        BigDecimal priceChangePercent = priceChange.multiply(BigDecimal.valueOf(100))
                .divide(oldest.getModalPrice(), 2, RoundingMode.HALF_UP);
        
        String recommendation;
        String reasoning;
        String expectedPriceChange;
        int suggestedHoldingDays;
        double confidenceLevel;
        
        if (priceChangePercent.compareTo(BigDecimal.valueOf(5)) > 0) {
            // Prices are rising
            recommendation = "HOLD";
            reasoning = String.format("Prices have increased by %.2f%% in the last %d days. " +
                    "Consider holding if storage is available to wait for potentially higher prices.",
                    priceChangePercent, recentPrices.size());
            expectedPriceChange = "POTENTIAL_INCREASE";
            suggestedHoldingDays = 14;
            confidenceLevel = 0.7;
        } else if (priceChangePercent.compareTo(BigDecimal.valueOf(-5)) < 0) {
            // Prices are falling
            recommendation = "SELL";
            reasoning = String.format("Prices have decreased by %.2f%% in the last %d days. " +
                    "Consider selling to avoid further losses.",
                    priceChangePercent.abs(), recentPrices.size());
            expectedPriceChange = "POTENTIAL_DECREASE";
            suggestedHoldingDays = 0;
            confidenceLevel = 0.75;
        } else {
            // Prices are stable
            recommendation = "HOLD";
            reasoning = String.format("Prices have been stable (change: %.2f%%) in the last %d days. " +
                    "Consider holding if storage costs are low.",
                    priceChangePercent, recentPrices.size());
            expectedPriceChange = "STABLE";
            suggestedHoldingDays = 7;
            confidenceLevel = 0.6;
        }
        
        return PriceTrendDto.StorageAdvisoryDto.builder()
                .recommendation(recommendation)
                .reasoning(reasoning)
                .confidenceLevel(confidenceLevel)
                .expectedPriceChange(expectedPriceChange)
                .suggestedHoldingDays(suggestedHoldingDays)
                .build();
    }

    /**
     * Analyzes price trend from historical data.
     */
    private PriceTrendDto analyzeTrend(List<MandiPriceDto> priceDtos, String commodity, int days) {
        // Group by date and calculate daily averages
        Map<LocalDate, List<MandiPriceDto>> byDate = priceDtos.stream()
                .collect(Collectors.groupingBy(MandiPriceDto::getPriceDate));
        
        List<PriceTrendDto.PricePointDto> pricePoints = new ArrayList<>();
        
        for (Map.Entry<LocalDate, List<MandiPriceDto>> entry : byDate.entrySet()) {
            List<MandiPriceDto> dayPrices = entry.getValue();
            BigDecimal avgModal = dayPrices.stream()
                    .map(MandiPriceDto::getModalPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(dayPrices.size()), 2, RoundingMode.HALF_UP);
            
            BigDecimal avgMin = dayPrices.stream()
                    .map(MandiPriceDto::getMinPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(dayPrices.size()), 2, RoundingMode.HALF_UP);
            
            BigDecimal avgMax = dayPrices.stream()
                    .map(MandiPriceDto::getMaxPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(dayPrices.size()), 2, RoundingMode.HALF_UP);
            
            BigDecimal totalArrival = dayPrices.stream()
                    .map(MandiPriceDto::getArrivalQuantityQuintals)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            String mandiName = dayPrices.get(0).getMandiName();
            
            pricePoints.add(PriceTrendDto.PricePointDto.builder()
                    .date(entry.getKey())
                    .modalPrice(avgModal)
                    .minPrice(avgMin)
                    .maxPrice(avgMax)
                    .arrivalQuantity(totalArrival)
                    .mandiName(mandiName)
                    .build());
        }
        
        // Sort by date
        pricePoints.sort(Comparator.comparing(PriceTrendDto.PricePointDto::getDate));
        
        // Calculate trend analysis
        PriceTrendDto.TrendAnalysisDto trendAnalysis = calculateTrendAnalysis(pricePoints);
        
        // Get MSP comparison
        PriceTrendDto.MspComparisonDto mspComparison = getMspComparison(commodity);
        
        // Get storage advisory
        PriceTrendDto.StorageAdvisoryDto storageAdvisory = getStorageAdvisory(commodity);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        return PriceTrendDto.builder()
                .commodityName(commodity)
                .variety(priceDtos.isEmpty() ? null : priceDtos.get(0).getVariety())
                .state(priceDtos.isEmpty() ? null : priceDtos.get(0).getState())
                .district(priceDtos.isEmpty() ? null : priceDtos.get(0).getDistrict())
                .priceHistory(pricePoints)
                .trendAnalysis(trendAnalysis)
                .mspComparison(mspComparison)
                .storageAdvisory(storageAdvisory)
                .startDate(startDate)
                .endDate(endDate)
                .totalDataPoints(pricePoints.size())
                .build();
    }

    /**
     * Calculates trend analysis from price points.
     */
    private PriceTrendDto.TrendAnalysisDto calculateTrendAnalysis(List<PriceTrendDto.PricePointDto> pricePoints) {
        if (pricePoints.size() < 2) {
            return PriceTrendDto.TrendAnalysisDto.builder()
                    .trendDirection("UNKNOWN")
                    .priceChangePercent(BigDecimal.ZERO)
                    .averagePrice(BigDecimal.ZERO)
                    .highestPrice(BigDecimal.ZERO)
                    .lowestPrice(BigDecimal.ZERO)
                    .priceVolatility(BigDecimal.ZERO)
                    .build();
        }
        
        BigDecimal firstPrice = pricePoints.get(0).getModalPrice();
        BigDecimal lastPrice = pricePoints.get(pricePoints.size() - 1).getModalPrice();
        
        BigDecimal priceChange = lastPrice.subtract(firstPrice);
        BigDecimal priceChangePercent = firstPrice.compareTo(BigDecimal.ZERO) != 0 
                ? priceChange.multiply(BigDecimal.valueOf(100)).divide(firstPrice, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        String trendDirection;
        if (priceChangePercent.compareTo(BigDecimal.valueOf(2)) > 0) {
            trendDirection = "INCREASING";
        } else if (priceChangePercent.compareTo(BigDecimal.valueOf(-2)) < 0) {
            trendDirection = "DECREASING";
        } else {
            trendDirection = "STABLE";
        }
        
        BigDecimal averagePrice = pricePoints.stream()
                .map(PriceTrendDto.PricePointDto::getModalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(pricePoints.size()), 2, RoundingMode.HALF_UP);
        
        BigDecimal highestPrice = pricePoints.stream()
                .map(PriceTrendDto.PricePointDto::getModalPrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal lowestPrice = pricePoints.stream()
                .map(PriceTrendDto.PricePointDto::getModalPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        // Calculate volatility (standard deviation)
        BigDecimal mean = averagePrice;
        BigDecimal sumSquaredDiff = pricePoints.stream()
                .map(p -> p.getModalPrice() != null ? p.getModalPrice() : BigDecimal.ZERO)
                .map(price -> price.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = sumSquaredDiff.divide(
                BigDecimal.valueOf(pricePoints.size()), 4, RoundingMode.HALF_UP);
        BigDecimal volatility = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()))
                .setScale(2, RoundingMode.HALF_UP);
        
        return PriceTrendDto.TrendAnalysisDto.builder()
                .trendDirection(trendDirection)
                .priceChangePercent(priceChangePercent)
                .averagePrice(averagePrice)
                .highestPrice(highestPrice)
                .lowestPrice(lowestPrice)
                .priceVolatility(volatility)
                .build();
    }

    /**
     * Builds an empty trend response.
     */
    private PriceTrendDto buildEmptyTrend(String commodity, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        return PriceTrendDto.builder()
                .commodityName(commodity)
                .priceHistory(Collections.emptyList())
                .trendAnalysis(PriceTrendDto.TrendAnalysisDto.builder()
                        .trendDirection("UNKNOWN")
                        .priceChangePercent(BigDecimal.ZERO)
                        .averagePrice(BigDecimal.ZERO)
                        .highestPrice(BigDecimal.ZERO)
                        .lowestPrice(BigDecimal.ZERO)
                        .priceVolatility(BigDecimal.ZERO)
                        .build())
                .mspComparison(PriceTrendDto.MspComparisonDto.builder()
                        .msp(MSP_DATA.getOrDefault(commodity, BigDecimal.ZERO))
                        .currentMarketPrice(BigDecimal.ZERO)
                        .difference(BigDecimal.ZERO)
                        .comparisonResult("UNKNOWN")
                        .recommendation("No market data available")
                        .build())
                .storageAdvisory(PriceTrendDto.StorageAdvisoryDto.builder()
                        .recommendation("HOLD")
                        .reasoning("No price data available to make a recommendation")
                        .confidenceLevel(0.5)
                        .expectedPriceChange("Unknown")
                        .suggestedHoldingDays(7)
                        .build())
                .startDate(startDate)
                .endDate(endDate)
                .totalDataPoints(0)
                .build();
    }
}