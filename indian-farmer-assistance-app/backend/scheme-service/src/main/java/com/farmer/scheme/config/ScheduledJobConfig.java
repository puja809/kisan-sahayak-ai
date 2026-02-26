package com.farmer.scheme.config;

import com.farmer.scheme.service.DeadlineNotificationService;
import com.farmer.scheme.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for scheduled jobs.
 * Enables scheduling for deadline notification processing.
 * 
 * Requirements: 4.8, 11D.9
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobConfig {

    private final DeadlineNotificationService deadlineNotificationService;
    private final UserService userService;

    /**
     * Send 7-day deadline reminders.
     * Runs daily at 8:00 AM.
     * Requirements: 11D.9
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void send7DayDeadlineReminders() {
        log.info("Starting scheduled job: 7-day deadline reminders");
        try {
            var allUserIds = userService.getAllActiveUserIds();
            int sent = deadlineNotificationService.send7DayDeadlineReminders(allUserIds);
            log.info("Completed scheduled job: 7-day deadline reminders. Sent {} notifications", sent);
        } catch (Exception e) {
            log.error("Error in 7-day deadline reminder job: {}", e.getMessage(), e);
        }
    }

    /**
     * Send 1-day deadline reminders.
     * Runs daily at 9:00 AM.
     * Requirements: 11D.9
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void send1DayDeadlineReminders() {
        log.info("Starting scheduled job: 1-day deadline reminders");
        try {
            var allUserIds = userService.getAllActiveUserIds();
            int sent = deadlineNotificationService.send1DayDeadlineReminders(allUserIds);
            log.info("Completed scheduled job: 1-day deadline reminders. Sent {} notifications", sent);
        } catch (Exception e) {
            log.error("Error in 1-day deadline reminder job: {}", e.getMessage(), e);
        }
    }

    /**
     * Process all deadline notifications.
     * Runs daily at 10:00 AM.
     * Requirements: 11D.9
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void processAllDeadlineNotifications() {
        log.info("Starting scheduled job: process all deadline notifications");
        try {
            var allUserIds = userService.getAllActiveUserIds();
            var results = deadlineNotificationService.processAllDeadlineNotifications(allUserIds);
            log.info("Completed scheduled job: process all deadline notifications. Results: {}", results);
        } catch (Exception e) {
            log.error("Error in deadline notification processing job: {}", e.getMessage(), e);
        }
    }

    /**
     * Log deadline statistics for admin monitoring.
     * Runs daily at 7:00 AM.
     * Requirements: 11D.9
     */
    @Scheduled(cron = "0 0 7 * * ?")
    public void logDeadlineStatistics() {
        log.info("Starting scheduled job: log deadline statistics");
        try {
            var stats = deadlineNotificationService.getDeadlineStatistics();
            log.info("Deadline statistics: {}", stats);
        } catch (Exception e) {
            log.error("Error in deadline statistics job: {}", e.getMessage(), e);
        }
    }
}