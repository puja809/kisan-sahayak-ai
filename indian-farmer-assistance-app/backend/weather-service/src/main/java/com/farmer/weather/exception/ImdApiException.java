package com.farmer.weather.exception;

/**
 * Exception thrown when IMD API request fails.
 */
public class ImdApiException extends RuntimeException {

    private final int attemptCount;

    public ImdApiException(String message) {
        super(message);
        this.attemptCount = 1;
    }

    public ImdApiException(String message, int attemptCount) {
        super(message);
        this.attemptCount = attemptCount;
    }

    public ImdApiException(String message, Throwable cause) {
        super(message, cause);
        this.attemptCount = 1;
    }

    public ImdApiException(String message, Throwable cause, int attemptCount) {
        super(message, cause);
        this.attemptCount = attemptCount;
    }

    public int getAttemptCount() {
        return attemptCount;
    }
}