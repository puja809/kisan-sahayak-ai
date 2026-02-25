package com.farmer.scheme.controller;

import com.farmer.scheme.dto.SchemeApplicationDTO;
import com.farmer.scheme.entity.SchemeApplication;
import com.farmer.scheme.entity.SchemeApplication.ApplicationStatus;
import com.farmer.scheme.service.SchemeApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for scheme application operations.
 * Requirements: 11D.10
 */
@RestController
@RequestMapping("/api/v1/schemes/applications")
@RequiredArgsConstructor
@Slf4j
public class SchemeApplicationController {

    private final SchemeApplicationService schemeApplicationService;

    /**
     * Get all applications for a user.
     * GET /api/v1/schemes/applications/user/{userId}
     * Requirements: 11D.10
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SchemeApplication>> getApplicationsByUserId(@PathVariable Long userId) {
        log.debug("GET /api/v1/schemes/applications/user/{} - Fetching applications for user", userId);
        List<SchemeApplication> applications = schemeApplicationService.getApplicationsByUserId(userId);
        return ResponseEntity.ok(applications);
    }

    /**
     * Get application by ID.
     * GET /api/v1/schemes/applications/{id}
     * Requirements: 11D.10
     */
    @GetMapping("/{id}")
    public ResponseEntity<SchemeApplication> getApplicationById(@PathVariable Long id) {
        log.debug("GET /api/v1/schemes/applications/{} - Fetching application by id", id);
        return schemeApplicationService.getApplicationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get applications by status.
     * GET /api/v1/schemes/applications/status/{status}
     * Requirements: 11D.10
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<SchemeApplication>> getApplicationsByStatus(@PathVariable ApplicationStatus status) {
        log.debug("GET /api/v1/schemes/applications/status/{} - Fetching applications by status", status);
        List<SchemeApplication> applications = schemeApplicationService.getApplicationsByStatus(status);
        return ResponseEntity.ok(applications);
    }

    /**
     * Get draft applications for a user.
     * GET /api/v1/schemes/applications/user/{userId}/draft
     * Requirements: 11D.10
     */
    @GetMapping("/user/{userId}/draft")
    public ResponseEntity<List<SchemeApplication>> getDraftApplicationsByUserId(@PathVariable Long userId) {
        log.debug("GET /api/v1/schemes/applications/user/{}/draft - Fetching draft applications", userId);
        List<SchemeApplication> applications = schemeApplicationService.getDraftApplicationsByUserId(userId);
        return ResponseEntity.ok(applications);
    }

    /**
     * Get all applications for a farmer.
     * GET /api/v1/schemes/applications/farmer/{farmerId}
     * Requirements: 11D.10
     */
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<SchemeApplication>> getApplicationsByFarmer(@PathVariable Long farmerId) {
        log.debug("GET /api/v1/schemes/applications/farmer/{} - Fetching applications for farmer", farmerId);
        List<SchemeApplication> applications = schemeApplicationService.getApplicationsByFarmer(farmerId);
        return ResponseEntity.ok(applications);
    }

    /**
     * Get current status of an application.
     * GET /api/v1/schemes/applications/{id}/status
     * Requirements: 11D.10
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> getApplicationStatus(@PathVariable Long id) {
        log.debug("GET /api/v1/schemes/applications/{}/status - Fetching application status", id);
        try {
            ApplicationStatus status = schemeApplicationService.getApplicationStatus(id);
            return ResponseEntity.ok(Map.of("status", status.name()));
        } catch (RuntimeException e) {
            log.error("Error fetching application status: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Submit a new application.
     * POST /api/v1/schemes/applications
     * Requirements: 11D.10
     */
    @PostMapping
    public ResponseEntity<SchemeApplication> submitApplication(@RequestBody SchemeApplicationDTO dto) {
        log.info("POST /api/v1/schemes/applications - Submitting application for user: {} and scheme: {}", 
                dto.getUserId(), dto.getSchemeId());
        
        try {
            SchemeApplication application = schemeApplicationService.submitApplication(dto);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            log.error("Error submitting application: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Save application as draft.
     * POST /api/v1/schemes/applications/draft
     * Requirements: 11D.10
     */
    @PostMapping("/draft")
    public ResponseEntity<SchemeApplication> saveDraft(@RequestBody SchemeApplicationDTO dto) {
        log.info("POST /api/v1/schemes/applications/draft - Saving draft for user: {} and scheme: {}", 
                dto.getUserId(), dto.getSchemeId());
        
        try {
            SchemeApplication application = schemeApplicationService.saveDraft(dto);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            log.error("Error saving draft: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Submit an application (change from DRAFT to SUBMITTED).
     * POST /api/v1/schemes/applications/{id}/submit
     * Requirements: 11D.10
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<SchemeApplication> submitApplication(@PathVariable Long id) {
        log.info("POST /api/v1/schemes/applications/{}/submit - Submitting application", id);
        try {
            SchemeApplication application = schemeApplicationService.submitApplication(id);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            log.error("Error submitting application: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update application status.
     * PUT /api/v1/schemes/applications/{id}/status
     * Requirements: 11D.10
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<SchemeApplication> updateApplicationStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, String> request) {
        String statusStr = request.get("status");
        log.info("PUT /api/v1/schemes/applications/{}/status - Updating status to: {}", id, statusStr);
        
        try {
            ApplicationStatus status = ApplicationStatus.valueOf(statusStr);
            SchemeApplication application = schemeApplicationService.updateApplicationStatus(id, status);
            return ResponseEntity.ok(application);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", statusStr);
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error updating application status: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update application documents.
     * PUT /api/v1/schemes/applications/{id}/documents
     * Requirements: 11D.10
     */
    @PutMapping("/{id}/documents")
    public ResponseEntity<SchemeApplication> updateApplicationDocuments(
            @PathVariable Long id, 
            @RequestBody Map<String, String> request) {
        String documents = request.get("documents");
        log.info("PUT /api/v1/schemes/applications/{}/documents - Updating documents", id);
        
        try {
            SchemeApplication application = schemeApplicationService.updateApplicationDocuments(id, documents);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            log.error("Error updating application documents: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update application remarks.
     * PUT /api/v1/schemes/applications/{id}/remarks
     * Requirements: 11D.10
     */
    @PutMapping("/{id}/remarks")
    public ResponseEntity<SchemeApplication> updateApplicationRemarks(
            @PathVariable Long id, 
            @RequestBody Map<String, String> request) {
        String remarks = request.get("remarks");
        log.info("PUT /api/v1/schemes/applications/{}/remarks - Updating remarks", id);
        
        try {
            SchemeApplication application = schemeApplicationService.updateApplicationRemarks(id, remarks);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            log.error("Error updating application remarks: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Add reviewer notes (for admin use).
     * PUT /api/v1/schemes/applications/{id}/review
     * Requirements: 11D.10
     */
    @PutMapping("/{id}/review")
    public ResponseEntity<SchemeApplication> addReviewerNotes(
            @PathVariable Long id, 
            @RequestBody Map<String, String> request) {
        String reviewerNotes = request.get("reviewerNotes");
        String statusStr = request.get("status");
        log.info("PUT /api/v1/schemes/applications/{}/review - Adding reviewer notes", id);
        
        try {
            ApplicationStatus status = ApplicationStatus.valueOf(statusStr);
            SchemeApplication application = schemeApplicationService.addReviewerNotes(id, reviewerNotes, status);
            return ResponseEntity.ok(application);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", statusStr);
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error adding reviewer notes: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get applications requiring deadline notifications.
     * GET /api/v1/schemes/applications/notifications?daysAhead=7
     * Requirements: 11D.9, 11D.10
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<SchemeApplication>> getApplicationsRequiringDeadlineNotification(
            @RequestParam(defaultValue = "7") int daysAhead) {
        log.debug("GET /api/v1/schemes/applications/notifications?daysAhead={} - Fetching applications requiring notification", daysAhead);
        List<SchemeApplication> applications = schemeApplicationService.getApplicationsRequiringDeadlineNotification(daysAhead);
        return ResponseEntity.ok(applications);
    }

    /**
     * Get approved but not disbursed applications.
     * GET /api/v1/schemes/applications/approved-not-disbursed
     * Requirements: 11D.10
     */
    @GetMapping("/approved-not-disbursed")
    public ResponseEntity<List<SchemeApplication>> getApprovedNotDisbursedApplications() {
        log.debug("GET /api/v1/schemes/applications/approved-not-disbursed - Fetching approved not disbursed applications");
        List<SchemeApplication> applications = schemeApplicationService.getApprovedNotDisbursedApplications();
        return ResponseEntity.ok(applications);
    }

    /**
     * Get application count by status.
     * GET /api/v1/schemes/applications/count/status/{status}
     * Requirements: 11D.10
     */
    @GetMapping("/count/status/{status}")
    public ResponseEntity<Map<String, Long>> getApplicationCountByStatus(@PathVariable ApplicationStatus status) {
        log.debug("GET /api/v1/schemes/applications/count/status/{} - Getting count by status", status);
        long count = schemeApplicationService.getApplicationCountByStatus(status);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Check if user has application for scheme.
     * GET /api/v1/schemes/applications/check?userId={userId}&schemeId={schemeId}
     * Requirements: 11D.10
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkApplicationExists(
            @RequestParam Long userId, 
            @RequestParam Long schemeId) {
        log.debug("GET /api/v1/schemes/applications/check?userId={}&schemeId={} - Checking if application exists", userId, schemeId);
        boolean exists = schemeApplicationService.hasApplicationForScheme(userId, schemeId);
        return ResponseEntity.ok(Map.of("hasApplication", exists));
    }
}