package com.farmer.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * OpenAPI/Swagger configuration for Indian Farmer Assistance Application
 * Provides centralized API documentation for all microservices
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Indian Farmer Assistance Application API")
                        .version("1.0.0")
                        .description("Comprehensive API documentation for the Indian Farmer Assistance Application. " +
                                "This platform provides farmers with real-time agricultural intelligence including " +
                                "weather forecasts, crop recommendations, government schemes, mandi prices, disease detection, " +
                                "and multilingual voice assistance.")
                        .contact(new Contact()
                                .name("Indian Farmer Assistance Team")
                                .email("support@farmerassistance.gov.in")
                                .url("https://farmerassistance.gov.in"))
                        .license(new License()
                                .name("Government of India License")
                                .url("https://data.gov.in/license")))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.farmerassistance.gov.in")
                                .description("Production Server"),
                        new Server()
                                .url("https://staging-api.farmerassistance.gov.in")
                                .description("Staging Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Bearer token for authentication")));
    }
}
