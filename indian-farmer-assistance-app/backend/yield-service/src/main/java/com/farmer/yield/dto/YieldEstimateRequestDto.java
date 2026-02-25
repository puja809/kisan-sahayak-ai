package com.farmer.yield.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for yield estimation.
 * 
 * Contains all inputs needed for yield prediction:
 * - Crop information (type, variety, sowing date, area)
 * - Growth stage
 * - Historical yield data reference
 * - Weather data reference
 * - Soil health data
 * - Irrigation information
 * - Pest/disease incidents
 * 
 * Validates: Requirements 11B.1, 11B.2, 11B.3, 11B.4, 11B.5, 11B.6
 */
public class YieldEstimateRequestDto {

    private String farmerId;
    private Long cropId;
    private String cropName;
    private String cropVariety;
    private LocalDate sowingDate;
    private BigDecimal areaAcres;
    private String growthStage;
    
    // Historical data reference
    private Boolean includeHistoricalData = true;
    private String historicalCropName; // For looking up past yields
    
    // Weather data (optional - will be fetched from weather service if not provided)
    private BigDecimal totalRainfallMm;
    private BigDecimal averageTemperatureCelsius;
    private Integer extremeWeatherEventsCount;
    private String weatherTrend; // "improving", "stable", "declining"
    
    // Soil health data (optional - will use defaults if not provided)
    private BigDecimal soilNitrogenKgHa;
    private BigDecimal soilPhosphorusKgHa;
    private BigDecimal soilPotassiumKgHa;
    private BigDecimal soilPh;
    
    // Irrigation information
    private String irrigationType; // "RAINFED", "DRIP", "SPRINKLER", "CANAL", "BOREWELL"
    private Integer irrigationFrequencyPerWeek;
    private BigDecimal waterAvailabilityIndex; // 0-1 scale
    
    // Pest/disease incidents
    private Integer pestIncidentCount;
    private Integer diseaseIncidentCount;
    private BigDecimal affectedAreaPercent;
    private String pestDiseaseControlStatus; // "controlled", "ongoing", "severe"
    
    // Financial projection preference
    private Boolean includeFinancialProjection = true;
    
    // Getters and Setters
    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public Long getCropId() {
        return cropId;
    }

    public void setCropId(Long cropId) {
        this.cropId = cropId;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getCropVariety() {
        return cropVariety;
    }

    public void setCropVariety(String cropVariety) {
        this.cropVariety = cropVariety;
    }

    public LocalDate getSowingDate() {
        return sowingDate;
    }

    public void setSowingDate(LocalDate sowingDate) {
        this.sowingDate = sowingDate;
    }

    public BigDecimal getAreaAcres() {
        return areaAcres;
    }

    public void setAreaAcres(BigDecimal areaAcres) {
        this.areaAcres = areaAcres;
    }

    public String getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }

    public Boolean getIncludeHistoricalData() {
        return includeHistoricalData;
    }

    public void setIncludeHistoricalData(Boolean includeHistoricalData) {
        this.includeHistoricalData = includeHistoricalData;
    }

    public String getHistoricalCropName() {
        return historicalCropName;
    }

    public void setHistoricalCropName(String historicalCropName) {
        this.historicalCropName = historicalCropName;
    }

    public BigDecimal getTotalRainfallMm() {
        return totalRainfallMm;
    }

    public void setTotalRainfallMm(BigDecimal totalRainfallMm) {
        this.totalRainfallMm = totalRainfallMm;
    }

    public BigDecimal getAverageTemperatureCelsius() {
        return averageTemperatureCelsius;
    }

    public void setAverageTemperatureCelsius(BigDecimal averageTemperatureCelsius) {
        this.averageTemperatureCelsius = averageTemperatureCelsius;
    }

    public Integer getExtremeWeatherEventsCount() {
        return extremeWeatherEventsCount;
    }

    public void setExtremeWeatherEventsCount(Integer extremeWeatherEventsCount) {
        this.extremeWeatherEventsCount = extremeWeatherEventsCount;
    }

    public String getWeatherTrend() {
        return weatherTrend;
    }

    public void setWeatherTrend(String weatherTrend) {
        this.weatherTrend = weatherTrend;
    }

    public BigDecimal getSoilNitrogenKgHa() {
        return soilNitrogenKgHa;
    }

    public void setSoilNitrogenKgHa(BigDecimal soilNitrogenKgHa) {
        this.soilNitrogenKgHa = soilNitrogenKgHa;
    }

    public BigDecimal getSoilPhosphorusKgHa() {
        return soilPhosphorusKgHa;
    }

    public void setSoilPhosphorusKgHa(BigDecimal soilPhosphorusKgHa) {
        this.soilPhosphorusKgHa = soilPhosphorusKgHa;
    }

    public BigDecimal getSoilPotassiumKgHa() {
        return soilPotassiumKgHa;
    }

    public void setSoilPotassiumKgHa(BigDecimal soilPotassiumKgHa) {
        this.soilPotassiumKgHa = soilPotassiumKgHa;
    }

    public BigDecimal getSoilPh() {
        return soilPh;
    }

    public void setSoilPh(BigDecimal soilPh) {
        this.soilPh = soilPh;
    }

    public String getIrrigationType() {
        return irrigationType;
    }

    public void setIrrigationType(String irrigationType) {
        this.irrigationType = irrigationType;
    }

    public Integer getIrrigationFrequencyPerWeek() {
        return irrigationFrequencyPerWeek;
    }

    public void setIrrigationFrequencyPerWeek(Integer irrigationFrequencyPerWeek) {
        this.irrigationFrequencyPerWeek = irrigationFrequencyPerWeek;
    }

    public BigDecimal getWaterAvailabilityIndex() {
        return waterAvailabilityIndex;
    }

    public void setWaterAvailabilityIndex(BigDecimal waterAvailabilityIndex) {
        this.waterAvailabilityIndex = waterAvailabilityIndex;
    }

    public Integer getPestIncidentCount() {
        return pestIncidentCount;
    }

    public void setPestIncidentCount(Integer pestIncidentCount) {
        this.pestIncidentCount = pestIncidentCount;
    }

    public Integer getDiseaseIncidentCount() {
        return diseaseIncidentCount;
    }

    public void setDiseaseIncidentCount(Integer diseaseIncidentCount) {
        this.diseaseIncidentCount = diseaseIncidentCount;
    }

    public BigDecimal getAffectedAreaPercent() {
        return affectedAreaPercent;
    }

    public void setAffectedAreaPercent(BigDecimal affectedAreaPercent) {
        this.affectedAreaPercent = affectedAreaPercent;
    }

    public String getPestDiseaseControlStatus() {
        return pestDiseaseControlStatus;
    }

    public void setPestDiseaseControlStatus(String pestDiseaseControlStatus) {
        this.pestDiseaseControlStatus = pestDiseaseControlStatus;
    }

    public Boolean getIncludeFinancialProjection() {
        return includeFinancialProjection;
    }

    public void setIncludeFinancialProjection(Boolean includeFinancialProjection) {
        this.includeFinancialProjection = includeFinancialProjection;
    }
}