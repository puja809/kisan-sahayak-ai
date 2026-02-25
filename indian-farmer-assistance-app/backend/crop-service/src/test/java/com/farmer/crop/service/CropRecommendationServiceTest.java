package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.AgroEcologicalZone;
import com.farmer.crop.repository.AgroEcologicalZoneRepository;
import com.farmer.crop.repository.GaezCropDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CropRecommendationService.
 * 
 * Tests the main crop recommendation service that integrates
 * agro-ecological zone mapping, GAEZ data, and soil health card data.
 * 
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@ExtendWith(MockitoExtension.class)
class CropRecommendationServiceTest {

    @Mock
    private AgroEcologicalZoneService agroEcologicalZoneService;

    @Mock
    private GaezSuitabilityService gaezSuitabilityService;

    @Mock
    private GaezDataImportService gaezDataImportService;

    @Mock
    private AgroEcologicalZoneRepository zoneRepository;

    @Mock
    private GaezCropDataRepository gaezCropDataRepository;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private ClimateRiskService climateRiskService;

    @Mock
    private SeedVarietyService seedVarietyService;

    private CropRecommendationService cropRecommendationService;

    @BeforeEach
    void setUp() {
        cropRecommendationService = new CropRecommendationService(
                agroEcologicalZoneService,
                gaezSuitabilityService,
                gaezDataImportService,
                zoneRepository,
                gaezCropDataRepository,
                marketDataService,
                climateRiskService,
                seedVarietyService);
    }

    @Test
    @DisplayName("Should generate recommendations for valid location")
    void testGenerateRecommendations() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .season(CropRecommendationRequestDto.Season.RABI)
                .build();

        ZoneLookupResponseDto zoneResponse = ZoneLookupResponseDto.builder()
                .success(true)
                .zone(AgroEcologicalZoneDto.builder()
                        .zoneCode("AEZ-05")
                        .zoneName("Upper Gangetic Plain Region")
                        .build())
                .build();

        when(agroEcologicalZoneService.getZoneForLocation(any(LocationRequestDto.class)))
                .thenReturn(zoneResponse);
        when(zoneRepository.findByZoneCode("AEZ-05"))
                .thenReturn(Optional.of(createZone("AEZ-05", "Upper Gangetic Plain Region")));

        List<GaezCropSuitabilityDto> suitabilityList = Arrays.asList(
                createSuitability("WHEAT", "Wheat", new BigDecimal("90"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.LOW),
                createSuitability("MUSTARD", "Mustard", new BigDecimal("88"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.LOW)
        );

        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);

        // Act
        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(2, response.getRecommendationCount());
        assertEquals("Upper Gangetic Plain Region", response.getAgroEcologicalZone());
        assertEquals(CropRecommendationRequestDto.Season.RABI, response.getSeason());
        
        // Verify ranking
        assertEquals("WHEAT", response.getRecommendations().get(0).getGaezSuitability().getCropCode());
        assertEquals(1, response.getRecommendations().get(0).getRank());
    }

    @Test
    @DisplayName("Should return error when zone cannot be determined")
    void testGenerateRecommendationsZoneNotFound() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Unknown")
                .state("Unknown")
                .build();

        ZoneLookupResponseDto zoneResponse = ZoneLookupResponseDto.builder()
                .success(false)
                .errorMessage("Location not found")
                .build();

        when(agroEcologicalZoneService.getZoneForLocation(any(LocationRequestDto.class)))
                .thenReturn(zoneResponse);

        // Act
        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(request);

        // Assert
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertEquals(0, response.getRecommendationCount());
    }

    @Test
    @DisplayName("Should incorporate soil health card data in recommendations")
    void testGenerateRecommendationsWithSoilHealthCard() {
        // Arrange
        SoilHealthCardDto soilHealthCard = SoilHealthCardDto.builder()
                .cardId("SHC-001")
                .farmerId("FARMER-001")
                .nitrogenKgHa(new BigDecimal("200"))
                .phosphorusKgHa(new BigDecimal("8"))
                .build();

        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .hasSoilHealthCard(true)
                .soilHealthCard(soilHealthCard)
                .build();

        ZoneLookupResponseDto zoneResponse = ZoneLookupResponseDto.builder()
                .success(true)
                .zone(AgroEcologicalZoneDto.builder()
                        .zoneCode("AEZ-05")
                        .zoneName("Upper Gangetic Plain Region")
                        .build())
                .build();

        when(agroEcologicalZoneService.getZoneForLocation(any(LocationRequestDto.class)))
                .thenReturn(zoneResponse);
        when(zoneRepository.findByZoneCode("AEZ-05"))
                .thenReturn(Optional.of(createZone("AEZ-05", "Upper Gangetic Plain Region")));

        List<GaezCropSuitabilityDto> suitabilityList = Arrays.asList(
                createSuitability("WHEAT", "Wheat", new BigDecimal("85"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.LOW)
        );

        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);

        // Act
        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertTrue(response.getSoilHealthCardUsed());
    }

    @Test
    @DisplayName("Should filter by preferred crops")
    void testGenerateRecommendationsWithPreferredCrops() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .preferredCrops(Arrays.asList("WHEAT", "MUSTARD"))
                .build();

        ZoneLookupResponseDto zoneResponse = ZoneLookupResponseDto.builder()
                .success(true)
                .zone(AgroEcologicalZoneDto.builder()
                        .zoneCode("AEZ-05")
                        .zoneName("Upper Gangetic Plain Region")
                        .build())
                .build();

        when(agroEcologicalZoneService.getZoneForLocation(any(LocationRequestDto.class)))
                .thenReturn(zoneResponse);
        when(zoneRepository.findByZoneCode("AEZ-05"))
                .thenReturn(Optional.of(createZone("AEZ-05", "Upper Gangetic Plain Region")));

        List<GaezCropSuitabilityDto> suitabilityList = Arrays.asList(
                createSuitability("RICE", "Rice", new BigDecimal("75"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.MEDIUM),
                createSuitability("WHEAT", "Wheat", new BigDecimal("90"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.LOW),
                createSuitability("COTTON", "Cotton", new BigDecimal("70"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.HIGH)
        );

        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);

        // Act
        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        // WHEAT should be ranked higher due to preference
        assertEquals("WHEAT", response.getRecommendations().get(0).getGaezSuitability().getCropCode());
    }

    @Test
    @DisplayName("Should exclude specified crops")
    void testGenerateRecommendationsWithExcludedCrops() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .excludeCrops(Arrays.asList("COTTON"))
                .build();

        ZoneLookupResponseDto zoneResponse = ZoneLookupResponseDto.builder()
                .success(true)
                .zone(AgroEcologicalZoneDto.builder()
                        .zoneCode("AEZ-05")
                        .zoneName("Upper Gangetic Plain Region")
                        .build())
                .build();

        when(agroEcologicalZoneService.getZoneForLocation(any(LocationRequestDto.class)))
                .thenReturn(zoneResponse);
        when(zoneRepository.findByZoneCode("AEZ-05"))
                .thenReturn(Optional.of(createZone("AEZ-05", "Upper Gangetic Plain Region")));

        List<GaezCropSuitabilityDto> suitabilityList = Arrays.asList(
                createSuitability("RICE", "Rice", new BigDecimal("75"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.MEDIUM),
                createSuitability("WHEAT", "Wheat", new BigDecimal("90"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.LOW),
                createSuitability("COTTON", "Cotton", new BigDecimal("70"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.HIGH)
        );

        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);

        // Act
        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        // COTTON should be excluded
        assertEquals(2, response.getRecommendationCount());
        assertTrue(response.getRecommendations().stream()
                .noneMatch(r -> "COTTON".equals(r.getGaezSuitability().getCropCode())));
    }

    @Test
    @DisplayName("Should apply minimum suitability threshold")
    void testGenerateRecommendationsWithMinThreshold() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .minSuitabilityScore(new BigDecimal("80"))
                .build();

        ZoneLookupResponseDto zoneResponse = ZoneLookupResponseDto.builder()
                .success(true)
                .zone(AgroEcologicalZoneDto.builder()
                        .zoneCode("AEZ-05")
                        .zoneName("Upper Gangetic Plain Region")
                        .build())
                .build();

        when(agroEcologicalZoneService.getZoneForLocation(any(LocationRequestDto.class)))
                .thenReturn(zoneResponse);
        when(zoneRepository.findByZoneCode("AEZ-05"))
                .thenReturn(Optional.of(createZone("AEZ-05", "Upper Gangetic Plain Region")));

        List<GaezCropSuitabilityDto> suitabilityList = Arrays.asList(
                createSuitability("RICE", "Rice", new BigDecimal("75"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.MEDIUM),
                createSuitability("WHEAT", "Wheat", new BigDecimal("90"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.LOW),
                createSuitability("COTTON", "Cotton", new BigDecimal("50"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.HIGH)
        );

        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);

        // Act
        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        // Only WHEAT should be returned (score >= 80)
        assertEquals(1, response.getRecommendationCount());
        assertEquals("WHEAT", response.getRecommendations().get(0).getGaezSuitability().getCropCode());
    }

    @Test
    @DisplayName("Should build climate risk summary")
    void testGenerateRecommendationsClimateRiskSummary() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .build();

        ZoneLookupResponseDto zoneResponse = ZoneLookupResponseDto.builder()
                .success(true)
                .zone(AgroEcologicalZoneDto.builder()
                        .zoneCode("AEZ-05")
                        .zoneName("Upper Gangetic Plain Region")
                        .build())
                .build();

        when(agroEcologicalZoneService.getZoneForLocation(any(LocationRequestDto.class)))
                .thenReturn(zoneResponse);
        when(zoneRepository.findByZoneCode("AEZ-05"))
                .thenReturn(Optional.of(createZone("AEZ-05", "Upper Gangetic Plain Region")));

        List<GaezCropSuitabilityDto> suitabilityList = Arrays.asList(
                createSuitability("WHEAT", "Wheat", new BigDecimal("90"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.LOW),
                createSuitability("COTTON", "Cotton", new BigDecimal("70"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.HIGH)
        );

        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);

        // Act
        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getClimateRiskSummary());
        assertEquals(1, response.getClimateRiskSummary().getHighRiskCropCount());
        assertEquals(1, response.getClimateRiskSummary().getLowRiskCropCount());
    }

    @Test
    @DisplayName("Should use provided zone code if available")
    void testGenerateRecommendationsWithProvidedZoneCode() {
        // Arrange
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .agroEcologicalZoneCode("AEZ-05")
                .build();

        when(zoneRepository.findByZoneCode("AEZ-05"))
                .thenReturn(Optional.of(createZone("AEZ-05", "Upper Gangetic Plain Region")));

        List<GaezCropSuitabilityDto> suitabilityList = Arrays.asList(
                createSuitability("WHEAT", "Wheat", new BigDecimal("90"), 
                        GaezCropSuitabilityDto.ClimateRiskLevel.LOW)
        );

        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);

        // Act
        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Upper Gangetic Plain Region", response.getAgroEcologicalZone());
    }

    // Helper methods

    private AgroEcologicalZone createZone(String zoneCode, String zoneName) {
        return AgroEcologicalZone.builder()
                .id(1L)
                .zoneCode(zoneCode)
                .zoneName(zoneName)
                .description("Test zone")
                .climateType("Subtropical")
                .isActive(true)
                .build();
    }

    private GaezCropSuitabilityDto createSuitability(String cropCode, String cropName, 
            BigDecimal score, GaezCropSuitabilityDto.ClimateRiskLevel climateRisk) {
        return GaezCropSuitabilityDto.builder()
                .cropCode(cropCode)
                .cropName(cropName)
                .overallSuitabilityScore(score)
                .suitabilityClassification(score.compareTo(new BigDecimal("80")) >= 0 ? 
                        GaezCropSuitabilityDto.SuitabilityClassification.HIGHLY_SUITABLE :
                        GaezCropSuitabilityDto.SuitabilityClassification.SUITABLE)
                .climateSuitabilityScore(score)
                .soilSuitabilityScore(score)
                .terrainSuitabilityScore(score)
                .waterSuitabilityScore(score)
                .rainfedPotentialYield(new BigDecimal("4500"))
                .irrigatedPotentialYield(new BigDecimal("5500"))
                .waterRequirementsMm(new BigDecimal("500"))
                .growingSeasonDays(120)
                .kharifSuitable(false)
                .rabiSuitable(true)
                .zaidSuitable(false)
                .climateRiskLevel(climateRisk)
                .dataVersion("v4")
                .dataResolution("5 arc-min")
                .build();
    }
}