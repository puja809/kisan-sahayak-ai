package com.farmer.scheme.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Security configuration for the scheme service.
 * Requirements: 22.3, 22.4, 22.5, 22.6
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Swagger/OpenAPI endpoints
                                                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs",
                                                                "/v3/api-docs/**",
                                                                "/swagger-resources", "/swagger-resources/**",
                                                                "/webjars/**")
                                                .permitAll()
                                                // Public endpoints for scheme browsing
                                                .requestMatchers("/api/v1/schemes").permitAll()
                                                // MCP SSE endpoints
                                                .requestMatchers("/sse/**", "/mcp/**").permitAll()
                                                .requestMatchers("/api/v1/schemes/**").permitAll()
                                                // Admin endpoints - auth is validated at API gateway level
                                                .requestMatchers("/api/v1/admin/**").permitAll()
                                                // All other endpoints require authentication
                                                .anyRequest().authenticated());

                return http.build();
        }

        /**
         * WebClient for inter-service communication.
         * Used to call user-service for farmer information.
         */
        @Bean
        public WebClient userServiceWebClient() {
                return WebClient.builder()
                                .baseUrl("http://localhost:8081")
                                .build();
        }
}
