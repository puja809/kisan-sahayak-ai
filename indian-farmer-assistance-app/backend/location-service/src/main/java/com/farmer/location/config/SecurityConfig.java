package com.farmer.location.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
                                "/swagger-resources", "/swagger-resources/**", "/webjars/**")
                        .permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/sse/**", "/mcp/**").permitAll()
                        .requestMatchers("/api/v1/government-bodies/search",
                                "/api/v1/government-bodies/state/**",
                                "/api/v1/government-bodies/district/**",
                                "/api/v1/government-bodies/state/*/district/**")
                        .permitAll()
                        .requestMatchers("/api/v1/government-bodies/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated());

        return http.build();
    }
}
