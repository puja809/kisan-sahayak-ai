package com.farmer.crop.dto;

import com.farmer.crop.enums.CropFamily;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

/**
 * DTO representing a nutrient depletion risk identified from crop history analysis.
 * 
 * Requirements: 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutrientDepletionRiskDto {

    private Long id;
    private CropFamily cropFamily;
    private String cropFamilyName;
    private RiskLevel riskLevel;
    private String riskDescription;
    private String affectedNutrients;
    private Integer consecutiveSeasons;
    private List<String> affectedCrops;
    private String recommendation;
    private Double severityScore; // 0-100 scale
    
    /**
     * Risk level for nutrient depletion.
     */
    public enum RiskLevel {
        LOW("Low risk - monitor closely"),
        MEDIUM("Medium risk - consider rotation"),
        HIGH("High risk - immediate action recommended"),
        CRITICAL("Critical risk - severe depletion likely");

        private final String description;

        RiskLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}