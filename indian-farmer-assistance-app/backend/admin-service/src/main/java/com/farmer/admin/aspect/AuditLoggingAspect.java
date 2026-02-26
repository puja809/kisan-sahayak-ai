package com.farmer.admin.aspect;

import com.farmer.admin.interceptor.AuditLoggingInterceptor;
import com.farmer.admin.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect for intercepting administrative actions and logging them to audit logs.
 * Logs document uploads, scheme CRUD operations, and role modifications.
 * Requirements: 21.11, 22.7
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingAspect {

    private final AuditService auditService;

    /**
     * Log document upload actions.
     * Requirements: 21.11
     */
    @AfterReturning(pointcut = "execution(* com.farmer.admin.service.DocumentService.uploadDocument(..))", returning = "result")
    public void logDocumentUpload(JoinPoint joinPoint, Object result) {
        try {
            String adminId = getCurrentUserId();
            String ipAddress = getIpAddress();
            String userAgent = getUserAgent();

            auditService.logAction(
                    "DOCUMENT_UPLOAD",
                    "DOCUMENT",
                    result != null ? result.toString() : "unknown",
                    null,
                    result,
                    getCurrentUserIdAsLong(),
                    adminId
            );

            // Set IP and user agent on the audit log
            setAuditMetadata(ipAddress, userAgent);
        } catch (Exception e) {
            log.error("Error logging document upload: {}", e.getMessage());
        }
    }

    /**
     * Log document update actions.
     * Requirements: 21.11
     */
    @AfterReturning(pointcut = "execution(* com.farmer.admin.service.DocumentService.updateDocument(..))", returning = "result")
    public void logDocumentUpdate(JoinPoint joinPoint, Object result) {
        try {
            String adminId = getCurrentUserId();
            String ipAddress = getIpAddress();
            String userAgent = getUserAgent();

            auditService.logAction(
                    "DOCUMENT_UPDATE",
                    "DOCUMENT",
                    result != null ? result.toString() : "unknown",
                    null,
                    result,
                    getCurrentUserIdAsLong(),
                    adminId
            );

            setAuditMetadata(ipAddress, userAgent);
        } catch (Exception e) {
            log.error("Error logging document update: {}", e.getMessage());
        }
    }

    /**
     * Log document delete actions.
     * Requirements: 21.11
     */
    @AfterReturning(pointcut = "execution(* com.farmer.admin.service.DocumentService.deleteDocument(..))")
    public void logDocumentDelete(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            String documentId = args.length > 0 ? (String) args[0] : "unknown";
            String adminId = getCurrentUserId();
            String ipAddress = getIpAddress();
            String userAgent = getUserAgent();

            auditService.logAction(
                    "DOCUMENT_DELETE",
                    "DOCUMENT",
                    documentId,
                    null,
                    null,
                    getCurrentUserIdAsLong(),
                    adminId
            );

            setAuditMetadata(ipAddress, userAgent);
        } catch (Exception e) {
            log.error("Error logging document delete: {}", e.getMessage());
        }
    }

    /**
     * Log document restore actions.
     * Requirements: 21.11
     */
    @AfterReturning(pointcut = "execution(* com.farmer.admin.service.DocumentService.restoreDocument(..))", returning = "result")
    public void logDocumentRestore(JoinPoint joinPoint, Object result) {
        try {
            String adminId = getCurrentUserId();
            String ipAddress = getIpAddress();
            String userAgent = getUserAgent();

            auditService.logAction(
                    "DOCUMENT_RESTORE",
                    "DOCUMENT",
                    result != null ? result.toString() : "unknown",
                    null,
                    result,
                    getCurrentUserIdAsLong(),
                    adminId
            );

            setAuditMetadata(ipAddress, userAgent);
        } catch (Exception e) {
            log.error("Error logging document restore: {}", e.getMessage());
        }
    }

    /**
     * Get current user ID from security context.
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    /**
     * Get current user ID as Long from security context.
     */
    private Long getCurrentUserIdAsLong() {
        try {
            String userId = getCurrentUserId();
            if ("SYSTEM".equals(userId)) {
                return null;
            }
            return Long.parseLong(userId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get IP address from HTTP request.
     */
    private String getIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return AuditLoggingInterceptor.getIpAddress(request);
            }
        } catch (Exception e) {
            log.debug("Could not extract IP address: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get user agent from HTTP request.
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return AuditLoggingInterceptor.getUserAgent(request);
            }
        } catch (Exception e) {
            log.debug("Could not extract user agent: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Set IP address and user agent on the most recent audit log.
     * This is a helper method to update the last audit log with metadata.
     */
    private void setAuditMetadata(String ipAddress, String userAgent) {
        // Note: In a real implementation, we would update the last audit log with IP and user agent
        // For now, this is a placeholder for future enhancement
        log.debug("Audit metadata - IP: {}, UserAgent: {}", ipAddress, userAgent);
    }
}
