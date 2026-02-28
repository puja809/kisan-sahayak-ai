package com.farmer.mandi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient and RestTemplate beans.
 * 
 * Provides WebClient for making HTTP requests to external APIs
 * like data.gov.in and AGMARKNET, and RestTemplate for synchronous calls.
 */
@Configuration
public class WebClientConfig {

    /**
     * Create WebClient bean with default headers.
     * 
     * @return Configured WebClient
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Indian-Farmer-Assistance-App/1.0")
                .build();
    }
    
    /**
     * Create RestTemplate bean for synchronous HTTP calls.
     * 
     * @return Configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
