package com.farmer.admin.exception;

/**
 * Exception thrown when a document is not found.
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String message) {
        super(message);
    }

    public DocumentNotFoundException(String documentId, Throwable cause) {
        super("Document not found: " + documentId, cause);
    }
}