package com.farmer.mandi.service;

import com.farmer.mandi.entity.MandiMarketData;
import com.farmer.mandi.repository.MandiMarketDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Service for importing Mandi market data from CSV
 */
@Service
@Slf4j
public class MandiDataImportService {
    
    @Autowired
    private MandiMarketDataRepository mandiMarketDataRepository;
    
    /**
     * Import market data from CSV file
     */
    @Transactional
    public ImportResult importFromCsv(MultipartFile file) {
        ImportResult result = new ImportResult();
        
        try {
            List<MandiMarketData> records = parseCsvFile(file);
            result.setTotalRecords(records.size());
            
            // Remove duplicates
            Set<String> uniqueKeys = new HashSet<>();
            List<MandiMarketData> uniqueRecords = new ArrayList<>();
            
            for (MandiMarketData record : records) {
                String key = generateKey(record);
                if (uniqueKeys.add(key)) {
                    uniqueRecords.add(record);
                }
            }
            
            result.setDuplicatesRemoved(records.size() - uniqueRecords.size());
            
            // Save to database
            List<MandiMarketData> savedRecords = mandiMarketDataRepository.saveAll(uniqueRecords);
            result.setSuccessfulImports(savedRecords.size());
            result.setStatus("SUCCESS");
            result.setMessage("Successfully imported " + savedRecords.size() + " records");
            
            log.info("Imported {} market data records", savedRecords.size());
            
        } catch (Exception e) {
            result.setStatus("ERROR");
            result.setMessage("Error importing data: " + e.getMessage());
            result.setErrorDetails(e.toString());
            log.error("Error importing market data", e);
        }
        
        return result;
    }
    
    /**
     * Parse CSV file and return list of MandiMarketData
     */
    private List<MandiMarketData> parseCsvFile(MultipartFile file) throws Exception {
        List<MandiMarketData> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip header
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                
                try {
                    MandiMarketData record = parseCsvLine(line);
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
    private MandiMarketData parseCsvLine(String line) {
        String[] parts = parseCSVLine(line);
        
        if (parts.length < 6) {
            return null;
        }
        
        try {
            return MandiMarketData.builder()
                .state(parts[0].trim())
                .district(parts[1].trim())
                .market(parts[2].trim())
                .commodity(parts[3].trim())
                .variety(parts[4].trim())
                .grade(parts[5].trim())
                .build();
        } catch (Exception e) {
            log.warn("Error creating MandiMarketData from line: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse CSV line handling quoted values
     */
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
    
    /**
     * Generate unique key for deduplication
     */
    private String generateKey(MandiMarketData record) {
        return String.format("%s|%s|%s|%s|%s|%s",
            record.getState(),
            record.getDistrict(),
            record.getMarket(),
            record.getCommodity(),
            record.getVariety(),
            record.getGrade()
        );
    }
    
    /**
     * Clear all market data
     */
    @Transactional
    public void clearAllData() {
        mandiMarketDataRepository.deleteAll();
        log.info("Cleared all market data");
    }
    
    /**
     * Get import statistics
     */
    public ImportStatistics getStatistics() {
        long totalRecords = mandiMarketDataRepository.count();
        List<String> states = mandiMarketDataRepository.findAllStates();
        List<String> commodities = mandiMarketDataRepository.findAllCommodities();
        List<String> varieties = mandiMarketDataRepository.findAllVarieties();
        List<String> grades = mandiMarketDataRepository.findAllGrades();
        
        return ImportStatistics.builder()
            .totalRecords(totalRecords)
            .uniqueStates(states.size())
            .uniqueCommodities(commodities.size())
            .uniqueVarieties(varieties.size())
            .uniqueGrades(grades.size())
            .states(states)
            .commodities(commodities)
            .varieties(varieties)
            .grades(grades)
            .build();
    }
    
    /**
     * Result class for import operation
     */
    public static class ImportResult {
        private String status;
        private String message;
        private int totalRecords;
        private int successfulImports;
        private int duplicatesRemoved;
        private String errorDetails;
        
        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
        
        public int getSuccessfulImports() { return successfulImports; }
        public void setSuccessfulImports(int successfulImports) { this.successfulImports = successfulImports; }
        
        public int getDuplicatesRemoved() { return duplicatesRemoved; }
        public void setDuplicatesRemoved(int duplicatesRemoved) { this.duplicatesRemoved = duplicatesRemoved; }
        
        public String getErrorDetails() { return errorDetails; }
        public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
    }
    
    /**
     * Statistics class
     */
    @lombok.Data
    @lombok.Builder
    public static class ImportStatistics {
        private long totalRecords;
        private int uniqueStates;
        private int uniqueCommodities;
        private int uniqueVarieties;
        private int uniqueGrades;
        private List<String> states;
        private List<String> commodities;
        private List<String> varieties;
        private List<String> grades;
    }
}
