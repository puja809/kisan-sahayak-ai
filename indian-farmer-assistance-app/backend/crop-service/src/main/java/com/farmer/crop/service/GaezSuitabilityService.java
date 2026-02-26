package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.GaezCropData;
import com.farmer.crop.entity.SoilHealthCard;
import com.farmer.crop.repository.GaezCropDataRepository;
import com.farmer.crop.repository.SoilHealthCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating crop suitability scores using GAEZ v4 framework.
 * 
 * This service implements the GAEZ (Global Agro-Ecological Zones) methodology
 * to assess crop suitability based on:
 * - Climate factors (temperature, precipitation, growing season)
 * - Soil resources (soil type, pH, fertility)
 * - Terrain characteristics (slope, elevation)
 * - Water availability (rainfall, irrigation potential)
 * 
 * When soil health card data is available, it incorporates nutrient parameters
 * to refine the suitability assessment.
 * 
 * Validates: Requirements 2.2, 2.3, 2.4
 */
@Service
@Transactional(readOnly = true)
public class GaezSuitabilityService {

    private static final Logger logger = LoggerFactory.getLogger(GaezSuitabilityService.class);
    
    // Weight factors for overall suitability calculation
    private static final Double CLIMATE_WEIGHT = 0.30;
    private static final Double SOIL_WEIGHT = 0.25;
    private static final Double TERRAIN_WEIGHT = 0.15;
    private static final Double WATER_WEIGHT = 0.20;
    private static final Double SOIL_HEALTH_WEIGHT = 0.10;
    
    // Minimum suitability threshold
    private static final Double MIN_SUITABILITY_THRESHOLD = 40.0;
    
    private final GaezCropDataRepository gaezCropDataRepository;
    private final SoilHealthCardRepository soilHealthCardRepository;

    public GaezSuitabilityService(
            GaezCropDataRepository gaezCropDataRepository,
            SoilHealthCardRepository soilHealthCardRepository) {
        this.gaezCropDataRepository = gaezCropDataRepository;
        this.soilHealthCardRepository = soilHealthCardRepository;
    }

    /**
     * Get crop suitability data for a location.
     * 
     * @param zoneCode GAEZ zone code
     * @return List of crop suitability data
     * 
     * Validates: Requirement 2.2
     */
    public List<GaezCropSuitabilityDto> getSuitabilityForZone(String zoneCode) {
        logger.debug("Getting GAEZ suitability data for zone: {}", zoneCode);
        
        List<GaezCropData> gaezData = gaezCropDataRepository.findByZoneCodeAndIsActiveTrue(zoneCode);
        
        return gaezData.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get crop suitability data for a specific crop in a zone.
     * 
     * @param zoneCode GAEZ zone code
     * @param cropCode GAEZ crop code
     * @return Optional containing the crop suitability data
     * 
     * Validates: Requirement 2.2
     */
    public Optional<GaezCropSuitabilityDto> getSuitabilityForCrop(String zoneCode, String cropCode) {
        logger.debug("Getting GAEZ suitability for crop {} in zone {}", cropCode, zoneCode);
        
        return gaezCropDataRepository.findByZoneCodeAndCropCodeAndIsActiveTrue(zoneCode, cropCode)
                .map(this::mapToDto);
    }

    /**
     * Calculate suitability scores for a recommendation request.
     * 
     * This method combines GAEZ data with soil health card data (if available)
     * to generate comprehensive suitability scores.
     * 
     * @param request Crop recommendation request
     * @return List of recommended crops with suitability scores
     * 
     * Validates: Requirements 2.3, 2.4, 2.5
     */
    public List<GaezCropSuitabilityDto> calculateSuitabilityScores(
            CropRecommendationRequestDto request) {
        
        logger.info("Calculating suitability scores for farmer: {}", request.getFarmerId());
        
        // Get GAEZ data for the zone
        String zoneCode = request.getAgroEcologicalZoneCode();
        if (zoneCode == null || zoneCode.isEmpty()) {
            logger.warn("No zone code provided, cannot calculate suitability");
            return Collections.emptyList();
        }

        List<GaezCropData> gaezDataList = gaezCropDataRepository
                .findByZoneCodeAndIsActiveTrue(zoneCode);

        if (gaezDataList.isEmpty()) {
            logger.warn("No GAEZ data found for zone: {}", zoneCode);
            return Collections.emptyList();
        }

        // Get soil health data if available
        SoilHealthCard soilHealthCard = null;
        if (request.hasSoilHealthData()) {
            soilHealthCard = mapToSoilHealthCard(request.getSoilHealthCard());
        } else if (request.getFarmerId() != null) {
            // Try to fetch from database
            soilHealthCard = soilHealthCardRepository
                    .findMostRecentByFarmerId(request.getFarmerId())
                    .orElse(null);
        }

        // Extract request values for use in lambda (must be effectively final)
        final CropRecommendationRequestDto.Season season = request.getSeason();
        final CropRecommendationRequestDto.IrrigationType irrigationType = request.getIrrigationType();
        final SoilHealthCard finalSoilHealthCard = soilHealthCard;

        // Filter by season if specified
        List<GaezCropData> filteredData = filterBySeason(gaezDataList, season);

        // Calculate adjusted scores
        List<GaezCropSuitabilityDto> suitabilityList = filteredData.stream()
                .map(gaezData -> calculateAdjustedSuitability(gaezData, irrigationType, finalSoilHealthCard))
                .filter(s -> s.getOverallSuitabilityScore().compareTo(MIN_SUITABILITY_THRESHOLD) >= 0)
                .sorted(Comparator.comparing(GaezCropSuitabilityDto::getOverallSuitabilityScore)
                        .reversed())
                .collect(Collectors.toList());

        logger.info("Calculated {} suitable crops for zone {}", suitabilityList.size(), zoneCode);
        return suitabilityList;
    }

    /**
     * Calculate adjusted suitability score incorporating soil health data.
     * 
     * @param gaezData GAEZ crop data
     * @param irrigationType Type of irrigation available
     * @param soilHealthCard Soil health card data (may be null)
     * @return Adjusted suitability DTO
     */
    private GaezCropSuitabilityDto calculateAdjustedSuitability(
            GaezCropData gaezData,
            CropRecommendationRequestDto.IrrigationType irrigationType,
            SoilHealthCard soilHealthCard) {
        
        // Start with GAEZ base scores
        Double climateScore = gaezData.getClimateSuitabilityScore();
        Double soilScore = gaezData.getSoilSuitabilityScore();
        Double terrainScore = gaezData.getTerrainSuitabilityScore();
        Double waterScore = gaezData.getWaterSuitabilityScore();
        
        // Adjust for irrigation type
        waterScore = adjustForIrrigation(waterScore, irrigationType);
        
        // Adjust for soil health if available
        Double soilHealthAdjustment = 0.0;
        List<String> soilHealthRecommendations = new ArrayList<>();
        
        if (soilHealthCard != null) {
            soilHealthAdjustment = calculateSoilHealthAdjustment(gaezData, soilHealthCard, soilHealthRecommendations);
            soilScore = soilScore + (soilHealthAdjustment);
        }

        // Calculate overall score
        Double overallScore = calculateOverallScore(
                climateScore, soilScore, terrainScore, waterScore, soilHealthAdjustment);

        // Determine suitability classification
        GaezCropSuitabilityDto.SuitabilityClassification classification = 
                determineClassification(overallScore);

        // Build the DTO
        return GaezCropSuitabilityDto.builder()
                .cropCode(gaezData.getCropCode())
                .cropName(gaezData.getCropName())
                .cropNameLocal(gaezData.getCropNameHindi())
                .overallSuitabilityScore(overallScore)
                .suitabilityClassification(classification)
                .climateSuitabilityScore(climateScore)
                .soilSuitabilityScore(soilScore)
                .terrainSuitabilityScore(terrainScore)
                .waterSuitabilityScore(waterScore)
                .rainfedPotentialYield(gaezData.getRainfedPotentialYield())
                .irrigatedPotentialYield(gaezData.getIrrigatedPotentialYield())
                .expectedYieldMin(calculateExpectedYield(gaezData, overallScore, true))
                .expectedYieldExpected(calculateExpectedYield(gaezData, overallScore, false))
                .expectedYieldMax(calculateExpectedYield(gaezData, overallScore, false))
                .waterRequirementsMm(gaezData.getWaterRequirementsMm())
                .growingSeasonDays(gaezData.getGrowingSeasonDays())
                .kharifSuitable(gaezData.getKharifSuitable())
                .rabiSuitable(gaezData.getRabiSuitable())
                .zaidSuitable(gaezData.getZaidSuitable())
                .climateRiskLevel(mapClimateRiskLevel(gaezData.getClimateRiskLevel()))
                .dataVersion(gaezData.getDataVersion())
                .dataResolution(gaezData.getDataResolution())
                .build();
    }

    /**
     * Adjust water score based on irrigation type.
     * 
     * @param baseScore Base water suitability score
     * @param irrigationType Type of irrigation available
     * @return Adjusted score
     */
    private Double adjustForIrrigation(Double baseScore, 
            CropRecommendationRequestDto.IrrigationType irrigationType) {
        
        if (irrigationType == null) {
            return baseScore;
        }

        Double adjustment = switch (irrigationType) {
            case RAINFED -> -10.0;  // Reduce score for rain-fed only
            case DRIP, SPRINKLER -> 5.0;  // Increase for efficient irrigation
            case CANAL, BOREWELL -> 2.0;  // Slight increase for reliable irrigation
            case MIXED -> 0.0;  // No adjustment needed
        };

        return Math.max(baseScore + adjustment, 0.0);
    }

    /**
     * Calculate soil health adjustment based on nutrient data.
     * 
     * @param gaezData GAEZ crop data
     * @param soilHealthCard Soil health card data
     * @param recommendations List to add recommendations to
     * @return Adjustment score
     */
    private Double calculateSoilHealthAdjustment(
            GaezCropData gaezData,
            SoilHealthCard soilHealthCard,
            List<String> recommendations) {
        
        Double adjustment = 0.0;
        
        // Check primary nutrients
        if (soilHealthCard.getNitrogenKgHa() != null) {
            Double nitrogenStatus = getNitrogenAdjustment(soilHealthCard.getNitrogenKgHa());
            adjustment = adjustment + (nitrogenStatus);
            if (nitrogenStatus.compareTo(0.0) < 0) {
                recommendations.add("Low nitrogen: Consider nitrogen application");
            }
        }
        
        if (soilHealthCard.getPhosphorusKgHa() != null) {
            Double phosphorusStatus = getPhosphorusAdjustment(soilHealthCard.getPhosphorusKgHa());
            adjustment = adjustment + (phosphorusStatus);
            if (phosphorusStatus.compareTo(0.0) < 0) {
                recommendations.add("Low phosphorus: Consider phosphorus application");
            }
        }
        
        if (soilHealthCard.getPotassiumKgHa() != null) {
            Double potassiumStatus = getPotassiumAdjustment(soilHealthCard.getPotassiumKgHa());
            adjustment = adjustment + (potassiumStatus);
            if (potassiumStatus.compareTo(0.0) < 0) {
                recommendations.add("Low potassium: Consider potassium application");
            }
        }
        
        // Check secondary nutrients (Sulfur)
        if (soilHealthCard.getSulfurPpm() != null && soilHealthCard.getSulfurPpm().compareTo(10.0) < 0) {
            adjustment = adjustment - (2.0);
            recommendations.add("Sulfur deficiency: Apply sulfur fertilizer");
        }
        
        // Check micronutrients
        if (soilHealthCard.getZincPpm() != null && soilHealthCard.getZincPpm().compareTo(0.6) < 0) {
            adjustment = adjustment - (3.0);
            recommendations.add("Zinc deficiency: Apply zinc sulfate");
        }
        
        if (soilHealthCard.getIronPpm() != null && soilHealthCard.getIronPpm().compareTo(4.5) < 0) {
            adjustment = adjustment - (2.0);
            recommendations.add("Iron deficiency: Consider iron application");
        }
        
        // Check pH
        if (soilHealthCard.getPh() != null) {
            Double phAdjustment = getPhAdjustment(soilHealthCard.getPh());
            adjustment = adjustment + phAdjustment;
        }
        
        // Limit adjustment range
        return Math.max(adjustment, -15.0) < 10.0 ? Math.max(adjustment, -15.0) : 10.0;
    }

    private Double getNitrogenAdjustment(Double nitrogen) {
        if (nitrogen.compareTo(560.0) >= 0) {
            return 5.0;
        } else if (nitrogen.compareTo(280.0) >= 0) {
            return 0.0;
        } else {
            return new Double("-5");
        }
    }

    private Double getPhosphorusAdjustment(Double phosphorus) {
        if (phosphorus.compareTo(25.0) >= 0) {
            return 5.0;
        } else if (phosphorus.compareTo(10.0) >= 0) {
            return 0.0;
        } else {
            return new Double("-5");
        }
    }

    private Double getPotassiumAdjustment(Double potassium) {
        if (potassium.compareTo(280.0) >= 0) {
            return 5.0;
        } else if (potassium.compareTo(108.0) >= 0) {
            return 0.0;
        } else {
            return new Double("-5");
        }
    }

    private Double getPhAdjustment(Double ph) {
        // Optimal pH range is 6.0-7.5 for most crops
        if (ph.compareTo(6.0) >= 0 && ph.compareTo(7.5) <= 0) {
            return 5.0;
        } else if (ph.compareTo(5.5) >= 0 && ph.compareTo(8.0) <= 0) {
            return 0.0;
        } else {
            return new Double("-5");
        }
    }

    /**
     * Calculate overall suitability score from component scores.
     * 
     * @param climateScore Climate suitability score
     * @param soilScore Soil suitability score
     * @param terrainScore Terrain suitability score
     * @param waterScore Water availability score
     * @param soilHealthAdjustment Additional adjustment from soil health
     * @return Overall suitability score
     */
    private Double calculateOverallScore(
            Double climateScore, Double soilScore, Double terrainScore, Double waterScore, Double soilHealthAdjustment) {
        
        Double weightedScore = climateScore * (CLIMATE_WEIGHT)
                 + (soilScore * (SOIL_WEIGHT))
                 + (terrainScore * (TERRAIN_WEIGHT))
                 + (waterScore * (WATER_WEIGHT));
        
        // Add soil health adjustment
        Double overall = weightedScore + soilHealthAdjustment;
        
        // Round to 2 decimal places
        return Math.max(overall, 0.0);
    }

    /**
     * Determine suitability classification from score.
     * 
     * @param score Overall suitability score
     * @return Suitability classification
     */
    private GaezCropSuitabilityDto.SuitabilityClassification determineClassification(Double score) {
        if (score.compareTo(80.0) >= 0) {
            return GaezCropSuitabilityDto.SuitabilityClassification.HIGHLY_SUITABLE;
        } else if (score.compareTo(60.0) >= 0) {
            return GaezCropSuitabilityDto.SuitabilityClassification.SUITABLE;
        } else if (score.compareTo(40.0) >= 0) {
            return GaezCropSuitabilityDto.SuitabilityClassification.MARGINALLY_SUITABLE;
        } else {
            return GaezCropSuitabilityDto.SuitabilityClassification.NOT_SUITABLE;
        }
    }

    /**
     * Calculate expected yield based on suitability score.
     * 
     * @param gaezData GAEZ crop data
     * @param suitabilityScore Overall suitability score
     * @param isMinimum Whether to calculate minimum yield
     * @return Expected yield
     */
    private Double calculateExpectedYield(GaezCropData gaezData, Double suitabilityScore, boolean isMinimum) {
        Double potentialYield = gaezData.getIrrigatedPotentialYield() != null 
                ? gaezData.getIrrigatedPotentialYield()
                : gaezData.getRainfedPotentialYield();
        
        if (potentialYield == null) {
            return null;
        }
        
        // Yield factor based on suitability
        Double yieldFactor = suitabilityScore / 100.0;
        
        if (isMinimum) {
            yieldFactor = yieldFactor * (0.7);  // 70% of potential
        } else {
            yieldFactor = yieldFactor * (0.85);  // 85% of potential
        }
        
        return potentialYield * (yieldFactor);
    }

    /**
     * Filter GAEZ data by season.
     * 
     * @param gaezDataList List of GAEZ data
     * @param season Season to filter by
     * @return Filtered list
     */
    private List<GaezCropData> filterBySeason(
            List<GaezCropData> gaezDataList,
            CropRecommendationRequestDto.Season season) {
        
        if (season == null || season == CropRecommendationRequestDto.Season.ALL) {
            return gaezDataList;
        }
        
        return gaezDataList.stream()
                .filter(data -> {
                    return switch (season) {
                        case KHARIF -> Boolean.TRUE.equals(data.getKharifSuitable());
                        case RABI -> Boolean.TRUE.equals(data.getRabiSuitable());
                        case ZAID -> Boolean.TRUE.equals(data.getZaidSuitable());
                        default -> true;
                    };
                })
                .collect(Collectors.toList());
    }

    /**
     * Map GaezCropData entity to DTO.
     * 
     * @param gaezData GAEZ crop data entity
     * @return GAEZ suitability DTO
     */
    private GaezCropSuitabilityDto mapToDto(GaezCropData gaezData) {
        return GaezCropSuitabilityDto.builder()
                .cropCode(gaezData.getCropCode())
                .cropName(gaezData.getCropName())
                .cropNameLocal(gaezData.getCropNameHindi())
                .overallSuitabilityScore(gaezData.getOverallSuitabilityScore())
                .suitabilityClassification(mapClassification(gaezData.getSuitabilityClassification()))
                .climateSuitabilityScore(gaezData.getClimateSuitabilityScore())
                .soilSuitabilityScore(gaezData.getSoilSuitabilityScore())
                .terrainSuitabilityScore(gaezData.getTerrainSuitabilityScore())
                .waterSuitabilityScore(gaezData.getWaterSuitabilityScore())
                .rainfedPotentialYield(gaezData.getRainfedPotentialYield())
                .irrigatedPotentialYield(gaezData.getIrrigatedPotentialYield())
                .waterRequirementsMm(gaezData.getWaterRequirementsMm())
                .growingSeasonDays(gaezData.getGrowingSeasonDays())
                .kharifSuitable(gaezData.getKharifSuitable())
                .rabiSuitable(gaezData.getRabiSuitable())
                .zaidSuitable(gaezData.getZaidSuitable())
                .climateRiskLevel(mapClimateRiskLevel(gaezData.getClimateRiskLevel()))
                .dataVersion(gaezData.getDataVersion())
                .dataResolution(gaezData.getDataResolution())
                .build();
    }

    /**
     * Map SoilHealthCardDto to SoilHealthCard entity.
     * 
     * @param dto Soil health card DTO
     * @return Soil health card entity
     */
    private SoilHealthCard mapToSoilHealthCard(SoilHealthCardDto dto) {
        if (dto == null) {
            return null;
        }
        
        return SoilHealthCard.builder()
                .cardId(dto.getCardId())
                .farmerId(dto.getFarmerId())
                .surveyNumber(dto.getSurveyNumber())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .district(dto.getDistrict())
                .state(dto.getState())
                .village(dto.getVillage())
                .sampleDate(dto.getSampleDate())
                .analysisDate(dto.getAnalysisDate())
                .ph(dto.getPh())
                .electricalConductivity(dto.getElectricalConductivity())
                .organicCarbon(dto.getOrganicCarbon())
                .nitrogenKgHa(dto.getNitrogenKgHa())
                .phosphorusKgHa(dto.getPhosphorusKgHa())
                .potassiumKgHa(dto.getPotassiumKgHa())
                .sulfurPpm(dto.getSulfurPpm())
                .zincPpm(dto.getZincPpm())
                .ironPpm(dto.getIronPpm())
                .copperPpm(dto.getCopperPpm())
                .manganesePpm(dto.getManganesePpm())
                .boronPpm(dto.getBoronPpm())
                .soilTexture(dto.getSoilTexture())
                .soilColor(dto.getSoilColor())
                .previousCrop(dto.getPreviousCrop())
                .overallStatus(mapSoilHealthStatus(dto.getOverallStatus()))
                .testingLaboratory(dto.getTestingLaboratory())
                .isOfficialCard(dto.getIsOfficialCard())
                .isActive(true)
                .build();
    }

    private GaezCropSuitabilityDto.SuitabilityClassification mapClassification(
            GaezCropData.SuitabilityClassification classification) {
        if (classification == null) return null;
        return GaezCropSuitabilityDto.SuitabilityClassification.valueOf(classification.name());
    }

    private GaezCropSuitabilityDto.ClimateRiskLevel mapClimateRiskLevel(
            GaezCropData.ClimateRiskLevel riskLevel) {
        if (riskLevel == null) return null;
        return GaezCropSuitabilityDto.ClimateRiskLevel.valueOf(riskLevel.name());
    }

    private SoilHealthCard.SoilHealthStatus mapSoilHealthStatus(
            SoilHealthCardDto.SoilHealthStatus status) {
        if (status == null) return null;
        return SoilHealthCard.SoilHealthStatus.valueOf(status.name());
    }
}








