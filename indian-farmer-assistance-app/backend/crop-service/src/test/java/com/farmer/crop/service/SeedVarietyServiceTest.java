package com.farmer.crop.service;

import com.farmer.crop.dto.CropRecommendationRequestDto;
import com.farmer.crop.dto.SeedVarietyDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SeedVarietyService.
 * 
 * Tests the state-released seed variety recommendations.
 * 
 * Validates: Requirement 2.9
 */
class SeedVarietyServiceTest {

    private SeedVarietyService seedVarietyService;

    @BeforeEach
    void setUp() {
        seedVarietyService = new SeedVarietyService();
    }

    @Test
    @DisplayName("Should get recommended varieties for a crop in a state")
    void testGetRecommendedVarieties() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getRecommendedVarieties("RICE", "Punjab");

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> v.getCropCode().equals("RICE")));
    }

    @Test
    @DisplayName("Should return varieties for different states")
    void testGetRecommendedVarieties_DifferentStates() {
        // Act
        List<SeedVarietyDto> upVarieties = seedVarietyService.getRecommendedVarieties("WHEAT", "Uttar Pradesh");
        List<SeedVarietyDto> punjabVarieties = seedVarietyService.getRecommendedVarieties("WHEAT", "Punjab");

        // Assert
        assertNotNull(upVarieties);
        assertNotNull(punjabVarieties);
    }

    @Test
    @DisplayName("Should get all varieties for a crop")
    void testGetAllVarietiesForCrop() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getAllVarietiesForCrop("RICE");

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> v.getCropCode().equals("RICE")));
    }

    @Test
    @DisplayName("Should get drought-tolerant varieties")
    void testGetDroughtTolerantVarieties() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getDroughtTolerantVarieties("GROUNDNUT");

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> Boolean.TRUE.equals(v.getDroughtTolerant())));
    }

    @Test
    @DisplayName("Should get flood-tolerant varieties")
    void testGetFloodTolerantVarieties() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getFloodTolerantVarieties("RICE");

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> Boolean.TRUE.equals(v.getFloodTolerant())));
    }

    @Test
    @DisplayName("Should get heat-tolerant varieties")
    void testGetHeatTolerantVarieties() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getHeatTolerantVarieties("WHEAT");

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> Boolean.TRUE.equals(v.getHeatTolerant())));
    }

    @Test
    @DisplayName("Should get varieties for Kharif season")
    void testGetVarietiesForSeason_Kharif() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getVarietiesForSeason(
                "RICE", CropRecommendationRequestDto.Season.KHARIF);

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> 
                v.getSeasonSuitability() != null && 
                Boolean.TRUE.equals(v.getSeasonSuitability().getKharif())));
    }

    @Test
    @DisplayName("Should get varieties for Rabi season")
    void testGetVarietiesForSeason_Rabi() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getVarietiesForSeason(
                "WHEAT", CropRecommendationRequestDto.Season.RABI);

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> 
                v.getSeasonSuitability() != null && 
                Boolean.TRUE.equals(v.getSeasonSuitability().getRabi())));
    }

    @Test
    @DisplayName("Should get variety by ID")
    void testGetVarietyById() {
        // Act
        Optional<SeedVarietyDto> variety = seedVarietyService.getVarietyById("RICE-UP-001");

        // Assert
        assertTrue(variety.isPresent());
        assertEquals("RICE-UP-001", variety.get().getVarietyId());
        assertEquals("PB-1509", variety.get().getVarietyName());
    }

    @Test
    @DisplayName("Should return empty optional for unknown variety ID")
    void testGetVarietyById_NotFound() {
        // Act
        Optional<SeedVarietyDto> variety = seedVarietyService.getVarietyById("UNKNOWN-ID");

        // Assert
        assertFalse(variety.isPresent());
    }

    @Test
    @DisplayName("Should get state recommendations as list of names")
    void testGetStateRecommendations() {
        // Act
        List<String> recommendations = seedVarietyService.getStateRecommendations("RICE", "Punjab");

        // Assert
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().allMatch(name -> name instanceof String));
    }

    @Test
    @DisplayName("Should get states where crop varieties are available")
    void testGetStatesForCrop() {
        // Act
        List<String> states = seedVarietyService.getStatesForCrop("RICE");

        // Assert
        assertNotNull(states);
        assertFalse(states.isEmpty());
        assertTrue(states.contains("Punjab") || states.contains("Uttar Pradesh"));
    }

    @Test
    @DisplayName("Should return empty list for unknown crop")
    void testGetRecommendedVarieties_UnknownCrop() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getRecommendedVarieties("UNKNOWN", "Unknown State");

        // Assert
        assertNotNull(varieties);
        assertTrue(varieties.isEmpty());
    }

    @Test
    @DisplayName("Should include variety characteristics")
    void testVarietyCharacteristics() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getRecommendedVarieties("RICE", "Punjab");

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> 
                v.getCharacteristics() != null && !v.getCharacteristics().isEmpty()));
    }

    @Test
    @DisplayName("Should include disease resistance information")
    void testDiseaseResistance() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getRecommendedVarieties("WHEAT", "Punjab");

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> 
                v.getDiseaseResistance() != null && !v.getDiseaseResistance().isEmpty()));
    }

    @Test
    @DisplayName("Should include climate resilience information")
    void testClimateResilience() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getRecommendedVarieties("COTTON", "Maharashtra");

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> 
                v.getClimateResilience() != null && !v.getClimateResilience().isEmpty()));
    }

    @Test
    @DisplayName("Should include seed cost information")
    void testSeedCost() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getRecommendedVarieties("RICE", "Punjab");

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> v.getSeedCostPerKg() != null));
    }

    @Test
    @DisplayName("Should include maturity days information")
    void testMaturityDays() {
        // Act
        List<SeedVarietyDto> varieties = seedVarietyService.getRecommendedVarieties("WHEAT", "Punjab");

        // Assert
        assertNotNull(varieties);
        assertFalse(varieties.isEmpty());
        assertTrue(varieties.stream().allMatch(v -> v.getMaturityDays() != null && v.getMaturityDays() > 0));
    }
}

