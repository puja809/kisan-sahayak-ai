package com.farmer.crop.controller;

import com.farmer.crop.dto.DiseaseDetectionResultDto;
import com.farmer.crop.dto.DiseaseDetectionStorageDto;
import com.farmer.crop.entity.DiseaseDetection;
import com.farmer.crop.service.DiseaseDetectionService;
import com.farmer.crop.service.DiseaseDetectionStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for disease detection operations.
 * 
 * Handles storage and retrieval of disease detection results.
 */
@RestController
@RequestMapping("/api/v1/crops/disease")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Disease Detection", description = "Plant disease detection and management endpoints")
public class DiseaseDetectionController {

    private final DiseaseDetectionStorageService storageService;
    private final DiseaseDetectionService detectionService;

    /**
     * Store a disease detection result.
     * 
     * @param storageDto The disease detection data
     * @return ResponseEntity with the stored detection
     */
    @PostMapping("/store")
    @Operation(summary = "Store disease detection", description = "Stores a disease detection result for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Detection stored successfully",
            content = @Content(schema = @Schema(implementation = DiseaseDetection.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<DiseaseDetection> storeDiseaseDetection(
            @Parameter(description = "Disease detection data") @RequestBody DiseaseDetectionStorageDto storageDto) {
        log.info("Storing disease detection for user: {}", storageDto.getUserId());
        
        DiseaseDetection stored = storageService.storeDiseaseDetection(storageDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(stored);
    }

    /**
     * Get all disease detections for a user.
     * 
     * @param userId The user ID
     * @return ResponseEntity with list of detections
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user disease detections", description = "Retrieves all disease detections for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detections retrieved successfully",
            content = @Content(schema = @Schema(implementation = DiseaseDetection.class)))
    })
    public ResponseEntity<List<DiseaseDetection>> getUserDetections(
            @Parameter(description = "The user ID") @PathVariable Long userId) {
        log.info("Retrieving disease detections for user: {}", userId);
        
        List<DiseaseDetection> detections = storageService.getDiseaseDetectionsForUser(userId);
        return ResponseEntity.ok(detections);
    }

    /**
     * Get all disease detections for a crop.
     * 
     * @param cropId The crop ID
     * @return ResponseEntity with list of detections
     */
    @GetMapping("/crop/{cropId}")
    @Operation(summary = "Get crop disease detections", description = "Retrieves all disease detections for a crop")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detections retrieved successfully",
            content = @Content(schema = @Schema(implementation = DiseaseDetection.class)))
    })
    public ResponseEntity<List<DiseaseDetection>> getCropDetections(
            @Parameter(description = "The crop ID") @PathVariable Long cropId) {
        log.info("Retrieving disease detections for crop: {}", cropId);
        
        List<DiseaseDetection> detections = storageService.getDiseaseDetectionsForCrop(cropId);
        return ResponseEntity.ok(detections);
    }

    /**
     * Get a specific disease detection by ID.
     * 
     * @param detectionId The detection ID
     * @return ResponseEntity with the detection
     */
    @GetMapping("/{detectionId}")
    @Operation(summary = "Get disease detection by ID", description = "Retrieves a specific disease detection by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detection retrieved successfully",
            content = @Content(schema = @Schema(implementation = DiseaseDetection.class))),
        @ApiResponse(responseCode = "404", description = "Detection not found")
    })
    public ResponseEntity<DiseaseDetection> getDetection(
            @Parameter(description = "The detection ID") @PathVariable Long detectionId) {
        log.info("Retrieving disease detection with ID: {}", detectionId);
        
        Optional<DiseaseDetection> detection = storageService.getDiseaseDetectionById(detectionId);
        return detection.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get the most recent disease detection for a crop.
     * 
     * @param cropId The crop ID
     * @return ResponseEntity with the most recent detection
     */
    @GetMapping("/crop/{cropId}/latest")
    @Operation(summary = "Get latest disease detection", description = "Retrieves the most recent disease detection for a crop")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detection retrieved successfully",
            content = @Content(schema = @Schema(implementation = DiseaseDetection.class))),
        @ApiResponse(responseCode = "404", description = "No detections found")
    })
    public ResponseEntity<DiseaseDetection> getLatestDetection(
            @Parameter(description = "The crop ID") @PathVariable Long cropId) {
        log.info("Retrieving latest disease detection for crop: {}", cropId);
        
        Optional<DiseaseDetection> detection = storageService.getMostRecentDetectionForCrop(cropId);
        return detection.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Delete a disease detection.
     * 
     * @param detectionId The detection ID
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/{detectionId}")
    @Operation(summary = "Delete disease detection", description = "Deletes a disease detection by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Detection deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Detection not found")
    })
    public ResponseEntity<Void> deleteDetection(
            @Parameter(description = "The detection ID") @PathVariable Long detectionId) {
        log.info("Deleting disease detection with ID: {}", detectionId);
        
        storageService.deleteDiseaseDetection(detectionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Rank disease detections by confidence score.
     * 
     * @param detections List of detections to rank
     * @return ResponseEntity with ranked detections
     */
    @PostMapping("/rank/confidence")
    @Operation(summary = "Rank by confidence", description = "Ranks disease detections by confidence score")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rankings generated successfully",
            content = @Content(schema = @Schema(implementation = DiseaseDetectionResultDto.class)))
    })
    public ResponseEntity<List<DiseaseDetectionResultDto>> rankByConfidence(
            @Parameter(description = "List of detections to rank") @RequestBody List<DiseaseDetectionResultDto> detections) {
        log.info("Ranking {} disease detections by confidence", detections.size());
        
        List<DiseaseDetectionResultDto> ranked = detectionService.rankByConfidence(detections);
        return ResponseEntity.ok(ranked);
    }

    /**
     * Rank disease detections by severity level.
     * 
     * @param detections List of detections to rank
     * @return ResponseEntity with ranked detections
     */
    @PostMapping("/rank/severity")
    @Operation(summary = "Rank by severity", description = "Ranks disease detections by severity level")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rankings generated successfully",
            content = @Content(schema = @Schema(implementation = DiseaseDetectionResultDto.class)))
    })
    public ResponseEntity<List<DiseaseDetectionResultDto>> rankBySeverity(
            @Parameter(description = "List of detections to rank") @RequestBody List<DiseaseDetectionResultDto> detections) {
        log.info("Ranking {} disease detections by severity", detections.size());
        
        List<DiseaseDetectionResultDto> ranked = detectionService.rankBySeverity(detections);
        return ResponseEntity.ok(ranked);
    }
}
