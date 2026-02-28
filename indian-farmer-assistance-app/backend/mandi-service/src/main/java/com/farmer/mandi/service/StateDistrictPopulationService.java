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

@Service
@RequiredArgsConstructor
@Slf4j
public class StateDistrictPopulationService {

    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;

    @Transactional
    public void initializeStateDistrictData(List<Map<String, String>> csvData) {
        Map<String, Set<String>> stateDistrictMap = new HashMap<>();
        
        for (Map<String, String> row : csvData) {
            String state = row.get("State");
            String district = row.get("District");
            
            if (state != null && !state.isEmpty() && district != null && !district.isEmpty()) {
                stateDistrictMap.computeIfAbsent(state, k -> new HashSet<>()).add(district);
            }
        }
        
        for (Map.Entry<String, Set<String>> entry : stateDistrictMap.entrySet()) {
            String stateName = entry.getKey();
            Set<String> districts = entry.getValue();
            
            State state = stateRepository.findByStateName(stateName)
                    .orElseGet(() -> {
                        State newState = State.builder()
                                .stateName(stateName)
                                .isActive(true)
                                .build();
                        return stateRepository.save(newState);
                    });
            
            for (String districtName : districts) {
                districtRepository.findByDistrictName(districtName)
                        .orElseGet(() -> {
                            District newDistrict = District.builder()
                                    .districtName(districtName)
                                    .state(state)
                                    .isActive(true)
                                    .build();
                            return districtRepository.save(newDistrict);
                        });
            }
        }
        
        log.info("State and district data initialized successfully");
    }

    public List<State> getAllStates() {
        return stateRepository.findAll();
    }

    public Optional<State> getStateByName(String stateName) {
        return stateRepository.findByStateName(stateName);
    }

    public List<District> getDistrictsByState(String stateName) {
        return stateRepository.findByStateName(stateName)
                .map(districtRepository::findByState)
                .orElse(Collections.emptyList());
    }

    public Optional<District> getDistrictByName(String districtName) {
        return districtRepository.findByDistrictName(districtName);
    }
}
