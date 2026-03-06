package com.farmer.user.controller;

import com.farmer.user.dto.UpdateProfileRequest;
import com.farmer.user.dto.UserResponse;
import com.farmer.user.entity.User;
import com.farmer.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile endpoints.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user.getFarmerId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(user.getFarmerId(), request));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deactivateAccount(@AuthenticationPrincipal User user) {
        userService.deactivateAccount(user.getFarmerId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{farmerId}")
    public ResponseEntity<UserResponse> getProfileById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String farmerId) {

        if (!farmerId.equals(currentUser.getFarmerId()) && currentUser.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(userService.getProfile(farmerId));
    }
}