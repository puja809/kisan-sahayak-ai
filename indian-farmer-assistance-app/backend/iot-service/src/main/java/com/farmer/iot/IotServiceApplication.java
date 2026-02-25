package com.farmer.iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IoT Device Management Service Application.
 * Provides device provisioning, sensor data collection, threshold monitoring,
 * and alert generation for farm IoT devices.
 */
@SpringBootApplication
@EnableScheduling
public class IotServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotServiceApplication.class, args);
    }
}