package com.farmer.crop.service;

import com.farmer.crop.dto.MarketDataDto;
import com.farmer.crop.dto.MarketDataDto.PriceRecommendation;
import com.farmer.crop.dto.MarketDataDto.PriceTrend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



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
    private static final Map<String, Double> MSP_VALUES = Map.ofEntries(
            Map.entry("RICE", 2300.0),
            Map.entry("WHEAT", 2650.0),
            Map.entry("COTTON", 6620.0),
            Map.entry("SOYBEAN", 5650.0),
            Map.entry("GROUNDNUT", 6375.0),
            Map.entry("MUSTARD", 5850.0),
            Map.entry("PULSES", 6000.0),
            Map.entry("MAIZE", 2090.0),
            Map.entry("SUGARCANE", 315.0)  // per quintal
    );

    // Approximate current market prices (INR per quintal) - would be fetched from AGMARKNET
    private static final Map<String, Double> CURRENT_PRICES = Map.ofEntries(
            Map.entry("RICE", 2200.0),
            Map.entry("WHEAT", 2500.0),
            Map.entry("COTTON", 5800.0),
            Map.entry("SOYBEAN", 5200.0),
            Map.entry("GROUNDNUT", 6200.0),
            Map.entry("MUSTARD", 5500.0),
            Map.entry("PULSES", 6500.0),
            Map.entry("MAIZE", 2100.0),
            Map.entry("SUGARCANE", 350.0),
            Map.entry("POTATO", 1500.0),
            Map.entry("ONION", 1800.0),
            Map.entry("TOMATO", 2000.0)
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
        Double currentPrice = CURRENT_PRICES.getOrDefault(cropCode, 2000.0);
        
        // Calculate price trends (would be based on historical data)
        Double price30DaysAgo = calculateHistoricalPrice(cropCode, currentPrice, -30);
        Double price7DaysAgo = calculateHistoricalPrice(cropCode, currentPrice, -7);
        
        // Calculate price changes
        Double priceChange30Days = calculatePriceChange(currentPrice, price30DaysAgo);
        Double priceChange7Days = calculatePriceChange(currentPrice, price7DaysAgo);
        
        // Determine trend
        PriceTrend trend = determineTrend(priceChange7Days);
        
        // Get MSP
        Double msp = MSP_VALUES.getOrDefault(cropCode, null);
        boolean aboveMsp = msp != null && (currentPrice > msp);
        
        // Calculate arrival quantity (would be fetched from AGMARKNET)
        Double arrivalQuantity = estimateArrivalQuantity(cropCode);
        Double arrivalChangePercent = estimateArrivalChange(cropCode);
        
        // Determine recommendation
        PriceRecommendation recommendation = determineRecommendation(
                currentPrice, msp, priceChange30Days, trend);
        
        // Estimate distance to nearest mandi (would use geo-location)
        Double distanceToMandi = estimateDistanceToMandi(state);
        
        return MarketDataDto.builder()
                .cropCode(cropCode)
                .cropName(getCropName(cropCode))
                .variety("Hybrid")
                .currentPrice(currentPrice)
                .minPrice(currentPrice * (0.9))
                .maxPrice(currentPrice * (1.1))
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
    public Double calculateMarketAdjustedScore(
            Double baseSuitabilityScore,
            MarketDataDto marketData,
            boolean includeMarketData) {
        
        if (!includeMarketData || marketData == null) {
            return baseSuitabilityScore;
        }
        
        // Market adjustment factors
        Double marketAdjustment = 0.0;
        
        // Price trend adjustment
        if (marketData.getTrend() == PriceTrend.UP) {
            marketAdjustment = marketAdjustment + 3.0;
        } else if (marketData.getTrend() == PriceTrend.DOWN) {
            marketAdjustment = marketAdjustment - 3.0;
        }
        
        // MSP comparison adjustment
        if (Boolean.TRUE.equals(marketData.getAboveMsp())) {
            marketAdjustment = marketAdjustment + 2.0;
        }
        
        // Price level adjustment (higher prices = better)
        if (marketData.getCurrentPrice() != null && marketData.getMsp() != null) {
            Double priceRatio = marketData.getCurrentPrice() / marketData.getMsp();
            if (priceRatio.compareTo(1.2) > 0) {
                marketAdjustment = marketAdjustment + 2.0;
            }
        }
        
        // Recommendation adjustment
        if (marketData.getRecommendation() == PriceRecommendation.SELL_NOW) {
            marketAdjustment = marketAdjustment + 2.0;
        } else if (marketData.getRecommendation() == PriceRecommendation.HOLD) {
            marketAdjustment = marketAdjustment - 1.0;
        }
        
        // Apply adjustment to base score
        Double adjustedScore = baseSuitabilityScore + marketAdjustment;
        
        // Clamp to valid range
        return Math.max(adjustedScore, 0.0);
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
                        m.getPriceChange30Days() : 0.0))
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
    public Double calculateExpectedRevenue(Double yieldPerAcre, MarketDataDto marketData) {
        if (yieldPerAcre == null || marketData == null || marketData.getCurrentPrice() == null) {
            return null;
        }
        
        return yieldPerAcre * (marketData.getCurrentPrice());
    }

    // Helper methods

    private Double calculateHistoricalPrice(String cropCode, Double currentPrice, int daysAgo) {
        // Simulate historical price based on current price and trend
        Double dailyChangeRate = 0.001; // 0.1% daily change
        Double daysFactor = new Double(Math.abs(daysAgo)) * (dailyChangeRate);
        
        if (daysAgo < 0) {
            return Math.max(currentPrice * (1.0 - daysFactor), currentPrice * 0.7);
        } else {
            return Math.min(currentPrice * (1.0 + daysFactor), currentPrice * 1.3);
        }
    }

    private Double calculatePriceChange(Double current, Double previous) {
        if (previous == null || previous.compareTo(0.0) == 0) {
            return 0.0;
        }
        
        return (current - previous) / previous * 100.0;
    }

    private PriceTrend determineTrend(Double priceChange7Days) {
        if (priceChange7Days == null) {
            return PriceTrend.STABLE;
        }
        
        if (priceChange7Days.compareTo(2.0) > 0) {
            return PriceTrend.UP;
        } else if (priceChange7Days.compareTo(new Double("-2")) < 0) {
            return PriceTrend.DOWN;
        } else {
            return PriceTrend.STABLE;
        }
    }

    private PriceRecommendation determineRecommendation(
            Double currentPrice, Double msp, Double priceChange30Days, PriceTrend trend) {
        
        // If price is significantly above MSP, recommend selling
        if (msp != null && currentPrice.compareTo(msp * (1.15)) > 0) {
            return PriceRecommendation.SELL_NOW;
        }
        
        // If price is trending up strongly, recommend holding
        if (trend == PriceTrend.UP && priceChange30Days != null && 
                priceChange30Days.compareTo(5.0) > 0) {
            return PriceRecommendation.HOLD;
        }
        
        // If price is trending down, recommend selling
        if (trend == PriceTrend.DOWN && priceChange30Days != null && 
                priceChange30Days.compareTo(new Double("-5")) < 0) {
            return PriceRecommendation.SELL_NOW;
        }
        
        // If price is near or below MSP, recommend monitoring
        if (msp != null && currentPrice.compareTo(msp * (1.05)) <= 0) {
            return PriceRecommendation.MONITOR;
        }
        
        return PriceRecommendation.CONSIDER_STORAGE;
    }

    private Double estimateArrivalQuantity(String cropCode) {
        // Simulate arrival quantity based on season
        return 1000.0 * (new Double(Math.random() * 2 + 0.5))
                ;
    }

    private Double estimateArrivalChange(String cropCode) {
        return new Double(Math.random() * 20 - 10);
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

    private Double estimateDistanceToMandi(String state) {
        if (state == null) return 25.0;
        return new Double(Math.random() * 30 + 10);
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








