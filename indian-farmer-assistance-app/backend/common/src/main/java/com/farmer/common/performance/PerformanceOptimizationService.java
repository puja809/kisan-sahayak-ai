package com.farmer.common.performance;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for performance optimization and monitoring.
 * Targets: 3-second launch time on 2GB RAM, ≤500ms navigation, ≤10s image processing.
 * Requirements: 18.1, 18.2, 18.3, 18.4, 18.5, 18.6
 */
public class PerformanceOptimizationService {

    public static class PerformanceMetrics {
        public String operationName;
        public long startTime;
        public long endTime;
        public long duration;
        public String status; // SUCCESS, TIMEOUT, CANCELLED
        public long memoryUsedMB;

        public PerformanceMetrics(String operationName) {
            this.operationName = operationName;
            this.startTime = System.currentTimeMillis();
            this.status = "IN_PROGRESS";
        }

        public void complete() {
            this.endTime = System.currentTimeMillis();
            this.duration = endTime - startTime;
            this.status = "SUCCESS";
        }

        public void timeout() {
            this.endTime = System.currentTimeMillis();
            this.duration = endTime - startTime;
            this.status = "TIMEOUT";
        }

        public void cancel() {
            this.endTime = System.currentTimeMillis();
            this.duration = endTime - startTime;
            this.status = "CANCELLED";
        }
    }

    private final Map<String, PerformanceMetrics> metrics = new HashMap<>();
    private static final long LAUNCH_TIME_TARGET = 3000; // 3 seconds
    private static final long NAVIGATION_TIME_TARGET = 500; // 500ms
    private static final long IMAGE_PROCESSING_TARGET = 10000; // 10 seconds
    private static final long REQUEST_TIMEOUT = 5000; // 5 seconds

    /**
     * Start tracking performance for an operation
     */
    public PerformanceMetrics startOperation(String operationName) {
        PerformanceMetrics metric = new PerformanceMetrics(operationName);
        metrics.put(operationName + "_" + System.nanoTime(), metric);
        return metric;
    }

    /**
     * Complete an operation and record metrics
     */
    public void completeOperation(PerformanceMetrics metric) {
        metric.complete();
        metric.memoryUsedMB = getMemoryUsageMB();
    }

    /**
     * Check if operation exceeded timeout (5 seconds)
     */
    public boolean isOperationTimeout(PerformanceMetrics metric) {
        return metric.duration > REQUEST_TIMEOUT;
    }

    /**
     * Check if launch time meets target (3 seconds on 2GB RAM)
     */
    public boolean isLaunchTimeOptimal(long launchTimeMs) {
        return launchTimeMs <= LAUNCH_TIME_TARGET;
    }

    /**
     * Check if navigation time meets target (≤500ms)
     */
    public boolean isNavigationTimeOptimal(long navigationTimeMs) {
        return navigationTimeMs <= NAVIGATION_TIME_TARGET;
    }

    /**
     * Check if image processing meets target (≤10 seconds)
     */
    public boolean isImageProcessingOptimal(long processingTimeMs) {
        return processingTimeMs <= IMAGE_PROCESSING_TARGET;
    }

    /**
     * Get current memory usage in MB
     */
    public long getMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024 * 1024);
    }

    /**
     * Check if memory usage is critical (>80% of available)
     */
    public boolean isMemoryCritical() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return (usedMemory * 100 / maxMemory) > 80;
    }

    /**
     * Clear non-essential caches to free memory
     */
    public void clearCachesOnLowMemory() {
        if (isMemoryCritical()) {
            // Clear caches, remove non-essential data
            System.gc();
        }
    }

    /**
     * Get all recorded metrics
     */
    public Map<String, PerformanceMetrics> getAllMetrics() {
        return new HashMap<>(metrics);
    }

    /**
     * Get average operation duration
     */
    public long getAverageOperationDuration(String operationName) {
        return (long) metrics.values().stream()
            .filter(m -> m.operationName.equals(operationName))
            .mapToLong(m -> m.duration)
            .average()
            .orElse(0);
    }

    /**
     * Get 95th percentile operation duration
     */
    public long get95thPercentileDuration(String operationName) {
        long[] durations = metrics.values().stream()
            .filter(m -> m.operationName.equals(operationName))
            .mapToLong(m -> m.duration)
            .sorted()
            .toArray();

        if (durations.length == 0) {
            return 0;
        }

        int index = (int) Math.ceil(durations.length * 0.95) - 1;
        return durations[Math.max(0, (int) index)];
    }

    /**
     * Clear old metrics to prevent memory bloat
     */
    public void clearOldMetrics(long olderThanMs) {
        long cutoffTime = System.currentTimeMillis() - olderThanMs;
        metrics.entrySet().removeIf(entry -> entry.getValue().endTime < cutoffTime);
    }
}
