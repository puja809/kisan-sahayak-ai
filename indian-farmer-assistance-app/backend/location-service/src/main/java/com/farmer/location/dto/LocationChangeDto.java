package com.farmer.location.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for location change detection response.
 * 
 * Validates: Requirement 14.4 (location change detection with >10km threshold)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationChangeDto {

    /**
     * User ID
     */
    private Long userId;

    /**
     * Previous location information
     */
    private LocationResponseDto previousLocation;

    /**
     * Current location information
     */
    private LocationResponseDto currentLocation;

    /**
     * Distance between locations in kilometers
     */
    private Double distanceKm;

    /**
     * Threshold for significant change (default 10km as per Requirement 14.4)
     */
    @Builder.Default
    private Double thresholdKm = 10.0;

    /**
     * Flag indicating if this is a significant change
     */
    private boolean isSignificantChange;

    /**
     * Previous district (if changed)
     */
    private String previousDistrict;

    /**
     * Current district (if changed)
     */
    private String currentDistrict;

    /**
     * Previous state (if changed)
     */
    private String previousState;

    /**
     * Current state (if changed)
     */
    private String currentState;

    /**
     * Flag indicating if district changed
     */
    private boolean districtChanged;

    /**
     * Flag indicating if state changed
     */
    private boolean stateChanged;

    /**
     * Timestamp of the location change detection
     */
    private LocalDateTime detectedAt;

    /**
     * Suggested actions to take due to location change
     */
    private String suggestedActions;

    /**
     * Check if location-dependent information should be updated
     */
    public boolean shouldUpdateLocationDependentInfo() {
        return isSignificantChange || districtChanged || stateChanged;
    }

    /**
     * Create a significant change response
     */
    public static LocationChangeDto significantChange(
            Long userId,
            LocationResponseDto previous,
            LocationResponseDto current,
            double distanceKm) {
        
        boolean districtChanged = false;
        boolean stateChanged = false;

        if (previous.getDistrict() != null && current.getDistrict() != null) {
            districtChanged = !previous.getDistrict().equalsIgnoreCase(current.getDistrict());
        }

        if (previous.getState() != null && current.getState() != null) {
            stateChanged = !previous.getState().equalsIgnoreCase(current.getState());
        }

        return LocationChangeDto.builder()
                .userId(userId)
                .previousLocation(previous)
                .currentLocation(current)
                .distanceKm(distanceKm)
                .isSignificantChange(distanceKm > 10.0)
                .previousDistrict(previous.getDistrict())
                .currentDistrict(current.getDistrict())
                .previousState(previous.getState())
                .currentState(current.getState())
                .districtChanged(districtChanged)
                .stateChanged(stateChanged)
                .detectedAt(LocalDateTime.now())
                .suggestedActions(generateSuggestedActions(previous, current, distanceKm))
                .build();
    }

    /**
     * Create a no-change response
     */
    public static LocationChangeDto noChange(
            Long userId,
            LocationResponseDto previous,
            LocationResponseDto current,
            double distanceKm) {
        
        return LocationChangeDto.builder()
                .userId(userId)
                .previousLocation(previous)
                .currentLocation(current)
                .distanceKm(distanceKm)
                .isSignificantChange(false)
                .previousDistrict(previous.getDistrict())
                .currentDistrict(current.getDistrict())
                .previousState(previous.getState())
                .currentState(current.getState())
                .districtChanged(false)
                .stateChanged(false)
                .detectedAt(LocalDateTime.now())
                .suggestedActions("No significant location change detected. Current location-dependent information remains valid.")
                .build();
    }

    private static String generateSuggestedActions(
            LocationResponseDto previous,
            LocationResponseDto current,
            double distanceKm) {
        
        StringBuilder actions = new StringBuilder();
        
        if (distanceKm > 10.0) {
            actions.append("Location changed by ").append(String.format("%.1f", distanceKm))
                   .append(" km. ");
        }

        if (previous.getDistrict() != null && current.getDistrict() != null &&
            !previous.getDistrict().equalsIgnoreCase(current.getDistrict())) {
            actions.append("District changed from ").append(previous.getDistrict())
                   .append(" to ").append(current.getDistrict()).append(". ");
            actions.append("Update crop recommendations for new district. ");
            actions.append("Update weather forecasts for new district. ");
            actions.append("Update scheme information for new district. ");
        }

        if (previous.getState() != null && current.getState() != null &&
            !previous.getState().equalsIgnoreCase(current.getState())) {
            actions.append("State changed from ").append(previous.getState())
                   .append(" to ").append(current.getState()).append(". ");
            actions.append("Update state-specific government schemes. ");
        }

        if (actions.length() == 0) {
            actions.append("Location-dependent information has been updated automatically.");
        }

        return actions.toString();
    }
}