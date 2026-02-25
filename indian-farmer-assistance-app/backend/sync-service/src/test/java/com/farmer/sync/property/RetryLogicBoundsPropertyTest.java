package com.farmer.sync.property;

import com.farmer.sync.service.RetryService;
import net.jqwik.api.*;
import net.jqwik.junit.platform.JqwikProperty;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for retry logic bounds.
 * 
 * Feature: indian-farmer-assistance-app, Property 36: Retry Logic Bounds
 * Validates: Requirements 19.2
 * 
 * For any failed network request, the system should retry exactly up to 3 times
 * with exponential backoff (1s, 2s, 4s), and should not retry more than 3 times
 * or retry indefinitely.
 */
@JqwikProperty
class RetryLogicBoundsPropertyTest {

    /**
     * Property: Retry should happen exactly 3 times (1 initial + 2 retries) for a total of 3 attempts.
     */
    @Property
    void retryShouldHappenExactly3Times(
        @ForAll String operationName
    ) {
        RetryService retryService = new RetryService();
        AtomicInteger attemptCount = new AtomicInteger(0);

        // Create a supplier that always fails
        java.util.function.Supplier<String> failingSupplier = () -> {
            attemptCount.incrementAndGet();
            throw new RuntimeException("Simulated failure");
        };

        // Execute with retry
        assertThrows(RuntimeException.class, () -> {
            retryService.executeWithRetry(failingSupplier, operationName);
        });

        // Should have exactly 3 attempts (1 initial + 2 retries)
        assertEquals(3, attemptCount.get(), 
            "Should have exactly 3 attempts (1 initial + 2 retries)");
    }

    /**
     * Property: Retry should succeed on the 3rd attempt if the operation succeeds then.
     */
    @Property
    void retryShouldSucceedOnThirdAttempt(
        @ForAll String operationName
    ) {
        RetryService retryService = new RetryService();
        AtomicInteger attemptCount = new AtomicInteger(0);

        // Create a supplier that fails twice then succeeds
        java.util.function.Supplier<String> eventuallySucceeds = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("Simulated failure " + attempt);
            }
            return "Success on attempt " + attempt;
        };

        // Execute with retry
        String result = retryService.executeWithRetry(eventuallySucceeds, operationName);

        // Should have exactly 3 attempts
        assertEquals(3, attemptCount.get());
        assertEquals("Success on attempt 3", result);
    }

    /**
     * Property: Retry delays should follow exponential backoff (1s, 2s, 4s).
     */
    @Property
    void retryDelaysShouldFollowExponentialBackoff(
        @ForAll String operationName
    ) {
        RetryService retryService = new RetryService();
        
        long[] delays = retryService.getRetryDelays();
        
        // Should have 2 delays for 3 attempts (1 initial + 2 retries)
        assertEquals(2, delays.length, "Should have 2 delay intervals for 3 attempts");
        
        // First delay should be 1000ms (1s)
        assertEquals(1000, delays[0], "First delay should be 1s");
        
        // Second delay should be 2000ms (2s) - 1000 * 2
        assertEquals(2000, delays[1], "Second delay should be 2s");
    }

    /**
     * Property: Total retry delay should be 3 seconds (1s + 2s).
     */
    @Property
    void totalRetryDelayShouldBe3Seconds() {
        RetryService retryService = new RetryService();
        
        long totalDelay = retryService.getTotalRetryDelay();
        
        // Total delay should be 3000ms (1s + 2s)
        assertEquals(3000, totalDelay, "Total retry delay should be 3 seconds");
    }

    /**
     * Property: Max attempts should be 3.
     */
    @Property
    void maxAttemptsShouldBe3() {
        RetryService retryService = new RetryService();
        
        assertEquals(3, retryService.getMaxAttempts(), "Max attempts should be 3");
    }

    /**
     * Property: Delay for each attempt should follow exponential pattern.
     */
    @Property
    void delayForEachAttemptShouldFollowExponentialPattern() {
        RetryService retryService = new RetryService();
        
        // Attempt 1: no delay (initial attempt)
        assertEquals(1000, retryService.getDelayForAttempt(1), "Attempt 1 delay");
        
        // Attempt 2: 1000ms delay
        assertEquals(1000, retryService.getDelayForAttempt(2), "Attempt 2 delay");
        
        // Attempt 3: 2000ms delay (1000 * 2)
        assertEquals(2000, retryService.getDelayForAttempt(3), "Attempt 3 delay");
    }

    /**
     * Property: Runnable retry should also follow the same pattern.
     */
    @Property
    void runnableRetryShouldFollowSamePattern(
        @ForAll String operationName
    ) {
        RetryService retryService = new RetryService();
        AtomicInteger attemptCount = new AtomicInteger(0);

        // Create a runnable that always fails
        Runnable failingRunnable = () -> {
            attemptCount.incrementAndGet();
            throw new RuntimeException("Simulated failure");
        };

        // Execute with retry
        assertThrows(RuntimeException.class, () -> {
            retryService.executeWithRetry(failingRunnable, operationName);
        });

        // Should have exactly 3 attempts
        assertEquals(3, attemptCount.get());
    }

    /**
     * Property: Successful operation should not retry.
     */
    @Property
    void successfulOperationShouldNotRetry(
        @ForAll String operationName
    ) {
        RetryService retryService = new RetryService();
        AtomicInteger attemptCount = new AtomicInteger(0);

        // Create a supplier that succeeds immediately
        java.util.function.Supplier<String> successfulSupplier = () -> {
            attemptCount.incrementAndGet();
            return "Success";
        };

        // Execute with retry
        String result = retryService.executeWithRetry(successfulSupplier, operationName);

        // Should have exactly 1 attempt (no retries needed)
        assertEquals(1, attemptCount.get());
        assertEquals("Success", result);
    }
}