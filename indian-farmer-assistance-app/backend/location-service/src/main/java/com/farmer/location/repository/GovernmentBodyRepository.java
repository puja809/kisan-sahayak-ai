package com.farmer.location.repository;

import com.farmer.location.entity.GovernmentBody;
import com.farmer.location.entity.GovernmentBody.GovernmentBodyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GovernmentBody entity.
 * 
 * Validates: Requirements 7.2, 7.3, 7.4, 7.5
 */
@Repository
public interface GovernmentBodyRepository extends JpaRepository<GovernmentBody, Long> {

    /**
     * Find all active government bodies of a specific type
     */
    List<GovernmentBody> findByBodyTypeAndIsActiveTrue(GovernmentBodyType bodyType);

    /**
     * Find all active government bodies in a specific state
     */
    List<GovernmentBody> findByStateAndIsActiveTrue(String state);

    /**
     * Find all active government bodies in a specific district
     */
    List<GovernmentBody> findByDistrictAndIsActiveTrue(String district);

    /**
     * Find all active government bodies in a specific state and district
     */
    List<GovernmentBody> findByStateAndDistrictAndIsActiveTrue(String state, String district);

    /**
     * Find KVKs within a specified distance using the Haversine formula.
     * This query finds all KVKs and the service layer filters by distance.
     */
    @Query("SELECT gb FROM GovernmentBody gb WHERE gb.bodyType = :bodyType AND gb.isActive = true " +
           "AND gb.latitude IS NOT NULL AND gb.longitude IS NOT NULL")
    List<GovernmentBody> findByBodyTypeWithCoordinates(@Param("bodyType") GovernmentBodyType bodyType);

    /**
     * Find all government bodies with coordinates (for distance-based queries)
     */
    @Query("SELECT gb FROM GovernmentBody gb WHERE gb.isActive = true " +
           "AND gb.latitude IS NOT NULL AND gb.longitude IS NOT NULL")
    List<GovernmentBody> findAllWithCoordinates();

    /**
     * Find government bodies by state and type
     */
    List<GovernmentBody> findByStateAndBodyTypeAndIsActiveTrue(String state, GovernmentBodyType bodyType);

    /**
     * Find KVK by name
     */
    Optional<GovernmentBody> findByNameAndBodyTypeAndIsActiveTrue(String name, GovernmentBodyType bodyType);

    /**
     * Count government bodies by type in a state
     */
    @Query("SELECT COUNT(gb) FROM GovernmentBody gb WHERE gb.state = :state " +
           "AND gb.bodyType = :bodyType AND gb.isActive = true")
    long countByStateAndType(@Param("state") String state, @Param("bodyType") GovernmentBodyType bodyType);

    /**
     * Find all KVKs (for bulk operations)
     */
    default List<GovernmentBody> findAllKvks() {
        return findByBodyTypeAndIsActiveTrue(GovernmentBodyType.KVK);
    }

    /**
     * Find all district agriculture offices
     */
    default List<GovernmentBody> findAllDistrictOffices() {
        return findByBodyTypeAndIsActiveTrue(GovernmentBodyType.DISTRICT_AGRICULTURE_OFFICE);
    }

    /**
     * Find all state departments
     */
    default List<GovernmentBody> findAllStateDepartments() {
        return findByBodyTypeAndIsActiveTrue(GovernmentBodyType.STATE_DEPARTMENT);
    }

    /**
     * Find all ATARI centers
     */
    default List<GovernmentBody> findAllAtariCenters() {
        return findByBodyTypeAndIsActiveTrue(GovernmentBodyType.ATARI);
    }
}