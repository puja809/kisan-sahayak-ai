package com.farmer.scheme.service;

import com.farmer.scheme.entity.SchemeApplication;
import com.farmer.scheme.repository.SchemeApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scheduled job to check for approaching scheme deadlines and send notifications.
 * Runs daily at 8 AM to identify schemes with deadlines in 7 days and 1 day,
 * then sends push notifications to farmers who haven't applied.
 * 
 * Requirements: 4.8, 11D.9
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.scheduler.deadline-notifications.enabled", havingValue = "true", matchIfMissing = true)
public class SchemeDeadlineScheduler {

    private final DeadlineNotificationService deadlineNotificationService;
    private final SchemeApplicationRepository schemeApplicationRepository;
    
    private final WebClient userServiceWebClient;
    
    @Value("${app.user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Daily scheduled job to check approaching deadlines and send notifications.
     * Runs at 8:00 AM every day.
     * Requirements: 11D.9
     */
    @Scheduled(cron = "${app.scheduler.deadline-notifications.cron:0 0 8 * * *}")
    public void checkAndNotifyApproachingDeadlines() {
        log.info("Starting deadline notification check at {}", LocalDateTime.now().format(FORMATTER));
        
        try {
            // Get all active farmers
            List<Long> allUserIds = getAllActiveFarmerIds();
            
            if (allUserIds.isEmpty()) {
                log.warn("No active farmers found for deadline notifications");
                return;
            }
            
            log.info("Processing deadline notifications for {} active farmers", allUserIds.size());
            
            // Process all deadline notifications (7-day and 1-day)
            Map<String, Integer> results = deadlineNotificationService.processAllDeadlineNotifications(allUserIds);
            
            // Log results
            log.info("Deadline notification job completed successfully");
            log.info("7-day reminders sent: {}", results.getOrDefault("7_day_reminders", 0));
            log.info("1-day reminders sent: {}", results.getOrDefault("1_day_reminders", 0));
            
        } catch (Exception e) {
            log.error("Error during deadline notification job", e);
        }
    }

    /**
     * Separate job for 7-day deadline reminders.
     * Can be configured to run at a different time if needed.
     * Requirements: 11D.9
     */
    @Scheduled(cron = "${app.scheduler.deadline-notifications.7day-cron:0 0 8 * * *}")
    public void send7DayDeadlineReminders() {
        log.info("Starting 7-day deadline reminder job at {}", LocalDateTime.now().format(FORMATTER));
        
        try {
            List<Long> allUserIds = getAllActiveFarmerIds();
            
            if (allUserIds.isEmpty()) {
                log.warn("No active farmers found for 7-day deadline reminders");
                return;
            }
            
            int sent = deadlineNotificationService.send7DayDeadlineReminders(allUserIds);
            log.info("7-day deadline reminders sent: {}", sent);
            
        } catch (Exception e) {
            log.error("Error sending 7-day deadline reminders", e);
        }
    }

    /**
     * Separate job for 1-day deadline reminders.
     * Runs at 8:00 AM, one day before the deadline.
     * Requirements: 11D.9
     */
    @Scheduled(cron = "${app.scheduler.deadline-notifications.1day-cron:0 0 8 * * *}")
    public void send1DayDeadlineReminders() {
        log.info("Starting 1-day deadline reminder job at {}", LocalDateTime.now().format(FORMATTER));
        
        try {
            List<Long> allUserIds = getAllActiveFarmerIds();
            
            if (allUserIds.isEmpty()) {
                log.warn("No active farmers found for 1-day deadline reminders");
                return;
            }
            
            int sent = deadlineNotificationService.send1DayDeadlineReminders(allUserIds);
            log.info("1-day deadline reminders sent: {}", sent);
            
        } catch (Exception e) {
            log.error("Error sending 1-day deadline reminders", e);
        }
    }

    /**
     * Weekly job to send deadline passed notifications.
     * Runs every Sunday at 9 AM to notify about missed deadlines.
     * Requirements: 4.8
     */
    @Scheduled(cron = "${app.scheduler.deadline-notifications.passed-cron:0 0 9 * * SUN}")
    public void sendDeadlinePassedNotifications() {
        log.info("Starting deadline passed notification job at {}", LocalDateTime.now().format(FORMATTER));
        
        try {
            List<Long> allUserIds = getAllActiveFarmerIds();
            
            if (allUserIds.isEmpty()) {
                log.warn("No active farmers found for deadline passed notifications");
                return;
            }
            
            int sent = deadlineNotificationService.sendDeadlinePassedNotifications(allUserIds);
            log.info("Deadline passed notifications sent: {}", sent);
            
        } catch (Exception e) {
            log.error("Error sending deadline passed notifications", e);
        }
    }

    /**
     * Get all active farmer user IDs.
     * First tries to fetch from user-service via REST API,
     * falls back to extracting from existing applications.
     * Requirements: 11D.9
     * 
     * @return List of active farmer user IDs
     */
    private List<Long> getAllActiveFarmerIds() {
        try {
            // Try to fetch from user-service via REST API
            @SuppressWarnings("unchecked")
            List<Long> userIds = userServiceWebClient.get()
                    .uri("/api/v1/users/ids/active")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
            
            if (userIds != null && !userIds.isEmpty()) {
                return userIds;
            }
        } catch (Exception e) {
            log.warn("Could not fetch user IDs from user-service, falling back to local extraction: {}", e.getMessage());
        }
        
        // Fallback: Extract unique user IDs from existing applications
        return schemeApplicationRepository.findAll().stream()
                .map(SchemeApplication::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get deadline statistics for monitoring.
     * Can be called by admin endpoints or monitoring systems.
     * Requirements: 11D.9
     * 
     * @return Map containing deadline statistics
     */
    public Map<String, Object> getDeadlineStatistics() {
        return deadlineNotificationService.getDeadlineStatistics();
    }
}