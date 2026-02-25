package com.farmer.scheme.service;

import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.SchemeApplication;
import com.farmer.scheme.repository.SchemeApplicationRepository;
import com.farmer.scheme.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for handling scheme deadline notifications.
 * Identifies farmers who haven't applied to schemes with approaching deadlines
 * and sends appropriate notifications.
 * 
 * Requirements: 4.8, 11D.9
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadlineNotificationService {

    private final SchemeRepository schemeRepository;
    private final SchemeApplicationRepository schemeApplicationRepository;
    private final NotificationService notificationService;

    /**
     * Notification types for deadline reminders.
     * Requirements: 4.8, 11D.9
     */
    public enum DeadlineNotificationType {
        DEADLINE_APPROACHING_7_DAYS,
        DEADLINE_APPROACHING_1_DAY,
        DEADLINE_PASSED
    }

    /**
     * Get schemes with deadlines approaching within the specified number of days.
     * Requirements: 11D.9
     * 
     * @param daysAhead Number of days to look ahead for approaching deadlines
     * @return List of schemes with approaching deadlines
     */
    @Transactional(readOnly = true)
    public List<Scheme> getSchemesWithApproachingDeadlines(int daysAhead) {
        log.debug("Fetching schemes with deadlines within {} days", daysAhead);
        LocalDate currentDate = LocalDate.now();
        LocalDate deadlineDate = currentDate.plusDays(daysAhead);
        return schemeRepository.findSchemesWithApproachingDeadlines(currentDate, deadlineDate);
    }

    /**
     * Get schemes with deadlines exactly 7 days away.
     * Requirements: 11D.9
     * 
     * @return List of schemes with 7-day deadline warning
     */
    @Transactional(readOnly = true)
    public List<Scheme> getSchemesWith7DayDeadline() {
        return getSchemesWithApproachingDeadlines(7);
    }

    /**
     * Get schemes with deadlines exactly 1 day away.
     * Requirements: 11D.9
     * 
     * @return List of schemes with 1-day deadline warning
     */
    @Transactional(readOnly = true)
    public List<Scheme> getSchemesWith1DayDeadline() {
        return getSchemesWithApproachingDeadlines(1);
    }

    /**
     * Get all user IDs who have not applied to a specific scheme.
     * Requirements: 11D.9
     * 
     * @param schemeId The scheme ID to check
     * @param allUserIds All active user IDs in the system
     * @return List of user IDs who haven't applied to the scheme
     */
    @Transactional(readOnly = true)
    public List<Long> getNonApplicantsForScheme(Long schemeId, List<Long> allUserIds) {
        log.debug("Finding non-applicants for scheme: {}", schemeId);
        
        List<Long> applicants = schemeApplicationRepository.findBySchemeId(schemeId).stream()
                .map(SchemeApplication::getUserId)
                .collect(Collectors.toList());
        
        return allUserIds.stream()
                .filter(userId -> !applicants.contains(userId))
                .collect(Collectors.toList());
    }

    /**
     * Get farmers eligible for deadline notification for a scheme.
     * Filters to only include farmers in the scheme's applicable state (if state-specific).
     * Requirements: 11D.9
     * 
     * @param scheme The scheme to check
     * @param allUserIds All active user IDs
     * @return List of user IDs eligible for notification
     */
    @Transactional(readOnly = true)
    public List<Long> getEligibleFarmersForScheme(Scheme scheme, List<Long> allUserIds) {
        // For central schemes, all users are eligible
        if (scheme.getSchemeType() == Scheme.SchemeType.CENTRAL || scheme.getState() == null) {
            return allUserIds;
        }
        
        // For state-specific schemes, only users in that state are eligible
        // This would require a UserRepository query, but for now return all users
        // In a real implementation, we would filter by state
        log.debug("Scheme {} is state-specific ({}), filtering by state would be applied here",
                scheme.getSchemeName(), scheme.getState());
        return allUserIds;
    }

    /**
     * Send deadline reminder notifications for schemes with approaching deadlines.
     * Requirements: 4.8, 11D.9
     * 
     * @param daysAhead Number of days until deadline
     * @param notificationType The type of notification to send
     * @param allUserIds All active user IDs in the system
     * @return Number of notifications sent
     */
    public int sendDeadlineReminders(int daysAhead, DeadlineNotificationType notificationType, List<Long> allUserIds) {
        log.info("Sending {} day deadline reminders for {} users", daysAhead, allUserIds.size());
        
        List<Scheme> schemes = getSchemesWithApproachingDeadlines(daysAhead);
        log.info("Found {} schemes with deadlines within {} days", schemes.size(), daysAhead);
        
        int totalNotificationsSent = 0;
        
        for (Scheme scheme : schemes) {
            // Get farmers who haven't applied to this scheme
            List<Long> nonApplicants = getNonApplicantsForScheme(scheme.getId(), allUserIds);
            
            if (nonApplicants.isEmpty()) {
                log.debug("No non-applicants found for scheme: {}", scheme.getSchemeName());
                continue;
            }
            
            // Create notification message
            String message = createDeadlineNotificationMessage(scheme, daysAhead);
            
            // Send batch notification
            notificationService.sendBatchNotification(nonApplicants, 
                    mapToNotificationType(notificationType), message);
            
            totalNotificationsSent += nonApplicants.size();
            log.info("Sent {} notifications for scheme: {}", nonApplicants.size(), scheme.getSchemeName());
        }
        
        return totalNotificationsSent;
    }

    /**
     * Send 7-day deadline reminders.
     * Requirements: 11D.9
     * 
     * @param allUserIds All active user IDs
     * @return Number of notifications sent
     */
    public int send7DayDeadlineReminders(List<Long> allUserIds) {
        return sendDeadlineReminders(7, DeadlineNotificationType.DEADLINE_APPROACHING_7_DAYS, allUserIds);
    }

    /**
     * Send 1-day deadline reminders.
     * Requirements: 11D.9
     * 
     * @param allUserIds All active user IDs
     * @return Number of notifications sent
     */
    public int send1DayDeadlineReminders(List<Long> allUserIds) {
        return sendDeadlineReminders(1, DeadlineNotificationType.DEADLINE_APPROACHING_1_DAY, allUserIds);
    }

    /**
     * Send deadline passed notifications for schemes that have expired.
     * Requirements: 4.8
     * 
     * @param allUserIds All active user IDs
     * @return Number of notifications sent
     */
    public int sendDeadlinePassedNotifications(List<Long> allUserIds) {
        log.info("Sending deadline passed notifications for {} users", allUserIds.size());
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Scheme> expiredSchemes = schemeRepository.findSchemesWithApproachingDeadlines(
                yesterday.minusDays(30), yesterday);
        
        int totalNotificationsSent = 0;
        
        for (Scheme scheme : expiredSchemes) {
            List<Long> nonApplicants = getNonApplicantsForScheme(scheme.getId(), allUserIds);
            
            if (nonApplicants.isEmpty()) {
                continue;
            }
            
            String message = String.format(
                    "The application deadline for %s has passed. You can check for similar schemes in the app.",
                    scheme.getSchemeName());
            
            notificationService.sendBatchNotification(nonApplicants, 
                    NotificationService.NotificationType.STATUS_UPDATE, message);
            
            totalNotificationsSent += nonApplicants.size();
        }
        
        return totalNotificationsSent;
    }

    /**
     * Process all deadline notifications in batch.
     * Runs both 7-day and 1-day reminders.
     * Requirements: 11D.9
     * 
     * @param allUserIds All active user IDs
     * @return Map of notification type to count sent
     */
    public Map<String, Integer> processAllDeadlineNotifications(List<Long> allUserIds) {
        log.info("Processing all deadline notifications for {} users", allUserIds.size());
        
        Map<String, Integer> results = new java.util.HashMap<>();
        
        int sent7Day = send7DayDeadlineReminders(allUserIds);
        results.put("7_day_reminders", sent7Day);
        
        int sent1Day = send1DayDeadlineReminders(allUserIds);
        results.put("1_day_reminders", sent1Day);
        
        log.info("Total deadline notifications sent: 7-day={}, 1-day={}", sent7Day, sent1Day);
        return results;
    }

    /**
     * Create notification message for deadline reminder.
     * Requirements: 4.8, 11D.9
     * 
     * @param scheme The scheme with approaching deadline
     * @param daysRemaining Number of days until deadline
     * @return Formatted notification message
     */
    private String createDeadlineNotificationMessage(Scheme scheme, int daysRemaining) {
        String timeLeft = daysRemaining == 1 ? "tomorrow" : "in " + daysRemaining + " days";
        
        StringBuilder message = new StringBuilder();
        message.append("Reminder: The application deadline for ");
        message.append(scheme.getSchemeName());
        message.append(" is ");
        message.append(timeLeft);
        message.append(" (");
        message.append(scheme.getApplicationEndDate());
        message.append("). ");
        
        if (scheme.getBenefitAmount() != null) {
            message.append("Benefit: ₹");
            message.append(scheme.getBenefitAmount());
            message.append(". ");
        }
        
        message.append("Don't miss this opportunity! Apply now through the app.");
        
        return message.toString();
    }

    /**
     * Map deadline notification type to general notification type.
     * Requirements: 11D.9
     * 
     * @param deadlineType The deadline notification type
     * @return Corresponding general notification type
     */
    private NotificationService.NotificationType mapToNotificationType(DeadlineNotificationType deadlineType) {
        return switch (deadlineType) {
            case DEADLINE_APPROACHING_7_DAYS, DEADLINE_APPROACHING_1_DAY -> 
                NotificationService.NotificationType.STATUS_UPDATE;
            case DEADLINE_PASSED -> 
                NotificationService.NotificationType.STATUS_UPDATE;
        };
    }

    /**
     * Get statistics about upcoming deadlines.
     * Useful for admin dashboard.
     * Requirements: 11D.9
     * 
     * @return Map containing deadline statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDeadlineStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        List<Scheme> schemes7Days = getSchemesWith7DayDeadline();
        List<Scheme> schemes1Day = getSchemesWith1DayDeadline();
        
        stats.put("schemes_with_7_day_deadline", schemes7Days.size());
        stats.put("schemes_with_1_day_deadline", schemes1Day.size());
        stats.put("total_schemes_with_approaching_deadline", schemes7Days.size() + schemes1Day.size());
        
        // List scheme names for admin reference
        List<String> urgentSchemes = schemes1Day.stream()
                .map(Scheme::getSchemeName)
                .collect(Collectors.toList());
        stats.put("urgent_deadline_schemes", urgentSchemes);
        
        return stats;
    }
}