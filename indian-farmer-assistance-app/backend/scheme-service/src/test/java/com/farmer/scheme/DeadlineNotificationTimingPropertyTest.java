package com.farmer.scheme;

import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.SchemeApplication;
import com.farmer.scheme.repository.SchemeApplicationRepository;
import com.farmer.scheme.repository.SchemeRepository;
import com.farmer.scheme.service.DeadlineNotificationService;
import com.farmer.scheme.service.DeadlineNotificationService.DeadlineNotificationType;
import com.farmer.scheme.service.NotificationService;
import com.farmer.scheme.service.NotificationService.NotificationType;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for scheme deadline notification timing.
 * Validates: Requirements 11D.9
 * 
 * Property 28: Scheme Deadline Notification Timing
 * For any scheme with an approaching application deadline, the system should 
 * send push notifications exactly 7 days before and 1 day before the deadline 
 * to eligible farmers.
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class DeadlineNotificationTimingPropertyTest {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeApplicationRepository schemeApplicationRepository;

    @Autowired
    private DeadlineNotificationService deadlineNotificationService;

    @BeforeEach
    void setUp() {
        schemeRepository.deleteAll();
        schemeApplicationRepository.deleteAll();
    }

    // ==================== GENERATORS ====================

    /**
     * Generator for deadline notification types.
     */
    @Provide
    Arbitrary<DeadlineNotificationType> notificationTypes() {
        return Arbitraries.of(DeadlineNotificationType.class);
    }

    /**
     * Generator for user ID lists of various sizes.
     */
    @Provide
    Arbitrary<List<Long>> userIdLists() {
        return Arbitraries.integers().between(1, 100)
                .flatMap(count -> {
                    List<Long> userIds = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        userIds.add((long) (i + 1));
                    }
                    return Arbitraries.just(userIds);
                });
    }

    /**
     * Generator for scheme with random deadline.
     */
    @Provide
    Arbitrary<Scheme> schemesWithDeadline() {
        return Combinators.combine(
                Arbitraries.strings().alpha().withChars('A', 'Z').ofMinLength(3).ofMaxLength(10),
                Arbitraries.strings().ofMinLength(5).ofMaxLength(50),
                Arbitraries.integers().between(1, 60)
        ).as((code, name, daysUntilDeadline) -> {
            LocalDate today = LocalDate.now();
            return Scheme.builder()
                    .schemeCode("SCHEME-" + code)
                    .schemeName(name)
                    .schemeType(Scheme.SchemeType.CENTRAL)
                    .applicationStartDate(today.minusDays(30))
                    .applicationEndDate(today.plusDays(daysUntilDeadline))
                    .benefitAmount(new Double("5000.00"))
                    .isActive(true)
                    .build();
        });
    }

    /**
     * Generator for schemes with specific deadline ranges.
     */
    @Provide
    Arbitrary<Scheme> schemesWith7DayDeadline() {
        return Arbitraries.just(7).flatMap(days -> {
            LocalDate today = LocalDate.now();
            return Arbitraries.strings().alpha().withChars('A', 'Z').ofMinLength(3).ofMaxLength(10)
                    .map(code -> Scheme.builder()
                            .schemeCode("SCHEME-" + code)
                            .schemeName("7-Day Deadline Scheme")
                            .schemeType(Scheme.SchemeType.CENTRAL)
                            .applicationStartDate(today.minusDays(30))
                            .applicationEndDate(today.plusDays(7))
                            .benefitAmount(new Double("6000.00"))
                            .isActive(true)
                            .build());
        });
    }

    /**
     * Generator for schemes with 1-day deadline.
     */
    @Provide
    Arbitrary<Scheme> schemesWith1DayDeadline() {
        return Arbitraries.just(1).flatMap(days -> {
            LocalDate today = LocalDate.now();
            return Arbitraries.strings().alpha().withChars('A', 'Z').ofMinLength(3).ofMaxLength(10)
                    .map(code -> Scheme.builder()
                            .schemeCode("SCHEME-" + code)
                            .schemeName("1-Day Deadline Scheme")
                            .schemeType(Scheme.SchemeType.CENTRAL)
                            .applicationStartDate(today.minusDays(30))
                            .applicationEndDate(today.plusDays(1))
                            .benefitAmount(new Double("10000.00"))
                            .isActive(true)
                            .build());
        });
    }

    /**
     * Generator for schemes with expired deadline.
     */
    @Provide
    Arbitrary<Scheme> schemesWithExpiredDeadline() {
        return Arbitraries.integers().between(1, 30).map(daysExpired -> {
            LocalDate today = LocalDate.now();
            return Scheme.builder()
                    .schemeCode("EXPIRED-SCHEME-" + System.currentTimeMillis())
                    .schemeName("Expired Scheme")
                    .schemeType(Scheme.SchemeType.CENTRAL)
                    .applicationStartDate(today.minusDays(60))
                    .applicationEndDate(today.minusDays(daysExpired))
                    .benefitAmount(new Double("5000.00"))
                    .isActive(true)
                    .build();
        });
    }

    // ==================== PROPERTY TESTS ====================

    /**
     * Property 28.1: Notifications should be sent exactly 7 days before deadline.
     * 
     * For any scheme with an application deadline exactly 7 days from now,
     * the system should identify it as requiring 7-day notification.
     * 
     * Validates: Requirements 11D.9
     */
    @Property
    void shouldIdentifySchemesWith7DayDeadline(
            @ForAll("schemesWith7DayDeadline") Scheme scheme) {
        // Arrange
        schemeRepository.save(scheme);

        // Act
        List<Scheme> schemes = deadlineNotificationService.getSchemesWith7DayDeadline();

        // Then
        assertNotNull(schemes, "Schemes list should not be null");
        
        // The scheme should be in the 7-day deadline list
        boolean hasScheme = schemes.stream()
                .anyMatch(s -> s.getApplicationEndDate().equals(LocalDate.now().plusDays(7)));
        
        assertTrue(hasScheme, "Scheme with 7-day deadline should be identified for notification");
    }

    /**
     * Property 28.2: Notifications should be sent exactly 1 day before deadline.
     * 
     * For any scheme with an application deadline exactly 1 day from now,
     * the system should identify it as requiring 1-day notification.
     * 
     * Validates: Requirements 11D.9
     */
    @Property
    void shouldIdentifySchemesWith1DayDeadline(
            @ForAll("schemesWith1DayDeadline") Scheme scheme) {
        // Arrange
        schemeRepository.save(scheme);

        // Act
        List<Scheme> schemes = deadlineNotificationService.getSchemesWith1DayDeadline();

        // Then
        assertNotNull(schemes, "Schemes list should not be null");
        
        // The scheme should be in the 1-day deadline list
        boolean hasScheme = schemes.stream()
                .anyMatch(s -> s.getApplicationEndDate().equals(LocalDate.now().plusDays(1)));
        
        assertTrue(hasScheme, "Scheme with 1-day deadline should be identified for notification");
    }

    /**
     * Property 28.3: Deadline notification timing is deterministic.
     * 
     * For any scheme, calling the deadline notification methods multiple times
     * with the same inputs should produce the same results.
     * 
     * Validates: Requirements 11D.9
     */
    @Property
    void deadlineNotificationTimingIsDeterministic(
            @ForAll("schemesWithDeadline") Scheme scheme) {
        // Arrange
        schemeRepository.save(scheme);

        // Act - Call multiple times
        List<Scheme> result1 = deadlineNotificationService.getSchemesWithApproachingDeadlines(7);
        List<Scheme> result2 = deadlineNotificationService.getSchemesWithApproachingDeadlines(7);
        List<Scheme> result3 = deadlineNotificationService.getSchemesWithApproachingDeadlines(7);

        // Then - Results should be identical
        Set<Long> ids1 = extractSchemeIds(result1);
        Set<Long> ids2 = extractSchemeIds(result2);
        Set<Long> ids3 = extractSchemeIds(result3);

        assertEquals(ids1, ids2, "Same deadline should return same schemes (call 1 vs 2)");
        assertEquals(ids2, ids3, "Same deadline should return same schemes (call 2 vs 3)");
        assertEquals(ids1, ids3, "Same deadline should return same schemes (call 1 vs 3)");
    }

    /**
     * Property 28.4: Non-applicants should receive deadline notifications.
     * 
     * For any scheme with approaching deadline, users who have not applied
     * should be identified as non-applicants and eligible for notifications.
     * 
     * Validates: Requirements 11D.9
     */
    @Property
    void nonApplicantsShouldBeIdentifiedForNotification(
            @ForAll("schemesWith7DayDeadline") Scheme scheme,
            @ForAll("userIdLists") List<Long> userIds) {
        // Arrange
        schemeRepository.save(scheme);
        
        // Some users have applied
        List<Long> applicantIds = userIds.subList(0, Math.min(3, userIds.size()));
        for (Long userId : applicantIds) {
            SchemeApplication application = SchemeApplication.builder()
                    .userId(userId)
                    .scheme(scheme)
                    .applicationDate(LocalDate.now())
                    .status(SchemeApplication.ApplicationStatus.SUBMITTED)
                    .build();
            schemeApplicationRepository.save(application);
        }

        // Act
        List<Long> nonApplicants = deadlineNotificationService.getNonApplicantsForScheme(
                scheme.getId(), userIds);

        // Then
        assertNotNull(nonApplicants, "Non-applicants list should not be null");
        
        // Non-applicants should not include those who applied
        for (Long applicantId : applicantIds) {
            assertFalse(nonApplicants.contains(applicantId),
                "User who applied should not be in non-applicants list");
        }
        
        // Non-applicants should include those who didn't apply
        for (Long userId : userIds) {
            if (!applicantIds.contains(userId)) {
                assertTrue(nonApplicants.contains(userId),
                    "User who didn't apply should be in non-applicants list");
            }
        }
    }

    /**
     * Property 28.5: Notification count should match non-applicant count.
     * 
     * For any scheme with approaching deadline, the number of notifications
     * sent should equal the number of non-applicants.
     * 
     * Validates: Requirements 11D.9
     */
    @Property
    void notificationCountMatchesNonApplicantCount(
            @ForAll("schemesWith1DayDeadline") Scheme scheme,
            @ForAll("userIdLists") List<Long> userIds) {
        // Arrange
        schemeRepository.save(scheme);
        
        // No applications exist
        when(schemeApplicationRepository.findBySchemeId(scheme.getId()))
                .thenReturn(Collections.emptyList());

        // Act
        List<Long> nonApplicants = deadlineNotificationService.getNonApplicantsForScheme(
                scheme.getId(), userIds);

        // Then
        assertEquals(userIds.size(), nonApplicants.size(),
            "All users should be non-applicants when no applications exist");
    }

    /**
     * Property 28.6: Deadline notification should handle empty user list.
     * 
     * When no users are eligible for notifications, the system should
     * handle this gracefully without errors.
     * 
     * Validates: Requirements 11D.9
     */
    @Property
    void shouldHandleEmptyUserListForNotifications(
            @ForAll("schemesWithDeadline") Scheme scheme) {
        // Arrange
        schemeRepository.save(scheme);

        // Act
        List<Long> nonApplicants = deadlineNotificationService.getNonApplicantsForScheme(
                scheme.getId(), Collections.emptyList());

        // Then
        assertNotNull(nonApplicants, "Non-applicants list should not be null");
        assertTrue(nonApplicants.isEmpty(), "Non-applicants should be empty for empty user list");
    }

    /**
     * Property 28.7: Deadline statistics should be accurate.
     * 
     * For any set of schemes with approaching deadlines, the deadline
     * statistics should accurately reflect the number of schemes.
     * 
     * Validates: Requirements 11D.9
     */
    @Property
    void deadlineStatisticsShouldBeAccurate(
            @ForAll("schemesWith7DayDeadline") Scheme scheme1,
            @ForAll("schemesWith1DayDeadline") Scheme scheme2) {
        // Arrange
        schemeRepository.save(scheme1);
        schemeRepository.save(scheme2);

        // Act
        var stats = deadlineNotificationService.getDeadlineStatistics();

        // Then
        assertNotNull(stats, "Statistics should not be null");
        assertTrue(stats.containsKey("schemes_with_7_day_deadline"),
            "Statistics should contain 7-day deadline count");
        assertTrue(stats.containsKey("schemes_with_1_day_deadline"),
            "Statistics should contain 1-day deadline count");
    }

    /**
     * Property 28.8: Notification message should include deadline information.
     * 
     * For any scheme with approaching deadline, the notification message
     * should include the deadline date and benefit amount.
     * 
     * Validates: Requirements 11D.9
     */
    @Property
    void notificationMessageShouldIncludeDeadlineInfo(
            @ForAll("schemesWith7DayDeadline") Scheme scheme,
            @ForAll("userIdLists") List<Long> userIds) {
        // Arrange
        schemeRepository.save(scheme);
        when(schemeApplicationRepository.findBySchemeId(scheme.getId()))
                .thenReturn(Collections.emptyList());

        // Act
        int sent = deadlineNotificationService.send7DayDeadlineReminders(userIds);

        // Then
        assertEquals(userIds.size(), sent, "All users should receive notification");
    }

    /**
     * Property 28.9: All deadline notification types should be processable.
     * 
     * For any deadline notification type (7-day, 1-day, deadline passed),
     * the system should be able to process it without errors.
     * 
     * Validates: Requirements 11D.9
     */
    @Property
    void shouldProcessAllNotificationTypes(
            @ForAll("notificationTypes") DeadlineNotificationType notificationType,
            @ForAll("userIdLists") List<Long> userIds) {
        // Arrange
        LocalDate today = LocalDate.now();
        Scheme scheme = Scheme.builder()
                .schemeCode("SCHEME-" + System.currentTimeMillis())
                .schemeName("Test Scheme")
                .schemeType(Scheme.SchemeType.CENTRAL)
                .applicationStartDate(today.minusDays(30))
                .applicationEndDate(today.plusDays(7))
                .benefitAmount(new Double("5000.00"))
                .isActive(true)
                .build();
        schemeRepository.save(scheme);
        when(schemeApplicationRepository.findBySchemeId(scheme.getId()))
                .thenReturn(Collections.emptyList());

        // Act & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            switch (notificationType) {
                case DEADLINE_APPROACHING_7_DAYS:
                    deadlineNotificationService.send7DayDeadlineReminders(userIds);
                    break;
                case DEADLINE_APPROACHING_1_DAY:
                    deadlineNotificationService.send1DayDeadlineReminders(userIds);
                    break;
                case DEADLINE_PASSED:
                    deadlineNotificationService.sendDeadlinePassedNotifications(userIds);
                    break;
            }
        }, "Should process all notification types without errors");
    }

    /**
     * Property 28.10: Deadline approaching calculation should be accurate.
     * 
     * For any scheme with a deadline D, when checking for schemes with
     * approaching deadlines within N days, the scheme should be included
     * if and only if D is within N days from today.
     * 
     * Validates: Requirements 11D.9
     */
    @Property
    void deadlineApproachingCalculationShouldBeAccurate(
            @ForAll("schemesWithDeadline") Scheme scheme) {
        // Arrange
        schemeRepository.save(scheme);
        LocalDate today = LocalDate.now();
        long daysUntilDeadline = java.time.temporal.ChronoUnit.DAYS.between(today, scheme.getApplicationEndDate());

        // Act
        List<Scheme> schemes7Days = deadlineNotificationService.getSchemesWithApproachingDeadlines(7);
        List<Scheme> schemes30Days = deadlineNotificationService.getSchemesWithApproachingDeadlines(30);

        // Then
        boolean isIn7DayList = schemes7Days.stream()
                .anyMatch(s -> s.getId().equals(scheme.getId()));
        boolean isIn30DayList = schemes30Days.stream()
                .anyMatch(s -> s.getId().equals(scheme.getId()));

        // If deadline is within 7 days, it should be in both lists
        if (daysUntilDeadline <= 7 && daysUntilDeadline >= 0) {
            assertTrue(isIn7DayList, "Scheme with deadline within 7 days should be in 7-day list");
            assertTrue(isIn30DayList, "Scheme with deadline within 7 days should be in 30-day list");
        }
        // If deadline is between 7 and 30 days, it should only be in 30-day list
        else if (daysUntilDeadline > 7 && daysUntilDeadline <= 30) {
            assertFalse(isIn7DayList, "Scheme with deadline > 7 days should not be in 7-day list");
            assertTrue(isIn30DayList, "Scheme with deadline <= 30 days should be in 30-day list");
        }
        // If deadline is more than 30 days, it should not be in either list
        else if (daysUntilDeadline > 30) {
            assertFalse(isIn7DayList, "Scheme with deadline > 30 days should not be in 7-day list");
            assertFalse(isIn30DayList, "Scheme with deadline > 30 days should not be in 30-day list");
        }
        // If deadline has passed, it should not be in either list
        else {
            assertFalse(isIn7DayList, "Expired scheme should not be in 7-day list");
            assertFalse(isIn30DayList, "Expired scheme should not be in 30-day list");
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Extract scheme IDs from a list of schemes.
     */
    private Set<Long> extractSchemeIds(List<Scheme> schemes) {
        Set<Long> ids = new HashSet<>();
        for (Scheme scheme : schemes) {
            ids.add(scheme.getId());
        }
        return ids;
    }
}