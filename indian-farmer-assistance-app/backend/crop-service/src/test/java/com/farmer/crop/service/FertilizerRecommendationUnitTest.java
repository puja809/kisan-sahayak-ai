package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.repository.FertilizerApplicationRepository;
import com.farmer.crop.repository.SoilHealthCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FertilizerRecommendationService.
 * 
 * Tests:
 * - Recommendation generation with soil data
 * - Default recommendations without soil data
 * - Nutrient calculation
 * - Over/under-application detection
 * 
 * Validates: Requirements 11C.1, 11C.2, 11C.7, 11C.8
 */
@ExtendWith(MockitoExtension.class)
class FertilizerRecommendationUnitTest {

    @Mock
    private SoilHealthCardRepository soilHealthCardRepository;

    @Mock
    private FertilizerApplicationRepository fertilizerApplicationRepository;

    private FertilizerRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);
    }

    @Test
    @DisplayName("Test recommendation generation with soil data")
    void testRecommendationWithSoilData() {
        // Arrange
        SoilHealthCardDto soilHealthCard = SoilHealthCardDto.builder()
                .nitrogenKgHa(200.0)  // Low nitrogen
                .phosphorusKgHa(8.0)  // Low phosphorus
                .potassiumKgHa(100.0) // Low potassium
                .zincPpm(0.4)      // Low zinc
                .build();

        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .cropName("RICE")
                .areaAcres(2.0)
                .soilHealthCard(soilHealthCard)
                .includeOrganicAlternatives(true)
                .includeSplitApplication(true)
                .build();

        // Act
        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getNutrientRequirements());
        assertNotNull(response.getRecommendations());
        assertFalse(response.getRecommendations().isEmpty());
        assertTrue(response.isSoilHealthCardUsed());
        assertNotNull(response.getNutrientDeficiencies());
        assertFalse(response.getNutrientDeficiencies().isEmpty());
        
        // Verify recommendations include fertilizer type, quantity, timing
        for (FertilizerRecommendationResponseDto.RecommendedFertilizerDto rec : response.getRecommendations()) {
            assertNotNull(rec.getFertilizerType());
            assertNotNull(rec.getQuantityKgPerAcre());
            assertNotNull(rec.getApplicationTiming());
            assertNotNull(rec.getApplicationStage());
        }
    }

    @Test
    @DisplayName("Test default recommendations without soil data")
    void testDefaultRecommendationsWithoutSoilData() {
        // Arrange
        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId("FARMER-002")
                .cropName("WHEAT")
                .areaAcres(1.0)
                .includeOrganicAlternatives(false)
                .includeSplitApplication(true)
                .build();

        // Act
        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getNutrientRequirements());
        assertFalse(response.isSoilHealthCardUsed());
        assertNotNull(response.getRecommendations());
        assertFalse(response.getRecommendations().isEmpty());
        
        // Verify default nutrient requirements for wheat
        assertTrue(response.getNutrientRequirements().getNitrogenKgPerAcre() > 0.0);
        assertTrue(response.getNutrientRequirements().getPhosphorusKgPerAcre() > 0.0);
        assertTrue(response.getNutrientRequirements().getPotassiumKgPerAcre() > 0.0);
    }

    @Test
    @DisplayName("Test nutrient calculation for multiple applications")
    void testNutrientCalculation() {
        // Arrange
        List<FertilizerApplication> applications = Arrays.asList(
                createFertilizerApplication(1L, "Urea", 50.0, 
                        46.0, 0.0, 0.0),
                createFertilizerApplication(2L, "DAP", 30.0, 
                        18.0, 46.0, 0.0),
                createFertilizerApplication(3L, "MOP", 25.0, 
                        0.0, 0.0, 60.0)
        );

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(applications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getTotalNutrientInput());
        
        // Verify nitrogen: 50 * 0.46 + 30 * 0.18 = 23 + 5.4 = 28.4 kg
        Double expectedN = 28.40;
        assertEquals(expectedN, response.getTotalNutrientInput().getTotalNitrogenKg(), 0.01,
                "Nitrogen calculation should be correct");
        
        // Verify phosphorus: 30 * 0.46 = 13.8 kg
        Double expectedP = 13.80;
        assertEquals(expectedP, response.getTotalNutrientInput().getTotalPhosphorusKg(), 0.01,
                "Phosphorus calculation should be correct");
        
        // Verify potassium: 25 * 0.60 = 15 kg
        Double expectedK = 15.00;
        assertEquals(expectedK, response.getTotalNutrientInput().getTotalPotassiumKg(), 0.01,
                "Potassium calculation should be correct");
    }

    @Test
    @DisplayName("Test over-application detection")
    void testOverApplicationDetection() {
        // Arrange - Create applications with excessive nutrients
        List<FertilizerApplication> applications = Arrays.asList(
                createFertilizerApplication(1L, "Urea", 200.0, 
                        46.0, 0.0, 0.0), // 92 kg N - excessive
                createFertilizerApplication(2L, "DAP", 100.0, 
                        18.0, 46.0, 0.0) // 46 kg P - excessive
        );

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(applications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getTotalNutrientInput());
        
        // Verify excessive nitrogen (92 kg vs typical 60 kg for rice)
        assertTrue(response.getTotalNutrientInput().getTotalNitrogenKg() > 60.0, 
                "Should detect over-application of nitrogen");
        
        // Verify excessive phosphorus (46 kg vs typical 30 kg for rice)
        assertTrue(response.getTotalNutrientInput().getTotalPhosphorusKg() > 30.0, 
                "Should detect over-application of phosphorus");
    }

    @Test
    @DisplayName("Test under-application detection")
    void testUnderApplicationDetection() {
        // Arrange - Create applications with insufficient nutrients
        List<FertilizerApplication> applications = Arrays.asList(
                createFertilizerApplication(1L, "Urea", 20.0, 
                        46.0, 0.0, 0.0), // 9.2 kg N - insufficient
                createFertilizerApplication(2L, "DAP", 10.0, 
                        18.0, 46.0, 0.0) // 4.6 kg P - insufficient
        );

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(applications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getTotalNutrientInput());
        
        // Verify insufficient nitrogen (9.2 kg vs typical 60 kg for rice)
        assertTrue(response.getTotalNutrientInput().getTotalNitrogenKg() < 60.0, 
                "Should detect under-application of nitrogen");
        
        // Verify insufficient phosphorus (4.6 kg vs typical 30 kg for rice)
        assertTrue(response.getTotalNutrientInput().getTotalPhosphorusKg() < 30.0, 
                "Should detect under-application of phosphorus");
    }

    @Test
    @DisplayName("Test organic alternatives generation")
    void testOrganicAlternatives() {
        // Arrange
        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId("FARMER-003")
                .cropName("COTTON")
                .areaAcres(1.0)
                .includeOrganicAlternatives(true)
                .build();

        // Act
        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getOrganicAlternatives());
        assertFalse(response.getOrganicAlternatives().isEmpty());
        
        // Verify organic alternatives include vermicompost, FYM, green manure, biofertilizers
        List<String> alternativeTypes = response.getOrganicAlternatives().stream()
                .map(FertilizerRecommendationResponseDto.OrganicAlternativeDto::getAlternativeType)
                .toList();
        
        assertTrue(alternativeTypes.contains("VERMICOMPOST"));
        assertTrue(alternativeTypes.contains("FYM"));
        assertTrue(alternativeTypes.contains("GREEN_MANURE"));
        assertTrue(alternativeTypes.contains("BIOFERTILIZER"));
    }

    @Test
    @DisplayName("Test split application schedule")
    void testSplitApplicationSchedule() {
        // Arrange
        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId("FARMER-004")
                .cropName("RICE")
                .areaAcres(1.0)
                .includeSplitApplication(true)
                .build();

        // Act
        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getApplicationSchedule());
        assertFalse(response.getApplicationSchedule().isEmpty());
        
        // Verify schedule includes basal dose and top dressing
        List<String> scheduleNames = response.getApplicationSchedule().stream()
                .map(FertilizerRecommendationResponseDto.ApplicationScheduleDto::getApplicationName)
                .toList();
        
        assertTrue(scheduleNames.stream().anyMatch(name -> name.contains("Basal") || name.toLowerCase().contains("basal")));
        assertTrue(scheduleNames.stream().anyMatch(name -> name.toLowerCase().contains("top") || name.toLowerCase().contains("dressing")));
        
        // Verify each schedule has specific date
        for (FertilizerRecommendationResponseDto.ApplicationScheduleDto schedule : response.getApplicationSchedule()) {
            assertNotNull(schedule.getSuggestedDate());
            assertNotNull(schedule.getDescription());
            assertNotNull(schedule.getFertilizers());
            assertFalse(schedule.getFertilizers().isEmpty());
        }
    }

    @Test
    @DisplayName("Test cost estimation")
    void testCostEstimation() {
        // Arrange
        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId("FARMER-005")
                .cropName("RICE")
                .areaAcres(2.0)
                .build();

        // Act
        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getEstimatedCost());
        assertTrue(response.getEstimatedCost() > 0.0);
        
        // Cost should scale with area (2 acres)
        Double costPerAcre = response.getEstimatedCost() / 2.0;
        assertTrue(costPerAcre > 0.0);
    }

    @Test
    @DisplayName("Test recording fertilizer application")
    void testRecordApplication() {
        // Arrange
        FertilizerApplicationRequestDto request = FertilizerApplicationRequestDto.builder()
                .cropId(1L)
                .farmerId("FARMER-006")
                .fertilizerType("Urea")
                .fertilizerCategory("CHEMICAL")
                .quantityKg(50.0)
                .applicationDate(LocalDate.now())
                .applicationStage("Basal")
                .cost(300.0)
                .nitrogenPercent(46.0)
                .build();

        FertilizerApplication savedApplication = FertilizerApplication.builder()
                .id(1L)
                .cropId(request.getCropId())
                .farmerId(request.getFarmerId())
                .fertilizerType(request.getFertilizerType())
                .fertilizerCategory(FertilizerApplication.FertilizerCategory.CHEMICAL)
                .quantityKg(request.getQuantityKg())
                .applicationDate(request.getApplicationDate())
                .applicationStage(request.getApplicationStage())
                .cost(request.getCost())
                .nitrogenPercent(request.getNitrogenPercent())
                .build();

        when(fertilizerApplicationRepository.save(any(FertilizerApplication.class)))
                .thenReturn(savedApplication);

        // Act
        FertilizerApplication result = service.recordApplication(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Urea", result.getFertilizerType());
        assertEquals(50.0, result.getQuantityKg());
        assertEquals(46.0, result.getNitrogenPercent());
        
        verify(fertilizerApplicationRepository).save(any(FertilizerApplication.class));
    }

    @Test
    @DisplayName("Test empty application history")
    void testEmptyApplicationHistory() {
        // Arrange
        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(Collections.emptyList());

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getApplications());
        assertTrue(response.getApplications().isEmpty());
        assertNotNull(response.getTotalNutrientInput());
        assertEquals(0.0, response.getTotalNutrientInput().getTotalNitrogenKg());
        assertEquals(0.0, response.getTotalNutrientInput().getTotalPhosphorusKg());
        assertEquals(0.0, response.getTotalNutrientInput().getTotalPotassiumKg());
    }

    @Test
    @DisplayName("Test different crop types have different recommendations")
    void testDifferentCropRecommendations() {
        // Arrange
        String[] crops = {"RICE", "WHEAT", "COTTON", "POTATO"};

        for (String crop : crops) {
            FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                    .farmerId("FARMER-007")
                    .cropName(crop)
                    .areaAcres(1.0)
                    .build();

            // Act
            FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

            // Assert
            assertTrue(response.isSuccess());
            assertNotNull(response.getNutrientRequirements());
            
            // Different crops should have different nutrient requirements
            assertNotNull(response.getRecommendations());
            assertFalse(response.getRecommendations().isEmpty());
        }
    }

    /**
     * Helper method to create a fertilizer application for testing.
     */
    private FertilizerApplication createFertilizerApplication(
            Long id, String type, Double quantity, 
            Double n, Double p, Double k) {
        return FertilizerApplication.builder()
                .id(id)
                .cropId(1L)
                .farmerId("TEST-FARMER")
                .fertilizerType(type)
                .fertilizerCategory(FertilizerApplication.FertilizerCategory.CHEMICAL)
                .quantityKg(quantity)
                .applicationDate(LocalDate.now().minusDays(id))
                .nitrogenPercent(n)
                .phosphorusPercent(p)
                .potassiumPercent(k)
                .build();
    }
}

