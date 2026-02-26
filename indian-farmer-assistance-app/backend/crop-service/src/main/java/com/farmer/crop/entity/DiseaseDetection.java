package com.farmer.crop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

/**
 * Entity for storing disease detection results.
 * 
 * Represents a disease detection performed on a crop image,
 * including the detected disease, confidence score, severity level,
 * and treatment recommendations.
 */
@Entity
@Table(name = "disease_detections", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_crop_id", columnList = "crop_id"),
    @Index(name = "idx_timestamp", columnList = "detection_timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "crop_id")
    private Long cropId;

    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;

    @Column(name = "disease_name", length = 200)
    private String diseaseName;

    @Column(name = "disease_name_local", length = 200)
    private String diseaseNameLocal;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level")
    private SeverityLevel severityLevel;

    @Column(name = "affected_area_percent")
    private Double affectedAreaPercent;

    @Column(name = "treatment_recommendations", columnDefinition = "JSON")
    private String treatmentRecommendations;

    @Column(name = "detection_timestamp", nullable = false)
    private LocalDateTime detectionTimestamp;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    public enum SeverityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
