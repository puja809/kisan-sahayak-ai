package com.farmer.user.repository;

import com.farmer.user.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Equipment entity with custom query methods.
 * Requirements: 11A.12
 */
@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    /**
     * Find all equipment for a user.
     * Requirements: 11A.12
     */
    List<Equipment> findByUserId(Long userId);

    /**
     * Find all active equipment for a user.
     * Requirements: 11A.12
     */
    List<Equipment> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find equipment by type for a user.
     * Requirements: 11A.12
     */
    List<Equipment> findByUserIdAndEquipmentType(Long userId, Equipment.EquipmentType equipmentType);

    /**
     * Find equipment by ID and user ID.
     */
    Optional<Equipment> findByIdAndUserId(Long id, Long userId);

    /**
     * Count equipment by type for a user.
     * Requirements: 11A.12
     */
    long countByUserIdAndEquipmentType(Long userId, Equipment.EquipmentType equipmentType);

    /**
     * Count total active equipment for a user.
     * Requirements: 11A.12
     */
    long countByUserIdAndIsActiveTrue(Long userId);

    /**
     * Find equipment requiring maintenance.
     * Requirements: 11A.12
     */
    @Query("SELECT e FROM Equipment e WHERE e.user.id = :userId AND e.isActive = true AND e.nextMaintenanceDate <= :date")
    List<Equipment> findEquipmentRequiringMaintenance(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * Find equipment by ownership type.
     * Requirements: 11A.12
     */
    List<Equipment> findByUserIdAndOwnershipType(Long userId, Equipment.OwnershipType ownershipType);

    /**
     * Calculate total value of equipment for a user.
     * Requirements: 11A.12
     */
    @Query("SELECT COALESCE(SUM(e.currentValue), 0) FROM Equipment e WHERE e.user.id = :userId AND e.isActive = true")
    Double calculateTotalEquipmentValue(@Param("userId") Long userId);

    /**
     * Find recently purchased equipment.
     */
    List<Equipment> findByUserIdAndPurchaseDateAfter(Long userId, LocalDate date);
}