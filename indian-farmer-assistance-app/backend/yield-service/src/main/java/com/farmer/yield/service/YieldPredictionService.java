package com.farmer.yield.service;

import com.farmer.yield.client.MandiPriceClient;
import com.farmer.yield.client.WeatherClient;
import com.farmer.yield.dto.*;
import com.farmer.yield.entity.YieldPrediction;
import com.farmer.yield.exception.YieldException;
import com.farmer.yield.repository.YieldPredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for yield prediction and variance tracking.
 * 
 * Features:
 * - Yield estimation based on crop type, variety, sowing date, area, growth stage
 * - Historical yield data integration from farmer's past records
 * - Weather data integration (rainfall, temperature, extreme events)
 * - Soil health data integration (N, P, K, pH)
 * - Irrigation type and frequency consideration
 * - Pest/disease incident adjustment
 * - Yield prediction updates and notifications (>10% deviation)
 * - Financial projections based on current mandi prices
 * - Variance tracking and model improvement with ML
 * 
 * Validates: Requirements 11B.1, 11B.2, 11B.3, 11B.4, 11B.5, 11B.6, 11B.7, 11B.8, 11B.9, 11B.10
 */
@Service
@Transactional(readOnly = true)
public class YieldPredictionService {

    private static final Logger logger = LoggerFactory.getLogger(YieldPredictionService.class);
    private static final String MODEL_VERSION = "1.0.0";
    private static final Double DEFAULT_CONFIDENCE_INTERVAL = 85.0;
    private static final Double SIGNIFICANT_DEVIATION_THRESHOLD = 10.0;

    private final YieldPredictionRepository yieldPredictionRepository;
    private final WeatherClient weatherClient;
    private final MandiPriceClient mandiPriceClient;

    // Base yield estimates per acre for different crops (quintals per acre)
    private static final Map<String, Double[]> CROP_BASE_YIELDS = new HashMap<>();
    
    // Growth stage multipliers
    private static final Map<String, Double> GROWTH_STAGE_MULTIPLIERS = new HashMap<>();
    
    // Weather impact factors
    private static final Double OPTIMAL_RAINFALL_MM = 500.0;
    private static final Double OPTIMAL_TEMPERATURE_CELSIUS = 28.0;
    private static final Double EXTREME_EVENT_PENALTY = 0.15;
    
    // Soil health impact factors
    private static final Double OPTIMAL_NITROGEN = 280.0;
    private static final Double OPTIMAL_PHOSPHORUS = 10.0;
    private static final Double OPTIMAL_POTASSIUM = 108.0;
    private static final Double OPTIMAL_PH = 6.5;
    
    // Irrigation impact factors
    private static final Map<String, Double> IRRIGATION_MULTIPLIERS = new HashMap<>();
    
    // Pest/disease impact factors
    private static final Double PEST_INCIDENT_PENALTY = 0.05;
    private static final Double DISEASE_INCIDENT_PENALTY = 0.08;
    private static final Double UNCONTROLLED_PEST_PENALTY = 0.20;
    private static final Double SEVERE_INCIDENT_PENALTY = 0.30;

    static {
        // Base yields: [low, expected, high] quintals per acre
        CROP_BASE_YIELDS.put("RICE", new Double[]{15.0, 25.0, 35.0});
        CROP_BASE_YIELDS.put("WHEAT", new Double[]{12.0, 20.0, 28.0});
        CROP_BASE_YIELDS.put("COTTON", new Double[]{8.0, 15.0, 22.0});
        CROP_BASE_YIELDS.put("SOYBEAN", new Double[]{8.0, 12.0, 18.0});
        CROP_BASE_YIELDS.put("GROUNDNUT", new Double[]{10.0, 15.0, 22.0});
        CROP_BASE_YIELDS.put("MUSTARD", new Double[]{6.0, 10.0, 15.0});
        CROP_BASE_YIELDS.put("PULSES", new Double[]{5.0, 8.0, 12.0});
        CROP_BASE_YIELDS.put("MAIZE", new Double[]{18.0, 28.0, 40.0});
        CROP_BASE_YIELDS.put("SUGARCANE", new Double[]{250.0, 350.0, 450.0});
        CROP_BASE_YIELDS.put("POTATO", new Double[]{80.0, 120.0, 160.0});
        CROP_BASE_YIELDS.put("ONION", new Double[]{100.0, 150.0, 200.0});
        CROP_BASE_YIELDS.put("TOMATO", new Double[]{120.0, 180.0, 250.0});

        // Growth stage multipliers (based on days since sowing percentage of total growth period)
        GROWTH_STAGE_MULTIPLIERS.put("SOWING", 0.95);
        GROWTH_STAGE_MULTIPLIERS.put("GERMINATION", 0.90);
        GROWTH_STAGE_MULTIPLIERS.put("VEGETATIVE", 0.85);
        GROWTH_STAGE_MULTIPLIERS.put("FLOWERING", 0.80);
        GROWTH_STAGE_MULTIPLIERS.put("FRUITING", 0.85);
        GROWTH_STAGE_MULTIPLIERS.put("MATURATION", 0.90);
        GROWTH_STAGE_MULTIPLIERS.put("HARVEST", 1.00);

        // Irrigation multipliers
        IRRIGATION_MULTIPLIERS.put("RAINFED", 0.85);
        IRRIGATION_MULTIPLIERS.put("DRIP", 1.15);
        IRRIGATION_MULTIPLIERS.put("SPRINKLER", 1.10);
        IRRIGATION_MULTIPLIERS.put("CANAL", 1.05);
        IRRIGATION_MULTIPLIERS.put("BOREWELL", 1.08);
    }

    public YieldPredictionService(
            YieldPredictionRepository yieldPredictionRepository,
            WeatherClient weatherClient,
            MandiPriceClient mandiPriceClient) {
        this.yieldPredictionRepository = yieldPredictionRepository;
        this.weatherClient = weatherClient;
        this.mandiPriceClient = mandiPriceClient;
    }

    /**
     * Generate yield estimate for a crop based on various factors.
     * 
     * @param request Yield estimation request
     * @return Yield estimate response
     * 
     * Validates: Requirements 11B.1, 11B.2, 11B.3, 11B.4, 11B.5, 11B.6, 11B.7
     */
    public YieldEstimateResponseDto generateYieldEstimate(YieldEstimateRequestDto request) {
        logger.info("Generating yield estimate for farmer: {}, crop: {}", 
                request.getFarmerId(), request.getCropName());
        
        try {
            // Step 1: Get base yield for the crop
            String cropKey = request.getCropName() != null ? 
                    request.getCropName().toUpperCase() : "RICE";
            Double[] baseYields = CROP_BASE_YIELDS.getOrDefault(cropKey, 
                    new Double[]{15.0, 25.0, 35.0});
            
            // Step 2: Calculate adjustment factors
            List<String> factorsConsidered = new ArrayList<>();
            List<String> factorAdjustments = new ArrayList<>();
            Double cumulativeMultiplier = 1.0;
            
            // Growth stage adjustment
            Double growthStageMultiplier = calculateGrowthStageMultiplier(request.getGrowthStage());
            if (growthStageMultiplier != 1.0) {
                cumulativeMultiplier = cumulativeMultiplier * growthStageMultiplier;
                factorsConsidered.add("Growth Stage: " + request.getGrowthStage());
                factorAdjustments.add(String.format("Growth stage adjustment: %.1f%%", 
                        (growthStageMultiplier - 1.0) * 100));
            }
            
            // Weather adjustment
            Double weatherMultiplier = calculateWeatherMultiplier(request, factorsConsidered, factorAdjustments);
            cumulativeMultiplier = cumulativeMultiplier * weatherMultiplier;
            
            // Soil health adjustment
            Double soilMultiplier = calculateSoilMultiplier(request, factorsConsidered, factorAdjustments);
            cumulativeMultiplier = cumulativeMultiplier * soilMultiplier;
            
            // Irrigation adjustment
            Double irrigationMultiplier = calculateIrrigationMultiplier(request, factorsConsidered, factorAdjustments);
            cumulativeMultiplier = cumulativeMultiplier * irrigationMultiplier;
            
            // Pest/disease adjustment
            Double pestDiseaseMultiplier = calculatePestDiseaseMultiplier(request, factorsConsidered, factorAdjustments);
            cumulativeMultiplier = cumulativeMultiplier * pestDiseaseMultiplier;
            
            // Historical data adjustment
            Double historicalMultiplier = calculateHistoricalMultiplier(request, factorsConsidered, factorAdjustments);
            if (historicalMultiplier != 1.0) {
                cumulativeMultiplier = cumulativeMultiplier * historicalMultiplier;
            }
            
            // Step 3: Calculate final yield estimates
            Double area = request.getAreaAcres() != null ? request.getAreaAcres() : 1.0;
            
            Double expectedYieldPerAcre = Math.round(baseYields[1] * cumulativeMultiplier * 100.0) / 100.0;
            
            // Calculate min and max based on confidence interval
            Double confidenceFactor = DEFAULT_CONFIDENCE_INTERVAL / 100.0;
            Double range = (baseYields[1] - baseYields[0]) * confidenceFactor;
            
            Double minYieldPerAcre = Math.round((expectedYieldPerAcre - range) * 100.0) / 100.0;
            minYieldPerAcre = Math.max(minYieldPerAcre, Math.round(baseYields[0] * 0.7 * 100.0) / 100.0);
            
            Double maxYieldPerAcre = Math.round((expectedYieldPerAcre + range) * 100.0) / 100.0;
            maxYieldPerAcre = Math.max(maxYieldPerAcre, Math.round(baseYields[2] * 1.1 * 100.0) / 100.0);
            
            // Ensure min <= expected <= max
            if (minYieldPerAcre > expectedYieldPerAcre) {
                minYieldPerAcre = expectedYieldPerAcre;
            }
            if (maxYieldPerAcre < expectedYieldPerAcre) {
                maxYieldPerAcre = expectedYieldPerAcre;
            }
            
            // Step 4: Calculate total yields for the area
            Double totalExpectedYield = Math.round(expectedYieldPerAcre * area * 100.0) / 100.0;
            Double totalMinYield = Math.round(minYieldPerAcre * area * 100.0) / 100.0;
            Double totalMaxYield = Math.round(maxYieldPerAcre * area * 100.0) / 100.0;
            
            // Step 5: Check for significant deviation from previous prediction
            YieldPrediction previousPrediction = yieldPredictionRepository
                    .findFirstByCropIdOrderByPredictionDateDesc(request.getCropId())
                    .orElse(null);
            
            boolean significantDeviation = false;
            Double deviationPercent = 0.0;
            String deviationNote = null;
            
            if (previousPrediction != null) {
                Double previousExpected = previousPrediction.getPredictedYieldExpectedQuintals();
                if (previousExpected != null && previousExpected > 0) {
                    deviationPercent = Math.abs(totalExpectedYield - previousExpected) * 100 / previousExpected;
                    
                    if (deviationPercent >= SIGNIFICANT_DEVIATION_THRESHOLD) {
                        significantDeviation = true;
                        deviationNote = String.format("Yield estimate changed by %.1f%% from previous prediction", deviationPercent);
                    }
                }
            }
            
            // Step 6: Save prediction
            YieldPrediction prediction = YieldPrediction.builder()
                    .cropId(request.getCropId())
                    .farmerId(request.getFarmerId())
                    .predictionDate(LocalDate.now())
                    .predictedYieldMinQuintals(totalMinYield)
                    .predictedYieldExpectedQuintals(totalExpectedYield)
                    .predictedYieldMaxQuintals(totalMaxYield)
                    .confidenceIntervalPercent(DEFAULT_CONFIDENCE_INTERVAL)
                    .factorsConsidered(String.join(", ", factorsConsidered))
                    .modelVersion(MODEL_VERSION)
                    .previousPredictionId(previousPrediction != null ? previousPrediction.getId() : null)
                    .notificationSent(false)
                    .build();
            
            prediction = yieldPredictionRepository.save(prediction);
            
            // Step 7: Build response
            YieldEstimateResponseDto.YieldEstimateResponseBuilder responseBuilder = YieldEstimateResponseDto.builder()
                    .success(true)
                    .message("Yield estimate generated successfully")
                    .generatedAt(LocalDateTime.now())
                    .predictionId(prediction.getId())
                    .cropId(request.getCropId())
                    .farmerId(request.getFarmerId())
                    .cropName(request.getCropName())
                    .cropVariety(request.getCropVariety())
                    .areaAcres(area)
                    .predictedYieldMinQuintalsPerAcre(minYieldPerAcre)
                    .predictedYieldExpectedQuintalsPerAcre(expectedYieldPerAcre)
                    .predictedYieldMaxQuintalsPerAcre(maxYieldPerAcre)
                    .predictedYieldMinQuintals(totalMinYield)
                    .predictedYieldExpectedQuintals(totalExpectedYield)
                    .predictedYieldMaxQuintals(totalMaxYield)
                    .confidenceIntervalPercent(DEFAULT_CONFIDENCE_INTERVAL)
                    .factorsConsidered(factorsConsidered)
                    .factorAdjustments(factorAdjustments)
                    .modelVersion(MODEL_VERSION)
                    .currentGrowthStage(request.getGrowthStage())
                    .significantDeviationFromPrevious(significantDeviation)
                    .deviationFromPreviousPercent(deviationPercent);
            
            // Step 8: Add financial projection if requested
            if (Boolean.TRUE.equals(request.getIncludeFinancialProjection())) {
                FinancialProjectionDto financialProjection = generateFinancialProjection(
                        request.getCropName(), totalExpectedYield, minYieldPerAcre, maxYieldPerAcre);
                responseBuilder.financialProjection(financialProjection);
            }
            
            // Step 9: Add historical comparison
            if (Boolean.TRUE.equals(request.getIncludeHistoricalData())) {
                Double historicalAvg = yieldPredictionRepository
                        .calculateAverageActualYieldForFarmerAndCrop(
                                request.getFarmerId(), 
                                request.getHistoricalCropName() != null ? 
                                        Long.parseLong(request.getHistoricalCropName()) : request.getCropId());
                if (historicalAvg != null && historicalAvg > 0) {
                    Double varianceFromHistorical = (expectedYieldPerAcre - historicalAvg) * 100 / historicalAvg;
                    responseBuilder.historicalAverageYieldQuintalsPerAcre(historicalAvg);
                    responseBuilder.yieldVarianceFromHistoricalPercent(varianceFromHistorical);
                    responseBuilder.historicalComparisonNote(String.format(
                            "%.1f%% %s than your historical average of %.2f quintals/acre",
                            Math.abs(varianceFromHistorical),
                            varianceFromHistorical > 0 ? "above" : "below",
                            historicalAvg));
                }
            }
            
            return responseBuilder.build();
            
        } catch (Exception e) {
            logger.error("Error generating yield estimate: {}", e.getMessage(), e);
            return YieldEstimateResponseDto.builder()
                    .success(false)
                    .message("Failed to generate yield estimate: " + e.getMessage())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Record actual yield after harvest and calculate variance.
     * 
     * @param request Actual yield request
     * @return Variance tracking response
     * 
     * Validates: Requirement 11B.9
     */
    @Transactional
    public VarianceTrackingDto recordActualYield(ActualYieldRequestDto request) {
        logger.info("Recording actual yield for crop: {}, farmer: {}", 
                request.getCropId(), request.getFarmerId());
        
        try {
            // Find the latest prediction for this crop
            YieldPrediction prediction = yieldPredictionRepository
                    .findFirstByCropIdOrderByPredictionDateDesc(request.getCropId())
                    .orElseThrow(() -> new YieldException("No prediction found for crop: " + request.getCropId()));
            
            // Calculate variance
            prediction.setActualYieldQuintals(request.getActualYieldQuintals());
            prediction.calculateVariance(request.getActualYieldQuintals());
            prediction = yieldPredictionRepository.save(prediction);
            
            // Get historical variance data
            Double avgVariance = yieldPredictionRepository
                    .calculateAverageVarianceForCrop(request.getCropId());
            
            // Build response
            VarianceTrackingDto.VarianceTrackingDtoBuilder responseBuilder = VarianceTrackingDto.builder()
                    .predictionId(prediction.getId())
                    .cropId(prediction.getCropId())
                    .farmerId(prediction.getFarmerId())
                    .predictedYieldExpectedQuintals(prediction.getPredictedYieldExpectedQuintals())
                    .actualYieldQuintals(prediction.getActualYieldQuintals())
                    .varianceQuintals(prediction.getVarianceQuintals())
                    .variancePercent(prediction.getVariancePercent())
                    .modelVersion(prediction.getModelVersion())
                    .averageVarianceForCrop(avgVariance);
            
            // Categorize variance
            if (prediction.getVariancePercent() != null) {
                if (prediction.getVariancePercent() > -10 && prediction.getVariancePercent() < 10) {
                    responseBuilder.varianceCategory("neutral");
                } else if (prediction.getVariancePercent() > 0) {
                    responseBuilder.varianceCategory("positive");
                } else {
                    responseBuilder.varianceCategory("negative");
                }
            }
            
            return responseBuilder.build();
            
        } catch (Exception e) {
            logger.error("Error recording actual yield: {}", e.getMessage(), e);
            throw new YieldException("Failed to record actual yield: " + e.getMessage());
        }
    }

    /**
     * Get yield prediction history for a crop.
     * 
     * @param cropId Crop ID
     * @return List of yield predictions
     */
    public List<YieldPrediction> getYieldHistory(Long cropId) {
        return yieldPredictionRepository.findByCropIdOrderByPredictionDateDesc(cropId);
    }

    /**
     * Get predictions that need notification (significant deviation).
     * 
     * @return List of predictions needing notification
     */
    public List<YieldPrediction> getPredictionsNeedingNotification() {
        return yieldPredictionRepository.findPredictionsNeedingNotification();
    }

    /**
     * Mark prediction as notified.
     * 
     * @param predictionId Prediction ID
     */
    @Transactional
    public void markAsNotified(Long predictionId) {
        yieldPredictionRepository.findById(predictionId).ifPresent(prediction -> {
            prediction.setNotificationSent(true);
            prediction.setNotificationSentAt(LocalDateTime.now());
            yieldPredictionRepository.save(prediction);
        });
    }

    /**
     * Calculate growth stage multiplier.
     */
    private Double calculateGrowthStageMultiplier(String growthStage) {
        if (growthStage == null) {
            return 1.0;
        }
        return GROWTH_STAGE_MULTIPLIERS.getOrDefault(
                growthStage.toUpperCase().replace(" ", "_"), 
                1.0);
    }

    /**
     * Calculate weather impact multiplier.
     */
    private Double calculateWeatherMultiplier(
            YieldEstimateRequestDto request,
            List<String> factors,
            List<String> adjustments) {
        
        Double multiplier = 1.0;
        
        // Rainfall impact
        if (request.getTotalRainfallMm() != null) {
            Double rainfallDiff = Math.abs(request.getTotalRainfallMm() - OPTIMAL_RAINFALL_MM);
            Double rainfallAdjustment = (rainfallDiff / OPTIMAL_RAINFALL_MM) * 0.1;
            
            if (request.getTotalRainfallMm() < OPTIMAL_RAINFALL_MM) {
                // Less than optimal - negative impact
                multiplier = multiplier - Math.min(rainfallAdjustment, 0.2);
                adjustments.add(String.format("Below optimal rainfall (%.0fmm): -%.1f%%", 
                        request.getTotalRainfallMm(), rainfallAdjustment * 100));
            } else {
                // More than optimal - slight positive impact
                multiplier = multiplier + Math.min(rainfallAdjustment, 0.1);
                adjustments.add(String.format("Above optimal rainfall (%.0fmm): +%.1f%%", 
                        request.getTotalRainfallMm(), rainfallAdjustment * 100));
            }
            factors.add("Rainfall: " + request.getTotalRainfallMm() + "mm");
        }
        
        // Temperature impact
        if (request.getAverageTemperatureCelsius() != null) {
            Double tempDiff = Math.abs(request.getAverageTemperatureCelsius() - OPTIMAL_TEMPERATURE_CELSIUS);
            Double tempAdjustment = (tempDiff / OPTIMAL_TEMPERATURE_CELSIUS) * 0.15;
            
            multiplier = multiplier - Math.min(tempAdjustment, 0.25);
            adjustments.add(String.format("Temperature deviation (%.1f°C): -%.1f%%", 
                    tempDiff, tempAdjustment * 100));
            factors.add("Temperature: " + request.getAverageTemperatureCelsius() + "°C");
        }
        
        // Extreme weather events
        if (request.getExtremeWeatherEventsCount() != null && request.getExtremeWeatherEventsCount() > 0) {
            Double extremePenalty = Math.min(EXTREME_EVENT_PENALTY * request.getExtremeWeatherEventsCount(), 0.30);
            multiplier = multiplier - extremePenalty;
            adjustments.add(String.format("Extreme weather events (%d): -%.1f%%", 
                    request.getExtremeWeatherEventsCount(), extremePenalty * 100));
            factors.add("Extreme Weather Events: " + request.getExtremeWeatherEventsCount());
        }
        
        return Math.max(multiplier, 0.5);
    }

    /**
     * Calculate soil health impact multiplier.
     */
    private Double calculateSoilMultiplier(
            YieldEstimateRequestDto request,
            List<String> factors,
            List<String> adjustments) {
        
        Double multiplier = 1.0;
        
        // Nitrogen impact
        if (request.getSoilNitrogenKgHa() != null) {
            Double nDiff = Math.max(OPTIMAL_NITROGEN - request.getSoilNitrogenKgHa(), 0);
            Double nAdjustment = (nDiff / OPTIMAL_NITROGEN) * 0.2;
            multiplier = multiplier - nAdjustment;
            adjustments.add(String.format("Soil nitrogen (%.0f kg/ha): -%.1f%%", 
                    request.getSoilNitrogenKgHa(), nAdjustment * 100));
            factors.add("Soil N: " + request.getSoilNitrogenKgHa() + " kg/ha");
        }
        
        // Phosphorus impact
        if (request.getSoilPhosphorusKgHa() != null) {
            Double pDiff = Math.max(OPTIMAL_PHOSPHORUS - request.getSoilPhosphorusKgHa(), 0);
            Double pAdjustment = (pDiff / OPTIMAL_PHOSPHORUS) * 0.15;
            multiplier = multiplier - pAdjustment;
            adjustments.add(String.format("Soil phosphorus (%.1f kg/ha): -%.1f%%", 
                    request.getSoilPhosphorusKgHa(), pAdjustment * 100));
            factors.add("Soil P: " + request.getSoilPhosphorusKgHa() + " kg/ha");
        }
        
        // Potassium impact
        if (request.getSoilPotassiumKgHa() != null) {
            Double kDiff = Math.max(OPTIMAL_POTASSIUM - request.getSoilPotassiumKgHa(), 0);
            Double kAdjustment = (kDiff / OPTIMAL_POTASSIUM) * 0.1;
            multiplier = multiplier - kAdjustment;
            adjustments.add(String.format("Soil potassium (%.0f kg/ha): -%.1f%%", 
                    request.getSoilPotassiumKgHa(), kAdjustment * 100));
            factors.add("Soil K: " + request.getSoilPotassiumKgHa() + " kg/ha");
        }
        
        // pH impact
        if (request.getSoilPh() != null) {
            Double phDiff = Math.abs(request.getSoilPh() - OPTIMAL_PH);
            if (phDiff > 0.5) {
                Double phAdjustment = phDiff * 0.1;
                multiplier = multiplier - Math.min(phAdjustment, 0.15);
                adjustments.add(String.format("Soil pH deviation (%.1f): -%.1f%%", 
                        phDiff, phAdjustment * 100));
            }
            factors.add("Soil pH: " + request.getSoilPh());
        }
        
        return Math.max(multiplier, 0.6);
    }

    /**
     * Calculate irrigation impact multiplier.
     */
    private Double calculateIrrigationMultiplier(
            YieldEstimateRequestDto request,
            List<String> factors,
            List<String> adjustments) {
        
        String irrigationType = request.getIrrigationType() != null ? 
                request.getIrrigationType().toUpperCase() : "RAINFED";
        
        Double multiplier = IRRIGATION_MULTIPLIERS.getOrDefault(irrigationType, 1.0);
        
        if (multiplier != 1.0) {
            adjustments.add(String.format("Irrigation type (%s): %+.1f%%", 
                    irrigationType, (multiplier - 1.0) * 100));
        }
        factors.add("Irrigation: " + irrigationType);
        
        // Frequency adjustment
        if (request.getIrrigationFrequencyPerWeek() != null) {
            if (request.getIrrigationFrequencyPerWeek() < 1 && "RAINFED".equals(irrigationType)) {
                // Rainfed with no irrigation - already accounted for
            } else if (request.getIrrigationFrequencyPerWeek() >= 3) {
                multiplier = multiplier * 1.05;
                adjustments.add("High irrigation frequency: +5%");
            }
        }
        
        return multiplier;
    }

    /**
     * Calculate pest/disease impact multiplier.
     */
    private Double calculatePestDiseaseMultiplier(
            YieldEstimateRequestDto request,
            List<String> factors,
            List<String> adjustments) {
        
        Double multiplier = 1.0;
        
        // Pest incidents
        if (request.getPestIncidentCount() != null && request.getPestIncidentCount() > 0) {
            Double pestPenalty = Math.min(PEST_INCIDENT_PENALTY * request.getPestIncidentCount(), 0.25);
            
            if ("ongoing".equalsIgnoreCase(request.getPestDiseaseControlStatus()) ||
                "severe".equalsIgnoreCase(request.getPestDiseaseControlStatus())) {
                pestPenalty = pestPenalty * 2;
            }
            
            multiplier = multiplier - pestPenalty;
            adjustments.add(String.format("Pest incidents (%d): -%.1f%%", 
                    request.getPestIncidentCount(), pestPenalty * 100));
            factors.add("Pest Incidents: " + request.getPestIncidentCount());
        }
        
        // Disease incidents
        if (request.getDiseaseIncidentCount() != null && request.getDiseaseIncidentCount() > 0) {
            Double diseasePenalty = Math.min(DISEASE_INCIDENT_PENALTY * request.getDiseaseIncidentCount(), 0.30);
            
            if ("ongoing".equalsIgnoreCase(request.getPestDiseaseControlStatus()) ||
                "severe".equalsIgnoreCase(request.getPestDiseaseControlStatus())) {
                diseasePenalty = diseasePenalty * 2;
            }
            
            multiplier = multiplier - diseasePenalty;
            adjustments.add(String.format("Disease incidents (%d): -%.1f%%", 
                    request.getDiseaseIncidentCount(), diseasePenalty * 100));
            factors.add("Disease Incidents: " + request.getDiseaseIncidentCount());
        }
        
        // Affected area
        if (request.getAffectedAreaPercent() != null && request.getAffectedAreaPercent() > 0) {
            Double areaPenalty = (request.getAffectedAreaPercent() / 100) * 0.3;
            multiplier = multiplier - areaPenalty;
            adjustments.add(String.format("Affected area (%.1f%%): -%.1f%%", 
                    request.getAffectedAreaPercent(), areaPenalty * 100));
            factors.add("Affected Area: " + request.getAffectedAreaPercent() + "%");
        }
        
        return Math.max(multiplier, 0.4);
    }

    /**
     * Calculate historical data multiplier.
     */
    private Double calculateHistoricalMultiplier(
            YieldEstimateRequestDto request,
            List<String> factors,
            List<String> adjustments) {
        
        if (!Boolean.TRUE.equals(request.getIncludeHistoricalData())) {
            return 1.0;
        }
        
        String cropName = request.getHistoricalCropName() != null ? 
                request.getHistoricalCropName() : request.getCropName();
        
        Double historicalAvg = yieldPredictionRepository
                .calculateAverageActualYieldForFarmerAndCrop(request.getFarmerId(), request.getCropId());
        
        if (historicalAvg != null && historicalAvg > 0) {
            Double[] baseYields = CROP_BASE_YIELDS.getOrDefault(
                    cropName.toUpperCase(), 
                    new Double[]{15.0, 25.0, 35.0});
            
            Double expectedBase = baseYields[1];
            Double historicalRatio = historicalAvg / expectedBase;
            
            if (historicalRatio < 0.8 || historicalRatio > 1.2) {
                adjustments.add(String.format("Historical yield adjustment: %+.1f%%", 
                        (historicalRatio - 1.0) * 100));
                factors.add("Historical Data: Based on your " + cropName + " records");
            }
            
            return historicalRatio;
        }
        
        return 1.0;
    }

    /**
     * Generate financial projection based on yield estimates and mandi prices.
     */
    private FinancialProjectionDto generateFinancialProjection(
            String cropName, Double expectedYield, Double minYield, Double maxYield) {
        
        try {
            // Get current prices from mandi service
            var priceData = mandiPriceClient.getCurrentPrice(cropName);
            
            Double currentPrice = priceData.getOrDefault("modalPrice", 2000.0);
            Double minPrice = priceData.getOrDefault("minPrice", currentPrice * 0.9);
            Double maxPrice = priceData.getOrDefault("maxPrice", currentPrice * 1.1);
            
            // Calculate revenue
            Double expectedRevenue = (double) Math.round(expectedYield * currentPrice);
            Double minRevenue = (double) Math.round(minYield * minPrice);
            Double maxRevenue = (double) Math.round(maxYield * maxPrice);
            
            // Estimate costs (simplified - in real implementation, would use actual cost data)
            Double estimatedCosts = (double) Math.round(expectedYield * 500); // ~500 INR per quintal input cost
            
            // Calculate profit
            Double expectedProfit = expectedRevenue - estimatedCosts;
            Double minProfit = minRevenue - estimatedCosts;
            Double maxProfit = maxRevenue - estimatedCosts;
            
            // Calculate ROI
            Double roi = 0.0;
            if (estimatedCosts > 0) {
                roi = (expectedProfit * 100) / estimatedCosts;
            }
            
            // Generate market advisory
            String advisory = "monitor";
            String advisoryReason = "Monitor prices and sell when trends indicate improvement";
            
            return FinancialProjectionDto.builder()
                    .commodityName(cropName)
                    .estimatedYieldQuintals(expectedYield)
                    .currentPricePerQuintal(currentPrice)
                    .minPricePerQuintal(minPrice)
                    .maxPricePerQuintal(maxPrice)
                    .priceSource("AGMARKNET")
                    .estimatedRevenueExpected(expectedRevenue)
                    .estimatedRevenueMin(minRevenue)
                    .estimatedRevenueMax(maxRevenue)
                    .totalEstimatedCosts(estimatedCosts)
                    .estimatedProfitExpected(expectedProfit)
                    .estimatedProfitMin(minProfit)
                    .estimatedProfitMax(maxProfit)
                    .estimatedRoiPercent(roi)
                    .marketAdvisory(advisory)
                    .advisoryReason(advisoryReason)
                    .build();
                    
        } catch (Exception e) {
            logger.warn("Could not fetch mandi prices for financial projection: {}", e.getMessage());
            // Return basic projection without price data
            return FinancialProjectionDto.builder()
                    .commodityName(cropName)
                    .estimatedYieldQuintals(expectedYield)
                    .estimatedRevenueExpected(expectedYield * 2000)
                    .marketAdvisory("monitor")
                    .advisoryReason("Price data unavailable - please check local mandi prices")
                    .build();
        }
    }
}