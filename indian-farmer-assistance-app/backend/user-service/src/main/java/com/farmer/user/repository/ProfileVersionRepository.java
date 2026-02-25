package com.farmer.user.repository;

import com.farmer.user.entity.ProfileVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ProfileVersion entity with custom query methods.
 * Requirements: 11A.7, 21.6
 */
@Repository
public interface ProfileVersionRepository extends JpaRepository<ProfileVersion, Long> {

    /**
     * Find all version history for a user.
     * Requirements: 11A.7
     */
    List<ProfileVersion> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find version history for a user with pagination.
     * Requirements: 11A.7
     */
    Page<ProfileVersion> findByUserId(Long userId, Pageable pageable);

    /**
     * Find version history for a specific entity.
     * Requirements: 11A.7
     */
    List<ProfileVersion> findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            Long userId, ProfileVersion.EntityType entityType, Long entityId);

    /**
     * Find version history by change type.
     * Requirements: 11A.7
     */
    List<ProfileVersion> findByUserIdAndChangeTypeOrderByCreatedAtDesc(
            Long userId, ProfileVersion.ChangeType changeType);

    /**
     * Find version history within a date range.
     * Requirements: 11A.7
     */
    @Query("SELECT v FROM ProfileVersion v WHERE v.userId = :userId AND v.createdAt BETWEEN :startDate AND :endDate ORDER BY v.createdAt DESC")
    List<ProfileVersion> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get the latest version number for a user.
     * Requirements: 11A.7
     */
    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM ProfileVersion v WHERE v.userId = :userId")
    Long getLatestVersionNumber(@Param("userId") Long userId);

    /**
     * Count changes by type for a user.
     * Requirements: 21.6
     */
    long countByUserIdAndChangeType(Long userId, ProfileVersion.ChangeType changeType);

    /**
     * Find version history by farmer ID.
     * Requirements: 11A.7
     */
    List<ProfileVersion> findByFarmerIdOrderByCreatedAtDesc(String farmerId);

    /**
     * Find recent changes for a user.
     * Requirements: 11A.7
     */
    @Query("SELECT v FROM ProfileVersion v WHERE v.userId = :userId ORDER BY v.createdAt DESC")
    List<ProfileVersion> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}