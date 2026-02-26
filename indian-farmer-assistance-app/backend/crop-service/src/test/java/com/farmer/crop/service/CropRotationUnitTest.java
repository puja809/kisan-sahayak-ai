package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.enums.CropFamily;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for crop rotation functionality.
 * 
 * Tests crop history analysis, nutrient depletion detection, rotation ranking algorithm,
 * and default patterns for new farmers.
 * 
 * Requirements: 3.1, 3.2, 3.9, 3.11
 */
class CropRotationUnitTest {

    private CropHistoryAnalyzer cropHistoryAnalyzer;
    private CropRotationService cropRotationService;
    private RotationRankingDisplayService rotationRankingDisplayService;
    private RotationRecommendationEngine rotationRecommendationEngine;

    @BeforeEach
    void setUp() {
        cropHistoryAnalyzer = new CropHistoryAnalyzer();
        cropRotationService = new CropRotationService();
        rotationRankingDisplayService = new RotationRankingDisplayService();
        rotationRecommendationEngine = new RotationRecommendationEngine();
    }

    // ========================================
    // SECTION 1: CROP HISTORY ANALYSIS TESTS
    // Requirement 3.1
    // ========================================

    @Nested
    @DisplayName("Crop History Analysis Tests (Requirement 3.1)")
    class CropHistoryAnalysisTests {

        @Test
        @DisplayName("Should analyze cropping pattern for past 3 seasons")
        void shouldAnalyzePastThreeSeasons() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI"),
                    createCropEntry("Greengram", LocalDate.now().minusMonths(15), "ZAID")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertNotNull(result);
            assertTrue(result.isHasSufficientHistory());
            assertEquals(3, result.getSeasonsAnalyzed());
        }

        @Test
        @DisplayName("Should handle null crop history gracefully")
        void shouldHandleNullHistory() {
            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(null);

            assertNotNull(result);
            assertFalse(result.isHasSufficientHistory());
            assertEquals(0, result.getSeasonsAnalyzed());
            assertTrue(result.getNutrientDepletionRisks().isEmpty());
        }

        @Test
        @DisplayName("Should handle empty crop history gracefully")
        void shouldHandleEmptyHistory() {
            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(Collections.emptyList());

            assertNotNull(result);
            assertFalse(result.isHasSufficientHistory());
            assertEquals(0, result.getSeasonsAnalyzed());
        }

        @Test
        @DisplayName("Should identify crop families for history entries")
        void shouldIdentifyCropFamilies() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Greengram", LocalDate.now().minusMonths(9), "RABI")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertNotNull(result);
            assertEquals(2, result.getCropHistory().size());
            assertEquals(CropFamily.CEREALS, result.getCropHistory().get(0).getCropFamily());
            assertEquals(CropFamily.LEGUMES, result.getCropHistory().get(1).getCropFamily());
        }

        @Test
        @DisplayName("Should identify root depths for history entries")
        void shouldIdentifyRootDepths() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Cabbage", LocalDate.now().minusMonths(9), "RABI")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertNotNull(result);
            assertEquals(CropFamily.RootDepth.DEEP, result.getCropHistory().get(0).getRootDepth());
            assertEquals(CropFamily.RootDepth.SHALLOW, result.getCropHistory().get(1).getRootDepth());
        }

        @Test
        @DisplayName("Should sort history by sowing date, most recent first")
        void shouldSortHistoryByDate() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(15), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(3), "RABI"),
                    createCropEntry("Greengram", LocalDate.now().minusMonths(9), "ZAID")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertNotNull(result);
            assertEquals("Wheat", result.getCropHistory().get(0).getCropName());
            assertEquals("Greengram", result.getCropHistory().get(1).getCropName());
            assertEquals("Rice", result.getCropHistory().get(2).getCropName());
        }

        @Test
        @DisplayName("Should limit analysis to maximum 3 seasons")
        void shouldLimitToThreeSeasons() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI"),
                    createCropEntry("Greengram", LocalDate.now().minusMonths(15), "ZAID"),
                    createCropEntry("Maize", LocalDate.now().minusMonths(21), "KHARIF"),
                    createCropEntry("Cabbage", LocalDate.now().minusMonths(27), "RABI")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertNotNull(result);
            assertEquals(3, result.getSeasonsAnalyzed());
        }

        @Test
        @DisplayName("Should generate analysis summary with rotation quality assessment")
        void shouldGenerateAnalysisSummary() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Greengram", LocalDate.now().minusMonths(9), "RABI")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertNotNull(result);
            assertNotNull(result.getSummary());
            assertNotNull(result.getSummary().getRotationPattern());
            assertNotNull(result.getSummary().getNutrientBalanceAssessment());
            assertNotNull(result.getSummary().getPestDiseaseRiskLevel());
        }

        @Test
        @DisplayName("Should generate recommendations based on analysis")
        void shouldGenerateRecommendations() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertNotNull(result);
            assertFalse(result.getRecommendations().isEmpty());
        }

        @Test
        @DisplayName("Should correctly identify dominant crop family")
        void shouldIdentifyDominantFamily() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI"),
                    createCropEntry("Maize", LocalDate.now().minusMonths(15), "KHARIF")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertNotNull(result);
            assertEquals("Cereals", result.getSummary().getDominantCropFamily());
        }
    }

    // ========================================
    // SECTION 2: NUTRIENT DEPLETION DETECTION TESTS
    // Requirement 3.2
    // ========================================

    @Nested
    @DisplayName("Nutrient Depletion Detection Tests (Requirement 3.2)")
    class NutrientDepletionDetectionTests {

        @Test
        @DisplayName("Should flag risk for 3 consecutive seasons of same family")
        void shouldFlagRiskForThreeConsecutiveSeasons() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI"),
                    createCropEntry("Maize", LocalDate.now().minusMonths(15), "KHARIF")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertFalse(result.getNutrientDepletionRisks().isEmpty());
            NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().get(0);
            assertEquals(CropFamily.CEREALS, risk.getCropFamily());
            assertTrue(risk.getConsecutiveSeasons() >= 3);
        }

        @Test
        @DisplayName("Should flag risk for 2 consecutive seasons of same family")
        void shouldFlagRiskForTwoConsecutiveSeasons() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertFalse(result.getNutrientDepletionRisks().isEmpty());
            NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().get(0);
            assertEquals(2, risk.getConsecutiveSeasons());
        }

        @Test
        @DisplayName("Should not flag risk for alternating crop families")
        void shouldNotFlagRiskForAlternatingFamilies() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Greengram", LocalDate.now().minusMonths(9), "RABI"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(15), "KHARIF")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertTrue(result.getSummary().isHasGoodRotation());
        }

        @Test
        @DisplayName("Should identify nitrogen depletion for cereal monoculture")
        void shouldIdentifyNitrogenDepletionForCereals() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().stream()
                    .filter(r -> r.getCropFamily() == CropFamily.CEREALS)
                    .findFirst()
                    .orElse(null);
            
            assertNotNull(risk);
            assertTrue(risk.getAffectedNutrients().contains("Nitrogen"));
        }

        @Test
        @DisplayName("Should identify potassium depletion for brassica monoculture")
        void shouldIdentifyPotassiumDepletionForBrassicas() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Cabbage", LocalDate.now().minusMonths(3), "RABI"),
                    createCropEntry("Cauliflower", LocalDate.now().minusMonths(9), "KHARIF")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().stream()
                    .filter(r -> r.getCropFamily() == CropFamily.BRASSICAS)
                    .findFirst()
                    .orElse(null);
            
            assertNotNull(risk);
            assertTrue(risk.getAffectedNutrients().contains("Potassium"));
        }

        @Test
        @DisplayName("Should calculate severity score based on consecutive count")
        void shouldCalculateSeverityScore() {
            List<CropHistoryEntryDto> twoSeasons = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI")
            );

            List<CropHistoryEntryDto> threeSeasons = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI"),
                    createCropEntry("Maize", LocalDate.now().minusMonths(15), "KHARIF")
            );

            CropHistoryAnalysisResultDto result2 = cropHistoryAnalyzer.analyzeCropHistory(twoSeasons);
            CropHistoryAnalysisResultDto result3 = cropHistoryAnalyzer.analyzeCropHistory(threeSeasons);

            Double score2 = result2.getNutrientDepletionRisks().get(0).getSeverityScore();
            Double score3 = result3.getNutrientDepletionRisks().get(0).getSeverityScore();

            assertTrue(score3 > score2);
        }

        @Test
        @DisplayName("Should assess CRITICAL risk level for 3+ consecutive seasons")
        void shouldAssessCriticalRiskForThreeConsecutive() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI"),
                    createCropEntry("Maize", LocalDate.now().minusMonths(15), "KHARIF")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            NutrientDepletionRiskDto criticalRisk = result.getNutrientDepletionRisks().stream()
                    .filter(r -> r.getRiskLevel() == NutrientDepletionRiskDto.RiskLevel.CRITICAL)
                    .findFirst()
                    .orElse(null);

            assertNotNull(criticalRisk, "Should have CRITICAL risk for 3 consecutive seasons");
        }

        @Test
        @DisplayName("Should provide recommendations for identified risks")
        void shouldProvideRecommendationsForRisks() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI")
            );

            CropHistoryAnalysisResultDto result = cropHistoryAnalyzer.analyzeCropHistory(history);

            assertFalse(result.getNutrientDepletionRisks().isEmpty());
            assertFalse(result.getRecommendations().isEmpty());
        }

        @Test
        @DisplayName("Helper method should detect consecutive monoculture")
        void helperShouldDetectConsecutiveMonoculture() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI")
            );

            assertTrue(cropHistoryAnalyzer.hasConsecutiveMonoculture(history));
        }

        @Test
        @DisplayName("Helper method should return max consecutive seasons")
        void helperShouldReturnMaxConsecutiveSeasons() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9), "RABI"),
                    createCropEntry("Maize", LocalDate.now().minusMonths(15), "KHARIF")
            );

            assertEquals(3, cropHistoryAnalyzer.getMaxConsecutiveSeasons(history));
        }
    }

    // ========================================
    // SECTION 3: ROTATION RANKING ALGORITHM TESTS
    // Requirement 3.9
    // ========================================

    @Nested
    @DisplayName("Rotation Ranking Algorithm Tests (Requirement 3.9)")
    class RotationRankingTests {

        @Test
        @DisplayName("Should rank options by overall benefit score in descending order")
        void shouldRankByOverallBenefit() {
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Rice -> Wheat -> Greengram", 
                            70.0, 75.0, 80.0),
                    createOption("Maize -> Wheat -> Chickpea", 
                            85.0, 80.0, 75.0),
                    createOption("Soybean -> Wheat -> Mustard", 
                            80.0, 78.0, 82.0)
            );

            List<RotationOptionDto> ranked = cropRotationService.rankRotationOptions(options);

            assertNotNull(ranked);
            assertEquals(3, ranked.size());
            assertEquals("Maize -> Wheat -> Chickpea", ranked.get(0).getCropSequence());
            assertTrue(ranked.get(0).getOverallBenefitScore()
                    .compareTo(ranked.get(1).getOverallBenefitScore()) >= 0);
        }

        @Test
        @DisplayName("Should rank by soil health benefit in descending order")
        void shouldRankBySoilHealthBenefit() {
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Rice -> Wheat -> Greengram", 
                            70.0, 75.0, 80.0),
                    createOption("Soybean -> Wheat -> Mustard", 
                            90.0, 78.0, 82.0),
                    createOption("Maize -> Wheat -> Chickpea", 
                            80.0, 80.0, 75.0)
            );

            List<RotationOptionDto> ranked = cropRotationService.rankBySoilHealthBenefit(options);

            assertNotNull(ranked);
            assertEquals(3, ranked.size());
            assertEquals("Soybean -> Wheat -> Mustard", ranked.get(0).getCropSequence());
            assertEquals(90.0, ranked.get(0).getSoilHealthBenefit());
        }

        @Test
        @DisplayName("Should rank by climate resilience in descending order")
        void shouldRankByClimateResilience() {
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Pearl Millet -> Wheat -> Mustard", 
                            75.0, 90.0, 70.0),
                    createOption("Rice -> Wheat -> Greengram", 
                            70.0, 75.0, 80.0),
                    createOption("Maize -> Wheat -> Chickpea", 
                            80.0, 80.0, 75.0)
            );

            List<RotationOptionDto> ranked = cropRotationService.rankByClimateResilience(options);

            assertNotNull(ranked);
            assertEquals(3, ranked.size());
            assertEquals("Pearl Millet -> Wheat -> Mustard", ranked.get(0).getCropSequence());
            assertEquals(90.0, ranked.get(0).getClimateResilience());
        }

        @Test
        @DisplayName("Should rank by economic viability in descending order")
        void shouldRankByEconomicViability() {
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Rice -> Wheat -> Greengram", 
                            70.0, 75.0, 80.0),
                    createOption("Mango -> Banana -> Taro", 
                            60.0, 65.0, 95.0),
                    createOption("Cotton -> Wheat -> Mustard", 
                            75.0, 72.0, 88.0)
            );

            List<RotationOptionDto> ranked = cropRotationService.rankByEconomicViability(options);

            assertNotNull(ranked);
            assertEquals(3, ranked.size());
            assertEquals("Mango -> Banana -> Taro", ranked.get(0).getCropSequence());
            assertEquals(95.0, ranked.get(0).getEconomicViability());
        }

        @Test
        @DisplayName("Should calculate overall benefit score correctly")
        void shouldCalculateOverallBenefitScore() {
            RotationOptionDto option = createOption("Rice -> Wheat -> Greengram",
                    80.0, 75.0, 85.0);

            Double overall = cropRotationService.calculateOverallBenefitScore(option);

            assertNotNull(overall);
            assertEquals(80.0, overall);
        }

        @Test
        @DisplayName("Should return zero for null option in score calculation")
        void shouldReturnZeroForNullOption() {
            Double overall = cropRotationService.calculateOverallBenefitScore(null);
            assertEquals(0.0, overall);
        }

        @Test
        @DisplayName("Should handle null component scores gracefully")
        void shouldHandleNullComponentScores() {
            RotationOptionDto option = new RotationOptionDto();
            option.setCropSequence("Test");
            option.setSoilHealthBenefit(80.0);

            Double overall = cropRotationService.calculateOverallBenefitScore(option);

            assertNotNull(overall);
            // (80 + 0 + 0) / 3 = 26.67
            assertEquals(26.67, overall);
        }

        @Test
        @DisplayName("Should return empty list for empty input")
        void shouldReturnEmptyForEmptyInput() {
            List<RotationOptionDto> ranked = cropRotationService.rankRotationOptions(Collections.emptyList());
            assertNotNull(ranked);
            assertTrue(ranked.isEmpty());
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            List<RotationOptionDto> ranked = cropRotationService.rankRotationOptions(null);
            assertNull(ranked);
        }

        @Test
        @DisplayName("Should rank options with equal scores stably")
        void shouldRankOptionsWithEqualScoresStably() {
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Rice -> Wheat -> Greengram", 
                            80.0, 80.0, 80.0),
                    createOption("Maize -> Wheat -> Chickpea", 
                            80.0, 80.0, 80.0)
            );

            List<RotationOptionDto> ranked = cropRotationService.rankRotationOptions(options);

            assertNotNull(ranked);
            assertEquals(2, ranked.size());
        }
    }

    // ========================================
    // SECTION 4: DEFAULT PATTERNS FOR NEW FARMERS TESTS
    // Requirement 3.11
    // ========================================

    @Nested
    @DisplayName("Default Rotation Patterns Tests (Requirement 3.11)")
    class DefaultPatternsTests {

        @Test
        @DisplayName("Should return default patterns for Indo-Gangetic Plains zone")
        void shouldReturnDefaultsForIndoGangetic() {
            List<RotationOptionDto> defaults = rotationRankingDisplayService
                    .getDefaultRotationPatterns("Indo-Gangetic Plains");

            assertNotNull(defaults);
            assertFalse(defaults.isEmpty());
            assertTrue(defaults.size() >= 3);
            
            boolean hasRiceWheat = defaults.stream()
                    .anyMatch(o -> o.getCropSequence().contains("Rice") && 
                                   o.getCropSequence().contains("Wheat"));
            assertTrue(hasRiceWheat);
        }

        @Test
        @DisplayName("Should return default patterns for Western Dry Region zone")
        void shouldReturnDefaultsForWesternDry() {
            List<RotationOptionDto> defaults = rotationRankingDisplayService
                    .getDefaultRotationPatterns("Western Dry Region");

            assertNotNull(defaults);
            assertFalse(defaults.isEmpty());
            
            boolean hasPearlMillet = defaults.stream()
                    .anyMatch(o -> o.getCropSequence().contains("Pearl Millet"));
            assertTrue(hasPearlMillet);
        }

        @Test
        @DisplayName("Should use Indo-Gangetic Plains as fallback for unknown zone")
        void shouldUseFallbackForUnknownZone() {
            List<RotationOptionDto> defaults = rotationRankingDisplayService
                    .getDefaultRotationPatterns("Unknown Zone");

            assertNotNull(defaults);
            assertFalse(defaults.isEmpty());
            assertTrue(defaults.stream()
                    .anyMatch(o -> o.getCropSequence().contains("Rice")));
        }

        @Test
        @DisplayName("Should return default patterns with complete information")
        void shouldReturnDefaultsWithCompleteInfo() {
            List<RotationOptionDto> defaults = rotationRankingDisplayService
                    .getDefaultRotationPatterns("Indo-Gangetic Plains");

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
        void shouldReturnDefaultsForSouthernPlateau() {
            List<RotationOptionDto> defaults = rotationRankingDisplayService
                    .getDefaultRotationPatterns("Southern Plateau and Hills");

            assertNotNull(defaults);
            assertFalse(defaults.isEmpty());
            
            boolean hasGroundnut = defaults.stream()
                    .anyMatch(o -> o.getCropSequence().contains("Groundnut"));
            assertTrue(hasGroundnut);
        }

        @Test
        @DisplayName("Should detect no crop history for null history")
        void shouldDetectNoHistoryForNull() {
            assertTrue(rotationRankingDisplayService.hasNoCropHistory(null));
        }

        @Test
        @DisplayName("Should detect no crop history for empty list")
        void shouldDetectNoHistoryForEmpty() {
            assertTrue(rotationRankingDisplayService.hasNoCropHistory(Collections.emptyList()));
        }

        @Test
        @DisplayName("Should detect crop history for non-empty list")
        void shouldDetectHistoryForNonEmpty() {
            List<CropHistoryEntryDto> history = List.of(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3), "KHARIF")
            );
            assertFalse(rotationRankingDisplayService.hasNoCropHistory(history));
        }

        @Test
        @DisplayName("Should create complete rotation display with defaults for no history")
        void shouldCreateCompleteDisplayWithDefaultsForNoHistory() {
            RotationRecommendationResultDto result = rotationRankingDisplayService
                    .createCompleteRotationDisplay(Collections.emptyList(), "Indo-Gangetic Plains", false);

            assertNotNull(result);
            assertNotNull(result.getDefaultPatterns());
            assertFalse(result.getDefaultPatterns().isEmpty());
        }

        @Test
        @DisplayName("Should use defaults as main options when no options and no history")
        void shouldUseDefaultsAsOptionsWhenNoHistory() {
            RotationRecommendationResultDto result = rotationRankingDisplayService
                    .createCompleteRotationDisplay(Collections.emptyList(), "Western Dry Region", false);

            assertNotNull(result);
            assertNotNull(result.getOptions());
            assertFalse(result.getOptions().isEmpty());
            assertNotNull(result.getDefaultPatterns());
        }

        @Test
        @DisplayName("Should generate season-wise schedules for default patterns")
        void shouldGenerateSeasonSchedulesForDefaults() {
            List<RotationOptionDto> defaults = rotationRankingDisplayService
                    .getDefaultRotationPatterns("Indo-Gangetic Plains");

            List<RotationOptionDto> withSchedules = rotationRankingDisplayService
                    .generateSeasonWiseSchedules(defaults);

            assertNotNull(withSchedules);
            assertFalse(withSchedules.isEmpty());
            
            for (RotationOptionDto option : withSchedules) {
                assertNotNull(option.getKharifCrops());
                assertNotNull(option.getRabiCrops());
            }
        }

        @Test
        @DisplayName("Should add residue management recommendations for defaults")
        void shouldAddResidueManagementForDefaults() {
            List<RotationOptionDto> defaults = rotationRankingDisplayService
                    .getDefaultRotationPatterns("Indo-Gangetic Plains");

            List<RotationOptionDto> withRecs = rotationRankingDisplayService
                    .addResidueManagementRecommendations(defaults);

            assertNotNull(withRecs);
            for (RotationOptionDto option : withRecs) {
                assertNotNull(option.getResidueManagementRecommendation());
                assertFalse(option.getResidueManagementRecommendation().isEmpty());
                assertNotNull(option.getOrganicMatterImpact());
            }
        }
    }

    // ========================================
    // INTEGRATION TESTS
    // ========================================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should generate complete rotation recommendations with history")
        void shouldGenerateCompleteRecommendationsWithHistory() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(2), "RABI")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = rotationRecommendationEngine.generateRecommendations(request);

            assertNotNull(result);
            assertFalse(result.getOptions().isEmpty());
            assertNotNull(result.getPestRiskLevel());
        }

        @Test
        @DisplayName("Should generate recommendations for farmer with no history")
        void shouldGenerateRecommendationsForNoHistory() {
            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(Collections.emptyList())
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = rotationRecommendationEngine.generateRecommendations(request);

            assertNotNull(result);
            assertFalse(result.getOptions().isEmpty());
        }

        @Test
        @DisplayName("Should rank options and add season schedules in display service")
        void shouldRankAndAddSchedulesInDisplayService() {
            List<RotationOptionDto> options = Arrays.asList(
                    createOption("Rice -> Wheat -> Greengram",
                            80.0, 75.0, 85.0),
                    createOption("Maize -> Chickpea -> Sesame",
                            85.0, 80.0, 78.0)
            );

            List<RotationOptionDto> ranked = cropRotationService.rankRotationOptions(options);
            List<RotationOptionDto> withSchedules = rotationRankingDisplayService
                    .generateSeasonWiseSchedules(ranked);

            assertNotNull(withSchedules);
            assertEquals(2, withSchedules.size());
            assertEquals("Maize -> Chickpea -> Sesame", withSchedules.get(0).getCropSequence());
        }

        @Test
        @DisplayName("Should handle complete workflow from history to display")
        void shouldHandleCompleteWorkflow() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

            CropHistoryAnalysisResultDto analysis = cropHistoryAnalyzer.analyzeCropHistory(history);

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto recommendations = rotationRecommendationEngine
                    .generateRecommendations(request);

            RotationRecommendationResultDto display = rotationRankingDisplayService
                    .createCompleteRotationDisplay(
                            recommendations.getOptions(),
                            "Indo-Gangetic Plains",
                            analysis.isHasSufficientHistory());

            assertNotNull(display);
            assertNotNull(display.getOptions());
            assertNotNull(display.getDefaultPatterns());
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private CropHistoryEntryDto createCropEntry(String cropName, LocalDate sowingDate, String season) {
        return CropHistoryEntryDto.builder()
                .cropId(System.currentTimeMillis())
                .cropName(cropName)
                .cropVariety("Common")
                .sowingDate(sowingDate)
                .expectedHarvestDate(sowingDate.plusMonths(4))
                .areaAcres(2.5)
                .season(season)
                .status("HARVESTED")
                .cropFamily(CropFamily.getFamilyForCrop(cropName))
                .rootDepth(CropFamily.getRootDepthForCrop(cropName))
                .build();
    }

    private RotationOptionDto createOption(String sequence, Double soilHealth, 
            Double climate, Double economic) {
        return RotationOptionDto.builder()
                .id(System.nanoTime())
                .cropSequence(sequence)
                .description("Test rotation option")
                .soilHealthBenefit(soilHealth)
                .climateResilience(climate)
                .economicViability(economic)
                .overallBenefitScore(cropRotationService.calculateOverallBenefitScore(
                        RotationOptionDto.builder()
                                .soilHealthBenefit(soilHealth)
                                .climateResilience(climate)
                                .economicViability(economic)
                                .build()))
                .build();
    }
}

