package com.farmer.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user registration request.
 * Requirements: 11.1, 11A.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number format")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "District is required")
    private String district;

    @Size(max = 100, message = "Village name must be less than 100 characters")
    private String village;

    @Pattern(regexp = "^[1-9]\\d{5}$", message = "Invalid PIN code format")
    private String pinCode;

    @Size(min = 64, max = 64, message = "Aadhaar hash must be 64 characters")
    private String aadhaarHash;

    /**
     * GPS coordinates for location-based services.
     * Requirements: 11A.2
     */
    private Double gpsLatitude;
    private Double gpsLongitude;

    /**
     * Preferred language for UI and voice interactions.
     * Requirements: 11A.1, 8.1
     */
    @Builder.Default
    private String preferredLanguage = "en";

    /**
     * Total land holding in acres.
     * Requirements: 11A.3
     */
    @Positive(message = "Land holding must be positive")
    private Double totalLandholdingAcres;

    /**
     * Soil type for crop recommendations.
     * Requirements: 11A.3
     */
    private String soilType;

    /**
     * Irrigation type for crop recommendations.
     * Requirements: 11A.3
     */
    private String irrigationType;
}