package com.farmer.mandi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing Indian states with population data.
 * Used for demographic analysis and regional filtering in the mandi service.
 */
@Entity
@Table(name = "states", 
       indexes = {
           @Index(name = "idx_state_code", columnList = "state_code"),
           @Index(name = "idx_state_name", columnList = "state_name")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class State {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_code", nullable = false, unique = true, length = 10)
    private String stateCode;

    @Column(name = "state_name", nullable = false, unique = true, length = 100)
    private String stateName;

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
