package com.farmer.crop.service;

import com.farmer.crop.dto.FertilizerApplicationRequestDto;
import com.farmer.crop.dto.FertilizerTrackingResponseDto;
import com.farmer.crop.entity.FertilizerApplication;
import com.farmer.crop.repository.FertilizerApplicationRepository;
import com.farmer.crop.repository.SoilHealthCardRepository;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
            @ForAll @BigRange(min = "1", max = "100") BigDecimal quantityKg,
            @ForAll @BigRange(min = "0", max = "46") BigDecimal nitrogenPercent,
            @ForAll @BigRange(min = "0", max = "46") BigDecimal phosphorusPercent,
            @ForAll @BigRange(min = "0", max = "60") BigDecimal potassiumPercent,
            @ForAll String farmerId
    ) {
        // Arrange - Create mock services
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        // Create mock applications with known values
        List<FertilizerApplication> mockApplications = new ArrayList<>();
        BigDecimal expectedTotalN = BigDecimal.ZERO;
        BigDecimal expectedTotalP = BigDecimal.ZERO;
        BigDecimal expectedTotalK = BigDecimal.ZERO;

        for (int i = 0; i < numApplications; i++) {
            BigDecimal n = nitrogenPercent.add(new BigDecimal(i)).min(new BigDecimal("46"));
            BigDecimal p = phosphorusPercent.add(new BigDecimal(i)).min(new BigDecimal("46"));
            BigDecimal k = potassiumPercent.add(new BigDecimal(i)).min(new BigDecimal("60"));
            
            BigDecimal nKg = quantityKg.multiply(n).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal pKg = quantityKg.multiply(p).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal kKg = quantityKg.multiply(k).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            
            expectedTotalN = expectedTotalN.add(nKg);
            expectedTotalP = expectedTotalP.add(pKg);
            expectedTotalK = expectedTotalK.add(kKg);
            
            FertilizerApplication app = FertilizerApplication.builder()
                    .id((long) (i + 1))
                    .cropId(1L)
                    .farmerId(farmerId)
                    .fertilizerType("Test Fertilizer " + i)
                    .fertilizerCategory(FertilizerApplication.FertilizerCategory.CHEMICAL)
                    .quantityKg(quantityKg)
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
        BigDecimal calculatedN = totalInput.getTotalNitrogenKg();
        assertNotNull(calculatedN, "Total nitrogen must be present");
        assertEquals(0, expectedTotalN.compareTo(calculatedN),
                String.format("Nitrogen calculation mismatch: expected %s, got %s", expectedTotalN, calculatedN));

        // Verify phosphorus calculation
        BigDecimal calculatedP = totalInput.getTotalPhosphorusKg();
        assertNotNull(calculatedP, "Total phosphorus must be present");
        assertEquals(0, expectedTotalP.compareTo(calculatedP),
                String.format("Phosphorus calculation mismatch: expected %s, got %s", expectedTotalP, calculatedP));

        // Verify potassium calculation
        BigDecimal calculatedK = totalInput.getTotalPotassiumKg();
        assertNotNull(calculatedK, "Total potassium must be present");
        assertEquals(0, expectedTotalK.compareTo(calculatedK),
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
            @ForAll @BigRange(min = "1", max = "100") BigDecimal quantityKg,
            @ForAll String farmerId
    ) {
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
                    .quantityKg(quantityKg)
                    .applicationDate(LocalDate.now().minusDays(i * 10))
                    .nitrogenPercent(BigDecimal.ZERO)
                    .phosphorusPercent(BigDecimal.ZERO)
                    .potassiumPercent(BigDecimal.ZERO)
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
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalNutrientInput().getTotalNitrogenKg()),
                "Total nitrogen should be zero when all applications have 0% N");
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalNutrientInput().getTotalPhosphorusKg()),
                "Total phosphorus should be zero when all applications have 0% P");
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalNutrientInput().getTotalPotassiumKg()),
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
            @ForAll @BigRange(min = "1", max = "100") BigDecimal quantityKg,
            @ForAll String farmerId
    ) {
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
                    .quantityKg(quantityKg)
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
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalNutrientInput().getTotalNitrogenKg()),
                "Total nitrogen should be zero when percentages are null");
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalNutrientInput().getTotalPhosphorusKg()),
                "Total phosphorus should be zero when percentages are null");
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalNutrientInput().getTotalPotassiumKg()),
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
            @ForAll @BigRange(min = "1", max = "50") BigDecimal quantityKg,
            @ForAll String farmerId
    ) {
        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        List<FertilizerApplication> mockApplications = new ArrayList<>();
        BigDecimal expectedTotalQty = quantityKg.multiply(new BigDecimal(numApplications));

        for (int i = 0; i < numApplications; i++) {
            FertilizerApplication app = FertilizerApplication.builder()
                    .id((long) (i + 1))
                    .cropId(1L)
                    .farmerId(farmerId)
                    .fertilizerType("Test Fertilizer " + i)
                    .fertilizerCategory(FertilizerApplication.FertilizerCategory.CHEMICAL)
                    .quantityKg(quantityKg)
                    .applicationDate(LocalDate.now().minusDays(i * 10))
                    .nitrogenPercent(new BigDecimal("10"))
                    .phosphorusPercent(new BigDecimal("10"))
                    .potassiumPercent(new BigDecimal("10"))
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

        BigDecimal calculatedTotalQty = response.getTotalNutrientInput().getTotalQuantityKg();
        assertNotNull(calculatedTotalQty, "Total quantity must be present");
        assertEquals(0, expectedTotalQty.compareTo(calculatedTotalQty),
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
                    .quantityKg(new BigDecimal("10"))
                    .applicationDate(appDate)
                    .nitrogenPercent(new BigDecimal("10"))
                    .phosphorusPercent(new BigDecimal("10"))
                    .potassiumPercent(new BigDecimal("10"))
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
            @ForAll @BigRange(min = "1", max = "100") BigDecimal quantityKg,
            @ForAll @BigRange(min = "5", max = "50") BigDecimal costPerKg,
            @ForAll String farmerId
    ) {
        // Arrange
        SoilHealthCardRepository soilHealthCardRepository = mock(SoilHealthCardRepository.class);
        FertilizerApplicationRepository fertilizerApplicationRepository = mock(FertilizerApplicationRepository.class);

        FertilizerRecommendationService service = new FertilizerRecommendationService(
                soilHealthCardRepository, fertilizerApplicationRepository);

        List<FertilizerApplication> mockApplications = new ArrayList<>();
        BigDecimal expectedTotalCost = BigDecimal.ZERO;

        for (int i = 0; i < numApplications; i++) {
            BigDecimal appCost = quantityKg.multiply(costPerKg);
            expectedTotalCost = expectedTotalCost.add(appCost);
            
            FertilizerApplication app = FertilizerApplication.builder()
                    .id((long) (i + 1))
                    .cropId(1L)
                    .farmerId(farmerId)
                    .fertilizerType("Test Fertilizer " + i)
                    .fertilizerCategory(FertilizerApplication.FertilizerCategory.CHEMICAL)
                    .quantityKg(quantityKg)
                    .applicationDate(LocalDate.now().minusDays(i * 10))
                    .cost(appCost)
                    .nitrogenPercent(new BigDecimal("10"))
                    .phosphorusPercent(new BigDecimal("10"))
                    .potassiumPercent(new BigDecimal("10"))
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

        BigDecimal calculatedTotalCost = response.getTotalNutrientInput().getTotalCost();
        assertNotNull(calculatedTotalCost, "Total cost must be present");
        assertEquals(0, expectedTotalCost.compareTo(calculatedTotalCost),
                String.format("Total cost mismatch: expected %s, got %s", expectedTotalCost, calculatedTotalCost));
    }
}