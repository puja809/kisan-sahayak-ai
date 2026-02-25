package com.farmer.admin.service;

import com.farmer.admin.entity.AuditLog;
import com.farmer.admin.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for audit logging of all administrative actions.
 * Requirements: 21.11, 22.7
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log a document action.
     * Requirements: 21.11
     */
    public void logDocumentAction(String action, String entityId, Object oldValue, Object newValue, String adminId) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType("DOCUMENT")
                    .entityId(entityId)
                    .oldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null)
                    .newValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null)
                    .changedBy(adminId)
                    .timestamp(LocalDateTime.now())
                    .status("SUCCESS")
                    .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for document action: {}", action);
        } catch (Exception e) {
            log.error("Failed to create audit log for document action: {}", e.getMessage());
        }
    }

    /**
     * Log a scheme action.
     * Requirements: 21.11
     */
    public void logSchemeAction(String action, Long entityId, Object oldValue, Object newValue, String adminId) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType("SCHEME")
                    .entityId(entityId != null ? entityId.toString() : null)
                    .oldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null)
                    .newValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null)
                    .changedBy(adminId)
                    .timestamp(LocalDateTime.now())
                    .status("SUCCESS")
                    .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for scheme action: {}", action);
        } catch (Exception e) {
            log.error("Failed to create audit log for scheme action: {}", e.getMessage());
        }
    }

    /**
     * Log any administrative action.
     * Requirements: 21.11, 22.7
     */
    public void logAction(String action, String entityType, String entityId, 
                          Object oldValue, Object newValue, Long userId, String userName) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userName(userName)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null)
                    .newValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null)
                    .timestamp(LocalDateTime.now())
                    .status("SUCCESS")
                    .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for action: {} on {}: {}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    /**
     * Log a failed action.
     * Requirements: 21.11
     */
    public void logFailedAction(String action, String entityType, String entityId, 
                                 String errorMessage, Long userId) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .timestamp(LocalDateTime.now())
                    .status("FAILED")
                    .errorMessage(errorMessage)
                    .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Failed audit log created for action: {}", action);
        } catch (Exception e) {
            log.error("Failed to create failed audit log: {}", e.getMessage());
        }
    }

    /**
     * Get audit logs with pagination.
     * Requirements: 21.11
     */
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    /**
     * Get audit logs by entity type.
     * Requirements: 21.11
     */
    public Page<AuditLog> getAuditLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType, pageable);
    }

    /**
     * Get recent audit logs.
     * Requirements: 21.11
     */
    public Page<AuditLog> getRecentAuditLogs(LocalDateTime since, Pageable pageable) {
        return auditLogRepository.findRecentLogs(since, pageable);
    }

    /**
     * Get audit logs by user.
     * Requirements: 21.11
     */
    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    /**
     * Get audit logs within a date range.
     * Requirements: 21.11
     */
    public java.util.List<AuditLog> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }
}