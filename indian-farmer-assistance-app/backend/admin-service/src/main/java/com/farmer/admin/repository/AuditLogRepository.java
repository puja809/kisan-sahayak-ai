package com.farmer.admin.repository;

import com.farmer.admin.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA repository for AuditLog entity.
 * Requirements: 21.11, 22.7
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by user ID.
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    /**
     * Find audit logs by entity type and entity ID.
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);

    /**
     * Find audit logs by action type.
     */
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    /**
     * Find audit logs within a date range.
     */
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Find audit logs by entity type.
     */
    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);

    /**
     * Count audit logs by action type.
     */
    long countByAction(String action);

    /**
     * Count audit logs by entity type.
     */
    long countByEntityType(String entityType);

    /**
     * Find recent audit logs with pagination.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp >= :since ORDER BY a.timestamp DESC")
    Page<AuditLog> findRecentLogs(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Delete old audit logs beyond retention period.
     */
    void deleteByTimestampBefore(LocalDateTime cutoffDate);
}