package com.farmer.user.controller;

import com.farmer.user.dto.*;
import com.farmer.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * Requirements: 11.1, 11.2, 11.6, 11.7, 11.8
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    private static final String SUPPORT_CONTACT = "+91-1800-XXX-XXXX";

    /**
     * Register a new user.
     * Requirements: 11.1, 11A.1
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for phone: {}", request.getPhone());

        try {
            AuthResponse response = userService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserService.AuthenticationException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .accessToken(null)
                            .refreshToken(null)
                            .build());
        }
    }

    /**
     * Login with phone and OTP.
     * Requirements: 11.1, 11.2
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for phone: {}", request.getPhone());

        try {
            AuthResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (UserService.AuthenticationException e) {
            log.warn("Login failed: {}", e.getMessage());
            ErrorResponse error = ErrorResponse.withSupport(
                    e.getErrorCode(),
                    e.getMessage(),
                    SUPPORT_CONTACT
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Admin login with phone and password.
     * Requirements: 11.1, 22.3
     */
    @PostMapping("/admin-login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        log.info("Admin login request received for phone: {}", request.getPhone());

        try {
            AuthResponse response = userService.adminLogin(request);
            return ResponseEntity.ok(response);
        } catch (UserService.AuthenticationException e) {
            log.warn("Admin login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Authenticate using AgriStack UFSI.
     * Requirements: 11.1, 11.2
     */
    @PostMapping("/agristack")
    public ResponseEntity<AuthResponse> authenticateWithAgriStack(
            @RequestParam String aadhaarNumber,
            @RequestParam String phoneNumber) {
        log.info("AgriStack authentication request for phone: {}", phoneNumber);

        try {
            AuthResponse response = userService.authenticateWithAgriStack(aadhaarNumber, phoneNumber);
            return ResponseEntity.ok(response);
        } catch (UserService.AuthenticationException e) {
            log.warn("AgriStack authentication failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Refresh access token.
     * Requirements: 11.6
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");

        try {
            AuthResponse response = userService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (UserService.AuthenticationException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Logout user.
     * Requirements: 11.6
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        if (token != null) {
            String farmerId = userService.getJwtService() != null ? 
                userService.getJwtService().extractFarmerId(token) : "unknown";
            userService.logout(farmerId);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Change admin password.
     * Requirements: 22.3
     */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        String token = extractToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String farmerId = userService.getJwtService().extractFarmerId(token);
        try {
            userService.changePassword(farmerId, request);
            return ResponseEntity.ok().build();
        } catch (UserService.AuthenticationException e) {
            log.warn("Password change failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Extract token from Authorization header.
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}