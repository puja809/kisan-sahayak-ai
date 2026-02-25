package com.farmer.sync.property;

import com.farmer.sync.dto.SyncRequestDto;
import com.farmer.sync.dto.SyncResponseDto;
import com.farmer.sync.entity.SyncQueueItem;
import com.farmer.sync.entity.SyncQueueItem.SyncStatus;
import com.farmer.sync.service.SyncQueueService;
import net.jqwik.api.*;
import net.jqwik.junit.platform.JqwikProperty;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for sync queue FIFO processing.
 * 
 * Feature: indian-farmer-assistance-app, Property 30: Sync Queue FIFO Processing
 * Validates: Requirements 12.4
 * 
 * For any set of queued requests created during offline mode, when connectivity
 * is restored, the system should process all queued requests in the order they
 * were created (FIFO), such that for any two requests R1 and R2 where R1 was
 * created before R2, R1 should be processed before R2.
 */
@JqwikProperty
class SyncQueueFifoProcessingPropertyTest {

    /**
     * Property: Requests should be processed in FIFO order based on creation time.
     */
    @Property
    void requestsShouldBeProcessedInFifoOrder(
        @ForAll String userId,
        @ForAll List<SyncRequestInput> requestInputs
    ) {
        if (requestInputs.isEmpty()) {
            return;
        }

        requestInputs.sort(Comparator.comparing(input -> input.timestamp));

        TestSyncQueueRepository queueRepository = new TestSyncQueueRepository();
        TestSyncStatusRepository statusRepository = new TestSyncStatusRepository();

        SyncQueueService service = new SyncQueueService(queueRepository, statusRepository);

        List<Long> queueIds = new ArrayList<>();
        for (SyncRequestInput input : requestInputs) {
            SyncRequestDto request = SyncRequestDto.builder()
                .entityType(input.entityType)
                .operationType(input.operationType)
                .entityId(input.entityId)
                .payload(input.payload)
                .clientTimestamp(input.timestamp)
                .priority(input.priority)
                .build();

            SyncResponseDto response = service.queueRequest(userId, request);
            queueIds.add(response.getQueueId());
        }

        List<SyncQueueItem> batch = service.getNextBatch(userId);

        if (batch.size() > 1) {
            for (int i = 1; i < batch.size(); i++) {
                assertTrue(
                    !batch.get(i).getCreatedAt().isBefore(batch.get(i - 1).getCreatedAt()),
                    "Batch items should be in FIFO order based on creation time"
                );
            }
        }
    }

    /**
     * Property: For any two requests R1 and R2 where R1 was created before R2,
     * R1 should be processed before R2.
     */
    @Property
    void earlierRequestsShouldBeProcessedBeforeLaterRequests(
        @ForAll String userId,
        @ForAll int requestCount
    ) {
        if (requestCount <= 0 || requestCount > 100) {
            requestCount = 10;
        }

        TestSyncQueueRepository queueRepository = new TestSyncQueueRepository();
        TestSyncStatusRepository statusRepository = new TestSyncStatusRepository();

        SyncQueueService service = new SyncQueueService(queueRepository, statusRepository);

        LocalDateTime baseTime = LocalDateTime.now();
        List<Long> queueIds = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            SyncRequestDto request = SyncRequestDto.builder()
                .entityType("CROP")
                .operationType(SyncQueueItem.OperationType.CREATE)
                .entityId(UUID.randomUUID().toString())
                .payload("{\"index\":" + i + "}")
                .clientTimestamp(baseTime.plusSeconds(i))
                .priority(0)
                .build();

            SyncResponseDto response = service.queueRequest(userId, request);
            queueIds.add(response.getQueueId());
        }

        List<SyncResponseDto> pending = service.getPendingItems(userId);

        for (int i = 1; i < pending.size(); i++) {
            LocalDateTime prevCreated = pending.get(i - 1).getCreatedAt();
            LocalDateTime currCreated = pending.get(i).getCreatedAt();

            assertTrue(
                !currCreated.isBefore(prevCreated),
                "Request created at " + currCreated + " should not be before " + prevCreated
            );
        }
    }

    /**
     * Property: Priority should not break FIFO order for same priority items.
     */
    @Property
    void samePriorityItemsShouldMaintainFifoOrder(
        @ForAll String userId,
        @ForAll int count
    ) {
        if (count <= 0 || count > 50) {
            count = 20;
        }

        TestSyncQueueRepository queueRepository = new TestSyncQueueRepository();
        TestSyncStatusRepository statusRepository = new TestSyncStatusRepository();

        SyncQueueService service = new SyncQueueService(queueRepository, statusRepository);

        LocalDateTime baseTime = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            SyncRequestDto request = SyncRequestDto.builder()
                .entityType("FERTILIZER")
                .operationType(SyncQueueItem.OperationType.CREATE)
                .entityId(UUID.randomUUID().toString())
                .payload("{\"item\":" + i + "}")
                .clientTimestamp(baseTime.plusSeconds(i))
                .priority(5)
                .build();

            service.queueRequest(userId, request);
        }

        List<SyncResponseDto> pending = service.getPendingItems(userId);

        for (int i = 1; i < pending.size(); i++) {
            assertTrue(
                !pending.get(i).getCreatedAt().isBefore(pending.get(i - 1).getCreatedAt()),
                "Same priority items should maintain FIFO order"
            );
        }
    }

    /**
     * Helper class for sync request input.
     */
    private static class SyncRequestInput {
        String entityType;
        SyncQueueItem.OperationType operationType;
        String entityId;
        String payload;
        LocalDateTime timestamp;
        int priority;
    }

    /**
     * Test repository for sync queue.
     */
    private static class TestSyncQueueRepository implements com.farmer.sync.repository.SyncQueueRepository {
        private final List<SyncQueueItem> items = new ArrayList<>();
        private long nextId = 1;

        @Override
        public SyncQueueItem save(SyncQueueItem item) {
            if (item.getId() == null) {
                item.setId(nextId++);
            }
            items.add(item);
            return item;
        }

        @Override
        public List<SyncQueueItem> findByUserIdAndStatusOrderByCreatedAtAsc(String userId, SyncStatus status) {
            return items.stream()
                .filter(i -> i.getUserId().equals(userId) && i.getStatus() == status)
                .sorted(Comparator.comparing(SyncQueueItem::getCreatedAt))
                .collect(Collectors.toList());
        }

        @Override
        public List<SyncQueueItem> findPendingByUserIdOrdered(String userId, SyncStatus status) {
            return items.stream()
                .filter(i -> i.getUserId().equals(userId) && i.getStatus() == status)
                .sorted(Comparator.comparing(SyncQueueItem::getPriority).reversed()
                    .thenComparing(SyncQueueItem::getCreatedAt))
                .collect(Collectors.toList());
        }

        @Override
        public long countByUserIdAndStatus(String userId, SyncStatus status) {
            return items.stream()
                .filter(i -> i.getUserId().equals(userId) && i.getStatus() == status)
                .count();
        }

        @Override
        public Optional<SyncQueueItem> findById(Long id) {
            return items.stream().filter(i -> i.getId().equals(id)).findFirst();
        }

        @Override
        public List<SyncQueueItem> findByUserIdAndStatus(String userId, SyncStatus status) {
            return items.stream()
                .filter(i -> i.getUserId().equals(userId) && i.getStatus() == status)
                .collect(Collectors.toList());
        }

        @Override
        public List<SyncQueueItem> findItemsForRetry(int maxRetries) {
            return items.stream()
                .filter(i -> i.getStatus() == SyncStatus.FAILED && i.getRetryCount() < maxRetries)
                .sorted(Comparator.comparing(SyncQueueItem::getProcessedAt))
                .collect(Collectors.toList());
        }

        @Override
        public int updateStatus(Long id, SyncStatus status, LocalDateTime processedAt) {
            return findById(id).map(item -> {
                item.setStatus(status);
                item.setProcessedAt(processedAt);
                return 1;
            }).orElse(0);
        }

        @Override
        public int updateStatusWithRetry(Long id, SyncStatus status, int retryCount, String error, LocalDateTime processedAt) {
            return findById(id).map(item -> {
                item.setStatus(status);
                item.setRetryCount(retryCount);
                item.setLastError(error);
                item.setProcessedAt(processedAt);
                return 1;
            }).orElse(0);
        }

        @Override
        public int deleteOldCompletedItems(LocalDateTime before) {
            int size = items.size();
            items.removeIf(i -> i.getStatus() == SyncStatus.COMPLETED && 
                i.getProcessedAt() != null && i.getProcessedAt().isBefore(before));
            return size - items.size();
        }

        @Override
        public List<SyncQueueItem> findByUserIdAndEntityTypeAndEntityId(String userId, String entityType, String entityId) {
            return items.stream()
                .filter(i -> i.getUserId().equals(userId) && 
                    i.getEntityType().equals(entityType) && 
                    i.getEntityId().equals(entityId))
                .collect(Collectors.toList());
        }

        @Override
        public int deleteByUserId(String userId) {
            int size = items.size();
            items.removeIf(i -> i.getUserId().equals(userId));
            return size - items.size();
        }

        @Override public List<SyncQueueItem> findAll() { return new ArrayList<>(); }
        @Override public Optional<SyncQueueItem> findById(Long id) { return Optional.empty(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {}
        @Override public void delete(SyncQueueItem entity) {}
        @Override public void deleteAll() {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public boolean existsById(Long id) { return false; }
    }

    /**
     * Test repository for sync status.
     */
    private static class TestSyncStatusRepository implements com.farmer.sync.repository.SyncStatusRepository {
        @Override
        public com.farmer.sync.entity.SyncStatus findByUserId(String userId) {
            return null;
        }

        @Override
        public int updateLastSyncAt(String userId, LocalDateTime syncAt) { return 0; }
        @Override
        public int updatePendingChanges(String userId, int count) { return 0; }
        @Override
        public int enterOfflineMode(String userId, LocalDateTime offlineSince) { return 0; }
        @Override
        public int exitOfflineMode(String userId) { return 0; }
        @Override
        public int updateSyncProgress(String userId, int syncing, int total, int percent) { return 0; }
        @Override
        public int setSyncError(String userId, String error) { return 0; }
        @Override
        public int updateDeviceInfo(String userId, String deviceId, String appVersion) { return 0; }

        @Override public List<com.farmer.sync.entity.SyncStatus> findAll() { return new ArrayList<>(); }
        @Override public Optional<com.farmer.sync.entity.SyncStatus> findById(Long id) { return Optional.empty(); }
        @Override public com.farmer.sync.entity.SyncStatus save(com.farmer.sync.entity.SyncStatus entity) { return entity; }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {}
        @Override public void delete(com.farmer.sync.entity.SyncStatus entity) {}
        @Override public void deleteAll() {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public boolean existsById(Long id) { return false; }
    }
}