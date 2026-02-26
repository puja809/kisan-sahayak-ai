package com.farmer.crop.service;

import com.farmer.crop.dto.CropHistoryEntryDto;
import com.farmer.crop.dto.RotationOptionDto;
import com.farmer.crop.dto.RotationRecommendationResultDto;
import com.farmer.crop.enums.CropFamily;
import net.jqwik.api.*;


import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for RotationRankingDisplayService.
 * 
 * Validates universal properties across all valid inputs for rotation ranking,
 * season-wise schedules, residue management, and default patterns.
 * 
 * Requirements: 3.8, 3.9, 3.10, 3.11
 */
class RotationRankingDisplayPropertyTest {

    private final RotationRankingDisplayService service = new RotationRankingDisplayService();

    private static final List<String> ALL_CROPS = Arrays.asList(
            "Rice", "Wheat", "Maize", "Soybean", "Groundnut",
            "Cotton", "Sugarcane", "Potato", "Tomato", "Onion",
            "Greengram", "Blackgram", "Chickpea", "Lentil", "Peas",
            "Mustard", "Sunflower", "Sesame", "Cabbage", "Cauliflower"
    );

    private static final List<String> ZONES = Arrays.asList(
            "Indo-Gangetic Plains",
            "Western Dry Region",
            "Southern Plateau and Hills",
            "Eastern Plateau and Hills",
            "Central Plateau and Hills",
            "Trans Himalayan Zone",
            "Himalayan Zone",
            "East Coast Plains and Hills",
            "West Coast Plains and Hills",
            "Gujarat Plains and Hills",
            "Island Region"
    );

    private static final List<String> SEASONS = Arrays.asList("KHARIF", "RABI", "ZAID");

    /**
     * Property 1: Descending Ranking Order
     * For any list of rotation options with numeric scores, the displayed list 
     * should be sorted in descending order such that for any two adjacent items, 
     * the first has a score greater than or equal to the second.
     * 
     * Validates: Requirement 3.9
     */
    @Property
    @Label("Rotation options should be sorted in descending order by overall benefit score")
    void testDescendingRankingOrder(
            @ForAll("validRotationOptions") List<RotationOptionDto> options
    ) {
        // Given - filter out nulls and ensure at least 2 items
        List<RotationOptionDto> validOptions = options.stream()
                .filter(o -> o != null && o.getOverallBenefitScore() != null)
                .collect(Collectors.toList());
        
        if (validOptions.size() < 2) {
            return; // Skip if not enough valid options
        }

        // When
        List<RotationOptionDto> ranked = service.rankRotationOptionsByOverallBenefit(validOptions);

        // Then - verify descending order
        for (int i = 0; i < ranked.size() - 1; i++) {
            Double current = ranked.get(i).getOverallBenefitScore();
            Double next = ranked.get(i + 1).getOverallBenefitScore();
            assertTrue(current >= next,
                    "Options should be in descending order. Found: " + current + " followed by " + next);
        }
    }

    /**
     * Property 2: Overall Score Calculation Consistency
     * For any rotation option, the overall benefit score should be calculated 
     * consistently using the weighted formula: 40% soil health + 30% climate + 30% economic.
     * 
     * Validates: Requirement 3.9
     */
    @Property
    @Label("Overall benefit score should be calculated consistently using weighted formula")
    void testOverallScoreCalculationConsistency(
            @ForAll("validScores") int[] scores
    ) {
        if (scores.length < 3) return;
        
        int soilHealth = scores[0] % 101;
        int climate = scores[1] % 101;
        int economic = scores[2] % 101;
        
        // Given
        RotationOptionDto option = RotationOptionDto.builder()
                .soilHealthBenefit((double) soilHealth)
                .climateResilience((double) climate)
                .economicViability((double) economic)
                .build();

        // When
        Double overall = service.calculateOverallBenefitScore(option);

        // Then - verify calculation matches expected formula
        Double expected = soilHealth * 0.40 + climate * 0.30 + economic * 0.30;

        assertEquals(expected, overall);
    }

    /**
     * Property 3: Season Schedule Population
     * For any rotation option with a valid crop sequence, the season-wise 
     * schedule should correctly populate kharif, rabi, and zaid crops.
     * 
     * Validates: Requirement 3.10
     */
    @Property
    @Label("Season-wise schedules should correctly populate from crop sequence")
    void testSeasonSchedulePopulation(
            @ForAll("cropSequences") String[] crops
    ) {
        if (crops.length < 3) return;
        
        String crop1 = crops[0];
        String crop2 = crops[1];
        String crop3 = crops[2];
        
        // Given
        String sequence = crop1 + " -> " + crop2 + " -> " + crop3;
        RotationOptionDto option = RotationOptionDto.builder()
                .cropSequence(sequence)
                .build();

        // When
        List<RotationOptionDto> result = service.generateSeasonWiseSchedules(List.of(option));

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        RotationOptionDto populated = result.get(0);
        assertEquals(crop1, populated.getKharifCrops());
        assertEquals(crop2, populated.getRabiCrops());
        assertEquals(crop3, populated.getZaidCrops());
    }

    /**
     * Property 4: Residue Management Recommendation Non-Null
     * For any rotation option, after adding residue management recommendations,
     * the recommendation should not be null or empty.
     * 
     * Validates: Requirement 3.8
     */
    @Property
    @Label("Residue management recommendations should not be null or empty")
    void testResidueManagementNotNull(
            @ForAll("validCrops") String cropName
    ) {
        // Given
        RotationOptionDto option = RotationOptionDto.builder()
                .cropSequence(cropName)
                .build();

        // When
        List<RotationOptionDto> result = service.addResidueManagementRecommendations(List.of(option));

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getResidueManagementRecommendation());
        assertFalse(result.get(0).getResidueManagementRecommendation().isEmpty());
    }

    /**
     * Property 5: Organic Matter Impact Non-Null
     * For any rotation option, the organic matter impact projection should not be null.
     * 
     * Validates: Requirement 3.8
     */
    @Property
    @Label("Organic matter impact projections should not be null")
    void testOrganicMatterImpactNotNull(
            @ForAll("validCrops") String cropName
    ) {
        // Given
        RotationOptionDto option = RotationOptionDto.builder()
                .cropSequence(cropName)
                .build();

        // When
        List<RotationOptionDto> result = service.addResidueManagementRecommendations(List.of(option));

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getOrganicMatterImpact());
    }

    /**
     * Property 6: Default Patterns Non-Empty
     * For any valid agro-ecological zone, the default rotation patterns 
     * should not be empty.
     * 
     * Validates: Requirement 3.11
     */
    @Property
    @Label("Default rotation patterns should not be empty for any zone")
    void testDefaultPatternsNotEmpty(
            @ForAll("validZones") String zone
    ) {
        // When
        List<RotationOptionDto> defaults = service.getDefaultRotationPatterns(zone);

        // Then
        assertNotNull(defaults);
        assertFalse(defaults.isEmpty());
    }

    /**
     * Property 7: Default Patterns Have Complete Information
     * For any valid agro-ecological zone, all default rotation patterns 
     * should have complete information (id, sequence, scores, recommendations).
     * 
     * Validates: Requirement 3.11
     */
    @Property
    @Label("Default rotation patterns should have complete information")
    void testDefaultPatternsCompleteInfo(
            @ForAll("validZones") String zone
    ) {
        // When
        List<RotationOptionDto> defaults = service.getDefaultRotationPatterns(zone);

        // Then
        for (RotationOptionDto option : defaults) {
            assertNotNull(option.getId(), "ID should not be null");
            assertNotNull(option.getCropSequence(), "Crop sequence should not be null");
            assertFalse(option.getCropSequence().isEmpty(), "Crop sequence should not be empty");
            assertNotNull(option.getSoilHealthBenefit(), "Soil health benefit should not be null");
            assertNotNull(option.getClimateResilience(), "Climate resilience should not be null");
            assertNotNull(option.getEconomicViability(), "Economic viability should not be null");
            assertNotNull(option.getOverallBenefitScore(), "Overall benefit score should not be null");
            assertNotNull(option.getResidueManagementRecommendation(), "Residue recommendation should not be null");
            assertNotNull(option.getOrganicMatterImpact(), "Organic matter impact should not be null");
        }
    }

    /**
     * Property 8: No History Detection
     * For any crop history list, the hasNoCropHistory method should correctly 
     * identify whether the farmer has no history.
     * 
     * Validates: Requirement 3.11
     */
    @Property
    @Label("No crop history detection should be accurate")
    void testNoHistoryDetection(
            @ForAll("cropHistoryLists") List<CropHistoryEntryDto> history
    ) {
        // When
        boolean hasNoHistory = service.hasNoCropHistory(history);

        // Then
        boolean expected = (history == null || history.isEmpty());
        assertEquals(expected, hasNoHistory);
    }

    /**
     * Property 9: Complete Display Has Options or Defaults
     * For any complete rotation display request, the result should have 
     * either options or default patterns.
     * 
     * Validates: Requirements 3.10, 3.11
     */
    @Property
    @Label("Complete rotation display should have options or defaults")
    void testCompleteDisplayHasOptionsOrDefaults(
            @ForAll("validZones") String zone,
            @ForAll boolean hasHistory
    ) {
        // When
        RotationRecommendationResultDto result = service.createCompleteRotationDisplay(
                new ArrayList<>(), zone, hasHistory);

        // Then
        assertNotNull(result);
        assertTrue(result.getDefaultPatterns() != null && !result.getDefaultPatterns().isEmpty(),
                "Should have default patterns");
    }

    /**
     * Property 10: Ranking Preserves All Options
     * When ranking rotation options, all original options should be present 
     * in the result (no options lost).
     * 
     * Validates: Requirement 3.9
     */
    @Property
    @Label("Ranking should preserve all original options")
    void testRankingPreservesAllOptions(
            @ForAll("validRotationOptions") List<RotationOptionDto> options
    ) {
        // Given - filter valid options
        List<RotationOptionDto> validOptions = options.stream()
                .filter(o -> o != null && o.getOverallBenefitScore() != null)
                .collect(Collectors.toList());
        
        if (validOptions.isEmpty()) {
            return;
        }

        // When
        List<RotationOptionDto> ranked = service.rankRotationOptionsByOverallBenefit(validOptions);

        // Then
        assertEquals(validOptions.size(), ranked.size(),
                "Ranking should preserve all options");
    }

    /**
     * Property 11: Season Schedule Info Returns Valid Data
     * For any valid season, the schedule info should return non-null values.
     * 
     * Validates: Requirement 3.10
     */
    @Property
    @Label("Season schedule info should return non-null values")
    void testSeasonScheduleInfoNonNull(
            @ForAll("randomSeason") String season
    ) {
        // When
        var schedule = service.getSeasonScheduleInfo(season);

        // Then
        assertNotNull(schedule);
        assertNotNull(schedule.get("plantingMonths"));
        assertNotNull(schedule.get("harvestMonths"));
    }

    /**
     * Property 12: Residue Management Contains Key Terms
     * For any rotation option, the residue management recommendation should 
     * contain relevant agricultural terms.
     * 
     * Validates: Requirement 3.8
     */
    @Property
    @Label("Residue management recommendations should contain relevant terms")
    void testResidueManagementContainsRelevantTerms(
            @ForAll("validCrops") String cropName
    ) {
        // Given
        RotationOptionDto option = RotationOptionDto.builder()
                .cropSequence(cropName)
                .build();

        // When
        List<RotationOptionDto> result = service.addResidueManagementRecommendations(List.of(option));
        String recommendation = result.get(0).getResidueManagementRecommendation();

        // Then - should contain at least one of key agricultural terms
        String lowerRec = recommendation.toLowerCase();
        boolean hasRelevantTerm = lowerRec.contains("incorpor") ||
                lowerRec.contains("soil") ||
                lowerRec.contains("decompos") ||
                lowerRec.contains("mulch") ||
                lowerRec.contains("compost");
        
        assertTrue(hasRelevantTerm, 
                "Recommendation should contain relevant agricultural terms: " + recommendation);
    }

    /**
     * Property 13: Default Patterns Score Range
     * All default rotation pattern scores should be within valid range (0-100).
     * 
     * Validates: Requirement 3.9
     */
    @Property
    @Label("Default pattern scores should be within valid range")
    void testDefaultPatternScoresInRange(
            @ForAll("validZones") String zone
    ) {
        // When
        List<RotationOptionDto> defaults = service.getDefaultRotationPatterns(zone);

        // Then
        for (RotationOptionDto option : defaults) {
            assertTrue(option.getSoilHealthBenefit().compareTo(0.0) >= 0,
                    "Soil health should be >= 0");
            assertTrue(option.getSoilHealthBenefit().compareTo(100.0) <= 0,
                    "Soil health should be <= 100");
            
            assertTrue(option.getClimateResilience().compareTo(0.0) >= 0,
                    "Climate resilience should be >= 0");
            assertTrue(option.getClimateResilience().compareTo(100.0) <= 0,
                    "Climate resilience should be <= 100");
            
            assertTrue(option.getEconomicViability().compareTo(0.0) >= 0,
                    "Economic viability should be >= 0");
            assertTrue(option.getEconomicViability().compareTo(100.0) <= 0,
                    "Economic viability should be <= 100");
        }
    }

    /**
     * Property 14: Overall Score Within Valid Range
     * The calculated overall benefit score should always be within 0-100 range.
     * 
     * Validates: Requirement 3.9
     */
    @Property
    @Label("Overall benefit score should be within valid range")
    void testOverallScoreInRange(
            @ForAll("validScores") int[] scores
    ) {
        if (scores.length < 3) return;
        
        int soilHealth = scores[0] % 101;
        int climate = scores[1] % 101;
        int economic = scores[2] % 101;
        
        // Given
        RotationOptionDto option = RotationOptionDto.builder()
                .soilHealthBenefit((double) soilHealth)
                .climateResilience((double) climate)
                .economicViability((double) economic)
                .build();

        // When
        Double overall = service.calculateOverallBenefitScore(option);

        // Then
        assertTrue(overall.compareTo(0.0) >= 0, "Overall score should be >= 0");
        assertTrue(overall.compareTo(100.0) <= 0, "Overall score should be <= 100");
    }

    /**
     * Property 15: Null Handling in Ranking
     * Ranking should handle null inputs gracefully without throwing exceptions.
     * 
     * Validates: Requirement 3.9
     */
    @Property
    @Label("Ranking should handle null inputs gracefully")
    void testRankingNullHandling() {
        // Should not throw exception
        assertDoesNotThrow(() -> service.rankRotationOptionsByOverallBenefit(null));
        assertDoesNotThrow(() -> service.rankBySoilHealthBenefit(null));
        assertDoesNotThrow(() -> service.rankByClimateResilience(null));
        assertDoesNotThrow(() -> service.rankByEconomicViability(null));
    }

    /**
     * Property 16: Empty List Handling
     * All methods should handle empty lists gracefully without throwing exceptions.
     * 
     * Validates: Requirement 3.9
     */
    @Property
    @Label("Methods should handle empty lists gracefully")
    void testEmptyListHandling() {
        List<RotationOptionDto> empty = new ArrayList<>();
        
        // Should not throw exception and should return empty or null
        assertDoesNotThrow(() -> service.rankRotationOptionsByOverallBenefit(empty));
        assertDoesNotThrow(() -> service.generateSeasonWiseSchedules(empty));
        assertDoesNotThrow(() -> service.addResidueManagementRecommendations(empty));
    }

    // Arbitraries for property testing
    @Provide
    @SuppressWarnings("unused")
    Arbitrary<List<RotationOptionDto>> validRotationOptions() {
        return generateRotationOptions(5);
    }

    @Provide
    @SuppressWarnings("unused")
    Arbitrary<int[]> validScores() {
        return Arbitraries.integers()
                .between(0, 100)
                .array(int[].class)
                .ofSize(3);
    }

    @Provide
    @SuppressWarnings("unused")
    Arbitrary<String[]> cropSequences() {
        return Combinators.combine(
                Arbitraries.of(ALL_CROPS),
                Arbitraries.of(ALL_CROPS),
                Arbitraries.of(ALL_CROPS)
        ).as((c1, c2, c3) -> new String[]{c1, c2, c3});
    }

    @Provide
    @SuppressWarnings("unused")
    Arbitrary<String> validCrops() {
        return Arbitraries.of(ALL_CROPS);
    }

    @Provide
    @SuppressWarnings("unused")
    Arbitrary<String> validZones() {
        return Arbitraries.of(ZONES);
    }

    @Provide
    @SuppressWarnings("unused")
    Arbitrary<List<CropHistoryEntryDto>> cropHistoryLists() {
        return generateCropHistory(5);
    }

    @Provide
    @SuppressWarnings("unused")
    Arbitrary<String> randomSeason() {
        return Arbitraries.of(SEASONS);
    }

    // Helper methods
    private Arbitrary<List<RotationOptionDto>> generateRotationOptions(int maxSize) {
        return Arbitraries.integers().between(2, maxSize)
                .map(size -> {
                    List<RotationOptionDto> options = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        RotationOptionDto option = RotationOptionDto.builder()
                                .id(System.nanoTime() + i)
                                .cropSequence(ALL_CROPS.get(i % ALL_CROPS.size()) + " -> " + 
                                        ALL_CROPS.get((i + 1) % ALL_CROPS.size()) + " -> " +
                                        ALL_CROPS.get((i + 2) % ALL_CROPS.size()))
                                .soilHealthBenefit((double) ((50 + i * 10) % 50))
                                .climateResilience((double) ((50 + i * 8) % 50))
                                .economicViability((double) ((50 + i * 12) % 50))
                                .build();
                        options.add(option);
                    }
                    return options;
                });
    }

    private Arbitrary<List<CropHistoryEntryDto>> generateCropHistory(int maxSize) {
        return Arbitraries.integers().between(0, maxSize - 1)
                .map(size -> {
                    List<CropHistoryEntryDto> history = new ArrayList<>();
                    LocalDate baseDate = LocalDate.now();
                    
                    for (int j = 0; j <= size; j++) {
                        String cropName = ALL_CROPS.get(j % ALL_CROPS.size());
                        
                        CropHistoryEntryDto entry = CropHistoryEntryDto.builder()
                                .cropId(System.currentTimeMillis() + j)
                                .cropName(cropName)
                                .cropVariety("Common")
                                .sowingDate(baseDate.minusMonths((long) (j + 1) * 4))
                                .expectedHarvestDate(baseDate.minusMonths((long) j * 4))
                                .areaAcres(2.5)
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
}

