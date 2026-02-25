package com.farmer.sync.dto;

import com.farmer.sync.entity.SyncQueueItem.SyncStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for sync queue response.
 * 
 * Validates: Requirements 12.4, 15.5, 15.6
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncResponseDto {

    /**
     * Queue item ID
     */
    private Long queueId;

    /**
     * User ID
     */
    private String userId;

    /**
     * Entity type
     */
    private String entityType;

    /**
     * Operation type
     */
    private String operationType;

    /**
     * Entity ID
     */
    private String entityId;

    /**
     * Current status
     */
    private SyncStatus status;

    /**
     * Number of retry attempts
     */
    private Integer retryCount;

    /**
     * Error message if failed
     */
    private String errorMessage;

    /**
     * When the request was created
     */
    private LocalDateTime createdAt;

    /**
     * When the request was processed
     */
    private LocalDateTime processedAt;
}