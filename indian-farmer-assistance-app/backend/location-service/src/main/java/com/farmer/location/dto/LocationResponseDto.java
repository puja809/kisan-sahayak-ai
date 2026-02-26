package com.farmer.location.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for location response with reverse geocoded information.
 * 
 * Validates: Requirements 14.1, 14.2, 14.3
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponseDto {

    /**
     * Success status of the location request
     */
    private boolean success;

    /**
     * Error message if the request failed
     */
    private String errorMessage;

    /**
     * GPS coordinates
     */
    private Double latitude;
    private Double longitude;

    /**
     * Reverse geocoded location information
     */
    private String district;
    private String state;
    private String village;
    private String pinCode;

    /**
     * Agro-ecological zone information
     */
    private String agroEcologicalZone;
    private String zoneCode;
    private String zoneDescription;

    /**
     * Region information
     */
    private String region;

    /**
     * Location accuracy in meters (if from GPS)
     */
    private Double accuracyMeters;

    /**
     * Source of the location data (GPS, NETWORK, MANUAL, CACHE)
     */
    private String locationSource;

    /**
     * Timestamp when the location was determined
     */
    private LocalDateTime timestamp;

    /**
     * Cache timestamp if data was served from cache
     */
    private LocalDateTime cacheTimestamp;

    /**
     * Data source for the location information
     */
    private String dataSource;

    /**
     * Flag indicating if location permission was requested
     */
    private Boolean permissionRequested;

    /**
     * Flag indicating if permission was granted
     */
    private Boolean permissionGranted;

    /**
     * Suggestion message for the user
     */
    private String suggestion;

    /**
     * Create a successful response
     */
    public static LocationResponseDto success(LocationRequestDto request) {
        return LocationResponseDto.builder()
                .success(true)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response
     */
    public static LocationResponseDto error(String message) {
        return LocationResponseDto.builder()
                .success(false)
                .errorMessage(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}