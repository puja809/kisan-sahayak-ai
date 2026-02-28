package com.farmer.mandi.controller;

import com.farmer.mandi.dto.DistrictDto;
import com.farmer.mandi.dto.StateDto;
import com.farmer.mandi.entity.District;
import com.farmer.mandi.entity.State;
import com.farmer.mandi.service.StateDistrictPopulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for state and district population data endpoints
 */
@RestController
@RequestMapping("/api/v1/mandi/states-districts")
@RequiredArgsConstructor
public class StateDistrictController {

    private final StateDistrictPopulationService stateDistrictService;

    /**
     * Get all states
     */
    @GetMapping("/states")
    public ResponseEntity<List<StateDto>> getAllStates() {
        List<StateDto> states = stateDistrictService.getAllStates()
                .stream()
                .map(this::convertToStateDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(states);
    }

    /**
     * Get state by name
     */
    @GetMapping("/states/{stateName}")
    public ResponseEntity<StateDto> getStateByName(@PathVariable String stateName) {
        return stateDistrictService.getStateByName(stateName)
                .map(state -> ResponseEntity.ok(convertToStateDto(state)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get districts by state
     */
    @GetMapping("/states/{stateName}/districts")
    public ResponseEntity<List<DistrictDto>> getDistrictsByState(@PathVariable String stateName) {
        List<DistrictDto> districts = stateDistrictService.getDistrictsByState(stateName)
                .stream()
                .map(this::convertToDistrictDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(districts);
    }

    /**
     * Get district by name
     */
    @GetMapping("/districts/{districtName}")
    public ResponseEntity<DistrictDto> getDistrictByName(@PathVariable String districtName) {
        return stateDistrictService.getDistrictByName(districtName)
                .map(district -> ResponseEntity.ok(convertToDistrictDto(district)))
                .orElse(ResponseEntity.notFound().build());
    }

    private StateDto convertToStateDto(State state) {
        return StateDto.builder()
                .id(state.getId())
                .stateName(state.getStateName())
                .isActive(state.getIsActive())
                .build();
    }

    private DistrictDto convertToDistrictDto(District district) {
        return DistrictDto.builder()
                .id(district.getId())
                .districtName(district.getDistrictName())
                .stateId(district.getState().getId())
                .stateName(district.getState().getStateName())
                .isActive(district.getIsActive())
                .build();
    }
}
