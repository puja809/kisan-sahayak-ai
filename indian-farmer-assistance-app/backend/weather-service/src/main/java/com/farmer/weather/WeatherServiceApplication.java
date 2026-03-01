package com.farmer.weather;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean;

/**
 * Weather Service Application.
 * Provides weather data integration with IMD API for farmer assistance.
 */
@SpringBootApplication

@OpenAPIDefinition(
    info = @Info(
        title = "Weather Service API",
        version = "1.0.0",
        description = "Weather service with IMD API integration for farmer assistance",
        contact = @Contact(name = "Farmer Assistance Team", email = "support@farmer-assistance.in")
    )
)
public class WeatherServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeatherServiceApplication.class, args);
    }
}