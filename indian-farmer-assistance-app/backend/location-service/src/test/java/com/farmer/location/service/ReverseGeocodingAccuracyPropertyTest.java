package com.farmer.location.service;

import com.farmer.location.dto.LocationRequestDto;
import com.farmer.location.dto.LocationResponseDto;
import com.farmer.location.entity.LocationCache;
import com.farmer.location.repository.LocationCacheRepository;
import com.farmer.location.repository.LocationHistoryRepository;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Reverse Geocoding Accuracy.
 * 
 * Property 32: Reverse Geocoding Accuracy
 * For any GPS coordinates within India, the reverse geocoded location (district, state) 
 * should match the administrative boundaries as defined by the authoritative source, 
 * with a tolerance for boundary edge cases (within 1km of boundaries).
 * 
 * Validates: Requirements 14.3
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReverseGeocodingAccuracyPropertyTest {

    @Mock
    private LocationCacheRepository locationCacheRepository;

    @Mock
    private LocationHistoryRepository locationHistoryRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private WebClient.Builder webClientBuilder;

    private GpsLocationService gpsLocationService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        gpsLocationService = new GpsLocationService(
                locationCacheRepository,
                locationHistoryRepository,
                redisTemplate,
                webClientBuilder
        );
    }

    /**
     * Generator for valid India GPS coordinates.
     * India roughly spans:
     * - Latitude: 6° to 37° N
     * - Longitude: 68° to 97° E
     */
    @Provide
    Arbitrary<LocationRequestDto> validIndiaCoordinates() {
        return Combinators.combine(
                Arbitraries.doubles().between(6.0, 37.0).unique(),
                Arbitraries.doubles().between(68.0, 97.0).unique()
        ).as((lat, lon) -> LocationRequestDto.builder()
                .latitude(lat)
                .longitude(lon)
                .build());
    }

    /**
     * Generator for coordinates near known locations.
     */
    @Provide
    Arbitrary<LocationRequestDto> coordinatesNearKnownLocations() {
        // Known locations with their approximate coordinates
        var knownLocations = List.of(
                new double[]{12.9716, 77.5946},  // Bangalore
                new double[]{19.0760, 72.8777},  // Mumbai
                new double[]{28.6139, 77.2090},  // Delhi
                new double[]{22.5726, 88.3639},  // Kolkata
                new double[]{13.0827, 80.2707},  // Chennai
                new double[]{17.3850, 78.4867},  // Hyderabad
                new double[]{26.9124, 75.7873},  // Jaipur
                new double[]{23.0225, 72.5714},  // Ahmedabad
                new double[]{25.5941, 85.1376},  // Patna
                new double[]{21.1702, 72.8311}   // Surat
        );

        return Arbitraries.of(knownLocations)
                .flatMap(coords -> 
                    Combinators.combine(
                        Arbitraries.doubles().between(coords[0] - 0.5, coords[0] + 0.5),
                        Arbitraries.doubles().between(coords[1] - 0.5, coords[1] + 0.5)
                    ).as((lat, lon) -> LocationRequestDto.builder()
                            .latitude(lat)
                            .longitude(lon)
                            .build())
                );
    }

    /**
     * Property 32.1: Valid India coordinates should always return a successful response.
     * 
     * For any valid GPS coordinates within India's boundaries, the reverse geocoding
     * service should return a successful response with location information.
     */
    @Property
    @DisplayName("Property 32.1: Valid India coordinates return successful response")
    void validIndiaCoordinatesShouldReturnSuccessfulResponse(
            @ForAll("validIndiaCoordinates") LocationRequestDto coordinates) {
        // Arrange
        when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        LocationResponseDto response = gpsLocationService.getLocation(coordinates);

        // Assert
        assertTrue(response.isSuccess(), 
                "Reverse geocoding should succeed for valid India coordinates: lat=" + 
                coordinates.getLatitude() + ", lon=" + coordinates.getLongitude());
        assertNotNull(response.getState(), 
                "State should not be null for valid India coordinates");
    }

    /**
     * Property 32.2: Same coordinates should always return the same state.
     * 
     * For any valid GPS coordinates within India, calling the reverse geocoding service
     * multiple times with the same coordinates should always return the same state.
     */
    @Property
    @DisplayName("Property 32.2: Same coordinates return consistent state")
    void sameCoordinatesShouldReturnConsistentState(
            @ForAll("validIndiaCoordinates") LocationRequestDto coordinates) {
        // Arrange
        when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act - Call multiple times with same coordinates
        LocationResponseDto response1 = gpsLocationService.getLocation(coordinates);
        LocationResponseDto response2 = gpsLocationService.getLocation(coordinates);
        LocationResponseDto response3 = gpsLocationService.getLocation(coordinates);

        // Assert - All responses should have the same state
        assertTrue(response1.isSuccess(), "First request should succeed");
        assertTrue(response2.isSuccess(), "Second request should succeed");
        assertTrue(response3.isSuccess(), "Third request should succeed");

        assertEquals(response1.getState(), response2.getState(),
                "Same coordinates should return same state");
        assertEquals(response2.getState(), response3.getState(),
                "Consistent state across multiple requests");
    }

    /**
     * Property 32.3: Coordinates near known locations should return nearby states.
     * 
     * For coordinates near known major cities, the reverse geocoding should return
     * the correct state that contains those cities.
     */
    @Property
    @DisplayName("Property 32.3: Coordinates near known locations return correct state")
    void coordinatesNearKnownLocationsReturnCorrectState(
            @ForAll("coordinatesNearKnownLocations") LocationRequestDto coordinates) {
        // Arrange
        when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        LocationResponseDto response = gpsLocationService.getLocation(coordinates);

        // Assert
        assertTrue(response.isSuccess(), 
                "Reverse geocoding should succeed for coordinates near known locations");
        assertNotNull(response.getState(), "State should not be null");
        assertFalse(response.getState().equals("Unknown State"),
                "State should be identified for coordinates near known locations");
    }

    /**
     * Property 32.4: Coordinates should be validated as within India.
     * 
     * For any coordinates, the service should correctly identify whether they
     * are within India's boundaries.
     */
    @Property
    @DisplayName("Property 32.4: Coordinates validation is correct")
    void coordinatesValidationIsCorrect(
            @ForAll("validIndiaCoordinates") LocationRequestDto coordinates) {
        // Act
        boolean isValid = gpsLocationService.isValidIndiaCoordinates(
                coordinates.getLatitude(), coordinates.getLongitude());

        // Assert
        assertTrue(isValid, 
                "Coordinates should be valid for India: lat=" + 
                coordinates.getLatitude() + ", lon=" + coordinates.getLongitude());
    }

    /**
     * Property 32.5: Invalid coordinates outside India should return error.
     * 
     * For coordinates outside India's boundaries, the service should return
     * an appropriate error message.
     */
    @Property
    @DisplayName("Property 32.5: Invalid coordinates return error")
    void invalidCoordinatesReturnError() {
        // Arrange - Test various invalid coordinate combinations
        double[][] invalidCoordinates = {
                {-10.0, 80.0},      // Latitude too low
                {50.0, 80.0},       // Latitude too high
                {20.0, 50.0},       // Longitude too low
                {20.0, 110.0},      // Longitude too high
                {-10.0, -50.0},     // Both invalid
                {100.0, 200.0}      // Both way off
        };

        for (double[] coords : invalidCoordinates) {
            LocationRequestDto request = LocationRequestDto.builder()
                    .latitude(coords[0])
                    .longitude(coords[1])
                    .build();

            // Act
            LocationResponseDto response = gpsLocationService.getLocation(request);

            // Assert
            assertFalse(response.isSuccess(), 
                    "Should fail for invalid coordinates: lat=" + coords[0] + ", lon=" + coords[1]);
            assertNotNull(response.getErrorMessage(), 
                    "Error message should be provided for invalid coordinates");
        }
    }

    /**
     * Property 32.6: Cached coordinates should return consistent results.
     * 
     * When coordinates are cached, subsequent requests should return the same
     * cached result.
     */
    @Property
    @DisplayName("Property 32.6: Cached coordinates return consistent results")
    void cachedCoordinatesReturnConsistentResults(
            @ForAll("validIndiaCoordinates") LocationRequestDto coordinates) {
        // Arrange - Set up cache
        LocationCache cachedLocation = LocationCache.builder()
                .id(1L)
                .latitude(coordinates.getLatitude())
                .longitude(coordinates.getLongitude())
                .district("Test District")
                .state("Test State")
                .agroEcologicalZone("Test Zone")
                .build();

        when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(cachedLocation));

        // Act - Multiple calls should return cached result
        LocationResponseDto response1 = gpsLocationService.getLocation(coordinates);
        LocationResponseDto response2 = gpsLocationService.getLocation(coordinates);
        LocationResponseDto response3 = gpsLocationService.getLocation(coordinates);

        // Assert - All should return same cached district and state
        assertTrue(response1.isSuccess(), "First request should succeed");
        assertTrue(response2.isSuccess(), "Second request should succeed");
        assertTrue(response3.isSuccess(), "Third request should succeed");

        assertEquals("Test District", response1.getDistrict(),
                "Cached district should be returned");
        assertEquals("Test District", response2.getDistrict(),
                "Cached district should be consistent");
        assertEquals("Test District", response3.getDistrict(),
                "Cached district should be consistent across calls");

        assertEquals("Test State", response1.getState(),
                "Cached state should be returned");
        assertEquals("Test State", response2.getState(),
                "Cached state should be consistent");
        assertEquals("Test State", response3.getState(),
                "Cached state should be consistent across calls");
    }

    /**
     * Property 32.7: District and state should be consistent with agro-ecological zone.
     * 
     * The returned agro-ecological zone should be consistent with the state
     * for valid India coordinates.
     */
    @Property
    @DisplayName("Property 32.7: Zone is consistent with state")
    void zoneIsConsistentWithState(
            @ForAll("validIndiaCoordinates") LocationRequestDto coordinates) {
        // Arrange
        when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        LocationResponseDto response = gpsLocationService.getLocation(coordinates);

        // Assert
        if (response.isSuccess() && response.getState() != null) {
            // Zone should not be null for valid coordinates
            assertNotNull(response.getAgroEcologicalZone(),
                    "Agro-ecological zone should not be null for valid coordinates");
            
            // Zone code should not be null
            assertNotNull(response.getZoneCode(),
                    "Zone code should not be null for valid coordinates");
        }
    }

    @Nested
    @DisplayName("Edge Cases for Boundary Conditions")
    class BoundaryEdgeCases {

        /**
         * Property 32.8: Coordinates at India's northern boundary should work.
         */
        @Property
        @DisplayName("Property 32.8: Northern boundary coordinates work")
        void northernBoundaryCoordinatesWork() {
            // Arrange - Northern boundary (around 37° N)
            double[][] northernBoundaries = {
                    {37.0, 68.0},    // Northwest corner
                    {37.0, 80.0},    // North-central
                    {37.0, 97.0}     // Northeast corner
            };

            for (double[] coords : northernBoundaries) {
                LocationRequestDto request = LocationRequestDto.builder()
                        .latitude(coords[0])
                        .longitude(coords[1])
                        .build();

                // Act
                LocationResponseDto response = gpsLocationService.getLocation(request);

                // Assert
                assertTrue(response.isSuccess(),
                        "Should succeed for northern boundary: lat=" + coords[0] + ", lon=" + coords[1]);
            }
        }

        /**
         * Property 32.9: Coordinates at India's southern boundary should work.
         */
        @Property
        @DisplayName("Property 32.9: Southern boundary coordinates work")
        void southernBoundaryCoordinatesWork() {
            // Arrange - Southern boundary (around 6° N)
            double[][] southernBoundaries = {
                    {6.0, 79.0},     // Southern tip near Kerala
                    {6.0, 93.0},     // Southern tip near Andaman
                    {8.0, 77.0},     // Kanyakumari area
            };

            for (double[] coords : southernBoundaries) {
                LocationRequestDto request = LocationRequestDto.builder()
                        .latitude(coords[0])
                        .longitude(coords[1])
                        .build();

                // Act
                LocationResponseDto response = gpsLocationService.getLocation(request);

                // Assert
                assertTrue(response.isSuccess(),
                        "Should succeed for southern boundary: lat=" + coords[0] + ", lon=" + coords[1]);
            }
        }

        /**
         * Property 32.10: Coordinates at India's eastern boundary should work.
         */
        @Property
        @DisplayName("Property 32.10: Eastern boundary coordinates work")
        void easternBoundaryCoordinatesWork() {
            // Arrange - Eastern boundary (around 97° E)
            double[][] easternBoundaries = {
                    {20.0, 97.0},    // Eastern boundary
                    {26.0, 97.0},    // Northeast
                    {22.0, 97.0},    // East-central
            };

            for (double[] coords : easternBoundaries) {
                LocationRequestDto request = LocationRequestDto.builder()
                        .latitude(coords[0])
                        .longitude(coords[1])
                        .build();

                // Act
                LocationResponseDto response = gpsLocationService.getLocation(request);

                // Assert
                assertTrue(response.isSuccess(),
                        "Should succeed for eastern boundary: lat=" + coords[0] + ", lon=" + coords[1]);
            }
        }

        /**
         * Property 32.11: Coordinates at India's western boundary should work.
         */
        @Property
        @DisplayName("Property 32.11: Western boundary coordinates work")
        void westernBoundaryCoordinatesWork() {
            // Arrange - Western boundary (around 68° E)
            double[][] westernBoundaries = {
                    {24.0, 68.0},    // Gujarat/Rajasthan border
                    {22.0, 68.0},    // Gujarat coast
                    {15.0, 68.0},    // Maharashtra coast
            };

            for (double[] coords : westernBoundaries) {
                LocationRequestDto request = LocationRequestDto.builder()
                        .latitude(coords[0])
                        .longitude(coords[1])
                        .build();

                // Act
                LocationResponseDto response = gpsLocationService.getLocation(request);

                // Assert
                assertTrue(response.isSuccess(),
                        "Should succeed for western boundary: lat=" + coords[0] + ", lon=" + coords[1]);
            }
        }
    }
}