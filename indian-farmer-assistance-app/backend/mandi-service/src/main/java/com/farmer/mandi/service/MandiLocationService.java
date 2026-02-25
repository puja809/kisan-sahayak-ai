package com.farmer.mandi.service;

import com.farmer.mandi.dto.MandiLocationDto;
import com.farmer.mandi.dto.MandiPriceDto;
import com.farmer.mandi.entity.MandiLocation;
import com.farmer.mandi.repository.MandiLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing mandi locations with distance-based sorting.
 * 
 * Requirements:
 * - 6.4: Sort mandis by distance from farmer's location
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MandiLocationService {

    private final MandiLocationRepository mandiLocationRepository;

    // Earth's radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Gets nearby mandis sorted by distance from farmer's location.
     * 
     * Property 11: Distance-Based Ascending Sort
     * Validates: Requirements 6.4, 7.2
     * 
     * For any list of locations (mandis, government bodies, KVKs) sorted by distance 
     * from a farmer's location, for any two adjacent items in the list, the first 
     * should have a distance less than or equal to the second.
     * 
     * @param latitude Farmer's latitude
     * @param longitude Farmer's longitude
     * @param radiusKm Search radius in kilometers
     * @return List of MandiLocationDto sorted by distance
     */
    public List<MandiLocationDto> getNearbyMandis(BigDecimal latitude, BigDecimal longitude, int radiusKm) {
        log.info("Finding mandis within {} km of location: {}, {}", radiusKm, latitude, longitude);
        
        // Calculate bounding box for initial filtering
        double[] boundingBox = calculateBoundingBox(latitude.doubleValue(), longitude.doubleValue(), radiusKm);
        
        // Find locations in bounding box
        List<MandiLocation> locations = mandiLocationRepository.findLocationsInBoundingBox(
                BigDecimal.valueOf(boundingBox[0]),
                BigDecimal.valueOf(boundingBox[1]),
                BigDecimal.valueOf(boundingBox[2]),
                BigDecimal.valueOf(boundingBox[3])
        );
        
        // Calculate distance and filter
        List<MandiLocationDto> result = locations.stream()
                .map(location -> {
                    BigDecimal distance = calculateDistance(latitude, longitude, 
                            location.getLatitude(), location.getLongitude());
                    return mapToDto(location, distance);
                })
                .filter(location -> location.getDistanceKm().compareTo(BigDecimal.valueOf(radiusKm)) <= 0)
                .sorted(Comparator.comparing(MandiLocationDto::getDistanceKm))
                .collect(Collectors.toList());
        
        log.info("Found {} mandis within {} km", result.size(), radiusKm);
        return result;
    }

    /**
     * Sorts mandi prices by distance from farmer's location.
     * 
     * @param prices List of mandi prices
     * @param farmerLatitude Farmer's latitude
     * @param farmerLongitude Farmer's longitude
     * @return List of MandiPriceDto sorted by distance
     */
    public List<MandiPriceDto> sortPricesByDistance(
            List<MandiPriceDto> prices, 
            BigDecimal farmerLatitude, 
            BigDecimal farmerLongitude) {
        
        if (prices == null || prices.isEmpty()) {
            return prices;
        }

        // Sort by distance
        return prices.stream()
                .map(price -> {
                    BigDecimal distance = calculateDistanceForMandi(
                            farmerLatitude, farmerLongitude, price.getMandiName());
                    return MandiPriceDto.builder()
                            .id(price.getId())
                            .commodityName(price.getCommodityName())
                            .variety(price.getVariety())
                            .mandiName(price.getMandiName())
                            .mandiCode(price.getMandiCode())
                            .state(price.getState())
                            .district(price.getDistrict())
                            .priceDate(price.getPriceDate())
                            .modalPrice(price.getModalPrice())
                            .minPrice(price.getMinPrice())
                            .maxPrice(price.getMaxPrice())
                            .arrivalQuantityQuintals(price.getArrivalQuantityQuintals())
                            .unit(price.getUnit())
                            .source(price.getSource())
                            .fetchedAt(price.getFetchedAt())
                            .distanceKm(distance)
                            .isCached(price.getIsCached())
                            .build();
                })
                .sorted(Comparator.comparing(
                        dto -> dto.getDistanceKm() != null ? dto.getDistanceKm() : BigDecimal.valueOf(Double.MAX_VALUE)))
                .collect(Collectors.toList());
    }

    /**
     * Gets all active mandi locations.
     * 
     * @return List of MandiLocationDto
     */
    public List<MandiLocationDto> getAllActiveLocations() {
        return mandiLocationRepository.findByIsActiveTrueOrderByMandiName()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets locations by state.
     * 
     * @param state The state name
     * @return List of MandiLocationDto
     */
    public List<MandiLocationDto> getLocationsByState(String state) {
        return mandiLocationRepository.findByStateAndIsActiveTrueOrderByMandiName(state)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets distinct states.
     * 
     * @return List of state names
     */
    public List<String> getStates() {
        return mandiLocationRepository.findDistinctStates();
    }

    /**
     * Gets distinct districts for a state.
     * 
     * @param state The state name
     * @return List of district names
     */
    public List<String> getDistricts(String state) {
        return mandiLocationRepository.findDistinctDistrictsByState(state);
    }

    /**
     * Calculates distance between two points using Haversine formula.
     * 
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in kilometers
     */
    public BigDecimal calculateDistance(
            BigDecimal lat1, BigDecimal lon1, 
            BigDecimal lat2, BigDecimal lon2) {
        
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return BigDecimal.valueOf(Double.MAX_VALUE);
        }

        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1.doubleValue())) * 
                   Math.cos(Math.toRadians(lat2.doubleValue())) * 
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;
        
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates distance for a mandi by looking up its location.
     * 
     * @param farmerLatitude Farmer's latitude
     * @param farmerLongitude Farmer's longitude
     * @param mandiName Name of the mandi
     * @return Distance in kilometers
     */
    private BigDecimal calculateDistanceForMandi(
            BigDecimal farmerLatitude, 
            BigDecimal farmerLongitude, 
            String mandiName) {
        
        // This would need to be implemented with proper mandi location lookup
        // For now, return a default value
        return BigDecimal.valueOf(-1);
    }

    /**
     * Calculates a bounding box for a given location and radius.
     * 
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param radiusKm Radius in kilometers
     * @return Array of [minLat, maxLat, minLon, maxLon]
     */
    private double[] calculateBoundingBox(double latitude, double longitude, double radiusKm) {
        // Approximate degrees per km at the equator
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));
        
        return new double[] {
            latitude - latDelta, // minLat
            latitude + latDelta, // maxLat
            longitude - lonDelta, // minLon
            longitude + lonDelta  // maxLon
        };
    }

    /**
     * Maps entity to DTO.
     */
    private MandiLocationDto mapToDto(MandiLocation entity) {
        return MandiLocationDto.builder()
                .mandiCode(entity.getMandiCode())
                .mandiName(entity.getMandiName())
                .state(entity.getState())
                .district(entity.getDistrict())
                .address(entity.getAddress())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .contactNumber(entity.getContactNumber())
                .operatingHours(entity.getOperatingHours())
                .distanceKm(BigDecimal.ZERO)
                .isActive(entity.getIsActive())
                .build();
    }

    /**
     * Maps entity to DTO with distance.
     */
    private MandiLocationDto mapToDto(MandiLocation entity, BigDecimal distance) {
        return MandiLocationDto.builder()
                .mandiCode(entity.getMandiCode())
                .mandiName(entity.getMandiName())
                .state(entity.getState())
                .district(entity.getDistrict())
                .address(entity.getAddress())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .contactNumber(entity.getContactNumber())
                .operatingHours(entity.getOperatingHours())
                .distanceKm(distance)
                .isActive(entity.getIsActive())
                .build();
    }
}