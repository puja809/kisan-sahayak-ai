package com.farmer.user;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main application class for the User Service.
 * Handles farmer authentication, profile management, and AgriStack integration.
 */
@SpringBootApplication
@EnableConfigurationProperties

@OpenAPIDefinition(
    info = @Info(
        title = "User Service API",
        version = "1.0.0",
        description = "User authentication and profile management service for Indian Farmer Assistance",
        contact = @Contact(name = "Farmer Assistance Team", email = "support@farmer-assistance.in")
    )
)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}