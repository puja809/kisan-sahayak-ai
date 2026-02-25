package com.farmer.crop.service;

import com.farmer.crop.dto.CropHistoryEntryDto;
import com.farmer.crop.dto.RotationOptionDto;
import com.farmer.crop.dto.RotationRecommendationRequestDto;
import com.farmer.crop.dto.RotationRecommendationResultDto;
import com.farmer.crop.enums.CropFamily;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rotation recommendation engine for crop rotation planning.
 * 
 * Implements nutrient cycling optimization, legume integration, rice diversification,
 * intercropping suggestions, and pest/disease carryover risk assessment.
 * 
 * Requirements: 3.3, 3.4, 3.5, 3.6, 3.7
 */
@Service
public class RotationRecommendationEngine {

    // Deep-rooted crops for nutrient cycling (access deeper soil nutrients)
    private static final Set<String> DEEP_ROOTED_CROPS = new HashSet<>(Arrays.asList(
            "Sunflower", "Sorghum", "Cotton", "Carrot", "Onion", "Tomato", "Maize",
            "Pigeon Pea", "Redgram", "Soybean", "Sesame", "Safflower"
    ));

    // Shallow-rooted crops (deplete topsoil nutrients)
    private static final Set<String> SHALLOW_ROOTED_CROPS = new HashSet<>(Arrays.asList(
            "Cabbage", "Cauliflower", "Broccoli", "Mustard", "Radish", "Lettuce",
            "Spinach", "Cucumber", "Bottle Gourd", "Watermelon", "Wheat", "Rice"
    ));

    // Legume crops for nitrogen fixation
    private static final Set<String> LEGUME_CROPS = new HashSet<>(Arrays.asList(
            "Greengram", "Blackgram", "Redgram", "Chickpea", "Lentil", "Peas",
            "Soybean", "Groundnut", "Cowpea", "Horsegram", "Mothbean", "Berseem"
    ));

    // Rice-based system crops for diversification
    private static final Set<String> RICE_DIVERSIFICATION_CROPS = new HashSet<>(Arrays.asList(
            "Greengram", "Blackgram", "Lentil", "Chickpea", "Mustard", "Sunflower",
            "Sesame", "Groundnut", "Wheat", "Barley", "Oat", "Rapeseed"
    ));

    // Relay cropping pairs (main crop -> relay crop)
    private static final Map<String, Set<String>> RELAY_CROP_PAIRS = new HashMap<>();
    static {
        RELAY_CROP_PAIRS.put("Rice", new HashSet<>(Arrays.asList("Lentil", "Chickpea", "Greengram", "Blackgram")));
        RELAY_CROP_PAIRS.put("Paddy", new HashSet<>(Arrays.asList("Lentil", "Chickpea", "Greengram", "Blackgram")));
        RELAY_CROP_PAIRS.put("Wheat", new HashSet<>(Arrays.asList("Chickpea", "Lentil", "Mustard")));
        RELAY_CROP_PAIRS.put("Maize", new HashSet<>(Arrays.asList("Cowpea", "Greengram", "Soybean")));
    }

    // Pest and disease carryover risk mappings
    private static final Map<String, Set<String>> CROP_PEST_RISK = new HashMap<>();
    static {
        CROP_PEST_RISK.put("Rice", new HashSet<>(Arrays.asList("Blast", "Bacterial Leaf Blight", "Brown Planthopper", "Stem Rot")));
        CROP_PEST_RISK.put("Wheat", new HashSet<>(Arrays.asList("Rust", "Karnal Bunt", "Powdery Mildew", "Aphids")));
        CROP_PEST_RISK.put("Cotton", new HashSet<>(Arrays.asList("Pink Bollworm", "Whitefly", "Leaf Curl Virus", "Wilt")));
        CROP_PEST_RISK.put("Sugarcane", new HashSet<>(Arrays.asList("Top Borer", "Pyrilla", "Red Rot", "Smut")));
        CROP_PEST_RISK.put("Groundnut", new HashSet<>(Arrays.asList("Leaf Spot", "Rust", "Aflatoxin", "Termites")));
        CROP_PEST_RISK.put("Soybean", new HashSet<>(Arrays.asList("Yellow Mosaic", "Stem Fly", "Girdle Beetle", "Rust")));
    }

    // Same family pest carryover crops (high risk)
    private static final Map<CropFamily, Set<CropFamily>> PEST_CARRYOVER_RISK = new HashMap<>();
    static {
        PEST_CARRYOVER_RISK.put(CropFamily.CEREALS, new HashSet<>(Arrays.asList(CropFamily.CEREALS)));
        PEST_CARRYOVER_RISK.put(CropFamily.LEGUMES, new HashSet<>(Arrays.asList(CropFamily.LEGUMES)));
        PEST_CARRYOVER_RISK.put(CropFamily.BRASSICAS, new HashSet<>(Arrays.asList(CropFamily.BRASSICAS)));
        PEST_CARRYOVER_RISK.put(CropFamily.SOLANACEOUS, new HashSet<>(Arrays.asList(CropFamily.SOLANACEOUS)));
        PEST_CARRYOVER_RISK.put(CropFamily.CUCURBITS, new HashSet<>(Arrays.asList(CropFamily.CUCURBITS)));
    }

    /**
     * Generate rotation recommendations based on crop history and farmer preferences.
     * 
     * @param request The rotation recommendation request
     * @return Rotation recommendation result with multiple options
     */
    public RotationRecommendationResultDto generateRecommendations(RotationRecommendationRequestDto request) {
        List<RotationOptionDto> options = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> generalRecommendations = new ArrayList<>();

        // Analyze current crop history
        List<CropHistoryEntryDto> history = request.getCropHistory();
        boolean hasRiceBasedSystem = hasRiceBasedSystem(history);
        boolean hasConsecutiveMonoculture = hasConsecutiveMonoculture(history, 2);

        // Generate nutrient cycling options
        options.addAll(generateNutrientCyclingOptions(history, request.getSeason()));

        // Generate legume integration options if applicable
        if (shouldRecommendLegumes(history)) {
            options.addAll(generateLegumeIntegrationOptions(history, request.getSeason()));
        }

        // Generate rice diversification options if rice-based system detected
        if (hasRiceBasedSystem) {
            options.addAll(generateRiceDiversificationOptions(history, request.getSeason()));
            warnings.add("Rice-based system detected. Consider diversification to break pest cycles and improve soil health.");
        }

        // Generate intercropping/relay cropping options
        options.addAll(generateIntercroppingOptions(history, request.getSeason()));

        // Calculate overall scores for each option BEFORE sorting
        CropRotationService rotationService = new CropRotationService();
        options = options.stream()
                .map(opt -> {
                    opt.setOverallBenefitScore(rotationService.calculateOverallBenefitScore(opt));
                    return opt;
                })
                .collect(Collectors.toList());

        // Rank options by overall benefit score (now that scores are calculated)
        options = rotationService.rankRotationOptions(options);

        // Assess pest and disease carryover risk
        List<String> pestRisks = assessPestDiseaseCarryoverRisk(history);
        if (!pestRisks.isEmpty()) {
            warnings.addAll(pestRisks);
        }

        // Add general recommendations
        generalRecommendations.addAll(generateGeneralRecommendations(history, hasRiceBasedSystem, hasConsecutiveMonoculture));

        return RotationRecommendationResultDto.builder()
                .options(options)
                .warnings(warnings)
                .recommendations(generalRecommendations)
                .hasRiceBasedSystem(hasRiceBasedSystem)
                .pestRiskLevel(calculateOverallPestRiskLevel(history))
                .build();
    }

    /**
     * Generate nutrient cycling optimization options by alternating deep and shallow-rooted crops.
     * Requirement 3.3
     */
    private List<RotationOptionDto> generateNutrientCyclingOptions(List<CropHistoryEntryDto> history, String targetSeason) {
        List<RotationOptionDto> options = new ArrayList<>();

        // Get last crop to determine what to rotate away from
        String lastCrop = history != null && !history.isEmpty() ? 
                history.get(0).getCropName() : null;
        CropFamily lastFamily = lastCrop != null ? CropFamily.getFamilyForCrop(lastCrop) : null;
        CropFamily.RootDepth lastDepth = lastCrop != null ? 
                CropFamily.getRootDepthForCrop(lastCrop) : CropFamily.RootDepth.MEDIUM;

        // Determine recommended depth (opposite of last crop)
        CropFamily.RootDepth recommendedDepth = lastDepth == CropFamily.RootDepth.DEEP ? 
                CropFamily.RootDepth.SHALLOW : CropFamily.RootDepth.DEEP;

        // Generate options based on recommended depth
        if (recommendedDepth == CropFamily.RootDepth.DEEP) {
            // Recommend deep-rooted crops
            List<String> deepCrops = getDeepRootedCropsExcludingFamily(lastFamily);
            for (String crop : deepCrops) {
                BigDecimal nutrientScore = calculateNutrientCyclingScore(crop, lastCrop);
                RotationOptionDto option = createNutrientCyclingOption(
                        crop, lastCrop, "Deep-rooted crop for nutrient cycling",
                        nutrientScore, targetSeason);
                options.add(option);
            }
        } else {
            // Recommend shallow-rooted crops
            List<String> shallowCrops = getShallowRootedCropsExcludingFamily(lastFamily);
            for (String crop : shallowCrops) {
                BigDecimal nutrientScore = calculateNutrientCyclingScore(crop, lastCrop);
                RotationOptionDto option = createNutrientCyclingOption(
                        crop, lastCrop, "Shallow-rooted crop for nutrient cycling",
                        nutrientScore, targetSeason);
                options.add(option);
            }
        }

        // Add a balanced rotation option (deep -> medium -> shallow -> legume)
        RotationOptionDto balancedOption = createBalancedRotationOption(lastCrop, targetSeason);
        options.add(balancedOption);

        return options;
    }

    /**
     * Generate legume integration options for biological nitrogen fixation.
     * Requirement 3.4
     */
    private List<RotationOptionDto> generateLegumeIntegrationOptions(List<CropHistoryEntryDto> history, String targetSeason) {
        List<RotationOptionDto> options = new ArrayList<>();

        String lastCrop = history != null && !history.isEmpty() ? 
                history.get(0).getCropName() : null;

        // Recommend legumes for nitrogen fixation
        for (String legume : LEGUME_CROPS) {
            if (lastCrop == null || !legume.equalsIgnoreCase(lastCrop)) {
                BigDecimal nitrogenFixation = new BigDecimal("90");
                BigDecimal soilHealth = new BigDecimal("85");
                BigDecimal pestManagement = calculatePestManagementScore(legume, lastCrop);

                RotationOptionDto option = RotationOptionDto.builder()
                        .id(generateId())
                        .cropSequence(lastCrop != null ? lastCrop + " → " + legume : legume)
                        .description("Legume integration for biological nitrogen fixation")
                        .soilHealthBenefit(soilHealth)
                        .climateResilience(new BigDecimal("75"))
                        .economicViability(new BigDecimal("70"))
                        .nutrientCyclingScore(nitrogenFixation)
                        .pestManagementScore(pestManagement)
                        .waterUsageScore(new BigDecimal("65"))
                        .overallBenefitScore(calculateOverall(nitrogenFixation, soilHealth, 
                                new BigDecimal("70"), new BigDecimal("75"), new BigDecimal("65")))
                        .benefits(Arrays.asList(
                                "Biological nitrogen fixation (40-60 kg N/ha)",
                                "Improves soil organic matter",
                                "Breaks pest and disease cycles",
                                "Reduces fertilizer requirements for subsequent crops"
                        ))
                        .considerations(Arrays.asList(
                                "Requires proper rhizobium inoculation",
                                "Market price fluctuations possible",
                                "May need additional phosphorus for nodulation"
                        ))
                        .residueManagementRecommendation("Incorporate crop residues or use as green manure")
                        .organicMatterImpact("High - adds organic matter and improves soil structure")
                        .build();

                options.add(option);
            }
        }

        return options;
    }

    /**
     * Generate rice-based system diversification options.
     * Requirement 3.5
     */
    private List<RotationOptionDto> generateRiceDiversificationOptions(List<CropHistoryEntryDto> history, String targetSeason) {
        List<RotationOptionDto> options = new ArrayList<>();

        String lastRiceCrop = findLastRiceCrop(history);

        // Recommend diversification crops for rice systems
        for (String crop : RICE_DIVERSIFICATION_CROPS) {
            if (!isRiceCrop(crop)) {
                BigDecimal soilHealth = new BigDecimal("80");
                BigDecimal pestManagement = calculatePestManagementScore(crop, lastRiceCrop);
                BigDecimal waterUsage = calculateWaterUsageScore(crop);

                RotationOptionDto option = RotationOptionDto.builder()
                        .id(generateId())
                        .cropSequence(lastRiceCrop + " → " + crop)
                        .description("Rice-based system diversification to leverage residual moisture")
                        .soilHealthBenefit(soilHealth)
                        .climateResilience(new BigDecimal("78"))
                        .economicViability(new BigDecimal("75"))
                        .nutrientCyclingScore(new BigDecimal("72"))
                        .pestManagementScore(pestManagement)
                        .waterUsageScore(waterUsage)
                        .overallBenefitScore(calculateOverall(
                                new BigDecimal("72"), soilHealth, new BigDecimal("75"), 
                                new BigDecimal("78"), waterUsage))
                        .benefits(Arrays.asList(
                                "Utilizes residual soil moisture after rice harvest",
                                "Breaks rice-specific pest and disease cycles",
                                "Diversifies income sources",
                                "Improves soil health through different root systems"
                        ))
                        .considerations(Arrays.asList(
                                "Timing critical - sow before soil dries completely",
                                "May require minimal irrigation if moisture insufficient",
                                "Consider market demand before selection"
                        ))
                        .residueManagementRecommendation("Manage rice residues properly to avoid pest habitat")
                        .organicMatterImpact("Moderate - varies by crop choice")
                        .build();

                options.add(option);
            }
        }

        return options;
    }

    /**
     * Generate intercropping and relay cropping suggestions.
     * Requirement 3.6
     */
    private List<RotationOptionDto> generateIntercroppingOptions(List<CropHistoryEntryDto> history, String targetSeason) {
        List<RotationOptionDto> options = new ArrayList<>();

        String lastCrop = history != null && !history.isEmpty() ? 
                history.get(0).getCropName() : null;

        // Generate relay cropping options (paira/utera cropping)
        if (lastCrop != null && RELAY_CROP_PAIRS.containsKey(lastCrop)) {
            for (String relayCrop : RELAY_CROP_PAIRS.get(lastCrop)) {
                RotationOptionDto option = createRelayCroppingOption(lastCrop, relayCrop, targetSeason);
                options.add(option);
            }
        }

        // Generate general intercropping options
        if (lastCrop != null) {
            List<String> intercropOptions = getIntercropOptions(lastCrop);
            for (String intercrop : intercropOptions) {
                RotationOptionDto option = createIntercroppingOption(lastCrop, intercrop, targetSeason);
                options.add(option);
            }
        }

        return options;
    }

    /**
     * Assess pest and disease carryover risk between crop cycles.
     * Requirement 3.7
     */
    private List<String> assessPestDiseaseCarryoverRisk(List<CropHistoryEntryDto> history) {
        List<String> risks = new ArrayList<>();

        if (history == null || history.isEmpty()) {
            return risks;
        }

        // Check for consecutive same-family crops
        for (int i = 0; i < history.size() - 1; i++) {
            String crop1 = history.get(i).getCropName();
            String crop2 = history.get(i + 1).getCropName();

            CropFamily family1 = CropFamily.getFamilyForCrop(crop1);
            CropFamily family2 = CropFamily.getFamilyForCrop(crop2);

            if (family1 != null && family1.equals(family2)) {
                Set<CropFamily> riskyFamilies = PEST_CARRYOVER_RISK.get(family1);
                if (riskyFamilies != null && riskyFamilies.contains(family1)) {
                    risks.add(String.format("High pest carryover risk: Consecutive %s crops may increase %s pest pressure. " +
                            "Consider rotating to a different crop family.", 
                            family1.getFamilyName(), family1.getFamilyName()));
                }
            }
        }

        // Check for specific crop pest risks (for the most recent crop)
        String lastCrop = history.get(0).getCropName();
        if (CROP_PEST_RISK.containsKey(lastCrop)) {
            Set<String> pests = CROP_PEST_RISK.get(lastCrop);
            if (pests != null && !pests.isEmpty()) {
                risks.add(String.format("%s may carry over pests/diseases: %s. " +
                        "Consider crop rotation or pest management measures.",
                        lastCrop, String.join(", ", pests)));
            }
        }

        return risks;
    }

    /**
     * Calculate overall pest risk level based on crop history.
     */
    private String calculateOverallPestRiskLevel(List<CropHistoryEntryDto> history) {
        if (history == null || history.isEmpty()) {
            return "LOW";
        }

        int consecutiveSameFamily = 0;
        int consecutiveHighRiskCrops = 0;

        for (int i = 0; i < history.size(); i++) {
            String crop = history.get(i).getCropName();
            boolean isHighRisk = CROP_PEST_RISK.containsKey(crop);

            if (i < history.size() - 1) {
                String nextCrop = history.get(i + 1).getCropName();
                CropFamily family1 = CropFamily.getFamilyForCrop(crop);
                CropFamily family2 = CropFamily.getFamilyForCrop(nextCrop);
                
                // Check for consecutive same family
                if (family1 != null && family1.equals(family2)) {
                    consecutiveSameFamily++;
                }
                
                // Check for consecutive high-risk crops
                if (isHighRisk && CROP_PEST_RISK.containsKey(nextCrop)) {
                    consecutiveHighRiskCrops++;
                }
            }
        }

        // For diverse rotations (no consecutive same family), risk should be LOW
        // even if there are some high-risk crops, as long as they're not consecutive
        if (consecutiveSameFamily == 0) {
            return "LOW";
        } else if (consecutiveSameFamily >= 2) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    /**
     * Generate general recommendations based on analysis.
     */
    private List<String> generateGeneralRecommendations(List<CropHistoryEntryDto> history, 
            boolean hasRiceBasedSystem, boolean hasConsecutiveMonoculture) {
        List<String> recommendations = new ArrayList<>();

        if (hasConsecutiveMonoculture) {
            recommendations.add("Consider rotating to a different crop family to break pest and disease cycles.");
        }

        if (hasRiceBasedSystem) {
            recommendations.add("For rice-based systems, consider green manuring with Sesbania or Crotalaria before next rice crop.");
            recommendations.add("Alternate rice with pulses or oilseeds to improve soil health and reduce fertilizer requirements.");
        }

        recommendations.add("Incorporate crop residues to increase organic matter content.");
        recommendations.add("Consider soil testing before finalizing rotation plan.");

        return recommendations;
    }

    // Helper methods

    private boolean hasRiceBasedSystem(List<CropHistoryEntryDto> history) {
        if (history == null) return false;
        return history.stream().anyMatch(entry -> isRiceCrop(entry.getCropName()));
    }

    private boolean isRiceCrop(String cropName) {
        return cropName != null && 
                (cropName.equalsIgnoreCase("Rice") || cropName.equalsIgnoreCase("Paddy"));
    }

    private String findLastRiceCrop(List<CropHistoryEntryDto> history) {
        if (history == null) return null;
        return history.stream()
                .filter(entry -> isRiceCrop(entry.getCropName()))
                .map(CropHistoryEntryDto::getCropName)
                .findFirst()
                .orElse(null);
    }

    private boolean hasConsecutiveMonoculture(List<CropHistoryEntryDto> history, int threshold) {
        if (history == null || history.size() < threshold) return false;

        for (int i = 0; i <= history.size() - threshold; i++) {
            boolean sameFamily = true;
            CropFamily firstFamily = CropFamily.getFamilyForCrop(history.get(i).getCropName());
            for (int j = i + 1; j < i + threshold; j++) {
                CropFamily currentFamily = CropFamily.getFamilyForCrop(history.get(j).getCropName());
                if (firstFamily == null || !firstFamily.equals(currentFamily)) {
                    sameFamily = false;
                    break;
                }
            }
            if (sameFamily) return true;
        }
        return false;
    }

    private boolean shouldRecommendLegumes(List<CropHistoryEntryDto> history) {
        if (history == null || history.isEmpty()) return true;
        String lastCrop = history.get(0).getCropName();
        CropFamily lastFamily = CropFamily.getFamilyForCrop(lastCrop);
        return lastFamily != CropFamily.LEGUMES;
    }

    private List<String> getDeepRootedCropsExcludingFamily(CropFamily excludeFamily) {
        return DEEP_ROOTED_CROPS.stream()
                .filter(crop -> {
                    CropFamily family = CropFamily.getFamilyForCrop(crop);
                    return family != excludeFamily;
                })
                .collect(Collectors.toList());
    }

    private List<String> getShallowRootedCropsExcludingFamily(CropFamily excludeFamily) {
        return SHALLOW_ROOTED_CROPS.stream()
                .filter(crop -> {
                    CropFamily family = CropFamily.getFamilyForCrop(crop);
                    return family != excludeFamily;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculateNutrientCyclingScore(String newCrop, String lastCrop) {
        if (lastCrop == null) return new BigDecimal("75");
        
        CropFamily.RootDepth lastDepth = CropFamily.getRootDepthForCrop(lastCrop);
        CropFamily.RootDepth newDepth = CropFamily.getRootDepthForCrop(newCrop);
        
        // Score higher for alternating depths
        if (!lastDepth.equals(newDepth)) {
            return new BigDecimal("85");
        }
        return new BigDecimal("65");
    }

    private BigDecimal calculatePestManagementScore(String newCrop, String lastCrop) {
        if (lastCrop == null) return new BigDecimal("70");
        
        CropFamily newFamily = CropFamily.getFamilyForCrop(newCrop);
        CropFamily lastFamily = CropFamily.getFamilyForCrop(lastCrop);
        
        if (newFamily == null || lastFamily == null) return new BigDecimal("70");
        
        // Higher score for different families (breaks pest cycles)
        if (!newFamily.equals(lastFamily)) {
            return new BigDecimal("85");
        }
        return new BigDecimal("50");
    }

    private BigDecimal calculateWaterUsageScore(String crop) {
        CropFamily.RootDepth depth = CropFamily.getRootDepthForCrop(crop);
        switch (depth) {
            case DEEP:
                return new BigDecimal("70");
            case MEDIUM:
                return new BigDecimal("75");
            case SHALLOW:
                return new BigDecimal("80");
            default:
                return new BigDecimal("75");
        }
    }

    private RotationOptionDto createNutrientCyclingOption(String newCrop, String lastCrop, 
            String description, BigDecimal nutrientScore, String targetSeason) {
        CropFamily.RootDepth depth = CropFamily.getRootDepthForCrop(newCrop);
        String depthDescription = depth == CropFamily.RootDepth.DEEP ? 
                "deep-rooted (nutrient cycling from deeper layers)" : 
                "shallow-rooted (topsoil nutrient utilization)";

        return RotationOptionDto.builder()
                .id(generateId())
                .cropSequence(lastCrop != null ? lastCrop + " → " + newCrop : newCrop)
                .description(description + ": " + newCrop + " (" + depthDescription + ")")
                .soilHealthBenefit(nutrientScore)
                .climateResilience(new BigDecimal("75"))
                .economicViability(new BigDecimal("70"))
                .nutrientCyclingScore(nutrientScore)
                .pestManagementScore(calculatePestManagementScore(newCrop, lastCrop))
                .waterUsageScore(calculateWaterUsageScore(newCrop))
                .overallBenefitScore(calculateOverall(nutrientScore, nutrientScore, 
                        new BigDecimal("70"), new BigDecimal("75"), calculateWaterUsageScore(newCrop)))
                .benefits(Arrays.asList(
                        "Alternates root depth for better nutrient utilization",
                        "Improves soil structure through different root systems",
                        "Reduces nutrient depletion in specific soil layers"
                ))
                .considerations(Arrays.asList(
                        "Consider market demand for the recommended crop",
                        "Ensure crop is suitable for the growing season",
                        "Check water requirements match available resources"
                ))
                .residueManagementRecommendation("Incorporate residues to enhance organic matter")
                .organicMatterImpact("Moderate - depends on biomass production")
                .build();
    }

    private RotationOptionDto createBalancedRotationOption(String lastCrop, String targetSeason) {
        String sequence = lastCrop != null ? lastCrop + " → Sunflower → Cabbage → Greengram" : 
                "Sunflower → Cabbage → Greengram";

        return RotationOptionDto.builder()
                .id(generateId())
                .cropSequence(sequence)
                .description("Balanced 3-year rotation for optimal nutrient cycling")
                .soilHealthBenefit(new BigDecimal("90"))
                .climateResilience(new BigDecimal("85"))
                .economicViability(new BigDecimal("80"))
                .nutrientCyclingScore(new BigDecimal("95"))
                .pestManagementScore(new BigDecimal("85"))
                .waterUsageScore(new BigDecimal("75"))
                .overallBenefitScore(new BigDecimal("86"))
                .benefits(Arrays.asList(
                        "Deep-rooted (Sunflower) accesses nutrients from deeper soil layers",
                        "Shallow-rooted (Cabbage) utilizes topsoil nutrients efficiently",
                        "Legume (Greengram) fixes atmospheric nitrogen",
                        "Breaks pest and disease cycles effectively"
                ))
                .considerations(Arrays.asList(
                        "Requires planning across multiple seasons",
                        "Market timing important for each crop",
                        "Adjust based on local climate and soil conditions"
                ))
                .residueManagementRecommendation("Rotate residue management practices between crops")
                .organicMatterImpact("High - diverse root systems contribute to soil organic matter")
                .build();
    }

    private RotationOptionDto createRelayCroppingOption(String mainCrop, String relayCrop, String targetSeason) {
        String description = mainCrop.equalsIgnoreCase("Rice") || mainCrop.equalsIgnoreCase("Paddy") ?
                "Paira/Utera relay cropping: Sow " + relayCrop + " into maturing " + mainCrop :
                "Relay cropping: Sow " + relayCrop + " into maturing " + mainCrop;

        return RotationOptionDto.builder()
                .id(generateId())
                .cropSequence(mainCrop + " (relay with " + relayCrop + ")")
                .description(description)
                .soilHealthBenefit(new BigDecimal("85"))
                .climateResilience(new BigDecimal("80"))
                .economicViability(new BigDecimal("88"))
                .nutrientCyclingScore(new BigDecimal("78"))
                .pestManagementScore(new BigDecimal("82"))
                .waterUsageScore(new BigDecimal("90"))
                .overallBenefitScore(calculateOverall(
                        new BigDecimal("78"), new BigDecimal("85"), 
                        new BigDecimal("88"), new BigDecimal("80"), new BigDecimal("90")))
                .benefits(Arrays.asList(
                        "Utilizes residual soil moisture efficiently",
                        "Maximizes land productivity per season",
                        "Reduces weed competition",
                        relayCrop + " fixes nitrogen benefiting subsequent crops"
                ))
                .considerations(Arrays.asList(
                        "Timing critical - sow relay crop 2-3 weeks before main crop harvest",
                        "May require minimal additional inputs",
                        "Ensure relay crop is suitable for the climate"
                ))
                .residueManagementRecommendation("Manage main crop residues to avoid smothering relay crop")
                .organicMatterImpact("High - dual crop biomass increases organic input")
                .build();
    }

    private List<String> getIntercropOptions(String mainCrop) {
        Map<String, List<String>> intercropMap = new HashMap<>();
        intercropMap.put("Rice", Arrays.asList("Soybean", "Greengram", "Blackgram"));
        intercropMap.put("Maize", Arrays.asList("Cowpea", "Greengram", "Soybean", "Beans"));
        intercropMap.put("Wheat", Arrays.asList("Chickpea", "Lentil", "Mustard"));
        intercropMap.put("Cotton", Arrays.asList("Greengram", "Blackgram", "Soybean"));
        intercropMap.put("Sugarcane", Arrays.asList("Soybean", "Greengram", "Potato", "Onion"));

        return intercropMap.getOrDefault(mainCrop, Collections.emptyList());
    }

    private RotationOptionDto createIntercroppingOption(String mainCrop, String intercrop, String targetSeason) {
        return RotationOptionDto.builder()
                .id(generateId())
                .cropSequence(mainCrop + " + " + intercrop + " (intercropping)")
                .description("Intercrop " + intercrop + " with " + mainCrop + " for better resource utilization")
                .soilHealthBenefit(new BigDecimal("82"))
                .climateResilience(new BigDecimal("78"))
                .economicViability(new BigDecimal("85"))
                .nutrientCyclingScore(new BigDecimal("75"))
                .pestManagementScore(new BigDecimal("80"))
                .waterUsageScore(new BigDecimal("72"))
                .overallBenefitScore(calculateOverall(
                        new BigDecimal("75"), new BigDecimal("82"), 
                        new BigDecimal("85"), new BigDecimal("78"), new BigDecimal("72")))
                .benefits(Arrays.asList(
                        "Maximizes land use efficiency",
                        "Intercrop may fix nitrogen (if legume)",
                        "Reduces pest and disease incidence through diversity",
                        "Provides additional income source"
                ))
                .considerations(Arrays.asList(
                        "Requires careful crop combination selection",
                        "May need adjustment of input rates",
                        "Harvest timing may be more complex"
                ))
                .residueManagementRecommendation("Manage residues of both crops appropriately")
                .organicMatterImpact("Moderate to High - depends on crop combination")
                .build();
    }

    private BigDecimal calculateOverall(BigDecimal nutrient, BigDecimal soilHealth, 
            BigDecimal economic, BigDecimal climate, BigDecimal water) {
        return nutrient.add(soilHealth).add(economic).add(climate).add(water)
                .divide(new BigDecimal("5"), 2, java.math.RoundingMode.HALF_UP);
    }

    private long generateId() {
        return System.currentTimeMillis() % 10000;
    }
}