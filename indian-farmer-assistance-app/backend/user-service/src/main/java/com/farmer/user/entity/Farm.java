package com.farmer.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Farm entity representing a farmer's land parcel.
 * Maps to the farms table with session-specific table prefix for data isolation.
 * 
 * Requirements: 11A.2, 11A.3, 11A.10
 */
@Entity
@Table(name = "sess_c05a946fe_farms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "parcel_number", length = 50)
    private String parcelNumber;

    @Column(name = "total_area_acres", nullable = false)
    private Double totalAreaAcres;

    @Column(name = "soil_type", length = 50)
    private String soilType;

    @Enumerated(EnumType.STRING)
    @Column(name = "irrigation_type")
    private IrrigationType irrigationType;

    @Column(name = "agro_ecological_zone", length = 100)
    private String agroEcologicalZone;

    @Column(name = "survey_number", length = 50)
    private String surveyNumber;

    @Column(name = "gps_latitude")
    private Double gpsLatitude;

    @Column(name = "gps_longitude")
    private Double gpsLongitude;

    @Column(name = "village", length = 100)
    private String village;

    @Column(name = "district", length = 50)
    private String district;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Crop> crops = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Irrigation types for farm land.
     * Requirements: 11A.3
     */
    public enum IrrigationType {
        RAINFED,
        DRIP,
        SPRINKLER,
        CANAL,
        BOREWELL
    }

    /**
     * Add a crop to this farm.
     */
    public void addCrop(Crop crop) {
        crops.add(crop);
        crop.setFarm(this);
    }

    /**
     * Remove a crop from this farm.
     */
    public void removeCrop(Crop crop) {
        crops.remove(crop);
        crop.setFarm(null);
    }
}