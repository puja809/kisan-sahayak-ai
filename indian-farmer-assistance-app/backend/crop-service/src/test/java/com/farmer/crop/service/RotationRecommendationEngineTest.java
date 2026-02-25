package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.enums.CropFamily;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RotationRecommendationEngine.
 * 
 * Tests nutrient cycling optimization, legume integration, rice diversification,
 * intercropping suggestions, and pest/disease carryover risk assessment.
 * 
 * Requirements: 3.3, 3.4, 3.5, 3.6, 3.7
 */
class RotationRecommendationEngineTest {

    private RotationRecommendationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RotationRecommendationEngine();
    }

    @Nested
    @DisplayName("Nutrient Cycling Optimization Tests (Requirement 3.3)")
    class NutrientCyclingTests {

        @Test
        @DisplayName("Should recommend deep-rooted crops after shallow-rooted cabbage")
        void shouldRecommendDeepRootedAfterShallow() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Cabbage", LocalDate.now().minusMonths(4), "RABI")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            assertFalse(result.getOptions().isEmpty());
            
            // Check that deep-rooted options are present
            boolean hasDeepRootedOption = result.getOptions().stream()
                    .anyMatch(opt -> opt.getCropSequence().contains("Sunflower") || 
                                     opt.getCropSequence().contains("Sorghum"));
            assertTrue(hasDeepRootedOption, "Should recommend deep-rooted crops after shallow-rooted");
        }

        @Test
        @DisplayName("Should recommend shallow-rooted crops after deep-rooted sunflower")
        void shouldRecommendShallowRootedAfterDeep() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Sunflower", LocalDate.now().minusMonths(4), "RABI")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            assertFalse(result.getOptions().isEmpty());
            
            // Check that shallow-rooted options are present
            boolean hasShallowRootedOption = result.getOptions().stream()
                    .anyMatch(opt -> opt.getCropSequence().contains("Cabbage") || 
                                     opt.getCropSequence().contains("Mustard"));
            assertTrue(hasShallowRootedOption, "Should recommend shallow-rooted crops after deep-rooted");
        }

        @Test
        @DisplayName("Should include balanced rotation option with deep-shallow-legume sequence")
        void shouldIncludeBalancedRotationOption() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            // Check for balanced rotation option
            boolean hasBalancedOption = result.getOptions().stream()
                    .anyMatch(opt -> opt.getCropSequence().contains("Sunflower") && 
                                     opt.getCropSequence().contains("Cabbage") && 
                                     opt.getCropSequence().contains("Greengram"));
            assertTrue(hasBalancedOption, "Should include balanced rotation option");
        }

        @Test
        @DisplayName("Should calculate high nutrient cycling score for alternating depths")
        void shouldCalculateHighNutrientScoreForAlternatingDepths() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Cabbage", LocalDate.now().minusMonths(4), "RABI")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            // Find a deep-rooted option and check its nutrient cycling score
            result.getOptions().stream()
                    .filter(opt -> opt.getCropSequence().contains("Sunflower"))
                    .findFirst()
                    .ifPresent(opt -> {
                        assertTrue(opt.getNutrientCyclingScore().compareTo(BigDecimal.ZERO) > 0);
                        assertTrue(opt.getNutrientCyclingScore().compareTo(new BigDecimal("80")) > 0,
                                "Alternating depths should have high nutrient cycling score");
                    });
        }
    }

    @Nested
    @DisplayName("Legume Integration Tests (Requirement 3.4)")
    class LegumeIntegrationTests {

        @Test
        @DisplayName("Should recommend legumes for nitrogen fixation after non-legume crops")
        void shouldRecommendLegumesForNitrogenFixation() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(2), "RABI")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            // Check for legume options
            boolean hasLegumeOption = result.getOptions().stream()
                    .anyMatch(opt -> opt.getCropSequence().contains("Greengram") || 
                                     opt.getCropSequence().contains("Blackgram") || 
                                     opt.getCropSequence().contains("Redgram") ||
                                     opt.getCropSequence().contains("Chickpea"));
            assertTrue(hasLegumeOption, "Should recommend legumes for nitrogen fixation");
        }

        @Test
        @DisplayName("Should not recommend legumes after legumes")
        void shouldNotRecommendLegumesAfterLegumes() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Greengram", LocalDate.now().minusMonths(4), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            // Check that legume integration options are not prominent
            long legumeCount = result.getOptions().stream()
                    .filter(opt -> opt.getDescription().contains("nitrogen fixation"))
                    .count();
            assertEquals(0, legumeCount, "Should not recommend legumes after legumes");
        }

        @Test
        @DisplayName("Legume options should mention nitrogen fixation benefits")
        void legumeOptionsShouldMentionNitrogenFixation() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            // Find legume option and check benefits
            result.getOptions().stream()
                    .filter(opt -> opt.getDescription().contains("nitrogen fixation"))
                    .findFirst()
                    .ifPresent(opt -> {
                        assertNotNull(opt.getBenefits());
                        assertTrue(opt.getBenefits().stream()
                                .anyMatch(b -> b.toLowerCase().contains("nitrogen")),
                                "Should mention nitrogen fixation in benefits");
                    });
        }
    }

    @Nested
    @DisplayName("Rice-Based System Diversification Tests (Requirement 3.5)")
    class RiceDiversificationTests {

        @Test
        @DisplayName("Should detect rice-based system and recommend diversification")
        void shouldDetectRiceSystemAndRecommendDiversification() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF"),
                    createCropEntry("Rice", LocalDate.now().minusMonths(2), "RABI")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            assertTrue(result.isHasRiceBasedSystem(), "Should detect rice-based system");
        }

        @Test
        @DisplayName("Rice diversification should recommend greengram, blackgram, and oilseeds")
        void shouldRecommendDiversificationCrops() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            // Check for diversification crops
            boolean hasGreengram = result.getOptions().stream()
                    .anyMatch(opt -> opt.getCropSequence().contains("Greengram"));
            boolean hasBlackgram = result.getOptions().stream()
                    .anyMatch(opt -> opt.getCropSequence().contains("Blackgram"));
            boolean hasOilseed = result.getOptions().stream()
                    .anyMatch(opt -> opt.getCropSequence().contains("Sunflower") || 
                                    opt.getCropSequence().contains("Mustard") ||
                                    opt.getCropSequence().contains("Sesame"));
            
            assertTrue(hasGreengram, "Should recommend greengram for rice diversification");
            assertTrue(hasBlackgram, "Should recommend blackgram for rice diversification");
            assertTrue(hasOilseed, "Should recommend oilseeds for rice diversification");
        }

        @Test
        @DisplayName("Rice diversification options should mention residual moisture utilization")
        void shouldMentionResidualMoisture() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            // Check that options mention residual moisture
            boolean mentionsMoisture = result.getOptions().stream()
                    .anyMatch(opt -> opt.getDescription().toLowerCase().contains("residual") ||
                                    opt.getBenefits().stream()
                                        .anyMatch(b -> b.toLowerCase().contains("moisture")));
            assertTrue(mentionsMoisture, "Should mention residual moisture utilization");
        }

        @Test
        @DisplayName("Should add warning for rice-based systems")
        void shouldAddWarningForRiceSystem() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            assertFalse(result.getWarnings().isEmpty(), "Should add warning for rice-based system");
            assertTrue(result.getWarnings().stream()
                    .anyMatch(w -> w.toLowerCase().contains("rice")),
                    "Warning should mention rice-based system");
        }
    }

    @Nested
    @DisplayName("Intercropping and Relay Cropping Tests (Requirement 3.6)")
    class IntercroppingTests {

        @Test
        @DisplayName("Should recommend paira/utera cropping for rice")
        void shouldRecommendPairaUteraForRice() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

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
            assertTrue(hasRelayOption, "Should recommend paira/utera cropping for rice");
        }

        @Test
        @DisplayName("Should recommend lentil or gram for relay cropping into maturing rice")
        void shouldRecommendLentilGramForRiceRelay() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            // Check for lentil or gram in relay options
            boolean hasRelayCrop = result.getOptions().stream()
                    .anyMatch(opt -> (opt.getCropSequence().contains("Lentil") || 
                                     opt.getCropSequence().contains("Chickpea") ||
                                     opt.getCropSequence().contains("Gram")) &&
                                    opt.getDescription().toLowerCase().contains("relay"));
            assertTrue(hasRelayCrop, "Should recommend lentil/gram for relay cropping");
        }

        @Test
        @DisplayName("Should recommend intercropping for maize with cowpea or greengram")
        void shouldRecommendIntercroppingForMaize() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Maize", LocalDate.now().minusMonths(4), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            // Check for intercropping options
            boolean hasIntercropOption = result.getOptions().stream()
                    .anyMatch(opt -> opt.getDescription().toLowerCase().contains("intercrop"));
            assertTrue(hasIntercropOption, "Should recommend intercropping for maize");
        }

        @Test
        @DisplayName("Intercropping options should mention land use efficiency")
        void intercroppingShouldMentionLandEfficiency() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            // Check that intercropping options mention land use efficiency
            result.getOptions().stream()
                    .filter(opt -> opt.getDescription().toLowerCase().contains("intercrop") ||
                                   opt.getDescription().toLowerCase().contains("relay"))
                    .findFirst()
                    .ifPresent(opt -> {
                        assertNotNull(opt.getBenefits());
                        assertTrue(opt.getBenefits().stream()
                                .anyMatch(b -> b.toLowerCase().contains("land") || 
                                              b.toLowerCase().contains("efficiency")),
                                "Should mention land use efficiency");
                    });
        }
    }

    @Nested
    @DisplayName("Pest and Disease Carryover Risk Assessment Tests (Requirement 3.7)")
    class PestRiskAssessmentTests {

        @Test
        @DisplayName("Should detect pest carryover risk for consecutive cereal crops")
        void shouldDetectPestRiskForConsecutiveCereals() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF"),
                    createCropEntry("Rice", LocalDate.now().minusMonths(2), "RABI")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            assertTrue(result.isHasPestDiseaseRisk() || 
                      result.getWarnings().stream().anyMatch(w -> w.toLowerCase().contains("pest")),
                    "Should detect pest carryover risk for consecutive rice");
        }

        @Test
        @DisplayName("Should assess HIGH risk level for multiple consecutive same-family crops")
        void shouldAssessHighRiskForConsecutiveMonoculture() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(10), "KHARIF"),
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "RABI"),
                    createCropEntry("Rice", LocalDate.now().minusMonths(2), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            assertEquals("HIGH", result.getPestRiskLevel(), 
                    "Should assess HIGH risk for multiple consecutive rice crops");
        }

        @Test
        @DisplayName("Should assess LOW risk for diverse crop rotation")
        void shouldAssessLowRiskForDiverseRotation() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF"),
                    createCropEntry("Wheat", LocalDate.now().minusMonths(2), "RABI"),
                    createCropEntry("Greengram", LocalDate.now().minusMonths(1), "ZAID")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            assertEquals("LOW", result.getPestRiskLevel(), 
                    "Should assess LOW risk for diverse crop rotation");
        }

        @Test
        @DisplayName("Should mention specific pests for high-risk crops")
        void shouldMentionSpecificPests() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            // Check that warnings mention specific pests
            boolean mentionsPests = result.getWarnings().stream()
                    .anyMatch(w -> w.toLowerCase().contains("blast") ||
                                  w.toLowerCase().contains("brown planthopper") ||
                                  w.toLowerCase().contains("bacterial"));
            assertTrue(mentionsPests, "Should mention specific pests for rice");
        }

        @Test
        @DisplayName("Should provide pest management recommendations")
        void shouldProvidePestManagementRecommendations() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Cotton", LocalDate.now().minusMonths(6), "KHARIF"),
                    createCropEntry("Cotton", LocalDate.now().minusMonths(2), "RABI")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            assertFalse(result.getRecommendations().isEmpty(), 
                    "Should provide pest management recommendations");
        }
    }

    @Nested
    @DisplayName("General Rotation Tests")
    class GeneralRotationTests {

        @Test
        @DisplayName("Should handle empty crop history")
        void shouldHandleEmptyHistory() {
            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(new ArrayList<>())
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            assertFalse(result.getOptions().isEmpty(), 
                    "Should provide options even with empty history");
        }

        @Test
        @DisplayName("Should handle null crop history")
        void shouldHandleNullHistory() {
            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(null)
                    .season("KHARIF")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            assertFalse(result.getOptions().isEmpty(), 
                    "Should provide options even with null history");
        }

        @Test
        @DisplayName("Should rank options by overall benefit score in descending order")
        void shouldRankOptionsByBenefitScore() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            assertFalse(result.getOptions().isEmpty());
            
            // Verify descending order
            List<RotationOptionDto> options = result.getOptions();
            for (int i = 0; i < options.size() - 1; i++) {
                assertTrue(options.get(i).getOverallBenefitScore()
                        .compareTo(options.get(i + 1).getOverallBenefitScore()) >= 0,
                        "Options should be sorted in descending order by benefit score");
            }
        }

        @Test
        @DisplayName("All options should have complete benefit information")
        void shouldHaveCompleteBenefitInformation() {
            List<CropHistoryEntryDto> history = Arrays.asList(
                    createCropEntry("Rice", LocalDate.now().minusMonths(6), "KHARIF")
            );

            RotationRecommendationRequestDto request = RotationRecommendationRequestDto.builder()
                    .cropHistory(history)
                    .season("RABI")
                    .build();

            RotationRecommendationResultDto result = engine.generateRecommendations(request);

            assertNotNull(result);
            
            for (RotationOptionDto option : result.getOptions()) {
                assertNotNull(option.getCropSequence());
                assertNotNull(option.getDescription());
                assertNotNull(option.getSoilHealthBenefit());
                assertNotNull(option.getClimateResilience());
                assertNotNull(option.getEconomicViability());
                assertNotNull(option.getNutrientCyclingScore());
                assertNotNull(option.getPestManagementScore());
                assertNotNull(option.getOverallBenefitScore());
                assertNotNull(option.getBenefits());
                assertFalse(option.getBenefits().isEmpty());
            }
        }
    }

    // Helper methods

    private CropHistoryEntryDto createCropEntry(String cropName, LocalDate sowingDate, String season) {
        return CropHistoryEntryDto.builder()
                .cropId(System.currentTimeMillis())
                .cropName(cropName)
                .cropVariety("Common")
                .sowingDate(sowingDate)
                .expectedHarvestDate(sowingDate.plusMonths(4))
                .areaAcres(new BigDecimal("2.5"))
                .season(season)
                .status("HARVESTED")
                .cropFamily(CropFamily.getFamilyForCrop(cropName))
                .rootDepth(CropFamily.getRootDepthForCrop(cropName))
                .build();
    }
}