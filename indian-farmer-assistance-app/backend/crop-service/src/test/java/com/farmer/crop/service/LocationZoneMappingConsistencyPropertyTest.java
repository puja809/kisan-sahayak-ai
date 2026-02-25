package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.AgroEcologicalZone;
import com.farmer.crop.entity.DistrictZoneMapping;
import com.farmer.crop.repository.AgroEcologicalZoneRepository;
import com.farmer.crop.repository.DistrictZoneMappingRepository;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Location to Zone Mapping Consistency.
 * 
 * Property 5: Location to Zone Mapping Consistency
 * For any valid GPS coordinates within India, the system should consistently 
 * map the same coordinates to the same agro-ecological zone across multiple requests.
 * 
 * Validates: Requirements 2.1
 */
class LocationZoneMappingConsistencyPropertyTest {

    private AgroEcologicalZoneService zoneService;
    private AgroEcologicalZone sampleZone;

    @BeforeEach
    void setUp() {
        AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
        DistrictZoneMappingRepository districtMappingRepository = mock(DistrictZoneMappingRepository.class);
        
        sampleZone = AgroEcologicalZone.builder()
                .id(1L)
                .zoneCode("AEZ-05")
                .zoneName("Upper Gangetic Plain Region")
                .description("Semi-arid subtropical climate with hot dry summers")
                .climateType("Semi-Arid Subtropical")
                .rainfallRange("700-1200")
                .temperatureRange("18-40")
                .soilTypes("Alluvial soils, desert soils")
                .suitableCrops("Wheat, Rice, Sugarcane, Cotton")
                .kharifSuitability("Rice, Cotton, Maize")
                .rabiSuitability("Wheat, Mustard, Potato")
                .zaidSuitability("Vegetables, Groundnut")
                .latitudeRange("24.0-30.0")
                .longitudeRange("75.0-85.0")
                .statesCovered("Uttar Pradesh (west), Punjab, Haryana")
                .isActive(true)
                .build();
        
        zoneService = new AgroEcologicalZoneService(zoneRepository, districtMappingRepository);
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
     * Generator for valid district/state combinations.
     */
    @Provide
    Arbitrary<LocationRequestDto> validDistrictStateCombinations() {
        return Combinators.combine(
                Arbitraries.of("Lucknow", "Kanpur", "Agra", "Varanasi", "Allahabad"),
                Arbitraries.of("Uttar Pradesh", "Madhya Pradesh", "Maharashtra", "Karnataka", "Tamil Nadu")
        ).as((district, state) -> LocationRequestDto.builder()
                .district(district)
                .state(state)
                .build());
    }

    /**
     * Property 5.1: Same GPS coordinates should always return the same zone.
     * 
     * For any valid GPS coordinates within India, calling the zone mapping service
     * multiple times with the same coordinates should always return the same zone.
     */
    @Property
    void sameGpsCoordinatesShouldMapToSameZone(@ForAll("validIndiaCoordinates") LocationRequestDto coordinates) {
        // Arrange
        AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
        DistrictZoneMappingRepository districtMappingRepository = mock(DistrictZoneMappingRepository.class);
        
        DistrictZoneMapping mapping = DistrictZoneMapping.builder()
                .id(1L)
                .districtName("Test District")
                .state("Test State")
                .zone(sampleZone)
                .latitude(coordinates.getLatitude())
                .longitude(coordinates.getLongitude())
                .region("Central")
                .isVerified(true)
                .dataSource("ICAR")
                .isActive(true)
                .build();

        when(districtMappingRepository.findNearestByCoordinates(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(mapping));

        AgroEcologicalZoneService localZoneService = new AgroEcologicalZoneService(zoneRepository, districtMappingRepository);

        // Act - Call the service multiple times with the same coordinates
        ZoneLookupResponseDto response1 = localZoneService.getZoneForLocation(coordinates);
        ZoneLookupResponseDto response2 = localZoneService.getZoneForLocation(coordinates);
        ZoneLookupResponseDto response3 = localZoneService.getZoneForLocation(coordinates);

        // Assert - All responses should be successful and return the same zone
        assertTrue(response1.isSuccess(), "First request should succeed");
        assertTrue(response2.isSuccess(), "Second request should succeed");
        assertTrue(response3.isSuccess(), "Third request should succeed");

        assertNotNull(response1.getZone(), "First response should have a zone");
        assertNotNull(response2.getZone(), "Second response should have a zone");
        assertNotNull(response3.getZone(), "Third response should have a zone");

        assertEquals(response1.getZone().getZoneCode(), response2.getZone().getZoneCode(),
                "Same coordinates should return same zone code");
        assertEquals(response1.getZone().getZoneName(), response2.getZone().getZoneName(),
                "Same coordinates should return same zone name");
        assertEquals(response2.getZone().getZoneCode(), response3.getZone().getZoneCode(),
                "Consistent mapping across multiple requests");
    }

    /**
     * Property 5.2: Same district and state should always return the same zone.
     * 
     * For any valid district/state combination, calling the zone mapping service
     * multiple times should always return the same zone.
     */
    @Property
    void sameDistrictStateShouldMapToSameZone(@ForAll("validDistrictStateCombinations") LocationRequestDto location) {
        // Arrange
        AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
        DistrictZoneMappingRepository districtMappingRepository = mock(DistrictZoneMappingRepository.class);
        
        DistrictZoneMapping mapping = DistrictZoneMapping.builder()
                .id(1L)
                .districtName(location.getDistrict())
                .state(location.getState())
                .zone(sampleZone)
                .latitude(26.8467)
                .longitude(80.9462)
                .region("Central")
                .isVerified(true)
                .dataSource("ICAR")
                .isActive(true)
                .build();

        when(districtMappingRepository.findByDistrictAndState(anyString(), anyString()))
                .thenReturn(Optional.of(mapping));

        AgroEcologicalZoneService localZoneService = new AgroEcologicalZoneService(zoneRepository, districtMappingRepository);

        // Act - Call the service multiple times with the same district/state
        ZoneLookupResponseDto response1 = localZoneService.getZoneForLocation(location);
        ZoneLookupResponseDto response2 = localZoneService.getZoneForLocation(location);
        ZoneLookupResponseDto response3 = localZoneService.getZoneForLocation(location);

        // Assert - All responses should be successful and return the same zone
        assertTrue(response1.isSuccess(), "First request should succeed");
        assertTrue(response2.isSuccess(), "Second request should succeed");
        assertTrue(response3.isSuccess(), "Third request should succeed");

        assertNotNull(response1.getZone(), "First response should have a zone");
        assertNotNull(response2.getZone(), "Second response should have a zone");
        assertNotNull(response3.getZone(), "Third response should have a zone");

        assertEquals(response1.getZone().getZoneCode(), response2.getZone().getZoneCode(),
                "Same district/state should return same zone code");
        assertEquals(response1.getZone().getZoneName(), response2.getZone().getZoneName(),
                "Same district/state should return same zone name");
        assertEquals(response2.getZone().getZoneCode(), response3.getZone().getZoneCode(),
                "Consistent mapping across multiple requests");
    }

    /**
     * Property 5.3: Zone mapping is deterministic - same input always produces same output.
     * 
     * This property verifies that the zone mapping function is pure and deterministic,
     * meaning it will always return the same result for the same input regardless of
     * how many times it's called or when it's called.
     */
    @Property
    void zoneMappingIsDeterministic(@ForAll("validIndiaCoordinates") LocationRequestDto coordinates) {
        // Arrange
        AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
        DistrictZoneMappingRepository districtMappingRepository = mock(DistrictZoneMappingRepository.class);
        
        DistrictZoneMapping mapping = DistrictZoneMapping.builder()
                .id(1L)
                .districtName("Test District")
                .state("Test State")
                .zone(sampleZone)
                .latitude(coordinates.getLatitude())
                .longitude(coordinates.getLongitude())
                .region("Central")
                .isVerified(true)
                .dataSource("ICAR")
                .isActive(true)
                .build();

        when(districtMappingRepository.findNearestByCoordinates(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(mapping));

        AgroEcologicalZoneService localZoneService = new AgroEcologicalZoneService(zoneRepository, districtMappingRepository);

        // Act - Call the service 5 times with the same coordinates
        java.util.Set<String> zoneCodes = new java.util.HashSet<>();
        for (int i = 0; i < 5; i++) {
            ZoneLookupResponseDto response = localZoneService.getZoneForLocation(coordinates);
            if (response.isSuccess() && response.getZone() != null) {
                zoneCodes.add(response.getZone().getZoneCode());
            }
        }

        // Assert - All calls should return the same zone code
        assertEquals(1, zoneCodes.size(), 
                "All calls with same coordinates should return the same zone code");
    }

    /**
     * Property 5.4: Coordinates in the same zone should consistently map to that zone.
     * 
     * When multiple coordinates fall within the same agro-ecological zone's boundaries,
     * they should all consistently map to that zone.
     */
    @Property
    void coordinatesInSameZoneMapConsistently() {
        // Arrange - Define coordinates within the same zone (Upper Gangetic Plain)
        // Zone spans roughly: Latitude 24.0-30.0, Longitude 75.0-85.0
        double[] latitudes = {25.0, 26.0, 27.0, 28.0, 29.0};
        double[] longitudes = {76.0, 78.0, 80.0, 82.0, 84.0};

        for (int i = 0; i < latitudes.length; i++) {
            double lat = latitudes[i];
            double lon = longitudes[i];

            LocationRequestDto coordinates = LocationRequestDto.builder()
                    .latitude(lat)
                    .longitude(lon)
                    .build();

            AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
            DistrictZoneMappingRepository districtMappingRepository = mock(DistrictZoneMappingRepository.class);
            
            DistrictZoneMapping mapping = DistrictZoneMapping.builder()
                    .id(1L)
                    .districtName("Test District")
                    .state("Test State")
                    .zone(sampleZone)
                    .latitude(lat)
                    .longitude(lon)
                    .region("Central")
                    .isVerified(true)
                    .dataSource("ICAR")
                    .isActive(true)
                    .build();

            when(districtMappingRepository.findNearestByCoordinates(
                    eq(lat), eq(lon), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(Optional.of(mapping));

            AgroEcologicalZoneService localZoneService = new AgroEcologicalZoneService(zoneRepository, districtMappingRepository);

            // Act - Call the service multiple times for each coordinate
            ZoneLookupResponseDto response1 = localZoneService.getZoneForLocation(coordinates);
            ZoneLookupResponseDto response2 = localZoneService.getZoneForLocation(coordinates);

            // Assert - Both calls should return the same zone
            assertTrue(response1.isSuccess(), "Request 1 should succeed for lat: " + lat + ", lon: " + lon);
            assertTrue(response2.isSuccess(), "Request 2 should succeed for lat: " + lat + ", lon: " + lon);
            assertEquals(response1.getZone().getZoneCode(), response2.getZone().getZoneCode(),
                    "Same coordinate should return same zone code");
        }
    }

    /**
     * Property 5.5: Zone mapping consistency across different lookup methods.
     * 
     * When looking up a zone by district/state and by GPS coordinates for the same
     * location, both methods should return consistent zone information.
     */
    @Property
    void zoneMappingConsistentAcrossLookupMethods() {
        // Arrange - Create a location with both district/state and coordinates
        String district = "Lucknow";
        String state = "Uttar Pradesh";
        double latitude = 26.8467;
        double longitude = 80.9462;

        LocationRequestDto districtRequest = LocationRequestDto.builder()
                .district(district)
                .state(state)
                .build();

        LocationRequestDto coordinateRequest = LocationRequestDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();

        AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
        DistrictZoneMappingRepository districtMappingRepository = mock(DistrictZoneMappingRepository.class);
        
        DistrictZoneMapping mapping = DistrictZoneMapping.builder()
                .id(1L)
                .districtName(district)
                .state(state)
                .zone(sampleZone)
                .latitude(latitude)
                .longitude(longitude)
                .region("Central")
                .isVerified(true)
                .dataSource("ICAR")
                .isActive(true)
                .build();

        when(districtMappingRepository.findByDistrictAndState(district, state))
                .thenReturn(Optional.of(mapping));
        when(districtMappingRepository.findNearestByCoordinates(
                eq(latitude), eq(longitude), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(mapping));

        AgroEcologicalZoneService localZoneService = new AgroEcologicalZoneService(zoneRepository, districtMappingRepository);

        // Act - Look up zone by district/state and by coordinates
        ZoneLookupResponseDto districtResponse = localZoneService.getZoneForLocation(districtRequest);
        ZoneLookupResponseDto coordinateResponse = localZoneService.getZoneForLocation(coordinateRequest);

        // Assert - Both methods should return the same zone
        assertTrue(districtResponse.isSuccess(), "District lookup should succeed");
        assertTrue(coordinateResponse.isSuccess(), "Coordinate lookup should succeed");
        assertEquals(districtResponse.getZone().getZoneCode(), coordinateResponse.getZone().getZoneCode(),
                "District and coordinate lookup should return same zone code");
        assertEquals(districtResponse.getZone().getZoneName(), coordinateResponse.getZone().getZoneName(),
                "District and coordinate lookup should return same zone name");
    }

    /**
     * Property 5.6: Invalid coordinates outside India should be handled consistently.
     * 
     * Coordinates outside India's boundaries should consistently fail or return
     * appropriate error responses.
     */
    @Property
    void invalidCoordinatesHandledConsistently() {
        // Arrange - Coordinates outside India
        double[] invalidLatitudes = {-10.0, 50.0, 100.0};
        double[] invalidLongitudes = {-50.0, 150.0, 200.0};

        for (double lat : invalidLatitudes) {
            for (double lon : invalidLongitudes) {
                LocationRequestDto coordinates = LocationRequestDto.builder()
                        .latitude(lat)
                        .longitude(lon)
                        .build();

                AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
                DistrictZoneMappingRepository districtMappingRepository = mock(DistrictZoneMappingRepository.class);
                
                when(districtMappingRepository.findNearestByCoordinates(
                        anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                        .thenReturn(Optional.empty());
                when(zoneRepository.findByCoordinates(anyDouble(), anyDouble()))
                        .thenReturn(Optional.empty());

                AgroEcologicalZoneService localZoneService = new AgroEcologicalZoneService(zoneRepository, districtMappingRepository);

                // Act - Call the service multiple times with invalid coordinates
                ZoneLookupResponseDto response1 = localZoneService.getZoneForLocation(coordinates);
                ZoneLookupResponseDto response2 = localZoneService.getZoneForLocation(coordinates);

                // Assert - Both calls should fail consistently
                assertFalse(response1.isSuccess(), "Invalid coordinates should fail: lat=" + lat + ", lon=" + lon);
                assertFalse(response2.isSuccess(), "Invalid coordinates should consistently fail");
                assertEquals(response1.isSuccess(), response2.isSuccess(),
                        "Same invalid coordinates should have same success status");
            }
        }
    }
}