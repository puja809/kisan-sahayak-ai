package com.farmer.scheme.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Scheme entity representing a government scheme in the system.
 * Maps to the schemes table with session-specific table prefix for data isolation.
 * 
 * Requirements: 4.1, 4.2, 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8
 */
@Entity
@Table(name = "sess_c05a946fe_schemes", indexes = {
    @Index(name = "idx_scheme_type", columnList = "scheme_type"),
    @Index(name = "idx_state", columnList = "state"),
    @Index(name = "idx_active", columnList = "is_active"),
    @Index(name = "idx_scheme_code", columnList = "scheme_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_code", unique = true, nullable = false, length = 50)
    private String schemeCode;

    @Column(name = "scheme_name", nullable = false, length = 200)
    private String schemeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "scheme_type", nullable = false, length = 20)
    private SchemeType schemeType;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "eligibility_criteria", columnDefinition = "JSON")
    private String eligibilityCriteria;

    @Column(name = "benefit_amount", precision = 12, scale = 2)
    private BigDecimal benefitAmount;

    @Column(name = "benefit_description", columnDefinition = "TEXT")
    private String benefitDescription;

    @Column(name = "application_start_date")
    private LocalDate applicationStartDate;

    @Column(name = "application_end_date")
    private LocalDate applicationEndDate;

    @Column(name = "application_url", length = 500)
    private String applicationUrl;

    @Column(name = "contact_info", columnDefinition = "JSON")
    private String contactInfo;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "applicable_crops", length = 500)
    private String applicableCrops;

    @Column(name = "subsidy_percentage", precision = 5, scale = 2)
    private BigDecimal subsidyPercentage;

    @Column(name = "max_benefit_amount", precision = 12, scale = 2)
    private BigDecimal maxBenefitAmount;

    @Column(name = "landholding_requirement", length = 100)
    private String landholdingRequirement;

    @Column(name = "target_beneficiaries", length = 200)
    private String targetBeneficiaries;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Scheme types for categorizing government schemes.
     * Requirements: 4.1, 5.1, 5.2
     */
    public enum SchemeType {
        CENTRAL,        // Central government schemes
        STATE,          // State-specific schemes
        CROP_SPECIFIC,  // Schemes specific to certain crops
        INSURANCE,      // Crop insurance schemes
        SUBSIDY,        // Input subsidy schemes
        WELFARE         // Farmer welfare schemes
    }
}