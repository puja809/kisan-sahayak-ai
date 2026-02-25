package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.enums.CropFamily;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for RotationRecommendationEngine.
 * 
 * Validates universal properties across all rotation recommendations.
 * 
 * Requirements: 3.3, 3.4, 3.5, 3.6, 3.7
 */
class RotationRecommendationPropertyTest {

    private final RotationRecommendationEngine engine = new RotationRecommendationEngine();

    private static final List<String> ALL_CROPS = Arrays.asList(
            "Rice", "Wheat", "Maize", "Cabbage", "Sunflower", "Greengram", 
            "Blackgram", "Redgram", "Chickpea", "Lentil", "Mustard", "Cotton",
            "Soybean", "Groundnut", "Sugarcane", "Tomato", "Potato"
    );

    private static final List<String> SEASONS = Arrays.asList("KHARIF", "RABI", "ZAID");

    /**
     * Property: All rotation options should have valid scores (0-100).
     * Validates: Requirements 3.3, 3.4, 3.5, 3.6, 3.7
     */
    @Property
    @Label("All rotation options should have valid scores between 0 and 100")
    void allOptionsShouldHaveValidScores(
            @ForAll("validCropHistory") List<CropHistoryEntryDto> history,
            @ForAll("randomSeason") String season
    ) {
        RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                .cropHistory(history)
                .season(season)
                .build();

        RotationRecommendationResultDto result = engine.generateRecommendations(request);

        assertNotNull(result);
        assertFalse(result.getOptions().isEmpty());

        for (RotationOptionDto option : result.getOptions()) {
            assertTrue(isValidScore(option.getSoilHealthBenefit()),
                    "Soil health benefit should be between 0 and 100");
            assertTrue(isValidScore(option.getClimateResilience()),
                    "Climate resilience should be between 0 and 100");
            assertTrue(isValidScore(option.getEconomicViability()),
                    "Economic viability should be between 0 and 100");
            assertTrue(isValidScore(option.getNutrientCyclingScore()),
                    "Nutrient cycling score should be between 0 and 100");
            assertTrue(isValidScore(option.getPestManagementScore()),
                    "Pest management score should be between 0 and 100");
            assertTrue(isValidScore(option.getOverallBenefitScore()),
                    "Overall benefit score should be between 0 and 100");
        }
    }

    /**
     * Property: Options should be sorted in descending order by overall benefit score.
     * Validates: Requirement 3.9
     */
    @Property
    @Label("Options should be sorted in descending order by overall benefit score")
    void optionsShouldBeSortedByBenefitScore(
            @ForAll("validCropHistory") List<CropHistoryEntryDto> history,
            @ForAll("randomSeason") String season
    ) {
        RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                .cropHistory(history)
                .season(season)
                .build();

        RotationRecommendationResultDto result = engine.generateRecommendations(request);

        assertNotNull(result);
        if (result.getOptions().size() > 1) {
            List<RotationOptionDto> options = result.getOptions();
            for (int i = 0; i < options.size() - 1; i++) {
                assertTrue(options.get(i).getOverallBenefitScore()
                        .compareTo(options.get(i + 1).getOverallBenefitScore()) >= 0,
                        "Options should be sorted in descending order");
            }
        }
    }

    /**
     * Property: Rice-based system detection should be consistent.
     * Validates: Requirement 3.5
     */
    @Property
    @Label("Rice-based system detection should be consistent for same input")
    void riceSystemDetectionShouldBeConsistent(
            @ForAll List<CropHistoryEntryDto> history
    ) {
        boolean hasRice = history.stream()
                .anyMatch(e -> "Rice".equalsIgnoreCase(e.getCropName()) || 
                              "Paddy".equalsIgnoreCase(e.getCropName()));

        RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                .cropHistory(history)
                .season("KHARIF")
                .build();

        RotationRecommendationResultDto result = engine.generateRecommendations(request);

        assertNotNull(result);
        assertEquals(hasRice, result.isHasRiceBasedSystem(),
                "Rice system detection should match input");
    }

    /**
     * Property: Legume recommendations should not follow legumes.
     * Validates: Requirement 3.4
     */
    @Property
    @Label("Legume recommendations should not follow legumes in rotation")
    void legumeRecommendationsShouldNotFollowLegumes(
            @ForAll("legumeHistory") List<CropHistoryEntryDto> history
    ) {
        RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                .cropHistory(history)
                .season("KHARIF")
                .build();

        RotationRecommendationResultDto result = engine.generateRecommendations(request);

        assertNotNull(result);
        
        // Check that legume integration options are not generated for legume history
        long legumeIntegrationCount = result.getOptions().stream()
                .filter(opt -> opt.getDescription().toLowerCase().contains("nitrogen fixation"))
                .count();
        
        // If last crop was a legume, should not recommend legume integration
        if (!history.isEmpty()) {
            String lastCrop = history.get(0).getCropName();
            CropFamily lastFamily = CropFamily.getFamilyForCrop(lastCrop);
            if (lastFamily == CropFamily.LEGUMES) {
                assertEquals(0, legumeIntegrationCount,
                        "Should not recommend legumes after legumes");
            }
        }
    }

    /**
     * Property: Pest risk level should be consistent with crop history.
     * Validates: Requirement 3.7
     */
    @Property
    @Label("Pest risk level should be consistent with crop history patterns")
    void pestRiskLevelShouldBeConsistent(
            @ForAll("monocultureHistory") List<CropHistoryEntryDto> history
    ) {
        RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                .cropHistory(history)
                .season("KHARIF")
                .build();

        RotationRecommendationResultDto result = engine.generateRecommendations(request);

        assertNotNull(result);
        assertNotNull(result.getPestRiskLevel());
        
        // HIGH risk should only appear for consecutive same-family crops
        if ("HIGH".equals(result.getPestRiskLevel())) {
            boolean hasConsecutive = hasConsecutiveSameFamily(history, 2);
            assertTrue(hasConsecutive, "HIGH risk should only occur with consecutive monoculture");
        }
    }

    /**
     * Property: All options should have non-empty benefits and considerations.
     * Validates: Requirements 3.3, 3.4, 3.5, 3.6, 3.7
     */
    @Property
    @Label("All options should have non-empty benefits and considerations")
    void allOptionsShouldHaveCompleteInformation(
            @ForAll("validCropHistory") List<CropHistoryEntryDto> history,
            @ForAll("randomSeason") String season
    ) {
        RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                .cropHistory(history)
                .season(season)
                .build();

        RotationRecommendationResultDto result = engine.generateRecommendations(request);

        assertNotNull(result);
        assertFalse(result.getOptions().isEmpty());

        for (RotationOptionDto option : result.getOptions()) {
            assertNotNull(option.getCropSequence());
            assertNotNull(option.getDescription());
            assertNotNull(option.getBenefits());
            assertFalse(option.getBenefits().isEmpty(),
                    "Each option should have at least one benefit listed");
            assertNotNull(option.getResidueManagementRecommendation());
            assertFalse(option.getResidueManagementRecommendation().isEmpty());
        }
    }

    /**
     * Property: Nutrient cycling score should be higher for alternating root depths.
     * Validates: Requirement 3.3
     */
    @Property
    @Label("Nutrient cycling score should reflect root depth alternation")
    void nutrientCyclingShouldReflectRootDepth(
            @ForAll("shallowCropHistory") List<CropHistoryEntryDto> history
    ) {
        RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                .cropHistory(history)
                .season("KHARIF")
                .build();

        RotationRecommendationResultDto result = engine.generateRecommendations(request);

        assertNotNull(result);
        assertFalse(result.getOptions().isEmpty());

        // After shallow crop, deep-rooted options should have high nutrient cycling scores
        List<RotationOptionDto> deepRootedOptions = result.getOptions().stream()
                .filter(opt -> opt.getCropSequence().contains("Sunflower") ||
                              opt.getCropSequence().contains("Sorghum") ||
                              opt.getCropSequence().contains("Cotton"))
                .collect(Collectors.toList());

        if (!deepRootedOptions.isEmpty()) {
            for (RotationOptionDto option : deepRootedOptions) {
                assertTrue(option.getNutrientCyclingScore().compareTo(new BigDecimal("70")) > 0,
                        "Deep-rooted after shallow should have high nutrient cycling score");
            }
        }
    }

    /**
     * Property: Rice diversification should always include pulses and oilseeds.
     * Validates: Requirement 3.5
     */
    @Property
    @Label("Rice diversification should include pulses and oilseeds")
    void riceDiversificationShouldIncludeKeyCrops(
            @ForAll("riceHistory") List<CropHistoryEntryDto> history
    ) {
        RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                .cropHistory(history)
                .season("RABI")
                .build();

        RotationRecommendationResultDto result = engine.generateRecommendations(request);

        assertNotNull(result);
        assertTrue(result.isHasRiceBasedSystem());

        // Check for pulse options
        boolean hasPulse = result.getOptions().stream()
                .anyMatch(opt -> opt.getCropSequence().contains("Greengram") ||
                                opt.getCropSequence().contains("Blackgram") ||
                                opt.getCropSequence().contains("Lentil") ||
                                opt.getCropSequence().contains("Chickpea"));
        assertTrue(hasPulse, "Rice diversification should include pulses");

        // Check for oilseed options
        boolean hasOilseed = result.getOptions().stream()
                .anyMatch(opt -> opt.getCropSequence().contains("Sunflower") ||
                                opt.getCropSequence().contains("Mustard") ||
                                opt.getCropSequence().contains("Sesame"));
        assertTrue(hasOilseed, "Rice diversification should include oilseeds");
    }

    /**
     * Property: Relay cropping should be recommended for rice systems.
     * Validates: Requirement 3.6
     */
    @Property
    @Label("Relay cropping should be recommended for rice systems")
    void relayCroppingShouldBeRecommendedForRice(
            @ForAll("riceHistory") List<CropHistoryEntryDto> history
    ) {
        RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                .cropHistory(history)
                .season("RABI")
                .build();

        RotationRecommendationResultDto result = engine.generateRecommendations(request);

        assertNotNull(result);
        
        // Check for relay cropping options
        boolean hasRelayOption = result.getOptions().stream()
                .anyMatch(opt -> opt.getDescription().toLowerCase().contains("paira") ||
                                opt.getDescription().toLowerCase().contains("utera") ||
                                opt.getDescription().toLowerCase().contains("relay"));
        assertTrue(hasRelayOption, "Relay cropping should be recommended for rice");
    }

    /**
     * Property: Warnings should be generated for pest risks.
     * Validates: Requirement 3.7
     */
    @Property
    @Label("Warnings should be generated for pest and disease risks")
    void warningsShouldBeGeneratedForPestRisks(
            @ForAll("monocultureHistory") List<CropHistoryEntryDto> history
    ) {
        RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                .cropHistory(history)
                .season("KHARIF")
                .build();

        RotationRecommendationResultDto result = engine.generateRecommendations(request);

        assertNotNull(result);
        
        // For consecutive monoculture, should have warnings
        if (hasConsecutiveSameFamily(history, 2)) {
            assertTrue(result.isHasPestDiseaseRisk() || !result.getWarnings().isEmpty(),
                    "Should have warnings for consecutive monoculture");
        }
    }

    // Generators

    @Provide
    Arbitrary<List<CropHistoryEntryDto>> validCropHistory() {
        return generateCropHistory(5, false);
    }

    @Provide
    Arbitrary<List<CropHistoryEntryDto>> legumeHistory() {
        return generateCropHistory(1, true);
    }

    @Provide
    Arbitrary<List<CropHistoryEntryDto>> shallowCropHistory() {
        return generateCropHistory(1, false, "Cabbage", "Mustard", "Wheat");
    }

    @Provide
    Arbitrary<List<CropHistoryEntryDto>> riceHistory() {
        return generateCropHistory(1, false, "Rice", "Paddy");
    }

    @Provide
    Arbitrary<List<CropHistoryEntryDto>> monocultureHistory() {
        return generateCropHistory(3, false, "Rice", "Rice", "Rice");
    }

    @Provide
    Arbitrary<String> randomSeason() {
        return Arbitraries.of(SEASONS);
    }

    private Arbitrary<List<CropHistoryEntryDto>> generateCropHistory(int maxSize, boolean forceLegume) {
        return generateCropHistory(maxSize, forceLegume, ALL_CROPS.toArray(new String[0]));
    }

    private Arbitrary<List<CropHistoryEntryDto>> generateCropHistory(
            int maxSize, boolean forceLegume, String... preferredCrops) {
        
        final List<String> crops;
        if (preferredCrops != null && preferredCrops.length > 0 && 
            !Arrays.equals(preferredCrops, ALL_CROPS.toArray(new String[0]))) {
            crops = Arrays.asList(preferredCrops);
        } else {
            crops = ALL_CROPS;
        }

        return Arbitraries.integers().between(0, maxSize - 1)
                .map(i -> {
                    List<CropHistoryEntryDto> history = new ArrayList<>();
                    LocalDate baseDate = LocalDate.now();
                    
                    for (int j = 0; j <= i; j++) {
                        String cropName;
                        if (forceLegume && j == 0) {
                            cropName = "Greengram";
                        } else {
                            cropName = crops.get(j % crops.size());
                        }
                        
                        CropHistoryEntryDto entry = CropHistoryEntryDto.builder()
                                .cropId(System.currentTimeMillis() + j)
                                .cropName(cropName)
                                .cropVariety("Common")
                                .sowingDate(baseDate.minusMonths((long) (j + 1) * 4))
                                .expectedHarvestDate(baseDate.minusMonths((long) j * 4))
                                .areaAcres(new BigDecimal("2.5"))
                                .season(j % 2 == 0 ? "KHARIF" : "RABI")
                                .status("HARVESTED")
                                .cropFamily(CropFamily.getFamilyForCrop(cropName))
                                .rootDepth(CropFamily.getRootDepthForCrop(cropName))
                                .build();
                        history.add(entry);
                    }
                    return history;
                });
    }

    // Helper methods

    private boolean isValidScore(BigDecimal score) {
        return score != null && 
                score.compareTo(BigDecimal.ZERO) >= 0 && 
                score.compareTo(new BigDecimal("100")) <= 0;
    }

    private boolean hasConsecutiveSameFamily(List<CropHistoryEntryDto> history, int threshold) {
        if (history == null || history.size() < threshold) return false;

        for (int i = 0; i <= history.size() - threshold; i++) {
            boolean sameFamily = true;
            CropFamily firstFamily = history.get(i).getCropFamily();
            for (int j = i + 1; j < i + threshold; j++) {
                if (firstFamily != history.get(j).getCropFamily()) {
                    sameFamily = false;
                    break;
                }
            }
            if (sameFamily) return true;
        }
        return false;
    }
}