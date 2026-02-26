package com.farmer.scheme.dto;

import com.farmer.scheme.entity.Scheme;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO representing the result of eligibility assessment for a farmer and scheme.
 * Contains eligibility status, confidence level, and matching details.
 * 
 * Requirements: 4.4, 4.5, 11D.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityResultDTO {
    
    /**
     * Scheme ID.
     */
    private Long schemeId;
    
    /**
     * Scheme code.
     */
    private String schemeCode;
    
    /**
     * Scheme name.
     */
    private String schemeName;
    
    /**
     * Overall eligibility status.
     */
    private EligibilityStatus eligibilityStatus;
    
    /**
     * Confidence level of the eligibility assessment.
     * HIGH: All criteria clearly met
     * MEDIUM: Most criteria met, some may need verification
     * LOW: Some criteria met, significant verification needed
     * 
     * Requirements: 4.5
     */
    private ConfidenceLevel confidenceLevel;
    
    /**
     * Benefit amount in INR.
     */
    private Double benefitAmount;
    
    /**
     * Maximum benefit amount cap if applicable.
     */
    private Double maxBenefitAmount;
    
    /**
     * Application deadline.
     */
    private LocalDate applicationDeadline;
    
    /**
     * Days remaining until deadline.
     */
    private Long daysUntilDeadline;
    
    /**
     * Number of criteria met.
     */
    private int criteriaMet;
    
    /**
     * Total number of criteria assessed.
     */
    private int totalCriteria;
    
    /**
     * List of criteria that are met.
     */
    private List<String> metCriteria;
    
    /**
     * List of criteria that are not met.
     */
    private List<String> unmetCriteria;
    
    /**
     * List of criteria that need verification.
     */
    private List<String> verificationNeeded;
    
    /**
     * Overall match percentage (0-100).
     */
    private int matchPercentage;
    
    /**
     * Ranking score based on benefit amount and deadline proximity.
     * Requirements: 11D.2
     */
    private Double rankingScore;
    
    /**
     * Rank among all eligible schemes for this farmer.
     */
    private Integer rank;
    
    /**
     * Whether the scheme is highlighted for the farmer.
     */
    private Boolean isHighlighted;
    
    /**
     * Scheme type.
     */
    private Scheme.SchemeType schemeType;
    
    /**
     * State if state-specific scheme.
     */
    private String state;
    
    /**
     * Applicable crops for crop-specific schemes.
     */
    private List<String> applicableCrops;
    
    /**
     * Application URL.
     */
    private String applicationUrl;
    
    /**
     * Additional notes or recommendations.
     */
    private String notes;
    
    /**
     * Eligibility status enum.
     */
    public enum EligibilityStatus {
        ELIGIBLE,       // Farmer meets all criteria
        POTENTIALLY_ELIGIBLE,  // Farmer may meet criteria with verification
        NOT_ELIGIBLE,   // Farmer does not meet criteria
        EXPIRED,        // Application deadline has passed
        NOT_YET_OPEN,   // Application period has not started
        INSUFFICIENT_DATA  // Not enough information to assess
    }
    
    /**
     * Confidence level enum for eligibility assessment.
     * Requirements: 4.5
     */
    public enum ConfidenceLevel {
        HIGH,           // All criteria clearly met with high confidence
        MEDIUM,         // Most criteria met, some assumptions made
        LOW             // Few criteria met, significant verification needed
    }
    
    /**
     * Calculate match percentage based on criteria met.
     */
    public void calculateMatchPercentage() {
        if (totalCriteria > 0) {
            this.matchPercentage = (criteriaMet * 100) / totalCriteria;
        } else {
            this.matchPercentage = 100;
        }
    }
    
    /**
     * Determine confidence level based on match percentage and verification needed.
     */
    public void determineConfidenceLevel() {
        if (eligibilityStatus == EligibilityStatus.NOT_ELIGIBLE || 
            eligibilityStatus == EligibilityStatus.EXPIRED ||
            eligibilityStatus == EligibilityStatus.NOT_YET_OPEN) {
            this.confidenceLevel = null;
        } else if (verificationNeeded.size() > 2) {
            this.confidenceLevel = ConfidenceLevel.LOW;
        } else if (verificationNeeded.size() > 0 || matchPercentage < 90) {
            this.confidenceLevel = ConfidenceLevel.MEDIUM;
        } else {
            this.confidenceLevel = ConfidenceLevel.HIGH;
        }
    }
}