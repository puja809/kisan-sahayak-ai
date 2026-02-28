package com.farmer.location.service;

import com.farmer.location.entity.GovernmentBody;
import com.farmer.location.repository.GovernmentBodyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GovernmentBodyService {
    
    private final GovernmentBodyRepository governmentBodyRepository;
    
    public List<GovernmentBody> findByStateAndDistrict(String state, String district) {
        log.info("Finding government bodies for state: {}, district: {}", state, district);
        
        // First try exact match
        List<GovernmentBody> bodies = governmentBodyRepository.findByStateAndDistrict(state, district);
        
        if (!bodies.isEmpty()) {
            log.info("Found {} government bodies for state: {}, district: {}", bodies.size(), state, district);
            return bodies;
        }
        
        // If no exact match, try by state only
        log.info("No exact match found, searching by state only");
        bodies = governmentBodyRepository.findByState(state);
        
        if (!bodies.isEmpty()) {
            log.info("Found {} government bodies for state: {}", bodies.size(), state);
        } else {
            log.warn("No government bodies found for state: {}", state);
        }
        
        return bodies;
    }
    
    public List<GovernmentBody> findByDistrict(String district) {
        log.info("Finding government bodies for district: {}", district);
        return governmentBodyRepository.findByDistrict(district);
    }
    
    public List<GovernmentBody> findByState(String state) {
        log.info("Finding government bodies for state: {}", state);
        return governmentBodyRepository.findByState(state);
    }
    
    @Transactional
    public GovernmentBody createGovernmentBody(GovernmentBody body) {
        log.info("Creating government body for state: {}, district: {}", body.getState(), body.getDistrict());
        return governmentBodyRepository.save(body);
    }
    
    @Transactional
    public GovernmentBody updateGovernmentBody(Long id, GovernmentBody body) {
        log.info("Updating government body id: {}", id);
        
        Optional<GovernmentBody> existing = governmentBodyRepository.findById(id);
        if (existing.isEmpty()) {
            log.warn("Government body not found for id: {}", id);
            return null;
        }
        
        GovernmentBody toUpdate = existing.get();
        toUpdate.setState(body.getState());
        toUpdate.setDistrict(body.getDistrict());
        toUpdate.setDistrictOfficer(body.getDistrictOfficer());
        toUpdate.setDistrictPhone(body.getDistrictPhone());
        toUpdate.setEmail(body.getEmail());
        toUpdate.setKvkPhone(body.getKvkPhone());
        toUpdate.setSampleVillage(body.getSampleVillage());
        
        return governmentBodyRepository.save(toUpdate);
    }
    
    @Transactional
    public boolean deleteGovernmentBody(Long id) {
        log.info("Deleting government body id: {}", id);
        
        if (!governmentBodyRepository.existsById(id)) {
            log.warn("Government body not found for deletion, id: {}", id);
            return false;
        }
        
        governmentBodyRepository.deleteById(id);
        return true;
    }
}
