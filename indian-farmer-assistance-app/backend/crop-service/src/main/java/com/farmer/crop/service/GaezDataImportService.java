package com.farmer.crop.service;

import com.farmer.crop.repository.GaezCropDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for importing GAEZ v4 data into the application.
 * 
 * GAEZ (Global Agro-Ecological Zones) data is typically provided as downloadable
 * datasets from FAO/IIASA. This service handles importing data from various sources
 * including CSV files, database imports, and external data providers.
 * 
 * The service supports:
 * - CSV file import
 * - Data validation and transformation
 * - Bulk insert with transaction management
 * - Incremental updates
 * 
 * Validates: Requirements 2.2, 2.3
 */
@Service
@Transactional
public class GaezDataImportService {

    private static final Logger logger = LoggerFactory.getLogger(GaezDataImportService.class);
    private static final String GAEZ_VERSION = "v4";
    private static final String DATA_RESOLUTION = "5 arc-min";

    private final GaezCropDataRepository gaezCropDataRepository;

    public GaezDataImportService(GaezCropDataRepository gaezCropDataRepository) {
        this.gaezCropDataRepository = gaezCropDataRepository;
    }

    /**
     * Import GAEZ data from a CSV file.
     * 
     * Expected CSV format:
     * zone_code,crop_code,crop_name,overall_score,climate_score,soil_score,
     * terrain_score,water_score,rainfed_yield,irrigated_yield,water_requirements,
     * growing_days,kharif,rabi,zaid,climate_risk
     * 
     * @param inputStream CSV file input stream
     * @param fileName Name of the file for logging
     * @return Import result with statistics
     */
    public ImportResult importFromCsv(InputStream inputStream, String fileName) {
        logger.info("Starting GAEZ data import from file: {}", fileName);
        
        int totalRecords = 0;
        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            // Skip header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                errors.add("Empty CSV file");
                return new ImportResult(0, 0, errors.size(), errors);
            }

            // Process each line
            String line;
            while ((line = reader.readLine()) != null) {
                totalRecords++;
                try {
                    GaezCropData data = parseCsvLine(line);
                    if (data != null) {
                        gaezCropDataRepository.save(data);
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = String.format("Error processing line %d: %s - %s", 
                            totalRecords, e.getMessage(), line);
                    errors.add(errorMsg);
                    logger.warn(errorMsg);
                }
            }

            logger.info("GAEZ import completed. Total: {}, Success: {}, Errors: {}", 
                    totalRecords, successCount, errorCount);

        } catch (Exception e) {
            logger.error("Error importing GAEZ data: {}", e.getMessage(), e);
            errors.add("Import failed: " + e.getMessage());
            errorCount = totalRecords;
        }

        return new ImportResult(totalRecords, successCount, errorCount, errors);
    }

    /**
     * Parse a single CSV line into GaezCropData entity.
     * 
     * @param line CSV line
     * @return GaezCropData entity or null if parsing fails
     */
    private GaezCropData parseCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] fields = line.split(",");
        if (fields.length < 15) {
            throw new IllegalArgumentException("Insufficient fields in CSV line");
        }

        try {
            GaezCropData data = GaezCropData.builder()
                    .zoneCode(fields[0].trim())
                    .cropCode(fields[1].trim())
                    .cropName(fields[2].trim())
                    .overallSuitabilityScore(parseDecimal(fields[3]))
                    .climateSuitabilityScore(parseDecimal(fields[4]))
                    .soilSuitabilityScore(parseDecimal(fields[5]))
                    .terrainSuitabilityScore(parseDecimal(fields[6]))
                    .waterSuitabilityScore(parseDecimal(fields[7]))
                    .rainfedPotentialYield(parseDecimal(fields[8]))
                    .irrigatedPotentialYield(parseDecimal(fields[9]))
                    .waterRequirementsMm(parseDecimal(fields[10]))
                    .growingSeasonDays(parseInt(fields[11]))
                    .kharifSuitable(parseBoolean(fields[12]))
                    .rabiSuitable(parseBoolean(fields[13]))
                    .zaidSuitable(parseBoolean(fields[14]))
                    .climateRiskLevel(parseClimateRisk(fields[15]))
                    .dataVersion(GAEZ_VERSION)
                    .dataResolution(DATA_RESOLUTION)
                    .isActive(true)
                    .build();

            return data;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse CSV line: " + e.getMessage(), e);
        }
    }

    /**
     * Import GAEZ data for Indian agro-ecological zones.
     * This method seeds the database with pre-configured GAEZ data for India.
     * 
     * @return Number of records imported
     */
    public int seedIndianGaezData() {
        logger.info("Seeding GAEZ data for Indian agro-ecological zones");
        
        List<GaezCropData> indianGaezData = createIndianGaezData();
        gaezCropDataRepository.saveAll(indianGaezData);
        
        logger.info("Seeded {} GAEZ data records for India", indianGaezData.size());
        return indianGaezData.size();
    }

    /**
     * Create GAEZ data for Indian agro-ecological zones.
     * This is a sample dataset based on ICAR's agro-ecological classification.
     * 
     * @return List of GAEZ crop data for India
     */
    private List<GaezCropData> createIndianGaezData() {
        List<GaezCropData> dataList = new ArrayList<>();

        // Upper Gangetic Plain Region (AEZ-05) - Uttar Pradesh, Punjab, Haryana
        dataList.add(createGaezCropData("AEZ-05", "RICE", "Rice", 
                75.0, 80.0, 70.0,
                85.0, 65.0, 3500.0,
                4500.0, 1200.0, 140, true, false, false, "MEDIUM"));
        
        dataList.add(createGaezCropData("AEZ-05", "WHEAT", "Wheat", 
                90.0, 95.0, 85.0,
                90.0, 85.0, 4500.0,
                5500.0, 500.0, 120, false, true, false, "LOW"));
        
        dataList.add(createGaezCropData("AEZ-05", "COTTON", "Cotton", 
                70.0, 75.0, 65.0,
                80.0, 60.0, 2500.0,
                3500.0, 700.0, 180, true, false, false, "HIGH"));
        
        dataList.add(createGaezCropData("AEZ-05", "SUGARCANE", "Sugarcane", 
                85.0, 80.0, 90.0,
                85.0, 75.0, 70000.0,
                90000.0, 1500.0, 365, true, true, false, "MEDIUM"));
        
        dataList.add(createGaezCropData("AEZ-05", "MUSTARD", "Mustard", 
                88.0, 90.0, 85.0,
                90.0, 85.0, 1800.0,
                2500.0, 400.0, 110, false, true, false, "LOW"));

        // Lower Gangetic Plain Region (AEZ-06) - West Bengal, Bihar
        dataList.add(createGaezCropData("AEZ-06", "RICE", "Rice", 
                95.0, 95.0, 95.0,
                90.0, 90.0, 4000.0,
                5000.0, 1500.0, 150, true, false, false, "MEDIUM"));
        
        dataList.add(createGaezCropData("AEZ-06", "JUTE", "Jute", 
                90.0, 85.0, 95.0,
                90.0, 85.0, 2500.0,
                3000.0, 1800.0, 120, true, false, false, "LOW"));
        
        dataList.add(createGaezCropData("AEZ-06", "POTATO", "Potato", 
                85.0, 80.0, 90.0,
                85.0, 80.0, 30000.0,
                45000.0, 500.0, 90, false, true, false, "MEDIUM"));

        // Central Plateau and Hills Region (AEZ-09) - Madhya Pradesh, Maharashtra
        dataList.add(createGaezCropData("AEZ-09", "SOYBEAN", "Soybean", 
                85.0, 90.0, 80.0,
                85.0, 75.0, 2000.0,
                2800.0, 600.0, 100, true, false, false, "MEDIUM"));
        
        dataList.add(createGaezCropData("AEZ-09", "COTTON", "Cotton", 
                80.0, 85.0, 75.0,
                80.0, 70.0, 2800.0,
                3800.0, 650.0, 180, true, false, false, "HIGH"));
        
        dataList.add(createGaezCropData("AEZ-09", "PULSES", "Pulses (Tur/Arhar)", 
                75.0, 80.0, 70.0,
                75.0, 65.0, 1500.0,
                2000.0, 450.0, 120, true, false, false, "LOW"));

        // Western Dry Region (AEZ-02) - Rajasthan, Gujarat
        dataList.add(createGaezCropData("AEZ-02", "PEARL_MILLET", "Pearl Millet (Bajra)", 
                85.0, 90.0, 80.0,
                85.0, 75.0, 1500.0,
                2500.0, 350.0, 90, true, false, false, "HIGH"));
        
        dataList.add(createGaezCropData("AEZ-02", "CLUSTER_BEAN", "Cluster Bean (Guar)", 
                80.0, 85.0, 75.0,
                80.0, 70.0, 1200.0,
                1800.0, 300.0, 80, true, false, false, "LOW"));
        
        dataList.add(createGaezCropData("AEZ-02", "GROUNDNUT", "Groundnut", 
                70.0, 75.0, 65.0,
                70.0, 60.0, 1500.0,
                2500.0, 500.0, 110, true, false, false, "MEDIUM"));

        // Southern Plateau and Hills Region (AEZ-10) - Karnataka, Andhra Pradesh, Tamil Nadu
        dataList.add(createGaezCropData("AEZ-10", "RICE", "Rice", 
                80.0, 85.0, 75.0,
                80.0, 70.0, 3500.0,
                4500.0, 1200.0, 130, true, false, false, "MEDIUM"));
        
        dataList.add(createGaezCropData("AEZ-10", "GROUNDNUT", "Groundnut", 
                85.0, 90.0, 80.0,
                85.0, 75.0, 1800.0,
                2800.0, 500.0, 110, true, false, false, "MEDIUM"));
        
        dataList.add(createGaezCropData("AEZ-10", "SUNFLOWER", "Sunflower", 
                78.0, 80.0, 75.0,
                80.0, 70.0, 1500.0,
                2200.0, 450.0, 100, false, true, false, "LOW"));
        
        dataList.add(createGaezCropData("AEZ-10", "SUGARCANE", "Sugarcane", 
                82.0, 78.0, 88.0,
                85.0, 72.0, 65000.0,
                85000.0, 1400.0, 365, true, true, false, "MEDIUM"));

        // Eastern Coastal Plain (AEZ-13) - Odisha, Andhra Pradesh (coastal)
        dataList.add(createGaezCropData("AEZ-13", "RICE", "Rice", 
                92.0, 95.0, 90.0,
                88.0, 88.0, 4200.0,
                5200.0, 1300.0, 145, true, false, false, "MEDIUM"));
        
        dataList.add(createGaezCropData("AEZ-13", "CASHEW", "Cashew", 
                85.0, 88.0, 82.0,
                85.0, 80.0, 800.0,
                1200.0, 1000.0, 210, true, false, false, "LOW"));
        
        dataList.add(createGaezCropData("AEZ-13", "COCONUT", "Coconut", 
                88.0, 90.0, 85.0,
                88.0, 85.0, 6000.0,
                8000.0, 1600.0, 365, true, false, false, "LOW"));

        // Western Himalayan Region (AEZ-14) - Himachal Pradesh, Jammu & Kashmir, Uttarakhand
        dataList.add(createGaezCropData("AEZ-14", "WHEAT", "Wheat", 
                75.0, 80.0, 70.0,
                75.0, 70.0, 3000.0,
                4000.0, 450.0, 130, false, true, false, "LOW"));
        
        dataList.add(createGaezCropData("AEZ-14", "APPLE", "Apple", 
                90.0, 85.0, 92.0,
                88.0, 85.0, 15000.0,
                25000.0, 800.0, 200, false, false, false, "LOW"));
        
        dataList.add(createGaezCropData("AEZ-14", "MAIZE", "Maize", 
                78.0, 82.0, 75.0,
                78.0, 72.0, 3500.0,
                4500.0, 550.0, 100, true, false, false, "MEDIUM"));

        return dataList;
    }

    /**
     * Helper method to create GaezCropData with common values.
     */
    private GaezCropData createGaezCropData(String zoneCode, String cropCode, String cropName, Double overallScore, Double climateScore, Double soilScore, Double terrainScore, Double waterScore, Double rainfedYield, Double irrigatedYield, Double waterRequirements, int growingDays,
            boolean kharif, boolean rabi, boolean zaid, String climateRisk) {
        
        GaezCropData.SuitabilityClassification classification;
        if (overallScore.compareTo(80.0) >= 0) {
            classification = GaezCropData.SuitabilityClassification.HIGHLY_SUITABLE;
        } else if (overallScore.compareTo(60.0) >= 0) {
            classification = GaezCropData.SuitabilityClassification.SUITABLE;
        } else if (overallScore.compareTo(40.0) >= 0) {
            classification = GaezCropData.SuitabilityClassification.MARGINALLY_SUITABLE;
        } else {
            classification = GaezCropData.SuitabilityClassification.NOT_SUITABLE;
        }

        return GaezCropData.builder()
                .zoneCode(zoneCode)
                .cropCode(cropCode)
                .cropName(cropName)
                .overallSuitabilityScore(overallScore)
                .suitabilityClassification(classification)
                .climateSuitabilityScore(climateScore)
                .soilSuitabilityScore(soilScore)
                .terrainSuitabilityScore(terrainScore)
                .waterSuitabilityScore(waterScore)
                .rainfedPotentialYield(rainfedYield)
                .irrigatedPotentialYield(irrigatedYield)
                .waterRequirementsMm(waterRequirements)
                .growingSeasonDays(growingDays)
                .kharifSuitable(kharif)
                .rabiSuitable(rabi)
                .zaidSuitable(zaid)
                .climateRiskLevel(GaezCropData.ClimateRiskLevel.valueOf(climateRisk))
                .dataVersion(GAEZ_VERSION)
                .dataResolution(DATA_RESOLUTION)
                .isActive(true)
                .build();
    }

    /**
     * Check if GAEZ data exists for a zone.
     * 
     * @param zoneCode GAEZ zone code
     * @return true if data exists
     */
    public boolean hasGaezDataForZone(String zoneCode) {
        return gaezCropDataRepository.existsByZoneCodeAndIsActiveTrue(zoneCode);
    }

    /**
     * Get the count of GAEZ data records for a zone.
     * 
     * @param zoneCode GAEZ zone code
     * @return Count of records
     */
    public long getGaezDataCountForZone(String zoneCode) {
        return gaezCropDataRepository.countByZoneCodeAndIsActiveTrue(zoneCode);
    }

    /**
     * Import result container.
     */
    public record ImportResult(int totalRecords, int successCount, int errorCount, List<String> errors) {
        public boolean hasErrors() {
            return errorCount > 0;
        }
        
        public double getSuccessRate() {
            if (totalRecords == 0) return 0.0;
            return (double) successCount / totalRecords * 100;
        }
    }

    // Helper parsing methods
    private Double parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new Double(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return value.trim().equalsIgnoreCase("true") || value.trim().equals("1");
    }

    private GaezCropData.ClimateRiskLevel parseClimateRisk(String value) {
        if (value == null || value.trim().isEmpty()) {
            return GaezCropData.ClimateRiskLevel.MEDIUM;
        }
        try {
            return GaezCropData.ClimateRiskLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return GaezCropData.ClimateRiskLevel.MEDIUM;
        }
    }
}









