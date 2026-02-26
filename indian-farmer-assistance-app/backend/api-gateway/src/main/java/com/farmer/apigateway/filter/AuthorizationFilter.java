package com.farmer.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authorization Filter
 * Checks user roles for endpoint access and enforces role-based access control
 */
@Component
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);
    private static final String X_USER_ROLE_HEADER = "X-User-Role";
    private static final String X_USER_ID_HEADER = "X-User-Id";

    // Define role-based access control rules
    private static final Map<String, List<String>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        // Admin endpoints - only ADMIN role
        ROLE_PERMISSIONS.put("/api/v1/admin", Arrays.asList("ADMIN"));
        ROLE_PERMISSIONS.put("/api/v1/schemes", Arrays.asList("ADMIN", "FARMER"));

        // User endpoints - FARMER and ADMIN
        ROLE_PERMISSIONS.put("/api/v1/users", Arrays.asList("FARMER", "ADMIN"));

        // Weather endpoints - all authenticated users
        ROLE_PERMISSIONS.put("/api/v1/weather", Arrays.asList("FARMER", "ADMIN"));

        // Crop endpoints - all authenticated users
        ROLE_PERMISSIONS.put("/api/v1/crops", Arrays.asList("FARMER", "ADMIN"));

        // Mandi endpoints - all authenticated users
        ROLE_PERMISSIONS.put("/api/v1/mandi", Arrays.asList("FARMER", "ADMIN"));

        // IoT endpoints - all authenticated users
        ROLE_PERMISSIONS.put("/api/v1/iot", Arrays.asList("FARMER", "ADMIN"));

        // AI endpoints - all authenticated users
        ROLE_PERMISSIONS.put("/api/v1/ai", Arrays.asList("FARMER", "ADMIN"));
    }

    public AuthorizationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            
            // Allow Swagger and documentation endpoints without authorization
            if (isSwaggerEndpoint(path)) {
                return chain.filter(exchange);
            }
            
            String userRole = exchange.getRequest().getHeaders().getFirst(X_USER_ROLE_HEADER);
            String userId = exchange.getRequest().getHeaders().getFirst(X_USER_ID_HEADER);

            // Check if user has permission to access this endpoint
            if (!hasPermission(path, userRole)) {
                logger.warn("Unauthorized access attempt by user {} (role: {}) to path: {}", userId, userRole, path);
                logUnauthorizedAccess(userId, userRole, path);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            logger.debug("User {} (role: {}) authorized to access path: {}", userId, userRole, path);
            return chain.filter(exchange);
        };
    }

    /**
     * Check if path is a Swagger/documentation endpoint
     */
    private boolean isSwaggerEndpoint(String path) {
        return path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs") || 
               path.startsWith("/swagger-resources") || 
               path.startsWith("/webjars");
    }

    /**
     * Check if user role has permission to access the endpoint
     */
    private boolean hasPermission(String path, String userRole) {
        if (userRole == null || userRole.isEmpty()) {
            return false;
        }

        // Find matching endpoint pattern
        for (Map.Entry<String, List<String>> entry : ROLE_PERMISSIONS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue().contains(userRole);
            }
        }

        // If no specific rule found, deny access
        return false;
    }

    /**
     * Log unauthorized access attempts for audit trail
     */
    private void logUnauthorizedAccess(String userId, String userRole, String path) {
        logger.error("UNAUTHORIZED_ACCESS: userId={}, role={}, path={}, timestamp={}", 
                userId, userRole, path, System.currentTimeMillis());
    }

    public static class Config {
        // Configuration class for filter
    }
}
