package com.farmer.scheme.dto;

import com.farmer.scheme.entity.Scheme;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO representing a personalized scheme recommendation for a farmer.
 * Contains scheme details along with eligibility assessment and ranking information.
 * 
 * Requirements: 4.4, 4.5, 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedSchemeDTO {

    /**
     * Reason for highlighting a scheme recommendation.
     * Requirements: 11D.3, 11D.4, 11D.7, 11D.8
     */
    public enum HighlightReason {
        HIGH_BENEFIT,           // Scheme has high benefit amount
        APPROACHING_DEADLINE,   // Application deadline is approaching (within 30 days)
        CROP_SPECIFIC,          // Scheme is specific to farmer's crops
        SMALL_FARMER_PRIORITY,  // Scheme prioritizes small/marginal farmers
        HIGH_CONFIDENCE,        // High confidence in eligibility
        ALREADY_APPLIED,        // Farmer has already applied
        EXPIRING_SOON           // Scheme is expiring soon
    }
    
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
     * Scheme description.
     */
    private String description;
    
    /**
     * Scheme type (CENTRAL, STATE, CROP_SPECIFIC, INSURANCE, SUBSIDY, WELFARE).
     */
    private Scheme.SchemeType schemeType;
    
    /**
     * State if state-specific scheme.
     */
    private String state;
    
    /**
     * Eligibility status.
     */
    private EligibilityResultDTO.EligibilityStatus eligibilityStatus;
    
    /**
     * Confidence level of eligibility assessment.
     * Requirements: 4.5
     */
    private EligibilityResultDTO.ConfidenceLevel confidenceLevel;
    
    /**
     * Benefit amount in INR.
     */
    private BigDecimal benefitAmount;
    
    /**
     * Maximum benefit amount cap if applicable.
     */
    private BigDecimal maxBenefitAmount;
    
    /**
     * Benefit description.
     */
    private String benefitDescription;
    
    /**
     * Application start date.
     */
    private LocalDate applicationStartDate;
    
    /**
     * Application end date (deadline).
     */
    private LocalDate applicationEndDate;
    
    /**
     * Days remaining until deadline.
     */
    private Long daysUntilDeadline;
    
    /**
     * Whether deadline is approaching (within 7 days).
     * Requirements: 4.8
     */
    private Boolean isDeadlineApproaching;
    
    /**
     * Application URL.
     */
    private String applicationUrl;
    
    /**
     * Applicable crops for crop-specific schemes.
     * Requirements: 11D.4
     */
    private List<String> applicableCrops;
    
    /**
     * Matching crops from farmer's profile.
     */
    private List<String> matchingCrops;
    
    /**
     * Ranking score based on benefit amount and deadline proximity.
     * Requirements: 11D.2
     */
    private BigDecimal rankingScore;
    
    /**
     * Rank among all recommended schemes.
     * Requirements: 11D.3
     */
    private Integer rank;
    
    /**
     * Whether the scheme is highlighted for the farmer.
     * Requirements: 4.5
     */
    private Boolean isHighlighted;
    
    /**
     * Priority for display (higher = more important).
     */
    private Integer priority;
    
    /**
     * Scheme-specific eligibility notes.
     */
    private String eligibilityNotes;
    
    /**
     * Contact information (JSON).
     */
    private String contactInfo;
    
    /**
     * Whether the farmer has already applied for this scheme.
     */
    private Boolean hasApplied;
    
    /**
     * Application status if already applied.
     */
    private SchemeApplicationDTO applicationStatus;
    
    /**
     * Summary of why this scheme is recommended.
     */
    private String recommendationReason;
    
    /**
     * Reason for highlighting this scheme.
     * Requirements: 11D.3, 11D.4, 11D.7, 11D.8
     */
    private HighlightReason highlightReason;
    
    /**
     * List of all highlight reasons for this scheme.
     */
    private List<HighlightReason> highlightReasons;
    
    /**
     * Create a PersonalizedSchemeDTO from an EligibilityResultDTO.
     */
    public static PersonalizedSchemeDTO fromEligibilityResult(
            EligibilityResultDTO eligibilityResult,
            Scheme scheme,
            List<String> matchingCrops,
            Boolean hasApplied,
            SchemeApplicationDTO applicationStatus) {
        
        LocalDate today = LocalDate.now();
        Long daysUntilDeadline = null;
        Boolean isDeadlineApproaching = false;
        
        if (scheme.getApplicationEndDate() != null) {
            daysUntilDeadline = java.time.temporal.ChronoUnit.DAYS.between(today, scheme.getApplicationEndDate());
            isDeadlineApproaching = daysUntilDeadline != null && daysUntilDeadline > 0 && daysUntilDeadline <= 30;
        }
        
        List<HighlightReason> reasons = new java.util.ArrayList<>();
        if (eligibilityResult.getRankingScore() != null && eligibilityResult.getRankingScore().compareTo(new BigDecimal("50")) > 0) {
            reasons.add(HighlightReason.HIGH_BENEFIT);
        }
        if (isDeadlineApproaching) {
            reasons.add(HighlightReason.APPROACHING_DEADLINE);
        }
        if (matchingCrops != null && !matchingCrops.isEmpty()) {
            reasons.add(HighlightReason.CROP_SPECIFIC);
        }
        if (eligibilityResult.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.HIGH) {
            reasons.add(HighlightReason.HIGH_CONFIDENCE);
        }
        if (Boolean.TRUE.equals(hasApplied)) {
            reasons.add(HighlightReason.ALREADY_APPLIED);
        }
        
        return PersonalizedSchemeDTO.builder()
                .schemeId(eligibilityResult.getSchemeId())
                .schemeCode(eligibilityResult.getSchemeCode())
                .schemeName(eligibilityResult.getSchemeName())
                .description(scheme.getDescription())
                .schemeType(scheme.getSchemeType())
                .state(scheme.getState())
                .eligibilityStatus(eligibilityResult.getEligibilityStatus())
                .confidenceLevel(eligibilityResult.getConfidenceLevel())
                .benefitAmount(eligibilityResult.getBenefitAmount())
                .maxBenefitAmount(eligibilityResult.getMaxBenefitAmount())
                .benefitDescription(scheme.getBenefitDescription())
                .applicationStartDate(scheme.getApplicationStartDate())
                .applicationEndDate(scheme.getApplicationEndDate())
                .daysUntilDeadline(daysUntilDeadline)
                .isDeadlineApproaching(isDeadlineApproaching)
                .applicationUrl(scheme.getApplicationUrl())
                .applicableCrops(scheme.getApplicableCrops() != null ? 
                        List.of(scheme.getApplicableCrops().split(",")) : List.of())
                .matchingCrops(matchingCrops)
                .rankingScore(eligibilityResult.getRankingScore())
                .rank(eligibilityResult.getRank())
                .isHighlighted(eligibilityResult.getIsHighlighted() || !reasons.isEmpty())
                .priority(scheme.getEligibilityCriteria() != null ? 
                        extractPriorityWeight(scheme.getEligibilityCriteria()) : 0)
                .eligibilityNotes(buildEligibilityNotes(eligibilityResult))
                .contactInfo(scheme.getContactInfo())
                .hasApplied(hasApplied)
                .applicationStatus(applicationStatus)
                .recommendationReason(buildRecommendationReason(eligibilityResult, matchingCrops, isDeadlineApproaching))
                .highlightReason(!reasons.isEmpty() ? reasons.get(0) : null)
                .highlightReasons(reasons)
                .build();
    }
    
    private static Integer extractPriorityWeight(String eligibilityCriteria) {
        // Parse priority weight from eligibility criteria JSON if present
        if (eligibilityCriteria != null && eligibilityCriteria.contains("\"priorityWeight\"")) {
            try {
                int start = eligibilityCriteria.indexOf("\"priorityWeight\"") + 17;
                int end = eligibilityCriteria.indexOf(",", start);
                if (end == -1) end = eligibilityCriteria.indexOf("}", start);
                return Integer.parseInt(eligibilityCriteria.substring(start, end).trim());
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
    
    private static String buildEligibilityNotes(EligibilityResultDTO result) {
        StringBuilder notes = new StringBuilder();
        
        if (result.getUnmetCriteria() != null && !result.getUnmetCriteria().isEmpty()) {
            notes.append("Criteria not met: ").append(String.join(", ", result.getUnmetCriteria()));
        }
        
        if (result.getVerificationNeeded() != null && !result.getVerificationNeeded().isEmpty()) {
            if (notes.length() > 0) notes.append(". ");
            notes.append("Verification needed: ").append(String.join(", ", result.getVerificationNeeded()));
        }
        
        return notes.toString();
    }
    
    private static String buildRecommendationReason(
            EligibilityResultDTO result,
            List<String> matchingCrops,
            Boolean isDeadlineApproaching) {
        
        StringBuilder reason = new StringBuilder();
        
        if (result.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.HIGH) {
            reason.append("You meet all eligibility criteria for this scheme.");
        } else if (result.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.MEDIUM) {
            reason.append("You may be eligible for this scheme. Some verification may be needed.");
        } else {
            reason.append("This scheme might be relevant based on your profile.");
        }
        
        if (matchingCrops != null && !matchingCrops.isEmpty()) {
            reason.append(" Applicable to your crops: ").append(String.join(", ", matchingCrops));
        }
        
        if (Boolean.TRUE.equals(isDeadlineApproaching) && result.getDaysUntilDeadline() != null && 
            result.getDaysUntilDeadline() > 0) {
            reason.append(" Deadline approaching in ").append(result.getDaysUntilDeadline()).append(" days!");
        }
        
        return reason.toString();
    }
}