package com.farmer.location.service;

import com.farmer.location.dto.*;
import com.farmer.location.entity.GovernmentBody;
import com.farmer.location.entity.GovernmentBody.GovernmentBodyType;
import com.farmer.location.entity.LocationHistory;
import com.farmer.location.repository.GovernmentBodyRepository;
import com.farmer.location.repository.LocationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for location services.
 * 
 * Tests cover:
 * - GPS coordinate retrieval
 * - Reverse geocoding with various coordinates
 * - Location change detection
 * - Government body lookup and distance calculation
 * 
 * Validates: Requirements 14.1, 14.3, 14.4, 7.2
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocationServicesUnitTest {

    @Mock
    private LocationCacheRepository locationCacheRepository;

    @Mock
    private LocationHistoryRepository locationHistoryRepository;

    @Mock
    private GovernmentBodyRepository governmentBodyRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private WebClient.Builder webClientBuilder;

    private GpsLocationService gpsLocationService;
    private LocationChangeDetectionService locationChangeDetectionService;
    private GovernmentBodyLocatorService governmentBodyLocatorService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        gpsLocationService = new GpsLocationService(
                locationCacheRepository,
                locationHistoryRepository,
                redisTemplate,
                webClientBuilder
        );

        locationChangeDetectionService = new LocationChangeDetectionService(
                locationHistoryRepository,
                gpsLocationService
        );

        governmentBodyLocatorService = new GovernmentBodyLocatorService(
                governmentBodyRepository,
                locationChangeDetectionService
        );
    }

    @Nested
    @DisplayName("GPS Coordinate Retrieval Tests")
    class GpsCoordinateRetrievalTests {

        @Test
        @DisplayName("Test getLocation with valid GPS coordinates returns success")
        void testGetLocationWithValidGpsCoordinates() {
            // Arrange
            LocationRequestDto request = LocationRequestDto.builder()
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .build();

            when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                    .thenReturn(Optional.empty());
            when(valueOperations.get(anyString())).thenReturn(null);

            // Act
            LocationResponseDto response = gpsLocationService.getLocation(request);

            // Assert
            assertTrue(response.isSuccess());
            assertEquals(12.9716, response.getLatitude());
            assertEquals(77.5946, response.getLongitude());
            assertNotNull(response.getState());
            assertNotNull(response.getDistrict());
        }

        @Test
        @DisplayName("Test getLocation with invalid coordinates returns error")
        void testGetLocationWithInvalidCoordinates() {
            // Arrange
            LocationRequestDto request = LocationRequestDto.builder()
                    .latitude(-10.0)
                    .longitude(80.0)
                    .build();

            // Act
            LocationResponseDto response = gpsLocationService.getLocation(request);

            // Assert
            assertFalse(response.isSuccess());
            assertNotNull(response.getErrorMessage());
            assertTrue(response.getErrorMessage().contains("outside India"));
        }

        @Test
        @DisplayName("Test getLocation with null coordinates returns error")
        void testGetLocationWithNullCoordinates() {
            // Arrange
            LocationRequestDto request = LocationRequestDto.builder()
                    .latitude(null)
                    .longitude(null)
                    .build();

            // Act
            LocationResponseDto response = gpsLocationService.getLocation(request);

            // Assert
            assertFalse(response.isSuccess());
            assertNotNull(response.getErrorMessage());
        }

        @Test
        @DisplayName("Test getLocation with district and state returns location info")
        void testGetLocationWithDistrictAndState() {
            // Arrange
            LocationRequestDto request = LocationRequestDto.builder()
                    .district("Bangalore Rural")
                    .state("Karnataka")
                    .build();

            when(locationCacheRepository.findByDistrictAndState(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(valueOperations.get(anyString())).thenReturn(null);

            // Act
            LocationResponseDto response = gpsLocationService.getLocation(request);

            // Assert
            assertTrue(response.isSuccess());
            assertEquals("Bangalore Rural", response.getDistrict());
            assertEquals("Karnataka", response.getState());
            assertEquals("MANUAL", response.getLocationSource());
        }

        @Test
        @DisplayName("Test isValidIndiaCoordinates with valid coordinates")
        void testIsValidIndiaCoordinatesWithValidCoordinates() {
            // Act & Assert
            assertTrue(gpsLocationService.isValidIndiaCoordinates(12.9716, 77.5946));
            assertTrue(gpsLocationService.isValidIndiaCoordinates(28.6139, 77.2090));
            assertTrue(gpsLocationService.isValidIndiaCoordinates(6.0, 68.0));
            assertTrue(gpsLocationService.isValidIndiaCoordinates(37.0, 97.0));
        }

        @Test
        @DisplayName("Test isValidIndiaCoordinates with invalid coordinates")
        void testIsValidIndiaCoordinatesWithInvalidCoordinates() {
            // Act & Assert
            assertFalse(gpsLocationService.isValidIndiaCoordinates(-10.0, 80.0));
            assertFalse(gpsLocationService.isValidIndiaCoordinates(50.0, 80.0));
            assertFalse(gpsLocationService.isValidIndiaCoordinates(20.0, 50.0));
            assertFalse(gpsLocationService.isValidIndiaCoordinates(20.0, 110.0));
            assertFalse(gpsLocationService.isValidIndiaCoordinates(null, 80.0));
            assertFalse(gpsLocationService.isValidIndiaCoordinates(20.0, null));
        }
    }

    @Nested
    @DisplayName("Reverse Geocoding Tests")
    class ReverseGeocodingTests {

        @Test
        @DisplayName("Test reverse geocoding returns correct state for Karnataka coordinates")
        void testReverseGeocodingReturnsCorrectStateForKarnataka() {
            // Arrange
            LocationRequestDto request = LocationRequestDto.builder()
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .build();

            when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                    .thenReturn(Optional.empty());
            when(valueOperations.get(anyString())).thenReturn(null);

            // Act
            LocationResponseDto response = gpsLocationService.getLocation(request);

            // Assert
            assertTrue(response.isSuccess());
            assertEquals("Karnataka", response.getState());
            assertNotNull(response.getAgroEcologicalZone());
            assertNotNull(response.getZoneCode());
        }

        @Test
        @DisplayName("Test reverse geocoding returns correct zone for Maharashtra")
        void testReverseGeocodingReturnsCorrectZoneForMaharashtra() {
            // Arrange
            LocationRequestDto request = LocationRequestDto.builder()
                    .latitude(19.0760)
                    .longitude(72.8777)
                    .build();

            when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                    .thenReturn(Optional.empty());
            when(valueOperations.get(anyString())).thenReturn(null);

            // Act
            LocationResponseDto response = gpsLocationService.getLocation(request);

            // Assert
            assertTrue(response.isSuccess());
            assertEquals("Maharashtra", response.getState());
            assertEquals("Western Plateau and Hills Region", response.getAgroEcologicalZone());
        }

        @Test
        @DisplayName("Test reverse geocoding returns cached location when available")
        void testReverseGeocodingReturnsCachedLocation() {
            // Arrange
            LocationRequestDto request = LocationRequestDto.builder()
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .build();

            com.farmer.location.entity.LocationCache cachedLocation = 
                    com.farmer.location.entity.LocationCache.builder()
                            .id(1L)
                            .latitude(12.9716)
                            .longitude(77.5946)
                            .district("Cached District")
                            .state("Cached State")
                            .agroEcologicalZone("Cached Zone")
                            .build();

            when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                    .thenReturn(Optional.of(cachedLocation));

            // Act
            LocationResponseDto response = gpsLocationService.getLocation(request);

            // Assert
            assertTrue(response.isSuccess());
            assertEquals("Cached District", response.getDistrict());
            assertEquals("Cached State", response.getState());
        }
    }

    @Nested
    @DisplayName("Location Change Detection Tests")
    class LocationChangeDetectionTests {

        @Test
        @DisplayName("Test detectLocationChange with first location records successfully")
        void testDetectLocationChangeWithFirstLocation() {
            // Arrange
            Long userId = 1L;
            LocationRequestDto request = LocationRequestDto.builder()
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .build();

            when(locationHistoryRepository.findMostRecentByUserId(userId))
                    .thenReturn(Optional.empty());
            when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                    .thenReturn(Optional.empty());
            when(valueOperations.get(anyString())).thenReturn(null);

            // Act
            LocationChangeDto response = locationChangeDetectionService.detectLocationChange(userId, request);

            // Assert
            assertNotNull(response);
            assertEquals(userId, response.getUserId());
            assertFalse(response.isSignificantChange());
            assertEquals(0.0, response.getDistanceKm());
            assertNotNull(response.getSuggestedActions());
        }

        @Test
        @DisplayName("Test detectLocationChange detects significant change (>10km)")
        void testDetectLocationChangeDetectsSignificantChange() {
            // Arrange
            Long userId = 1L;
            
            // Previous location (Bangalore)
            LocationHistory previousLocation = LocationHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .district("Bangalore Rural")
                    .state("Karnataka")
                    .build();

            // New location (Mumbai - approximately 980 km away)
            LocationRequestDto newRequest = LocationRequestDto.builder()
                    .latitude(19.0760)
                    .longitude(72.8777)
                    .build();

            when(locationHistoryRepository.findMostRecentByUserId(userId))
                    .thenReturn(Optional.of(previousLocation));
            when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                    .thenReturn(Optional.empty());
            when(valueOperations.get(anyString())).thenReturn(null);
            when(locationHistoryRepository.save(any())).thenReturn(null);

            // Act
            LocationChangeDto response = locationChangeDetectionService.detectLocationChange(userId, newRequest);

            // Assert
            assertNotNull(response);
            assertTrue(response.isSignificantChange());
            assertTrue(response.getDistanceKm() > 10.0);
            assertTrue(response.shouldUpdateLocationDependentInfo());
        }

        @Test
        @DisplayName("Test detectLocationChange ignores small changes (<10km)")
        void testDetectLocationChangeIgnoresSmallChanges() {
            // Arrange
            Long userId = 1L;
            
            // Previous location
            LocationHistory previousLocation = LocationHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .district("Bangalore Rural")
                    .state("Karnataka")
                    .build();

            // New location (very close - 5 km away)
            LocationRequestDto newRequest = LocationRequestDto.builder()
                    .latitude(13.0)
                    .longitude(77.6)
                    .build();

            when(locationHistoryRepository.findMostRecentByUserId(userId))
                    .thenReturn(Optional.of(previousLocation));
            when(locationCacheRepository.findByCoordinates(anyDouble(), anyDouble()))
                    .thenReturn(Optional.empty());
            when(valueOperations.get(anyString())).thenReturn(null);
            when(locationHistoryRepository.save(any())).thenReturn(null);

            // Act
            LocationChangeDto response = locationChangeDetectionService.detectLocationChange(userId, newRequest);

            // Assert
            assertNotNull(response);
            assertFalse(response.isSignificantChange());
            assertTrue(response.getDistanceKm() < 10.0);
        }

        @Test
        @DisplayName("Test calculateDistance returns correct distance")
        void testCalculateDistanceReturnsCorrectDistance() {
            // Act - Distance from Bangalore to Mumbai
            double distance = locationChangeDetectionService.calculateDistance(
                    12.9716, 77.5946,  // Bangalore
                    19.0760, 72.8777   // Mumbai
            );

            // Assert - Should be approximately 980 km
            assertTrue(distance > 900.0);
            assertTrue(distance < 1100.0);
        }

        @Test
        @DisplayName("Test calculateDistance returns 0 for null coordinates")
        void testCalculateDistanceReturnsZeroForNullCoordinates() {
            // Act & Assert
            assertEquals(0.0, locationChangeDetectionService.calculateDistance(null, 77.0, 12.0, 77.0));
            assertEquals(0.0, locationChangeDetectionService.calculateDistance(12.0, null, 12.0, 77.0));
            assertEquals(0.0, locationChangeDetectionService.calculateDistance(12.0, 77.0, null, 77.0));
            assertEquals(0.0, locationChangeDetectionService.calculateDistance(12.0, 77.0, 12.0, null));
        }
    }

    @Nested
    @DisplayName("Government Body Lookup Tests")
    class GovernmentBodyLookupTests {

        @Test
        @DisplayName("Test searchNearbyGovernmentBodies returns bodies sorted by distance")
        void testSearchNearbyGovernmentBodiesReturnsBodiesSortedByDistance() {
            // Arrange
            GovernmentBodySearchRequestDto request = GovernmentBodySearchRequestDto.builder()
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .maxDistanceKm(100.0)
                    .build();

            GovernmentBody kvk1 = createMockGovernmentBody(1L, GovernmentBodyType.KVK, 
                    "KVK1", 12.9716, 77.5946, "Bangalore Rural", "Karnataka");
            GovernmentBody kvk2 = createMockGovernmentBody(2L, GovernmentBodyType.KVK, 
                    "KVK2", 13.0, 78.0, "Bangalore Urban", "Karnataka");
            GovernmentBody kvk3 = createMockGovernmentBody(3L, GovernmentBodyType.KVK, 
                    "KVK3", 14.0, 79.0, "Kolar", "Karnataka");

            when(governmentBodyRepository.findAllWithCoordinates())
                    .thenReturn(List.of(kvk1, kvk2, kvk3));

            // Act
            GovernmentBodySearchResponseDto response = 
                    governmentBodyLocatorService.searchNearbyGovernmentBodies(request);

            // Assert
            assertTrue(response.isSuccess());
            assertEquals(3, response.getTotalFound());
            assertEquals(3, response.getGovernmentBodies().size());
            
            // Verify sorted by distance
            List<GovernmentBodyDto> bodies = response.getGovernmentBodies();
            for (int i = 0; i < bodies.size() - 1; i++) {
                assertTrue(bodies.get(i).getDistanceKm() <= bodies.get(i + 1).getDistanceKm());
            }
        }

        @Test
        @DisplayName("Test searchNearbyGovernmentBodies filters by body type")
        void testSearchNearbyGovernmentBodiesFiltersByBodyType() {
            // Arrange
            GovernmentBodySearchRequestDto request = GovernmentBodySearchRequestDto.builder()
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .maxDistanceKm(100.0)
                    .bodyTypes(List.of(GovernmentBodyType.KVK))
                    .build();

            GovernmentBody kvk = createMockGovernmentBody(1L, GovernmentBodyType.KVK, 
                    "KVK1", 12.9716, 77.5946, "Bangalore Rural", "Karnataka");
            GovernmentBody districtOffice = createMockGovernmentBody(2L, 
                    GovernmentBodyType.DISTRICT_AGRICULTURE_OFFICE, 
                    "District Office", 13.0, 78.0, "Bangalore Urban", "Karnataka");

            when(governmentBodyRepository.findAllWithCoordinates())
                    .thenReturn(List.of(kvk, districtOffice));

            // Act
            GovernmentBodySearchResponseDto response = 
                    governmentBodyLocatorService.searchNearbyGovernmentBodies(request);

            // Assert
            assertTrue(response.isSuccess());
            assertEquals(1, response.getTotalFound());
            assertEquals(GovernmentBodyType.KVK, response.getGovernmentBodies().get(0).getBodyType());
        }

        @Test
        @DisplayName("Test getNearbyKvks returns KVKs within distance")
        void testGetNearbyKvksReturnsKvksWithinDistance() {
            // Arrange
            GovernmentBody kvk1 = createMockGovernmentBody(1L, GovernmentBodyType.KVK, 
                    "KVK1", 12.9716, 77.5946, "Bangalore Rural", "Karnataka");
            GovernmentBody kvk2 = createMockGovernmentBody(2L, GovernmentBodyType.KVK, 
                    "KVK2", 30.0, 78.0, "Dehradun", "Uttarakhand"); // Far away

            when(governmentBodyRepository.findAllWithCoordinates())
                    .thenReturn(List.of(kvk1, kvk2));

            // Act
            List<GovernmentBodyDto> kvks = governmentBodyLocatorService.getNearbyKvks(
                    12.9716, 77.5946, 50.0);

            // Assert
            assertEquals(1, kvks.size());
            assertEquals("KVK1", kvks.get(0).getName());
        }

        @Test
        @DisplayName("Test government body includes directions URL")
        void testGovernmentBodyIncludesDirectionsUrl() {
            // Arrange
            GovernmentBodySearchRequestDto request = GovernmentBodySearchRequestDto.builder()
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .maxDistanceKm(100.0)
                    .includeDirections(true)
                    .build();

            GovernmentBody kvk = createMockGovernmentBody(1L, GovernmentBodyType.KVK, 
                    "KVK1", 13.0, 78.0, "Bangalore Urban", "Karnataka");

            when(governmentBodyRepository.findAllWithCoordinates())
                    .thenReturn(List.of(kvk));

            // Act
            GovernmentBodySearchResponseDto response = 
                    governmentBodyLocatorService.searchNearbyGovernmentBodies(request);

            // Assert
            assertTrue(response.isSuccess());
            assertNotNull(response.getGovernmentBodies().get(0).getDirectionsUrl());
            assertTrue(response.getGovernmentBodies().get(0).getDirectionsUrl().contains("maps/dir"));
        }

        @Test
        @DisplayName("Test government body includes specialization areas for KVK")
        void testGovernmentBodyIncludesSpecializationAreasForKvk() {
            // Arrange
            GovernmentBodySearchRequestDto request = GovernmentBodySearchRequestDto.builder()
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .maxDistanceKm(100.0)
                    .includeSpecializationAreas(true)
                    .build();

            GovernmentBody kvk = createMockGovernmentBody(1L, GovernmentBodyType.KVK, 
                    "KVK1", 13.0, 78.0, "Bangalore Urban", "Karnataka");
            kvk.setSpecializationAreas("Horticulture, Soil Science, Plant Protection");

            when(governmentBodyRepository.findAllWithCoordinates())
                    .thenReturn(List.of(kvk));

            // Act
            GovernmentBodySearchResponseDto response = 
                    governmentBodyLocatorService.searchNearbyGovernmentBodies(request);

            // Assert
            assertTrue(response.isSuccess());
            GovernmentBodyDto kvkDto = response.getGovernmentBodies().get(0);
            assertNotNull(kvkDto.getSpecializationAreas());
            assertEquals(3, kvkDto.getSpecializationAreas().size());
            assertTrue(kvkDto.getSpecializationAreas().contains("Horticulture"));
        }

        private GovernmentBody createMockGovernmentBody(
                Long id, GovernmentBodyType type, String name,
                Double lat, Double lon, String district, String state) {
            return GovernmentBody.builder()
                    .id(id)
                    .bodyType(type)
                    .name(name)
                    .address("Test Address")
                    .district(district)
                    .state(state)
                    .latitude(lat)
                    .longitude(lon)
                    .contactNumber("1234567890")
                    .email("test@example.com")
                    .isActive(true)
                    .build();
        }
    }

    @Nested
    @DisplayName("Location History Tests")
    class LocationHistoryTests {

        @Test
        @DisplayName("Test recordLocationHistory saves history record")
        void testRecordLocationHistorySavesRecord() {
            // Arrange
            Long userId = 1L;
            LocationResponseDto location = LocationResponseDto.builder()
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .district("Bangalore Rural")
                    .state("Karnataka")
                    .locationSource("GPS")
                    .build();

            when(locationHistoryRepository.save(any())).thenReturn(null);

            // Act
            gpsLocationService.recordLocationHistory(userId, location, 5.0);

            // Assert
            verify(locationHistoryRepository).save(argThat(history ->
                    history.getUserId().equals(userId) &&
                    history.getLatitude().equals(12.9716) &&
                    history.getLongitude().equals(77.5946) &&
                    history.getDistanceFromLastKm().equals(5.0) &&
                    history.getIsSignificantChange().equals(false)
            ));
        }

        @Test
        @DisplayName("Test getLastKnownLocation returns last location")
        void testGetLastKnownLocationReturnsLastLocation() {
            // Arrange
            Long userId = 1L;
            LocationHistory history = LocationHistory.builder()
                    .id(1L)
                    .userId(userId)
                    .latitude(12.9716)
                    .longitude(77.5946)
                    .district("Bangalore Rural")
                    .state("Karnataka")
                    .locationSource("GPS")
                    .build();

            when(locationHistoryRepository.findMostRecentByUserId(userId))
                    .thenReturn(Optional.of(history));

            // Act
            var result = gpsLocationService.getLastKnownLocation(userId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(12.9716, result.get().getLatitude());
            assertEquals(77.5946, result.get().getLongitude());
        }

        @Test
        @DisplayName("Test getLastKnownLocation returns empty for new user")
        void testGetLastKnownLocationReturnsEmptyForNewUser() {
            // Arrange
            Long userId = 999L;
            when(locationHistoryRepository.findMostRecentByUserId(userId))
                    .thenReturn(Optional.empty());

            // Act
            var result = gpsLocationService.getLastKnownLocation(userId);

            // Assert
            assertTrue(result.isEmpty());
        }
    }
}