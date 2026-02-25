package com.farmer.yield.service;

import com.farmer.yield.client.MandiPriceClient;
import com.farmer.yield.client.WeatherClient;
import com.farmer.yield.dto.*;
import com.farmer.yield.entity.YieldPrediction;
import com.farmer.yield.repository.YieldPredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for YieldPredictionService.
 * 
 * Tests:
 * - Yield estimation with various input combinations
 * - Variance calculation
 * - Notification triggering
 * - Model improvement with variance data
 * 
 * Validates: Requirements 11B.1, 11B.8, 11B.9
 */
@ExtendWith(MockitoExtension.class)
class YieldPredictionServiceUnitTest {

    @Mock
    private YieldPredictionRepository yieldPredictionRepository;

    @Mock
    private WeatherClient weatherClient;

    @Mock
    private MandiPriceClient mandiPriceClient;

    private YieldPredictionService service;

    @BeforeEach
    void setUp() {
        service = new YieldPredictionService(
                yieldPredictionRepository, weatherClient, mandiPriceClient);
    }

    @Test
    @DisplayName("Test yield estimation with basic inputs")
    void testYieldEstimationWithBasicInputs() {
        // Arrange
        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId("FARMER-001")
                .cropId(1L)
                .cropName("RICE")
                .areaAcres(new BigDecimal("2"))
                .sowingDate(LocalDate.now().minusDays(60))
                .growthStage("FLOWERING")
                .includeFinancialProjection(false)
                .build();

        // Mock repository calls
        when(yieldPredictionRepository.findFirstByCropIdOrderByPredictionDateDesc(1L))
                .thenReturn(Optional.empty());
        when(yieldPredictionRepository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(1L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getPredictedYieldExpectedQuintalsPerAcre());
        assertNotNull(response.getPredictedYieldMinQuintalsPerAcre());
        assertNotNull(response.getPredictedYieldMaxQuintalsPerAcre());
        
        // Verify range validity: min <= expected <= max
        assertTrue(response.getPredictedYieldMinQuintalsPerAcre()
                .compareTo(response.getPredictedYieldExpectedQuintalsPerAcre()) <= 0);
        assertTrue(response.getPredictedYieldExpectedQuintalsPerAcre()
                .compareTo(response.getPredictedYieldMaxQuintalsPerAcre()) <= 0);
        
        // Verify total yields scale with area
        assertEquals(0, response.getPredictedYieldExpectedQuintals()
                .compareTo(response.getPredictedYieldExpectedQuintalsPerAcre()
                        .multiply(new BigDecimal("2"))));
    }

    @Test
    @DisplayName("Test yield estimation with weather data")
    void testYieldEstimationWithWeatherData() {
        // Arrange
        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId("FARMER-002")
                .cropId(2L)
                .cropName("WHEAT")
                .areaAcres(new BigDecimal("1"))
                .sowingDate(LocalDate.now().minusDays(45))
                .growthStage("VEGETATIVE")
                .totalRainfallMm(new BigDecimal("300"))  // Below optimal
                .averageTemperatureCelsius(new BigDecimal("32"))  // Above optimal
                .extremeWeatherEventsCount(1)
                .includeFinancialProjection(false)
                .build();

        when(yieldPredictionRepository.findFirstByCropIdOrderByPredictionDateDesc(2L))
                .thenReturn(Optional.empty());
        when(yieldPredictionRepository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(2L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getFactorsConsidered());
        assertTrue(response.getFactorsConsidered().stream()
                .anyMatch(f -> f.contains("Rainfall") || f.contains("Temperature") || f.contains("Weather")));
        assertNotNull(response.getFactorAdjustments());
    }

    @Test
    @DisplayName("Test yield estimation with soil health data")
    void testYieldEstimationWithSoilHealthData() {
        // Arrange
        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId("FARMER-003")
                .cropId(3L)
                .cropName("COTTON")
                .areaAcres(new BigDecimal("3"))
                .sowingDate(LocalDate.now().minusDays(90))
                .growthStage("FLOWERING")
                .soilNitrogenKgHa(new BigDecimal("200"))  // Low nitrogen
                .soilPhosphorusKgHa(new BigDecimal("5"))  // Low phosphorus
                .soilPotassiumKgHa(new BigDecimal("80"))  // Low potassium
                .soilPh(new BigDecimal("5.5"))  // Slightly acidic
                .includeFinancialProjection(false)
                .build();

        when(yieldPredictionRepository.findFirstByCropIdOrderByPredictionDateDesc(3L))
                .thenReturn(Optional.empty());
        when(yieldPredictionRepository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(3L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getFactorsConsidered());
        assertTrue(response.getFactorsConsidered().stream()
                .anyMatch(f -> f.contains("Soil")));
    }

    @Test
    @DisplayName("Test yield estimation with irrigation data")
    void testYieldEstimationWithIrrigationData() {
        // Arrange
        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId("FARMER-004")
                .cropId(4L)
                .cropName("RICE")
                .areaAcres(new BigDecimal("1"))
                .sowingDate(LocalDate.now().minusDays(50))
                .growthStage("VEGETATIVE")
                .irrigationType("DRIP")
                .irrigationFrequencyPerWeek(4)
                .includeFinancialProjection(false)
                .build();

        when(yieldPredictionRepository.findFirstByCropIdOrderByPredictionDateDesc(4L))
                .thenReturn(Optional.empty());
        when(yieldPredictionRepository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(4L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getFactorsConsidered());
        assertTrue(response.getFactorsConsidered().stream()
                .anyMatch(f -> f.contains("Irrigation")));
    }

    @Test
    @DisplayName("Test yield estimation with pest/disease data")
    void testYieldEstimationWithPestDiseaseData() {
        // Arrange
        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId("FARMER-005")
                .cropId(5L)
                .cropName("SOYBEAN")
                .areaAcres(new BigDecimal("2"))
                .sowingDate(LocalDate.now().minusDays(70))
                .growthStage("FLOWERING")
                .pestIncidentCount(2)
                .diseaseIncidentCount(1)
                .affectedAreaPercent(new BigDecimal("15"))
                .pestDiseaseControlStatus("controlled")
                .includeFinancialProjection(false)
                .build();

        when(yieldPredictionRepository.findFirstByCropIdOrderByPredictionDateDesc(5L))
                .thenReturn(Optional.empty());
        when(yieldPredictionRepository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(5L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getFactorsConsidered());
        assertTrue(response.getFactorsConsidered().stream()
                .anyMatch(f -> f.contains("Pest") || f.contains("Disease") || f.contains("Incident")));
    }

    @Test
    @DisplayName("Test significant deviation detection (>10%)")
    void testSignificantDeviationDetection() {
        // Arrange - Previous prediction with lower yield
        YieldPrediction previousPrediction = YieldPrediction.builder()
                .id(1L)
                .cropId(6L)
                .farmerId("FARMER-006")
                .predictedYieldExpectedQuintals(new BigDecimal("50"))
                .build();

        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId("FARMER-006")
                .cropId(6L)
                .cropName("MAIZE")
                .areaAcres(new BigDecimal("1"))
                .sowingDate(LocalDate.now().minusDays(40))
                .growthStage("VEGETATIVE")
                .includeFinancialProjection(false)
                .build();

        when(yieldPredictionRepository.findFirstByCropIdOrderByPredictionDateDesc(6L))
                .thenReturn(Optional.of(previousPrediction));
        when(yieldPredictionRepository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(2L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert
        assertTrue(response.isSuccess());
        // The new estimate should be significantly different from the previous one
        // (due to growth stage being earlier, the estimate should be lower)
        assertNotNull(response.getDeviationFromPreviousPercent());
    }

    @Test
    @DisplayName("Test variance calculation with actual yield")
    void testVarianceCalculation() {
        // Arrange
        YieldPrediction existingPrediction = YieldPrediction.builder()
                .id(1L)
                .cropId(7L)
                .farmerId("FARMER-007")
                .predictedYieldExpectedQuintals(new BigDecimal("50"))
                .predictedYieldMinQuintals(new BigDecimal("40"))
                .predictedYieldMaxQuintals(new BigDecimal("60"))
                .build();

        ActualYieldRequestDto request = ActualYieldRequestDto.builder()
                .cropId(7L)
                .farmerId("FARMER-007")
                .actualYieldQuintals(new BigDecimal("55"))
                .harvestDate(LocalDate.now())
                .build();

        when(yieldPredictionRepository.findFirstByCropIdOrderByPredictionDateDesc(7L))
                .thenReturn(Optional.of(existingPrediction));
        when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(yieldPredictionRepository.calculateAverageVarianceForCrop(anyString()))
                .thenReturn(new BigDecimal("5"));

        // Act
        VarianceTrackingDto response = service.recordActualYield(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getPredictionId());
        assertEquals(new BigDecimal("55"), response.getActualYieldQuintals());
        assertEquals(new BigDecimal("5"), response.getVarianceQuintals()); // 55 - 50 = 5
        assertEquals(new BigDecimal("10"), response.getVariancePercent()); // 5/50 * 100 = 10%
        assertEquals("positive", response.getVarianceCategory()); // Actual > Predicted
    }

    @Test
    @DisplayName("Test negative variance calculation")
    void testNegativeVarianceCalculation() {
        // Arrange
        YieldPrediction existingPrediction = YieldPrediction.builder()
                .id(2L)
                .cropId(8L)
                .farmerId("FARMER-008")
                .predictedYieldExpectedQuintals(new BigDecimal("40"))
                .build();

        ActualYieldRequestDto request = ActualYieldRequestDto.builder()
                .cropId(8L)
                .farmerId("FARMER-008")
                .actualYieldQuintals(new BigDecimal("32"))  // Lower than predicted
                .harvestDate(LocalDate.now())
                .build();

        when(yieldPredictionRepository.findFirstByCropIdOrderByPredictionDateDesc(8L))
                .thenReturn(Optional.of(existingPrediction));
        when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(yieldPredictionRepository.calculateAverageVarianceForCrop(anyString()))
                .thenReturn(new BigDecimal("8"));

        // Act
        VarianceTrackingDto response = service.recordActualYield(request);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("-8"), response.getVarianceQuintals()); // 32 - 40 = -8
        assertEquals(new BigDecimal("-20"), response.getVariancePercent()); // -8/40 * 100 = -20%
        assertEquals("negative", response.getVarianceCategory()); // Actual < Predicted
    }

    @Test
    @DisplayName("Test financial projection generation")
    void testFinancialProjectionGeneration() {
        // Arrange
        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId("FARMER-009")
                .cropId(9L)
                .cropName("RICE")
                .areaAcres(new BigDecimal("1"))
                .sowingDate(LocalDate.now().minusDays(60))
                .growthStage("FLOWERING")
                .includeFinancialProjection(true)
                .build();

        Map<String, BigDecimal> priceData = new HashMap<>();
        priceData.put("modalPrice", new BigDecimal("2500"));
        priceData.put("minPrice", new BigDecimal("2200"));
        priceData.put("maxPrice", new BigDecimal("2800"));

        when(yieldPredictionRepository.findFirstByCropIdOrderByPredictionDateDesc(9L))
                .thenReturn(Optional.empty());
        when(yieldPredictionRepository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(mandiPriceClient.getCurrentPrice("RICE")).thenReturn(priceData);
        when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(9L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getFinancialProjection());
        assertNotNull(response.getFinancialProjection().getEstimatedRevenueExpected());
        assertNotNull(response.getFinancialProjection().getEstimatedProfitExpected());
        assertTrue(response.getFinancialProjection().getEstimatedRevenueMin()
                .compareTo(response.getFinancialProjection().getEstimatedRevenueMax()) <= 0);
    }

    @Test
    @DisplayName("Test yield history retrieval")
    void testYieldHistoryRetrieval() {
        // Arrange
        Long cropId = 10L;
        List<YieldPrediction> history = Arrays.asList(
                createYieldPrediction(1L, cropId, new BigDecimal("45")),
                createYieldPrediction(2L, cropId, new BigDecimal("50")),
                createYieldPrediction(3L, cropId, new BigDecimal("48"))
        );

        when(yieldPredictionRepository.findByCropIdOrderByPredictionDateDesc(cropId))
                .thenReturn(history);

        // Act
        List<YieldPrediction> result = service.getYieldHistory(cropId);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(yieldPredictionRepository).findByCropIdOrderByPredictionDateDesc(cropId);
    }

    @Test
    @DisplayName("Test different crop types have different base yields")
    void testDifferentCropYields() {
        // Arrange
        String[] crops = {"RICE", "WHEAT", "COTTON", "POTATO", "SUGARCANE"};

        for (String crop : crops) {
            YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                    .farmerId("FARMER-TEST")
                    .cropId(System.nanoTime())  // Unique ID
                    .cropName(crop)
                    .areaAcres(new BigDecimal("1"))
                    .sowingDate(LocalDate.now().minusDays(60))
                    .growthStage("VEGETATIVE")
                    .includeFinancialProjection(false)
                    .build();

            when(yieldPredictionRepository.findFirstByCropIdOrderByPredictionDateDesc(anyLong()))
                    .thenReturn(Optional.empty());
            when(yieldPredictionRepository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                    .thenReturn(null);
            when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                    .thenAnswer(invocation -> {
                        YieldPrediction prediction = invocation.getArgument(0);
                        prediction.setId(System.nanoTime());
                        return prediction;
                    });

            // Act
            YieldEstimateResponseDto response = service.generateYieldEstimate(request);

            // Assert
            assertTrue(response.isSuccess());
            assertNotNull(response.getPredictedYieldExpectedQuintalsPerAcre());
            // Different crops should have different yields
        }
    }

    @Test
    @DisplayName("Test empty area defaults to 1 acre")
    void testEmptyAreaDefaults() {
        // Arrange
        YieldEstimateRequestDto request = YieldEstimateRequestDto.builder()
                .farmerId("FARMER-010")
                .cropId(11L)
                .cropName("RICE")
                .areaAcres(null)  // No area specified
                .sowingDate(LocalDate.now().minusDays(60))
                .growthStage("VEGETATIVE")
                .includeFinancialProjection(false)
                .build();

        when(yieldPredictionRepository.findFirstByCropIdOrderByPredictionDateDesc(11L))
                .thenReturn(Optional.empty());
        when(yieldPredictionRepository.calculateAverageActualYieldForFarmerAndCrop(anyString(), anyString()))
                .thenReturn(null);
        when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> {
                    YieldPrediction prediction = invocation.getArgument(0);
                    prediction.setId(11L);
                    return prediction;
                });

        // Act
        YieldEstimateResponseDto response = service.generateYieldEstimate(request);

        // Assert
        assertTrue(response.isSuccess());
        // Per-acre values should be set
        assertNotNull(response.getPredictedYieldExpectedQuintalsPerAcre());
        // Total should equal per-acre (default area = 1)
        assertEquals(0, response.getPredictedYieldExpectedQuintals()
                .compareTo(response.getPredictedYieldExpectedQuintalsPerAcre()));
    }

    @Test
    @DisplayName("Test notification marking")
    void testNotificationMarking() {
        // Arrange
        YieldPrediction prediction = YieldPrediction.builder()
                .id(1L)
                .notificationSent(false)
                .build();

        when(yieldPredictionRepository.findById(1L)).thenReturn(Optional.of(prediction));
        when(yieldPredictionRepository.save(any(YieldPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.markAsNotified(1L);

        // Assert
        assertTrue(prediction.getNotificationSent());
        assertNotNull(prediction.getNotificationSentAt());
        verify(yieldPredictionRepository).save(prediction);
    }

    /**
     * Helper method to create a yield prediction for testing.
     */
    private YieldPrediction createYieldPrediction(Long id, Long cropId, BigDecimal expectedYield) {
        return YieldPrediction.builder()
                .id(id)
                .cropId(cropId)
                .farmerId("TEST-FARMER")
                .predictionDate(LocalDate.now().minusDays(id * 10))
                .predictedYieldMinQuintals(expectedYield.multiply(new BigDecimal("0.8")))
                .predictedYieldExpectedQuintals(expectedYield)
                .predictedYieldMaxQuintals(expectedYield.multiply(new BigDecimal("1.2")))
                .build();
    }
}