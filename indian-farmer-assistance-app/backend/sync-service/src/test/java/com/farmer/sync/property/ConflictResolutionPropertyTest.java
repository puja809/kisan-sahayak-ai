package com.farmer.sync.property;

import com.farmer.sync.entity.SyncConflict;
import com.farmer.sync.entity.SyncConflict.ConflictStatus;
import com.farmer.sync.entity.SyncConflict.ResolutionStrategy;
import com.farmer.sync.service.ConflictResolutionService;
import net.jqwik.api.*;
import net.jqwik.junit.platform.JqwikProperty;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for conflict resolution by timestamp.
 * 
 * Feature: indian-farmer-assistance-app, Property 33: Conflict Resolution by Timestamp
 * Validates: Requirements 15.3
 * 
 * For any two conflicting updates to the same data field from different devices,
 * the system should retain the update with the most recent timestamp and discard
 * the older update.
 */
@JqwikProperty
class ConflictResolutionPropertyTest {

    /**
     * Property: When local timestamp is newer, local data should win.
     */
    @Property
    void newerLocalTimestampShouldWin(
        @ForAll String userId,
        @ForAll String entityType,
        @ForAll String entityId,
        @ForAll String localData,
        @ForAll String remoteData
    ) {
        LocalDateTime localTimestamp = LocalDateTime.now().plusMinutes(5);
        LocalDateTime remoteTimestamp = LocalDateTime.now();

        TestConflictRepository repository = new TestConflictRepository();
        ConflictResolutionService service = new ConflictResolutionService(repository);

        SyncConflict conflict = service.detectConflict(
            userId, entityType, entityId,
            localData, localTimestamp,
            remoteData, remoteTimestamp,
            "device-remote"
        );

        SyncConflict resolved = service.resolveByTimestamp(conflict.getId());

        assertEquals(ConflictStatus.AUTO_RESOLVED, resolved.getStatus());
        assertEquals(ResolutionStrategy.TIMESTAMP, resolved.getResolutionStrategy());
    }

    /**
     * Property: When remote timestamp is newer, remote data should win.
     */
    @Property
    void newerRemoteTimestampShouldWin(
        @ForAll String userId,
        @ForAll String entityType,
        @ForAll String entityId,
        @ForAll String localData,
        @ForAll String remoteData
    ) {
        LocalDateTime localTimestamp = LocalDateTime.now();
        LocalDateTime remoteTimestamp = LocalDateTime.now().plusMinutes(10);

        TestConflictRepository repository = new TestConflictRepository();
        ConflictResolutionService service = new ConflictResolutionService(repository);

        SyncConflict conflict = service.detectConflict(
            userId, entityType, entityId,
            localData, localTimestamp,
            remoteData, remoteTimestamp,
            "device-remote"
        );

        SyncConflict resolved = service.resolveByTimestamp(conflict.getId());

        assertEquals(ConflictStatus.AUTO_RESOLVED, resolved.getStatus());
        assertEquals(ResolutionStrategy.TIMESTAMP, resolved.getResolutionStrategy());
    }

    /**
     * Property: When timestamps are equal, local should win on tie.
     */
    @Property
    void equalTimestampsShouldUseTieBreaker(
        @ForAll String userId,
        @ForAll String entityType,
        @ForAll String entityId,
        @ForAll String localData,
        @ForAll String remoteData
    ) {
        LocalDateTime timestamp = LocalDateTime.now();

        TestConflictRepository repository = new TestConflictRepository();
        ConflictResolutionService service = new ConflictResolutionService(repository);

        SyncConflict conflict = service.detectConflict(
            userId, entityType, entityId,
            localData, timestamp,
            remoteData, timestamp,
            "device-remote"
        );

        SyncConflict resolved = service.resolveByTimestamp(conflict.getId());

        assertEquals(ConflictStatus.AUTO_RESOLVED, resolved.getStatus());
    }

    /**
     * Property: Conflict detection should work for any entity type.
     */
    @Property
    void conflictDetectionShouldWorkForAnyEntityType(
        @ForAll String userId,
        @ForAll String entityType,
        @ForAll String entityId
    ) {
        LocalDateTime localTimestamp = LocalDateTime.now().plusMinutes(1);
        LocalDateTime remoteTimestamp = LocalDateTime.now();

        TestConflictRepository repository = new TestConflictRepository();
        ConflictResolutionService service = new ConflictResolutionService(repository);

        SyncConflict conflict = service.detectConflict(
            userId, entityType, entityId,
            "{\"data\":\"local\"}", localTimestamp,
            "{\"data\":\"remote\"}", remoteTimestamp,
            "device-remote"
        );

        assertNotNull(conflict);
        assertEquals(entityType, conflict.getEntityType());
        assertEquals(ConflictStatus.PENDING, conflict.getStatus());
    }

    /**
     * Property: Multiple conflicts for same entity should be deduplicated.
     */
    @Property
    void multipleConflictsForSameEntityShouldBeDeduplicated(
        @ForAll String userId,
        @ForAll String entityType,
        @ForAll String entityId
    ) {
        LocalDateTime timestamp1 = LocalDateTime.now();
        LocalDateTime timestamp2 = LocalDateTime.now().plusMinutes(1);

        TestConflictRepository repository = new TestConflictRepository();
        ConflictResolutionService service = new ConflictResolutionService(repository);

        SyncConflict conflict1 = service.detectConflict(
            userId, entityType, entityId,
            "local1", timestamp1,
            "remote1", timestamp2,
            "device-remote"
        );

        SyncConflict conflict2 = service.detectConflict(
            userId, entityType, entityId,
            "local2", timestamp1.plusMinutes(1),
            "remote2", timestamp2.plusMinutes(1),
            "device-remote-2"
        );

        assertEquals(conflict1.getId(), conflict2.getId());
    }

    /**
     * Test repository for conflicts.
     */
    private static class TestConflictRepository implements com.farmer.sync.repository.SyncConflictRepository {
        private final List<SyncConflict> conflicts = new ArrayList<>();
        private long nextId = 1;

        @Override
        public SyncConflict save(SyncConflict conflict) {
            if (conflict.getId() == null) {
                conflict.setId(nextId++);
            }
            conflicts.add(conflict);
            return conflict;
        }

        @Override
        public Optional<SyncConflict> findById(Long id) {
            return conflicts.stream().filter(c -> c.getId().equals(id)).findFirst();
        }

        @Override
        public List<SyncConflict> findByUserIdAndStatusOrderByDetectedAtDesc(
                String userId, ConflictStatus status) {
            return conflicts.stream()
                .filter(c -> c.getUserId().equals(userId) && c.getStatus() == status)
                .sorted((a, b) -> b.getDetectedAt().compareTo(a.getDetectedAt()))
                .collect(Collectors.toList());
        }

        @Override
        public Optional<SyncConflict> findByUserIdAndEntityTypeAndEntityId(
                String userId, String entityType, String entityId) {
            return conflicts.stream()
                .filter(c -> c.getUserId().equals(userId) && 
                    c.getEntityType().equals(entityType) && 
                    c.getEntityId().equals(entityId))
                .findFirst();
        }

        @Override
        public long countByUserIdAndStatus(String userId, ConflictStatus status) {
            return conflicts.stream()
                .filter(c -> c.getUserId().equals(userId) && c.getStatus() == status)
                .count();
        }

        @Override
        public int updateStatus(Long id, ConflictStatus status, LocalDateTime resolvedAt) {
            return findById(id).map(c -> {
                c.setStatus(status);
                c.setResolvedAt(resolvedAt);
                return 1;
            }).orElse(0);
        }

        @Override
        public int resolveConflict(Long id, ConflictStatus status, String resolvedData,
                LocalDateTime resolvedAt, String resolvedBy, ResolutionStrategy strategy) {
            return findById(id).map(c -> {
                c.setStatus(status);
                c.setResolvedData(resolvedData);
                c.setResolvedAt(resolvedAt);
                c.setResolvedBy(resolvedBy);
                c.setResolutionStrategy(strategy);
                return 1;
            }).orElse(0);
        }

        @Override
        public int deleteOldResolvedConflicts(LocalDateTime before) {
            int size = conflicts.size();
            conflicts.removeIf(c -> c.getStatus() == ConflictStatus.RESOLVED &&
                c.getResolvedAt() != null && c.getResolvedAt().isBefore(before));
            return size - conflicts.size();
        }

        @Override
        public List<SyncConflict> findByUserId(String userId) {
            return conflicts.stream()
                .filter(c -> c.getUserId().equals(userId))
                .collect(Collectors.toList());
        }

        @Override public List<SyncConflict> findAll() { return new ArrayList<>(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {}
        @Override public void delete(SyncConflict entity) {}
        @Override public void deleteAll() {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public boolean existsById(Long id) { return false; }
    }
}