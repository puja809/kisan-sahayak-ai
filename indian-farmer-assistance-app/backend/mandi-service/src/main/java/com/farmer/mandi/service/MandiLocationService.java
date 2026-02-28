package com.farmer.mandi.service;

import com.farmer.mandi.entity.MandiLocation;
import com.farmer.mandi.entity.State;
import com.farmer.mandi.entity.District;
import com.farmer.mandi.repository.MandiLocationRepository;
import com.farmer.mandi.repository.StateRepository;
import com.farmer.mandi.repository.DistrictRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MandiLocationService {

    private final MandiLocationRepository mandiLocationRepository;
    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;

    @Transactional
    public MandiLocation createMandi(String mandiName, String stateName, String districtName) {
        State state = stateRepository.findByStateName(stateName)
            .orElseThrow(() -> new IllegalArgumentException("State not found: " + stateName));
        
        District district = districtRepository.findByDistrictName(districtName)
            .orElseThrow(() -> new IllegalArgumentException("District not found: " + districtName));
        
        MandiLocation mandi = MandiLocation.builder()
                .mandiName(mandiName)
                .state(state)
                .district(district)
                .isActive(true)
                .build();
        return mandiLocationRepository.save(mandi);
    }

    public List<MandiLocation> getMandisByState(String stateName) {
        State state = stateRepository.findByStateName(stateName)
            .orElseThrow(() -> new IllegalArgumentException("State not found: " + stateName));
        return mandiLocationRepository.findByStateAndIsActiveTrue(state);
    }

    public List<MandiLocation> getMandisByDistrict(String stateName, String districtName) {
        State state = stateRepository.findByStateName(stateName)
            .orElseThrow(() -> new IllegalArgumentException("State not found: " + stateName));
        District district = districtRepository.findByDistrictName(districtName)
            .orElseThrow(() -> new IllegalArgumentException("District not found: " + districtName));
        return mandiLocationRepository.findByStateAndDistrictAndIsActiveTrue(state, district);
    }

    public Optional<MandiLocation> getMandiByName(String mandiName) {
        return mandiLocationRepository.findByMandiNameAndIsActiveTrue(mandiName);
    }

    public List<MandiLocation> getAllMandis() {
        return mandiLocationRepository.findByIsActiveTrue();
    }
}
