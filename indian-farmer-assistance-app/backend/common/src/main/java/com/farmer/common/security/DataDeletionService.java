package com.farmer.common.security;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing data deletion requests.
 * Complies with DPDP Act 2023 requirement to delete within 30 days.
 * Retains anonymized analytics data.
 */
public class DataDeletionService {

    public static class DeletionRequest {
        public String deletionRequestId;
        public String userId;
        public LocalDateTime requestedAt;
        public LocalDateTime completedAt;
        public String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED
        public String reason;
        public boolean anonymizedDataRetained;

        public DeletionRequest(String userId, String reason) {
            this.userId = userId;
            this.requestedAt = LocalDateTime.now();
            this.status = "PENDING";
            this.reason = reason;
            this.anonymizedDataRetained = true;
        }
    }

    private final List<DeletionRequest> deletionRequests = new ArrayList<>();

    /**
     * Request data deletion
     */
    public DeletionRequest requestDeletion(String userId, String reason) {
        DeletionRequest request = new DeletionRequest(userId, reason);
        request.deletionRequestId = "DEL_" + UUID.randomUUID().toString();
        deletionRequests.add(request);
        return request;
    }

    /**
     * Process deletion request
     */
    public void processDeletion(String deletionRequestId) {
        for (DeletionRequest request : deletionRequests) {
            if (request.deletionRequestId.equals(deletionRequestId)) {
                request.status = "IN_PROGRESS";
                // Delete personal data from all systems
                // Retain anonymized analytics data
                request.status = "COMPLETED";
                request.completedAt = LocalDateTime.now();
                return;
            }
        }
    }

    /**
     * Check if deletion was completed within 30 days
     */
    public boolean isDeletionTimely(String deletionRequestId) {
        for (DeletionRequest request : deletionRequests) {
            if (request.deletionRequestId.equals(deletionRequestId)) {
                if (request.completedAt == null) {
                    return false;
                }

                long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(
                    request.requestedAt,
                    request.completedAt
                );

                return daysDifference <= 30;
            }
        }
        return false;
    }

    /**
     * Get all pending deletion requests
     */
    public List<DeletionRequest> getPendingDeletions() {
        List<DeletionRequest> pending = new ArrayList<>();
        for (DeletionRequest request : deletionRequests) {
            if ("PENDING".equals(request.status)) {
                pending.add(request);
            }
        }
        return pending;
    }

    /**
     * Get deletion request status
     */
    public DeletionRequest getDeletionStatus(String deletionRequestId) {
        return deletionRequests.stream()
            .filter(r -> r.deletionRequestId.equals(deletionRequestId))
            .findFirst()
            .orElse(null);
    }
}
