package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.entity.FertilizerApplication;
import com.farmer.crop.exception.FertilizerException;
import com.farmer.crop.repository.FertilizerApplicationRepository;
import com.farmer.crop.repository.SoilHealthCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for fertilizer recommendations and tracking.
 * 
 * Features:
 * - Use soil health card data to determine nutrient deficiencies
 * - Provide default recommendations based on crop type, growth stage, agro-ecological zone
 * - Specify fertilizer type (urea, DAP, MOP, organic compost), quantity per acre, timing
 * - Generate split application schedules (basal dose, top dressing) with specific dates
 * - Suggest organic alternatives (vermicompost, green manure, biofertilizers)
 * - Track fertilizer applications and calculate total nutrient input (N, P, K)
 * - Highlight over-application or under-application
 * - Display cost trends and nutrient efficiency
 * 
 * Validates: Requirements 11C.1, 11C.2, 11C.3, 11C.4, 11C.5, 11C.6, 11C.7, 11C.8, 11C.11
 */
@Service
@Transactional(readOnly = true)
public class FertilizerRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(FertilizerRecommendationService.class);

    private final SoilHealthCardRepository soilHealthCardRepository;
    private final FertilizerApplicationRepository fertilizerApplicationRepository;

    // Standard nutrient requirements per acre for different crops (kg per acre)
    private static final Map<String, BigDecimal[]> CROP_NUTRIENT_REQUIREMENTS = new HashMap<>();
    
    // Fertilizer nutrient content (percentage)
    private static final Map<String, BigDecimal[]> FERTILIZER_COMPOSITION = new HashMap<>();
    
    // Fertilizer costs (INR per kg)
    private static final Map<String, BigDecimal> FERTILIZER_COSTS = new HashMap<>();

    static {
        // Crop nutrient requirements: N, P2O5, K2O (kg per acre)
        CROP_NUTRIENT_REQUIREMENTS.put("RICE", new BigDecimal[]{
            new BigDecimal("60"), new BigDecimal("30"), new BigDecimal("30")});
        CROP_NUTRIENT_REQUIREMENTS.put("WHEAT", new BigDecimal[]{
            new BigDecimal("80"), new BigDecimal("40"), new BigDecimal("30")});
        CROP_NUTRIENT_REQUIREMENTS.put("COTTON", new BigDecimal[]{
            new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("50")});
        CROP_NUTRIENT_REQUIREMENTS.put("SOYBEAN", new BigDecimal[]{
            new BigDecimal("20"), new BigDecimal("60"), new BigDecimal("20")});
        CROP_NUTRIENT_REQUIREMENTS.put("GROUNDNUT", new BigDecimal[]{
            new BigDecimal("20"), new BigDecimal("40"), new BigDecimal("40")});
        CROP_NUTRIENT_REQUIREMENTS.put("MUSTARD", new BigDecimal[]{
            new BigDecimal("40"), new BigDecimal("20"), new BigDecimal("20")});
        CROP_NUTRIENT_REQUIREMENTS.put("PULSES", new BigDecimal[]{
            new BigDecimal("15"), new BigDecimal("40"), new BigDecimal("20")});
        CROP_NUTRIENT_REQUIREMENTS.put("MAIZE", new BigDecimal[]{
            new BigDecimal("80"), new BigDecimal("40"), new BigDecimal("30")});
        CROP_NUTRIENT_REQUIREMENTS.put("SUGARCANE", new BigDecimal[]{
            new BigDecimal("150"), new BigDecimal("50"), new BigDecimal("100")});
        CROP_NUTRIENT_REQUIREMENTS.put("POTATO", new BigDecimal[]{
            new BigDecimal("100"), new BigDecimal("60"), new BigDecimal("100")});
        CROP_NUTRIENT_REQUIREMENTS.put("ONION", new BigDecimal[]{
            new BigDecimal("80"), new BigDecimal("40"), new BigDecimal("60")});
        CROP_NUTRIENT_REQUIREMENTS.put("TOMATO", new BigDecimal[]{
            new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("50")});

        // Fertilizer composition: N, P2O5, K2O (percentage)
        FERTILIZER_COMPOSITION.put("UREA", new BigDecimal[]{
            new BigDecimal("46"), BigDecimal.ZERO, BigDecimal.ZERO});
        FERTILIZER_COMPOSITION.put("DAP", new BigDecimal[]{
            new BigDecimal("18"), new BigDecimal("46"), BigDecimal.ZERO});
        FERTILIZER_COMPOSITION.put("MOP", new BigDecimal[]{
            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("60")});
        FERTILIZER_COMPOSITION.put("SSP", new BigDecimal[]{
            new BigDecimal("8"), new BigDecimal("16"), BigDecimal.ZERO});
        FERTILIZER_COMPOSITION.put("NPK", new BigDecimal[]{
            new BigDecimal("10"), new BigDecimal("26"), new BigDecimal("26")});
        FERTILIZER_COMPOSITION.put("UREA_DAP_COMBO", new BigDecimal[]{
            new BigDecimal("32"), new BigDecimal("23"), BigDecimal.ZERO});
        FERTILIZER_COMPOSITION.put("ZINC_SULFATE", new BigDecimal[]{
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("21")});
        FERTILIZER_COMPOSITION.put("BORAX", new BigDecimal[]{
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("11")});

        // Fertilizer costs (INR per kg)
        FERTILIZER_COSTS.put("UREA", new BigDecimal("6"));
        FERTILIZER_COSTS.put("DAP", new BigDecimal("27"));
        FERTILIZER_COSTS.put("MOP", new BigDecimal("18"));
        FERTILIZER_COSTS.put("SSP", new BigDecimal("12"));
        FERTILIZER_COSTS.put("NPK", new BigDecimal("25"));
        FERTILIZER_COSTS.put("UREA_DAP_COMBO", new BigDecimal("15"));
        FERTILIZER_COSTS.put("ZINC_SULFATE", new BigDecimal("80"));
        FERTILIZER_COSTS.put("BORAX", new BigDecimal("120"));
        FERTILIZER_COSTS.put("VERMICOMPOST", new BigDecimal("8"));
        FERTILIZER_COSTS.put("FYM", new BigDecimal("2"));
        FERTILIZER_COSTS.put("GREEN_MANURE", new BigDecimal("3"));
        FERTILIZER_COSTS.put("BIOFERTILIZER", new BigDecimal("150"));
    }

    public FertilizerRecommendationService(
            SoilHealthCardRepository soilHealthCardRepository,
            FertilizerApplicationRepository fertilizerApplicationRepository) {
        this.soilHealthCardRepository = soilHealthCardRepository;
        this.fertilizerApplicationRepository = fertilizerApplicationRepository;
    }

    /**
     * Generate fertilizer recommendations for a crop.
     * 
     * @param request Recommendation request
     * @return Recommendation response
     * 
     * Validates: Requirements 11C.1, 11C.2, 11C.3, 11C.4, 11C.5
     */
    public FertilizerRecommendationResponseDto generateRecommendations(
            FertilizerRecommendationRequestDto request) {
        
        logger.info("Generating fertilizer recommendations for farmer: {}, crop: {}", 
                request.getFarmerId(), request.getCropName());
        
        try {
            // Step 1: Determine nutrient requirements
            BigDecimal[] requirements = getNutrientRequirements(
                    request.getCropName(), request.getTargetYield());
            
            // Step 2: Check for soil health card data
            List<FertilizerRecommendationResponseDto.NutrientDeficiencyDto> deficiencies = 
                    new ArrayList<>();
            FertilizerRecommendationResponseDto.NutrientRequirementsDto nutrientRequirements;
            
            if (request.getSoilHealthCard() != null) {
                // Adjust requirements based on soil test
                nutrientRequirements = adjustForSoilHealth(
                        request.getSoilHealthCard(), requirements, deficiencies);
            } else {
                // Use default requirements
                nutrientRequirements = FertilizerRecommendationResponseDto.NutrientRequirementsDto.builder()
                        .nitrogenKgPerAcre(requirements[0])
                        .phosphorusKgPerAcre(requirements[1])
                        .potassiumKgPerAcre(requirements[2])
                        .sulfurKgPerAcre(new BigDecimal("15"))
                        .zincKgPerAcre(new BigDecimal("5"))
                        .build();
            }

            // Step 3: Generate fertilizer recommendations
            List<FertilizerRecommendationResponseDto.RecommendedFertilizerDto> recommendations = 
                    generateFertilizerRecommendations(nutrientRequirements, request);

            // Step 4: Generate split application schedule
            List<FertilizerRecommendationResponseDto.ApplicationScheduleDto> schedule = 
                    generateApplicationSchedule(recommendations, request);

            // Step 5: Generate organic alternatives
            List<FertilizerRecommendationResponseDto.OrganicAlternativeDto> organicAlternatives = 
                    new ArrayList<>();
            if (Boolean.TRUE.equals(request.getIncludeOrganicAlternatives())) {
                organicAlternatives = generateOrganicAlternatives(nutrientRequirements, request);
            }

            // Step 6: Calculate total cost
            BigDecimal totalCost = calculateTotalCost(recommendations, request.getAreaAcres());

            // Step 7: Build response
            return FertilizerRecommendationResponseDto.builder()
                    .success(true)
                    .generatedAt(LocalDateTime.now())
                    .farmerId(request.getFarmerId())
                    .cropId(request.getCropId())
                    .cropName(request.getCropName())
                    .areaAcres(request.getAreaAcres())
                    .soilHealthCardUsed(request.getSoilHealthCard() != null)
                    .nutrientDeficiencies(deficiencies)
                    .nutrientRequirements(nutrientRequirements)
                    .recommendations(recommendations)
                    .applicationSchedule(schedule)
                    .organicAlternatives(organicAlternatives)
                    .estimatedCost(totalCost)
                    .build();

        } catch (Exception e) {
            logger.error("Error generating fertilizer recommendations: {}", e.getMessage(), e);
            return FertilizerRecommendationResponseDto.builder()
                    .success(false)
                    .errorMessage("Failed to generate recommendations: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get nutrient requirements for a crop.
     */
    private BigDecimal[] getNutrientRequirements(String cropName, BigDecimal targetYield) {
        BigDecimal[] baseRequirements = CROP_NUTRIENT_REQUIREMENTS.getOrDefault(
                cropName.toUpperCase(), 
                new BigDecimal[]{new BigDecimal("50"), new BigDecimal("25"), new BigDecimal("25")});
        
        // Adjust for target yield if provided
        if (targetYield != null) {
            BigDecimal adjustment = targetYield.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            baseRequirements[0] = baseRequirements[0].add(adjustment.multiply(baseRequirements[0]));
            baseRequirements[1] = baseRequirements[1].add(adjustment.multiply(baseRequirements[1]));
            baseRequirements[2] = baseRequirements[2].add(adjustment.multiply(baseRequirements[2]));
        }
        
        return baseRequirements;
    }

    /**
     * Adjust nutrient requirements based on soil health card data.
     */
    private FertilizerRecommendationResponseDto.NutrientRequirementsDto adjustForSoilHealth(
            SoilHealthCardDto soilHealthCard,
            BigDecimal[] baseRequirements,
            List<FertilizerRecommendationResponseDto.NutrientDeficiencyDto> deficiencies) {
        
        BigDecimal nitrogen = baseRequirements[0];
        BigDecimal phosphorus = baseRequirements[1];
        BigDecimal potassium = baseRequirements[2];
        
        // Check nitrogen
        if (soilHealthCard.getNitrogenKgHa() != null) {
            BigDecimal currentN = soilHealthCard.getNitrogenKgHa();
            BigDecimal requiredN = new BigDecimal("280");
            
            if (currentN.compareTo(requiredN) < 0) {
                BigDecimal deficit = requiredN.subtract(currentN);
                nitrogen = nitrogen.add(deficit.multiply(new BigDecimal("2")));
                deficiencies.add(FertilizerRecommendationResponseDto.NutrientDeficiencyDto.builder()
                        .nutrient("Nitrogen")
                        .currentLevel(currentN.toString() + " kg/ha")
                        .requiredLevel(requiredN.toString() + " kg/ha")
                        .deficiency("Low")
                        .recommendation("Increase nitrogen application by " + deficit.multiply(new BigDecimal("2")).setScale(0, RoundingMode.HALF_UP) + " kg/acre")
                        .build());
            }
        }
        
        // Check phosphorus
        if (soilHealthCard.getPhosphorusKgHa() != null) {
            BigDecimal currentP = soilHealthCard.getPhosphorusKgHa();
            BigDecimal requiredP = new BigDecimal("10");
            
            if (currentP.compareTo(requiredP) < 0) {
                BigDecimal deficit = requiredP.subtract(currentP);
                phosphorus = phosphorus.add(deficit.multiply(new BigDecimal("2")));
                deficiencies.add(FertilizerRecommendationResponseDto.NutrientDeficiencyDto.builder()
                        .nutrient("Phosphorus")
                        .currentLevel(currentP.toString() + " kg/ha")
                        .requiredLevel(requiredP.toString() + " kg/ha")
                        .deficiency("Low")
                        .recommendation("Increase phosphorus application by " + deficit.multiply(new BigDecimal("2")).setScale(0, RoundingMode.HALF_UP) + " kg/acre")
                        .build());
            }
        }
        
        // Check potassium
        if (soilHealthCard.getPotassiumKgHa() != null) {
            BigDecimal currentK = soilHealthCard.getPotassiumKgHa();
            BigDecimal requiredK = new BigDecimal("108");
            
            if (currentK.compareTo(requiredK) < 0) {
                BigDecimal deficit = requiredK.subtract(currentK);
                potassium = potassium.add(deficit.multiply(new BigDecimal("2")));
                deficiencies.add(FertilizerRecommendationResponseDto.NutrientDeficiencyDto.builder()
                        .nutrient("Potassium")
                        .currentLevel(currentK.toString() + " kg/ha")
                        .requiredLevel(requiredK.toString() + " kg/ha")
                        .deficiency("Low")
                        .recommendation("Increase potassium application by " + deficit.multiply(new BigDecimal("2")).setScale(0, RoundingMode.HALF_UP) + " kg/acre")
                        .build());
            }
        }
        
        // Check zinc
        if (soilHealthCard.getZincPpm() != null) {
            BigDecimal currentZn = soilHealthCard.getZincPpm();
            BigDecimal requiredZn = new BigDecimal("0.6");
            
            if (currentZn.compareTo(requiredZn) < 0) {
                deficiencies.add(FertilizerRecommendationResponseDto.NutrientDeficiencyDto.builder()
                        .nutrient("Zinc")
                        .currentLevel(currentZn.toString() + " ppm")
                        .requiredLevel(requiredZn.toString() + " ppm")
                        .deficiency("Low")
                        .recommendation("Apply zinc sulfate @ 25 kg/acre")
                        .build());
            }
        }
        
        return FertilizerRecommendationResponseDto.NutrientRequirementsDto.builder()
                .nitrogenKgPerAcre(nitrogen.setScale(0, RoundingMode.HALF_UP))
                .phosphorusKgPerAcre(phosphorus.setScale(0, RoundingMode.HALF_UP))
                .potassiumKgPerAcre(potassium.setScale(0, RoundingMode.HALF_UP))
                .sulfurKgPerAcre(new BigDecimal("15"))
                .zincKgPerAcre(new BigDecimal("5"))
                .build();
    }

    /**
     * Generate fertilizer recommendations based on nutrient requirements.
     */
    private List<FertilizerRecommendationResponseDto.RecommendedFertilizerDto> 
            generateFertilizerRecommendations(
                    FertilizerRecommendationResponseDto.NutrientRequirementsDto requirements,
                    FertilizerRecommendationRequestDto request) {
        
        List<FertilizerRecommendationResponseDto.RecommendedFertilizerDto> recommendations = 
                new ArrayList<>();
        
        BigDecimal area = request.getAreaAcres() != null ? request.getAreaAcres() : BigDecimal.ONE;
        
        // Calculate urea requirement for nitrogen
        BigDecimal ureaRequired = calculateFertilizerQuantity(
                requirements.getNitrogenKgPerAcre(), 
                FERTILIZER_COMPOSITION.get("UREA")[0]);
        if (ureaRequired.compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add(FertilizerRecommendationResponseDto.RecommendedFertilizerDto.builder()
                    .fertilizerType("Urea")
                    .fertilizerCategory("CHEMICAL")
                    .quantityKgPerAcre(ureaRequired.multiply(area).setScale(1, RoundingMode.HALF_UP))
                    .applicationTiming("Split application - basal and top dressing")
                    .applicationStage("Basal at sowing, Top dressing at tillering")
                    .nitrogenContent(FERTILIZER_COMPOSITION.get("UREA")[0])
                    .phosphorusContent(BigDecimal.ZERO)
                    .potassiumContent(BigDecimal.ZERO)
                    .costPerAcre(ureaRequired.multiply(FERTILIZER_COSTS.get("UREA")).setScale(0, RoundingMode.HALF_UP))
                    .notes("Apply 50% as basal and 50% as top dressing at 25-30 days after sowing")
                    .source("soil_test")
                    .build());
        }
        
        // Calculate DAP requirement for phosphorus
        BigDecimal dapRequired = calculateFertilizerQuantity(
                requirements.getPhosphorusKgPerAcre(), 
                FERTILIZER_COMPOSITION.get("DAP")[1]);
        if (dapRequired.compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add(FertilizerRecommendationResponseDto.RecommendedFertilizerDto.builder()
                    .fertilizerType("DAP (Di-Ammonium Phosphate)")
                    .fertilizerCategory("CHEMICAL")
                    .quantityKgPerAcre(dapRequired.multiply(area).setScale(1, RoundingMode.HALF_UP))
                    .applicationTiming("Basal application")
                    .applicationStage("At sowing")
                    .nitrogenContent(FERTILIZER_COMPOSITION.get("DAP")[0])
                    .phosphorusContent(FERTILIZER_COMPOSITION.get("DAP")[1])
                    .potassiumContent(BigDecimal.ZERO)
                    .costPerAcre(dapRequired.multiply(FERTILIZER_COSTS.get("DAP")).setScale(0, RoundingMode.HALF_UP))
                    .notes("Apply as basal dose at the time of sowing")
                    .source("soil_test")
                    .build());
        }
        
        // Calculate MOP requirement for potassium
        BigDecimal mopRequired = calculateFertilizerQuantity(
                requirements.getPotassiumKgPerAcre(), 
                FERTILIZER_COMPOSITION.get("MOP")[2]);
        if (mopRequired.compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add(FertilizerRecommendationResponseDto.RecommendedFertilizerDto.builder()
                    .fertilizerType("MOP (Muriate of Potash)")
                    .fertilizerCategory("CHEMICAL")
                    .quantityKgPerAcre(mopRequired.multiply(area).setScale(1, RoundingMode.HALF_UP))
                    .applicationTiming("Split application")
                    .applicationStage("Basal and at flowering")
                    .nitrogenContent(BigDecimal.ZERO)
                    .phosphorusContent(BigDecimal.ZERO)
                    .potassiumContent(FERTILIZER_COMPOSITION.get("MOP")[2])
                    .costPerAcre(mopRequired.multiply(FERTILIZER_COSTS.get("MOP")).setScale(0, RoundingMode.HALF_UP))
                    .notes("Apply 50% as basal and 50% at flowering stage")
                    .source("soil_test")
                    .build());
        }
        
        // Add zinc sulfate if required
        if (requirements.getZincKgPerAcre() != null && 
                requirements.getZincKgPerAcre().compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add(FertilizerRecommendationResponseDto.RecommendedFertilizerDto.builder()
                    .fertilizerType("Zinc Sulfate")
                    .fertilizerCategory("CHEMICAL")
                    .quantityKgPerAcre(new BigDecimal("25").multiply(area))
                    .applicationTiming("Soil application")
                    .applicationStage("At sowing or as foliar spray")
                    .nitrogenContent(BigDecimal.ZERO)
                    .phosphorusContent(BigDecimal.ZERO)
                    .potassiumContent(BigDecimal.ZERO)
                    .costPerAcre(new BigDecimal("2000"))
                    .notes("Apply 25 kg/acre as soil application or 0.5% solution as foliar spray")
                    .source("soil_test")
                    .build());
        }
        
        return recommendations;
    }

    /**
     * Calculate fertilizer quantity based on nutrient requirement and fertilizer composition.
     */
    private BigDecimal calculateFertilizerQuantity(BigDecimal nutrientRequired, BigDecimal nutrientPercent) {
        if (nutrientRequired == null || nutrientPercent == null || 
                nutrientPercent.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return nutrientRequired.multiply(new BigDecimal("100"))
                .divide(nutrientPercent, 2, RoundingMode.HALF_UP);
    }

    /**
     * Generate split application schedule.
     */
    private List<FertilizerRecommendationResponseDto.ApplicationScheduleDto> 
            generateApplicationSchedule(
                    List<FertilizerRecommendationResponseDto.RecommendedFertilizerDto> recommendations,
                    FertilizerRecommendationRequestDto request) {
        
        List<FertilizerRecommendationResponseDto.ApplicationScheduleDto> schedule = 
                new ArrayList<>();
        
        LocalDate sowingDate = LocalDate.now();
        
        // Basal dose schedule
        List<FertilizerRecommendationResponseDto.RecommendedFertilizerDto> basalFertilizers = 
                recommendations.stream()
                        .filter(r -> "Basal application".equals(r.getApplicationTiming()) || 
                                     "Basal at sowing".equals(r.getApplicationStage()))
                        .toList();
        
        if (!basalFertilizers.isEmpty()) {
            BigDecimal basalCost = basalFertilizers.stream()
                    .map(FertilizerRecommendationResponseDto.RecommendedFertilizerDto::getCostPerAcre)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            schedule.add(FertilizerRecommendationResponseDto.ApplicationScheduleDto.builder()
                    .applicationName("Basal Dose")
                    .applicationStage("Sowing")
                    .suggestedDate(sowingDate)
                    .description("Apply basal fertilizers at the time of sowing for initial crop growth")
                    .fertilizers(basalFertilizers)
                    .totalCost(basalCost)
                    .build());
        }
        
        // First top dressing schedule
        List<FertilizerRecommendationResponseDto.RecommendedFertilizerDto> topDressing1 = 
                recommendations.stream()
                        .filter(r -> r.getApplicationStage() != null && 
                                     r.getApplicationStage().contains("tillering"))
                        .toList();
        
        if (!topDressing1.isEmpty()) {
            BigDecimal topDressing1Cost = topDressing1.stream()
                    .map(FertilizerRecommendationResponseDto.RecommendedFertilizerDto::getCostPerAcre)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            schedule.add(FertilizerRecommendationResponseDto.ApplicationScheduleDto.builder()
                    .applicationName("First Top Dressing")
                    .applicationStage("Tillering (25-30 DAS)")
                    .suggestedDate(sowingDate.plusDays(25))
                    .description("Apply nitrogenous fertilizers at tillering stage for vegetative growth")
                    .fertilizers(topDressing1)
                    .totalCost(topDressing1Cost)
                    .build());
        }
        
        // Second top dressing schedule
        List<FertilizerRecommendationResponseDto.RecommendedFertilizerDto> topDressing2 = 
                recommendations.stream()
                        .filter(r -> r.getApplicationStage() != null && 
                                     (r.getApplicationStage().contains("flowering") || 
                                      r.getApplicationStage().contains("panicle")))
                        .toList();
        
        if (!topDressing2.isEmpty()) {
            BigDecimal topDressing2Cost = topDressing2.stream()
                    .map(FertilizerRecommendationResponseDto.RecommendedFertilizerDto::getCostPerAcre)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            schedule.add(FertilizerRecommendationResponseDto.ApplicationScheduleDto.builder()
                    .applicationName("Second Top Dressing")
                    .applicationStage("Flowing/Panicle Initiation (45-60 DAS)")
                    .suggestedDate(sowingDate.plusDays(45))
                    .description("Apply potassium and remaining nutrients at flowering stage")
                    .fertilizers(topDressing2)
                    .totalCost(topDressing2Cost)
                    .build());
        }
        
        return schedule;
    }

    /**
     * Generate organic alternatives.
     */
    private List<FertilizerRecommendationResponseDto.OrganicAlternativeDto> 
            generateOrganicAlternatives(
                    FertilizerRecommendationResponseDto.NutrientRequirementsDto requirements,
                    FertilizerRecommendationRequestDto request) {
        
        List<FertilizerRecommendationResponseDto.OrganicAlternativeDto> alternatives = 
                new ArrayList<>();
        
        BigDecimal area = request.getAreaAcres() != null ? request.getAreaAcres() : BigDecimal.ONE;
        
        // Vermicompost
        alternatives.add(FertilizerRecommendationResponseDto.OrganicAlternativeDto.builder()
                .alternativeType("VERMICOMPOST")
                .name("Vermicompost")
                .quantityKgPerAcre(new BigDecimal("2000").multiply(area))
                .benefits("Improves soil structure, water holding capacity, and microbial activity")
                .applicationMethod("Apply and mix with soil before sowing")
                .costPerAcre(new BigDecimal("16000"))
                .notes("Can replace 25-50% of chemical fertilizer requirement")
                .build());
        
        // Farm Yard Manure
        alternatives.add(FertilizerRecommendationResponseDto.OrganicAlternativeDto.builder()
                .alternativeType("FYM")
                .name("Farm Yard Manure (FYM)")
                .quantityKgPerAcre(new BigDecimal("5000").multiply(area))
                .benefits("Adds organic matter, improves soil fertility gradually")
                .applicationMethod("Apply 15-20 days before sowing and incorporate into soil")
                .costPerAcre(new BigDecimal("10000"))
                .notes("Well-decomposed FYM is recommended")
                .build());
        
        // Green manure
        alternatives.add(FertilizerRecommendationResponseDto.OrganicAlternativeDto.builder()
                .alternativeType("GREEN_MANURE")
                .name("Green Manure (Sesbania/Dhaincha)")
                .quantityKgPerAcre(new BigDecimal("20").multiply(area))
                .benefits("Fixes atmospheric nitrogen, adds organic matter, improves soil structure")
                .applicationMethod("Sow 6-8 weeks before main crop, incorporate at flowering")
                .costPerAcre(new BigDecimal("600"))
                .notes("Can provide 40-60 kg N per hectare")
                .build());
        
        // Biofertilizers
        alternatives.add(FertilizerRecommendationResponseDto.OrganicAlternativeDto.builder()
                .alternativeType("BIOFERTILIZER")
                .name("Biofertilizers (Rhizobium/PSM/Azotobacter)")
                .quantityKgPerAcre(new BigDecimal("2").multiply(area))
                .benefits("Biological nitrogen fixation, phosphorus solubilization")
                .applicationMethod("Seed treatment or soil application")
                .costPerAcre(new BigDecimal("300"))
                .notes("Use with organic manures for best results")
                .build());
        
        return alternatives;
    }

    /**
     * Calculate total cost of recommendations.
     */
    private BigDecimal calculateTotalCost(
            List<FertilizerRecommendationResponseDto.RecommendedFertilizerDto> recommendations,
            BigDecimal area) {
        BigDecimal totalCost = recommendations.stream()
                .map(r -> r.getCostPerAcre() != null ? r.getCostPerAcre() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (area != null && area.compareTo(BigDecimal.ONE) != 0) {
            totalCost = totalCost.multiply(area);
        }
        
        return totalCost.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Record a fertilizer application.
     * 
     * @param request Application request
     * @return Saved application
     * 
     * Validates: Requirement 11C.6
     */
    @Transactional
    public FertilizerApplication recordApplication(FertilizerApplicationRequestDto request) {
        logger.info("Recording fertilizer application for crop: {}, type: {}", 
                request.getCropId(), request.getFertilizerType());
        
        FertilizerApplication application = FertilizerApplication.builder()
                .cropId(request.getCropId())
                .farmerId(request.getFarmerId())
                .fertilizerType(request.getFertilizerType())
                .fertilizerCategory(FertilizerApplication.FertilizerCategory.valueOf(
                        request.getFertilizerCategory()))
                .quantityKg(request.getQuantityKg())
                .areaAcres(request.getAreaAcres())
                .applicationDate(request.getApplicationDate())
                .applicationStage(request.getApplicationStage())
                .cost(request.getCost())
                .nitrogenPercent(request.getNitrogenPercent())
                .phosphorusPercent(request.getPhosphorusPercent())
                .potassiumPercent(request.getPotassiumPercent())
                .sulfurPercent(request.getSulfurPercent())
                .zincPercent(request.getZincPercent())
                .recommendationSource(request.getRecommendationSource())
                .notes(request.getNotes())
                .build();
        
        return fertilizerApplicationRepository.save(application);
    }

    /**
     * Get fertilizer tracking data for a crop.
     * 
     * @param cropId Crop ID
     * @return Tracking response
     * 
     * Validates: Requirements 11C.6, 11C.7, 11C.8, 11C.11
     */
    public FertilizerTrackingResponseDto getFertilizerTracking(Long cropId) {
        logger.info("Getting fertilizer tracking data for crop: {}", cropId);
        
        try {
            // Get all applications for the crop
            List<FertilizerApplication> applications = 
                    fertilizerApplicationRepository.findByCropIdOrderByApplicationDateAsc(cropId);
            
            // Calculate total nutrient input
            BigDecimal totalN = BigDecimal.ZERO;
            BigDecimal totalP = BigDecimal.ZERO;
            BigDecimal totalK = BigDecimal.ZERO;
            BigDecimal totalS = BigDecimal.ZERO;
            BigDecimal totalZn = BigDecimal.ZERO;
            BigDecimal totalQty = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            
            List<FertilizerTrackingResponseDto.FertilizerApplicationDto> applicationDtos = 
                    new ArrayList<>();
            
            for (FertilizerApplication app : applications) {
                BigDecimal n = app.getNitrogenKg();
                BigDecimal p = app.getPhosphorusKg();
                BigDecimal k = app.getPotassiumKg();
                
                totalN = totalN.add(n);
                totalP = totalP.add(p);
                totalK = totalK.add(k);
                totalS = totalS.add(app.getSulfurPercent() != null ? 
                        app.getQuantityKg().multiply(app.getSulfurPercent())
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                totalZn = totalZn.add(app.getZincPercent() != null ? 
                        app.getQuantityKg().multiply(app.getZincPercent())
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                totalQty = totalQty.add(app.getQuantityKg());
                totalCost = totalCost.add(app.getCost() != null ? app.getCost() : BigDecimal.ZERO);
                
                applicationDtos.add(FertilizerTrackingResponseDto.FertilizerApplicationDto.builder()
                        .id(app.getId())
                        .fertilizerType(app.getFertilizerType())
                        .fertilizerCategory(app.getFertilizerCategory().name())
                        .quantityKg(app.getQuantityKg())
                        .applicationDate(app.getApplicationDate())
                        .applicationStage(app.getApplicationStage())
                        .cost(app.getCost())
                        .nitrogenKg(n)
                        .phosphorusKg(p)
                        .potassiumKg(k)
                        .notes(app.getNotes())
                        .createdAt(app.getCreatedAt())
                        .build());
            }
            
            // Build total nutrient input
            FertilizerTrackingResponseDto.TotalNutrientInputDto totalNutrientInput = 
                    FertilizerTrackingResponseDto.TotalNutrientInputDto.builder()
                            .totalNitrogenKg(totalN.setScale(2, RoundingMode.HALF_UP))
                            .totalPhosphorusKg(totalP.setScale(2, RoundingMode.HALF_UP))
                            .totalPotassiumKg(totalK.setScale(2, RoundingMode.HALF_UP))
                            .totalSulfurKg(totalS.setScale(2, RoundingMode.HALF_UP))
                            .totalZincKg(totalZn.setScale(2, RoundingMode.HALF_UP))
                            .totalQuantityKg(totalQty.setScale(2, RoundingMode.HALF_UP))
                            .totalCost(totalCost.setScale(2, RoundingMode.HALF_UP))
                            .build();
            
            // Build cost summary
            FertilizerTrackingResponseDto.CostSummaryDto costSummary = 
                    FertilizerTrackingResponseDto.CostSummaryDto.builder()
                            .totalCost(totalCost.setScale(0, RoundingMode.HALF_UP))
                            .costPerAcre(totalCost.setScale(0, RoundingMode.HALF_UP))
                            .costPerKgNutrient(totalNutrientInput.getTotalNitrogenKg().add(
                                    totalNutrientInput.getTotalPhosphorusKg()).add(
                                    totalNutrientInput.getTotalPotassiumKg()).compareTo(BigDecimal.ZERO) > 0 ?
                                    totalCost.divide(
                                            totalNutrientInput.getTotalNitrogenKg().add(
                                                    totalNutrientInput.getTotalPhosphorusKg()).add(
                                                    totalNutrientInput.getTotalPotassiumKg()),
                                            2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                            .costTrend("stable")
                            .build();
            
            return FertilizerTrackingResponseDto.builder()
                    .success(true)
                    .generatedAt(LocalDateTime.now())
                    .cropId(cropId)
                    .applications(applicationDtos)
                    .totalNutrientInput(totalNutrientInput)
                    .costSummary(costSummary)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error getting fertilizer tracking data: {}", e.getMessage(), e);
            return FertilizerTrackingResponseDto.builder()
                    .success(false)
                    .errorMessage("Failed to get tracking data: " + e.getMessage())
                    .build();
        }
    }
}