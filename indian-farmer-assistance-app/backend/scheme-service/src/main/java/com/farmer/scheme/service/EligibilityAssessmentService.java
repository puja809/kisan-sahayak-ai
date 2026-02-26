package com.farmer.scheme.service;

import com.farmer.scheme.dto.*;
import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.repository.SchemeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for assessing farmer eligibility for government schemes.
 * Pre-assesses eligibility using farmer profile data before displaying schemes.
 * Generates confidence indicators (high, medium, low) for eligibility matches.
 * 
 * Requirements: 4.4, 4.5, 11D.1, 11D.2
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EligibilityAssessmentService {

    private final SchemeRepository schemeRepository;
    private final ObjectMapper objectMapper;

    /**
     * Assess eligibility for a specific scheme.
     * Requirements: 4.4
     */
    public EligibilityResultDTO assessEligibility(FarmerProfileDTO farmer, Scheme scheme) {
        log.debug("Assessing eligibility for farmer {} and scheme {}", 
                farmer.getUserId(), scheme.getId());

        EligibilityCriteriaDTO criteria = parseEligibilityCriteria(scheme.getEligibilityCriteria());
        
        List<String> metCriteria = new ArrayList<>();
        List<String> unmetCriteria = new ArrayList<>();
        List<String> verificationNeeded = new ArrayList<>();
        
        // Check eligibility criteria
        checkLandholdingCriteria(farmer, criteria, metCriteria, unmetCriteria, verificationNeeded);
        checkStateCriteria(farmer, criteria, metCriteria, unmetCriteria);
        checkCropCriteria(farmer, scheme, criteria, metCriteria, unmetCriteria, verificationNeeded);
        checkCategoryCriteria(farmer, criteria, metCriteria, unmetCriteria);
        checkGenderCriteria(farmer, criteria, metCriteria, unmetCriteria);
        checkAgeCriteria(farmer, criteria, metCriteria, unmetCriteria);
        checkIncomeCriteria(farmer, criteria, metCriteria, unmetCriteria);
        checkIrrigationCriteria(farmer, criteria, metCriteria, unmetCriteria);
        checkTenantFarmerCriteria(farmer, criteria, metCriteria, unmetCriteria);
        checkExperienceCriteria(farmer, criteria, metCriteria, unmetCriteria);
        checkKCCCriteria(farmer, criteria, metCriteria, unmetCriteria);
        checkPMKisanCriteria(farmer, criteria, metCriteria, unmetCriteria);
        checkPMFBYCriteria(farmer, criteria, metCriteria, unmetCriteria);
        checkApplicationDeadline(scheme, unmetCriteria);
        
        // Calculate overall eligibility status
        EligibilityResultDTO.EligibilityStatus status = determineEligibilityStatus(
                unmetCriteria, verificationNeeded, scheme);
        
        // Create result
        EligibilityResultDTO result = EligibilityResultDTO.builder()
                .schemeId(scheme.getId())
                .schemeCode(scheme.getSchemeCode())
                .schemeName(scheme.getSchemeName())
                .eligibilityStatus(status)
                .benefitAmount(scheme.getBenefitAmount())
                .maxBenefitAmount(scheme.getMaxBenefitAmount())
                .applicationDeadline(scheme.getApplicationEndDate())
                .daysUntilDeadline(calculateDaysUntilDeadline(scheme))
                .criteriaMet(metCriteria.size())
                .totalCriteria(metCriteria.size() + unmetCriteria.size() + verificationNeeded.size())
                .metCriteria(metCriteria)
                .unmetCriteria(unmetCriteria)
                .verificationNeeded(verificationNeeded)
                .schemeType(scheme.getSchemeType())
                .state(scheme.getState())
                .applicableCrops(scheme.getApplicableCrops() != null ? 
                        Arrays.asList(scheme.getApplicableCrops().split(",")) : List.of())
                .applicationUrl(scheme.getApplicationUrl())
                .build();
        
        // Calculate match percentage and confidence level
        result.calculateMatchPercentage();
        result.determineConfidenceLevel();
        
        // Calculate ranking score
        result.setRankingScore(calculateRankingScore(result, scheme));
        
        // Set highlighted flag
        result.setIsHighlighted(result.getConfidenceLevel() != null && 
                (result.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.HIGH ||
                 result.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.MEDIUM));
        
        return result;
    }

    /**
     * Get personalized scheme recommendations for a farmer.
     * Analyzes farmer's profile for all scheme eligibility and ranks by benefit and deadline.
     * Returns PersonalizedSchemeDTO with enhanced features including highlighting and ranking.
     * Requirements: 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8
     * 
     * @param farmer The farmer's profile
     * @return List of personalized scheme recommendations with ranking and highlighting
     */
    public List<PersonalizedSchemeDTO> getPersonalizedRecommendations(FarmerProfileDTO farmer) {
        log.debug("Getting personalized recommendations for farmer {}", farmer.getUserId());
        
        // Get all active schemes for the farmer's state (create mutable copy)
        List<Scheme> schemes = new ArrayList<>(schemeRepository.findActiveSchemesForState(farmer.getState()));
        
        // Also include central schemes
        List<Scheme> centralSchemes = schemeRepository.findAllCentralSchemes();
        Set<Long> existingIds = schemes.stream().map(Scheme::getId).collect(Collectors.toSet());
        for (Scheme scheme : centralSchemes) {
            if (!existingIds.contains(scheme.getId())) {
                schemes.add(scheme);
            }
        }
        
        // Assess eligibility for each scheme and create personalized recommendations
        List<PersonalizedSchemeDTO> recommendations = schemes.stream()
                .map(scheme -> createPersonalizedRecommendation(farmer, scheme))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(
                        (PersonalizedSchemeDTO r) -> r.getRankingScore() != null ? r.getRankingScore() : 0.0)
                        .reversed()
                        .thenComparing(r -> r.getDaysUntilDeadline() != null ? r.getDaysUntilDeadline() : Long.MAX_VALUE))
                .collect(Collectors.toList());
        
        // Set ranks
        for (int i = 0; i < recommendations.size(); i++) {
            recommendations.get(i).setRank(i + 1);
        }
        
        log.debug("Found {} personalized recommendations for farmer {}", recommendations.size(), farmer.getUserId());
        return recommendations;
    }

    /**
     * Create a personalized scheme recommendation for a farmer and scheme.
     * Requirements: 11D.1, 11D.3, 11D.4, 11D.6, 11D.7
     */
    private PersonalizedSchemeDTO createPersonalizedRecommendation(FarmerProfileDTO farmer, Scheme scheme) {
        // Assess eligibility
        EligibilityResultDTO eligibilityResult = assessEligibility(farmer, scheme);
        
        // Skip expired or not-yet-open schemes
        if (eligibilityResult.getEligibilityStatus() == EligibilityResultDTO.EligibilityStatus.EXPIRED ||
            eligibilityResult.getEligibilityStatus() == EligibilityResultDTO.EligibilityStatus.NOT_YET_OPEN) {
            return null;
        }
        
        // Find matching crops
        List<String> matchingCrops = findMatchingCrops(farmer, scheme);
        
        // Determine small/marginal farmer priority
        boolean isSmallMarginalFarmer = Boolean.TRUE.equals(farmer.getIsSmallMarginalFarmer()) ||
                (farmer.getTotalLandholdingAcres() != null && 
                 farmer.getTotalLandholdingAcres().compareTo(new Double("4.94")) < 0); // 2 hectares ≈ 4.94 acres
        
        // Calculate enhanced ranking score with small farmer priority
        Double rankingScore = calculateEnhancedRankingScore(eligibilityResult, scheme, isSmallMarginalFarmer, matchingCrops);
        eligibilityResult.setRankingScore(rankingScore);
        
        // Determine highlight reasons
        List<PersonalizedSchemeDTO.HighlightReason> highlightReasons = determineHighlightReasons(
                eligibilityResult, scheme, matchingCrops, isSmallMarginalFarmer);
        
        // Set highlighted flag
        boolean isHighlighted = eligibilityResult.getIsHighlighted() || !highlightReasons.isEmpty();
        eligibilityResult.setIsHighlighted(isHighlighted);
        
        // Create personalized DTO
        PersonalizedSchemeDTO recommendation = PersonalizedSchemeDTO.fromEligibilityResult(
                eligibilityResult, scheme, matchingCrops, false, null);
        
        // Add small farmer priority highlight
        if (isSmallMarginalFarmer && isSchemeForSmallFarmers(scheme)) {
            if (recommendation.getHighlightReasons() == null) {
                recommendation.setHighlightReasons(new java.util.ArrayList<>());
            }
            recommendation.getHighlightReasons().add(PersonalizedSchemeDTO.HighlightReason.SMALL_FARMER_PRIORITY);
            recommendation.setHighlightReason(PersonalizedSchemeDTO.HighlightReason.SMALL_FARMER_PRIORITY);
        }
        
        return recommendation;
    }

    /**
     * Find crops that match between farmer's profile and scheme's applicable crops.
     * Requirements: 11D.3
     */
    private List<String> findMatchingCrops(FarmerProfileDTO farmer, Scheme scheme) {
        if (farmer.getCrops() == null || farmer.getCrops().isEmpty()) {
            return List.of();
        }
        
        if (scheme.getApplicableCrops() == null || scheme.getApplicableCrops().isEmpty()) {
            return List.of();
        }
        
        List<String> schemeCrops = Arrays.asList(scheme.getApplicableCrops().split(","));
        List<String> matchingCrops = new ArrayList<>();
        
        for (String farmerCrop : farmer.getCrops()) {
            for (String schemeCrop : schemeCrops) {
                if (farmerCrop.equalsIgnoreCase(schemeCrop.trim())) {
                    matchingCrops.add(farmerCrop);
                    break;
                }
            }
        }
        
        return matchingCrops;
    }

    /**
     * Check if a scheme is specifically for small/marginal farmers.
     * Requirements: 11D.4
     */
    private boolean isSchemeForSmallFarmers(Scheme scheme) {
        if (scheme.getEligibilityCriteria() == null) {
            return false;
        }
        
        try {
            EligibilityCriteriaDTO criteria = parseEligibilityCriteria(scheme.getEligibilityCriteria());
            return Boolean.TRUE.equals(criteria.getSmallMarginalFarmersOnly());
        } catch (Exception e) {
            log.warn("Error parsing eligibility criteria for small farmer check", e);
            return false;
        }
    }

    /**
     * Calculate enhanced ranking score with small farmer priority and crop-specific weighting.
     * Requirements: 11D.2, 11D.4, 11D.8
     */
    private Double calculateEnhancedRankingScore(
            EligibilityResultDTO result, 
            Scheme scheme, 
            boolean isSmallMarginalFarmer,
            List<String> matchingCrops) {
        
        // Start with base ranking score
        Double baseScore = calculateRankingScore(result, scheme);
        
        // Add small/marginal farmer priority boost
        Double smallFarmerBoost = 0.0;
        if (isSmallMarginalFarmer && isSchemeForSmallFarmers(scheme)) {
            smallFarmerBoost = 15.0; // Significant boost for small farmer schemes
        }
        
        // Add crop-specific boost
        Double cropBoost = 0.0;
        if (matchingCrops != null && !matchingCrops.isEmpty()) {
            cropBoost = 10.0 * matchingCrops.size();
            cropBoost = Math.min(cropBoost, 20.0); // Cap at 20 points
        }
        
        // Add high confidence boost
        Double confidenceBoost = 0.0;
        if (result.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.HIGH) {
            confidenceBoost = 10.0;
        } else if (result.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.MEDIUM) {
            confidenceBoost = 5.0;
        }
        
        // Add approaching deadline urgency boost
        Double deadlineBoost = 0.0;
        if (result.getDaysUntilDeadline() != null && result.getDaysUntilDeadline() > 0) {
            if (result.getDaysUntilDeadline() <= 7) {
                deadlineBoost = 25.0; // Urgent
            } else if (result.getDaysUntilDeadline() <= 15) {
                deadlineBoost = 15.0;
            } else if (result.getDaysUntilDeadline() <= 30) {
                deadlineBoost = 10.0;
            }
        }
        
        // Calculate final score
        Double totalScore = baseScore + smallFarmerBoost + cropBoost + confidenceBoost + deadlineBoost;
        
        return totalScore;
    }

    /**
     * Determine highlight reasons for a scheme recommendation.
     * Requirements: 11D.3, 11D.4, 11D.7, 11D.8
     */
    private List<PersonalizedSchemeDTO.HighlightReason> determineHighlightReasons(
            EligibilityResultDTO result,
            Scheme scheme,
            List<String> matchingCrops,
            boolean isSmallMarginalFarmer) {
        
        List<PersonalizedSchemeDTO.HighlightReason> reasons = new ArrayList<>();
        
        // High benefit amount
        if (result.getRankingScore() != null && result.getRankingScore().compareTo(new Double("50")) > 0) {
            reasons.add(PersonalizedSchemeDTO.HighlightReason.HIGH_BENEFIT);
        }
        
        // Approaching deadline
        if (result.getDaysUntilDeadline() != null && result.getDaysUntilDeadline() > 0 && 
            result.getDaysUntilDeadline() <= 30) {
            reasons.add(PersonalizedSchemeDTO.HighlightReason.APPROACHING_DEADLINE);
        }
        
        // Crop-specific scheme
        if (matchingCrops != null && !matchingCrops.isEmpty()) {
            reasons.add(PersonalizedSchemeDTO.HighlightReason.CROP_SPECIFIC);
        }
        
        // Small/marginal farmer priority
        if (isSmallMarginalFarmer && isSchemeForSmallFarmers(scheme)) {
            reasons.add(PersonalizedSchemeDTO.HighlightReason.SMALL_FARMER_PRIORITY);
        }
        
        // High confidence
        if (result.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.HIGH) {
            reasons.add(PersonalizedSchemeDTO.HighlightReason.HIGH_CONFIDENCE);
        }
        
        return reasons;
    }

    /**
     * Get personalized recommendations returning EligibilityResultDTO (legacy method).
     * Kept for backward compatibility.
     * Requirements: 11D.1, 11D.2
     */
    public List<EligibilityResultDTO> getPersonalizedRecommendationsLegacy(FarmerProfileDTO farmer) {
        log.debug("Getting personalized recommendations (old format) for farmer {}", farmer.getUserId());
        
        // Get all active schemes for the farmer's state
        List<Scheme> schemes = schemeRepository.findActiveSchemesForState(farmer.getState());
        
        // Also include central schemes
        List<Scheme> centralSchemes = schemeRepository.findAllCentralSchemes();
        Set<Long> existingIds = schemes.stream().map(Scheme::getId).collect(Collectors.toSet());
        for (Scheme scheme : centralSchemes) {
            if (!existingIds.contains(scheme.getId())) {
                schemes.add(scheme);
            }
        }
        
        // Assess eligibility for each scheme
        List<EligibilityResultDTO> results = schemes.stream()
                .map(scheme -> assessEligibility(farmer, scheme))
                .filter(result -> result.getEligibilityStatus() == EligibilityResultDTO.EligibilityStatus.ELIGIBLE ||
                                  result.getEligibilityStatus() == EligibilityResultDTO.EligibilityStatus.POTENTIALLY_ELIGIBLE)
                .sorted(Comparator.comparing(
                        (EligibilityResultDTO r) -> r.getRankingScore() != null ? r.getRankingScore() : 0.0)
                        .reversed()
                        .thenComparing(r -> r.getDaysUntilDeadline() != null ? r.getDaysUntilDeadline() : Long.MAX_VALUE))
                .collect(Collectors.toList());
        
        // Set ranks
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }
        
        log.debug("Found {} personalized recommendations for farmer {}", results.size(), farmer.getUserId());
        return results;
    }

    /**
     * Get all schemes with eligibility assessment for a farmer.
     * Requirements: 11D.1
     */
    public List<EligibilityResultDTO> getAllSchemesWithEligibility(FarmerProfileDTO farmer) {
        log.debug("Getting all schemes with eligibility for farmer {}", farmer.getUserId());
        
        List<Scheme> schemes = schemeRepository.findActiveSchemesForState(farmer.getState());
        
        // Also include central schemes
        List<Scheme> centralSchemes = schemeRepository.findAllCentralSchemes();
        Set<Long> existingIds = schemes.stream().map(Scheme::getId).collect(Collectors.toSet());
        for (Scheme scheme : centralSchemes) {
            if (!existingIds.contains(scheme.getId())) {
                schemes.add(scheme);
            }
        }
        
        return schemes.stream()
                .map(scheme -> assessEligibility(farmer, scheme))
                .sorted(Comparator.comparing(
                        (EligibilityResultDTO r) -> r.getRankingScore() != null ? r.getRankingScore() : 0.0)
                        .reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get schemes with high eligibility confidence for a farmer.
     * Requirements: 4.5, 11D.6
     */
    public List<PersonalizedSchemeDTO> getHighConfidenceSchemes(FarmerProfileDTO farmer) {
        return getPersonalizedRecommendations(farmer).stream()
                .filter(r -> r.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.HIGH)
                .collect(Collectors.toList());
    }

    /**
     * Get schemes with approaching deadlines for a farmer.
     * Requirements: 4.8, 11D.7, 11D.9
     */
    public List<PersonalizedSchemeDTO> getSchemesWithApproachingDeadlines(FarmerProfileDTO farmer, int daysAhead) {
        return getPersonalizedRecommendations(farmer).stream()
                .filter(r -> r.getDaysUntilDeadline() != null && 
                            r.getDaysUntilDeadline() > 0 && 
                            r.getDaysUntilDeadline() <= daysAhead)
                .collect(Collectors.toList());
    }

    // Private helper methods for checking eligibility criteria

    private void checkLandholdingCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet, List<String> verification) {
        if (criteria.getMinLandholdingAcres() != null || criteria.getMaxLandholdingAcres() != null) {
            Double landholding = farmer.getTotalLandholdingAcres();
            if (landholding == null) {
                verification.add("Landholding size needs verification");
                return;
            }
            
            boolean metMin = criteria.getMinLandholdingAcres() == null || 
                            landholding.compareTo(criteria.getMinLandholdingAcres()) >= 0;
            boolean metMax = criteria.getMaxLandholdingAcres() == null || 
                            landholding.compareTo(criteria.getMaxLandholdingAcres()) <= 0;
            
            if (metMin && metMax) {
                met.add("Landholding (" + landholding + " acres) within required range");
            } else {
                unmet.add("Landholding requirement not met");
            }
        }
        
        if (Boolean.TRUE.equals(criteria.getSmallMarginalFarmersOnly())) {
            if (Boolean.TRUE.equals(farmer.getIsSmallMarginalFarmer())) {
                met.add("Small/marginal farmer");
            } else {
                unmet.add("Scheme only for small/marginal farmers");
            }
        }
    }

    private void checkStateCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet) {
        if (criteria.getRequiredStates() != null && !criteria.getRequiredStates().isEmpty()) {
            if (criteria.getRequiredStates().contains(farmer.getState())) {
                met.add("State eligibility verified");
            } else {
                unmet.add("Not available in " + farmer.getState());
            }
        }
        
        if (criteria.getExcludedStates() != null && criteria.getExcludedStates().contains(farmer.getState())) {
            unmet.add("Not available in " + farmer.getState());
        }
    }

    private void checkCropCriteria(FarmerProfileDTO farmer, Scheme scheme, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet, List<String> verification) {
        if (criteria.getRequiredCrops() != null && !criteria.getRequiredCrops().isEmpty()) {
            if (farmer.getCrops() == null || farmer.getCrops().isEmpty()) {
                verification.add("Crop information needs to be added");
                return;
            }
            
            List<String> matchingCrops = new ArrayList<>();
            for (String requiredCrop : criteria.getRequiredCrops()) {
                for (String farmerCrop : farmer.getCrops()) {
                    if (farmerCrop.equalsIgnoreCase(requiredCrop)) {
                        matchingCrops.add(farmerCrop);
                        break;
                    }
                }
            }
            
            if (!matchingCrops.isEmpty()) {
                met.add("Crops match: " + String.join(", ", matchingCrops));
            } else {
                unmet.add("Required crops not found in your profile");
            }
        }
        
        if (criteria.getExcludedCrops() != null && !criteria.getExcludedCrops().isEmpty()) {
            if (farmer.getCrops() != null) {
                for (String excludedCrop : criteria.getExcludedCrops()) {
                    for (String farmerCrop : farmer.getCrops()) {
                        if (farmerCrop.equalsIgnoreCase(excludedCrop)) {
                            unmet.add("Excluded crop: " + farmerCrop);
                            return;
                        }
                    }
                }
            }
            met.add("No excluded crops found");
        }
    }

    private void checkCategoryCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet) {
        if (criteria.getRequiredCategories() != null && !criteria.getRequiredCategories().isEmpty()) {
            if (farmer.getCategory() != null && 
                criteria.getRequiredCategories().stream()
                    .anyMatch(c -> c.equalsIgnoreCase(farmer.getCategory()))) {
                met.add("Category: " + farmer.getCategory());
            } else {
                unmet.add("Category requirement not met");
            }
        }
    }

    private void checkGenderCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet) {
        if (criteria.getRequiredGenders() != null && !criteria.getRequiredGenders().isEmpty()) {
            if (farmer.getGender() != null && 
                criteria.getRequiredGenders().stream()
                    .anyMatch(g -> g.equalsIgnoreCase(farmer.getGender()))) {
                met.add("Gender requirement met");
            } else {
                unmet.add("Gender requirement not met");
            }
        }
    }

    private void checkAgeCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet) {
        if (criteria.getMinAge() != null || criteria.getMaxAge() != null) {
            if (farmer.getAge() == null) {
                unmet.add("Age verification needed");
                return;
            }
            
            boolean metMin = criteria.getMinAge() == null || farmer.getAge() >= criteria.getMinAge();
            boolean metMax = criteria.getMaxAge() == null || farmer.getAge() <= criteria.getMaxAge();
            
            if (metMin && metMax) {
                met.add("Age (" + farmer.getAge() + ") within required range");
            } else {
                unmet.add("Age requirement not met");
            }
        }
    }

    private void checkIncomeCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet) {
        if (criteria.getMaxAnnualIncome() != null) {
            if (farmer.getAnnualIncome() == null) {
                unmet.add("Income verification needed");
                return;
            }
            
            if (farmer.getAnnualIncome().compareTo(criteria.getMaxAnnualIncome()) <= 0) {
                met.add("Income within limit");
            } else {
                unmet.add("Income exceeds maximum allowed");
            }
        }
    }

    private void checkIrrigationCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet) {
        if (criteria.getRequiredIrrigationTypes() != null && !criteria.getRequiredIrrigationTypes().isEmpty()) {
            if (farmer.getIrrigationType() != null && 
                criteria.getRequiredIrrigationTypes().stream()
                    .anyMatch(i -> i.equalsIgnoreCase(farmer.getIrrigationType()))) {
                met.add("Irrigation type: " + farmer.getIrrigationType());
            } else {
                unmet.add("Irrigation type requirement not met");
            }
        }
    }

    private void checkTenantFarmerCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet) {
        if (criteria.getTenantFarmersEligible() != null) {
            if (Boolean.TRUE.equals(criteria.getTenantFarmersEligible())) {
                if (Boolean.TRUE.equals(farmer.getIsTenantFarmer())) {
                    met.add("Tenant farmer eligible");
                } else {
                    // Not a tenant farmer, but scheme allows tenant farmers
                    met.add("Scheme allows tenant farmers");
                }
            } else {
                // Scheme does not allow tenant farmers
                if (Boolean.TRUE.equals(farmer.getIsTenantFarmer())) {
                    unmet.add("Scheme not available for tenant farmers");
                } else {
                    met.add("Not a tenant farmer (requirement met)");
                }
            }
        }
    }

    private void checkExperienceCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet) {
        if (criteria.getMinFarmingExperience() != null) {
            if (farmer.getFarmingExperienceYears() == null) {
                unmet.add("Farming experience verification needed");
                return;
            }
            
            if (farmer.getFarmingExperienceYears() >= criteria.getMinFarmingExperience()) {
                met.add("Farming experience: " + farmer.getFarmingExperienceYears() + " years");
            } else {
                unmet.add("Minimum farming experience not met");
            }
        }
    }

    private void checkKCCCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet) {
        if (Boolean.TRUE.equals(criteria.getKccRequired())) {
            if (Boolean.TRUE.equals(farmer.getHasKisanCreditCard())) {
                met.add("Kisan Credit Card available");
            } else {
                unmet.add("Kisan Credit Card required");
            }
        }
    }

    private void checkPMKisanCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet) {
        if (Boolean.TRUE.equals(criteria.getPmKisanRequired())) {
            if (Boolean.TRUE.equals(farmer.getHasPMKisanRegistration())) {
                met.add("PM-Kisan registration verified");
            } else {
                unmet.add("PM-Kisan registration required");
            }
        }
    }

    private void checkPMFBYCriteria(FarmerProfileDTO farmer, EligibilityCriteriaDTO criteria,
            List<String> met, List<String> unmet) {
        if (Boolean.TRUE.equals(criteria.getPmfbyRequired())) {
            if (Boolean.TRUE.equals(farmer.getHasPMFBYInsurance())) {
                met.add("PMFBY insurance verified");
            } else {
                unmet.add("PMFBY insurance required");
            }
        }
    }

    private void checkApplicationDeadline(Scheme scheme, List<String> unmet) {
        LocalDate today = LocalDate.now();
        
        if (scheme.getApplicationStartDate() != null && today.isBefore(scheme.getApplicationStartDate())) {
            unmet.add("Application period not yet started");
        }
        
        if (scheme.getApplicationEndDate() != null && today.isAfter(scheme.getApplicationEndDate())) {
            unmet.add("Application deadline has passed");
        }
    }

    private EligibilityResultDTO.EligibilityStatus determineEligibilityStatus(
            List<String> unmet, List<String> verification, Scheme scheme) {
        
        LocalDate today = LocalDate.now();
        
        // Check deadline status first
        if (scheme.getApplicationEndDate() != null && today.isAfter(scheme.getApplicationEndDate())) {
            return EligibilityResultDTO.EligibilityStatus.EXPIRED;
        }
        
        if (scheme.getApplicationStartDate() != null && today.isBefore(scheme.getApplicationStartDate())) {
            return EligibilityResultDTO.EligibilityStatus.NOT_YET_OPEN;
        }
        
        // If there are verification needs but no unmet criteria
        if (!verification.isEmpty() && unmet.isEmpty()) {
            return EligibilityResultDTO.EligibilityStatus.POTENTIALLY_ELIGIBLE;
        }
        
        // Check if there are unmet criteria
        if (!unmet.isEmpty()) {
            // Check if all unmet criteria are deadline-related
            boolean onlyDeadlineIssues = unmet.stream()
                    .allMatch(c -> c.contains("deadline") || c.contains("not yet started"));
            if (onlyDeadlineIssues) {
                return EligibilityResultDTO.EligibilityStatus.POTENTIALLY_ELIGIBLE;
            }
            return EligibilityResultDTO.EligibilityStatus.NOT_ELIGIBLE;
        }
        
        // All criteria met
        return EligibilityResultDTO.EligibilityStatus.ELIGIBLE;
    }

    private Long calculateDaysUntilDeadline(Scheme scheme) {
        if (scheme.getApplicationEndDate() == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), scheme.getApplicationEndDate());
    }

    private Double calculateRankingScore(EligibilityResultDTO result, Scheme scheme) {
        // Base score from benefit amount (normalized to 0-100)
        Double benefitScore = 0.0;
        if (scheme.getBenefitAmount() != null) {
            // Assuming max benefit of 10 lakhs for normalization
            benefitScore = Math.min(scheme.getBenefitAmount() / 100000.0, 100.0);
        }
        
        // Deadline proximity score (closer deadline = higher score)
        Double deadlineScore = 0.0;
        if (result.getDaysUntilDeadline() != null && result.getDaysUntilDeadline() > 0) {
            if (result.getDaysUntilDeadline() <= 7) {
                deadlineScore = 30.0; // High priority for urgent deadlines
            } else if (result.getDaysUntilDeadline() <= 30) {
                deadlineScore = 20.0;
            } else {
                deadlineScore = 10.0;
            }
        } else if (result.getDaysUntilDeadline() == null || result.getDaysUntilDeadline() < 0) {
            deadlineScore = 5.0; // Lower priority for expired or no deadline
        }
        
        // Confidence multiplier
        Double confidenceMultiplier = 1.0;
        if (result.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.HIGH) {
            confidenceMultiplier = 1.5;
        } else if (result.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.MEDIUM) {
            confidenceMultiplier = 1.2;
        } else if (result.getConfidenceLevel() == EligibilityResultDTO.ConfidenceLevel.LOW) {
            confidenceMultiplier = 0.8;
        }
        
        // Calculate final score
        Double score = benefitScore + (deadlineScore * confidenceMultiplier);
        
        // Add priority weight if available
        if (scheme.getEligibilityCriteria() != null) {
            try {
                EligibilityCriteriaDTO criteria = parseEligibilityCriteria(scheme.getEligibilityCriteria());
                if (criteria.getPriorityWeight() != null) {
                    score = score + criteria.getPriorityWeight();
                }
            } catch (Exception e) {
                log.warn("Error parsing priority weight from eligibility criteria", e);
            }
        }
        
        return score;
    }

    private EligibilityCriteriaDTO parseEligibilityCriteria(String eligibilityCriteriaJson) {
        if (eligibilityCriteriaJson == null || eligibilityCriteriaJson.isEmpty()) {
            return EligibilityCriteriaDTO.builder().build();
        }
        
        try {
            return objectMapper.readValue(eligibilityCriteriaJson, EligibilityCriteriaDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("Error parsing eligibility criteria JSON: {}", e.getMessage());
            return EligibilityCriteriaDTO.builder().build();
        }
    }
}