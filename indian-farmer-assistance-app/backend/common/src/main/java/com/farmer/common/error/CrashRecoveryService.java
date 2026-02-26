package com.farmer.common.error;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for crash recovery and session restoration.
 * Logs crash details and restores previous session on restart.
 * Requirements: 19.3, 19.4
 */
public class CrashRecoveryService {

    public static class CrashLog {
        public String crashId;
        public String userId;
        public LocalDateTime crashTime;
        public String errorMessage;
        public String stackTrace;
        public Map<String, String> sessionData;
        public boolean recovered;

        public CrashLog(String userId, String errorMessage, String stackTrace) {
            this.crashId = "CRASH_" + UUID.randomUUID().toString();
            this.userId = userId;
            this.crashTime = LocalDateTime.now();
            this.errorMessage = errorMessage;
            this.stackTrace = stackTrace;
            this.sessionData = new HashMap<>();
            this.recovered = false;
        }
    }

    private final List<CrashLog> crashLogs = new ArrayList<>();
    private final Map<String, Map<String, String>> sessionSnapshots = new HashMap<>();

    /**
     * Log a crash
     */
    public CrashLog logCrash(String userId, String errorMessage, String stackTrace) {
        CrashLog log = new CrashLog(userId, errorMessage, stackTrace);
        crashLogs.add(log);
        return log;
    }

    /**
     * Save session snapshot for recovery
     */
    public void saveSessionSnapshot(String userId, Map<String, String> sessionData) {
        sessionSnapshots.put(userId, new HashMap<>(sessionData));
    }

    /**
     * Restore session from snapshot
     */
    public Map<String, String> restoreSession(String userId) {
        return sessionSnapshots.getOrDefault(userId, new HashMap<>());
    }

    /**
     * Mark crash as recovered
     */
    public void markCrashRecovered(String crashId) {
        for (CrashLog log : crashLogs) {
            if (log.crashId.equals(crashId)) {
                log.recovered = true;
                return;
            }
        }
    }

    /**
     * Get crash log by ID
     */
    public CrashLog getCrashLog(String crashId) {
        for (CrashLog log : crashLogs) {
            if (log.crashId.equals(crashId)) {
                return log;
            }
        }
        return null;
    }

    /**
     * Get all crash logs for a user
     */
    public List<CrashLog> getCrashLogsByUser(String userId) {
        List<CrashLog> logs = new ArrayList<>();
        for (CrashLog log : crashLogs) {
            if (userId.equals(log.userId)) {
                logs.add(log);
            }
        }
        return logs;
    }

    /**
     * Get unrecovered crashes
     */
    public List<CrashLog> getUnrecoveredCrashes() {
        List<CrashLog> logs = new ArrayList<>();
        for (CrashLog log : crashLogs) {
            if (!log.recovered) {
                logs.add(log);
            }
        }
        return logs;
    }

    /**
     * Get recent crashes
     */
    public List<CrashLog> getRecentCrashes(int count) {
        int startIndex = Math.max(0, crashLogs.size() - count);
        return new ArrayList<>(crashLogs.subList(startIndex, crashLogs.size()));
    }

    /**
     * Clear old crash logs
     */
    public void clearOldCrashLogs(long olderThanDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(olderThanDays);
        crashLogs.removeIf(log -> log.crashTime.isBefore(cutoffTime));
    }

    /**
     * Clear session snapshot
     */
    public void clearSessionSnapshot(String userId) {
        sessionSnapshots.remove(userId);
    }

    /**
     * Get all crash logs
     */
    public List<CrashLog> getAllCrashLogs() {
        return new ArrayList<>(crashLogs);
    }

    /**
     * Get crash statistics
     */
    public Map<String, Integer> getCrashStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_crashes", crashLogs.size());
        stats.put("recovered_crashes", (int) crashLogs.stream().filter(c -> c.recovered).count());
        stats.put("unrecovered_crashes", (int) crashLogs.stream().filter(c -> !c.recovered).count());
        return stats;
    }
}
