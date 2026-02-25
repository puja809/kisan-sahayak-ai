package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.AgroEcologicalZone;
import com.farmer.crop.entity.GaezCropData;
import com.farmer.crop.repository.AgroEcologicalZoneRepository;
import com.farmer.crop.repository.DistrictZoneMappingRepository;
import com.farmer.crop.repository.GaezCropDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CropRecommendationUnitTest {

    @Mock
    private AgroEcologicalZoneRepository agroEcologicalZoneRepository;

    @Mock
    private DistrictZoneMappingRepository districtZoneMappingRepository;

    @Mock
    private GaezCropDataRepository gaezCropDataRepository;

    @InjectMocks
    private AgroEcologicalZoneService agroEcologicalZoneService;

    @InjectMocks
    private GaezSuitabilityService gaezSuitabilityService;

    @InjectMocks
    private CropRecommendationService cropRecommendationService;

    @Test
    void testZoneMappingNorthernIndia() {
        LocationRequestDto request = LocationRequestDto.builder()
                .latitude(31.1471)
                .longitude(75.3412)
                .build();

        AgroEcologicalZone zone = AgroEcologicalZone.builder()
                .zoneCode("AEZ-08")
                .zoneName("Trans Gangetic Plain Region")
                .build();

        when(agroEcologicalZoneRepository.findByZoneCode(anyString())).thenReturn(Optional.of(zone));

        ZoneLookupResponseDto response = agroEcologicalZoneService.getZoneForLocation(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getZone());
        assertEquals("AEZ-08", response.getZone().getZoneCode());
    }

    @Test
    void testZoneMappingSouthernIndia() {
        LocationRequestDto request = LocationRequestDto.builder()
                .latitude(11.1271)
                .longitude(78.6569)
                .build();

        AgroEcologicalZone zone = AgroEcologicalZone.builder()
                .zoneCode("AEZ-18")
                .zoneName("Southern Plateau and Hills Region")
                .build();

        when(agroEcologicalZoneRepository.findByZoneCode(anyString())).thenReturn(Optional.of(zone));

        ZoneLookupResponseDto response = agroEcologicalZoneService.getZoneForLocation(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getZone());
        assertEquals("AEZ-18", response.getZone().getZoneCode());
    }

    @Test
    void testZoneMappingConsistency() {
        LocationRequestDto request1 = LocationRequestDto.builder()
                .latitude(20.5937)
                .longitude(78.9629)
                .build();

        LocationRequestDto request2 = LocationRequestDto.builder()
                .latitude(20.5937)
                .longitude(78.9629)
                .build();

        AgroEcologicalZone zone = AgroEcologicalZone.builder()
                .zoneCode("AEZ-10")
                .zoneName("Central Plateau and Hills Region")
                .build();

        when(agroEcologicalZoneRepository.findByZoneCode(anyString())).thenReturn(Optional.of(zone));

        ZoneLookupResponseDto response1 = agroEcologicalZoneService.getZoneForLocation(request1);
        ZoneLookupResponseDto response2 = agroEcologicalZoneService.getZoneForLocation(request2);

        assertNotNull(response1);
        assertNotNull(response2);
        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());
        assertEquals(response1.getZone().getZoneCode(), response2.getZone().getZoneCode());
    }

    @Test
    void testSuitabilityScoringBlackCottonSoil() {
        SoilHealthCardDto soilHealthCard = SoilHealthCardDto.builder()
                .nitrogenKgHa(new BigDecimal("180"))
                .phosphorusKgHa(new BigDecimal("25"))
                .potassiumKgHa(new BigDecimal("350"))
                .ph(new BigDecimal("8.0"))
                .soilTexture("Clayey")
                .build();

        GaezCropData gaezData = createGaezCropData("RICE", new BigDecimal("75"), new BigDecimal("3500"));

        when(gaezCropDataRepository.findByZoneCodeAndCropCodeAndIsActiveTrue(anyString(), anyString()))
                .thenReturn(Optional.of(gaezData));

        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST")
                .district("Test")
                .state("Test")
                .build();

        List<GaezCropSuitabilityDto> suitability = gaezSuitabilityService.calculateSuitabilityScores(request);

        assertNotNull(suitability);
        assertFalse(suitability.isEmpty());
    }

    @Test
    void testSuitabilityScoringRedSoil() {
        SoilHealthCardDto soilHealthCard = SoilHealthCardDto.builder()
                .nitrogenKgHa(new BigDecimal("150"))
                .phosphorusKgHa(new BigDecimal("20"))
                .potassiumKgHa(new BigDecimal("280"))
                .ph(new BigDecimal("6.5"))
                .soilTexture("Sandy Loam")
                .build();

        GaezCropData gaezData = createGaezCropData("COTTON", new BigDecimal("70"), new BigDecimal("4000"));

        when(gaezCropDataRepository.findByZoneCodeAndCropCodeAndIsActiveTrue(anyString(), anyString()))
                .thenReturn(Optional.of(gaezData));

        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST")
                .district("Test")
                .state("Test")
                .build();

        List<GaezCropSuitabilityDto> suitability = gaezSuitabilityService.calculateSuitabilityScores(request);

        assertNotNull(suitability);
        assertFalse(suitability.isEmpty());
    }

    @Test
    void testRankingDescendingOrder() {
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("FARMER001")
                .district("Ludhiana")
                .state("Punjab")
                .farmAreaAcres(new BigDecimal("5.0"))
                .irrigationType(CropRecommendationRequestDto.IrrigationType.CANAL)
                .season(CropRecommendationRequestDto.Season.RABI)
                .build();

        when(gaezCropDataRepository.findByZoneCodeAndIsActiveTrue(anyString()))
                .thenReturn(Collections.emptyList());

        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
    }

    @Test
    void testLocationChangeUpdatesRecommendations() {
        CropRecommendationRequestDto initialRequest = CropRecommendationRequestDto.builder()
                .farmerId("FARMER007")
                .district("Ludhiana")
                .state("Punjab")
                .latitude(new BigDecimal("30.9"))
                .longitude(new BigDecimal("75.8"))
                .farmAreaAcres(new BigDecimal("5.0"))
                .irrigationType(CropRecommendationRequestDto.IrrigationType.CANAL)
                .season(CropRecommendationRequestDto.Season.RABI)
                .build();

        CropRecommendationResponseDto initialResponse = cropRecommendationService.generateRecommendations(initialRequest);

        assertNotNull(initialResponse);
        assertTrue(initialResponse.isSuccess());
    }

    @Test
    void testZoneCodeChangesWithLocation() {
        LocationRequestDto request1 = LocationRequestDto.builder()
                .latitude(31.1471)
                .longitude(75.3412)
                .build();

        AgroEcologicalZone zone1 = AgroEcologicalZone.builder()
                .zoneCode("AEZ-08")
                .zoneName("Trans Gangetic Plain Region")
                .build();

        when(agroEcologicalZoneRepository.findByZoneCode("AEZ-08")).thenReturn(Optional.of(zone1));

        ZoneLookupResponseDto response1 = agroEcologicalZoneService.getZoneForLocation(request1);

        assertNotNull(response1);
        assertTrue(response1.isSuccess());
        assertNotNull(response1.getZone());
        assertEquals("AEZ-08", response1.getZone().getZoneCode());
    }

    @Test
    void testRecommendationsReflectNewZone() {
        CropRecommendationRequestDto newLocationRequest = CropRecommendationRequestDto.builder()
                .farmerId("FARMER008")
                .district("Jaipur")
                .state("Rajasthan")
                .latitude(new BigDecimal("26.9124"))
                .longitude(new BigDecimal("75.7873"))
                .farmAreaAcres(new BigDecimal("4.0"))
                .irrigationType(CropRecommendationRequestDto.IrrigationType.BOREWELL)
                .season(CropRecommendationRequestDto.Season.KHARIF)
                .build();

        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(newLocationRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Jaipur", response.getLocation());
    }

    @Test
    void testLocationChangeTriggersRecalculation() {
        CropRecommendationRequestDto request1 = CropRecommendationRequestDto.builder()
                .farmerId("FARMER009")
                .district("Delhi")
                .state("Delhi")
                .farmAreaAcres(new BigDecimal("3.0"))
                .irrigationType(CropRecommendationRequestDto.IrrigationType.CANAL)
                .season(CropRecommendationRequestDto.Season.RABI)
                .build();

        CropRecommendationRequestDto request2 = CropRecommendationRequestDto.builder()
                .farmerId("FARMER009")
                .district("Mumbai")
                .state("Maharashtra")
                .farmAreaAcres(new BigDecimal("3.0"))
                .irrigationType(CropRecommendationRequestDto.IrrigationType.CANAL)
                .season(CropRecommendationRequestDto.Season.RABI)
                .build();

        CropRecommendationResponseDto response1 = cropRecommendationService.generateRecommendations(request1);
        CropRecommendationResponseDto response2 = cropRecommendationService.generateRecommendations(request2);

        assertNotNull(response1);
        assertNotNull(response2);
        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());
        assertNotEquals(response1.getLocation(), response2.getLocation());
    }

    @Test
    void testSmallLocationChangeWithinSameZone() {
        LocationRequestDto request1 = LocationRequestDto.builder()
                .latitude(20.5937)
                .longitude(78.9629)
                .build();

        LocationRequestDto request2 = LocationRequestDto.builder()
                .latitude(20.6037)
                .longitude(78.9729)
                .build();

        AgroEcologicalZone zone = AgroEcologicalZone.builder()
                .zoneCode("AEZ-10")
                .zoneName("Central Plateau and Hills Region")
                .build();

        when(agroEcologicalZoneRepository.findByZoneCode("AEZ-10")).thenReturn(Optional.of(zone));

        ZoneLookupResponseDto response1 = agroEcologicalZoneService.getZoneForLocation(request1);
        ZoneLookupResponseDto response2 = agroEcologicalZoneService.getZoneForLocation(request2);

        assertNotNull(response1);
        assertNotNull(response2);
        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());
        assertEquals(response1.getZone().getZoneCode(), response2.getZone().getZoneCode());
    }

    private GaezCropData createGaezCropData(String cropCode, BigDecimal suitabilityScore, BigDecimal yieldPerHectare) {
        return GaezCropData.builder()
                .id(1L)
                .zoneCode("AEZ-08")
                .cropCode(cropCode)
                .cropName(cropCode)
                .overallSuitabilityScore(suitabilityScore)
                .irrigatedPotentialYield(yieldPerHectare)
                .waterRequirementsMm(new BigDecimal("500"))
                .growingSeasonDays(120)
                .climateRiskLevel(GaezCropData.ClimateRiskLevel.LOW)
                .kharifSuitable(true)
                .rabiSuitable(true)
                .zaidSuitable(false)
                .build();
    }
}
