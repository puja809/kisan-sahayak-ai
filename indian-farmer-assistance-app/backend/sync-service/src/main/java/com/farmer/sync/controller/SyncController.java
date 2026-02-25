package com.farmer.sync.controller;

import com.farmer.sync.dto.*;
import com.farmer.sync.entity.SyncConflict;
import com.farmer.sync.entity.SyncQueueItem;
import com.farmer.sync.service.ConflictResolutionService;
import com.farmer.sync.service.SyncQueueService;
import com.farmer.sync.service.SyncStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for sync operations.
 * 
 * Provides endpoints for:
 * - Queueing sync requests
 * - Getting sync status
 * - Managing conflicts
 * - Triggering synchronization
 * 
 * Validates: Requirements 12.3, 12.4, 15.2, 15.3, 15.5, 15.6
 */
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final SyncQueueService syncQueueService;
    private final SyncStatusService syncStatusService;
    private final ConflictResolutionService conflictResolutionService;

    /**
     * Queue a sync request for later processing.
     * 
     * POST /api/v1/sync/queue
     */
    @PostMapping("/queue")
    public ResponseEntity<SyncResponseDto> queueRequest(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SyncRequestDto request) {
        
        log.info("Queueing sync request for user {}", userId);
        SyncResponseDto response = syncQueueService.queueRequest(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all pending sync items for a user.
     * 
     * GET /api/v1/sync/queue
     */
    @GetMapping("/queue")
    public ResponseEntity<List<SyncResponseDto>> getPendingItems(
            @RequestHeader("X-User-Id") String userId) {
        
        List<SyncResponseDto> items = syncQueueService.getPendingItems(userId);
        return ResponseEntity.ok(items);
    }

    /**
     * Get sync status for a user.
     * 
     * GET /api/v1/sync/status
     */
    @GetMapping("/status")
    public ResponseEntity<SyncStatusDto> getSyncStatus(
            @RequestHeader("X-User-Id") String userId) {
        
        SyncStatusDto status = syncStatusService.getStatusDto(userId);
        return ResponseEntity.ok(status);
    }

    /**
     * Enter offline mode.
     * 
     * POST /api/v1/sync/offline
     */
    @PostMapping("/offline")
    public ResponseEntity<Void> enterOfflineMode(
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("User {} entering offline mode", userId);
        syncStatusService.enterOfflineMode(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Exit offline mode and trigger sync.
     * 
     * POST /api/v1/sync/online
     */
    @PostMapping("/online")
    public ResponseEntity<SyncStatusDto> exitOfflineMode(
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("User {} exiting offline mode", userId);
        syncStatusService.exitOfflineMode(userId);
        SyncStatusDto status = syncStatusService.getStatusDto(userId);
        return ResponseEntity.ok(status);
    }

    /**
     * Get pending conflicts for a user.
     * 
     * GET /api/v1/sync/conflicts
     */
    @GetMapping("/conflicts")
    public ResponseEntity<List<ConflictDto>> getConflicts(
            @RequestHeader("X-User-Id") String userId) {
        
        List<ConflictDto> conflicts = conflictResolutionService.getPendingConflicts(userId);
        return ResponseEntity.ok(conflicts);
    }

    /**
     * Resolve a conflict by timestamp (auto-resolve).
     * 
     * POST /api/v1/sync/conflicts/{conflictId}/resolve/timestamp
     */
    @PostMapping("/conflicts/{conflictId}/resolve/timestamp")
    public ResponseEntity<SyncConflict> resolveConflictByTimestamp(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long conflictId) {
        
        log.info("Resolving conflict {} by timestamp for user {}", conflictId, userId);
        SyncConflict resolved = conflictResolutionService.resolveByTimestamp(conflictId);
        return ResponseEntity.ok(resolved);
    }

    /**
     * Resolve a conflict manually.
     * 
     * POST /api/v1/sync/conflicts/{conflictId}/resolve
     */
    @PostMapping("/conflicts/{conflictId}/resolve")
    public ResponseEntity<SyncConflict> resolveConflictManually(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long conflictId,
            @RequestBody ConflictResolutionDto resolution) {
        
        log.info("Resolving conflict {} manually for user {}", conflictId, userId);
        resolution.setConflictId(conflictId);
        SyncConflict resolved = conflictResolutionService.resolveManually(resolution);
        return ResponseEntity.ok(resolved);
    }

    /**
     * Auto-resolve all pending conflicts.
     * 
     * POST /api/v1/sync/conflicts/auto-resolve-all
     */
    @PostMapping("/conflicts/auto-resolve-all")
    public ResponseEntity<Integer> autoResolveAllConflicts(
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("Auto-resolving all conflicts for user {}", userId);
        int resolved = conflictResolutionService.autoResolveAll(userId);
        return ResponseEntity.ok(resolved);
    }

    /**
     * Trigger synchronization for a user.
     * 
     * POST /api/v1/sync/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<SyncStatusDto> triggerSync(
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("Triggering sync for user {}", userId);
        // This would typically trigger the sync process
        // For now, just return the current status
        SyncStatusDto status = syncStatusService.getStatusDto(userId);
        return ResponseEntity.ok(status);
    }

    /**
     * Clear completed sync items.
     * 
     * DELETE /api/v1/sync/queue
     */
    @DeleteMapping("/queue")
    public ResponseEntity<Integer> clearCompletedItems(
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("Clearing completed sync items for user {}", userId);
        int cleared = syncQueueService.clearCompletedItems(userId);
        return ResponseEntity.ok(cleared);
    }

    /**
     * Update device info.
     * 
     * PUT /api/v1/sync/device
     */
    @PutMapping("/device")
    public ResponseEntity<Void> updateDeviceInfo(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String deviceId,
            @RequestParam String appVersion) {
        
        log.info("Updating device info for user {}", userId);
        syncStatusService.updateDeviceInfo(userId, deviceId, appVersion);
        return ResponseEntity.ok().build();
    }
}