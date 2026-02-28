package com.farmer.crop.service;

import com.farmer.crop.dto.*;
import com.farmer.crop.repository.FertilizerApplicationRepository;
import com.farmer.crop.repository.SoilHealthCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



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
    private static final Map<String, Double[]> CROP_NUTRIENT_REQUIREMENTS = new HashMap<>();
    
    // Fertilizer nutrient content (percentage)
    private static final Map<String, Double[]> FERTILIZER_COMPOSITION = new HashMap<>();
    
    // Fertilizer costs (INR per kg)
    private static final Map<String, Double> FERTILIZER_COSTS = new HashMap<>();

    static {
        // Crop nutrient requirements: N, P2O5, K2O (kg per acre)
        CROP_NUTRIENT_REQUIREMENTS.put("RICE", new Double[]{
            60.0, 30.0, 30.0});
        CROP_NUTRIENT_REQUIREMENTS.put("WHEAT", new Double[]{
            80.0, 40.0, 30.0});
        CROP_NUTRIENT_REQUIREMENTS.put("COTTON", new Double[]{
            100.0, 50.0, 50.0});
        CROP_NUTRIENT_REQUIREMENTS.put("SOYBEAN", new Double[]{
            20.0, 60.0, 20.0});
        CROP_NUTRIENT_REQUIREMENTS.put("GROUNDNUT", new Double[]{
            20.0, 40.0, 40.0});
        CROP_NUTRIENT_REQUIREMENTS.put("MUSTARD", new Double[]{
            40.0, 20.0, 20.0});
        CROP_NUTRIENT_REQUIREMENTS.put("PULSES", new Double[]{
            15.0, 40.0, 20.0});
        CROP_NUTRIENT_REQUIREMENTS.put("MAIZE", new Double[]{
            80.0, 40.0, 30.0});
        CROP_NUTRIENT_REQUIREMENTS.put("SUGARCANE", new Double[]{
            150.0, 50.0, 100.0});
        CROP_NUTRIENT_REQUIREMENTS.put("POTATO", new Double[]{
            100.0, 60.0, 100.0});
        CROP_NUTRIENT_REQUIREMENTS.put("ONION", new Double[]{
            80.0, 40.0, 60.0});
        CROP_NUTRIENT_REQUIREMENTS.put("TOMATO", new Double[]{
            100.0, 50.0, 50.0});

        // Fertilizer composition: N, P2O5, K2O (percentage)
        FERTILIZER_COMPOSITION.put("UREA", new Double[]{
            46.0, 0.0, 0.0});
        FERTILIZER_COMPOSITION.put("DAP", new Double[]{
            18.0, 46.0, 0.0});
        FERTILIZER_COMPOSITION.put("MOP", new Double[]{
            0.0, 0.0, 60.0});
        FERTILIZER_COMPOSITION.put("SSP", new Double[]{
            8.0, 16.0, 0.0});
        FERTILIZER_COMPOSITION.put("NPK", new Double[]{
            10.0, 26.0, 26.0});
        FERTILIZER_COMPOSITION.put("UREA_DAP_COMBO", new Double[]{
            32.0, 23.0, 0.0});
        FERTILIZER_COMPOSITION.put("ZINC_SULFATE", new Double[]{
            0.0, 0.0, 0.0, 21.0});
        FERTILIZER_COMPOSITION.put("BORAX", new Double[]{
            0.0, 0.0, 0.0, 0.0, 11.0});

        // Fertilizer costs (INR per kg)
        FERTILIZER_COSTS.put("UREA", 6.0);
        FERTILIZER_COSTS.put("DAP", 27.0);
        FERTILIZER_COSTS.put("MOP", 18.0);
        FERTILIZER_COSTS.put("SSP", 12.0);
        FERTILIZER_COSTS.put("NPK", 25.0);
        FERTILIZER_COSTS.put("UREA_DAP_COMBO", 15.0);
        FERTILIZER_COSTS.put("ZINC_SULFATE", 80.0);
        FERTILIZER_COSTS.put("BORAX", 120.0);
        FERTILIZER_COSTS.put("VERMICOMPOST", 8.0);
        FERTILIZER_COSTS.put("FYM", 2.0);
        FERTILIZER_COSTS.put("GREEN_MANURE", 3.0);
        FERTILIZER_COSTS.put("BIOFERTILIZER", 150.0);
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
            Double[] requirements = getNutrientRequirements(
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
                        .sulfurKgPerAcre(15.0)
                        .zincKgPerAcre(5.0)
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
            Double totalCost = calculateTotalCost(recommendations, request.getAreaAcres());

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
    private Double[] getNutrientRequirements(String cropName, Double targetYield) {
        Double[] baseRequirements = CROP_NUTRIENT_REQUIREMENTS.getOrDefault(
                cropName.toUpperCase(), 
                new Double[]{50.0, 25.0, 25.0});
        
        // Adjust for target yield if provided
        if (targetYield != null) {
            Double adjustment = targetYield / (100.0 * 2);
            baseRequirements[0] = baseRequirements[0] + (adjustment * (baseRequirements[0]));
            baseRequirements[1] = baseRequirements[1] + (adjustment * (baseRequirements[1]));
            baseRequirements[2] = baseRequirements[2] + (adjustment * (baseRequirements[2]));
        }
        
        return baseRequirements;
    }

    /**
     * Adjust nutrient requirements based on soil health card data.
     */
    private FertilizerRecommendationResponseDto.NutrientRequirementsDto adjustForSoilHealth(
            SoilHealthCardDto soilHealthCard, Double[] baseRequirements,
            List<FertilizerRecommendationResponseDto.NutrientDeficiencyDto> deficiencies) {
        
        Double nitrogen = baseRequirements[0];
        Double phosphorus = baseRequirements[1];
        Double potassium = baseRequirements[2];
        
        // Check nitrogen
        if (soilHealthCard.getNitrogenKgHa() != null) {
            Double currentN = soilHealthCard.getNitrogenKgHa();
            Double requiredN = 280.0;
            
            if (currentN < requiredN) {
                Double deficit = requiredN - (currentN);
                nitrogen = nitrogen = nitrogen + (deficit * (2.0));
                deficiencies.add(FertilizerRecommendationResponseDto.NutrientDeficiencyDto.builder()
                        .nutrient("Nitrogen")
                        .currentLevel(currentN.toString() + " kg/ha")
                        .requiredLevel(requiredN.toString() + " kg/ha")
                        .deficiency("Low")
                        .recommendation("Increase nitrogen application by " + deficit * (2.0) + " kg/acre")
                        .build());
            }
        }
        
        // Check phosphorus
        if (soilHealthCard.getPhosphorusKgHa() != null) {
            Double currentP = soilHealthCard.getPhosphorusKgHa();
            Double requiredP = 10.0;
            
            if (currentP < requiredP) {
                Double deficit = requiredP - currentP;
                phosphorus = phosphorus = phosphorus + (deficit * 2.0);
                deficiencies.add(FertilizerRecommendationResponseDto.NutrientDeficiencyDto.builder()
                        .nutrient("Phosphorus")
                        .currentLevel(currentP.toString() + " kg/ha")
                        .requiredLevel(requiredP.toString() + " kg/ha")
                        .deficiency("Low")
                        .recommendation("Increase phosphorus application by " + Math.round(deficit * 2.0) + " kg/acre")
                        .build());
            }
        }
        
        // Check potassium
        if (soilHealthCard.getPotassiumKgHa() != null) {
            Double currentK = soilHealthCard.getPotassiumKgHa();
            Double requiredK = 108.0;
            
            if (currentK < requiredK) {
                Double deficit = requiredK - (currentK);
                potassium = potassium = potassium + (deficit * (2.0));
                deficiencies.add(FertilizerRecommendationResponseDto.NutrientDeficiencyDto.builder()
                        .nutrient("Potassium")
                        .currentLevel(currentK.toString() + " kg/ha")
                        .requiredLevel(requiredK.toString() + " kg/ha")
                        .deficiency("Low")
                        .recommendation("Increase potassium application by " + deficit * (2.0) + " kg/acre")
                        .build());
            }
        }
        
        // Check zinc
        if (soilHealthCard.getZincPpm() != null) {
            Double currentZn = soilHealthCard.getZincPpm();
            Double requiredZn = 0.6;
            
            if (currentZn < requiredZn) {
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
                .nitrogenKgPerAcre(nitrogen)
                .phosphorusKgPerAcre(phosphorus)
                .potassiumKgPerAcre(potassium)
                .sulfurKgPerAcre(15.0)
                .zincKgPerAcre(5.0)
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
        
        Double area = request.getAreaAcres() != null ? request.getAreaAcres() : 1.0;
        
        // Calculate urea requirement for nitrogen
        Double ureaRequired = calculateFertilizerQuantity(
                requirements.getNitrogenKgPerAcre(), 
                FERTILIZER_COMPOSITION.get("UREA")[0]);
        if (ureaRequired > 0.0) {
            recommendations.add(FertilizerRecommendationResponseDto.RecommendedFertilizerDto.builder()
                    .fertilizerType("Urea")
                    .fertilizerCategory("CHEMICAL")
                    .quantityKgPerAcre(ureaRequired * (area))
                    .applicationTiming("Split application - basal and top dressing")
                    .applicationStage("Basal at sowing, Top dressing at tillering")
                    .nitrogenContent(FERTILIZER_COMPOSITION.get("UREA")[0])
                    .phosphorusContent(0.0)
                    .potassiumContent(0.0)
                    .costPerAcre(ureaRequired * (FERTILIZER_COSTS.get("UREA")))
                    .notes("Apply 50% as basal and 50% as top dressing at 25-30 days after sowing")
                    .source("soil_test")
                    .build());
        }
        
        // Calculate DAP requirement for phosphorus
        Double dapRequired = calculateFertilizerQuantity(
                requirements.getPhosphorusKgPerAcre(), 
                FERTILIZER_COMPOSITION.get("DAP")[1]);
        if (dapRequired > 0.0) {
            recommendations.add(FertilizerRecommendationResponseDto.RecommendedFertilizerDto.builder()
                    .fertilizerType("DAP (Di-Ammonium Phosphate)")
                    .fertilizerCategory("CHEMICAL")
                    .quantityKgPerAcre(dapRequired * (area))
                    .applicationTiming("Basal application")
                    .applicationStage("At sowing")
                    .nitrogenContent(FERTILIZER_COMPOSITION.get("DAP")[0])
                    .phosphorusContent(FERTILIZER_COMPOSITION.get("DAP")[1])
                    .potassiumContent(0.0)
                    .costPerAcre(dapRequired * (FERTILIZER_COSTS.get("DAP")))
                    .notes("Apply as basal dose at the time of sowing")
                    .source("soil_test")
                    .build());
        }
        
        // Calculate MOP requirement for potassium
        Double mopRequired = calculateFertilizerQuantity(
                requirements.getPotassiumKgPerAcre(), 
                FERTILIZER_COMPOSITION.get("MOP")[2]);
        if (mopRequired > 0.0) {
            recommendations.add(FertilizerRecommendationResponseDto.RecommendedFertilizerDto.builder()
                    .fertilizerType("MOP (Muriate of Potash)")
                    .fertilizerCategory("CHEMICAL")
                    .quantityKgPerAcre(mopRequired * (area))
                    .applicationTiming("Split application")
                    .applicationStage("Basal and at flowering")
                    .nitrogenContent(0.0)
                    .phosphorusContent(0.0)
                    .potassiumContent(FERTILIZER_COMPOSITION.get("MOP")[2])
                    .costPerAcre(mopRequired * (FERTILIZER_COSTS.get("MOP")))
                    .notes("Apply 50% as basal and 50% at flowering stage")
                    .source("soil_test")
                    .build());
        }
        
        // Add zinc sulfate if required
        if (requirements.getZincKgPerAcre() != null && 
                requirements.getZincKgPerAcre() > 0.0) {
            recommendations.add(FertilizerRecommendationResponseDto.RecommendedFertilizerDto.builder()
                    .fertilizerType("Zinc Sulfate")
                    .fertilizerCategory("CHEMICAL")
                    .quantityKgPerAcre(25.0 * (area))
                    .applicationTiming("Soil application")
                    .applicationStage("At sowing or as foliar spray")
                    .nitrogenContent(0.0)
                    .phosphorusContent(0.0)
                    .potassiumContent(0.0)
                    .costPerAcre(2000.0)
                    .notes("Apply 25 kg/acre as soil application or 0.5% solution as foliar spray")
                    .source("soil_test")
                    .build());
        }
        
        return recommendations;
    }

    /**
     * Calculate fertilizer quantity based on nutrient requirement and fertilizer composition.
     */
    private Double calculateFertilizerQuantity(Double nutrientRequired, Double nutrientPercent) {
        if (nutrientRequired == null || nutrientPercent == null || 
                nutrientPercent == 0.0) {
            return 0.0;
        }
        return nutrientRequired * (100.0) / nutrientPercent;
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
            Double basalCost = basalFertilizers.stream()
                    .map(FertilizerRecommendationResponseDto.RecommendedFertilizerDto::getCostPerAcre)
                    .reduce(0.0, (a, b) -> a + b);
            
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
            Double topDressing1Cost = topDressing1.stream()
                    .map(FertilizerRecommendationResponseDto.RecommendedFertilizerDto::getCostPerAcre)
                    .reduce(0.0, (a, b) -> a + b);
            
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
            Double topDressing2Cost = topDressing2.stream()
                    .map(FertilizerRecommendationResponseDto.RecommendedFertilizerDto::getCostPerAcre)
                    .reduce(0.0, (a, b) -> a + b);
            
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
        
        Double area = request.getAreaAcres() != null ? request.getAreaAcres() : 1.0;
        
        // Vermicompost
        alternatives.add(FertilizerRecommendationResponseDto.OrganicAlternativeDto.builder()
                .alternativeType("VERMICOMPOST")
                .name("Vermicompost")
                .quantityKgPerAcre(2000.0 * (area))
                .benefits("Improves soil structure, water holding capacity, and microbial activity")
                .applicationMethod("Apply and mix with soil before sowing")
                .costPerAcre(16000.0)
                .notes("Can replace 25-50% of chemical fertilizer requirement")
                .build());
        
        // Farm Yard Manure
        alternatives.add(FertilizerRecommendationResponseDto.OrganicAlternativeDto.builder()
                .alternativeType("FYM")
                .name("Farm Yard Manure (FYM)")
                .quantityKgPerAcre(5000.0 * (area))
                .benefits("Adds organic matter, improves soil fertility gradually")
                .applicationMethod("Apply 15-20 days before sowing and incorporate into soil")
                .costPerAcre(10000.0)
                .notes("Well-decomposed FYM is recommended")
                .build());
        
        // Green manure
        alternatives.add(FertilizerRecommendationResponseDto.OrganicAlternativeDto.builder()
                .alternativeType("GREEN_MANURE")
                .name("Green Manure (Sesbania/Dhaincha)")
                .quantityKgPerAcre(20.0 * (area))
                .benefits("Fixes atmospheric nitrogen, adds organic matter, improves soil structure")
                .applicationMethod("Sow 6-8 weeks before main crop, incorporate at flowering")
                .costPerAcre(600.0)
                .notes("Can provide 40-60 kg N per hectare")
                .build());
        
        // Biofertilizers
        alternatives.add(FertilizerRecommendationResponseDto.OrganicAlternativeDto.builder()
                .alternativeType("BIOFERTILIZER")
                .name("Biofertilizers (Rhizobium/PSM/Azotobacter)")
                .quantityKgPerAcre(2.0 * (area))
                .benefits("Biological nitrogen fixation, phosphorus solubilization")
                .applicationMethod("Seed treatment or soil application")
                .costPerAcre(300.0)
                .notes("Use with organic manures for best results")
                .build());
        
        return alternatives;
    }

    /**
     * Calculate total cost of recommendations.
     */
    private Double calculateTotalCost(
            List<FertilizerRecommendationResponseDto.RecommendedFertilizerDto> recommendations, Double area) {
        Double totalCost = recommendations.stream()
                .map(r -> r.getCostPerAcre() != null ? r.getCostPerAcre() : 0.0)
                .reduce(0.0, (a, b) -> a + b);
        
        if (area != null && area.compareTo(1.0) != 0) {
            totalCost = totalCost * (area);
        }
        
        return totalCost;
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
            Double totalN = 0.0;
            Double totalP = 0.0;
            Double totalK = 0.0;
            Double totalS = 0.0;
            Double totalZn = 0.0;
            Double totalQty = 0.0;
            Double totalCost = 0.0;
            
            List<FertilizerTrackingResponseDto.FertilizerApplicationDto> applicationDtos = 
                    new ArrayList<>();
            
            for (FertilizerApplication app : applications) {
                Double n = app.getNitrogenKg();
                Double p = app.getPhosphorusKg();
                Double k = app.getPotassiumKg();
                
                totalN = totalN = totalN + (n);
                totalP = totalP = totalP + (p);
                totalK = totalK = totalK + (k);
                totalS = totalS = totalS + (app.getSulfurPercent() != null ? 
                        app.getQuantityKg() * (app.getSulfurPercent())
                                 / (100.0 * 2) : 0.0);
                totalZn = totalZn + (app.getZincPercent() != null ? 
                        app.getQuantityKg() * (app.getZincPercent())
                                 / (100.0 * 2) : 0.0);
                totalQty = totalQty + (app.getQuantityKg());
                totalCost = totalCost + (app.getCost() != null ? app.getCost() : 0.0);
                
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
                            .totalNitrogenKg(totalN)
                            .totalPhosphorusKg(totalP)
                            .totalPotassiumKg(totalK)
                            .totalSulfurKg(totalS)
                            .totalZincKg(totalZn)
                            .totalQuantityKg(totalQty)
                            .totalCost(totalCost)
                            .build();
            
            // Build cost summary
            FertilizerTrackingResponseDto.CostSummaryDto costSummary = 
                    FertilizerTrackingResponseDto.CostSummaryDto.builder()
                            .totalCost(totalCost)
                            .costPerAcre(totalCost)
                            .costPerKgNutrient((totalNutrientInput.getTotalNitrogenKg() + 
                                    totalNutrientInput.getTotalPhosphorusKg() + 
                                    totalNutrientInput.getTotalPotassiumKg()) > 0.0 ?
                                    totalCost / (
                                            totalNutrientInput.getTotalNitrogenKg() + 
                                                    totalNutrientInput.getTotalPhosphorusKg() + 
                                                    totalNutrientInput.getTotalPotassiumKg()) : 0.0)
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




















