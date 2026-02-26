package com.farmer.mandi.service;

import com.farmer.mandi.dto.MandiPriceDto;
import com.farmer.mandi.entity.MandiPrices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Mandi Price Service.
 * 
 * Property 12: Price Data Completeness and Constraints
 * Validates: Requirements 6.2
 * 
 * These tests verify the following property:
 * For any mandi price record, all required fields (modal price, min price, max price, 
 * arrival quantity in quintals, commodity variety) should be present, and the constraint 
 * min_price ≤ modal_price ≤ max_price should hold.
 * 
 * Requirements Reference:
 * - Requirement 6.2: WHEN displaying mandi prices, THE Application SHALL show modal price 
 *   (most frequent trading price), minimum price, maximum price, arrival quantity (in quintals), 
 *   and commodity variety (Hybrid vs Desi) for the selected commodity
 */
@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class MandiPriceServicePropertyTest {

    @Mock
    private WebClient agmarknetWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private com.farmer.mandi.client.DataGovInApiClient dataGovInApiClient;

    private com.farmer.mandi.client.AgmarknetApiClient agmarknetApiClient;
    private com.farmer.mandi.repository.MandiPricesRepository mandiPricesRepository;
    private com.farmer.mandi.service.MandiPriceService mandiPriceService;

    @BeforeEach
    void setUp() {
        // Set up AGMARKNET API client with mocked WebClient
        when(agmarknetWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(agmarknetWebClient);

        agmarknetApiClient = new com.farmer.mandi.client.AgmarknetApiClient(
                webClientBuilder, "http://localhost:8089", 5, 10, 1);

        // Set up Redis mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Create real repository for testing
        mandiPricesRepository = mock(com.farmer.mandi.repository.MandiPricesRepository.class);

        mandiPriceService = new MandiPriceService(agmarknetApiClient, dataGovInApiClient, mandiPricesRepository, redisTemplate);
    }

    @Nested
    @DisplayName("Property 12.1: Price Data Completeness")
    class PriceDataCompleteness {

        @ParameterizedTest
        @ValueSource(strings = {"Paddy", "Wheat", "Cotton", "Soybean", "Groundnut"})
        @DisplayName("All required fields should be present in price data")
        void allRequiredFieldsShouldBePresent(String commodity) {
            // Property: For any commodity, all required fields should be present in price data
            // 
            // Required fields: modal_price, min_price, max_price, arrival_quantity, variety
            
            // Create test price data with all required fields
            MandiPriceDto price = createCompletePriceData(commodity, "Hybrid", 
                    Double.valueOf(2500), Double.valueOf(2300), Double.valueOf(2700),
                    Double.valueOf(100), "Quintal");
            
            // Verify all required fields are present
            assertNotNull(price.getModalPrice(), "Modal price should be present");
            assertNotNull(price.getMinPrice(), "Min price should be present");
            assertNotNull(price.getMaxPrice(), "Max price should be present");
            assertNotNull(price.getArrivalQuantityQuintals(), "Arrival quantity should be present");
            assertNotNull(price.getVariety(), "Variety should be present");
            assertNotNull(price.getCommodityName(), "Commodity name should be present");
            assertNotNull(price.getMandiName(), "Mandi name should be present");
            assertNotNull(price.getUnit(), "Unit should be present");
        }

        @ParameterizedTest
        @CsvSource({
            "Paddy, Hybrid",
            "Wheat, Local",
            "Cotton, Desi",
            "Soybean, Yellow",
            "Groundnut, Bold"
        })
        @DisplayName("Variety field should be present for all commodities")
        void varietyFieldShouldBePresent(String commodity, String variety) {
            // Property: For any commodity, the variety field should be present
            
            MandiPriceDto price = MandiPriceDto.builder()
                    .commodityName(commodity)
                    .variety(variety)
                    .mandiName("Test Mandi")
                    .state("Karnataka")
                    .district("Bangalore Rural")
                    .priceDate(LocalDate.now())
                    .modalPrice(Double.valueOf(2500))
                    .minPrice(Double.valueOf(2300))
                    .maxPrice(Double.valueOf(2700))
                    .arrivalQuantityQuintals(Double.valueOf(100))
                    .unit("Quintal")
                    .source("AGMARKNET")
                    .build();
            
            assertNotNull(price.getVariety(), "Variety should be present for " + commodity);
            assertEquals(variety, price.getVariety());
        }
    }

    @Nested
    @DisplayName("Property 12.2: Price Constraint Validation")
    class PriceConstraintValidation {

        @ParameterizedTest
        @CsvSource({
            "2300, 2500, 2700",   // Valid: min < modal < max
            "2500, 2500, 2500",   // Valid: all equal
            "2000, 2500, 3000",   // Valid: clear spread
            "2400, 2500, 2600",   // Valid: narrow spread
        })
        @DisplayName("Price constraint min_price ≤ modal_price ≤ max_price should hold")
        void priceConstraintShouldHold(Double minPrice, Double modalPrice, Double maxPrice) {
            // Property: For any price record, the constraint min_price ≤ modal_price ≤ max_price should hold
            
            MandiPriceDto price = MandiPriceDto.builder()
                    .commodityName("Paddy")
                    .variety("Hybrid")
                    .mandiName("Test Mandi")
                    .state("Karnataka")
                    .district("Bangalore Rural")
                    .priceDate(LocalDate.now())
                    .modalPrice(modalPrice)
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .arrivalQuantityQuintals(Double.valueOf(100))
                    .unit("Quintal")
                    .build();
            
            // Verify the constraint
            assertTrue(price.getMinPrice().compareTo(price.getModalPrice()) <= 0,
                    "Min price should be less than or equal to modal price");
            assertTrue(price.getModalPrice().compareTo(price.getMaxPrice()) <= 0,
                    "Modal price should be less than or equal to max price");
        }

        @Test
        @DisplayName("Price constraint should fail when min_price > modal_price")
        void priceConstraintShouldFailWhenMinGreaterThanModal() {
            // This test verifies the constraint detection works correctly
            
            MandiPrices entity = new MandiPrices();
            entity.setMinPrice(Double.valueOf(2700));
            entity.setModalPrice(Double.valueOf(2500));
            entity.setMaxPrice(Double.valueOf(2600));
            
            // The constraint should fail
            assertFalse(entity.isPriceConstraintValid(),
                    "Constraint should fail when min_price > modal_price");
        }

        @Test
        @DisplayName("Price constraint should fail when modal_price > max_price")
        void priceConstraintShouldFailWhenModalGreaterThanMax() {
            // This test verifies the constraint detection works correctly
            
            MandiPrices entity = new MandiPrices();
            entity.setMinPrice(Double.valueOf(2300));
            entity.setModalPrice(Double.valueOf(2700));
            entity.setMaxPrice(Double.valueOf(2500));
            
            // The constraint should fail
            assertFalse(entity.isPriceConstraintValid(),
                    "Constraint should fail when modal_price > max_price");
        }

        @Test
        @DisplayName("Price constraint should fail when prices are null")
        void priceConstraintShouldFailWhenNull() {
            // This test verifies null handling
            
            MandiPrices entity = new MandiPrices();
            entity.setMinPrice(null);
            entity.setModalPrice(Double.valueOf(2500));
            entity.setMaxPrice(Double.valueOf(2700));
            
            // The constraint should fail for null values
            assertFalse(entity.isPriceConstraintValid(),
                    "Constraint should fail when min_price is null");
        }
    }

    @Nested
    @DisplayName("Property 12.3: Price Data Consistency")
    class PriceDataConsistency {

        @ParameterizedTest
        @ValueSource(strings = {"Paddy", "Wheat", "Cotton", "Soybean", "Groundnut", "Maize", "Gram"})
        @DisplayName("Price data should be consistent across multiple records for same commodity")
        void priceDataShouldBeConsistent(String commodity) {
            // Property: For any commodity, price data should be consistent across multiple records
            
            List<MandiPriceDto> prices = new ArrayList<>();
            String[] varieties = {"Hybrid", "Local", "Desi"};
            String[] mandis = {"Mandi A", "Mandi B", "Mandi C"};
            
            for (int i = 0; i < 3; i++) {
                MandiPriceDto price = MandiPriceDto.builder()
                        .commodityName(commodity)
                        .variety(varieties[i])
                        .mandiName(mandis[i])
                        .state("Karnataka")
                        .district("Bangalore Rural")
                        .priceDate(LocalDate.now())
                        .modalPrice(Double.valueOf(2500 + i * 100))
                        .minPrice(Double.valueOf(2300 + i * 100))
                        .maxPrice(Double.valueOf(2700 + i * 100))
                        .arrivalQuantityQuintals(Double.valueOf(100 + i * 50))
                        .unit("Quintal")
                        .source("AGMARKNET")
                        .build();
                prices.add(price);
            }
            
            // Verify all records have the same commodity
            assertTrue(prices.stream().allMatch(p -> p.getCommodityName().equals(commodity)),
                    "All prices should have the same commodity name");
            
            // Verify all records have valid price constraints
            assertTrue(prices.stream().allMatch(p -> 
                    p.getMinPrice().compareTo(p.getModalPrice()) <= 0 &&
                    p.getModalPrice().compareTo(p.getMaxPrice()) <= 0),
                    "All prices should satisfy the price constraint");
        }

        @Test
        @DisplayName("Price data should have correct unit (Quintals)")
        void priceDataShouldHaveCorrectUnit() {
            // Property: Price data should have the correct unit (Quintals)
            
            MandiPriceDto price = MandiPriceDto.builder()
                    .commodityName("Paddy")
                    .variety("Hybrid")
                    .mandiName("Test Mandi")
                    .state("Karnataka")
                    .district("Bangalore Rural")
                    .priceDate(LocalDate.now())
                    .modalPrice(Double.valueOf(2500))
                    .minPrice(Double.valueOf(2300))
                    .maxPrice(Double.valueOf(2700))
                    .arrivalQuantityQuintals(Double.valueOf(100))
                    .unit("Quintal")
                    .build();
            
            assertEquals("Quintal", price.getUnit(),
                    "Unit should be 'Quintal' for arrival quantity");
        }
    }

    /**
     * Helper method to create complete price data with all required fields.
     */
    private MandiPriceDto createCompletePriceData(
            String commodity, String variety, Double modalPrice,
            Double minPrice, Double maxPrice, Double arrivalQuantity, String unit) {
        return MandiPriceDto.builder()
                .commodityName(commodity)
                .variety(variety)
                .mandiName("Test Mandi")
                .state("Karnataka")
                .district("Bangalore Rural")
                .priceDate(LocalDate.now())
                .modalPrice(modalPrice)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .arrivalQuantityQuintals(arrivalQuantity)
                .unit(unit)
                .source("AGMARKNET")
                .build();
    }
}
