package com.farmer.scheme;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Scheme Service.
 * Handles government schemes catalog and application tracking.
 * 
 * Requirements: 4.1, 4.2, 11D.10
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

@OpenAPIDefinition(info = @Info(title = "Scheme Service API", version = "1.0.0", description = "Government schemes catalog and application tracking service", contact = @Contact(name = "Farmer Assistance Team", email = "support@farmer-assistance.in")))
public class SchemeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemeServiceApplication.class, args);
    }
}