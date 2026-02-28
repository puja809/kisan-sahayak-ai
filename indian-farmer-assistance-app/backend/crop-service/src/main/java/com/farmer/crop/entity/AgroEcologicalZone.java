package com.farmer.crop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing an ICAR Agro-Ecological Zone.
 * 
 * ICAR has classified India into 20 agro-ecological zones based on
 * climate, soil, and terrain characteristics. These zones are used
 * for crop planning and recommendations.
 * 
 * DEPRECATED: This entity is maintained for backward compatibility only.
 * New implementations should use soil data from Kaegro API (SoilDataDto)
 * instead of relying on agro-ecological zone classifications.
 * 
 * Validates: Requirement 2.1
 * 
 * @deprecated Use SoilDataDto from Kaegro API for soil information
 */
@Deprecated(since = "2.0", forRemoval = false)
@Entity
@Table(name = "agro_ecological_zones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgroEcologicalZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ICAR zone code (e.g., "AEZ-01", "AEZ-02", etc.)
     */
    @Column(name = "zone_code", nullable = false, unique = true, length = 10)
    private String zoneCode;

    /**
     * Official zone name as per ICAR classification
     */
    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;

    /**
     * Detailed description of the zone including climate, soil, and terrain characteristics
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Climate type (e.g., "Tropical", "Subtropical", "Temperate", "Arid", "Semi-Arid")
     */
    @Column(name = "climate_type", length = 50)
    private String climateType;

    /**
     * Annual rainfall range in mm (e.g., "600-1200")
     */
    @Column(name = "rainfall_range", length = 50)
    private String rainfallRange;

    /**
     * Temperature range in Celsius (e.g., "20-35")
     */
    @Column(name = "temperature_range", length = 50)
    private String temperatureRange;

    /**
     * Major soil types found in this zone
     */
    @Column(name = "soil_types", length = 500)
    private String soilTypes;

    /**
     * Major crops suitable for this zone
     */
    @Column(name = "suitable_crops", columnDefinition = "TEXT")
    private String suitableCrops;

    /**
     * Kharif season suitability description
     */
    @Column(name = "kharif_suitability", columnDefinition = "TEXT")
    private String kharifSuitability;

    /**
     * Rabi season suitability description
     */
    @Column(name = "rabi_suitability", columnDefinition = "TEXT")
    private String rabiSuitability;

    /**
     * Zaid season suitability description
     */
    @Column(name = "zaid_suitability", columnDefinition = "TEXT")
    private String zaidSuitability;

    /**
     * Latitude range for this zone (e.g., "8.0-15.0")
     */
    @Column(name = "latitude_range", length = 50)
    private String latitudeRange;

    /**
     * Longitude range for this zone (e.g., "68.0-80.0")
     */
    @Column(name = "longitude_range", length = 50)
    private String longitudeRange;

    /**
     * States covered by this zone (comma-separated)
     */
    @Column(name = "states_covered", length = 500)
    private String statesCovered;

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