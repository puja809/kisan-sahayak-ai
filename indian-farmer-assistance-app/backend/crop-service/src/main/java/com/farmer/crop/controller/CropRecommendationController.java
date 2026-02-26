package com.farmer.crop.controller;

import com.farmer.crop.dto.*;
import com.farmer.crop.service.CropRecommendationService;
import com.farmer.crop.service.GaezSuitabilityService;
import com.farmer.crop.service.SeedVarietyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for crop recommendations.
 * 
 * Provides endpoints for:
 * - Generating crop recommendations based on location and preferences
 * - Getting crop suitability data for a location
 * - Getting recommended seed varieties for a crop
 * 
 * Validates: Requirements 2.1, 2.2, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10
 */
@RestController
@RequestMapping("/api/v1/crops")
@Tag(name = "Crop Recommendations", description = "Crop recommendation and suitability endpoints")
public class CropRecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(CropRecommendationController.class);

    private final CropRecommendationService cropRecommendationService;
    private final GaezSuitabilityService gaezSuitabilityService;
    private final SeedVarietyService seedVarietyService;

    public CropRecommendationController(
            CropRecommendationService cropRecommendationService,
            GaezSuitabilityService gaezSuitabilityService,
            SeedVarietyService seedVarietyService) {
        this.cropRecommendationService = cropRecommendationService;
        this.gaezSuitabilityService = gaezSuitabilityService;
        this.seedVarietyService = seedVarietyService;
    }

    /**
     * Generate crop recommendations for a farmer.
     * 
     * This endpoint generates personalized crop recommendations based on:
     * - Location (district, state, or GPS coordinates)
     * - Soil health data (if available)
     * - Irrigation type (rain-fed, drip, sprinkler, canal, borewell)
     * - Season (Kharif, Rabi, Zaid)
     * - Market data integration (optional)
     * - Climate risk assessment (optional)
     * 
     * @param request Recommendation request with location and preferences
     * @return Ranked list of recommended crops with suitability scores
     * 
     * Validates: Requirements 2.5, 2.6, 2.7, 2.8, 2.9
     */
    @PostMapping("/recommendations/calculate")
    @Operation(summary = "Generate crop recommendations", description = "Generates personalized crop recommendations based on location and preferences")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recommendations generated successfully",
            content = @Content(schema = @Schema(implementation = CropRecommendationResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing location data")
    })
    public ResponseEntity<CropRecommendationResponseDto> generateRecommendations(
            @RequestBody CropRecommendationRequestDto request) {
        
        logger.info("Received crop recommendation request for farmer: {}", request.getFarmerId());
        
        // Validate request
        if (!request.hasLocationData()) {
            logger.warn("Invalid location data in request");
            return ResponseEntity.badRequest().body(
                    CropRecommendationResponseDto.builder()
                            .success(false)
                            .errorMessage("Location data is required. Provide district/state or GPS coordinates.")
                            .build()
            );
        }

        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(request);
        
        if (response.isSuccess()) {
            logger.info("Generated {} recommendations for farmer: {}", 
                    response.getRecommendationCount(), request.getFarmerId());
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Failed to generate recommendations: {}", response.getErrorMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get crop recommendations for a farmer (GET version with query parameters).
     * 
     * @param farmerId Farmer ID
     * @param district District name
     * @param state State name
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @param irrigationType Irrigation type
     * @param season Season (KHARIF, RABI, ZAID, ALL)
     * @param includeMarketData Whether to include market data
     * @param includeClimateRisk Whether to include climate risk assessment
     * @return Ranked list of recommended crops
     * 
     * Validates: Requirements 2.5, 2.6, 2.7, 2.8, 2.9
     */
    @GetMapping("/recommendations")
    @Operation(summary = "Get crop recommendations (GET)", description = "Generates crop recommendations using query parameters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recommendations generated successfully",
            content = @Content(schema = @Schema(implementation = CropRecommendationResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing location data")
    })
    public ResponseEntity<CropRecommendationResponseDto> getRecommendations(
            @RequestParam(required = false) String farmerId,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) CropRecommendationRequestDto.IrrigationType irrigationType,
            @RequestParam(required = false, defaultValue = "ALL") CropRecommendationRequestDto.Season season,
            @RequestParam(required = false, defaultValue = "true") Boolean includeMarketData,
            @RequestParam(required = false, defaultValue = "true") Boolean includeClimateRisk) {
        
        logger.info("Received GET recommendation request for farmer: {}", farmerId);
        
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId(farmerId)
                .district(district)
                .state(state)
                .latitude(latitude)
                .longitude(longitude)
                .irrigationType(irrigationType)
                .season(season)
                .includeMarketData(includeMarketData)
                .includeClimateRiskAssessment(includeClimateRisk)
                .build();

        if (!request.hasLocationData()) {
            return ResponseEntity.badRequest().body(
                    CropRecommendationResponseDto.builder()
                            .success(false)
                            .errorMessage("Location data is required. Provide district/state or GPS coordinates.")
                            .build()
            );
        }

        CropRecommendationResponseDto response = cropRecommendationService.generateRecommendations(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get crop suitability data for a location.
     * 
     * This endpoint returns GAEZ v4 crop suitability data for the specified location,
     * including climate, soil, terrain, and water suitability scores.
     * 
     * @param location Location identifier (zone code, district, or coordinates)
     * @return Crop suitability data for the location
     * 
     * Validates: Requirements 2.1, 2.2, 2.3
     */
    @GetMapping("/suitability/{location}")
    public ResponseEntity<?> getSuitabilityForLocation(
            @PathVariable String location) {
        
        logger.info("Getting suitability data for location: {}", location);
        
        // Try to interpret the location parameter
        List<GaezCropSuitabilityDto> suitabilityData;
        
        // If it looks like a zone code (e.g., "AEZ-05")
        if (location.startsWith("AEZ-")) {
            suitabilityData = gaezSuitabilityService.getSuitabilityForZone(location);
        } else {
            // For district or other location, we need to first get the zone
            // This is a simplified implementation
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid location format. Please provide a valid zone code (e.g., AEZ-05)"
            ));
        }
        
        if (suitabilityData.isEmpty()) {
            logger.warn("No suitability data found for location: {}", location);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(suitabilityData);
    }

    /**
     * Get recommended seed varieties for a crop.
     * 
     * This endpoint returns state-released seed varieties suitable for the
     * specified crop and state, including variety characteristics and availability.
     * 
     * @param cropName Crop name (e.g., "RICE", "WHEAT")
     * @param state State name (optional, for state-specific recommendations)
     * @return List of recommended seed varieties
     * 
     * Validates: Requirement 2.9
     */
    @GetMapping("/varieties/{cropName}")
    public ResponseEntity<List<SeedVarietyDto>> getVarieties(
            @PathVariable String cropName,
            @RequestParam(required = false) String state) {
        
        logger.info("Getting varieties for crop: {} in state: {}", cropName, state);
        
        List<SeedVarietyDto> varieties;
        
        if (state != null && !state.isEmpty()) {
            varieties = seedVarietyService.getRecommendedVarieties(cropName.toUpperCase(), state);
        } else {
            varieties = seedVarietyService.getAllVarietiesForCrop(cropName.toUpperCase());
        }
        
        if (varieties.isEmpty()) {
            logger.warn("No varieties found for crop: {} in state: {}", cropName, state);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(varieties);
    }

    /**
     * Get drought-tolerant varieties for a crop.
     * 
     * @param cropName Crop name
     * @return List of drought-tolerant varieties
     * 
     * Validates: Requirement 2.9
     */
    @GetMapping("/varieties/{cropName}/drought-tolerant")
    public ResponseEntity<List<SeedVarietyDto>> getDroughtTolerantVarieties(
            @PathVariable String cropName) {
        
        logger.info("Getting drought-tolerant varieties for crop: {}", cropName);
        
        List<SeedVarietyDto> varieties = seedVarietyService.getDroughtTolerantVarieties(cropName.toUpperCase());
        
        if (varieties.isEmpty()) {
            logger.warn("No drought-tolerant varieties found for crop: {}", cropName);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(varieties);
    }

    /**
     * Get flood-tolerant varieties for a crop.
     * 
     * @param cropName Crop name
     * @return List of flood-tolerant varieties
     * 
     * Validates: Requirement 2.9
     */
    @GetMapping("/varieties/{cropName}/flood-tolerant")
    public ResponseEntity<List<SeedVarietyDto>> getFloodTolerantVarieties(
            @PathVariable String cropName) {
        
        logger.info("Getting flood-tolerant varieties for crop: {}", cropName);
        
        List<SeedVarietyDto> varieties = seedVarietyService.getFloodTolerantVarieties(cropName.toUpperCase());
        
        if (varieties.isEmpty()) {
            logger.warn("No flood-tolerant varieties found for crop: {}", cropName);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(varieties);
    }

    /**
     * Get heat-tolerant varieties for a crop.
     * 
     * @param cropName Crop name
     * @return List of heat-tolerant varieties
     * 
     * Validates: Requirement 2.9
     */
    @GetMapping("/varieties/{cropName}/heat-tolerant")
    public ResponseEntity<List<SeedVarietyDto>> getHeatTolerantVarieties(
            @PathVariable String cropName) {
        
        logger.info("Getting heat-tolerant varieties for crop: {}", cropName);
        
        List<SeedVarietyDto> varieties = seedVarietyService.getHeatTolerantVarieties(cropName.toUpperCase());
        
        if (varieties.isEmpty()) {
            logger.warn("No heat-tolerant varieties found for crop: {}", cropName);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(varieties);
    }

    /**
     * Get states where a crop has recommended varieties.
     * 
     * @param cropName Crop name
     * @return List of states with recommended varieties
     * 
     * Validates: Requirement 2.9
     */
    @GetMapping("/varieties/{cropName}/states")
    public ResponseEntity<List<String>> getStatesForCrop(
            @PathVariable String cropName) {
        
        logger.info("Getting states for crop: {}", cropName);
        
        List<String> states = seedVarietyService.getStatesForCrop(cropName.toUpperCase());
        
        if (states.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(states);
    }
}