package com.farmer.crop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for location-based zone lookup requests.
 * 
 * Validates: Requirement 2.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequestDto {

    /**
     * District name (optional if GPS coordinates provided)
     */
    private String district;

    /**
     * State name (optional if GPS coordinates provided)
     */
    private String state;

    /**
     * GPS latitude (optional if district/state provided)
     */
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    /**
     * GPS longitude (optional if district/state provided)
     */
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    /**
     * Village name (optional)
     */
    private String village;
}