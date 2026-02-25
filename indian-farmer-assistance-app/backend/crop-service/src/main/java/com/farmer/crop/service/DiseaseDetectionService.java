package com.farmer.crop.service;

import com.farmer.crop.dto.DiseaseDetectionResultDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for disease detection operations.
 * 
 * Handles disease detection ranking and processing.
 */
@Service
public class DiseaseDetectionService {

    /**
     * Ranks disease detections by confidence score in descending order.
     * When confidence scores are equal, higher severity is ranked first.
     * 
     * @param detections List of disease detections to rank
     * @return Sorted list in descending order by confidence
     */
    public List<DiseaseDetectionResultDto> rankByConfidence(List<DiseaseDetectionResultDto> detections) {
        if (detections == null || detections.isEmpty()) {
            return detections;
        }
        
        return detections.stream()
                .sorted(Comparator
                        // Primary sort: confidence score descending
                        .comparing(DiseaseDetectionResultDto::getConfidenceScore, 
                                Comparator.reverseOrder())
                        // Secondary sort: severity (CRITICAL > HIGH > MEDIUM > LOW)
                        .thenComparing(this::getSeverityRank, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Ranks disease detections by severity level.
     * 
     * @param detections List of disease detections to rank
     * @return Sorted list in descending order by severity
     */
    public List<DiseaseDetectionResultDto> rankBySeverity(List<DiseaseDetectionResultDto> detections) {
        if (detections == null || detections.isEmpty()) {
            return detections;
        }
        
        return detections.stream()
                .sorted(Comparator.comparing(this::getSeverityRank, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Filters disease detections by confidence threshold.
     * 
     * @param detections List of disease detections
     * @param threshold Minimum confidence score (0-100)
     * @return Filtered list with confidence >= threshold
     */
    public List<DiseaseDetectionResultDto> filterByConfidenceThreshold(
            List<DiseaseDetectionResultDto> detections, BigDecimal threshold) {
        if (detections == null || detections.isEmpty()) {
            return detections;
        }
        
        return detections.stream()
                .filter(d -> d.getConfidenceScore() != null && 
                        d.getConfidenceScore().compareTo(threshold) >= 0)
                .collect(Collectors.toList());
    }

    /**
     * Gets the severity rank for sorting purposes.
     * CRITICAL = 4, HIGH = 3, MEDIUM = 2, LOW = 1
     * 
     * @param detection The disease detection
     * @return Severity rank integer
     */
    private int getSeverityRank(DiseaseDetectionResultDto detection) {
        if (detection == null || detection.getSeverityLevel() == null) {
            return 0;
        }
        
        switch (detection.getSeverityLevel()) {
            case CRITICAL:
                return 4;
            case HIGH:
                return 3;
            case MEDIUM:
                return 2;
            case LOW:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * Checks if any detection has confidence below threshold.
     * 
     * @param detections List of disease detections
     * @param threshold Minimum confidence score
     * @return true if any detection is below threshold
     */
    public boolean hasLowConfidenceDetections(List<DiseaseDetectionResultDto> detections, 
            BigDecimal threshold) {
        if (detections == null || detections.isEmpty()) {
            return false;
        }
        
        return detections.stream()
                .anyMatch(d -> d.getConfidenceScore() != null && 
                        d.getConfidenceScore().compareTo(threshold) < 0);
    }
}