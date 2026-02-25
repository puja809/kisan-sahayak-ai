package com.farmer.user.controller;

import com.farmer.user.dto.*;
import com.farmer.user.entity.Crop;
import com.farmer.user.entity.FertilizerApplication;
import com.farmer.user.entity.ProfileVersion;
import com.farmer.user.service.ProfileDashboardService;
import com.farmer.user.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for farmer profile management.
 * Requirements: 11A.2, 11A.3, 11A.4, 11A.5, 11A.7, 11A.8, 11A.10, 11A.11, 11A.12
 */
@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileDashboardService profileDashboardService;

    // ==================== Dashboard ====================

    /**
     * Get farmer profile dashboard with aggregated data.
     * Requirements: 11A.8
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ProfileDashboardResponse> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Dashboard request for farmer: {}", farmerId);

        ProfileDashboardResponse dashboard = profileDashboardService.getDashboard(farmerId);
        return ResponseEntity.ok(dashboard);
    }

    // ==================== Farm Management ====================

    /**
     * Get all farms for a farmer.
     * Requirements: 11A.10
     */
    @GetMapping("/farms")
    public ResponseEntity<List<FarmResponse>> getFarms(
            @AuthenticationPrincipal UserDetails userDetails) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Get farms request for farmer: {}", farmerId);

        List<FarmResponse> farms = profileService.getFarms(farmerId);
        return ResponseEntity.ok(farms);
    }

    /**
     * Get a specific farm.
     * Requirements: 11A.10
     */
    @GetMapping("/farms/{farmId}")
    public ResponseEntity<FarmResponse> getFarm(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long farmId) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Get farm request for farmer: {}, farm: {}", farmerId, farmId);

        FarmResponse farm = profileService.getFarm(farmerId, farmId);
        return ResponseEntity.ok(farm);
    }

    /**
     * Create a new farm.
     * Requirements: 11A.2, 11A.3, 11A.10
     */
    @PostMapping("/farms")
    public ResponseEntity<FarmResponse> createFarm(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FarmRequest request) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Create farm request for farmer: {}", farmerId);

        FarmResponse farm = profileService.createFarm(farmerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(farm);
    }

    /**
     * Update a farm.
     * Requirements: 11A.7, 11A.10
     */
    @PutMapping("/farms/{farmId}")
    public ResponseEntity<FarmResponse> updateFarm(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long farmId,
            @Valid @RequestBody FarmRequest request) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Update farm request for farmer: {}, farm: {}", farmerId, farmId);

        FarmResponse farm = profileService.updateFarm(farmerId, farmId, request);
        return ResponseEntity.ok(farm);
    }

    /**
     * Delete a farm.
     * Requirements: 11A.10
     */
    @DeleteMapping("/farms/{farmId}")
    public ResponseEntity<Void> deleteFarm(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long farmId) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Delete farm request for farmer: {}, farm: {}", farmerId, farmId);

        profileService.deleteFarm(farmerId, farmId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Crop Management ====================

    /**
     * Get all crops for a farmer.
     * Requirements: 11A.4
     */
    @GetMapping("/crops")
    public ResponseEntity<List<CropResponse>> getCrops(
            @AuthenticationPrincipal UserDetails userDetails) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Get crops request for farmer: {}", farmerId);

        List<CropResponse> crops = profileService.getCrops(farmerId);
        return ResponseEntity.ok(crops);
    }

    /**
     * Get current (active) crops for a farmer.
     * Requirements: 11A.4, 11A.8
     */
    @GetMapping("/crops/current")
    public ResponseEntity<List<CropResponse>> getCurrentCrops(
            @AuthenticationPrincipal UserDetails userDetails) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Get current crops request for farmer: {}", farmerId);

        List<CropResponse> crops = profileService.getCurrentCrops(farmerId);
        return ResponseEntity.ok(crops);
    }

    /**
     * Get a specific crop.
     * Requirements: 11A.4
     */
    @GetMapping("/crops/{cropId}")
    public ResponseEntity<CropResponse> getCrop(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cropId) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Get crop request for farmer: {}, crop: {}", farmerId, cropId);

        CropResponse crop = profileService.getCrop(farmerId, cropId);
        return ResponseEntity.ok(crop);
    }

    /**
     * Add a new crop.
     * Requirements: 11A.4
     */
    @PostMapping("/crops")
    public ResponseEntity<CropResponse> addCrop(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CropRequest request) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Add crop request for farmer: {}", farmerId);

        CropResponse crop = profileService.addCrop(farmerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(crop);
    }

    /**
     * Update a crop.
     * Requirements: 11A.4, 11A.7
     */
    @PutMapping("/crops/{cropId}")
    public ResponseEntity<CropResponse> updateCrop(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cropId,
            @Valid @RequestBody CropRequest request) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Update crop request for farmer: {}, crop: {}", farmerId, cropId);

        CropResponse crop = profileService.updateCrop(farmerId, cropId, request);
        return ResponseEntity.ok(crop);
    }

    /**
     * Record harvest data for a crop.
     * Requirements: 11A.5
     */
    @PostMapping("/crops/{cropId}/harvest")
    public ResponseEntity<CropResponse> recordHarvest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cropId,
            @Valid @RequestBody HarvestRequest request) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Record harvest request for farmer: {}, crop: {}", farmerId, cropId);

        request.setCropId(cropId);
        CropResponse crop = profileService.recordHarvest(farmerId, request);
        return ResponseEntity.ok(crop);
    }

    /**
     * Delete a crop.
     * Requirements: 11A.4
     */
    @DeleteMapping("/crops/{cropId}")
    public ResponseEntity<Void> deleteCrop(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cropId) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Delete crop request for farmer: {}, crop: {}", farmerId, cropId);

        profileService.deleteCrop(farmerId, cropId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Fertilizer Management ====================

    /**
     * Get fertilizer applications for a crop.
     * Requirements: 11A.4
     */
    @GetMapping("/crops/{cropId}/fertilizers")
    public ResponseEntity<List<FertilizerApplicationResponse>> getFertilizerApplications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cropId) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Get fertilizer applications request for farmer: {}, crop: {}", farmerId, cropId);

        List<FertilizerApplicationResponse> applications = 
                profileService.getFertilizerApplications(farmerId, cropId);
        return ResponseEntity.ok(applications);
    }

    /**
     * Add fertilizer application to a crop.
     * Requirements: 11A.4
     */
    @PostMapping("/crops/{cropId}/fertilizers")
    public ResponseEntity<FertilizerApplicationResponse> addFertilizerApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cropId,
            @Valid @RequestBody FertilizerApplication request) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Add fertilizer application request for farmer: {}, crop: {}", farmerId, cropId);

        FertilizerApplicationResponse application = 
                profileService.addFertilizerApplication(farmerId, cropId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }

    // ==================== Livestock Management ====================

    /**
     * Get all livestock for a farmer.
     * Requirements: 11A.11
     */
    @GetMapping("/livestock")
    public ResponseEntity<List<LivestockResponse>> getLivestock(
            @AuthenticationPrincipal UserDetails userDetails) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Get livestock request for farmer: {}", farmerId);

        List<LivestockResponse> livestock = profileService.getLivestock(farmerId);
        return ResponseEntity.ok(livestock);
    }

    /**
     * Add new livestock.
     * Requirements: 11A.11
     */
    @PostMapping("/livestock")
    public ResponseEntity<LivestockResponse> addLivestock(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LivestockRequest request) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Add livestock request for farmer: {}", farmerId);

        LivestockResponse livestock = profileService.addLivestock(farmerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(livestock);
    }

    /**
     * Update livestock.
     * Requirements: 11A.11
     */
    @PutMapping("/livestock/{livestockId}")
    public ResponseEntity<LivestockResponse> updateLivestock(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long livestockId,
            @Valid @RequestBody LivestockRequest request) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Update livestock request for farmer: {}, livestock: {}", farmerId, livestockId);

        LivestockResponse livestock = profileService.updateLivestock(farmerId, livestockId, request);
        return ResponseEntity.ok(livestock);
    }

    /**
     * Delete livestock.
     * Requirements: 11A.11
     */
    @DeleteMapping("/livestock/{livestockId}")
    public ResponseEntity<Void> deleteLivestock(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long livestockId) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Delete livestock request for farmer: {}, livestock: {}", farmerId, livestockId);

        profileService.deleteLivestock(farmerId, livestockId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Equipment Management ====================

    /**
     * Get all equipment for a farmer.
     * Requirements: 11A.12
     */
    @GetMapping("/equipment")
    public ResponseEntity<List<EquipmentResponse>> getEquipment(
            @AuthenticationPrincipal UserDetails userDetails) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Get equipment request for farmer: {}", farmerId);

        List<EquipmentResponse> equipment = profileService.getEquipment(farmerId);
        return ResponseEntity.ok(equipment);
    }

    /**
     * Add new equipment.
     * Requirements: 11A.12
     */
    @PostMapping("/equipment")
    public ResponseEntity<EquipmentResponse> addEquipment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EquipmentRequest request) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Add equipment request for farmer: {}", farmerId);

        EquipmentResponse equipment = profileService.addEquipment(farmerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(equipment);
    }

    /**
     * Update equipment.
     * Requirements: 11A.12
     */
    @PutMapping("/equipment/{equipmentId}")
    public ResponseEntity<EquipmentResponse> updateEquipment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentRequest request) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Update equipment request for farmer: {}, equipment: {}", farmerId, equipmentId);

        EquipmentResponse equipment = profileService.updateEquipment(farmerId, equipmentId, request);
        return ResponseEntity.ok(equipment);
    }

    /**
     * Delete equipment.
     * Requirements: 11A.12
     */
    @DeleteMapping("/equipment/{equipmentId}")
    public ResponseEntity<Void> deleteEquipment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long equipmentId) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Delete equipment request for farmer: {}, equipment: {}", farmerId, equipmentId);

        profileService.deleteEquipment(farmerId, equipmentId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Version History ====================

    /**
     * Get version history for a farmer.
     * Requirements: 11A.7
     */
    @GetMapping("/history")
    public ResponseEntity<List<ProfileVersion>> getVersionHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        String farmerId = extractFarmerId(userDetails);
        log.info("Get version history request for farmer: {}", farmerId);

        List<ProfileVersion> history = profileService.getVersionHistory(farmerId);
        return ResponseEntity.ok(history);
    }

    // ==================== Helper Methods ====================

    private String extractFarmerId(UserDetails userDetails) {
        if (userDetails instanceof com.farmer.user.security.JwtService.FarmerUserDetails) {
            return ((com.farmer.user.security.JwtService.FarmerUserDetails) userDetails).getFarmerId();
        }
        return userDetails.getUsername();
    }
}