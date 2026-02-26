package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

/**
 * DTO for storing disease detection results from AI service.
 * 
 * Used to receive disease detection results from the Python AI service
 * and store them in the database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseDetectionStorageDto {

    private Long userId;
    private Long cropId;
    private String imagePath;
    private String diseaseName;
    private String diseaseNameLocal;
    private Double confidenceScore;
    private String severityLevel;
    private Double affectedAreaPercent;
    private String treatmentRecommendations;
    private LocalDateTime detectionTimestamp;
    private String modelVersion;
}
