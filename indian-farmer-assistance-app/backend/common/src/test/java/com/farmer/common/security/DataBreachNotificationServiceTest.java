package com.farmer.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for data breach notification service.
 * Tests breach detection and notification within 72 hours.
 * Requirements: 17.5
 */
public class DataBreachNotificationServiceTest {

    private DataBreachNotificationService breachService;

    @BeforeEach
    public void setUp() {
        breachService = new DataBreachNotificationService();
    }

    @Test
    public void testReportBreach() {
        String affectedUserId = "farmer_001";
        String breachDescription = "Unauthorized access to user profile";

        DataBreachNotificationService.BreachNotification notification = breachService.reportBreach(affectedUserId, breachDescription);

        assertNotNull(notification, "Breach notification should be created");
        assertEquals(affectedUserId, notification.affectedUserId, "Affected user ID should match");
        assertEquals(breachDescription, notification.breachDescription, "Breach description should match");
        assertNotNull(notification.breachDetectedAt, "Breach detection timestamp should be set");
        assertNull(notification.notificationSentAt, "Notification sent timestamp should be null initially");
        assertFalse(notification.acknowledged, "Notification should not be acknowledged initially");
    }

    @Test
    public void testSendNotification() {
        DataBreachNotificationService.BreachNotification notification = breachService.reportBreach("farmer_001", "Unauthorized access");

        breachService.sendNotification(notification.breachId, "EMAIL");

        assertNotNull(notification.notificationSentAt, "Notification sent timestamp should be set");
        assertEquals("EMAIL", notification.notificationMethod, "Notification method should be EMAIL");
    }

    @Test
    public void testNotificationTimeliness() {
        DataBreachNotificationService.BreachNotification notification = breachService.reportBreach("farmer_001", "Unauthorized access");

        breachService.sendNotification(notification.breachId, "EMAIL");

        assertTrue(breachService.isNotificationTimely(notification.breachId), "Notification should be timely (within 72 hours)");
    }

    @Test
    public void testGetPendingNotifications() {
        breachService.reportBreach("farmer_001", "Breach 1");
        breachService.reportBreach("farmer_002", "Breach 2");
        DataBreachNotificationService.BreachNotification notification3 = breachService.reportBreach("farmer_003", "Breach 3");

        breachService.sendNotification(notification3.breachId, "SMS");

        List<DataBreachNotificationService.BreachNotification> pending = breachService.getPendingNotifications();
        assertEquals(2, pending.size(), "Should have 2 pending notifications");
    }

    @Test
    public void testAcknowledgeNotification() {
        DataBreachNotificationService.BreachNotification notification = breachService.reportBreach("farmer_001", "Unauthorized access");

        breachService.sendNotification(notification.breachId, "EMAIL");
        assertFalse(notification.acknowledged, "Notification should not be acknowledged initially");

        breachService.acknowledgeNotification(notification.breachId);
        assertTrue(notification.acknowledged, "Notification should be acknowledged");
    }

    @Test
    public void testMultipleBreaches() {
        DataBreachNotificationService.BreachNotification breach1 = breachService.reportBreach("farmer_001", "Breach 1");
        DataBreachNotificationService.BreachNotification breach2 = breachService.reportBreach("farmer_002", "Breach 2");
        DataBreachNotificationService.BreachNotification breach3 = breachService.reportBreach("farmer_003", "Breach 3");

        breachService.sendNotification(breach1.breachId, "EMAIL");
        breachService.sendNotification(breach2.breachId, "SMS");
        breachService.sendNotification(breach3.breachId, "IN_APP");

        assertTrue(breachService.isNotificationTimely(breach1.breachId), "Breach 1 notification should be timely");
        assertTrue(breachService.isNotificationTimely(breach2.breachId), "Breach 2 notification should be timely");
        assertTrue(breachService.isNotificationTimely(breach3.breachId), "Breach 3 notification should be timely");
    }

    @Test
    public void testBreachIdUniqueness() {
        DataBreachNotificationService.BreachNotification breach1 = breachService.reportBreach("farmer_001", "Breach 1");
        DataBreachNotificationService.BreachNotification breach2 = breachService.reportBreach("farmer_001", "Breach 2");

        assertNotEquals(breach1.breachId, breach2.breachId, "Breach IDs should be unique");
    }

    @Test
    public void testNotificationWithDifferentMethods() {
        DataBreachNotificationService.BreachNotification breach1 = breachService.reportBreach("farmer_001", "Breach 1");
        DataBreachNotificationService.BreachNotification breach2 = breachService.reportBreach("farmer_002", "Breach 2");
        DataBreachNotificationService.BreachNotification breach3 = breachService.reportBreach("farmer_003", "Breach 3");

        breachService.sendNotification(breach1.breachId, "EMAIL");
        breachService.sendNotification(breach2.breachId, "SMS");
        breachService.sendNotification(breach3.breachId, "IN_APP");

        assertEquals("EMAIL", breach1.notificationMethod, "Breach 1 should use EMAIL");
        assertEquals("SMS", breach2.notificationMethod, "Breach 2 should use SMS");
        assertEquals("IN_APP", breach3.notificationMethod, "Breach 3 should use IN_APP");
    }

    @Test
    public void testSendNotificationToNonExistentBreach() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            breachService.sendNotification("non_existent_breach_id", "EMAIL");
        }, "Sending notification to non-existent breach should not throw exception");
    }
}
