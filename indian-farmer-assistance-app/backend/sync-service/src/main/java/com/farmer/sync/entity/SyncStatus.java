package com.farmer.sync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing the sync status for a user.
 * 
 * Tracks the last sync time, pending changes, and overall sync health.
 * 
 * Validates: Requirements 15.2, 15.5, 15.6
 */
@Entity
@Table(name = "sync_status", indexes = {
    @Index(name = "idx_sync_status_user", columnList = "user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User ID this sync status belongs to
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    /**
     * Last successful sync timestamp
     */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    /**
     * Number of pending changes to sync
     */
    @Column(name = "pending_changes")
    @Builder.Default
    private Integer pendingChanges = 0;

    /**
     * Current sync state
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_state", nullable = false)
    @Builder.Default
    private State syncState = State.IDLE;

    /**
     * Number of items currently syncing
     */
    @Column(name = "syncing_count")
    @Builder.Default
    private Integer syncingCount = 0;

    /**
     * Total items to sync in current batch
     */
    @Column(name = "total_to_sync")
    @Builder.Default
    private Integer totalToSync = 0;

    /**
     * Progress percentage (0-100)
     */
    @Column(name = "progress_percent")
    @Builder.Default
    private Integer progressPercent = 0;

    /**
     * Last error message if sync failed
     */
    @Column(name = "last_error", length = 1000)
    private String lastError;

    /**
     * Whether the user is currently offline
     */
    @Column(name = "is_offline")
    @Builder.Default
    private Boolean isOffline = false;

    /**
     * Timestamp when offline mode was entered
     */
    @Column(name = "offline_since")
    private LocalDateTime offlineSince;

    /**
     * Device ID for this client
     */
    @Column(name = "device_id")
    private String deviceId;

    /**
     * App version for compatibility
     */
    @Column(name = "app_version")
    private String appVersion;

    /**
     * Sync states
     */
    public enum State {
        IDLE,           // No sync activity
        SYNCING,        // Currently syncing
        OFFLINE,        // User is offline
        ERROR,          // Sync error occurred
        PENDING_SYNC    // Changes pending, will sync when online
    }
}