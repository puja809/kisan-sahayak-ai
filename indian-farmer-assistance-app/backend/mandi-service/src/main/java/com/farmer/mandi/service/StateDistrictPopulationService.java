package com.farmer.mandi.service;

import com.farmer.mandi.entity.District;
import com.farmer.mandi.entity.State;
import com.farmer.mandi.repository.DistrictRepository;
import com.farmer.mandi.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing state and district population data.
 * Handles initialization and updates of demographic information.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StateDistrictPopulationService {

    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;

    /**
     * Initialize state and district population data from CSV.
     * Extracts unique states and districts from mandi market data.
     */
    @Transactional
    public void initializeStateDistrictData(List<Map<String, String>> csvData) {
        Map<String, Set<String>> stateDistrictMap = new HashMap<>();
        
        // Extract unique states and districts from CSV
        for (Map<String, String> row : csvData) {
            String state = row.get("State");
            String district = row.get("District");
            
            if (state != null && !state.isEmpty() && district != null && !district.isEmpty()) {
                stateDistrictMap.computeIfAbsent(state, k -> new HashSet<>()).add(district);
            }
        }
        
        // Populate states and districts
        for (Map.Entry<String, Set<String>> entry : stateDistrictMap.entrySet()) {
            String stateName = entry.getKey();
            Set<String> districts = entry.getValue();
            
            // Create or get state
            State state = stateRepository.findByStateName(stateName)
                    .orElseGet(() -> {
                        State newState = State.builder()
                                .stateName(stateName)
                                .stateCode(generateStateCode(stateName))
                                .isActive(true)
                                .build();
                        return stateRepository.save(newState);
                    });
            
            if (state != null) {
                // Create or get districts
                for (String districtName : districts) {
                    districtRepository.findByDistrictName(districtName)
                            .orElseGet(() -> {
                                District newDistrict = District.builder()
                                        .districtName(districtName)
                                        .districtCode(generateDistrictCode(districtName))
                                        .state(state)
                                        .isActive(true)
                                        .build();
                                return districtRepository.save(newDistrict);
                            });
                }
            }
        }
        
        log.info("State and district population data initialized successfully");
    }

    /**
     * Get all states
     */
    public List<State> getAllStates() {
        return stateRepository.findAll();
    }

    /**
     * Get state by name
     */
    public Optional<State> getStateByName(String stateName) {
        return stateRepository.findByStateName(stateName);
    }

    /**
     * Get districts by state
     */
    public List<District> getDistrictsByState(String stateName) {
        return stateRepository.findByStateName(stateName)
                .map(districtRepository::findByState)
                .orElse(Collections.emptyList());
    }

    /**
     * Get district by name
     */
    public Optional<District> getDistrictByName(String districtName) {
        return districtRepository.findByDistrictName(districtName);
    }

    /**
     * Update state population data
     */
    @Transactional
    public State updateStatePopulation(String stateName, Long population, Double areaSqKm, Double literacyRate) {
        State state = stateRepository.findByStateName(stateName)
                .orElseThrow(() -> new IllegalArgumentException("State not found: " + stateName));
        
        state.setPopulation(population);
        state.setAreaSqKm(areaSqKm);
        state.setLiteracyRate(literacyRate);
        
        return stateRepository.save(state);
    }

    /**
     * Update district population data
     */
    @Transactional
    public District updateDistrictPopulation(String districtName, Long population, Double areaSqKm, Double literacyRate) {
        District district = districtRepository.findByDistrictName(districtName)
                .orElseThrow(() -> new IllegalArgumentException("District not found: " + districtName));
        
        district.setPopulation(population);
        district.setAreaSqKm(areaSqKm);
        district.setLiteracyRate(literacyRate);
        
        return districtRepository.save(district);
    }

    private String generateStateCode(String stateName) {
        String cleaned = stateName.replaceAll("\\s+", "").toUpperCase();
        return cleaned.substring(0, Math.min(10, cleaned.length()));
    }

    private String generateDistrictCode(String districtName) {
        String cleaned = districtName.replaceAll("\\s+", "").toUpperCase();
        return cleaned.substring(0, Math.min(10, cleaned.length()));
    }
}
