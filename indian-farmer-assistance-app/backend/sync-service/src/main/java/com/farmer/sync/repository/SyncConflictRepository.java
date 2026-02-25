package com.farmer.sync.repository;

import com.farmer.sync.entity.SyncConflict;
import com.farmer.sync.entity.SyncConflict.ConflictStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for sync conflict operations.
 * 
 * Provides methods for managing conflicts between devices.
 * 
 * Validates: Requirement 15.3
 */
@Repository
public interface SyncConflictRepository extends JpaRepository<SyncConflict, Long> {

    /**
     * Find all pending conflicts for a user.
     */
    List<SyncConflict> findByUserIdAndStatusOrderByDetectedAtDesc(
        String userId, 
        ConflictStatus status
    );

    /**
     * Find conflict for a specific entity.
     */
    Optional<SyncConflict> findByUserIdAndEntityTypeAndEntityId(
        String userId, 
        String entityType, 
        String entityId
    );

    /**
     * Count pending conflicts for a user.
     */
    long countByUserIdAndStatus(String userId, ConflictStatus status);

    /**
     * Update conflict status.
     */
    @Modifying
    @Query("UPDATE SyncConflict c SET c.status = :status, c.resolvedAt = :resolvedAt " +
           "WHERE c.id = :id")
    int updateStatus(
        @Param("id") Long id, 
        @Param("status") ConflictStatus status, 
        @Param("resolvedAt") LocalDateTime resolvedAt
    );

    /**
     * Resolve conflict with data.
     */
    @Modifying
    @Query("UPDATE SyncConflict c SET c.status = :status, c.resolvedData = :resolvedData, " +
           "c.resolvedAt = :resolvedAt, c.resolvedBy = :resolvedBy, " +
           "c.resolutionStrategy = :strategy WHERE c.id = :id")
    int resolveConflict(
        @Param("id") Long id,
        @Param("status") ConflictStatus status,
        @Param("resolvedData") String resolvedData,
        @Param("resolvedAt") LocalDateTime resolvedAt,
        @Param("resolvedBy") String resolvedBy,
        @Param("strategy") com.farmer.sync.entity.SyncConflict.ResolutionStrategy strategy
    );

    /**
     * Delete old resolved conflicts.
     */
    @Modifying
    @Query("DELETE FROM SyncConflict c WHERE c.status = 'RESOLVED' " +
           "AND c.resolvedAt < :before")
    int deleteOldResolvedConflicts(@Param("before") LocalDateTime before);

    /**
     * Find all conflicts for a user.
     */
    List<SyncConflict> findByUserId(String userId);
}