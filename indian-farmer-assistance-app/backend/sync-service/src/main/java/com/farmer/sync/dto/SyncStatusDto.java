package com.farmer.sync.dto;

import com.farmer.sync.entity.SyncStatus.State;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for sync status response.
 * 
 * Provides information about the current sync state, last sync time,
 * and pending changes count.
 * 
 * Validates: Requirements 15.2, 15.5, 15.6
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncStatusDto {

    /**
     * User ID
     */
    private String userId;

    /**
     * Last successful sync timestamp
     */
    private LocalDateTime lastSyncAt;

    /**
     * Number of pending changes to sync
     */
    private Integer pendingChanges;

    /**
     * Current sync state
     */
    private State syncState;

    /**
     * Number of items currently syncing
     */
    private Integer syncingCount;

    /**
     * Total items to sync in current batch
     */
    private Integer totalToSync;

    /**
     * Progress percentage (0-100)
     */
    private Integer progressPercent;

    /**
     * Whether the user is currently offline
     */
    private Boolean isOffline;

    /**
     * Time since offline mode was entered (in seconds)
     */
    private Long offlineDurationSeconds;

    /**
     * Last error message if sync failed
     */
    private String lastError;

    /**
     * Human-readable sync status message
     */
    private String statusMessage;
}