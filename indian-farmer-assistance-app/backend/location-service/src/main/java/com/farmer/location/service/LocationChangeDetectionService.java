package com.farmer.location.service;

import com.farmer.location.dto.LocationChangeDto;
import com.farmer.location.dto.LocationRequestDto;
import com.farmer.location.dto.LocationResponseDto;
import com.farmer.location.entity.LocationHistory;
import com.farmer.location.repository.LocationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for detecting and handling location changes.
 * 
 * Validates: Requirement 14.4 (location change detection with >10km threshold)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationChangeDetectionService {

    private static final double SIGNIFICANT_CHANGE_THRESHOLD_KM = 10.0;

    private final LocationHistoryRepository locationHistoryRepository;
    private final GpsLocationService gpsLocationService;

    /**
     * Detect location changes for a user.
     * 
     * @param userId User ID
     * @param newLocationRequest New location request
     * @return Location change detection result
     */
    @Transactional
    public LocationChangeDto detectLocationChange(Long userId, LocationRequestDto newLocationRequest) {
        log.info("Detecting location change for user: {}", userId);

        // Get current location
        LocationResponseDto currentLocation = gpsLocationService.getLocation(newLocationRequest);
        if (!currentLocation.isSuccess()) {
            return LocationChangeDto.builder()
                    .userId(userId)
                    .currentLocation(currentLocation)
                    .isSignificantChange(false)
                    .detectedAt(java.time.LocalDateTime.now())
                    .suggestedActions("Unable to determine current location.")
                    .build();
        }

        // Get last known location
        Optional<LocationHistory> lastLocationOpt = locationHistoryRepository.findMostRecentByUserId(userId);
        
        if (lastLocationOpt.isEmpty()) {
            // First location record for user
            gpsLocationService.recordLocationHistory(userId, currentLocation, null);
            return LocationChangeDto.builder()
                    .userId(userId)
                    .currentLocation(currentLocation)
                    .distanceKm(0.0)
                    .isSignificantChange(false)
                    .currentDistrict(currentLocation.getDistrict())
                    .currentState(currentLocation.getState())
                    .detectedAt(java.time.LocalDateTime.now())
                    .suggestedActions("Initial location recorded. Location-dependent information will be updated based on this location.")
                    .build();
        }

        // Calculate distance from last location
        LocationHistory lastLocation = lastLocationOpt.get();
        LocationResponseDto previousLocation = LocationResponseDto.builder()
                .success(true)
                .latitude(lastLocation.getLatitude())
                .longitude(lastLocation.getLongitude())
                .district(lastLocation.getDistrict())
                .state(lastLocation.getState())
                .timestamp(lastLocation.getRecordedAt())
                .locationSource(lastLocation.getLocationSource())
                .build();

        double distanceKm = calculateDistance(
                lastLocation.getLatitude(), lastLocation.getLongitude(),
                currentLocation.getLatitude(), currentLocation.getLongitude()
        );

        // Record the new location
        gpsLocationService.recordLocationHistory(userId, currentLocation, distanceKm);

        // Determine if this is a significant change
        boolean isSignificantChange = distanceKm > SIGNIFICANT_CHANGE_THRESHOLD_KM;
        boolean districtChanged = hasDistrictChanged(previousLocation, currentLocation);
        boolean stateChanged = hasStateChanged(previousLocation, currentLocation);

        if (isSignificantChange || districtChanged || stateChanged) {
            log.info("Significant location change detected for user {}: {} km, district: {}, state: {}",
                    userId, distanceKm, districtChanged, stateChanged);
            return LocationChangeDto.significantChange(userId, previousLocation, currentLocation, distanceKm);
        } else {
            return LocationChangeDto.noChange(userId, previousLocation, currentLocation, distanceKm);
        }
    }

    /**
     * Check if location has changed significantly (>10km).
     * 
     * @param userId User ID
     * @param newLatitude New latitude
     * @param newLongitude New longitude
     * @return true if location has changed significantly
     */
    public boolean hasLocationChangedSignificantly(Long userId, Double newLatitude, Double newLongitude) {
        Optional<LocationHistory> lastLocationOpt = locationHistoryRepository.findMostRecentByUserId(userId);
        
        if (lastLocationOpt.isEmpty()) {
            return true; // First location, consider as significant change
        }

        LocationHistory lastLocation = lastLocationOpt.get();
        double distanceKm = calculateDistance(
                lastLocation.getLatitude(), lastLocation.getLongitude(),
                newLatitude, newLongitude
        );

        return distanceKm > SIGNIFICANT_CHANGE_THRESHOLD_KM;
    }

    /**
     * Get location change history for a user.
     * 
     * @param userId User ID
     * @param limit Maximum number of records to return
     * @return List of location history records
     */
    public List<LocationHistory> getLocationHistory(Long userId, int limit) {
        if (limit > 0) {
            return locationHistoryRepository.findLastNByUserId(userId, limit);
        }
        return locationHistoryRepository.findByUserIdOrderByRecordedAtDesc(userId);
    }

    /**
     * Get significant location changes for a user.
     * 
     * @param userId User ID
     * @return List of significant location changes
     */
    public List<LocationHistory> getSignificantChanges(Long userId) {
        return locationHistoryRepository.findSignificantChangesByUserId(userId);
    }

    /**
     * Calculate distance between two coordinates using Haversine formula.
     * 
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    public double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return 0.0;
        }

        final double R = 6371.0; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Check if district has changed between two locations.
     */
    private boolean hasDistrictChanged(LocationResponseDto previous, LocationResponseDto current) {
        if (previous.getDistrict() == null || current.getDistrict() == null) {
            return false;
        }
        return !previous.getDistrict().equalsIgnoreCase(current.getDistrict());
    }

    /**
     * Check if state has changed between two locations.
     */
    private boolean hasStateChanged(LocationResponseDto previous, LocationResponseDto current) {
        if (previous.getState() == null || current.getState() == null) {
            return false;
        }
        return !previous.getState().equalsIgnoreCase(current.getState());
    }

    /**
     * Get the distance threshold for significant changes.
     */
    public double getSignificantChangeThresholdKm() {
        return SIGNIFICANT_CHANGE_THRESHOLD_KM;
    }
}