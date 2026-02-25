package com.farmer.location.controller;

import com.farmer.location.dto.*;
import com.farmer.location.entity.LocationHistory;
import com.farmer.location.service.GpsLocationService;
import com.farmer.location.service.GovernmentBodyLocatorService;
import com.farmer.location.service.LocationChangeDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for location services.
 * 
 * Validates: Requirements 14.1, 14.2, 14.3, 14.4, 14.5, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7
 */
@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final GpsLocationService gpsLocationService;
    private final LocationChangeDetectionService locationChangeDetectionService;
    private final GovernmentBodyLocatorService governmentBodyLocatorService;

    /**
     * Get location from GPS coordinates or district/state.
     * 
     * GET /api/v1/location
     * POST /api/v1/location
     */
    @GetMapping
    public ResponseEntity<LocationResponseDto> getLocation(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Boolean requestPermission) {
        
        LocationRequestDto request = LocationRequestDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .district(district)
                .state(state)
                .requestPermission(requestPermission != null && requestPermission)
                .build();

        LocationResponseDto response = gpsLocationService.getLocation(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get location from GPS coordinates or district/state (POST).
     */
    @PostMapping
    public ResponseEntity<LocationResponseDto> getLocationPost(@Valid @RequestBody LocationRequestDto request) {
        LocationResponseDto response = gpsLocationService.getLocation(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Detect location changes for a user.
     * 
     * POST /api/v1/location/detect-change
     */
    @PostMapping("/detect-change")
    public ResponseEntity<LocationChangeDto> detectLocationChange(
            @RequestParam Long userId,
            @Valid @RequestBody LocationRequestDto request) {
        
        LocationChangeDto response = locationChangeDetectionService.detectLocationChange(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get location history for a user.
     * 
     * GET /api/v1/location/history/{userId}
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<LocationHistory>> getLocationHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<LocationHistory> history = locationChangeDetectionService.getLocationHistory(userId, limit);
        return ResponseEntity.ok(history);
    }

    /**
     * Get significant location changes for a user.
     * 
     * GET /api/v1/location/changes/{userId}
     */
    @GetMapping("/changes/{userId}")
    public ResponseEntity<List<LocationHistory>> getSignificantChanges(@PathVariable Long userId) {
        List<LocationHistory> changes = locationChangeDetectionService.getSignificantChanges(userId);
        return ResponseEntity.ok(changes);
    }

    /**
     * Search for nearby government bodies.
     * 
     * POST /api/v1/location/government-bodies/search
     */
    @PostMapping("/government-bodies/search")
    public ResponseEntity<GovernmentBodySearchResponseDto> searchGovernmentBodies(
            @Valid @RequestBody GovernmentBodySearchRequestDto request) {
        
        GovernmentBodySearchResponseDto response = governmentBodyLocatorService.searchNearbyGovernmentBodies(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get nearby KVKs.
     * 
     * GET /api/v1/location/kvks
     */
    @GetMapping("/kvks")
    public ResponseEntity<List<GovernmentBodyDto>> getNearbyKvks(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "50") Double maxDistanceKm) {
        
        List<GovernmentBodyDto> kvks = governmentBodyLocatorService.getNearbyKvks(latitude, longitude, maxDistanceKm);
        return ResponseEntity.ok(kvks);
    }

    /**
     * Get nearby district agriculture offices.
     * 
     * GET /api/v1/location/district-offices
     */
    @GetMapping("/district-offices")
    public ResponseEntity<List<GovernmentBodyDto>> getNearbyDistrictOffices(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "50") Double maxDistanceKm) {
        
        List<GovernmentBodyDto> offices = governmentBodyLocatorService.getNearbyDistrictOffices(latitude, longitude, maxDistanceKm);
        return ResponseEntity.ok(offices);
    }

    /**
     * Get nearby state departments.
     * 
     * GET /api/v1/location/state-departments
     */
    @GetMapping("/state-departments")
    public ResponseEntity<List<GovernmentBodyDto>> getNearbyStateDepartments(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "50") Double maxDistanceKm) {
        
        List<GovernmentBodyDto> departments = governmentBodyLocatorService.getNearbyStateDepartments(latitude, longitude, maxDistanceKm);
        return ResponseEntity.ok(departments);
    }

    /**
     * Get nearby ATARI centers.
     * 
     * GET /api/v1/location/atari-centers
     */
    @GetMapping("/atari-centers")
    public ResponseEntity<List<GovernmentBodyDto>> getNearbyAtariCenters(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "50") Double maxDistanceKm) {
        
        List<GovernmentBodyDto> centers = governmentBodyLocatorService.getNearbyAtariCenters(latitude, longitude, maxDistanceKm);
        return ResponseEntity.ok(centers);
    }

    /**
     * Get government body by ID.
     * 
     * GET /api/v1/location/government-bodies/{id}
     */
    @GetMapping("/government-bodies/{id}")
    public ResponseEntity<GovernmentBodyDto> getGovernmentBodyById(@PathVariable Long id) {
        GovernmentBodyDto body = governmentBodyLocatorService.getGovernmentBodyById(id);
        if (body == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(body);
    }

    /**
     * Get KVKs by state.
     * 
     * GET /api/v1/location/kvks/state/{state}
     */
    @GetMapping("/kvks/state/{state}")
    public ResponseEntity<List<GovernmentBodyDto>> getKvksByState(@PathVariable String state) {
        List<GovernmentBodyDto> kvks = governmentBodyLocatorService.getKvksByState(state);
        return ResponseEntity.ok(kvks);
    }

    /**
     * Health check endpoint.
     * 
     * GET /api/v1/location/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Location service is healthy");
    }
}