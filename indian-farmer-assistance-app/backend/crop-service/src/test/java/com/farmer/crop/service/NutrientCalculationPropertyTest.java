package com.farmer.crop.service;

import com.farmer.crop.dto.FertilizerTrackingResponseDto;
import com.farmer.crop.entity.FertilizerApplication;
import com.farmer.crop.repository.FertilizerApplicationRepository;
import com.farmer.crop.repository.SoilHealthCardRepository;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for nutrient calculation accuracy.
 * 
 * **Validates: Requirement 11C.7**
 * 
 * Property 27: Nutrient Calculation Accuracy
 * - For any set of fertilizer applications for a crop cycle, the total nutrient input 
 *   (N, P, K) should equal the sum of (quantity_kg × nutrient_content_percent / 100) 
 *   across all applications for each nutrient type.
 */
class NutrientCalculationPropertyTest {

    /**
     * Property 27: Nutrient Calculation Accuracy
     * 
     * For any set of fertilizer applications, the total nutrient input should equal
     * the sum of (quantity_kg × nutrient_content_percent / 100) for each nutrient.
     * 
     * **Validates: Requirement 11C.7**
     */
    @Property
    void propertyNutrientCalculationAccuracy(
            @ForAll @IntRange(min = 1, max = 5) int numApplications,
            @ForAll Double quantityKg,
            @ForAll Double nitrogenPercent,
            @ForAll Double phosphorusPercent,
            @ForAll Double potassiumPercent,
            @ForAll String farmerId
    ) {
        // Clamp percentages to valid range
        Double nP = Math.max(0.0, Math.min(46.0, nitrogenPercent));
        Double pP = Math.max(0.0, Math.min(46.0, phosphorusPercent));
        Double kP = Math.max(0.0, Math.min(60.0, potassiumPercent));
        Double qKg = Math.max(1.0, Math.min(100.0, quantityKg));

        // Arrange - Create mock services
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        // Create mock applications with known values
        List<FertilizerApplication> mockApplications = new ArrayList<>();
        Double expectedTotalN = 0.0;
        Double expectedTotalP = 0.0;
        Double expectedTotalK = 0.0;

        for (int i = 0; i < numApplications; i++) {
            Double n = Math.min(46.0, nP + i);
            Double p = Math.min(46.0, pP + i);
            Double k = Math.min(60.0, kP + i);
            
            expectedTotalN += (qKg * n) / 100.0;
            expectedTotalP += (qKg * p) / 100.0;
            expectedTotalK += (qKg * k) / 100.0;
            
            FertilizerApplication app = FertilizerApplication.builder()
                    .id((long) (i + 1))
                    .cropId(1L)
                    .farmerId(farmerId)
                    .fertilizerType("Test Fertilizer " + i)
                    .fertilizerCategory(FertilizerApplication.FertilizerCategory.CHEMICAL)
                    .quantityKg(qKg)
                    .applicationDate(LocalDate.now().minusDays(i * 10))
                    .nitrogenPercent(n)
                    .phosphorusPercent(p)
                    .potassiumPercent(k)
                    .build();
            mockApplications.add(app);
        }

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(mockApplications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert - Verify nutrient calculation accuracy
        assertTrue(response.isSuccess(), "Tracking should be successful");
        assertNotNull(response.getTotalNutrientInput(), "Total nutrient input must be present");

        FertilizerTrackingResponseDto.TotalNutrientInputDto totalInput = response.getTotalNutrientInput();

        // Verify nitrogen calculation
        Double calculatedN = totalInput.getTotalNitrogenKg();
        assertNotNull(calculatedN, "Total nitrogen must be present");
        assertEquals(expectedTotalN, calculatedN, 0.01,
                String.format("Nitrogen calculation mismatch: expected %s, got %s", expectedTotalN, calculatedN));

        // Verify phosphorus calculation
        Double calculatedP = totalInput.getTotalPhosphorusKg();
        assertNotNull(calculatedP, "Total phosphorus must be present");
        assertEquals(expectedTotalP, calculatedP, 0.01,
                String.format("Phosphorus calculation mismatch: expected %s, got %s", expectedTotalP, calculatedP));

        // Verify potassium calculation
        Double calculatedK = totalInput.getTotalPotassiumKg();
        assertNotNull(calculatedK, "Total potassium must be present");
        assertEquals(expectedTotalK, calculatedK, 0.01,
                String.format("Potassium calculation mismatch: expected %s, got %s", expectedTotalK, calculatedK));
    }

    /**
     * Property: Nutrient Calculation with Zero Values
     * 
     * When nutrient percentages are zero, total should be zero.
     */
    @Property
    void propertyNutrientCalculationWithZeroValues(
            @ForAll @IntRange(min = 1, max = 5) int numApplications,
            @ForAll Double quantityKg,
            @ForAll String farmerId
    ) {
        Double qKg = Math.max(1.0, Math.min(100.0, quantityKg));

        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        List<FertilizerApplication> mockApplications = new ArrayList<>();

        for (int i = 0; i < numApplications; i++) {
            FertilizerApplication app = FertilizerApplication.builder()
                    .id((long) (i + 1))
                    .cropId(1L)
                    .farmerId(farmerId)
                    .fertilizerType("Zero Nutrient Fertilizer")
                    .fertilizerCategory(FertilizerApplication.FertilizerCategory.ORGANIC)
                    .quantityKg(qKg)
                    .applicationDate(LocalDate.now().minusDays(i * 10))
                    .nitrogenPercent(0.0)
                    .phosphorusPercent(0.0)
                    .potassiumPercent(0.0)
                    .build();
            mockApplications.add(app);
        }

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(mockApplications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getTotalNutrientInput());

        // All totals should be zero
        assertEquals(0.0, response.getTotalNutrientInput().getTotalNitrogenKg(),
                "Total nitrogen should be zero when all applications have 0% N");
        assertEquals(0.0, response.getTotalNutrientInput().getTotalPhosphorusKg(),
                "Total phosphorus should be zero when all applications have 0% P");
        assertEquals(0.0, response.getTotalNutrientInput().getTotalPotassiumKg(),
                "Total potassium should be zero when all applications have 0% K");
    }

    /**
     * Property: Nutrient Calculation with Null Percentages
     * 
     * When nutrient percentages are null, total should be zero (not throw exception).
     */
    @Property
    void propertyNutrientCalculationWithNullPercentages(
            @ForAll @IntRange(min = 1, max = 5) int numApplications,
            @ForAll Double quantityKg,
            @ForAll String farmerId
    ) {
        Double qKg = Math.max(1.0, Math.min(100.0, quantityKg));

        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        List<FertilizerApplication> mockApplications = new ArrayList<>();

        for (int i = 0; i < numApplications; i++) {
            FertilizerApplication app = FertilizerApplication.builder()
                    .id((long) (i + 1))
                    .cropId(1L)
                    .farmerId(farmerId)
                    .fertilizerType("Unknown Nutrient Fertilizer")
                    .fertilizerCategory(FertilizerApplication.FertilizerCategory.BIO)
                    .quantityKg(qKg)
                    .applicationDate(LocalDate.now().minusDays(i * 10))
                    .nitrogenPercent(null)
                    .phosphorusPercent(null)
                    .potassiumPercent(null)
                    .build();
            mockApplications.add(app);
        }

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(mockApplications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert - Should not throw exception and should return zeros
        assertTrue(response.isSuccess());
        assertNotNull(response.getTotalNutrientInput());
        assertEquals(0.0, response.getTotalNutrientInput().getTotalNitrogenKg(),
                "Total nitrogen should be zero when percentages are null");
        assertEquals(0.0, response.getTotalNutrientInput().getTotalPhosphorusKg(),
                "Total phosphorus should be zero when percentages are null");
        assertEquals(0.0, response.getTotalNutrientInput().getTotalPotassiumKg(),
                "Total potassium should be zero when percentages are null");
    }

    /**
     * Property: Total Quantity Calculation
     * 
     * Total quantity should equal sum of all application quantities.
     */
    @Property
    void propertyTotalQuantityCalculation(
            @ForAll @IntRange(min = 1, max = 5) int numApplications,
            @ForAll Double quantityKg,
            @ForAll String farmerId
    ) {
        Double qKg = Math.max(1.0, Math.min(50.0, quantityKg));

        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        List<FertilizerApplication> mockApplications = new ArrayList<>();
        Double expectedTotalQty = qKg * numApplications;

        for (int i = 0; i < numApplications; i++) {
            FertilizerApplication app = FertilizerApplication.builder()
                    .id((long) (i + 1))
                    .cropId(1L)
                    .farmerId(farmerId)
                    .fertilizerType("Test Fertilizer " + i)
                    .fertilizerCategory(FertilizerApplication.FertilizerCategory.CHEMICAL)
                    .quantityKg(qKg)
                    .applicationDate(LocalDate.now().minusDays(i * 10))
                    .nitrogenPercent(10.0)
                    .phosphorusPercent(10.0)
                    .potassiumPercent(10.0)
                    .build();
            mockApplications.add(app);
        }

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(mockApplications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getTotalNutrientInput());

        Double calculatedTotalQty = response.getTotalNutrientInput().getTotalQuantityKg();
        assertNotNull(calculatedTotalQty, "Total quantity must be present");
        assertEquals(expectedTotalQty, calculatedTotalQty, 0.01,
                String.format("Total quantity mismatch: expected %s, got %s", expectedTotalQty, calculatedTotalQty));
    }

    /**
     * Property: Application History Order
     * 
     * Applications should be returned in ascending order by application date.
     */
    @Property
    void propertyApplicationHistoryOrder(
            @ForAll @IntRange(min = 2, max = 5) int numApplications,
            @ForAll String farmerId
    ) {
        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        List<FertilizerApplication> mockApplications = new ArrayList<>();

        for (int i = 0; i < numApplications; i++) {
            // Create applications with decreasing dates (older first)
            LocalDate appDate = LocalDate.now().minusDays((numApplications - i) * 10L);
            
            FertilizerApplication app = FertilizerApplication.builder()
                    .id((long) (i + 1))
                    .cropId(1L)
                    .farmerId(farmerId)
                    .fertilizerType("Test Fertilizer " + i)
                    .fertilizerCategory(FertilizerApplication.FertilizerCategory.CHEMICAL)
                    .quantityKg(10.0)
                    .applicationDate(appDate)
                    .nitrogenPercent(10.0)
                    .phosphorusPercent(10.0)
                    .potassiumPercent(10.0)
                    .build();
            mockApplications.add(app);
        }

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(mockApplications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getApplications());
        assertEquals(numApplications, response.getApplications().size());

        // Verify ascending order by date
        List<FertilizerTrackingResponseDto.FertilizerApplicationDto> apps = response.getApplications();
        for (int i = 0; i < apps.size() - 1; i++) {
            assertTrue(apps.get(i).getApplicationDate().isBefore(apps.get(i + 1).getApplicationDate()) ||
                       apps.get(i).getApplicationDate().isEqual(apps.get(i + 1).getApplicationDate()),
                    "Applications should be in ascending order by date");
        }
    }

    /**
     * Property: Cost Calculation
     * 
     * Total cost should be sum of individual application costs.
     */
    @Property
    void propertyCostCalculation(
            @ForAll @IntRange(min = 1, max = 5) int numApplications,
            @ForAll Double quantityKg,
            @ForAll Double costPerKg,
            @ForAll String farmerId
    ) {
        Double qKg = Math.max(1.0, Math.min(100.0, quantityKg));
        Double cKg = Math.max(5.0, Math.min(50.0, costPerKg));

        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        List<FertilizerApplication> mockApplications = new ArrayList<>();
        Double expectedTotalCost = 0.0;

        for (int i = 0; i < numApplications; i++) {
            Double appCost = qKg * cKg;
            expectedTotalCost += appCost;
            
            FertilizerApplication app = FertilizerApplication.builder()
                    .id((long) (i + 1))
                    .cropId(1L)
                    .farmerId(farmerId)
                    .fertilizerType("Test Fertilizer " + i)
                    .fertilizerCategory(FertilizerApplication.FertilizerCategory.CHEMICAL)
                    .quantityKg(qKg)
                    .applicationDate(LocalDate.now().minusDays(i * 10))
                    .cost(appCost)
                    .nitrogenPercent(10.0)
                    .phosphorusPercent(10.0)
                    .potassiumPercent(10.0)
                    .build();
            mockApplications.add(app);
        }

        when(fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(1L))
                .thenReturn(mockApplications);

        // Act
        FertilizerTrackingResponseDto response = service.getFertilizerTracking(1L);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getTotalNutrientInput());

        Double calculatedTotalCost = response.getTotalNutrientInput().getTotalCost();
        assertNotNull(calculatedTotalCost, "Total cost must be present");
        assertEquals(expectedTotalCost, calculatedTotalCost, 0.01,
                String.format("Total cost mismatch: expected %s, got %s", expectedTotalCost, calculatedTotalCost));
    }
}

