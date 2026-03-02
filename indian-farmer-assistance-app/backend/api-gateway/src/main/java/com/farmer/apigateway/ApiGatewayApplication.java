package com.farmer.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Service - Main Application Entry Point
 * 
 * This service handles:
 * - Request routing and load balancing
 * - Authentication and authorization
 * - Rate limiting and throttling
 * - Request/response transformation
 */
@SpringBootApplication(scanBasePackages = { "com.farmer.apigateway" })

public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}