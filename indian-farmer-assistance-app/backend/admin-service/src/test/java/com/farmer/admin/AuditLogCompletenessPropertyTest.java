package com.farmer.admin;

import com.farmer.admin.entity.AuditLog;
import com.farmer.admin.repository.AuditLogRepository;
import com.farmer.admin.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import net.jqwik.junit5.JqwikTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for audit log completeness.
 * Validates: Requirements 21.11, 22.7
 * 
 * Property 42: Audit Log Completeness
 * For any administrative action (document upload, scheme creation/update/deletion, 
 * user role modification), the system should create an audit log entry containing 
 * timestamp, admin identifier, action type, entity type, entity ID, old value, and new value.
 */
@ExtendWith(MockitoExtension.class)
@JqwikTest
class AuditLogCompletenessPropertyTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private AuditService auditService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        auditService = new AuditService(auditLogRepository, objectMapper);
    }

    // ==================== GENERATORS ====================

    /**
     * Generator for valid action types.
     */
    @Provide
    Arbitrary<String> actionTypes() {
        return Arbitraries.of(
            "DOCUMENT_UPLOAD", "DOCUMENT_UPDATE", "DOCUMENT_DELETE", "DOCUMENT_RESTORE",
            "SCHEME_CREATE", "SCHEME_UPDATE", "SCHEME_DELETE", "SCHEME_ACTIVATE", "SCHEME_DEACTIVATE",
            "USER_ROLE_CHANGE", "USER_CREATE", "USER_UPDATE", "USER_DELETE",
            "SYSTEM_CONFIG_CHANGE", "DATA_EXPORT", "DATA_IMPORT"
        );
    }

    /**
     * Generator for entity types.
     */
    @Provide
    Arbitrary<String> entityTypes() {
        return Arbitraries.of("DOCUMENT", "SCHEME", "USER", "SYSTEM", "DATA");
    }

    /**
     * Generator for admin IDs.
     */
    @Provide
    Arbitrary<String> adminIds() {
        return Arbitraries.strings().numeric().ofMinLength(1).ofMaxLength(20);
    }

    /**
     * Generator for entity IDs.
     */
    @Provide
    Arbitrary<String> entityIds() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(50);
    }

    /**
     * Generator for user IDs.
     */
    @Provide
    Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1, 100000);
    }

    /**
     * Generator for action statuses.
     */
    @Provide
    Arbitrary<String> actionStatuses() {
        return Arbitraries.of("SUCCESS", "FAILED");
    }

    /**
     * Generator for error messages.
     */
    @Provide
    Arbitrary<String> errorMessages() {
        return Arbitraries.of(
            "Invalid input", "Permission denied", "Resource not found", 
            "Database error", "Network timeout", "Validation failed"
        );
    }

    // ==================== PROPERTY TESTS ====================

    /**
     * Property 42.1: Document actions should create complete audit logs.
     * 
     * For any document action (upload, update, delete, restore), the system
     * should create an audit log with all required fields.
     * 
     * Validates: Requirements 21.11
     */
    @Property
    void documentActionsShouldCreateCompleteAuditLogs(
            @ForAll("actionTypes") String action,
            @ForAll("entityIds") String entityId,
            @ForAll("adminIds") String adminId) {
        // Arrange
        if (!action.startsWith("DOCUMENT_")) return;
        
        Document testDocument = Document.builder()
                .id("test-id")
                .documentId(entityId)
                .title("Test Document")
                .category("schemes")
                .build();

        // Act
        auditService.logDocumentAction(action, entityId, null, testDocument, adminId);

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertNotNull(savedLog, "Audit log should not be null");
        assertEquals(action, savedLog.getAction(), "Action should match");
        assertEquals("DOCUMENT", savedLog.getEntityType(), "Entity type should be DOCUMENT");
        assertEquals(entityId, savedLog.getEntityId(), "Entity ID should match");
        assertNotNull(savedLog.getTimestamp(), "Timestamp should not be null");
        assertEquals("SUCCESS", savedLog.getStatus(), "Status should be SUCCESS");
        assertNotNull(savedLog.getNewValue(), "New value should not be null");
    }

    /**
     * Property 42.2: Scheme actions should create complete audit logs.
     * 
     * For any scheme action (create, update, delete, activate, deactivate),
     * the system should create an audit log with all required fields.
     * 
     * Validates: Requirements 21.11
     */
    @Property
    void schemeActionsShouldCreateCompleteAuditLogs(
            @ForAll("actionTypes") String action,
            @ForAll("userIds") Long entityId,
            @ForAll("adminIds") String adminId) {
        // Arrange
        if (!action.startsWith("SCHEME_")) return;
        
        Scheme testScheme = Scheme.builder()
                .id(entityId)
                .schemeCode("TEST-001")
                .schemeName("Test Scheme")
                .build();

        // Act
        auditService.logSchemeAction(action, entityId, null, testScheme, adminId);

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertNotNull(savedLog, "Audit log should not be null");
        assertEquals(action, savedLog.getAction(), "Action should match");
        assertEquals("SCHEME", savedLog.getEntityType(), "Entity type should be SCHEME");
        assertEquals(entityId.toString(), savedLog.getEntityId(), "Entity ID should match");
        assertNotNull(savedLog.getTimestamp(), "Timestamp should not be null");
        assertEquals("SUCCESS", savedLog.getStatus(), "Status should be SUCCESS");
        assertNotNull(savedLog.getNewValue(), "New value should not be null");
    }

    /**
     * Property 42.3: Generic actions should create complete audit logs.
     * 
     * For any administrative action, the system should create an audit log
     * with all required fields including user information.
     * 
     * Validates: Requirements 21.11, 22.7
     */
    @Property
    void genericActionsShouldCreateCompleteAuditLogs(
            @ForAll("actionTypes") String action,
            @ForAll("entityTypes") String entityType,
            @ForAll("entityIds") String entityId,
            @ForAll("userIds") Long userId,
            @ForAll("adminIds") String adminId) {
        // Arrange
        Object oldValue = new TestEntity("old-" + entityId);
        Object newValue = new TestEntity("new-" + entityId);

        // Act
        auditService.logAction(action, entityType, entityId, oldValue, newValue, userId, adminId);

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertNotNull(savedLog, "Audit log should not be null");
        assertEquals(action, savedLog.getAction(), "Action should match");
        assertEquals(entityType, savedLog.getEntityType(), "Entity type should match");
        assertEquals(entityId, savedLog.getEntityId(), "Entity ID should match");
        assertEquals(userId, savedLog.getUserId(), "User ID should match");
        assertEquals(adminId, savedLog.getUserName(), "User name should match");
        assertNotNull(savedLog.getTimestamp(), "Timestamp should not be null");
        assertEquals("SUCCESS", savedLog.getStatus(), "Status should be SUCCESS");
        assertNotNull(savedLog.getOldValue(), "Old value should not be null");
        assertNotNull(savedLog.getNewValue(), "New value should not be null");
    }

    /**
     * Property 42.4: Failed actions should create complete audit logs.
     * 
     * For any failed action, the system should create an audit log with
     * the error message and FAILED status.
     * 
     * Validates: Requirements 21.11
     */
    @Property
    void failedActionsShouldCreateCompleteAuditLogs(
            @ForAll("actionTypes") String action,
            @ForAll("entityTypes") String entityType,
            @ForAll("entityIds") String entityId,
            @ForAll("userIds") Long userId,
            @ForAll("errorMessages") String errorMessage) {
        // Arrange
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditService.logFailedAction(action, entityType, entityId, errorMessage, userId);

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertNotNull(savedLog, "Audit log should not be null");
        assertEquals(action, savedLog.getAction(), "Action should match");
        assertEquals(entityType, savedLog.getEntityType(), "Entity type should match");
        assertEquals(entityId, savedLog.getEntityId(), "Entity ID should match");
        assertEquals(userId, savedLog.getUserId(), "User ID should match");
        assertNotNull(savedLog.getTimestamp(), "Timestamp should not be null");
        assertEquals("FAILED", savedLog.getStatus(), "Status should be FAILED");
        assertEquals(errorMessage, savedLog.getErrorMessage(), "Error message should match");
    }

    /**
     * Property 42.5: Audit log timestamp should be recent.
     * 
     * For any audit log created, the timestamp should be within a few
     * seconds of the current time.
     * 
     * Validates: Requirements 21.11
     */
    @Property
    void auditLogTimestampShouldBeRecent(
            @ForAll("actionTypes") String action,
            @ForAll("entityTypes") String entityType,
            @ForAll("entityIds") String entityId,
            @ForAll("userIds") Long userId) {
        // Arrange
        LocalDateTime before = LocalDateTime.now();

        // Act
        auditService.logAction(action, entityType, entityId, null, null, userId, "admin");

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        LocalDateTime after = LocalDateTime.now();
        assertNotNull(savedLog.getTimestamp(), "Timestamp should not be null");
        assertTrue(!savedLog.getTimestamp().isBefore(before), 
            "Timestamp should not be before the start of the test");
        assertTrue(!savedLog.getTimestamp().isAfter(after), 
            "Timestamp should not be after the end of the test");
    }

    /**
     * Property 42.6: Audit log creation is deterministic.
     * 
     * For the same action with the same inputs, the audit log should
     * contain the same values (except for timestamp).
     * 
     * Validates: Requirements 21.11
     */
    @Property
    void auditLogCreationIsDeterministic(
            @ForAll("actionTypes") String action,
            @ForAll("entityTypes") String entityType,
            @ForAll("entityIds") String entityId,
            @ForAll("userIds") Long userId,
            @ForAll("adminIds") String adminId) {
        // Arrange
        Object oldValue = new TestEntity("old");
        Object newValue = new TestEntity("new");

        // Act - First call
        auditService.logAction(action, entityType, entityId, oldValue, newValue, userId, adminId);
        AuditLog firstLog = auditLogCaptor.getValue();

        // Reset mock
        reset(auditLogRepository);

        // Act - Second call with same inputs
        auditService.logAction(action, entityType, entityId, oldValue, newValue, userId, adminId);
        AuditLog secondLog = auditLogCaptor.getValue();

        // Then - Compare non-timestamp fields
        assertEquals(firstLog.getAction(), secondLog.getAction(), "Actions should match");
        assertEquals(firstLog.getEntityType(), secondLog.getEntityType(), "Entity types should match");
        assertEquals(firstLog.getEntityId(), secondLog.getEntityId(), "Entity IDs should match");
        assertEquals(firstLog.getUserId(), secondLog.getUserId(), "User IDs should match");
        assertEquals(firstLog.getUserName(), secondLog.getUserName(), "User names should match");
        assertEquals(firstLog.getOldValue(), secondLog.getOldValue(), "Old values should match");
        assertEquals(firstLog.getNewValue(), secondLog.getNewValue(), "New values should match");
    }

    /**
     * Property 42.7: All action types should create audit logs.
     * 
     * For each defined action type, the system should be able to create
     * an audit log without errors.
     * 
     * Validates: Requirements 21.11, 22.7
     */
    @Property
    void allActionTypesShouldCreateAuditLogs(
            @ForAll("actionTypes") String action,
            @ForAll("entityTypes") String entityType,
            @ForAll("entityIds") String entityId,
            @ForAll("userIds") Long userId,
            @ForAll("adminIds") String adminId) {
        // Arrange & Act & Then
        assertDoesNotThrow(() -> {
            auditService.logAction(action, entityType, entityId, null, null, userId, adminId);
        }, "All action types should create audit logs without errors");
    }

    /**
     * Property 42.8: Audit log should handle null old and new values.
     * 
     * For actions with null old or new values, the system should still
     * create a valid audit log.
     * 
     * Validates: Requirements 21.11
     */
    @Property
    void auditLogShouldHandleNullValues(
            @ForAll("actionTypes") String action,
            @ForAll("entityTypes") String entityType,
            @ForAll("entityIds") String entityId,
            @ForAll("userIds") Long userId) {
        // Arrange & Act
        auditService.logAction(action, entityType, entityId, null, null, userId, "admin");

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        assertNotNull(savedLog, "Audit log should not be null");
        assertEquals(action, savedLog.getAction(), "Action should match");
        assertEquals(entityType, savedLog.getEntityType(), "Entity type should match");
        assertEquals(entityId, savedLog.getEntityId(), "Entity ID should match");
        assertNotNull(savedLog.getTimestamp(), "Timestamp should not be null");
    }

    /**
     * Property 42.9: Multiple audit logs can be created in sequence.
     * 
     * The system should be able to create multiple audit logs in sequence
     * without any issues.
     * 
     * Validates: Requirements 21.11
     */
    @Property
    void multipleAuditLogsCanBeCreated(@ForAll("actionTypes") String action) {
        // Arrange
        int count = 10;
        List<AuditLog> logs = new ArrayList<>();
        when(auditLogRepository.save(any())).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            logs.add(log);
            return log;
        });

        // Act
        for (int i = 0; i < count; i++) {
            auditService.logAction(action, "TEST", "id-" + i, null, null, (long) i, "admin-" + i);
        }

        // Then
        assertEquals(count, logs.size(), "Should create " + count + " audit logs");
        for (int i = 0; i < count; i++) {
            assertNotNull(logs.get(i).getTimestamp(), "Log " + i + " should have timestamp");
            assertEquals("id-" + i, logs.get(i).getEntityId(), "Log " + i + " should have correct entity ID");
        }
    }

    /**
     * Property 42.10: Audit log should contain all required metadata.
     * 
     * For any audit log created, it should contain all required metadata
     * fields as specified in the requirements.
     * 
     * Validates: Requirements 21.11, 22.7
     */
    @Property
    void auditLogShouldContainAllRequiredMetadata(
            @ForAll("actionTypes") String action,
            @ForAll("entityTypes") String entityType,
            @ForAll("entityIds") String entityId,
            @ForAll("userIds") Long userId,
            @ForAll("adminIds") String adminId) {
        // Arrange
        Object oldValue = new TestEntity("old");
        Object newValue = new TestEntity("new");

        // Act
        auditService.logAction(action, entityType, entityId, oldValue, newValue, userId, adminId);

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();

        // Verify all required fields are present
        assertNotNull(savedLog.getTimestamp(), "Timestamp is required");
        assertNotNull(savedLog.getAction(), "Action is required");
        assertNotNull(savedLog.getEntityType(), "Entity type is required");
        assertNotNull(savedLog.getEntityId(), "Entity ID is required");
        assertNotNull(savedLog.getStatus(), "Status is required");
    }

    // ==================== HELPER CLASSES ====================

    /**
     * Simple test entity for audit log values.
     */
    private static class TestEntity {
        private String id;
        private String name;

        public TestEntity(String id) {
            this.id = id;
            this.name = "Test-" + id;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Simple scheme entity for testing.
     */
    private static class Scheme {
        private Long id;
        private String schemeCode;
        private String schemeName;

        public static Scheme builder() {
            return new Scheme();
        }

        public Scheme id(Long id) {
            this.id = id;
            return this;
        }

        public Scheme schemeCode(String code) {
            this.schemeCode = code;
            return this;
        }

        public Scheme schemeName(String name) {
            this.schemeName = name;
            return this;
        }

        public Long getId() {
            return id;
        }

        public String getSchemeCode() {
            return schemeCode;
        }

        public String getSchemeName() {
            return schemeName;
        }
    }
}