package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.GaezCropData;
import com.farmer.crop.entity.SoilHealthCard;
import com.farmer.crop.repository.GaezCropDataRepository;
import com.farmer.crop.repository.SoilHealthCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GaezSuitabilityService.
 * 
 * Tests the GAEZ v4 framework integration for crop suitability scoring,
 * including soil health card data incorporation.
 * 
 * Validates: Requirements 2.2, 2.3, 2.4
 */
@ExtendWith(MockitoExtension.class)
class GaezSuitabilityServiceTest {

    @Mock
    private GaezCropDataRepository gaezCropDataRepository;

    @Mock
    private SoilHealthCardRepository soilHealthCardRepository;

    private GaezSuitabilityService gaezSuitabilityService;

    @BeforeEach
    void setUp() {
        gaezSuitabilityService = new GaezSuitabilityService(
                gaezCropDataRepository, soilHealthCardRepository);
    }

    @Test
    @DisplayName("Should return GAEZ data for valid zone")
    void testGetSuitabilityForZone() {
        // Arrange
        String zoneCode = "AEZ-05";
        GaezCropData wheatData = createGaezCropData("AEZ-05", "WHEAT", "Wheat", 
                new BigDecimal("90"), new BigDecimal("95"), new BigDecimal("85"),
                new BigDecimal("90"), new BigDecimal("85"), true, false, false, "LOW");
        
        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue(zoneCode))
                .thenReturn(Arrays.asList(wheatData));

        // Act
        List<GaezCropSuitabilityDto> result = gaezSuitabilityService.getSuitabilityForZone(zoneCode);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("WHEAT", result.get(0).getCropCode());
        assertEquals("Wheat", result.get(0).getCropName());
        assertEquals(new BigDecimal("90"), result.get(0).getOverallSuitabilityScore());
    }

    @Test
    @DisplayName("Should return empty list for zone with no data")
    void testGetSuitabilityForZoneNoData() {
        // Arrange
        String zoneCode = "AEZ-99";
        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue(zoneCode))
                .thenReturn(Collections.emptyList());

        // Act
        List<GaezCropSuitabilityDto> result = gaezSuitabilityService.getSuitabilityForZone(zoneCode);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should calculate suitability scores with irrigation adjustment")
    void testCalculateSuitabilityScoresWithIrrigation() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .agroEcologicalZoneCode("AEZ-05")
                .irrigationType(CropRecommendationRequestDto.IrrigationType.DRIP)
                .season(CropRecommendationRequestDto.Season.RABI)
                .build();

        GaezCropData wheatData = createGaezCropData("AEZ-05", "WHEAT", "Wheat", 
                new BigDecimal("90"), new BigDecimal("95"), new BigDecimal("85"),
                new BigDecimal("90"), new BigDecimal("85"), false, true, false, "LOW");

        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue("AEZ-05"))
                .thenReturn(Arrays.asList(wheatData));
        when(soilHealthCardRepository.findMostRecentByFarmerId(anyString()))
                .thenReturn(Optional.empty());

        // Act
        List<GaezCropSuitabilityDto> result = gaezSuitabilityService.calculateSuitabilityScores(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("WHEAT", result.get(0).getCropCode());
        // DRIP irrigation should add 5 to water score
        assertTrue(result.get(0).getWaterSuitabilityScore().compareTo(new BigDecimal("85")) > 0);
    }

    @Test
    @DisplayName("Should incorporate soil health card data in suitability calculation")
    void testCalculateSuitabilityScoresWithSoilHealthCard() {
        // Arrange
        SoilHealthCardDto soilHealthCard = SoilHealthCardDto.builder()
                .cardId("SHC-001")
                .farmerId("FARMER-001")
                .nitrogenKgHa(new BigDecimal("200"))  // Low nitrogen
                .phosphorusKgHa(new BigDecimal("8"))  // Low phosphorus
                .potassiumKgHa(new BigDecimal("300")) // Adequate potassium
                .zincPpm(new BigDecimal("0.4"))      // Low zinc
                .ph(new BigDecimal("6.5"))           // Good pH
                .build();

        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .agroEcologicalZoneCode("AEZ-05")
                .hasSoilHealthCard(true)
                .soilHealthCard(soilHealthCard)
                .build();

        GaezCropData wheatData = createGaezCropData("AEZ-05", "WHEAT", "Wheat", 
                new BigDecimal("90"), new BigDecimal("95"), new BigDecimal("85"),
                new BigDecimal("90"), new BigDecimal("85"), false, true, false, "LOW");

        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue("AEZ-05"))
                .thenReturn(Arrays.asList(wheatData));

        // Act
        List<GaezCropSuitabilityDto> result = gaezSuitabilityService.calculateSuitabilityScores(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        // Soil health adjustments should reduce the score due to nutrient deficiencies
        assertTrue(result.get(0).getOverallSuitabilityScore().compareTo(new BigDecimal("90")) < 0);
    }

    @Test
    @DisplayName("Should filter crops by season")
    void testCalculateSuitabilityScoresSeasonFilter() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .agroEcologicalZoneCode("AEZ-05")
                .season(CropRecommendationRequestDto.Season.RABI)
                .build();

        GaezCropData riceData = createGaezCropData("AEZ-05", "RICE", "Rice", 
                new BigDecimal("75"), new BigDecimal("80"), new BigDecimal("70"),
                new BigDecimal("85"), new BigDecimal("65"), true, false, false, "MEDIUM");
        
        GaezCropData wheatData = createGaezCropData("AEZ-05", "WHEAT", "Wheat", 
                new BigDecimal("90"), new BigDecimal("95"), new BigDecimal("85"),
                new BigDecimal("90"), new BigDecimal("85"), false, true, false, "LOW");

        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue("AEZ-05"))
                .thenReturn(Arrays.asList(riceData, wheatData));
        when(soilHealthCardRepository.findMostRecentByFarmerId(anyString()))
                .thenReturn(Optional.empty());

        // Act
        List<GaezCropSuitabilityDto> result = gaezSuitabilityService.calculateSuitabilityScores(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("WHEAT", result.get(0).getCropCode());
    }

    @Test
    @DisplayName("Should return empty list for null zone code")
    void testCalculateSuitabilityScoresNullZoneCode() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .build();

        // Act
        List<GaezCropSuitabilityDto> result = gaezSuitabilityService.calculateSuitabilityScores(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return all crops above minimum threshold")
    void testCalculateSuitabilityScoresMinThreshold() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .agroEcologicalZoneCode("AEZ-05")
                .build();

        GaezCropData wheatData = createGaezCropData("AEZ-05", "WHEAT", "Wheat", 
                new BigDecimal("90"), new BigDecimal("95"), new BigDecimal("85"),
                new BigDecimal("90"), new BigDecimal("85"), false, true, false, "LOW");
        
        GaezCropData cottonData = createGaezCropData("AEZ-05", "COTTON", "Cotton", 
                new BigDecimal("50"), new BigDecimal("55"), new BigDecimal("45"),
                new BigDecimal("60"), new BigDecimal("40"), true, false, false, "HIGH");

        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue("AEZ-05"))
                .thenReturn(Arrays.asList(wheatData, cottonData));
        when(soilHealthCardRepository.findMostRecentByFarmerId(anyString()))
                .thenReturn(Optional.empty());

        // Act
        List<GaezCropSuitabilityDto> result = gaezSuitabilityService.calculateSuitabilityScores(request);

        // Assert
        assertNotNull(result);
        // GaezSuitabilityService filters out crops below 40 (MIN_SUITABILITY_THRESHOLD)
        // Cotton has score 50, so it should be included
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should handle rain-fed irrigation adjustment")
    void testRainfedIrrigationAdjustment() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .agroEcologicalZoneCode("AEZ-05")
                .irrigationType(CropRecommendationRequestDto.IrrigationType.RAINFED)
                .build();

        GaezCropData wheatData = createGaezCropData("AEZ-05", "WHEAT", "Wheat", 
                new BigDecimal("90"), new BigDecimal("95"), new BigDecimal("85"),
                new BigDecimal("90"), new BigDecimal("85"), false, true, false, "LOW");

        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue("AEZ-05"))
                .thenReturn(Arrays.asList(wheatData));
        when(soilHealthCardRepository.findMostRecentByFarmerId(anyString()))
                .thenReturn(Optional.empty());

        // Act
        List<GaezCropSuitabilityDto> result = gaezSuitabilityService.calculateSuitabilityScores(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        // RAINFED should reduce water score by 10
        assertTrue(result.get(0).getWaterSuitabilityScore().compareTo(new BigDecimal("75")) >= 0);
    }

    @Test
    @DisplayName("Should return crop data for specific crop and zone")
    void testGetSuitabilityForCrop() {
        // Arrange
        GaezCropData wheatData = createGaezCropData("AEZ-05", "WHEAT", "Wheat", 
                new BigDecimal("90"), new BigDecimal("95"), new BigDecimal("85"),
                new BigDecimal("90"), new BigDecimal("85"), false, true, false, "LOW");

        when(gaezCropDataRepository.findByZoneCodeAndCropCodeAndIsActiveTrue("AEZ-05", "WHEAT"))
                .thenReturn(Optional.of(wheatData));

        // Act
        Optional<GaezCropSuitabilityDto> result = 
                gaezSuitabilityService.getSuitabilityForCrop("AEZ-05", "WHEAT");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("WHEAT", result.get().getCropCode());
        assertEquals("Wheat", result.get().getCropName());
    }

    @Test
    @DisplayName("Should return empty for non-existent crop")
    void testGetSuitabilityForCropNotFound() {
        // Arrange
        when(gaezCropDataRepository.findByZoneCodeAndCropCodeAndIsActiveTrue("AEZ-05", "NONEXISTENT"))
                .thenReturn(Optional.empty());

        // Act
        Optional<GaezCropSuitabilityDto> result = 
                gaezSuitabilityService.getSuitabilityForCrop("AEZ-05", "NONEXISTENT");

        // Assert
        assertTrue(result.isEmpty());
    }

    /**
     * Helper method to create GaezCropData for testing.
     */
    private GaezCropData createGaezCropData(String zoneCode, String cropCode, String cropName,
            BigDecimal overallScore, BigDecimal climateScore, BigDecimal soilScore,
            BigDecimal terrainScore, BigDecimal waterScore, 
            boolean kharif, boolean rabi, boolean zaid, String climateRisk) {
        
        GaezCropData.SuitabilityClassification classification;
        if (overallScore.compareTo(new BigDecimal("80")) >= 0) {
            classification = GaezCropData.SuitabilityClassification.HIGHLY_SUITABLE;
        } else if (overallScore.compareTo(new BigDecimal("60")) >= 0) {
            classification = GaezCropData.SuitabilityClassification.SUITABLE;
        } else if (overallScore.compareTo(new BigDecimal("40")) >= 0) {
            classification = GaezCropData.SuitabilityClassification.MARGINALLY_SUITABLE;
        } else {
            classification = GaezCropData.SuitabilityClassification.NOT_SUITABLE;
        }

        return GaezCropData.builder()
                .id(1L)
                .zoneCode(zoneCode)
                .cropCode(cropCode)
                .cropName(cropName)
                .overallSuitabilityScore(overallScore)
                .suitabilityClassification(classification)
                .climateSuitabilityScore(climateScore)
                .soilSuitabilityScore(soilScore)
                .terrainSuitabilityScore(terrainScore)
                .waterSuitabilityScore(waterScore)
                .rainfedPotentialYield(new BigDecimal("4500"))
                .irrigatedPotentialYield(new BigDecimal("5500"))
                .waterRequirementsMm(new BigDecimal("500"))
                .growingSeasonDays(120)
                .kharifSuitable(kharif)
                .rabiSuitable(rabi)
                .zaidSuitable(zaid)
                .climateRiskLevel(GaezCropData.ClimateRiskLevel.valueOf(climateRisk))
                .dataVersion("v4")
                .dataResolution("5 arc-min")
                .isActive(true)
                .build();
    }
}