package com.farmer.crop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmer.crop.dto.*;
import com.farmer.crop.service.CropRecommendationService;
import com.farmer.crop.service.GaezSuitabilityService;
import com.farmer.crop.service.SeedVarietyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CropRecommendationController.
 * 
 * Tests the REST endpoints for crop recommendations, ensuring proper
 * request handling and response formatting.
 * 
 * Validates: Requirements 2.5, 2.6, 2.7, 2.8, 2.9
 */
@WebMvcTest(CropRecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class CropRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CropRecommendationService cropRecommendationService;

    @MockBean
    private GaezSuitabilityService gaezSuitabilityService;

    @MockBean
    private SeedVarietyService seedVarietyService;

    private CropRecommendationRequestDto validRequest;
    private CropRecommendationResponseDto mockResponse;

    @BeforeEach
    void setUp() {
        // Create valid request
        validRequest = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .district("Lucknow")
                .state("Uttar Pradesh")
                .latitude(new BigDecimal("26.8467"))
                .longitude(new BigDecimal("80.9462"))
                .irrigationType(CropRecommendationRequestDto.IrrigationType.CANAL)
                .season(CropRecommendationRequestDto.Season.KHARIF)
                .includeMarketData(true)
                .includeClimateRiskAssessment(true)
                .build();

        // Create mock response
        mockResponse = createMockResponse();
    }

    @Test
    void generateRecommendations_WithValidRequest_ReturnsOk() throws Exception {
        when(cropRecommendationService.generateRecommendations(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/crops/recommendations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.recommendationCount").value(2))
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void generateRecommendations_WithInvalidLocation_ReturnsBadRequest() throws Exception {
        CropRecommendationRequestDto invalidRequest = CropRecommendationRequestDto.builder()
                .farmerId("FARMER-001")
                .build();

        mockMvc.perform(post("/api/v1/crops/recommendations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").exists());
    }

    @Test
    void generateRecommendations_WithServiceError_ReturnsBadRequest() throws Exception {
        CropRecommendationResponseDto errorResponse = CropRecommendationResponseDto.builder()
                .success(false)
                .errorMessage("Could not determine agro-ecological zone for the location")
                .generatedAt(LocalDateTime.now())
                .recommendations(Collections.emptyList())
                .recommendationCount(0)
                .build();

        when(cropRecommendationService.generateRecommendations(any())).thenReturn(errorResponse);

        mockMvc.perform(post("/api/v1/crops/recommendations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("Could not determine agro-ecological zone for the location"));
    }

    @Test
    void getRecommendations_WithValidParams_ReturnsOk() throws Exception {
        when(cropRecommendationService.generateRecommendations(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/crops/recommendations")
                        .param("farmerId", "FARMER-001")
                        .param("district", "Lucknow")
                        .param("state", "Uttar Pradesh")
                        .param("season", "KHARIF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    void getRecommendations_WithMissingLocation_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/crops/recommendations")
                        .param("farmerId", "FARMER-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getRecommendations_WithCoordinates_ReturnsOk() throws Exception {
        when(cropRecommendationService.generateRecommendations(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/crops/recommendations")
                        .param("farmerId", "FARMER-001")
                        .param("latitude", "26.8467")
                        .param("longitude", "80.9462")
                        .param("season", "RABI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getVarieties_WithValidCropAndState_ReturnsOk() throws Exception {
        List<SeedVarietyDto> varieties = createMockVarieties();
        when(seedVarietyService.getRecommendedVarieties("RICE", "Uttar Pradesh")).thenReturn(varieties);

        mockMvc.perform(get("/api/v1/crops/varieties/RICE")
                        .param("state", "Uttar Pradesh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].varietyName").value("PB-1509"));
    }

    @Test
    void getVarieties_WithNoVarieties_ReturnsNotFound() throws Exception {
        when(seedVarietyService.getRecommendedVarieties("UNKNOWN", "UnknownState")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/crops/varieties/UNKNOWN")
                        .param("state", "UnknownState"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDroughtTolerantVarieties_WithValidCrop_ReturnsOk() throws Exception {
        List<SeedVarietyDto> varieties = createMockVarieties();
        when(seedVarietyService.getDroughtTolerantVarieties("RICE")).thenReturn(varieties);

        mockMvc.perform(get("/api/v1/crops/varieties/RICE/drought-tolerant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getFloodTolerantVarieties_WithValidCrop_ReturnsOk() throws Exception {
        List<SeedVarietyDto> varieties = createMockVarieties();
        when(seedVarietyService.getFloodTolerantVarieties("RICE")).thenReturn(varieties);

        mockMvc.perform(get("/api/v1/crops/varieties/RICE/flood-tolerant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getHeatTolerantVarieties_WithValidCrop_ReturnsOk() throws Exception {
        List<SeedVarietyDto> varieties = createMockVarieties();
        when(seedVarietyService.getHeatTolerantVarieties("RICE")).thenReturn(varieties);

        mockMvc.perform(get("/api/v1/crops/varieties/RICE/heat-tolerant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getStatesForCrop_WithValidCrop_ReturnsOk() throws Exception {
        List<String> states = Arrays.asList("Punjab", "Haryana", "Uttar Pradesh");
        when(seedVarietyService.getStatesForCrop("RICE")).thenReturn(states);

        mockMvc.perform(get("/api/v1/crops/varieties/RICE/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("Punjab"));
    }

    @Test
    void getSuitability_WithValidZone_ReturnsOk() throws Exception {
        List<GaezCropSuitabilityDto> suitabilityList = createMockSuitabilityList();
        when(gaezSuitabilityService.getSuitabilityForZone("AEZ-05")).thenReturn(suitabilityList);

        mockMvc.perform(get("/api/v1/crops/suitability/AEZ-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].cropCode").value("RICE"));
    }

    @Test
    void getSuitability_WithInvalidZoneFormat_ReturnsBadRequest() throws Exception {
        // "INVALID" doesn't start with "AEZ-", so it returns 400 for invalid format
        mockMvc.perform(get("/api/v1/crops/suitability/INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getSuitability_WithValidZoneNoData_ReturnsNotFound() throws Exception {
        // "AEZ-INVALID" starts with "AEZ-" but has no data, so it returns 404
        when(gaezSuitabilityService.getSuitabilityForZone("AEZ-INVALID")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/crops/suitability/AEZ-INVALID"))
                .andExpect(status().isNotFound());
    }

    // Helper methods

    private CropRecommendationResponseDto createMockResponse() {
        List<CropRecommendationResponseDto.RecommendedCropDto> recommendations = new ArrayList<>();
        
        recommendations.add(CropRecommendationResponseDto.RecommendedCropDto.builder()
                .rank(1)
                .overallSuitabilityScore(new BigDecimal("85"))
                .expectedYieldPerAcre(new BigDecimal("25"))
                .waterRequirementPerAcre(new BigDecimal("450000"))
                .growingDurationDays(120)
                .climateRiskLevel(GaezCropSuitabilityDto.ClimateRiskLevel.LOW)
                .seasonSuitable(true)
                .recommendedVarieties(Arrays.asList("PB-1509", "HD-2967"))
                .build());
        
        recommendations.add(CropRecommendationResponseDto.RecommendedCropDto.builder()
                .rank(2)
                .overallSuitabilityScore(new BigDecimal("75"))
                .expectedYieldPerAcre(new BigDecimal("20"))
                .waterRequirementPerAcre(new BigDecimal("400000"))
                .growingDurationDays(100)
                .climateRiskLevel(GaezCropSuitabilityDto.ClimateRiskLevel.MEDIUM)
                .seasonSuitable(true)
                .recommendedVarieties(Arrays.asList("HD-3086", "DBW-187"))
                .build());

        return CropRecommendationResponseDto.builder()
                .success(true)
                .generatedAt(LocalDateTime.now())
                .farmerId("FARMER-001")
                .location("Lucknow, Uttar Pradesh")
                .agroEcologicalZone("AEZ-05")
                .season(CropRecommendationRequestDto.Season.KHARIF)
                .recommendations(recommendations)
                .recommendationCount(2)
                .soilHealthCardUsed(false)
                .climateRiskSummary(CropRecommendationResponseDto.ClimateRiskSummary.builder()
                        .overallRiskLevel(GaezCropSuitabilityDto.ClimateRiskLevel.LOW)
                        .highRiskCropCount(0)
                        .mediumRiskCropCount(1)
                        .lowRiskCropCount(1)
                        .build())
                .marketDataStatus(CropRecommendationResponseDto.MarketDataStatus.builder()
                        .integrated(true)
                        .cropsWithMarketData(2)
                        .priceTrendSummary("Prices stable across most crops")
                        .build())
                .build();
    }

    private List<SeedVarietyDto> createMockVarieties() {
        return Arrays.asList(
                SeedVarietyDto.builder()
                        .varietyId("RICE-UP-001")
                        .cropCode("RICE")
                        .cropName("Rice")
                        .varietyName("PB-1509")
                        .releasingInstitute("Punjab Agricultural University, Ludhiana")
                        .releaseYear(2013)
                        .recommendedStates(Arrays.asList("Punjab", "Haryana", "Uttar Pradesh"))
                        .maturityDays(145)
                        .averageYieldQtlHa(new BigDecimal("45"))
                        .droughtTolerant(false)
                        .floodTolerant(false)
                        .heatTolerant(true)
                        .isAvailable(true)
                        .build(),
                SeedVarietyDto.builder()
                        .varietyId("RICE-UP-002")
                        .cropCode("RICE")
                        .cropName("Rice")
                        .varietyName("HD-2967")
                        .releasingInstitute("Indian Agricultural Research Institute, New Delhi")
                        .releaseYear(2011)
                        .recommendedStates(Arrays.asList("Uttar Pradesh", "Punjab", "Haryana"))
                        .maturityDays(142)
                        .averageYieldQtlHa(new BigDecimal("48"))
                        .droughtTolerant(false)
                        .floodTolerant(false)
                        .heatTolerant(true)
                        .isAvailable(true)
                        .build()
        );
    }

    private List<GaezCropSuitabilityDto> createMockSuitabilityList() {
        return Arrays.asList(
                GaezCropSuitabilityDto.builder()
                        .cropCode("RICE")
                        .cropName("Rice")
                        .overallSuitabilityScore(new BigDecimal("85"))
                        .suitabilityClassification(GaezCropSuitabilityDto.SuitabilityClassification.HIGHLY_SUITABLE)
                        .climateSuitabilityScore(new BigDecimal("90"))
                        .soilSuitabilityScore(new BigDecimal("80"))
                        .waterSuitabilityScore(new BigDecimal("85"))
                        .expectedYieldExpected(new BigDecimal("4500"))
                        .waterRequirementsMm(new BigDecimal("900"))
                        .growingSeasonDays(145)
                        .kharifSuitable(true)
                        .rabiSuitable(false)
                        .zaidSuitable(false)
                        .climateRiskLevel(GaezCropSuitabilityDto.ClimateRiskLevel.LOW)
                        .build(),
                GaezCropSuitabilityDto.builder()
                        .cropCode("WHEAT")
                        .cropName("Wheat")
                        .overallSuitabilityScore(new BigDecimal("75"))
                        .suitabilityClassification(GaezCropSuitabilityDto.SuitabilityClassification.SUITABLE)
                        .climateSuitabilityScore(new BigDecimal("80"))
                        .soilSuitabilityScore(new BigDecimal("75"))
                        .waterSuitabilityScore(new BigDecimal("70"))
                        .expectedYieldExpected(new BigDecimal("4000"))
                        .waterRequirementsMm(new BigDecimal("450"))
                        .growingSeasonDays(120)
                        .kharifSuitable(false)
                        .rabiSuitable(true)
                        .zaidSuitable(false)
                        .climateRiskLevel(GaezCropSuitabilityDto.ClimateRiskLevel.MEDIUM)
                        .build()
        );
    }
}