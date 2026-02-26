package com.farmer.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for admin password-based login request.
 * Requirements: 11.1, 22.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$|^\\+91[6-9]\\d{9}$", message = "Invalid Indian phone number format")
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;
}
