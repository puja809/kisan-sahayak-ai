package com.farmer.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Authentication Filter
 * Validates JWT tokens on all requests and extracts user information
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_USER_ROLE_HEADER = "X-User-Role";
    private static final String X_USER_EMAIL_HEADER = "X-User-Email";

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/health",
            "/actuator",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/swagger-config",
            "/swagger-resources",
            "/swagger-resources/configuration/ui",
            "/swagger-resources/configuration/security",
            "/webjars",
            "/favicon.ico",
            "/swagger-ui/",
            "/v3/",
            "/swagger-resources/",
            "/webjars/"
    );

    @Value("${jwt.secret:your-secret-key-change-in-production}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Skip authentication for public endpoints
            if (isPublicEndpoint(path)) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                logger.warn("Missing or invalid authorization header for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            try {
                String token = authHeader.substring(BEARER_PREFIX.length());
                Claims claims = validateToken(token);

                // Extract user information from token
                String userId = claims.getSubject();
                String userRole = claims.get("role", String.class);
                String userEmail = claims.get("email", String.class);

                // Propagate user context to downstream services
                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .header(X_USER_ID_HEADER, userId)
                                .header(X_USER_ROLE_HEADER, userRole != null ? userRole : "FARMER")
                                .header(X_USER_EMAIL_HEADER, userEmail != null ? userEmail : "")
                                .build())
                        .build();

                logger.debug("User {} authenticated successfully", userId);
                return chain.filter(modifiedExchange);

            } catch (Exception e) {
                logger.warn("Token validation failed: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    /**
     * Validate JWT token
     */
    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if endpoint is public (doesn't require authentication)
     */
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    public static class Config {
        // Configuration class for filter
    }
}
