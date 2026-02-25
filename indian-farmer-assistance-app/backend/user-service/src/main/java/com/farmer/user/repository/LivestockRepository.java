package com.farmer.user.repository;

import com.farmer.user.entity.Livestock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Livestock entity with custom query methods.
 * Requirements: 11A.11
 */
@Repository
public interface LivestockRepository extends JpaRepository<Livestock, Long> {

    /**
     * Find all livestock for a user.
     * Requirements: 11A.11
     */
    List<Livestock> findByUserId(Long userId);

    /**
     * Find all active livestock for a user.
     * Requirements: 11A.11
     */
    List<Livestock> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find livestock by type for a user.
     * Requirements: 11A.11
     */
    List<Livestock> findByUserIdAndLivestockType(Long userId, Livestock.LivestockType livestockType);

    /**
     * Find livestock by purpose for a user.
     * Requirements: 11A.11
     */
    List<Livestock> findByUserIdAndPurpose(Long userId, Livestock.LivestockPurpose purpose);

    /**
     * Find livestock by ID and user ID.
     */
    Optional<Livestock> findByIdAndUserId(Long id, Long userId);

    /**
     * Count livestock by type for a user.
     * Requirements: 11A.11
     */
    long countByUserIdAndLivestockType(Long userId, Livestock.LivestockType livestockType);

    /**
     * Count total livestock for a user.
     * Requirements: 11A.11
     */
    long countByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find livestock for a specific farm.
     * Requirements: 11A.11
     */
    List<Livestock> findByFarmId(Long farmId);
}