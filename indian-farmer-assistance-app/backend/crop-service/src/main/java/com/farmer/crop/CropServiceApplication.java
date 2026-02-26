package com.farmer.crop;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Crop Service Application
 * 
 * Provides crop recommendations based on agro-ecological zones,
 * GAEZ v4 framework integration, and soil health data.
 * 
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10
 */
@SpringBootApplication
@EnableDiscoveryClient
@OpenAPIDefinition(
    info = @Info(
        title = "Crop Service API",
        version = "1.0.0",
        description = "Crop recommendation service with agro-ecological zone mapping and GAEZ integration",
        contact = @Contact(name = "Farmer Assistance Team", email = "support@farmer-assistance.in")
    )
)
public class CropServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CropServiceApplication.class, args);
    }
}