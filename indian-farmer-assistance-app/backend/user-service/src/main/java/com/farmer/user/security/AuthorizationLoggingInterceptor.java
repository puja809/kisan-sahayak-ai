package com.farmer.user.security;

import com.farmer.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interceptor for logging authorization attempts and enforcing role-based access.
 * Requirements: 22.3, 22.4, 22.5, 22.6
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationLoggingInterceptor implements HandlerInterceptor {

    private static final Set<String> FARMER_ENDPOINTS = Set.of(
            "/api/v1/weather", "/api/v1/crops", "/api/v1/schemes",
            "/api/v1/mandi", "/api/v1/ai/voice", "/api/v1/ai/disease",
            "/api/v1/iot"
    );

    private static final Set<String> ADMIN_ENDPOINTS = Set.of(
            "/api/v1/admin", "/api/v1/admin/roles"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String userId = authentication.getName();
            String userRole = getUserRole(authentication);
            String requestPath = request.getRequestURI();
            String requestMethod = request.getMethod();

            // Log the access attempt
            log.debug("Access attempt: user={}, role={}, path={}, method={}",
                    userId, userRole, requestPath, requestMethod);

            // Check if admin is accessing farmer endpoints (allowed with logging)
            if (isAdmin(authentication) && isFarmerEndpoint(requestPath)) {
                log.info("Admin {} accessing farmer endpoint: {} {} - Allowed for testing purposes",
                        userId, requestMethod, requestPath);
            }

            // Check if farmer is accessing admin endpoints (will be denied by security config)
            if (isFarmer(authentication) && isAdminEndpoint(requestPath)) {
                log.warn("Farmer {} attempting to access admin endpoint: {} {} - Will be denied",
                        userId, requestMethod, requestPath);
            }
        }

        return true;
    }

    /**
     * Get the user's role from authentication.
     */
    private String getUserRole(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("UNKNOWN");
    }

    /**
     * Check if the user has admin role.
     */
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Check if the user has farmer role.
     */
    private boolean isFarmer(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_FARMER"));
    }

    /**
     * Check if the request path is a farmer endpoint.
     */
    private boolean isFarmerEndpoint(String path) {
        return FARMER_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    /**
     * Check if the request path is an admin endpoint.
     */
    private boolean isAdminEndpoint(String path) {
        return ADMIN_ENDPOINTS.stream().anyMatch(path::startsWith);
    }
}