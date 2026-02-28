package com.farmer.crop.service;

import com.farmer.crop.dto.DiseaseDetectionStorageDto;
import com.farmer.crop.repository.DiseaseDetectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for storing and retrieving disease detection results.
 * 
 * Handles persistence of disease detection data from the AI service
 * and provides query methods for retrieving detection history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiseaseDetectionStorageService {

    private final DiseaseDetectionRepository diseaseDetectionRepository;

    /**
     * Store a disease detection result.
     * 
     * @param storageDto The disease detection data to store
     * @return The stored DiseaseDetection entity
     */
    @Transactional
    public DiseaseDetection storeDiseaseDetection(DiseaseDetectionStorageDto storageDto) {
        log.info("Storing disease detection for user: {}, crop: {}", 
                 storageDto.getUserId(), storageDto.getCropId());

        DiseaseDetection detection = DiseaseDetection.builder()
                .userId(storageDto.getUserId())
                .cropId(storageDto.getCropId())
                .imagePath(storageDto.getImagePath())
                .diseaseName(storageDto.getDiseaseName())
                .diseaseNameLocal(storageDto.getDiseaseNameLocal())
                .confidenceScore(storageDto.getConfidenceScore())
                .severityLevel(DiseaseDetection.SeverityLevel.valueOf(storageDto.getSeverityLevel()))
                .affectedAreaPercent(storageDto.getAffectedAreaPercent())
                .treatmentRecommendations(storageDto.getTreatmentRecommendations())
                .detectionTimestamp(storageDto.getDetectionTimestamp())
                .modelVersion(storageDto.getModelVersion())
                .build();

        DiseaseDetection saved = diseaseDetectionRepository.save(detection);
        log.info("Disease detection stored with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Get all disease detections for a user.
     * 
     * @param userId The user ID
     * @return List of disease detections
     */
    public List<DiseaseDetection> getDiseaseDetectionsForUser(Long userId) {
        log.debug("Retrieving disease detections for user: {}", userId);
        return diseaseDetectionRepository.findByUserId(userId);
    }

    /**
     * Get all disease detections for a crop.
     * 
     * @param cropId The crop ID
     * @return List of disease detections
     */
    public List<DiseaseDetection> getDiseaseDetectionsForCrop(Long cropId) {
        log.debug("Retrieving disease detections for crop: {}", cropId);
        return diseaseDetectionRepository.findByCropId(cropId);
    }

    /**
     * Get a specific disease detection by ID.
     * 
     * @param detectionId The detection ID
     * @return Optional containing the disease detection
     */
    public Optional<DiseaseDetection> getDiseaseDetectionById(Long detectionId) {
        log.debug("Retrieving disease detection with ID: {}", detectionId);
        return diseaseDetectionRepository.findById(detectionId);
    }

    /**
     * Get the most recent disease detection for a crop.
     * 
     * @param cropId The crop ID
     * @return Optional containing the most recent detection
     */
    public Optional<DiseaseDetection> getMostRecentDetectionForCrop(Long cropId) {
        log.debug("Retrieving most recent disease detection for crop: {}", cropId);
        return diseaseDetectionRepository.findMostRecentByCropId(cropId);
    }

    /**
     * Count disease detections for a user.
     * 
     * @param userId The user ID
     * @return Count of disease detections
     */
    public long countDetectionsForUser(Long userId) {
        return diseaseDetectionRepository.countByUserId(userId);
    }

    /**
     * Count disease detections for a crop.
     * 
     * @param cropId The crop ID
     * @return Count of disease detections
     */
    public long countDetectionsForCrop(Long cropId) {
        return diseaseDetectionRepository.countByCropId(cropId);
    }

    /**
     * Delete a disease detection.
     * 
     * @param detectionId The detection ID
     */
    @Transactional
    public void deleteDiseaseDetection(Long detectionId) {
        log.info("Deleting disease detection with ID: {}", detectionId);
        diseaseDetectionRepository.deleteById(detectionId);
    }
}









