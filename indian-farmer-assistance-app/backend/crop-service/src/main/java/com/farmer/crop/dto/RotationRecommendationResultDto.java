package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result DTO for rotation recommendation generation.
 * Contains multiple rotation options with analysis and warnings.
 * 
 * Requirements: 3.3, 3.4, 3.5, 3.6, 3.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotationRecommendationResultDto {

    // List of rotation options ranked by overall benefit
    private List<RotationOptionDto> options;
    
    // Warnings about potential issues (pest risks, nutrient depletion, etc.)
    private List<String> warnings;
    
    // General recommendations based on analysis
    private List<String> recommendations;
    
    // Analysis flags
    private boolean hasRiceBasedSystem;
    private boolean hasNutrientDepletionRisk;
    private boolean hasPestDiseaseRisk;
    private String pestRiskLevel; // LOW, MEDIUM, HIGH
    
    // Season information
    private String targetSeason;
    
    // Number of seasons analyzed
    private Integer seasonsAnalyzed;
    
    // Default rotation patterns for farmers with no history
    private List<RotationOptionDto> defaultPatterns;
}