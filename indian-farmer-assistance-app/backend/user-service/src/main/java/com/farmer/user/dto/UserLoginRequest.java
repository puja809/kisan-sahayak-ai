package com.farmer.user.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user password-based login request.
 * Supports login via phone number or email.
 * Requirements: 11.1, 11.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRequest {

    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private String password;

    /**
     * Validates that either email or phone is provided
     */
    public boolean isValid() {
        return (email != null && !email.isBlank()) || (phone != null && !phone.isBlank());
    }
}
