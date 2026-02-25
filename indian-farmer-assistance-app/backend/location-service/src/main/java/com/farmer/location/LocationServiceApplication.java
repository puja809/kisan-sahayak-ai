package com.farmer.location;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Location Service Application for Indian Farmer Assistance.
 * Provides GPS location services, reverse geocoding, and government body locator.
 * 
 * Validates:
 * - Requirement 14.1: GPS coordinate retrieval
 * - Requirement 14.2: Location permission requests
 * - Requirement 14.3: Reverse geocoding to determine district, state, agro-ecological zone
 * - Requirement 14.4: Location change detection with >10km threshold
 * - Requirement 14.5: Fallback to network-based location or manual selection
 * - Requirement 7.1-7.7: Government body locator (KVK, district agriculture office, state department)
 */
@SpringBootApplication
@EnableScheduling
public class LocationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocationServiceApplication.class, args);
    }
}