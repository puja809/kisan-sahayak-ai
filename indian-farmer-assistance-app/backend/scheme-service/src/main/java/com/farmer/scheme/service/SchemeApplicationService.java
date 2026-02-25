package com.farmer.scheme.service;

import com.farmer.scheme.dto.SchemeApplicationDTO;
import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.SchemeApplication;
import com.farmer.scheme.entity.SchemeApplication.ApplicationStatus;
import com.farmer.scheme.repository.SchemeApplicationRepository;
import com.farmer.scheme.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing scheme applications.
 * Requirements: 11D.10
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SchemeApplicationService {

    private final SchemeApplicationRepository schemeApplicationRepository;
    private final SchemeRepository schemeRepository;
    private final NotificationService notificationService;

    /**
     * Valid status transitions for scheme applications.
     * Requirements: 11D.10
     */
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> VALID_TRANSITIONS = Map.of(
            ApplicationStatus.DRAFT, Set.of(ApplicationStatus.SUBMITTED),
            ApplicationStatus.SUBMITTED, Set.of(ApplicationStatus.UNDER_REVIEW),
            ApplicationStatus.UNDER_REVIEW, Set.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED),
            ApplicationStatus.APPROVED, Set.of(ApplicationStatus.DISBURSED)
    );

    /**
     * Get all applications for a user.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public List<SchemeApplication> getApplicationsByUserId(Long userId) {
        log.debug("Fetching applications for user: {}", userId);
        return schemeApplicationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get application by ID.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public Optional<SchemeApplication> getApplicationById(Long id) {
        log.debug("Fetching application with id: {}", id);
        return schemeApplicationRepository.findById(id);
    }

    /**
     * Get applications by status.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public List<SchemeApplication> getApplicationsByStatus(ApplicationStatus status) {
        log.debug("Fetching applications with status: {}", status);
        return schemeApplicationRepository.findByStatus(status);
    }

    /**
     * Get draft applications for a user.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public List<SchemeApplication> getDraftApplicationsByUserId(Long userId) {
        log.debug("Fetching draft applications for user: {}", userId);
        return schemeApplicationRepository.findDraftApplicationsByUserId(userId);
    }

    /**
     * Create a new scheme application.
     * Requirements: 11D.10
     */
    public SchemeApplication createApplication(Long userId, Long schemeId) {
        log.info("Creating new application for user: {} and scheme: {}", userId, schemeId);
        
        Scheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new RuntimeException("Scheme not found with id: " + schemeId));
        
        // Check if user already has an application for this scheme
        if (schemeApplicationRepository.existsByUserIdAndSchemeId(userId, schemeId)) {
            throw new RuntimeException("User already has an application for this scheme");
        }
        
        SchemeApplication application = SchemeApplication.builder()
                .userId(userId)
                .scheme(scheme)
                .applicationDate(LocalDate.now())
                .applicationNumber(generateApplicationNumber())
                .status(ApplicationStatus.DRAFT)
                .build();
        
        return schemeApplicationRepository.save(application);
    }

    /**
     * Update application status.
     * Requirements: 11D.10
     */
    public SchemeApplication updateApplicationStatus(Long applicationId, ApplicationStatus newStatus) {
        log.info("Updating application {} status to: {}", applicationId, newStatus);
        
        return schemeApplicationRepository.findById(applicationId)
                .map(application -> {
                    application.setStatus(newStatus);
                    
                    // Set timestamp for specific status changes
                    switch (newStatus) {
                        case SUBMITTED:
                            application.setSubmittedAt(LocalDateTime.now());
                            break;
                        case UNDER_REVIEW:
                            application.setReviewedAt(LocalDateTime.now());
                            break;
                        case DISBURSED:
                            application.setDisbursedAt(LocalDateTime.now());
                            break;
                        default:
                            break;
                    }
                    
                    return schemeApplicationRepository.save(application);
                })
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));
    }

    /**
     * Submit an application (change from DRAFT to SUBMITTED).
     * Requirements: 11D.10
     */
    public SchemeApplication submitApplication(Long applicationId) {
        log.info("Submitting application: {}", applicationId);
        return updateApplicationStatus(applicationId, ApplicationStatus.SUBMITTED);
    }

    /**
     * Update application documents.
     * Requirements: 11D.10
     */
    public SchemeApplication updateApplicationDocuments(Long applicationId, String documents) {
        log.info("Updating documents for application: {}", applicationId);
        
        return schemeApplicationRepository.findById(applicationId)
                .map(application -> {
                    application.setDocuments(documents);
                    return schemeApplicationRepository.save(application);
                })
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));
    }

    /**
     * Update application remarks.
     * Requirements: 11D.10
     */
    public SchemeApplication updateApplicationRemarks(Long applicationId, String remarks) {
        log.info("Updating remarks for application: {}", applicationId);
        
        return schemeApplicationRepository.findById(applicationId)
                .map(application -> {
                    application.setRemarks(remarks);
                    return schemeApplicationRepository.save(application);
                })
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));
    }

    /**
     * Add reviewer notes (for admin use).
     * Requirements: 11D.10
     */
    public SchemeApplication addReviewerNotes(Long applicationId, String reviewerNotes, ApplicationStatus status) {
        log.info("Adding reviewer notes for application: {}", applicationId);
        
        return schemeApplicationRepository.findById(applicationId)
                .map(application -> {
                    application.setReviewerNotes(reviewerNotes);
                    application.setStatus(status);
                    application.setReviewedAt(LocalDateTime.now());
                    return schemeApplicationRepository.save(application);
                })
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));
    }

    /**
     * Get applications requiring deadline notifications.
     * Requirements: 11D.9, 11D.10
     */
    @Transactional(readOnly = true)
    public List<SchemeApplication> getApplicationsRequiringDeadlineNotification(int daysAhead) {
        log.debug("Fetching applications requiring deadline notification within {} days", daysAhead);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysAhead);
        return schemeApplicationRepository.findApplicationsRequiringDeadlineNotification(startDate, endDate);
    }

    /**
     * Get approved but not disbursed applications.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public List<SchemeApplication> getApprovedNotDisbursedApplications() {
        log.debug("Fetching approved but not disbursed applications");
        return schemeApplicationRepository.findApprovedNotDisbursed();
    }

    /**
     * Get application count by status.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public long getApplicationCountByStatus(ApplicationStatus status) {
        return schemeApplicationRepository.countByStatus(status);
    }

    /**
     * Get application count by user and status.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public long getApplicationCountByUserAndStatus(Long userId, ApplicationStatus status) {
        return schemeApplicationRepository.countByUserIdAndStatus(userId, status);
    }

    /**
     * Get recent applications for a user.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public List<SchemeApplication> getRecentApplicationsByUserId(Long userId, int limit) {
        log.debug("Fetching {} recent applications for user: {}", limit, userId);
        return schemeApplicationRepository.findRecentApplicationsByUserId(userId, limit);
    }

    /**
     * Generate unique application number.
     */
    private String generateApplicationNumber() {
        return "SCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Validate status transition.
     * Requirements: 11D.10
     */
    private void validateStatusTransition(ApplicationStatus currentStatus, ApplicationStatus newStatus) {
        Set<ApplicationStatus> validNextStatuses = VALID_TRANSITIONS.get(currentStatus);
        if (validNextStatuses == null || !validNextStatuses.contains(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s. Valid transitions: %s",
                            currentStatus, newStatus, validNextStatuses));
        }
    }

    /**
     * Submit a new application using DTO.
     * Requirements: 11D.10
     */
    public SchemeApplication submitApplication(SchemeApplicationDTO dto) {
        log.info("Submitting application for user: {} and scheme: {}", dto.getUserId(), dto.getSchemeId());
        
        Scheme scheme = schemeRepository.findById(dto.getSchemeId())
                .orElseThrow(() -> new RuntimeException("Scheme not found with id: " + dto.getSchemeId()));
        
        // Check if user already has an application for this scheme
        if (schemeApplicationRepository.existsByUserIdAndSchemeId(dto.getUserId(), dto.getSchemeId())) {
            throw new RuntimeException("User already has an application for this scheme");
        }
        
        SchemeApplication application = SchemeApplication.builder()
                .userId(dto.getUserId())
                .scheme(scheme)
                .applicationDate(LocalDate.now())
                .applicationNumber(generateApplicationNumber())
                .status(ApplicationStatus.SUBMITTED)
                .remarks(dto.getRemarks())
                .documents(dto.getDocuments() != null ? dto.getDocuments().toString() : null)
                .submittedAt(LocalDateTime.now())
                .build();
        
        SchemeApplication saved = schemeApplicationRepository.save(application);
        
        // Send notification
        notificationService.sendNotification(saved.getUserId(), 
                NotificationService.NotificationType.APPLICATION_RECEIVED,
                "Your application for " + scheme.getSchemeName() + " has been received. Application Number: " + saved.getApplicationNumber());
        
        return saved;
    }

    /**
     * Save application as draft using DTO.
     * Requirements: 11D.10
     */
    public SchemeApplication saveDraft(SchemeApplicationDTO dto) {
        log.info("Saving draft for user: {} and scheme: {}", dto.getUserId(), dto.getSchemeId());
        
        Scheme scheme = schemeRepository.findById(dto.getSchemeId())
                .orElseThrow(() -> new RuntimeException("Scheme not found with id: " + dto.getSchemeId()));
        
        // Check if user already has an application for this scheme
        if (schemeApplicationRepository.existsByUserIdAndSchemeId(dto.getUserId(), dto.getSchemeId())) {
            throw new RuntimeException("User already has an application for this scheme");
        }
        
        SchemeApplication application = SchemeApplication.builder()
                .userId(dto.getUserId())
                .scheme(scheme)
                .applicationDate(LocalDate.now())
                .applicationNumber(generateApplicationNumber())
                .status(ApplicationStatus.DRAFT)
                .remarks(dto.getRemarks())
                .documents(dto.getDocuments() != null ? dto.getDocuments().toString() : null)
                .build();
        
        return schemeApplicationRepository.save(application);
    }

    /**
     * Get current status of an application.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public ApplicationStatus getApplicationStatus(Long applicationId) {
        return schemeApplicationRepository.findById(applicationId)
                .map(SchemeApplication::getStatus)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));
    }

    /**
     * Update application status with validation and notification.
     * Requirements: 11D.10
     */
    public SchemeApplication updateApplicationStatus(Long applicationId, String newStatusStr) {
        log.info("Updating application {} status to: {}", applicationId, newStatusStr);
        
        ApplicationStatus newStatus = ApplicationStatus.valueOf(newStatusStr);
        
        return schemeApplicationRepository.findById(applicationId)
                .map(application -> {
                    ApplicationStatus currentStatus = application.getStatus();
                    validateStatusTransition(currentStatus, newStatus);
                    
                    application.setStatus(newStatus);
                    
                    // Set timestamp for specific status changes
                    switch (newStatus) {
                        case SUBMITTED:
                            application.setSubmittedAt(LocalDateTime.now());
                            break;
                        case UNDER_REVIEW:
                            application.setReviewedAt(LocalDateTime.now());
                            break;
                        case DISBURSED:
                            application.setDisbursedAt(LocalDateTime.now());
                            break;
                        default:
                            break;
                    }
                    
                    SchemeApplication saved = schemeApplicationRepository.save(application);
                    
                    // Send status update notification
                    sendStatusNotification(application, newStatus);
                    
                    return saved;
                })
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));
    }

    /**
     * Send notification based on status change.
     * Requirements: 11D.10
     */
    private void sendStatusNotification(SchemeApplication application, ApplicationStatus newStatus) {
        String schemeName = application.getScheme().getSchemeName();
        String message;
        NotificationService.NotificationType type;
        
        switch (newStatus) {
            case SUBMITTED:
                message = "Your application for " + schemeName + " has been submitted successfully.";
                type = NotificationService.NotificationType.STATUS_UPDATE;
                break;
            case UNDER_REVIEW:
                message = "Your application for " + schemeName + " is now under review.";
                type = NotificationService.NotificationType.STATUS_UPDATE;
                break;
            case APPROVED:
                message = "Congratulations! Your application for " + schemeName + " has been approved.";
                type = NotificationService.NotificationType.APPROVAL;
                break;
            case REJECTED:
                message = "Your application for " + schemeName + " has been rejected. Please contact the scheme office for more details.";
                type = NotificationService.NotificationType.REJECTION;
                break;
            case DISBURSED:
                message = "The benefit for your " + schemeName + " application has been disbursed.";
                type = NotificationService.NotificationType.DISBURSEMENT;
                break;
            default:
                return;
        }
        
        notificationService.sendNotification(application.getUserId(), type, message);
    }

    /**
     * Get all applications for a farmer.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public List<SchemeApplication> getApplicationsByFarmer(Long farmerId) {
        log.debug("Fetching applications for farmer: {}", farmerId);
        return schemeApplicationRepository.findByUserIdOrderByCreatedAtDesc(farmerId);
    }

    /**
     * Get applications by status (admin).
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public List<SchemeApplication> getApplicationsByStatus(String statusStr) {
        ApplicationStatus status = ApplicationStatus.valueOf(statusStr);
        log.debug("Fetching applications with status: {}", status);
        return schemeApplicationRepository.findByStatus(status);
    }

    /**
     * Check if user has application for scheme.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public boolean hasApplicationForScheme(Long userId, Long schemeId) {
        return schemeApplicationRepository.existsByUserIdAndSchemeId(userId, schemeId);
    }

    /**
     * Get application by user and scheme.
     * Requirements: 11D.10
     */
    @Transactional(readOnly = true)
    public Optional<SchemeApplication> getApplicationByUserAndScheme(Long userId, Long schemeId) {
        return schemeApplicationRepository.findByUserIdAndSchemeId(userId, schemeId);
    }
}