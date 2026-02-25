package com.farmer.user;

import com.farmer.user.entity.User;
import com.farmer.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JWT service.
 * Requirements: 11.6
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", 
            "indian-farmer-assistance-app-jwt-secret-key-must-be-at-least-256-bits-long");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L);
        ReflectionTestUtils.setField(jwtService, "issuer", "indian-farmer-assistance");
    }

    @Test
    void shouldGenerateAccessToken() {
        // Given
        User user = User.builder()
                .farmerId("FARMER-12345678")
                .name("Test Farmer")
                .phone("9876543210")
                .role(User.Role.FARMER)
                .build();

        // When
        String token = jwtService.generateAccessToken(user);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void shouldGenerateRefreshToken() {
        // Given
        User user = User.builder()
                .farmerId("FARMER-12345678")
                .name("Test Farmer")
                .phone("9876543210")
                .role(User.Role.FARMER)
                .build();

        // When
        String token = jwtService.generateRefreshToken(user);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void shouldExtractUsernameFromToken() {
        // Given
        User user = User.builder()
                .farmerId("FARMER-12345678")
                .name("Test Farmer")
                .phone("9876543210")
                .role(User.Role.FARMER)
                .build();
        String token = jwtService.generateAccessToken(user);

        // When
        String username = jwtService.extractUsername(token);

        // Then
        assertEquals("FARMER-12345678", username);
    }

    @Test
    void shouldExtractRoleFromToken() {
        // Given
        User user = User.builder()
                .farmerId("FARMER-12345678")
                .name("Test Farmer")
                .phone("9876543210")
                .role(User.Role.FARMER)
                .build();
        String token = jwtService.generateAccessToken(user);

        // When
        String role = jwtService.extractRole(token);

        // Then
        assertEquals("FARMER", role);
    }

    @Test
    void shouldExtractFarmerIdFromToken() {
        // Given
        User user = User.builder()
                .farmerId("FARMER-12345678")
                .name("Test Farmer")
                .phone("9876543210")
                .role(User.Role.FARMER)
                .build();
        String token = jwtService.generateAccessToken(user);

        // When
        String farmerId = jwtService.extractFarmerId(token);

        // Then
        assertEquals("FARMER-12345678", farmerId);
    }

    @Test
    void shouldValidateTokenForCorrectUser() {
        // Given
        User user = User.builder()
                .farmerId("FARMER-12345678")
                .name("Test Farmer")
                .phone("9876543210")
                .role(User.Role.FARMER)
                .build();
        String token = jwtService.generateAccessToken(user);

        // When
        boolean isValid = jwtService.validateToken(token, user);

        // Then
        assertTrue(isValid);
    }

    @Test
    void shouldNotValidateTokenForWrongUser() {
        // Given
        User user1 = User.builder()
                .farmerId("FARMER-12345678")
                .name("Test Farmer 1")
                .phone("9876543210")
                .role(User.Role.FARMER)
                .build();
        User user2 = User.builder()
                .farmerId("FARMER-87654321")
                .name("Test Farmer 2")
                .phone("0123456789")
                .role(User.Role.FARMER)
                .build();
        String token = jwtService.generateAccessToken(user1);

        // When
        boolean isValid = jwtService.validateToken(token, user2);

        // Then
        assertFalse(isValid);
    }

    @Test
    void shouldNotValidateExpiredToken() {
        // Given - create a service with very short expiration
        JwtService shortExpiryService = new JwtService();
        ReflectionTestUtils.setField(shortExpiryService, "jwtSecret", 
            "indian-farmer-assistance-app-jwt-secret-key-must-be-at-least-256-bits-long");
        ReflectionTestUtils.setField(shortExpiryService, "jwtExpiration", 1L); // 1ms expiration
        ReflectionTestUtils.setField(shortExpiryService, "refreshExpiration", 604800000L);
        ReflectionTestUtils.setField(shortExpiryService, "issuer", "indian-farmer-assistance");
        
        User user = User.builder()
                .farmerId("FARMER-12345678")
                .name("Test Farmer")
                .phone("9876543210")
                .role(User.Role.FARMER)
                .build();
        String token = shortExpiryService.generateAccessToken(user);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean isValid = shortExpiryService.validateToken(token);

        // Then
        assertFalse(isValid);
    }

    @Test
    void shouldNotValidateInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtService.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void shouldReturnCorrectExpirationTime() {
        // When
        long expirationSeconds = jwtService.getExpirationSeconds();

        // Then
        assertEquals(86400, expirationSeconds); // 24 hours in seconds
    }

    @Test
    void shouldReturnCorrectRefreshExpirationTime() {
        // When
        long refreshExpirationSeconds = jwtService.getRefreshExpirationSeconds();

        // Then
        assertEquals(604800, refreshExpirationSeconds); // 7 days in seconds
    }
}