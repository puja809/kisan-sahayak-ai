package com.farmer.mandi.config;

import com.farmer.mandi.repository.*;
import com.farmer.mandi.service.MandiDataImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Listener that initializes mandi data on application startup.
 * Checks if data exists in database, if not loads from CSV file.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializationListener {

    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final MandiLocationRepository mandiLocationRepository;
    private final CommodityRepository commodityRepository;
    private final VarietyRepository varietyRepository;
    private final GradeRepository gradeRepository;
    private final MandiDataImportService mandiDataImportService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        log.info("Starting data initialization check...");

        try {
            // Check if data already exists
            if (isDataAlreadyLoaded()) {
                log.info("Data already exists in database. Skipping initialization.");
                return;
            }

            log.info("Data not found in database. Loading from CSV file...");
            loadDataFromCsv();
            log.info("Data initialization completed successfully.");

        } catch (Exception e) {
            log.error("Error during data initialization: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if data is already loaded in the database.
     * Returns true if all entities have data.
     */
    private boolean isDataAlreadyLoaded() {
        long stateCount = stateRepository.count();
        long districtCount = districtRepository.count();
        long mandiCount = mandiLocationRepository.count();
        long commodityCount = commodityRepository.count();
        long varietyCount = varietyRepository.count();
        long gradeCount = gradeRepository.count();

        log.info("Current data counts - States: {}, Districts: {}, Mandis: {}, Commodities: {}, Varieties: {}, Grades: {}",
                stateCount, districtCount, mandiCount, commodityCount, varietyCount, gradeCount);

        return stateCount > 0 && districtCount > 0 && mandiCount > 0 && 
               commodityCount > 0 && varietyCount > 0 && gradeCount > 0;
    }

    /**
     * Load data from CSV file.
     */
    private void loadDataFromCsv() throws Exception {
        try {
            // Try to load from classpath resources
            ClassPathResource resource = new ClassPathResource("sample_mandi_data.csv");
            if (resource.exists()) {
                log.info("Loading data from classpath: sample_mandi_data.csv");
                mandiDataImportService.importFromFile(resource.getFile());
                return;
            }
        } catch (Exception e) {
            log.debug("Could not load from classpath: {}", e.getMessage());
        }

        // Try to load from documents folder
        try {
            File csvFile = new File("documents/9ef84268-d588-465a-a308-a864a43d0070.csv");
            if (csvFile.exists()) {
                log.info("Loading data from file: {}", csvFile.getAbsolutePath());
                mandiDataImportService.importFromFile(csvFile);
                return;
            }
        } catch (Exception e) {
            log.debug("Could not load from documents folder: {}", e.getMessage());
        }

        log.warn("CSV file not found. Data initialization skipped.");
    }
}
