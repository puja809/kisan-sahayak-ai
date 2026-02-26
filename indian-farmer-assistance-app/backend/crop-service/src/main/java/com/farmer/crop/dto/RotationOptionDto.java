package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for crop rotation options.
 * 
 * Represents a rotation option with various benefit scores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotationOptionDto {

    private Long id;
    private String cropSequence;
    private String description;
    
    // Benefit scores (0-100)
    private Double soilHealthBenefit;
    private Double climateResilience;
    private Double economicViability;
    
    // Component scores
    private Double nutrientCyclingScore;
    private Double pestManagementScore;
    private Double waterUsageScore;
    
    // Overall benefit score
    private Double overallBenefitScore;
    
    // Season information
    private String kharifCrops;
    private String rabiCrops;
    private String zaidCrops;
    
    // Additional details
    private List<String> benefits;
    private List<String> considerations;
    private String residueManagementRecommendation;
    private String organicMatterImpact;
}