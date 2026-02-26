package com.farmer.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for data deletion service.
 * Tests data deletion within 30 days and anonymized data retention.
 * Requirements: 17.6
 */
public class DataDeletionServiceTest {

    private DataDeletionService deletionService;

    @BeforeEach
    public void setUp() {
        deletionService = new DataDeletionService();
    }

    @Test
    public void testRequestDeletion() {
        String userId = "farmer_001";
        String reason = "User requested account deletion";

        DataDeletionService.DeletionRequest request = deletionService.requestDeletion(userId, reason);

        assertNotNull(request, "Deletion request should be created");
        assertEquals(userId, request.userId, "User ID should match");
        assertEquals(reason, request.reason, "Reason should match");
        assertEquals("PENDING", request.status, "Initial status should be PENDING");
        assertNotNull(request.requestedAt, "Request timestamp should be set");
        assertNull(request.completedAt, "Completion timestamp should be null initially");
        assertTrue(request.anonymizedDataRetained, "Anonymized data should be retained");
    }

    @Test
    public void testProcessDeletion() {
        String userId = "farmer_001";
        DataDeletionService.DeletionRequest request = deletionService.requestDeletion(userId, "User requested");

        deletionService.processDeletion(request.deletionRequestId);

        DataDeletionService.DeletionRequest processedRequest = deletionService.getDeletionStatus(request.deletionRequestId);
        assertEquals("COMPLETED", processedRequest.status, "Status should be COMPLETED after processing");
        assertNotNull(processedRequest.completedAt, "Completion timestamp should be set");
    }

    @Test
    public void testDeletionTimeliness() {
        String userId = "farmer_001";
        DataDeletionService.DeletionRequest request = deletionService.requestDeletion(userId, "User requested");

        deletionService.processDeletion(request.deletionRequestId);

        assertTrue(deletionService.isDeletionTimely(request.deletionRequestId), "Deletion should be timely (within 30 days)");
    }

    @Test
    public void testGetPendingDeletions() {
        deletionService.requestDeletion("farmer_001", "Reason 1");
        deletionService.requestDeletion("farmer_002", "Reason 2");
        DataDeletionService.DeletionRequest request3 = deletionService.requestDeletion("farmer_003", "Reason 3");

        deletionService.processDeletion(request3.deletionRequestId);

        List<DataDeletionService.DeletionRequest> pending = deletionService.getPendingDeletions();
        assertEquals(2, pending.size(), "Should have 2 pending deletions");
    }

    @Test
    public void testGetDeletionStatus() {
        DataDeletionService.DeletionRequest request = deletionService.requestDeletion("farmer_001", "User requested");

        DataDeletionService.DeletionRequest status = deletionService.getDeletionStatus(request.deletionRequestId);
        assertNotNull(status, "Deletion status should be retrievable");
        assertEquals(request.deletionRequestId, status.deletionRequestId, "Deletion request ID should match");
    }

    @Test
    public void testGetNonExistentDeletionStatus() {
        DataDeletionService.DeletionRequest status = deletionService.getDeletionStatus("non_existent_id");
        assertNull(status, "Non-existent deletion request should return null");
    }

    @Test
    public void testAnonymizedDataRetention() {
        DataDeletionService.DeletionRequest request = deletionService.requestDeletion("farmer_001", "User requested");

        assertTrue(request.anonymizedDataRetained, "Anonymized data should be retained");

        deletionService.processDeletion(request.deletionRequestId);

        DataDeletionService.DeletionRequest processedRequest = deletionService.getDeletionStatus(request.deletionRequestId);
        assertTrue(processedRequest.anonymizedDataRetained, "Anonymized data should still be retained after deletion");
    }

    @Test
    public void testMultipleDeletionRequests() {
        DataDeletionService.DeletionRequest request1 = deletionService.requestDeletion("farmer_001", "Reason 1");
        DataDeletionService.DeletionRequest request2 = deletionService.requestDeletion("farmer_002", "Reason 2");
        DataDeletionService.DeletionRequest request3 = deletionService.requestDeletion("farmer_003", "Reason 3");

        assertEquals("PENDING", request1.status, "Request 1 should be pending");
        assertEquals("PENDING", request2.status, "Request 2 should be pending");
        assertEquals("PENDING", request3.status, "Request 3 should be pending");

        deletionService.processDeletion(request1.deletionRequestId);
        deletionService.processDeletion(request3.deletionRequestId);

        assertEquals("COMPLETED", deletionService.getDeletionStatus(request1.deletionRequestId).status, "Request 1 should be completed");
        assertEquals("PENDING", deletionService.getDeletionStatus(request2.deletionRequestId).status, "Request 2 should still be pending");
        assertEquals("COMPLETED", deletionService.getDeletionStatus(request3.deletionRequestId).status, "Request 3 should be completed");
    }

    @Test
    public void testDeletionRequestIdUniqueness() {
        DataDeletionService.DeletionRequest request1 = deletionService.requestDeletion("farmer_001", "Reason 1");
        DataDeletionService.DeletionRequest request2 = deletionService.requestDeletion("farmer_001", "Reason 2");

        assertNotEquals(request1.deletionRequestId, request2.deletionRequestId, "Deletion request IDs should be unique");
    }
}
