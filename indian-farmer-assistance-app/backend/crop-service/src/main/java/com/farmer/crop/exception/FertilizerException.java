package com.farmer.crop.exception;

/**
 * Exception for fertilizer service errors.
 */
public class FertilizerException extends RuntimeException {

    public FertilizerException(String message) {
        super(message);
    }

    public FertilizerException(String message, Throwable cause) {
        super(message, cause);
    }
}