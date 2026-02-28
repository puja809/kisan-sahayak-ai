package com.farmer.mandi.service;

import com.farmer.mandi.entity.*;
import com.farmer.mandi.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Service for parsing CSV files and importing mandi market data
 * CSV Format: State, District, Market, Commodity, Variety, Grade
 */
@Service
@Slf4j
public class MandiDataImportService {
    
    @Autowired
    private StateRepository stateRepository;
    
    @Autowired
    private DistrictRepository districtRepository;
    
    @Autowired
    private MandiLocationRepository mandiLocationRepository;
    
    @Autowired
    private CommodityRepository commodityRepository;
    
    @Autowired
    private VarietyRepository varietyRepository;
    
    @Autowired
    private GradeRepository gradeRepository;
    
    /**
     * Parse and validate CSV file, then import data
     */
    @Transactional
    public ImportResult validateCsv(MultipartFile file) {
        ImportResult result = new ImportResult();
        
        try {
            List<CsvRecord> records = parseCsvFile(file);
            result.setTotalRecords(records.size());
            
            int successCount = 0;
            for (CsvRecord record : records) {
                try {
                    importRecord(record);
                    successCount++;
                } catch (Exception e) {
                    log.warn("Error importing record: {}", record, e);
                }
            }
            
            result.setSuccessfulImports(successCount);
            result.setStatus("SUCCESS");
            result.setMessage("CSV imported successfully with " + successCount + " records");
            
            log.info("Imported {} records from CSV", successCount);
            
        } catch (Exception e) {
            result.setStatus("ERROR");
            result.setMessage("Error importing CSV: " + e.getMessage());
            result.setErrorDetails(e.toString());
            log.error("Error importing CSV", e);
        }
        
        return result;
    }
    /**
     * Import data from a File (used during startup initialization)
     */
    public void importFromFile(File file) throws Exception {
        log.info("Starting import from file: {}", file.getAbsolutePath());

        List<CsvRecord> records = parseCsvFile(file);
        log.info("Parsed {} records from file", records.size());

        int successCount = 0;
        for (CsvRecord record : records) {
            try {
                importRecord(record);
                successCount++;
            } catch (Exception e) {
                log.warn("Error importing record: {}", record, e);
            }
        }

        log.info("Successfully imported {} records from file", successCount);
    }
    
    /**
     * Import a single CSV record into the database
     */
    @Transactional
    private void importRecord(CsvRecord record) {
        // Get or create State
        State state = stateRepository.findByStateName(record.getState())
            .orElseGet(() -> {
                State newState = State.builder()
                    .stateName(record.getState())
                    .isActive(true)
                    .build();
                return stateRepository.save(newState);
            });
        
        // Get or create District
        District district = districtRepository.findByDistrictName(record.getDistrict())
            .orElseGet(() -> {
                District newDistrict = District.builder()
                    .districtName(record.getDistrict())
                    .state(state)
                    .isActive(true)
                    .build();
                return districtRepository.save(newDistrict);
            });
        
        // Get or create MandiLocation (Market)
        mandiLocationRepository.findByMandiNameAndIsActiveTrue(record.getMarket())
            .orElseGet(() -> {
                MandiLocation newMarket = MandiLocation.builder()
                    .mandiName(record.getMarket())
                    .state(state)
                    .district(district)
                    .isActive(true)
                    .build();
                return mandiLocationRepository.save(newMarket);
            });
        
        // Get or create Commodity
        Commodity commodity = commodityRepository.findByCommodityName(record.getCommodity())
            .orElseGet(() -> {
                Commodity newCommodity = Commodity.builder()
                    .commodityName(record.getCommodity())
                    .isActive(true)
                    .build();
                return commodityRepository.save(newCommodity);
            });
        
        // Get or create Variety
        Variety variety = varietyRepository.findByCommodityAndVarietyName(commodity, record.getVariety())
            .orElseGet(() -> {
                Variety newVariety = Variety.builder()
                    .varietyName(record.getVariety())
                    .commodity(commodity)
                    .isActive(true)
                    .build();
                return varietyRepository.save(newVariety);
            });
        
        // Get or create Grade
        Grade grade = gradeRepository.findByVarietyAndGradeName(variety, record.getGrade());
        if (grade == null) {
            grade = Grade.builder()
                .gradeName(record.getGrade())
                .variety(variety)
                .isActive(true)
                .build();
            gradeRepository.save(grade);
        }
    }
    
    /**
     * Parse CSV file from MultipartFile into records
     */
    private List<CsvRecord> parseCsvFile(MultipartFile file) throws Exception {
        List<CsvRecord> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                
                try {
                    CsvRecord record = parseCsvLine(line);
                    if (record != null) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing line {}: {}", lineNumber, e.getMessage());
                }
            }
        }
        
        return records;
    }

    /**
     * Parse CSV file from File object into records
     */
    private List<CsvRecord> parseCsvFile(File file) throws Exception {
        List<CsvRecord> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                
                try {
                    CsvRecord record = parseCsvLine(line);
                    if (record != null) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing line {}: {}", lineNumber, e.getMessage());
                }
            }
        }
        
        return records;
    }
    
    /**
     * Parse a single CSV line
     */
    private CsvRecord parseCsvLine(String line) {
        String[] parts = line.split(",");
        
        if (parts.length < 6) {
            return null;
        }
        
        try {
            return CsvRecord.builder()
                .state(parts[0].trim())
                .district(parts[1].trim())
                .market(parts[2].trim())
                .commodity(parts[3].trim())
                .variety(parts[4].trim())
                .grade(parts[5].trim())
                .build();
        } catch (Exception e) {
            log.warn("Error creating CsvRecord from line: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * CSV Record DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class CsvRecord {
        private String state;
        private String district;
        private String market;
        private String commodity;
        private String variety;
        private String grade;
    }
    
    /**
     * Import Result DTO
     */
    public static class ImportResult {
        private String status;
        private String message;
        private int totalRecords;
        private int successfulImports;
        private String errorDetails;
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
        
        public int getSuccessfulImports() { return successfulImports; }
        public void setSuccessfulImports(int successfulImports) { this.successfulImports = successfulImports; }
        
        public String getErrorDetails() { return errorDetails; }
        public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
    }
}
