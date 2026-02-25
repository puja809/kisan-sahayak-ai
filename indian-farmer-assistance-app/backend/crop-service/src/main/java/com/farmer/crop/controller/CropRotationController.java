package com.farmer.crop.controller;

import com.farmer.crop.dto.*;
import com.farmer.crop.service.CropHistoryAnalyzer;
import com.farmer.crop.service.CropRotationService;
import com.farmer.crop.service.RotationRecommendationEngine;
import com.farmer.crop.service.RotationRankingDisplayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST controller for crop rotation recommendations.
 * 
 * Provides endpoints for:
 * - Getting rotation recommendations for a farmer based on their crop history
 * - Generating new rotation recommendations with preferences
 * 
 * Validates: Requirements 3.1, 3.9, 3.10, 3.11
 */
@RestController
@RequestMapping("/api/v1/crops/rotation")
public class CropRotationController {

    private static final Logger logger = LoggerFactory.getLogger(CropRotationController.class);

    private final CropHistoryAnalyzer cropHistoryAnalyzer;
    private final RotationRecommendationEngine rotationRecommendationEngine;
    private final RotationRankingDisplayService rotationRankingDisplayService;
    private final CropRotationService cropRotationService;

    public CropRotationController(
            CropHistoryAnalyzer cropHistoryAnalyzer,
            RotationRecommendationEngine rotationRecommendationEngine,
            RotationRankingDisplayService rotationRankingDisplayService,
            CropRotationService cropRotationService) {
        this.cropHistoryAnalyzer = cropHistoryAnalyzer;
        this.rotationRecommendationEngine = rotationRecommendationEngine;
        this.rotationRankingDisplayService = rotationRankingDisplayService;
        this.cropRotationService = cropRotationService;
    }

    /**
     * Get crop rotation recommendations for a farmer.
     * 
     * This endpoint retrieves rotation recommendations based on the farmer's
     * crop history, analyzing patterns for the past 3 seasons and providing
     * ranked rotation options.
     * 
     * @param farmerId Farmer ID
     * @param season Optional season filter (KHARIF, RABI, ZAID)
     * @return Rotation recommendation result with options and analysis
     * 
     * Validates: Requirements 3.1, 3.9, 3.10, 3.11
     */
    @GetMapping("/{farmerId}")
    public ResponseEntity<CropRotationResponseDto> getRotationRecommendations(
            @PathVariable String farmerId,
            @RequestParam(required = false) String season) {
        
        logger.info("Received rotation recommendation request for farmer: {}", farmerId);
        
        // Validate farmer ID
        if (farmerId == null || farmerId.trim().isEmpty()) {
            logger.warn("Invalid farmer ID in request");
            return ResponseEntity.badRequest().body(
                    CropRotationResponseDto.builder()
                            .success(false)
                            .errorMessage("Farmer ID is required")
                            .build()
            );
        }

        try {
            // Build request with farmer ID
            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .farmerId(Long.parseLong(farmerId))
                    .season(season)
                    .cropHistory(Collections.emptyList()) // Will be populated from repository
                    .build();

            // Generate recommendations
            RotationRecommendationResultDto result = rotationRecommendationEngine.generateRecommendations(request);
            
            // Add season-wise schedules
            List<RotationOptionDto> optionsWithSchedules = 
                    rotationRankingDisplayService.generateSeasonWiseSchedules(result.getOptions());
            
            // Add residue management recommendations
            optionsWithSchedules = rotationRankingDisplayService.addResidueManagementRecommendations(optionsWithSchedules);
            
            // Rank options by overall benefit
            List<RotationOptionDto> rankedOptions = 
                    cropRotationService.rankRotationOptions(optionsWithSchedules);
            
            // Build response
            CropRotationResponseDto response = CropRotationResponseDto.builder()
                    .success(true)
                    .farmerId(farmerId)
                    .recommendations(rankedOptions)
                    .warnings(result.getWarnings())
                    .generalRecommendations(result.getRecommendations())
                    .hasRiceBasedSystem(result.isHasRiceBasedSystem())
                    .pestRiskLevel(result.getPestRiskLevel())
                    .targetSeason(result.getTargetSeason())
                    .seasonsAnalyzed(result.getSeasonsAnalyzed())
                    .build();

            logger.info("Generated {} rotation recommendations for farmer: {}", 
                    rankedOptions.size(), farmerId);
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid farmer ID format: {}", farmerId);
            return ResponseEntity.badRequest().body(
                    CropRotationResponseDto.builder()
                            .success(false)
                            .errorMessage("Invalid farmer ID format")
                            .build()
            );
        } catch (Exception e) {
            logger.error("Error generating rotation recommendations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    CropRotationResponseDto.builder()
                            .success(false)
                            .errorMessage("Failed to generate rotation recommendations: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Generate crop rotation recommendations with full request body.
     * 
     * This endpoint generates personalized rotation recommendations based on:
     * - Crop history (past 3 seasons)
     * - Farmer preferences (preferred/avoided crops)
     * - Farm constraints (irrigation, area)
     * - Agro-ecological zone for zone-specific recommendations
     * 
     * @param request Rotation recommendation request with full details
     * @return Rotation recommendation result with ranked options and season-wise schedules
     * 
     * Validates: Requirements 3.1, 3.9, 3.10, 3.11
     */
    @PostMapping("/generate")
    public ResponseEntity<CropRotationResponseDto> generateRotationRecommendations(
            @RequestBody RotationRecommendationRequestDto request) {
        
        logger.info("Received rotation generation request for farmer: {}", request.getFarmerId());
        
        // Validate request
        if (request.getFarmerId() == null) {
            logger.warn("Missing farmer ID in request");
            return ResponseEntity.badRequest().body(
                    CropRotationResponseDto.builder()
                            .success(false)
                            .errorMessage("Farmer ID is required")
                            .build()
            );
        }

        try {
            // Analyze crop history if provided
            CropHistoryAnalysisResultDto historyAnalysis = null;
            if (request.getCropHistory() != null && !request.getCropHistory().isEmpty()) {
                historyAnalysis = cropHistoryAnalyzer.analyzeCropHistory(request.getCropHistory());
                logger.info("Analyzed {} seasons of crop history for farmer: {}", 
                        historyAnalysis.getSeasonsAnalyzed(), request.getFarmerId());
            }

            // Generate rotation recommendations
            RotationRecommendationResultDto result = rotationRecommendationEngine.generateRecommendations(request);
            
            // Add season-wise schedules
            List<RotationOptionDto> optionsWithSchedules = 
                    rotationRankingDisplayService.generateSeasonWiseSchedules(result.getOptions());
            
            // Add residue management recommendations
            optionsWithSchedules = rotationRankingDisplayService.addResidueManagementRecommendations(optionsWithSchedules);
            
            // Rank options by overall benefit (soil health, climate resilience, economic viability)
            List<RotationOptionDto> rankedOptions = 
                    cropRotationService.rankRotationOptions(optionsWithSchedules);

            // If no crop history, provide default patterns for the agro-ecological zone
            List<RotationOptionDto> defaultPatterns = null;
            if (request.getCropHistory() == null || request.getCropHistory().isEmpty()) {
                String zone = request.getAgroEcologicalZone();
                if (zone == null || zone.isEmpty()) {
                    zone = "Indo-Gangetic Plains"; // Default zone
                }
                defaultPatterns = rotationRankingDisplayService.getDefaultRotationPatterns(zone);
                logger.info("Generated {} default rotation patterns for zone: {}", 
                        defaultPatterns.size(), zone);
            }

            // Build response
            CropRotationResponseDto response = CropRotationResponseDto.builder()
                    .success(true)
                    .farmerId(String.valueOf(request.getFarmerId()))
                    .recommendations(rankedOptions)
                    .defaultPatterns(defaultPatterns)
                    .warnings(result.getWarnings())
                    .generalRecommendations(result.getRecommendations())
                    .hasRiceBasedSystem(result.isHasRiceBasedSystem())
                    .pestRiskLevel(result.getPestRiskLevel())
                    .targetSeason(request.getSeason())
                    .seasonsAnalyzed(historyAnalysis != null ? historyAnalysis.getSeasonsAnalyzed() : 0)
                    .historyAnalysis(historyAnalysis)
                    .build();

            logger.info("Generated {} rotation recommendations for farmer: {}", 
                    rankedOptions.size(), request.getFarmerId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating rotation recommendations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    CropRotationResponseDto.builder()
                            .success(false)
                            .errorMessage("Failed to generate rotation recommendations: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Analyze crop history for a farmer.
     * 
     * This endpoint analyzes the farmer's crop history to identify:
     * - Nutrient depletion risks
     * - Rotation patterns
     * - Pest and disease risks
     * 
     * @param farmerId Farmer ID
     * @return Crop history analysis result
     * 
     * Validates: Requirements 3.1, 3.2
     */
    @GetMapping("/{farmerId}/history-analysis")
    public ResponseEntity<CropHistoryAnalysisResultDto> analyzeCropHistory(
            @PathVariable String farmerId) {
        
        logger.info("Received crop history analysis request for farmer: {}", farmerId);
        
        // Validate farmer ID
        if (farmerId == null || farmerId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    CropHistoryAnalysisResultDto.builder()
                            .hasSufficientHistory(false)
                            .recommendations(Collections.singletonList("Farmer ID is required"))
                            .build()
            );
        }

        try {
            // For now, return empty analysis - in real implementation, 
            // this would fetch from the crop history repository
            CropHistoryAnalysisResultDto emptyAnalysis = CropHistoryAnalysisResultDto.builder()
                    .farmerId(Long.parseLong(farmerId))
                    .hasSufficientHistory(false)
                    .seasonsAnalyzed(0)
                    .cropHistory(Collections.emptyList())
                    .nutrientDepletionRisks(Collections.emptyList())
                    .summary(CropHistoryAnalysisResultDto.AnalysisSummary.builder()
                            .rotationPattern("No crop history available")
                            .nutrientBalanceAssessment("Cannot assess - no history")
                            .pestDiseaseRiskLevel("Cannot assess - no history")
                            .hasGoodRotation(false)
                            .hasNutrientDepletionRisk(false)
                            .build())
                    .recommendations(Collections.singletonList(
                            "Start recording crop history to receive personalized rotation recommendations"))
                    .build();

            return ResponseEntity.ok(emptyAnalysis);
            
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    CropHistoryAnalysisResultDto.builder()
                            .hasSufficientHistory(false)
                            .recommendations(Collections.singletonList("Invalid farmer ID format"))
                            .build()
            );
        }
    }

    /**
     * Get default rotation patterns for an agro-ecological zone.
     * 
     * This endpoint provides default rotation patterns for farmers with no
     * crop history, based on their agro-ecological zone.
     * 
     * @param zone Agro-ecological zone name
     * @return List of default rotation patterns
     * 
     * Validates: Requirement 3.11
     */
    @GetMapping("/defaults/{zone}")
    public ResponseEntity<Map<String, Object>> getDefaultRotationPatterns(
            @PathVariable String zone) {
        
        logger.info("Received default rotation patterns request for zone: {}", zone);
        
        if (zone == null || zone.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Agro-ecological zone is required"
            ));
        }

        try {
            List<RotationOptionDto> patterns = 
                    rotationRankingDisplayService.getDefaultRotationPatterns(zone);
            
            return ResponseEntity.ok(Map.of(
                    "zone", zone,
                    "defaultPatterns", patterns,
                    "patternCount", patterns.size()
            ));
            
        } catch (Exception e) {
            logger.error("Error getting default rotation patterns: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to get default rotation patterns: " + e.getMessage()
            ));
        }
    }
}