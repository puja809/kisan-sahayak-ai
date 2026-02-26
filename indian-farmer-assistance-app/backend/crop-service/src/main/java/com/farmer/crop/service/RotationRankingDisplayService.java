package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.enums.CropFamily;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for rotation ranking and display logic.
 * 
 * Handles ranking rotation options by soil health benefit, climate resilience,
 * and economic viability. Generates season-wise planting schedules (Kharif, Rabi, Zaid).
 * Includes residue management recommendations and default patterns for farmers
 * with no crop history.
 * 
 * Requirements: 3.8, 3.9, 3.10, 3.11
 */
@Service
public class RotationRankingDisplayService {

    // Season constants for Indian agricultural calendar
    private static final String SEASON_KHARIF = "KHARIF";
    private static final String SEASON_RABI = "RABI";
    private static final String SEASON_ZAID = "ZAID";

    // Default planting months for each season
    private static final Map<String, String> SEASON_PLANTING_MONTHS = Map.of(
            SEASON_KHARIF, "June - July",
            SEASON_RABI, "October - November",
            SEASON_ZAID, "February - March"
    );

    // Default harvest months for each season
    private static final Map<String, String> SEASON_HARVEST_MONTHS = Map.of(
            SEASON_KHARIF, "September - October",
            SEASON_RABI, "March - April",
            SEASON_ZAID, "May - June"
    );

    // Residue management recommendations by crop family
    private static final Map<CropFamily, String> RESIDUE_MANAGEMENT_RECOMMENDATIONS;
    static {
        Map<CropFamily, String> map = new HashMap<>();
        map.put(CropFamily.CEREALS, "Incorporate straw into soil or use as mulch. " +
                "Rice straw can be used for mushroom cultivation. " +
                "Wheat stubble should be chopped and incorporated.");
        map.put(CropFamily.LEGUMES, "Legume residues are nitrogen-rich. " +
                "Incorporate residues after partial decomposition. " +
                "Can be used as green manure for next crop.");
        map.put(CropFamily.BRASSICAS, "Heavy residue from brassicas. " +
                "Chop and incorporate with adequate moisture. " +
                "May require additional nitrogen for decomposition.");
        map.put(CropFamily.SOLANACEOUS, "Moderate residue. Incorporate into soil. " +
                "Remove diseased plant material to prevent carryover.");
        map.put(CropFamily.CUCURBITS, "Rapid decomposition. Incorporate residues. " +
                "Vine material provides good organic matter.");
        map.put(CropFamily.ROOT_TUBERS, "Tuber residues decompose quickly. " +
                "Incorporate tops into soil. Leave tubers for soil structure.");
        map.put(CropFamily.FIBER, "Cotton stalks should be destroyed or incorporated. " +
                "Avoid leaving residue that harbors pests.");
        map.put(CropFamily.OILSEEDS, "Oilseed residues are carbon-rich. " +
                "Incorporate with care to avoid nitrogen immobilization.");
        map.put(CropFamily.SPICES, "Minimal residue. Incorporate any plant material. " +
                "Some spices (turmeric, ginger) can be composted.");
        map.put(CropFamily.FRUITS, "Perennial crops have minimal seasonal residue. " +
                "Prunings can be chipped and used as mulch.");
        map.put(CropFamily.GREEN_MANURE, "Ideally suited for incorporation. " +
                "Incorporate at flowering stage for maximum nitrogen.");
        map.put(CropFamily.FODDER, "Fodder residues decompose quickly. " +
                "Incorporate or use as livestock bedding first.");
        RESIDUE_MANAGEMENT_RECOMMENDATIONS = Collections.unmodifiableMap(map);
    }

    // Organic matter impact projections by crop family
    private static final Map<CropFamily, String> ORGANIC_MATTER_IMPACTS;
    static {
        Map<CropFamily, String> map = new HashMap<>();
        map.put(CropFamily.CEREALS, "High organic matter addition. " +
                "Rice straw adds silica; wheat straw adds carbon. " +
                "Expected OM increase: 0.3-0.5% per season.");
        map.put(CropFamily.LEGUMES, "Nitrogen fixation plus organic matter. " +
                "Expected OM increase: 0.4-0.6% per season. " +
                "Improves soil nitrogen status.");
        map.put(CropFamily.BRASSICAS, "Moderate organic matter. " +
                "Fast decomposition. Expected OM increase: 0.2-0.4%.");
        map.put(CropFamily.SOLANACEOUS, "Moderate organic matter addition. " +
                "Expected OM increase: 0.2-0.3%.");
        map.put(CropFamily.CUCURBITS, "Good organic matter from vines. " +
                "Expected OM increase: 0.3-0.5%.");
        map.put(CropFamily.ROOT_TUBERS, "Moderate from tops, improves soil structure. " +
                "Expected OM increase: 0.2-0.4%.");
        map.put(CropFamily.FIBER, "Variable organic matter. " +
                "Cotton adds minimal OM. Expected increase: 0.1-0.2%.");
        map.put(CropFamily.OILSEEDS, "Moderate organic matter. " +
                "Expected OM increase: 0.2-0.4%.");
        map.put(CropFamily.SPICES, "Minimal organic matter addition. " +
                "Expected OM increase: 0.1-0.2%.");
        map.put(CropFamily.FRUITS, "Minimal seasonal contribution. " +
                "Prunings add small amounts of OM.");
        map.put(CropFamily.GREEN_MANURE, "Excellent organic matter addition. " +
                "Expected OM increase: 0.5-1.0% per season.");
        map.put(CropFamily.FODDER, "Good organic matter from multiple harvests. " +
                "Expected OM increase: 0.4-0.6%.");
        ORGANIC_MATTER_IMPACTS = Collections.unmodifiableMap(map);
    }

    // Default rotation patterns by agro-ecological zone
    private static final Map<String, List<List<String>>> DEFAULT_ROTATION_PATTERNS;
    static {
        Map<String, List<List<String>>> map = new HashMap<>();
        map.put("Trans Himalayan Zone", List.of(
                List.of("Wheat", "Mustard", "Peas"),
                List.of("Wheat", "Potato", "Peas"),
                List.of("Barley", "Mustard", "Greengram")
        ));
        map.put("Himalayan Zone", List.of(
                List.of("Rice", "Wheat", "Greengram"),
                List.of("Maize", "Wheat", "Lentil"),
                List.of("Rice", "Peas", "Sesame")
        ));
        map.put("Indo-Gangetic Plains", List.of(
                List.of("Rice", "Wheat", "Greengram"),
                List.of("Rice", "Wheat", "Mustard"),
                List.of("Maize", "Wheat", "Lentil"),
                List.of("Rice", "Potato", "Cowpea")
        ));
        map.put("Eastern Plateau and Hills", List.of(
                List.of("Rice", "Lentil", "Sesame"),
                List.of("Maize", "Lentil", "Greengram"),
                List.of("Rice", "Peas", "Sunflower")
        ));
        map.put("Central Plateau and Hills", List.of(
                List.of("Soybean", "Wheat", "Chickpea"),
                List.of("Sorghum", "Wheat", "Mustard"),
                List.of("Rice", "Chickpea", "Sesame")
        ));
        map.put("Western Plateau and Hills", List.of(
                List.of("Cotton", "Wheat", "Greengram"),
                List.of("Sorghum", "Wheat", "Chickpea"),
                List.of("Soybean", "Wheat", "Mustard")
        ));
        map.put("Southern Plateau and Hills", List.of(
                List.of("Rice", "Groundnut", "Sunflower"),
                List.of("Maize", "Groundnut", "Sunflower"),
                List.of("Cotton", "Groundnut", "Sesame")
        ));
        map.put("East Coast Plains and Hills", List.of(
                List.of("Rice", "Groundnut", "Sesame"),
                List.of("Rice", "Blackgram", "Sunflower"),
                List.of("Maize", "Groundnut", "Greengram")
        ));
        map.put("West Coast Plains and Hills", List.of(
                List.of("Rice", "Groundnut", "Sesame"),
                List.of("Coconut", "Banana", "Pepper"),
                List.of("Rice", "Blackgram", "Sunflower")
        ));
        map.put("Gujarat Plains and Hills", List.of(
                List.of("Cotton", "Wheat", "Mustard"),
                List.of("Groundnut", "Wheat", "Sesame"),
                List.of("Pearl Millet", "Wheat", "Chickpea")
        ));
        map.put("Western Dry Region", List.of(
                List.of("Pearl Millet", "Wheat", "Mustard"),
                List.of("Sorghum", "Wheat", "Chickpea"),
                List.of("Pearl Millet", "Cluster Bean", "Sesame")
        ));
        map.put("Island Region", List.of(
                List.of("Rice", "Groundnut", "Sesame"),
                List.of("Rice", "Blackgram", "Sunflower"),
                List.of("Coconut", "Banana", "Taro")
        ));
        DEFAULT_ROTATION_PATTERNS = Collections.unmodifiableMap(map);
    }

    // Climate resilience scores by crop
    private static final Map<String, Double> CLIMATE_RESILIENCE_SCORES;
    static {
        Map<String, Double> map = new HashMap<>();
        map.put("Rice", 75.0);
        map.put("Wheat", 65.0);
        map.put("Maize", 80.0);
        map.put("Sorghum", 85.0);
        map.put("Pearl Millet", 90.0);
        map.put("Finger Millet", 88.0);
        map.put("Barley", 70.0);
        map.put("Greengram", 82.0);
        map.put("Blackgram", 80.0);
        map.put("Redgram", 78.0);
        map.put("Chickpea", 75.0);
        map.put("Lentil", 72.0);
        map.put("Peas", 70.0);
        map.put("Groundnut", 77.0);
        map.put("Soybean", 78.0);
        map.put("Mustard", 76.0);
        map.put("Sunflower", 80.0);
        map.put("Sesame", 85.0);
        map.put("Cotton", 72.0);
        map.put("Sugarcane", 68.0);
        map.put("Potato", 65.0);
        map.put("Tomato", 70.0);
        map.put("Onion", 68.0);
        map.put("Cabbage", 65.0);
        map.put("Cauliflower", 64.0);
        map.put("Carrot", 70.0);
        map.put("Banana", 62.0);
        map.put("Mango", 60.0);
        map.put("Citrus", 65.0);
        map.put("Turmeric", 72.0);
        map.put("Ginger", 70.0);
        CLIMATE_RESILIENCE_SCORES = Collections.unmodifiableMap(map);
    }

    // Economic viability scores by crop
    private static final Map<String, Double> ECONOMIC_VIABILITY_SCORES;
    static {
        Map<String, Double> map = new HashMap<>();
        map.put("Rice", 85.0);
        map.put("Wheat", 80.0);
        map.put("Maize", 82.0);
        map.put("Sorghum", 70.0);
        map.put("Pearl Millet", 65.0);
        map.put("Finger Millet", 72.0);
        map.put("Barley", 68.0);
        map.put("Greengram", 78.0);
        map.put("Blackgram", 76.0);
        map.put("Redgram", 80.0);
        map.put("Chickpea", 75.0);
        map.put("Lentil", 73.0);
        map.put("Peas", 77.0);
        map.put("Groundnut", 82.0);
        map.put("Soybean", 78.0);
        map.put("Mustard", 76.0);
        map.put("Sunflower", 79.0);
        map.put("Sesame", 75.0);
        map.put("Cotton", 88.0);
        map.put("Sugarcane", 85.0);
        map.put("Potato", 84.0);
        map.put("Tomato", 86.0);
        map.put("Onion", 83.0);
        map.put("Cabbage", 75.0);
        map.put("Cauliflower", 76.0);
        map.put("Carrot", 78.0);
        map.put("Banana", 90.0);
        map.put("Mango", 92.0);
        map.put("Citrus", 85.0);
        map.put("Turmeric", 88.0);
        map.put("Ginger", 90.0);
        ECONOMIC_VIABILITY_SCORES = Collections.unmodifiableMap(map);
    }

    /**
     * Ranks rotation options by overall benefit score in descending order.
     * Overall score is calculated from soil health benefit, climate resilience, and economic viability.
     * 
     * Requirement 3.9: WHEN multiple rotation options exist, THE Application SHALL 
     * rank them by soil health benefit, climate resilience, and economic viability
     * 
     * @param options List of rotation options to rank
     * @return Sorted list in descending order by overall benefit score
     */
    public List<RotationOptionDto> rankRotationOptionsByOverallBenefit(List<RotationOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return options;
        }

        return options.stream()
                .sorted(Comparator.comparing(
                        this::calculateOverallBenefitScore,
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Ranks rotation options by soil health benefit in descending order.
     * 
     * @param options List of rotation options to rank
     * @return Sorted list in descending order by soil health benefit
     */
    public List<RotationOptionDto> rankBySoilHealthBenefit(List<RotationOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return options;
        }

        return options.stream()
                .sorted(Comparator.comparing(
                        RotationOptionDto::getSoilHealthBenefit,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Ranks rotation options by climate resilience in descending order.
     * 
     * @param options List of rotation options to rank
     * @return Sorted list in descending order by climate resilience
     */
    public List<RotationOptionDto> rankByClimateResilience(List<RotationOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return options;
        }

        return options.stream()
                .sorted(Comparator.comparing(
                        RotationOptionDto::getClimateResilience,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Ranks rotation options by economic viability in descending order.
     * 
     * @param options List of rotation options to rank
     * @return Sorted list in descending order by economic viability
     */
    public List<RotationOptionDto> rankByEconomicViability(List<RotationOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return options;
        }

        return options.stream()
                .sorted(Comparator.comparing(
                        RotationOptionDto::getEconomicViability,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Calculates overall benefit score from component scores.
     * Uses weighted average: 40% soil health, 30% climate resilience, 30% economic viability.
     * 
     * @param option The rotation option to calculate for
     * @return Overall benefit score (0-100)
     */
    public Double calculateOverallBenefitScore(RotationOptionDto option) {
        if (option == null) {
            return 0.0;
        }

        Double soilHealth = option.getSoilHealthBenefit() != null ?
                option.getSoilHealthBenefit() : 0.0;
        Double climate = option.getClimateResilience() != null ?
                option.getClimateResilience() : 0.0;
        Double economic = option.getEconomicViability() != null ?
                option.getEconomicViability() : 0.0;

        // Weighted average: 40% soil health, 30% climate resilience, 30% economic viability
        return soilHealth*(0.40)
                +(climate*(0.30))
                +(economic*(0.30))
                ;
    }

    /**
     * Generates season-wise planting schedules for rotation options.
     * 
     * Requirement 3.10: WHEN displaying rotation plans, THE Application SHALL 
     * show season-wise planting schedules (Kharif, Rabi, Zaid) with expected benefits
     * 
     * @param options List of rotation options
     * @return List of options with populated season-wise schedules
     */
    public List<RotationOptionDto> generateSeasonWiseSchedules(List<RotationOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return options;
        }

        return options.stream()
                .map(this::populateSeasonSchedule)
                .collect(Collectors.toList());
    }

    /**
     * Populates season schedule for a single rotation option.
     */
    private RotationOptionDto populateSeasonSchedule(RotationOptionDto option) {
        if (option == null) {
            return option;
        }

        // Parse crop sequence and assign to seasons
        String cropSequence = option.getCropSequence();
        if (cropSequence != null && !cropSequence.isEmpty()) {
            String[] crops = cropSequence.split(" -> ");
            
            if (crops.length >= 1) {
                option.setKharifCrops(crops[0]);
            }
            if (crops.length >= 2) {
                option.setRabiCrops(crops[1]);
            }
            if (crops.length >= 3) {
                option.setZaidCrops(crops[2]);
            }
        }

        return option;
    }

    /**
     * Adds residue management recommendations to rotation options.
     * 
     * Requirement 3.8: WHEN displaying rotation recommendations, THE Application SHALL 
     * include residue management recommendations and organic matter impact projections
     * 
     * @param options List of rotation options
     * @return List of options with residue management recommendations
     */
    public List<RotationOptionDto> addResidueManagementRecommendations(List<RotationOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return options;
        }

        return options.stream()
                .map(this::populateResidueManagement)
                .collect(Collectors.toList());
    }

    /**
     * Populates residue management recommendation for a single option.
     */
    private RotationOptionDto populateResidueManagement(RotationOptionDto option) {
        if (option == null) {
            return option;
        }

        // Get the main crop from the sequence
        String cropSequence = option.getCropSequence();
        if (cropSequence != null && !cropSequence.isEmpty()) {
            String[] crops = cropSequence.split(" -> ");
            if (crops.length > 0) {
                String mainCrop = crops[0];
                CropFamily family = CropFamily.getFamilyForCrop(mainCrop);
                
                if (family != null) {
                    // Set residue management recommendation
                    String residueRec = RESIDUE_MANAGEMENT_RECOMMENDATIONS.get(family);
                    if (residueRec != null) {
                        option.setResidueManagementRecommendation(residueRec);
                    }

                    // Set organic matter impact projection
                    String omImpact = ORGANIC_MATTER_IMPACTS.get(family);
                    if (omImpact != null) {
                        option.setOrganicMatterImpact(omImpact);
                    }
                }
            }
        }

        // If no family found, provide default recommendation
        if (option.getResidueManagementRecommendation() == null) {
            option.setResidueManagementRecommendation(
                    "Incorporate crop residues into soil. " +
                    "Ensure adequate moisture for decomposition. " +
                    "Consider composting if residues are heavy.");
        }
        if (option.getOrganicMatterImpact() == null) {
            option.setOrganicMatterImpact(
                    "Expected organic matter increase: 0.2-0.4% per season. " +
                    "Actual impact depends on residue quantity and decomposition conditions.");
        }

        return option;
    }

    /**
     * Provides default rotation patterns for farmers with no crop history.
     * 
     * Requirement 3.11: WHEN the farmer has no crop history, THE Application SHALL 
     * provide default rotation patterns for their agro-ecological zone
     * 
     * @param agroEcologicalZone The farmer's agro-ecological zone
     * @return List of default rotation patterns for the zone
     */
    public List<RotationOptionDto> getDefaultRotationPatterns(String agroEcologicalZone) {
        List<List<String>> patterns = DEFAULT_ROTATION_PATTERNS.get(agroEcologicalZone);
        
        if (patterns == null || patterns.isEmpty()) {
            // Use Indo-Gangetic Plains as default fallback
            patterns = DEFAULT_ROTATION_PATTERNS.get("Indo-Gangetic Plains");
        }

        if (patterns == null) {
            patterns = List.of(
                    List.of("Rice", "Wheat", "Greengram"),
                    List.of("Maize", "Wheat", "Lentil"),
                    List.of("Rice", "Peas", "Sesame")
            );
        }

        return patterns.stream()
                .map(this::createDefaultRotationOption)
                .collect(Collectors.toList());
    }

    /**
     * Creates a rotation option from a default pattern.
     */
    private RotationOptionDto createDefaultRotationOption(List<String> cropSequence) {
        String sequence = String.join(" -> ", cropSequence);
        
        // Calculate scores for the first crop in sequence
        String mainCrop = cropSequence.get(0);
        Double soilHealth = calculateSoilHealthBenefit(cropSequence);
        Double climateResilience = getClimateResilienceScore(mainCrop);
        Double economicViability = getEconomicViabilityScore(mainCrop);
        Double overallScore = calculateOverallBenefitScoreFromComponents(
                soilHealth, climateResilience, economicViability);

        // Get residue management for main crop
        CropFamily family = CropFamily.getFamilyForCrop(mainCrop);
        String residueRec = family != null ? 
                RESIDUE_MANAGEMENT_RECOMMENDATIONS.get(family) : null;
        String omImpact = family != null ? 
                ORGANIC_MATTER_IMPACTS.get(family) : null;

        return RotationOptionDto.builder()
                .id(generateId())
                .cropSequence(sequence)
                .description("Default rotation pattern for balanced nutrition and income")
                .soilHealthBenefit(soilHealth)
                .climateResilience(climateResilience)
                .economicViability(economicViability)
                .overallBenefitScore(overallScore)
                .kharifCrops(cropSequence.size() > 0 ? cropSequence.get(0) : null)
                .rabiCrops(cropSequence.size() > 1 ? cropSequence.get(1) : null)
                .zaidCrops(cropSequence.size() > 2 ? cropSequence.get(2) : null)
                .benefits(generateBenefitsList(cropSequence))
                .considerations(generateConsiderationsList(cropSequence))
                .residueManagementRecommendation(residueRec != null ? residueRec :
                        "Incorporate residues into soil for organic matter addition.")
                .organicMatterImpact(omImpact != null ? omImpact :
                        "Expected organic matter increase: 0.3-0.5% per season.")
                .build();
    }

    /**
     * Calculates soil health benefit score for a rotation sequence.
     */
    private Double calculateSoilHealthBenefit(List<String> cropSequence) {
        if (cropSequence == null || cropSequence.isEmpty()) {
            return 50.0;
        }

        Double totalScore = 0.0;
        int legumeCount = 0;
        int deepRootedCount = 0;

        for (String crop : cropSequence) {
            CropFamily family = CropFamily.getFamilyForCrop(crop);
            if (family != null) {
                // Base score from family
                Double familyScore = switch (family) {
                    case LEGUMES -> 90.0; // Nitrogen fixation
                    case GREEN_MANURE -> 95.0; // Excellent for soil
                    case CEREALS -> 70.0; // Moderate
                    case BRASSICAS -> 65.0; // Heavy feeder
                    case OILSEEDS -> 72.0; // Moderate
                    case ROOT_TUBERS -> 75.0; // Soil structure
                    default -> 68.0;
                };
                totalScore = totalScore + familyScore;

                if (family == CropFamily.LEGUMES || family == CropFamily.GREEN_MANURE) {
                    legumeCount++;
                }
                if (family.getTypicalRootDepth() == CropFamily.RootDepth.DEEP) {
                    deepRootedCount++;
                }
            } else {
                totalScore = totalScore + 65.0;
            }
        }

        // Average score
        Double avgScore = totalScore / cropSequence.size();

        // Bonus for legume inclusion (nitrogen fixation)
        if (legumeCount > 0) {
            avgScore = avgScore + (5.0);
        }

        // Bonus for deep-rooted crops (nutrient cycling)
        if (deepRootedCount > 0) {
            avgScore = avgScore + (3.0);
        }

        return avgScore;
    }

    /**
     * Gets climate resilience score for a crop.
     */
    private Double getClimateResilienceScore(String crop) {
        if (crop == null) {
            return 65.0;
        }
        return CLIMATE_RESILIENCE_SCORES.getOrDefault(crop, 70.0);
    }

    /**
     * Gets economic viability score for a crop.
     */
    private Double getEconomicViabilityScore(String crop) {
        if (crop == null) {
            return 65.0;
        }
        return ECONOMIC_VIABILITY_SCORES.getOrDefault(crop, 70.0);
    }

    /**
     * Calculates overall benefit score from individual components.
     */
    private Double calculateOverallBenefitScoreFromComponents(
            Double soilHealth, Double climate, Double economic) {
        return soilHealth * (0.40)
                + (climate * (0.30))
                + (economic * (0.30))
                ;
    }

    /**
     * Generates benefits list for a rotation sequence.
     */
    private List<String> generateBenefitsList(List<String> cropSequence) {
        List<String> benefits = new ArrayList<>();

        boolean hasLegume = false;
        boolean hasCereal = false;
        boolean hasOilseed = false;

        for (String crop : cropSequence) {
            CropFamily family = CropFamily.getFamilyForCrop(crop);
            if (family != null) {
                if (family == CropFamily.LEGUMES) hasLegume = true;
                if (family == CropFamily.CEREALS) hasCereal = true;
                if (family == CropFamily.OILSEEDS) hasOilseed = true;
            }
        }

        if (hasLegume) {
            benefits.add("Biological nitrogen fixation improves soil fertility");
        }
        if (hasCereal && hasLegume) {
            benefits.add("Cereal-legume rotation provides balanced nutrition");
        }
        if (hasOilseed) {
            benefits.add("Oilseed break helps manage pest and disease cycles");
        }
        benefits.add("Diverse crop sequence reduces risk of total crop failure");
        benefits.add("Multiple income sources throughout the year");

        return benefits;
    }

    /**
     * Generates considerations list for a rotation sequence.
     */
    private List<String> generateConsiderationsList(List<String> cropSequence) {
        List<String> considerations = new ArrayList<>();
        considerations.add("Adjust input costs based on each crop's requirements");
        considerations.add("Ensure irrigation availability for all seasons");
        considerations.add("Plan marketing strategy for each crop");
        considerations.add("Consider labor requirements for each crop");
        return considerations;
    }

    /**
     * Generates a unique ID for rotation options.
     */
    private long generateId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }

    /**
     * Gets planting schedule information for a season.
     * 
     * @param season The agricultural season (KHARIF, RABI, ZAID)
     * @return Map containing planting and harvest timing information
     */
    public Map<String, String> getSeasonScheduleInfo(String season) {
        Map<String, String> schedule = new HashMap<>();
        schedule.put("plantingMonths", SEASON_PLANTING_MONTHS.getOrDefault(season, "Varies"));
        schedule.put("harvestMonths", SEASON_HARVEST_MONTHS.getOrDefault(season, "Varies"));
        return schedule;
    }

    /**
     * Checks if a farmer has crop history.
     * 
     * @param cropHistory List of crop history entries
     * @return true if farmer has no crop history
     */
    public boolean hasNoCropHistory(List<CropHistoryEntryDto> cropHistory) {
        return cropHistory == null || cropHistory.isEmpty();
    }

    /**
     * Creates a complete rotation recommendation result with all display logic applied.
     * 
     * @param options List of rotation options
     * @param agroEcologicalZone Farmer's agro-ecological zone
     * @param hasHistory Whether farmer has crop history
     * @return Complete result with ranked options, schedules, and residue recommendations
     */
    public RotationRecommendationResultDto createCompleteRotationDisplay(
            List<RotationOptionDto> options,
            String agroEcologicalZone,
            boolean hasHistory) {
        
        RotationRecommendationResultDto result = new RotationRecommendationResultDto();
        
        if (options != null && !options.isEmpty()) {
            // Apply ranking by overall benefit
            List<RotationOptionDto> rankedOptions = rankRotationOptionsByOverallBenefit(options);
            
            // Add season-wise schedules
            rankedOptions = generateSeasonWiseSchedules(rankedOptions);
            
            // Add residue management recommendations
            rankedOptions = addResidueManagementRecommendations(rankedOptions);
            
            result.setOptions(rankedOptions);
        }
        
        // Always get default patterns for the zone
        List<RotationOptionDto> defaults = getDefaultRotationPatterns(agroEcologicalZone);
        result.setDefaultPatterns(defaults);
        
        // Use defaults as main options if no options provided
        if (result.getOptions() == null || result.getOptions().isEmpty()) {
            result.setOptions(defaults);
        }
        
        return result;
    }
}








