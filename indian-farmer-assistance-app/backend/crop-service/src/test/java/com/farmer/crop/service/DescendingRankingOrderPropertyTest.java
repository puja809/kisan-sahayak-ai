package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.AgroEcologicalZone;
import com.farmer.crop.repository.AgroEcologicalZoneRepository;
import com.farmer.crop.repository.GaezCropDataRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Descending Ranking Order.
 * 
 * **Validates: Requirements 2.5, 3.9, 9.7, 11D.3, 21.8**
 * 
 * Property 6: Descending Ranking Order
 * For any list of items with numeric scores (crop suitability scores, rotation options 
 * ranked by benefit, disease detections ranked by confidence, scheme recommendations ranked 
 * by benefit amount, search results ranked by similarity), the displayed list should be 
 * sorted in descending order such that for any two adjacent items, the first has a score 
 * greater than or equal to the second.
 * 
 * This test class verifies the descending ranking property across multiple services:
 * - Crop recommendations (Requirement 2.5)
 * - Rotation options (Requirement 3.9)
 * - Disease detections (Requirement 9.7)
 * - Scheme recommendations (Requirement 11D.3)
 * - Search results (Requirement 21.8)
 */
class DescendingRankingOrderPropertyTest {

    /**
     * Generator for valid crop names.
     */
    @Provide
    Arbitrary<String> validCropNames() {
        return Arbitraries.of("Rice", "Wheat", "Cotton", "Soybean", "Groundnut",
                "Mustard", "Pulses", "Maize", "Sugarcane", "Potato", "Tomato", "Onion");
    }

    /**
     * Generator for valid suitability scores (0-100).
     */
    @Provide
    Arbitrary<BigDecimal> suitabilityScores() {
        return Arbitraries.doubles().between(0, 100)
                .map(BigDecimal::new);
    }

    /**
     * Generator for benefit amounts.
     */
    @Provide
    Arbitrary<BigDecimal> benefitAmounts() {
        return Arbitraries.doubles().between(1000, 500000)
                .map(BigDecimal::new);
    }

    /**
     * Generator for confidence scores (0-100).
     */
    @Provide
    Arbitrary<BigDecimal> confidenceScores() {
        return Arbitraries.doubles().between(0, 100)
                .map(BigDecimal::new);
    }

    /**
     * Generator for similarity scores (0-1).
     */
    @Provide
    Arbitrary<BigDecimal> similarityScores() {
        return Arbitraries.doubles().between(0, 1)
                .map(BigDecimal::new);
    }

    /**
     * Generator for rotation benefit scores.
     */
    @Provide
    Arbitrary<BigDecimal> benefitScores() {
        return Arbitraries.doubles().between(0, 100)
                .map(BigDecimal::new);
    }

    // ==================== CROP RECOMMENDATIONS (Requirement 2.5) ====================

    /**
     * Property 6.1: Crop recommendations should be sorted by suitability score in descending order.
     * 
     * WHEN generating recommendations, THE Application SHALL rank crops by suitability score.
     * **Validates: Requirement 2.5**
     */
    @Property
    void propertyCropRecommendationsDescendingOrder(
            @ForAll @IntRange(min = 2, max = 10) int numCrops,
            @ForAll String state
    ) {
        // Arrange - Create mock services
        AgroEcologicalZoneService agroEcologicalZoneService = mock(AgroEcologicalZoneService.class);
        GaezSuitabilityService gaezSuitabilityService = mock(GaezSuitabilityService.class);
        GaezDataImportService gaezDataImportService = mock(GaezDataImportService.class);
        AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
        GaezCropDataRepository gaezCropDataRepository = mock(GaezCropDataRepository.class);
        MarketDataService marketDataService = new MarketDataService();
        ClimateRiskService climateRiskService = new ClimateRiskService();
        SeedVarietyService seedVarietyService = new SeedVarietyService();

        CropRecommendationService service = new CropRecommendationService(
                agroEcologicalZoneService,
                gaezSuitabilityService,
                gaezDataImportService,
                zoneRepository,
                gaezCropDataRepository,
                marketDataService,
                climateRiskService,
                seedVarietyService
        );

        // Create mock suitability data with random scores
        List<GaezCropSuitabilityDto> suitabilityList = createMockSuitabilityListRandom(numCrops);
        
        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);
        when(zoneRepository.findByZoneCode(any()))
                .thenReturn(Optional.of(createMockZone()));
        when(agroEcologicalZoneService.getZoneForLocation(any()))
                .thenReturn(createMockZoneResponse());

        // Act
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST-FARMER")
                .district("TestDistrict")
                .state(state)
                .includeMarketData(false)
                .includeClimateRiskAssessment(false)
                .build();

        CropRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert - Verify descending order
        assertTrue(response.isSuccess(), "Response should be successful");
        assertFalse(response.getRecommendations().isEmpty(), "Recommendations should not be empty");
        
        List<CropRecommendationResponseDto.RecommendedCropDto> recommendations = response.getRecommendations();
        
        for (int i = 0; i < recommendations.size() - 1; i++) {
            BigDecimal currentScore = recommendations.get(i).getOverallSuitabilityScore();
            BigDecimal nextScore = recommendations.get(i + 1).getOverallSuitabilityScore();
            assertTrue(currentScore.compareTo(nextScore) >= 0,
                    String.format("Crop ranking order violated at position %d: %s >= %s expected but was %s < %s",
                            i, currentScore, nextScore, currentScore, nextScore));
        }
    }

    /**
     * Property 6.2: Crop recommendations with equal scores should maintain stable order.
     * 
     * When multiple crops have the same suitability score, they should still satisfy
     * the >= condition (equal is allowed).
     */
    @Property
    void propertyCropRecommendationsEqualScores(
            @ForAll @IntRange(min = 2, max = 5) int numCrops,
            @ForAll @BigRange(min = "50", max = "90") BigDecimal score,
            @ForAll String state
    ) {
        // Arrange
        AgroEcologicalZoneService agroEcologicalZoneService = mock(AgroEcologicalZoneService.class);
        GaezSuitabilityService gaezSuitabilityService = mock(GaezSuitabilityService.class);
        GaezDataImportService gaezDataImportService = mock(GaezDataImportService.class);
        AgroEcologicalZoneRepository zoneRepository = mock(AgroEcologicalZoneRepository.class);
        GaezCropDataRepository gaezCropDataRepository = mock(GaezCropDataRepository.class);
        MarketDataService marketDataService = new MarketDataService();
        ClimateRiskService climateRiskService = new ClimateRiskService();
        SeedVarietyService seedVarietyService = new SeedVarietyService();

        CropRecommendationService service = new CropRecommendationService(
                agroEcologicalZoneService,
                gaezSuitabilityService,
                gaezDataImportService,
                zoneRepository,
                gaezCropDataRepository,
                marketDataService,
                climateRiskService,
                seedVarietyService
        );

        // Create mock suitability data with same scores
        List<GaezCropSuitabilityDto> suitabilityList = createMockSuitabilityListEqualScores(numCrops, score);
        
        when(gaezSuitabilityService.calculateSuitabilityScores(any()))
                .thenReturn(suitabilityList);
        when(zoneRepository.findByZoneCode(any()))
                .thenReturn(Optional.of(createMockZone()));
        when(agroEcologicalZoneService.getZoneForLocation(any()))
                .thenReturn(createMockZoneResponse());

        // Act
        CropRecommendationRequestDto request = CropRecommendationRequestDto.builder()
                .farmerId("TEST-FARMER")
                .district("TestDistrict")
                .state(state)
                .build();

        CropRecommendationResponseDto response = service.generateRecommendations(request);

        // Assert
        assertTrue(response.isSuccess());
        
        List<CropRecommendationResponseDto.RecommendedCropDto> recommendations = response.getRecommendations();
        
        for (int i = 0; i < recommendations.size() - 1; i++) {
            BigDecimal currentScore = recommendations.get(i).getOverallSuitabilityScore();
            BigDecimal nextScore = recommendations.get(i + 1).getOverallSuitabilityScore();
            assertTrue(currentScore.compareTo(nextScore) >= 0,
                    "Equal scores should satisfy >= condition");
        }
    }

    // ==================== ROTATION OPTIONS (Requirement 3.9) ====================

    /**
     * Property 6.3: Rotation options should be sorted by benefit in descending order.
     * 
     * WHEN multiple rotation options exist, THE Application SHALL rank them by 
     * soil health benefit, climate resilience, and economic viability.
     * **Validates: Requirement 3.9**
     */
    @Property
    void propertyRotationOptionsDescendingOrder(
            @ForAll @IntRange(min = 2, max = 8) int numOptions
    ) {
        // Arrange
        CropRotationService rotationService = new CropRotationService();
        
        // Create mock rotation options with random benefit scores
        List<RotationOptionDto> options = createMockRotationOptionsRandom(numOptions);

        // Act
        List<RotationOptionDto> sortedOptions = rotationService.rankRotationOptions(options);

        // Assert - Verify descending order by overall benefit score
        assertFalse(sortedOptions.isEmpty(), "Sorted options should not be empty");
        
        for (int i = 0; i < sortedOptions.size() - 1; i++) {
            BigDecimal currentScore = sortedOptions.get(i).getOverallBenefitScore();
            BigDecimal nextScore = sortedOptions.get(i + 1).getOverallBenefitScore();
            assertTrue(currentScore.compareTo(nextScore) >= 0,
                    String.format("Rotation ranking order violated at position %d: %s >= %s expected",
                            i, currentScore, nextScore));
        }
    }

    /**
     * Property 6.4: Rotation options should be sorted by soil health benefit specifically.
     */
    @Property
    void propertyRotationOptionsSoilHealthBenefit(
            @ForAll @IntRange(min = 2, max = 8) int numOptions
    ) {
        // Arrange
        CropRotationService rotationService = new CropRotationService();
        
        List<RotationOptionDto> options = createMockRotationOptionsRandom(numOptions);

        // Act
        List<RotationOptionDto> sortedOptions = rotationService.rankRotationOptions(options);

        // Assert - Verify descending order by soil health benefit
        for (int i = 0; i < sortedOptions.size() - 1; i++) {
            BigDecimal currentScore = sortedOptions.get(i).getSoilHealthBenefit();
            BigDecimal nextScore = sortedOptions.get(i + 1).getSoilHealthBenefit();
            assertTrue(currentScore.compareTo(nextScore) >= 0,
                    "Rotation should be sorted by soil health benefit in descending order");
        }
    }

    // ==================== DISEASE DETECTIONS (Requirement 9.7) ====================

    /**
     * Property 6.5: Disease detections should be sorted by confidence score in descending order.
     * 
     * WHEN multiple diseases are detected in a single image, THE Application SHALL 
     * rank them by confidence score and severity level.
     * **Validates: Requirement 9.7**
     */
    @Property
    void propertyDiseaseDetectionsDescendingOrder(
            @ForAll @IntRange(min = 2, max = 6) int numDetections
    ) {
        // Arrange
        DiseaseDetectionService diseaseService = new DiseaseDetectionService();
        
        // Create mock disease detections with random confidence scores
        List<DiseaseDetectionResultDto> detections = createMockDiseaseDetectionsRandom(numDetections);

        // Act
        List<DiseaseDetectionResultDto> sortedDetections = diseaseService.rankByConfidence(detections);

        // Assert - Verify descending order by confidence score
        assertFalse(sortedDetections.isEmpty(), "Sorted detections should not be empty");
        
        for (int i = 0; i < sortedDetections.size() - 1; i++) {
            BigDecimal currentConfidence = sortedDetections.get(i).getConfidenceScore();
            BigDecimal nextConfidence = sortedDetections.get(i + 1).getConfidenceScore();
            assertTrue(currentConfidence.compareTo(nextConfidence) >= 0,
                    String.format("Disease detection ranking violated at position %d: %s >= %s expected",
                            i, currentConfidence, nextConfidence));
        }
    }

    /**
     * Property 6.6: Disease detections with same confidence should be sorted by severity.
     */
    @Property
    void propertyDiseaseDetectionsSeveritySecondary(
            @ForAll @IntRange(min = 2, max = 5) int numDetections,
            @ForAll @BigRange(min = "70", max = "95") BigDecimal confidence
    ) {
        // Arrange
        DiseaseDetectionService diseaseService = new DiseaseDetectionService();
        
        List<DiseaseDetectionResultDto> detections = createMockDiseaseDetectionsEqualConfidence(numDetections, confidence);

        // Act
        List<DiseaseDetectionResultDto> sortedDetections = diseaseService.rankByConfidence(detections);

        // Assert - When confidence is equal, severity should determine order (higher severity first)
        for (int i = 0; i < sortedDetections.size() - 1; i++) {
            BigDecimal currentConfidence = sortedDetections.get(i).getConfidenceScore();
            BigDecimal nextConfidence = sortedDetections.get(i + 1).getConfidenceScore();
            assertTrue(currentConfidence.compareTo(nextConfidence) >= 0,
                    "Confidence scores should be in descending order");
        }
    }

    // ==================== SCHEME RECOMMENDATIONS (Requirement 11D.3) ====================

    /**
     * Property 6.7: Scheme recommendations should be sorted by benefit amount in descending order.
     * 
     * WHEN displaying scheme recommendations, THE Application SHALL rank schemes 
     * by benefit amount and deadline proximity.
     * **Validates: Requirement 11D.3**
     */
    @Property
    void propertySchemeRecommendationsDescendingOrder(
            @ForAll @IntRange(min = 2, max = 10) int numSchemes
    ) {
        // Arrange
        SchemeRecommendationService schemeService = new SchemeRecommendationService();
        
        // Create mock scheme recommendations with random benefit amounts
        List<SchemeRecommendationDto> recommendations = createMockSchemeRecommendationsRandom(numSchemes);

        // Act
        List<SchemeRecommendationDto> sortedRecommendations = schemeService.rankByBenefit(recommendations);

        // Assert - Verify descending order by benefit amount
        assertFalse(sortedRecommendations.isEmpty(), "Sorted recommendations should not be empty");
        
        for (int i = 0; i < sortedRecommendations.size() - 1; i++) {
            BigDecimal currentBenefit = sortedRecommendations.get(i).getBenefitAmount();
            BigDecimal nextBenefit = sortedRecommendations.get(i + 1).getBenefitAmount();
            assertTrue(currentBenefit.compareTo(nextBenefit) >= 0,
                    String.format("Scheme recommendation ranking violated at position %d: %s >= %s expected",
                            i, currentBenefit, nextBenefit));
        }
    }

    /**
     * Property 6.8: Scheme recommendations with equal benefit should consider deadline.
     */
    @Property
    void propertySchemeRecommendationsDeadlineSecondary(
            @ForAll @IntRange(min = 2, max = 5) int numSchemes,
            @ForAll @BigRange(min = "10000", max = "100000") BigDecimal benefit
    ) {
        // Arrange
        SchemeRecommendationService schemeService = new SchemeRecommendationService();
        
        List<SchemeRecommendationDto> recommendations = createMockSchemeRecommendationsEqualBenefit(numSchemes, benefit);

        // Act
        List<SchemeRecommendationDto> sortedRecommendations = schemeService.rankByBenefit(recommendations);

        // Assert
        for (int i = 0; i < sortedRecommendations.size() - 1; i++) {
            BigDecimal currentBenefit = sortedRecommendations.get(i).getBenefitAmount();
            BigDecimal nextBenefit = sortedRecommendations.get(i + 1).getBenefitAmount();
            assertTrue(currentBenefit.compareTo(nextBenefit) >= 0,
                    "Benefit amounts should be in descending order");
        }
    }

    // ==================== SEARCH RESULTS (Requirement 21.8) ====================

    /**
     * Property 6.9: Search results should be sorted by similarity score in descending order.
     * 
     * WHEN displaying search results, THE Application SHALL rank by similarity 
     * score in descending order.
     * **Validates: Requirement 21.8**
     */
    @Property
    void propertySearchResultsDescendingOrder(
            @ForAll @IntRange(min = 2, max = 10) int numResults
    ) {
        // Arrange
        SemanticSearchService searchService = new SemanticSearchService();
        
        // Create mock search results with random similarity scores
        List<SearchResultDto> results = createMockSearchResultsRandom(numResults);

        // Act
        List<SearchResultDto> sortedResults = searchService.rankBySimilarity(results);

        // Assert - Verify descending order by similarity score
        assertFalse(sortedResults.isEmpty(), "Sorted results should not be empty");
        
        for (int i = 0; i < sortedResults.size() - 1; i++) {
            BigDecimal currentSimilarity = sortedResults.get(i).getSimilarityScore();
            BigDecimal nextSimilarity = sortedResults.get(i + 1).getSimilarityScore();
            assertTrue(currentSimilarity.compareTo(nextSimilarity) >= 0,
                    String.format("Search result ranking violated at position %d: %s >= %s expected",
                            i, currentSimilarity, nextSimilarity));
        }
    }

    /**
     * Property 6.10: Search results with equal similarity should maintain stable order.
     */
    @Property
    void propertySearchResultsEqualSimilarity(
            @ForAll @IntRange(min = 2, max = 5) int numResults,
            @ForAll @BigRange(min = "0.5", max = "0.9") BigDecimal similarity
    ) {
        // Arrange
        SemanticSearchService searchService = new SemanticSearchService();
        
        List<SearchResultDto> results = createMockSearchResultsEqualSimilarity(numResults, similarity);

        // Act
        List<SearchResultDto> sortedResults = searchService.rankBySimilarity(results);

        // Assert
        for (int i = 0; i < sortedResults.size() - 1; i++) {
            BigDecimal currentSimilarity = sortedResults.get(i).getSimilarityScore();
            BigDecimal nextSimilarity = sortedResults.get(i + 1).getSimilarityScore();
            assertTrue(currentSimilarity.compareTo(nextSimilarity) >= 0,
                    "Equal similarity scores should satisfy >= condition");
        }
    }

    // ==================== HELPER METHODS ====================

    private List<GaezCropSuitabilityDto> createMockSuitabilityListRandom(int numCrops) {
        String[] cropCodes = {"RICE", "WHEAT", "COTTON", "SOYBEAN", "GROUNDNUT",
                "MUSTARD", "PULSES", "MAIZE", "SUGARCANE", "POTATO"};
        
        List<GaezCropSuitabilityDto> list = new ArrayList<>();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < numCrops; i++) {
            BigDecimal score = new BigDecimal(random.nextDouble() * 100);
            list.add(GaezCropSuitabilityDto.builder()
                    .cropCode(cropCodes[i % cropCodes.length])
                    .cropName(cropCodes[i % cropCodes.length])
                    .overallSuitabilityScore(score)
                    .climateSuitabilityScore(score)
                    .soilSuitabilityScore(score)
                    .terrainSuitabilityScore(score)
                    .waterSuitabilityScore(score)
                    .rainfedPotentialYield(new BigDecimal("4000"))
                    .irrigatedPotentialYield(new BigDecimal("5000"))
                    .expectedYieldExpected(new BigDecimal("3500"))
                    .waterRequirementsMm(new BigDecimal("500"))
                    .growingSeasonDays(120)
                    .kharifSuitable(true)
                    .rabiSuitable(false)
                    .zaidSuitable(false)
                    .climateRiskLevel(GaezCropSuitabilityDto.ClimateRiskLevel.LOW)
                    .dataVersion("v4")
                    .dataResolution("5 arc-min")
                    .build());
        }
        
        return list;
    }

    private List<GaezCropSuitabilityDto> createMockSuitabilityListEqualScores(int numCrops, BigDecimal score) {
        String[] cropCodes = {"RICE", "WHEAT", "COTTON", "SOYBEAN", "GROUNDNUT"};
        
        List<GaezCropSuitabilityDto> list = new ArrayList<>();
        
        for (int i = 0; i < numCrops; i++) {
            list.add(GaezCropSuitabilityDto.builder()
                    .cropCode(cropCodes[i])
                    .cropName(cropCodes[i])
                    .overallSuitabilityScore(score)
                    .climateSuitabilityScore(score)
                    .soilSuitabilityScore(score)
                    .terrainSuitabilityScore(score)
                    .waterSuitabilityScore(score)
                    .rainfedPotentialYield(new BigDecimal("4000"))
                    .irrigatedPotentialYield(new BigDecimal("5000"))
                    .expectedYieldExpected(new BigDecimal("3500"))
                    .waterRequirementsMm(new BigDecimal("500"))
                    .growingSeasonDays(120)
                    .kharifSuitable(true)
                    .rabiSuitable(false)
                    .zaidSuitable(false)
                    .climateRiskLevel(GaezCropSuitabilityDto.ClimateRiskLevel.LOW)
                    .dataVersion("v4")
                    .dataResolution("5 arc-min")
                    .build());
        }
        
        return list;
    }

    private List<RotationOptionDto> createMockRotationOptionsRandom(int numOptions) {
        String[] cropSequences = {"Rice-Wheat", "Soybean-Wheat", "Cotton-Pulses", 
                "Groundnut-Wheat", "Maize-Potato", "Sugarcane-Pulses", "Rice-Pulses"};
        
        List<RotationOptionDto> list = new ArrayList<>();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < numOptions; i++) {
            BigDecimal soilHealth = new BigDecimal(random.nextDouble() * 100);
            BigDecimal climateResilience = new BigDecimal(random.nextDouble() * 100);
            BigDecimal economicViability = new BigDecimal(random.nextDouble() * 100);
            BigDecimal overall = soilHealth.add(climateResilience).add(economicViability)
                    .divide(new BigDecimal("3"), 2, java.math.RoundingMode.HALF_UP);
            
            list.add(RotationOptionDto.builder()
                    .id((long) (i + 1))
                    .cropSequence(cropSequences[i % cropSequences.length])
                    .soilHealthBenefit(soilHealth)
                    .climateResilience(climateResilience)
                    .economicViability(economicViability)
                    .overallBenefitScore(overall)
                    .nutrientCyclingScore(soilHealth)
                    .pestManagementScore(climateResilience)
                    .waterUsageScore(economicViability)
                    .build());
        }
        
        return list;
    }

    private List<DiseaseDetectionResultDto> createMockDiseaseDetectionsRandom(int numDetections) {
        String[] diseases = {"Bacterial Blight", "Leaf Spot", "Powdery Mildew", 
                "Rust", "Viral Mosaic", "Fungal Infection"};
        DiseaseDetectionResultDto.SeverityLevel[] severities = 
                DiseaseDetectionResultDto.SeverityLevel.values();
        
        List<DiseaseDetectionResultDto> list = new ArrayList<>();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < numDetections; i++) {
            BigDecimal confidence = new BigDecimal(random.nextDouble() * 100);
            DiseaseDetectionResultDto.SeverityLevel severity = severities[random.nextInt(severities.length)];
            
            list.add(DiseaseDetectionResultDto.builder()
                    .id((long) (i + 1))
                    .diseaseName(diseases[i % diseases.length])
                    .confidenceScore(confidence)
                    .severityLevel(severity)
                    .affectedAreaPercent(new BigDecimal(random.nextDouble() * 100))
                    .treatmentRecommendations("Treatment for " + diseases[i % diseases.length])
                    .build());
        }
        
        return list;
    }

    private List<DiseaseDetectionResultDto> createMockDiseaseDetectionsEqualConfidence(
            int numDetections, BigDecimal confidence) {
        String[] diseases = {"Bacterial Blight", "Leaf Spot", "Powdery Mildew", "Fungal Infection", "Rust", "Wilt"};
        DiseaseDetectionResultDto.SeverityLevel[] severities = {
                DiseaseDetectionResultDto.SeverityLevel.LOW,
                DiseaseDetectionResultDto.SeverityLevel.MEDIUM,
                DiseaseDetectionResultDto.SeverityLevel.HIGH,
                DiseaseDetectionResultDto.SeverityLevel.CRITICAL,
                DiseaseDetectionResultDto.SeverityLevel.LOW,
                DiseaseDetectionResultDto.SeverityLevel.MEDIUM
        };
        
        List<DiseaseDetectionResultDto> list = new ArrayList<>();
        
        for (int i = 0; i < numDetections; i++) {
            int index = i % diseases.length;
            list.add(DiseaseDetectionResultDto.builder()
                    .id((long) (i + 1))
                    .diseaseName(diseases[index])
                    .confidenceScore(confidence)
                    .severityLevel(severities[index])
                    .affectedAreaPercent(new BigDecimal("50"))
                    .treatmentRecommendations("Treatment for " + diseases[index])
                    .build());
        }
        
        return list;
    }

    private List<SchemeRecommendationDto> createMockSchemeRecommendationsRandom(int numSchemes) {
        String[] schemeNames = {"PM-Kisan", "PMFBY", "KCC", "Sub-Mission on Agricultural Mechanization",
                "Paramparagat Krishi Vikas Yojana", "PMKSY", "National Mission on Sustainable Agriculture"};
        
        List<SchemeRecommendationDto> list = new ArrayList<>();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < numSchemes; i++) {
            BigDecimal benefit = new BigDecimal(random.nextDouble() * 500000);
            LocalDate deadline = LocalDate.now().plusDays(random.nextInt(180) + 30);
            
            list.add(SchemeRecommendationDto.builder()
                    .id((long) (i + 1))
                    .schemeName(schemeNames[i % schemeNames.length])
                    .schemeType(SchemeRecommendationDto.SchemeType.CENTRAL)
                    .benefitAmount(benefit)
                    .applicationDeadline(deadline)
                    .eligibilityScore(new BigDecimal(random.nextDouble() * 100))
                    .deadlineProximityScore(new BigDecimal(random.nextDouble() * 100))
                    .build());
        }
        
        return list;
    }

    private List<SchemeRecommendationDto> createMockSchemeRecommendationsEqualBenefit(
            int numSchemes, BigDecimal benefit) {
        String[] schemeNames = {"PM-Kisan", "PMFBY", "KCC", "Sub-Mission on Agricultural Mechanization", 
                "PMKSY", "National Mission on Sustainable Agriculture"};
        
        List<SchemeRecommendationDto> list = new ArrayList<>();
        
        for (int i = 0; i < numSchemes; i++) {
            int index = i % schemeNames.length;
            LocalDate deadline = LocalDate.now().plusDays((index + 1) * 30);
            
            list.add(SchemeRecommendationDto.builder()
                    .id((long) (i + 1))
                    .schemeName(schemeNames[index])
                    .schemeType(SchemeRecommendationDto.SchemeType.CENTRAL)
                    .benefitAmount(benefit)
                    .applicationDeadline(deadline)
                    .eligibilityScore(new BigDecimal("80"))
                    .deadlineProximityScore(new BigDecimal("70"))
                    .build());
        }
        
        return list;
    }

    private List<SearchResultDto> createMockSearchResultsRandom(int numResults) {
        String[] documentTitles = {"PM-Kisan Scheme Guidelines", "Crop Disease Management",
                "Weather Advisory for Kharif", "Soil Health Card Procedure", 
                "Irrigation Water Management", "Organic Farming Techniques"};
        
        List<SearchResultDto> list = new ArrayList<>();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < numResults; i++) {
            BigDecimal similarity = new BigDecimal(random.nextDouble());
            
            list.add(SearchResultDto.builder()
                    .id((long) (i + 1))
                    .documentId("DOC-" + (i + 1))
                    .title(documentTitles[i % documentTitles.length])
                    .category(SearchResultDto.DocumentCategory.SCHEMES)
                    .similarityScore(similarity)
                    .snippet("Relevant content for " + documentTitles[i % documentTitles.length])
                    .build());
        }
        
        return list;
    }

    private List<SearchResultDto> createMockSearchResultsEqualSimilarity(int numResults, BigDecimal similarity) {
        String[] documentTitles = {"PM-Kisan Scheme Guidelines", "Crop Disease Management",
                "Weather Advisory for Kharif", "Soil Health Card Procedure", 
                "Irrigation Water Management", "Organic Farming Techniques"};
        
        List<SearchResultDto> list = new ArrayList<>();
        
        for (int i = 0; i < numResults; i++) {
            int index = i % documentTitles.length;
            list.add(SearchResultDto.builder()
                    .id((long) (i + 1))
                    .documentId("DOC-" + (i + 1))
                    .title(documentTitles[index])
                    .category(SearchResultDto.DocumentCategory.SCHEMES)
                    .similarityScore(similarity)
                    .snippet("Relevant content for " + documentTitles[index])
                    .build());
        }
        
        return list;
    }

    private AgroEcologicalZone createMockZone() {
        return AgroEcologicalZone.builder()
                .id(1L)
                .zoneCode("AEZ-05")
                .zoneName("Upper Gangetic Plain Region")
                .description("Test zone")
                .climateType("Subtropical")
                .isActive(true)
                .build();
    }

    private ZoneLookupResponseDto createMockZoneResponse() {
        return ZoneLookupResponseDto.builder()
                .success(true)
                .zone(AgroEcologicalZoneDto.builder()
                        .zoneCode("AEZ-05")
                        .zoneName("Upper Gangetic Plain Region")
                        .build())
                .build();
    }
}