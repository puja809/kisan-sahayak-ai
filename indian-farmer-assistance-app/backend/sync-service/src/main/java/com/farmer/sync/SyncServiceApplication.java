package com.farmer.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sync Service Application
 * 
 * Provides offline capability and data synchronization functionality for the
 * Indian Farmer Assistance Application.
 * 
 * Features:
 * - Offline mode detection and management
 * - Request queuing for offline operations
 * - FIFO-based synchronization queue processing
 * - Conflict resolution by timestamp
 * - Retry logic with exponential backoff
 */
@SpringBootApplication
@EnableScheduling
public class SyncServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncServiceApplication.class, args);
    }
}