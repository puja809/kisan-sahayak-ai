package com.farmer.sync;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
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
@EnableDiscoveryClient
@OpenAPIDefinition(
    info = @Info(
        title = "Sync Service API",
        version = "1.0.0",
        description = "Offline capability and data synchronization service",
        contact = @Contact(name = "Farmer Assistance Team", email = "support@farmer-assistance.in")
    )
)
public class SyncServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncServiceApplication.class, args);
    }
}