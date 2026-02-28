package com.farmer.crop.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for RestTemplate bean.
 * Used for making HTTP requests to external APIs like Kaegro.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Create a RestTemplate bean with timeout configuration.
     * 
     * @param builder RestTemplateBuilder for building the RestTemplate
     * @return Configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
