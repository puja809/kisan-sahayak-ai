package com.farmer.admin.controller;

import com.farmer.admin.dto.AuditLogResponse;
import com.farmer.admin.entity.AuditLog;
import com.farmer.admin.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for admin audit log operations.
 * Requirements: 21.11
 */
@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
@Slf4j
// @PreAuthorize("hasRole('ADMIN')") // Temporarily disabled until JWT filter is
// implemented
public class AdminAuditController {

    private final AuditService auditService;

    /**
     * Get all audit logs with pagination.
     * GET /api/v1/admin/audit/logs
     * Requirements: 21.11
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/v1/admin/audit/logs - Fetching audit logs, page: {}, size: {}", page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> logs = auditService.getAuditLogs(pageable);
        return ResponseEntity.ok(logs.map(this::toResponse));
    }

    /**
     * Get audit logs by entity type.
     * GET /api/v1/admin/audit/logs/entity/{entityType}
     * Requirements: 21.11
     */
    @GetMapping("/logs/entity/{entityType}")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByEntityType(
            @PathVariable String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/v1/admin/audit/logs/entity/{} - Fetching audit logs by entity type", entityType);
        PageRequest pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditService.getAuditLogsByEntityType(entityType, pageable);
        return ResponseEntity.ok(logs.map(this::toResponse));
    }

    /**
     * Get recent audit logs.
     * GET /api/v1/admin/audit/logs/recent
     * Requirements: 21.11
     */
    @GetMapping("/logs/recent")
    public ResponseEntity<Page<AuditLogResponse>> getRecentAuditLogs(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/v1/admin/audit/logs/recent - Fetching recent audit logs, hours: {}", hours);
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        PageRequest pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditService.getRecentAuditLogs(since, pageable);
        return ResponseEntity.ok(logs.map(this::toResponse));
    }

    /**
     * Get audit logs by user.
     * GET /api/v1/admin/audit/logs/user/{userId}
     * Requirements: 21.11
     */
    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/v1/admin/audit/logs/user/{} - Fetching audit logs by user", userId);
        PageRequest pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditService.getAuditLogsByUser(userId, pageable);
        return ResponseEntity.ok(logs.map(this::toResponse));
    }

    /**
     * Get audit logs within a date range.
     * GET /api/v1/admin/audit/logs/range
     * Requirements: 21.11
     */
    @GetMapping("/logs/range")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        log.debug("GET /api/v1/admin/audit/logs/range - Fetching audit logs by date range");
        List<AuditLog> logs = auditService.getAuditLogsByDateRange(start, end);
        return ResponseEntity.ok(logs.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /**
     * Convert AuditLog entity to AuditLogResponse DTO.
     */
    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .userName(auditLog.getUserName())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}