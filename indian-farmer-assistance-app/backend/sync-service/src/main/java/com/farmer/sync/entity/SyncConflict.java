package com.farmer.sync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing a sync conflict between devices.
 * 
 * When conflicting updates occur, the conflict is stored for resolution.
 * The most recent timestamp wins according to Requirement 15.3.
 * 
 * Validates: Requirements 15.3, 15.5
 */
@Entity
@Table(name = "sync_conflicts", indexes = {
    @Index(name = "idx_sync_conflict_user", columnList = "user_id"),
    @Index(name = "idx_sync_conflict_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_sync_conflict_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User ID who owns this conflict
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Type of entity with conflict
     */
    @Column(name = "entity_type", nullable = false)
    private String entityType;

    /**
     * Entity ID with conflict
     */
    @Column(name = "entity_id", nullable = false)
    private String entityId;

    /**
     * Local version data (from current device)
     */
    @Column(name = "local_data", columnDefinition = "TEXT")
    private String localData;

    /**
     * Local version timestamp
     */
    @Column(name = "local_timestamp", nullable = false)
    private LocalDateTime localTimestamp;

    /**
     * Remote version data (from other device)
     */
    @Column(name = "remote_data", columnDefinition = "TEXT")
    private String remoteData;

    /**
     * Remote version timestamp
     */
    @Column(name = "remote_timestamp", nullable = false)
    private LocalDateTime remoteTimestamp;

    /**
     * Device ID that made the remote change
     */
    @Column(name = "remote_device_id")
    private String remoteDeviceId;

    /**
     * Resolution strategy used
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_strategy")
    private ResolutionStrategy resolutionStrategy;

    /**
     * Resolution result
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ConflictStatus status = ConflictStatus.PENDING;

    /**
     * Resolved data (if auto-resolved)
     */
    @Column(name = "resolved_data", columnDefinition = "TEXT")
    private String resolvedData;

    /**
     * Timestamp when conflict was detected
     */
    @Column(name = "detected_at", nullable = false)
    @Builder.Default
    private LocalDateTime detectedAt = LocalDateTime.now();

    /**
     * Timestamp when conflict was resolved
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * User ID who resolved the conflict (if manual)
     */
    @Column(name = "resolved_by")
    private String resolvedBy;

    /**
     * Conflict resolution strategies
     */
    public enum ResolutionStrategy {
        TIMESTAMP,    // Most recent timestamp wins
        MANUAL,       // User manually resolves
        MERGE,        // Data is merged
        LOCAL_WINS,   // Local version wins
        REMOTE_WINS   // Remote version wins
    }

    /**
     * Conflict status
     */
    public enum ConflictStatus {
        PENDING,      // Awaiting resolution
        AUTO_RESOLVED,// Automatically resolved
        MANUAL_RESOLUTION, // Requires user intervention
        RESOLVED      // Fully resolved
    }
}