package com.farmer.yield.exception;

/**
 * Custom exception for yield prediction service.
 */
public class YieldException extends RuntimeException {

    public YieldException(String message) {
        super(message);
    }

    public YieldException(String message, Throwable cause) {
        super(message, cause);
    }
}