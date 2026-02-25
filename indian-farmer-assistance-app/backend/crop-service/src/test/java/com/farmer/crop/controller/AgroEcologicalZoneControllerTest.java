package com.farmer.crop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmer.crop.dto.*;
import com.farmer.crop.service.AgroEcologicalZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AgroEcologicalZoneController.
 * 
 * Validates: Requirement 2.1
 */
@WebMvcTest(AgroEcologicalZoneController.class)
@AutoConfigureMockMvc(addFilters = false)
class AgroEcologicalZoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgroEcologicalZoneService zoneService;

    private AgroEcologicalZoneDto sampleZoneDto;
    private ZoneLookupResponseDto sampleResponseDto;

    @BeforeEach
    void setUp() {
        sampleZoneDto = AgroEcologicalZoneDto.builder()
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
                .build();

        sampleResponseDto = ZoneLookupResponseDto.builder()
                .success(true)
                .inputLocation("Lucknow, Uttar Pradesh")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .latitude(26.8467)
                .longitude(80.9462)
                .zone(sampleZoneDto)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/crops/zones/lookup - Should return zone for valid location")
    void lookupZone_WithValidLocation_ReturnsZone() throws Exception {
        // Arrange
        LocationRequestDto request = LocationRequestDto.builder()
                .district("Lucknow")
                .state("Uttar Pradesh")
                .build();

        when(zoneService.getZoneForLocation(any(LocationRequestDto.class)))
                .thenReturn(sampleResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/crops/zones/lookup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.district").value("Lucknow"))
                .andExpect(jsonPath("$.state").value("Uttar Pradesh"))
                .andExpect(jsonPath("$.zone.zoneCode").value("AEZ-05"))
                .andExpect(jsonPath("$.zone.zoneName").value("Upper Gangetic Plain Region"));
    }

    @Test
    @DisplayName("POST /api/v1/crops/zones/lookup - Should return 404 for invalid location")
    void lookupZone_WithInvalidLocation_Returns404() throws Exception {
        // Arrange
        LocationRequestDto request = LocationRequestDto.builder()
                .district("InvalidDistrict")
                .state("Uttar Pradesh")
                .build();

        ZoneLookupResponseDto failureResponse = ZoneLookupResponseDto.builder()
                .success(false)
                .inputLocation("InvalidDistrict, Uttar Pradesh")
                .district("InvalidDistrict")
                .state("Uttar Pradesh")
                .errorMessage("Location not found")
                .build();

        when(zoneService.getZoneForLocation(any(LocationRequestDto.class)))
                .thenReturn(failureResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/crops/zones/lookup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/crops/zones/district/{district}/state/{state} - Should return zone")
    void getZoneByDistrictAndState_WithValidParams_ReturnsZone() throws Exception {
        // Arrange
        when(zoneService.getZoneForLocation(any(LocationRequestDto.class)))
                .thenReturn(sampleResponseDto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/crops/zones/district/Lucknow/state/Uttar%20Pradesh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.district").value("Lucknow"))
                .andExpect(jsonPath("$.zone.zoneCode").value("AEZ-05"));
    }

    @Test
    @DisplayName("GET /api/v1/crops/zones/coordinates - Should return zone for valid coordinates")
    void getZoneByCoordinates_WithValidParams_ReturnsZone() throws Exception {
        // Arrange
        ZoneLookupResponseDto coordinateResponse = ZoneLookupResponseDto.builder()
                .success(true)
                .inputLocation("26.8467, 80.9462")
                .latitude(26.8467)
                .longitude(80.9462)
                .zone(sampleZoneDto)
                .build();

        when(zoneService.getZoneForLocation(any(LocationRequestDto.class)))
                .thenReturn(coordinateResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/crops/zones/coordinates")
                .param("latitude", "26.8467")
                .param("longitude", "80.9462"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.latitude").value(26.8467))
                .andExpect(jsonPath("$.longitude").value(80.9462))
                .andExpect(jsonPath("$.zone.zoneCode").value("AEZ-05"));
    }

    @Test
    @DisplayName("GET /api/v1/crops/zones - Should return all zones")
    void getAllZones_ReturnsAllZones() throws Exception {
        // Arrange
        AgroEcologicalZoneDto zone2 = AgroEcologicalZoneDto.builder()
                .id(2L)
                .zoneCode("AEZ-06")
                .zoneName("Trans-Gangetic Plain Region")
                .climateType("Semi-Arid Subtropical")
                .build();

        when(zoneService.getAllZones())
                .thenReturn(List.of(sampleZoneDto, zone2));

        // Act & Assert
        mockMvc.perform(get("/api/v1/crops/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].zoneCode").value("AEZ-05"))
                .andExpect(jsonPath("$[1].zoneCode").value("AEZ-06"));
    }

    @Test
    @DisplayName("GET /api/v1/crops/zones/code/{zoneCode} - Should return zone by code")
    void getZoneByCode_WithValidCode_ReturnsZone() throws Exception {
        // Arrange
        when(zoneService.getZoneByCode("AEZ-05"))
                .thenReturn(Optional.of(sampleZoneDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/crops/zones/code/AEZ-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneCode").value("AEZ-05"))
                .andExpect(jsonPath("$.zoneName").value("Upper Gangetic Plain Region"));
    }

    @Test
    @DisplayName("GET /api/v1/crops/zones/code/{zoneCode} - Should return 404 for invalid code")
    void getZoneByCode_WithInvalidCode_Returns404() throws Exception {
        // Arrange
        when(zoneService.getZoneByCode("AEZ-99"))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/crops/zones/code/AEZ-99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/crops/zones/climate/{climateType} - Should return zones by climate")
    void getZonesByClimateType_WithValidClimate_ReturnsZones() throws Exception {
        // Arrange
        AgroEcologicalZoneDto zoneDto = AgroEcologicalZoneDto.builder()
                .id(1L)
                .zoneCode("AEZ-05")
                .zoneName("Upper Gangetic Plain Region")
                .climateType("Tropical")
                .build();

        when(zoneService.getZonesByClimateType("Tropical"))
                .thenReturn(List.of(zoneDto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/crops/zones/climate/Tropical"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].climateType").value("Tropical"));
    }

    @Test
    @DisplayName("GET /api/v1/crops/zones/states/{state}/districts - Should return districts")
    void getDistrictsByState_WithValidState_ReturnsDistricts() throws Exception {
        // Arrange
        when(zoneService.getDistrictsByState("Uttar Pradesh"))
                .thenReturn(List.of("Lucknow", "Kanpur", "Agra"));
        when(zoneService.getDistrictsByState("Maharashtra"))
                .thenReturn(List.of("Mumbai", "Pune", "Nagpur"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/crops/zones/states/Maharashtra/districts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Mumbai"))
                .andExpect(jsonPath("$[1]").value("Pune"))
                .andExpect(jsonPath("$[2]").value("Nagpur"));
    }
}