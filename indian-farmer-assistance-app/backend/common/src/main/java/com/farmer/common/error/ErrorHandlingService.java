package com.farmer.common.error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for error handling and recovery.
 * Implements user-friendly error messages, automatic retry, crash recovery, and error reporting.
 * Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6
 */
public class ErrorHandlingService {

    public static class ErrorLog {
        public String errorId;
        public String errorMessage;
        public String userFriendlyMessage;
        public String language;
        public long timestamp;
        public String stackTrace;
        public int retryCount;
        public String status; // PENDING, RETRYING, RESOLVED, FAILED

        public ErrorLog(String errorMessage, String userFriendlyMessage, String language) {
            this.errorId = "ERR_" + System.currentTimeMillis();
            this.errorMessage = errorMessage;
            this.userFriendlyMessage = userFriendlyMessage;
            this.language = language;
            this.timestamp = System.currentTimeMillis();
            this.retryCount = 0;
            this.status = "PENDING";
        }
    }

    private final List<ErrorLog> errorLogs = new ArrayList<>();
    private final Map<String, Integer> retryAttempts = new HashMap<>();
    private static final int MAX_RETRIES = 3;
    private static final long[] BACKOFF_DELAYS = {1000, 2000, 4000}; // 1s, 2s, 4s

    /**
     * Log an error with user-friendly message
     */
    public ErrorLog logError(String errorMessage, String userFriendlyMessage, String language) {
        ErrorLog log = new ErrorLog(errorMessage, userFriendlyMessage, language);
        errorLogs.add(log);
        return log;
    }

    /**
     * Log error with stack trace
     */
    public ErrorLog logErrorWithStackTrace(String errorMessage, String userFriendlyMessage, String language, String stackTrace) {
        ErrorLog log = logError(errorMessage, userFriendlyMessage, language);
        log.stackTrace = stackTrace;
        return log;
    }

    /**
     * Get user-friendly error message
     */
    public String getUserFriendlyMessage(String errorId) {
        for (ErrorLog log : errorLogs) {
            if (log.errorId.equals(errorId)) {
                return log.userFriendlyMessage;
            }
        }
        return "An error occurred. Please try again.";
    }

    /**
     * Check if error should be retried
     */
    public boolean shouldRetry(String errorId) {
        Integer attempts = retryAttempts.getOrDefault(errorId, 0);
        return attempts < MAX_RETRIES;
    }

    /**
     * Get next retry delay in milliseconds
     */
    public long getNextRetryDelay(String errorId) {
        Integer attempts = retryAttempts.getOrDefault(errorId, 0);
        if (attempts >= MAX_RETRIES) {
            return -1; // No more retries
        }
        return BACKOFF_DELAYS[attempts];
    }

    /**
     * Record retry attempt
     */
    public void recordRetryAttempt(String errorId) {
        Integer attempts = retryAttempts.getOrDefault(errorId, 0);
        retryAttempts.put(errorId, attempts + 1);

        for (ErrorLog log : errorLogs) {
            if (log.errorId.equals(errorId)) {
                log.retryCount = attempts + 1;
                log.status = "RETRYING";
                return;
            }
        }
    }

    /**
     * Mark error as resolved
     */
    public void markResolved(String errorId) {
        for (ErrorLog log : errorLogs) {
            if (log.errorId.equals(errorId)) {
                log.status = "RESOLVED";
                return;
            }
        }
    }

    /**
     * Mark error as failed (max retries exceeded)
     */
    public void markFailed(String errorId) {
        for (ErrorLog log : errorLogs) {
            if (log.errorId.equals(errorId)) {
                log.status = "FAILED";
                return;
            }
        }
    }

    /**
     * Get all error logs
     */
    public List<ErrorLog> getAllErrorLogs() {
        return new ArrayList<>(errorLogs);
    }

    /**
     * Get error logs for a specific language
     */
    public List<ErrorLog> getErrorLogsByLanguage(String language) {
        List<ErrorLog> logs = new ArrayList<>();
        for (ErrorLog log : errorLogs) {
            if (language.equals(log.language)) {
                logs.add(log);
            }
        }
        return logs;
    }

    /**
     * Get recent errors (last N)
     */
    public List<ErrorLog> getRecentErrors(int count) {
        int startIndex = Math.max(0, errorLogs.size() - count);
        return new ArrayList<>(errorLogs.subList(startIndex, errorLogs.size()));
    }

    /**
     * Clear old error logs (older than specified milliseconds)
     */
    public void clearOldErrorLogs(long olderThanMs) {
        long cutoffTime = System.currentTimeMillis() - olderThanMs;
        errorLogs.removeIf(log -> log.timestamp < cutoffTime);
    }

    /**
     * Get error statistics
     */
    public Map<String, Integer> getErrorStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        for (ErrorLog log : errorLogs) {
            stats.put(log.status, stats.getOrDefault(log.status, 0) + 1);
        }
        return stats;
    }
}
