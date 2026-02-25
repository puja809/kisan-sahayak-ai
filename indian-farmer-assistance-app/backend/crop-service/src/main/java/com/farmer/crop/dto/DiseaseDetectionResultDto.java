package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for disease detection results.
 * 
 * Represents a detected disease with confidence score and severity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseDetectionResultDto {

    private Long id;
    private String diseaseName;
    private String diseaseNameLocal;
    
    // Confidence score (0-100)
    private BigDecimal confidenceScore;
    
    // Severity levels
    public enum SeverityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    private SeverityLevel severityLevel;
    
    // Affected area
    private BigDecimal affectedAreaPercent;
    
    // Treatment recommendations
    private String treatmentRecommendations;
    private List<String> organicTreatments;
    private List<String> chemicalTreatments;
    private List<String> preventiveMeasures;
    
    // Cost information
    private BigDecimal estimatedTreatmentCost;
    
    // Application details
    private String applicationTiming;
    private String dosageInformation;
    private String safetyPrecautions;
    
    // Expert contact
    private String kvkExpertContact;
    private String extensionServiceContact;
    
    // Metadata
    private String modelVersion;
    private String imagePath;
}