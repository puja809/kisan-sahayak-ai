package com.farmer.admin.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for audit log response.
 * Requirements: 21.11
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String action;
    private String entityType;
    private String entityId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
}