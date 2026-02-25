package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.enums.CropFamily;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RotationRankingDisplayService.
 * 
 * Tests rotation ranking, season-wise schedules, residue management,
 * and default patterns for farmers with no crop history.
 * 
 * Requirements: 3.8, 3.9, 3.10, 3.11
 */
class RotationRankingDisplayServiceTest {

    private RotationRankingDisplayService service;

    @BeforeEach
    void setUp() {
        service = new RotationRankingDisplayService();
    }

    @Nested
    @DisplayName("Rotation Option Ranking Tests")
    class RankingTests {

        @Test
        @DisplayName("Should rank options by overall benefit score in descending order")
        void testRankByOverallBenefit() {
            // Given
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Rice -> Wheat -> Greengram", 
                            new BigDecimal("70"), new BigDecimal("75"), new BigDecimal("80")),
                    createOption("Maize -> Wheat -> Chickpea", 
                            new BigDecimal("85"), new BigDecimal("80"), new BigDecimal("75")),
                    createOption("Soybean -> Wheat -> Mustard", 
                            new BigDecimal("80"), new BigDecimal("78"), new BigDecimal("82"))
            );

            // When
            List<RotationOptionDto> ranked = service.rankRotationOptionsByOverallBenefit(options);

            // Then
            assertNotNull(ranked);
            assertEquals(3, ranked.size());
            
            // Maize -> Wheat -> Chickpea should be first (highest overall: 80.67)
            assertEquals("Maize -> Wheat -> Chickpea", ranked.get(0).getCropSequence());
            
            // Soybean -> Wheat -> Mustard should be second (overall: 80)
            assertEquals("Soybean -> Wheat -> Mustard", ranked.get(1).getCropSequence());
            
            // Rice -> Wheat -> Greengram should be third (overall: 75)
            assertEquals("Rice -> Wheat -> Greengram", ranked.get(2).getCropSequence());
        }

        @Test
        @DisplayName("Should return empty list when input is empty")
        void testRankEmptyList() {
            List<RotationOptionDto> ranked = service.rankRotationOptionsByOverallBenefit(List.of());
            assertNotNull(ranked);
            assertTrue(ranked.isEmpty());
        }

        @Test
        @DisplayName("Should return same list when input is null")
        void testRankNullList() {
            List<RotationOptionDto> ranked = service.rankRotationOptionsByOverallBenefit(null);
            assertNull(ranked);
        }

        @Test
        @DisplayName("Should rank by soil health benefit in descending order")
        void testRankBySoilHealthBenefit() {
            // Given
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Rice -> Wheat -> Greengram", 
                            new BigDecimal("70"), new BigDecimal("75"), new BigDecimal("80")),
                    createOption("Soybean -> Wheat -> Mustard", 
                            new BigDecimal("90"), new BigDecimal("78"), new BigDecimal("82")),
                    createOption("Maize -> Wheat -> Chickpea", 
                            new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("75"))
            );

            // When
            List<RotationOptionDto> ranked = service.rankBySoilHealthBenefit(options);

            // Then
            assertNotNull(ranked);
            assertEquals(3, ranked.size());
            assertEquals("Soybean -> Wheat -> Mustard", ranked.get(0).getCropSequence());
            assertEquals(new BigDecimal("90"), ranked.get(0).getSoilHealthBenefit());
        }

        @Test
        @DisplayName("Should rank by climate resilience in descending order")
        void testRankByClimateResilience() {
            // Given
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Pearl Millet -> Wheat -> Mustard", 
                            new BigDecimal("75"), new BigDecimal("90"), new BigDecimal("70")),
                    createOption("Rice -> Wheat -> Greengram", 
                            new BigDecimal("70"), new BigDecimal("75"), new BigDecimal("80")),
                    createOption("Maize -> Wheat -> Chickpea", 
                            new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("75"))
            );

            // When
            List<RotationOptionDto> ranked = service.rankByClimateResilience(options);

            // Then
            assertNotNull(ranked);
            assertEquals(3, ranked.size());
            assertEquals("Pearl Millet -> Wheat -> Mustard", ranked.get(0).getCropSequence());
            assertEquals(new BigDecimal("90"), ranked.get(0).getClimateResilience());
        }

        @Test
        @DisplayName("Should rank by economic viability in descending order")
        void testRankByEconomicViability() {
            // Given
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Rice -> Wheat -> Greengram", 
                            new BigDecimal("70"), new BigDecimal("75"), new BigDecimal("80")),
                    createOption("Mango -> Banana -> Taro", 
                            new BigDecimal("60"), new BigDecimal("65"), new BigDecimal("95")),
                    createOption("Cotton -> Wheat -> Mustard", 
                            new BigDecimal("75"), new BigDecimal("72"), new BigDecimal("88"))
            );

            // When
            List<RotationOptionDto> ranked = service.rankByEconomicViability(options);

            // Then
            assertNotNull(ranked);
            assertEquals(3, ranked.size());
            assertEquals("Mango -> Banana -> Taro", ranked.get(0).getCropSequence());
            assertEquals(new BigDecimal("95"), ranked.get(0).getEconomicViability());
        }
    }

    @Nested
    @DisplayName("Overall Benefit Score Calculation Tests")
    class OverallScoreTests {

        @Test
        @DisplayName("Should calculate overall benefit score correctly")
        void testCalculateOverallBenefitScore() {
            // Given
            RotationOptionDto option = createOption("Rice -> Wheat -> Greengram",
                    new BigDecimal("80"), new BigDecimal("75"), new BigDecimal("85"));

            // When
            BigDecimal overall = service.calculateOverallBenefitScore(option);

            // Then
            assertNotNull(overall);
            // Expected: 80 * 0.40 + 75 * 0.30 + 85 * 0.30 = 32 + 22.5 + 25.5 = 80
            assertEquals(new BigDecimal("80.00"), overall);
        }

        @Test
        @DisplayName("Should return zero for null option")
        void testCalculateOverallBenefitScoreNull() {
            BigDecimal overall = service.calculateOverallBenefitScore(null);
            assertEquals(BigDecimal.ZERO, overall);
        }

        @Test
        @DisplayName("Should handle null component scores")
        void testCalculateOverallBenefitScoreWithNulls() {
            // Given
            RotationOptionDto option = new RotationOptionDto();
            option.setCropSequence("Test");
            option.setSoilHealthBenefit(new BigDecimal("80"));
            // climateResilience and economicViability are null

            // When
            BigDecimal overall = service.calculateOverallBenefitScore(option);

            // Then
            assertNotNull(overall);
            // Expected: 80 * 0.40 + 0 + 0 = 32
            assertEquals(new BigDecimal("32.00"), overall);
        }

        @Test
        @DisplayName("Should calculate correct weighted average")
        void testWeightedAverageCalculation() {
            // Test with specific values
            RotationOptionDto option = createOption("Test",
                    new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"));

            BigDecimal overall = service.calculateOverallBenefitScore(option);
            assertEquals(0, new BigDecimal("100.00").compareTo(overall));

            // Test with minimum values
            option = createOption("Test",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            overall = service.calculateOverallBenefitScore(option);
            assertEquals(0, BigDecimal.ZERO.compareTo(overall));
        }
    }

    @Nested
    @DisplayName("Season-wise Schedule Generation Tests")
    class SeasonScheduleTests {

        @Test
        @DisplayName("Should generate season-wise schedules for rotation options")
        void testGenerateSeasonWiseSchedules() {
            // Given
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Rice -> Wheat -> Greengram", 
                            new BigDecimal("80"), new BigDecimal("75"), new BigDecimal("85")),
                    createOption("Maize -> Chickpea -> Sesame", 
                            new BigDecimal("85"), new BigDecimal("80"), new BigDecimal("78"))
            );

            // When
            List<RotationOptionDto> withSchedules = service.generateSeasonWiseSchedules(options);

            // Then
            assertNotNull(withSchedules);
            assertEquals(2, withSchedules.size());
            
            // First option
            RotationOptionDto first = withSchedules.get(0);
            assertEquals("Rice", first.getKharifCrops());
            assertEquals("Wheat", first.getRabiCrops());
            assertEquals("Greengram", first.getZaidCrops());
            
            // Second option
            RotationOptionDto second = withSchedules.get(1);
            assertEquals("Maize", second.getKharifCrops());
            assertEquals("Chickpea", second.getRabiCrops());
            assertEquals("Sesame", second.getZaidCrops());
        }

        @Test
        @DisplayName("Should handle single crop sequence")
        void testSingleCropSchedule() {
            // Given
            RotationOptionDto option = createOption("Rice", 
                    new BigDecimal("80"), new BigDecimal("75"), new BigDecimal("85"));

            // When
            List<RotationOptionDto> withSchedules = service.generateSeasonWiseSchedules(List.of(option));

            // Then
            assertNotNull(withSchedules);
            assertEquals(1, withSchedules.size());
            assertEquals("Rice", withSchedules.get(0).getKharifCrops());
            assertNull(withSchedules.get(0).getRabiCrops());
            assertNull(withSchedules.get(0).getZaidCrops());
        }

        @Test
        @DisplayName("Should return empty list for empty input")
        void testEmptySeasonSchedules() {
            List<RotationOptionDto> withSchedules = service.generateSeasonWiseSchedules(List.of());
            assertNotNull(withSchedules);
            assertTrue(withSchedules.isEmpty());
        }

        @Test
        @DisplayName("Should return null for null input")
        void testNullSeasonSchedules() {
            List<RotationOptionDto> withSchedules = service.generateSeasonWiseSchedules(null);
            assertNull(withSchedules);
        }
    }

    @Nested
    @DisplayName("Residue Management Recommendation Tests")
    class ResidueManagementTests {

        @Test
        @DisplayName("Should add residue management recommendations for cereals")
        void testResidueManagementCereals() {
            // Given
            RotationOptionDto option = createOption("Rice -> Wheat -> Greengram",
                    new BigDecimal("80"), new BigDecimal("75"), new BigDecimal("85"));

            // When
            List<RotationOptionDto> withRecs = service.addResidueManagementRecommendations(List.of(option));

            // Then
            assertNotNull(withRecs);
            assertEquals(1, withRecs.size());
            
            RotationOptionDto result = withRecs.get(0);
            assertNotNull(result.getResidueManagementRecommendation());
            assertTrue(result.getResidueManagementRecommendation().contains("straw"));
            assertTrue(result.getResidueManagementRecommendation().contains("incorporate"));
            
            assertNotNull(result.getOrganicMatterImpact());
            assertTrue(result.getOrganicMatterImpact().contains("organic matter"));
        }

        @Test
        @DisplayName("Should add residue management recommendations for legumes")
        void testResidueManagementLegumes() {
            // Given
            RotationOptionDto option = createOption("Greengram -> Chickpea -> Lentil",
                    new BigDecimal("85"), new BigDecimal("80"), new BigDecimal("78"));

            // When
            List<RotationOptionDto> withRecs = service.addResidueManagementRecommendations(List.of(option));

            // Then
            assertNotNull(withRecs);
            RotationOptionDto result = withRecs.get(0);
            assertNotNull(result.getResidueManagementRecommendation());
            assertTrue(result.getResidueManagementRecommendation().contains("nitrogen"));
            assertTrue(result.getResidueManagementRecommendation().contains("green manure"));
        }

        @Test
        @DisplayName("Should add residue management recommendations for brassicas")
        void testResidueManagementBrassicas() {
            // Given
            RotationOptionDto option = createOption("Mustard -> Cabbage -> Cauliflower",
                    new BigDecimal("75"), new BigDecimal("78"), new BigDecimal("80"));

            // When
            List<RotationOptionDto> withRecs = service.addResidueManagementRecommendations(List.of(option));

            // Then
            assertNotNull(withRecs);
            RotationOptionDto result = withRecs.get(0);
            assertNotNull(result.getResidueManagementRecommendation());
            assertTrue(result.getResidueManagementRecommendation().contains("brassicas"));
            assertTrue(result.getResidueManagementRecommendation().contains("decomposition"));
        }

        @Test
        @DisplayName("Should provide default recommendation for unknown crops")
        void testResidueManagementUnknownCrops() {
            // Given - crop not in our database
            RotationOptionDto option = createOption("UnknownCrop1 -> UnknownCrop2",
                    new BigDecimal("70"), new BigDecimal("75"), new BigDecimal("80"));

            // When
            List<RotationOptionDto> withRecs = service.addResidueManagementRecommendations(List.of(option));

            // Then
            assertNotNull(withRecs);
            RotationOptionDto result = withRecs.get(0);
            assertNotNull(result.getResidueManagementRecommendation());
            assertTrue(result.getResidueManagementRecommendation().contains("Incorporate"));
            assertNotNull(result.getOrganicMatterImpact());
        }

        @Test
        @DisplayName("Should handle empty list")
        void testEmptyResidueManagement() {
            List<RotationOptionDto> withRecs = service.addResidueManagementRecommendations(List.of());
            assertNotNull(withRecs);
            assertTrue(withRecs.isEmpty());
        }

        @Test
        @DisplayName("Should handle null input")
        void testNullResidueManagement() {
            List<RotationOptionDto> withRecs = service.addResidueManagementRecommendations(null);
            assertNull(withRecs);
        }
    }

    @Nested
    @DisplayName("Default Rotation Pattern Tests")
    class DefaultPatternTests {

        @Test
        @DisplayName("Should return default patterns for Indo-Gangetic Plains zone")
        void testDefaultPatternsIndoGangetic() {
            // When
            List<RotationOptionDto> defaults = service.getDefaultRotationPatterns("Indo-Gangetic Plains");

            // Then
            assertNotNull(defaults);
            assertFalse(defaults.isEmpty());
            assertTrue(defaults.size() >= 3);
            
            // Check that patterns contain expected crops
            boolean hasRiceWheatGreengram = defaults.stream()
                    .anyMatch(o -> o.getCropSequence().contains("Rice") && 
                                   o.getCropSequence().contains("Wheat"));
            assertTrue(hasRiceWheatGreengram);
        }

        @Test
        @DisplayName("Should return default patterns for Western Dry Region zone")
        void testDefaultPatternsWesternDry() {
            // When
            List<RotationOptionDto> defaults = service.getDefaultRotationPatterns("Western Dry Region");

            // Then
            assertNotNull(defaults);
            assertFalse(defaults.isEmpty());
            
            // Should contain pearl millet based patterns
            boolean hasPearlMillet = defaults.stream()
                    .anyMatch(o -> o.getCropSequence().contains("Pearl Millet"));
            assertTrue(hasPearlMillet);
        }

        @Test
        @DisplayName("Should use Indo-Gangetic Plains as fallback for unknown zone")
        void testDefaultPatternsUnknownZone() {
            // When
            List<RotationOptionDto> defaults = service.getDefaultRotationPatterns("Unknown Zone");

            // Then
            assertNotNull(defaults);
            assertFalse(defaults.isEmpty());
            // Should fall back to Indo-Gangetic Plains patterns
            assertTrue(defaults.stream()
                    .anyMatch(o -> o.getCropSequence().contains("Rice")));
        }

        @Test
        @DisplayName("Should return default patterns with complete information")
        void testDefaultPatternsCompleteInfo() {
            // When
            List<RotationOptionDto> defaults = service.getDefaultRotationPatterns("Indo-Gangetic Plains");

            // Then
            assertNotNull(defaults);
            for (RotationOptionDto option : defaults) {
                assertNotNull(option.getId());
                assertNotNull(option.getCropSequence());
                assertNotNull(option.getDescription());
                assertNotNull(option.getSoilHealthBenefit());
                assertNotNull(option.getClimateResilience());
                assertNotNull(option.getEconomicViability());
                assertNotNull(option.getOverallBenefitScore());
                assertNotNull(option.getResidueManagementRecommendation());
                assertNotNull(option.getOrganicMatterImpact());
                assertNotNull(option.getBenefits());
                assertFalse(option.getBenefits().isEmpty());
            }
        }

        @Test
        @DisplayName("Should return Southern Plateau patterns for that zone")
        void testDefaultPatternsSouthernPlateau() {
            // When
            List<RotationOptionDto> defaults = service.getDefaultRotationPatterns("Southern Plateau and Hills");

            // Then
            assertNotNull(defaults);
            assertFalse(defaults.isEmpty());
            
            // Should contain groundnut and rice based patterns
            boolean hasGroundnut = defaults.stream()
                    .anyMatch(o -> o.getCropSequence().contains("Groundnut"));
            assertTrue(hasGroundnut);
        }
    }

    @Nested
    @DisplayName("Season Schedule Info Tests")
    class SeasonScheduleInfoTests {

        @Test
        @DisplayName("Should return correct schedule for Kharif season")
        void testKharifScheduleInfo() {
            Map<String, String> schedule = service.getSeasonScheduleInfo("KHARIF");
            
            assertNotNull(schedule);
            assertEquals("June - July", schedule.get("plantingMonths"));
            assertEquals("September - October", schedule.get("harvestMonths"));
        }

        @Test
        @DisplayName("Should return correct schedule for Rabi season")
        void testRabiScheduleInfo() {
            Map<String, String> schedule = service.getSeasonScheduleInfo("RABI");
            
            assertNotNull(schedule);
            assertEquals("October - November", schedule.get("plantingMonths"));
            assertEquals("March - April", schedule.get("harvestMonths"));
        }

        @Test
        @DisplayName("Should return correct schedule for Zaid season")
        void testZaidScheduleInfo() {
            Map<String, String> schedule = service.getSeasonScheduleInfo("ZAID");
            
            assertNotNull(schedule);
            assertEquals("February - March", schedule.get("plantingMonths"));
            assertEquals("May - June", schedule.get("harvestMonths"));
        }

        @Test
        @DisplayName("Should return 'Varies' for unknown season")
        void testUnknownSeasonScheduleInfo() {
            Map<String, String> schedule = service.getSeasonScheduleInfo("UNKNOWN");
            
            assertNotNull(schedule);
            assertEquals("Varies", schedule.get("plantingMonths"));
            assertEquals("Varies", schedule.get("harvestMonths"));
        }
    }

    @Nested
    @DisplayName("No Crop History Detection Tests")
    class NoHistoryTests {

        @Test
        @DisplayName("Should return true for null crop history")
        void testNullHistory() {
            assertTrue(service.hasNoCropHistory(null));
        }

        @Test
        @DisplayName("Should return true for empty crop history")
        void testEmptyHistory() {
            assertTrue(service.hasNoCropHistory(List.of()));
        }

        @Test
        @DisplayName("Should return false for non-empty crop history")
        void testNonEmptyHistory() {
            List<CropHistoryEntryDto> history = List.of(
                    new CropHistoryEntryDto());
            assertFalse(service.hasNoCropHistory(history));
        }
    }

    @Nested
    @DisplayName("Complete Rotation Display Tests")
    class CompleteDisplayTests {

        @Test
        @DisplayName("Should create complete rotation display with options")
        void testCompleteRotationDisplayWithOptions() {
            // Given
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Rice -> Wheat -> Greengram",
                            new BigDecimal("80"), new BigDecimal("75"), new BigDecimal("85")),
                    createOption("Maize -> Chickpea -> Sesame",
                            new BigDecimal("85"), new BigDecimal("80"), new BigDecimal("78"))
            );

            // When
            RotationRecommendationResultDto result = service.createCompleteRotationDisplay(
                    options, "Indo-Gangetic Plains", true);

            // Then
            assertNotNull(result);
            assertNotNull(result.getOptions());
            assertEquals(2, result.getOptions().size());
            // Options should be sorted by overall benefit score (Maize has higher score)
            assertEquals("Maize -> Chickpea -> Sesame", result.getOptions().get(0).getCropSequence());
            // Default patterns are still set for reference
            assertNotNull(result.getDefaultPatterns());
            assertFalse(result.getDefaultPatterns().isEmpty());
        }

        @Test
        @DisplayName("Should create complete rotation display with defaults for no history")
        void testCompleteRotationDisplayNoHistory() {
            // Given
            List<RotationOptionDto> options = List.of();

            // When
            RotationRecommendationResultDto result = service.createCompleteRotationDisplay(
                    options, "Indo-Gangetic Plains", false);

            // Then
            assertNotNull(result);
            assertNotNull(result.getDefaultPatterns());
            assertFalse(result.getDefaultPatterns().isEmpty());
        }

        @Test
        @DisplayName("Should use defaults as main options when no options provided and no history")
        void testCompleteRotationDisplayDefaultsAsOptions() {
            // Given
            List<RotationOptionDto> options = List.of();

            // When
            RotationRecommendationResultDto result = service.createCompleteRotationDisplay(
                    options, "Western Dry Region", false);

            // Then
            assertNotNull(result);
            assertNotNull(result.getOptions());
            assertFalse(result.getOptions().isEmpty());
            assertNotNull(result.getDefaultPatterns());
        }

        @Test
        @DisplayName("Should handle null options gracefully")
        void testCompleteRotationDisplayNullOptions() {
            // When
            RotationRecommendationResultDto result = service.createCompleteRotationDisplay(
                    null, "Indo-Gangetic Plains", false);

            // Then
            assertNotNull(result);
            assertNotNull(result.getDefaultPatterns());
        }
    }

    // Helper method to create test rotation options
    private RotationOptionDto createOption(String sequence, BigDecimal soilHealth, 
            BigDecimal climate, BigDecimal economic) {
        return RotationOptionDto.builder()
                .id(System.nanoTime())
                .cropSequence(sequence)
                .description("Test rotation option")
                .soilHealthBenefit(soilHealth)
                .climateResilience(climate)
                .economicViability(economic)
                .overallBenefitScore(service.calculateOverallBenefitScore(
                        RotationOptionDto.builder()
                                .soilHealthBenefit(soilHealth)
                                .climateResilience(climate)
                                .economicViability(economic)
                                .build()))
                .build();
    }
}