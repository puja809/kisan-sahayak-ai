package com.farmer.location.dto;

import com.farmer.location.entity.GovernmentBody.GovernmentBodyType;
import lombok.*;
import java.util.List;

/**
 * DTO for government body information.
 * 
 * Validates: Requirements 7.2, 7.3, 7.4, 7.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentBodyDto {

    private Long id;
    private GovernmentBodyType bodyType;
    private String name;
    private String address;
    private String district;
    private String state;
    private String pinCode;
    private Double latitude;
    private Double longitude;

    /**
     * Distance from the farmer's location in kilometers
     */
    private Double distanceKm;

    /**
     * Contact information
     */
    private String contactNumber;
    private String email;
    private String website;
    private String operatingHours;

    /**
     * Specialization areas (for KVKs)
     */
    private List<String> specializationAreas;

    /**
     * Senior Scientist and Head contact
     */
    private String seniorScientistHead;

    /**
     * Capabilities and programs
     */
    private String onFarmTestingCapabilities;
    private String frontlineDemonstrationPrograms;
    private String capacityDevelopmentTraining;

    /**
     * Directions URL for map integration
     */
    private String directionsUrl;

    /**
     * Check if this body is within the specified distance
     */
    public boolean isWithinDistance(double maxDistanceKm) {
        return distanceKm != null && distanceKm <= maxDistanceKm;
    }
}