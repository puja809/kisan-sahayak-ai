package com.farmer.location.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for location requests (GPS coordinates or district/state).
 * 
 * Validates: Requirements 14.1, 14.2, 14.3
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequestDto {

    /**
     * GPS latitude coordinate (India: 6° to 37° N)
     */
    @Min(value = 6, message = "Latitude must be at least 6.0 for India")
    @Max(value = 37, message = "Latitude must be at most 37.0 for India")
    private Double latitude;

    /**
     * GPS longitude coordinate (India: 68° to 97° E)
     */
    @Min(value = 68, message = "Longitude must be at least 68.0 for India")
    @Max(value = 97, message = "Longitude must be at most 97.0 for India")
    private Double longitude;

    /**
     * District name for manual location entry
     */
    private String district;

    /**
     * State name for manual location entry
     */
    private String state;

    /**
     * User ID for tracking location history
     */
    private Long userId;

    /**
     * Flag to indicate if this is a manual location entry
     */
    @Builder.Default
    private Boolean isManual = false;

    /**
     * Flag to indicate if location permission should be requested
     */
    @Builder.Default
    private Boolean requestPermission = false;

    /**
     * Check if this is a coordinate-based request
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    /**
     * Check if this is a district/state-based request
     */
    public boolean hasDistrictState() {
        return district != null && state != null;
    }

    /**
     * Check if this is a valid location request
     */
    public boolean isValid() {
        return hasCoordinates() || hasDistrictState();
    }
}