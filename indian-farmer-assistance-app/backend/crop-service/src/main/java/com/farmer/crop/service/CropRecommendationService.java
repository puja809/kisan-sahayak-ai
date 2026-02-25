package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.AgroEcologicalZone;
import com.farmer.crop.exception.LocationNotFoundException;
import com.farmer.crop.exception.RecommendationException;
import com.farmer.crop.repository.AgroEcologicalZoneRepository;
import com.farmer.crop.repository.GaezCropDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for generating crop recommendations.
 * 
 * This service integrates:
 * - Agro-ecological zone mapping (ICAR classification)
 * - GAEZ v4 crop suitability data
 * - Soil health card data (when available)
 * - Irrigation type considerations
 * - Seasonal filtering
 * - Market data for market-linked recommendations
 * - Climate risk assessment under rainfall deviation scenarios
 * - State-released seed varieties
 * 
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9
 */
@Service
@Transactional(readOnly = true)
public class CropRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(CropRecommendationService.class);

    private final AgroEcologicalZoneService agroEcologicalZoneService;
    private final GaezSuitabilityService gaezSuitabilityService;
    private final GaezDataImportService gaezDataImportService;
    private final AgroEcologicalZoneRepository zoneRepository;
    private final GaezCropDataRepository gaezCropDataRepository;
    private final MarketDataService marketDataService;
    private final ClimateRiskService climateRiskService;
    private final SeedVarietyService seedVarietyService;

    public CropRecommendationService(
            AgroEcologicalZoneService agroEcologicalZoneService,
            GaezSuitabilityService gaezSuitabilityService,
            GaezDataImportService gaezDataImportService,
            AgroEcologicalZoneRepository zoneRepository,
            GaezCropDataRepository gaezCropDataRepository,
            MarketDataService marketDataService,
            ClimateRiskService climateRiskService,
            SeedVarietyService seedVarietyService) {
        this.agroEcologicalZoneService = agroEcologicalZoneService;
        this.gaezSuitabilityService = gaezSuitabilityService;
        this.gaezDataImportService = gaezDataImportService;
        this.zoneRepository = zoneRepository;
        this.gaezCropDataRepository = gaezCropDataRepository;
        this.marketDataService = marketDataService;
        this.climateRiskService = climateRiskService;
        this.seedVarietyService = seedVarietyService;
    }

    /**
     * Generate crop recommendations for a farmer.
     * 
     * @param request Recommendation request with location and preferences
     * @return Recommendation response with ranked crop list
     * 
     * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9
     */
    public CropRecommendationResponseDto generateRecommendations(
            CropRecommendationRequestDto request) {
        
        logger.info("Generating crop recommendations for farmer: {}", request.getFarmerId());
        
        try {
            // Step 1: Determine agro-ecological zone
            String zoneCode = determineZoneCode(request);
            if (zoneCode == null) {
                return buildErrorResponse("Could not determine agro-ecological zone for the location");
            }

            // Step 2: Get zone information
            Optional<AgroEcologicalZone> zoneOpt = zoneRepository.findByZoneCode(zoneCode);
            String zoneName = zoneOpt.map(AgroEcologicalZone::getZoneName).orElse("Unknown");

            // Step 3: Calculate suitability scores
            List<GaezCropSuitabilityDto> suitabilityList = 
                    gaezSuitabilityService.calculateSuitabilityScores(request);

            if (suitabilityList.isEmpty()) {
                return buildErrorResponse("No suitable crops found for the location");
            }

            // Step 4: Apply filters and preferences
            List<GaezCropSuitabilityDto> filteredList = applyFilters(suitabilityList, request);

            // Step 5: Fetch market data if requested
            Map<String, MarketDataDto> marketDataMap = new HashMap<>();
            if (Boolean.TRUE.equals(request.getIncludeMarketData())) {
                List<String> cropCodes = filteredList.stream()
                        .map(GaezCropSuitabilityDto::getCropCode)
                        .collect(Collectors.toList());
                marketDataMap = marketDataService.getMarketDataForCrops(cropCodes, request.getState());
            }

            // Step 6: Analyze climate risk if requested
            Map<String, ClimateRiskDto> climateRiskMap = new HashMap<>();
            if (Boolean.TRUE.equals(request.getIncludeClimateRiskAssessment())) {
                List<String> cropCodes = filteredList.stream()
                        .map(GaezCropSuitabilityDto::getCropCode)
                        .collect(Collectors.toList());
                // Default projected rainfall deviation of -10% (below normal)
                BigDecimal projectedDeviation = new BigDecimal("-10");
                climateRiskMap = climateRiskService.analyzeClimateRiskForCrops(cropCodes, projectedDeviation);
            }

            // Step 7: Build ranked recommendations
            List<CropRecommendationResponseDto.RecommendedCropDto> recommendations = 
                    buildRecommendations(filteredList, request, marketDataMap, climateRiskMap);

            // Step 8: Build climate risk summary
            CropRecommendationResponseDto.ClimateRiskSummary climateRiskSummary = 
                    buildClimateRiskSummary(filteredList, climateRiskMap);

            // Step 9: Build market data status
            CropRecommendationResponseDto.MarketDataStatus marketDataStatus = 
                    buildMarketDataStatus(request, marketDataMap);

            // Step 10: Build response
            return CropRecommendationResponseDto.builder()
                    .success(true)
                    .generatedAt(LocalDateTime.now())
                    .farmerId(request.getFarmerId())
                    .location(formatLocation(request))
                    .agroEcologicalZone(zoneName)
                    .season(request.getSeason() != null ? request.getSeason() : 
                           CropRecommendationRequestDto.Season.ALL)
                    .recommendations(recommendations)
                    .recommendationCount(recommendations.size())
                    .soilHealthCardUsed(request.hasSoilHealthData())
                    .climateRiskSummary(climateRiskSummary)
                    .marketDataStatus(marketDataStatus)
                    .build();

        } catch (LocationNotFoundException e) {
            logger.warn("Location not found: {}", e.getMessage());
            return buildErrorResponse("Location not found: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error generating recommendations: {}", e.getMessage(), e);
            throw new RecommendationException("Failed to generate recommendations", e);
        }
    }

    /**
     * Determine the agro-ecological zone code for the request.
     * 
     * @param request Recommendation request
     * @return Zone code
     */
    private String determineZoneCode(CropRecommendationRequestDto request) {
        // If zone code is already provided, use it
        if (request.getAgroEcologicalZoneCode() != null && !request.getAgroEcologicalZoneCode().isEmpty()) {
            return request.getAgroEcologicalZoneCode();
        }

        // Otherwise, determine from location
        LocationRequestDto locationRequest = LocationRequestDto.builder()
                .district(request.getDistrict())
                .state(request.getState())
                .latitude(request.getLatitude() != null ? request.getLatitude().doubleValue() : null)
                .longitude(request.getLongitude() != null ? request.getLongitude().doubleValue() : null)
                .build();

        ZoneLookupResponseDto zoneResponse = agroEcologicalZoneService.getZoneForLocation(locationRequest);
        
        if (zoneResponse.isSuccess() && zoneResponse.getZone() != null) {
            return zoneResponse.getZone().getZoneCode();
        }

        return null;
    }

    /**
     * Apply filters and preferences to the suitability list.
     * 
     * @param suitabilityList List of crop suitability data
     * @param request Recommendation request
     * @return Filtered list
     */
    private List<GaezCropSuitabilityDto> applyFilters(
            List<GaezCropSuitabilityDto> suitabilityList,
            CropRecommendationRequestDto request) {
        
        List<GaezCropSuitabilityDto> filtered = new ArrayList<>(suitabilityList);

        // Apply minimum score filter
        if (request.getMinSuitabilityScore() != null) {
            filtered = filtered.stream()
                    .filter(c -> c.getOverallSuitabilityScore()
                            .compareTo(request.getMinSuitabilityScore()) >= 0)
                    .collect(Collectors.toList());
        }

        // Apply preferred crops filter (prioritize)
        if (request.getPreferredCrops() != null && !request.getPreferredCrops().isEmpty()) {
            // Sort preferred crops to the top
            filtered.sort((a, b) -> {
                boolean aPreferred = request.getPreferredCrops().contains(a.getCropCode());
                boolean bPreferred = request.getPreferredCrops().contains(b.getCropCode());
                if (aPreferred && !bPreferred) return -1;
                if (!aPreferred && bPreferred) return 1;
                return b.getOverallSuitabilityScore().compareTo(a.getOverallSuitabilityScore());
            });
        }

        // Apply exclude crops filter
        if (request.getExcludeCrops() != null && !request.getExcludeCrops().isEmpty()) {
            filtered = filtered.stream()
                    .filter(c -> !request.getExcludeCrops().contains(c.getCropCode()))
                    .collect(Collectors.toList());
        }

        return filtered;
    }

    /**
     * Build recommended crop DTOs from suitability data.
     * 
     * @param suitabilityList List of crop suitability data
     * @param request Recommendation request
     * @param marketDataMap Market data for crops
     * @param climateRiskMap Climate risk analysis for crops
     * @return List of recommended crops
     * 
     * Validates: Requirements 2.5, 2.6, 2.7, 2.8, 2.9
     */
    private List<CropRecommendationResponseDto.RecommendedCropDto> buildRecommendations(
            List<GaezCropSuitabilityDto> suitabilityList,
            CropRecommendationRequestDto request,
            Map<String, MarketDataDto> marketDataMap,
            Map<String, ClimateRiskDto> climateRiskMap) {
        
        List<CropRecommendationResponseDto.RecommendedCropDto> recommendations = new ArrayList<>();
        int rank = 1;

        for (GaezCropSuitabilityDto suitability : suitabilityList) {
            String cropCode = suitability.getCropCode();
            
            // Get market data for this crop
            MarketDataDto marketData = marketDataMap.get(cropCode);
            
            // Get climate risk for this crop
            ClimateRiskDto climateRisk = climateRiskMap.get(cropCode);
            
            // Calculate irrigation-adjusted score
            BigDecimal irrigationAdjustedScore = adjustForIrrigation(
                    suitability.getOverallSuitabilityScore(), request.getIrrigationType());
            
            // Calculate soil health adjusted score
            BigDecimal soilHealthAdjustedScore = suitability.getOverallSuitabilityScore();
            List<String> soilHealthRecommendations = new ArrayList<>();
            if (request.hasSoilHealthData()) {
                soilHealthAdjustedScore = calculateSoilHealthAdjustedScore(
                        suitability, request.getSoilHealthCard(), soilHealthRecommendations);
            }
            
            // Calculate climate-adjusted score
            BigDecimal climateAdjustedScore = climateRiskService.calculateClimateAdjustedScore(
                    irrigationAdjustedScore, climateRisk);
            
            // Calculate market-adjusted score
            BigDecimal marketAdjustedScore = marketDataService.calculateMarketAdjustedScore(
                    climateAdjustedScore, marketData, Boolean.TRUE.equals(request.getIncludeMarketData()));
            
            // Calculate expected yield per acre (convert from kg/ha to quintals/acre)
            BigDecimal expectedYieldPerAcre = calculateYieldPerAcre(suitability);
            
            // Calculate water requirement per acre (convert from mm to liters)
            BigDecimal waterRequirementPerAcre = calculateWaterPerAcre(suitability);
            
            // Estimate input cost and net profit
            BigDecimal estimatedInputCost = estimateInputCost(suitability);
            
            // Calculate expected revenue with market data
            BigDecimal expectedRevenuePerAcre = marketDataService.calculateExpectedRevenue(
                    expectedYieldPerAcre, marketData);
            if (expectedRevenuePerAcre == null) {
                expectedRevenuePerAcre = calculateRevenue(expectedYieldPerAcre, cropCode);
            }
            
            // Calculate net profit (handle null case)
            BigDecimal estimatedNetProfit = null;
            if (expectedRevenuePerAcre != null && estimatedInputCost != null) {
                estimatedNetProfit = expectedRevenuePerAcre.subtract(estimatedInputCost)
                        .setScale(0, RoundingMode.HALF_UP);
            }
            
            // Calculate potential yield gap
            BigDecimal potentialYieldGap = calculateYieldGap(suitability);
            
            // Determine season suitability
            boolean seasonSuitable = isSeasonSuitable(suitability, request.getSeason());
            
            // Get state-released seed varieties
            List<String> recommendedVarieties = seedVarietyService.getStateRecommendations(
                    cropCode, request.getState());
            if (recommendedVarieties.isEmpty()) {
                recommendedVarieties = getDefaultVarieties(cropCode);
            }
            
            // Get climate risk level (convert to GaezCropSuitabilityDto.ClimateRiskLevel)
            GaezCropSuitabilityDto.ClimateRiskLevel climateRiskLevel = 
                    mapToGaezClimateRiskLevel(climateRisk != null ? 
                            climateRisk.getRiskLevel() : null, 
                            suitability.getClimateRiskLevel());
            
            // Identify risk factors
            List<String> riskFactors = identifyRiskFactors(suitability, climateRisk);
            
            // Generate market recommendations
            List<String> marketRecommendations = new ArrayList<>();
            if (marketData != null && marketData.getRecommendation() != null) {
                switch (marketData.getRecommendation()) {
                    case SELL_NOW -> marketRecommendations.add("Good time to sell - prices above average");
                    case HOLD -> marketRecommendations.add("Consider holding for better prices");
                    case MONITOR -> marketRecommendations.add("Monitor prices closely before selling");
                    case CONSIDER_STORAGE -> marketRecommendations.add("Consider storage if facilities available");
                }
            }

            CropRecommendationResponseDto.RecommendedCropDto.RecommendedCropDtoBuilder builder = 
                    CropRecommendationResponseDto.RecommendedCropDto.builder()
                            .rank(rank++)
                            .gaezSuitability(suitability)
                            .overallSuitabilityScore(marketAdjustedScore)
                            .irrigationAdjustedScore(irrigationAdjustedScore)
                            .soilHealthAdjustedScore(soilHealthAdjustedScore)
                            .expectedYieldPerAcre(expectedYieldPerAcre)
                            .expectedRevenuePerAcre(expectedRevenuePerAcre)
                            .waterRequirementPerAcre(waterRequirementPerAcre)
                            .growingDurationDays(suitability.getGrowingSeasonDays())
                            .climateRiskLevel(climateRiskLevel)
                            .seasonSuitable(seasonSuitable)
                            .recommendedVarieties(recommendedVarieties)
                            .soilHealthRecommendations(soilHealthRecommendations)
                            .potentialYieldGap(potentialYieldGap)
                            .estimatedInputCost(estimatedInputCost)
                            .estimatedNetProfit(estimatedNetProfit)
                            .riskFactors(riskFactors)
                            .notes(generateNotes(suitability, request, climateRisk));

            recommendations.add(builder.build());
        }

        return recommendations;
    }

    /**
     * Adjust score for irrigation type.
     */
    private BigDecimal adjustForIrrigation(BigDecimal baseScore, 
            CropRecommendationRequestDto.IrrigationType irrigationType) {
        if (irrigationType == null) return baseScore;
        
        BigDecimal adjustment = switch (irrigationType) {
            case RAINFED -> new BigDecimal("-5");
            case DRIP, SPRINKLER -> new BigDecimal("3");
            case CANAL, BOREWELL -> new BigDecimal("2");
            case MIXED -> BigDecimal.ZERO;
        };
        
        return baseScore.add(adjustment).max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * Calculate soil health adjusted score.
     */
    private BigDecimal calculateSoilHealthAdjustedScore(
            GaezCropSuitabilityDto suitability,
            SoilHealthCardDto soilHealthCard,
            List<String> recommendations) {
        
        BigDecimal adjustment = BigDecimal.ZERO;
        
        if (soilHealthCard == null) return suitability.getOverallSuitabilityScore();
        
        // Check nutrient deficiencies
        if (soilHealthCard.getNitrogenKgHa() != null && 
                soilHealthCard.getNitrogenKgHa().compareTo(new BigDecimal("280")) < 0) {
            adjustment = adjustment.subtract(new BigDecimal("3"));
            recommendations.add("Low nitrogen - apply nitrogen fertilizer");
        }
        
        if (soilHealthCard.getPhosphorusKgHa() != null && 
                soilHealthCard.getPhosphorusKgHa().compareTo(new BigDecimal("10")) < 0) {
            adjustment = adjustment.subtract(new BigDecimal("3"));
            recommendations.add("Low phosphorus - apply phosphorus fertilizer");
        }
        
        if (soilHealthCard.getPotassiumKgHa() != null && 
                soilHealthCard.getPotassiumKgHa().compareTo(new BigDecimal("108")) < 0) {
            adjustment = adjustment.subtract(new BigDecimal("2"));
            recommendations.add("Low potassium - apply potassium fertilizer");
        }
        
        if (soilHealthCard.getZincPpm() != null && 
                soilHealthCard.getZincPpm().compareTo(new BigDecimal("0.6")) < 0) {
            adjustment = adjustment.subtract(new BigDecimal("2"));
            recommendations.add("Zinc deficiency - apply zinc sulfate");
        }
        
        return suitability.getOverallSuitabilityScore().add(adjustment)
                .max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * Calculate yield per acre (convert from kg/ha to quintals/acre).
     * 1 hectare = 2.47 acres, 1 quintal = 100 kg
     */
    private BigDecimal calculateYieldPerAcre(GaezCropSuitabilityDto suitability) {
        if (suitability.getExpectedYieldExpected() == null) return null;
        
        // Convert kg/ha to quintals/acre
        BigDecimal yieldKgPerAcre = suitability.getExpectedYieldExpected()
                .divide(new BigDecimal("2.47"), 2, RoundingMode.HALF_UP);
        
        return yieldKgPerAcre.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate water requirement per acre (convert from mm to liters).
     * 1 mm = 10000 liters per hectare = 4047 liters per acre
     */
    private BigDecimal calculateWaterPerAcre(GaezCropSuitabilityDto suitability) {
        if (suitability.getWaterRequirementsMm() == null) return null;
        
        return suitability.getWaterRequirementsMm()
                .multiply(new BigDecimal("4047"))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Estimate input cost per acre (INR).
     */
    private BigDecimal estimateInputCost(GaezCropSuitabilityDto suitability) {
        // Base cost varies by crop type
        BigDecimal baseCost = switch (suitability.getCropCode()) {
            case "RICE" -> new BigDecimal("25000");
            case "WHEAT" -> new BigDecimal("20000");
            case "COTTON" -> new BigDecimal("35000");
            case "SUGARCANE" -> new BigDecimal("45000");
            case "SOYBEAN" -> new BigDecimal("15000");
            case "GROUNDNUT" -> new BigDecimal("18000");
            case "PULSES" -> new BigDecimal("12000");
            case "MUSTARD" -> new BigDecimal("15000");
            case "MAIZE" -> new BigDecimal("18000");
            case "POTATO" -> new BigDecimal("30000");
            default -> new BigDecimal("20000");
        };
        
        return baseCost.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Estimate net profit per acre (INR).
     */
    private BigDecimal estimateNetProfit(GaezCropSuitabilityDto suitability, BigDecimal yieldPerAcre) {
        if (yieldPerAcre == null) return null;
        
        BigDecimal estimatedRevenue = calculateRevenue(yieldPerAcre, suitability.getCropCode());
        BigDecimal inputCost = estimateInputCost(suitability);
        
        return estimatedRevenue.subtract(inputCost).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Calculate revenue from yield (INR per acre).
     */
    private BigDecimal calculateRevenue(BigDecimal yieldPerAcre, String cropCode) {
        if (yieldPerAcre == null) return null;
        
        // Approximate prices per quintal (INR)
        BigDecimal pricePerQuintal = switch (cropCode) {
            case "RICE" -> new BigDecimal("2200");
            case "WHEAT" -> new BigDecimal("2500");
            case "COTTON" -> new BigDecimal("6000");
            case "SUGARCANE" -> new BigDecimal("350");  // per quintal
            case "SOYBEAN" -> new BigDecimal("5000");
            case "GROUNDNUT" -> new BigDecimal("6200");
            case "PULSES" -> new BigDecimal("7000");
            case "MUSTARD" -> new BigDecimal("5500");
            case "MAIZE" -> new BigDecimal("2100");
            case "POTATO" -> new BigDecimal("1500");
            default -> new BigDecimal("2000");
        };
        
        return yieldPerAcre.multiply(pricePerQuintal).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Calculate potential yield gap.
     */
    private BigDecimal calculateYieldGap(GaezCropSuitabilityDto suitability) {
        if (suitability.getIrrigatedPotentialYield() == null || 
                suitability.getExpectedYieldExpected() == null) {
            return null;
        }
        
        BigDecimal potential = suitability.getIrrigatedPotentialYield()
                .divide(new BigDecimal("2.47"), 2, RoundingMode.HALF_UP)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        
        BigDecimal expected = suitability.getExpectedYieldExpected();
        
        if (potential.compareTo(BigDecimal.ZERO) == 0) return null;
        
        return potential.subtract(expected).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Check if crop is suitable for the requested season.
     */
    private boolean isSeasonSuitable(GaezCropSuitabilityDto suitability, 
            CropRecommendationRequestDto.Season season) {
        if (season == null || season == CropRecommendationRequestDto.Season.ALL) {
            return true;
        }
        
        return switch (season) {
            case KHARIF -> Boolean.TRUE.equals(suitability.getKharifSuitable());
            case RABI -> Boolean.TRUE.equals(suitability.getRabiSuitable());
            case ZAID -> Boolean.TRUE.equals(suitability.getZaidSuitable());
            default -> true;
        };
    }

    /**
     * Get recommended varieties for a crop.
     */
    private List<String> getRecommendedVarieties(String cropCode) {
        return switch (cropCode) {
            case "RICE" -> Arrays.asList("PB-1509", "HD-2967", "WHD-1", "Basmati varieties");
            case "WHEAT" -> Arrays.asList("HD-2967", "HD-3086", "DBW-187", "WH-1105");
            case "COTTON" -> Arrays.asList("Bt Cotton varieties", "Desi cotton");
            case "SOYBEAN" -> Arrays.asList("JS-335", "PS-1347", "MACS-450");
            case "GROUNDNUT" -> Arrays.asList("TAG-24", "JL-24", "ICGS-44");
            case "MUSTARD" -> Arrays.asList("Varuna", "RH-749", "Pusa Bold");
            case "PULSES" -> Arrays.asList("Tur (Arhar)", "Moong", "Urad", "Chana");
            default -> Collections.emptyList();
        };
    }

    /**
     * Identify risk factors for a crop.
     * 
     * @param suitability GAEZ crop suitability data
     * @param climateRisk Climate risk analysis
     * @return List of risk factors
     */
    private List<String> identifyRiskFactors(GaezCropSuitabilityDto suitability, ClimateRiskDto climateRisk) {
        List<String> risks = new ArrayList<>();
        
        // Climate risk factors
        if (climateRisk != null) {
            if (climateRisk.getKeyRisks() != null) {
                risks.addAll(climateRisk.getKeyRisks());
            }
            
            if (climateRisk.getDroughtRisk() == ClimateRiskDto.DroughtRiskLevel.HIGH ||
                    climateRisk.getDroughtRisk() == ClimateRiskDto.DroughtRiskLevel.SEVERE) {
                risks.add("High drought risk - consider drought-tolerant varieties");
            }
            
            if (climateRisk.getFloodRisk() == ClimateRiskDto.FloodRiskLevel.HIGH ||
                    climateRisk.getFloodRisk() == ClimateRiskDto.FloodRiskLevel.SEVERE) {
                risks.add("High flood risk - ensure proper drainage");
            }
        } else {
            // Fallback to GAEZ climate risk level
            if (suitability.getClimateRiskLevel() == GaezCropSuitabilityDto.ClimateRiskLevel.HIGH) {
                risks.add("High climate risk - consider climate-smart practices");
            }
        }
        
        // Water stress risk
        if (suitability.getWaterSuitabilityScore() != null && 
                suitability.getWaterSuitabilityScore().compareTo(new BigDecimal("50")) < 0) {
            risks.add("Water stress risk - ensure adequate irrigation");
        }
        
        // Soil quality concerns
        if (suitability.getSoilSuitabilityScore() != null && 
                suitability.getSoilSuitabilityScore().compareTo(new BigDecimal("50")) < 0) {
            risks.add("Soil quality concerns - consider soil amendments");
        }
        
        return risks;
    }

    /**
     * Generate notes for the recommendation.
     * 
     * @param suitability GAEZ crop suitability data
     * @param request Recommendation request
     * @param climateRisk Climate risk analysis
     * @return Notes for the recommendation
     */
    private String generateNotes(GaezCropSuitabilityDto suitability, 
            CropRecommendationRequestDto request, ClimateRiskDto climateRisk) {
        StringBuilder notes = new StringBuilder();
        
        notes.append("Suitable for ").append(suitability.getCropName());
        
        if (suitability.getGrowingSeasonDays() != null) {
            notes.append(". Growing period: ").append(suitability.getGrowingSeasonDays()).append(" days");
        }
        
        // Climate risk notes
        if (climateRisk != null) {
            if (climateRisk.getRiskLevel() == ClimateRiskDto.ClimateRiskLevel.HIGH ||
                    climateRisk.getRiskLevel() == ClimateRiskDto.ClimateRiskLevel.VERY_HIGH) {
                notes.append(". High climate risk - review mitigation strategies.");
            }
            
            if (climateRisk.getRainfallScenario() != null) {
                ClimateRiskDto.RainfallDeviationScenario.ScenarioType scenario = 
                        climateRisk.getRainfallScenario().getScenarioType();
                if (scenario == ClimateRiskDto.RainfallDeviationScenario.ScenarioType.DEFICIT) {
                    notes.append(". Rainfall deficit expected - consider drought-tolerant varieties.");
                } else if (scenario == ClimateRiskDto.RainfallDeviationScenario.ScenarioType.EXCESS) {
                    notes.append(". Excess rainfall expected - ensure proper drainage.");
                }
            }
        } else if (suitability.getClimateRiskLevel() == GaezCropSuitabilityDto.ClimateRiskLevel.HIGH) {
            notes.append(". Monitor weather conditions closely.");
        }
        
        return notes.toString();
    }

    /**
     * Build climate risk summary.
     * 
     * @param suitabilityList List of crop suitability data
     * @param climateRiskMap Climate risk analysis for crops
     * @return Climate risk summary
     */
    private CropRecommendationResponseDto.ClimateRiskSummary buildClimateRiskSummary(
            List<GaezCropSuitabilityDto> suitabilityList,
            Map<String, ClimateRiskDto> climateRiskMap) {
        
        int highRisk = 0, mediumRisk = 0, lowRisk = 0;
        Set<String> keyRisks = new HashSet<>();
        Set<String> mitigationSet = new HashSet<>();
        
        for (GaezCropSuitabilityDto suitability : suitabilityList) {
            ClimateRiskDto climateRisk = climateRiskMap.get(suitability.getCropCode());
            
            ClimateRiskDto.ClimateRiskLevel riskLevel;
            if (climateRisk != null) {
                riskLevel = climateRisk.getRiskLevel();
                
                // Collect key risks from climate risk analysis
                if (climateRisk.getKeyRisks() != null) {
                    keyRisks.addAll(climateRisk.getKeyRisks());
                }
                
                // Collect mitigation strategies
                if (climateRisk.getMitigationStrategies() != null) {
                    mitigationSet.addAll(climateRisk.getMitigationStrategies());
                }
            } else {
                // Fallback to GAEZ climate risk level
                riskLevel = mapToClimateRiskLevel(suitability.getClimateRiskLevel());
            }
            
            if (riskLevel == null) continue;
            
            switch (riskLevel) {
                case HIGH, VERY_HIGH -> highRisk++;
                case MEDIUM -> mediumRisk++;
                case LOW -> lowRisk++;
            }
        }
        
        ClimateRiskDto.ClimateRiskLevel overallRiskLevel = highRisk > 0 ? 
                ClimateRiskDto.ClimateRiskLevel.HIGH : 
                (mediumRisk > 0 ? ClimateRiskDto.ClimateRiskLevel.MEDIUM : 
                 ClimateRiskDto.ClimateRiskLevel.LOW);
        
        // Convert to GAEZ climate risk level
        GaezCropSuitabilityDto.ClimateRiskLevel overallRisk = mapToGaezClimateRiskLevel(
                overallRiskLevel, GaezCropSuitabilityDto.ClimateRiskLevel.LOW);
        
        List<String> mitigationStrategies = new ArrayList<>(mitigationSet);
        if (mitigationStrategies.isEmpty() && highRisk > 0) {
            mitigationStrategies.add("Consider climate-resilient varieties");
            mitigationStrategies.add("Implement water conservation practices");
            mitigationStrategies.add("Monitor weather forecasts regularly");
        }
        
        return CropRecommendationResponseDto.ClimateRiskSummary.builder()
                .overallRiskLevel(overallRisk)
                .highRiskCropCount(highRisk)
                .mediumRiskCropCount(mediumRisk)
                .lowRiskCropCount(lowRisk)
                .keyClimateRisks(new ArrayList<>(keyRisks))
                .mitigationStrategies(mitigationStrategies)
                .build();
    }

    /**
     * Build market data status.
     * 
     * @param request Recommendation request
     * @param marketDataMap Market data for crops
     * @return Market data status
     */
    private CropRecommendationResponseDto.MarketDataStatus buildMarketDataStatus(
            CropRecommendationRequestDto request,
            Map<String, MarketDataDto> marketDataMap) {
        
        if (!Boolean.TRUE.equals(request.getIncludeMarketData())) {
            return CropRecommendationResponseDto.MarketDataStatus.builder()
                    .integrated(false)
                    .cropsWithMarketData(0)
                    .build();
        }
        
        if (marketDataMap == null || marketDataMap.isEmpty()) {
            return CropRecommendationResponseDto.MarketDataStatus.builder()
                    .integrated(true)
                    .cropsWithMarketData(0)
                    .priceTrendSummary("Market data unavailable")
                    .marketRecommendations(Collections.emptyList())
                    .build();
        }
        
        // Calculate overall price trend
        long upTrend = marketDataMap.values().stream()
                .filter(m -> m.getTrend() == MarketDataDto.PriceTrend.UP)
                .count();
        long downTrend = marketDataMap.values().stream()
                .filter(m -> m.getTrend() == MarketDataDto.PriceTrend.DOWN)
                .count();
        
        String priceTrendSummary;
        if (upTrend > downTrend) {
            priceTrendSummary = String.format("Prices trending UP for %d crops", upTrend);
        } else if (downTrend > upTrend) {
            priceTrendSummary = String.format("Prices trending DOWN for %d crops", downTrend);
        } else {
            priceTrendSummary = "Prices stable across most crops";
        }
        
        // Generate market recommendations
        List<String> marketRecommendations = marketDataService.getMarketRecommendations(marketDataMap);
        if (marketRecommendations.isEmpty()) {
            marketRecommendations = Arrays.asList(
                    "Consider selling at optimal maturity",
                    "Monitor price trends before harvest");
        }
        
        return CropRecommendationResponseDto.MarketDataStatus.builder()
                .integrated(true)
                .cropsWithMarketData(marketDataMap.size())
                .priceTrendSummary(priceTrendSummary)
                .marketRecommendations(marketRecommendations)
                .build();
    }

    /**
     * Build error response.
     */
    private CropRecommendationResponseDto buildErrorResponse(String message) {
        return CropRecommendationResponseDto.builder()
                .success(false)
                .errorMessage(message)
                .generatedAt(LocalDateTime.now())
                .recommendations(Collections.emptyList())
                .recommendationCount(0)
                .build();
    }

    /**
     * Format location string.
     */
    private String formatLocation(CropRecommendationRequestDto request) {
        if (request.getDistrict() != null && request.getState() != null) {
            return request.getDistrict() + ", " + request.getState();
        }
        if (request.getLatitude() != null && request.getLongitude() != null) {
            return String.format("%.4f, %.4f", request.getLatitude(), request.getLongitude());
        }
        return "Unknown";
    }

    /**
     * Map GAEZ climate risk level to ClimateRiskDto level.
     * 
     * @param gaezRiskLevel GAEZ climate risk level
     * @return ClimateRiskDto climate risk level
     */
    private ClimateRiskDto.ClimateRiskLevel mapToClimateRiskLevel(
            GaezCropSuitabilityDto.ClimateRiskLevel gaezRiskLevel) {
        if (gaezRiskLevel == null) return null;
        return switch (gaezRiskLevel) {
            case LOW -> ClimateRiskDto.ClimateRiskLevel.LOW;
            case MEDIUM -> ClimateRiskDto.ClimateRiskLevel.MEDIUM;
            case HIGH -> ClimateRiskDto.ClimateRiskLevel.HIGH;
        };
    }

    /**
     * Map ClimateRiskDto level to GAEZ climate risk level.
     * 
     * @param climateRiskLevel ClimateRiskDto climate risk level
     * @param fallbackGaezLevel Fallback GAEZ climate risk level
     * @return GAEZ climate risk level
     */
    private GaezCropSuitabilityDto.ClimateRiskLevel mapToGaezClimateRiskLevel(
            ClimateRiskDto.ClimateRiskLevel climateRiskLevel,
            GaezCropSuitabilityDto.ClimateRiskLevel fallbackGaezLevel) {
        if (climateRiskLevel != null) {
            return switch (climateRiskLevel) {
                case LOW -> GaezCropSuitabilityDto.ClimateRiskLevel.LOW;
                case MEDIUM -> GaezCropSuitabilityDto.ClimateRiskLevel.MEDIUM;
                case HIGH, VERY_HIGH -> GaezCropSuitabilityDto.ClimateRiskLevel.HIGH;
            };
        }
        return fallbackGaezLevel;
    }

    /**
     * Get default varieties for a crop when state-specific varieties are not available.
     * 
     * @param cropCode Crop code
     * @return List of default variety names
     */
    private List<String> getDefaultVarieties(String cropCode) {
        return switch (cropCode) {
            case "RICE" -> Arrays.asList("PB-1509", "HD-2967", "WHD-1", "Basmati varieties");
            case "WHEAT" -> Arrays.asList("HD-2967", "HD-3086", "DBW-187", "WH-1105");
            case "COTTON" -> Arrays.asList("Bt Cotton varieties", "Desi cotton");
            case "SOYBEAN" -> Arrays.asList("JS-335", "PS-1347", "MACS-450");
            case "GROUNDNUT" -> Arrays.asList("TAG-24", "JL-24", "ICGS-44");
            case "MUSTARD" -> Arrays.asList("Varuna", "RH-749", "Pusa Bold");
            case "PULSES" -> Arrays.asList("Tur (Arhar)", "Moong", "Urad", "Chana");
            case "MAIZE" -> Arrays.asList("PMH-1", "Hybrid varieties", "Local composites");
            case "SUGARCANE" -> Arrays.asList("Co-86032", "Co-99004", "Co-92005");
            default -> Collections.emptyList();
        };
    }
}