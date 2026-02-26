package com.farmer.crop.service;

import com.farmer.crop.dto.ClimateRiskDto;
import com.farmer.crop.dto.ClimateRiskDto.ClimateRiskLevel;
import com.farmer.crop.dto.ClimateRiskDto.DroughtRiskLevel;
import com.farmer.crop.dto.ClimateRiskDto.FloodRiskLevel;
import com.farmer.crop.dto.GaezCropSuitabilityDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.util.*;

/**
 * Service for analyzing climate risk under rainfall deviation scenarios.
 * 
 * This service assesses crop vulnerability to climate variability and
 * flags crops with high climate risk under projected rainfall scenarios.
 * 
 * Validates: Requirement 2.8
 */
@Service
@Transactional(readOnly = true)
public class ClimateRiskService {

    private static final Logger logger = LoggerFactory.getLogger(ClimateRiskService.class);

    // Crop-specific climate sensitivity factors
    private static final Map<String, CropClimateProfile> CROP_CLIMATE_PROFILES = new HashMap<>();

    static {
        // Initialize crop climate profiles
        CROP_CLIMATE_PROFILES.put("RICE", new CropClimateProfile(
                400.0, 2500.0,  // Rainfall range (mm)
                20.0, 35.0,     // Temperature range (°C)
                20.0,                          // Heat stress threshold
                10.0,                          // Cold stress threshold
                DroughtRiskLevel.HIGH,                         // Drought risk
                FloodRiskLevel.HIGH,                           // Flood risk
                Arrays.asList("blast", "bacterial leaf blight"),
                Arrays.asList("heat-tolerant varieties", "water management")
        ));

        CROP_CLIMATE_PROFILES.put("WHEAT", new CropClimateProfile(
                250.0, 750.0,
                10.0, 25.0,
                30.0,
                5.0,
                DroughtRiskLevel.MODERATE,
                FloodRiskLevel.LOW,
                Arrays.asList("rust", "heat stress"),
                Arrays.asList("early sowing", "heat-tolerant varieties")
        ));

        CROP_CLIMATE_PROFILES.put("COTTON", new CropClimateProfile(
                350.0, 750.0,
                20.0, 40.0,
                38.0,
                12.0,
                DroughtRiskLevel.MODERATE,
                FloodRiskLevel.LOW,
                Arrays.asList("pink bollworm", "whitefly"),
                Arrays.asList("Bt varieties", "IPM")
        ));

        CROP_CLIMATE_PROFILES.put("SOYBEAN", new CropClimateProfile(
                300.0, 800.0,
                15.0, 35.0,
                32.0,
                8.0,
                DroughtRiskLevel.MODERATE,
                FloodRiskLevel.MODERATE,
                Arrays.asList("rust", "stem fly"),
                Arrays.asList("drought-tolerant varieties", "timely sowing")
        ));

        CROP_CLIMATE_PROFILES.put("GROUNDNUT", new CropClimateProfile(
                350.0, 500.0,
                20.0, 35.0,
                38.0,
                10.0,
                DroughtRiskLevel.HIGH,
                FloodRiskLevel.LOW,
                Arrays.asList("tikka disease", "rust"),
                Arrays.asList("rain-fed varieties", "mulching")
        ));

        CROP_CLIMATE_PROFILES.put("MUSTARD", new CropClimateProfile(
                200.0, 600.0,
                10.0, 30.0,
                32.0,
                5.0,
                DroughtRiskLevel.MODERATE,
                FloodRiskLevel.LOW,
                Arrays.asList("alternaria blight", "white rust"),
                Arrays.asList("early maturing varieties", "proper spacing")
        ));

        CROP_CLIMATE_PROFILES.put("PULSES", new CropClimateProfile(
                250.0, 700.0,
                15.0, 35.0,
                35.0,
                8.0,
                DroughtRiskLevel.MODERATE,
                FloodRiskLevel.LOW,
                Arrays.asList("wilt", "powdery mildew"),
                Arrays.asList("drought-tolerant varieties", "seed treatment")
        ));

        CROP_CLIMATE_PROFILES.put("MAIZE", new CropClimateProfile(
                400.0, 800.0,
                15.0, 38.0,
                35.0,
                8.0,
                DroughtRiskLevel.MODERATE,
                FloodRiskLevel.MODERATE,
                Arrays.asList("stem borer", "leaf blight"),
                Arrays.asList("hybrid varieties", "timely irrigation")
        ));

        CROP_CLIMATE_PROFILES.put("SUGARCANE", new CropClimateProfile(
                750.0, 1500.0,
                20.0, 35.0,
                38.0,
                10.0,
                DroughtRiskLevel.MODERATE,
                FloodRiskLevel.HIGH,
                Arrays.asList("red rot", "wilt"),
                Arrays.asList("drought-tolerant varieties", "trash mulching")
        ));
    }

    /**
     * Analyze climate risk for a crop.
     * 
     * @param cropCode Crop code
     * @param projectedRainfallDeviation Projected rainfall deviation (%)
     * @param projectedTempDeviation Projected temperature deviation (°C)
     * @return Climate risk analysis
     * 
     * Validates: Requirement 2.8
     */
    public ClimateRiskDto analyzeClimateRisk(
            String cropCode, Double projectedRainfallDeviation, Double projectedTempDeviation) {
        
        logger.info("Analyzing climate risk for crop: {} with rainfall deviation: {}%", 
                cropCode, projectedRainfallDeviation);
        
        CropClimateProfile profile = CROP_CLIMATE_PROFILES.getOrDefault(cropCode, 
                getDefaultProfile(cropCode));
        
        // Analyze rainfall deviation scenario
        ClimateRiskDto.RainfallDeviationScenario rainfallScenario = analyzeRainfallScenario(
                profile, projectedRainfallDeviation);
        
        // Analyze temperature stress
        ClimateRiskDto.TemperatureStressAnalysis tempStress = analyzeTemperatureStress(
                profile, projectedTempDeviation);
        
        // Calculate overall risk score
        Double riskScore = calculateRiskScore(rainfallScenario, tempStress, profile);
        
        // Determine risk level
        ClimateRiskLevel riskLevel = determineRiskLevel(riskScore);
        
        // Identify key risks
        List<String> keyRisks = identifyKeyRisks(rainfallScenario, tempStress, profile);
        
        // Generate mitigation strategies
        List<String> mitigationStrategies = generateMitigationStrategies(
                rainfallScenario, tempStress, riskLevel, profile);
        
        // Get resilient varieties
        List<String> resilientVarieties = getResilientVarieties(cropCode);
        
        // Determine drought and flood risk
        DroughtRiskLevel droughtRisk = assessDroughtRisk(profile, projectedRainfallDeviation);
        FloodRiskLevel floodRisk = assessFloodRisk(profile, projectedRainfallDeviation);
        
        // Determine if insurance is recommended
        Boolean insuranceRecommended = riskLevel == ClimateRiskLevel.HIGH || 
                riskLevel == ClimateRiskLevel.VERY_HIGH;
        
        return ClimateRiskDto.builder()
                .cropCode(cropCode)
                .cropName(getCropName(cropCode))
                .riskLevel(riskLevel)
                .riskScore(riskScore)
                .rainfallScenario(rainfallScenario)
                .temperatureStress(tempStress)
                .droughtRisk(droughtRisk)
                .floodRisk(floodRisk)
                .keyRisks(keyRisks)
                .mitigationStrategies(mitigationStrategies)
                .resilientVarieties(resilientVarieties)
                .optimalPlantingWindow(determineOptimalPlantingWindow(cropCode))
                .insuranceRecommended(insuranceRecommended)
                .build();
    }

    /**
     * Analyze climate risk for multiple crops.
     * 
     * @param cropCodes List of crop codes
     * @param projectedRainfallDeviation Projected rainfall deviation (%)
     * @return Map of crop code to climate risk analysis
     * 
     * Validates: Requirement 2.8
     */
    public Map<String, ClimateRiskDto> analyzeClimateRiskForCrops(
            List<String> cropCodes, Double projectedRainfallDeviation) {
        
        logger.info("Analyzing climate risk for {} crops with rainfall deviation: {}", 
                cropCodes.size(), projectedRainfallDeviation);
        
        Map<String, ClimateRiskDto> riskMap = new HashMap<>();
        
        for (String cropCode : cropCodes) {
            ClimateRiskDto risk = analyzeClimateRisk(cropCode, projectedRainfallDeviation, 0.0);
            riskMap.put(cropCode, risk);
        }
        
        return riskMap;
    }

    /**
     * Flag crops with high climate risk.
     * 
     * @param climateRiskMap Map of crop code to climate risk
     * @return List of high-risk crops
     * 
     * Validates: Requirement 2.8
     */
    public List<String> flagHighRiskCrops(Map<String, ClimateRiskDto> climateRiskMap) {
        return climateRiskMap.entrySet().stream()
                .filter(e -> e.getValue().getRiskLevel() == ClimateRiskLevel.HIGH ||
                        e.getValue().getRiskLevel() == ClimateRiskLevel.VERY_HIGH)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Calculate climate-adjusted suitability score.
     * 
     * @param baseScore Base suitability score
     * @param climateRisk Climate risk analysis
     * @return Climate-adjusted score
     * 
     * Validates: Requirement 2.8
     */
    public Double calculateClimateAdjustedScore(
            Double baseScore,
            ClimateRiskDto climateRisk) {
        
        if (climateRisk == null) {
            return baseScore;
        }
        
        Double adjustment = switch (climateRisk.getRiskLevel()) {
            case LOW -> 0.0;
            case MEDIUM -> new Double("-3");
            case HIGH -> -7.0;
            case VERY_HIGH -> -12.0;
        };
        
        Double adjustedScore = baseScore + (adjustment);
        return Math.max(adjustedScore, 0.0);
    }

    // Helper methods

    private ClimateRiskDto.RainfallDeviationScenario analyzeRainfallScenario(
            CropClimateProfile profile, Double deviation) {
        
        ClimateRiskDto.RainfallDeviationScenario.ScenarioType scenarioType;
        Double yieldImpact;
        ClimateRiskLevel scenarioRiskLevel;
        Double probability = 30.0; // Default probability
        
        if (deviation == null) {
            deviation = 0.0;
        }
        
        if (deviation.compareTo(new Double("-20")) < 0) {
            scenarioType = ClimateRiskDto.RainfallDeviationScenario.ScenarioType.DEFICIT;
            yieldImpact = deviation * (0.5); // 0.5% impact per 1% deficit
            scenarioRiskLevel = deviation.compareTo(new Double("-30")) < 0 ? 
                    ClimateRiskLevel.HIGH : ClimateRiskLevel.MEDIUM;
            probability = 25.0;
        } else if (deviation.compareTo(20.0) > 0) {
            scenarioType = ClimateRiskDto.RainfallDeviationScenario.ScenarioType.EXCESS;
            yieldImpact = deviation * (0.3);
            scenarioRiskLevel = deviation.compareTo(30.0) > 0 ? 
                    ClimateRiskLevel.HIGH : ClimateRiskLevel.MEDIUM;
            probability = 20.0;
        } else {
            scenarioType = ClimateRiskDto.RainfallDeviationScenario.ScenarioType.NORMAL;
            yieldImpact = 0.0;
            scenarioRiskLevel = ClimateRiskLevel.LOW;
            probability = 55.0;
        }
        
        return ClimateRiskDto.RainfallDeviationScenario.builder()
                .historicalAverageMm((profile.rainfallRangeMin + profile.rainfallRangeMax) / 2.0)
                .projectedDeviationPercent(deviation)
                .scenarioType(scenarioType)
                .yieldImpactPercent(yieldImpact)
                .probability(probability)
                .scenarioRiskLevel(scenarioRiskLevel)
                .build();
    }

    private ClimateRiskDto.TemperatureStressAnalysis analyzeTemperatureStress(
            CropClimateProfile profile, Double deviation) {
        
        if (deviation == null) {
            deviation = 0.0;
        }
        
        Double projectedMaxTemp = profile.tempMax + (deviation);
        Double projectedMinTemp = profile.tempMin + (deviation);
        
        int extremeHeatDays = 0;
        int extremeColdDays = 0;
        
        if (projectedMaxTemp.compareTo(profile.heatStressThreshold) > 0) {
            Double excess = projectedMaxTemp - (profile.heatStressThreshold);
            extremeHeatDays = (int)(excess * 5.0); // 5 days per °C excess
        }
        
        if (projectedMinTemp.compareTo(profile.coldStressThreshold) < 0) {
            Double deficit = profile.coldStressThreshold - (projectedMinTemp);
            extremeColdDays = (int)(deficit * 3.0);
        }
        
        ClimateRiskLevel stressRiskLevel;
        if (extremeHeatDays > 10 || extremeColdDays > 10) {
            stressRiskLevel = ClimateRiskLevel.HIGH;
        } else if (extremeHeatDays > 5 || extremeColdDays > 5) {
            stressRiskLevel = ClimateRiskLevel.MEDIUM;
        } else {
            stressRiskLevel = ClimateRiskLevel.LOW;
        }
        
        return ClimateRiskDto.TemperatureStressAnalysis.builder()
                .optimalTempMin(profile.tempMin)
                .optimalTempMax(profile.tempMax)
                .heatStressThreshold(profile.heatStressThreshold)
                .coldStressThreshold(profile.coldStressThreshold)
                .projectedDeviation(deviation)
                .extremeHeatDays(Math.min(extremeHeatDays, 30))
                .extremeColdDays(Math.min(extremeColdDays, 30))
                .stressRiskLevel(stressRiskLevel)
                .build();
    }

    private Double calculateRiskScore(
            ClimateRiskDto.RainfallDeviationScenario rainfall,
            ClimateRiskDto.TemperatureStressAnalysis temp,
            CropClimateProfile profile) {
        
        Double score = 0.0;
        
        // Rainfall scenario contribution
        if (rainfall.getScenarioRiskLevel() == ClimateRiskLevel.HIGH) {
            score = score + (30.0);
        } else if (rainfall.getScenarioRiskLevel() == ClimateRiskLevel.MEDIUM) {
            score = score + (15.0);
        }
        
        // Temperature stress contribution
        if (temp.getStressRiskLevel() == ClimateRiskLevel.HIGH) {
            score = score + 25.0;
        } else if (temp.getStressRiskLevel() == ClimateRiskLevel.MEDIUM) {
            score = score + 12.0;
        }
        
        // Drought risk contribution
        if (profile.droughtRisk == DroughtRiskLevel.HIGH) {
            score = score + 15.0;
        } else if (profile.droughtRisk == DroughtRiskLevel.MODERATE) {
            score = score + 8.0;
        }
        
        // Flood risk contribution
        if (profile.floodRisk == FloodRiskLevel.HIGH) {
            score = score + 15.0;
        } else if (profile.floodRisk == FloodRiskLevel.MODERATE) {
            score = score + 8.0;
        }
        
        return score;
    }

    private ClimateRiskLevel determineRiskLevel(Double riskScore) {
        if (riskScore.compareTo(60.0) >= 0) {
            return ClimateRiskLevel.VERY_HIGH;
        } else if (riskScore.compareTo(40.0) >= 0) {
            return ClimateRiskLevel.HIGH;
        } else if (riskScore.compareTo(20.0) >= 0) {
            return ClimateRiskLevel.MEDIUM;
        } else {
            return ClimateRiskLevel.LOW;
        }
    }

    private List<String> identifyKeyRisks(
            ClimateRiskDto.RainfallDeviationScenario rainfall,
            ClimateRiskDto.TemperatureStressAnalysis temp,
            CropClimateProfile profile) {
        
        List<String> risks = new ArrayList<>();
        
        if (rainfall.getScenarioType() == ClimateRiskDto.RainfallDeviationScenario.ScenarioType.DEFICIT) {
            risks.add("Rainfall deficit - drought conditions expected");
        } else if (rainfall.getScenarioType() == ClimateRiskDto.RainfallDeviationScenario.ScenarioType.EXCESS) {
            risks.add("Excessive rainfall - flood/waterlogging risk");
        }
        
        if (temp.getExtremeHeatDays() > 5) {
            risks.add(String.format("Expected %d days of extreme heat stress", temp.getExtremeHeatDays()));
        }
        
        if (temp.getExtremeColdDays() > 5) {
            risks.add(String.format("Expected %d days of extreme cold stress", temp.getExtremeColdDays()));
        }
        
        if (profile.droughtRisk == DroughtRiskLevel.HIGH) {
            risks.add("High drought vulnerability");
        }
        
        if (profile.floodRisk == FloodRiskLevel.HIGH) {
            risks.add("High flood/waterlogging vulnerability");
        }
        
        return risks;
    }

    private List<String> generateMitigationStrategies(
            ClimateRiskDto.RainfallDeviationScenario rainfall,
            ClimateRiskDto.TemperatureStressAnalysis temp,
            ClimateRiskLevel riskLevel,
            CropClimateProfile profile) {
        
        List<String> strategies = new ArrayList<>();
        
        if (riskLevel == ClimateRiskLevel.HIGH || riskLevel == ClimateRiskLevel.VERY_HIGH) {
            strategies.add("Consider climate-resilient varieties");
            strategies.add("Implement soil moisture conservation techniques");
            strategies.add("Monitor weather forecasts closely");
        }
        
        if (rainfall.getScenarioType() == ClimateRiskDto.RainfallDeviationScenario.ScenarioType.DEFICIT) {
            strategies.add("Use drought-tolerant varieties");
            strategies.add("Apply mulching to conserve soil moisture");
            strategies.add("Consider supplemental irrigation if available");
            strategies.add("Adjust planting dates to avoid dry periods");
        } else if (rainfall.getScenarioType() == ClimateRiskDto.RainfallDeviationScenario.ScenarioType.EXCESS) {
            strategies.add("Ensure proper drainage");
            strategies.add("Use raised bed planting");
            strategies.add("Avoid waterlogging-sensitive varieties");
        }
        
        if (temp.getExtremeHeatDays() > 5) {
            strategies.add("Apply foliar sprays during heat stress");
            strategies.add("Use heat-tolerant varieties");
            strategies.add("Irrigate during heat waves");
        }
        
        if (profile.droughtRisk == DroughtRiskLevel.HIGH) {
            strategies.addAll(profile.mitigationStrategies);
        }
        
        return strategies;
    }

    private List<String> getResilientVarieties(String cropCode) {
        return switch (cropCode) {
            case "RICE" -> Arrays.asList("DRR Dhan 44 (drought-tolerant)", "Swarna-Sub1 (flood-tolerant)");
            case "WHEAT" -> Arrays.asList("HD-2967 (heat-tolerant)", "DBW-187 (drought-tolerant)");
            case "COTTON" -> Arrays.asList("Bt hybrids (pest-resistant)", "Drought-tolerant Bt varieties");
            case "SOYBEAN" -> Arrays.asList("JS-335 (early maturing)", "MACS-450 (drought-tolerant)");
            case "GROUNDNUT" -> Arrays.asList("TAG-24 (drought-tolerant)", "ICGS-44 (climate-resilient)");
            case "MUSTARD" -> Arrays.asList("Varuna (heat-tolerant)", "RH-749 (drought-tolerant)");
            case "PULSES" -> Arrays.asList("Drought-tolerant varieties", "Early maturing varieties");
            case "MAIZE" -> Arrays.asList("Hybrid varieties (heat-tolerant)", "Quality protein maize");
            case "SUGARCANE" -> Arrays.asList("Co-86032 (drought-tolerant)", "Co-99004 (water-use efficient)");
            default -> Collections.emptyList();
        };
    }

    private String determineOptimalPlantingWindow(String cropCode) {
        return switch (cropCode) {
            case "RICE" -> "Kharif: June-July (after monsoon onset)";
            case "WHEAT" -> "Rabi: October-November";
            case "COTTON" -> "Kharif: April-May";
            case "SOYBEAN" -> "Kharif: June-July";
            case "GROUNDNUT" -> "Kharif: June-July, Rabi: October-November";
            case "MUSTARD" -> "Rabi: October-November";
            case "PULSES" -> "Kharif: June-July, Rabi: October-November";
            case "MAIZE" -> "Kharif: June-July, Zaid: February-March";
            case "SUGARCANE" -> "October-November (planting), February-March (ratooning)";
            default -> "Consult local agricultural office";
        };
    }

    private DroughtRiskLevel assessDroughtRisk(CropClimateProfile profile, Double deviation) {
        if (deviation.compareTo(new Double("-20")) < 0) {
            return switch (profile.droughtRisk) {
                case HIGH -> DroughtRiskLevel.SEVERE;
                case MODERATE -> DroughtRiskLevel.HIGH;
                default -> DroughtRiskLevel.MODERATE;
            };
        }
        return profile.droughtRisk;
    }

    private FloodRiskLevel assessFloodRisk(CropClimateProfile profile, Double deviation) {
        if (deviation.compareTo(20.0) > 0) {
            return switch (profile.floodRisk) {
                case HIGH -> FloodRiskLevel.SEVERE;
                case MODERATE -> FloodRiskLevel.HIGH;
                default -> FloodRiskLevel.MODERATE;
            };
        }
        return profile.floodRisk;
    }

    private CropClimateProfile getDefaultProfile(String cropCode) {
        return new CropClimateProfile(
                300.0, 800.0,
                15.0, 35.0,
                35.0,
                8.0,
                DroughtRiskLevel.MODERATE,
                FloodRiskLevel.MODERATE,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private String getCropName(String cropCode) {
        return switch (cropCode) {
            case "RICE" -> "Rice";
            case "WHEAT" -> "Wheat";
            case "COTTON" -> "Cotton";
            case "SOYBEAN" -> "Soybean";
            case "GROUNDNUT" -> "Groundnut";
            case "MUSTARD" -> "Mustard";
            case "PULSES" -> "Pulses";
            case "MAIZE" -> "Maize";
            case "SUGARCANE" -> "Sugarcane";
            default -> cropCode;
        };
    }

    /**
     * Internal class for crop climate profile
     */
    private static class CropClimateProfile {
        Double rainfallRangeMin;
        Double rainfallRangeMax;
        Double tempMin;
        Double tempMax;
        Double heatStressThreshold;
        Double coldStressThreshold;
        DroughtRiskLevel droughtRisk;
        FloodRiskLevel floodRisk;
        List<String> diseaseSusceptibility;
        List<String> mitigationStrategies;

        CropClimateProfile(Double rainfallRangeMin, Double rainfallRangeMax, Double tempMin, Double tempMax, Double heatStressThreshold, Double coldStressThreshold, DroughtRiskLevel droughtRisk,
                FloodRiskLevel floodRisk, List<String> diseaseSusceptibility,
                List<String> mitigationStrategies) {
            this.rainfallRangeMin = rainfallRangeMin;
            this.rainfallRangeMax = rainfallRangeMax;
            this.tempMin = tempMin;
            this.tempMax = tempMax;
            this.heatStressThreshold = heatStressThreshold;
            this.coldStressThreshold = coldStressThreshold;
            this.droughtRisk = droughtRisk;
            this.floodRisk = floodRisk;
            this.diseaseSusceptibility = diseaseSusceptibility;
            this.mitigationStrategies = mitigationStrategies;
        }
    }
}










