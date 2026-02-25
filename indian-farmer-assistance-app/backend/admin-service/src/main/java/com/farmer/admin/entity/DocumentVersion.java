package com.farmer.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Document version history entity for tracking document changes.
 * Requirements: 21.6
 */
@Entity
@Table(name = "sess_c05a946fe_document_versions", indexes = {
    @Index(name = "idx_document_id", columnList = "document_id"),
    @Index(name = "idx_version", columnList = "version"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, length = 100)
    private String documentId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "change_type", length = 20)
    private String changeType; // CREATED, UPDATED, DELETED, RESTORED

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}