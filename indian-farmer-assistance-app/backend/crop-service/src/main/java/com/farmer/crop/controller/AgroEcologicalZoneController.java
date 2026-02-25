package com.farmer.crop.controller;

import com.farmer.crop.dto.*;
import com.farmer.crop.service.AgroEcologicalZoneService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Agro-Ecological Zone mapping.
 * 
 * Provides endpoints for mapping locations to ICAR agro-ecological zones.
 * 
 * Validates: Requirement 2.1
 */
@RestController
@RequestMapping("/api/v1/crops/zones")
public class AgroEcologicalZoneController {

    private static final Logger logger = LoggerFactory.getLogger(AgroEcologicalZoneController.class);

    private final AgroEcologicalZoneService zoneService;

    public AgroEcologicalZoneController(AgroEcologicalZoneService zoneService) {
        this.zoneService = zoneService;
    }

    /**
     * Get agro-ecological zone for a location.
     * 
     * Supports lookup by:
     * - District and state name
     * - GPS coordinates (latitude and longitude)
     * 
     * @param request Location request
     * @return Zone lookup response
     * 
     * Validates: Requirement 2.1
     */
    @PostMapping("/lookup")
    public ResponseEntity<ZoneLookupResponseDto> getZoneForLocation(
            @Valid @RequestBody LocationRequestDto request) {
        logger.info("Zone lookup request: {}", request);
        
        ZoneLookupResponseDto response = zoneService.getZoneForLocation(request);
        
        if (!response.isSuccess()) {
            logger.warn("Zone lookup failed: {}", response.getErrorMessage());
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get agro-ecological zone by district and state.
     * 
     * @param district District name
     * @param state State name
     * @return Zone lookup response
     * 
     * Validates: Requirement 2.1
     */
    @GetMapping("/district/{district}/state/{state}")
    public ResponseEntity<ZoneLookupResponseDto> getZoneByDistrictAndState(
            @PathVariable String district,
            @PathVariable String state) {
        logger.info("Zone lookup by district and state: {}, {}", district, state);
        
        LocationRequestDto request = LocationRequestDto.builder()
                .district(district)
                .state(state)
                .build();
        
        ZoneLookupResponseDto response = zoneService.getZoneForLocation(request);
        
        if (!response.isSuccess()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get agro-ecological zone by GPS coordinates.
     * 
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @return Zone lookup response
     * 
     * Validates: Requirement 2.1
     */
    @GetMapping("/coordinates")
    public ResponseEntity<ZoneLookupResponseDto> getZoneByCoordinates(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        logger.info("Zone lookup by coordinates: {}, {}", latitude, longitude);
        
        LocationRequestDto request = LocationRequestDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
        
        ZoneLookupResponseDto response = zoneService.getZoneForLocation(request);
        
        if (!response.isSuccess()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all active agro-ecological zones.
     * 
     * @return List of all active zones
     * 
     * Validates: Requirement 2.1
     */
    @GetMapping
    public ResponseEntity<List<AgroEcologicalZoneDto>> getAllZones() {
        logger.info("Getting all agro-ecological zones");
        
        List<AgroEcologicalZoneDto> zones = zoneService.getAllZones();
        return ResponseEntity.ok(zones);
    }

    /**
     * Get zone by zone code.
     * 
     * @param zoneCode ICAR zone code (e.g., "AEZ-01")
     * @return Zone information
     * 
     * Validates: Requirement 2.1
     */
    @GetMapping("/code/{zoneCode}")
    public ResponseEntity<AgroEcologicalZoneDto> getZoneByCode(
            @PathVariable String zoneCode) {
        logger.info("Getting zone by code: {}", zoneCode);
        
        return zoneService.getZoneByCode(zoneCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get zones by climate type.
     * 
     * @param climateType Climate type (e.g., "Tropical", "Subtropical")
     * @return List of zones with the specified climate type
     * 
     * Validates: Requirement 2.1
     */
    @GetMapping("/climate/{climateType}")
    public ResponseEntity<List<AgroEcologicalZoneDto>> getZonesByClimateType(
            @PathVariable String climateType) {
        logger.info("Getting zones by climate type: {}", climateType);
        
        List<AgroEcologicalZoneDto> zones = zoneService.getZonesByClimateType(climateType);
        return ResponseEntity.ok(zones);
    }

    /**
     * Get districts in a state.
     * 
     * @param state State name
     * @return List of district names
     * 
     * Validates: Requirement 2.1
     */
    @GetMapping("/states/{state}/districts")
    public ResponseEntity<List<String>> getDistrictsByState(
            @PathVariable String state) {
        logger.info("Getting districts for state: {}", state);
        
        List<String> districts = zoneService.getDistrictsByState(state);
        return ResponseEntity.ok(districts);
    }
}