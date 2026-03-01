package com.farmer.mandi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Mandi Price Service.
 * Provides AGMARKNET integration for agricultural market prices.
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

@OpenAPIDefinition(info = @Info(title = "Mandi Service API", version = "1.0.0", description = "Mandi price service with AGMARKNET integration for farmer assistance", contact = @Contact(name = "Farmer Assistance Team", email = "support@farmer-assistance.in")))
public class MandiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MandiServiceApplication.class, args);
    }
}