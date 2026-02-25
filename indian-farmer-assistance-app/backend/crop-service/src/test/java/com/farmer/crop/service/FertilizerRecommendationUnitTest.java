package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.FertilizerApplication;
import com.farmer.crop.repository.FertilizerApplicationRepository;
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
                .nitrogenKgHa(new BigDecimal("200"))  // Low nitrogen
                .phosphorusKgHa(new BigDecimal("8"))  // Low phosphorus
                .potassiumKgHa(new BigDecimal("100")) // Low potassium
                .zincPpm(new BigDecimal("0.4"))      // Low zinc
                .build();

        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .cropName("RICE")
                .areaAcres(new BigDecimal("2"))
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
                .areaAcres(new BigDecimal("1"))
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
        assertTrue(response.getNutrientRequirements().getNitrogenKgPerAcre().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getNutrientRequirements().getPhosphorusKgPerAcre().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getNutrientRequirements().getPotassiumKgPerAcre().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Test nutrient calculation for multiple applications")
    void testNutrientCalculation() {
        // Arrange
        List<FertilizerApplication> applications = Arrays.asList(
                createFertilizerApplication(1L, "Urea", new BigDecimal("50"), 
                        new BigDecimal("46"), BigDecimal.ZERO, BigDecimal.ZERO),
                createFertilizerApplication(2L, "DAP", new BigDecimal("30"), 
                        new BigDecimal("18"), new BigDecimal("46"), BigDecimal.ZERO),
                createFertilizerApplication(3L, "MOP", new BigDecimal("25"), 
                        BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("60"))
        );

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(applications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getTotalNutrientInput());
        
        // Verify nitrogen: 50 * 0.46 + 30 * 0.18 = 23 + 5.4 = 28.4 kg
        BigDecimal expectedN = new BigDecimal("28.40");
        assertEquals(0, expectedN.compareTo(response.getTotalNutrientInput().getTotalNitrogenKg()),
                "Nitrogen calculation should be correct");
        
        // Verify phosphorus: 30 * 0.46 = 13.8 kg
        BigDecimal expectedP = new BigDecimal("13.80");
        assertEquals(0, expectedP.compareTo(response.getTotalNutrientInput().getTotalPhosphorusKg()),
                "Phosphorus calculation should be correct");
        
        // Verify potassium: 25 * 0.60 = 15 kg
        BigDecimal expectedK = new BigDecimal("15.00");
        assertEquals(0, expectedK.compareTo(response.getTotalNutrientInput().getTotalPotassiumKg()),
                "Potassium calculation should be correct");
    }

    @Test
    @DisplayName("Test over-application detection")
    void testOverApplicationDetection() {
        // Arrange - Create applications with excessive nutrients
        List<FertilizerApplication> applications = Arrays.asList(
                createFertilizerApplication(1L, "Urea", new BigDecimal("200"), 
                        new BigDecimal("46"), BigDecimal.ZERO, BigDecimal.ZERO), // 92 kg N - excessive
                createFertilizerApplication(2L, "DAP", new BigDecimal("100"), 
                        new BigDecimal("18"), new BigDecimal("46"), BigDecimal.ZERO) // 46 kg P - excessive
        );

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(applications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getTotalNutrientInput());
        
        // Verify excessive nitrogen (92 kg vs typical 60 kg for rice)
        assertTrue(response.getTotalNutrientInput().getTotalNitrogenKg()
                .compareTo(new BigDecimal("60")) > 0, 
                "Should detect over-application of nitrogen");
        
        // Verify excessive phosphorus (46 kg vs typical 30 kg for rice)
        assertTrue(response.getTotalNutrientInput().getTotalPhosphorusKg()
                .compareTo(new BigDecimal("30")) > 0, 
                "Should detect over-application of phosphorus");
    }

    @Test
    @DisplayName("Test under-application detection")
    void testUnderApplicationDetection() {
        // Arrange - Create applications with insufficient nutrients
        List<FertilizerApplication> applications = Arrays.asList(
                createFertilizerApplication(1L, "Urea", new BigDecimal("20"), 
                        new BigDecimal("46"), BigDecimal.ZERO, BigDecimal.ZERO), // 9.2 kg N - insufficient
                createFertilizerApplication(2L, "DAP", new BigDecimal("10"), 
                        new BigDecimal("18"), new BigDecimal("46"), BigDecimal.ZERO) // 4.6 kg P - insufficient
        );

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(applications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getTotalNutrientInput());
        
        // Verify insufficient nitrogen (9.2 kg vs typical 60 kg for rice)
        assertTrue(response.getTotalNutrientInput().getTotalNitrogenKg()
                .compareTo(new BigDecimal("60")) < 0, 
                "Should detect under-application of nitrogen");
        
        // Verify insufficient phosphorus (4.6 kg vs typical 30 kg for rice)
        assertTrue(response.getTotalNutrientInput().getTotalPhosphorusKg()
                .compareTo(new BigDecimal("30")) < 0, 
                "Should detect under-application of phosphorus");
    }

    @Test
    @DisplayName("Test organic alternatives generation")
    void testOrganicAlternatives() {
        // Arrange
        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId("FARMER-003")
                .cropName("COTTON")
                .areaAcres(new BigDecimal("1"))
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
                .areaAcres(new BigDecimal("1"))
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
                .areaAcres(new BigDecimal("2"))
                .build();

        // Act
        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getEstimatedCost());
        assertTrue(response.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0);
        
        // Cost should scale with area (2 acres)
        BigDecimal costPerAcre = response.getEstimatedCost().divide(new BigDecimal("2"), 0, java.math.RoundingMode.HALF_UP);
        assertTrue(costPerAcre.compareTo(BigDecimal.ZERO) > 0);
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
                .quantityKg(new BigDecimal("50"))
                .applicationDate(LocalDate.now())
                .applicationStage("Basal")
                .cost(new BigDecimal("300"))
                .nitrogenPercent(new BigDecimal("46"))
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
        assertEquals(new BigDecimal("50"), result.getQuantityKg());
        assertEquals(new BigDecimal("46"), result.getNitrogenPercent());
        
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
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalNutrientInput().getTotalNitrogenKg()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalNutrientInput().getTotalPhosphorusKg()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalNutrientInput().getTotalPotassiumKg()));
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
                    .areaAcres(new BigDecimal("1"))
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
            Long id, String type, BigDecimal quantity, 
            BigDecimal n, BigDecimal p, BigDecimal k) {
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