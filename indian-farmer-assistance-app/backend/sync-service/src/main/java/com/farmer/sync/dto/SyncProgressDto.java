package com.farmer.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sync progress information.
 * 
 * Provides progress indicators for large data sets during synchronization.
 * 
 * Validates: Requirements 15.6
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncProgressDto {
    private String userId;
    private int totalItems;
    private int processedItems;
    private int failedItems;
    private Integer pendingItems;
    private Integer inProgressItems;
    private int progressPercent;
    private String status;
}
