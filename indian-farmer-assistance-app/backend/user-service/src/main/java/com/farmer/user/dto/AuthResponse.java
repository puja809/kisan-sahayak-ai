package com.farmer.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for authentication response containing JWT tokens.
 * Requirements: 11.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * JWT access token for API authentication.
     * Requirements: 11.6
     */
    private String accessToken;

    /**
     * JWT refresh token for obtaining new access tokens.
     * Requirements: 11.6
     */
    private String refreshToken;

    /**
     * Token type (Bearer).
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Token expiration time in seconds.
     */
    private Long expiresIn;

    /**
     * User information.
     */
    private UserResponse user;

    /**
     * Session creation time.
     */
    private LocalDateTime sessionCreatedAt;

    /**
     * Whether this is a new registration.
     */
    private Boolean isNewUser;
}