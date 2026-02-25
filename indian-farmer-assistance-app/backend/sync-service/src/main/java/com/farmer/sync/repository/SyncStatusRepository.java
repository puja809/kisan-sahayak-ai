package com.farmer.sync.repository;

import com.farmer.sync.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for sync status operations.
 * 
 * Provides methods for managing sync status for users.
 * 
 * Validates: Requirements 15.2, 15.5, 15.6
 */
@Repository
public interface SyncStatusRepository extends JpaRepository<SyncStatus, Long> {

    /**
     * Find sync status by user ID.
     */
    Optional<SyncStatus> findByUserId(String userId);

    /**
     * Update last sync timestamp.
     */
    @Modifying
    @Query("UPDATE SyncStatus s SET s.lastSyncAt = :syncAt WHERE s.userId = :userId")
    int updateLastSyncAt(
        @Param("userId") String userId, 
        @Param("syncAt") LocalDateTime syncAt
    );

    /**
     * Update pending changes count.
     */
    @Modifying
    @Query("UPDATE SyncStatus s SET s.pendingChanges = :count WHERE s.userId = :userId")
    int updatePendingChanges(
        @Param("userId") String userId, 
        @Param("count") int count
    );

    /**
     * Enter offline mode.
     */
    @Modifying
    @Query("UPDATE SyncStatus s SET s.isOffline = true, s.offlineSince = :offlineSince, " +
           "s.syncState = 'OFFLINE' WHERE s.userId = :userId")
    int enterOfflineMode(
        @Param("userId") String userId, 
        @Param("offlineSince") LocalDateTime offlineSince
    );

    /**
     * Exit offline mode.
     */
    @Modifying
    @Query("UPDATE SyncStatus s SET s.isOffline = false, s.offlineSince = null, " +
           "s.syncState = 'IDLE' WHERE s.userId = :userId")
    int exitOfflineMode(@Param("userId") String userId);

    /**
     * Update sync progress.
     */
    @Modifying
    @Query("UPDATE SyncStatus s SET s.syncingCount = :syncing, " +
           "s.totalToSync = :total, s.progressPercent = :percent, " +
           "s.syncState = 'SYNCING' WHERE s.userId = :userId")
    int updateSyncProgress(
        @Param("userId") String userId,
        @Param("syncing") int syncing,
        @Param("total") int total,
        @Param("percent") int percent
    );

    /**
     * Set sync error.
     */
    @Modifying
    @Query("UPDATE SyncStatus s SET s.syncState = 'ERROR', " +
           "s.lastError = :error WHERE s.userId = :userId")
    int setSyncError(
        @Param("userId") String userId, 
        @Param("error") String error
    );

    /**
     * Create or update sync status for a user.
     */
    @Modifying
    @Query("UPDATE SyncStatus s SET s.deviceId = :deviceId, s.appVersion = :appVersion " +
           "WHERE s.userId = :userId")
    int updateDeviceInfo(
        @Param("userId") String userId,
        @Param("deviceId") String deviceId,
        @Param("appVersion") String appVersion
    );
}