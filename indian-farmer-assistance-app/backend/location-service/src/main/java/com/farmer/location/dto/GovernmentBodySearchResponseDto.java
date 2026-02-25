package com.farmer.location.dto;

import lombok.*;
import java.util.List;

/**
 * DTO for government body search response.
 * 
 * Validates: Requirements 7.2, 7.3, 7.4, 7.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentBodySearchResponseDto {

    /**
     * Success status of the search
     */
    private boolean success;

    /**
     * Error message if the search failed
     */
    private String errorMessage;

    /**
     * Search parameters used
     */
    private Double searchLatitude;
    private Double searchLongitude;
    private Double searchRadiusKm;

    /**
     * Total number of bodies found
     */
    private int totalFound;

    /**
     * Number of bodies within the specified radius
     */
    private int withinRadius;

    /**
     * List of government bodies found
     */
    private List<GovernmentBodyDto> governmentBodies;

    /**
     * Summary by body type
     */
    private int kvkCount;
    private int districtOfficeCount;
    private int stateDepartmentCount;
    private int atariCount;

    /**
     * Timestamp of the search
     */
    private java.time.LocalDateTime timestamp;

    /**
     * Create a successful response
     */
    public static GovernmentBodySearchResponseDto success(
            GovernmentBodySearchRequestDto request,
            List<GovernmentBodyDto> bodies) {
        
        int kvkCount = 0;
        int districtOfficeCount = 0;
        int stateDepartmentCount = 0;
        int atariCount = 0;
        int withinRadius = 0;

        for (GovernmentBodyDto body : bodies) {
            if (body.getDistanceKm() != null && body.getDistanceKm() <= request.getMaxDistanceKm()) {
                withinRadius++;
            }
            switch (body.getBodyType()) {
                case KVK -> kvkCount++;
                case DISTRICT_AGRICULTURE_OFFICE -> districtOfficeCount++;
                case STATE_DEPARTMENT -> stateDepartmentCount++;
                case ATARI -> atariCount++;
            }
        }

        return GovernmentBodySearchResponseDto.builder()
                .success(true)
                .searchLatitude(request.getLatitude())
                .searchLongitude(request.getLongitude())
                .searchRadiusKm(request.getMaxDistanceKm())
                .totalFound(bodies.size())
                .withinRadius(withinRadius)
                .governmentBodies(bodies)
                .kvkCount(kvkCount)
                .districtOfficeCount(districtOfficeCount)
                .stateDepartmentCount(stateDepartmentCount)
                .atariCount(atariCount)
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response
     */
    public static GovernmentBodySearchResponseDto error(String message) {
        return GovernmentBodySearchResponseDto.builder()
                .success(false)
                .errorMessage(message)
                .timestamp(java.time.LocalDateTime.now())
                .build();
    }
}