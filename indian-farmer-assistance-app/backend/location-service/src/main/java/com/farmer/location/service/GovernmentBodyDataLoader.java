package com.farmer.location.service;

import com.farmer.location.entity.GovernmentBody;
import com.farmer.location.repository.GovernmentBodyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GovernmentBodyDataLoader {
    
    private final GovernmentBodyRepository governmentBodyRepository;
    
    @Transactional
    public void loadGovernmentBodiesFromCsv() {
        try {
            if (governmentBodyRepository.count() > 0) {
                log.info("Government bodies data already exists, skipping CSV load");
                return;
            }
            
            log.info("Loading government bodies from CSV...");
            ClassPathResource resource = new ClassPathResource("data/india_agri_office_directory_expanded.csv");
            
            List<GovernmentBody> bodies = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                boolean isHeader = true;
                
                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }
                    
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    
                    if (parts.length >= 7) {
                        GovernmentBody body = GovernmentBody.builder()
                            .state(parts[0].trim())
                            .district(parts[1].trim())
                            .districtOfficer(parts[2].trim())
                            .districtPhone(parts[3].trim())
                            .email(parts[4].trim())
                            .kvkPhone(parts[5].trim())
                            .sampleVillage(parts[6].trim())
                            .build();
                        
                        bodies.add(body);
                    }
                }
                
                if (!bodies.isEmpty()) {
                    governmentBodyRepository.saveAll(bodies);
                    log.info("Successfully loaded {} government bodies from CSV", bodies.size());
                } else {
                    log.warn("No government bodies found in CSV");
                }
            }
        } catch (Exception e) {
            log.error("Error loading government bodies from CSV", e);
        }
    }
}
