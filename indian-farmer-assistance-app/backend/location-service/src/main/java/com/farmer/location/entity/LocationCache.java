package com.farmer.location.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity for caching location data including reverse geocoding results.
 * 
 * Validates: Requirement 14.3 (reverse geocoding)
 */
@Entity
@Table(name = "location_cache", indexes = {
    @Index(name = "idx_location_cache_coordinates", columnList = "latitude, longitude"),
    @Index(name = "idx_location_cache_district_state", columnList = "district, state")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "village", length = 200)
    private String village;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "agro_ecological_zone", length = 200)
    private String agroEcologicalZone;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "data_source", length = 100)
    private String dataSource;

    @Column(name = "cache_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime cacheTimestamp = LocalDateTime.now();

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}