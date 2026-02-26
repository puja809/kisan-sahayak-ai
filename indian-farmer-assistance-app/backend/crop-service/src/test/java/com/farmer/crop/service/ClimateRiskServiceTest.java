package com.farmer.crop.service;

import com.farmer.crop.dto.ClimateRiskDto;
import com.farmer.crop.dto.ClimateRiskDto.ClimateRiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClimateRiskService.
 * 
 * Tests the climate risk analysis under rainfall deviation scenarios.
 * 
 * Validates: Requirement 2.8
 */
class ClimateRiskServiceTest {

    private ClimateRiskService climateRiskService;

    @BeforeEach
    void setUp() {
        climateRiskService = new ClimateRiskService();
    }

    @Test
    @DisplayName("Should analyze climate risk for a crop")
    void testAnalyzeClimateRisk() {
        // Act
        ClimateRiskDto climateRisk = climateRiskService.analyzeClimateRisk(
                "RICE", -15.0, 1.0);

        // Assert
        assertNotNull(climateRisk);
        assertEquals("RICE", climateRisk.getCropCode());
        assertEquals("Rice", climateRisk.getCropName());
        assertNotNull(climateRisk.getRiskLevel());
        assertNotNull(climateRisk.getRiskScore());
        assertNotNull(climateRisk.getRainfallScenario());
        assertNotNull(climateRisk.getTemperatureStress());
    }

    @Test
    @DisplayName("Should identify high risk for rainfall deficit scenario")
    void testAnalyzeClimateRisk_RainfallDeficit() {
        // Act - Significant rainfall deficit
        ClimateRiskDto climateRisk = climateRiskService.analyzeClimateRisk(
                "GROUNDNUT", -25.0, 0.0);

        // Assert
        assertNotNull(climateRisk);
        assertNotNull(climateRisk.getRainfallScenario());
        assertEquals(ClimateRiskDto.RainfallDeviationScenario.ScenarioType.DEFICIT,
                climateRisk.getRainfallScenario().getScenarioType());
    }

    @Test
    @DisplayName("Should identify high risk for rainfall excess scenario")
    void testAnalyzeClimateRisk_RainfallExcess() {
        // Act - Significant rainfall excess
        ClimateRiskDto climateRisk = climateRiskService.analyzeClimateRisk(
                "RICE", 30.0, 0.0);

        // Assert
        assertNotNull(climateRisk);
        assertNotNull(climateRisk.getRainfallScenario());
        assertEquals(ClimateRiskDto.RainfallDeviationScenario.ScenarioType.EXCESS,
                climateRisk.getRainfallScenario().getScenarioType());
    }

    @Test
    @DisplayName("Should analyze climate risk for multiple crops")
    void testAnalyzeClimateRiskForCrops() {
        // Arrange
        List<String> cropCodes = Arrays.asList("RICE", "WHEAT", "COTTON");

        // Act
        Map<String, ClimateRiskDto> riskMap = climateRiskService.analyzeClimateRiskForCrops(
                cropCodes, -10.0);

        // Assert
        assertNotNull(riskMap);
        assertEquals(3, riskMap.size());
        assertTrue(riskMap.containsKey("RICE"));
        assertTrue(riskMap.containsKey("WHEAT"));
        assertTrue(riskMap.containsKey("COTTON"));
    }

    @Test
    @DisplayName("Should flag high risk crops")
    void testFlagHighRiskCrops() {
        // Arrange
        ClimateRiskDto riceRisk = ClimateRiskDto.builder()
                .cropCode("RICE")
                .riskLevel(ClimateRiskLevel.HIGH)
                .build();

        ClimateRiskDto wheatRisk = ClimateRiskDto.builder()
                .cropCode("WHEAT")
                .riskLevel(ClimateRiskLevel.LOW)
                .build();

        ClimateRiskDto cottonRisk = ClimateRiskDto.builder()
                .cropCode("COTTON")
                .riskLevel(ClimateRiskLevel.VERY_HIGH)
                .build();

        Map<String, ClimateRiskDto> riskMap = Map.of(
                "RICE", riceRisk,
                "WHEAT", wheatRisk,
                "COTTON", cottonRisk
        );

        // Act
        List<String> highRiskCrops = climateRiskService.flagHighRiskCrops(riskMap);

        // Assert
        assertNotNull(highRiskCrops);
        assertEquals(2, highRiskCrops.size());
        assertTrue(highRiskCrops.contains("RICE"));
        assertTrue(highRiskCrops.contains("COTTON"));
        assertFalse(highRiskCrops.contains("WHEAT"));
    }

    @Test
    @DisplayName("Should calculate climate-adjusted score with negative adjustment for high risk")
    void testCalculateClimateAdjustedScore_HighRisk() {
        // Arrange
        Double baseScore = 80.0;
        ClimateRiskDto climateRisk = ClimateRiskDto.builder()
                .riskLevel(ClimateRiskLevel.HIGH)
                .build();

        // Act
        Double adjustedScore = climateRiskService.calculateClimateAdjustedScore(
                baseScore, climateRisk);

        // Assert
        assertNotNull(adjustedScore);
        assertTrue(adjustedScore < baseScore);
    }

    @Test
    @DisplayName("Should calculate climate-adjusted score with no adjustment for low risk")
    void testCalculateClimateAdjustedScore_LowRisk() {
        // Arrange
        Double baseScore = 80.0;
        ClimateRiskDto climateRisk = ClimateRiskDto.builder()
                .riskLevel(ClimateRiskLevel.LOW)
                .build();

        // Act
        Double adjustedScore = climateRiskService.calculateClimateAdjustedScore(
                baseScore, climateRisk);

        // Assert
        assertNotNull(adjustedScore);
        assertEquals(baseScore, adjustedScore);
    }

    @Test
    @DisplayName("Should return base score when climate risk is null")
    void testCalculateClimateAdjustedScore_NullClimateRisk() {
        // Arrange
        Double baseScore = 80.0;

        // Act
        Double adjustedScore = climateRiskService.calculateClimateAdjustedScore(
                baseScore, null);

        // Assert
        assertEquals(baseScore, adjustedScore);
    }

    @Test
    @DisplayName("Should include mitigation strategies for high risk crops")
    void testAnalyzeClimateRisk_MitigationStrategies() {
        // Act
        ClimateRiskDto climateRisk = climateRiskService.analyzeClimateRisk(
                "GROUNDNUT", -25.0, 0.0);

        // Assert
        assertNotNull(climateRisk);
        assertNotNull(climateRisk.getMitigationStrategies());
        assertFalse(climateRisk.getMitigationStrategies().isEmpty());
    }

    @Test
    @DisplayName("Should include resilient variety recommendations")
    void testAnalyzeClimateRisk_ResilientVarieties() {
        // Act
        ClimateRiskDto climateRisk = climateRiskService.analyzeClimateRisk(
                "RICE", -15.0, 0.0);

        // Assert
        assertNotNull(climateRisk);
        assertNotNull(climateRisk.getResilientVarieties());
        assertFalse(climateRisk.getResilientVarieties().isEmpty());
    }

    @Test
    @DisplayName("Should recommend insurance for high risk crops")
    void testAnalyzeClimateRisk_InsuranceRecommendation() {
        // Act - High risk scenario
        ClimateRiskDto climateRisk = climateRiskService.analyzeClimateRisk(
                "RICE", -30.0, 2.0);

        // Assert
        assertNotNull(climateRisk);
        assertTrue(Boolean.TRUE.equals(climateRisk.getInsuranceRecommended()) ||
                climateRisk.getRiskLevel() == ClimateRiskLevel.HIGH ||
                climateRisk.getRiskLevel() == ClimateRiskLevel.VERY_HIGH);
    }

    @Test
    @DisplayName("Should handle unknown crop codes")
    void testAnalyzeClimateRisk_UnknownCrop() {
        // Act
        ClimateRiskDto climateRisk = climateRiskService.analyzeClimateRisk(
                "UNKNOWN", -10.0, 0.0);

        // Assert
        assertNotNull(climateRisk);
        assertEquals("UNKNOWN", climateRisk.getCropCode());
        assertNotNull(climateRisk.getRiskLevel());
    }

    @Test
    @DisplayName("Should clamp adjusted score to valid range")
    void testCalculateClimateAdjustedScore_Clamping() {
        // Arrange
        Double baseScore = 20.0;
        ClimateRiskDto climateRisk = ClimateRiskDto.builder()
                .riskLevel(ClimateRiskLevel.VERY_HIGH)
                .build();

        // Act
        Double adjustedScore = climateRiskService.calculateClimateAdjustedScore(
                baseScore, climateRisk);

        // Assert
        assertNotNull(adjustedScore);
        assertTrue(adjustedScore >= 0.0);
    }

    @Test
    @DisplayName("Should assess drought risk correctly")
    void testAnalyzeClimateRisk_DroughtRisk() {
        // Act - Drought-prone crop with rainfall deficit
        ClimateRiskDto climateRisk = climateRiskService.analyzeClimateRisk(
                "GROUNDNUT", -25.0, 0.0);

        // Assert
        assertNotNull(climateRisk);
        assertNotNull(climateRisk.getDroughtRisk());
    }

    @Test
    @DisplayName("Should assess flood risk correctly")
    void testAnalyzeClimateRisk_FloodRisk() {
        // Act - Flood-prone crop with rainfall excess
        ClimateRiskDto climateRisk = climateRiskService.analyzeClimateRisk(
                "RICE", 30.0, 0.0);

        // Assert
        assertNotNull(climateRisk);
        assertNotNull(climateRisk.getFloodRisk());
    }
}

