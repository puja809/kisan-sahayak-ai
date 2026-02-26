package com.farmer.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Audit log entity for tracking all administrative actions.
 * Requirements: 21.11, 22.7
 */
@Entity
@Table(name = "sess_c05a946fe_audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 200)
    private String userName;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "old_value", columnDefinition = "JSON")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "JSON")
    private String newValue;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    /**
     * Audit action types for document and scheme operations.
     * Requirements: 21.11
     */
    public enum AuditAction {
        // Document actions
        DOCUMENT_UPLOAD("DOCUMENT_UPLOAD"),
        DOCUMENT_UPDATE("DOCUMENT_UPDATE"),
        DOCUMENT_DELETE("DOCUMENT_DELETE"),
        DOCUMENT_RESTORE("DOCUMENT_RESTORE"),
        DOCUMENT_VIEW("DOCUMENT_VIEW"),
        
        // Scheme actions
        SCHEME_CREATE("SCHEME_CREATE"),
        SCHEME_UPDATE("SCHEME_UPDATE"),
        SCHEME_DELETE("SCHEME_DELETE"),
        SCHEME_ACTIVATE("SCHEME_ACTIVATE"),
        SCHEME_DEACTIVATE("SCHEME_DEACTIVATE"),
        
        // User actions
        USER_ROLE_CHANGE("USER_ROLE_CHANGE"),
        USER_CREATE("USER_CREATE"),
        USER_UPDATE("USER_UPDATE"),
        USER_DELETE("USER_DELETE"),
        
        // System actions
        SYSTEM_CONFIG_CHANGE("SYSTEM_CONFIG_CHANGE"),
        DATA_EXPORT("DATA_EXPORT"),
        DATA_IMPORT("DATA_IMPORT");

        private final String value;

        AuditAction(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}