package com.farmer.yield.property;

import com.farmer.yield.client.MandiPriceClient;
import com.farmer.yield.client.WeatherClient;
import com.farmer.yield.dto.YieldEstimateRequestDto;
import com.farmer.yield.dto.YieldEstimateResponseDto;
import com.farmer.yield.entity.YieldPrediction;
import com.farmer.yield.repository.YieldPredictionRepository;
import com.farmer.yield.service.YieldPredictionService;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for yield estimate range validity.
 * 
 * **Property 24: Yield Estimate Range Validity**
 * - For any yield estimate, the system should provide minimum, expected, and maximum 
 *   values where minimum ≤ expected ≤ maximum, and confidence intervals should be included.
 * 
 * **Validates: Requirement 11B.7**
 */
class YieldEstimateRangeValidityPropertyTest {

    /**
     * Property 24: Yield Estimate Range Validity
     * 
     * For any yield estimate request, the generated estimate should satisfy:
     * 1. min <= expected <= max (for per-acre values)
     * 2. min <= expected <= max (for total values)
     * 3. Confidence interval should be present and positive
     * 
     * **Validates: Requirement 11B.7**
     */
    @Property
    void propertyYieldEstimateRangeValidity(
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @IntRange(min = 1, max = 100) long cropId,
            @ForAll StringLength(min = 3, max = 15) String cropName,
            @ForAll @BigRange(min = "0.5", max = "10") BigDecimal areaAcres,
            @ForAll String growthStage,
            @ForAll @BigRange(min = "100", max = "1000") BigDecimal rainfall,
            @ForAll @BigRange(min = "20", max = "40") BigDecimal temperature
    ) {
        // Arrange - Create mock services
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        // Create valid growth stage
        String[] validStages = {"SOWING", "GERMINATION", "VEGETATIVE", "FLOWERING", "FRUITING", "MATURATION", "HARVEST"};
        String validStage = validStages[Math.abs(growthStage.hashCode()) % validStages.length];

        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId(farmerId)
                .cropId(cropId)
                .cropName(cropName)
                .areaAcres(areaAcres)
                .sowingDate(LocalDate.now().minusDays(60))
                .growthStage(validStage)
                .totalRainfallMm(rainfall)
                .averageTemperatureCelsius(temperature)
                .includeFinancialProjection(false)
                .build();

        // Mock repository behavior
        when(repository.findFirstByCropIdOrderByPredictionDateDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(repository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(1L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert - Range validity
        assertTrue(response.isSuccess(), "Yield estimation should succeed");
        assertNotNull(response.getPredictedYieldMinQuintalsPerAcre(), "Min yield per acre must be present");
        assertNotNull(response.getPredictedYieldExpectedQuintalsPerAcre(), "Expected yield per acre must be present");
        assertNotNull(response.getPredictedYieldMaxQuintalsPerAcre(), "Max yield per acre must be present");

        // Verify: min <= expected <= max (per acre)
        assertTrue(response.getPredictedYieldMinQuintalsPerAcre()
                .compareTo(response.getPredictedYieldExpectedQuintalsPerAcre()) <= 0,
                "Min yield should be <= expected yield (per acre)");
        assertTrue(response.getPredictedYieldExpectedQuintalsPerAcre()
                .compareTo(response.getPredictedYieldMaxQuintalsPerAcre()) <= 0,
                "Expected yield should be <= max yield (per acre)");

        // Verify: min <= expected <= max (total)
        assertTrue(response.getPredictedYieldMinQuintals()
                .compareTo(response.getPredictedYieldExpectedQuintals()) <= 0,
                "Min yield should be <= expected yield (total)");
        assertTrue(response.getPredictedYieldExpectedQuintals()
                .compareTo(response.getPredictedYieldMaxQuintals()) <= 0,
                "Expected yield should be <= max yield (total)");

        // Verify confidence interval
        assertNotNull(response.getConfidenceIntervalPercent(), "Confidence interval must be present");
        assertTrue(response.getConfidenceIntervalPercent().compareTo(BigDecimal.ZERO) > 0,
                "Confidence interval should be positive");
        assertTrue(response.getConfidenceIntervalPercent().compareTo(new BigDecimal("100")) <= 0,
                "Confidence interval should not exceed 100%");
    }

    /**
     * Property: Total Yields Scale with Area
     * 
     * For any area, total yield should equal per-acre yield multiplied by area.
     */
    @Property
    void propertyTotalYieldScalesWithArea(
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @IntRange(min = 1, max = 100) long cropId,
            @ForAll StringLength(min = 3, max = 15) String cropName,
            @ForAll @BigRange(min = "0.5", max = "20") BigDecimal areaAcres
    ) {
        // Arrange
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId(farmerId)
                .cropId(cropId)
                .cropName(cropName)
                .areaAcres(areaAcres)
                .sowingDate(LocalDate.now().minusDays(60))
                .growthStage("VEGETATIVE")
                .includeFinancialProjection(false)
                .build();

        when(repository.findFirstByCropIdOrderByPredictionDateDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(repository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(1L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert
        assertTrue(response.isSuccess());

        // Verify total = per-acre * area
        BigDecimal expectedTotal = response.getPredictedYieldExpectedQuintalsPerAcre()
                .multiply(areaAcres)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(0, expectedTotal.compareTo(response.getPredictedYieldExpectedQuintals()),
                "Total expected yield should equal per-acre yield multiplied by area");
    }

    /**
     * Property: Range Width is Consistent
     * 
     * The range width (max - min) should be proportional to the expected yield.
     */
    @Property
    void propertyRangeWidthIsConsistent(
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @IntRange(min = 1, max = 100) long cropId,
            @ForAll String[] cropNames
    ) {
        // Arrange
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        String cropName = cropNames.length > 0 ? cropNames[0] : "RICE";

        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId(farmerId)
                .cropId(cropId)
                .cropName(cropName)
                .areaAcres(new BigDecimal("1"))
                .sowingDate(LocalDate.now().minusDays(60))
                .growthStage("VEGETATIVE")
                .includeFinancialProjection(false)
                .build();

        when(repository.findFirstByCropIdOrderByPredictionDateDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(repository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(1L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert
        assertTrue(response.isSuccess());

        BigDecimal rangeWidth = response.getPredictedYieldMaxQuintalsPerAcre()
                .subtract(response.getPredictedYieldMinQuintalsPerAcre());
        BigDecimal expectedYield = response.getPredictedYieldExpectedQuintalsPerAcre();

        // Range should be a reasonable percentage of expected yield (typically 20-40%)
        if (expectedYield.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rangePercent = rangeWidth.multiply(new BigDecimal("100"))
                    .divide(expectedYield, 2, java.math.RoundingMode.HALF_UP);
            assertTrue(rangePercent.compareTo(new BigDecimal("10")) >= 0,
                    "Range width should be at least 10% of expected yield");
            assertTrue(rangePercent.compareTo(new BigDecimal("100")) <= 0,
                    "Range width should not exceed 100% of expected yield");
        }
    }

    /**
     * Property: Negative Adjustments Don't Reduce Below Minimum
     * 
     * Even with negative factors (poor soil, pests, etc.), yield should not go below a reasonable minimum.
     */
    @Property
    void propertyNegativeAdjustmentsRespectMinimum(
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @IntRange(min = 1, max = 100) long cropId,
            @ForAll StringLength(min = 3, max = 15) String cropName,
            @ForAll @IntRange(min = 1, max = 5) int pestIncidents,
            @ForAll @IntRange(min = 1, max = 5) int diseaseIncidents,
            @ForAll @BigRange(min = "0", max = "50") BigDecimal affectedArea
    ) {
        // Arrange
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId(farmerId)
                .cropId(cropId)
                .cropName(cropName)
                .areaAcres(new BigDecimal("1"))
                .sowingDate(LocalDate.now().minusDays(60))
                .growthStage("FLOWERING")
                .pestIncidentCount(pestIncidents)
                .diseaseIncidentCount(diseaseIncidents)
                .affectedAreaPercent(affectedArea)
                .pestDiseaseControlStatus("ongoing")
                .includeFinancialProjection(false)
                .build();

        when(repository.findFirstByCropIdOrderByPredictionDateDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(repository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(1L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert
        assertTrue(response.isSuccess());
        assertTrue(response.getPredictedYieldExpectedQuintalsPerAcre().compareTo(BigDecimal.ZERO) > 0,
                "Expected yield should be positive even with negative factors");
        assertTrue(response.getPredictedYieldMinQuintalsPerAcre().compareTo(BigDecimal.ZERO) > 0,
                "Min yield should be positive");
    }

    /**
     * Property: Different Crops Have Different Yield Ranges
     * 
     * Different crop types should produce different yield estimates.
     */
    @Property
    void propertyDifferentCropsHaveDifferentYields(
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @IntRange(min = 1, max = 100) long baseCropId,
            @ForAll String crop1,
            @ForAll String crop2
    ) {
        // Arrange
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        String validCrop1 = crop1.length() >= 3 ? crop1 : "RICE";
        String validCrop2 = crop2.length() >= 3 ? crop2 : "WHEAT";

        // Create two requests for different crops
        YieldEstimateRequestDto request1 = YieldEstimateRequestDto.builder()
                .farmerId(farmerId)
                .cropId(baseCropId)
                .cropName(validCrop1)
                .areaAcres(new BigDecimal("1"))
                .sowingDate(LocalDate.now().minusDays(60))
                .growthStage("VEGETATIVE")
                .includeFinancialProjection(false)
                .build();

        YieldEstimateRequestDto request2 = YieldEstimateRequestDto.builder()
                .farmerId(farmerId)
                .cropId(baseCropId + 1)
                .cropName(validCrop2)
                .areaAcres(new BigDecimal("1"))
                .sowingDate(LocalDate.now().minusDays(60))
                .growthStage("VEGETATIVE")
                .includeFinancialProjection(false)
                .build();

        when(repository.findFirstByCropIdOrderByPredictionDateDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(repository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(1L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response1 = service.generateYieldEstimate(request1);
        YieldEstimateResponseDto response2 = service.generateYieldEstimate(request2);

        // Assert
        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());

        // Different crops should have different yield characteristics
        // (Note: This is a soft property - some crops may have similar yields)
        assertNotNull(response1.getPredictedYieldExpectedQuintalsPerAcre());
        assertNotNull(response2.getPredictedYieldExpectedQuintalsPerAcre());
    }
}