package com.farmer.apigateway;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway Service - Main Application Entry Point
 * 
 * This service handles:
 * - Request routing and load balancing
 * - Authentication and authorization
 * - Rate limiting and throttling
 * - Request/response transformation
 */
@SpringBootApplication(scanBasePackages = {"com.farmer.apigateway", "com.farmer.common"})
@EnableDiscoveryClient
@OpenAPIDefinition(
    info = @Info(
        title = "API Gateway Service",
        version = "1.0.0",
        description = "Spring Cloud Gateway for routing and load balancing",
        contact = @Contact(name = "Farmer Assistance Team", email = "support@farmer-assistance.in")
    )
)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}