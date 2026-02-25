package com.farmer.mandi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Mandi Price Service.
 * Provides AGMARKNET integration for agricultural market prices.
 */
@SpringBootApplication
@EnableScheduling
public class MandiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MandiServiceApplication.class, args);
    }
}