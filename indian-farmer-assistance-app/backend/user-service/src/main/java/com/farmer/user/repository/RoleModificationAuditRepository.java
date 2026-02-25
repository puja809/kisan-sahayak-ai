package com.farmer.user.repository;

import com.farmer.user.entity.RoleModificationAudit;
import com.farmer.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for RoleModificationAudit entity.
 * Requirements: 22.2, 22.7
 */
@Repository
public interface RoleModificationAuditRepository extends JpaRepository<RoleModificationAudit, Long> {

    /**
     * Find audit records by user ID.
     * Requirements: 22.7
     */
    List<RoleModificationAudit> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find audit records by farmer ID.
     * Requirements: 22.7
     */
    List<RoleModificationAudit> findByFarmerIdOrderByCreatedAtDesc(String farmerId);

    /**
     * Find audit records by modifier ID.
     * Requirements: 22.7
     */
    List<RoleModificationAudit> findByModifierIdOrderByCreatedAtDesc(String modifierId);

    /**
     * Find audit records within a date range.
     * Requirements: 22.7
     */
    List<RoleModificationAudit> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find audit records by old role.
     * Requirements: 22.7
     */
    List<RoleModificationAudit> findByOldRoleOrderByCreatedAtDesc(User.Role oldRole);

    /**
     * Find audit records by new role.
     * Requirements: 22.7
     */
    List<RoleModificationAudit> findByNewRoleOrderByCreatedAtDesc(User.Role newRole);

    /**
     * Count role modifications within a date range.
     * Requirements: 22.7
     */
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}