package com.farmer.crop.exception;

/**
 * Exception thrown when crop recommendation generation fails.
 * 
 * This exception is thrown when there are errors during the crop
 * recommendation process, such as:
 * - Missing required data
 * - Service unavailability
 * - Processing errors
 */
public class RecommendationException extends RuntimeException {

    /**
     * Creates a new RecommendationException with the specified message.
     * 
     * @param message Error message describing the failure
     */
    public RecommendationException(String message) {
        super(message);
    }

    /**
     * Creates a new RecommendationException with the specified message and cause.
     * 
     * @param message Error message describing the failure
     * @param cause The underlying cause of the exception
     */
    public RecommendationException(String message, Throwable cause) {
        super(message, cause);
    }
}