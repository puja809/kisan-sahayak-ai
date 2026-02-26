package com.farmer.scheme;

import com.farmer.scheme.dto.SchemeStatistics;
import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.Scheme.SchemeType;
import com.farmer.scheme.repository.SchemeRepository;
import com.farmer.scheme.service.SchemeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SchemeService.
 * Requirements: 4.1, 4.2, 4.3, 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8
 */
@ExtendWith(MockitoExtension.class)
class SchemeServiceTest {

    @Mock
    private SchemeRepository schemeRepository;

    @InjectMocks
    private SchemeService schemeService;

    private Scheme testScheme;

    @BeforeEach
    void setUp() {
        testScheme = Scheme.builder()
                .id(1L)
                .schemeCode("PM-KISAN-001")
                .schemeName("PM-Kisan Samman Nidhi")
                .schemeType(SchemeType.CENTRAL)
                .description("Income support of Rs. 6,000 per year to farmer families")
                .eligibilityCriteria("{\"landholding\": \"any\", \"income\": \"less than 3 lakh/year\"}")
                .benefitAmount(new Double("6000.00"))
                .benefitDescription("Rs. 6,000 per year in three installments of Rs. 2,000 each")
                .applicationStartDate(LocalDate.now().minusDays(30))
                .applicationEndDate(LocalDate.now().plusDays(60))
                .applicationUrl("https://pmkisan.gov.in")
                .contactInfo("{\"phone\": \"011-23381092\", \"email\": \"pmkisan@gov.in\"}")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldGetAllActiveSchemes() {
        // Given
        List<Scheme> schemes = Arrays.asList(testScheme);
        when(schemeRepository.findByIsActiveTrue()).thenReturn(schemes);

        // When
        List<Scheme> result = schemeService.getAllActiveSchemes();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PM-Kisan Samman Nidhi", result.get(0).getSchemeName());
        verify(schemeRepository).findByIsActiveTrue();
    }

    @Test
    void shouldGetSchemeById() {
        // Given
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(testScheme));

        // When
        Optional<Scheme> result = schemeService.getSchemeById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("PM-Kisan Samman Nidhi", result.get().getSchemeName());
        verify(schemeRepository).findById(1L);
    }

    @Test
    void shouldReturnEmptyWhenSchemeNotFound() {
        // Given
        when(schemeRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Scheme> result = schemeService.getSchemeById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(schemeRepository).findById(999L);
    }

    @Test
    void shouldGetSchemeByCode() {
        // Given
        when(schemeRepository.findBySchemeCode("PM-KISAN-001")).thenReturn(Optional.of(testScheme));

        // When
        Optional<Scheme> result = schemeService.getSchemeByCode("PM-KISAN-001");

        // Then
        assertTrue(result.isPresent());
        assertEquals("PM-KISAN-001", result.get().getSchemeCode());
        verify(schemeRepository).findBySchemeCode("PM-KISAN-001");
    }

    @Test
    void shouldGetSchemesByType() {
        // Given
        List<Scheme> schemes = Arrays.asList(testScheme);
        when(schemeRepository.findBySchemeTypeAndIsActiveTrue(SchemeType.CENTRAL)).thenReturn(schemes);

        // When
        List<Scheme> result = schemeService.getSchemesByType(SchemeType.CENTRAL);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(SchemeType.CENTRAL, result.get(0).getSchemeType());
        verify(schemeRepository).findBySchemeTypeAndIsActiveTrue(SchemeType.CENTRAL);
    }

    @Test
    void shouldGetSchemesForState() {
        // Given
        List<Scheme> schemes = Arrays.asList(testScheme);
        when(schemeRepository.findActiveSchemesForState("Maharashtra")).thenReturn(schemes);

        // When
        List<Scheme> result = schemeService.getSchemesForState("Maharashtra");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(schemeRepository).findActiveSchemesForState("Maharashtra");
    }

    @Test
    void shouldGetSchemesByCrop() {
        // Given
        List<Scheme> schemes = Arrays.asList(testScheme);
        when(schemeRepository.findByApplicableCrop("Paddy")).thenReturn(schemes);

        // When
        List<Scheme> result = schemeService.getSchemesByCrop("Paddy");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(schemeRepository).findByApplicableCrop("Paddy");
    }

    @Test
    void shouldGetSchemesWithOpenApplications() {
        // Given
        List<Scheme> schemes = Arrays.asList(testScheme);
        when(schemeRepository.findActiveSchemesWithOpenApplications(any(LocalDate.class))).thenReturn(schemes);

        // When
        List<Scheme> result = schemeService.getSchemesWithOpenApplications();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(schemeRepository).findActiveSchemesWithOpenApplications(any(LocalDate.class));
    }

    @Test
    void shouldGetSchemesWithApproachingDeadlines() {
        // Given
        List<Scheme> schemes = Arrays.asList(testScheme);
        when(schemeRepository.findSchemesWithApproachingDeadlines(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(schemes);

        // When
        List<Scheme> result = schemeService.getSchemesWithApproachingDeadlines(7);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(schemeRepository).findSchemesWithApproachingDeadlines(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void shouldCreateScheme() {
        // Given
        Scheme newScheme = Scheme.builder()
                .schemeCode("NEW-SCHEME-001")
                .schemeName("New Scheme")
                .schemeType(SchemeType.STATE)
                .state("Maharashtra")
                .build();

        when(schemeRepository.save(any(Scheme.class))).thenReturn(newScheme);

        // When
        Scheme result = schemeService.createScheme(newScheme);

        // Then
        assertNotNull(result);
        assertEquals("NEW-SCHEME-001", result.getSchemeCode());
        verify(schemeRepository).save(newScheme);
    }

    @Test
    void shouldUpdateScheme() {
        // Given
        Scheme schemeDetails = Scheme.builder()
                .schemeName("Updated Scheme Name")
                .description("Updated description")
                .benefitAmount(new Double("10000.00"))
                .build();

        when(schemeRepository.findById(1L)).thenReturn(Optional.of(testScheme));
        when(schemeRepository.save(any(Scheme.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Scheme result = schemeService.updateScheme(1L, schemeDetails);

        // Then
        assertNotNull(result);
        assertEquals("Updated Scheme Name", result.getSchemeName());
        assertEquals("Updated description", result.getDescription());
        assertEquals(new Double("10000.00"), result.getBenefitAmount());
        verify(schemeRepository).findById(1L);
        verify(schemeRepository).save(any(Scheme.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentScheme() {
        // Given
        when(schemeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> schemeService.updateScheme(999L, testScheme));
        verify(schemeRepository).findById(999L);
    }

    @Test
    void shouldDeactivateScheme() {
        // Given
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(testScheme));
        when(schemeRepository.save(any(Scheme.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        schemeService.deactivateScheme(1L);

        // Then
        verify(schemeRepository).findById(1L);
        verify(schemeRepository).save(argThat(scheme -> !scheme.getIsActive()));
    }

    @Test
    void shouldGetAllCentralSchemes() {
        // Given
        List<Scheme> schemes = Arrays.asList(testScheme);
        when(schemeRepository.findAllCentralSchemes()).thenReturn(schemes);

        // When
        List<Scheme> result = schemeService.getAllCentralSchemes();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(schemeRepository).findAllCentralSchemes();
    }

    @Test
    void shouldGetStateSchemes() {
        // Given
        Scheme stateScheme = Scheme.builder()
                .id(2L)
                .schemeCode("MAHA-SCHEME-001")
                .schemeName("Maharashtra State Scheme")
                .schemeType(SchemeType.STATE)
                .state("Maharashtra")
                .build();

        List<Scheme> schemes = Arrays.asList(stateScheme);
        when(schemeRepository.findStateSchemes("Maharashtra")).thenReturn(schemes);

        // When
        List<Scheme> result = schemeService.getStateSchemes("Maharashtra");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Maharashtra", result.get(0).getState());
        verify(schemeRepository).findStateSchemes("Maharashtra");
    }

    @Test
    void shouldGetAllSchemesIncludingInactive() {
        // Given
        Scheme activeScheme = Scheme.builder()
                .id(1L)
                .schemeCode("ACTIVE-001")
                .schemeName("Active Scheme")
                .schemeType(SchemeType.CENTRAL)
                .isActive(true)
                .build();

        Scheme inactiveScheme = Scheme.builder()
                .id(2L)
                .schemeCode("INACTIVE-001")
                .schemeName("Inactive Scheme")
                .schemeType(SchemeType.STATE)
                .isActive(false)
                .build();

        List<Scheme> schemes = Arrays.asList(activeScheme, inactiveScheme);
        when(schemeRepository.findAll()).thenReturn(schemes);

        // When
        List<Scheme> result = schemeService.getAllSchemes();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(schemeRepository).findAll();
    }

    @Test
    void shouldActivateDeactivatedScheme() {
        // Given
        testScheme.setIsActive(false);
        when(schemeRepository.findById(1L)).thenReturn(Optional.of(testScheme));
        when(schemeRepository.save(any(Scheme.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Scheme result = schemeService.activateScheme(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.getIsActive());
        verify(schemeRepository).findById(1L);
        verify(schemeRepository).save(any(Scheme.class));
    }

    @Test
    void shouldThrowExceptionWhenActivatingNonExistentScheme() {
        // Given
        when(schemeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> schemeService.activateScheme(999L));
        verify(schemeRepository).findById(999L);
    }

    @Test
    void shouldGetSchemeStatistics() {
        // Given
        when(schemeRepository.count()).thenReturn(10L);
        when(schemeRepository.countByIsActiveTrue()).thenReturn(8L);
        when(schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.CENTRAL)).thenReturn(3L);
        when(schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.STATE)).thenReturn(2L);
        when(schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.CROP_SPECIFIC)).thenReturn(1L);
        when(schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.INSURANCE)).thenReturn(1L);
        when(schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.SUBSIDY)).thenReturn(1L);
        when(schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.WELFARE)).thenReturn(0L);

        // When
        SchemeStatistics stats = schemeService.getSchemeStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(10L, stats.totalSchemes());
        assertEquals(8L, stats.activeSchemes());
        assertEquals(3L, stats.centralSchemes());
        assertEquals(2L, stats.stateSchemes());
        assertEquals(1L, stats.cropSpecificSchemes());
        assertEquals(1L, stats.insuranceSchemes());
        assertEquals(1L, stats.subsidySchemes());
        assertEquals(0L, stats.welfareSchemes());
    }

    @Test
    void shouldGetSchemesForAllIndianStates() {
        // Test all states mentioned in requirements
        String[] states = {"Karnataka", "Maharashtra", "Telangana", "Andhra Pradesh", 
                          "Haryana", "Uttar Pradesh", "Punjab"};

        for (String state : states) {
            // Given
            List<Scheme> schemes = Arrays.asList(testScheme);
            when(schemeRepository.findActiveSchemesForState(state)).thenReturn(schemes);

            // When
            List<Scheme> result = schemeService.getSchemesForState(state);

            // Then
            assertNotNull(result);
            verify(schemeRepository).findActiveSchemesForState(state);
        }
    }

    @Test
    void shouldGetSchemesByAllSchemeTypes() {
        // Test all scheme types
        SchemeType[] types = SchemeType.values();

        for (SchemeType type : types) {
            // Given
            List<Scheme> schemes = Arrays.asList(testScheme);
            when(schemeRepository.findBySchemeTypeAndIsActiveTrue(type)).thenReturn(schemes);

            // When
            List<Scheme> result = schemeService.getSchemesByType(type);

            // Then
            assertNotNull(result);
            verify(schemeRepository).findBySchemeTypeAndIsActiveTrue(type);
        }
    }

    @Test
    void shouldHandleEmptyResultsForStateWithNoSchemes() {
        // Given
        when(schemeRepository.findActiveSchemesForState("Sikkim")).thenReturn(Arrays.asList());

        // When
        List<Scheme> result = schemeService.getSchemesForState("Sikkim");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(schemeRepository).findActiveSchemesForState("Sikkim");
    }

    @Test
    void shouldHandleEmptyResultsForCropWithNoSchemes() {
        // Given
        when(schemeRepository.findByApplicableCrop("Coffee")).thenReturn(Arrays.asList());

        // When
        List<Scheme> result = schemeService.getSchemesByCrop("Coffee");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(schemeRepository).findByApplicableCrop("Coffee");
    }
}