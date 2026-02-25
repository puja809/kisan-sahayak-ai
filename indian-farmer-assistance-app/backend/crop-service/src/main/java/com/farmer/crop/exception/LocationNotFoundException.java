package com.farmer.crop.exception;

/**
 * Exception thrown when a location cannot be found in the zone mapping database.
 * 
 * Validates: Requirement 2.1
 */
public class LocationNotFoundException extends ZoneMappingException {

    public LocationNotFoundException(String message) {
        super(message);
    }

    public LocationNotFoundException(String district, String state) {
        super(String.format("Location not found: district=%s, state=%s", district, state));
    }
}