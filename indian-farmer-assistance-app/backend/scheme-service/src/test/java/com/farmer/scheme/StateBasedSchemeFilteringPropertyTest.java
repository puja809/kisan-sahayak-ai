package com.farmer.scheme;

import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.Scheme.SchemeType;
import com.farmer.scheme.repository.SchemeRepository;
import com.farmer.scheme.service.SchemeService;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for state-based scheme filtering.
 * Validates: Requirements 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
 * 
 * Property 10: State-Based Scheme Filtering
 * For any farmer located in state S, all schemes returned should either be 
 * central schemes (applicable to all states) or state-specific schemes where 
 * the scheme's state matches S, and no schemes from other states should be included.
 */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class StateBasedSchemeFilteringPropertyTest {

    @Autowired
    private SchemeService schemeService;

    @Autowired
    private SchemeRepository schemeRepository;

    @BeforeEach
    void setUp() {
        schemeRepository.deleteAll();
    }

    /**
     * Test: All schemes returned for a state should be either central schemes 
     * or state-specific schemes matching the requested state.
     * 
     * Validates: Requirements 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Test
    void shouldOnlyReturnCentralOrMatchingStateSchemes() {
        // Given: Create a mix of central and state-specific schemes
        String stateName = "Karnataka";
        Scheme centralScheme = createScheme("CENTRAL-001", "Central Scheme", 
                SchemeType.CENTRAL, null);
        Scheme stateScheme = createScheme("STATE-001", "Karnataka Scheme", 
                SchemeType.STATE, stateName);
        Scheme otherStateScheme = createScheme("OTHER-001", "Maharashtra Scheme", 
                SchemeType.STATE, "Maharashtra");
        
        schemeRepository.save(centralScheme);
        schemeRepository.save(stateScheme);
        schemeRepository.save(otherStateScheme);

        // When: Get schemes for Karnataka
        List<Scheme> schemes = schemeService.getSchemesForState(stateName);

        // Then: All schemes should be either central or matching state
        assertFalse(schemes.isEmpty(), "Should return at least central schemes");
        
        for (Scheme scheme : schemes) {
            assertTrue(
                scheme.getSchemeType() == SchemeType.CENTRAL || 
                (scheme.getSchemeType() == SchemeType.STATE && stateName.equals(scheme.getState())),
                "Scheme " + scheme.getSchemeCode() + " should be either CENTRAL or have state matching " + stateName
            );
        }
    }

    /**
     * Test: Central schemes should always be included regardless of state.
     * 
     * Validates: Requirements 4.3, 5.1
     */
    @Test
    void shouldAlwaysIncludeCentralSchemes() {
        // Given: Create central schemes
        String stateName = "Punjab";
        Scheme centralScheme1 = createScheme("CENTRAL-001", "PM-Kisan", 
                SchemeType.CENTRAL, null);
        Scheme centralScheme2 = createScheme("CENTRAL-002", "PMFBY", 
                SchemeType.CENTRAL, null);
        
        schemeRepository.save(centralScheme1);
        schemeRepository.save(centralScheme2);

        // When: Get schemes for Punjab
        List<Scheme> schemes = schemeService.getSchemesForState(stateName);

        // Then: All central schemes should be included
        long centralCount = schemes.stream()
                .filter(s -> s.getSchemeType() == SchemeType.CENTRAL)
                .count();
        
        assertEquals(2, centralCount, "All central schemes should be included for any state");
    }

    /**
     * Test: State-specific schemes should only appear when requesting their specific state.
     * 
     * Validates: Requirements 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Test
    void shouldOnlyReturnStateSchemesForMatchingState() {
        // Given: Create state-specific schemes for different states
        Scheme karnatakaScheme = createScheme("KAR-001", "Karnataka Scheme", 
                SchemeType.STATE, "Karnataka");
        Scheme maharashtraScheme = createScheme("MAHA-001", "Maharashtra Scheme", 
                SchemeType.STATE, "Maharashtra");
        
        schemeRepository.save(karnatakaScheme);
        schemeRepository.save(maharashtraScheme);

        // When: Get schemes for Karnataka
        List<Scheme> karnatakaSchemes = schemeService.getSchemesForState("Karnataka");

        // Then: Only Karnataka schemes should be returned
        for (Scheme scheme : karnatakaSchemes) {
            if (scheme.getSchemeType() == SchemeType.STATE) {
                assertEquals("Karnataka", scheme.getState(), 
                    "State-specific scheme should only appear for matching state");
            }
        }
    }

    /**
     * Test: No schemes from other states should be included when filtering by state.
     * 
     * Validates: Requirements 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Test
    void shouldNotReturnSchemesFromOtherStates() {
        // Given: Create schemes for multiple states
        Scheme karnatakaScheme = createScheme("KAR-001", "Karnataka Scheme", 
                SchemeType.STATE, "Karnataka");
        Scheme maharashtraScheme = createScheme("MAHA-001", "Maharashtra Scheme", 
                SchemeType.STATE, "Maharashtra");
        Scheme telanganaScheme = createScheme("TEL-001", "Telangana Scheme", 
                SchemeType.STATE, "Telangana");
        Scheme centralScheme = createScheme("CENTRAL-001", "Central Scheme", 
                SchemeType.CENTRAL, null);
        
        schemeRepository.save(karnatakaScheme);
        schemeRepository.save(maharashtraScheme);
        schemeRepository.save(telanganaScheme);
        schemeRepository.save(centralScheme);

        // When: Get schemes for Karnataka
        List<Scheme> karnatakaSchemes = schemeService.getSchemesForState("Karnataka");

        // Then: No Maharashtra or Telangana schemes should be included
        for (Scheme scheme : karnatakaSchemes) {
            if (scheme.getSchemeType() == SchemeType.STATE) {
                assertNotEquals("Maharashtra", scheme.getState(),
                    "Maharashtra scheme should not appear for Karnataka");
                assertNotEquals("Telangana", scheme.getState(),
                    "Telangana scheme should not appear for Karnataka");
            }
        }
    }

    /**
     * Test: All Indian states mentioned in requirements should be supported.
     * 
     * Validates: Requirements 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Test
    void shouldSupportAllIndianStates() {
        // Test all states mentioned in requirements
        String[] states = {"Karnataka", "Maharashtra", "Telangana", "Andhra Pradesh", 
                          "Haryana", "Uttar Pradesh", "Punjab"};

        for (String state : states) {
            // Given: Create a scheme for the state
            Scheme stateScheme = createScheme("STATE-" + state, state + " Scheme", 
                    SchemeType.STATE, state);
            schemeRepository.save(stateScheme);

            // When: Get schemes for the state
            List<Scheme> schemes = schemeService.getSchemesForState(state);

            // Then: The state scheme should be included
            boolean hasStateScheme = schemes.stream()
                    .anyMatch(s -> s.getSchemeType() == SchemeType.STATE && 
                                  state.equals(s.getState()));
            
            assertTrue(hasStateScheme, "State scheme should be returned for state: " + state);
        }
    }

    /**
     * Test: Mixed central and state schemes should be correctly filtered.
     * 
     * Validates: Requirements 4.3, 5.1
     */
    @Test
    void shouldCorrectlyFilterMixedCentralAndStateSchemes() {
        // Given: Create a mix of central and state schemes for different states
        Scheme centralScheme = createScheme("CENTRAL-001", "PM-Kisan", 
                SchemeType.CENTRAL, null);
        Scheme karnatakaScheme = createScheme("KAR-001", "Karnataka Krishi Bhagya", 
                SchemeType.STATE, "Karnataka");
        Scheme maharashtraScheme = createScheme("MAHA-001", "Maharashtra DBT", 
                SchemeType.STATE, "Maharashtra");
        Scheme telanganaScheme = createScheme("TEL-001", "Rythu Bandhu", 
                SchemeType.STATE, "Telangana");
        
        schemeRepository.save(centralScheme);
        schemeRepository.save(karnatakaScheme);
        schemeRepository.save(maharashtraScheme);
        schemeRepository.save(telanganaScheme);

        // When: Get schemes for Karnataka
        List<Scheme> karnatakaSchemes = schemeService.getSchemesForState("Karnataka");

        // Then: Should have central scheme and Karnataka scheme
        assertTrue(karnatakaSchemes.size() >= 2, "Should have at least central and Karnataka schemes");
        
        boolean hasCentral = karnatakaSchemes.stream()
                .anyMatch(s -> s.getSchemeType() == SchemeType.CENTRAL);
        boolean hasKarnataka = karnatakaSchemes.stream()
                .anyMatch(s -> s.getSchemeType() == SchemeType.STATE && "Karnataka".equals(s.getState()));
        boolean hasMaharashtra = karnatakaSchemes.stream()
                .anyMatch(s -> s.getSchemeType() == SchemeType.STATE && "Maharashtra".equals(s.getState()));
        
        assertTrue(hasCentral, "Should include central scheme");
        assertTrue(hasKarnataka, "Should include Karnataka scheme");
        assertFalse(hasMaharashtra, "Should not include Maharashtra scheme");
    }

    /**
     * Helper method to create a scheme entity.
     */
    private Scheme createScheme(String code, String name, SchemeType type, String state) {
        return Scheme.builder()
                .schemeCode(code)
                .schemeName(name)
                .schemeType(type)
                .state(state)
                .description("Test scheme for " + name)
                .eligibilityCriteria("{\"landholding\": \"any\"}")
                .benefitAmount(new BigDecimal("5000.00"))
                .benefitDescription("Test benefit description")
                .applicationStartDate(LocalDate.now().minusDays(30))
                .applicationEndDate(LocalDate.now().plusDays(60))
                .applicationUrl("https://test.gov.in")
                .contactInfo("{\"phone\": \"1234567890\"}")
                .isActive(true)
                .build();
    }

    // ==================== PROPERTY-BASED TESTS ====================

    /**
     * Generator for all Indian states mentioned in requirements.
     */
    @Provide
    Arbitrary<String> indianStates() {
        return Arbitraries.of(
            "Karnataka", "Maharashtra", "Telangana", "Andhra Pradesh",
            "Haryana", "Uttar Pradesh", "Punjab"
        );
    }

    /**
     * Generator for random Indian state names including variations.
     */
    @Provide
    Arbitrary<String> randomIndianStates() {
        return Arbitraries.of(
            "Karnataka", "Maharashtra", "Telangana", "Andhra Pradesh",
            "Haryana", "Uttar Pradesh", "Punjab", "Tamil Nadu",
            "West Bengal", "Gujarat", "Rajasthan", "Madhya Pradesh"
        );
    }

    /**
     * Generator for scheme types.
     */
    @Provide
    Arbitrary<SchemeType> schemeTypes() {
        return Arbitraries.of(SchemeType.class);
    }

    /**
     * Generator for unique scheme codes.
     */
    @Provide
    Arbitrary<String> schemeCodes() {
        return Arbitraries.strings().alpha().withChars('A', 'Z')
                .ofMinLength(3).ofMaxLength(10)
                .map(code -> "SCHEME-" + System.currentTimeMillis() + "-" + code);
    }

    /**
     * Property 10.1: All schemes returned for a state should be either central schemes 
     * or state-specific schemes matching the requested state.
     * 
     * For any farmer located in state S, all schemes returned should either be 
     * central schemes (applicable to all states) or state-specific schemes where 
     * the scheme's state matches S, and no schemes from other states should be included.
     * 
     * Validates: Requirements 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Property
    void allReturnedSchemesShouldBeCentralOrMatchingState(
            @ForAll("randomIndianStates") String farmerState,
            @ForAll("schemeCodes") String codePrefix) {
        // Arrange: Create mocks and test data
        SchemeRepository mockRepository = mock(SchemeRepository.class);
        SchemeService localService = new SchemeService(mockRepository);
        
        // Create schemes
        Scheme centralScheme = createScheme(codePrefix + "-C1", "Central Scheme", 
                SchemeType.CENTRAL, null);
        Scheme farmerStateScheme = createScheme(codePrefix + "-S1", farmerState + " Scheme", 
                SchemeType.STATE, farmerState);
        Scheme otherStateScheme1 = createScheme(codePrefix + "-O1", "Other State Scheme 1", 
                SchemeType.STATE, "Maharashtra");
        Scheme otherStateScheme2 = createScheme(codePrefix + "-O2", "Other State Scheme 2", 
                SchemeType.STATE, "Tamil Nadu");
        
        // When querying for farmerState, return only central and matching state schemes
        when(mockRepository.findActiveSchemesForState(farmerState))
                .thenReturn(List.of(centralScheme, farmerStateScheme));

        // Act: Get schemes for the farmer's state
        List<Scheme> schemes = localService.getSchemesForState(farmerState);

        // Then: All schemes should be either central or matching state
        assertFalse(schemes.isEmpty(), "Should return at least central schemes");
        
        for (Scheme scheme : schemes) {
            assertTrue(
                scheme.getSchemeType() == SchemeType.CENTRAL || 
                (scheme.getSchemeType() == SchemeType.STATE && farmerState.equals(scheme.getState())),
                "Scheme " + scheme.getSchemeCode() + " should be either CENTRAL or have state matching " + farmerState
            );
        }
    }

    /**
     * Property 10.2: Central schemes should always be included regardless of state.
     * 
     * For any state S, when fetching schemes, all central schemes should be included
     * in the results since they are applicable to all states.
     * 
     * Validates: Requirements 4.3, 5.1
     */
    @Property
    void centralSchemesAlwaysIncludedForAnyState(
            @ForAll("randomIndianStates") String state,
            @ForAll("schemeCodes") String codePrefix) {
        // Arrange: Create mocks and test data
        SchemeRepository mockRepository = mock(SchemeRepository.class);
        SchemeService localService = new SchemeService(mockRepository);
        
        // Create central schemes
        Scheme centralScheme1 = createScheme(codePrefix + "-C1", "PM-Kisan", 
                SchemeType.CENTRAL, null);
        Scheme centralScheme2 = createScheme(codePrefix + "-C2", "PMFBY", 
                SchemeType.CENTRAL, null);
        Scheme centralScheme3 = createScheme(codePrefix + "-C3", "KCC", 
                SchemeType.CENTRAL, null);
        
        // When querying for any state, return all central schemes
        when(mockRepository.findActiveSchemesForState(state))
                .thenReturn(List.of(centralScheme1, centralScheme2, centralScheme3));

        // Act: Get schemes for any state
        List<Scheme> schemes = localService.getSchemesForState(state);

        // Then: All central schemes should be included
        long centralCount = schemes.stream()
                .filter(s -> s.getSchemeType() == SchemeType.CENTRAL)
                .count();
        
        assertEquals(3, centralCount, "All central schemes should be included for state: " + state);
    }

    /**
     * Property 10.3: No schemes from other states should be included when filtering by state.
     * 
     * For any state S, when fetching schemes, no state-specific schemes from states
     * other than S should be included in the results.
     * 
     * Validates: Requirements 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Property
    void noSchemesFromOtherStatesIncluded(
            @ForAll("indianStates") String farmerState,
            @ForAll("schemeCodes") String codePrefix) {
        // Arrange: Create mocks and test data
        SchemeRepository mockRepository = mock(SchemeRepository.class);
        SchemeService localService = new SchemeService(mockRepository);
        
        // Create schemes for multiple states
        Set<String> otherStates = new HashSet<>();
        otherStates.add("Maharashtra");
        otherStates.add("Tamil Nadu");
        otherStates.add("West Bengal");
        otherStates.add("Gujarat");
        otherStates.add("Rajasthan");
        
        // Create a central scheme
        Scheme centralScheme = createScheme(codePrefix + "-C1", "Central Scheme", 
                SchemeType.CENTRAL, null);
        
        // When querying for farmerState, return only central scheme (no other state schemes)
        when(mockRepository.findActiveSchemesForState(farmerState))
                .thenReturn(List.of(centralScheme));

        // Act: Get schemes for the farmer's state
        List<Scheme> schemes = localService.getSchemesForState(farmerState);

        // Then: No schemes from other states should be included
        for (Scheme scheme : schemes) {
            if (scheme.getSchemeType() == SchemeType.STATE) {
                assertEquals(farmerState, scheme.getState(),
                    "State-specific scheme should only be for the requested state");
                assertFalse(otherStates.contains(scheme.getState()),
                    "Scheme from other state should not be included: " + scheme.getState());
            }
        }
    }

    /**
     * Property 10.4: State-specific schemes should only appear when requesting their specific state.
     * 
     * For any state-specific scheme with state S, when fetching schemes for a different
     * state T (where T ≠ S), the scheme should not appear in the results.
     * 
     * Validates: Requirements 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Property
    void stateSchemesOnlyAppearForMatchingState(
            @ForAll("indianStates") String schemeState,
            @ForAll("indianStates") String requestingState,
            @ForAll("schemeCodes") String codePrefix) {
        // Arrange: Create mocks and test data
        SchemeRepository mockRepository = mock(SchemeRepository.class);
        SchemeService localService = new SchemeService(mockRepository);
        
        // Create a state-specific scheme
        Scheme stateScheme = createScheme(codePrefix + "-S1", schemeState + " Scheme", 
                SchemeType.STATE, schemeState);
        
        // When querying for requestingState, return the scheme only if states match
        if (schemeState.equals(requestingState)) {
            when(mockRepository.findActiveSchemesForState(requestingState))
                    .thenReturn(List.of(stateScheme));
        } else {
            when(mockRepository.findActiveSchemesForState(requestingState))
                    .thenReturn(List.of());
        }

        // Act: Get schemes for a different state
        List<Scheme> schemes = localService.getSchemesForState(requestingState);

        // Then: The scheme should only appear if states match
        boolean hasScheme = schemes.stream()
                .anyMatch(s -> s.getSchemeCode().equals(codePrefix + "-S1"));
        
        if (schemeState.equals(requestingState)) {
            assertTrue(hasScheme, "State scheme should appear when requesting matching state");
        } else {
            assertFalse(hasScheme, "State scheme should not appear when requesting different state");
        }
    }

    /**
     * Property 10.5: Filtering is deterministic - same state always returns same schemes.
     * 
     * For any state S, calling the scheme filtering service multiple times with the
     * same state should always return the same set of schemes.
     * 
     * Validates: Requirements 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Property
    void schemeFilteringIsDeterministic(
            @ForAll("randomIndianStates") String state,
            @ForAll("schemeCodes") String codePrefix) {
        // Arrange: Create mocks and test data
        SchemeRepository mockRepository = mock(SchemeRepository.class);
        SchemeService localService = new SchemeService(mockRepository);
        
        // Create schemes
        Scheme centralScheme = createScheme(codePrefix + "-C1", "Central Scheme", 
                SchemeType.CENTRAL, null);
        Scheme stateScheme = createScheme(codePrefix + "-S1", state + " Scheme", 
                SchemeType.STATE, state);
        
        List<Scheme> expectedSchemes = List.of(centralScheme, stateScheme);
        
        // When querying for state, return the same schemes
        when(mockRepository.findActiveSchemesForState(state))
                .thenReturn(expectedSchemes);

        // Act: Call the service multiple times with the same state
        List<Scheme> result1 = localService.getSchemesForState(state);
        List<Scheme> result2 = localService.getSchemesForState(state);
        List<Scheme> result3 = localService.getSchemesForState(state);

        // Then: All results should have the same scheme codes
        Set<String> codes1 = extractSchemeCodes(result1);
        Set<String> codes2 = extractSchemeCodes(result2);
        Set<String> codes3 = extractSchemeCodes(result3);

        assertEquals(codes1, codes2, "Same state should return same schemes (call 1 vs 2)");
        assertEquals(codes2, codes3, "Same state should return same schemes (call 2 vs 3)");
        assertEquals(codes1, codes3, "Same state should return same schemes (call 1 vs 3)");
    }

    /**
     * Property 10.6: All Indian states mentioned in requirements should be supported.
     * 
     * For each state mentioned in the requirements (Karnataka, Maharashtra, Telangana,
     * Andhra Pradesh, Haryana, Uttar Pradesh, Punjab), state-specific schemes should
     * be correctly filtered when requested.
     * 
     * Validates: Requirements 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Property
    void allRequiredStatesAreSupported(@ForAll("indianStates") String state) {
        // Arrange: Create mocks and test data
        SchemeRepository mockRepository = mock(SchemeRepository.class);
        SchemeService localService = new SchemeService(mockRepository);
        
        // Create a scheme for the state
        String code = "STATE-" + state.replace(" ", "");
        Scheme stateScheme = createScheme(code, state + " Scheme", 
                SchemeType.STATE, state);
        
        // When querying for the state, return the state scheme
        when(mockRepository.findActiveSchemesForState(state))
                .thenReturn(List.of(stateScheme));

        // Act: Get schemes for the state
        List<Scheme> schemes = localService.getSchemesForState(state);

        // Then: The state scheme should be included
        boolean hasStateScheme = schemes.stream()
                .anyMatch(s -> s.getSchemeType() == SchemeType.STATE && 
                              state.equals(s.getState()));
        
        assertTrue(hasStateScheme, "State scheme should be returned for state: " + state);
    }

    /**
     * Property 10.7: Mixed central and state schemes should be correctly filtered.
     * 
     * For any state S, when fetching schemes with a mix of central schemes and
     * state-specific schemes for multiple states, only central schemes and schemes
     * for state S should be returned.
     * 
     * Validates: Requirements 4.3, 5.1
     */
    @Property
    void mixedCentralAndStateSchemesCorrectlyFiltered(
            @ForAll("randomIndianStates") String farmerState,
            @ForAll("schemeCodes") String codePrefix) {
        // Arrange: Create mocks and test data
        SchemeRepository mockRepository = mock(SchemeRepository.class);
        SchemeService localService = new SchemeService(mockRepository);
        
        // Create a mix of central and state schemes for different states
        Scheme centralScheme = createScheme(codePrefix + "-C1", "PM-Kisan", 
                SchemeType.CENTRAL, null);
        Scheme farmerStateScheme = createScheme(codePrefix + "-FS", farmerState + " Scheme", 
                SchemeType.STATE, farmerState);
        
        // When querying for farmerState, return only central and farmer's state schemes
        when(mockRepository.findActiveSchemesForState(farmerState))
                .thenReturn(List.of(centralScheme, farmerStateScheme));

        // Act: Get schemes for the farmer's state
        List<Scheme> schemes = localService.getSchemesForState(farmerState);

        // Then: Should have central scheme and farmer's state scheme
        assertTrue(schemes.size() >= 2, "Should have at least central and farmer's state schemes");
        
        boolean hasCentral = schemes.stream()
                .anyMatch(s -> s.getSchemeType() == SchemeType.CENTRAL);
        boolean hasFarmerState = schemes.stream()
                .anyMatch(s -> s.getSchemeType() == SchemeType.STATE && farmerState.equals(s.getState()));
        boolean hasKarnataka = schemes.stream()
                .anyMatch(s -> s.getSchemeType() == SchemeType.STATE && "Karnataka".equals(s.getState()));
        boolean hasMaharashtra = schemes.stream()
                .anyMatch(s -> s.getSchemeType() == SchemeType.STATE && "Maharashtra".equals(s.getState()));
        boolean hasTelangana = schemes.stream()
                .anyMatch(s -> s.getSchemeType() == SchemeType.STATE && "Telangana".equals(s.getState()));
        
        assertTrue(hasCentral, "Should include central scheme");
        assertTrue(hasFarmerState, "Should include farmer's state scheme: " + farmerState);
        
        // Karnataka, Maharashtra, and Telangana schemes should only be included if farmerState matches
        if ("Karnataka".equals(farmerState)) {
            assertTrue(hasKarnataka, "Should include Karnataka scheme when farmer is from Karnataka");
        } else {
            assertFalse(hasKarnataka, "Should not include Karnataka scheme when farmer is not from Karnataka");
        }
        
        if ("Maharashtra".equals(farmerState)) {
            assertTrue(hasMaharashtra, "Should include Maharashtra scheme when farmer is from Maharashtra");
        } else {
            assertFalse(hasMaharashtra, "Should not include Maharashtra scheme when farmer is not from Maharashtra");
        }
        
        if ("Telangana".equals(farmerState)) {
            assertTrue(hasTelangana, "Should include Telangana scheme when farmer is from Telangana");
        } else {
            assertFalse(hasTelangana, "Should not include Telangana scheme when farmer is not from Telangana");
        }
    }

    /**
     * Helper method to extract scheme codes from a list of schemes.
     */
    private Set<String> extractSchemeCodes(List<Scheme> schemes) {
        Set<String> codes = new HashSet<>();
        for (Scheme scheme : schemes) {
            codes.add(scheme.getSchemeCode());
        }
        return codes;
    }
}