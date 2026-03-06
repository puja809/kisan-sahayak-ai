package com.farmer.user.config;

import com.farmer.user.security.CustomAccessDeniedHandler;
import com.farmer.user.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the user service.
 * Requirements: 11.6, 22.3, 22.4, 22.5, 22.6
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final CustomAccessDeniedHandler accessDeniedHandler;

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
                                                // Public endpoints
                                                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                                                .permitAll()
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers("/actuator/info").permitAll()
                                                // Admin endpoints require ADMIN role
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                                // Role management endpoints
                                                .requestMatchers("/api/v1/admin/roles/**").hasRole("ADMIN")
                                                // All other endpoints require authentication
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .exceptionHandling(exceptions -> exceptions
                                                .accessDeniedHandler(accessDeniedHandler));

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }
}
