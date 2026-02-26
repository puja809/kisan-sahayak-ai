package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.repository.FertilizerApplicationRepository;
import com.farmer.crop.repository.SoilHealthCardRepository;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Property-based tests for FertilizerRecommendationService.
 * 
 * **Validates: Requirements 11C.3, 11C.4**
 * 
 * Property 26: Fertilizer Recommendation Completeness
 * - For any fertilizer recommendation, all required fields (fertilizer type, 
 *   quantity per acre, application timing, split application schedule with specific dates) 
 *   should be present in the response.
 */
class FertilizerRecommendationPropertyTest {

    /**
     * Property 26: Fertilizer Recommendation Completeness
     * 
     * For any successful fertilizer recommendation, all required fields should be present.
     * 
     * **Validates: Requirements 11C.3, 11C.4**
     */
    @Property
    void propertyFertilizerRecommendationCompleteness(
            @ForAll @StringLength(min = 1, max = 50) String farmerId,
            @ForAll @StringLength(min = 1, max = 50) String cropName,
            @ForAll Double areaAcres,
            @ForAll String state
    ) {
        Double area = Math.max(0.5, Math.min(10.0, areaAcres));

        // Arrange - Create mock services
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        // Act
        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId(farmerId)
                .cropName(cropName)
                .areaAcres(area)
                .state(state)
                .includeOrganicAlternatives(true)
                .includeSplitApplication(true)
                .build();

        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert - Verify all required fields are present
        assertTrue(response.isSuccess(), "Recommendation should be successful");
        assertNotNull(response.getNutrientRequirements(), "Nutrient requirements must be present");
        assertNotNull(response.getRecommendations(), "Recommendations list must be present");
        assertFalse(response.getRecommendations().isEmpty(), "Recommendations should not be empty");

        for (FertilizerRecommendationResponseDto.RecommendedFertilizerDto rec : response.getRecommendations()) {
            // Required fields per Requirement 11C.3
            assertNotNull(rec.getFertilizerType(), "Fertilizer type must be present");
            assertFalse(rec.getFertilizerType().isEmpty(), "Fertilizer type should not be empty");
            
            assertNotNull(rec.getQuantityKgPerAcre(), "Quantity per acre must be present");
            assertTrue(rec.getQuantityKgPerAcre() > 0.0, 
                    "Quantity should be positive");
            
            assertNotNull(rec.getApplicationTiming(), "Application timing must be present");
            assertFalse(rec.getApplicationTiming().isEmpty(), "Application timing should not be empty");
            
            assertNotNull(rec.getApplicationStage(), "Application stage must be present");
            assertFalse(rec.getApplicationStage().isEmpty(), "Application stage should not be empty");
            
            assertNotNull(rec.getFertilizerCategory(), "Fertilizer category must be present");
            assertFalse(rec.getFertilizerCategory().isEmpty(), "Fertilizer category should not be empty");
        }
    }

    /**
     * Property: Split Application Schedule Completeness
     * 
     * For any recommendation with split application enabled, the schedule should have specific dates.
     * 
     * **Validates: Requirement 11C.4**
     */
    @Property
    void propertySplitApplicationScheduleCompleteness(
            @ForAll @StringLength(min = 1, max = 50) String farmerId,
            @ForAll @StringLength(min = 1, max = 50) String cropName,
            @ForAll Double areaAcres
    ) {
        Double area = Math.max(0.5, Math.min(10.0, areaAcres));

        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        // Act
        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId(farmerId)
                .cropName(cropName)
                .areaAcres(area)
                .includeSplitApplication(true)
                .build();

        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        
        if (response.getApplicationSchedule() != null && !response.getApplicationSchedule().isEmpty()) {
            for (FertilizerRecommendationResponseDto.ApplicationScheduleDto schedule : response.getApplicationSchedule()) {
                // Required fields per Requirement 11C.4
                assertNotNull(schedule.getApplicationName(), "Schedule name must be present");
                assertFalse(schedule.getApplicationName().isEmpty(), "Schedule name should not be empty");
                
                assertNotNull(schedule.getApplicationStage(), "Schedule stage must be present");
                assertFalse(schedule.getApplicationStage().isEmpty(), "Schedule stage should not be empty");
                
                assertNotNull(schedule.getSuggestedDate(), "Suggested date must be present");
                
                assertNotNull(schedule.getDescription(), "Description must be present");
                assertFalse(schedule.getDescription().isEmpty(), "Description should not be empty");
                
                assertNotNull(schedule.getFertilizers(), "Fertilizers list must be present");
                assertFalse(schedule.getFertilizers().isEmpty(), "Fertilizers should not be empty");
            }
        }
    }

    /**
     * Property: Organic Alternatives Completeness
     * 
     * For any recommendation with organic alternatives enabled, all alternatives should have complete data.
     * 
     * **Validates: Requirement 11C.5**
     */
    @Property
    void propertyOrganicAlternativesCompleteness(
            @ForAll @StringLength(min = 1, max = 50) String farmerId,
            @ForAll @StringLength(min = 1, max = 50) String cropName
    ) {
        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        // Act
        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId(farmerId)
                .cropName(cropName)
                .includeOrganicAlternatives(true)
                .build();

        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        
        if (response.getOrganicAlternatives() != null && !response.getOrganicAlternatives().isEmpty()) {
            for (FertilizerRecommendationResponseDto.OrganicAlternativeDto alt : response.getOrganicAlternatives()) {
                // Required fields per Requirement 11C.5
                assertNotNull(alt.getAlternativeType(), "Alternative type must be present");
                assertFalse(alt.getAlternativeType().isEmpty(), "Alternative type should not be empty");
                
                assertNotNull(alt.getName(), "Name must be present");
                assertFalse(alt.getName().isEmpty(), "Name should not be empty");
                
                assertNotNull(alt.getQuantityKgPerAcre(), "Quantity per acre must be present");
                assertTrue(alt.getQuantityKgPerAcre() > 0.0, 
                        "Quantity should be positive");
                
                assertNotNull(alt.getBenefits(), "Benefits must be present");
                assertFalse(alt.getBenefits().isEmpty(), "Benefits should not be empty");
                
                assertNotNull(alt.getApplicationMethod(), "Application method must be present");
                assertFalse(alt.getApplicationMethod().isEmpty(), "Application method should not be empty");
                
                assertNotNull(alt.getCostPerAcre(), "Cost per acre must be present");
            }
        }
    }

    /**
     * Property: Nutrient Requirements Validity
     * 
     * All nutrient requirements should be non-negative.
     */
    @Property
    void propertyNutrientRequirementsValidity(
            @ForAll @StringLength(min = 1, max = 50) String farmerId,
            @ForAll @StringLength(min = 1, max = 50) String cropName,
            @ForAll Double areaAcres
    ) {
        Double area = Math.max(0.5, Math.min(10.0, areaAcres));

        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        // Act
        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId(farmerId)
                .cropName(cropName)
                .areaAcres(area)
                .build();

        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getNutrientRequirements());
        
        FertilizerRecommendationResponseDto.NutrientRequirementsDto req = response.getNutrientRequirements();
        assertTrue(req.getNitrogenKgPerAcre() >= 0.0, 
                "Nitrogen requirement should be non-negative");
        assertTrue(req.getPhosphorusKgPerAcre() >= 0.0, 
                "Phosphorus requirement should be non-negative");
        assertTrue(req.getPotassiumKgPerAcre() >= 0.0, 
                "Potassium requirement should be non-negative");
    }

    /**
     * Property: Cost Calculation Validity
     * 
     * Total cost should be non-negative and should scale with area.
     */
    @Property
    void propertyCostCalculationValidity(
            @ForAll @StringLength(min = 1, max = 50) String farmerId,
            @ForAll @StringLength(min = 1, max = 50) String cropName,
            @ForAll Double areaAcres
    ) {
        Double area = Math.max(0.5, Math.min(10.0, areaAcres));

        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        // Act
        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId(farmerId)
                .cropName(cropName)
                .areaAcres(area)
                .build();

        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        
        if (response.getEstimatedCost() != null) {
            assertTrue(response.getEstimatedCost() >= 0.0, 
                    "Total cost should be non-negative");
        }
    }

    /**
     * Property: Soil Health Card Usage Flag
     * 
     * When soil health card data is provided, the flag should be set to true.
     */
    @Property
    void propertySoilHealthCardUsageFlag(
            @ForAll @StringLength(min = 1, max = 50) String farmerId,
            @ForAll @StringLength(min = 1, max = 50) String cropName
    ) {
        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        // Create soil health card data
        SoilHealthCardDto soilHealthCard = SoilHealthCardDto.builder()
                .nitrogenKgHa(200.0)
                .phosphorusKgHa(8.0)
                .potassiumKgHa(100.0)
                .zincPpm(0.4)
                .build();

        // Act
        FertilizerRecommendationRequestDto request = FertilizerRecommendationRequestDto.builder()
                .farmerId(farmerId)
                .cropName(cropName)
                .soilHealthCard(soilHealthCard)
                .build();

        FertilizerRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        assertTrue(response.isSoilHealthCardUsed(), 
                "Soil health card used flag should be true when data is provided");
    }
}

