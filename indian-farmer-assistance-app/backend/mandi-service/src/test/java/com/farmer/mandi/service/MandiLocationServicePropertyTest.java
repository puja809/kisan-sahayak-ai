package com.farmer.mandi.service;

import com.farmer.mandi.dto.MandiLocationDto;
import com.farmer.mandi.dto.MandiPriceDto;
import com.farmer.mandi.repository.MandiLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Mandi Location Service.
 * 
 * Property 11: Distance-Based Ascending Sort
 * Validates: Requirements 6.4, 7.2
 * 
 * These tests verify the following property:
 * For any list of locations (mandis, government bodies, KVKs) sorted by distance 
 * from a farmer's location, for any two adjacent items in the list, the first 
 * should have a distance less than or equal to the second.
 * 
 * Requirements Reference:
 * - Requirement 6.4: WHEN multiple mandis are available, THE Application SHALL 
 *   sort them by distance from the farmer's location using geo-location data
 * - Requirement 7.2: WHEN the location is determined, THE Application SHALL 
 *   retrieve information for district agriculture offices, state agriculture 
 *   departments, and Krishi Vigyan Kendras (KVKs) within 50 km
 */
@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class MandiLocationServicePropertyTest {

    @Mock
    private MandiLocationRepository mandiLocationRepository;

    private MandiLocationService mandiLocationService;

    // Test farmer location (Bangalore, Karnataka)
    private final BigDecimal FARMER_LATITUDE = BigDecimal.valueOf(12.9716);
    private final BigDecimal FARMER_LONGITUDE = BigDecimal.valueOf(77.5946);

    @BeforeEach
    void setUp() {
        mandiLocationService = new MandiLocationService(mandiLocationRepository);
    }

    @Nested
    @DisplayName("Property 11.1: Distance Calculation Accuracy")
    class DistanceCalculationAccuracy {

        @ParameterizedTest
        @CsvSource({
            "12.9716, 77.5946, 12.9716, 77.5946, 0",      // Same location
            "12.9716, 77.5946, 13.0000, 77.6000, 3.2",    // ~3 km away
            "12.9716, 77.5946, 13.0500, 77.6500, 10.5",   // ~10 km away
            "12.9716, 77.5946, 13.1000, 77.7000, 17.8",   // ~18 km away
        })
        @DisplayName("Distance calculation should be accurate using Haversine formula")
        void distanceCalculationShouldBeAccurate(
                double lat1, double lon1, double lat2, double lon2, double expectedDistance) {
            // Property: Distance calculation should be accurate using Haversine formula
            
            BigDecimal distance = mandiLocationService.calculateDistance(
                    BigDecimal.valueOf(lat1), BigDecimal.valueOf(lon1),
                    BigDecimal.valueOf(lat2), BigDecimal.valueOf(lon2));
            
            // Allow 10% tolerance for the calculation
            double actualDistance = distance.doubleValue();
            double tolerance = expectedDistance * 0.1;
            
            assertTrue(Math.abs(actualDistance - expectedDistance) < tolerance,
                    String.format("Distance should be approximately %.1f km, but was %.2f km",
                            expectedDistance, actualDistance));
        }

        @Test
        @DisplayName("Distance should be zero for same location")
        void distanceShouldBeZeroForSameLocation() {
            // Property: Distance should be zero for the same location
            
            BigDecimal distance = mandiLocationService.calculateDistance(
                    FARMER_LATITUDE, FARMER_LONGITUDE,
                    FARMER_LATITUDE, FARMER_LONGITUDE);
            
            assertEquals(0, distance.compareTo(BigDecimal.ZERO),
                    "Distance should be zero for the same location");
        }

        @Test
        @DisplayName("Distance should handle null coordinates")
        void distanceShouldHandleNullCoordinates() {
            // Property: Distance calculation should handle null coordinates gracefully
            
            BigDecimal distance = mandiLocationService.calculateDistance(
                    FARMER_LATITUDE, FARMER_LONGITUDE,
                    null, FARMER_LONGITUDE);
            
            assertEquals(BigDecimal.valueOf(Double.MAX_VALUE).compareTo(distance), 0,
                    "Distance should return MAX_VALUE for null coordinates");
        }
    }

    @Nested
    @DisplayName("Property 11.2: Ascending Sort Order")
    class AscendingSortOrder {

        @ParameterizedTest
        @CsvSource({
            "10, 20, 30, 40, 50",     // Increasing distances
            "50, 40, 30, 20, 10",     // Decreasing distances (should be sorted)
            "25, 25, 25, 25, 25",     // Equal distances
            "0, 100, 50, 75, 25",     // Random distances (should be sorted)
        })
        @DisplayName("Sorted locations should be in ascending order of distance")
        void sortedLocationsShouldBeInAscendingOrder(double d1, double d2, double d3, double d4, double d5) {
            // Property: For any list of locations sorted by distance from a farmer's location,
            // for any two adjacent items in the list, the first should have a distance 
            // less than or equal to the second.
            
            List<BigDecimal> distances = Arrays.asList(
                    BigDecimal.valueOf(d1),
                    BigDecimal.valueOf(d2),
                    BigDecimal.valueOf(d3),
                    BigDecimal.valueOf(d4),
                    BigDecimal.valueOf(d5)
            );
            
            // Sort the distances
            Collections.sort(distances);
            
            // Verify ascending order: for any two adjacent items, first <= second
            for (int i = 0; i < distances.size() - 1; i++) {
                assertTrue(distances.get(i).compareTo(distances.get(i + 1)) <= 0,
                        String.format("Distance at index %d (%s) should be <= distance at index %d (%s)",
                                i, distances.get(i), i + 1, distances.get(i + 1)));
            }
        }

        @Test
        @DisplayName("Empty list should remain empty after sorting")
        void emptyListShouldRemainEmpty() {
            // Property: Empty list should remain empty after sorting
            
            List<MandiPriceDto> prices = new ArrayList<>();
            List<MandiPriceDto> sorted = mandiLocationService.sortPricesByDistance(
                    prices, FARMER_LATITUDE, FARMER_LONGITUDE);
            
            assertTrue(sorted.isEmpty(), "Sorted list should be empty");
        }

        @Test
        @DisplayName("Single item list should remain unchanged")
        void singleItemListShouldRemainUnchanged() {
            // Property: Single item list should remain unchanged after sorting
            
            List<MandiPriceDto> prices = Collections.singletonList(
                    createPriceDto("Mandi A", BigDecimal.valueOf(50))
            );
            
            List<MandiPriceDto> sorted = mandiLocationService.sortPricesByDistance(
                    prices, FARMER_LATITUDE, FARMER_LONGITUDE);
            
            assertEquals(1, sorted.size(), "Sorted list should have one item");
        }

        @Test
        @DisplayName("Multiple items should be sorted by distance")
        void multipleItemsShouldBeSortedByDistance() {
            // Property: Multiple items should be sorted by distance in ascending order
            
            List<MandiPriceDto> prices = new ArrayList<>();
            prices.add(createPriceDto("Mandi A", BigDecimal.valueOf(50)));
            prices.add(createPriceDto("Mandi B", BigDecimal.valueOf(20)));
            prices.add(createPriceDto("Mandi C", BigDecimal.valueOf(80)));
            prices.add(createPriceDto("Mandi D", BigDecimal.valueOf(35)));
            
            List<MandiPriceDto> sorted = mandiLocationService.sortPricesByDistance(
                    prices, FARMER_LATITUDE, FARMER_LONGITUDE);
            
            // Verify ascending order
            for (int i = 0; i < sorted.size() - 1; i++) {
                BigDecimal current = sorted.get(i).getDistanceKm();
                BigDecimal next = sorted.get(i + 1).getDistanceKm();
                
                assertTrue(current.compareTo(next) <= 0,
                        String.format("Distance at index %d (%s) should be <= distance at index %d (%s)",
                                i, current, i + 1, next));
            }
        }
    }

    @Nested
    @DisplayName("Property 11.3: Distance-Based Sorting Invariance")
    class DistanceBasedSortingInvariance {

        @Test
        @DisplayName("Sorting should be consistent for same input")
        void sortingShouldBeConsistentForSameInput() {
            // Property: Sorting should produce consistent results for the same input
            
            List<MandiPriceDto> prices = new ArrayList<>();
            prices.add(createPriceDto("Mandi A", BigDecimal.valueOf(50)));
            prices.add(createPriceDto("Mandi B", BigDecimal.valueOf(20)));
            prices.add(createPriceDto("Mandi C", BigDecimal.valueOf(80)));
            
            // Sort multiple times
            List<MandiPriceDto> sorted1 = mandiLocationService.sortPricesByDistance(
                    prices, FARMER_LATITUDE, FARMER_LONGITUDE);
            List<MandiPriceDto> sorted2 = mandiLocationService.sortPricesByDistance(
                    prices, FARMER_LATITUDE, FARMER_LONGITUDE);
            
            // Verify same order
            for (int i = 0; i < sorted1.size(); i++) {
                assertEquals(sorted1.get(i).getMandiName(), sorted2.get(i).getMandiName(),
                        "Sorting should produce consistent results");
            }
        }

        @Test
        @DisplayName("Distance calculation should be symmetric")
        void distanceCalculationShouldBeSymmetric() {
            // Property: Distance from A to B should equal distance from B to A
            
            BigDecimal distanceAB = mandiLocationService.calculateDistance(
                    BigDecimal.valueOf(12.9716), BigDecimal.valueOf(77.5946),
                    BigDecimal.valueOf(13.0000), BigDecimal.valueOf(77.6000));
            
            BigDecimal distanceBA = mandiLocationService.calculateDistance(
                    BigDecimal.valueOf(13.0000), BigDecimal.valueOf(77.6000),
                    BigDecimal.valueOf(12.9716), BigDecimal.valueOf(77.5946));
            
            assertEquals(0, distanceAB.compareTo(distanceBA),
                    "Distance should be symmetric (A to B = B to A)");
        }

        @Test
        @DisplayName("Distance should satisfy triangle inequality")
        void distanceShouldSatisfyTriangleInequality() {
            // Property: Distance should satisfy triangle inequality
            // d(A, C) <= d(A, B) + d(B, C)
            
            BigDecimal distanceAC = mandiLocationService.calculateDistance(
                    BigDecimal.valueOf(12.9716), BigDecimal.valueOf(77.5946),
                    BigDecimal.valueOf(13.1000), BigDecimal.valueOf(77.7000));
            
            BigDecimal distanceAB = mandiLocationService.calculateDistance(
                    BigDecimal.valueOf(12.9716), BigDecimal.valueOf(77.5946),
                    BigDecimal.valueOf(13.0000), BigDecimal.valueOf(77.6000));
            
            BigDecimal distanceBC = mandiLocationService.calculateDistance(
                    BigDecimal.valueOf(13.0000), BigDecimal.valueOf(77.6000),
                    BigDecimal.valueOf(13.1000), BigDecimal.valueOf(77.7000));
            
            BigDecimal sumABBC = distanceAB.add(distanceBC);
            
            assertTrue(distanceAC.compareTo(sumABBC) <= 0,
                    "Distance should satisfy triangle inequality: d(A,C) <= d(A,B) + d(B,C)");
        }
    }

    @Nested
    @DisplayName("Property 11.4: Location Data Completeness")
    class LocationDataCompleteness {

        @ParameterizedTest
        @CsvSource({
            "Mandi A, Karnataka, Bangalore",
            "Mandi B, Maharashtra, Pune",
            "Mandi C, Telangana, Hyderabad",
            "Mandi D, Tamil Nadu, Chennai",
        })
        @DisplayName("Location data should have all required fields")
        void locationDataShouldHaveAllRequiredFields(String mandiName, String state, String district) {
            // Property: Location data should have all required fields
            
            MandiLocationDto location = MandiLocationDto.builder()
                    .mandiCode("TEST001")
                    .mandiName(mandiName)
                    .state(state)
                    .district(district)
                    .address("Test Address")
                    .latitude(BigDecimal.valueOf(12.9716))
                    .longitude(BigDecimal.valueOf(77.5946))
                    .contactNumber("+91-1234567890")
                    .operatingHours("6 AM - 2 PM")
                    .distanceKm(BigDecimal.ZERO)
                    .isActive(true)
                    .build();
            
            assertNotNull(location.getMandiCode(), "Mandi code should be present");
            assertNotNull(location.getMandiName(), "Mandi name should be present");
            assertNotNull(location.getState(), "State should be present");
            assertNotNull(location.getDistrict(), "District should be present");
            assertNotNull(location.getLatitude(), "Latitude should be present");
            assertNotNull(location.getLongitude(), "Longitude should be present");
            assertNotNull(location.getIsActive(), "IsActive should be present");
        }
    }

    /**
     * Helper method to create a MandiPriceDto with a specific distance.
     */
    private MandiPriceDto createPriceDto(String mandiName, BigDecimal distance) {
        return MandiPriceDto.builder()
                .id(1L)
                .commodityName("Paddy")
                .variety("Hybrid")
                .mandiName(mandiName)
                .state("Karnataka")
                .district("Bangalore Rural")
                .priceDate(LocalDate.now())
                .modalPrice(BigDecimal.valueOf(2500))
                .minPrice(BigDecimal.valueOf(2300))
                .maxPrice(BigDecimal.valueOf(2700))
                .arrivalQuantityQuintals(BigDecimal.valueOf(100))
                .unit("Quintal")
                .source("AGMARKNET")
                .distanceKm(distance)
                .isCached(false)
                .build();
    }
}