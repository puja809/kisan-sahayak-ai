package com.farmer.crop.service;

import com.farmer.crop.dto.SchemeRecommendationDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for scheme recommendation operations.
 * 
 * Handles scheme recommendation ranking and filtering.
 */
@Service
public class SchemeRecommendationService {

    /**
     * Ranks scheme recommendations by benefit amount in descending order.
     * When benefit amounts are equal, schemes with closer deadlines are ranked first.
     * 
     * @param recommendations List of scheme recommendations to rank
     * @return Sorted list in descending order by benefit amount
     */
    public List<SchemeRecommendationDto> rankByBenefit(List<SchemeRecommendationDto> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return recommendations;
        }
        
        return recommendations.stream()
                .sorted(Comparator
                        // Primary sort: benefit amount descending
                        .comparing(SchemeRecommendationDto::getBenefitAmount, 
                                Comparator.reverseOrder())
                        // Secondary sort: deadline proximity (closer deadline first)
                        .thenComparing(this::getDaysUntilDeadline, Comparator.naturalOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Ranks scheme recommendations by deadline proximity.
     * Schemes with closer deadlines are ranked first.
     * 
     * @param recommendations List of scheme recommendations to rank
     * @return Sorted list by deadline proximity
     */
    public List<SchemeRecommendationDto> rankByDeadlineProximity(List<SchemeRecommendationDto> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return recommendations;
        }
        
        return recommendations.stream()
                .sorted(Comparator.comparing(this::getDaysUntilDeadline, Comparator.naturalOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Ranks scheme recommendations by eligibility score.
     * 
     * @param recommendations List of scheme recommendations to rank
     * @return Sorted list in descending order by eligibility
     */
    public List<SchemeRecommendationDto> rankByEligibility(List<SchemeRecommendationDto> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return recommendations;
        }
        
        return recommendations.stream()
                .sorted(Comparator.comparing(SchemeRecommendationDto::getEligibilityScore, 
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Calculates overall score combining benefit, eligibility, and deadline proximity.
     * 
     * @param recommendation The scheme recommendation
     * @return Overall score
     */
    public BigDecimal calculateOverallScore(SchemeRecommendationDto recommendation) {
        if (recommendation == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal benefit = recommendation.getBenefitAmount() != null ? 
                normalizeBenefit(recommendation.getBenefitAmount()) : BigDecimal.ZERO;
        BigDecimal eligibility = recommendation.getEligibilityScore() != null ? 
                recommendation.getEligibilityScore() : BigDecimal.ZERO;
        BigDecimal deadline = recommendation.getDeadlineProximityScore() != null ? 
                recommendation.getDeadlineProximityScore() : BigDecimal.ZERO;
        
        // Weighted average: benefit 50%, eligibility 30%, deadline 20%
        return benefit.multiply(new BigDecimal("0.5"))
                .add(eligibility.multiply(new BigDecimal("0.3")))
                .add(deadline.multiply(new BigDecimal("0.2")));
    }

    /**
     * Gets days until application deadline.
     * 
     * @param recommendation The scheme recommendation
     * @return Days until deadline (0 if deadline has passed)
     */
    private long getDaysUntilDeadline(SchemeRecommendationDto recommendation) {
        if (recommendation == null || recommendation.getApplicationDeadline() == null) {
            return Long.MAX_VALUE;
        }
        
        LocalDate deadline = recommendation.getApplicationDeadline();
        LocalDate today = LocalDate.now();
        
        if (deadline.isBefore(today)) {
            return 0; // Already passed
        }
        
        return ChronoUnit.DAYS.between(today, deadline);
    }

    /**
     * Normalizes benefit amount to 0-100 scale.
     * 
     * @param benefit The raw benefit amount
     * @return Normalized score
     */
    private BigDecimal normalizeBenefit(BigDecimal benefit) {
        if (benefit == null) {
            return BigDecimal.ZERO;
        }
        
        // Assuming max benefit is 500,000
        BigDecimal maxBenefit = new BigDecimal("500000");
        BigDecimal normalized = benefit.divide(maxBenefit, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        return normalized.min(new BigDecimal("100"));
    }

    /**
     * Filters schemes with approaching deadlines.
     * 
     * @param recommendations List of scheme recommendations
     * @param daysThreshold Number of days to consider as "approaching"
     * @return Filtered list with deadlines within threshold
     */
    public List<SchemeRecommendationDto> filterByApproachingDeadline(
            List<SchemeRecommendationDto> recommendations, int daysThreshold) {
        if (recommendations == null || recommendations.isEmpty()) {
            return recommendations;
        }
        
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);
        
        return recommendations.stream()
                .filter(r -> r.getApplicationDeadline() != null && 
                        !r.getApplicationDeadline().isBefore(LocalDate.now()) &&
                        !r.getApplicationDeadline().isAfter(thresholdDate))
                .collect(Collectors.toList());
    }
}