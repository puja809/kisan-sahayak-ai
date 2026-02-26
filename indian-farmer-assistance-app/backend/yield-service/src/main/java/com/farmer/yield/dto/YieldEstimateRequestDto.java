package com.farmer.yield.dto;

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
    private Double areaAcres;
    private String growthStage;
    
    // Historical data reference
    private Boolean includeHistoricalData = true;
    private String historicalCropName; // For looking up past yields
    
    // Weather data (optional - will be fetched from weather service if not provided)
    private Double totalRainfallMm;
    private Double averageTemperatureCelsius;
    private Integer extremeWeatherEventsCount;
    private String weatherTrend; // "improving", "stable", "declining"
    
    // Soil health data (optional - will use defaults if not provided)
    private Double soilNitrogenKgHa;
    private Double soilPhosphorusKgHa;
    private Double soilPotassiumKgHa;
    private Double soilPh;
    
    // Irrigation information
    private String irrigationType; // "RAINFED", "DRIP", "SPRINKLER", "CANAL", "BOREWELL"
    private Integer irrigationFrequencyPerWeek;
    private Double waterAvailabilityIndex; // 0-1 scale
    
    // Pest/disease incidents
    private Integer pestIncidentCount;
    private Integer diseaseIncidentCount;
    private Double affectedAreaPercent;
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

    public Double getAreaAcres() {
        return areaAcres;
    }

    public void setAreaAcres(Double areaAcres) {
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

    public Double getTotalRainfallMm() {
        return totalRainfallMm;
    }

    public void setTotalRainfallMm(Double totalRainfallMm) {
        this.totalRainfallMm = totalRainfallMm;
    }

    public Double getAverageTemperatureCelsius() {
        return averageTemperatureCelsius;
    }

    public void setAverageTemperatureCelsius(Double averageTemperatureCelsius) {
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

    public Double getSoilNitrogenKgHa() {
        return soilNitrogenKgHa;
    }

    public void setSoilNitrogenKgHa(Double soilNitrogenKgHa) {
        this.soilNitrogenKgHa = soilNitrogenKgHa;
    }

    public Double getSoilPhosphorusKgHa() {
        return soilPhosphorusKgHa;
    }

    public void setSoilPhosphorusKgHa(Double soilPhosphorusKgHa) {
        this.soilPhosphorusKgHa = soilPhosphorusKgHa;
    }

    public Double getSoilPotassiumKgHa() {
        return soilPotassiumKgHa;
    }

    public void setSoilPotassiumKgHa(Double soilPotassiumKgHa) {
        this.soilPotassiumKgHa = soilPotassiumKgHa;
    }

    public Double getSoilPh() {
        return soilPh;
    }

    public void setSoilPh(Double soilPh) {
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

    public Double getWaterAvailabilityIndex() {
        return waterAvailabilityIndex;
    }

    public void setWaterAvailabilityIndex(Double waterAvailabilityIndex) {
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

    public Double getAffectedAreaPercent() {
        return affectedAreaPercent;
    }

    public void setAffectedAreaPercent(Double affectedAreaPercent) {
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