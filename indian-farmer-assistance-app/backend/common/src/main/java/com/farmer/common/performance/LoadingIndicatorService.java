package com.farmer.common.performance;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing loading indicators and request cancellation.
 * Allows cancellation for requests exceeding 5 seconds.
 * Requirements: 18.3
 */
public class LoadingIndicatorService {

    public static class LoadingOperation {
        public String operationId;
        public String operationName;
        public long startTime;
        public boolean cancelled;
        public String status; // LOADING, COMPLETED, CANCELLED, FAILED

        public LoadingOperation(String operationId, String operationName) {
            this.operationId = operationId;
            this.operationName = operationName;
            this.startTime = System.currentTimeMillis();
            this.cancelled = false;
            this.status = "LOADING";
        }

        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }

        public boolean isExceededTimeout() {
            return getElapsedTime() > 5000; // 5 seconds
        }
    }

    private final Map<String, LoadingOperation> activeOperations = new HashMap<>();
    private static final long TIMEOUT_THRESHOLD = 5000; // 5 seconds

    /**
     * Start a loading operation
     */
    public LoadingOperation startLoading(String operationId, String operationName) {
        LoadingOperation operation = new LoadingOperation(operationId, operationName);
        activeOperations.put(operationId, operation);
        return operation;
    }

    /**
     * Complete a loading operation
     */
    public void completeLoading(String operationId) {
        LoadingOperation operation = activeOperations.get(operationId);
        if (operation != null) {
            operation.status = "COMPLETED";
        }
    }

    /**
     * Cancel a loading operation
     */
    public void cancelLoading(String operationId) {
        LoadingOperation operation = activeOperations.get(operationId);
        if (operation != null) {
            operation.cancelled = true;
            operation.status = "CANCELLED";
        }
    }

    /**
     * Mark operation as failed
     */
    public void failLoading(String operationId) {
        LoadingOperation operation = activeOperations.get(operationId);
        if (operation != null) {
            operation.status = "FAILED";
        }
    }

    /**
     * Check if operation should be cancelled (exceeded timeout)
     */
    public boolean shouldCancelOperation(String operationId) {
        LoadingOperation operation = activeOperations.get(operationId);
        if (operation == null) {
            return false;
        }
        return operation.isExceededTimeout();
    }

    /**
     * Get operation status
     */
    public LoadingOperation getOperation(String operationId) {
        return activeOperations.get(operationId);
    }

    /**
     * Check if operation is still loading
     */
    public boolean isLoading(String operationId) {
        LoadingOperation operation = activeOperations.get(operationId);
        return operation != null && "LOADING".equals(operation.status);
    }

    /**
     * Get all active operations
     */
    public Map<String, LoadingOperation> getActiveOperations() {
        return new HashMap<>(activeOperations);
    }

    /**
     * Clean up completed operations
     */
    public void cleanupCompletedOperations() {
        activeOperations.entrySet().removeIf(entry -> 
            !"LOADING".equals(entry.getValue().status)
        );
    }

    /**
     * Get count of operations exceeding timeout
     */
    public int getTimeoutOperationCount() {
        return (int) activeOperations.values().stream()
            .filter(LoadingOperation::isExceededTimeout)
            .count();
    }
}
