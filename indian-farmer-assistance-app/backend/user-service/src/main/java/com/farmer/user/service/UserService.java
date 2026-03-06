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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for user authentication and profile management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationException("EMAIL_EXISTS", "A user with this email already exists");
        }

        String farmerId = generateFarmerId();

        User user = User.builder()
                .farmerId(farmerId)
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "en")
                .state(request.getState())
                .district(request.getDistrict())
                .village(request.getVillage())
                .pinCode(request.getPinCode())
                .totalLandholdingAcres(request.getTotalLandholdingAcres())
                .soilType(request.getSoilType())
                .irrigationType(request.getIrrigationType())
                .role(User.Role.FARMER)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", farmerId);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken, true);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("USER_NOT_FOUND", "User not found"));

        if (!user.getIsActive()) {
            throw new AuthenticationException("ACCOUNT_INACTIVE", "Your account has been deactivated.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("INVALID_PASSWORD", "Invalid password");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("Login successful for: {}", user.getFarmerId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken, false);
    }

    @Transactional
    public AuthResponse adminLogin(AdminLoginRequest request) {
        log.info("Admin login attempt: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("USER_NOT_FOUND", "User not found"));

        if (user.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("UNAUTHORIZED", "Only admin users can login here");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("INVALID_PASSWORD", "Invalid password");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken, false);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new AuthenticationException("INVALID_TOKEN", "Invalid refresh token");
        }

        String farmerId = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new AuthenticationException("USER_NOT_FOUND", "User not found"));

        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, newRefreshToken, false);
    }

    public UserResponse getProfile(String farmerId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + farmerId));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateProfile(String farmerId, UpdateProfileRequest request) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + farmerId));

        if (request.getName() != null)
            user.setName(request.getName());
        if (request.getEmail() != null)
            user.setEmail(request.getEmail());
        if (request.getVillage() != null)
            user.setVillage(request.getVillage());
        if (request.getPinCode() != null)
            user.setPinCode(request.getPinCode());
        if (request.getPreferredLanguage() != null)
            user.setPreferredLanguage(request.getPreferredLanguage());
        if (request.getTotalLandholdingAcres() != null)
            user.setTotalLandholdingAcres(request.getTotalLandholdingAcres());

        user = userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    public void logout(String farmerId) {
        log.info("User logged out: {}", farmerId);
    }

    @Transactional
    public void changePassword(String farmerId, ChangePasswordRequest request) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + farmerId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AuthenticationException("INVALID_PASSWORD", "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deactivateAccount(String farmerId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + farmerId));
        user.setIsActive(false);
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken, boolean isNewUser) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(UserResponse.fromEntity(user))
                .isNewUser(isNewUser)
                .build();
    }

    private String generateFarmerId() {
        return "FARMER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public JwtService getJwtService() {
        return jwtService;
    }

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

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}