package com.farmer.location.controller;

import com.farmer.location.entity.GovernmentBody;
import com.farmer.location.service.GovernmentBodyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/government-bodies")
@RequiredArgsConstructor
@Slf4j
public class GovernmentBodyController {
    
    private final GovernmentBodyService governmentBodyService;
    
    @GetMapping("/search")
    public ResponseEntity<List<GovernmentBody>> searchByLocation(
            @RequestParam String state,
            @RequestParam(required = false) String district) {
        
        log.info("Searching government bodies for state: {}, district: {}", state, district);
        
        List<GovernmentBody> bodies;
        if (district != null && !district.isEmpty()) {
            bodies = governmentBodyService.findByStateAndDistrict(state, district);
        } else {
            bodies = governmentBodyService.findByState(state);
        }
        
        return ResponseEntity.ok(bodies);
    }
    
    @GetMapping("/state/{state}")
    public ResponseEntity<List<GovernmentBody>> getByState(@PathVariable String state) {
        log.info("Getting government bodies for state: {}", state);
        List<GovernmentBody> bodies = governmentBodyService.findByState(state);
        return ResponseEntity.ok(bodies);
    }
    
    @GetMapping("/district/{district}")
    public ResponseEntity<List<GovernmentBody>> getByDistrict(@PathVariable String district) {
        log.info("Getting government bodies for district: {}", district);
        List<GovernmentBody> bodies = governmentBodyService.findByDistrict(district);
        return ResponseEntity.ok(bodies);
    }
    
    @GetMapping("/state/{state}/district/{district}")
    public ResponseEntity<List<GovernmentBody>> getByStateAndDistrict(
            @PathVariable String state,
            @PathVariable String district) {
        log.info("Getting government bodies for state: {}, district: {}", state, district);
        List<GovernmentBody> bodies = governmentBodyService.findByStateAndDistrict(state, district);
        return ResponseEntity.ok(bodies);
    }
}
