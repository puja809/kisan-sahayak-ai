package com.farmer.crop.service;

import com.farmer.crop.dto.CropRecommendationRequestDto;
import com.farmer.crop.dto.SeedVarietyDto;
import com.farmer.crop.dto.SeedVarietyDto.SeasonSuitability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing state-released seed varieties.
 * 
 * This service provides information about seed varieties released by
 * state agricultural universities and institutes, suitable for
 * specific locations and conditions.
 * 
 * Validates: Requirement 2.9
 */
@Service
@Transactional(readOnly = true)
public class SeedVarietyService {

    private static final Logger logger = LoggerFactory.getLogger(SeedVarietyService.class);

    // State-released seed varieties database
    private static final List<SeedVarietyDto> SEED_VARIETIES = new ArrayList<>();

    static {
        // Rice varieties
        SEED_VARIETIES.add(SeedVarietyDto.builder()
                .varietyId("RICE-UP-001")
                .cropCode("RICE")
                .cropName("Rice")
                .varietyName("PB-1509")
                .varietyNameLocal("पीबी-1509")
                .releasingInstitute("Punjab Agricultural University, Ludhiana")
                .releaseYear(2013)
                .recommendedStates(Arrays.asList("Punjab", "Haryana", "Uttar Pradesh"))
                .suitableZones(Arrays.asList("AEZ-04", "AEZ-05"))
                .seasonSuitability(SeasonSuitability.builder().kharif(true).rabi(false).zaid(false).build())
                .maturityDays(145)
                .averageYieldQtlHa(45.0)
                .potentialYieldQtlHa(55.0)
                .characteristics(Arrays.asList("Basmati quality", "Medium slender grain", "Excellent cooking quality"))
                .diseaseResistance(Arrays.asList("Blast resistant", "Bacterial leaf blight tolerant"))
                .climateResilience(Arrays.asList("Heat tolerant during flowering"))
                .waterRequirementMm(900.0)
                .droughtTolerant(false)
                .floodTolerant(false)
                .heatTolerant(true)
                .seedRateKgHa(20.0)
                .spacing("20cm x 15cm")
                .isAvailable(true)
                .seedCostPerKg(45.0)
                .build());

        SEED_VARIETIES.add(SeedVarietyDto.builder()
                .varietyId("RICE-UP-002")
                .cropCode("RICE")
                .cropName("Rice")
                .varietyName("HD-2967")
                .varietyNameLocal("एचडी-2967")
                .releasingInstitute("Indian Agricultural Research Institute, New Delhi")
                .releaseYear(2011)
                .recommendedStates(Arrays.asList("Uttar Pradesh", "Punjab", "Haryana", "Madhya Pradesh"))
                .suitableZones(Arrays.asList("AEZ-04", "AEZ-05", "AEZ-10"))
                .seasonSuitability(SeasonSuitability.builder().kharif(true).rabi(false).zaid(false).build())
                .maturityDays(142)
                .averageYieldQtlHa(48.0)
                .potentialYieldQtlHa(60.0)
                .characteristics(Arrays.asList("High yielding", "Long slender grain", "Good cooking quality"))
                .diseaseResistance(Arrays.asList("Blast moderately resistant"))
                .climateResilience(Arrays.asList("Tolerant to temperature fluctuations"))
                .waterRequirementMm(1000.0)
                .droughtTolerant(false)
                .floodTolerant(false)
                .heatTolerant(true)
                .seedRateKgHa(20.0)
                .spacing("20cm x 15cm")
                .isAvailable(true)
                .seedCostPerKg(40.0)
                .build());

        SEED_VARIETIES.add(SeedVarietyDto.builder()
                .varietyId("RICE-MH-001")
                .cropCode("RICE")
                .cropName("Rice")
                .varietyName("WHD-1")
                .varietyNameLocal("डब्ल्यूएचडी-1")
                .releasingInstitute("Dr. Balasaheb Sawant Konkan Krishi Vidyapeeth, Dapoli")
                .releaseYear(2015)
                .recommendedStates(Arrays.asList("Maharashtra", "Goa", "Karnataka"))
                .suitableZones(Arrays.asList("AEZ-06", "AEZ-07"))
                .seasonSuitability(SeasonSuitability.builder().kharif(true).rabi(false).zaid(false).build())
                .maturityDays(150)
                .averageYieldQtlHa(42.0)
                .potentialYieldQtlHa(52.0)
                .characteristics(Arrays.asList("Suitable for coastal regions", "Medium slender grain"))
                .diseaseResistance(Arrays.asList("Blast resistant", "Sheath blight tolerant"))
                .climateResilience(Arrays.asList("Salt tolerant", "Tolerant to high humidity"))
                .waterRequirementMm(850.0)
                .droughtTolerant(false)
                .floodTolerant(true)
                .heatTolerant(false)
                .seedRateKgHa(22.0)
                .spacing("20cm x 15cm")
                .isAvailable(true)
                .seedCostPerKg(42.0)
                .build());

        // Wheat varieties
        SEED_VARIETIES.add(SeedVarietyDto.builder()
                .varietyId("WHEAT-UP-001")
                .cropCode("WHEAT")
                .cropName("Wheat")
                .varietyName("HD-3086")
                .varietyNameLocal("एचडी-3086")
                .releasingInstitute("Indian Agricultural Research Institute, New Delhi")
                .releaseYear(2014)
                .recommendedStates(Arrays.asList("Uttar Pradesh", "Punjab", "Haryana", "Madhya Pradesh"))
                .suitableZones(Arrays.asList("AEZ-04", "AEZ-05", "AEZ-10"))
                .seasonSuitability(SeasonSuitability.builder().kharif(false).rabi(true).zaid(false).build())
                .maturityDays(122)
                .averageYieldQtlHa(50.0)
                .potentialYieldQtlHa(65.0)
                .characteristics(Arrays.asList("High yielding", "Early maturing", "Good grain quality"))
                .diseaseResistance(Arrays.asList("Rust resistant", "Powdery mildew tolerant"))
                .climateResilience(Arrays.asList("Heat tolerant during grain filling"))
                .waterRequirementMm(450.0)
                .droughtTolerant(false)
                .floodTolerant(false)
                .heatTolerant(true)
                .seedRateKgHa(100.0)
                .spacing("22.5cm x 10cm")
                .isAvailable(true)
                .seedCostPerKg(25.0)
                .build());

        SEED_VARIETIES.add(SeedVarietyDto.builder()
                .varietyId("WHEAT-PB-001")
                .cropCode("WHEAT")
                .cropName("Wheat")
                .varietyName("DBW-187")
                .varietyNameLocal("डीबीडब्ल्यू-187")
                .releasingInstitute("Punjab Agricultural University, Ludhiana")
                .releaseYear(2017)
                .recommendedStates(Arrays.asList("Punjab", "Haryana"))
                .suitableZones(Arrays.asList("AEZ-04"))
                .seasonSuitability(SeasonSuitability.builder().kharif(false).rabi(true).zaid(false).build())
                .maturityDays(135)
                .averageYieldQtlHa(52.0)
                .potentialYieldQtlHa(68.0)
                .characteristics(Arrays.asList("Extra bold grain", "High protein content", "Excellent chapatti quality"))
                .diseaseResistance(Arrays.asList("Rust resistant", "Leaf blight tolerant"))
                .climateResilience(Arrays.asList("Tolerant to terminal heat stress"))
                .waterRequirementMm(500.0)
                .droughtTolerant(false)
                .floodTolerant(false)
                .heatTolerant(true)
                .seedRateKgHa(100.0)
                .spacing("22.5cm x 10cm")
                .isAvailable(true)
                .seedCostPerKg(28.0)
                .build());

        // Cotton varieties
        SEED_VARIETIES.add(SeedVarietyDto.builder()
                .varietyId("COTTON-MH-001")
                .cropCode("COTTON")
                .cropName("Cotton")
                .varietyName("Bt Cotton Hybrid")
                .varietyNameLocal("बीटी कॉटन संकर")
                .releasingInstitute("Mahatma Phule Krishi Vidyapeeth, Rahuri")
                .releaseYear(2018)
                .recommendedStates(Arrays.asList("Maharashtra", "Gujarat", "Madhya Pradesh"))
                .suitableZones(Arrays.asList("AEZ-06", "AEZ-09", "AEZ-10"))
                .seasonSuitability(SeasonSuitability.builder().kharif(true).rabi(false).zaid(false).build())
                .maturityDays(160)
                .averageYieldQtlHa(18.0)
                .potentialYieldQtlHa(25.0)
                .characteristics(Arrays.asList("Bt technology", "Pink bollworm resistant", "Medium staple"))
                .diseaseResistance(Arrays.asList("Cotton leaf curl disease tolerant"))
                .climateResilience(Arrays.asList("Drought tolerant", "Heat tolerant"))
                .waterRequirementMm(550.0)
                .droughtTolerant(true)
                .floodTolerant(false)
                .heatTolerant(true)
                .seedRateKgHa(1.5)
                .spacing("90cm x 60cm")
                .isAvailable(true)
                .seedCostPerKg(850.0)
                .build());

        // Soybean varieties
        SEED_VARIETIES.add(SeedVarietyDto.builder()
                .varietyId("SOY-MP-001")
                .cropCode("SOYBEAN")
                .cropName("Soybean")
                .varietyName("JS-335")
                .varietyNameLocal("जेएस-335")
                .releasingInstitute("Jawaharlal Nehru Krishi Vishwa Vidyalaya, Jabalpur")
                .releaseYear(2008)
                .recommendedStates(Arrays.asList("Madhya Pradesh", "Maharashtra", "Rajasthan"))
                .suitableZones(Arrays.asList("AEZ-09", "AEZ-10"))
                .seasonSuitability(SeasonSuitability.builder().kharif(true).rabi(false).zaid(false).build())
                .maturityDays(95)
                .averageYieldQtlHa(24.0)
                .potentialYieldQtlHa(32.0)
                .characteristics(Arrays.asList("Early maturing", "Semi-determinate growth habit", "Bold seed"))
                .diseaseResistance(Arrays.asList("Mosaic virus resistant", "Rust tolerant"))
                .climateResilience(Arrays.asList("Drought tolerant", "Low input requirement"))
                .waterRequirementMm(400.0)
                .droughtTolerant(true)
                .floodTolerant(false)
                .heatTolerant(false)
                .seedRateKgHa(65.0)
                .spacing("45cm x 5cm")
                .isAvailable(true)
                .seedCostPerKg(55.0)
                .build());

        // Groundnut varieties
        SEED_VARIETIES.add(SeedVarietyDto.builder()
                .varietyId("GNDT-AP-001")
                .cropCode("GROUNDNUT")
                .cropName("Groundnut")
                .varietyName("TAG-24")
                .varietyNameLocal("टीएजी-24")
                .releasingInstitute("Acharya N.G. Ranga Agricultural University, Hyderabad")
                .releaseYear(1995)
                .recommendedStates(Arrays.asList("Andhra Pradesh", "Telangana", "Karnataka", "Tamil Nadu"))
                .suitableZones(Arrays.asList("AEZ-06", "AEZ-07", "AEZ-08"))
                .seasonSuitability(SeasonSuitability.builder().kharif(true).rabi(false).zaid(true).build())
                .maturityDays(110)
                .averageYieldQtlHa(28.0)
                .potentialYieldQtlHa(38.0)
                .characteristics(Arrays.asList("Spanish bunch type", "Bold kernels", "High oil content"))
                .diseaseResistance(Arrays.asList("Tikka disease tolerant", "Rust tolerant"))
                .climateResilience(Arrays.asList("Drought tolerant", "Heat tolerant"))
                .waterRequirementMm(450.0)
                .droughtTolerant(true)
                .floodTolerant(false)
                .heatTolerant(true)
                .seedRateKgHa(120.0)
                .spacing("30cm x 10cm")
                .isAvailable(true)
                .seedCostPerKg(65.0)
                .build());

        // Mustard varieties
        SEED_VARIETIES.add(SeedVarietyDto.builder()
                .varietyId("MST-UP-001")
                .cropCode("MUSTARD")
                .cropName("Mustard")
                .varietyName("Varuna")
                .varietyNameLocal("वरुणा")
                .releasingInstitute("Chandra Shekhar Azad University of Agriculture & Technology, Kanpur")
                .releaseYear(1988)
                .recommendedStates(Arrays.asList("Uttar Pradesh", "Rajasthan", "Madhya Pradesh"))
                .suitableZones(Arrays.asList("AEZ-04", "AEZ-05", "AEZ-09", "AEZ-10"))
                .seasonSuitability(SeasonSuitability.builder().kharif(false).rabi(true).zaid(false).build())
                .maturityDays(110)
                .averageYieldQtlHa(18.0)
                .potentialYieldQtlHa(25.0)
                .characteristics(Arrays.asList("High yielding", "Bold seed", "High oil content (40%)"))
                .diseaseResistance(Arrays.asList("Alternaria blight tolerant", "White rust tolerant"))
                .climateResilience(Arrays.asList("Heat tolerant", "Cold tolerant"))
                .waterRequirementMm(350.0)
                .droughtTolerant(true)
                .floodTolerant(false)
                .heatTolerant(true)
                .seedRateKgHa(5.0)
                .spacing("45cm x 15cm")
                .isAvailable(true)
                .seedCostPerKg(120.0)
                .build());

        // Maize varieties
        SEED_VARIETIES.add(SeedVarietyDto.builder()
                .varietyId("MAIZE-PB-001")
                .cropCode("MAIZE")
                .cropName("Maize")
                .varietyName("PMH-1")
                .varietyNameLocal("पीएमएच-1")
                .releasingInstitute("Punjab Agricultural University, Ludhiana")
                .releaseYear(2010)
                .recommendedStates(Arrays.asList("Punjab", "Haryana", "Uttar Pradesh"))
                .suitableZones(Arrays.asList("AEZ-04", "AEZ-05"))
                .seasonSuitability(SeasonSuitability.builder().kharif(true).rabi(false).zaid(true).build())
                .maturityDays(95)
                .averageYieldQtlHa(45.0)
                .potentialYieldQtlHa(60.0)
                .characteristics(Arrays.asList("Single cross hybrid", "Yellow flinty grain", "High starch content"))
                .diseaseResistance(Arrays.asList("Maydis leaf blight resistant", "Turcicum leaf blight tolerant"))
                .climateResilience(Arrays.asList("Heat tolerant during flowering"))
                .waterRequirementMm(500.0)
                .droughtTolerant(false)
                .floodTolerant(false)
                .heatTolerant(true)
                .seedRateKgHa(20.0)
                .spacing("60cm x 20cm")
                .isAvailable(true)
                .seedCostPerKg(250.0)
                .build());
    }

    /**
     * Get recommended seed varieties for a crop in a state.
     * 
     * @param cropCode Crop code
     * @param state State name
     * @return List of recommended varieties
     * 
     * Validates: Requirement 2.9
     */
    public List<SeedVarietyDto> getRecommendedVarieties(String cropCode, String state) {
        logger.info("Fetching recommended varieties for crop: {} in state: {}", cropCode, state);
        
        return SEED_VARIETIES.stream()
                .filter(v -> v.getCropCode().equalsIgnoreCase(cropCode))
                .filter(v -> v.getRecommendedStates().stream()
                        .anyMatch(s -> s.equalsIgnoreCase(state)))
                .sorted(Comparator.comparing(SeedVarietyDto::getReleaseYear).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get all varieties for a crop.
     * 
     * @param cropCode Crop code
     * @return List of all varieties for the crop
     */
    public List<SeedVarietyDto> getAllVarietiesForCrop(String cropCode) {
        return SEED_VARIETIES.stream()
                .filter(v -> v.getCropCode().equalsIgnoreCase(cropCode))
                .sorted(Comparator.comparing(SeedVarietyDto::getReleaseYear).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get drought-tolerant varieties for a crop.
     * 
     * @param cropCode Crop code
     * @return List of drought-tolerant varieties
     * 
     * Validates: Requirement 2.9
     */
    public List<SeedVarietyDto> getDroughtTolerantVarieties(String cropCode) {
        return SEED_VARIETIES.stream()
                .filter(v -> v.getCropCode().equalsIgnoreCase(cropCode))
                .filter(v -> Boolean.TRUE.equals(v.getDroughtTolerant()))
                .collect(Collectors.toList());
    }

    /**
     * Get flood-tolerant varieties for a crop.
     * 
     * @param cropCode Crop code
     * @return List of flood-tolerant varieties
     * 
     * Validates: Requirement 2.9
     */
    public List<SeedVarietyDto> getFloodTolerantVarieties(String cropCode) {
        return SEED_VARIETIES.stream()
                .filter(v -> v.getCropCode().equalsIgnoreCase(cropCode))
                .filter(v -> Boolean.TRUE.equals(v.getFloodTolerant()))
                .collect(Collectors.toList());
    }

    /**
     * Get heat-tolerant varieties for a crop.
     * 
     * @param cropCode Crop code
     * @return List of heat-tolerant varieties
     * 
     * Validates: Requirement 2.9
     */
    public List<SeedVarietyDto> getHeatTolerantVarieties(String cropCode) {
        return SEED_VARIETIES.stream()
                .filter(v -> v.getCropCode().equalsIgnoreCase(cropCode))
                .filter(v -> Boolean.TRUE.equals(v.getHeatTolerant()))
                .collect(Collectors.toList());
    }

    /**
     * Get varieties suitable for a specific season.
     * 
     * @param cropCode Crop code
     * @param season Season (KHARIF, RABI, ZAID)
     * @return List of suitable varieties
     */
    public List<SeedVarietyDto> getVarietiesForSeason(
            String cropCode, CropRecommendationRequestDto.Season season) {
        
        return SEED_VARIETIES.stream()
                .filter(v -> v.getCropCode().equalsIgnoreCase(cropCode))
                .filter(v -> {
                    SeasonSuitability ss = v.getSeasonSuitability();
                    if (ss == null) return false;
                    return switch (season) {
                        case KHARIF -> Boolean.TRUE.equals(ss.getKharif());
                        case RABI -> Boolean.TRUE.equals(ss.getRabi());
                        case ZAID -> Boolean.TRUE.equals(ss.getZaid());
                        default -> false;
                    };
                })
                .collect(Collectors.toList());
    }

    /**
     * Get variety by ID.
     * 
     * @param varietyId Variety ID
     * @return Optional containing the variety if found
     */
    public Optional<SeedVarietyDto> getVarietyById(String varietyId) {
        return SEED_VARIETIES.stream()
                .filter(v -> v.getVarietyId().equals(varietyId))
                .findFirst();
    }

    /**
     * Get state-specific recommendations for a crop.
     * 
     * @param cropCode Crop code
     * @param state State name
     * @return List of variety names recommended for the state
     * 
     * Validates: Requirement 2.9
     */
    public List<String> getStateRecommendations(String cropCode, String state) {
        return getRecommendedVarieties(cropCode, state).stream()
                .map(SeedVarietyDto::getVarietyName)
                .collect(Collectors.toList());
    }

    /**
     * Get all available states for a crop.
     * 
     * @param cropCode Crop code
     * @return List of states where varieties are recommended
     */
    public List<String> getStatesForCrop(String cropCode) {
        return SEED_VARIETIES.stream()
                .filter(v -> v.getCropCode().equalsIgnoreCase(cropCode))
                .flatMap(v -> v.getRecommendedStates().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}








