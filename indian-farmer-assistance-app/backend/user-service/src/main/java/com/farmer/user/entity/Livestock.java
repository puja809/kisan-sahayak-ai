package com.farmer.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Livestock entity for recording farmer's livestock details.
 * Maps to the livestock table with session-specific table prefix.
 * 
 * Requirements: 11A.11
 */
@Entity
@Table(name = "sess_c05a946fe_livestock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livestock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @Enumerated(EnumType.STRING)
    @Column(name = "livestock_type", nullable = false)
    private LivestockType livestockType;

    @Column(name = "breed", length = 100)
    private String breed;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose")
    private LivestockPurpose purpose;

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "tag_number", length = 50)
    private String tagNumber;

    @Column(name = "health_status", length = 50)
    private String healthStatus;

    @Column(name = "vaccination_status", length = 50)
    private String vaccinationStatus;

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
     * Types of livestock.
     * Requirements: 11A.11
     */
    public enum LivestockType {
        CATTLE,
        BUFFALO,
        GOAT,
        SHEEP,
        POULTRY,
        PIGS,
        OTHER
    }

    /**
     * Purpose of livestock.
     * Requirements: 11A.11
     */
    public enum LivestockPurpose {
        DAIRY,
        MEAT,
        EGGS,
        DRAFT,
        BREEDING,
        MULTIPURPOSE
    }
}