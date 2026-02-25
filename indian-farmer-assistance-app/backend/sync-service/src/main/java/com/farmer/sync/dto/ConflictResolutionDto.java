package com.farmer.sync.dto;

import com.farmer.sync.entity.SyncConflict.ResolutionStrategy;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for conflict resolution.
 * 
 * Used to resolve conflicts between local and remote versions.
 * 
 * Validates: Requirement 15.3
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConflictResolutionDto {

    /**
     * Conflict ID
     */
    private Long conflictId;

    /**
     * Resolution strategy to use
     */
    private ResolutionStrategy resolutionStrategy;

    /**
     * Resolved data (for manual or merge resolution)
     */
    private String resolvedData;

    /**
     * User ID who resolved the conflict
     */
    private String resolvedBy;
}