package com.farmer.scheme.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SchemeApplication entity representing a farmer's application for a government scheme.
 * Maps to the scheme_applications table with session-specific table prefix for data isolation.
 * 
 * Requirements: 11D.10
 */
@Entity
@Table(name = "sess_c05a946fe_scheme_applications", indexes = {
    @Index(name = "idx_sa_user_id", columnList = "user_id"),
    @Index(name = "idx_sa_scheme_id", columnList = "scheme_id"),
    @Index(name = "idx_sa_status", columnList = "status"),
    @Index(name = "idx_sa_application_number", columnList = "application_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    private Scheme scheme;

    @Column(name = "application_date", nullable = false)
    @Builder.Default
    private LocalDate applicationDate = LocalDate.now();

    @Column(name = "application_number", length = 100)
    private String applicationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "documents", columnDefinition = "JSON")
    private String documents;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "disbursement_amount", precision = 12, scale = 2)
    private java.math.BigDecimal disbursementAmount;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Application status for tracking scheme applications.
     * Requirements: 11D.10
     */
    public enum ApplicationStatus {
        DRAFT,          // Application is being drafted
        SUBMITTED,      // Application has been submitted
        UNDER_REVIEW,   // Application is under review
        APPROVED,       // Application has been approved
        REJECTED,       // Application has been rejected
        DISBURSED       // Benefits have been disbursed
    }
}