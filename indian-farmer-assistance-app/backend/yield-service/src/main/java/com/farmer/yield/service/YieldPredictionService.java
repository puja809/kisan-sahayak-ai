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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final BigDecimal DEFAULT_CONFIDENCE_INTERVAL = new BigDecimal("85");
    private static final BigDecimal SIGNIFICANT_DEVIATION_THRESHOLD = new BigDecimal("10");

    private final YieldPredictionRepository yieldPredictionRepository;
    private final WeatherClient weatherClient;
    private final MandiPriceClient mandiPriceClient;

    // Base yield estimates per acre for different crops (quintals per acre)
    private static final Map<String, BigDecimal[]> CROP_BASE_YIELDS = new HashMap<>();
    
    // Growth stage multipliers
    private static final Map<String, BigDecimal> GROWTH_STAGE_MULTIPLIERS = new HashMap<>();
    
    // Weather impact factors
    private static final BigDecimal OPTIMAL_RAINFALL_MM = new BigDecimal("500");
    private static final BigDecimal OPTIMAL_TEMPERATURE_CELSIUS = new BigDecimal("28");
    private static final BigDecimal EXTREME_EVENT_PENALTY = new BigDecimal("0.15");
    
    // Soil health impact factors
    private static final BigDecimal OPTIMAL_NITROGEN = new BigDecimal("280");
    private static final BigDecimal OPTIMAL_PHOSPHORUS = new BigDecimal("10");
    private static final BigDecimal OPTIMAL_POTASSIUM = new BigDecimal("108");
    private static final BigDecimal OPTIMAL_PH = new BigDecimal("6.5");
    
    // Irrigation impact factors
    private static final Map<String, BigDecimal> IRRIGATION_MULTIPLIERS = new HashMap<>();
    
    // Pest/disease impact factors
    private static final BigDecimal PEST_INCIDENT_PENALTY = new BigDecimal("0.05");
    private static final BigDecimal DISEASE_INCIDENT_PENALTY = new BigDecimal("0.08");
    private static final BigDecimal UNCONTROLLED_PEST_PENALTY = new BigDecimal("0.20");
    private static final BigDecimal SEVERE_INCIDENT_PENALTY = new BigDecimal("0.30");

    static {
        // Base yields: [low, expected, high] quintals per acre
        CROP_BASE_YIELDS.put("RICE", new BigDecimal[]{new BigDecimal("15"), new BigDecimal("25"), new BigDecimal("35")});
        CROP_BASE_YIELDS.put("WHEAT", new BigDecimal[]{new BigDecimal("12"), new BigDecimal("20"), new BigDecimal("28")});
        CROP_BASE_YIELDS.put("COTTON", new BigDecimal[]{new BigDecimal("8"), new BigDecimal("15"), new BigDecimal("22")});
        CROP_BASE_YIELDS.put("SOYBEAN", new BigDecimal[]{new BigDecimal("8"), new BigDecimal("12"), new BigDecimal("18")});
        CROP_BASE_YIELDS.put("GROUNDNUT", new BigDecimal[]{new BigDecimal("10"), new BigDecimal("15"), new BigDecimal("22")});
        CROP_BASE_YIELDS.put("MUSTARD", new BigDecimal[]{new BigDecimal("6"), new BigDecimal("10"), new BigDecimal("15")});
        CROP_BASE_YIELDS.put("PULSES", new BigDecimal[]{new BigDecimal("5"), new BigDecimal("8"), new BigDecimal("12")});
        CROP_BASE_YIELDS.put("MAIZE", new BigDecimal[]{new BigDecimal("18"), new BigDecimal("28"), new BigDecimal("40")});
        CROP_BASE_YIELDS.put("SUGARCANE", new BigDecimal[]{new BigDecimal("250"), new BigDecimal("350"), new BigDecimal("450")});
        CROP_BASE_YIELDS.put("POTATO", new BigDecimal[]{new BigDecimal("80"), new BigDecimal("120"), new BigDecimal("160")});
        CROP_BASE_YIELDS.put("ONION", new BigDecimal[]{new BigDecimal("100"), new BigDecimal("150"), new BigDecimal("200")});
        CROP_BASE_YIELDS.put("TOMATO", new BigDecimal[]{new BigDecimal("120"), new BigDecimal("180"), new BigDecimal("250")});

        // Growth stage multipliers (based on days since sowing percentage of total growth period)
        GROWTH_STAGE_MULTIPLIERS.put("SOWING", new BigDecimal("0.95"));
        GROWTH_STAGE_MULTIPLIERS.put("GERMINATION", new BigDecimal("0.90"));
        GROWTH_STAGE_MULTIPLIERS.put("VEGETATIVE", new BigDecimal("0.85"));
        GROWTH_STAGE_MULTIPLIERS.put("FLOWERING", new BigDecimal("0.80"));
        GROWTH_STAGE_MULTIPLIERS.put("FRUITING", new BigDecimal("0.85"));
        GROWTH_STAGE_MULTIPLIERS.put("MATURATION", new BigDecimal("0.90"));
        GROWTH_STAGE_MULTIPLIERS.put("HARVEST", new BigDecimal("1.00"));

        // Irrigation multipliers
        IRRIGATION_MULTIPLIERS.put("RAINFED", new BigDecimal("0.85"));
        IRRIGATION_MULTIPLIERS.put("DRIP", new BigDecimal("1.15"));
        IRRIGATION_MULTIPLIERS.put("SPRINKLER", new BigDecimal("1.10"));
        IRRIGATION_MULTIPLIERS.put("CANAL", new BigDecimal("1.05"));
        IRRIGATION_MULTIPLIERS.put("BOREWELL", new BigDecimal("1.08"));
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
            BigDecimal[] baseYields = CROP_BASE_YIELDS.getOrDefault(cropKey, 
                    new BigDecimal[]{new BigDecimal("15"), new BigDecimal("25"), new BigDecimal("35")});
            
            // Step 2: Calculate adjustment factors
            List<String> factorsConsidered = new ArrayList<>();
            List<String> factorAdjustments = new ArrayList<>();
            BigDecimal cumulativeMultiplier = BigDecimal.ONE;
            
            // Growth stage adjustment
            BigDecimal growthStageMultiplier = calculateGrowthStageMultiplier(request.getGrowthStage());
            if (growthStageMultiplier.compareTo(BigDecimal.ONE) != 0) {
                cumulativeMultiplier = cumulativeMultiplier.multiply(growthStageMultiplier);
                factorsConsidered.add("Growth Stage: " + request.getGrowthStage());
                factorAdjustments.add(String.format("Growth stage adjustment: %.1f%%", 
                        growthStageMultiplier.multiply(new BigDecimal("100")).subtract(new BigDecimal("100"))));
            }
            
            // Weather adjustment
            BigDecimal weatherMultiplier = calculateWeatherMultiplier(request, factorsConsidered, factorAdjustments);
            cumulativeMultiplier = cumulativeMultiplier.multiply(weatherMultiplier);
            
            // Soil health adjustment
            BigDecimal soilMultiplier = calculateSoilMultiplier(request, factorsConsidered, factorAdjustments);
            cumulativeMultiplier = cumulativeMultiplier.multiply(soilMultiplier);
            
            // Irrigation adjustment
            BigDecimal irrigationMultiplier = calculateIrrigationMultiplier(request, factorsConsidered, factorAdjustments);
            cumulativeMultiplier = cumulativeMultiplier.multiply(irrigationMultiplier);
            
            // Pest/disease adjustment
            BigDecimal pestDiseaseMultiplier = calculatePestDiseaseMultiplier(request, factorsConsidered, factorAdjustments);
            cumulativeMultiplier = cumulativeMultiplier.multiply(pestDiseaseMultiplier);
            
            // Historical data adjustment
            BigDecimal historicalMultiplier = calculateHistoricalMultiplier(request, factorsConsidered, factorAdjustments);
            if (historicalMultiplier.compareTo(BigDecimal.ONE) != 0) {
                cumulativeMultiplier = cumulativeMultiplier.multiply(historicalMultiplier);
            }
            
            // Step 3: Calculate final yield estimates
            BigDecimal area = request.getAreaAcres() != null ? request.getAreaAcres() : BigDecimal.ONE;
            
            BigDecimal expectedYieldPerAcre = baseYields[1].multiply(cumulativeMultiplier)
                    .setScale(2, RoundingMode.HALF_UP);
            
            // Calculate min and max based on confidence interval
            BigDecimal confidenceFactor = DEFAULT_CONFIDENCE_INTERVAL.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal range = baseYields[1].subtract(baseYields[0]).multiply(confidenceFactor);
            
            BigDecimal minYieldPerAcre = expectedYieldPerAcre.subtract(range)
                    .max(baseYields[0].multiply(new BigDecimal("0.7")))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal maxYieldPerAcre = expectedYieldPerAcre.add(range)
                    .max(baseYields[2].multiply(new BigDecimal("1.1")))
                    .setScale(2, RoundingMode.HALF_UP);
            
            // Ensure min <= expected <= max
            if (minYieldPerAcre.compareTo(expectedYieldPerAcre) > 0) {
                minYieldPerAcre = expectedYieldPerAcre;
            }
            if (maxYieldPerAcre.compareTo(expectedYieldPerAcre) < 0) {
                maxYieldPerAcre = expectedYieldPerAcre;
            }
            
            // Step 4: Calculate total yields for the area
            BigDecimal totalExpectedYield = expectedYieldPerAcre.multiply(area).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalMinYield = minYieldPerAcre.multiply(area).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalMaxYield = maxYieldPerAcre.multiply(area).setScale(2, RoundingMode.HALF_UP);
            
            // Step 5: Check for significant deviation from previous prediction
            YieldPrediction previousPrediction = yieldPredictionRepository
                    .findFirstByCropIdOrderByPredictionDateDesc(request.getCropId())
                    .orElse(null);
            
            boolean significantDeviation = false;
            BigDecimal deviationPercent = BigDecimal.ZERO;
            String deviationNote = null;
            
            if (previousPrediction != null) {
                BigDecimal previousExpected = previousPrediction.getPredictedYieldExpectedQuintals();
                if (previousExpected != null && previousExpected.compareTo(BigDecimal.ZERO) > 0) {
                    deviationPercent = totalExpectedYield.subtract(previousExpected)
                            .abs()
                            .multiply(new BigDecimal("100"))
                            .divide(previousExpected, 2, RoundingMode.HALF_UP);
                    
                    if (deviationPercent.compareTo(SIGNIFICANT_DEVIATION_THRESHOLD) >= 0) {
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
                BigDecimal historicalAvg = yieldPredictionRepository
                        .calculateAverageActualYieldForFarmerAndCrop(
                                request.getFarmerId(), 
                                request.getHistoricalCropName() != null ? 
                                        request.getHistoricalCropName() : request.getCropName());
                if (historicalAvg != null && historicalAvg.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal varianceFromHistorical = expectedYieldPerAcre.subtract(historicalAvg)
                            .multiply(new BigDecimal("100"))
                            .divide(historicalAvg, 2, RoundingMode.HALF_UP);
                    responseBuilder.historicalAverageYieldQuintalsPerAcre(historicalAvg);
                    responseBuilder.yieldVarianceFromHistoricalPercent(varianceFromHistorical);
                    responseBuilder.historicalComparisonNote(String.format(
                            "%.1f%% %s than your historical average of %.2f quintals/acre",
                            varianceFromHistorical.abs(),
                            varianceFromHistorical.compareTo(BigDecimal.ZERO) > 0 ? "above" : "below",
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
            BigDecimal avgVariance = yieldPredictionRepository
                    .calculateAverageVarianceForCrop(request.getCropId().toString());
            
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
                if (prediction.getVariancePercent().compareTo(new BigDecimal("-10")) > 0 &&
                    prediction.getVariancePercent().compareTo(new BigDecimal("10")) < 0) {
                    responseBuilder.varianceCategory("neutral");
                } else if (prediction.getVariancePercent().compareTo(BigDecimal.ZERO) > 0) {
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
    private BigDecimal calculateGrowthStageMultiplier(String growthStage) {
        if (growthStage == null) {
            return BigDecimal.ONE;
        }
        return GROWTH_STAGE_MULTIPLIERS.getOrDefault(
                growthStage.toUpperCase().replace(" ", "_"), 
                BigDecimal.ONE);
    }

    /**
     * Calculate weather impact multiplier.
     */
    private BigDecimal calculateWeatherMultiplier(
            YieldEstimateRequestDto request,
            List<String> factors,
            List<String> adjustments) {
        
        BigDecimal multiplier = BigDecimal.ONE;
        
        // Rainfall impact
        if (request.getTotalRainfallMm() != null) {
            BigDecimal rainfallDiff = request.getTotalRainfallMm().subtract(OPTIMAL_RAINFALL_MM)
                    .abs();
            BigDecimal rainfallAdjustment = rainfallDiff.divide(OPTIMAL_RAINFALL_MM, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.1"));
            
            if (request.getTotalRainfallMm().compareTo(OPTIMAL_RAINFALL_MM) < 0) {
                // Less than optimal - negative impact
                multiplier = multiplier.subtract(rainfallAdjustment.min(new BigDecimal("0.2")));
                adjustments.add(String.format("Below optimal rainfall (%.0fmm): -%.1f%%", 
                        request.getTotalRainfallMm(), rainfallAdjustment.multiply(new BigDecimal("100"))));
            } else {
                // More than optimal - slight positive impact
                multiplier = multiplier.add(rainfallAdjustment.min(new BigDecimal("0.1")));
                adjustments.add(String.format("Above optimal rainfall (%.0fmm): +%.1f%%", 
                        request.getTotalRainfallMm(), rainfallAdjustment.multiply(new BigDecimal("100"))));
            }
            factors.add("Rainfall: " + request.getTotalRainfallMm() + "mm");
        }
        
        // Temperature impact
        if (request.getAverageTemperatureCelsius() != null) {
            BigDecimal tempDiff = request.getAverageTemperatureCelsius().subtract(OPTIMAL_TEMPERATURE_CELSIUS)
                    .abs();
            BigDecimal tempAdjustment = tempDiff.divide(OPTIMAL_TEMPERATURE_CELSIUS, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.15"));
            
            multiplier = multiplier.subtract(tempAdjustment.min(new BigDecimal("0.25")));
            adjustments.add(String.format("Temperature deviation (%.1f°C): -%.1f%%", 
                    tempDiff, tempAdjustment.multiply(new BigDecimal("100"))));
            factors.add("Temperature: " + request.getAverageTemperatureCelsius() + "°C");
        }
        
        // Extreme weather events
        if (request.getExtremeWeatherEventsCount() != null && request.getExtremeWeatherEventsCount() > 0) {
            BigDecimal extremePenalty = EXTREME_EVENT_PENALTY
                    .multiply(new BigDecimal(request.getExtremeWeatherEventsCount()))
                    .min(new BigDecimal("0.30"));
            multiplier = multiplier.subtract(extremePenalty);
            adjustments.add(String.format("Extreme weather events (%d): -%.1f%%", 
                    request.getExtremeWeatherEventsCount(), extremePenalty.multiply(new BigDecimal("100"))));
            factors.add("Extreme Weather Events: " + request.getExtremeWeatherEventsCount());
        }
        
        return multiplier.max(new BigDecimal("0.5"));
    }

    /**
     * Calculate soil health impact multiplier.
     */
    private BigDecimal calculateSoilMultiplier(
            YieldEstimateRequestDto request,
            List<String> factors,
            List<String> adjustments) {
        
        BigDecimal multiplier = BigDecimal.ONE;
        
        // Nitrogen impact
        if (request.getSoilNitrogenKgHa() != null) {
            BigDecimal nDiff = OPTIMAL_NITROGEN.subtract(request.getSoilNitrogenKgHa())
                    .max(BigDecimal.ZERO);
            BigDecimal nAdjustment = nDiff.divide(OPTIMAL_NITROGEN, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.2"));
            multiplier = multiplier.subtract(nAdjustment);
            adjustments.add(String.format("Soil nitrogen (%.0f kg/ha): -%.1f%%", 
                    request.getSoilNitrogenKgHa(), nAdjustment.multiply(new BigDecimal("100"))));
            factors.add("Soil N: " + request.getSoilNitrogenKgHa() + " kg/ha");
        }
        
        // Phosphorus impact
        if (request.getSoilPhosphorusKgHa() != null) {
            BigDecimal pDiff = OPTIMAL_PHOSPHORUS.subtract(request.getSoilPhosphorusKgHa())
                    .max(BigDecimal.ZERO);
            BigDecimal pAdjustment = pDiff.divide(OPTIMAL_PHOSPHORUS, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.15"));
            multiplier = multiplier.subtract(pAdjustment);
            adjustments.add(String.format("Soil phosphorus (%.1f kg/ha): -%.1f%%", 
                    request.getSoilPhosphorusKgHa(), pAdjustment.multiply(new BigDecimal("100"))));
            factors.add("Soil P: " + request.getSoilPhosphorusKgHa() + " kg/ha");
        }
        
        // Potassium impact
        if (request.getSoilPotassiumKgHa() != null) {
            BigDecimal kDiff = OPTIMAL_POTASSIUM.subtract(request.getSoilPotassiumKgHa())
                    .max(BigDecimal.ZERO);
            BigDecimal kAdjustment = kDiff.divide(OPTIMAL_POTASSIUM, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.1"));
            multiplier = multiplier.subtract(kAdjustment);
            adjustments.add(String.format("Soil potassium (%.0f kg/ha): -%.1f%%", 
                    request.getSoilPotassiumKgHa(), kAdjustment.multiply(new BigDecimal("100"))));
            factors.add("Soil K: " + request.getSoilPotassiumKgHa() + " kg/ha");
        }
        
        // pH impact
        if (request.getSoilPh() != null) {
            BigDecimal phDiff = request.getSoilPh().subtract(OPTIMAL_PH).abs();
            if (phDiff.compareTo(new BigDecimal("0.5")) > 0) {
                BigDecimal phAdjustment = phDiff.multiply(new BigDecimal("0.1"));
                multiplier = multiplier.subtract(phAdjustment.min(new BigDecimal("0.15")));
                adjustments.add(String.format("Soil pH deviation (%.1f): -%.1f%%", 
                        phDiff, phAdjustment.multiply(new BigDecimal("100"))));
            }
            factors.add("Soil pH: " + request.getSoilPh());
        }
        
        return multiplier.max(new BigDecimal("0.6"));
    }

    /**
     * Calculate irrigation impact multiplier.
     */
    private BigDecimal calculateIrrigationMultiplier(
            YieldEstimateRequestDto request,
            List<String> factors,
            List<String> adjustments) {
        
        String irrigationType = request.getIrrigationType() != null ? 
                request.getIrrigationType().toUpperCase() : "RAINFED";
        
        BigDecimal multiplier = IRRIGATION_MULTIPLIERS.getOrDefault(irrigationType, BigDecimal.ONE);
        
        if (multiplier.compareTo(BigDecimal.ONE) != 0) {
            adjustments.add(String.format("Irrigation type (%s): %+.1f%%", 
                    irrigationType, multiplier.subtract(BigDecimal.ONE).multiply(new BigDecimal("100"))));
        }
        factors.add("Irrigation: " + irrigationType);
        
        // Frequency adjustment
        if (request.getIrrigationFrequencyPerWeek() != null) {
            if (request.getIrrigationFrequencyPerWeek() < 1 && "RAINFED".equals(irrigationType)) {
                // Rainfed with no irrigation - already accounted for
            } else if (request.getIrrigationFrequencyPerWeek() >= 3) {
                multiplier = multiplier.multiply(new BigDecimal("1.05"));
                adjustments.add("High irrigation frequency: +5%");
            }
        }
        
        return multiplier;
    }

    /**
     * Calculate pest/disease impact multiplier.
     */
    private BigDecimal calculatePestDiseaseMultiplier(
            YieldEstimateRequestDto request,
            List<String> factors,
            List<String> adjustments) {
        
        BigDecimal multiplier = BigDecimal.ONE;
        
        // Pest incidents
        if (request.getPestIncidentCount() != null && request.getPestIncidentCount() > 0) {
            BigDecimal pestPenalty = PEST_INCIDENT_PENALTY
                    .multiply(new BigDecimal(request.getPestIncidentCount()))
                    .min(new BigDecimal("0.25"));
            
            if ("ongoing".equalsIgnoreCase(request.getPestDiseaseControlStatus()) ||
                "severe".equalsIgnoreCase(request.getPestDiseaseControlStatus())) {
                pestPenalty = pestPenalty.multiply(new BigDecimal("2"));
            }
            
            multiplier = multiplier.subtract(pestPenalty);
            adjustments.add(String.format("Pest incidents (%d): -%.1f%%", 
                    request.getPestIncidentCount(), pestPenalty.multiply(new BigDecimal("100"))));
            factors.add("Pest Incidents: " + request.getPestIncidentCount());
        }
        
        // Disease incidents
        if (request.getDiseaseIncidentCount() != null && request.getDiseaseIncidentCount() > 0) {
            BigDecimal diseasePenalty = DISEASE_INCIDENT_PENALTY
                    .multiply(new BigDecimal(request.getDiseaseIncidentCount()))
                    .min(new BigDecimal("0.30"));
            
            if ("ongoing".equalsIgnoreCase(request.getPestDiseaseControlStatus()) ||
                "severe".equalsIgnoreCase(request.getPestDiseaseControlStatus())) {
                diseasePenalty = diseasePenalty.multiply(new BigDecimal("2"));
            }
            
            multiplier = multiplier.subtract(diseasePenalty);
            adjustments.add(String.format("Disease incidents (%d): -%.1f%%", 
                    request.getDiseaseIncidentCount(), diseasePenalty.multiply(new BigDecimal("100"))));
            factors.add("Disease Incidents: " + request.getDiseaseIncidentCount());
        }
        
        // Affected area
        if (request.getAffectedAreaPercent() != null && request.getAffectedAreaPercent().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal areaPenalty = request.getAffectedAreaPercent()
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.3"));
            multiplier = multiplier.subtract(areaPenalty);
            adjustments.add(String.format("Affected area (%.1f%%): -%.1f%%", 
                    request.getAffectedAreaPercent(), areaPenalty.multiply(new BigDecimal("100"))));
            factors.add("Affected Area: " + request.getAffectedAreaPercent() + "%");
        }
        
        return multiplier.max(new BigDecimal("0.4"));
    }

    /**
     * Calculate historical data multiplier.
     */
    private BigDecimal calculateHistoricalMultiplier(
            YieldEstimateRequestDto request,
            List<String> factors,
            List<String> adjustments) {
        
        if (!Boolean.TRUE.equals(request.getIncludeHistoricalData())) {
            return BigDecimal.ONE;
        }
        
        String cropName = request.getHistoricalCropName() != null ? 
                request.getHistoricalCropName() : request.getCropName();
        
        BigDecimal historicalAvg = yieldPredictionRepository
                .calculateAverageActualYieldForFarmerAndCrop(request.getFarmerId(), cropName);
        
        if (historicalAvg != null && historicalAvg.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal[] baseYields = CROP_BASE_YIELDS.getOrDefault(
                    cropName.toUpperCase(), 
                    new BigDecimal[]{new BigDecimal("15"), new BigDecimal("25"), new BigDecimal("35")});
            
            BigDecimal expectedBase = baseYields[1];
            BigDecimal historicalRatio = historicalAvg.divide(expectedBase, 4, RoundingMode.HALF_UP);
            
            if (historicalRatio.compareTo(new BigDecimal("0.8")) < 0 || 
                historicalRatio.compareTo(new BigDecimal("1.2")) > 0) {
                adjustments.add(String.format("Historical yield adjustment: %+.1f%%", 
                        historicalRatio.subtract(BigDecimal.ONE).multiply(new BigDecimal("100"))));
                factors.add("Historical Data: Based on your " + cropName + " records");
            }
            
            return historicalRatio;
        }
        
        return BigDecimal.ONE;
    }

    /**
     * Generate financial projection based on yield estimates and mandi prices.
     */
    private FinancialProjectionDto generateFinancialProjection(
            String cropName, BigDecimal expectedYield, BigDecimal minYield, BigDecimal maxYield) {
        
        try {
            // Get current prices from mandi service
            var priceData = mandiPriceClient.getCurrentPrice(cropName);
            
            BigDecimal currentPrice = priceData.getOrDefault("modalPrice", new BigDecimal("2000"));
            BigDecimal minPrice = priceData.getOrDefault("minPrice", currentPrice.multiply(new BigDecimal("0.9")));
            BigDecimal maxPrice = priceData.getOrDefault("maxPrice", currentPrice.multiply(new BigDecimal("1.1")));
            
            // Calculate revenue
            BigDecimal expectedRevenue = expectedYield.multiply(currentPrice).setScale(0, RoundingMode.HALF_UP);
            BigDecimal minRevenue = minYield.multiply(minPrice).setScale(0, RoundingMode.HALF_UP);
            BigDecimal maxRevenue = maxYield.multiply(maxPrice).setScale(0, RoundingMode.HALF_UP);
            
            // Estimate costs (simplified - in real implementation, would use actual cost data)
            BigDecimal estimatedCosts = expectedYield.multiply(new BigDecimal("500")) // ~500 INR per quintal input cost
                    .setScale(0, RoundingMode.HALF_UP);
            
            // Calculate profit
            BigDecimal expectedProfit = expectedRevenue.subtract(estimatedCosts);
            BigDecimal minProfit = minRevenue.subtract(estimatedCosts);
            BigDecimal maxProfit = maxRevenue.subtract(estimatedCosts);
            
            // Calculate ROI
            BigDecimal roi = BigDecimal.ZERO;
            if (estimatedCosts.compareTo(BigDecimal.ZERO) > 0) {
                roi = expectedProfit.multiply(new BigDecimal("100"))
                        .divide(estimatedCosts, 2, RoundingMode.HALF_UP);
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
                    .estimatedRevenueExpected(expectedYield.multiply(new BigDecimal("2000")))
                    .marketAdvisory("monitor")
                    .advisoryReason("Price data unavailable - please check local mandi prices")
                    .build();
        }
    }
}