package com.farmer.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating user profile.
 * Requirements: 11A.4, 11A.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 100, message = "Village name must be less than 100 characters")
    private String village;

    @Pattern(regexp = "^[1-9]\\d{5}$", message = "Invalid PIN code format")
    private String pinCode;

    /**
     * GPS coordinates for location-based services.
     */
    private Double gpsLatitude;
    private Double gpsLongitude;

    /**
     * Preferred language for UI and voice interactions.
     */
    private String preferredLanguage;

    /**
     * Total land holding in acres.
     */
    @Positive(message = "Land holding must be positive")
    private Double totalLandholdingAcres;

    /**
     * Soil type for crop recommendations.
     */
    private String soilType;

    /**
     * Irrigation type for crop recommendations.
     */
    private String irrigationType;
}