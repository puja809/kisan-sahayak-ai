package com.farmer.scheme;

import com.farmer.scheme.dto.SchemeApplicationDTO;
import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.Scheme.SchemeType;
import com.farmer.scheme.entity.SchemeApplication;
import com.farmer.scheme.entity.SchemeApplication.ApplicationStatus;
import com.farmer.scheme.repository.SchemeApplicationRepository;
import com.farmer.scheme.repository.SchemeRepository;
import com.farmer.scheme.service.NotificationService;
import com.farmer.scheme.service.SchemeApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SchemeApplicationService.
 * Requirements: 11D.10
 */
@ExtendWith(MockitoExtension.class)
class SchemeApplicationServiceTest {

    @Mock
    private SchemeApplicationRepository schemeApplicationRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SchemeApplicationService schemeApplicationService;

    private Scheme testScheme;
    private SchemeApplication testApplication;

    @BeforeEach
    void setUp() {
        testScheme = Scheme.builder()
                .id(1L)
                .schemeCode("PM-KISAN-001")
                .schemeName("PM-Kisan Samman Nidhi")
                .schemeType(SchemeType.CENTRAL)
                .description("Income support of Rs. 6,000 per year")
                .benefitAmount(new BigDecimal("6000.00"))
                .applicationStartDate(LocalDate.now().minusDays(30))
                .applicationEndDate(LocalDate.now().plusDays(60))
                .isActive(true)
                .build();

        testApplication = SchemeApplication.builder()
                .id(1L)
                .userId(100L)
                .scheme(testScheme)
                .applicationDate(LocalDate.now())
                .applicationNumber("SCH-ABC12345")
                .status(ApplicationStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldGetApplicationsByUserId() {
        // Given
        List<SchemeApplication> applications = Arrays.asList(testApplication);
        when(schemeApplicationRepository.findByUserIdOrderByCreatedAtDesc(100L)).thenReturn(applications);

        // When
        List<SchemeApplication> result = schemeApplicationService.getApplicationsByUserId(100L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getUserId());
        verify(schemeApplicationRepository).findByUserIdOrderByCreatedAtDesc(100L);
    }

    @Test
    void shouldGetApplicationById() {
        // Given
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // When
        Optional<SchemeApplication> result = schemeApplicationService.getApplicationById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("SCH-ABC12345", result.get().getApplicationNumber());
        verify(schemeApplicationRepository).findById(1L);
    }

    @Test
    void shouldGetApplicationsByStatus() {
        // Given
        List<SchemeApplication> applications = Arrays.asList(testApplication);
        when(schemeApplicationRepository.findByStatus(ApplicationStatus.DRAFT)).thenReturn(applications);

        // When
        List<SchemeApplication> result = schemeApplicationService.getApplicationsByStatus(ApplicationStatus.DRAFT);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ApplicationStatus.DRAFT, result.get(0).getStatus());
        verify(schemeApplicationRepository).findByStatus(ApplicationStatus.DRAFT);
    }

    @Test
    void shouldGetDraftApplicationsByUserId() {
        // Given
        List<SchemeApplication> applications = Arrays.asList(testApplication);
        when(schemeApplicationRepository.findDraftApplicationsByUserId(100L)).thenReturn(applications);

        // When
        List<SchemeApplication> result = schemeApplicationService.getDraftApplicationsByUserId(100L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(schemeApplicationRepository).findDraftApplicationsByUserId(100L);
    }

    @Test
    void shouldCreateApplication() {
        // Given
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(testScheme));
        when(schemeApplicationRepository.existsByUserIdAndSchemeId(100L, 1L)).thenReturn(false);
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> {
            SchemeApplication app = invocation.getArgument(0);
            app.setId(2L);
            return app;
        });

        // When
        SchemeApplication result = schemeApplicationService.createApplication(100L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getUserId());
        assertEquals(ApplicationStatus.DRAFT, result.getStatus());
        assertNotNull(result.getApplicationNumber());
        verify(schemeRepository).findById(1L);
        verify(schemeApplicationRepository).save(any(SchemeApplication.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingDuplicateApplication() {
        // Given
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(testScheme));
        when(schemeApplicationRepository.existsByUserIdAndSchemeId(100L, 1L)).thenReturn(true);

        // When & Then
        assertThrows(RuntimeException.class, () -> schemeApplicationService.createApplication(100L, 1L));
        verify(schemeApplicationRepository, never()).save(any(SchemeApplication.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingApplicationForNonExistentScheme() {
        // Given
        when(schemeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> schemeApplicationService.createApplication(100L, 999L));
    }

    @Test
    void shouldSubmitApplication() {
        // Given
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SchemeApplication result = schemeApplicationService.submitApplication(1L);

        // Then
        assertNotNull(result);
        assertEquals(ApplicationStatus.SUBMITTED, result.getStatus());
        assertNotNull(result.getSubmittedAt());
        verify(schemeApplicationRepository).save(any(SchemeApplication.class));
    }

    @Test
    void shouldUpdateApplicationStatus() {
        // Given
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SchemeApplication result = schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.UNDER_REVIEW);

        // Then
        assertNotNull(result);
        assertEquals(ApplicationStatus.UNDER_REVIEW, result.getStatus());
        assertNotNull(result.getReviewedAt());
        verify(schemeApplicationRepository).save(any(SchemeApplication.class));
    }

    @Test
    void shouldSetDisbursedAtWhenStatusIsDisbursed() {
        // Given
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SchemeApplication result = schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.DISBURSED);

        // Then
        assertNotNull(result);
        assertEquals(ApplicationStatus.DISBURSED, result.getStatus());
        assertNotNull(result.getDisbursedAt());
        verify(schemeApplicationRepository).save(any(SchemeApplication.class));
    }

    @Test
    void shouldUpdateApplicationDocuments() {
        // Given
        String documents = "{\"aadhaar\": \"doc1.pdf\", \"land_records\": \"doc2.pdf\"}";
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SchemeApplication result = schemeApplicationService.updateApplicationDocuments(1L, documents);

        // Then
        assertNotNull(result);
        assertEquals(documents, result.getDocuments());
        verify(schemeApplicationRepository).save(any(SchemeApplication.class));
    }

    @Test
    void shouldUpdateApplicationRemarks() {
        // Given
        String remarks = "Application verified and complete";
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SchemeApplication result = schemeApplicationService.updateApplicationRemarks(1L, remarks);

        // Then
        assertNotNull(result);
        assertEquals(remarks, result.getRemarks());
        verify(schemeApplicationRepository).save(any(SchemeApplication.class));
    }

    @Test
    void shouldAddReviewerNotes() {
        // Given
        String reviewerNotes = "Application approved after verification";
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SchemeApplication result = schemeApplicationService.addReviewerNotes(1L, reviewerNotes, ApplicationStatus.APPROVED);

        // Then
        assertNotNull(result);
        assertEquals(reviewerNotes, result.getReviewerNotes());
        assertEquals(ApplicationStatus.APPROVED, result.getStatus());
        assertNotNull(result.getReviewedAt());
        verify(schemeApplicationRepository).save(any(SchemeApplication.class));
    }

    @Test
    void shouldGetApplicationsRequiringDeadlineNotification() {
        // Given
        List<SchemeApplication> applications = Arrays.asList(testApplication);
        when(schemeApplicationRepository.findApplicationsRequiringDeadlineNotification(any(), any()))
                .thenReturn(applications);

        // When
        List<SchemeApplication> result = schemeApplicationService.getApplicationsRequiringDeadlineNotification(7);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(schemeApplicationRepository).findApplicationsRequiringDeadlineNotification(any(), any());
    }

    @Test
    void shouldGetApprovedNotDisbursedApplications() {
        // Given
        testApplication.setStatus(ApplicationStatus.APPROVED);
        List<SchemeApplication> applications = Arrays.asList(testApplication);
        when(schemeApplicationRepository.findApprovedNotDisbursed()).thenReturn(applications);

        // When
        List<SchemeApplication> result = schemeApplicationService.getApprovedNotDisbursedApplications();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(schemeApplicationRepository).findApprovedNotDisbursed();
    }

    @Test
    void shouldGetApplicationCountByStatus() {
        // Given
        when(schemeApplicationRepository.countByStatus(ApplicationStatus.SUBMITTED)).thenReturn(10L);

        // When
        long result = schemeApplicationService.getApplicationCountByStatus(ApplicationStatus.SUBMITTED);

        // Then
        assertEquals(10L, result);
        verify(schemeApplicationRepository).countByStatus(ApplicationStatus.SUBMITTED);
    }

    @Test
    void shouldGetApplicationCountByUserAndStatus() {
        // Given
        when(schemeApplicationRepository.countByUserIdAndStatus(100L, ApplicationStatus.DRAFT)).thenReturn(2L);

        // When
        long result = schemeApplicationService.getApplicationCountByUserAndStatus(100L, ApplicationStatus.DRAFT);

        // Then
        assertEquals(2L, result);
        verify(schemeApplicationRepository).countByUserIdAndStatus(100L, ApplicationStatus.DRAFT);
    }

    @Test
    void shouldGetRecentApplicationsByUserId() {
        // Given
        List<SchemeApplication> applications = Arrays.asList(testApplication);
        when(schemeApplicationRepository.findRecentApplicationsByUserId(100L, 5)).thenReturn(applications);

        // When
        List<SchemeApplication> result = schemeApplicationService.getRecentApplicationsByUserId(100L, 5);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(schemeApplicationRepository).findRecentApplicationsByUserId(100L, 5);
    }

    @Test
    void shouldCheckIfUserHasApplicationForScheme() {
        // Given
        when(schemeApplicationRepository.existsByUserIdAndSchemeId(100L, 1L)).thenReturn(true);

        // When
        boolean result = schemeApplicationService.hasApplicationForScheme(100L, 1L);

        // Then
        assertTrue(result);
        verify(schemeApplicationRepository).existsByUserIdAndSchemeId(100L, 1L);
    }

    @Test
    void shouldGetApplicationByUserAndScheme() {
        // Given
        when(schemeApplicationRepository.findByUserIdAndSchemeId(100L, 1L)).thenReturn(Optional.of(testApplication));

        // When
        Optional<SchemeApplication> result = schemeApplicationService.getApplicationByUserAndScheme(100L, 1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(100L, result.get().getUserId());
        verify(schemeApplicationRepository).findByUserIdAndSchemeId(100L, 1L);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentApplication() {
        // Given
        when(schemeApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> schemeApplicationService.updateApplicationStatus(999L, ApplicationStatus.SUBMITTED));
        verify(schemeApplicationRepository).findById(999L);
    }

    // ==================== Tests for DTO-based methods ====================

    @Test
    void shouldSubmitApplicationWithDTO() {
        // Given
        SchemeApplicationDTO dto = SchemeApplicationDTO.builder()
                .userId(100L)
                .schemeId(1L)
                .remarks("Test application")
                .build();

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(testScheme));
        when(schemeApplicationRepository.existsByUserIdAndSchemeId(100L, 1L)).thenReturn(false);
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> {
            SchemeApplication app = invocation.getArgument(0);
            app.setId(2L);
            return app;
        });

        // When
        SchemeApplication result = schemeApplicationService.submitApplication(dto);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getUserId());
        assertEquals(ApplicationStatus.SUBMITTED, result.getStatus());
        assertNotNull(result.getSubmittedAt());
        verify(schemeRepository).findById(1L);
        verify(schemeApplicationRepository).save(any(SchemeApplication.class));
    }

    @Test
    void shouldSaveDraftWithDTO() {
        // Given
        SchemeApplicationDTO dto = SchemeApplicationDTO.builder()
                .userId(100L)
                .schemeId(1L)
                .remarks("Draft application")
                .build();

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(testScheme));
        when(schemeApplicationRepository.existsByUserIdAndSchemeId(100L, 1L)).thenReturn(false);
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> {
            SchemeApplication app = invocation.getArgument(0);
            app.setId(2L);
            return app;
        });

        // When
        SchemeApplication result = schemeApplicationService.saveDraft(dto);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getUserId());
        assertEquals(ApplicationStatus.DRAFT, result.getStatus());
        verify(schemeRepository).findById(1L);
        verify(schemeApplicationRepository).save(any(SchemeApplication.class));
    }

    @Test
    void shouldGetApplicationStatus() {
        // Given
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // When
        ApplicationStatus status = schemeApplicationService.getApplicationStatus(1L);

        // Then
        assertEquals(ApplicationStatus.DRAFT, status);
        verify(schemeApplicationRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenGettingStatusOfNonExistentApplication() {
        // Given
        when(schemeApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> schemeApplicationService.getApplicationStatus(999L));
        verify(schemeApplicationRepository).findById(999L);
    }

    @Test
    void shouldGetApplicationsByFarmer() {
        // Given
        List<SchemeApplication> applications = Arrays.asList(testApplication);
        when(schemeApplicationRepository.findByUserIdOrderByCreatedAtDesc(100L)).thenReturn(applications);

        // When
        List<SchemeApplication> result = schemeApplicationService.getApplicationsByFarmer(100L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getUserId());
        verify(schemeApplicationRepository).findByUserIdOrderByCreatedAtDesc(100L);
    }

    @Test
    void shouldGetApplicationsByStatusString() {
        // Given
        List<SchemeApplication> applications = Arrays.asList(testApplication);
        when(schemeApplicationRepository.findByStatus(ApplicationStatus.DRAFT)).thenReturn(applications);

        // When
        List<SchemeApplication> result = schemeApplicationService.getApplicationsByStatus("DRAFT");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ApplicationStatus.DRAFT, result.get(0).getStatus());
        verify(schemeApplicationRepository).findByStatus(ApplicationStatus.DRAFT);
    }

    // ==================== Tests for status transition validation ====================

    @Test
    void shouldRejectInvalidStatusTransitionFromDraft() {
        // Given
        testApplication.setStatus(ApplicationStatus.DRAFT);
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // When & Then - Cannot go directly from DRAFT to APPROVED
        assertThrows(IllegalStateException.class, 
                () -> schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.APPROVED.name()));
    }

    @Test
    void shouldRejectInvalidStatusTransitionFromSubmitted() {
        // Given
        testApplication.setStatus(ApplicationStatus.SUBMITTED);
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // When & Then - Cannot go directly from SUBMITTED to DISBURSED
        assertThrows(IllegalStateException.class, 
                () -> schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.DISBURSED.name()));
    }

    @Test
    void shouldRejectInvalidStatusTransitionFromUnderReview() {
        // Given
        testApplication.setStatus(ApplicationStatus.UNDER_REVIEW);
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // When & Then - Cannot go directly from UNDER_REVIEW to DISBURSED
        assertThrows(IllegalStateException.class, 
                () -> schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.DISBURSED.name()));
    }

    @Test
    void shouldRejectInvalidStatusTransitionFromRejected() {
        // Given
        testApplication.setStatus(ApplicationStatus.REJECTED);
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // When & Then - Cannot transition from REJECTED
        assertThrows(IllegalStateException.class, 
                () -> schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.APPROVED.name()));
    }

    @Test
    void shouldRejectInvalidStatusTransitionFromDisbursed() {
        // Given
        testApplication.setStatus(ApplicationStatus.DISBURSED);
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // When & Then - Cannot transition from DISBURSED
        assertThrows(IllegalStateException.class, 
                () -> schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.UNDER_REVIEW.name()));
    }

    @Test
    void shouldAllowValidStatusTransitionFromDraftToSubmitted() {
        // Given
        testApplication.setStatus(ApplicationStatus.DRAFT);
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SchemeApplication result = schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.SUBMITTED.name());

        // Then
        assertNotNull(result);
        assertEquals(ApplicationStatus.SUBMITTED, result.getStatus());
        assertNotNull(result.getSubmittedAt());
    }

    @Test
    void shouldAllowValidStatusTransitionFromSubmittedToUnderReview() {
        // Given
        testApplication.setStatus(ApplicationStatus.SUBMITTED);
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SchemeApplication result = schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.UNDER_REVIEW.name());

        // Then
        assertNotNull(result);
        assertEquals(ApplicationStatus.UNDER_REVIEW, result.getStatus());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    void shouldAllowValidStatusTransitionFromUnderReviewToApproved() {
        // Given
        testApplication.setStatus(ApplicationStatus.UNDER_REVIEW);
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SchemeApplication result = schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.APPROVED.name());

        // Then
        assertNotNull(result);
        assertEquals(ApplicationStatus.APPROVED, result.getStatus());
    }

    @Test
    void shouldAllowValidStatusTransitionFromUnderReviewToRejected() {
        // Given
        testApplication.setStatus(ApplicationStatus.UNDER_REVIEW);
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SchemeApplication result = schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.REJECTED.name());

        // Then
        assertNotNull(result);
        assertEquals(ApplicationStatus.REJECTED, result.getStatus());
    }

    @Test
    void shouldAllowValidStatusTransitionFromApprovedToDisbursed() {
        // Given
        testApplication.setStatus(ApplicationStatus.APPROVED);
        when(schemeApplicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(schemeApplicationRepository.save(any(SchemeApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SchemeApplication result = schemeApplicationService.updateApplicationStatus(1L, ApplicationStatus.DISBURSED.name());

        // Then
        assertNotNull(result);
        assertEquals(ApplicationStatus.DISBURSED, result.getStatus());
        assertNotNull(result.getDisbursedAt());
    }
}