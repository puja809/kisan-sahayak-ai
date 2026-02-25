package com.farmer.sync.exception;

/**
 * Exception for sync-related errors.
 */
public class SyncException extends RuntimeException {

    public SyncException(String message) {
        super(message);
    }

    public SyncException(String message, Throwable cause) {
        super(message, cause);
    }
}