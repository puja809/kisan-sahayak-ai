package com.farmer.scheme.controller;

import com.farmer.scheme.dto.*;
import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.service.EligibilityAssessmentService;
import com.farmer.scheme.service.SchemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for eligibility assessment and personalized scheme recommendations.
 * Requirements: 4.4, 4.5, 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8
 */
@RestController
@RequestMapping("/api/v1/schemes")
@RequiredArgsConstructor
@Slf4j
public class EligibilityController {

    private final EligibilityAssessmentService eligibilityAssessmentService;
    private final SchemeService schemeService;

    /**
     * Check eligibility for a specific scheme.
     * POST /api/v1/schemes/eligibility/check/{schemeId}
     * Requirements: 4.4
     */
    @PostMapping("/eligibility/check/{schemeId}")
    public ResponseEntity<EligibilityResultDTO> checkSchemeEligibility(
            @RequestBody FarmerProfileDTO farmerProfile,
            @PathVariable Long schemeId) {
        log.debug("POST /api/v1/schemes/eligibility/check/{} - Checking eligibility for scheme", schemeId);
        
        return schemeService.getSchemeById(schemeId)
                .map(scheme -> {
                    EligibilityResultDTO result = eligibilityAssessmentService.assessEligibility(farmerProfile, scheme);
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get personalized scheme recommendations for a farmer.
     * Analyzes farmer's profile for all scheme eligibility and ranks by benefit and deadline.
     * GET /api/v1/schemes/personalized/{farmerId}
     * Requirements: 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8
     */
    @GetMapping("/personalized/{farmerId}")
    public ResponseEntity<List<PersonalizedSchemeDTO>> getPersonalizedRecommendations(
            @PathVariable Long farmerId,
            @RequestParam(required = false) String state) {
        log.debug("GET /api/v1/schemes/personalized/{} - Getting personalized recommendations", farmerId);
        
        // Build farmer profile from request parameters
        FarmerProfileDTO farmerProfile = FarmerProfileDTO.builder()
                .userId(farmerId)
                .state(state != null ? state : "Maharashtra") // Default state
                .build();
        
        List<PersonalizedSchemeDTO> recommendations = 
                eligibilityAssessmentService.getPersonalizedRecommendations(farmerProfile);
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get personalized recommendations with farmer profile in request body.
     * POST /api/v1/schemes/personalized
     * Requirements: 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8
     */
    @PostMapping("/personalized")
    public ResponseEntity<List<PersonalizedSchemeDTO>> getPersonalizedRecommendationsWithProfile(
            @RequestBody FarmerProfileDTO farmerProfile) {
        log.debug("POST /api/v1/schemes/personalized - Getting personalized recommendations with profile");
        
        List<PersonalizedSchemeDTO> recommendations = 
                eligibilityAssessmentService.getPersonalizedRecommendations(farmerProfile);
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get all schemes with eligibility assessment for a farmer.
     * POST /api/v1/schemes/eligibility/all
     * Requirements: 11D.1
     */
    @PostMapping("/eligibility/all")
    public ResponseEntity<List<EligibilityResultDTO>> getAllSchemesWithEligibility(
            @RequestBody FarmerProfileDTO farmerProfile) {
        log.debug("POST /api/v1/schemes/eligibility/all - Getting all schemes with eligibility");
        
        List<EligibilityResultDTO> schemes = 
                eligibilityAssessmentService.getAllSchemesWithEligibility(farmerProfile);
        
        return ResponseEntity.ok(schemes);
    }

    /**
     * Get schemes with high eligibility confidence for a farmer.
     * POST /api/v1/schemes/eligibility/high-confidence
     * Requirements: 4.5, 11D.6
     */
    @PostMapping("/eligibility/high-confidence")
    public ResponseEntity<List<PersonalizedSchemeDTO>> getHighConfidenceSchemes(
            @RequestBody FarmerProfileDTO farmerProfile) {
        log.debug("POST /api/v1/schemes/eligibility/high-confidence - Getting high confidence schemes");
        
        List<PersonalizedSchemeDTO> schemes = 
                eligibilityAssessmentService.getHighConfidenceSchemes(farmerProfile);
        
        return ResponseEntity.ok(schemes);
    }

    /**
     * Get schemes with approaching deadlines for a farmer.
     * POST /api/v1/schemes/eligibility/approaching-deadlines
     * Requirements: 4.8, 11D.7, 11D.9
     */
    @PostMapping("/eligibility/approaching-deadlines")
    public ResponseEntity<List<PersonalizedSchemeDTO>> getSchemesWithApproachingDeadlines(
            @RequestBody FarmerProfileDTO farmerProfile,
            @RequestParam(defaultValue = "30") int daysAhead) {
        log.debug("POST /api/v1/schemes/eligibility/approaching-deadlines - Getting schemes with approaching deadlines");
        
        List<PersonalizedSchemeDTO> schemes = 
                eligibilityAssessmentService.getSchemesWithApproachingDeadlines(farmerProfile, daysAhead);
        
        return ResponseEntity.ok(schemes);
    }

    /**
     * Get personalized scheme recommendations with full details.
     * Returns personalized scheme DTOs with additional information.
     * POST /api/v1/schemes/recommendations
     * Requirements: 4.4, 4.5, 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8
     */
    @PostMapping("/recommendations")
    public ResponseEntity<List<PersonalizedSchemeDTO>> getRecommendations(
            @RequestBody FarmerProfileDTO farmerProfile) {
        log.debug("POST /api/v1/schemes/recommendations - Getting full recommendations");
        
        List<PersonalizedSchemeDTO> recommendations = 
                eligibilityAssessmentService.getPersonalizedRecommendations(farmerProfile);
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get schemes filtered by confidence level.
     * GET /api/v1/schemes/eligibility/filter?confidence=HIGH&state=Maharashtra
     * Requirements: 4.5, 11D.6
     */
    @GetMapping("/eligibility/filter")
    public ResponseEntity<List<PersonalizedSchemeDTO>> filterSchemesByConfidence(
            @RequestBody FarmerProfileDTO farmerProfile,
            @RequestParam(required = false) EligibilityResultDTO.ConfidenceLevel confidenceLevel,
            @RequestParam(required = false) Boolean approachingDeadline,
            @RequestParam(required = false) Scheme.SchemeType schemeType) {
        log.debug("GET /api/v1/schemes/eligibility/filter - Filtering schemes");
        
        List<PersonalizedSchemeDTO> schemes = 
                eligibilityAssessmentService.getPersonalizedRecommendations(farmerProfile);
        
        // Apply filters
        if (confidenceLevel != null) {
            schemes = schemes.stream()
                    .filter(s -> s.getConfidenceLevel() == confidenceLevel)
                    .collect(Collectors.toList());
        }
        
        if (approachingDeadline != null && approachingDeadline) {
            schemes = schemes.stream()
                    .filter(s -> s.getDaysUntilDeadline() != null && 
                                s.getDaysUntilDeadline() > 0 && 
                                s.getDaysUntilDeadline() <= 30)
                    .collect(Collectors.toList());
        }
        
        if (schemeType != null) {
            schemes = schemes.stream()
                    .filter(s -> s.getSchemeType() == schemeType)
                    .collect(Collectors.toList());
        }
        
        return ResponseEntity.ok(schemes);
    }
}