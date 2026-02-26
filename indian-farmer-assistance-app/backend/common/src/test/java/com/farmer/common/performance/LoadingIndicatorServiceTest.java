package com.farmer.common.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for loading indicator service.
 * Tests loading indicators and request cancellation.
 * Requirements: 18.3
 */
public class LoadingIndicatorServiceTest {

    private LoadingIndicatorService loadingService;

    @BeforeEach
    public void setUp() {
        loadingService = new LoadingIndicatorService();
    }

    @Test
    public void testStartLoading() {
        LoadingIndicatorService.LoadingOperation operation = loadingService.startLoading("op_1", "Test Operation");
        assertNotNull(operation, "Loading operation should be created");
        assertEquals("op_1", operation.operationId, "Operation ID should match");
        assertEquals("Test Operation", operation.operationName, "Operation name should match");
        assertEquals("LOADING", operation.status, "Initial status should be LOADING");
        assertFalse(operation.cancelled, "Should not be cancelled initially");
    }

    @Test
    public void testCompleteLoading() {
        LoadingIndicatorService.LoadingOperation operation = loadingService.startLoading("op_1", "Test Operation");
        loadingService.completeLoading("op_1");

        assertEquals("COMPLETED", operation.status, "Status should be COMPLETED");
    }

    @Test
    public void testCancelLoading() {
        LoadingIndicatorService.LoadingOperation operation = loadingService.startLoading("op_1", "Test Operation");
        loadingService.cancelLoading("op_1");

        assertTrue(operation.cancelled, "Operation should be marked as cancelled");
        assertEquals("CANCELLED", operation.status, "Status should be CANCELLED");
    }

    @Test
    public void testFailLoading() {
        LoadingIndicatorService.LoadingOperation operation = loadingService.startLoading("op_1", "Test Operation");
        loadingService.failLoading("op_1");

        assertEquals("FAILED", operation.status, "Status should be FAILED");
    }

    @Test
    public void testGetElapsedTime() throws InterruptedException {
        LoadingIndicatorService.LoadingOperation operation = loadingService.startLoading("op_1", "Test Operation");
        Thread.sleep(100);
        long elapsedTime = operation.getElapsedTime();

        assertTrue(elapsedTime >= 100, "Elapsed time should be at least 100ms");
    }

    @Test
    public void testIsExceededTimeout() {
        LoadingIndicatorService.LoadingOperation operation = loadingService.startLoading("op_1", "Test Operation");
        operation.startTime = System.currentTimeMillis() - 6000; // 6 seconds ago

        assertTrue(operation.isExceededTimeout(), "Operation should exceed 5 second timeout");
    }

    @Test
    public void testShouldCancelOperation() {
        LoadingIndicatorService.LoadingOperation operation = loadingService.startLoading("op_1", "Test Operation");
        operation.startTime = System.currentTimeMillis() - 6000; // 6 seconds ago

        assertTrue(loadingService.shouldCancelOperation("op_1"), "Operation should be cancelled");
    }

    @Test
    public void testGetOperation() {
        LoadingIndicatorService.LoadingOperation operation = loadingService.startLoading("op_1", "Test Operation");
        LoadingIndicatorService.LoadingOperation retrieved = loadingService.getOperation("op_1");

        assertNotNull(retrieved, "Operation should be retrievable");
        assertEquals(operation.operationId, retrieved.operationId, "Operation ID should match");
    }

    @Test
    public void testIsLoading() {
        loadingService.startLoading("op_1", "Test Operation");
        assertTrue(loadingService.isLoading("op_1"), "Operation should be loading");

        loadingService.completeLoading("op_1");
        assertFalse(loadingService.isLoading("op_1"), "Operation should not be loading after completion");
    }

    @Test
    public void testGetActiveOperations() {
        loadingService.startLoading("op_1", "Operation 1");
        loadingService.startLoading("op_2", "Operation 2");
        loadingService.startLoading("op_3", "Operation 3");

        assertEquals(3, loadingService.getActiveOperations().size(), "Should have 3 active operations");
    }

    @Test
    public void testCleanupCompletedOperations() {
        loadingService.startLoading("op_1", "Operation 1");
        loadingService.startLoading("op_2", "Operation 2");
        loadingService.startLoading("op_3", "Operation 3");

        loadingService.completeLoading("op_1");
        loadingService.completeLoading("op_3");

        loadingService.cleanupCompletedOperations();

        assertEquals(1, loadingService.getActiveOperations().size(), "Should have 1 active operation after cleanup");
        assertTrue(loadingService.isLoading("op_2"), "Operation 2 should still be loading");
    }

    @Test
    public void testGetTimeoutOperationCount() {
        LoadingIndicatorService.LoadingOperation op1 = loadingService.startLoading("op_1", "Operation 1");
        LoadingIndicatorService.LoadingOperation op2 = loadingService.startLoading("op_2", "Operation 2");
        LoadingIndicatorService.LoadingOperation op3 = loadingService.startLoading("op_3", "Operation 3");

        op1.startTime = System.currentTimeMillis() - 6000; // 6 seconds ago
        op3.startTime = System.currentTimeMillis() - 6000; // 6 seconds ago

        assertEquals(2, loadingService.getTimeoutOperationCount(), "Should have 2 timeout operations");
    }

    @Test
    public void testMultipleOperations() {
        loadingService.startLoading("op_1", "Operation 1");
        loadingService.startLoading("op_2", "Operation 2");
        loadingService.startLoading("op_3", "Operation 3");

        loadingService.completeLoading("op_1");
        loadingService.cancelLoading("op_2");
        loadingService.failLoading("op_3");

        assertEquals("COMPLETED", loadingService.getOperation("op_1").status, "Operation 1 should be completed");
        assertEquals("CANCELLED", loadingService.getOperation("op_2").status, "Operation 2 should be cancelled");
        assertEquals("FAILED", loadingService.getOperation("op_3").status, "Operation 3 should be failed");
    }

    @Test
    public void testCancelNonExistentOperation() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            loadingService.cancelLoading("non_existent_op");
        }, "Cancelling non-existent operation should not throw exception");
    }

    @Test
    public void testCompleteNonExistentOperation() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            loadingService.completeLoading("non_existent_op");
        }, "Completing non-existent operation should not throw exception");
    }
}
