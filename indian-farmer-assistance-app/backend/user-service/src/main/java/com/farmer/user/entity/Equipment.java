package com.farmer.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Equipment entity for recording farmer's farm equipment.
 * Maps to the equipment table with session-specific table prefix.
 * 
 * Requirements: 11A.12
 */
@Entity
@Table(name = "sess_c05a946fe_equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", nullable = false)
    private EquipmentType equipmentType;

    @Column(name = "equipment_name", length = 200)
    private String equipmentName;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost")
    private Double purchaseCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_type")
    @Builder.Default
    private OwnershipType ownershipType = OwnershipType.OWNED;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Column(name = "current_value")
    private Double currentValue;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Types of farm equipment.
     * Requirements: 11A.12
     */
    public enum EquipmentType {
        TRACTOR,
        HARVESTER,
        PLOW,
        SEED_DRILL,
        SPRAYER,
        PUMP_SET,
        THRESHER,
        LEVELER,
        ROTAVATOR,
        CULTIVATOR,
        SEED_CUM_FERTILIZER_DRILL,
        MOUNTED_SPRAYER,
        TRAILER,
        POWER_TILLER,
        OTHER
    }

    /**
     * Ownership types for equipment.
     * Requirements: 11A.12
     */
    public enum OwnershipType {
        OWNED,
        LEASED,
        SHARED
    }
}