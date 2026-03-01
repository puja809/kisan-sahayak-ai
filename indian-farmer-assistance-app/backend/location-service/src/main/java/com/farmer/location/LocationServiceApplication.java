package com.farmer.location;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
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
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;

@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
@EnableScheduling
@EnableDiscoveryClient
@OpenAPIDefinition(info = @Info(title = "Location Service API", version = "1.0.0", description = "Location services with GPS, reverse geocoding, and government body locator", contact = @Contact(name = "Farmer Assistance Team", email = "support@farmer-assistance.in")))
public class LocationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocationServiceApplication.class, args);
    }
}