package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.AgroEcologicalZone;
import com.farmer.crop.exception.LocationNotFoundException;
import com.farmer.crop.exception.ZoneMappingException;
import com.farmer.crop.repository.AgroEcologicalZoneRepository;
import com.farmer.crop.repository.DistrictZoneMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for mapping locations to ICAR Agro-Ecological Zones.
 * 
 * This service implements GPS to agro-ecological zone mapping using ICAR classification.
 * It supports lookup by district/state name as well as GPS coordinates.
 * 
 * Validates: Requirement 2.1
 */
@Service
@Transactional(readOnly = true)
public class AgroEcologicalZoneService {

    private static final Logger logger = LoggerFactory.getLogger(AgroEcologicalZoneService.class);

    private final AgroEcologicalZoneRepository zoneRepository;
    private final DistrictZoneMappingRepository districtMappingRepository;

    public AgroEcologicalZoneService(
            AgroEcologicalZoneRepository zoneRepository,
            DistrictZoneMappingRepository districtMappingRepository) {
        this.zoneRepository = zoneRepository;
        this.districtMappingRepository = districtMappingRepository;
    }

    /**
     * Get agro-ecological zone for a location.
     * Supports lookup by district/state name or GPS coordinates.
     * 
     * @param request Location request containing district/state or GPS coordinates
     * @return Zone lookup response with zone information
     * @throws LocationNotFoundException if location cannot be mapped to a zone
     * @throws ZoneMappingException if an error occurs during mapping
     * 
     * Validates: Requirement 2.1
     */
    public ZoneLookupResponseDto getZoneForLocation(LocationRequestDto request) {
        logger.info("Looking up zone for location: {}", request);

        try {
            // Validate input
            validateLocationRequest(request);

            // Try district/state lookup first
            if (request.getDistrict() != null && request.getState() != null) {
                return getZoneByDistrictAndState(request.getDistrict(), request.getState());
            }

            // Try GPS coordinates lookup
            if (request.getLatitude() != null && request.getLongitude() != null) {
                return getZoneByCoordinates(request.getLatitude(), request.getLongitude());
            }

            throw new LocationNotFoundException("Insufficient location information provided");
        } catch (LocationNotFoundException e) {
            logger.warn("Location not found: {}", e.getMessage());
            return ZoneLookupResponseDto.builder()
                    .success(false)
                    .inputLocation(formatInputLocation(request))
                    .district(request.getDistrict())
                    .state(request.getState())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .errorMessage(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error mapping location to zone: {}", e.getMessage(), e);
            throw new ZoneMappingException("Failed to map location to zone", e);
        }
    }

    /**
     * Get zone by district name and state.
     * 
     * @param district District name
     * @param state State name
     * @return Zone lookup response
     */
    private ZoneLookupResponseDto getZoneByDistrictAndState(String district, String state) {
        logger.debug("Looking up zone for district: {}, state: {}", district, state);

        // Try exact match first
        Optional<DistrictZoneMapping> mapping = districtMappingRepository
                .findByDistrictAndState(district, state);

        // Try case-insensitive match
        if (mapping.isEmpty()) {
            mapping = districtMappingRepository.findByDistrictAndState(
                    district.toLowerCase(), state.toLowerCase());
        }

        // Try alternative names
        if (mapping.isEmpty()) {
            List<DistrictZoneMapping> alternatives = districtMappingRepository
                    .findByDistrictName(district);
            mapping = alternatives.stream()
                    .filter(m -> m.getState().equalsIgnoreCase(state))
                    .findFirst();
        }

        if (mapping.isEmpty()) {
            throw new LocationNotFoundException(district, state);
        }

        DistrictZoneMapping districtMapping = mapping.get();
        AgroEcologicalZone zone = districtMapping.getZone();

        return ZoneLookupResponseDto.builder()
                .success(true)
                .inputLocation(formatLocation(district, state))
                .district(district)
                .state(state)
                .latitude(districtMapping.getLatitude())
                .longitude(districtMapping.getLongitude())
                .zone(mapToDto(zone))
                .build();
    }

    /**
     * Get zone by GPS coordinates.
     * Uses a two-step approach:
     * 1. First try to find the nearest district by coordinates
     * 2. If no district found, use the zone's bounding box
     * 
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @return Zone lookup response
     */
    private ZoneLookupResponseDto getZoneByCoordinates(Double latitude, Double longitude) {
        logger.debug("Looking up zone for coordinates: {}, {}", latitude, longitude);

        // First try to find the nearest district by coordinates
        double latBuffer = 0.5; // ~50km buffer
        double lonBuffer = 0.5; // ~50km buffer

        Optional<DistrictZoneMapping> districtMapping = districtMappingRepository
                .findNearestByCoordinates(
                        latitude, longitude,
                        latitude - latBuffer, latitude + latBuffer,
                        longitude - lonBuffer, longitude + lonBuffer);

        if (districtMapping.isPresent()) {
            DistrictZoneMapping mapping = districtMapping.get();
            AgroEcologicalZone zone = mapping.getZone();

            return ZoneLookupResponseDto.builder()
                    .success(true)
                    .inputLocation(formatCoordinates(latitude, longitude))
                    .district(mapping.getDistrictName())
                    .state(mapping.getState())
                    .latitude(latitude)
                    .longitude(longitude)
                    .zone(mapToDto(zone))
                    .build();
        }

        // Fall back to zone bounding box lookup
        Optional<AgroEcologicalZone> zone = zoneRepository.findByCoordinates(latitude, longitude);

        if (zone.isPresent()) {
            return ZoneLookupResponseDto.builder()
                    .success(true)
                    .inputLocation(formatCoordinates(latitude, longitude))
                    .latitude(latitude)
                    .longitude(longitude)
                    .zone(mapToDto(zone.get()))
                    .build();
        }

        throw new LocationNotFoundException(
                String.format("No zone found for coordinates: %.4f, %.4f", latitude, longitude));
    }

    /**
     * Get all active agro-ecological zones.
     * 
     * @return List of all active zones
     */
    public List<AgroEcologicalZoneDto> getAllZones() {
        return zoneRepository.findByIsActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get zone by zone code.
     * 
     * @param zoneCode ICAR zone code (e.g., "AEZ-01")
     * @return Zone information
     */
    public Optional<AgroEcologicalZoneDto> getZoneByCode(String zoneCode) {
        return zoneRepository.findByZoneCode(zoneCode)
                .map(this::mapToDto);
    }

    /**
     * Get zones by climate type.
     * 
     * @param climateType Climate type (e.g., "Tropical", "Subtropical")
     * @return List of zones with the specified climate type
     */
    public List<AgroEcologicalZoneDto> getZonesByClimateType(String climateType) {
        return zoneRepository.findByClimateTypeAndIsActiveTrue(climateType).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get districts in a state.
     * 
     * @param state State name
     * @return List of district names
     */
    public List<String> getDistrictsByState(String state) {
        return districtMappingRepository.findByState(state).stream()
                .map(DistrictZoneMapping::getDistrictName)
                .collect(Collectors.toList());
    }

    /**
     * Get zone information for a district.
     * 
     * @param district District name
     * @param state State name
     * @return Zone information
     */
    public Optional<AgroEcologicalZoneDto> getZoneForDistrict(String district, String state) {
        return districtMappingRepository.findByDistrictAndState(district, state)
                .map(DistrictZoneMapping::getZone)
                .map(this::mapToDto);
    }

    /**
     * Validate location request.
     * 
     * @param request Location request
     * @throws IllegalArgumentException if request is invalid
     */
    private void validateLocationRequest(LocationRequestDto request) {
        boolean hasDistrictState = request.getDistrict() != null && request.getState() != null;
        boolean hasCoordinates = request.getLatitude() != null && request.getLongitude() != null;

        if (!hasDistrictState && !hasCoordinates) {
            throw new IllegalArgumentException(
                    "Either district and state, or GPS coordinates must be provided");
        }

        if (hasCoordinates) {
            if (request.getLatitude() < -90 || request.getLatitude() > 90) {
                throw new IllegalArgumentException("Invalid latitude: must be between -90 and 90");
            }
            if (request.getLongitude() < -180 || request.getLongitude() > 180) {
                throw new IllegalArgumentException("Invalid longitude: must be between -180 and 180");
            }
        }
    }

    /**
     * Map entity to DTO.
     * 
     * @param zone Zone entity
     * @return Zone DTO
     */
    private AgroEcologicalZoneDto mapToDto(AgroEcologicalZone zone) {
        return AgroEcologicalZoneDto.builder()
                .id(zone.getId())
                .zoneCode(zone.getZoneCode())
                .zoneName(zone.getZoneName())
                .description(zone.getDescription())
                .climateType(zone.getClimateType())
                .rainfallRange(zone.getRainfallRange())
                .temperatureRange(zone.getTemperatureRange())
                .soilTypes(zone.getSoilTypes())
                .suitableCrops(zone.getSuitableCrops())
                .kharifSuitability(zone.getKharifSuitability())
                .rabiSuitability(zone.getRabiSuitability())
                .zaidSuitability(zone.getZaidSuitability())
                .latitudeRange(zone.getLatitudeRange())
                .longitudeRange(zone.getLongitudeRange())
                .statesCovered(zone.getStatesCovered())
                .build();
    }

    /**
     * Format input location for display.
     * 
     * @param request Location request
     * @return Formatted location string
     */
    private String formatInputLocation(LocationRequestDto request) {
        if (request.getDistrict() != null && request.getState() != null) {
            return formatLocation(request.getDistrict(), request.getState());
        }
        if (request.getLatitude() != null && request.getLongitude() != null) {
            return formatCoordinates(request.getLatitude(), request.getLongitude());
        }
        return "Unknown";
    }

    /**
     * Format location as "District, State".
     * 
     * @param district District name
     * @param state State name
     * @return Formatted location string
     */
    private String formatLocation(String district, String state) {
        return String.format("%s, %s", district, state);
    }

    /**
     * Format coordinates as "lat, lon".
     * 
     * @param latitude Latitude
     * @param longitude Longitude
     * @return Formatted coordinates string
     */
    private String formatCoordinates(Double latitude, Double longitude) {
        return String.format("%.4f, %.4f", latitude, longitude);
    }
}








