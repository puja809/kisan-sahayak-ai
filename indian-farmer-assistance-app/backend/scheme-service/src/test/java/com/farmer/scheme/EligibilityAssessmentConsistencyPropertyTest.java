package com.farmer.scheme;
import com.farmer.scheme.dto.*;
import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.Scheme.SchemeType;
import com.farmer.scheme.repository.SchemeRepository;
import com.farmer.scheme.service.EligibilityAssessmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;


import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Eligibility Assessment Consistency.
 * 
 * Property 9: Eligibility Assessment Consistency
 * For any farmer profile and government scheme, running the eligibility check
 * multiple times with the same inputs should produce the same eligibility result
 * (eligible/not eligible with the same confidence level).
 * 
 * Validates: Requirements 4.4, 11D.1
 * 
 * Requirements Reference:
 * - Requirement 4.4: WHEN displaying scheme eligibility, THE Application SHALL 
 *   pre-assess eligibility using the farmer's landholding, crop, and demographic 
 *   data from AgriStack before showing schemes
 * - Requirement 11D.1: Pre-assess eligibility before displaying schemes
 */
class EligibilityAssessmentConsistencyPropertyTest {

    private EligibilityAssessmentService eligibilityService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        objectMapper = new ObjectMapper();
        eligibilityService = new EligibilityAssessmentService(schemeRepository, objectMapper);
    }

    // ==================== Generators for Farmer Profiles ====================

    /**
     * Generator for valid farmer profiles with various combinations of attributes.
     */
    @Provide
    Arbitrary<FarmerProfileDTO> validFarmerProfiles() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 1000),
                Arbitraries.of("FARM001", "FARM002", "FARM003", "FARM004", "FARM005"),
                Arbitraries.of("Ramesh Kumar", "Suresh Patel", "Anita Devi", "John Smith", "Priya Sharma"),
                Arbitraries.of("Maharashtra", "Karnataka", "Tamil Nadu", "Uttar Pradesh", "Punjab", 
                              "Madhya Pradesh", "Gujarat", "Rajasthan", "Andhra Pradesh", "Telangana"),
                Arbitraries.of("Pune", "Mysore", "Chennai", "Lucknow", "Ludhiana", 
                              "Bhopal", "Ahmedabad", "Jaipur", "Visakhapatnam", "Hyderabad"),
                Arbitraries.of("Wheat", "Rice", "Cotton", "Soybean", "Sugarcane", "Corn", "Pulses", "Groundnut"),
                Arbitraries.of("BOREWELL", "CANAL", "DRIP", "SPRINKLER", "RAINFED"),
                Arbitraries.of("SC", "ST", "OBC", "General")
        ).flatAs((userId, farmerId, name, state, district, crop, irrigation, category) -> 
            Combinators.combine(
                Arbitraries.of("Male", "Female", "Other"),
                Arbitraries.integers().between(18, 80)
            ).as((gender, age) -> 
                FarmerProfileDTO.builder()
                    .userId((long) userId)
                    .farmerId(farmerId)
                    .name(name)
                    .state(state)
                    .district(district)
                    .totalLandholdingAcres(new Double(userId % 20 + 1))
                    .crops(Arrays.asList(crop))
                    .irrigationType(irrigation)
                    .category(category)
                    .gender(gender)
                    .age(age)
                    .isSmallMarginalFarmer(userId % 3 == 0)
                    .isTenantFarmer(userId % 4 == 0)
                    .annualIncome(new Double((userId % 10 + 1) * 50000))
                    .hasKisanCreditCard(userId % 2 == 0)
                    .hasPMKisanRegistration(userId % 3 == 1)
                    .hasPMFBYInsurance(userId % 2 == 1)
                    .farmingExperienceYears(userId % 5 + 1)
                    .primaryOccupation("Agriculture")
                    .socialCategory(userId % 2 == 0 ? "APL" : "BPL")
                    .build()));
    }

    /**
     * Generator for farmer profiles with null optional fields.
     */
    @Provide
    Arbitrary<FarmerProfileDTO> farmerProfilesWithNullFields() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 100),
                Arbitraries.of("Maharashtra", "Karnataka", "Tamil Nadu", "Uttar Pradesh")
        ).as((userId, state) -> {
            FarmerProfileDTO.FarmerProfileDTOBuilder builder = FarmerProfileDTO.builder()
                    .userId((long) userId)
                    .farmerId("FARM" + userId)
                    .name("Test Farmer " + userId)
                    .state(state)
                    .district("Test District");
            
            // Randomly set or nullify optional fields
            if (userId % 2 == 0) {
                builder.totalLandholdingAcres(new Double(userId % 10 + 1));
            }
            if (userId % 3 == 0) {
                builder.crops(Arrays.asList("Wheat", "Rice"));
            }
            if (userId % 4 == 0) {
                builder.irrigationType("BOREWELL");
            }
            if (userId % 5 == 0) {
                builder.category("OBC");
            }
            if (userId % 6 == 0) {
                builder.gender("Male");
            }
            if (userId % 7 == 0) {
                builder.age(35);
            }
            
            return builder.build();
        });
    }

    // ==================== Generators for Schemes ====================

    /**
     * Generator for valid schemes with various eligibility criteria.
     */
    @Provide
    Arbitrary<Scheme> validSchemes() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 1000),
                Arbitraries.of("PMKISAN", "PMFBY", "KCC", "SCHEME001", "SCHEME002", "RKVY", "PMKSY"),
                Arbitraries.of("PM-Kisan Samman Nidhi", "PM Fasal Bima Yojana", 
                              "Kisan Credit Card", "RKVY Grant", "PMKSY Subsidy"),
                Arbitraries.of(SchemeType.CENTRAL, SchemeType.STATE, SchemeType.CROP_SPECIFIC, 
                              SchemeType.INSURANCE, SchemeType.SUBSIDY),
                Arbitraries.of("Maharashtra", "Karnataka", "Tamil Nadu", null),
                Arbitraries.of(LocalDate.now().minusDays(30), LocalDate.now().minusDays(10), LocalDate.now()),
                Arbitraries.of(LocalDate.now().plusDays(30), LocalDate.now().plusDays(60), LocalDate.now().plusDays(90))
        ).as((id, code, name, type, state, startDate, endDate) -> {
            Scheme.SchemeBuilder builder = Scheme.builder()
                    .id((long) id)
                    .schemeCode(code)
                    .schemeName(name)
                    .schemeType(type)
                    .benefitAmount(new Double((id % 10 + 1) * 10000))
                    .applicationStartDate(startDate)
                    .applicationEndDate(endDate)
                    .isActive(true);
            
            if (type == SchemeType.STATE && state != null) {
                builder.state(state);
            }
            
            return builder.build();
        });
    }

    /**
     * Generator for schemes with specific eligibility criteria JSON.
     */
    @Provide
    Arbitrary<Scheme> schemesWithEligibilityCriteria() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 100),
                Arbitraries.of("SCHEME001", "SCHEME002", "SCHEME003"),
                Arbitraries.of("Test Scheme 1", "Test Scheme 2", "Test Scheme 3"),
                Arbitraries.of(SchemeType.CENTRAL, SchemeType.STATE)
        ).as((id, code, name, type) -> {
            String eligibilityCriteria = String.format("""
                {
                    "minLandholdingAcres": %d.0,
                    "maxLandholdingAcres": %d0.0,
                    "smallMarginalFarmersOnly": %s,
                    "requiredStates": ["Maharashtra", "Karnataka"],
                    "maxAnnualIncome": %d00000
                }
                """, id % 5 + 1, id % 10 + 5, id % 2 == 0 ? "true" : "false", id % 5 + 1);
            
            return Scheme.builder()
                    .id((long) id)
                    .schemeCode(code)
                    .schemeName(name)
                    .schemeType(type)
                    .eligibilityCriteria(eligibilityCriteria)
                    .benefitAmount(new Double((id % 10 + 1) * 5000))
                    .applicationStartDate(LocalDate.now().minusDays(30))
                    .applicationEndDate(LocalDate.now().plusDays(60))
                    .isActive(true)
                    .build();
        });
    }

    // ==================== Property 9.1: Same Inputs Produce Same Result ====================

    /**
     * Property 9.1: Same farmer profile and scheme should produce identical eligibility results.
     * 
     * For any valid farmer profile and government scheme, calling the eligibility assessment
     * service multiple times with the same inputs should always produce the same result.
     * This verifies the idempotency of the eligibility assessment operation.
     */
    @Property
    void sameFarmerAndSchemeShouldProduceIdenticalResults(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer,
            @ForAll("validSchemes") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times with the same inputs
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - All results should be identical
        assertNotNull(result1, "First result should not be null");
        assertNotNull(result2, "Second result should not be null");
        assertNotNull(result3, "Third result should not be null");

        assertEquals(result1.getEligibilityStatus(), result2.getEligibilityStatus(),
                "Same inputs should produce same eligibility status");
        assertEquals(result1.getEligibilityStatus(), result3.getEligibilityStatus(),
                "Same inputs should produce same eligibility status across multiple calls");

        assertEquals(result1.getSchemeId(), result2.getSchemeId(),
                "Same inputs should produce same scheme ID");
        assertEquals(result1.getSchemeCode(), result2.getSchemeCode(),
                "Same inputs should produce same scheme code");
    }

    /**
     * Property 9.2: Confidence level should be consistent across multiple assessments.
     * 
     * For any farmer profile and scheme, the confidence level assigned should be
     * consistent across multiple assessment calls with the same inputs.
     */
    @Property
    void confidenceLevelShouldBeConsistent(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer,
            @ForAll("validSchemes") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Confidence levels should be consistent
        assertEquals(result1.getConfidenceLevel(), result2.getConfidenceLevel(),
                "Same inputs should produce same confidence level");
        assertEquals(result2.getConfidenceLevel(), result3.getConfidenceLevel(),
                "Same inputs should produce same confidence level across multiple calls");
    }

    /**
     * Property 9.3: Match percentage should be deterministic.
     * 
     * For any farmer profile and scheme, the match percentage should be
     * identical across multiple assessment calls.
     */
    @Property
    void matchPercentageShouldBeDeterministic(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer,
            @ForAll("validSchemes") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Match percentages should be identical
        assertEquals(result1.getMatchPercentage(), result2.getMatchPercentage(),
                "Same inputs should produce same match percentage");
        assertEquals(result2.getMatchPercentage(), result3.getMatchPercentage(),
                "Same inputs should produce same match percentage across multiple calls");
    }

    // ==================== Property 9.4: Met/Unmet Criteria Consistency ====================

    /**
     * Property 9.4: The list of met criteria should be consistent.
     * 
     * For any farmer profile and scheme, the criteria that are marked as met
     * should be identical across multiple assessment calls.
     */
    @Property
    void metCriteriaShouldBeConsistent(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer,
            @ForAll("schemesWithEligibilityCriteria") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);

        // Assert - Met criteria should be identical
        assertEquals(result1.getMetCriteria().size(), result2.getMetCriteria().size(),
                "Same inputs should produce same number of met criteria");
        
        // Check that all met criteria from result1 are present in result2
        for (String criterion : result1.getMetCriteria()) {
            assertTrue(result2.getMetCriteria().contains(criterion),
                    "Met criterion '" + criterion + "' should be consistent across calls");
        }
    }

    /**
     * Property 9.5: The list of unmet criteria should be consistent.
     * 
     * For any farmer profile and scheme, the criteria that are marked as unmet
     * should be identical across multiple assessment calls.
     */
    @Property
    void unmetCriteriaShouldBeConsistent(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer,
            @ForAll("schemesWithEligibilityCriteria") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);

        // Assert - Unmet criteria should be identical
        assertEquals(result1.getUnmetCriteria().size(), result2.getUnmetCriteria().size(),
                "Same inputs should produce same number of unmet criteria");
        
        // Check that all unmet criteria from result1 are present in result2
        for (String criterion : result1.getUnmetCriteria()) {
            assertTrue(result2.getUnmetCriteria().contains(criterion),
                    "Unmet criterion '" + criterion + "' should be consistent across calls");
        }
    }

    // ==================== Property 9.6: Ranking Score Determinism ====================

    /**
     * Property 9.6: Ranking score should be deterministic.
     * 
     * For any farmer profile and scheme, the ranking score should be
     * identical across multiple assessment calls.
     */
    @Property
    void rankingScoreShouldBeDeterministic(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer,
            @ForAll("validSchemes") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Ranking scores should be identical
        assertEquals(result1.getRankingScore(), result2.getRankingScore(),
                "Same inputs should produce same ranking score");
        assertEquals(result2.getRankingScore(), result3.getRankingScore(),
                "Same inputs should produce same ranking score across multiple calls");
    }

    // ==================== Property 9.7: Days Until Deadline Consistency ====================

    /**
     * Property 9.7: Days until deadline should be consistent.
     * 
     * For any scheme, the days until deadline calculation should be
     * consistent across multiple assessment calls.
     */
    @Property
    void daysUntilDeadlineShouldBeConsistent(@ForAll("validSchemes") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);
        
        // Create a minimal farmer profile
        FarmerProfileDTO farmer = FarmerProfileDTO.builder()
                .userId(1L)
                .farmerId("FARM001")
                .name("Test Farmer")
                .state("Maharashtra")
                .build();

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Days until deadline should be identical
        assertEquals(result1.getDaysUntilDeadline(), result2.getDaysUntilDeadline(),
                "Same inputs should produce same days until deadline");
        assertEquals(result2.getDaysUntilDeadline(), result3.getDaysUntilDeadline(),
                "Same inputs should produce same days until deadline across multiple calls");
    }

    // ==================== Property 9.8: Highlight Flag Consistency ====================

    /**
     * Property 9.8: The isHighlighted flag should be consistent.
     * 
     * For any farmer profile and scheme, the isHighlighted flag should be
     * identical across multiple assessment calls.
     */
    @Property
    void highlightFlagShouldBeConsistent(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer,
            @ForAll("validSchemes") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Highlight flag should be identical
        assertEquals(result1.getIsHighlighted(), result2.getIsHighlighted(),
                "Same inputs should produce same isHighlighted flag");
        assertEquals(result2.getIsHighlighted(), result3.getIsHighlighted(),
                "Same inputs should produce same isHighlighted flag across multiple calls");
    }

    // ==================== Property 9.9: Pre-Assessment Logic Consistency ====================

    /**
     * Property 9.9: Pre-assessment should consistently identify eligible/ineligible farmers.
     * 
     * For any farmer profile and scheme, the pre-assessment logic should
     * consistently identify whether the farmer is eligible, potentially eligible,
     * or not eligible.
     */
    @Property
    void preAssessmentShouldIdentifyEligibilityConsistently(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer,
            @ForAll("schemesWithEligibilityCriteria") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Eligibility status should be consistent
        assertTrue(
            result1.getEligibilityStatus() == result2.getEligibilityStatus() &&
            result2.getEligibilityStatus() == result3.getEligibilityStatus(),
            "Pre-assessment should consistently identify eligibility status"
        );
    }

    // ==================== Property 9.10: Null/Empty Criteria Handling ====================

    /**
     * Property 9.10: Handling of null/empty eligibility criteria should be consistent.
     * 
     * For any farmer profile and scheme with null or empty eligibility criteria,
     * the assessment should consistently return HIGH confidence for eligible farmers.
     */
    @Property
    void nullEligibilityCriteriaShouldBeHandledConsistently(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);
        
        // Create a scheme with null eligibility criteria
        Scheme scheme = Scheme.builder()
                .id(1L)
                .schemeCode("NULL_CRITERIA")
                .schemeName("Scheme with Null Criteria")
                .schemeType(SchemeType.CENTRAL)
                .eligibilityCriteria(null)
                .benefitAmount(new Double("10000"))
                .applicationStartDate(LocalDate.now().minusDays(30))
                .applicationEndDate(LocalDate.now().plusDays(60))
                .isActive(true)
                .build();

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Results should be consistent
        assertEquals(result1.getEligibilityStatus(), result2.getEligibilityStatus(),
                "Null criteria should be handled consistently");
        assertEquals(result2.getEligibilityStatus(), result3.getEligibilityStatus(),
                "Null criteria should be handled consistently across multiple calls");
        
        assertEquals(result1.getConfidenceLevel(), result2.getConfidenceLevel(),
                "Null criteria should produce consistent confidence level");
        assertEquals(result2.getConfidenceLevel(), result3.getConfidenceLevel(),
                "Null criteria should produce consistent confidence level across calls");
    }

    /**
     * Property 9.11: Empty eligibility criteria string should be handled consistently.
     */
    @Property
    void emptyEligibilityCriteriaShouldBeHandledConsistently(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);
        
        // Create a scheme with empty eligibility criteria
        Scheme scheme = Scheme.builder()
                .id(1L)
                .schemeCode("EMPTY_CRITERIA")
                .schemeName("Scheme with Empty Criteria")
                .schemeType(SchemeType.CENTRAL)
                .eligibilityCriteria("")
                .benefitAmount(new Double("10000"))
                .applicationStartDate(LocalDate.now().minusDays(30))
                .applicationEndDate(LocalDate.now().plusDays(60))
                .isActive(true)
                .build();

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Results should be consistent
        assertEquals(result1.getEligibilityStatus(), result2.getEligibilityStatus(),
                "Empty criteria should be handled consistently");
        assertEquals(result2.getEligibilityStatus(), result3.getEligibilityStatus(),
                "Empty criteria should be handled consistently across multiple calls");
    }

    // ==================== Property 9.12: Farmer Profiles with Null Fields ====================

    /**
     * Property 9.12: Assessment should be consistent for profiles with null fields.
     * 
     * For any scheme, the assessment should be consistent even when the farmer
     * profile has null values for optional fields.
     */
    @Property
    void assessmentShouldBeConsistentForProfilesWithNullFields(
            @ForAll("farmerProfilesWithNullFields") FarmerProfileDTO farmer,
            @ForAll("validSchemes") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Results should be consistent
        assertEquals(result1.getEligibilityStatus(), result2.getEligibilityStatus(),
                "Assessment should be consistent for profiles with null fields");
        assertEquals(result2.getEligibilityStatus(), result3.getEligibilityStatus(),
                "Assessment should be consistent across multiple calls");
        
        assertEquals(result1.getConfidenceLevel(), result2.getConfidenceLevel(),
                "Confidence level should be consistent for profiles with null fields");
        assertEquals(result2.getConfidenceLevel(), result3.getConfidenceLevel(),
                "Confidence level should be consistent across multiple calls");
    }

    // ==================== Property 9.13: Benefit Amount Consistency ====================

    /**
     * Property 9.13: Benefit amount should be consistently returned.
     * 
     * For any farmer profile and scheme, the benefit amount should be
     * identical across multiple assessment calls.
     */
    @Property
    void benefitAmountShouldBeConsistent(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer,
            @ForAll("validSchemes") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Benefit amounts should be identical
        assertEquals(result1.getBenefitAmount(), result2.getBenefitAmount(),
                "Same inputs should produce same benefit amount");
        assertEquals(result2.getBenefitAmount(), result3.getBenefitAmount(),
                "Same inputs should produce same benefit amount across multiple calls");
    }

    // ==================== Property 9.14: Verification Needed List Consistency ====================

    /**
     * Property 9.14: The verification needed list should be consistent.
     * 
     * For any farmer profile and scheme, the criteria that need verification
     * should be identical across multiple assessment calls.
     */
    @Property
    void verificationNeededShouldBeConsistent(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer,
            @ForAll("schemesWithEligibilityCriteria") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Verification needed lists should be identical
        assertEquals(result1.getVerificationNeeded().size(), result2.getVerificationNeeded().size(),
                "Same inputs should produce same number of verification needed items");
        assertEquals(result2.getVerificationNeeded().size(), result3.getVerificationNeeded().size(),
                "Same inputs should produce same number of verification needed items across calls");
        
        // Check content consistency
        for (String item : result1.getVerificationNeeded()) {
            assertTrue(result2.getVerificationNeeded().contains(item),
                    "Verification needed item should be consistent");
        }
    }

    // ==================== Property 9.15: Total Criteria Count Consistency ====================

    /**
     * Property 9.15: Total criteria count should be consistent.
     * 
     * For any farmer profile and scheme, the total criteria count should be
     * identical across multiple assessment calls.
     */
    @Property
    void totalCriteriaCountShouldBeConsistent(
            @ForAll("validFarmerProfiles") FarmerProfileDTO farmer,
            @ForAll("schemesWithEligibilityCriteria") Scheme scheme) {
        // Arrange
        SchemeRepository schemeRepository = mock(SchemeRepository.class);
        EligibilityAssessmentService localService = new EligibilityAssessmentService(schemeRepository, objectMapper);

        // Act - Call the service multiple times
        EligibilityResultDTO result1 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result2 = localService.assessEligibility(farmer, scheme);
        EligibilityResultDTO result3 = localService.assessEligibility(farmer, scheme);

        // Assert - Total criteria counts should be identical
        assertEquals(result1.getTotalCriteria(), result2.getTotalCriteria(),
                "Same inputs should produce same total criteria count");
        assertEquals(result2.getTotalCriteria(), result3.getTotalCriteria(),
                "Same inputs should produce same total criteria count across multiple calls");
        
        assertEquals(result1.getCriteriaMet(), result2.getCriteriaMet(),
                "Same inputs should produce same criteria met count");
        assertEquals(result2.getCriteriaMet(), result3.getCriteriaMet(),
                "Same inputs should produce same criteria met count across multiple calls");
    }
}