package com.farmer.crop.service;

import com.farmer.crop.dto.CropHistoryAnalysisResultDto;
import com.farmer.crop.dto.CropHistoryEntryDto;
import com.farmer.crop.dto.NutrientDepletionRiskDto;
import com.farmer.crop.enums.CropFamily;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CropHistoryAnalyzer.
 * 
 * Tests the core functionality of analyzing crop history for rotation patterns
 * and nutrient depletion risks.
 * 
 * Requirements: 3.1, 3.2
 */
class CropHistoryAnalyzerTest {

    private CropHistoryAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new CropHistoryAnalyzer();
    }

    @Nested
    @DisplayName("Empty or null history tests")
    class EmptyHistoryTests {

        @Test
        @DisplayName("Should return empty analysis for null crop history")
        void nullHistoryReturnsEmptyAnalysis() {
            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(null);

            assertNotNull(result);
            assertFalse(result.isHasSufficientHistory());
            assertEquals(0, result.getSeasonsAnalyzed());
            assertTrue(result.getNutrientDepletionRisks().isEmpty());
        }

        @Test
        @DisplayName("Should return empty analysis for empty crop history")
        void emptyHistoryReturnsEmptyAnalysis() {
            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(Collections.emptyList());

            assertNotNull(result);
            assertFalse(result.isHasSufficientHistory());
            assertEquals(0, result.getSeasonsAnalyzed());
            assertTrue(result.getNutrientDepletionRisks().isEmpty());
        }

        @Test
        @DisplayName("Should return empty analysis for single crop history")
        void singleCropHistoryReturnsInsufficientHistory() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertNotNull(result);
            assertFalse(result.isHasSufficientHistory());
            assertEquals(1, result.getSeasonsAnalyzed());
            assertTrue(result.getNutrientDepletionRisks().isEmpty());
        }
    }

    @Nested
    @DisplayName("Consecutive monoculture detection tests")
    class ConsecutiveMonocultureTests {

        @Test
        @DisplayName("Should detect consecutive cereal planting")
        void detectConsecutiveCereals() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9)),
                    createCropEntry("Rice", LocalDate.now().minusMonths(15))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertTrue(result.isHasSufficientHistory());
            assertFalse(result.getNutrientDepletionRisks().isEmpty());
            
            NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().get(0);
            assertEquals(CropFamily.CEREALS, risk.getCropFamily());
            assertTrue(risk.getConsecutiveSeasons() >= 2);
        }

        @Test
        @DisplayName("Should detect consecutive legume planting")
        void detectConsecutiveLegumes() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Greengram", LocalDate.now().minusMonths(3)),
                    createCropEntry("Blackgram", LocalDate.now().minusMonths(9)),
                    createCropEntry("Chickpea", LocalDate.now().minusMonths(15))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertTrue(result.isHasSufficientHistory());
            assertFalse(result.getNutrientDepletionRisks().isEmpty());
            
            NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().get(0);
            assertEquals(CropFamily.LEGUMES, risk.getCropFamily());
        }

        @Test
        @DisplayName("Should not flag good rotation with different families")
        void goodRotationShouldNotFlagRisks() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),     // Cereals
                    createCropEntry("Greengram", LocalDate.now().minusMonths(9)), // Legumes
                    createCropEntry("Wheat", LocalDate.now().minusMonths(15))     // Cereals
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertTrue(result.isHasSufficientHistory());
            // With legumes in between, should not flag as consecutive
            assertTrue(result.getSummary().isHasGoodRotation());
        }

        @Test
        @DisplayName("Should detect 3 consecutive seasons of same family as critical")
        void threeConsecutiveSeasonsCritical() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9)),
                    createCropEntry("Maize", LocalDate.now().minusMonths(15))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertFalse(result.getNutrientDepletionRisks().isEmpty());
            
            NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().stream()
                    .filter(r -> r.getRiskLevel() == NutrientDepletionRiskDto.RiskLevel.CRITICAL)
                    .findFirst()
                    .orElse(null);
            
            assertNotNull(risk, "Should have a critical risk for 3 consecutive seasons");
            assertEquals(3, risk.getConsecutiveSeasons());
        }
    }

    @Nested
    @DisplayName("Nutrient depletion risk assessment tests")
    class NutrientDepletionRiskTests {

        @Test
        @DisplayName("Should identify nitrogen depletion for cereal monoculture")
        void cerealMonocultureNitrogenDepletion() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().stream()
                    .filter(r -> r.getCropFamily() == CropFamily.CEREALS)
                    .findFirst()
                    .orElse(null);
            
            assertNotNull(risk);
            assertTrue(risk.getAffectedNutrients().contains("Nitrogen"));
        }

        @Test
        @DisplayName("Should identify potassium depletion for brassica monoculture")
        void brassicaMonoculturePotassiumDepletion() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Cabbage", LocalDate.now().minusMonths(3)),
                    createCropEntry("Cauliflower", LocalDate.now().minusMonths(9))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().stream()
                    .filter(r -> r.getCropFamily() == CropFamily.BRASSICAS)
                    .findFirst()
                    .orElse(null);
            
            assertNotNull(risk);
            assertTrue(risk.getAffectedNutrients().contains("Potassium"));
        }

        @Test
        @DisplayName("Should calculate severity score based on consecutive seasons")
        void severityScoreIncreasesWithConsecutiveSeasons() {
            List<CropHistoryEntryDto> twoSeasons = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9))
            );

            List<CropHistoryEntryDto> threeSeasons = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9)),
                    createCropEntry("Maize", LocalDate.now().minusMonths(15))
            );

            CropHistoryAnalysisResultDto result2 = analyzer.analyzeCropHistory(twoSeasons);
            CropHistoryAnalysisResultDto result3 = analyzer.analyzeCropHistory(threeSeasons);

            Double score2 = result2.getNutrientDepletionRisks().stream()
                    .map(NutrientDepletionRiskDto::getSeverityScore)
                    .findFirst()
                    .orElse(0.0);
            
            Double score3 = result3.getNutrientDepletionRisks().stream()
                    .map(NutrientDepletionRiskDto::getSeverityScore)
                    .findFirst()
                    .orElse(0.0);

            assertTrue(score3 > score2, 
                    "Severity score should increase with more consecutive seasons");
        }
    }

    @Nested
    @DisplayName("Helper method tests")
    class HelperMethodTests {

        @Test
        @DisplayName("hasConsecutiveMonoculture should return true for consecutive same family")
        void hasConsecutiveMonocultureReturnsTrue() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9))
            );

            assertTrue(analyzer.hasConsecutiveMonoculture(history));
        }

        @Test
        @DisplayName("hasConsecutiveMonoculture should return false for good rotation")
        void hasConsecutiveMonocultureReturnsFalse() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Greengram", LocalDate.now().minusMonths(9))
            );

            assertFalse(analyzer.hasConsecutiveMonoculture(history));
        }

        @Test
        @DisplayName("getMaxConsecutiveSeasons should return correct count")
        void getMaxConsecutiveSeasonsReturnsCorrectCount() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9)),
                    createCropEntry("Maize", LocalDate.now().minusMonths(15))
            );

            assertEquals(3, analyzer.getMaxConsecutiveSeasons(history));
        }

        @Test
        @DisplayName("getMaxConsecutiveSeasons should return 0 for empty history")
        void getMaxConsecutiveSeasonsReturnsZeroForEmpty() {
            assertEquals(0, analyzer.getMaxConsecutiveSeasons(Collections.emptyList()));
            assertEquals(0, analyzer.getMaxConsecutiveSeasons(null));
        }
    }

    @Nested
    @DisplayName("Analysis summary tests")
    class AnalysisSummaryTests {

        @Test
        @DisplayName("Should correctly identify dominant crop family")
        void identifyDominantCropFamily() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9)),
                    createCropEntry("Rice", LocalDate.now().minusMonths(15)),
                    createCropEntry("Greengram", LocalDate.now().minusMonths(21))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertEquals("Cereals", result.getSummary().getDominantCropFamily());
        }

        @Test
        @DisplayName("Should assess nutrient balance correctly")
        void assessNutrientBalance() {
            List<CropHistoryEntryDto> historyWithLegumes = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Greengram", LocalDate.now().minusMonths(9))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(historyWithLegumes);

            assertTrue(result.getSummary().getNutrientBalanceAssessment().contains("Good") ||
                       result.getSummary().getNutrientBalanceAssessment().contains("Moderate"));
        }

        @Test
        @DisplayName("Should assess pest disease risk correctly")
        void assessPestDiseaseRisk() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9)),
                    createCropEntry("Maize", LocalDate.now().minusMonths(15))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertNotNull(result.getSummary().getPestDiseaseRiskLevel());
            assertFalse(result.getSummary().getPestDiseaseRiskLevel().isEmpty());
        }
    }

    @Nested
    @DisplayName("Recommendation generation tests")
    class RecommendationTests {

        @Test
        @DisplayName("Should generate recommendations for identified risks")
        void generateRecommendationsForRisks() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertFalse(result.getRecommendations().isEmpty());
            assertTrue(result.getRecommendations().stream()
                    .anyMatch(r -> r.toLowerCase().contains("legume") || 
                                   r.toLowerCase().contains("nitrogen")));
        }

        @Test
        @DisplayName("Should recommend legumes when not present in rotation")
        void recommendLegumesWhenNotPresent() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(9)),
                    createCropEntry("Maize", LocalDate.now().minusMonths(15))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertTrue(result.getRecommendations().stream()
                    .anyMatch(r -> r.toLowerCase().contains("legume") || 
                                   r.toLowerCase().contains("greengram") ||
                                   r.toLowerCase().contains("blackgram")));
        }

        @Test
        @DisplayName("Should recommend varied root depths when not present")
        void recommendVariedRootDepths() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Cabbage", LocalDate.now().minusMonths(3)),
                    createCropEntry("Cauliflower", LocalDate.now().minusMonths(9)),
                    createCropEntry("Broccoli", LocalDate.now().minusMonths(15))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertTrue(result.getRecommendations().stream()
                    .anyMatch(r -> r.toLowerCase().contains("deep-rooted") || 
                                   r.toLowerCase().contains("sunflower")));
        }
    }

    @Nested
    @DisplayName("Crop family classification tests")
    class CropFamilyClassificationTests {

        @Test
        @DisplayName("Should correctly classify rice as cereal")
        void classifyRiceAsCereal() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertEquals(CropFamily.CEREALS, result.getCropHistory().get(0).getCropFamily());
        }

        @Test
        @DisplayName("Should correctly classify greengram as legume")
        void classifyGreengramAsLegume() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Greengram", LocalDate.now().minusMonths(3))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertEquals(CropFamily.LEGUMES, result.getCropHistory().get(0).getCropFamily());
        }

        @Test
        @DisplayName("Should correctly classify cabbage as brassica")
        void classifyCabbageAsBrassica() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Cabbage", LocalDate.now().minusMonths(3))
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertEquals(CropFamily.BRASSICAS, result.getCropHistory().get(0).getCropFamily());
        }

        @Test
        @DisplayName("Should correctly identify root depth for crops")
        void identifyRootDepth() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(3)),     // Cereals - deep
                    createCropEntry("Cabbage", LocalDate.now().minusMonths(9))   // Brassicas - shallow
            );

            CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

            assertEquals(CropFamily.RootDepth.DEEP, result.getCropHistory().get(0).getRootDepth());
            assertEquals(CropFamily.RootDepth.SHALLOW, result.getCropHistory().get(1).getRootDepth());
        }
    }

    /**
     * Helper method to create a crop history entry.
     */
    private CropHistoryEntryDto createCropEntry(String cropName, LocalDate sowingDate) {
        return CropHistoryEntryDto.builder()
                .cropId((long) cropName.hashCode())
                .cropName(cropName)
                .sowingDate(sowingDate)
                .areaAcres(2.5)
                .season("KHARIF")
                .status("HARVESTED")
                .build();
    }
}

