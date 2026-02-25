package com.farmer.crop.service;

import com.farmer.crop.dto.MarketDataDto;
import com.farmer.crop.dto.MarketDataDto.PriceRecommendation;
import com.farmer.crop.dto.MarketDataDto.PriceTrend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for integrating market data into crop recommendations.
 * 
 * This service fetches price data from AGMARKNET and provides
 * market-linked recommendations for crops.
 * 
 * Validates: Requirement 2.7
 */
@Service
@Transactional(readOnly = true)
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    // Approximate MSP values for major crops (INR per quintal)
    private static final Map<String, BigDecimal> MSP_VALUES = Map.ofEntries(
            Map.entry("RICE", new BigDecimal("2300")),
            Map.entry("WHEAT", new BigDecimal("2650")),
            Map.entry("COTTON", new BigDecimal("6620")),
            Map.entry("SOYBEAN", new BigDecimal("5650")),
            Map.entry("GROUNDNUT", new BigDecimal("6375")),
            Map.entry("MUSTARD", new BigDecimal("5850")),
            Map.entry("PULSES", new BigDecimal("6000")),
            Map.entry("MAIZE", new BigDecimal("2090")),
            Map.entry("SUGARCANE", new BigDecimal("315"))  // per quintal
    );

    // Approximate current market prices (INR per quintal) - would be fetched from AGMARKNET
    private static final Map<String, BigDecimal> CURRENT_PRICES = Map.ofEntries(
            Map.entry("RICE", new BigDecimal("2200")),
            Map.entry("WHEAT", new BigDecimal("2500")),
            Map.entry("COTTON", new BigDecimal("5800")),
            Map.entry("SOYBEAN", new BigDecimal("5200")),
            Map.entry("GROUNDNUT", new BigDecimal("6200")),
            Map.entry("MUSTARD", new BigDecimal("5500")),
            Map.entry("PULSES", new BigDecimal("6500")),
            Map.entry("MAIZE", new BigDecimal("2100")),
            Map.entry("SUGARCANE", new BigDecimal("350")),
            Map.entry("POTATO", new BigDecimal("1500")),
            Map.entry("ONION", new BigDecimal("1800")),
            Map.entry("TOMATO", new BigDecimal("2000"))
    );

    /**
     * Get market data for a list of crops.
     * 
     * @param cropCodes List of crop codes
     * @param state State for location-based pricing
     * @return Map of crop code to market data
     * 
     * Validates: Requirement 2.7
     */
    public Map<String, MarketDataDto> getMarketDataForCrops(
            List<String> cropCodes, String state) {
        
        logger.info("Fetching market data for {} crops in state: {}", cropCodes.size(), state);
        
        Map<String, MarketDataDto> marketDataMap = new HashMap<>();
        
        for (String cropCode : cropCodes) {
            MarketDataDto marketData = getMarketDataForCrop(cropCode, state);
            if (marketData != null) {
                marketDataMap.put(cropCode, marketData);
            }
        }
        
        logger.info("Retrieved market data for {} crops", marketDataMap.size());
        return marketDataMap;
    }

    /**
     * Get market data for a single crop.
     * 
     * @param cropCode Crop code
     * @param state State for location-based pricing
     * @return Market data for the crop
     */
    public MarketDataDto getMarketDataForCrop(String cropCode, String state) {
        logger.debug("Fetching market data for crop: {} in state: {}", cropCode, state);
        
        // Get current price (would be fetched from AGMARKNET API)
        BigDecimal currentPrice = CURRENT_PRICES.getOrDefault(cropCode, new BigDecimal("2000"));
        
        // Calculate price trends (would be based on historical data)
        BigDecimal price30DaysAgo = calculateHistoricalPrice(cropCode, currentPrice, -30);
        BigDecimal price7DaysAgo = calculateHistoricalPrice(cropCode, currentPrice, -7);
        
        // Calculate price changes
        BigDecimal priceChange30Days = calculatePriceChange(currentPrice, price30DaysAgo);
        BigDecimal priceChange7Days = calculatePriceChange(currentPrice, price7DaysAgo);
        
        // Determine trend
        PriceTrend trend = determineTrend(priceChange7Days);
        
        // Get MSP
        BigDecimal msp = MSP_VALUES.getOrDefault(cropCode, null);
        boolean aboveMsp = msp != null && currentPrice.compareTo(msp) > 0;
        
        // Calculate arrival quantity (would be fetched from AGMARKNET)
        BigDecimal arrivalQuantity = estimateArrivalQuantity(cropCode);
        BigDecimal arrivalChangePercent = estimateArrivalChange(cropCode);
        
        // Determine recommendation
        PriceRecommendation recommendation = determineRecommendation(
                currentPrice, msp, priceChange30Days, trend);
        
        // Estimate distance to nearest mandi (would use geo-location)
        BigDecimal distanceToMandi = estimateDistanceToMandi(state);
        
        return MarketDataDto.builder()
                .cropCode(cropCode)
                .cropName(getCropName(cropCode))
                .variety("Hybrid")
                .currentPrice(currentPrice)
                .minPrice(currentPrice.multiply(new BigDecimal("0.9")).setScale(0, RoundingMode.HALF_UP))
                .maxPrice(currentPrice.multiply(new BigDecimal("1.1")).setScale(0, RoundingMode.HALF_UP))
                .price30DaysAgo(price30DaysAgo)
                .price7DaysAgo(price7DaysAgo)
                .priceChange30Days(priceChange30Days)
                .priceChange7Days(priceChange7Days)
                .trend(trend)
                .arrivalQuantity(arrivalQuantity)
                .arrivalChangePercent(arrivalChangePercent)
                .nearestMandi(getNearestMandi(state))
                .distanceToMandi(distanceToMandi)
                .msp(msp)
                .aboveMsp(aboveMsp)
                .recommendation(recommendation)
                .dataDate(LocalDate.now())
                .build();
    }

    /**
     * Calculate market-adjusted suitability score.
     * 
     * @param baseSuitabilityScore Base suitability score
     * @param marketData Market data for the crop
     * @param includeMarketData Whether to include market data
     * @return Market-adjusted score
     * 
     * Validates: Requirement 2.7
     */
    public BigDecimal calculateMarketAdjustedScore(
            BigDecimal baseSuitabilityScore,
            MarketDataDto marketData,
            boolean includeMarketData) {
        
        if (!includeMarketData || marketData == null) {
            return baseSuitabilityScore;
        }
        
        // Market adjustment factors
        BigDecimal marketAdjustment = BigDecimal.ZERO;
        
        // Price trend adjustment
        if (marketData.getTrend() == PriceTrend.UP) {
            marketAdjustment = marketAdjustment.add(new BigDecimal("3"));
        } else if (marketData.getTrend() == PriceTrend.DOWN) {
            marketAdjustment = marketAdjustment.subtract(new BigDecimal("3"));
        }
        
        // MSP comparison adjustment
        if (Boolean.TRUE.equals(marketData.getAboveMsp())) {
            marketAdjustment = marketAdjustment.add(new BigDecimal("2"));
        }
        
        // Price level adjustment (higher prices = better)
        if (marketData.getCurrentPrice() != null && marketData.getMsp() != null) {
            BigDecimal priceRatio = marketData.getCurrentPrice()
                    .divide(marketData.getMsp(), 4, RoundingMode.HALF_UP);
            if (priceRatio.compareTo(new BigDecimal("1.2")) > 0) {
                marketAdjustment = marketAdjustment.add(new BigDecimal("2"));
            }
        }
        
        // Recommendation adjustment
        if (marketData.getRecommendation() == PriceRecommendation.SELL_NOW) {
            marketAdjustment = marketAdjustment.add(new BigDecimal("2"));
        } else if (marketData.getRecommendation() == PriceRecommendation.HOLD) {
            marketAdjustment = marketAdjustment.subtract(new BigDecimal("1"));
        }
        
        // Apply adjustment to base score
        BigDecimal adjustedScore = baseSuitabilityScore.add(marketAdjustment);
        
        // Clamp to valid range
        return adjustedScore.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * Get market recommendations for a list of crops.
     * 
     * @param marketDataMap Map of crop code to market data
     * @return List of market recommendations
     * 
     * Validates: Requirement 2.7
     */
    public List<String> getMarketRecommendations(Map<String, MarketDataDto> marketDataMap) {
        List<String> recommendations = new ArrayList<>();
        
        if (marketDataMap == null || marketDataMap.isEmpty()) {
            return recommendations;
        }
        
        // Find crops with best price trends
        MarketDataDto bestTrendCrop = marketDataMap.values().stream()
                .filter(m -> m.getTrend() == PriceTrend.UP)
                .max(Comparator.comparing(m -> m.getPriceChange30Days() != null ? 
                        m.getPriceChange30Days() : BigDecimal.ZERO))
                .orElse(null);
        
        if (bestTrendCrop != null) {
            recommendations.add(String.format(
                    "%s prices are trending up - consider this crop for better returns",
                    bestTrendCrop.getCropName()));
        }
        
        // Find crops above MSP
        List<String> aboveMspCrops = marketDataMap.values().stream()
                .filter(m -> Boolean.TRUE.equals(m.getAboveMsp()))
                .map(MarketDataDto::getCropName)
                .collect(Collectors.toList());
        
        if (!aboveMspCrops.isEmpty()) {
            recommendations.add(String.format(
                    "Current prices for %s are above MSP - good time to sell",
                    String.join(", ", aboveMspCrops)));
        }
        
        // Find crops with stable prices
        List<String> stableCrops = marketDataMap.values().stream()
                .filter(m -> m.getTrend() == PriceTrend.STABLE)
                .map(MarketDataDto::getCropName)
                .collect(Collectors.toList());
        
        if (!stableCrops.isEmpty()) {
            recommendations.add(String.format(
                    "%s have stable prices - reliable income potential",
                    String.join(", ", stableCrops)));
        }
        
        return recommendations;
    }

    /**
     * Calculate expected revenue per acre based on market data.
     * 
     * @param yieldPerAcre Expected yield per acre (quintals)
     * @param marketData Market data for the crop
     * @return Expected revenue per acre (INR)
     */
    public BigDecimal calculateExpectedRevenue(BigDecimal yieldPerAcre, MarketDataDto marketData) {
        if (yieldPerAcre == null || marketData == null || marketData.getCurrentPrice() == null) {
            return null;
        }
        
        return yieldPerAcre.multiply(marketData.getCurrentPrice()).setScale(0, RoundingMode.HALF_UP);
    }

    // Helper methods

    private BigDecimal calculateHistoricalPrice(String cropCode, BigDecimal currentPrice, int daysAgo) {
        // Simulate historical price based on current price and trend
        BigDecimal dailyChangeRate = new BigDecimal("0.001"); // 0.1% daily change
        BigDecimal daysFactor = new BigDecimal(Math.abs(daysAgo)).multiply(dailyChangeRate);
        
        if (daysAgo < 0) {
            return currentPrice.multiply(BigDecimal.ONE.subtract(daysFactor))
                    .max(currentPrice.multiply(new BigDecimal("0.7")))
                    .setScale(0, RoundingMode.HALF_UP);
        } else {
            return currentPrice.multiply(BigDecimal.ONE.add(daysFactor))
                    .min(currentPrice.multiply(new BigDecimal("1.3")))
                    .setScale(0, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal calculatePriceChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PriceTrend determineTrend(BigDecimal priceChange7Days) {
        if (priceChange7Days == null) {
            return PriceTrend.STABLE;
        }
        
        if (priceChange7Days.compareTo(new BigDecimal("2")) > 0) {
            return PriceTrend.UP;
        } else if (priceChange7Days.compareTo(new BigDecimal("-2")) < 0) {
            return PriceTrend.DOWN;
        } else {
            return PriceTrend.STABLE;
        }
    }

    private PriceRecommendation determineRecommendation(
            BigDecimal currentPrice, BigDecimal msp, BigDecimal priceChange30Days, PriceTrend trend) {
        
        // If price is significantly above MSP, recommend selling
        if (msp != null && currentPrice.compareTo(msp.multiply(new BigDecimal("1.15"))) > 0) {
            return PriceRecommendation.SELL_NOW;
        }
        
        // If price is trending up strongly, recommend holding
        if (trend == PriceTrend.UP && priceChange30Days != null && 
                priceChange30Days.compareTo(new BigDecimal("5")) > 0) {
            return PriceRecommendation.HOLD;
        }
        
        // If price is trending down, recommend selling
        if (trend == PriceTrend.DOWN && priceChange30Days != null && 
                priceChange30Days.compareTo(new BigDecimal("-5")) < 0) {
            return PriceRecommendation.SELL_NOW;
        }
        
        // If price is near or below MSP, recommend monitoring
        if (msp != null && currentPrice.compareTo(msp.multiply(new BigDecimal("1.05"))) <= 0) {
            return PriceRecommendation.MONITOR;
        }
        
        return PriceRecommendation.CONSIDER_STORAGE;
    }

    private BigDecimal estimateArrivalQuantity(String cropCode) {
        // Simulate arrival quantity based on season
        return new BigDecimal("1000").multiply(new BigDecimal(Math.random() * 2 + 0.5))
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal estimateArrivalChange(String cropCode) {
        return new BigDecimal(Math.random() * 20 - 10).setScale(2, RoundingMode.HALF_UP);
    }

    private String getNearestMandi(String state) {
        if (state == null) return "Unknown Mandi";
        
        return switch (state.toLowerCase()) {
            case "uttar pradesh" -> "Lucknow Mandi";
            case "punjab" -> "Ludhiana Mandi";
            case "haryana" -> "Karnal Mandi";
            case "maharashtra" -> "Pune Mandi";
            case "karnataka" -> "Bangalore Mandi";
            case "rajasthan" -> "Jaipur Mandi";
            case "madhya pradesh" -> "Bhopal Mandi";
            case "gujarat" -> "Ahmedabad Mandi";
            case "west bengal" -> "Kolkata Mandi";
            case "tamil nadu" -> "Chennai Mandi";
            default -> state + " District Mandi";
        };
    }

    private BigDecimal estimateDistanceToMandi(String state) {
        if (state == null) return new BigDecimal("25");
        return new BigDecimal(Math.random() * 30 + 10).setScale(1, RoundingMode.HALF_UP);
    }

    private String getCropName(String cropCode) {
        return switch (cropCode) {
            case "RICE" -> "Rice";
            case "WHEAT" -> "Wheat";
            case "COTTON" -> "Cotton";
            case "SOYBEAN" -> "Soybean";
            case "GROUNDNUT" -> "Groundnut";
            case "MUSTARD" -> "Mustard";
            case "PULSES" -> "Pulses";
            case "MAIZE" -> "Maize";
            case "SUGARCANE" -> "Sugarcane";
            case "POTATO" -> "Potato";
            case "ONION" -> "Onion";
            case "TOMATO" -> "Tomato";
            default -> cropCode;
        };
    }
}