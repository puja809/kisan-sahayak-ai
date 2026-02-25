package com.farmer.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ProfileVersion entity for tracking profile changes with audit trail.
 * Maps to the profile_versions table with session-specific table prefix.
 * 
 * Requirements: 11A.7, 21.6
 */
@Entity
@Table(name = "sess_c05a946fe_profile_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "farmer_id", nullable = false, length = 50)
    private String farmerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "changed_by", length = 50)
    private String changedBy;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "version_number")
    @Builder.Default
    private Long versionNumber = 1L;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Entity types for version tracking.
     * Requirements: 11A.7
     */
    public enum EntityType {
        USER_PROFILE,
        FARM,
        CROP,
        FERTILIZER_APPLICATION,
        LIVESTOCK,
        EQUIPMENT,
        HARVEST_RECORD
    }

    /**
     * Types of changes tracked.
     * Requirements: 11A.7
     */
    public enum ChangeType {
        CREATE,
        UPDATE,
        DELETE,
        SYNC
    }
}