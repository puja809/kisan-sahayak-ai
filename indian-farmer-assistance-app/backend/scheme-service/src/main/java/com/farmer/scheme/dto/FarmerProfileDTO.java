package com.farmer.scheme.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO representing farmer profile data for eligibility assessment.
 * Contains all relevant farmer information needed to assess scheme eligibility.
 * 
 * Requirements: 4.4, 11D.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerProfileDTO {
    
    private Long userId;
    private String farmerId;
    private String name;
    private String state;
    private String district;
    private String village;
    
    /**
     * Total landholding in acres.
     * Used for landholding-based eligibility criteria.
     * Requirements: 4.4
     */
    private BigDecimal totalLandholdingAcres;
    
    /**
     * List of crops currently or recently cultivated by the farmer.
     * Used for crop-specific scheme eligibility.
     * Requirements: 4.4, 11D.4
     */
    private List<String> crops;
    
    /**
     * Type of irrigation used on the farm.
     * Used for irrigation-specific scheme eligibility.
     */
    private String irrigationType;
    
    /**
     * Soil type of the farm land.
     * Used for soil-specific scheme eligibility.
     */
    private String soilType;
    
    /**
     * Agro-ecological zone of the farm.
     * Used for zone-specific scheme eligibility.
     */
    private String agroEcologicalZone;
    
    /**
     * Farmer's category (SC, ST, OBC, General, etc.).
     * Used for category-based eligibility criteria.
     */
    private String category;
    
    /**
     * Farmer's gender.
     * Used for gender-specific scheme eligibility.
     */
    private String gender;
    
    /**
     * Age of the farmer in years.
     * Used for age-based eligibility criteria.
     */
    private Integer age;
    
    /**
     * Whether the farmer is a small/marginal farmer (landholding < 2 hectares).
     * Used for small farmer-specific scheme eligibility.
     * Requirements: 11D.5
     */
    private Boolean isSmallMarginalFarmer;
    
    /**
     * Whether the farmer is a tenant/lessee farmer.
     * Used for tenant farmer-specific scheme eligibility.
     */
    private Boolean isTenantFarmer;
    
    /**
     * Annual income of the farmer in INR.
     * Used for income-based eligibility criteria.
     */
    private BigDecimal annualIncome;
    
    /**
     * Whether the farmer has Kisan Credit Card.
     * Used for KCC-related scheme eligibility.
     */
    private Boolean hasKisanCreditCard;
    
    /**
     * Whether the farmer has PM-Kisan registration.
     * Used for PM-Kisan related scheme eligibility.
     */
    private Boolean hasPMKisanRegistration;
    
    /**
     * Whether the farmer has PMFBY insurance.
     * Used for insurance-related scheme eligibility.
     */
    private Boolean hasPMFBYInsurance;
    
    /**
     * Years of farming experience.
     * Used for experience-based eligibility criteria.
     */
    private Integer farmingExperienceYears;
    
    /**
     * Primary occupation (Agriculture, Allied, etc.).
     * Used for occupation-based eligibility criteria.
     */
    private String primaryOccupation;
    
    /**
     * Social category (BPL, APL, etc.).
     * Used for economic status-based eligibility criteria.
     */
    private String socialCategory;
}