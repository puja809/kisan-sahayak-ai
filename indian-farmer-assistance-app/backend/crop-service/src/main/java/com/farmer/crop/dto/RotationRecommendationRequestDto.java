package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for rotation recommendation generation.
 * 
 * Requirements: 3.3, 3.4, 3.5, 3.6, 3.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotationRecommendationRequestDto {

    private Long farmerId;
    private Long farmId;
    
    // Crop history for analysis (past 3 seasons)
    private List<CropHistoryEntryDto> cropHistory;
    
    // Target season for recommendations (KHARIF, RABI, ZAID)
    private String season;
    
    // Farmer preferences
    private List<String> preferredCrops;
    private List<String> avoidedCrops;
    
    // Farm constraints
    private Boolean isIrrigated;
    private Double totalAreaAcres;
    
    // Agro-ecological zone for zone-specific recommendations
    private String agroEcologicalZone;
}