package com.farmer.sync.dto;

import com.farmer.sync.entity.SyncConflict.ConflictStatus;
import com.farmer.sync.entity.SyncConflict.ResolutionStrategy;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for conflict information.
 * 
 * Returns conflict details to the client for resolution.
 * 
 * Validates: Requirements 15.3, 15.5
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConflictDto {

    /**
     * Conflict ID
     */
    private Long conflictId;

    /**
     * User ID
     */
    private String userId;

    /**
     * Entity type
     */
    private String entityType;

    /**
     * Entity ID
     */
    private String entityId;

    /**
     * Local version data
     */
    private String localData;

    /**
     * Local version timestamp
     */
    private LocalDateTime localTimestamp;

    /**
     * Remote version data
     */
    private String remoteData;

    /**
     * Remote version timestamp
     */
    private LocalDateTime remoteTimestamp;

    /**
     * Device ID that made the remote change
     */
    private String remoteDeviceId;

    /**
     * Resolution strategy used
     */
    private ResolutionStrategy resolutionStrategy;

    /**
     * Conflict status
     */
    private ConflictStatus status;

    /**
     * When conflict was detected
     */
    private LocalDateTime detectedAt;

    /**
     * Suggested resolution (most recent wins)
     */
    private String suggestedResolution;

    /**
     * Which version is newer
     */
    private String newerVersion;
}