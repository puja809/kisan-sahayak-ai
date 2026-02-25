package com.farmer.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity for auditing role modifications.
 * Requirements: 22.2, 22.7
 */
@Entity
@Table(name = "sess_c05a946fe_role_modification_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleModificationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "farmer_id", nullable = false, length = 50)
    private String farmerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_role", nullable = false)
    private User.Role oldRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_role", nullable = false)
    private User.Role newRole;

    @Column(name = "modifier_id", nullable = false, length = 50)
    private String modifierId;

    @Column(name = "modifier_name", length = 100)
    private String modifierName;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}