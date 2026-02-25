package com.farmer.crop.service;

import com.farmer.crop.dto.MarketDataDto;
import com.farmer.crop.dto.MarketDataDto.PriceRecommendation;
import com.farmer.crop.dto.MarketDataDto.PriceTrend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MarketDataService.
 * 
 * Tests the market data integration for crop recommendations.
 * 
 * Validates: Requirement 2.7
 */
class MarketDataServiceTest {

    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        marketDataService = new MarketDataService();
    }

    @Test
    @DisplayName("Should get market data for a single crop")
    void testGetMarketDataForCrop() {
        // Act
        MarketDataDto marketData = marketDataService.getMarketDataForCrop("RICE", "Uttar Pradesh");

        // Assert
        assertNotNull(marketData);
        assertEquals("RICE", marketData.getCropCode());
        assertEquals("Rice", marketData.getCropName());
        assertNotNull(marketData.getCurrentPrice());
        assertNotNull(marketData.getMinPrice());
        assertNotNull(marketData.getMaxPrice());
        assertNotNull(marketData.getMsp());
        assertTrue(marketData.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should get market data for multiple crops")
    void testGetMarketDataForCrops() {
        // Arrange
        List<String> cropCodes = Arrays.asList("RICE", "WHEAT", "COTTON");

        // Act
        Map<String, MarketDataDto> marketDataMap = marketDataService.getMarketDataForCrops(
                cropCodes, "Uttar Pradesh");

        // Assert
        assertNotNull(marketDataMap);
        assertEquals(3, marketDataMap.size());
        assertTrue(marketDataMap.containsKey("RICE"));
        assertTrue(marketDataMap.containsKey("WHEAT"));
        assertTrue(marketDataMap.containsKey("COTTON"));
    }

    @Test
    @DisplayName("Should calculate market-adjusted score with positive adjustment for uptrend")
    void testCalculateMarketAdjustedScore_Uptrend() {
        // Arrange
        BigDecimal baseScore = new BigDecimal("80");
        MarketDataDto marketData = MarketDataDto.builder()
                .cropCode("RICE")
                .trend(PriceTrend.UP)
                .aboveMsp(true)
                .currentPrice(new BigDecimal("2500"))
                .msp(new BigDecimal("2300"))
                .recommendation(PriceRecommendation.SELL_NOW)
                .build();

        // Act
        BigDecimal adjustedScore = marketDataService.calculateMarketAdjustedScore(
                baseScore, marketData, true);

        // Assert
        assertNotNull(adjustedScore);
        assertTrue(adjustedScore.compareTo(baseScore) > 0);
    }

    @Test
    @DisplayName("Should calculate market-adjusted score with negative adjustment for downtrend")
    void testCalculateMarketAdjustedScore_Downtrend() {
        // Arrange
        BigDecimal baseScore = new BigDecimal("80");
        MarketDataDto marketData = MarketDataDto.builder()
                .cropCode("RICE")
                .trend(PriceTrend.DOWN)
                .aboveMsp(false)
                .currentPrice(new BigDecimal("2200"))
                .msp(new BigDecimal("2300"))
                .recommendation(PriceRecommendation.HOLD)
                .build();

        // Act
        BigDecimal adjustedScore = marketDataService.calculateMarketAdjustedScore(
                baseScore, marketData, true);

        // Assert
        assertNotNull(adjustedScore);
        assertTrue(adjustedScore.compareTo(baseScore) < 0);
    }

    @Test
    @DisplayName("Should return base score when market data is null")
    void testCalculateMarketAdjustedScore_NullMarketData() {
        // Arrange
        BigDecimal baseScore = new BigDecimal("80");

        // Act
        BigDecimal adjustedScore = marketDataService.calculateMarketAdjustedScore(
                baseScore, null, true);

        // Assert
        assertEquals(baseScore, adjustedScore);
    }

    @Test
    @DisplayName("Should return base score when includeMarketData is false")
    void testCalculateMarketAdjustedScore_IncludeMarketDataFalse() {
        // Arrange
        BigDecimal baseScore = new BigDecimal("80");
        MarketDataDto marketData = MarketDataDto.builder()
                .cropCode("RICE")
                .trend(PriceTrend.UP)
                .build();

        // Act
        BigDecimal adjustedScore = marketDataService.calculateMarketAdjustedScore(
                baseScore, marketData, false);

        // Assert
        assertEquals(baseScore, adjustedScore);
    }

    @Test
    @DisplayName("Should calculate expected revenue correctly")
    void testCalculateExpectedRevenue() {
        // Arrange
        BigDecimal yieldPerAcre = new BigDecimal("25"); // 25 quintals per acre
        MarketDataDto marketData = MarketDataDto.builder()
                .currentPrice(new BigDecimal("2200"))
                .build();

        // Act
        BigDecimal revenue = marketDataService.calculateExpectedRevenue(yieldPerAcre, marketData);

        // Assert
        assertNotNull(revenue);
        assertEquals(new BigDecimal("55000"), revenue);
    }

    @Test
    @DisplayName("Should return null when yield is null")
    void testCalculateExpectedRevenue_NullYield() {
        // Arrange
        MarketDataDto marketData = MarketDataDto.builder()
                .currentPrice(new BigDecimal("2200"))
                .build();

        // Act
        BigDecimal revenue = marketDataService.calculateExpectedRevenue(null, marketData);

        // Assert
        assertNull(revenue);
    }

    @Test
    @DisplayName("Should generate market recommendations")
    void testGetMarketRecommendations() {
        // Arrange
        MarketDataDto riceData = MarketDataDto.builder()
                .cropCode("RICE")
                .cropName("Rice")
                .trend(PriceTrend.UP)
                .priceChange30Days(new BigDecimal("8"))
                .aboveMsp(true)
                .build();

        MarketDataDto wheatData = MarketDataDto.builder()
                .cropCode("WHEAT")
                .cropName("Wheat")
                .trend(PriceTrend.STABLE)
                .aboveMsp(true)
                .build();

        Map<String, MarketDataDto> marketDataMap = Map.of(
                "RICE", riceData,
                "WHEAT", wheatData
        );

        // Act
        List<String> recommendations = marketDataService.getMarketRecommendations(marketDataMap);

        // Assert
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }

    @Test
    @DisplayName("Should return empty recommendations for empty map")
    void testGetMarketRecommendations_EmptyMap() {
        // Act
        List<String> recommendations = marketDataService.getMarketRecommendations(Map.of());

        // Assert
        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty());
    }

    @Test
    @DisplayName("Should return empty recommendations for null map")
    void testGetMarketRecommendations_NullMap() {
        // Act
        List<String> recommendations = marketDataService.getMarketRecommendations(null);

        // Assert
        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty());
    }

    @Test
    @DisplayName("Should handle unknown crop codes")
    void testGetMarketDataForCrop_UnknownCrop() {
        // Act
        MarketDataDto marketData = marketDataService.getMarketDataForCrop("UNKNOWN", "Unknown State");

        // Assert
        assertNotNull(marketData);
        assertEquals("UNKNOWN", marketData.getCropCode());
        assertNotNull(marketData.getCurrentPrice());
    }

    @Test
    @DisplayName("Should clamp adjusted score to valid range")
    void testCalculateMarketAdjustedScore_Clamping() {
        // Arrange
        BigDecimal baseScore = new BigDecimal("95");
        MarketDataDto marketData = MarketDataDto.builder()
                .cropCode("RICE")
                .trend(PriceTrend.UP)
                .aboveMsp(true)
                .currentPrice(new BigDecimal("3000"))
                .msp(new BigDecimal("2300"))
                .recommendation(PriceRecommendation.SELL_NOW)
                .build();

        // Act
        BigDecimal adjustedScore = marketDataService.calculateMarketAdjustedScore(
                baseScore, marketData, true);

        // Assert
        assertNotNull(adjustedScore);
        assertTrue(adjustedScore.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(adjustedScore.compareTo(new BigDecimal("100")) <= 0);
    }
}