package com.farmer.sync.service;

import com.farmer.sync.dto.*;
import com.farmer.sync.entity.SyncConflict;
import com.farmer.sync.entity.SyncConflict.ConflictStatus;
import com.farmer.sync.entity.SyncConflict.ResolutionStrategy;
import com.farmer.sync.entity.SyncQueueItem;
import com.farmer.sync.entity.SyncQueueItem.OperationType;
import com.farmer.sync.entity.SyncQueueItem.SyncStatus;
import com.farmer.sync.entity.SyncStatus.State;
import com.farmer.sync.repository.SyncConflictRepository;
import com.farmer.sync.repository.SyncQueueRepository;
import com.farmer.sync.repository.SyncStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for offline and sync functionality.
 * 
 * Validates: Requirements 12.1, 12.3, 12.4, 15.3, 15.4
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SyncServiceUnitTest {

    @Mock
    private SyncQueueRepository syncQueueRepository;

    @Mock
    private SyncConflictRepository syncConflictRepository;

    @Mock
    private SyncStatusRepository syncStatusRepository;

    private SyncQueueService syncQueueService;
    private ConflictResolutionService conflictResolutionService;
    private SyncStatusService syncStatusService;
    private RetryService retryService;

    @BeforeEach
    void setUp() {
        syncQueueService = new SyncQueueService(syncQueueRepository, syncStatusRepository);
        conflictResolutionService = new ConflictResolutionService(syncConflictRepository);
        syncStatusService = new SyncStatusService(syncStatusRepository, syncQueueRepository);
        retryService = new RetryService();
    }

    @Nested
    @DisplayName("Offline Mode Activation Tests")
    class OfflineModeActivationTests {

        @Test
        @DisplayName("Test entering offline mode")
        void testEnterOfflineMode() {
            String userId = "farmer123";

            when(syncStatusRepository.enterOfflineMode(eq(userId), any(LocalDateTime.class)))
                .thenReturn(1);

            syncStatusService.enterOfflineMode(userId);

            verify(syncStatusRepository).enterOfflineMode(eq(userId), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Test exiting offline mode")
        void testExitOfflineMode() {
            String userId = "farmer123";

            when(syncStatusRepository.exitOfflineMode(userId)).thenReturn(1);

            syncStatusService.exitOfflineMode(userId);

            verify(syncStatusRepository).exitOfflineMode(userId);
        }

        @Test
        @DisplayName("Test checking if user is offline")
        void testIsOffline() {
            String userId = "farmer123";

            com.farmer.sync.entity.SyncStatus status = com.farmer.sync.entity.SyncStatus.builder()
                .userId(userId)
                .isOffline(true)
                .build();

            when(syncStatusRepository.findByUserId(userId)).thenReturn(Optional.of(status));

            assertTrue(syncStatusService.isOffline(userId));
        }

        @Test
        @DisplayName("Test offline mode detection within 2 seconds")
        void testOfflineModeDetectionWithin2Seconds() {
            String userId = "farmer123";

            when(syncStatusRepository.enterOfflineMode(eq(userId), any(LocalDateTime.class)))
                .thenReturn(1);

            long startTime = System.currentTimeMillis();
            syncStatusService.enterOfflineMode(userId);
            long detectionTime = System.currentTimeMillis() - startTime;

            assertTrue(detectionTime < 2000, 
                "Offline mode detection should be within 2 seconds, was: " + detectionTime + "ms");
        }
    }

    @Nested
    @DisplayName("Request Queuing Tests")
    class RequestQueuingTests {

        @Test
        @DisplayName("Test queuing a sync request")
        void testQueueRequest() {
            String userId = "farmer123";
            SyncRequestDto request = SyncRequestDto.builder()
                .entityType("CROP")
                .operationType(OperationType.CREATE)
                .entityId("crop-123")
                .payload("{\"name\":\"Rice\"}")
                .clientTimestamp(LocalDateTime.now())
                .priority(0)
                .build();

            SyncQueueItem savedItem = SyncQueueItem.builder()
                .id(1L)
                .userId(userId)
                .entityType("CROP")
                .operationType(OperationType.CREATE)
                .entityId("crop-123")
                .payload("{\"name\":\"Rice\"}")
                .status(SyncStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

            when(syncQueueRepository.save(any(SyncQueueItem.class))).thenReturn(savedItem);
            when(syncQueueRepository.countByUserIdAndStatus(eq(userId), eq(SyncStatus.PENDING)))
                .thenReturn(1L);
            when(syncStatusRepository.updatePendingChanges(eq(userId), eq(1))).thenReturn(1);

            SyncResponseDto response = syncQueueService.queueRequest(userId, request);

            assertNotNull(response);
            assertEquals(1L, response.getQueueId());
            assertEquals(userId, response.getUserId());
            assertEquals("CROP", response.getEntityType());
            assertEquals("CREATE", response.getOperationType());
            assertEquals(SyncStatus.PENDING, response.getStatus());
        }

        @Test
        @DisplayName("Test getting pending items")
        void testGetPendingItems() {
            String userId = "farmer123";

            List<SyncQueueItem> items = Arrays.asList(
                createQueueItem(1L, userId, "CROP", SyncStatus.PENDING),
                createQueueItem(2L, userId, "FERTILIZER", SyncStatus.PENDING)
            );

            when(syncQueueRepository.findByUserIdAndStatusOrderByCreatedAtAsc(
                eq(userId), eq(SyncStatus.PENDING)))
                .thenReturn(items);

            List<SyncResponseDto> pending = syncQueueService.getPendingItems(userId);

            assertEquals(2, pending.size());
            verify(syncQueueRepository).findByUserIdAndStatusOrderByCreatedAtAsc(
                eq(userId), eq(SyncStatus.PENDING));
        }

        @Test
        @DisplayName("Test getting pending count")
        void testGetPendingCount() {
            String userId = "farmer123";

            when(syncQueueRepository.countByUserIdAndStatus(eq(userId), eq(SyncStatus.PENDING)))
                .thenReturn(5L);

            long count = syncQueueService.getPendingCount(userId);

            assertEquals(5L, count);
        }
    }

    @Nested
    @DisplayName("FIFO Processing Tests")
    class FifoProcessingTests {

        @Test
        @DisplayName("Test getting next batch in FIFO order")
        void testGetNextBatchInFifoOrder() {
            String userId = "farmer123";

            LocalDateTime time1 = LocalDateTime.now().minusSeconds(10);
            LocalDateTime time2 = LocalDateTime.now().minusSeconds(5);
            LocalDateTime time3 = LocalDateTime.now();

            List<SyncQueueItem> items = Arrays.asList(
                createQueueItem(1L, userId, "CROP", SyncStatus.PENDING, time1),
                createQueueItem(2L, userId, "FERTILIZER", SyncStatus.PENDING, time2),
                createQueueItem(3L, userId, "HARVEST", SyncStatus.PENDING, time3)
            );

            when(syncQueueRepository.findPendingByUserIdOrdered(
                eq(userId), eq(SyncStatus.PENDING)))
                .thenReturn(items);
            when(syncQueueRepository.save(any(SyncQueueItem.class))).thenAnswer(i -> i.getArgument(0));

            List<SyncQueueItem> batch = syncQueueService.getNextBatch(userId);

            assertEquals(3, batch.size());
            for (SyncQueueItem item : batch) {
                assertEquals(SyncStatus.IN_PROGRESS, item.getStatus());
            }
        }

        @Test
        @DisplayName("Test marking item as completed")
        void testMarkCompleted() {
            Long queueItemId = 1L;

            when(syncQueueRepository.updateStatus(
                eq(queueItemId), eq(SyncStatus.COMPLETED), any(LocalDateTime.class)))
                .thenReturn(1);

            syncQueueService.markCompleted(queueItemId);

            verify(syncQueueRepository).updateStatus(
                eq(queueItemId), eq(SyncStatus.COMPLETED), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Test clearing completed items")
        void testClearCompletedItems() {
            String userId = "farmer123";

            List<SyncQueueItem> completed = Arrays.asList(
                createQueueItem(1L, userId, "CROP", SyncStatus.COMPLETED),
                createQueueItem(2L, userId, "FERTILIZER", SyncStatus.COMPLETED)
            );

            when(syncQueueRepository.findByUserIdAndStatus(
                eq(userId), eq(SyncStatus.COMPLETED)))
                .thenReturn(completed);

            int cleared = syncQueueService.clearCompletedItems(userId);

            assertEquals(2, cleared);
            verify(syncQueueRepository).deleteAll(completed);
        }
    }

    @Nested
    @DisplayName("Conflict Resolution Tests")
    class ConflictResolutionTests {

        @Test
        @DisplayName("Test detecting conflict")
        void testDetectConflict() {
            String userId = "farmer123";
            String entityType = "CROP";
            String entityId = "crop-123";
            String localData = "{\"name\":\"Rice\"}";
            String remoteData = "{\"name\":\"Wheat\"}";
            LocalDateTime localTimestamp = LocalDateTime.now().plusMinutes(5);
            LocalDateTime remoteTimestamp = LocalDateTime.now();

            SyncConflict conflict = SyncConflict.builder()
                .id(1L)
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .localData(localData)
                .localTimestamp(localTimestamp)
                .remoteData(remoteData)
                .remoteTimestamp(remoteTimestamp)
                .status(ConflictStatus.PENDING)
                .detectedAt(LocalDateTime.now())
                .build();

            when(syncConflictRepository.findByUserIdAndEntityTypeAndEntityId(
                eq(userId), eq(entityType), eq(entityId)))
                .thenReturn(Optional.empty());
            when(syncConflictRepository.save(any(SyncConflict.class))).thenReturn(conflict);

            SyncConflict detected = conflictResolutionService.detectConflict(
                userId, entityType, entityId,
                localData, localTimestamp,
                remoteData, remoteTimestamp,
                "device-remote"
            );

            assertNotNull(detected);
            assertEquals(1L, detected.getId());
            assertEquals(ConflictStatus.PENDING, detected.getStatus());
        }

        @Test
        @DisplayName("Test resolving conflict by timestamp - local wins")
        void testResolveConflictLocalWins() {
            Long conflictId = 1L;
            String localData = "{\"name\":\"Rice\"}";
            String remoteData = "{\"name\":\"Wheat\"}";

            SyncConflict conflict = SyncConflict.builder()
                .id(conflictId)
                .userId("farmer123")
                .entityType("CROP")
                .entityId("crop-123")
                .localData(localData)
                .localTimestamp(LocalDateTime.now().plusMinutes(5))
                .remoteData(remoteData)
                .remoteTimestamp(LocalDateTime.now())
                .status(ConflictStatus.PENDING)
                .build();

            when(syncConflictRepository.findById(conflictId)).thenReturn(Optional.of(conflict));
            when(syncConflictRepository.resolveConflict(
                eq(conflictId), eq(ConflictStatus.AUTO_RESOLVED), eq(localData),
                any(LocalDateTime.class), eq("SYSTEM"), eq(ResolutionStrategy.TIMESTAMP)))
                .thenReturn(1);
            when(syncConflictRepository.findById(conflictId)).thenReturn(Optional.of(conflict));

            SyncConflict resolved = conflictResolutionService.resolveByTimestamp(conflictId);

            assertNotNull(resolved);
            verify(syncConflictRepository).resolveConflict(
                eq(conflictId), eq(ConflictStatus.AUTO_RESOLVED), eq(localData),
                any(LocalDateTime.class), eq("SYSTEM"), eq(ResolutionStrategy.TIMESTAMP));
        }

        @Test
        @DisplayName("Test resolving conflict by timestamp - remote wins")
        void testResolveConflictRemoteWins() {
            Long conflictId = 1L;
            String localData = "{\"name\":\"Rice\"}";
            String remoteData = "{\"name\":\"Wheat\"}";

            SyncConflict conflict = SyncConflict.builder()
                .id(conflictId)
                .userId("farmer123")
                .entityType("CROP")
                .entityId("crop-123")
                .localData(localData)
                .localTimestamp(LocalDateTime.now())
                .remoteData(remoteData)
                .remoteTimestamp(LocalDateTime.now().plusMinutes(10))
                .status(ConflictStatus.PENDING)
                .build();

            when(syncConflictRepository.findById(conflictId)).thenReturn(Optional.of(conflict));
            when(syncConflictRepository.resolveConflict(
                eq(conflictId), eq(ConflictStatus.AUTO_RESOLVED), eq(remoteData),
                any(LocalDateTime.class), eq("SYSTEM"), eq(ResolutionStrategy.TIMESTAMP)))
                .thenReturn(1);
            when(syncConflictRepository.findById(conflictId)).thenReturn(Optional.of(conflict));

            SyncConflict resolved = conflictResolutionService.resolveByTimestamp(conflictId);

            assertNotNull(resolved);
            verify(syncConflictRepository).resolveConflict(
                eq(conflictId), eq(ConflictStatus.AUTO_RESOLVED), eq(remoteData),
                any(LocalDateTime.class), eq("SYSTEM"), eq(ResolutionStrategy.TIMESTAMP));
        }

        @Test
        @DisplayName("Test getting pending conflicts")
        void testGetPendingConflicts() {
            String userId = "farmer123";

            List<SyncConflict> conflicts = Arrays.asList(
                SyncConflict.builder().id(1L).userId(userId).entityType("CROP")
                    .entityId("crop-1").status(ConflictStatus.PENDING).build(),
                SyncConflict.builder().id(2L).userId(userId).entityType("FERTILIZER")
                    .entityId("fert-1").status(ConflictStatus.PENDING).build()
            );

            when(syncConflictRepository.findByUserIdAndStatusOrderByDetectedAtDesc(
                eq(userId), eq(ConflictStatus.PENDING)))
                .thenReturn(conflicts);

            List<ConflictDto> pending = conflictResolutionService.getPendingConflicts(userId);

            assertEquals(2, pending.size());
        }
    }

    @Nested
    @DisplayName("Retry Logic Tests")
    class RetryLogicTests {

        @Test
        @DisplayName("Test retry with exponential backoff")
        void testRetryWithExponentialBackoff() {
            AtomicInteger attemptCount = new AtomicInteger(0);

            assertThrows(RuntimeException.class, () -> {
                retryService.executeWithRetry(() -> {
                    attemptCount.incrementAndGet();
                    throw new RuntimeException("Simulated failure");
                }, "test-operation");
            });

            assertEquals(3, attemptCount.get());
        }

        @Test
        @DisplayName("Test retry succeeds on third attempt")
        void testRetrySucceedsOnThirdAttempt() {
            AtomicInteger attemptCount = new AtomicInteger(0);

            String result = retryService.executeWithRetry(() -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    throw new RuntimeException("Failure " + attempt);
                }
                return "Success";
            }, "test-operation");

            assertEquals("Success", result);
            assertEquals(3, attemptCount.get());
        }

        @Test
        @DisplayName("Test no retry for successful operation")
        void testNoRetryForSuccessfulOperation() {
            AtomicInteger attemptCount = new AtomicInteger(0);

            String result = retryService.executeWithRetry(() -> {
                attemptCount.incrementAndGet();
                return "Success";
            }, "test-operation");

            assertEquals("Success", result);
            assertEquals(1, attemptCount.get());
        }

        @Test
        @DisplayName("Test retry delays follow exponential pattern")
        void testRetryDelaysFollowExponentialPattern() {
            long[] delays = retryService.getRetryDelays();

            assertEquals(2, delays.length);
            assertEquals(1000, delays[0]);
            assertEquals(2000, delays[1]);
        }

        @Test
        @DisplayName("Test total retry delay")
        void testTotalRetryDelay() {
            long totalDelay = retryService.getTotalRetryDelay();

            assertEquals(3000, totalDelay);
        }

        @Test
        @DisplayName("Test max attempts")
        void testMaxAttempts() {
            assertEquals(3, retryService.getMaxAttempts());
        }
    }

    @Nested
    @DisplayName("Sync Status Tests")
    class SyncStatusTests {

        @Test
        @DisplayName("Test getting sync status")
        void testGetSyncStatus() {
            String userId = "farmer123";

            com.farmer.sync.entity.SyncStatus status = com.farmer.sync.entity.SyncStatus.builder()
                .userId(userId)
                .syncState(State.IDLE)
                .pendingChanges(0)
                .isOffline(false)
                .build();

            when(syncStatusRepository.findByUserId(userId)).thenReturn(Optional.of(status));

            SyncStatusDto statusDto = syncStatusService.getStatusDto(userId);

            assertNotNull(statusDto);
            assertEquals(userId, statusDto.getUserId());
            assertEquals(State.IDLE, statusDto.getSyncState());
            assertFalse(statusDto.getIsOffline());
        }

        @Test
        @DisplayName("Test updating last sync time")
        void testUpdateLastSyncTime() {
            String userId = "farmer123";

            com.farmer.sync.entity.SyncStatus status = com.farmer.sync.entity.SyncStatus.builder()
                .userId(userId)
                .syncState(State.SYNCING)
                .pendingChanges(5)
                .build();

            when(syncStatusRepository.updateLastSyncAt(eq(userId), any(LocalDateTime.class)))
                .thenReturn(1);
            when(syncStatusRepository.findByUserId(userId)).thenReturn(Optional.of(status));
            when(syncStatusRepository.save(any(com.farmer.sync.entity.SyncStatus.class)))
                .thenReturn(status);

            syncStatusService.updateLastSyncTime(userId);

            verify(syncStatusRepository).updateLastSyncAt(eq(userId), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Test setting sync error")
        void testSetSyncError() {
            String userId = "farmer123";
            String error = "Network error";

            when(syncStatusRepository.setSyncError(userId, error)).thenReturn(1);

            syncStatusService.setSyncError(userId, error);

            verify(syncStatusRepository).setSyncError(userId, error);
        }

        @Test
        @DisplayName("Test updating device info")
        void testUpdateDeviceInfo() {
            String userId = "farmer123";
            String deviceId = "device-123";
            String appVersion = "1.0.0";

            when(syncStatusRepository.updateDeviceInfo(userId, deviceId, appVersion))
                .thenReturn(1);

            syncStatusService.updateDeviceInfo(userId, deviceId, appVersion);

            verify(syncStatusRepository).updateDeviceInfo(userId, deviceId, appVersion);
        }
    }

    private SyncQueueItem createQueueItem(Long id, String userId, String entityType, SyncStatus status) {
        return createQueueItem(id, userId, entityType, status, LocalDateTime.now());
    }

    private SyncQueueItem createQueueItem(Long id, String userId, String entityType, 
            SyncStatus status, LocalDateTime createdAt) {
        return SyncQueueItem.builder()
            .id(id)
            .userId(userId)
            .entityType(entityType)
            .operationType(OperationType.CREATE)
            .entityId("entity-" + id)
            .payload("{}")
            .status(status)
            .retryCount(0)
            .createdAt(createdAt)
            .clientTimestamp(createdAt)
            .build();
    }
}