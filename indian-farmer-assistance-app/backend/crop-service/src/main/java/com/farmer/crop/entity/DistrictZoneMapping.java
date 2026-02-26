package com.farmer.crop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

/**
 * Entity representing the mapping between Indian districts and agro-ecological zones.
 * 
 * This entity provides a lookup table for determining the agro-ecological zone
 * based on district and state information.
 * 
 * Validates: Requirement 2.1
 */
@Entity
@Table(name = "district_zone_mappings", 
       indexes = {
           @Index(name = "idx_district_state", columnList = "district_name, state"),
           @Index(name = "idx_zone_id", columnList = "zone_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistrictZoneMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * District name
     */
    @Column(name = "district_name", nullable = false, length = 100)
    private String districtName;

    /**
     * State name
     */
    @Column(name = "state", nullable = false, length = 100)
    private String state;

    /**
     * Reference to the agro-ecological zone
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private AgroEcologicalZone zone;

    /**
     * Latitude of district center (for GPS-based lookup)
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * Longitude of district center (for GPS-based lookup)
     */
    @Column(name = "longitude")
    private Double longitude;

    /**
     * Alternative district names or spellings (comma-separated)
     */
    @Column(name = "alternative_names", length = 500)
    private String alternativeNames;

    /**
     * Region within the state (e.g., "North", "South", "East", "West")
     */
    @Column(name = "region", length = 50)
    private String region;

    /**
     * Whether this mapping is verified
     */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * Data source for this mapping (e.g., "ICAR", "State Agriculture Dept")
     */
    @Column(name = "data_source", length = 100)
    private String dataSource;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
