package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.AgroEcologicalZone;
import com.farmer.crop.repository.AgroEcologicalZoneRepository;
import com.farmer.crop.repository.GaezCropDataRepository;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for CropRecommendationService.
 * 
 * These tests verify universal properties that should hold across all valid inputs.
 * 
 * **Validates: Requirements 2.5, 2.6**
 * 
 * Property 6: Descending Ranking Order
 * - For any list of items with numeric scores, the displayed list should be sorted 
 *   in descending order such that for any two adjacent items, the first has a score 
 *   greater than or equal to the second.
 * 
 * Property 7: Crop Recommendation Data Completeness
 * - For any crop recommendation, all required fields (crop name, expected yield range, 
 *   potential yield gap, water requirements, growing season duration, suitability score) 
 *   should be present in the response.
 */
class CropRecommendationPropertyTest {

    /**
     * Property 6: Descending Ranking Order
     * 
     * For any list of recommended crops, the overall suitability scores should be 
     * in descending order (higher scores ranked first).
     * 
     * **Validates: Requirement 2.5**
     */
    @Property
    void propertyDescendingRankingOrder(
            @ForAll @IntRange(min = 1, max = 10) int numCrops,
            @ForAll Double baseScore,
            @ForAll String state
    ) {
        // Arrange - Create mock services
        AgroEcologicalZoneService agroEcologicalZoneService = mock(AgroEcologicalZoneService.class);
        GaezSuitabilityService gaezSuitabilityService = mock(GaezSuitabilityService.class);
        GaezDataImportService gaezDataImportService = mock(GaezDataImportService.class);
        AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
        GaezCropDataRepository gaezCropDataRepository = mock(GaezCropDataRepository.class);
        MarketDataService marketDataService = new MarketDataService();
        ClimateRiskService climateRiskService = new ClimateRiskService();
        SeedVarietyService seedVarietyService = new SeedVarietyService();

        CropRecommendationService service = new CropRecommendationService(
                agroEcologicalZoneService,
                gaezSuitabilityService,
                gaezDataImportService,
                zoneRepository,
                gaezCropDataRepository,
                marketDataService,
                climateRiskService,
                seedVarietyService
        );

        // Create mock suitability data with descending scores
        List<GaezCropSuitabilityDto> suitabilityList = createMockSuitabilityList(numCrops, baseScore);
        
        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);
        when(zoneRepository.findByZoneCode(any()))
                .thenReturn(Optional.of(createMockZone()));
        when(agroEcologicalZoneService.getZoneForLocation(any()))
                .thenReturn(createMockZoneResponse());

        // Act
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST-FARMER")
                .district("TestDistrict")
                .state(state)
                .includeMarketData(false)
                .includeClimateRiskAssessment(false)
                .build();

        CropRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert - Verify descending order
        assertTrue(response.isSuccess());
        assertFalse(response.getRecommendations().isEmpty());
        
        List<CropRecommendationResponseDto.RecommendedCropDto> recommendations = response.getRecommendations();
        
        for (int i = 0; i < recommendations.size() - 1; i++) {
            Double currentScore = recommendations.get(i).getOverallSuitabilityScore();
            Double nextScore = recommendations.get(i + 1).getOverallSuitabilityScore();
            assertTrue(currentScore >= nextScore,
                    String.format("Ranking order violated at position %d: %s >= %s expected but was %s < %s",
                            i, currentScore, nextScore, currentScore, nextScore));
        }
    }

    /**
     * Property 7: Crop Recommendation Data Completeness
     * 
     * For any successful crop recommendation, all required fields should be present.
     * 
     * **Validates: Requirement 2.6**
     */
    @Property
    void propertyCropRecommendationDataCompleteness(
            @ForAll @IntRange(min = 1, max = 10) int numCrops,
            @ForAll String state
    ) {
        // Arrange - Create mock services
        AgroEcologicalZoneService agroEcologicalZoneService = mock(AgroEcologicalZoneService.class);
        GaezSuitabilityService gaezSuitabilityService = mock(GaezSuitabilityService.class);
        GaezDataImportService gaezDataImportService = mock(GaezDataImportService.class);
        AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
        GaezCropDataRepository gaezCropDataRepository = mock(GaezCropDataRepository.class);
        MarketDataService marketDataService = new MarketDataService();
        ClimateRiskService climateRiskService = new ClimateRiskService();
        SeedVarietyService seedVarietyService = new SeedVarietyService();

        CropRecommendationService service = new CropRecommendationService(
                agroEcologicalZoneService,
                gaezSuitabilityService,
                gaezDataImportService,
                zoneRepository,
                gaezCropDataRepository,
                marketDataService,
                climateRiskService,
                seedVarietyService
        );

        // Create mock suitability data
        List<GaezCropSuitabilityDto> suitabilityList = createMockSuitabilityListWithCompleteData(numCrops);
        
        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);
        when(zoneRepository.findByZoneCode(any()))
                .thenReturn(Optional.of(createMockZone()));
        when(agroEcologicalZoneService.getZoneForLocation(any()))
                .thenReturn(createMockZoneResponse());

        // Act
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST-FARMER")
                .district("TestDistrict")
                .state(state)
                .includeMarketData(true)
                .includeClimateRiskAssessment(true)
                .build();

        CropRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert - Verify all required fields are present
        assertTrue(response.isSuccess());
        assertFalse(response.getRecommendations().isEmpty());
        
        for (CropRecommendationResponseDto.RecommendedCropDto rec : response.getRecommendations()) {
            // Required fields per Requirement 2.6
            assertNotNull(rec.getGaezSuitability(), "GAEZ suitability data must be present");
            assertNotNull(rec.getOverallSuitabilityScore(), "Suitability score must be present");
            assertNotNull(rec.getExpectedYieldPerAcre(), "Expected yield must be present");
            assertNotNull(rec.getWaterRequirementPerAcre(), "Water requirements must be present");
            assertNotNull(rec.getGrowingDurationDays(), "Growing season duration must be present");
            assertNotNull(rec.getClimateRiskLevel(), "Climate risk level must be present");
            assertNotNull(rec.getRecommendedVarieties(), "Recommended varieties must be present");
            assertNotNull(rec.getEstimatedInputCost(), "Input cost must be present");
            assertNotNull(rec.getEstimatedNetProfit(), "Net profit must be present");
            
            // Verify yield gap is calculated when potential yield is available
            GaezCropSuitabilityDto gaez = rec.getGaezSuitability();
            if (gaez.getIrrigatedPotentialYield() != null && gaez.getExpectedYieldExpected() != null) {
                assertNotNull(rec.getPotentialYieldGap(), "Potential yield gap must be present when potential yield data exists");
            }
        }
    }

    /**
     * Property: Suitability Score Range
     * 
     * All suitability scores should be within valid range [0, 100].
     */
    @Property
    void propertySuitabilityScoreRange(
            @ForAll @IntRange(min = 1, max = 10) int numCrops,
            @ForAll String state
    ) {
        // Arrange
        AgroEcologicalZoneService agroEcologicalZoneService = mock(AgroEcologicalZoneService.class);
        GaezSuitabilityService gaezSuitabilityService = mock(GaezSuitabilityService.class);
        GaezDataImportService gaezDataImportService = mock(GaezDataImportService.class);
        AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
        GaezCropDataRepository gaezCropDataRepository = mock(GaezCropDataRepository.class);
        MarketDataService marketDataService = new MarketDataService();
        ClimateRiskService climateRiskService = new ClimateRiskService();
        SeedVarietyService seedVarietyService = new SeedVarietyService();

        CropRecommendationService service = new CropRecommendationService(
                agroEcologicalZoneService,
                gaezSuitabilityService,
                gaezDataImportService,
                zoneRepository,
                gaezCropDataRepository,
                marketDataService,
                climateRiskService,
                seedVarietyService
        );

        List<GaezCropSuitabilityDto> suitabilityList = createMockSuitabilityListWithCompleteData(numCrops);
        
        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);
        when(zoneRepository.findByZoneCode(any()))
                .thenReturn(Optional.of(createMockZone()));
        when(agroEcologicalZoneService.getZoneForLocation(any()))
                .thenReturn(createMockZoneResponse());

        // Act
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST-FARMER")
                .district("TestDistrict")
                .state(state)
                .build();

        CropRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        
        for (CropRecommendationResponseDto.RecommendedCropDto rec : response.getRecommendations()) {
            assertTrue(rec.getOverallSuitabilityScore() >= 0.0,
                    "Suitability score should be >= 0");
            assertTrue(rec.getOverallSuitabilityScore() <= 100.0,
                    "Suitability score should be <= 100");
        }
    }

    /**
     * Property: Rank Continuity
     * 
     * Ranks should be consecutive integers starting from 1.
     */
    @Property
    void propertyRankContinuity(
            @ForAll @IntRange(min = 1, max = 10) int numCrops,
            @ForAll String state
    ) {
        // Arrange
        AgroEcologicalZoneService agroEcologicalZoneService = mock(AgroEcologicalZoneService.class);
        GaezSuitabilityService gaezSuitabilityService = mock(GaezSuitabilityService.class);
        GaezDataImportService gaezDataImportService = mock(GaezDataImportService.class);
        AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
        GaezCropDataRepository gaezCropDataRepository = mock(GaezCropDataRepository.class);
        MarketDataService marketDataService = new MarketDataService();
        ClimateRiskService climateRiskService = new ClimateRiskService();
        SeedVarietyService seedVarietyService = new SeedVarietyService();

        CropRecommendationService service = new CropRecommendationService(
                agroEcologicalZoneService,
                gaezSuitabilityService,
                gaezDataImportService,
                zoneRepository,
                gaezCropDataRepository,
                marketDataService,
                climateRiskService,
                seedVarietyService
        );

        List<GaezCropSuitabilityDto> suitabilityList = createMockSuitabilityListWithCompleteData(numCrops);
        
        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);
        when(zoneRepository.findByZoneCode(any()))
                .thenReturn(Optional.of(createMockZone()));
        when(agroEcologicalZoneService.getZoneForLocation(any()))
                .thenReturn(createMockZoneResponse());

        // Act
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST-FARMER")
                .district("TestDistrict")
                .state(state)
                .build();

        CropRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        
        List<CropRecommendationResponseDto.RecommendedCropDto> recommendations = response.getRecommendations();
        
        for (int i = 0; i < recommendations.size(); i++) {
            assertEquals(i + 1, recommendations.get(i).getRank(),
                    "Rank should be consecutive starting from 1");
        }
    }

    // Helper methods

    private List<GaezCropSuitabilityDto> createMockSuitabilityList(
            int numCrops, Double baseScore) {
        String[] cropCodes = {"RICE", "WHEAT", "COTTON", "SOYBEAN", "GROUNDNUT", 
                              "MUSTARD", "PULSES", "MAIZE", "SUGARCANE", "POTATO"};
        
        java.util.List<GaezCropSuitabilityDto> list = new java.util.ArrayList<>();
        
        for (int i = 0; i < numCrops; i++) {
            Double score = baseScore - (i * 5.0);
            list.add(GaezCropSuitabilityDto.builder()
                    .cropCode(cropCodes[i])
                    .cropName(cropCodes[i])
                    .overallSuitabilityScore(Math.max(0.0, score))
                    .climateSuitabilityScore(score)
                    .soilSuitabilityScore(score)
                    .terrainSuitabilityScore(score)
                    .waterSuitabilityScore(score)
                    .rainfedPotentialYield(4000.0)
                    .irrigatedPotentialYield(5000.0)
                    .expectedYieldExpected(3500.0)
                    .waterRequirementsMm(500.0)
                    .growingSeasonDays(120)
                    .kharifSuitable(true)
                    .rabiSuitable(false)
                    .zaidSuitable(false)
                    .climateRiskLevel(GaezCropSuitabilityDto.ClimateRiskLevel.LOW)
                    .dataVersion("v4")
                    .dataResolution("5 arc-min")
                    .build());
        }
        
        return list;
    }

    private List<GaezCropSuitabilityDto> createMockSuitabilityListWithCompleteData(int numCrops) {
        String[] cropCodes = {"RICE", "WHEAT", "COTTON", "SOYBEAN", "GROUNDNUT", 
                              "MUSTARD", "PULSES", "MAIZE", "SUGARCANE", "POTATO"};
        
        java.util.List<GaezCropSuitabilityDto> list = new java.util.ArrayList<>();
        
        for (int i = 0; i < numCrops; i++) {
            Double score = 90.0 - i * 5.0;
            list.add(GaezCropSuitabilityDto.builder()
                    .cropCode(cropCodes[i])
                    .cropName(cropCodes[i])
                    .cropNameLocal(cropCodes[i] + " (Local)")
                    .overallSuitabilityScore(score)
                    .suitabilityClassification(GaezCropSuitabilityDto.SuitabilityClassification.SUITABLE)
                    .climateSuitabilityScore(score)
                    .soilSuitabilityScore(score)
                    .terrainSuitabilityScore(score)
                    .waterSuitabilityScore(score)
                    .rainfedPotentialYield(4000.0)
                    .irrigatedPotentialYield(5000.0)
                    .expectedYieldMin(3000.0)
                    .expectedYieldExpected(3500.0)
                    .expectedYieldMax(4000.0)
                    .waterRequirementsMm(500.0)
                    .growingSeasonDays(120)
                    .kharifSuitable(true)
                    .rabiSuitable(false)
                    .zaidSuitable(false)
                    .climateRiskLevel(GaezCropSuitabilityDto.ClimateRiskLevel.LOW)
                    .dataVersion("v4")
                    .dataResolution("5 arc-min")
                    .build());
        }
        
        return list;
    }

    private AgroEcologicalZone createMockZone() {
        return AgroEcologicalZone.builder()
                .id(1L)
                .zoneCode("AEZ-05")
                .zoneName("Upper Gangetic Plain Region")
                .description("Test zone")
                .climateType("Subtropical")
                .isActive(true)
                .build();
    }

    private ZoneLookupResponseDto createMockZoneResponse() {
        return ZoneLookupResponseDto.builder()
                .success(true)
                .zone(AgroEcologicalZoneDto.builder()
                        .zoneCode("AEZ-05")
                        .zoneName("Upper Gangetic Plain Region")
                        .build())
                .build();
    }
}

