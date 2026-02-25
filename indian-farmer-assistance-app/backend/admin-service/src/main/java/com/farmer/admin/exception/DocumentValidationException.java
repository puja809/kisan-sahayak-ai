package com.farmer.admin.exception;

/**
 * Exception thrown when document validation fails.
 * Requirements: 21.2
 */
public class DocumentValidationException extends RuntimeException {

    public DocumentValidationException(String message) {
        super(message);
    }

    public DocumentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}