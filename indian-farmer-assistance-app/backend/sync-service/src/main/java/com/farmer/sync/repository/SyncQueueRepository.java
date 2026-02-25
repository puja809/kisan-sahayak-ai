package com.farmer.sync.repository;

import com.farmer.sync.entity.SyncQueueItem;
import com.farmer.sync.entity.SyncQueueItem.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for sync queue operations.
 * 
 * Provides methods for managing queued sync requests with FIFO processing.
 * 
 * Validates: Requirements 12.3, 12.4
 */
@Repository
public interface SyncQueueRepository extends JpaRepository<SyncQueueItem, Long> {

    /**
     * Find all pending items for a user, ordered by creation time (FIFO).
     */
    List<SyncQueueItem> findByUserIdAndStatusOrderByCreatedAtAsc(
        String userId, 
        SyncStatus status
    );

    /**
     * Find all pending items for a user.
     */
    List<SyncQueueItem> findByUserIdAndStatus(
        String userId, 
        SyncStatus status
    );

    /**
     * Find all pending items for a user, ordered by priority and creation time.
     */
    @Query("SELECT q FROM SyncQueueItem q WHERE q.userId = :userId AND q.status = :status " +
           "ORDER BY q.priority DESC, q.createdAt ASC")
    List<SyncQueueItem> findPendingByUserIdOrdered(
        @Param("userId") String userId, 
        @Param("status") SyncStatus status
    );

    /**
     * Count pending items for a user.
     */
    long countByUserIdAndStatus(String userId, SyncStatus status);

    /**
     * Find items that need retry (failed but not exceeded max retries).
     */
    @Query("SELECT q FROM SyncQueueItem q WHERE q.status = 'FAILED' " +
           "AND q.retryCount < :maxRetries ORDER BY q.processedAt ASC")
    List<SyncQueueItem> findItemsForRetry(@Param("maxRetries") int maxRetries);

    /**
     * Update status of a queue item.
     */
    @Modifying
    @Query("UPDATE SyncQueueItem q SET q.status = :status, q.processedAt = :processedAt " +
           "WHERE q.id = :id")
    int updateStatus(
        @Param("id") Long id, 
        @Param("status") SyncStatus status, 
        @Param("processedAt") LocalDateTime processedAt
    );

    /**
     * Update status and retry count.
     */
    @Modifying
    @Query("UPDATE SyncQueueItem q SET q.status = :status, q.retryCount = :retryCount, " +
           "q.lastError = :error, q.processedAt = :processedAt WHERE q.id = :id")
    int updateStatusWithRetry(
        @Param("id") Long id, 
        @Param("status") SyncStatus status, 
        @Param("retryCount") int retryCount,
        @Param("error") String error,
        @Param("processedAt") LocalDateTime processedAt
    );

    /**
     * Delete old completed items.
     */
    @Modifying
    @Query("DELETE FROM SyncQueueItem q WHERE q.status = 'COMPLETED' " +
           "AND q.processedAt < :before")
    int deleteOldCompletedItems(@Param("before") LocalDateTime before);

    /**
     * Find all items for a user and entity.
     */
    List<SyncQueueItem> findByUserIdAndEntityTypeAndEntityId(
        String userId, 
        String entityType, 
        String entityId
    );

    /**
     * Delete all items for a user.
     */
    @Modifying
    @Query("DELETE FROM SyncQueueItem q WHERE q.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}