package com.farmer.location.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing government agricultural bodies such as KVKs, 
 * district agriculture offices, and state departments.
 * 
 * Validates: Requirements 7.2, 7.3, 7.4, 7.5
 */
@Entity
@Table(name = "government_bodies", indexes = {
    @Index(name = "idx_government_body_type", columnList = "bodyType"),
    @Index(name = "idx_government_body_state", columnList = "state"),
    @Index(name = "idx_government_body_district", columnList = "district"),
    @Index(name = "idx_government_body_location", columnList = "latitude, longitude")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentBody {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of government body: KVK, DISTRICT_AGRICULTURE_OFFICE, STATE_DEPARTMENT, ATARI
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "body_type", nullable = false)
    private GovernmentBodyType bodyType;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "website", length = 200)
    private String website;

    /**
     * Specialization areas for KVKs: Horticulture, Soil Science, Plant Protection, etc.
     * Stored as comma-separated values.
     */
    @Column(name = "specialization_areas", length = 500)
    private String specializationAreas;

    @Column(name = "senior_scientist_head", length = 200)
    private String seniorScientistHead;

    @Column(name = "on_farm_testing_capabilities", length = 500)
    private String onFarmTestingCapabilities;

    @Column(name = "frontline_demonstration_programs", length = 500)
    private String frontlineDemonstrationPrograms;

    @Column(name = "capacity_development_training", length = 500)
    private String capacityDevelopmentTraining;

    @Column(name = "operating_hours", length = 100)
    private String operatingHours;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Enum for government body types.
     */
    public enum GovernmentBodyType {
        KVK,                    // Krishi Vigyan Kendra
        DISTRICT_AGRICULTURE_OFFICE,
        STATE_DEPARTMENT,       // State Agriculture Department
        ATARI                   // Agricultural Technology Application Research Institute
    }
}