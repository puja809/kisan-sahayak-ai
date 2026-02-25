package com.farmer.scheme.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications to farmers about application status changes.
 * Requirements: 11D.10
 */
@Service
@Slf4j
public class NotificationService {

    /**
     * Notification types for scheme applications.
     * Requirements: 11D.10
     */
    public enum NotificationType {
        APPLICATION_RECEIVED,
        STATUS_UPDATE,
        APPROVAL,
        REJECTION,
        DISBURSEMENT
    }

    /**
     * Send notification to a user.
     * Requirements: 11D.10
     */
    public void sendNotification(Long userId, NotificationType type, String message) {
        log.info("Sending notification to user {} - Type: {}, Message: {}", userId, type, message);
        
        // In a real implementation, this would:
        // 1. Store notification in database
        // 2. Send push notification to mobile app
        // 3. Send SMS if configured
        // 4. Send email if configured
        
        // For now, just log the notification
        log.debug("Notification sent successfully to user: {}", userId);
    }

    /**
     * Send batch notifications to multiple users.
     * Requirements: 11D.10
     */
    public void sendBatchNotification(java.util.List<Long> userIds, NotificationType type, String message) {
        log.info("Sending batch notification to {} users - Type: {}", userIds.size(), type);
        userIds.forEach(userId -> sendNotification(userId, type, message));
    }
}