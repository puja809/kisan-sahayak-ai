package com.farmer.location.dto;

import com.farmer.location.entity.GovernmentBody.GovernmentBodyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

/**
 * DTO for government body search requests.
 * 
 * Validates: Requirements 7.1, 7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentBodySearchRequestDto {

    /**
     * Farmer's current latitude
     */
    @NotNull(message = "Latitude is required")
    @Min(value = 6.0, message = "Latitude must be at least 6.0 for India")
    @Max(value = 37.0, message = "Latitude must be at most 37.0 for India")
    private Double latitude;

    /**
     * Farmer's current longitude
     */
    @NotNull(message = "Longitude is required")
    @Min(value = 68.0, message = "Longitude must be at least 68.0 for India")
    @Max(value = 97.0, message = "Longitude must be at most 97.0 for India")
    private Double longitude;

    /**
     * Maximum distance in kilometers (default 50km as per Requirement 7.2)
     */
    @Builder.Default
    @Max(value = 100, message = "Maximum search radius cannot exceed 100 km")
    private Double maxDistanceKm = 50.0;

    /**
     * Filter by body types (if null or empty, all types are included)
     */
    private List<GovernmentBodyType> bodyTypes;

    /**
     * Filter by state (if null, all states are included)
     */
    private String state;

    /**
     * Filter by district (if null, all districts are included)
     */
    private String district;

    /**
     * Include KVK specialization areas in response
     */
    @Builder.Default
    private Boolean includeSpecializationAreas = true;

    /**
     * Include contact information in response
     */
    @Builder.Default
    private Boolean includeContactInfo = true;

    /**
     * Include capabilities and programs in response
     */
    @Builder.Default
    private Boolean includeCapabilities = true;

    /**
     * Include directions URL in response
     */
    @Builder.Default
    private Boolean includeDirections = true;
}