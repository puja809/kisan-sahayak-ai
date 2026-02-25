package com.farmer.sync.dto;

import com.farmer.sync.entity.SyncQueueItem.OperationType;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for creating a sync queue request.
 * 
 * Used when the frontend queues a request while offline.
 * 
 * Validates: Requirement 12.3
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncRequestDto {

    /**
     * Type of entity being synced (CROP, FERTILIZER, HARVEST, etc.)
     */
    private String entityType;

    /**
     * Type of operation (CREATE, UPDATE, DELETE)
     */
    private OperationType operationType;

    /**
     * Entity ID being synced (null for create operations)
     */
    private String entityId;

    /**
     * JSON payload containing the request data
     */
    private String payload;

    /**
     * Client timestamp for conflict resolution
     */
    private LocalDateTime clientTimestamp;

    /**
     * Priority of the sync request
     */
    @Builder.Default
    private Integer priority = 0;
}