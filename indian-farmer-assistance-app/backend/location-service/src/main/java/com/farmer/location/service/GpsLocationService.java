package com.farmer.location.service;

import com.farmer.location.dto.LocationRequestDto;
import com.farmer.location.dto.LocationResponseDto;
import com.farmer.location.entity.LocationCache;
import com.farmer.location.entity.LocationHistory;
import com.farmer.location.exception.LocationException;
import com.farmer.location.repository.LocationCacheRepository;
import com.farmer.location.repository.LocationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for GPS location retrieval and reverse geocoding.
 * 
 * Validates: Requirements 14.1, 14.2, 14.3, 14.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GpsLocationService {

    private static final String LOCATION_CACHE_KEY_PREFIX = "location:cache:";
    private static final Duration REDIS_CACHE_TTL = Duration.ofHours(24);
    private static final double INDIA_MIN_LAT = 6.0;
    private static final double INDIA_MAX_LAT = 37.0;
    private static final double INDIA_MIN_LON = 68.0;
    private static final double INDIA_MAX_LON = 97.0;

    private final LocationCacheRepository locationCacheRepository;
    private final LocationHistoryRepository locationHistoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient.Builder webClientBuilder;

    /**
     * Get location from GPS coordinates with reverse geocoding.
     * 
     * @param request Location request with GPS coordinates
     * @return Location response with reverse geocoded information
     */
    @Transactional
    public LocationResponseDto getLocation(LocationRequestDto request) {
        log.info("Getting location for request: {}", request);

        // Validate request
        if (!request.isValid()) {
            return LocationResponseDto.error("Invalid location request. Provide either GPS coordinates or district/state.");
        }

        // Handle permission request
        if (Boolean.TRUE.equals(request.getRequestPermission())) {
            return handlePermissionRequest(request);
        }

        // Check if this is a coordinate-based request
        if (request.hasCoordinates()) {
            return getLocationFromCoordinates(request);
        }

        // Handle district/state-based request
        return getLocationFromDistrictState(request);
    }

    /**
     * Get location from GPS coordinates with reverse geocoding.
     */
    private LocationResponseDto getLocationFromCoordinates(LocationRequestDto request) {
        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();

        // Validate coordinates are within India
        if (!isValidIndiaCoordinates(latitude, longitude)) {
            return LocationResponseDto.error(
                    "Coordinates are outside India. Latitude must be between 6° and 37° N, " +
                    "longitude between 68° and 97° E.");
        }

        // Check Redis cache first
        LocationResponseDto cachedResponse = getFromRedisCache(latitude, longitude);
        if (cachedResponse != null) {
            log.info("Returning location from Redis cache for coordinates: {}, {}", latitude, longitude);
            return cachedResponse;
        }

        // Check database cache
        Optional<LocationCache> dbCache = locationCacheRepository.findByCoordinates(latitude, longitude);
        if (dbCache.isPresent()) {
            LocationResponseDto response = buildResponseFromCache(dbCache.get());
            response.setCacheTimestamp(dbCache.get().getCacheTimestamp());
            cacheInRedis(latitude, longitude, response);
            log.info("Returning location from database cache for coordinates: {}, {}", latitude, longitude);
            return response;
        }

        // Perform reverse geocoding (simulated - in production, call actual geocoding API)
        LocationResponseDto response = performReverseGeocoding(request);
        
        // Cache the result
        cacheLocation(latitude, longitude, response);
        
        return response;
    }

    /**
     * Get location from district and state (manual entry or fallback).
     */
    private LocationResponseDto getLocationFromDistrictState(LocationRequestDto request) {
        String district = request.getDistrict();
        String state = request.getState();

        // Check database cache first
        Optional<LocationCache> dbCache = locationCacheRepository.findByDistrictAndState(district, state);
        if (dbCache.isPresent()) {
            LocationResponseDto response = buildResponseFromCache(dbCache.get());
            response.setCacheTimestamp(dbCache.get().getCacheTimestamp());
            response.setLocationSource("MANUAL");
            log.info("Returning location from cache for district: {}, state: {}", district, state);
            return response;
        }

        // Perform reverse geocoding for district/state
        LocationResponseDto response = performReverseGeocoding(request);
        response.setLocationSource("MANUAL");

        // Cache the result
        if (response.getLatitude() != null && response.getLongitude() != null) {
            cacheLocation(response.getLatitude(), response.getLongitude(), response);
        }

        return response;
    }

    /**
     * Handle location permission request.
     */
    private LocationResponseDto handlePermissionRequest(LocationRequestDto request) {
        return LocationResponseDto.builder()
                .success(true)
                .permissionRequested(true)
                .suggestion("Please grant location permission to enable GPS-based location detection. " +
                           "If you prefer not to share your location, you can enter your district and state manually.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Perform reverse geocoding to determine district, state, and agro-ecological zone.
     * In production, this would call an actual geocoding API (e.g., Google Maps, OSM, or Indian government APIs).
     */
    private LocationResponseDto performReverseGeocoding(LocationRequestDto request) {
        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();
        String district = request.getDistrict();
        String state = request.getState();

        // Simulated reverse geocoding logic
        // In production, this would call an actual geocoding API
        if (latitude != null && longitude != null) {
            // Use coordinates to determine location
            district = determineDistrictFromCoordinates(latitude, longitude);
            state = determineStateFromCoordinates(latitude, longitude);
        }

        String zone = determineAgroEcologicalZone(latitude, longitude, state, district);
        String region = determineRegion(state);

        return LocationResponseDto.builder()
                .success(true)
                .latitude(latitude)
                .longitude(longitude)
                .district(district)
                .state(state)
                .agroEcologicalZone(zone)
                .zoneCode(extractZoneCode(zone))
                .zoneDescription(getZoneDescription(zone))
                .region(region)
                .locationSource(Boolean.TRUE.equals(request.getIsManual()) ? "MANUAL" : "GPS")
                .timestamp(LocalDateTime.now())
                .dataSource("ICAR")
                .build();
    }

    /**
     * Determine district from GPS coordinates.
     * In production, this would use actual geocoding data.
     */
    private String determineDistrictFromCoordinates(Double latitude, Double longitude) {
        // Simulated district determination based on coordinates
        // In production, this would use actual geocoding data
        if (latitude == null || longitude == null) {
            return null;
        }

        // Example: Based on rough coordinate ranges
        if (latitude >= 12.0 && latitude <= 13.5 && longitude >= 77.0 && longitude <= 78.5) {
            return "Bangalore Rural";
        } else if (latitude >= 22.5 && latitude <= 25.5 && longitude >= 85.0 && longitude <= 88.0) {
            return "Ranchi";
        } else if (latitude >= 26.0 && latitude <= 27.0 && longitude >= 80.0 && longitude <= 82.0) {
            return "Lucknow";
        } else if (latitude >= 19.0 && latitude <= 20.0 && longitude >= 72.0 && longitude <= 73.5) {
            return "Mumbai";
        } else if (latitude >= 28.0 && latitude <= 29.0 && longitude >= 76.0 && longitude <= 78.0) {
            return "Hisar";
        } else if (latitude >= 23.0 && latitude <= 25.0 && longitude >= 72.0 && longitude <= 75.0) {
            return "Ahmedabad";
        } else if (latitude >= 17.0 && longitude >= 78.0) {
            return "Hyderabad";
        } else if (latitude >= 20.0 && longitude >= 85.0) {
            return "Bhubaneswar";
        } else if (latitude >= 11.0 && longitude >= 76.0) {
            return "Coimbatore";
        } else if (latitude >= 30.0 && longitude >= 78.0) {
            return "Dehradun";
        } else {
            return "Unknown District";
        }
    }

    /**
     * Determine state from GPS coordinates.
     */
    private String determineStateFromCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        // Rough state determination based on coordinates
        if (latitude >= 12.0 && latitude <= 14.0 && longitude >= 74.0 && longitude <= 78.0) {
            return "Karnataka";
        } else if (latitude >= 22.0 && latitude <= 27.0 && longitude >= 85.0 && longitude <= 88.0) {
            return "Jharkhand";
        } else if (latitude >= 26.0 && latitude <= 30.0 && longitude >= 79.0 && longitude <= 85.0) {
            return "Uttar Pradesh";
        } else if (latitude >= 15.0 && latitude <= 22.0 && longitude >= 72.0 && longitude <= 82.0) {
            return "Maharashtra";
        } else if (latitude >= 28.0 && latitude <= 30.0 && longitude >= 74.0 && longitude <= 80.0) {
            return "Haryana";
        } else if (latitude >= 20.0 && latitude <= 25.0 && longitude >= 69.0 && longitude >= 75.0) {
            return "Gujarat";
        } else if (latitude >= 16.0 && latitude <= 20.0 && longitude >= 77.0 && longitude <= 84.0) {
            return "Telangana";
        } else if (latitude >= 19.0 && latitude <= 23.0 && longitude >= 82.0 && longitude >= 88.0) {
            return "Odisha";
        } else if (latitude >= 10.0 && latitude <= 13.0 && longitude >= 76.0 && longitude <= 80.0) {
            return "Tamil Nadu";
        } else if (latitude >= 28.0 && latitude <= 31.0 && longitude >= 77.0 && longitude <= 81.0) {
            return "Uttarakhand";
        } else {
            return "Unknown State";
        }
    }

    /**
     * Determine agro-ecological zone based on location.
     */
    private String determineAgroEcologicalZone(Double latitude, Double longitude, String state, String district) {
        // ICAR agro-ecological zone classification
        // This is a simplified version - production would use detailed boundary data
        
        if (state == null) {
            return "Unknown Zone";
        }

        return switch (state) {
            case "Karnataka" -> "Southern Plateau and Hills Region";
            case "Maharashtra" -> "Western Plateau and Hills Region";
            case "Uttar Pradesh" -> "Upper Gangetic Plain Region";
            case "Punjab" -> "Trans-Gangetic Plain Region";
            case "Haryana" -> "Trans-Gangetic Plain Region";
            case "Gujarat" -> "Gujarat Plains and Hills Region";
            case "Tamil Nadu" -> "Southern Plateau and Hills Region";
            case "Telangana" -> "Southern Plateau and Hills Region";
            case "Odisha" -> "East Coast Plains and Hills Region";
            case "West Bengal" -> "Ganga Delta Region";
            case "Rajasthan" -> "Western Dry Region";
            case "Madhya Pradesh" -> "Central Plateau and Hills Region";
            default -> "Unknown Zone";
        };
    }

    /**
     * Determine region from state.
     */
    private String determineRegion(String state) {
        if (state == null) {
            return "Unknown";
        }

        return switch (state) {
            case "Punjab", "Haryana", "Uttar Pradesh", "Uttarakhand", "Himachal Pradesh" -> "North";
            case "Rajasthan", "Gujarat", "Maharashtra" -> "West";
            case "West Bengal", "Odisha", "Jharkhand", "Bihar" -> "East";
            case "Karnataka", "Tamil Nadu", "Telangana", "Andhra Pradesh", "Kerala" -> "South";
            case "Madhya Pradesh", "Chhattisgarh" -> "Central";
            case "Assam", "Meghalaya", "Manipur", "Nagaland", "Tripura", "Arunachal Pradesh" -> "Northeast";
            default -> "Unknown";
        };
    }

    /**
     * Extract zone code from zone name.
     */
    private String extractZoneCode(String zoneName) {
        if (zoneName == null) {
            return null;
        }

        return switch (zoneName) {
            case "Southern Plateau and Hills Region" -> "AEZ-09";
            case "Western Plateau and Hills Region" -> "AEZ-08";
            case "Upper Gangetic Plain Region" -> "AEZ-05";
            case "Trans-Gangetic Plain Region" -> "AEZ-04";
            case "Gujarat Plains and Hills Region" -> "AEZ-07";
            case "East Coast Plains and Hills Region" -> "AEZ-12";
            case "Ganga Delta Region" -> "AEZ-13";
            case "Western Dry Region" -> "AEZ-02";
            case "Central Plateau and Hills Region" -> "AEZ-06";
            default -> "AEZ-00";
        };
    }

    /**
     * Get zone description.
     */
    private String getZoneDescription(String zoneName) {
        if (zoneName == null) {
            return null;
        }

        return switch (zoneName) {
            case "Southern Plateau and Hills Region" -> 
                "Semi-arid subtropical climate with red and lateritic soils";
            case "Western Plateau and Hills Region" -> 
                "Semi-arid climate with black cotton soils";
            case "Upper Gangetic Plain Region" -> 
                "Subtropical climate with alluvial soils";
            case "Trans-Gangetic Plain Region" -> 
                "Semi-arid to sub-humid climate with alluvial soils";
            case "Gujarat Plains and Hills Region" -> 
                "Semi-arid climate with medium to deep black soils";
            case "East Coast Plains and Hills Region" -> 
                "Coastal plains with alluvial and red soils";
            case "Ganga Delta Region" -> 
                "Humid subtropical climate with alluvial soils";
            case "Western Dry Region" -> 
                "Arid climate with desert soils";
            case "Central Plateau and Hills Region" -> 
                "Subtropical climate with mixed red and black soils";
            default -> "Unknown agro-ecological zone";
        };
    }

    /**
     * Validate that coordinates are within India.
     */
    public boolean isValidIndiaCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude >= INDIA_MIN_LAT && latitude <= INDIA_MAX_LAT &&
               longitude >= INDIA_MIN_LON && longitude <= INDIA_MAX_LON;
    }

    /**
     * Record location in history.
     */
    @Transactional
    public void recordLocationHistory(Long userId, LocationResponseDto location, Double distanceFromLastKm) {
        LocationHistory history = LocationHistory.builder()
                .userId(userId)
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .district(location.getDistrict())
                .state(location.getState())
                .locationSource(location.getLocationSource())
                .distanceFromLastKm(distanceFromLastKm)
                .isSignificantChange(distanceFromLastKm != null && distanceFromLastKm > 10.0)
                .build();

        locationHistoryRepository.save(history);
    }

    /**
     * Get last known location for a user.
     */
    public Optional<LocationResponseDto> getLastKnownLocation(Long userId) {
        return locationHistoryRepository.findMostRecentByUserId(userId)
                .map(history -> LocationResponseDto.builder()
                        .success(true)
                        .latitude(history.getLatitude())
                        .longitude(history.getLongitude())
                        .district(history.getDistrict())
                        .state(history.getState())
                        .timestamp(history.getRecordedAt())
                        .locationSource(history.getLocationSource())
                        .build());
    }

    /**
     * Cache location in Redis.
     */
    private void cacheInRedis(Double latitude, Double longitude, LocationResponseDto response) {
        try {
            String key = LOCATION_CACHE_KEY_PREFIX + latitude + ":" + longitude;
            redisTemplate.opsForValue().set(key, response, REDIS_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache location in Redis: {}", e.getMessage());
        }
    }

    /**
     * Get location from Redis cache.
     */
    private LocationResponseDto getFromRedisCache(Double latitude, Double longitude) {
        try {
            String key = LOCATION_CACHE_KEY_PREFIX + latitude + ":" + longitude;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof LocationResponseDto) {
                LocationResponseDto response = (LocationResponseDto) cached;
                response.setCacheTimestamp(LocalDateTime.now());
                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to get location from Redis cache: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Cache location in database.
     */
    private void cacheLocation(Double latitude, Double longitude, LocationResponseDto response) {
        try {
            LocationCache cache = LocationCache.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .district(response.getDistrict())
                    .state(response.getState())
                    .village(response.getVillage())
                    .pinCode(response.getPinCode())
                    .agroEcologicalZone(response.getAgroEcologicalZone())
                    .region(response.getRegion())
                    .dataSource(response.getDataSource())
                    .cacheTimestamp(LocalDateTime.now())
                    .isVerified(false)
                    .build();

            locationCacheRepository.save(cache);
            cacheInRedis(latitude, longitude, response);
        } catch (Exception e) {
            log.warn("Failed to cache location in database: {}", e.getMessage());
        }
    }

    /**
     * Build response from cache entity.
     */
    private LocationResponseDto buildResponseFromCache(LocationCache cache) {
        return LocationResponseDto.builder()
                .success(true)
                .latitude(cache.getLatitude())
                .longitude(cache.getLongitude())
                .district(cache.getDistrict())
                .state(cache.getState())
                .village(cache.getVillage())
                .pinCode(cache.getPinCode())
                .agroEcologicalZone(cache.getAgroEcologicalZone())
                .region(cache.getRegion())
                .dataSource(cache.getDataSource())
                .timestamp(LocalDateTime.now())
                .build();
    }
}