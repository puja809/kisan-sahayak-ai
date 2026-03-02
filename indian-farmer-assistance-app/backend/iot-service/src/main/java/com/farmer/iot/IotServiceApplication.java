package com.farmer.iot;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IoT Device Management Service Application.
 * Provides device provisioning, sensor data collection, threshold monitoring,
 * and alert generation for farm IoT devices.
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

@OpenAPIDefinition(info = @Info(title = "IoT Service API", version = "1.0.0", description = "IoT device management service for farm monitoring", contact = @Contact(name = "Farmer Assistance Team", email = "support@farmer-assistance.in")))
public class IotServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotServiceApplication.class, args);
    }
}