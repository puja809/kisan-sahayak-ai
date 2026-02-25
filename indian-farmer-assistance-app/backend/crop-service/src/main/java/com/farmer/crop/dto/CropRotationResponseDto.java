package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for crop rotation endpoints.
 * 
 * Contains rotation recommendations, warnings, and analysis results.
 * 
 * Requirements: 3.1, 3.9, 3.10, 3.11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropRotationResponseDto {

    // Success indicator
    private boolean success;
    
    // Error message if success is false
    private String errorMessage;
    
    // Farmer ID
    private String farmerId;
    
    // List of rotation options ranked by overall benefit
    private List<RotationOptionDto> recommendations;
    
    // Default rotation patterns for farmers with no history
    private List<RotationOptionDto> defaultPatterns;
    
    // Warnings about potential issues
    private List<String> warnings;
    
    // General recommendations based on analysis
    private List<String> generalRecommendations;
    
    // Analysis flags
    private boolean hasRiceBasedSystem;
    private String pestRiskLevel; // LOW, MEDIUM, HIGH
    
    // Season information
    private String targetSeason;
    
    // Number of seasons analyzed
    private Integer seasonsAnalyzed;
    
    // Detailed history analysis (if crop history was provided)
    private CropHistoryAnalysisResultDto historyAnalysis;
}