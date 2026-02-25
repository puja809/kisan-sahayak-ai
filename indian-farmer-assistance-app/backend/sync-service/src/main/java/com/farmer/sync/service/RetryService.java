package com.farmer.sync.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Service for retry logic with exponential backoff.
 * 
 * Implements retry with exponential backoff (1s, 2s, 4s) up to 3 retries
 * as specified in Requirement 19.2.
 * 
 * Validates: Requirements 15.4, 19.2
 */
@Service
@Slf4j
public class RetryService {

    @Value("${app.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${app.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${app.retry.max-delay-ms:10000}")
    private long maxDelayMs;

    /**
     * Execute a supplier with retry and exponential backoff.
     * 
     * @param <T> Return type
     * @param supplier Supplier to execute
     * @param operationName Name of the operation for logging
     * @return Result of the supplier
     * @throws RuntimeException if all retries fail
     */
    public <T> T executeWithRetry(Supplier<T> supplier, String operationName) {
        int attempt = 0;
        long delay = initialDelayMs;

        while (true) {
            try {
                attempt++;
                log.debug("Attempt {} of {} for {}", attempt, maxAttempts, operationName);
                return supplier.get();
            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    log.error("All {} attempts failed for {}", maxAttempts, operationName, e);
                    throw new RuntimeException(
                        "Operation failed after " + maxAttempts + " attempts: " + operationName, e);
                }

                log.warn("Attempt {} failed for {}: {}", attempt, operationName, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }

                // Exponential backoff with cap
                delay = Math.min((long) (delay * multiplier), maxDelayMs);
            }
        }
    }

    /**
     * Execute a runnable with retry and exponential backoff.
     * 
     * @param runnable Runnable to execute
     * @param operationName Name of the operation for logging
     * @throws RuntimeException if all retries fail
     */
    public void executeWithRetry(Runnable runnable, String operationName) {
        int attempt = 0;
        long delay = initialDelayMs;

        while (true) {
            try {
                attempt++;
                log.debug("Attempt {} of {} for {}", attempt, maxAttempts, operationName);
                runnable.run();
                return;
            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    log.error("All {} attempts failed for {}", maxAttempts, operationName, e);
                    throw new RuntimeException(
                        "Operation failed after " + maxAttempts + " attempts: " + operationName, e);
                }

                log.warn("Attempt {} failed for {}: {}", attempt, operationName, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }

                // Exponential backoff with cap
                delay = Math.min((long) (delay * multiplier), maxDelayMs);
            }
        }
    }

    /**
     * Calculate the delay for a specific attempt number.
     * 
     * @param attemptNumber Attempt number (1-based)
     * @return Delay in milliseconds
     */
    public long getDelayForAttempt(int attemptNumber) {
        if (attemptNumber <= 1) {
            return initialDelayMs;
        }
        long delay = (long) (initialDelayMs * Math.pow(multiplier, attemptNumber - 1));
        return Math.min(delay, maxDelayMs);
    }

    /**
     * Get the total delay for all retries.
     * 
     * @return Total delay in milliseconds
     */
    public long getTotalRetryDelay() {
        long total = 0;
        long delay = initialDelayMs;
        for (int i = 1; i < maxAttempts; i++) {
            total += delay;
            delay = Math.min((long) (delay * multiplier), maxDelayMs);
        }
        return total;
    }

    /**
     * Get maximum number of retry attempts.
     * 
     * @return Max attempts
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Get the retry delays as an array.
     * 
     * @return Array of delays in milliseconds
     */
    public long[] getRetryDelays() {
        long[] delays = new long[maxAttempts - 1];
        long delay = initialDelayMs;
        for (int i = 0; i < delays.length; i++) {
            delays[i] = delay;
            delay = Math.min((long) (delay * multiplier), maxDelayMs);
        }
        return delays;
    }
}