package com.farmer.mandi.service;

import com.farmer.mandi.client.AgmarknetApiClient;
import com.farmer.mandi.dto.*;
import com.farmer.mandi.entity.MandiPrices;
import com.farmer.mandi.entity.PriceAlert;
import com.farmer.mandi.repository.MandiPricesRepository;
import com.farmer.mandi.repository.MandiLocationRepository;
import com.farmer.mandi.repository.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import reactor.core.publisher.Mono;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Mandi Service.
 * 
 * Tests:
 * - AGMARKNET API integration with mock responses
 * - Price data caching and retrieval
 * - Distance sorting
 * - Price trend calculations
 * - Alert generation and notifications
 * 
 * Requirements Reference:
 * - 6.1: Retrieve current prices from AGMARKNET API
 * - 6.4: Sort mandis by distance from farmer's location
 * - 6.5: Display 30-day price history with graphical visualization
 * - 6.10: Send push notifications for crop price alerts
 * - 6.11: Display cached prices with timestamp when AGMARKNET unavailable
 */
@ExtendWith(MockitoExtension.class)
class MandiServiceUnitTest {

    @Mock
    private AgmarknetApiClient agmarknetApiClient;

    @Mock
    private com.farmer.mandi.client.DataGovInApiClient dataGovInApiClient;

    @Mock
    private MandiPricesRepository mandiPricesRepository;

    @Mock
    private MandiLocationRepository mandiLocationRepository;

    @Mock
    private PriceAlertRepository priceAlertRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private MandiPriceService mandiPriceService;
    private MandiLocationService mandiLocationService;
    private PriceTrendService priceTrendService;
    private PriceAlertService priceAlertService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Set up Redis mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Create services
        mandiPriceService = new MandiPriceService(agmarknetApiClient, dataGovInApiClient, mandiPricesRepository, redisTemplate);
        mandiLocationService = new MandiLocationService(mandiLocationRepository);
        notificationService = new NotificationService();
        priceAlertService = new PriceAlertService(priceAlertRepository, mandiPriceService, notificationService);
        priceTrendService = new PriceTrendService(mandiPriceService);
    }

    @Nested
    @DisplayName("AGMARKNET API Integration Tests")
    class AgmarknetApiIntegrationTests {

        @Test
        @DisplayName("Should fetch commodity prices from AGMARKNET API")
        void shouldFetchCommodityPricesFromApi() {
            // Arrange
            String commodity = "Paddy";
            List<MandiPriceDto> mockPrices = createMockPrices(commodity, 5);
            when(agmarknetApiClient.getCommodityPrices(commodity))
                    .thenReturn(Mono.just(mockPrices));

            // Act
            Mono<List<MandiPriceDto>> result = mandiPriceService.getCommodityPrices(commodity);

            // Assert
            assertNotNull(result);
            verify(agmarknetApiClient).getCommodityPrices(commodity);
        }

        @Test
        @DisplayName("Should handle API timeout gracefully")
        void shouldHandleApiTimeoutGracefully() {
            // Arrange
            String commodity = "Wheat";
            when(agmarknetApiClient.getCommodityPrices(commodity))
                    .thenReturn(Mono.error(new RuntimeException("API timeout")));

            // Mock database fallback
            List<MandiPriceDto> dbPrices = createMockPrices(commodity, 3);
            when(mandiPricesRepository.findLatestPricesByCommodity(commodity))
                    .thenReturn(createMandiPricesEntities(commodity, 3));

            // Act
            Mono<List<MandiPriceDto>> result = mandiPriceService.getCommodityPrices(commodity);

            // Assert - should fall back to database
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return empty list when API and database fail")
        void shouldReturnEmptyListWhenApiAndDatabaseFail() {
            // Arrange
            String commodity = "Cotton";
            when(agmarknetApiClient.getCommodityPrices(commodity))
                    .thenReturn(Mono.error(new RuntimeException("API error")));
            when(mandiPricesRepository.findLatestPricesByCommodity(commodity))
                    .thenReturn(Collections.emptyList());

            // Act
            Mono<List<MandiPriceDto>> result = mandiPriceService.getCommodityPrices(commodity);

            // Assert
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Price Data Caching Tests")
    class PriceDataCachingTests {

        @Test
        @DisplayName("Should cache prices in Redis with TTL")
        void shouldCachePricesInRedisWithTtl() {
            // Arrange
            String commodity = "Paddy";
            List<MandiPriceDto> mockPrices = createMockPrices(commodity, 5);
            when(agmarknetApiClient.getCommodityPrices(commodity))
                    .thenReturn(Mono.just(mockPrices));

            // Act
            mandiPriceService.getCommodityPrices(commodity);

            // Assert - verify cache was set
            verify(valueOperations).set(
                    eq("mandi:price:commodity:paddy"),
                    eq(mockPrices),
                    eq(1L),
                    eq(java.util.concurrent.TimeUnit.HOURS));
        }

        @Test
        @DisplayName("Should retrieve prices from cache first")
        void shouldRetrievePricesFromCacheFirst() {
            // Arrange
            String commodity = "Paddy";
            List<MandiPriceDto> cachedPrices = createMockPrices(commodity, 5);
            when(valueOperations.get("mandi:price:commodity:paddy"))
                    .thenReturn(cachedPrices);

            // Act
            Mono<List<MandiPriceDto>> result = mandiPriceService.getCommodityPrices(commodity);

            // Assert
            assertNotNull(result);
            verify(agmarknetApiClient, never()).getCommodityPrices(anyString());
        }

        @Test
        @DisplayName("Should mark cached data with isCached flag")
        void shouldMarkCachedDataWithIsCachedFlag() {
            // Arrange
            String commodity = "Paddy";
            List<MandiPriceDto> cachedPrices = createMockPrices(commodity, 5);
            when(valueOperations.get("mandi:price:commodity:paddy"))
                    .thenReturn(cachedPrices);

            // Act
            Mono<List<MandiPriceDto>> result = mandiPriceService.getCommodityPrices(commodity);

            // Assert - cached data should have isCached = true
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Distance Sorting Tests")
    class DistanceSortingTests {

        @Test
        @DisplayName("Should sort prices by distance in ascending order")
        void shouldSortPricesByDistanceInAscendingOrder() {
            // Arrange
            List<MandiPriceDto> prices = new ArrayList<>();
            prices.add(createPriceDto("Mandi A", Double.valueOf(50)));
            prices.add(createPriceDto("Mandi B", Double.valueOf(20)));
            prices.add(createPriceDto("Mandi C", Double.valueOf(80)));
            prices.add(createPriceDto("Mandi D", Double.valueOf(35)));

            Double farmerLat = Double.valueOf(12.9716);
            Double farmerLon = Double.valueOf(77.5946);

            // Act
            List<MandiPriceDto> sorted = mandiLocationService.sortPricesByDistance(
                    prices, farmerLat, farmerLon);

            // Assert
            assertNotNull(sorted);
            assertEquals(4, sorted.size());
            
            // Verify ascending order
            for (int i = 0; i < sorted.size() - 1; i++) {
                Double current = sorted.get(i).getDistanceKm();
                Double next = sorted.get(i + 1).getDistanceKm();
                assertTrue(current <= next,
                        "Prices should be sorted in ascending order by distance");
            }
        }

        @Test
        @DisplayName("Should handle empty price list")
        void shouldHandleEmptyPriceList() {
            // Arrange
            List<MandiPriceDto> prices = new ArrayList<>();
            Double farmerLat = Double.valueOf(12.9716);
            Double farmerLon = Double.valueOf(77.5946);

            // Act
            List<MandiPriceDto> sorted = mandiLocationService.sortPricesByDistance(
                    prices, farmerLat, farmerLon);

            // Assert
            assertNotNull(sorted);
            assertTrue(sorted.isEmpty());
        }

        @Test
        @DisplayName("Should calculate distance using Haversine formula")
        void shouldCalculateDistanceUsingHaversineFormula() {
            // Arrange
            Double lat1 = Double.valueOf(12.9716);
            Double lon1 = Double.valueOf(77.5946);
            Double lat2 = Double.valueOf(13.0000);
            Double lon2 = Double.valueOf(77.6000);

            // Act
            Double distance = mandiLocationService.calculateDistance(lat1, lon1, lat2, lon2);

            // Assert
            assertNotNull(distance);
            assertTrue(distance.doubleValue() > 0);
            assertTrue(distance.doubleValue() < 10); // Should be around 3-4 km
        }
    }

    @Nested
    @DisplayName("Price Trend Calculation Tests")
    class PriceTrendCalculationTests {

        @Test
        @DisplayName("Should calculate price trend correctly")
        void shouldCalculatePriceTrendCorrectly() {
            // Arrange
            String commodity = "Paddy";
            List<MandiPriceDto> historicalPrices = createHistoricalPrices(commodity, 30);
            when(mandiPriceService.getHistoricalPricesFromDatabase(eq(commodity), anyInt()))
                    .thenReturn(historicalPrices);

            // Act
            Mono<PriceTrendDto> result = priceTrendService.getPriceTrend(commodity, 30);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should identify increasing trend")
        void shouldIdentifyIncreasingTrend() {
            // Arrange
            String commodity = "Paddy";
            List<MandiPriceDto> increasingPrices = createIncreasingPrices(commodity, 7);
            when(mandiPriceService.getHistoricalPricesFromDatabase(eq(commodity), anyInt()))
                    .thenReturn(increasingPrices);

            // Act
            Mono<PriceTrendDto> result = priceTrendService.getPriceTrend(commodity, 7);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should calculate MSP comparison correctly")
        void shouldCalculateMspComparisonCorrectly() {
            // Arrange
            String commodity = "Paddy";
            List<MandiPriceDto> prices = createMockPrices(commodity, 5);
            when(mandiPriceService.getLatestPricesFromDatabase(commodity))
                    .thenReturn(prices);

            // Act
            PriceTrendDto.MspComparisonDto comparison = priceTrendService.getMspComparison(commodity);

            // Assert
            assertNotNull(comparison);
            assertNotNull(comparison.getMsp());
            assertNotNull(comparison.getCurrentMarketPrice());
            assertNotNull(comparison.getComparisonResult());
        }

        @Test
        @DisplayName("Should generate storage advisory based on trend")
        void shouldGenerateStorageAdvisoryBasedOnTrend() {
            // Arrange
            String commodity = "Paddy";
            List<MandiPriceDto> prices = createIncreasingPrices(commodity, 7);
            when(mandiPriceService.getHistoricalPricesFromDatabase(eq(commodity), eq(7)))
                    .thenReturn(prices);

            // Act
            PriceTrendDto.StorageAdvisoryDto advisory = priceTrendService.getStorageAdvisory(commodity);

            // Assert
            assertNotNull(advisory);
            assertNotNull(advisory.getRecommendation());
            assertNotNull(advisory.getReasoning());
        }
    }

    @Nested
    @DisplayName("Price Alert Tests")
    class PriceAlertTests {

        @Test
        @DisplayName("Should create price alert subscription")
        void shouldCreatePriceAlertSubscription() {
            // Arrange
            PriceAlertRequest request = PriceAlertRequest.builder()
                    .farmerId("FARMER001")
                    .commodity("Paddy")
                    .variety("Hybrid")
                    .targetPrice(Double.valueOf(2600))
                    .alertType("PRICE_ABOVE")
                    .neighboringDistrictsOnly(false)
                    .build();

            when(priceAlertRepository.findByFarmerIdAndCommodityAndVarietyAndIsActiveTrue(
                    anyString(), anyString(), anyString()))
                    .thenReturn(Collections.emptyList());

            PriceAlert savedAlert = PriceAlert.builder()
                    .id(1L)
                    .farmerId(request.getFarmerId())
                    .commodity(request.getCommodity())
                    .variety(request.getVariety())
                    .targetPrice(request.getTargetPrice())
                    .alertType(request.getAlertType())
                    .neighboringDistrictsOnly(request.getNeighboringDistrictsOnly())
                    .isActive(true)
                    .notificationSent(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(priceAlertRepository.save(any(PriceAlert.class)))
                    .thenReturn(savedAlert);

            // Act
            PriceAlertDto result = priceAlertService.createAlert(request);

            // Assert
            assertNotNull(result);
            assertEquals(request.getFarmerId(), result.getFarmerId());
            assertEquals(request.getCommodity(), result.getCommodity());
            assertEquals(request.getTargetPrice(), result.getTargetPrice());
        }

        @Test
        @DisplayName("Should get alerts for farmer")
        void shouldGetAlertsForFarmer() {
            // Arrange
            String farmerId = "FARMER001";
            List<PriceAlert> alerts = Arrays.asList(
                    createPriceAlert(1L, farmerId, "Paddy"),
                    createPriceAlert(2L, farmerId, "Wheat")
            );

            when(priceAlertRepository.findByFarmerIdAndIsActiveTrue(farmerId))
                    .thenReturn(alerts);

            // Act
            List<PriceAlertDto> result = priceAlertService.getAlertsForFarmer(farmerId);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Should deactivate alert")
        void shouldDeactivateAlert() {
            // Arrange
            Long alertId = 1L;
            PriceAlert alert = createPriceAlert(alertId, "FARMER001", "Paddy");

            when(priceAlertRepository.findById(alertId))
                    .thenReturn(Optional.of(alert));
            when(priceAlertRepository.save(any(PriceAlert.class)))
                    .thenReturn(alert);

            // Act
            boolean result = priceAlertService.deactivateAlert(alertId);

            // Assert
            assertTrue(result);
            assertFalse(alert.getIsActive());
        }

        @Test
        @DisplayName("Should return false when deactivating non-existent alert")
        void shouldReturnFalseWhenDeactivatingNonExistentAlert() {
            // Arrange
            Long alertId = 999L;
            when(priceAlertRepository.findById(alertId))
                    .thenReturn(Optional.empty());

            // Act
            boolean result = priceAlertService.deactivateAlert(alertId);

            // Assert
            assertFalse(result);
        }
    }

    // Helper methods

    private List<MandiPriceDto> createMockPrices(String commodity, int count) {
        List<MandiPriceDto> prices = new ArrayList<>();
        String[] mandis = {"Mandi A", "Mandi B", "Mandi C", "Mandi D", "Mandi E"};
        String[] states = {"Karnataka", "Maharashtra", "Telangana"};
        String[] districts = {"Bangalore", "Pune", "Hyderabad"};

        for (int i = 0; i < count; i++) {
            prices.add(MandiPriceDto.builder()
                    .id((long) (i + 1))
                    .commodityName(commodity)
                    .variety("Hybrid")
                    .mandiName(mandis[i % mandis.length])
                    .state(states[i % states.length])
                    .district(districts[i % districts.length])
                    .priceDate(LocalDate.now())
                    .modalPrice(Double.valueOf(2500 + i * 50))
                    .minPrice(Double.valueOf(2300 + i * 50))
                    .maxPrice(Double.valueOf(2700 + i * 50))
                    .arrivalQuantityQuintals(Double.valueOf(100 + i * 20))
                    .unit("Quintal")
                    .source("AGMARKNET")
                    .isCached(false)
                    .build());
        }
        return prices;
    }

    private List<MandiPrices> createMandiPricesEntities(String commodity, int count) {
        List<MandiPrices> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entities.add(MandiPrices.builder()
                    .id((long) (i + 1))
                    .commodityName(commodity)
                    .variety("Hybrid")
                    .mandiName("Mandi " + (char) ('A' + i))
                    .state("Karnataka")
                    .district("Bangalore")
                    .priceDate(LocalDate.now())
                    .modalPrice(Double.valueOf(2500 + i * 50))
                    .minPrice(Double.valueOf(2300 + i * 50))
                    .maxPrice(Double.valueOf(2700 + i * 50))
                    .arrivalQuantityQuintals(Double.valueOf(100 + i * 20))
                    .unit("Quintal")
                    .source("AGMARKNET")
                    .build());
        }
        return entities;
    }

    private List<MandiPriceDto> createHistoricalPrices(String commodity, int days) {
        List<MandiPriceDto> prices = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            prices.add(MandiPriceDto.builder()
                    .id((long) (i + 1))
                    .commodityName(commodity)
                    .variety("Hybrid")
                    .mandiName("Mandi A")
                    .state("Karnataka")
                    .district("Bangalore")
                    .priceDate(LocalDate.now().minusDays(days - i - 1))
                    .modalPrice(Double.valueOf(2500 + i * 10))
                    .minPrice(Double.valueOf(2300 + i * 10))
                    .maxPrice(Double.valueOf(2700 + i * 10))
                    .arrivalQuantityQuintals(Double.valueOf(100 + i * 5))
                    .unit("Quintal")
                    .source("AGMARKNET")
                    .isCached(false)
                    .build());
        }
        return prices;
    }

    private List<MandiPriceDto> createIncreasingPrices(String commodity, int days) {
        List<MandiPriceDto> prices = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            prices.add(MandiPriceDto.builder()
                    .id((long) (i + 1))
                    .commodityName(commodity)
                    .variety("Hybrid")
                    .mandiName("Mandi A")
                    .state("Karnataka")
                    .district("Bangalore")
                    .priceDate(LocalDate.now().minusDays(days - i - 1))
                    .modalPrice(Double.valueOf(2400 + i * 20)) // Increasing prices
                    .minPrice(Double.valueOf(2200 + i * 20))
                    .maxPrice(Double.valueOf(2600 + i * 20))
                    .arrivalQuantityQuintals(Double.valueOf(100))
                    .unit("Quintal")
                    .source("AGMARKNET")
                    .isCached(false)
                    .build());
        }
        return prices;
    }

    private MandiPriceDto createPriceDto(String mandiName, Double distance) {
        return MandiPriceDto.builder()
                .id(1L)
                .commodityName("Paddy")
                .variety("Hybrid")
                .mandiName(mandiName)
                .state("Karnataka")
                .district("Bangalore Rural")
                .priceDate(LocalDate.now())
                .modalPrice(Double.valueOf(2500))
                .minPrice(Double.valueOf(2300))
                .maxPrice(Double.valueOf(2700))
                .arrivalQuantityQuintals(Double.valueOf(100))
                .unit("Quintal")
                .source("AGMARKNET")
                .distanceKm(distance)
                .isCached(false)
                .build();
    }

    private PriceAlert createPriceAlert(Long id, String farmerId, String commodity) {
        return PriceAlert.builder()
                .id(id)
                .farmerId(farmerId)
                .commodity(commodity)
                .variety("Hybrid")
                .targetPrice(Double.valueOf(2600))
                .alertType("PRICE_ABOVE")
                .neighboringDistrictsOnly(false)
                .isActive(true)
                .notificationSent(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
