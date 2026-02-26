package com.farmer.bandwidth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataUsageMonitoringService.
 * Validates: Requirements 13.4, 13.5
 */
class DataUsageMonitoringServiceTest {

    private DataUsageMonitoringService service;

    @BeforeEach
    void setUp() {
        service = new DataUsageMonitoringService();
        ReflectionTestUtils.setField(service, "defaultDataLimitMb", 100L);
        ReflectionTestUtils.setField(service, "warningThresholdPercent", 80);
    }

    @Test
    void testCreateTrackerWithDefaultLimit() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker();
        assertEquals(100 * 1024 * 1024, tracker.getLimitBytes());
        assertEquals(0, tracker.getTotalUsedBytes());
    }

    @Test
    void testCreateTrackerWithCustomLimit() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(50);
        assertEquals(50 * 1024 * 1024, tracker.getLimitBytes());
    }

    @Test
    void testAddUsage() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(100);
        tracker.addUsage(10 * 1024 * 1024); // 10MB
        assertEquals(10 * 1024 * 1024, tracker.getTotalUsedBytes());
    }

    @Test
    void testGetUsagePercentage() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(100);
        tracker.addUsage(50 * 1024 * 1024); // 50MB
        assertEquals(50.0, tracker.getUsagePercentage());
    }

    @Test
    void testGetRemainingBytes() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(100);
        tracker.addUsage(30 * 1024 * 1024); // 30MB
        assertEquals(70 * 1024 * 1024, tracker.getRemainingBytes());
    }

    @Test
    void testHasExceededLimit() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(100);
        tracker.addUsage(101 * 1024 * 1024); // 101MB
        assertTrue(tracker.hasExceededLimit());
    }

    @Test
    void testHasNotExceededLimit() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(100);
        tracker.addUsage(50 * 1024 * 1024); // 50MB
        assertFalse(tracker.hasExceededLimit());
    }

    @Test
    void testIsNearLimit() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(100);
        tracker.addUsage(85 * 1024 * 1024); // 85MB (85%)
        assertTrue(tracker.isNearLimit(80));
    }

    @Test
    void testIsNotNearLimit() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(100);
        tracker.addUsage(70 * 1024 * 1024); // 70MB
        assertFalse(tracker.isNearLimit(80));
    }

    @Test
    void testResetTracker() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(100);
        tracker.addUsage(50 * 1024 * 1024);
        tracker.reset();
        assertEquals(0, tracker.getTotalUsedBytes());
    }

    @Test
    void testGetDataSavingSuggestionsExceeded() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(100);
        tracker.addUsage(101 * 1024 * 1024);
        String[] suggestions = service.getDataSavingSuggestions(tracker);
        assertTrue(suggestions.length > 0);
        assertTrue(suggestions[0].contains("exceeded"));
    }

    @Test
    void testGetDataSavingSuggestionsNearLimit() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(100);
        tracker.addUsage(85 * 1024 * 1024);
        String[] suggestions = service.getDataSavingSuggestions(tracker);
        assertTrue(suggestions.length > 0);
        assertTrue(suggestions[0].contains("near limit"));
    }

    @Test
    void testGetDataSavingSuggestionsNormal() {
        DataUsageMonitoringService.DataUsageTracker tracker = service.createTracker(100);
        tracker.addUsage(50 * 1024 * 1024);
        String[] suggestions = service.getDataSavingSuggestions(tracker);
        assertEquals(0, suggestions.length);
    }

    @Test
    void testGetDefaultDataLimitMb() {
        assertEquals(100L, service.getDefaultDataLimitMb());
    }

    @Test
    void testGetWarningThresholdPercent() {
        assertEquals(80, service.getWarningThresholdPercent());
    }
}
