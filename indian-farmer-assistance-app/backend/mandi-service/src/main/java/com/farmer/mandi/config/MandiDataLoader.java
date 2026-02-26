package com.farmer.mandi.config;

import com.farmer.mandi.entity.MandiMarketData;
import com.farmer.mandi.repository.MandiMarketDataRepository;
import com.farmer.mandi.service.StateDistrictPopulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class MandiDataLoader implements CommandLineRunner {

    private final MandiMarketDataRepository mandiMarketDataRepository;
    private final StateDistrictPopulationService stateDistrictService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeMandiData();
    }

    private void initializeMandiData() {
        try {
            long count = mandiMarketDataRepository.count();
            if (count > 0) {
                log.info("Mandi data already exists, skipping initialization");
                return;
            }

            List<MandiMarketData> records = loadCsvData();
            if (records.isEmpty()) {
                log.warn("No data loaded from CSV");
                return;
            }

            // Initialize state and district data
            List<Map<String, String>> csvDataMaps = new ArrayList<>();
            for (MandiMarketData record : records) {
                Map<String, String> map = new HashMap<>();
                map.put("State", record.getState());
                map.put("District", record.getDistrict());
                csvDataMaps.add(map);
            }
            stateDistrictService.initializeStateDistrictData(csvDataMaps);

            // Save market data
            mandiMarketDataRepository.saveAll(records);
            log.info("Initialized {} mandi market records", records.size());

        } catch (Exception e) {
            log.error("Error initializing mandi data: {}", e.getMessage(), e);
        }
    }

    private List<MandiMarketData> loadCsvData() throws Exception {
        List<MandiMarketData> records = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("sample_mandi_data.csv");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    MandiMarketData record = MandiMarketData.builder()
                            .state(parts[0].trim())
                            .district(parts[1].trim())
                            .market(parts[2].trim())
                            .commodity(parts[3].trim())
                            .variety(parts[4].trim())
                            .grade(parts[5].trim())
                            .build();
                    records.add(record);
                }
            }
        }

        return records;
    }
}
