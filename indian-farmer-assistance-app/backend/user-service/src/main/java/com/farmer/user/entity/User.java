package com.farmer.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * User entity representing a farmer in the system.
 * Maps to the users table with session-specific table prefix for data isolation.
 * 
 * Requirements: 11.1, 11A.1, 22.1
 */
@Entity
@Table(name = "sess_c05a946fe_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farmer_id", unique = true, nullable = false, length = 50)
    private String farmerId;

    @Column(name = "aadhaar_hash", unique = true, nullable = false, length = 64)
    private String aadhaarHash;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", unique = true, nullable = false, length = 15)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "en";

    @Column(name = "state", nullable = false, length = 50)
    private String state;

    @Column(name = "district", nullable = false, length = 50)
    private String district;

    @Column(name = "village", length = 100)
    private String village;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "gps_latitude", precision = 10, scale = 8)
    private Double gpsLatitude;

    @Column(name = "gps_longitude", precision = 11, scale = 8)
    private Double gpsLongitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Role role = Role.FARMER;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "agristack_farmer_id", length = 100)
    private String agristackFarmerId;

    @Column(name = "total_landholding_acres", precision = 10, scale = 2)
    private Double totalLandholdingAcres;

    @Column(name = "soil_type", length = 50)
    private String soilType;

    @Column(name = "irrigation_type", length = 50)
    private String irrigationType;

    @Column(name = "agro_ecological_zone", length = 100)
    private String agroEcologicalZone;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * User roles for role-based access control.
     * Requirements: 22.1
     */
    public enum Role {
        FARMER,
        ADMIN
    }
}