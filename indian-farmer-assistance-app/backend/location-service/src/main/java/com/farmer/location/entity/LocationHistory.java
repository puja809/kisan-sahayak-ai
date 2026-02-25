package com.farmer.location.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity for tracking user location history and detecting significant changes.
 * 
 * Validates: Requirement 14.4 (location change detection with >10km threshold)
 */
@Entity
@Table(name = "location_history", indexes = {
    @Index(name = "idx_location_history_user", columnList = "user_id"),
    @Index(name = "idx_location_history_timestamp", columnList = "recorded_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private Double latitude;

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private Double longitude;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "location_accuracy_meters")
    private Double locationAccuracyMeters;

    @Column(name = "location_source", length = 50)
    private String locationSource; // GPS, NETWORK, MANUAL

    @Column(name = "distance_from_last_km")
    private Double distanceFromLastKm;

    @Column(name = "is_significant_change")
    @Builder.Default
    private Boolean isSignificantChange = false;

    @Column(name = "recorded_at", nullable = false)
    @Builder.Default
    private LocalDateTime recordedAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}