package com.farmer.mandi.controller;

import com.farmer.mandi.service.MandiDataImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for Mandi market data import
 */
@RestController
@RequestMapping("/api/mandi/import")
@Tag(name = "Mandi Data Import", description = "APIs for importing market data")
@Slf4j
public class MandiDataImportController {
    
    @Autowired
    private MandiDataImportService mandiDataImportService;
    
    /**
     * Import market data from CSV file
     */
    @PostMapping(value = "/csv", consumes = "multipart/form-data", produces = "application/json")
    @Operation(
        summary = "Import market data from CSV",
        description = "Upload CSV file with market data. File should contain columns: State, District, Market, Commodity, Variety, Grade"
    )
    public ResponseEntity<?> importCsv(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File is empty"));
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".csv")) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File must be CSV format"));
            }
            
            MandiDataImportService.ImportResult result = mandiDataImportService.importFromCsv(file);
            
            if ("SUCCESS".equals(result.getStatus())) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error importing CSV", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error importing file: " + e.getMessage()));
        }
    }
    
    /**
     * Get import statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get import statistics", description = "Get statistics about imported data")
    public ResponseEntity<?> getStatistics() {
        try {
            MandiDataImportService.ImportStatistics stats = mandiDataImportService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error getting statistics: " + e.getMessage()));
        }
    }
    
    /**
     * Clear all imported data
     */
    @DeleteMapping("/clear")
    @Operation(summary = "Clear all data", description = "Delete all imported market data")
    public ResponseEntity<?> clearData() {
        try {
            mandiDataImportService.clearAllData();
            return ResponseEntity.ok(new SuccessResponse("All data cleared successfully"));
        } catch (Exception e) {
            log.error("Error clearing data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error clearing data: " + e.getMessage()));
        }
    }
    
    /**
     * Error response class
     */
    public static class ErrorResponse {
        private String error;
        private long timestamp;
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getError() { return error; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Success response class
     */
    public static class SuccessResponse {
        private String message;
        private long timestamp;
        
        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}
