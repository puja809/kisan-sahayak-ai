package com.farmer.yield.property;

import com.farmer.yield.client.MandiPriceClient;
import com.farmer.yield.client.WeatherClient;
import com.farmer.yield.dto.ActualYieldRequestDto;
import com.farmer.yield.dto.VarianceTrackingDto;
import com.farmer.yield.entity.YieldPrediction;
import com.farmer.yield.repository.YieldPredictionRepository;
import com.farmer.yield.service.YieldPredictionService;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for prediction variance calculation.
 * 
 * **Property 25: Prediction Variance Calculation**
 * - For any crop where both predicted yield and actual harvest data exist, the system 
 *   should calculate and store the variance (actual - predicted) for use in improving 
 *   future predictions.
 * 
 * **Validates: Requirement 11B.9**
 */
class PredictionVarianceCalculationPropertyTest {

    /**
     * Property 25: Prediction Variance Calculation
     * 
     * For any prediction with actual yield recorded:
     * 1. Variance should equal (actual - predicted)
     * 2. Variance percentage should be calculated correctly
     * 3. Variance category should be correctly determined
     * 
     * **Validates: Requirement 11B.9**
     */
    @Property
    void propertyVarianceCalculation(
            @ForAll @IntRange(min = 1, max = 1000) long cropId,
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @BigRange(min = "10", max = "100") BigDecimal predictedYield,
            @ForAll @BigRange(min = "5", max = "120") BigDecimal actualYield
    ) {
        // Arrange - Create mock services
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        // Create existing prediction
        YieldPrediction existingPrediction = YieldPrediction.builder()
                .id(1L)
                .cropId(cropId)
                .farmerId(farmerId)
                .predictedYieldExpectedQuintals(predictedYield)
                .predictedYieldMinQuintals(predictedYield.multiply(new BigDecimal("0.8")))
                .predictedYieldMaxQuintals(predictedYield.multiply(new BigDecimal("1.2")))
                .build();

        // Create actual yield request
        ActualYieldRequestDto request = ActualYieldRequestDto.builder()
                .cropId(cropId)
                .farmerId(farmerId)
                .actualYieldQuintals(actualYield)
                .harvestDate(LocalDate.now())
                .build();

        // Mock repository behavior
        when(repository.findFirstByCropIdOrderByPredictionDateDesc(cropId))
                .thenReturn(Optional.of(existingPrediction));
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.calculateAverageVarianceForCrop(anyString()))
                .thenReturn(new BigDecimal("10"));

        // Act
        VarianceTrackingDto response = service.recordActualYield(request);

        // Assert - Variance calculation correctness
        assertNotNull(response, "Variance tracking response should not be null");
        assertEquals(cropId, response.getCropId(), "Crop ID should match");
        assertEquals(farmerId, response.getFarmerId(), "Farmer ID should match");

        // Verify variance calculation: variance = actual - predicted
        BigDecimal expectedVariance = actualYield.subtract(predictedYield);
        assertEquals(0, expectedVariance.compareTo(response.getVarianceQuintals()),
                String.format("Variance should equal (actual - predicted): expected %s, got %s",
                        expectedVariance, response.getVarianceQuintals()));

        // Verify variance percentage calculation: variance% = (variance / predicted) * 100
        if (predictedYield.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal expectedVariancePercent = expectedVariance
                    .multiply(new BigDecimal("100"))
                    .divide(predictedYield, 2, RoundingMode.HALF_UP);
            assertEquals(0, expectedVariancePercent.compareTo(response.getVariancePercent()),
                    String.format("Variance percentage should be calculated correctly: expected %s, got %s",
                            expectedVariancePercent, response.getVariancePercent()));
        }

        // Verify variance category
        if (response.getVariancePercent() != null) {
            if (response.getVariancePercent().compareTo(new BigDecimal("-10")) > 0 &&
                response.getVariancePercent().compareTo(new BigDecimal("10")) < 0) {
                assertEquals("neutral", response.getVarianceCategory(),
                        "Variance within ±10% should be 'neutral'");
            } else if (response.getVariancePercent().compareTo(BigDecimal.ZERO) > 0) {
                assertEquals("positive", response.getVarianceCategory(),
                        "Variance > 10% should be 'positive' (actual > predicted)");
            } else {
                assertEquals("negative", response.getVarianceCategory(),
                        "Variance < -10% should be 'negative' (actual < predicted)");
            }
        }
    }

    /**
     * Property: Variance is Zero When Actual Equals Predicted
     * 
     * When actual yield exactly matches predicted yield, variance should be zero.
     */
    @Property
    void propertyZeroVarianceWhenActualEqualsPredicted(
            @ForAll @IntRange(min = 1, max = 1000) long cropId,
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @BigRange(min = "20", max = "80") BigDecimal predictedYield
    ) {
        // Arrange
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        YieldPrediction existingPrediction = YieldPrediction.builder()
                .id(1L)
                .cropId(cropId)
                .farmerId(farmerId)
                .predictedYieldExpectedQuintals(predictedYield)
                .build();

        ActualYieldRequestDto request = ActualYieldRequestDto.builder()
                .cropId(cropId)
                .farmerId(farmerId)
                .actualYieldQuintals(predictedYield)  // Same as predicted
                .harvestDate(LocalDate.now())
                .build();

        when(repository.findFirstByCropIdOrderByPredictionDateDesc(cropId))
                .thenReturn(Optional.of(existingPrediction));
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.calculateAverageVarianceForCrop(anyString()))
                .thenReturn(BigDecimal.ZERO);

        // Act
        VarianceTrackingDto response = service.recordActualYield(request);

        // Assert
        assertNotNull(response.getVarianceQuintals());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getVarianceQuintals()),
                "Variance should be zero when actual equals predicted");
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getVariancePercent()),
                "Variance percentage should be zero when actual equals predicted");
        assertEquals("neutral", response.getVarianceCategory(),
                "Zero variance should be 'neutral' category");
    }

    /**
     * Property: Positive Variance When Actual Exceeds Predicted
     * 
     * When actual yield is higher than predicted, variance should be positive.
     */
    @Property
    void propertyPositiveVarianceWhenActualExceedsPredicted(
            @ForAll @IntRange(min = 1, max = 1000) long cropId,
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @BigRange(min = "20", max = "60") BigDecimal predictedYield,
            @ForAll @BigRange(min = "1.1", max = "2.0") BigDecimal multiplier
    ) {
        // Arrange
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        YieldPrediction existingPrediction = YieldPrediction.builder()
                .id(1L)
                .cropId(cropId)
                .farmerId(farmerId)
                .predictedYieldExpectedQuintals(predictedYield)
                .build();

        BigDecimal actualYield = predictedYield.multiply(multiplier);

        ActualYieldRequestDto request = ActualYieldRequestDto.builder()
                .cropId(cropId)
                .farmerId(farmerId)
                .actualYieldQuintals(actualYield)
                .harvestDate(LocalDate.now())
                .build();

        when(repository.findFirstByCropIdOrderByPredictionDateDesc(cropId))
                .thenReturn(Optional.of(existingPrediction));
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.calculateAverageVarianceForCrop(anyString()))
                .thenReturn(new BigDecimal("15"));

        // Act
        VarianceTrackingDto response = service.recordActualYield(request);

        // Assert
        assertTrue(response.getVarianceQuintals().compareTo(BigDecimal.ZERO) > 0,
                "Variance should be positive when actual > predicted");
        assertTrue(response.getVariancePercent().compareTo(BigDecimal.ZERO) > 0,
                "Variance percentage should be positive when actual > predicted");
        assertEquals("positive", response.getVarianceCategory(),
                "Category should be 'positive' when actual > predicted");
    }

    /**
     * Property: Negative Variance When Actual Below Predicted
     * 
     * When actual yield is lower than predicted, variance should be negative.
     */
    @Property
    void propertyNegativeVarianceWhenActualBelowPredicted(
            @ForAll @IntRange(min = 1, max = 1000) long cropId,
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @BigRange(min = "30", max = "80") BigDecimal predictedYield,
            @ForAll @BigRange(min = "0.3", max = "0.9") BigDecimal multiplier
    ) {
        // Arrange
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        YieldPrediction existingPrediction = YieldPrediction.builder()
                .id(1L)
                .cropId(cropId)
                .farmerId(farmerId)
                .predictedYieldExpectedQuintals(predictedYield)
                .build();

        BigDecimal actualYield = predictedYield.multiply(multiplier);

        ActualYieldRequestDto request = ActualYieldRequestDto.builder()
                .cropId(cropId)
                .farmerId(farmerId)
                .actualYieldQuintals(actualYield)
                .harvestDate(LocalDate.now())
                .build();

        when(repository.findFirstByCropIdOrderByPredictionDateDesc(cropId))
                .thenReturn(Optional.of(existingPrediction));
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.calculateAverageVarianceForCrop(anyString()))
                .thenReturn(new BigDecimal("-10"));

        // Act
        VarianceTrackingDto response = service.recordActualYield(request);

        // Assert
        assertTrue(response.getVarianceQuintals().compareTo(BigDecimal.ZERO) < 0,
                "Variance should be negative when actual < predicted");
        assertTrue(response.getVariancePercent().compareTo(BigDecimal.ZERO) < 0,
                "Variance percentage should be negative when actual < predicted");
        assertEquals("negative", response.getVarianceCategory(),
                "Category should be 'negative' when actual < predicted");
    }

    /**
     * Property: Variance Percentage Symmetry
     * 
     * A +20% variance should have the same absolute percentage as a -20% variance.
     */
    @Property
    void propertyVariancePercentageSymmetry(
            @ForAll @IntRange(min = 1, max = 1000) long cropId,
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @BigRange(min = "30", max = "70") BigDecimal predictedYield,
            @ForAll @BigRange(min = "0.1", max = "0.3") BigDecimal deviationPercent
    ) {
        // Arrange
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        YieldPrediction existingPrediction = YieldPrediction.builder()
                .id(1L)
                .cropId(cropId)
                .farmerId(farmerId)
                .predictedYieldExpectedQuintals(predictedYield)
                .build();

        // Calculate actual yields with positive and negative deviation
        BigDecimal positiveActual = predictedYield.multiply(BigDecimal.ONE.add(deviationPercent));
        BigDecimal negativeActual = predictedYield.multiply(BigDecimal.ONE.subtract(deviationPercent));

        // Positive deviation case
        ActualYieldRequestDto positiveRequest = ActualYieldRequestDto.builder()
                .cropId(cropId)
                .farmerId(farmerId)
                .actualYieldQuintals(positiveActual)
                .harvestDate(LocalDate.now())
                .build();

        when(repository.findFirstByCropIdOrderByPredictionDateDesc(cropId))
                .thenReturn(Optional.of(existingPrediction));
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.calculateAverageVarianceForCrop(anyString()))
                .thenReturn(BigDecimal.ZERO);

        VarianceTrackingDto positiveResponse = service.recordActualYield(positiveRequest);

        // Negative deviation case
        ActualYieldRequestDto negativeRequest = ActualYieldRequestDto.builder()
                .cropId(cropId)
                .farmerId(farmerId)
                .actualYieldQuintals(negativeActual)
                .harvestDate(LocalDate.now())
                .build();

        VarianceTrackingDto negativeResponse = service.recordActualYield(negativeRequest);

        // Assert - Absolute percentages should be equal
        BigDecimal positiveAbsPercent = positiveResponse.getVariancePercent().abs();
        BigDecimal negativeAbsPercent = negativeResponse.getVariancePercent().abs();

        assertEquals(0, positiveAbsPercent.compareTo(negativeAbsPercent),
                String.format("Absolute variance percentages should be symmetric: +%s%% vs -%s%%",
                        positiveAbsPercent, negativeAbsPercent));
    }

    /**
     * Property: Variance Scales with Yield Difference
     * 
     * Larger differences between actual and predicted should result in proportionally larger variance.
     */
    @Property
    void propertyVarianceScalesWithDifference(
            @ForAll @IntRange(min = 1, max = 1000) long cropId,
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @BigRange(min = "40", max = "60") BigDecimal predictedYield,
            @ForAll @BigRange(min = "1", max = "50") BigDecimal difference1,
            @ForAll @BigRange(min = "51", max = "100") BigDecimal difference2
    ) {
        // Arrange
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        YieldPrediction existingPrediction = YieldPrediction.builder()
                .id(1L)
                .cropId(cropId)
                .farmerId(farmerId)
                .predictedYieldExpectedQuintals(predictedYield)
                .build();

        // Two cases with different differences
        BigDecimal actual1 = predictedYield.add(difference1);
        BigDecimal actual2 = predictedYield.add(difference2);

        when(repository.findFirstByCropIdOrderByPredictionDateDesc(cropId))
                .thenReturn(Optional.of(existingPrediction));
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.calculateAverageVarianceForCrop(anyString()))
                .thenReturn(BigDecimal.ZERO);

        // First case
        ActualYieldRequestDto request1 = ActualYieldRequestDto.builder()
                .cropId(cropId)
                .farmerId(farmerId)
                .actualYieldQuintals(actual1)
                .harvestDate(LocalDate.now())
                .build();
        VarianceTrackingDto response1 = service.recordActualYield(request1);

        // Second case
        ActualYieldRequestDto request2 = ActualYieldRequestDto.builder()
                .cropId(cropId)
                .farmerId(farmerId)
                .actualYieldQuintals(actual2)
                .harvestDate(LocalDate.now())
                .build();
        VarianceTrackingDto response2 = service.recordActualYield(request2);

        // Assert - Larger difference should have larger variance
        assertTrue(response2.getVarianceQuintals().compareTo(response1.getVarianceQuintals()) > 0,
                "Larger difference should result in larger variance");
        assertTrue(response2.getVariancePercent().compareTo(response1.getVariancePercent()) > 0,
                "Larger difference should result in larger variance percentage");
    }

    /**
     * Property: Zero Predicted Yield Edge Case
     * 
     * When predicted yield is zero, variance calculation should not throw exception.
     */
    @Property
    void propertyZeroPredictedYieldEdgeCase(
            @ForAll @IntRange(min = 1, max = 1000) long cropId,
            @ForAll @StringLength(min = 5, max = 20) String farmerId,
            @ForAll @BigRange(min = "5", max = "50") BigDecimal actualYield
    ) {
        // Arrange
        YieldPredictionRepository repository = mock(YieldPredictionRepository.class);
        WeatherClient weatherClient = mock(WeatherClient.class);
        MandiPriceClient mandiPriceClient = mock(MandiPriceClient.class);

        YieldPredictionService service = new YieldPredictionService(
                repository, weatherClient, mandiPriceClient);

        YieldPrediction existingPrediction = YieldPrediction.builder()
                .id(1L)
                .cropId(cropId)
                .farmerId(farmerId)
                .predictedYieldExpectedQuintals(BigDecimal.ZERO)  // Edge case: zero prediction
                .build();

        ActualYieldRequestDto request = ActualYieldRequestDto.builder()
                .cropId(cropId)
                .farmerId(farmerId)
                .actualYieldQuintals(actualYield)
                .harvestDate(LocalDate.now())
                .build();

        when(repository.findFirstByCropIdOrderByPredictionDateDesc(cropId))
                .thenReturn(Optional.of(existingPrediction));
        when(repository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.calculateAverageVarianceForCrop(anyString()))
                .thenReturn(BigDecimal.ZERO);

        // Act
        VarianceTrackingDto response = service.recordActualYield(request);

        // Assert - Should handle gracefully
        assertNotNull(response);
        assertEquals(actualYield, response.getVarianceQuintals(),
                "Variance should equal actual yield when predicted is zero");
    }
}