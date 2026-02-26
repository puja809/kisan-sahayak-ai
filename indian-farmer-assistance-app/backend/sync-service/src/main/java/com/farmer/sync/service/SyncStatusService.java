package com.farmer.sync.service;

import com.farmer.sync.dto.SyncStatusDto;
import com.farmer.sync.entity.SyncStatus;
import com.farmer.sync.entity.SyncStatus.State;
import com.farmer.sync.repository.SyncQueueRepository;
import com.farmer.sync.repository.SyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing sync status.
 * 
 * Tracks the last sync time, pending changes, and overall sync health.
 * 
 * Validates: Requirements 15.2, 15.5, 15.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncStatusService {

    private final SyncStatusRepository syncStatusRepository;
    private final SyncQueueRepository syncQueueRepository;

    /**
     * Get or create sync status for a user.
     * 
     * @param userId User ID
     * @return Sync status
     */
    @Transactional
    public SyncStatus getOrCreateStatus(String userId) {
        return syncStatusRepository.findByUserId(userId)
            .orElseGet(() -> createStatus(userId));
    }

    /**
     * Create a new sync status for a user.
     * 
     * @param userId User ID
     * @return Created sync status
     */
    @Transactional
    public SyncStatus createStatus(String userId) {
        log.info("Creating sync status for user {}", userId);

        SyncStatus status = SyncStatus.builder()
            .userId(userId)
            .syncState(State.IDLE)
            .pendingChanges(0)
            .syncingCount(0)
            .totalToSync(0)
            .progressPercent(0)
            .isOffline(false)
            .build();

        return syncStatusRepository.save(status);
    }

    /**
     * Get sync status DTO for a user.
     * 
     * @param userId User ID
     * @return Sync status DTO
     */
    @Transactional(readOnly = true)
    public SyncStatusDto getStatusDto(String userId) {
        SyncStatus status = getOrCreateStatus(userId);
        return mapToDto(status);
    }

    /**
     * Update last sync timestamp.
     * 
     * @param userId User ID
     */
    @Transactional
    public void updateLastSyncTime(String userId) {
        syncStatusRepository.updateLastSyncAt(userId, LocalDateTime.now());
        
        syncStatusRepository.findByUserId(userId).ifPresent(status -> {
            status.setSyncState(State.IDLE);
            status.setPendingChanges(0);
            status.setProgressPercent(100);
            syncStatusRepository.save(status);
        });

        log.info("Updated last sync time for user {}", userId);
    }

    /**
     * Enter offline mode for a user.
     * 
     * @param userId User ID
     */
    @Transactional
    public void enterOfflineMode(String userId) {
        syncStatusRepository.enterOfflineMode(userId, LocalDateTime.now());
        log.info("User {} entered offline mode", userId);
    }

    /**
     * Exit offline mode for a user.
     * 
     * @param userId User ID
     */
    @Transactional
    public void exitOfflineMode(String userId) {
        syncStatusRepository.exitOfflineMode(userId);
        log.info("User {} exited offline mode", userId);
    }

    /**
     * Update sync progress.
     * 
     * @param userId User ID
     * @param syncing Number of items currently syncing
     * @param total Total items to sync
     * @param progress Progress percentage
     */
    @Transactional
    public void updateProgress(String userId, int syncing, int total, int progress) {
        syncStatusRepository.updateSyncProgress(userId, syncing, total, progress);
    }

    /**
     * Set sync error for a user.
     * 
     * @param userId User ID
     * @param error Error message
     */
    @Transactional
    public void setSyncError(String userId, String error) {
        syncStatusRepository.setSyncError(userId, error);
        log.warn("Sync error for user {}: {}", userId, error);
    }

    /**
     * Update device info for a user.
     * 
     * @param userId User ID
     * @param deviceId Device ID
     * @param appVersion App version
     */
    @Transactional
    public void updateDeviceInfo(String userId, String deviceId, String appVersion) {
        syncStatusRepository.updateDeviceInfo(userId, deviceId, appVersion);
    }

    /**
     * Check if user is offline.
     * 
     * @param userId User ID
     * @return true if offline
     */
    @Transactional(readOnly = true)
    public boolean isOffline(String userId) {
        return syncStatusRepository.findByUserId(userId)
            .map(SyncStatus::getIsOffline)
            .orElse(false);
    }

    /**
     * Get pending changes count.
     * 
     * @param userId User ID
     * @return Pending changes count
     */
    @Transactional(readOnly = true)
    public int getPendingChangesCount(String userId) {
        return (int) syncQueueRepository.countByUserIdAndStatus(
            userId, com.farmer.sync.entity.SyncQueueItem.SyncStatus.PENDING);
    }

    /**
     * Map entity to DTO.
     */
    private SyncStatusDto mapToDto(SyncStatus status) {
        Long offlineDurationSeconds = null;
        if (status.getIsOffline() != null && status.getIsOffline() && status.getOfflineSince() != null) {
            offlineDurationSeconds = Duration.between(status.getOfflineSince(), LocalDateTime.now()).getSeconds();
        }

        String statusMessage = getStatusMessage(status);

        return SyncStatusDto.builder()
            .userId(status.getUserId())
            .lastSyncAt(status.getLastSyncAt())
            .pendingChanges(status.getPendingChanges())
            .syncState(status.getSyncState())
            .syncingCount(status.getSyncingCount())
            .totalToSync(status.getTotalToSync())
            .progressPercent(status.getProgressPercent())
            .isOffline(status.getIsOffline())
            .offlineDurationSeconds(offlineDurationSeconds)
            .lastError(status.getLastError())
            .statusMessage(statusMessage)
            .build();
    }

    /**
     * Get human-readable status message.
     */
    private String getStatusMessage(SyncStatus status) {
        if (status.getIsOffline() != null && status.getIsOffline()) {
            return "You are offline. Changes will sync when you're back online.";
        }

        return switch (status.getSyncState()) {
            case IDLE -> "All data is synced.";
            case SYNCING -> "Syncing " + status.getSyncingCount() + " of " + status.getTotalToSync() + " items...";
            case PENDING_SYNC -> status.getPendingChanges() + " changes pending sync.";
            case OFFLINE -> "You are offline.";
            case ERROR -> "Sync error: " + (status.getLastError() != null ? status.getLastError() : "Unknown error");
        };
    }
}