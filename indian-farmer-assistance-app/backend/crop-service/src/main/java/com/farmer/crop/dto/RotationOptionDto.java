package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private BigDecimal soilHealthBenefit;
    private BigDecimal climateResilience;
    private BigDecimal economicViability;
    
    // Component scores
    private BigDecimal nutrientCyclingScore;
    private BigDecimal pestManagementScore;
    private BigDecimal waterUsageScore;
    
    // Overall benefit score
    private BigDecimal overallBenefitScore;
    
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