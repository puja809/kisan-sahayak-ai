package com.farmer.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for session management service.
 * Tests session cleanup on logout and secure session handling.
 * Requirements: 17.4
 */
public class SessionManagementServiceTest {

    private SessionManagementService sessionService;

    @BeforeEach
    public void setUp() {
        sessionService = new SessionManagementService();
    }

    @Test
    public void testCreateSession() {
        String sessionId = "session_123";
        String userId = "farmer_001";
        String token = "jwt_token_xyz";
        long expiresAt = System.currentTimeMillis() + 3600000; // 1 hour
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        sessionService.createSession(sessionId, userId, token, expiresAt, ipAddress, userAgent);

        SessionManagementService.SessionData session = sessionService.getSession(sessionId);
        assertNotNull(session, "Session should be created");
        assertEquals(userId, session.userId, "User ID should match");
        assertEquals(token, session.token, "Token should match");
    }

    @Test
    public void testSessionValidation() {
        String sessionId = "session_123";
        String userId = "farmer_001";
        String token = "jwt_token_xyz";
        long expiresAt = System.currentTimeMillis() + 3600000; // 1 hour

        sessionService.createSession(sessionId, userId, token, expiresAt, "192.168.1.1", "Mozilla/5.0");

        assertTrue(sessionService.isSessionValid(sessionId), "Valid session should return true");
    }

    @Test
    public void testExpiredSessionValidation() {
        String sessionId = "session_123";
        String userId = "farmer_001";
        String token = "jwt_token_xyz";
        long expiresAt = System.currentTimeMillis() - 1000; // Already expired

        sessionService.createSession(sessionId, userId, token, expiresAt, "192.168.1.1", "Mozilla/5.0");

        assertFalse(sessionService.isSessionValid(sessionId), "Expired session should return false");
    }

    @Test
    public void testLogoutClearsSensitiveData() {
        String sessionId = "session_123";
        String userId = "farmer_001";
        String token = "jwt_token_xyz";
        long expiresAt = System.currentTimeMillis() + 3600000;

        sessionService.createSession(sessionId, userId, token, expiresAt, "192.168.1.1", "Mozilla/5.0");

        SessionManagementService.SessionData sessionBefore = sessionService.getSession(sessionId);
        assertNotNull(sessionBefore.token, "Token should exist before logout");

        sessionService.logout(sessionId);

        SessionManagementService.SessionData sessionAfter = sessionService.getSession(sessionId);
        assertNull(sessionAfter, "Session should be removed after logout");
    }

    @Test
    public void testLogoutNonExistentSession() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            sessionService.logout("non_existent_session");
        }, "Logout of non-existent session should not throw exception");
    }

    @Test
    public void testClearExpiredSessions() throws InterruptedException {
        String sessionId1 = "session_1";
        String sessionId2 = "session_2";
        long expiredTime = System.currentTimeMillis() - 1000;
        long validTime = System.currentTimeMillis() + 3600000;

        sessionService.createSession(sessionId1, "farmer_001", "token_1", expiredTime, "192.168.1.1", "Mozilla/5.0");
        sessionService.createSession(sessionId2, "farmer_002", "token_2", validTime, "192.168.1.2", "Mozilla/5.0");

        sessionService.clearExpiredSessions();

        assertNull(sessionService.getSession(sessionId1), "Expired session should be cleared");
        assertNotNull(sessionService.getSession(sessionId2), "Valid session should remain");
    }

    @Test
    public void testSessionDataClearSensitiveData() {
        SessionManagementService.SessionData sessionData = new SessionManagementService.SessionData(
            "farmer_001", "token_xyz", System.currentTimeMillis() + 3600000, "192.168.1.1", "Mozilla/5.0"
        );

        assertNotNull(sessionData.token, "Token should exist before clearing");
        assertNotNull(sessionData.userId, "User ID should exist before clearing");

        sessionData.clearSensitiveData();

        assertNull(sessionData.token, "Token should be null after clearing");
        assertNull(sessionData.userId, "User ID should be null after clearing");
        assertNull(sessionData.ipAddress, "IP address should be null after clearing");
        assertNull(sessionData.userAgent, "User agent should be null after clearing");
    }

    @Test
    public void testMultipleSessions() {
        sessionService.createSession("session_1", "farmer_001", "token_1", System.currentTimeMillis() + 3600000, "192.168.1.1", "Mozilla/5.0");
        sessionService.createSession("session_2", "farmer_002", "token_2", System.currentTimeMillis() + 3600000, "192.168.1.2", "Mozilla/5.0");
        sessionService.createSession("session_3", "farmer_003", "token_3", System.currentTimeMillis() + 3600000, "192.168.1.3", "Mozilla/5.0");

        assertTrue(sessionService.isSessionValid("session_1"), "Session 1 should be valid");
        assertTrue(sessionService.isSessionValid("session_2"), "Session 2 should be valid");
        assertTrue(sessionService.isSessionValid("session_3"), "Session 3 should be valid");

        sessionService.logout("session_2");

        assertTrue(sessionService.isSessionValid("session_1"), "Session 1 should still be valid");
        assertFalse(sessionService.isSessionValid("session_2"), "Session 2 should be invalid after logout");
        assertTrue(sessionService.isSessionValid("session_3"), "Session 3 should still be valid");
    }
}
