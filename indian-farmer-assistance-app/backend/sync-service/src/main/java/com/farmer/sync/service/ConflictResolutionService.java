package com.farmer.sync.service;

import com.farmer.sync.dto.ConflictDto;
import com.farmer.sync.dto.ConflictResolutionDto;
import com.farmer.sync.entity.SyncConflict;
import com.farmer.sync.entity.SyncConflict.ConflictStatus;
import com.farmer.sync.entity.SyncConflict.ResolutionStrategy;
import com.farmer.sync.repository.SyncConflictRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for resolving sync conflicts between devices.
 * 
 * Uses timestamp-based conflict resolution where the most recent
 * timestamp wins.
 * 
 * Validates: Requirement 15.3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConflictResolutionService {

    private final SyncConflictRepository conflictRepository;

    /**
     * Detect and create a conflict between local and remote versions.
     * 
     * @param userId User ID
     * @param entityType Entity type
     * @param entityId Entity ID
     * @param localData Local version data
     * @param localTimestamp Local version timestamp
     * @param remoteData Remote version data
     * @param remoteTimestamp Remote version timestamp
     * @param remoteDeviceId Device ID that made the remote change
     * @return Created conflict
     */
    @Transactional
    public SyncConflict detectConflict(
            String userId,
            String entityType,
            String entityId,
            String localData,
            LocalDateTime localTimestamp,
            String remoteData,
            LocalDateTime remoteTimestamp,
            String remoteDeviceId) {

        log.debug("Detecting conflict for user {}: {} {}", 
            userId, entityType, entityId);

        // Check if conflict already exists
        Optional<SyncConflict> existing = conflictRepository
            .findByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId);

        if (existing.isPresent()) {
            log.info("Conflict already exists for {} {} {}", entityType, entityId, userId);
            return existing.get();
        }

        // Create new conflict
        SyncConflict conflict = SyncConflict.builder()
            .userId(userId)
            .entityType(entityType)
            .entityId(entityId)
            .localData(localData)
            .localTimestamp(localTimestamp)
            .remoteData(remoteData)
            .remoteTimestamp(remoteTimestamp)
            .remoteDeviceId(remoteDeviceId)
            .status(ConflictStatus.PENDING)
            .detectedAt(LocalDateTime.now())
            .build();

        conflict = conflictRepository.save(conflict);

        log.info("Created conflict {} for user {}: {} {}", 
            conflict.getId(), userId, entityType, entityId);

        return conflict;
    }

    /**
     * Resolve a conflict using timestamp-based resolution.
     * The most recent timestamp wins.
     * 
     * @param conflictId Conflict ID
     * @return Resolved conflict data
     */
    @Transactional
    public SyncConflict resolveByTimestamp(Long conflictId) {
        SyncConflict conflict = conflictRepository.findById(conflictId)
            .orElseThrow(() -> new RuntimeException("Conflict not found: " + conflictId));

        // Determine which version is newer
        boolean localIsNewer = !conflict.getLocalTimestamp()
            .isBefore(conflict.getRemoteTimestamp());

        String resolvedData = localIsNewer 
            ? conflict.getLocalData() 
            : conflict.getRemoteData();

        ResolutionStrategy strategy = ResolutionStrategy.TIMESTAMP;

        conflictRepository.resolveConflict(
            conflictId,
            ConflictStatus.AUTO_RESOLVED,
            resolvedData,
            LocalDateTime.now(),
            "SYSTEM",
            strategy
        );

        log.info("Resolved conflict {} by timestamp: {} wins", 
            conflictId, localIsNewer ? "local" : "remote");

        // Return updated conflict
        return conflictRepository.findById(conflictId).orElse(conflict);
    }

    /**
     * Manually resolve a conflict.
     * 
     * @param resolution Resolution details
     * @return Resolved conflict
     */
    @Transactional
    public SyncConflict resolveManually(ConflictResolutionDto resolution) {
        SyncConflict conflict = conflictRepository.findById(resolution.getConflictId())
            .orElseThrow(() -> new RuntimeException("Conflict not found: " + resolution.getConflictId()));

        String resolvedData = resolution.getResolvedData() != null
            ? resolution.getResolvedData()
            : conflict.getLocalData();

        conflictRepository.resolveConflict(
            resolution.getConflictId(),
            ConflictStatus.RESOLVED,
            resolvedData,
            LocalDateTime.now(),
            resolution.getResolvedBy(),
            resolution.getResolutionStrategy()
        );

        log.info("Manually resolved conflict {} by {}", 
            resolution.getConflictId(), resolution.getResolvedBy());

        return conflictRepository.findById(resolution.getConflictId()).orElse(conflict);
    }

    /**
     * Get all pending conflicts for a user.
     * 
     * @param userId User ID
     * @return List of conflicts
     */
    @Transactional(readOnly = true)
    public List<ConflictDto> getPendingConflicts(String userId) {
        List<SyncConflict> conflicts = conflictRepository
            .findByUserIdAndStatusOrderByDetectedAtDesc(userId, ConflictStatus.PENDING);

        return conflicts.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    /**
     * Get all conflicts for a user.
     * 
     * @param userId User ID
     * @return List of all conflicts
     */
    @Transactional(readOnly = true)
    public List<ConflictDto> getAllConflicts(String userId) {
        List<SyncConflict> conflicts = conflictRepository.findByUserId(userId);

        return conflicts.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    /**
     * Get count of pending conflicts for a user.
     * 
     * @param userId User ID
     * @return Pending conflicts count
     */
    @Transactional(readOnly = true)
    public long getPendingConflictCount(String userId) {
        return conflictRepository.countByUserIdAndStatus(userId, ConflictStatus.PENDING);
    }

    /**
     * Auto-resolve all pending conflicts using timestamp.
     * 
     * @param userId User ID
     * @return Number of conflicts resolved
     */
    @Transactional
    public int autoResolveAll(String userId) {
        List<SyncConflict> conflicts = conflictRepository
            .findByUserIdAndStatusOrderByDetectedAtDesc(userId, ConflictStatus.PENDING);

        int resolved = 0;
        for (SyncConflict conflict : conflicts) {
            try {
                resolveByTimestamp(conflict.getId());
                resolved++;
            } catch (Exception e) {
                log.error("Failed to auto-resolve conflict {}: {}", conflict.getId(), e.getMessage());
            }
        }

        log.info("Auto-resolved {} conflicts for user {}", resolved, userId);
        return resolved;
    }

    /**
     * Map entity to DTO.
     */
    private ConflictDto mapToDto(SyncConflict conflict) {
        boolean localIsNewer = !conflict.getLocalTimestamp()
            .isBefore(conflict.getRemoteTimestamp());

        return ConflictDto.builder()
            .conflictId(conflict.getId())
            .userId(conflict.getUserId())
            .entityType(conflict.getEntityType())
            .entityId(conflict.getEntityId())
            .localData(conflict.getLocalData())
            .localTimestamp(conflict.getLocalTimestamp())
            .remoteData(conflict.getRemoteData())
            .remoteTimestamp(conflict.getRemoteTimestamp())
            .remoteDeviceId(conflict.getRemoteDeviceId())
            .resolutionStrategy(conflict.getResolutionStrategy())
            .status(conflict.getStatus())
            .detectedAt(conflict.getDetectedAt())
            .suggestedResolution(localIsNewer ? "local" : "remote")
            .newerVersion(localIsNewer ? "local" : "remote")
            .build();
    }
}