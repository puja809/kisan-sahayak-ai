package com.farmer.scheme.repository;

import com.farmer.scheme.entity.SchemeApplication;
import com.farmer.scheme.entity.SchemeApplication.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SchemeApplication entity with custom query methods.
 * Requirements: 11D.10
 */
@Repository
public interface SchemeApplicationRepository extends JpaRepository<SchemeApplication, Long> {

    /**
     * Find all applications for a user.
     * Requirements: 11D.10
     */
    List<SchemeApplication> findByUserId(Long userId);

    /**
     * Find all applications for a user with a specific status.
     * Requirements: 11D.10
     */
    List<SchemeApplication> findByUserIdAndStatus(Long userId, ApplicationStatus status);

    /**
     * Find all applications for a scheme.
     * Requirements: 11D.10
     */
    List<SchemeApplication> findBySchemeId(Long schemeId);

    /**
     * Find application by application number.
     * Requirements: 11D.10
     */
    Optional<SchemeApplication> findByApplicationNumber(String applicationNumber);

    /**
     * Check if application exists for user and scheme.
     * Requirements: 11D.10
     */
    boolean existsByUserIdAndSchemeId(Long userId, Long schemeId);

    /**
     * Find applications by status.
     * Requirements: 11D.10
     */
    List<SchemeApplication> findByStatus(ApplicationStatus status);

    /**
     * Find applications under review for a scheme.
     * Requirements: 11D.10
     */
    @Query("SELECT sa FROM SchemeApplication sa WHERE sa.scheme.id = :schemeId AND sa.status = 'UNDER_REVIEW'")
    List<SchemeApplication> findUnderReviewBySchemeId(@Param("schemeId") Long schemeId);

    /**
     * Find applications by user and scheme.
     * Requirements: 11D.10
     */
    Optional<SchemeApplication> findByUserIdAndSchemeId(Long userId, Long schemeId);

    /**
     * Find applications with status in list.
     * Requirements: 11D.10
     */
    List<SchemeApplication> findByStatusIn(List<ApplicationStatus> statuses);

    /**
     * Find applications created after a specific date.
     * Requirements: 11D.10
     */
    List<SchemeApplication> findByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * Find applications submitted after a specific date.
     * Requirements: 11D.10
     */
    List<SchemeApplication> findBySubmittedAtAfter(LocalDateTime dateTime);

    /**
     * Count applications by status.
     * Requirements: 11D.10
     */
    long countByStatus(ApplicationStatus status);

    /**
     * Count applications by user and status.
     * Requirements: 11D.10
     */
    long countByUserIdAndStatus(Long userId, ApplicationStatus status);

    /**
     * Find applications for user ordered by created date descending.
     * Requirements: 11D.10
     */
    @Query("SELECT sa FROM SchemeApplication sa WHERE sa.userId = :userId ORDER BY sa.createdAt DESC")
    List<SchemeApplication> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Find draft applications for a user.
     * Requirements: 11D.10
     */
    @Query("SELECT sa FROM SchemeApplication sa WHERE sa.userId = :userId AND sa.status = 'DRAFT'")
    List<SchemeApplication> findDraftApplicationsByUserId(@Param("userId") Long userId);

    /**
     * Find approved but not disbursed applications.
     * Requirements: 11D.10
     */
    @Query("SELECT sa FROM SchemeApplication sa WHERE sa.status = 'APPROVED' AND sa.disbursedAt IS NULL")
    List<SchemeApplication> findApprovedNotDisbursed();

    /**
     * Find applications requiring deadline notifications (submitted with approaching scheme deadlines).
     * Requirements: 11D.9, 11D.10
     */
    @Query("SELECT sa FROM SchemeApplication sa WHERE sa.status = 'SUBMITTED' AND " +
           "sa.scheme.applicationEndDate BETWEEN :startDate AND :endDate")
    List<SchemeApplication> findApplicationsRequiringDeadlineNotification(
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate);

    /**
     * Find applications by scheme and status.
     * Requirements: 11D.10
     */
    List<SchemeApplication> findBySchemeIdAndStatus(Long schemeId, ApplicationStatus status);

    /**
     * Count applications by scheme.
     * Requirements: 11D.10
     */
    long countBySchemeId(Long schemeId);

    /**
     * Find recent applications for a user.
     * Requirements: 11D.10
     */
    @Query("SELECT sa FROM SchemeApplication sa WHERE sa.userId = :userId " +
           "ORDER BY sa.updatedAt DESC LIMIT :limit")
    List<SchemeApplication> findRecentApplicationsByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit);
}