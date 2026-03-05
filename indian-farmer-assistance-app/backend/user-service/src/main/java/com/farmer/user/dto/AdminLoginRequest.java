package com.farmer.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for admin password-based login request.
 * Supports login via phone number or email.
 * Requirements: 11.1, 22.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginRequest {

    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Validates that either email or phone is provided
     */
    public boolean isValid() {
        return (email != null && !email.isBlank()) || (phone != null && !phone.isBlank());
    }
}
