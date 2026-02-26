package com.farmer.admin.interceptor;

import com.farmer.admin.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for capturing HTTP request metadata (IP address, user agent) for audit logging.
 * Requirements: 21.11, 22.7
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingInterceptor implements HandlerInterceptor {

    private final AuditService auditService;

    private static final String IP_ADDRESS_ATTRIBUTE = "auditIpAddress";
    private static final String USER_AGENT_ATTRIBUTE = "auditUserAgent";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Extract and store IP address
        String ipAddress = extractClientIpAddress(request);
        request.setAttribute(IP_ADDRESS_ATTRIBUTE, ipAddress);

        // Extract and store user agent
        String userAgent = request.getHeader("User-Agent");
        request.setAttribute(USER_AGENT_ATTRIBUTE, userAgent);

        log.debug("Audit interceptor: IP={}, UserAgent={}", ipAddress, userAgent);
        return true;
    }

    /**
     * Extract client IP address from request, handling proxies.
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        // Check for X-Forwarded-For header (proxy)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        // Check for X-Real-IP header (nginx proxy)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }

    /**
     * Get IP address from request attribute.
     */
    public static String getIpAddress(HttpServletRequest request) {
        Object ipAddress = request.getAttribute(IP_ADDRESS_ATTRIBUTE);
        return ipAddress != null ? (String) ipAddress : null;
    }

    /**
     * Get user agent from request attribute.
     */
    public static String getUserAgent(HttpServletRequest request) {
        Object userAgent = request.getAttribute(USER_AGENT_ATTRIBUTE);
        return userAgent != null ? (String) userAgent : null;
    }
}
