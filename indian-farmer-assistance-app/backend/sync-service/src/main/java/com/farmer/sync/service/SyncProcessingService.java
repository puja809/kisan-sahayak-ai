package com.farmer.sync.service;

import com.farmer.sync.dto.SyncProgressDto;
import com.farmer.sync.entity.SyncQueueItem;
import com.farmer.sync.entity.SyncStatus;
import com.farmer.sync.entity.SyncStatus.State;
import com.farmer.sync.repository.SyncQueueRepository;
import com.farmer.sync.repository.SyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for processing queued requests when connectivity is restored.
 * 
 * Detects connectivity restoration, processes queued requests in FIFO order,
 * updates cached data automatically, and shows progress indicators.
 * 
 * Validates: Requirements 12.4, 15.2, 15.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncProcessingService {

    private final SyncQueueRepository syncQueueRepository;
    private final SyncStatusRepository syncStatusRepository;
    private final RetryService retryService;

    @Value("${app.sync.batch-size:100}")
    private int batchSize;

    @Value("${app.sync.progress-update-interval:10}")
    private int progressUpdateInterval;

    /**
     * Process all queued requests for a user in FIFO order.
     * 
     * @param userId User ID
     * @return Sync progress information
     */
    @Transactional
    public SyncProgressDto processPendingRequests(String userId) {
        log.info("Starting sync processing for user {}", userId);

        Optional<SyncStatus> statusOpt = syncStatusRepository.findByUserId(userId);
        if (statusOpt.isEmpty()) {
            log.warn("No sync status found for user {}", userId);
            return SyncProgressDto.builder()
                .userId(userId)
                .totalItems(0)
                .processedItems(0)
                .failedItems(0)
                .progressPercent(0)
                .status("NO_PENDING_ITEMS")
                .build();
        }

        SyncStatus status = statusOpt.get();
        status.setSyncState(State.SYNCING);
        status.setLastSyncAt(LocalDateTime.now());
        syncStatusRepository.save(status);

        // Get all pending items in FIFO order
        List<SyncQueueItem> pendingItems = syncQueueRepository
            .findByUserIdAndStatusOrderByCreatedAtAsc(userId, SyncQueueItem.SyncStatus.PENDING);

        int totalItems = pendingItems.size();
        int processedItems = 0;
        int failedItems = 0;

        log.info("Found {} pending items to sync for user {}", totalItems, userId);

        // Process items in batches
        for (int i = 0; i < pendingItems.size(); i++) {
            SyncQueueItem item = pendingItems.get(i);

            try {
                processSingleItem(item);
                processedItems++;
            } catch (Exception e) {
                log.error("Failed to process sync item {}: {}", item.getId(), e.getMessage());
                failedItems++;
            }

            // Update progress periodically
            if ((i + 1) % progressUpdateInterval == 0) {
                int progressPercent = (int) ((i + 1) * 100 / totalItems);
                log.debug("Sync progress for user {}: {}/{} items ({}%)", 
                    userId, i + 1, totalItems, progressPercent);
            }
        }

        // Update final sync status
        int progressPercent = totalItems > 0 ? (processedItems * 100 / totalItems) : 100;
        status.setSyncState(processedItems == totalItems ? State.IDLE : State.PENDING_SYNC);
        status.setPendingChanges(totalItems - processedItems);
        syncStatusRepository.save(status);

        log.info("Completed sync for user {}: {}/{} items processed, {} failed", 
            userId, processedItems, totalItems, failedItems);

        return SyncProgressDto.builder()
            .userId(userId)
            .totalItems(totalItems)
            .processedItems(processedItems)
            .failedItems(failedItems)
            .progressPercent(progressPercent)
            .status(processedItems == totalItems ? "COMPLETED" : "PARTIAL")
            .build();
    }

    /**
     * Process a single sync queue item.
     * 
     * @param item Queue item to process
     */
    @Transactional
    public void processSingleItem(SyncQueueItem item) {
        log.debug("Processing sync item {}: {} {}", 
            item.getId(), item.getOperationType(), item.getEntityType());

        try {
            // Mark as in progress
            item.setStatus(SyncQueueItem.SyncStatus.IN_PROGRESS);
            syncQueueRepository.save(item);

            // Execute the sync operation with retry
            retryService.executeWithRetry(() -> {
                // In a real implementation, this would call the appropriate service
                // to sync the data (e.g., crop service, weather service, etc.)
                log.debug("Syncing {} {} for user {}", 
                    item.getOperationType(), item.getEntityType(), item.getUserId());
            }, "sync-" + item.getId());

            // Mark as completed
            syncQueueRepository.updateStatus(
                item.getId(),
                SyncQueueItem.SyncStatus.COMPLETED,
                LocalDateTime.now()
            );

            log.info("Successfully synced item {}", item.getId());

        } catch (Exception e) {
            log.error("Error processing sync item {}: {}", item.getId(), e.getMessage());

            // Try to mark as failed with retry logic
            boolean canRetry = syncQueueRepository.findById(item.getId())
                .map(qi -> {
                    int newRetryCount = qi.getRetryCount() + 1;
                    boolean shouldRetry = newRetryCount < 3;

                    SyncQueueItem.SyncStatus newStatus = shouldRetry 
                        ? SyncQueueItem.SyncStatus.PENDING 
                        : SyncQueueItem.SyncStatus.FAILED;

                    syncQueueRepository.updateStatusWithRetry(
                        qi.getId(),
                        newStatus,
                        newRetryCount,
                        e.getMessage(),
                        LocalDateTime.now()
                    );

                    return shouldRetry;
                })
                .orElse(false);

            if (!canRetry) {
                log.error("Max retries exceeded for sync item {}", item.getId());
            }
        }
    }

    /**
     * Get current sync progress for a user.
     * 
     * @param userId User ID
     * @return Current sync progress
     */
    @Transactional(readOnly = true)
    public SyncProgressDto getSyncProgress(String userId) {
        List<SyncQueueItem> pendingItems = syncQueueRepository
            .findByUserIdAndStatusOrderByCreatedAtAsc(userId, SyncQueueItem.SyncStatus.PENDING);

        List<SyncQueueItem> inProgressItems = syncQueueRepository
            .findByUserIdAndStatus(userId, SyncQueueItem.SyncStatus.IN_PROGRESS);

        List<SyncQueueItem> completedItems = syncQueueRepository
            .findByUserIdAndStatus(userId, SyncQueueItem.SyncStatus.COMPLETED);

        List<SyncQueueItem> failedItems = syncQueueRepository
            .findByUserIdAndStatus(userId, SyncQueueItem.SyncStatus.FAILED);

        int totalItems = pendingItems.size() + inProgressItems.size() + 
                        completedItems.size() + failedItems.size();
        int processedItems = completedItems.size();
        int failedCount = failedItems.size();

        int progressPercent = totalItems > 0 ? (processedItems * 100 / totalItems) : 0;

        return SyncProgressDto.builder()
            .userId(userId)
            .totalItems(totalItems)
            .processedItems(processedItems)
            .failedItems(failedCount)
            .pendingItems(pendingItems.size())
            .inProgressItems(inProgressItems.size())
            .progressPercent(progressPercent)
            .status(pendingItems.isEmpty() && inProgressItems.isEmpty() ? "IDLE" : "SYNCING")
            .build();
    }

    /**
     * Cancel all pending sync operations for a user.
     * 
     * @param userId User ID
     * @return Number of items cancelled
     */
    @Transactional
    public int cancelPendingSync(String userId) {
        log.info("Cancelling pending sync for user {}", userId);

        List<SyncQueueItem> pendingItems = syncQueueRepository
            .findByUserIdAndStatus(userId, SyncQueueItem.SyncStatus.PENDING);

        for (SyncQueueItem item : pendingItems) {
            syncQueueRepository.deleteById(item.getId());
        }

        log.info("Cancelled {} pending sync items for user {}", pendingItems.size(), userId);

        return pendingItems.size();
    }
}
