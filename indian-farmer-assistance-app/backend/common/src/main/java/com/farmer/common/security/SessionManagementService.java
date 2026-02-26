package com.farmer.common.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for secure session management.
 * Clears sensitive data from memory on logout.
 */
public class SessionManagementService {

    private final Map<String, SessionData> activeSessions = new HashMap<>();

    public static class SessionData {
        public String userId;
        public String token;
        public long createdAt;
        public long expiresAt;
        public String ipAddress;
        public String userAgent;

        public SessionData(String userId, String token, long expiresAt, String ipAddress, String userAgent) {
            this.userId = userId;
            this.token = token;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = expiresAt;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
        }

        /**
         * Clear sensitive data from memory
         */
        public void clearSensitiveData() {
            this.token = null;
            this.userId = null;
            this.ipAddress = null;
            this.userAgent = null;
        }
    }

    /**
     * Create a new session
     */
    public void createSession(String sessionId, String userId, String token, long expiresAt, String ipAddress, String userAgent) {
        activeSessions.put(sessionId, new SessionData(userId, token, expiresAt, ipAddress, userAgent));
    }

    /**
     * Get session data
     */
    public SessionData getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * Validate session is still active
     */
    public boolean isSessionValid(String sessionId) {
        SessionData session = activeSessions.get(sessionId);
        if (session == null) {
            return false;
        }
        return System.currentTimeMillis() < session.expiresAt;
    }

    /**
     * Logout and clear sensitive data
     */
    public void logout(String sessionId) {
        SessionData session = activeSessions.get(sessionId);
        if (session != null) {
            session.clearSensitiveData();
            activeSessions.remove(sessionId);
        }
    }

    /**
     * Clear all expired sessions
     */
    public void clearExpiredSessions() {
        List<String> expiredSessions = new ArrayList<>();
        for (String sessionId : activeSessions.keySet()) {
            if (System.currentTimeMillis() >= activeSessions.get(sessionId).expiresAt) {
                expiredSessions.add(sessionId);
            }
        }
        for (String sessionId : expiredSessions) {
            activeSessions.remove(sessionId);
        }
    }
}
