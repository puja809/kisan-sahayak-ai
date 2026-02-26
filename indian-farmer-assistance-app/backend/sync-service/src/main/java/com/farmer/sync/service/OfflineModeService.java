package com.farmer.sync.service;

import com.farmer.sync.entity.SyncStatus;
import com.farmer.sync.entity.SyncStatus.State;
import com.farmer.sync.repository.SyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing offline mode detection and state.
 * 
 * Detects network connectivity changes and automatically enables/disables offline mode.
 * Displays offline indicator to user within 2 seconds of detecting disconnection.
 * 
 * Validates: Requirements 12.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfflineModeService {

    private final SyncStatusRepository syncStatusRepository;

    @Value("${app.offline.detection-timeout-ms:2000}")
    private long detectionTimeoutMs;

    /**
     * Detect network connectivity and update offline mode state.
     * 
     * @param userId User ID
     * @param isConnected Whether network is connected
     * @return Updated sync status
     */
    @Transactional
    public SyncStatus updateConnectivityState(String userId, boolean isConnected) {
        log.debug("Updating connectivity state for user {}: {}", userId, isConnected);

        Optional<SyncStatus> existing = syncStatusRepository.findByUserId(userId);
        SyncStatus status = existing.orElseGet(() -> createNewSyncStatus(userId));

        boolean wasOffline = status.getIsOffline();
        status.setIsOffline(!isConnected);

        // Update sync state based on connectivity
        if (isConnected && wasOffline) {
            // Connectivity restored
            status.setSyncState(State.SYNCING);
            log.info("Connectivity restored for user {}, initiating sync", userId);
        } else if (!isConnected && !wasOffline) {
            // Connectivity lost
            status.setSyncState(State.OFFLINE);
            status.setOfflineSince(LocalDateTime.now());
            log.info("Connectivity lost for user {}, enabling offline mode", userId);
        }

        status = syncStatusRepository.save(status);

        log.debug("Updated sync status for user {}: offline={}, state={}", 
            userId, status.getIsOffline(), status.getSyncState());

        return status;
    }

    /**
     * Check if user is in offline mode.
     * 
     * @param userId User ID
     * @return true if offline, false if online
     */
    @Transactional(readOnly = true)
    public boolean isOffline(String userId) {
        Optional<SyncStatus> status = syncStatusRepository.findByUserId(userId);
        return status.map(SyncStatus::getIsOffline).orElse(false);
    }

    /**
     * Get offline indicator for display to user.
     * Should be displayed within 2 seconds of detecting disconnection.
     * 
     * @param userId User ID
     * @return Offline indicator DTO
     */
    @Transactional(readOnly = true)
    public OfflineIndicator getOfflineIndicator(String userId) {
        Optional<SyncStatus> status = syncStatusRepository.findByUserId(userId);

        if (status.isEmpty()) {
            return OfflineIndicator.builder()
                .isOffline(false)
                .message("Online")
                .build();
        }

        SyncStatus syncStatus = status.get();
        boolean isOffline = syncStatus.getIsOffline();

        return OfflineIndicator.builder()
            .isOffline(isOffline)
            .message(isOffline ? "Offline - Changes will sync when online" : "Online")
            .pendingChanges(syncStatus.getPendingChanges())
            .lastSyncTime(syncStatus.getLastSyncAt())
            .syncState(syncStatus.getSyncState())
            .build();
    }

    /**
     * Enable offline mode for a user.
     * 
     * @param userId User ID
     */
    @Transactional
    public void enableOfflineMode(String userId) {
        log.info("Enabling offline mode for user {}", userId);

        Optional<SyncStatus> existing = syncStatusRepository.findByUserId(userId);
        SyncStatus status = existing.orElseGet(() -> createNewSyncStatus(userId));

        status.setIsOffline(true);
        status.setSyncState(State.OFFLINE);
        status.setOfflineSince(LocalDateTime.now());

        syncStatusRepository.save(status);
    }

    /**
     * Disable offline mode for a user.
     * 
     * @param userId User ID
     */
    @Transactional
    public void disableOfflineMode(String userId) {
        log.info("Disabling offline mode for user {}", userId);

        Optional<SyncStatus> existing = syncStatusRepository.findByUserId(userId);
        if (existing.isEmpty()) {
            return;
        }

        SyncStatus status = existing.get();
        status.setIsOffline(false);
        status.setSyncState(State.SYNCING);

        syncStatusRepository.save(status);
    }

    /**
     * Create a new sync status for a user.
     * 
     * @param userId User ID
     * @return New sync status
     */
    private SyncStatus createNewSyncStatus(String userId) {
        return SyncStatus.builder()
            .userId(userId)
            .isOffline(false)
            .syncState(State.IDLE)
            .pendingChanges(0)
            .lastSyncAt(null)
            .build();
    }

    /**
     * DTO for offline indicator display.
     */
    @lombok.Data
    @lombok.Builder
    public static class OfflineIndicator {
        private boolean isOffline;
        private String message;
        private Integer pendingChanges;
        private LocalDateTime lastSyncTime;
        private State syncState;
    }
}
