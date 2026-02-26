package com.farmer.crop.service;

import com.farmer.crop.dto.CropHistoryAnalysisResultDto;
import com.farmer.crop.dto.CropHistoryEntryDto;
import com.farmer.crop.dto.NutrientDepletionRiskDto;
import com.farmer.crop.enums.CropFamily;
import net.jqwik.api.*;


import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Nutrient Depletion Risk Detection.
 * 
 * Property 8: Nutrient Depletion Risk Detection
 * For any crop history sequence, if the same crop family is grown consecutively 
 * for 3 or more seasons, the system should flag a nutrient depletion risk for that crop family.
 * 
 * Validates: Requirements 3.2
 */
class NutrientDepletionRiskPropertyTest {

    private final CropHistoryAnalyzer analyzer = new CropHistoryAnalyzer();

    /**
     * Generator for valid crop names from known families.
     */
    @Provide
    Arbitrary<String> validCropName() {
        return Arbitraries.of("Rice", "Wheat", "Maize", "Greengram", "Blackgram", 
                "Chickpea", "Cabbage", "Cauliflower", "Tomato", "Potato", 
                "Cucumber", "Bitter Gourd", "Sunflower", "Sesame", "Groundnut");
    }

    /**
     * Generator for cereal crop names.
     */
    @Provide
    Arbitrary<String> cerealCropName() {
        return Arbitraries.of("Rice", "Wheat", "Maize", "Barley", "Sorghum");
    }

    /**
     * Generator for legume crop names.
     */
    @Provide
    Arbitrary<String> legumeCropName() {
        return Arbitraries.of("Greengram", "Blackgram", "Chickpea", "Lentil", "Peas");
    }

    /**
     * Generator for brassica crop names.
     */
    @Provide
    Arbitrary<String> brassicaCropName() {
        return Arbitraries.of("Cabbage", "Cauliflower", "Mustard", "Broccoli");
    }

    /**
     * Property 8.1: Same crop family for 3+ consecutive seasons should always flag a risk.
     * 
     * For any crop history sequence where the same crop family is grown consecutively
     * for 3 or more seasons, the system should flag a nutrient depletion risk.
     */
    @Property
    void threeConsecutiveCerealsShouldFlagRisk(
            @ForAll("cerealCropName") String crop1,
            @ForAll("cerealCropName") String crop2,
            @ForAll("cerealCropName") String crop3) {
        // Arrange - Create 3 consecutive seasons of cereals
        List<CropHistoryEntryDto> history = Arrays.asList(
                createCropEntry(crop1, LocalDate.now().minusMonths(3)),
                createCropEntry(crop2, LocalDate.now().minusMonths(9)),
                createCropEntry(crop3, LocalDate.now().minusMonths(15))
        );

        // Act
        CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

        // Assert - Should have at least one risk flagged
        assertFalse(result.getNutrientDepletionRisks().isEmpty(), 
                "Should flag risk for 3+ consecutive seasons of same family: " + crop1 + ", " + crop2 + ", " + crop3);
        
        // Verify the risk is for the correct family
        NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().get(0);
        assertNotNull(risk.getCropFamily());
        assertEquals(CropFamily.CEREALS, risk.getCropFamily());
        assertTrue(risk.getConsecutiveSeasons() >= 3);
        assertTrue(risk.getSeverityScore().compareTo(0.0) > 0);
    }

    /**
     * Property 8.1b: Same crop family for 3+ consecutive seasons should always flag a risk (legumes).
     */
    @Property
    void threeConsecutiveLegumesShouldFlagRisk(
            @ForAll("legumeCropName") String crop1,
            @ForAll("legumeCropName") String crop2,
            @ForAll("legumeCropName") String crop3) {
        // Arrange - Create 3 consecutive seasons of legumes
        List<CropHistoryEntryDto> history = Arrays.asList(
                createCropEntry(crop1, LocalDate.now().minusMonths(3)),
                createCropEntry(crop2, LocalDate.now().minusMonths(9)),
                createCropEntry(crop3, LocalDate.now().minusMonths(15))
        );

        // Act
        CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

        // Assert - Should have at least one risk flagged
        assertFalse(result.getNutrientDepletionRisks().isEmpty(), 
                "Should flag risk for 3+ consecutive seasons of legumes: " + crop1 + ", " + crop2 + ", " + crop3);
        
        NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().get(0);
        assertNotNull(risk.getCropFamily());
        assertEquals(CropFamily.LEGUMES, risk.getCropFamily());
    }

    /**
     * Property 8.2: Two consecutive seasons should flag a risk.
     */
    @Property
    void twoConsecutiveSeasonsShouldFlagRisk(
            @ForAll("cerealCropName") String crop1,
            @ForAll("cerealCropName") String crop2) {
        // Arrange - Create exactly 2 consecutive seasons of same family
        List<CropHistoryEntryDto> history = Arrays.asList(
                createCropEntry(crop1, LocalDate.now().minusMonths(3)),
                createCropEntry(crop2, LocalDate.now().minusMonths(9))
        );

        // Act
        CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

        // Assert
        assertFalse(result.getNutrientDepletionRisks().isEmpty(), 
                "Should flag risk for 2 consecutive seasons: " + crop1 + ", " + crop2);
        
        NutrientDepletionRiskDto risk = result.getNutrientDepletionRisks().get(0);
        assertEquals(2, risk.getConsecutiveSeasons());
        assertTrue(risk.getRiskLevel() == NutrientDepletionRiskDto.RiskLevel.MEDIUM ||
                   risk.getRiskLevel() == NutrientDepletionRiskDto.RiskLevel.HIGH);
    }

    /**
     * Property 8.3: Alternating crop families should not flag consecutive risks.
     * 
     * For any crop history sequence where different crop families are alternated
     * (e.g., cereal -> legume -> cereal -> legume), the system should not flag
     * consecutive monoculture risks.
     */
    @Property
    void alternatingFamiliesShouldNotFlagConsecutiveRisks(
            @ForAll("cerealCropName") String cereal1,
            @ForAll("cerealCropName") String cereal2,
            @ForAll("legumeCropName") String legume1,
            @ForAll("legumeCropName") String legume2) {
        // Arrange - Create alternating pattern
        List<CropHistoryEntryDto> history = Arrays.asList(
                createCropEntry(cereal1, LocalDate.now().minusMonths(3)),     // Cereal
                createCropEntry(legume1, LocalDate.now().minusMonths(9)),     // Legume
                createCropEntry(cereal2, LocalDate.now().minusMonths(15)),    // Cereal
                createCropEntry(legume2, LocalDate.now().minusMonths(21))     // Legume
        );

        // Act
        CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

        // Assert - Should have good rotation flag
        assertTrue(result.getSummary().isHasGoodRotation(), 
                "Alternating families should indicate good rotation");
    }

    /**
     * Property 8.4: Risk severity should increase with consecutive count.
     * 
     * For any crop family, the severity score should be higher for more
     * consecutive seasons of that family.
     */
    @Property
    void severityIncreasesWithConsecutiveCount(
            @ForAll("cerealCropName") String c1,
            @ForAll("cerealCropName") String c2,
            @ForAll("cerealCropName") String c3,
            @ForAll("cerealCropName") String c4) {
        // Arrange - Create histories with different consecutive counts
        List<CropHistoryEntryDto> twoSeasons = Arrays.asList(
                createCropEntry(c1, LocalDate.now().minusMonths(3)),
                createCropEntry(c2, LocalDate.now().minusMonths(9))
        );

        List<CropHistoryEntryDto> threeSeasons = Arrays.asList(
                createCropEntry(c1, LocalDate.now().minusMonths(3)),
                createCropEntry(c2, LocalDate.now().minusMonths(9)),
                createCropEntry(c3, LocalDate.now().minusMonths(15))
        );

        List<CropHistoryEntryDto> fourSeasons = Arrays.asList(
                createCropEntry(c1, LocalDate.now().minusMonths(3)),
                createCropEntry(c2, LocalDate.now().minusMonths(9)),
                createCropEntry(c3, LocalDate.now().minusMonths(15)),
                createCropEntry(c4, LocalDate.now().minusMonths(21))
        );

        // Act
        CropHistoryAnalysisResultDto result2 = analyzer.analyzeCropHistory(twoSeasons);
        CropHistoryAnalysisResultDto result3 = analyzer.analyzeCropHistory(threeSeasons);
        CropHistoryAnalysisResultDto result4 = analyzer.analyzeCropHistory(fourSeasons);

        // Get severity scores
        Double score2 = getMaxSeverityScore(result2);
        Double score3 = getMaxSeverityScore(result3);
        Double score4 = getMaxSeverityScore(result4);

        // Assert - Severity should increase with consecutive count
        assertTrue(score3 >= score2, 
                "3 seasons should have >= severity than 2 seasons");
        assertTrue(score4 >= score3, 
                "4 seasons should have >= severity than 3 seasons");
    }

    /**
     * Property 8.5: Different crop families should have different affected nutrients.
     * 
     * For any crop family, the affected nutrients should match the known
     * nutrient depletion patterns for that family.
     */
    @Property
    void differentFamiliesHaveDifferentAffectedNutrients(
            @ForAll("cerealCropName") String cereal,
            @ForAll("brassicaCropName") String brassica) {
        // Arrange - Create histories for different families
        List<CropHistoryEntryDto> cerealHistory = Arrays.asList(
                createCropEntry(cereal, LocalDate.now().minusMonths(3)),
                createCropEntry(cereal, LocalDate.now().minusMonths(9)),
                createCropEntry(cereal, LocalDate.now().minusMonths(15))
        );

        List<CropHistoryEntryDto> brassicaHistory = Arrays.asList(
                createCropEntry(brassica, LocalDate.now().minusMonths(3)),
                createCropEntry(brassica, LocalDate.now().minusMonths(9)),
                createCropEntry(brassica, LocalDate.now().minusMonths(15))
        );

        // Act
        CropHistoryAnalysisResultDto cerealResult = analyzer.analyzeCropHistory(cerealHistory);
        CropHistoryAnalysisResultDto brassicaResult = analyzer.analyzeCropHistory(brassicaHistory);

        // Assert - Different families should have different affected nutrients
        NutrientDepletionRiskDto cerealRisk = cerealResult.getNutrientDepletionRisks().stream()
                .filter(r -> r.getCropFamily() == CropFamily.CEREALS)
                .findFirst()
                .orElse(null);
        
        NutrientDepletionRiskDto brassicaRisk = brassicaResult.getNutrientDepletionRisks().stream()
                .filter(r -> r.getCropFamily() == CropFamily.BRASSICAS)
                .findFirst()
                .orElse(null);

        assertNotNull(cerealRisk, "Should have cereal risk");
        assertNotNull(brassicaRisk, "Should have brassica risk");
        
        assertNotEquals(cerealRisk.getAffectedNutrients(), brassicaRisk.getAffectedNutrients(),
                "Cereals and brassicas should have different affected nutrients");
    }

    /**
     * Property 8.6: Recommendations should be generated for flagged risks.
     * 
     * For any crop history that triggers a nutrient depletion risk,
     * the system should generate at least one recommendation.
     */
    @Property
    void recommendationsGeneratedForRisks(
            @ForAll("cerealCropName") String c1,
            @ForAll("cerealCropName") String c2,
            @ForAll("cerealCropName") String c3) {
        // Arrange - Create history with risk
        List<CropHistoryEntryDto> history = Arrays.asList(
                createCropEntry(c1, LocalDate.now().minusMonths(3)),
                createCropEntry(c2, LocalDate.now().minusMonths(9)),
                createCropEntry(c3, LocalDate.now().minusMonths(15))
        );

        // Act
        CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

        // Assert
        assertFalse(result.getNutrientDepletionRisks().isEmpty());
        assertFalse(result.getRecommendations().isEmpty(), 
                "Should generate recommendations for identified risks");
    }

    /**
     * Property 8.7: Helper methods should correctly detect monoculture.
     * 
     * The hasConsecutiveMonoculture and getMaxConsecutiveSeasons methods
     * should correctly identify monoculture patterns.
     */
    @Property
    void helperMethodsDetectMonocultureCorrectly(
            @ForAll("cerealCropName") String c1,
            @ForAll("cerealCropName") String c2,
            @ForAll("cerealCropName") String c3) {
        // Arrange - Create history with monoculture
        List<CropHistoryEntryDto> monoculture = Arrays.asList(
                createCropEntry(c1, LocalDate.now().minusMonths(3)),
                createCropEntry(c2, LocalDate.now().minusMonths(9)),
                createCropEntry(c3, LocalDate.now().minusMonths(15))
        );

        // Act & Assert
        assertTrue(analyzer.hasConsecutiveMonoculture(monoculture),
                "Should detect monoculture");
        assertEquals(3, analyzer.getMaxConsecutiveSeasons(monoculture),
                "Should return 3 for 3 consecutive seasons");
    }

    /**
     * Property 8.8: Empty or insufficient history should not crash.
     * 
     * The analyzer should handle empty or insufficient history gracefully
     * without throwing exceptions.
     */
    @Property
    void handlesInsufficientHistoryGracefully() {
        // Arrange - Empty and single-season histories
        List<CropHistoryEntryDto> empty = Collections.emptyList();
        List<CropHistoryEntryDto> single = Arrays.asList(
                createCropEntry("Rice", LocalDate.now().minusMonths(3))
        );

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> analyzer.analyzeCropHistory(empty));
        assertDoesNotThrow(() -> analyzer.analyzeCropHistory(single));
        assertDoesNotThrow(() -> analyzer.hasConsecutiveMonoculture(empty));
        assertDoesNotThrow(() -> analyzer.hasConsecutiveMonoculture(single));
        assertEquals(0, analyzer.getMaxConsecutiveSeasons(empty));
        assertEquals(0, analyzer.getMaxConsecutiveSeasons(single));
    }

    /**
     * Property 8.9: Analysis should correctly identify crop families.
     * 
     * For any valid crop name, the analyzer should correctly identify
     * its crop family.
     */
    @Property
    void correctlyIdentifiesCropFamilies(@ForAll("validCropName") String cropName) {
        // Arrange
        List<CropHistoryEntryDto> history = Arrays.asList(
                createCropEntry(cropName, LocalDate.now().minusMonths(3))
        );

        // Act
        CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

        // Assert
        assertEquals(1, result.getCropHistory().size());
        assertNotNull(result.getCropHistory().get(0).getCropFamily(),
                "Should identify crop family for: " + cropName);
    }

    /**
     * Property 8.10: Analysis should correctly identify root depths.
     * 
     * For any valid crop name, the analyzer should correctly identify
     * its typical root depth.
     */
    @Property
    void correctlyIdentifiesRootDepths(@ForAll("validCropName") String cropName) {
        // Arrange
        List<CropHistoryEntryDto> history = Arrays.asList(
                createCropEntry(cropName, LocalDate.now().minusMonths(3))
        );

        // Act
        CropHistoryAnalysisResultDto result = analyzer.analyzeCropHistory(history);

        // Assert
        assertEquals(1, result.getCropHistory().size());
        assertNotNull(result.getCropHistory().get(0).getRootDepth(),
                "Should identify root depth for: " + cropName);
    }

    /**
     * Helper method to get the maximum severity score from analysis result.
     */
    private Double getMaxSeverityScore(CropHistoryAnalysisResultDto result) {
        return result.getNutrientDepletionRisks().stream()
                .map(NutrientDepletionRiskDto::getSeverityScore)
                .max(Double::compareTo)
                .orElse(0.0);
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

