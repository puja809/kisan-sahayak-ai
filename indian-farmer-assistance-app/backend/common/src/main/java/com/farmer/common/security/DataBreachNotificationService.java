package com.farmer.common.security;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for detecting and notifying data breaches.
 * Complies with DPDP Act 2023 requirement to notify within 72 hours.
 */
public class DataBreachNotificationService {

    public static class BreachNotification {
        public String breachId;
        public String affectedUserId;
        public String breachDescription;
        public LocalDateTime breachDetectedAt;
        public LocalDateTime notificationSentAt;
        public String notificationMethod; // EMAIL, SMS, IN_APP
        public boolean acknowledged;

        public BreachNotification(String breachId, String affectedUserId, String breachDescription) {
            this.breachId = breachId;
            this.affectedUserId = affectedUserId;
            this.breachDescription = breachDescription;
            this.breachDetectedAt = LocalDateTime.now();
            this.notificationSentAt = null;
            this.acknowledged = false;
        }
    }

    private final List<BreachNotification> breachNotifications = new ArrayList<>();

    /**
     * Report a data breach
     */
    public BreachNotification reportBreach(String affectedUserId, String breachDescription) {
        String breachId = "BREACH_" + UUID.randomUUID().toString();
        BreachNotification notification = new BreachNotification(breachId, affectedUserId, breachDescription);
        breachNotifications.add(notification);
        return notification;
    }

    /**
     * Send notification to affected user (within 72 hours)
     */
    public void sendNotification(String breachId, String notificationMethod) {
        for (BreachNotification notification : breachNotifications) {
            if (notification.breachId.equals(breachId)) {
                notification.notificationSentAt = LocalDateTime.now();
                notification.notificationMethod = notificationMethod;
                // In production, send actual notification via email/SMS/in-app
                return;
            }
        }
    }

    /**
     * Check if notification was sent within 72 hours
     */
    public boolean isNotificationTimely(String breachId) {
        for (BreachNotification notification : breachNotifications) {
            if (notification.breachId.equals(breachId)) {
                if (notification.notificationSentAt == null) {
                    return false;
                }

                long hoursDifference = java.time.temporal.ChronoUnit.HOURS.between(
                    notification.breachDetectedAt,
                    notification.notificationSentAt
                );

                return hoursDifference <= 72;
            }
        }
        return false;
    }

    /**
     * Get all pending notifications
     */
    public List<BreachNotification> getPendingNotifications() {
        List<BreachNotification> pending = new ArrayList<>();
        for (BreachNotification notification : breachNotifications) {
            if (notification.notificationSentAt == null) {
                pending.add(notification);
            }
        }
        return pending;
    }

    /**
     * Mark notification as acknowledged by user
     */
    public void acknowledgeNotification(String breachId) {
        for (BreachNotification notification : breachNotifications) {
            if (notification.breachId.equals(breachId)) {
                notification.acknowledged = true;
                return;
            }
        }
    }
}
