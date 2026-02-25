package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for scheme recommendations.
 * 
 * Represents a government scheme recommendation with benefit and eligibility information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemeRecommendationDto {

    private Long id;
    private String schemeCode;
    private String schemeName;
    
    // Scheme type
    public enum SchemeType {
        CENTRAL, STATE, CROP_SPECIFIC, INSURANCE, SUBSIDY, WELFARE
    }
    
    private SchemeType schemeType;
    private String state;
    
    // Benefit information
    private BigDecimal benefitAmount;
    private String benefitDescription;
    
    // Eligibility
    private BigDecimal eligibilityScore;
    private String eligibilityConfidence; // HIGH, MEDIUM, LOW
    private List<String> eligibilityCriteria;
    
    // Deadline
    private LocalDate applicationDeadline;
    private BigDecimal deadlineProximityScore; // Higher = closer deadline
    
    // Application information
    private String applicationUrl;
    private String applicationStatus;
    
    // Contact
    private String contactInfo;
    private String supportPhone;
    
    // Additional details
    private String description;
    private List<String> documentsRequired;
    private String schemeCategory;
    
    // Ranking scores
    private BigDecimal overallScore;
    private Integer rank;
}