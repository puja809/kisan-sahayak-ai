package com.farmer.user.service;

import com.farmer.user.dto.*;
import com.farmer.user.entity.User;
import com.farmer.user.repository.UserRepository;
import com.farmer.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for user authentication and profile management.
 * Requirements: 11.1, 11.2, 11.3, 11.6, 11.7, 11.8, 11.9, 11A.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AgriStackService agristackService;

    private static final String SUPPORT_CONTACT = "+91-1800-XXX-XXXX";

    /**
     * Register a new user.
     * Requirements: 11.1, 11A.1
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with phone: {}", request.getPhone());

        // Check if user already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AuthenticationException("PHONE_EXISTS", "A user with this phone number already exists");
        }

        // Hash Aadhaar number for privacy
        String aadhaarHash = hashAadhaar(request.getAadhaarHash());

        if (userRepository.existsByAadhaarHash(aadhaarHash)) {
            throw new AuthenticationException("AADHAAR_EXISTS", "A user with this Aadhaar number already exists");
        }

        // Generate unique farmer ID
        String farmerId = generateFarmerId();

        // Create user entity
        User user = User.builder()
                .farmerId(farmerId)
                .aadhaarHash(aadhaarHash)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "en")
                .state(request.getState())
                .district(request.getDistrict())
                .village(request.getVillage())
                .pinCode(request.getPinCode())
                .gpsLatitude(request.getGpsLatitude())
                .gpsLongitude(request.getGpsLongitude())
                .totalLandholdingAcres(request.getTotalLandholdingAcres())
                .soilType(request.getSoilType())
                .irrigationType(request.getIrrigationType())
                .role(User.Role.FARMER)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", farmerId);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken, true);
    }

    /**
     * Authenticate user with phone and OTP.
     * Requirements: 11.1, 11.2
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for phone: {}", request.getPhone());

        // Find user by phone
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AuthenticationException("USER_NOT_FOUND", "User not found with this phone number"));

        // Check if user is active
        if (!user.getIsActive()) {
            throw new AuthenticationException("ACCOUNT_INACTIVE", "Your account has been deactivated. Please contact support.");
        }

        // Verify OTP (in production, this would validate against SMS service)
        if (!validateOtp(request.getPhone(), request.getOtp())) {
            throw new AuthenticationException("INVALID_OTP", "Invalid OTP. Please try again.");
        }

        // Update last login time
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("Login successful for: {}", user.getFarmerId());

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken, false);
    }

    /**
     * Authenticate admin with phone and password.
     * Requirements: 11.1, 22.3
     */
    @Transactional
    public AuthResponse adminLogin(AdminLoginRequest request) {
        log.info("Admin login attempt for phone: {}", request.getPhone());

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AuthenticationException("USER_NOT_FOUND", "User not found with this phone number"));

        if (user.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("UNAUTHORIZED", "Only admin users can login with password");
        }

        if (!user.getIsActive()) {
            throw new AuthenticationException("ACCOUNT_INACTIVE", "Your account has been deactivated. Please contact support.");
        }

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("INVALID_PASSWORD", "Invalid password. Please try again.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("Admin login successful for: {}", user.getFarmerId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken, false);
    }

    /**
     * Authenticate using AgriStack UFSI.
     * Requirements: 11.1, 11.2
     */
    @Transactional
    public AuthResponse authenticateWithAgriStack(String aadhaarNumber, String phoneNumber) {
        log.info("AgriStack authentication attempt for phone: {}", phoneNumber);

        // Hash Aadhaar for lookup
        String aadhaarHash = hashAadhaar(aadhaarNumber);

        // Try to find existing user
        Optional<User> existingUser = userRepository.findByAadhaarHash(aadhaarHash);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (!user.getIsActive()) {
                throw new AuthenticationException("ACCOUNT_INACTIVE", "Your account has been deactivated.");
            }

            // Update login time
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            return buildAuthResponse(user, accessToken, refreshToken, false);
        }

        // New user - authenticate with AgriStack and create profile
        try {
            AgriStackService.AgriStackAuthResponse authResponse = agristackService
                    .authenticateFarmer(aadhaarNumber, phoneNumber)
                    .blockOptional()
                    .orElseThrow(() -> new AuthenticationException("AGRISTACK_ERROR", "Failed to authenticate with AgriStack"));

            if (!authResponse.getIsAuthenticated()) {
                throw new AuthenticationException("AGRISTACK_AUTH_FAILED", authResponse.getMessage());
            }

            // Create new user with AgriStack data
            String farmerId = generateFarmerId();
            User newUser = User.builder()
                    .farmerId(farmerId)
                    .aadhaarHash(aadhaarHash)
                    .name(authResponse.getFarmerId()) // Would be populated from response
                    .phone(phoneNumber)
                    .agristackFarmerId(authResponse.getFarmerId())
                    .role(User.Role.FARMER)
                    .isActive(true)
                    .build();

            newUser = userRepository.save(newUser);

            // Sync profile from AgriStack registries
            syncAgriStackProfile(newUser);

            String accessToken = jwtService.generateAccessToken(newUser);
            String refreshToken = jwtService.generateRefreshToken(newUser);

            return buildAuthResponse(newUser, accessToken, refreshToken, true);

        } catch (Exception e) {
            log.error("AgriStack authentication failed: {}", e.getMessage());
            throw new AuthenticationException("AGRISTACK_ERROR", "Authentication failed. Please try again.");
        }
    }

    /**
     * Refresh access token using refresh token.
     * Requirements: 11.6
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Token refresh attempt");

        if (!jwtService.validateToken(refreshToken)) {
            throw new AuthenticationException("INVALID_TOKEN", "Invalid refresh token");
        }

        String farmerId = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new AuthenticationException("USER_NOT_FOUND", "User not found"));

        if (!user.getIsActive()) {
            throw new AuthenticationException("ACCOUNT_INACTIVE", "Account is inactive");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, newRefreshToken, false);
    }

    /**
     * Get user profile by farmer ID.
     * Requirements: 11A.1
     */
    public UserResponse getProfile(String farmerId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + farmerId));

        return UserResponse.fromEntity(user);
    }

    /**
     * Update user profile.
     * Requirements: 11A.4, 11A.7
     */
    @Transactional
    public UserResponse updateProfile(String farmerId, UpdateProfileRequest request) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + farmerId));

        // Update fields if provided
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getVillage() != null) {
            user.setVillage(request.getVillage());
        }
        if (request.getPinCode() != null) {
            user.setPinCode(request.getPinCode());
        }
        if (request.getGpsLatitude() != null) {
            user.setGpsLatitude(request.getGpsLatitude());
        }
        if (request.getGpsLongitude() != null) {
            user.setGpsLongitude(request.getGpsLongitude());
        }
        if (request.getPreferredLanguage() != null) {
            user.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getTotalLandholdingAcres() != null) {
            user.setTotalLandholdingAcres(request.getTotalLandholdingAcres());
        }
        if (request.getSoilType() != null) {
            user.setSoilType(request.getSoilType());
        }
        if (request.getIrrigationType() != null) {
            user.setIrrigationType(request.getIrrigationType());
        }

        user = userRepository.save(user);
        log.info("Profile updated for: {}", farmerId);

        return UserResponse.fromEntity(user);
    }

    /**
     * Sync user profile with AgriStack registries.
     * Requirements: 11.2, 11.3, 11.9
     */
    @Transactional
    public void syncAgriStackProfile(User user) {
        if (user.getAgristackFarmerId() == null) {
            log.warn("Cannot sync AgriStack profile: farmer ID not set");
            return;
        }

        try {
            AgriStackProfileResponse profile = agristackService.getFarmerProfile(user.getAgristackFarmerId())
                    .blockOptional()
                    .orElse(null);

            if (profile != null && profile.getSuccess()) {
                // Update user with Farmer Registry data
                if (profile.getFarmerRegistry() != null) {
                    AgriStackProfileResponse.FarmerRegistryInfo registry = profile.getFarmerRegistry();
                    if (user.getName() == null && registry.getName() != null) {
                        user.setName(registry.getName());
                    }
                    if (registry.getState() != null) {
                        user.setState(registry.getState());
                    }
                    if (registry.getDistrict() != null) {
                        user.setDistrict(registry.getDistrict());
                    }
                    if (registry.getVillage() != null) {
                        user.setVillage(registry.getVillage());
                    }
                    if (registry.getPinCode() != null) {
                        user.setPinCode(registry.getPinCode());
                    }
                    if (registry.getLatitude() != null) {
                        user.setGpsLatitude(registry.getLatitude());
                    }
                    if (registry.getLongitude() != null) {
                        user.setGpsLongitude(registry.getLongitude());
                    }
                }

                // Update land holding from Geo Map Registry
                if (profile.getGeoMapRegistry() != null && profile.getGeoMapRegistry().getTotalLandAreaHectares() != null) {
                    double acres = profile.getGeoMapRegistry().getTotalLandAreaHectares() * 2.47105; // Convert hectares to acres
                    user.setTotalLandholdingAcres(acres);
                }

                userRepository.save(user);
                log.info("AgriStack profile synced for: {}", user.getFarmerId());
            }
        } catch (Exception e) {
            log.error("Failed to sync AgriStack profile: {}", e.getMessage());
            // Don't throw - sync is best-effort
        }
    }

    /**
     * Logout user (invalidate token on client side).
     * Requirements: 11.6
     */
    public void logout(String farmerId) {
        log.info("User logged out: {}", farmerId);
        // In a stateless JWT setup, logout is handled client-side
        // For enhanced security, we could add tokens to a blacklist in Redis
    }

    /**
     * Deactivate user account.
     */
    @Transactional
    public void deactivateAccount(String farmerId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + farmerId));

        user.setIsActive(false);
        userRepository.save(user);
        log.info("Account deactivated for: {}", farmerId);
    }

    /**
     * Build authentication response with tokens and user info.
     */
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken, boolean isNewUser) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(UserResponse.fromEntity(user))
                .sessionCreatedAt(LocalDateTime.now())
                .isNewUser(isNewUser)
                .build();
    }

    /**
     * Generate unique farmer ID.
     */
    private String generateFarmerId() {
        return "FARMER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Hash Aadhaar number for secure storage.
     */
    private String hashAadhaar(String aadhaarNumber) {
        if (aadhaarNumber == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(aadhaarNumber.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash Aadhaar number", e);
        }
    }

    /**
     * Validate OTP (simplified - in production, this would validate against SMS service).
     */
    private boolean validateOtp(String phone, String otp) {
        // For demo purposes, accept "123456" as valid OTP
        // In production, this would validate against SMS service or stored OTP
        return "123456".equals(otp);
    }

    /**
     * Custom exception for authentication errors.
     */
    public static class AuthenticationException extends RuntimeException {
        private final String errorCode;

        public AuthenticationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    /**
     * Custom exception for user not found errors.
     */
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Change admin password.
     * Requirements: 22.3
     */
    @Transactional
    public void changePassword(String farmerId, ChangePasswordRequest request) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + farmerId));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthenticationException("INVALID_PASSWORD", "Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AuthenticationException("PASSWORD_MISMATCH", "New password and confirmation do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", farmerId);
    }

    /**
     * Get JWT service for token operations.
     */
    public JwtService getJwtService() {
        return jwtService;
    }
}