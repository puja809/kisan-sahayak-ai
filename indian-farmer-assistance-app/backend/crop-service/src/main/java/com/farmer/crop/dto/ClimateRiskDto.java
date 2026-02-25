package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for climate risk analysis.
 * 
 * Contains climate risk assessment including rainfall deviation scenarios
 * to flag crops with high climate risk.
 * 
 * Validates: Requirement 2.8
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClimateRiskDto {
    
    /**
     * Crop code
     */
    private String cropCode;
    
    /**
     * Crop name
     */
    private String cropName;
    
    /**
     * Overall climate risk level
     */
    private ClimateRiskLevel riskLevel;
    
    /**
     * Risk score (0-100, higher = more risky)
     */
    private BigDecimal riskScore;
    
    /**
     * Rainfall deviation scenario analysis
     */
    private RainfallDeviationScenario rainfallScenario;
    
    /**
     * Temperature stress analysis
     */
    private TemperatureStressAnalysis temperatureStress;
    
    /**
     * Drought risk level
     */
    private DroughtRiskLevel droughtRisk;
    
    /**
     * Flood risk level
     */
    private FloodRiskLevel floodRisk;
    
    /**
     * Key climate risks for this crop
     */
    private List<String> keyRisks;
    
    /**
     * Recommended mitigation strategies
     */
    private List<String> mitigationStrategies;
    
    /**
     * Climate-resilient variety recommendations
     */
    private List<String> resilientVarieties;
    
    /**
     * Optimal planting window considering climate
     */
    private String optimalPlantingWindow;
    
    /**
     * Whether insurance is recommended
     */
    private Boolean insuranceRecommended;
    
    /**
     * Climate risk level enum
     */
    public enum ClimateRiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH
    }
    
    /**
     * Rainfall deviation scenario
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RainfallDeviationScenario {
        
        /**
         * Historical average rainfall (mm)
         */
        private BigDecimal historicalAverageMm;
        
        /**
         * Projected rainfall deviation (%)
         * Negative = below normal, Positive = above normal
         */
        private BigDecimal projectedDeviationPercent;
        
        /**
         * Scenario type
         */
        private ScenarioType scenarioType;
        
        /**
         * Impact on crop yield (%)
         */
        private BigDecimal yieldImpactPercent;
        
        /**
         * Probability of this scenario (%)
         */
        private BigDecimal probability;
        
        /**
         * Risk level for this scenario
         */
        private ClimateRiskLevel scenarioRiskLevel;
        
        /**
         * Scenario type enum
         */
        public enum ScenarioType {
            DEFICIT,          // Below normal rainfall
            NORMAL,           // Normal rainfall
            EXCESS,           // Above normal rainfall
            EARLY_ONSET,      // Early monsoon onset
            LATE_WITHDRAWAL,  // Late monsoon withdrawal
            DRY_SPELL,        // Extended dry spell during growing season
            HEAVY_RAINFALL    // Heavy rainfall events
        }
    }
    
    /**
     * Temperature stress analysis
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemperatureStressAnalysis {
        
        /**
         * Optimal temperature range (°C)
         */
        private BigDecimal optimalTempMin;
        private BigDecimal optimalTempMax;
        
        /**
         * Heat stress threshold (°C)
         */
        private BigDecimal heatStressThreshold;
        
        /**
         * Cold stress threshold (°C)
         */
        private BigDecimal coldStressThreshold;
        
        /**
         * Projected temperature deviation (°C)
         */
        private BigDecimal projectedDeviation;
        
        /**
         * Number of extreme heat days projected
         */
        private Integer extremeHeatDays;
        
        /**
         * Number of extreme cold days projected
         */
        private Integer extremeColdDays;
        
        /**
         * Temperature stress risk level
         */
        private ClimateRiskLevel stressRiskLevel;
    }
    
    /**
     * Drought risk level enum
     */
    public enum DroughtRiskLevel {
        NONE,
        LOW,
        MODERATE,
        HIGH,
        SEVERE
    }
    
    /**
     * Flood risk level enum
     */
    public enum FloodRiskLevel {
        NONE,
        LOW,
        MODERATE,
        HIGH,
        SEVERE
    }
}