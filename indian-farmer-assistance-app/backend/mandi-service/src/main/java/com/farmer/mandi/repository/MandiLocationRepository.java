package com.farmer.mandi.repository;

import com.farmer.mandi.entity.MandiLocation;
import com.farmer.mandi.entity.State;
import com.farmer.mandi.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MandiLocationRepository extends JpaRepository<MandiLocation, Long> {
    List<MandiLocation> findByStateAndIsActiveTrue(State state);
    List<MandiLocation> findByStateAndDistrictAndIsActiveTrue(State state, District district);
    List<MandiLocation> findByState_StateNameAndDistrict_DistrictName(String stateName, String districtName);
    Optional<MandiLocation> findByMandiNameAndIsActiveTrue(String mandiName);
    List<MandiLocation> findByIsActiveTrue();
}
