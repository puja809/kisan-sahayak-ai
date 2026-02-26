package com.farmer.common.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for error handling service.
 * Tests user-friendly error messages, retry logic, and error reporting.
 * Requirements: 19.1, 19.2, 19.3, 19.5, 19.6
 */
public class ErrorHandlingServiceTest {

    private ErrorHandlingService errorService;

    @BeforeEach
    public void setUp() {
        errorService = new ErrorHandlingService();
    }

    @Test
    public void testLogError() {
        String errorMessage = "Database connection failed";
        String userFriendlyMessage = "Unable to connect to server. Please check your internet connection.";
        String language = "en";

        ErrorHandlingService.ErrorLog log = errorService.logError(errorMessage, userFriendlyMessage, language);

        assertNotNull(log, "Error log should be created");
        assertEquals(errorMessage, log.errorMessage, "Error message should match");
        assertEquals(userFriendlyMessage, log.userFriendlyMessage, "User-friendly message should match");
        assertEquals(language, log.language, "Language should match");
        assertEquals("PENDING", log.status, "Initial status should be PENDING");
    }

    @Test
    public void testLogErrorWithStackTrace() {
        String errorMessage = "NullPointerException";
        String userFriendlyMessage = "An unexpected error occurred.";
        String stackTrace = "java.lang.NullPointerException at com.farmer.service.UserService.getUser()";

        ErrorHandlingService.ErrorLog log = errorService.logErrorWithStackTrace(errorMessage, userFriendlyMessage, "en", stackTrace);

        assertNotNull(log.stackTrace, "Stack trace should be recorded");
        assertEquals(stackTrace, log.stackTrace, "Stack trace should match");
    }

    @Test
    public void testGetUserFriendlyMessage() {
        ErrorHandlingService.ErrorLog log = errorService.logError("Technical error", "User-friendly message", "en");
        String message = errorService.getUserFriendlyMessage(log.errorId);

        assertEquals("User-friendly message", message, "User-friendly message should be retrieved");
    }

    @Test
    public void testGetUserFriendlyMessageForNonExistentError() {
        String message = errorService.getUserFriendlyMessage("non_existent_error");
        assertEquals("An error occurred. Please try again.", message, "Default message should be returned");
    }

    @Test
    public void testShouldRetry() {
        ErrorHandlingService.ErrorLog log = errorService.logError("Error", "User message", "en");

        assertTrue(errorService.shouldRetry(log.errorId), "Should retry initially");

        errorService.recordRetryAttempt(log.errorId);
        errorService.recordRetryAttempt(log.errorId);
        errorService.recordRetryAttempt(log.errorId);

        assertFalse(errorService.shouldRetry(log.errorId), "Should not retry after max attempts");
    }

    @Test
    public void testGetNextRetryDelay() {
        ErrorHandlingService.ErrorLog log = errorService.logError("Error", "User message", "en");

        assertEquals(1000, errorService.getNextRetryDelay(log.errorId), "First retry delay should be 1 second");

        errorService.recordRetryAttempt(log.errorId);
        assertEquals(2000, errorService.getNextRetryDelay(log.errorId), "Second retry delay should be 2 seconds");

        errorService.recordRetryAttempt(log.errorId);
        assertEquals(4000, errorService.getNextRetryDelay(log.errorId), "Third retry delay should be 4 seconds");

        errorService.recordRetryAttempt(log.errorId);
        assertEquals(-1, errorService.getNextRetryDelay(log.errorId), "No more retries after max attempts");
    }

    @Test
    public void testRecordRetryAttempt() {
        ErrorHandlingService.ErrorLog log = errorService.logError("Error", "User message", "en");

        assertEquals(0, log.retryCount, "Initial retry count should be 0");

        errorService.recordRetryAttempt(log.errorId);
        assertEquals(1, log.retryCount, "Retry count should be 1");

        errorService.recordRetryAttempt(log.errorId);
        assertEquals(2, log.retryCount, "Retry count should be 2");
    }

    @Test
    public void testMarkResolved() {
        ErrorHandlingService.ErrorLog log = errorService.logError("Error", "User message", "en");
        errorService.markResolved(log.errorId);

        assertEquals("RESOLVED", log.status, "Status should be RESOLVED");
    }

    @Test
    public void testMarkFailed() {
        ErrorHandlingService.ErrorLog log = errorService.logError("Error", "User message", "en");
        errorService.markFailed(log.errorId);

        assertEquals("FAILED", log.status, "Status should be FAILED");
    }

    @Test
    public void testGetErrorLogsByLanguage() {
        errorService.logError("Error 1", "Message 1", "en");
        errorService.logError("Error 2", "Message 2", "hi");
        errorService.logError("Error 3", "Message 3", "en");

        List<ErrorHandlingService.ErrorLog> englishLogs = errorService.getErrorLogsByLanguage("en");
        assertEquals(2, englishLogs.size(), "Should have 2 English error logs");

        List<ErrorHandlingService.ErrorLog> hindiLogs = errorService.getErrorLogsByLanguage("hi");
        assertEquals(1, hindiLogs.size(), "Should have 1 Hindi error log");
    }

    @Test
    public void testGetRecentErrors() {
        errorService.logError("Error 1", "Message 1", "en");
        errorService.logError("Error 2", "Message 2", "en");
        errorService.logError("Error 3", "Message 3", "en");
        errorService.logError("Error 4", "Message 4", "en");

        List<ErrorHandlingService.ErrorLog> recent = errorService.getRecentErrors(2);
        assertEquals(2, recent.size(), "Should return 2 recent errors");
    }

    @Test
    public void testClearOldErrorLogs() {
        ErrorHandlingService.ErrorLog log = errorService.logError("Error", "Message", "en");
        log.timestamp = System.currentTimeMillis() - 100000; // 100 seconds ago

        errorService.clearOldErrorLogs(60000); // Clear logs older than 60 seconds

        assertTrue(errorService.getAllErrorLogs().isEmpty() || 
                   errorService.getAllErrorLogs().stream().noneMatch(l -> l.errorId.equals(log.errorId)),
                   "Old error logs should be cleared");
    }

    @Test
    public void testMultipleErrors() {
        ErrorHandlingService.ErrorLog log1 = errorService.logError("Error 1", "Message 1", "en");
        ErrorHandlingService.ErrorLog log2 = errorService.logError("Error 2", "Message 2", "en");
        ErrorHandlingService.ErrorLog log3 = errorService.logError("Error 3", "Message 3", "en");

        assertEquals(3, errorService.getAllErrorLogs().size(), "Should have 3 error logs");
    }

    @Test
    public void testRetryExponentialBackoff() {
        ErrorHandlingService.ErrorLog log = errorService.logError("Error", "Message", "en");

        long delay1 = errorService.getNextRetryDelay(log.errorId);
        errorService.recordRetryAttempt(log.errorId);

        long delay2 = errorService.getNextRetryDelay(log.errorId);
        errorService.recordRetryAttempt(log.errorId);

        long delay3 = errorService.getNextRetryDelay(log.errorId);

        assertEquals(1000, delay1, "First delay should be 1 second");
        assertEquals(2000, delay2, "Second delay should be 2 seconds");
        assertEquals(4000, delay3, "Third delay should be 4 seconds");
        assertTrue(delay2 > delay1 && delay3 > delay2, "Delays should increase exponentially");
    }
}
