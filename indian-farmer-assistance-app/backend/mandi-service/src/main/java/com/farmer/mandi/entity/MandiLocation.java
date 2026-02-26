package com.farmer.mandi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing mandi (agricultural market) location data.
 * Stores geo-location information for mandis to enable distance-based sorting.
 * 
 * Requirements:
 * - 6.4: Sort mandis by distance from farmer's location
 */
@Entity
@Table(name = "mandi_locations", 
       indexes = {
           @Index(name = "idx_mandi_code", columnList = "mandi_code"),
           @Index(name = "idx_state", columnList = "state"),
           @Index(name = "idx_district", columnList = "district")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MandiLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mandi_code", nullable = false, unique = true, length = 50)
    private String mandiCode;

    @Column(name = "mandi_name", nullable = false, length = 100)
    private String mandiName;

    @Column(name = "state", nullable = false, length = 50)
    private String state;

    @Column(name = "district", nullable = false, length = 50)
    private String district;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "operating_hours", length = 100)
    private String operatingHours;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}