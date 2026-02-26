package com.farmer.mandi.repository;

import com.farmer.mandi.entity.District;
import com.farmer.mandi.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {
    Optional<District> findByDistrictName(String districtName);
    Optional<District> findByDistrictCode(String districtCode);
    List<District> findByState(State state);
}
