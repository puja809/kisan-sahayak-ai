package com.farmer.crop.service;

import com.farmer.crop.dto.CropHistoryAnalysisResultDto;
import com.farmer.crop.dto.CropHistoryEntryDto;
import com.farmer.crop.dto.NutrientDepletionRiskDto;
import com.farmer.crop.enums.CropFamily;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing crop history to identify rotation patterns and nutrient depletion risks.
 * 
 * Requirements: 3.1, 3.2
 */
@Service
public class CropHistoryAnalyzer {

    private static final int MAX_SEASONS_TO_ANALYZE = 3;
    private static final int CONSECUTIVE_THRESHOLD = 2; // 2+ consecutive seasons of same family = risk
    private static final int CRITICAL_CONSECUTIVE = 3; // 3+ consecutive = critical risk

    /**
     * Analyzes crop history for the past seasons to identify rotation patterns and risks.
     * 
     * @param cropHistory List of crop history entries (should be ordered by date, most recent first)
     * @return Analysis result with identified risks and recommendations
     */
    public CropHistoryAnalysisResultDto analyzeCropHistory(List<CropHistoryEntryDto> cropHistory) {
        if (cropHistory == null || cropHistory.isEmpty()) {
            return createEmptyAnalysis();
        }

        // Sort by sowing date, most recent first
        List<CropHistoryEntryDto> sortedHistory = cropHistory.stream()
                .filter(c -> c.getSowingDate() != null)
                .sorted(Comparator.comparing(CropHistoryEntryDto::getSowingDate).reversed())
                .limit(MAX_SEASONS_TO_ANALYZE)
                .collect(Collectors.toList());

        // Add derived fields
        enrichCropHistoryEntries(sortedHistory);

        // Identify nutrient depletion risks
        List<NutrientDepletionRiskDto> risks = identifyNutrientDepletionRisks(sortedHistory);

        // Generate analysis summary
        CropHistoryAnalysisResultDto.AnalysisSummary summary = generateAnalysisSummary(sortedHistory, risks);

        // Generate recommendations
        List<String> recommendations = generateRecommendations(sortedHistory, risks, summary);

        return CropHistoryAnalysisResultDto.builder()
                .hasSufficientHistory(sortedHistory.size() >= 2)
                .seasonsAnalyzed(sortedHistory.size())
                .cropHistory(sortedHistory)
                .nutrientDepletionRisks(risks)
                .summary(summary)
                .recommendations(recommendations)
                .build();
    }

    /**
     * Enriches crop history entries with derived fields like crop family and root depth.
     */
    private void enrichCropHistoryEntries(List<CropHistoryEntryDto> history) {
        int order = 1;
        for (CropHistoryEntryDto entry : history) {
            entry.setCropFamily(CropFamily.getFamilyForCrop(entry.getCropName()));
            entry.setRootDepth(CropFamily.getRootDepthForCrop(entry.getCropName()));
            entry.setSeasonOrder(order++);
        }
    }

    /**
     * Identifies nutrient depletion risks based on consecutive planting of same crop family.
     * 
     * Requirements: 3.2
     */
    private List<NutrientDepletionRiskDto> identifyNutrientDepletionRisks(List<CropHistoryEntryDto> history) {
        List<NutrientDepletionRiskDto> risks = new ArrayList<>();

        if (history.size() < 2) {
            return risks;
        }

        // Group consecutive crops by family
        List<List<CropHistoryEntryDto>> consecutiveGroups = groupConsecutiveByFamily(history);

        for (List<CropHistoryEntryDto> group : consecutiveGroups) {
            if (group.size() >= CONSECUTIVE_THRESHOLD) {
                NutrientDepletionRiskDto risk = createRiskForGroup(group);
                if (risk != null) {
                    risks.add(risk);
                }
            }
        }

        // Also check for overall pattern (even if not strictly consecutive)
        Map<CropFamily, Long> familyCounts = history.stream()
                .map(CropHistoryEntryDto::getCropFamily)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(f -> f, Collectors.counting()));

        for (Map.Entry<CropFamily, Long> entry : familyCounts.entrySet()) {
            if (entry.getValue() >= CONSECUTIVE_THRESHOLD) {
                // Check if this family already has a consecutive risk
                boolean alreadyCovered = risks.stream()
                        .anyMatch(r -> r.getCropFamily().equals(entry.getKey()));
                
                if (!alreadyCovered) {
                    NutrientDepletionRiskDto risk = createOverallFamilyRisk(entry.getKey(), entry.getValue().intValue());
                    if (risk != null) {
                        risks.add(risk);
                    }
                }
            }
        }

        return risks;
    }

    /**
     * Groups consecutive crops that belong to the same family.
     */
    private List<List<CropHistoryEntryDto>> groupConsecutiveByFamily(List<CropHistoryEntryDto> history) {
        List<List<CropHistoryEntryDto>> groups = new ArrayList<>();
        List<CropHistoryEntryDto> currentGroup = new ArrayList<>();
        CropFamily previousFamily = null;

        for (CropHistoryEntryDto crop : history) {
            CropFamily currentFamily = crop.getCropFamily();
            
            if (currentFamily == null) {
                // Skip crops without family classification
                continue;
            }

            if (previousFamily == null) {
                currentGroup.add(crop);
            } else if (currentFamily.equals(previousFamily)) {
                currentGroup.add(crop);
            } else {
                if (currentGroup.size() >= CONSECUTIVE_THRESHOLD) {
                    groups.add(new ArrayList<>(currentGroup));
                }
                currentGroup = new ArrayList<>();
                currentGroup.add(crop);
            }
            previousFamily = currentFamily;
        }

        if (currentGroup.size() >= CONSECUTIVE_THRESHOLD) {
            groups.add(new ArrayList<>(currentGroup));
        }

        return groups;
    }

    /**
     * Creates a nutrient depletion risk for a group of consecutive same-family crops.
     */
    private NutrientDepletionRiskDto createRiskForGroup(List<CropHistoryEntryDto> group) {
        if (group.isEmpty()) {
            return null;
        }

        CropFamily family = group.get(0).getCropFamily();
        int consecutiveCount = group.size();

        NutrientDepletionRiskDto.RiskLevel riskLevel;
        if (consecutiveCount >= CRITICAL_CONSECUTIVE) {
            riskLevel = NutrientDepletionRiskDto.RiskLevel.CRITICAL;
        } else if (consecutiveCount >= CONSECUTIVE_THRESHOLD + 1) {
            riskLevel = NutrientDepletionRiskDto.RiskLevel.HIGH;
        } else {
            riskLevel = NutrientDepletionRiskDto.RiskLevel.MEDIUM;
        }

        String affectedNutrients = getAffectedNutrientsForFamily(family);
        String recommendation = getRecommendationForFamily(family, consecutiveCount);
        List<String> affectedCrops = group.stream()
                .map(CropHistoryEntryDto::getCropName)
                .collect(Collectors.toList());

        Double severityScore = calculateSeverityScore(riskLevel, consecutiveCount);

        return NutrientDepletionRiskDto.builder()
                .cropFamily(family)
                .cropFamilyName(family.getFamilyName())
                .riskLevel(riskLevel)
                .riskDescription(buildRiskDescription(family, consecutiveCount))
                .affectedNutrients(affectedNutrients)
                .consecutiveSeasons(consecutiveCount)
                .affectedCrops(affectedCrops)
                .recommendation(recommendation)
                .severityScore(severityScore)
                .build();
    }

    /**
     * Creates an overall family risk when a family appears frequently but not strictly consecutively.
     */
    private NutrientDepletionRiskDto createOverallFamilyRisk(CropFamily family, int count) {
        NutrientDepletionRiskDto.RiskLevel riskLevel = count >= CRITICAL_CONSECUTIVE 
                ? NutrientDepletionRiskDto.RiskLevel.HIGH 
                : NutrientDepletionRiskDto.RiskLevel.MEDIUM;

        return NutrientDepletionRiskDto.builder()
                .cropFamily(family)
                .cropFamilyName(family.getFamilyName())
                .riskLevel(riskLevel)
                .riskDescription(String.format(
                        "Crop family '%s' has been planted %d times in the analyzed period, indicating potential nutrient depletion risk.",
                        family.getFamilyName(), count))
                .affectedNutrients(getAffectedNutrientsForFamily(family))
                .consecutiveSeasons(count)
                .affectedCrops(family.getCommonCrops().subList(0, Math.min(3, family.getCommonCrops().size())))
                .recommendation(getRecommendationForFamily(family, count))
                .severityScore(calculateSeverityScore(riskLevel, count))
                .build();
    }

    /**
     * Gets the affected nutrients for a given crop family.
     */
    private String getAffectedNutrientsForFamily(CropFamily family) {
        switch (family) {
            case CEREALS:
                return "Nitrogen (N), Zinc (Zn)";
            case LEGUMES:
                return "Phosphorus (P), Potassium (K)"; // Legumes fix N but deplete P and K
            case BRASSICAS:
                return "Potassium (K), Calcium (Ca), Boron (B)";
            case SOLANACEOUS:
                return "Phosphorus (P), Calcium (Ca), Magnesium (Mg)";
            case CUCURBITS:
                return "Nitrogen (N), Potassium (K)";
            case ROOT_TUBERS:
                return "Potassium (K), Phosphorus (P)";
            case FIBER:
                return "Nitrogen (N), Potassium (K)";
            case OILSEEDS:
                return "Sulfur (S), Boron (B)";
            case SPICES:
                return "Various micronutrients depending on crop";
            default:
                return "Nitrogen (N), Phosphorus (P), Potassium (K)";
        }
    }

    /**
     * Gets a recommendation for breaking the monoculture pattern.
     */
    private String getRecommendationForFamily(CropFamily family, int consecutiveCount) {
        StringBuilder recommendation = new StringBuilder();
        
        switch (family) {
            case CEREALS:
                recommendation.append("Consider rotating with legumes (greengram, blackgram, chickpea) for nitrogen fixation. ");
                recommendation.append("Follow with oilseeds (sunflower, sesame) to break pest cycles. ");
                break;
            case LEGUMES:
                recommendation.append("After legumes, plant cereals (wheat, rice) to utilize fixed nitrogen. ");
                recommendation.append("Include brassicas for diverse nutrient uptake. ");
                break;
            case BRASSICAS:
                recommendation.append("Rotate with cereals or root crops to reduce pest pressure. ");
                recommendation.append("Add lime if soil pH has dropped due to brassica cultivation. ");
                break;
            case SOLANACEOUS:
                recommendation.append("Avoid consecutive solanaceous crops to reduce disease buildup. ");
                recommendation.append("Rotate with cereals or legumes. ");
                break;
            case CUCURBITS:
                recommendation.append("Rotate with deep-rooted crops (sunflower, maize) to access deeper nutrients. ");
                recommendation.append("Avoid consecutive cucurbits to prevent soil-borne diseases. ");
                break;
            case ROOT_TUBERS:
                recommendation.append("Follow with cereals to utilize residual potassium. ");
                recommendation.append("Add organic matter to replenish soil after root crop harvest. ");
                break;
            case FIBER:
                recommendation.append("Rotate with legumes to restore nitrogen. ");
                recommendation.append("Include green manure crops before next fiber crop. ");
                break;
            case OILSEEDS:
                recommendation.append("Follow oilseeds with cereals for balanced nutrient utilization. ");
                recommendation.append("Add sulfur-containing fertilizers if needed. ");
                break;
            default:
                recommendation.append("Introduce crops from different families to restore balance. ");
        }

        if (consecutiveCount >= CRITICAL_CONSECUTIVE) {
            recommendation.append("URGENT: Immediate rotation change strongly recommended. ");
        }

        return recommendation.toString();
    }

    /**
     * Builds a description of the risk.
     */
    private String buildRiskDescription(CropFamily family, int consecutiveCount) {
        StringBuilder desc = new StringBuilder();
        desc.append(String.format("Consecutive planting of %s family crops for %d season(s). ", 
                family.getFamilyName(), consecutiveCount));
        desc.append(family.getTypicalRootDepth().getNutrientImpact());
        return desc.toString();
    }

    /**
     * Calculates a severity score (0-100) based on risk level and consecutive count.
     */
    private Double calculateSeverityScore(NutrientDepletionRiskDto.RiskLevel riskLevel, int consecutiveCount) {
        int baseScore;
        switch (riskLevel) {
            case CRITICAL:
                baseScore = 90;
                break;
            case HIGH:
                baseScore = 70;
                break;
            case MEDIUM:
                baseScore = 50;
                break;
            default:
                baseScore = 25;
        }
        
        // Add bonus for each consecutive season beyond threshold
        int bonus = Math.max(0, consecutiveCount - CONSECUTIVE_THRESHOLD) * 5;
        return Double.valueOf(Math.min(100, baseScore + bonus));
    }

    /**
     * Generates an analysis summary from the crop history and identified risks.
     */
    private CropHistoryAnalysisResultDto.AnalysisSummary generateAnalysisSummary(
            List<CropHistoryEntryDto> history, 
            List<NutrientDepletionRiskDto> risks) {
        
        // Find dominant crop family
        Map<CropFamily, Long> familyCounts = history.stream()
                .map(CropHistoryEntryDto::getCropFamily)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(f -> f, Collectors.counting()));

        CropFamily dominantFamily = familyCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // Check for consecutive monoculture
        int maxConsecutive = 0;
        CropFamily consecutiveFamily = null;
        
        List<List<CropHistoryEntryDto>> groups = groupConsecutiveByFamily(history);
        for (List<CropHistoryEntryDto> group : groups) {
            if (group.size() > maxConsecutive) {
                maxConsecutive = group.size();
                consecutiveFamily = group.get(0).getCropFamily();
            }
        }

        // Assess rotation quality
        boolean hasGoodRotation = groups.stream()
                .allMatch(g -> g.size() < CONSECUTIVE_THRESHOLD);

        // Assess nutrient balance
        String nutrientBalance = assessNutrientBalance(history);

        // Assess pest/disease risk
        String pestRisk = assessPestDiseaseRisk(groups);

        return CropHistoryAnalysisResultDto.AnalysisSummary.builder()
                .dominantCropFamily(dominantFamily != null ? dominantFamily.getFamilyName() : null)
                .consecutiveMonocultureCount(maxConsecutive)
                .rotationPattern(describeRotationPattern(groups))
                .nutrientBalanceAssessment(nutrientBalance)
                .pestDiseaseRiskLevel(pestRisk)
                .hasGoodRotation(hasGoodRotation)
                .hasNutrientDepletionRisk(!risks.isEmpty())
                .build();
    }

    /**
     * Assesses the overall nutrient balance based on crop history.
     */
    private String assessNutrientBalance(List<CropHistoryEntryDto> history) {
        boolean hasLegumes = history.stream()
                .anyMatch(c -> c.getCropFamily() == CropFamily.LEGUMES);
        
        boolean hasCereals = history.stream()
                .anyMatch(c -> c.getCropFamily() == CropFamily.CEREALS);
        
        boolean hasVariedRootDepths = history.stream()
                .map(CropHistoryEntryDto::getRootDepth)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size() > 1;

        if (hasLegumes && hasCereals && hasVariedRootDepths) {
            return "Good - Balanced nutrient cycling with legumes and varied root depths";
        } else if (hasLegumes) {
            return "Moderate - Nitrogen fixation present, consider varied root depths";
        } else if (hasVariedRootDepths) {
            return "Moderate - Varied root depths help, consider adding legumes";
        } else {
            return "Poor - Risk of nutrient depletion, recommend diverse rotation with legumes";
        }
    }

    /**
     * Assesses pest and disease risk based on rotation patterns.
     */
    private String assessPestDiseaseRisk(List<List<CropHistoryEntryDto>> groups) {
        long criticalGroups = groups.stream()
                .filter(g -> g.size() >= CRITICAL_CONSECUTIVE)
                .count();
        
        if (criticalGroups > 0) {
            return "HIGH - Multiple seasons of same family increase pest/disease buildup risk";
        }
        
        long riskGroups = groups.stream()
                .filter(g -> g.size() >= CONSECUTIVE_THRESHOLD)
                .count();
        
        if (riskGroups > 0) {
            return "MODERATE - Some pest/disease pressure likely, monitor closely";
        }
        
        return "LOW - Good rotation reduces pest/disease buildup";
    }

    /**
     * Describes the overall rotation pattern.
     */
    private String describeRotationPattern(List<List<CropHistoryEntryDto>> groups) {
        if (groups.isEmpty()) {
            return "Insufficient data to assess rotation pattern";
        }
        
        long goodGroups = groups.stream()
                .filter(g -> g.size() < CONSECUTIVE_THRESHOLD)
                .count();
        
        if (goodGroups == groups.size()) {
            return "Good rotation - crops from different families are alternated";
        }
        
        long badGroups = groups.stream()
                .filter(g -> g.size() >= CONSECUTIVE_THRESHOLD)
                .count();
        
        if (badGroups > goodGroups) {
            return "Poor rotation - monoculture patterns detected";
        }
        
        return "Moderate rotation - some diversification present";
    }

    /**
     * Generates recommendations based on the analysis.
     */
    private List<String> generateRecommendations(
            List<CropHistoryEntryDto> history,
            List<NutrientDepletionRiskDto> risks,
            CropHistoryAnalysisResultDto.AnalysisSummary summary) {
        
        List<String> recommendations = new ArrayList<>();

        if (risks.isEmpty()) {
            recommendations.add("Current rotation pattern appears healthy - continue monitoring");
            return recommendations;
        }

        for (NutrientDepletionRiskDto risk : risks) {
            recommendations.add(risk.getRecommendation());
        }

        // Add general recommendations based on summary
        if (summary.getConsecutiveMonocultureCount() != null && 
            summary.getConsecutiveMonocultureCount() >= CONSECUTIVE_THRESHOLD) {
            recommendations.add("Consider planting a cover crop or green manure before next main season");
        }

        boolean hasLegumes = history.stream()
                .anyMatch(c -> c.getCropFamily() == CropFamily.LEGUMES);
        
        if (!hasLegumes) {
            recommendations.add("Add legumes (greengram, blackgram, chickpea) to next rotation for biological nitrogen fixation");
        }

        // Check for root depth variety
        boolean hasDeepRoots = history.stream()
                .anyMatch(c -> c.getRootDepth() == CropFamily.RootDepth.DEEP);
        boolean hasShallowRoots = history.stream()
                .anyMatch(c -> c.getRootDepth() == CropFamily.RootDepth.SHALLOW);
        
        if (!hasDeepRoots || !hasShallowRoots) {
            recommendations.add("Include both deep-rooted (sunflower, maize) and shallow-rooted (cabbage, cucumber) crops for better nutrient cycling");
        }

        return recommendations;
    }

    /**
     * Creates an empty analysis result for when there's no crop history.
     */
    private CropHistoryAnalysisResultDto createEmptyAnalysis() {
        return CropHistoryAnalysisResultDto.builder()
                .hasSufficientHistory(false)
                .seasonsAnalyzed(0)
                .cropHistory(Collections.emptyList())
                .nutrientDepletionRisks(Collections.emptyList())
                .summary(CropHistoryAnalysisResultDto.AnalysisSummary.builder()
                        .hasGoodRotation(false)
                        .hasNutrientDepletionRisk(false)
                        .rotationPattern("No crop history available")
                        .nutrientBalanceAssessment("Cannot assess - no history")
                        .pestDiseaseRiskLevel("Cannot assess - no history")
                        .build())
                .recommendations(Collections.singletonList("Start recording crop history to receive personalized rotation recommendations"))
                .build();
    }

    /**
     * Detects if there are consecutive plantings of the same crop family.
     * 
     * Requirements: 3.2
     * 
     * @param cropHistory List of crop history entries
     * @return true if consecutive same-family planting detected
     */
    public boolean hasConsecutiveMonoculture(List<CropHistoryEntryDto> cropHistory) {
        if (cropHistory == null || cropHistory.size() < CONSECUTIVE_THRESHOLD) {
            return false;
        }

        // Enrich entries with crop family information
        enrichCropHistoryEntries(cropHistory);
        
        List<List<CropHistoryEntryDto>> groups = groupConsecutiveByFamily(cropHistory);
        return groups.stream().anyMatch(g -> g.size() >= CONSECUTIVE_THRESHOLD);
    }

    /**
     * Gets the number of consecutive seasons of the same crop family.
     * 
     * @param cropHistory List of crop history entries (most recent first)
     * @return Maximum number of consecutive seasons with same family
     */
    public int getMaxConsecutiveSeasons(List<CropHistoryEntryDto> cropHistory) {
        if (cropHistory == null || cropHistory.isEmpty()) {
            return 0;
        }

        // Enrich entries with crop family information
        enrichCropHistoryEntries(cropHistory);
        
        List<List<CropHistoryEntryDto>> groups = groupConsecutiveByFamily(cropHistory);
        return groups.stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);
    }
}








