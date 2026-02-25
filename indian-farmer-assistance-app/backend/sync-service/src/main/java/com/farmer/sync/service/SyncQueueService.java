package com.farmer.sync.service;

import com.farmer.sync.dto.SyncRequestDto;
import com.farmer.sync.dto.SyncResponseDto;
import com.farmer.sync.entity.SyncQueueItem;
import com.farmer.sync.entity.SyncQueueItem.SyncStatus;
import com.farmer.sync.entity.SyncStatus.State;
import com.farmer.sync.exception.SyncException;
import com.farmer.sync.repository.SyncQueueRepository;
import com.farmer.sync.repository.SyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing the sync queue.
 * 
 * Handles queueing requests when offline and processing them in FIFO order
 * when connectivity is restored.
 * 
 * Validates: Requirements 12.3, 12.4
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncQueueService {

    private final SyncQueueRepository syncQueueRepository;
    private final SyncStatusRepository syncStatusRepository;

    @Value("${app.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.sync.batch-size:100}")
    private int batchSize;

    /**
     * Queue a sync request for later processing.
     * 
     * @param userId User ID
     * @param request Sync request details
     * @return Queued item response
     */
    @Transactional
    public SyncResponseDto queueRequest(String userId, SyncRequestDto request) {
        log.debug("Queueing sync request for user {}: {} {}", 
            userId, request.getOperationType(), request.getEntityType());

        SyncQueueItem queueItem = SyncQueueItem.builder()
            .userId(userId)
            .entityType(request.getEntityType())
            .operationType(request.getOperationType())
            .entityId(request.getEntityId())
            .payload(request.getPayload())
            .clientTimestamp(request.getClientTimestamp() != null 
                ? request.getClientTimestamp() 
                : LocalDateTime.now())
            .priority(request.getPriority() != null ? request.getPriority() : 0)
            .status(SyncStatus.PENDING)
            .retryCount(0)
            .createdAt(LocalDateTime.now())
            .build();

        queueItem = syncQueueRepository.save(queueItem);

        // Update pending changes count
        updatePendingChangesCount(userId);

        log.info("Queued sync request {} for user {}", queueItem.getId(), userId);

        return mapToResponseDto(queueItem);
    }

    /**
     * Get all pending items for a user in FIFO order.
     * 
     * @param userId User ID
     * @return List of pending sync items
     */
    @Transactional(readOnly = true)
    public List<SyncResponseDto> getPendingItems(String userId) {
        List<SyncQueueItem> items = syncQueueRepository
            .findByUserIdAndStatusOrderByCreatedAtAsc(userId, SyncStatus.PENDING);

        return items.stream()
            .map(this::mapToResponseDto)
            .collect(Collectors.toList());
    }

    /**
     * Get the next batch of items to sync in FIFO order.
     * 
     * @param userId User ID
     * @return List of items to sync
     */
    @Transactional
    public List<SyncQueueItem> getNextBatch(String userId) {
        List<SyncQueueItem> items = syncQueueRepository
            .findPendingByUserIdOrdered(userId, SyncStatus.PENDING)
            .stream()
            .limit(batchSize)
            .collect(Collectors.toList());

        // Mark items as in progress
        LocalDateTime now = LocalDateTime.now();
        for (SyncQueueItem item : items) {
            item.setStatus(SyncStatus.IN_PROGRESS);
            syncQueueRepository.save(item);
        }

        return items;
    }

    /**
     * Mark a queue item as completed.
     * 
     * @param queueItemId Queue item ID
     */
    @Transactional
    public void markCompleted(Long queueItemId) {
        syncQueueRepository.updateStatus(
            queueItemId, 
            SyncStatus.COMPLETED, 
            LocalDateTime.now()
        );
    }

    /**
     * Mark a queue item as failed with error.
     * 
     * @param queueItemId Queue item ID
     * @param error Error message
     * @return true if can retry, false if max retries exceeded
     */
    @Transactional
    public boolean markFailed(Long queueItemId, String error) {
        SyncQueueItem item = syncQueueRepository.findById(queueItemId)
            .orElseThrow(() -> new SyncException("Queue item not found: " + queueItemId));

        int newRetryCount = item.getRetryCount() + 1;
        boolean canRetry = newRetryCount < maxRetryAttempts;

        SyncStatus newStatus = canRetry ? SyncStatus.PENDING : SyncStatus.FAILED;

        syncQueueRepository.updateStatusWithRetry(
            queueItemId,
            newStatus,
            newRetryCount,
            error,
            LocalDateTime.now()
        );

        log.warn("Queue item {} failed (attempt {}): {}", 
            queueItemId, newRetryCount, error);

        return canRetry;
    }

    /**
     * Mark a queue item as conflicted.
     * 
     * @param queueItemId Queue item ID
     */
    @Transactional
    public void markConflicted(Long queueItemId) {
        syncQueueRepository.updateStatus(
            queueItemId, 
            SyncStatus.CONFLICT, 
            LocalDateTime.now()
        );
    }

    /**
     * Get count of pending items for a user.
     * 
     * @param userId User ID
     * @return Pending items count
     */
    @Transactional(readOnly = true)
    public long getPendingCount(String userId) {
        return syncQueueRepository.countByUserIdAndStatus(userId, SyncStatus.PENDING);
    }

    /**
     * Clear all completed items for a user.
     * 
     * @param userId User ID
     * @return Number of items deleted
     */
    @Transactional
    public int clearCompletedItems(String userId) {
        List<SyncQueueItem> completed = syncQueueRepository
            .findByUserIdAndStatus(userId, SyncStatus.COMPLETED);
        
        syncQueueRepository.deleteAll(completed);
        
        log.info("Cleared {} completed sync items for user {}", completed.size(), userId);
        
        return completed.size();
    }

    /**
     * Delete a specific queue item.
     * 
     * @param queueItemId Queue item ID
     */
    @Transactional
    public void deleteQueueItem(Long queueItemId) {
        syncQueueRepository.deleteById(queueItemId);
    }

    /**
     * Get items that need retry.
     * 
     * @return List of items for retry
     */
    @Transactional(readOnly = true)
    public List<SyncQueueItem> getItemsForRetry() {
        return syncQueueRepository.findItemsForRetry(maxRetryAttempts);
    }

    /**
     * Update pending changes count in sync status.
     * 
     * @param userId User ID
     */
    private void updatePendingChangesCount(String userId) {
        long count = getPendingCount(userId);
        syncStatusRepository.updatePendingChanges(userId, (int) count);
        
        // Update sync state
        syncStatusRepository.findByUserId(userId).ifPresent(status -> {
            if (count > 0 && !status.getIsOffline()) {
                status.setSyncState(State.PENDING_SYNC);
                syncStatusRepository.save(status);
            }
        });
    }

    /**
     * Map entity to DTO.
     */
    private SyncResponseDto mapToResponseDto(SyncQueueItem item) {
        return SyncResponseDto.builder()
            .queueId(item.getId())
            .userId(item.getUserId())
            .entityType(item.getEntityType())
            .operationType(item.getOperationType().name())
            .entityId(item.getEntityId())
            .status(item.getStatus())
            .retryCount(item.getRetryCount())
            .errorMessage(item.getLastError())
            .createdAt(item.getCreatedAt())
            .processedAt(item.getProcessedAt())
            .build();
    }
}