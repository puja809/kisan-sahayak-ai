package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for fertilizer recommendation.
 * 
 * Validates: Requirements 11C.1, 11C.2, 11C.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FertilizerRecommendationRequestDto {

    /**
     * Farmer ID
     */
    private String farmerId;

    /**
     * Crop ID (optional, if not provided will use crop details)
     */
    private Long cropId;

    /**
     * Crop name (required if cropId not provided)
     */
    private String cropName;

    /**
     * Crop variety
     */
    private String cropVariety;

    /**
     * Season (KHARIF, RABI, ZAID)
     */
    private String season;

    /**
     * Growth stage of the crop
     */
    private String growthStage;

    /**
     * Area in acres
     */
    private Double areaAcres;

    /**
     * Agro-ecological zone code
     */
    private String agroEcologicalZone;

    /**
     * State
     */
    private String state;

    /**
     * District
     */
    private String district;

    /**
     * Soil health card data (optional)
     */
    private SoilHealthCardDto soilHealthCard;

    /**
     * Whether to include organic alternatives
     */
    private Boolean includeOrganicAlternatives;

    /**
     * Whether to include split application schedule
     */
    private Boolean includeSplitApplication;

    /**
     * Irrigation type
     */
    private String irrigationType;

    /**
     * Previous crop grown (for nutrient balance calculation)
     */
    private String previousCrop;

    /**
     * Target yield in quintals per acre
     */
    private Double targetYield;
}