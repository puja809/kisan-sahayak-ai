package com.farmer.crop.service;

import com.farmer.crop.dto.RotationOptionDto;
import org.springframework.stereotype.Service;


import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for crop rotation operations.
 * 
 * Handles rotation option generation and ranking.
 */
@Service
public class CropRotationService {

    /**
     * Ranks rotation options by overall benefit score in descending order.
     * 
     * @param options List of rotation options to rank
     * @return Sorted list in descending order by benefit score
     */
    public List<RotationOptionDto> rankRotationOptions(List<RotationOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return options;
        }
        
        return options.stream()
                .sorted(Comparator.comparing(RotationOptionDto::getOverallBenefitScore, 
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Ranks rotation options by soil health benefit specifically.
     * 
     * @param options List of rotation options to rank
     * @return Sorted list in descending order by soil health benefit
     */
    public List<RotationOptionDto> rankBySoilHealthBenefit(List<RotationOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return options;
        }
        
        return options.stream()
                .sorted(Comparator.comparing(RotationOptionDto::getSoilHealthBenefit, 
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Ranks rotation options by climate resilience.
     * 
     * @param options List of rotation options to rank
     * @return Sorted list in descending order by climate resilience
     */
    public List<RotationOptionDto> rankByClimateResilience(List<RotationOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return options;
        }
        
        return options.stream()
                .sorted(Comparator.comparing(RotationOptionDto::getClimateResilience, 
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Ranks rotation options by economic viability.
     * 
     * @param options List of rotation options to rank
     * @return Sorted list in descending order by economic viability
     */
    public List<RotationOptionDto> rankByEconomicViability(List<RotationOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return options;
        }
        
        return options.stream()
                .sorted(Comparator.comparing(RotationOptionDto::getEconomicViability, 
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Calculates overall benefit score from component scores.
     * 
     * @param option The rotation option to calculate for
     * @return Overall benefit score
     */
    public Double calculateOverallBenefitScore(RotationOptionDto option) {
        if (option == null) {
            return 0.0;
        }
        
        Double soilHealth = option.getSoilHealthBenefit() != null ? 
                option.getSoilHealthBenefit() : 0.0;
        Double climate = option.getClimateResilience() != null ? 
                option.getClimateResilience() : 0.0;
        Double economic = option.getEconomicViability() != null ? 
                option.getEconomicViability() : 0.0;
        
        return (soilHealth + climate + economic) / 3.0;
    }
}








