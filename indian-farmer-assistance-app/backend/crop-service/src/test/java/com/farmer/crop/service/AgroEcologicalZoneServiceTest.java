package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.AgroEcologicalZone;
import com.farmer.crop.entity.DistrictZoneMapping;
import com.farmer.crop.exception.LocationNotFoundException;
import com.farmer.crop.exception.ZoneMappingException;
import com.farmer.crop.repository.AgroEcologicalZoneRepository;
import com.farmer.crop.repository.DistrictZoneMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgroEcologicalZoneService.
 * 
 * Validates: Requirement 2.1
 */
@ExtendWith(MockitoExtension.class)
class AgroEcologicalZoneServiceTest {

    @Mock
    private AgroEcologicalZoneRepository zoneRepository;

    @Mock
    private DistrictZoneMappingRepository districtMappingRepository;

    @InjectMocks
    private AgroEcologicalZoneService zoneService;

    private AgroEcologicalZone sampleZone;
    private DistrictZoneMapping sampleMapping;

    @BeforeEach
    void setUp() {
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

        sampleMapping = DistrictZoneMapping.builder()
                .id(1L)
                .districtName("Lucknow")
                .state("Uttar Pradesh")
                .zone(sampleZone)
                .latitude(26.8467)
                .longitude(80.9462)
                .region("Central")
                .isVerified(true)
                .dataSource("ICAR")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should return zone for valid district and state")
    void getZoneForLocation_WithValidDistrictAndState_ReturnsZone() {
        // Arrange
        LocationRequestDto request = LocationRequestDto.builder()
                .district("Lucknow")
                .state("Uttar Pradesh")
                .build();

        when(districtMappingRepository.findByDistrictAndState("Lucknow", "Uttar Pradesh"))
                .thenReturn(Optional.of(sampleMapping));

        // Act
        ZoneLookupResponseDto response = zoneService.getZoneForLocation(request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Lucknow", response.getDistrict());
        assertEquals("Uttar Pradesh", response.getState());
        assertNotNull(response.getZone());
        assertEquals("AEZ-05", response.getZone().getZoneCode());
        assertEquals("Upper Gangetic Plain Region", response.getZone().getZoneName());
        
        verify(districtMappingRepository).findByDistrictAndState("Lucknow", "Uttar Pradesh");
    }

    @Test
    @DisplayName("Should return zone for valid GPS coordinates")
    void getZoneForLocation_WithValidCoordinates_ReturnsZone() {
        // Arrange
        LocationRequestDto request = LocationRequestDto.builder()
                .latitude(26.8467)
                .longitude(80.9462)
                .build();

        when(districtMappingRepository.findNearestByCoordinates(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(sampleMapping));

        // Act
        ZoneLookupResponseDto response = zoneService.getZoneForLocation(request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(26.8467, response.getLatitude());
        assertEquals(80.9462, response.getLongitude());
        assertNotNull(response.getZone());
        assertEquals("AEZ-05", response.getZone().getZoneCode());
        
        verify(districtMappingRepository).findNearestByCoordinates(
                eq(26.8467), eq(80.9462), anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should return failure for invalid district")
    void getZoneForLocation_WithInvalidDistrict_ReturnsFailure() {
        // Arrange
        LocationRequestDto request = LocationRequestDto.builder()
                .district("InvalidDistrict")
                .state("Uttar Pradesh")
                .build();

        when(districtMappingRepository.findByDistrictAndState(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(districtMappingRepository.findByDistrictName(anyString()))
                .thenReturn(List.of());

        // Act
        ZoneLookupResponseDto response = zoneService.getZoneForLocation(request);

        // Assert
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertEquals("InvalidDistrict", response.getDistrict());
        assertEquals("Uttar Pradesh", response.getState());
    }

    @Test
    @DisplayName("Should return failure for invalid coordinates")
    void getZoneForLocation_WithInvalidCoordinates_ReturnsFailure() {
        // Arrange
        LocationRequestDto request = LocationRequestDto.builder()
                .latitude(0.0)
                .longitude(0.0)
                .build();

        when(districtMappingRepository.findNearestByCoordinates(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        when(zoneRepository.findByCoordinates(anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());

        // Act
        ZoneLookupResponseDto response = zoneService.getZoneForLocation(request);

        // Assert
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
    }

    @Test
    @DisplayName("Should throw exception for insufficient location information")
    void getZoneForLocation_WithNoLocation_ThrowsException() {
        // Arrange
        LocationRequestDto request = LocationRequestDto.builder()
                .district("Lucknow")
                .build();

        // Act & Assert
        assertThrows(ZoneMappingException.class, 
                () -> zoneService.getZoneForLocation(request));
    }

    @Test
    @DisplayName("Should throw exception for invalid latitude")
    void getZoneForLocation_WithInvalidLatitude_ThrowsException() {
        // Arrange
        LocationRequestDto request = LocationRequestDto.builder()
                .latitude(100.0)
                .longitude(80.0)
                .build();

        // Act & Assert
        assertThrows(ZoneMappingException.class, 
                () -> zoneService.getZoneForLocation(request));
    }

    @Test
    @DisplayName("Should throw exception for invalid longitude")
    void getZoneForLocation_WithInvalidLongitude_ThrowsException() {
        // Arrange
        LocationRequestDto request = LocationRequestDto.builder()
                .latitude(26.0)
                .longitude(200.0)
                .build();

        // Act & Assert
        assertThrows(ZoneMappingException.class, 
                () -> zoneService.getZoneForLocation(request));
    }

    @Test
    @DisplayName("Should return all active zones")
    void getAllZones_ReturnsAllActiveZones() {
        // Arrange
        AgroEcologicalZone zone2 = AgroEcologicalZone.builder()
                .id(2L)
                .zoneCode("AEZ-06")
                .zoneName("Trans-Gangetic Plain Region")
                .isActive(true)
                .build();

        when(zoneRepository.findByIsActiveTrue())
                .thenReturn(List.of(sampleZone, zone2));

        // Act
        List<AgroEcologicalZoneDto> zones = zoneService.getAllZones();

        // Assert
        assertEquals(2, zones.size());
        assertEquals("AEZ-05", zones.get(0).getZoneCode());
        assertEquals("AEZ-06", zones.get(1).getZoneCode());
    }

    @Test
    @DisplayName("Should return zone by code")
    void getZoneByCode_WithValidCode_ReturnsZone() {
        // Arrange
        when(zoneRepository.findByZoneCode("AEZ-05"))
                .thenReturn(Optional.of(sampleZone));

        // Act
        Optional<AgroEcologicalZoneDto> result = zoneService.getZoneByCode("AEZ-05");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("AEZ-05", result.get().getZoneCode());
        assertEquals("Upper Gangetic Plain Region", result.get().getZoneName());
    }

    @Test
    @DisplayName("Should return empty for invalid code")
    void getZoneByCode_WithInvalidCode_ReturnsEmpty() {
        // Arrange
        when(zoneRepository.findByZoneCode("AEZ-99"))
                .thenReturn(Optional.empty());

        // Act
        Optional<AgroEcologicalZoneDto> result = zoneService.getZoneByCode("AEZ-99");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return zones by climate type")
    void getZonesByClimateType_WithValidClimate_ReturnsZones() {
        // Arrange
        when(zoneRepository.findByClimateTypeAndIsActiveTrue("Semi-Arid Subtropical"))
                .thenReturn(List.of(sampleZone));

        // Act
        List<AgroEcologicalZoneDto> zones = zoneService.getZonesByClimateType("Semi-Arid Subtropical");

        // Assert
        assertEquals(1, zones.size());
        assertEquals("Semi-Arid Subtropical", zones.get(0).getClimateType());
    }

    @Test
    @DisplayName("Should return districts by state")
    void getDistrictsByState_WithValidState_ReturnsDistricts() {
        // Arrange
        when(districtMappingRepository.findByState("Uttar Pradesh"))
                .thenReturn(List.of(sampleMapping));

        // Act
        List<String> districts = zoneService.getDistrictsByState("Uttar Pradesh");

        // Assert
        assertEquals(1, districts.size());
        assertEquals("Lucknow", districts.get(0));
    }

    @Test
    @DisplayName("Should return zone for district")
    void getZoneForDistrict_WithValidDistrict_ReturnsZone() {
        // Arrange
        when(districtMappingRepository.findByDistrictAndState("Lucknow", "Uttar Pradesh"))
                .thenReturn(Optional.of(sampleMapping));

        // Act
        Optional<AgroEcologicalZoneDto> result = zoneService.getZoneForDistrict("Lucknow", "Uttar Pradesh");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("AEZ-05", result.get().getZoneCode());
    }

    @Test
    @DisplayName("Should handle case-insensitive district lookup")
    void getZoneForLocation_WithCaseInsensitiveDistrict_ReturnsZone() {
        // Arrange
        LocationRequestDto request = LocationRequestDto.builder()
                .district("LUCKNOW")
                .state("uttar pradesh")
                .build();

        when(districtMappingRepository.findByDistrictAndState("LUCKNOW", "uttar pradesh"))
                .thenReturn(Optional.empty());
        when(districtMappingRepository.findByDistrictAndState("lucknow", "uttar pradesh"))
                .thenReturn(Optional.of(sampleMapping));

        // Act
        ZoneLookupResponseDto response = zoneService.getZoneForLocation(request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("LUCKNOW", response.getDistrict());
    }

    @Test
    @DisplayName("Should fallback to zone bounding box when district not found")
    void getZoneForLocation_WithCoordinatesFallbackToZone_ReturnsZone() {
        // Arrange
        LocationRequestDto request = LocationRequestDto.builder()
                .latitude(27.0)
                .longitude(78.0)
                .build();

        when(districtMappingRepository.findNearestByCoordinates(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        when(zoneRepository.findByCoordinates(27.0, 78.0))
                .thenReturn(Optional.of(sampleZone));

        // Act
        ZoneLookupResponseDto response = zoneService.getZoneForLocation(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getZone());
        assertEquals("AEZ-05", response.getZone().getZoneCode());
    }
}