package com.farmer.yield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Yield Prediction Service Application.
 * 
 * Features:
 * - Yield estimation based on crop type, variety, sowing date, area, growth stage
 * - Historical yield data integration from farmer's past records
 * - Weather data integration (rainfall, temperature, extreme events)
 * - Soil health data integration (N, P, K, pH)
 * - Irrigation type and frequency consideration
 * - Pest/disease incident adjustment
 * - Yield prediction updates and notifications (>10% deviation)
 * - Financial projections based on current mandi prices
 * - Variance tracking and model improvement with ML
 * 
 * Validates: Requirements 11B.1, 11B.2, 11B.3, 11B.4, 11B.5, 11B.6, 11B.7, 11B.8, 11B.9, 11B.10
 */
@SpringBootApplication
@EnableTransactionManagement
public class YieldServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YieldServiceApplication.class, args);
    }
}