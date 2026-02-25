package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the complete crop history analysis result.
 * 
 * Requirements: 3.1, 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropHistoryAnalysisResultDto {

    private Long farmerId;
    private Long farmId;
    private boolean hasSufficientHistory;
    private Integer seasonsAnalyzed;
    
    // Crop history entries (past 3 seasons)
    private List<CropHistoryEntryDto> cropHistory;
    
    // Identified risks
    private List<NutrientDepletionRiskDto> nutrientDepletionRisks;
    
    // Analysis summary
    private AnalysisSummary summary;
    
    // Recommendations
    private List<String> recommendations;
    
    /**
     * Summary of the crop history analysis.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisSummary {
        private String dominantCropFamily;
        private Integer consecutiveMonocultureCount;
        private String rotationPattern;
        private String nutrientBalanceAssessment;
        private String pestDiseaseRiskLevel;
        private boolean hasGoodRotation;
        private boolean hasNutrientDepletionRisk;
    }
}