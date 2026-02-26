package com.farmer.mandi.service;

import com.farmer.mandi.dto.MandiPriceDto;
import com.farmer.mandi.dto.PriceTrendDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;



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
    private static final Map<String, Double> MSP_DATA = new HashMap<>();
    
    static {
        // Common MSP prices per quintal (2024-2025)
        MSP_DATA.put("Paddy", Double.valueOf(2300));
        MSP_DATA.put("Wheat", Double.valueOf(2650));
        MSP_DATA.put("Cotton", Double.valueOf(6620));
        MSP_DATA.put("Groundnut", Double.valueOf(6375));
        MSP_DATA.put("Soybean", Double.valueOf(4892));
        MSP_DATA.put("Sunflower", Double.valueOf(6760));
        MSP_DATA.put("Mustard", Double.valueOf(5950));
        MSP_DATA.put("Gram", Double.valueOf(5440));
        MSP_DATA.put("Masoor", Double.valueOf(6000));
        MSP_DATA.put("Moong", Double.valueOf(8559));
        MSP_DATA.put("Urad", Double.valueOf(8600));
        MSP_DATA.put("Rice", Double.valueOf(2900));
        MSP_DATA.put("Bajra", Double.valueOf(2500));
        MSP_DATA.put("Jowar", Double.valueOf(3180));
        MSP_DATA.put("Ragi", Double.valueOf(4290));
        MSP_DATA.put("Maize", Double.valueOf(2250));
        MSP_DATA.put("Sugarcane", Double.valueOf(315)); // per quintal
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
        
        Double msp = MSP_DATA.getOrDefault(commodity, 0.0);
        
        // Get latest market price
        List<MandiPriceDto> latestPrices = mandiPriceService.getLatestPricesFromDatabase(commodity);
        Double currentMarketPrice = 0.0;
        
        if (!latestPrices.isEmpty()) {
            currentMarketPrice = latestPrices.stream()
                    .map(MandiPriceDto::getModalPrice)
                    .filter(Objects::nonNull)
                    .reduce(0.0, (a, b) -> a + b) / latestPrices.size();
        }
        
        Double difference = currentMarketPrice - msp;
        String comparisonResult;
        String recommendation;
        
        if (currentMarketPrice > msp) {
            comparisonResult = "ABOVE_MSP";
            recommendation = "Current market prices are above MSP. Consider selling if prices meet your expectations.";
        } else if (currentMarketPrice < msp) {
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
        
        Double priceChange = latest.getModalPrice() - oldest.getModalPrice();
        Double priceChangePercent = priceChange * 100.0 / oldest.getModalPrice();
        
        String recommendation;
        String reasoning;
        String expectedPriceChange;
        int suggestedHoldingDays;
        double confidenceLevel;
        
        if (priceChangePercent.compareTo(5.0) > 0) {
            // Prices are rising
            recommendation = "HOLD";
            reasoning = String.format("Prices have increased by %.2f%% in the last %d days. " +
                    "Consider holding if storage is available to wait for potentially higher prices.",
                    priceChangePercent, recentPrices.size());
            expectedPriceChange = "POTENTIAL_INCREASE";
            suggestedHoldingDays = 14;
            confidenceLevel = 0.7;
        } else if (priceChangePercent.compareTo(-5.0) < 0) {
            // Prices are falling
            recommendation = "SELL";
            reasoning = String.format("Prices have decreased by %.2f%% in the last %d days. " +
                    "Consider selling to avoid further losses.",
                    Math.abs(priceChangePercent), recentPrices.size());
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
            Double avgModal = dayPrices.stream()
                    .map(MandiPriceDto::getModalPrice)
                    .filter(Objects::nonNull)
                    .reduce(0.0, (a, b) -> a + b) / dayPrices.size();
            
            Double avgMin = dayPrices.stream()
                    .map(MandiPriceDto::getMinPrice)
                    .filter(Objects::nonNull)
                    .reduce(0.0, (a, b) -> a + b) / dayPrices.size();
            
            Double avgMax = dayPrices.stream()
                    .map(MandiPriceDto::getMaxPrice)
                    .filter(Objects::nonNull)
                    .reduce(0.0, (a, b) -> a + b) / dayPrices.size();
            
            Double totalArrival = dayPrices.stream()
                    .map(MandiPriceDto::getArrivalQuantityQuintals)
                    .filter(Objects::nonNull)
                    .reduce(0.0, (a, b) -> a + b);
            
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
                    .priceChangePercent(0.0)
                    .averagePrice(0.0)
                    .highestPrice(0.0)
                    .lowestPrice(0.0)
                    .priceVolatility(0.0)
                    .build();
        }
        
        Double firstPrice = pricePoints.get(0).getModalPrice();
        Double lastPrice = pricePoints.get(pricePoints.size() - 1).getModalPrice();
        
        Double priceChange = lastPrice - firstPrice;
        Double priceChangePercent = firstPrice.compareTo(0.0) != 0 
                ? priceChange * 100.0 / firstPrice
                : 0.0;
        
        String trendDirection;
        if (priceChangePercent.compareTo(2.0) > 0) {
            trendDirection = "INCREASING";
        } else if (priceChangePercent.compareTo(-2.0) < 0) {
            trendDirection = "DECREASING";
        } else {
            trendDirection = "STABLE";
        }
        
        Double averagePrice = pricePoints.stream()
                .map(PriceTrendDto.PricePointDto::getModalPrice)
                .filter(Objects::nonNull)
                .reduce(0.0, (a, b) -> a + b) / pricePoints.size();
        
        Double highestPrice = pricePoints.stream()
                .map(PriceTrendDto.PricePointDto::getModalPrice)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(0.0);
        
        Double lowestPrice = pricePoints.stream()
                .map(PriceTrendDto.PricePointDto::getModalPrice)
                .filter(Objects::nonNull)
                .min(Double::compareTo)
                .orElse(0.0);
        
        // Calculate volatility (standard deviation)
        Double mean = averagePrice;
        Double sumSquaredDiff = pricePoints.stream()
                .map(p -> p.getModalPrice() != null ? p.getModalPrice() : 0.0)
                .map(price -> Math.pow(price - mean, 2))
                .reduce(0.0, (a, b) -> a + b);
        Double variance = sumSquaredDiff / pricePoints.size();
        Double volatility = Math.sqrt(variance);
        
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
                        .priceChangePercent(0.0)
                        .averagePrice(0.0)
                        .highestPrice(0.0)
                        .lowestPrice(0.0)
                        .priceVolatility(0.0)
                        .build())
                .mspComparison(PriceTrendDto.MspComparisonDto.builder()
                        .msp(MSP_DATA.getOrDefault(commodity, 0.0))
                        .currentMarketPrice(0.0)
                        .difference(0.0)
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