package com.farmer.location.exception;

import lombok.Getter;

/**
 * Exception for location-related errors.
 */
@Getter
public class LocationException extends RuntimeException {

    private final String errorCode;

    public LocationException(String message) {
        super(message);
        this.errorCode = "LOCATION_ERROR";
    }

    public LocationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public LocationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "LOCATION_ERROR";
    }

    public LocationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}