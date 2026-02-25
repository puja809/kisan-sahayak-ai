package com.farmer.sync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing a queued sync request.
 * 
 * When the application is offline, requests are queued for later synchronization.
 * Requests are processed in FIFO (First-In-First-Out) order when connectivity is restored.
 * 
 * Validates: Requirements 12.3, 12.4
 */
@Entity
@Table(name = "sync_queue", indexes = {
    @Index(name = "idx_sync_queue_user", columnList = "user_id"),
    @Index(name = "idx_sync_queue_status", columnList = "status"),
    @Index(name = "idx_sync_queue_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User ID who created this sync request
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Type of entity being synced (CROP, FERTILIZER, HARVEST, etc.)
     */
    @Column(name = "entity_type", nullable = false)
    private String entityType;

    /**
     * Type of operation (CREATE, UPDATE, DELETE)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    /**
     * Entity ID being synced (null for create operations)
     */
    @Column(name = "entity_id")
    private String entityId;

    /**
     * JSON payload containing the request data
     */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /**
     * Current status of the sync request
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SyncStatus status = SyncStatus.PENDING;

    /**
     * Number of retry attempts made
     */
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Last error message if sync failed
     */
    @Column(name = "last_error", length = 1000)
    private String lastError;

    /**
     * Timestamp when the request was created
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Timestamp when the request was last processed
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Timestamp for conflict resolution
     */
    @Column(name = "client_timestamp", nullable = false)
    private LocalDateTime clientTimestamp;

    /**
     * Priority of the sync request (higher = processed first)
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    /**
     * Operation types for sync requests
     */
    public enum OperationType {
        CREATE,
        UPDATE,
        DELETE
    }

    /**
     * Status of sync queue items
     */
    public enum SyncStatus {
        PENDING,      // Waiting to be processed
        IN_PROGRESS,  // Currently being processed
        COMPLETED,    // Successfully synced
        FAILED,       // Sync failed after all retries
        CONFLICT      // Conflict detected, requires manual resolution
    }
}