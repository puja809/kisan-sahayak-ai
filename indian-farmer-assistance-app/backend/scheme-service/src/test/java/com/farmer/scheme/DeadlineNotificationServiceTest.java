package com.farmer.scheme;

import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.SchemeApplication;
import com.farmer.scheme.repository.SchemeApplicationRepository;
import com.farmer.scheme.repository.SchemeRepository;
import com.farmer.scheme.service.DeadlineNotificationService;
import com.farmer.scheme.service.DeadlineNotificationService.DeadlineNotificationType;
import com.farmer.scheme.service.NotificationService;
import com.farmer.scheme.service.NotificationService.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeadlineNotificationService.
 * Tests deadline notification logic for scheme applications.
 * 
 * Requirements: 4.8, 11D.9
 */
@ExtendWith(MockitoExtension.class)
class DeadlineNotificationServiceTest {

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private SchemeApplicationRepository schemeApplicationRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DeadlineNotificationService deadlineNotificationService;

    private Scheme centralScheme;
    private Scheme stateScheme;
    private List<Long> allUserIds;

    @BeforeEach
    void setUp() {
        // Set up central scheme with approaching deadline
        centralScheme = Scheme.builder()
                .id(1L)
                .schemeCode("PM-KISAN")
                .schemeName("PM-Kisan Scheme")
                .schemeType(Scheme.SchemeType.CENTRAL)
                .applicationStartDate(LocalDate.now().minusDays(30))
                .applicationEndDate(LocalDate.now().plusDays(7))
                .benefitAmount(new Double("6000"))
                .isActive(true)
                .build();

        // Set up state scheme with approaching deadline
        stateScheme = Scheme.builder()
                .id(2L)
                .schemeCode("KRISHI-BHAGYA-KA")
                .schemeName("Krishi Bhagya Karnataka")
                .schemeType(Scheme.SchemeType.STATE)
                .state("Karnataka")
                .applicationStartDate(LocalDate.now().minusDays(30))
                .applicationEndDate(LocalDate.now().plusDays(1))
                .benefitAmount(new Double("50000"))
                .isActive(true)
                .build();

        allUserIds = Arrays.asList(100L, 101L, 102L, 103L, 104L);
    }

    @Test
    @DisplayName("Should return schemes with approaching deadlines within specified days")
    void testGetSchemesWithApproachingDeadlines() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(centralScheme));

        // When
        List<Scheme> result = deadlineNotificationService.getSchemesWithApproachingDeadlines(7);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PM-Kisan Scheme", result.get(0).getSchemeName());
        verify(schemeRepository).findSchemesWithApproachingDeadlines(LocalDate.now(), LocalDate.now().plusDays(7));
    }

    @Test
    @DisplayName("Should return empty list when no schemes have approaching deadlines")
    void testGetSchemesWithApproachingDeadlines_NoSchemes() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<Scheme> result = deadlineNotificationService.getSchemesWithApproachingDeadlines(7);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return schemes with 7-day deadline")
    void testGetSchemesWith7DayDeadline() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), eq(LocalDate.now().plusDays(7))))
                .thenReturn(Arrays.asList(centralScheme));

        // When
        List<Scheme> result = deadlineNotificationService.getSchemesWith7DayDeadline();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PM-Kisan Scheme", result.get(0).getSchemeName());
    }

    @Test
    @DisplayName("Should return schemes with 1-day deadline")
    void testGetSchemesWith1DayDeadline() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), eq(LocalDate.now().plusDays(1))))
                .thenReturn(Arrays.asList(stateScheme));

        // When
        List<Scheme> result = deadlineNotificationService.getSchemesWith1DayDeadline();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Krishi Bhagya Karnataka", result.get(0).getSchemeName());
    }

    @Test
    @DisplayName("Should return non-applicants for a scheme")
    void testGetNonApplicantsForScheme() {
        // Given
        Long schemeId = 1L;
        List<SchemeApplication> applications = Arrays.asList(
                createApplication(100L, schemeId),
                createApplication(101L, schemeId)
        );
        when(schemeApplicationRepository.findBySchemeId(schemeId)).thenReturn(applications);

        // When
        List<Long> nonApplicants = deadlineNotificationService.getNonApplicantsForScheme(schemeId, allUserIds);

        // Then
        assertNotNull(nonApplicants);
        assertEquals(3, nonApplicants.size());
        assertTrue(nonApplicants.contains(102L));
        assertTrue(nonApplicants.contains(103L));
        assertTrue(nonApplicants.contains(104L));
        assertFalse(nonApplicants.contains(100L));
        assertFalse(nonApplicants.contains(101L));
    }

    @Test
    @DisplayName("Should return all users as non-applicants when no applications exist")
    void testGetNonApplicantsForScheme_NoApplications() {
        // Given
        Long schemeId = 1L;
        when(schemeApplicationRepository.findBySchemeId(schemeId)).thenReturn(Collections.emptyList());

        // When
        List<Long> nonApplicants = deadlineNotificationService.getNonApplicantsForScheme(schemeId, allUserIds);

        // Then
        assertNotNull(nonApplicants);
        assertEquals(5, nonApplicants.size());
        assertTrue(nonApplicants.containsAll(allUserIds));
    }

    @Test
    @DisplayName("Should send 7-day deadline reminders")
    void testSend7DayDeadlineReminders() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), eq(LocalDate.now().plusDays(7))))
                .thenReturn(Arrays.asList(centralScheme));
        when(schemeApplicationRepository.findBySchemeId(centralScheme.getId()))
                .thenReturn(Arrays.asList(createApplication(100L, centralScheme.getId())));

        // When
        int sent = deadlineNotificationService.send7DayDeadlineReminders(allUserIds);

        // Then
        assertEquals(4, sent); // 5 users - 1 applicant = 4 notifications
        verify(notificationService).sendBatchNotification(
                argThat(list -> list.size() == 4 && list.contains(101L) && list.contains(102L)),
                eq(NotificationType.STATUS_UPDATE),
                anyString()
        );
    }

    @Test
    @DisplayName("Should send 1-day deadline reminders")
    void testSend1DayDeadlineReminders() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), eq(LocalDate.now().plusDays(1))))
                .thenReturn(Arrays.asList(stateScheme));
        when(schemeApplicationRepository.findBySchemeId(stateScheme.getId()))
                .thenReturn(Collections.emptyList());

        // When
        int sent = deadlineNotificationService.send1DayDeadlineReminders(allUserIds);

        // Then
        assertEquals(5, sent); // All 5 users are non-applicants
        verify(notificationService).sendBatchNotification(
                eq(allUserIds),
                eq(NotificationType.STATUS_UPDATE),
                contains("tomorrow")
        );
    }

    @Test
    @DisplayName("Should not send notifications when all users have applied")
    void testSendDeadlineReminders_AllApplied() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(centralScheme));
        when(schemeApplicationRepository.findBySchemeId(centralScheme.getId()))
                .thenReturn(Arrays.asList(
                        createApplication(100L, centralScheme.getId()),
                        createApplication(101L, centralScheme.getId()),
                        createApplication(102L, centralScheme.getId()),
                        createApplication(103L, centralScheme.getId()),
                        createApplication(104L, centralScheme.getId())
                ));

        // When
        int sent = deadlineNotificationService.send7DayDeadlineReminders(allUserIds);

        // Then
        assertEquals(0, sent);
        verify(notificationService, never()).sendBatchNotification(anyList(), any(), anyString());
    }

    @Test
    @DisplayName("Should process all deadline notifications")
    void testProcessAllDeadlineNotifications() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), eq(LocalDate.now().plusDays(7))))
                .thenReturn(Arrays.asList(centralScheme));
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), eq(LocalDate.now().plusDays(1))))
                .thenReturn(Arrays.asList(stateScheme));
        when(schemeApplicationRepository.findBySchemeId(centralScheme.getId()))
                .thenReturn(Arrays.asList(createApplication(100L, centralScheme.getId())));
        when(schemeApplicationRepository.findBySchemeId(stateScheme.getId()))
                .thenReturn(Collections.emptyList());

        // When
        var results = deadlineNotificationService.processAllDeadlineNotifications(allUserIds);

        // Then
        assertNotNull(results);
        assertTrue(results.containsKey("7_day_reminders"));
        assertTrue(results.containsKey("1_day_reminders"));
    }

    @Test
    @DisplayName("Should return deadline statistics")
    void testGetDeadlineStatistics() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), eq(LocalDate.now().plusDays(7))))
                .thenReturn(Arrays.asList(centralScheme));
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), eq(LocalDate.now().plusDays(1))))
                .thenReturn(Arrays.asList(stateScheme));

        // When
        var stats = deadlineNotificationService.getDeadlineStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(1, stats.get("schemes_with_7_day_deadline"));
        assertEquals(1, stats.get("schemes_with_1_day_deadline"));
        assertEquals(2, stats.get("total_schemes_with_approaching_deadline"));
    }

    @Test
    @DisplayName("Should handle empty user list gracefully")
    void testSendDeadlineReminders_EmptyUserList() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(centralScheme));

        // When
        int sent = deadlineNotificationService.send7DayDeadlineReminders(Collections.emptyList());

        // Then
        assertEquals(0, sent);
        verify(notificationService, never()).sendBatchNotification(anyList(), any(), anyString());
    }

    @Test
    @DisplayName("Should handle multiple schemes with approaching deadlines")
    void testSendDeadlineReminders_MultipleSchemes() {
        // Given
        Scheme anotherScheme = Scheme.builder()
                .id(3L)
                .schemeCode("PMFBY")
                .schemeName("PM Fasal Bima Yojana")
                .schemeType(Scheme.SchemeType.CENTRAL)
                .applicationEndDate(LocalDate.now().plusDays(7))
                .benefitAmount(new Double("50000"))
                .isActive(true)
                .build();

        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), eq(LocalDate.now().plusDays(7))))
                .thenReturn(Arrays.asList(centralScheme, anotherScheme));
        when(schemeApplicationRepository.findBySchemeId(centralScheme.getId()))
                .thenReturn(Arrays.asList(createApplication(100L, centralScheme.getId())));
        when(schemeApplicationRepository.findBySchemeId(anotherScheme.getId()))
                .thenReturn(Collections.emptyList());

        // When
        int sent = deadlineNotificationService.send7DayDeadlineReminders(allUserIds);

        // Then
        assertEquals(9, sent); // 4 for central scheme + 5 for another scheme (one user applied to central)
        verify(notificationService, times(2)).sendBatchNotification(anyList(), any(), anyString());
    }

    @Test
    @DisplayName("Should include benefit amount in notification message")
    void testNotificationMessage_IncludesBenefitAmount() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), eq(LocalDate.now().plusDays(7))))
                .thenReturn(Arrays.asList(centralScheme));
        when(schemeApplicationRepository.findBySchemeId(centralScheme.getId()))
                .thenReturn(Collections.emptyList());

        // When
        deadlineNotificationService.send7DayDeadlineReminders(allUserIds);

        // Then
        verify(notificationService).sendBatchNotification(
                eq(allUserIds),
                eq(NotificationType.STATUS_UPDATE),
                contains("₹6000")
        );
    }

    @Test
    @DisplayName("Should include deadline date in notification message")
    void testNotificationMessage_IncludesDeadlineDate() {
        // Given
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), eq(LocalDate.now().plusDays(7))))
                .thenReturn(Arrays.asList(centralScheme));
        when(schemeApplicationRepository.findBySchemeId(centralScheme.getId()))
                .thenReturn(Collections.emptyList());

        // When
        deadlineNotificationService.send7DayDeadlineReminders(allUserIds);

        // Then
        verify(notificationService).sendBatchNotification(
                eq(allUserIds),
                eq(NotificationType.STATUS_UPDATE),
                contains(centralScheme.getApplicationEndDate().toString())
        );
    }

    /**
     * Helper method to create a SchemeApplication for testing.
     */
    private SchemeApplication createApplication(Long userId, Long schemeId) {
        return SchemeApplication.builder()
                .id(userId * 10 + schemeId)
                .userId(userId)
                .scheme(centralScheme)
                .applicationDate(LocalDate.now())
                .status(SchemeApplication.ApplicationStatus.SUBMITTED)
                .build();
    }
}