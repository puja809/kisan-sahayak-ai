package com.farmer.mandi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing Indian districts with population data.
 * Used for demographic analysis and regional filtering in the mandi service.
 */
@Entity
@Table(name = "districts", 
       indexes = {
           @Index(name = "idx_district_code", columnList = "district_code"),
           @Index(name = "idx_district_name", columnList = "district_name"),
           @Index(name = "idx_state_id", columnList = "state_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "district_code", nullable = false, unique = true, length = 10)
    private String districtCode;

    @Column(name = "district_name", nullable = false, length = 100)
    private String districtName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id", nullable = false)
    private State state;

    @Column(name = "population")
    private Long population;

    @Column(name = "area_sq_km")
    private Double areaSqKm;

    @Column(name = "literacy_rate")
    private Double literacyRate;

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
