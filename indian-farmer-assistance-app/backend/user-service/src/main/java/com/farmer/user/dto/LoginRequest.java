package com.farmer.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user login request.
 * Requirements: 11.1, 11.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number format")
    private String phone;

    @NotBlank(message = "OTP is required")
    private String otp;

    /**
     * Device ID for offline authentication caching.
     * Requirements: 11.8
     */
    private String deviceId;
}