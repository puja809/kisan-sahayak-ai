package com.farmer.location.service;

import com.farmer.location.dto.GovernmentBodyDto;
import com.farmer.location.dto.GovernmentBodySearchRequestDto;
import com.farmer.location.dto.GovernmentBodySearchResponseDto;
import com.farmer.location.entity.GovernmentBody;
import com.farmer.location.entity.GovernmentBody.GovernmentBodyType;
import com.farmer.location.repository.GovernmentBodyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for locating government agricultural bodies (KVKs, district offices, etc.).
 * 
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GovernmentBodyLocatorService {

    private final GovernmentBodyRepository governmentBodyRepository;
    private final LocationChangeDetectionService locationChangeDetectionService;

    /**
     * Search for government bodies near a location.
     * 
     * @param request Search request with location and filters
     * @return Search response with matching government bodies
     */
    public GovernmentBodySearchResponseDto searchNearbyGovernmentBodies(GovernmentBodySearchRequestDto request) {
        log.info("Searching for government bodies near: {}, {} within {} km",
                request.getLatitude(), request.getLongitude(), request.getMaxDistanceKm());

        try {
            // Get all government bodies with coordinates
            List<GovernmentBody> allBodies = governmentBodyRepository.findAllWithCoordinates();

            // Filter by body types if specified
            if (request.getBodyTypes() != null && !request.getBodyTypes().isEmpty()) {
                allBodies = allBodies.stream()
                        .filter(body -> request.getBodyTypes().contains(body.getBodyType()))
                        .collect(Collectors.toList());
            }

            // Filter by state if specified
            if (request.getState() != null && !request.getState().isEmpty()) {
                allBodies = allBodies.stream()
                        .filter(body -> request.getState().equalsIgnoreCase(body.getState()))
                        .collect(Collectors.toList());
            }

            // Filter by district if specified
            if (request.getDistrict() != null && !request.getDistrict().isEmpty()) {
                allBodies = allBodies.stream()
                        .filter(body -> request.getDistrict().equalsIgnoreCase(body.getDistrict()))
                        .collect(Collectors.toList());
            }

            // Calculate distance and filter by radius
            List<GovernmentBodyDto> matchingBodies = new ArrayList<>();
            for (GovernmentBody body : allBodies) {
                if (body.getLatitude() != null && body.getLongitude() != null) {
                    double distanceKm = locationChangeDetectionService.calculateDistance(
                            request.getLatitude(), request.getLongitude(),
                            body.getLatitude(), body.getLongitude()
                    );

                    GovernmentBodyDto dto = convertToDto(body, distanceKm, request);
                    matchingBodies.add(dto);
                }
            }

            // Sort by distance
            matchingBodies.sort((a, b) -> Double.compare(
                    a.getDistanceKm() != null ? a.getDistanceKm() : Double.MAX_VALUE,
                    b.getDistanceKm() != null ? b.getDistanceKm() : Double.MAX_VALUE
            ));

            log.info("Found {} government bodies near the location", matchingBodies.size());
            return GovernmentBodySearchResponseDto.success(request, matchingBodies);

        } catch (Exception e) {
            log.error("Error searching for government bodies: {}", e.getMessage(), e);
            return GovernmentBodySearchResponseDto.error("Failed to search for government bodies: " + e.getMessage());
        }
    }

    /**
     * Get KVKs near a location.
     * 
     * @param latitude Farmer's latitude
     * @param longitude Farmer's longitude
     * @param maxDistanceKm Maximum distance in kilometers
     * @return List of nearby KVKs
     */
    public List<GovernmentBodyDto> getNearbyKvks(Double latitude, Double longitude, Double maxDistanceKm) {
        GovernmentBodySearchRequestDto request = GovernmentBodySearchRequestDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .maxDistanceKm(maxDistanceKm)
                .bodyTypes(List.of(GovernmentBodyType.KVK))
                .includeSpecializationAreas(true)
                .includeContactInfo(true)
                .includeCapabilities(true)
                .includeDirections(true)
                .build();

        GovernmentBodySearchResponseDto response = searchNearbyGovernmentBodies(request);
        return response.getGovernmentBodies();
    }

    /**
     * Get district agriculture offices near a location.
     * 
     * @param latitude Farmer's latitude
     * @param longitude Farmer's longitude
     * @param maxDistanceKm Maximum distance in kilometers
     * @return List of nearby district agriculture offices
     */
    public List<GovernmentBodyDto> getNearbyDistrictOffices(Double latitude, Double longitude, Double maxDistanceKm) {
        GovernmentBodySearchRequestDto request = GovernmentBodySearchRequestDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .maxDistanceKm(maxDistanceKm)
                .bodyTypes(List.of(GovernmentBodyType.DISTRICT_AGRICULTURE_OFFICE))
                .includeContactInfo(true)
                .includeDirections(true)
                .build();

        GovernmentBodySearchResponseDto response = searchNearbyGovernmentBodies(request);
        return response.getGovernmentBodies();
    }

    /**
     * Get state department offices near a location.
     * 
     * @param latitude Farmer's latitude
     * @param longitude Farmer's longitude
     * @param maxDistanceKm Maximum distance in kilometers
     * @return List of nearby state department offices
     */
    public List<GovernmentBodyDto> getNearbyStateDepartments(Double latitude, Double longitude, Double maxDistanceKm) {
        GovernmentBodySearchRequestDto request = GovernmentBodySearchRequestDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .maxDistanceKm(maxDistanceKm)
                .bodyTypes(List.of(GovernmentBodyType.STATE_DEPARTMENT))
                .includeContactInfo(true)
                .includeDirections(true)
                .build();

        GovernmentBodySearchResponseDto response = searchNearbyGovernmentBodies(request);
        return response.getGovernmentBodies();
    }

    /**
     * Get ATARI centers near a location.
     * 
     * @param latitude Farmer's latitude
     * @param longitude Farmer's longitude
     * @param maxDistanceKm Maximum distance in kilometers
     * @return List of nearby ATARI centers
     */
    public List<GovernmentBodyDto> getNearbyAtariCenters(Double latitude, Double longitude, Double maxDistanceKm) {
        GovernmentBodySearchRequestDto request = GovernmentBodySearchRequestDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .maxDistanceKm(maxDistanceKm)
                .bodyTypes(List.of(GovernmentBodyType.ATARI))
                .includeContactInfo(true)
                .includeDirections(true)
                .build();

        GovernmentBodySearchResponseDto response = searchNearbyGovernmentBodies(request);
        return response.getGovernmentBodies();
    }

    /**
     * Get government body by ID.
     * 
     * @param id Government body ID
     * @return Government body DTO or null if not found
     */
    public GovernmentBodyDto getGovernmentBodyById(Long id) {
        return governmentBodyRepository.findById(id)
                .map(body -> convertToDto(body, 0.0, createDefaultRequest()))
                .orElse(null);
    }

    /**
     * Get all KVKs in a state.
     * 
     * @param state State name
     * @return List of KVKs in the state
     */
    public List<GovernmentBodyDto> getKvksByState(String state) {
        return governmentBodyRepository.findByStateAndBodyTypeAndIsActiveTrue(state, GovernmentBodyType.KVK)
                .stream()
                .map(body -> convertToDto(body, 0.0, createDefaultRequest()))
                .collect(Collectors.toList());
    }

    /**
     * Convert GovernmentBody entity to DTO.
     */
    private GovernmentBodyDto convertToDto(GovernmentBody body, double distanceKm, GovernmentBodySearchRequestDto request) {
        GovernmentBodyDto.GovBodyDtoBuilder builder = GovernmentBodyDto.builder()
                .id(body.getId())
                .bodyType(body.getBodyType())
                .name(body.getName())
                .address(body.getAddress())
                .district(body.getDistrict())
                .state(body.getState())
                .pinCode(body.getPinCode())
                .latitude(body.getLatitude())
                .longitude(body.getLongitude())
                .distanceKm(distanceKm);

        // Include contact info if requested
        if (Boolean.TRUE.equals(request.getIncludeContactInfo())) {
            builder.contactNumber(body.getContactNumber())
                   .email(body.getEmail())
                   .website(body.getWebsite())
                   .operatingHours(body.getOperatingHours());
        }

        // Include specialization areas for KVKs if requested
        if (Boolean.TRUE.equals(request.getIncludeSpecializationAreas()) && 
            body.getBodyType() == GovernmentBodyType.KVK && 
            body.getSpecializationAreas() != null) {
            List<String> specializations = Arrays.asList(body.getSpecializationAreas().split(","));
            specializations = specializations.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            builder.specializationAreas(specializations);
        }

        // Include capabilities if requested
        if (Boolean.TRUE.equals(request.getIncludeCapabilities()) && 
            body.getBodyType() == GovernmentBodyType.KVK) {
            builder.seniorScientistHead(body.getSeniorScientistHead())
                   .onFarmTestingCapabilities(body.getOnFarmTestingCapabilities())
                   .frontlineDemonstrationPrograms(body.getFrontlineDemonstrationPrograms())
                   .capacityDevelopmentTraining(body.getCapacityDevelopmentTraining());
        }

        // Include directions URL if requested
        if (Boolean.TRUE.equals(request.getIncludeDirections()) && 
            body.getLatitude() != null && body.getLongitude() != null) {
            String directionsUrl = String.format(
                    "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f",
                    request.getLatitude(), request.getLongitude(),
                    body.getLatitude(), body.getLongitude()
            );
            builder.directionsUrl(directionsUrl);
        }

        return builder.build();
    }

    /**
     * Create a default search request for single entity lookups.
     */
    private GovernmentBodySearchRequestDto createDefaultRequest() {
        return GovernmentBodySearchRequestDto.builder()
                .latitude(0.0)
                .longitude(0.0)
                .maxDistanceKm(50.0)
                .includeSpecializationAreas(true)
                .includeContactInfo(true)
                .includeCapabilities(true)
                .includeDirections(true)
                .build();
    }
}