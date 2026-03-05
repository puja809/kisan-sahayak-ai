package com.farmer.user.controller;

import com.farmer.user.dto.CropRequest;
import com.farmer.user.dto.CropResponse;
import com.farmer.user.entity.User;
import com.farmer.user.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user profile management including crops and profile data.
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    // --- Crop Management ---

    @PostMapping("/crops")
    public ResponseEntity<CropResponse> addCrop(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CropRequest request) {
        log.info("Adding crop for user: {}", user.getFarmerId());
        return ResponseEntity.ok(profileService.addCrop(user.getFarmerId(), request));
    }

    @GetMapping("/crops")
    public ResponseEntity<List<CropResponse>> getCrops(@AuthenticationPrincipal User user) {
        log.debug("Fetching crops for user: {}", user.getFarmerId());
        return ResponseEntity.ok(profileService.getUserCrops(user.getFarmerId()));
    }

    @GetMapping("/crops/{cropId}")
    public ResponseEntity<CropResponse> getCrop(
            @AuthenticationPrincipal User user,
            @PathVariable Long cropId) {
        return ResponseEntity.ok(profileService.getCrop(user.getFarmerId(), cropId));
    }

    @PutMapping("/crops/{cropId}")
    public ResponseEntity<CropResponse> updateCrop(
            @AuthenticationPrincipal User user,
            @PathVariable Long cropId,
            @Valid @RequestBody CropRequest request) {
        return ResponseEntity.ok(profileService.updateCrop(user.getFarmerId(), cropId, request));
    }

    @DeleteMapping("/crops/{cropId}")
    public ResponseEntity<Void> deleteCrop(
            @AuthenticationPrincipal User user,
            @PathVariable Long cropId) {
        profileService.deleteCrop(user.getFarmerId(), cropId);
        return ResponseEntity.noContent().build();
    }
}