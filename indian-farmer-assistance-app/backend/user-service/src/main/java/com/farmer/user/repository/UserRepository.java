package com.farmer.user.repository;

import com.farmer.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity with custom query methods.
 * Requirements: 11.1, 11A.1
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by farmer ID.
     * Requirements: 11.1
     */
    Optional<User> findByFarmerId(String farmerId);

    /**
     * Find user by phone number.
     * Requirements: 11.1
     */
    Optional<User> findByPhone(String phone);

    /**
     * Find user by Aadhaar hash.
     * Requirements: 11.1
     */
    Optional<User> findByAadhaarHash(String aadhaarHash);

    /**
     * Find user by AgriStack farmer ID.
     * Requirements: 11.2, 11.3
     */
    Optional<User> findByAgristackFarmerId(String agristackFarmerId);

    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all active users.
     */
    List<User> findByIsActiveTrue();

    /**
     * Find users by state.
     * Requirements: 11A.1
     */
    List<User> findByState(String state);

    /**
     * Find users by state and district.
     * Requirements: 11A.1
     */
    List<User> findByStateAndDistrict(String state, String district);

    /**
     * Check if a user exists by phone number.
     */
    boolean existsByPhone(String phone);

    /**
     * Check if a user exists by farmer ID.
     */
    boolean existsByFarmerId(String farmerId);

    /**
     * Check if a user exists by Aadhaar hash.
     */
    boolean existsByAadhaarHash(String aadhaarHash);

    /**
     * Find users by role.
     * Requirements: 22.1
     */
    List<User> findByRole(User.Role role);

    /**
     * Find active users by role.
     * Requirements: 22.1
     */
    List<User> findByRoleAndIsActiveTrue(User.Role role);

    /**
     * Count users by role.
     * Requirements: 22.1
     */
    long countByRole(User.Role role);

    /**
     * Find users with pagination and sorting.
     */
    @Query("SELECT u FROM User u WHERE u.state = :state AND u.isActive = true ORDER BY u.name ASC")
    List<User> findActiveUsersByStateOrderedByName(@Param("state") String state);
}