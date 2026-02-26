package com.farmer.yield.dto;


import java.time.LocalDate;

/**
 * Request DTO for recording actual yield after harvest.
 * 
 * Used to:
 * - Record actual harvest data
 * - Calculate variance from predictions
 * - Trigger model improvement feedback loop
 * 
 * Validates: Requirement 11B.9
 */
public class ActualYieldRequestDto {

    private Long cropId;
    private String farmerId;
    private Double actualYieldQuintals;
    private LocalDate harvestDate;
    private String qualityGrade; // "A", "B", "C" or custom grades
    private Double sellingPricePerQuintal;
    private String mandiName;
    private String notes;
    
    // Getters and Setters
    public Long getCropId() {
        return cropId;
    }

    public void setCropId(Long cropId) {
        this.cropId = cropId;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public Double getActualYieldQuintals() {
        return actualYieldQuintals;
    }

    public void setActualYieldQuintals(Double actualYieldQuintals) {
        this.actualYieldQuintals = actualYieldQuintals;
    }

    public LocalDate getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(LocalDate harvestDate) {
        this.harvestDate = harvestDate;
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(String qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public Double getSellingPricePerQuintal() {
        return sellingPricePerQuintal;
    }

    public void setSellingPricePerQuintal(Double sellingPricePerQuintal) {
        this.sellingPricePerQuintal = sellingPricePerQuintal;
    }

    public String getMandiName() {
        return mandiName;
    }

    public void setMandiName(String mandiName) {
        this.mandiName = mandiName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}