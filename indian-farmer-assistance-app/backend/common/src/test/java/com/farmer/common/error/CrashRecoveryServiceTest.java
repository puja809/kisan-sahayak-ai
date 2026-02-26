package com.farmer.common.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for crash recovery service.
 * Tests crash logging, session restoration, and recovery tracking.
 * Requirements: 19.3, 19.4
 */
public class CrashRecoveryServiceTest {

    private CrashRecoveryService crashService;

    @BeforeEach
    public void setUp() {
        crashService = new CrashRecoveryService();
    }

    @Test
    public void testLogCrash() {
        String userId = "farmer_001";
        String errorMessage = "NullPointerException";
        String stackTrace = "java.lang.NullPointerException at com.farmer.service.UserService.getUser()";

        CrashRecoveryService.CrashLog log = crashService.logCrash(userId, errorMessage, stackTrace);

        assertNotNull(log, "Crash log should be created");
        assertEquals(userId, log.userId, "User ID should match");
        assertEquals(errorMessage, log.errorMessage, "Error message should match");
        assertEquals(stackTrace, log.stackTrace, "Stack trace should match");
        assertFalse(log.recovered, "Should not be recovered initially");
    }

    @Test
    public void testSaveSessionSnapshot() {
        String userId = "farmer_001";
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("crop_id", "crop_123");
        sessionData.put("farm_id", "farm_456");

        crashService.saveSessionSnapshot(userId, sessionData);

        Map<String, String> restored = crashService.restoreSession(userId);
        assertEquals("crop_123", restored.get("crop_id"), "Crop ID should be restored");
        assertEquals("farm_456", restored.get("farm_id"), "Farm ID should be restored");
    }

    @Test
    public void testRestoreSessionForNonExistentUser() {
        Map<String, String> restored = crashService.restoreSession("non_existent_user");
        assertTrue(restored.isEmpty(), "Should return empty map for non-existent user");
    }

    @Test
    public void testMarkCrashRecovered() {
        CrashRecoveryService.CrashLog log = crashService.logCrash("farmer_001", "Error", "Stack trace");
        assertFalse(log.recovered, "Should not be recovered initially");

        crashService.markCrashRecovered(log.crashId);
        assertTrue(log.recovered, "Should be marked as recovered");
    }

    @Test
    public void testGetCrashLog() {
        CrashRecoveryService.CrashLog log = crashService.logCrash("farmer_001", "Error", "Stack trace");
        CrashRecoveryService.CrashLog retrieved = crashService.getCrashLog(log.crashId);

        assertNotNull(retrieved, "Crash log should be retrievable");
        assertEquals(log.crashId, retrieved.crashId, "Crash ID should match");
    }

    @Test
    public void testGetCrashLogByNonExistentId() {
        CrashRecoveryService.CrashLog log = crashService.getCrashLog("non_existent_crash");
        assertNull(log, "Should return null for non-existent crash");
    }

    @Test
    public void testGetCrashLogsByUser() {
        crashService.logCrash("farmer_001", "Error 1", "Stack trace 1");
        crashService.logCrash("farmer_002", "Error 2", "Stack trace 2");
        crashService.logCrash("farmer_001", "Error 3", "Stack trace 3");

        List<CrashRecoveryService.CrashLog> logs = crashService.getCrashLogsByUser("farmer_001");
        assertEquals(2, logs.size(), "Should have 2 crash logs for farmer_001");
    }

    @Test
    public void testGetUnrecoveredCrashes() {
        CrashRecoveryService.CrashLog log1 = crashService.logCrash("farmer_001", "Error 1", "Stack trace 1");
        CrashRecoveryService.CrashLog log2 = crashService.logCrash("farmer_002", "Error 2", "Stack trace 2");
        CrashRecoveryService.CrashLog log3 = crashService.logCrash("farmer_003", "Error 3", "Stack trace 3");

        crashService.markCrashRecovered(log1.crashId);

        List<CrashRecoveryService.CrashLog> unrecovered = crashService.getUnrecoveredCrashes();
        assertEquals(2, unrecovered.size(), "Should have 2 unrecovered crashes");
    }

    @Test
    public void testGetRecentCrashes() {
        crashService.logCrash("farmer_001", "Error 1", "Stack trace 1");
        crashService.logCrash("farmer_002", "Error 2", "Stack trace 2");
        crashService.logCrash("farmer_003", "Error 3", "Stack trace 3");
        crashService.logCrash("farmer_004", "Error 4", "Stack trace 4");

        List<CrashRecoveryService.CrashLog> recent = crashService.getRecentCrashes(2);
        assertEquals(2, recent.size(), "Should return 2 recent crashes");
    }

    @Test
    public void testClearSessionSnapshot() {
        String userId = "farmer_001";
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("crop_id", "crop_123");

        crashService.saveSessionSnapshot(userId, sessionData);
        crashService.clearSessionSnapshot(userId);

        Map<String, String> restored = crashService.restoreSession(userId);
        assertTrue(restored.isEmpty(), "Session snapshot should be cleared");
    }

    @Test
    public void testGetCrashStatistics() {
        CrashRecoveryService.CrashLog log1 = crashService.logCrash("farmer_001", "Error 1", "Stack trace 1");
        CrashRecoveryService.CrashLog log2 = crashService.logCrash("farmer_002", "Error 2", "Stack trace 2");
        CrashRecoveryService.CrashLog log3 = crashService.logCrash("farmer_003", "Error 3", "Stack trace 3");

        crashService.markCrashRecovered(log1.crashId);
        crashService.markCrashRecovered(log2.crashId);

        Map<String, Integer> stats = crashService.getCrashStatistics();
        assertEquals(3, stats.get("total_crashes"), "Total crashes should be 3");
        assertEquals(2, stats.get("recovered_crashes"), "Recovered crashes should be 2");
        assertEquals(1, stats.get("unrecovered_crashes"), "Unrecovered crashes should be 1");
    }

    @Test
    public void testMultipleCrashes() {
        CrashRecoveryService.CrashLog log1 = crashService.logCrash("farmer_001", "Error 1", "Stack trace 1");
        CrashRecoveryService.CrashLog log2 = crashService.logCrash("farmer_002", "Error 2", "Stack trace 2");
        CrashRecoveryService.CrashLog log3 = crashService.logCrash("farmer_003", "Error 3", "Stack trace 3");

        assertEquals(3, crashService.getAllCrashLogs().size(), "Should have 3 crash logs");
    }

    @Test
    public void testCrashIdUniqueness() {
        CrashRecoveryService.CrashLog log1 = crashService.logCrash("farmer_001", "Error 1", "Stack trace 1");
        CrashRecoveryService.CrashLog log2 = crashService.logCrash("farmer_001", "Error 2", "Stack trace 2");

        assertNotEquals(log1.crashId, log2.crashId, "Crash IDs should be unique");
    }

    @Test
    public void testSessionDataPreservation() {
        String userId = "farmer_001";
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("crop_id", "crop_123");
        sessionData.put("farm_id", "farm_456");
        sessionData.put("last_action", "view_weather");

        crashService.saveSessionSnapshot(userId, sessionData);

        Map<String, String> restored = crashService.restoreSession(userId);
        assertEquals(3, restored.size(), "All session data should be preserved");
        assertEquals("crop_123", restored.get("crop_id"), "Crop ID should be preserved");
        assertEquals("farm_456", restored.get("farm_id"), "Farm ID should be preserved");
        assertEquals("view_weather", restored.get("last_action"), "Last action should be preserved");
    }
}
