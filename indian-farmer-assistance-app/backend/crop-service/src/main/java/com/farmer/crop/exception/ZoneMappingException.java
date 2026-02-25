package com.farmer.crop.exception;

/**
 * Exception thrown when zone mapping fails.
 * 
 * Validates: Requirement 2.1
 */
public class ZoneMappingException extends RuntimeException {

    public ZoneMappingException(String message) {
        super(message);
    }

    public ZoneMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}