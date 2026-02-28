package com.farmer.mandi.repository;

import com.farmer.mandi.entity.Grade;
import com.farmer.mandi.entity.Variety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Grade entity.
 */
@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    /**
     * Find all grades by variety.
     */
    List<Grade> findByVarietyAndIsActiveTrue(Variety variety);

    /**
     * Find all grades by variety ID.
     */
    @Query("SELECT g FROM Grade g WHERE g.variety.id = :varietyId AND g.isActive = true ORDER BY g.gradeName")
    List<Grade> findByVarietyId(@Param("varietyId") Long varietyId);

    /**
     * Find grade by variety and grade name.
     */
    Grade findByVarietyAndGradeName(Variety variety, String gradeName);

    /**
     * Find all distinct grades.
     */
    @Query("SELECT DISTINCT g.gradeName FROM Grade g WHERE g.isActive = true ORDER BY g.gradeName")
    List<String> findAllDistinctGrades();
}
