package com.farmer.weather.exception;

/**
 * Exception thrown when IMD API rate limit is exceeded.
 */
public class RateLimitExceededException extends ImdApiException {

    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException(String message, String responseBody) {
        super(message + ": " + responseBody);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}