package com.farmer.scheme.repository;

import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.Scheme.SchemeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Scheme entity with custom query methods.
 * Requirements: 4.1, 4.2, 4.3, 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8
 */
@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {

    /**
     * Find scheme by scheme code.
     * Requirements: 4.1
     */
    Optional<Scheme> findBySchemeCode(String schemeCode);

    /**
     * Check if a scheme exists by scheme code.
     */
    boolean existsBySchemeCode(String schemeCode);

    /**
     * Find all active schemes.
     * Requirements: 4.1, 4.2
     */
    List<Scheme> findByIsActiveTrue();

    /**
     * Find active schemes by type.
     * Requirements: 4.1, 5.1, 5.2
     */
    List<Scheme> findBySchemeTypeAndIsActiveTrue(SchemeType schemeType);

    /**
     * Find active schemes by state.
     * Requirements: 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    List<Scheme> findByStateAndIsActiveTrue(String state);

    /**
     * Find central schemes (applicable to all states) and state-specific schemes.
     * Requirements: 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Query("SELECT s FROM Scheme s WHERE s.isActive = true AND " +
           "(s.schemeType = 'CENTRAL' OR s.state IS NULL OR s.state = :state)")
    List<Scheme> findActiveSchemesForState(@Param("state") String state);

    /**
     * Find schemes by type and state.
     * Requirements: 4.1, 4.3, 5.1, 5.2
     */
    @Query("SELECT s FROM Scheme s WHERE s.isActive = true AND " +
           "(s.schemeType = :type OR s.state = :state)")
    List<Scheme> findByTypeAndState(@Param("type") SchemeType type, @Param("state") String state);

    /**
     * Find schemes with active application windows.
     * Requirements: 4.2, 11D.8, 11D.9
     */
    @Query("SELECT s FROM Scheme s WHERE s.isActive = true AND " +
           "s.applicationStartDate <= :currentDate AND s.applicationEndDate >= :currentDate")
    List<Scheme> findActiveSchemesWithOpenApplications(@Param("currentDate") LocalDate currentDate);

    /**
     * Find schemes with approaching deadlines (within specified days).
     * Requirements: 4.8, 11D.9
     */
    @Query("SELECT s FROM Scheme s WHERE s.isActive = true AND " +
           "s.applicationEndDate BETWEEN :currentDate AND :deadlineDate")
    List<Scheme> findSchemesWithApproachingDeadlines(
            @Param("currentDate") LocalDate currentDate,
            @Param("deadlineDate") LocalDate deadlineDate);

    /**
     * Find schemes by applicable crops.
     * Requirements: 4.1, 11D.4
     */
    @Query("SELECT s FROM Scheme s WHERE s.isActive = true AND " +
           "s.applicableCrops LIKE %:cropName%")
    List<Scheme> findByApplicableCrop(@Param("cropName") String cropName);

    /**
     * Find schemes for state with open applications.
     * Requirements: 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Query("SELECT s FROM Scheme s WHERE s.isActive = true AND " +
           "(s.schemeType = 'CENTRAL' OR s.state IS NULL OR s.state = :state) AND " +
           "s.applicationStartDate <= :currentDate AND s.applicationEndDate >= :currentDate")
    List<Scheme> findActiveSchemesForStateWithOpenApplications(
            @Param("state") String state,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Find schemes by multiple types.
     * Requirements: 4.1, 5.1, 5.2
     */
    List<Scheme> findBySchemeTypeInAndIsActiveTrue(List<SchemeType> schemeTypes);

    /**
     * Find all central schemes.
     * Requirements: 4.1, 5.1
     */
    @Query("SELECT s FROM Scheme s WHERE s.isActive = true AND s.schemeType = 'CENTRAL'")
    List<Scheme> findAllCentralSchemes();

    /**
     * Find state-specific schemes for a given state.
     * Requirements: 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Query("SELECT s FROM Scheme s WHERE s.isActive = true AND s.state = :state")
    List<Scheme> findStateSchemes(@Param("state") String state);

    /**
     * Count active schemes by type.
     * Requirements: 4.1
     */
    long countBySchemeTypeAndIsActiveTrue(SchemeType schemeType);

    /**
     * Count all active schemes.
     * Requirements: 4.1
     */
    long countByIsActiveTrue();

    /**
     * Find schemes ordered by application end date (for deadline notifications).
     * Requirements: 11D.9
     */
    @Query("SELECT s FROM Scheme s WHERE s.isActive = true AND s.applicationEndDate >= :currentDate " +
           "ORDER BY s.applicationEndDate ASC")
    List<Scheme> findActiveSchemesOrderByDeadline(@Param("currentDate") LocalDate currentDate);
}