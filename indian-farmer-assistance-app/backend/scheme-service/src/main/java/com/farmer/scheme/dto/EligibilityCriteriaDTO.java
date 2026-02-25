package com.farmer.scheme.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO representing eligibility criteria for a government scheme.
 * Parsed from the eligibility_criteria JSON field in the Scheme entity.
 * 
 * Requirements: 4.4, 4.5, 11D.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityCriteriaDTO {
    
    /**
     * Minimum landholding required in acres.
     * null means no minimum requirement.
     */
    private BigDecimal minLandholdingAcres;
    
    /**
     * Maximum landholding allowed in acres.
     * null means no maximum requirement.
     */
    private BigDecimal maxLandholdingAcres;
    
    /**
     * Whether small/marginal farmers are specifically targeted.
     */
    private Boolean smallMarginalFarmersOnly;
    
    /**
     * List of required crops for crop-specific schemes.
     * null or empty means not crop-specific.
     */
    private List<String> requiredCrops;
    
    /**
     * List of excluded crops.
     */
    private List<String> excludedCrops;
    
    /**
     * List of required states for state-specific schemes.
     * null or empty means applicable to all states.
     */
    private List<String> requiredStates;
    
    /**
     * List of excluded states.
     */
    private List<String> excludedStates;
    
    /**
     * Required social categories (SC, ST, OBC, General).
     * null or empty means all categories eligible.
     */
    private List<String> requiredCategories;
    
    /**
     * Required gender (Male, Female, Other).
     * null or empty means all genders eligible.
     */
    private List<String> requiredGenders;
    
    /**
     * Minimum age required.
     * null means no minimum age.
     */
    private Integer minAge;
    
    /**
     * Maximum age allowed.
     * null means no maximum age.
     */
    private Integer maxAge;
    
    /**
     * Maximum annual income allowed in INR.
     * null means no income limit.
     */
    private BigDecimal maxAnnualIncome;
    
    /**
     * Required irrigation types.
     * null or empty means all irrigation types eligible.
     */
    private List<String> requiredIrrigationTypes;
    
    /**
     * Required soil types.
     * null or empty means all soil types eligible.
     */
    private List<String> requiredSoilTypes;
    
    /**
     * Required agro-ecological zones.
     * null or empty means all zones eligible.
     */
    private List<String> requiredAgroEcologicalZones;
    
    /**
     * Whether tenant farmers are eligible.
     * null means not specified.
     */
    private Boolean tenantFarmersEligible;
    
    /**
     * Minimum farming experience required in years.
     * null means no experience requirement.
     */
    private Integer minFarmingExperience;
    
    /**
     * Required primary occupations.
     * null or empty means all occupations eligible.
     */
    private List<String> requiredOccupations;
    
    /**
     * Required social/economic categories (BPL, APL).
     * null or empty means all categories eligible.
     */
    private List<String> requiredSocialCategories;
    
    /**
     * Whether Kisan Credit Card is required.
     * null means not specified.
     */
    private Boolean kccRequired;
    
    /**
     * Whether PM-Kisan registration is required.
     * null means not specified.
     */
    private Boolean pmKisanRequired;
    
    /**
     * Whether PMFBY insurance is required.
     * null means not specified.
     */
    private Boolean pmfbyRequired;
    
    /**
     * Scheme-specific custom eligibility rules (JSON string).
     */
    private String customRules;
    
    /**
     * Priority weight for ranking schemes.
     * Higher weight means higher priority.
     */
    private Integer priorityWeight;
    
    /**
     * Additional notes about eligibility criteria.
     */
    private String notes;
}