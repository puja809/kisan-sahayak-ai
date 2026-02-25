package com.farmer.admin.repository;

import com.farmer.admin.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for DocumentVersion entity.
 * Requirements: 21.6
 */
@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    /**
     * Find all versions of a document, ordered by version descending.
     */
    List<DocumentVersion> findByDocumentIdOrderByVersionDesc(String documentId);

    /**
     * Find specific version of a document.
     */
    Optional<DocumentVersion> findByDocumentIdAndVersion(String documentId, Integer version);

    /**
     * Find latest version of a document.
     */
    Optional<DocumentVersion> findFirstByDocumentIdOrderByVersionDesc(String documentId);

    /**
     * Find versions by changer.
     */
    List<DocumentVersion> findByChangedByOrderByCreatedAtDesc(String changedBy);

    /**
     * Find versions created within a date range.
     */
    List<DocumentVersion> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Count versions of a document.
     */
    long countByDocumentId(String documentId);

    /**
     * Delete old versions beyond retention period.
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}