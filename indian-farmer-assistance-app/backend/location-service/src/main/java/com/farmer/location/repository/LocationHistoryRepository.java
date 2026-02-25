package com.farmer.location.repository;

import com.farmer.location.entity.LocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for LocationHistory entity.
 * 
 * Validates: Requirement 14.4 (location change detection)
 */
@Repository
public interface LocationHistoryRepository extends JpaRepository<LocationHistory, Long> {

    /**
     * Find the most recent location for a user
     */
    @Query("SELECT lh FROM LocationHistory lh WHERE lh.userId = :userId " +
           "ORDER BY lh.recordedAt DESC LIMIT 1")
    Optional<LocationHistory> findMostRecentByUserId(@Param("userId") Long userId);

    /**
     * Find all locations for a user, ordered by recording time
     */
    List<LocationHistory> findByUserIdOrderByRecordedAtDesc(Long userId);

    /**
     * Find locations for a user within a time range
     */
    @Query("SELECT lh FROM LocationHistory lh WHERE lh.userId = :userId " +
           "AND lh.recordedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY lh.recordedAt DESC")
    List<LocationHistory> findByUserIdAndTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find significant location changes for a user
     */
    @Query("SELECT lh FROM LocationHistory lh WHERE lh.userId = :userId " +
           "AND lh.isSignificantChange = true " +
           "ORDER BY lh.recordedAt DESC")
    List<LocationHistory> findSignificantChangesByUserId(@Param("userId") Long userId);

    /**
     * Find locations by district
     */
    List<LocationHistory> findByDistrict(String district);

    /**
     * Find locations by state
     */
    List<LocationHistory> findByState(String state);

    /**
     * Count location records for a user
     */
    long countByUserId(Long userId);

    /**
     * Delete old location history entries
     */
    void deleteByRecordedAtBefore(LocalDateTime timestamp);

    /**
     * Find the last N locations for a user
     */
    @Query("SELECT lh FROM LocationHistory lh WHERE lh.userId = :userId " +
           "ORDER BY lh.recordedAt DESC LIMIT :limit")
    List<LocationHistory> findLastNByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit);
}