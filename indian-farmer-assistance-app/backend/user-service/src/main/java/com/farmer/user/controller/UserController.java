package com.farmer.user.controller;

import com.farmer.user.dto.UpdateProfileRequest;
import com.farmer.user.dto.UserResponse;
import com.farmer.user.entity.User;
import com.farmer.user.security.JwtService;
import com.farmer.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile endpoints.
 * Requirements: 11A.1, 11A.4, 11A.7
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    private static final String SUPPORT_CONTACT = "+91-1800-XXX-XXXX";

    /**
     * Get current user's profile.
     * Requirements: 11A.1
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User user) {
        log.info("Profile request for user: {}", user.getFarmerId());

        try {
            UserResponse response = userService.getProfile(user.getFarmerId());
            return ResponseEntity.ok(response);
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", user.getFarmerId());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update current user's profile.
     * Requirements: 11A.4, 11A.7
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("Profile update request for user: {}", user.getFarmerId());

        try {
            UserResponse response = userService.updateProfile(user.getFarmerId(), request);
            return ResponseEntity.ok(response);
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", user.getFarmerId());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Sync profile with AgriStack registries.
     * Requirements: 11.2, 11.3, 11.9
     */
    @PostMapping("/agristack/sync")
    public ResponseEntity<Void> syncAgriStackProfile(@AuthenticationPrincipal User user) {
        log.info("AgriStack sync request for user: {}", user.getFarmerId());

        try {
            userService.syncAgriStackProfile(user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("AgriStack sync failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Deactivate current user's account.
     */
    @DeleteMapping("/account")
    public ResponseEntity<Void> deactivateAccount(@AuthenticationPrincipal User user) {
        log.info("Account deactivation request for user: {}", user.getFarmerId());

        try {
            userService.deactivateAccount(user.getFarmerId());
            return ResponseEntity.ok().build();
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", user.getFarmerId());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get profile by farmer ID (admin or self).
     * Requirements: 22.3, 22.5
     */
    @GetMapping("/{farmerId}")
    public ResponseEntity<UserResponse> getProfileById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String farmerId) {
        log.info("Profile request by ID: {} for user: {}", farmerId, currentUser.getFarmerId());

        // Check if user is requesting their own profile or is an admin
        if (!farmerId.equals(currentUser.getFarmerId()) && currentUser.getRole() != User.Role.ADMIN) {
            log.warn("Unauthorized profile access attempt: {} tried to access {}",
                    currentUser.getFarmerId(), farmerId);
            return ResponseEntity.status(403).build();
        }

        try {
            UserResponse response = userService.getProfile(farmerId);
            return ResponseEntity.ok(response);
        } catch (UserService.UserNotFoundException e) {
            log.warn("User not found: {}", farmerId);
            return ResponseEntity.notFound().build();
        }
    }
}