package com.farmer.common.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for performance optimization service.
 * Tests launch time, navigation, image processing, and memory management.
 * Requirements: 18.1, 18.2, 18.3, 18.4, 18.5, 18.6
 */
public class PerformanceOptimizationServiceTest {

    private PerformanceOptimizationService performanceService;

    @BeforeEach
    public void setUp() {
        performanceService = new PerformanceOptimizationService();
    }

    @Test
    public void testStartAndCompleteOperation() {
        PerformanceOptimizationService.PerformanceMetrics metric = performanceService.startOperation("test_operation");
        assertNotNull(metric, "Metric should be created");
        assertEquals("IN_PROGRESS", metric.status, "Initial status should be IN_PROGRESS");

        performanceService.completeOperation(metric);
        assertEquals("SUCCESS", metric.status, "Status should be SUCCESS after completion");
        assertTrue(metric.duration >= 0, "Duration should be recorded");
    }

    @Test
    public void testLaunchTimeOptimal() {
        assertTrue(performanceService.isLaunchTimeOptimal(2500), "2.5 seconds should be optimal");
        assertTrue(performanceService.isLaunchTimeOptimal(3000), "3 seconds should be optimal");
        assertFalse(performanceService.isLaunchTimeOptimal(3500), "3.5 seconds should not be optimal");
    }

    @Test
    public void testNavigationTimeOptimal() {
        assertTrue(performanceService.isNavigationTimeOptimal(300), "300ms should be optimal");
        assertTrue(performanceService.isNavigationTimeOptimal(500), "500ms should be optimal");
        assertFalse(performanceService.isNavigationTimeOptimal(600), "600ms should not be optimal");
    }

    @Test
    public void testImageProcessingTimeOptimal() {
        assertTrue(performanceService.isImageProcessingOptimal(5000), "5 seconds should be optimal");
        assertTrue(performanceService.isImageProcessingOptimal(10000), "10 seconds should be optimal");
        assertFalse(performanceService.isImageProcessingOptimal(12000), "12 seconds should not be optimal");
    }

    @Test
    public void testOperationTimeout() {
        PerformanceOptimizationService.PerformanceMetrics metric = performanceService.startOperation("slow_operation");
        metric.duration = 6000; // 6 seconds
        assertTrue(performanceService.isOperationTimeout(metric), "6 seconds should exceed 5 second timeout");

        PerformanceOptimizationService.PerformanceMetrics fastMetric = performanceService.startOperation("fast_operation");
        fastMetric.duration = 3000; // 3 seconds
        assertFalse(performanceService.isOperationTimeout(fastMetric), "3 seconds should not exceed timeout");
    }

    @Test
    public void testMemoryUsage() {
        long memoryUsage = performanceService.getMemoryUsageMB();
        assertTrue(memoryUsage > 0, "Memory usage should be positive");
    }

    @Test
    public void testMemoryCritical() {
        boolean isCritical = performanceService.isMemoryCritical();
        // Just verify it returns a boolean without throwing exception
        assertNotNull(isCritical, "Memory critical check should return a value");
    }

    @Test
    public void testClearCachesOnLowMemory() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            performanceService.clearCachesOnLowMemory();
        }, "Clear caches should not throw exception");
    }

    @Test
    public void testGetAverageOperationDuration() {
        PerformanceOptimizationService.PerformanceMetrics metric1 = performanceService.startOperation("test_op");
        metric1.duration = 100;
        PerformanceOptimizationService.PerformanceMetrics metric2 = performanceService.startOperation("test_op");
        metric2.duration = 200;

        long average = performanceService.getAverageOperationDuration("test_op");
        assertTrue(average > 0, "Average should be calculated");
    }

    @Test
    public void testGet95thPercentileDuration() {
        for (int i = 0; i < 100; i++) {
            PerformanceOptimizationService.PerformanceMetrics metric = performanceService.startOperation("percentile_test");
            metric.duration = i * 10;
        }

        long percentile95 = performanceService.get95thPercentileDuration("percentile_test");
        assertTrue(percentile95 > 0, "95th percentile should be calculated");
    }

    @Test
    public void testClearOldMetrics() {
        PerformanceOptimizationService.PerformanceMetrics metric = performanceService.startOperation("old_operation");
        metric.endTime = System.currentTimeMillis() - 100000; // 100 seconds ago

        performanceService.clearOldMetrics(60000); // Clear metrics older than 60 seconds

        // Verify old metrics are cleared
        assertTrue(performanceService.getAllMetrics().isEmpty() || 
                   performanceService.getAllMetrics().values().stream()
                       .noneMatch(m -> m.operationName.equals("old_operation")),
                   "Old metrics should be cleared");
    }

    @Test
    public void testMultipleOperations() {
        PerformanceOptimizationService.PerformanceMetrics metric1 = performanceService.startOperation("operation_1");
        PerformanceOptimizationService.PerformanceMetrics metric2 = performanceService.startOperation("operation_2");
        PerformanceOptimizationService.PerformanceMetrics metric3 = performanceService.startOperation("operation_3");

        performanceService.completeOperation(metric1);
        performanceService.completeOperation(metric2);
        performanceService.completeOperation(metric3);

        assertEquals("SUCCESS", metric1.status, "Operation 1 should be successful");
        assertEquals("SUCCESS", metric2.status, "Operation 2 should be successful");
        assertEquals("SUCCESS", metric3.status, "Operation 3 should be successful");
    }

    @Test
    public void testPerformanceMetricsTimeout() {
        PerformanceOptimizationService.PerformanceMetrics metric = performanceService.startOperation("timeout_test");
        metric.timeout();
        assertEquals("TIMEOUT", metric.status, "Status should be TIMEOUT");
    }

    @Test
    public void testPerformanceMetricsCancel() {
        PerformanceOptimizationService.PerformanceMetrics metric = performanceService.startOperation("cancel_test");
        metric.cancel();
        assertEquals("CANCELLED", metric.status, "Status should be CANCELLED");
    }
}
