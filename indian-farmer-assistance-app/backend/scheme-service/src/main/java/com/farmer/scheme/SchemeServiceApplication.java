package com.farmer.scheme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Scheme Service.
 * Handles government schemes catalog and application tracking.
 * 
 * Requirements: 4.1, 4.2, 11D.10
 */
@SpringBootApplication
public class SchemeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemeServiceApplication.class, args);
    }
}