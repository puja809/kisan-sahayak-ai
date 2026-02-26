package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.GaezCropData;
import com.farmer.crop.repository.GaezCropDataRepository;
import com.farmer.crop.repository.SoilHealthCardRepository;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for GAEZ v4 framework integration.
 * 
 * These tests verify universal properties that should hold across all valid inputs
 * for crop suitability scoring.
 * 
 * Validates: Requirements 2.2, 2.3, 2.4
 */
class GaezSuitabilityPropertyTest {

    private GaezCropDataRepository gaezCropDataRepository;
    private SoilHealthCardRepository soilHealthCardRepository;
    private GaezSuitabilityService gaezSuitabilityService;

    @BeforeEach
    void setUp() {
        gaezCropDataRepository = mock(GaezCropDataRepository.class);
        soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        gaezSuitabilityService = new GaezSuitabilityService(
                gaezCropDataRepository, soilHealthCardRepository);
    }

    /**
     * Property 1: Suitability scores should always be between 0 and 100.
     * 
     * For any valid GAEZ crop data, the calculated suitability scores
     * should always be within the valid range [0, 100].
     */
    @Property
    @DisplayName("Suitability scores should be between 0 and 100")
    void suitabilityScoresShouldBeInValidRange(
            @ForAll("validGaezCropData") GaezCropData gaezData) {
        // Arrange
        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue(gaezData.getZoneCode()))
                .thenReturn(List.of(gaezData));
        when(soilHealthCardRepository.findMostRecentByFarmerId(anyString()))
                .thenReturn(java.util.Optional.empty());

        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST-FARMER")
                .agroEcologicalZoneCode(gaezData.getZoneCode())
                .build();

        // Act
        List<GaezCropSuitabilityDto> result = 
                gaezSuitabilityService.calculateSuitabilityScores(request);

        // Assert
        if (!result.isEmpty()) {
            GaezCropSuitabilityDto suitability = result.get(0);
            
            assertAll("All scores should be in valid range",
                () -> assertTrue(suitability.getOverallSuitabilityScore() >= 0.0,
                    "Overall score should be >= 0"),
                () -> assertTrue(suitability.getOverallSuitabilityScore() <= 100.0,
                    "Overall score should be <= 100"),
                () -> assertTrue(suitability.getClimateSuitabilityScore() >= 0.0,
                    "Climate score should be >= 0"),
                () -> assertTrue(suitability.getClimateSuitabilityScore() <= 100.0,
                    "Climate score should be <= 100"),
                () -> assertTrue(suitability.getSoilSuitabilityScore() >= 0.0,
                    "Soil score should be >= 0"),
                () -> assertTrue(suitability.getSoilSuitabilityScore() <= 100.0,
                    "Soil score should be <= 100"),
                () -> assertTrue(suitability.getTerrainSuitabilityScore() >= 0.0,
                    "Terrain score should be >= 0"),
                () -> assertTrue(suitability.getTerrainSuitabilityScore() <= 100.0,
                    "Terrain score should be <= 100"),
                () -> assertTrue(suitability.getWaterSuitabilityScore() >= 0.0,
                    "Water score should be >= 0"),
                () -> assertTrue(suitability.getWaterSuitabilityScore() <= 100.0,
                    "Water score should be <= 100")
            );
        }
    }

    /**
     * Property 2: Overall score should be a weighted average of component scores.
     * 
     * The overall suitability score should be calculated as a weighted combination
     * of climate, soil, terrain, and water scores.
     */
    @Property
    @DisplayName("Overall score should be derived from component scores")
    void overallScoreShouldBeDerivedFromComponents(
            @ForAll("validGaezCropData") GaezCropData gaezData) {
        // Arrange
        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue(gaezData.getZoneCode()))
                .thenReturn(List.of(gaezData));
        when(soilHealthCardRepository.findMostRecentByFarmerId(anyString()))
                .thenReturn(java.util.Optional.empty());

        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST-FARMER")
                .agroEcologicalZoneCode(gaezData.getZoneCode())
                .build();

        // Act
        List<GaezCropSuitabilityDto> result = 
                gaezSuitabilityService.calculateSuitabilityScores(request);

        // Assert
        if (!result.isEmpty()) {
            GaezCropSuitabilityDto suitability = result.get(0);
            
            // Overall score should be between min and max of component scores
            Double minComponent = minOf(
                    suitability.getClimateSuitabilityScore(),
                    suitability.getSoilSuitabilityScore(),
                    suitability.getTerrainSuitabilityScore(),
                    suitability.getWaterSuitabilityScore()
            );
            
            Double maxComponent = maxOf(
                    suitability.getClimateSuitabilityScore(),
                    suitability.getSoilSuitabilityScore(),
                    suitability.getTerrainSuitabilityScore(),
                    suitability.getWaterSuitabilityScore()
            );
            
            assertTrue(
                suitability.getOverallSuitabilityScore() >= minComponent &&
                suitability.getOverallSuitabilityScore() <= maxComponent,
                "Overall score should be between min and max component scores"
            );
        }
    }

    /**
     * Property 3: Classification should match score range.
     * 
     * The suitability classification (HIGHLY_SUITABLE, SUITABLE, MARGINALLY_SUITABLE, NOT_SUITABLE)
     * should correctly correspond to the overall score range.
     */
    @Property
    @DisplayName("Classification should match score range")
    void classificationShouldMatchScoreRange(
            @ForAll("validGaezCropData") GaezCropData gaezData) {
        // Arrange
        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue(gaezData.getZoneCode()))
                .thenReturn(List.of(gaezData));
        when(soilHealthCardRepository.findMostRecentByFarmerId(anyString()))
                .thenReturn(java.util.Optional.empty());

        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST-FARMER")
                .agroEcologicalZoneCode(gaezData.getZoneCode())
                .build();

        // Act
        List<GaezCropSuitabilityDto> result = 
                gaezSuitabilityService.calculateSuitabilityScores(request);

        // Assert
        if (!result.isEmpty()) {
            GaezCropSuitabilityDto suitability = result.get(0);
            Double score = suitability.getOverallSuitabilityScore();
            GaezCropSuitabilityDto.SuitabilityClassification classification = 
                    suitability.getSuitabilityClassification();
            
            assertAll("Classification should match score range",
                () -> {
                    if (score >= 80.0) {
                        assertEquals(GaezCropSuitabilityDto.SuitabilityClassification.HIGHLY_SUITABLE,
                            classification, "Score >= 80 should be HIGHLY_SUITABLE");
                    }
                },
                () -> {
                    if (score >= 60.0 && score < 80.0) {
                        assertEquals(GaezCropSuitabilityDto.SuitabilityClassification.SUITABLE,
                            classification, "Score 60-79 should be SUITABLE");
                    }
                },
                () -> {
                    if (score >= 40.0 && score < 60.0) {
                        assertEquals(GaezCropSuitabilityDto.SuitabilityClassification.MARGINALLY_SUITABLE,
                            classification, "Score 40-59 should be MARGINALLY_SUITABLE");
                    }
                },
                () -> {
                    if (score < 40.0) {
                        assertEquals(GaezCropSuitabilityDto.SuitabilityClassification.NOT_SUITABLE,
                            classification, "Score < 40 should be NOT_SUITABLE");
                    }
                }
            );
        }
    }

    /**
     * Property 4: Results should be sorted by overall score in descending order.
     * 
     * When multiple crops are returned, they should be sorted by overall
     * suitability score in descending order.
     */
    @Property
    @DisplayName("Results should be sorted by overall score in descending order")
    void resultsShouldBeSortedByScoreDescending(
            @ForAll("gaezDataListWithMultipleCrops") List<GaezCropData> gaezDataList) {
        // Arrange
        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue("AEZ-05"))
                .thenReturn(gaezDataList);
        when(soilHealthCardRepository.findMostRecentByFarmerId(anyString()))
                .thenReturn(java.util.Optional.empty());

        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST-FARMER")
                .agroEcologicalZoneCode("AEZ-05")
                .build();

        // Act
        List<GaezCropSuitabilityDto> result = 
                gaezSuitabilityService.calculateSuitabilityScores(request);

        // Assert
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(
                result.get(i).getOverallSuitabilityScore() >= result.get(i + 1).getOverallSuitabilityScore(),
                String.format("Crop at index %d should have score >= crop at index %d: %s >= %s",
                    i, i + 1,
                    result.get(i).getOverallSuitabilityScore(),
                    result.get(i + 1).getOverallSuitabilityScore())
            );
        }
    }

    // Generators

    @Provide
    Arbitrary<GaezCropData> validGaezCropData() {
        return Combinators.combine(
            Arbitraries.of("AEZ-05", "AEZ-06", "AEZ-09", "AEZ-10", "AEZ-13", "AEZ-14"),
            Arbitraries.of("RICE", "WHEAT", "COTTON", "SOYBEAN", "GROUNDNUT", "MUSTARD", "MAIZE"),
            Arbitraries.doubles().between(40, 100),
            Arbitraries.doubles().between(40, 100),
            Arbitraries.doubles().between(40, 100),
            Arbitraries.doubles().between(40, 100),
            Arbitraries.of(true, false),
            Arbitraries.of("LOW", "MEDIUM", "HIGH")
        ).as((zoneCode, cropCode, overall, climate, soil, terrain, kharif, risk) -> {
            GaezCropData.SuitabilityClassification classification;
            if (overall >= 80) {
                classification = GaezCropData.SuitabilityClassification.HIGHLY_SUITABLE;
            } else if (overall >= 60) {
                classification = GaezCropData.SuitabilityClassification.SUITABLE;
            } else if (overall >= 40) {
                classification = GaezCropData.SuitabilityClassification.MARGINALLY_SUITABLE;
            } else {
                classification = GaezCropData.SuitabilityClassification.NOT_SUITABLE;
            }

            return GaezCropData.builder()
                    .id(1L)
                    .zoneCode(zoneCode)
                    .cropCode(cropCode)
                    .cropName(cropCode)
                    .overallSuitabilityScore(overall)
                    .suitabilityClassification(classification)
                    .climateSuitabilityScore(climate)
                    .soilSuitabilityScore(soil)
                    .terrainSuitabilityScore(terrain)
                    .waterSuitabilityScore(terrain)
                    .rainfedPotentialYield(3000.0)
                    .irrigatedPotentialYield(4500.0)
                    .waterRequirementsMm(500.0)
                    .growingSeasonDays(120)
                    .kharifSuitable(kharif)
                    .rabiSuitable(!kharif)
                    .zaidSuitable(false)
                    .climateRiskLevel(GaezCropData.ClimateRiskLevel.valueOf(risk))
                    .dataVersion("v4")
                    .dataResolution("5 arc-min")
                    .isActive(true)
                    .build();
        });
    }

    @Provide
    Arbitrary<List<GaezCropData>> gaezDataListWithMultipleCrops() {
        return validGaezCropData().flatMap(arbitrary -> 
            Arbitraries.integers().between(3, 10).flatMap(size -> 
                Arbitraries.of(arbitrary).list().ofSize(size)
            )
        );
    }

    // Helper methods

    private Double minOf(Double... values) {
        Double min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    private Double maxOf(Double... values) {
        Double max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }
}

