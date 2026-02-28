package com.farmer.location.repository;

import com.farmer.location.entity.GovernmentBody;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernmentBodyRepository extends JpaRepository<GovernmentBody, Long> {
    List<GovernmentBody> findByState(String state);
    List<GovernmentBody> findByDistrict(String district);
    List<GovernmentBody> findByStateAndDistrict(String state, String district);
    long countByState(String state);
}
