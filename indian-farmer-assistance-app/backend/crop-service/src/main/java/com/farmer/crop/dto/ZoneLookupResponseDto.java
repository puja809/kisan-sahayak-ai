package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for zone lookup response.
 * 
 * Validates: Requirement 2.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneLookupResponseDto {

    /**
     * Whether the lookup was successful
     */
    private boolean success;

    /**
     * Input location that was provided
     */
    private String inputLocation;

    /**
     * District name from input
     */
    private String district;

    /**
     * State name from input
     */
    private String state;

    /**
     * GPS latitude used for lookup (if provided)
     */
    private Double latitude;

    /**
     * GPS longitude used for lookup (if provided)
     */
    private Double longitude;

    /**
     * The agro-ecological zone information
     */
    private AgroEcologicalZoneDto zone;

    /**
     * Error message if lookup failed
     */
    private String errorMessage;

    /**
     * Timestamp of the lookup
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}