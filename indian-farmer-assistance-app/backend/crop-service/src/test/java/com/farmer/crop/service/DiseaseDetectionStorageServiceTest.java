package com.farmer.crop.service;

import com.farmer.crop.dto.DiseaseDetectionStorageDto;
import com.farmer.crop.entity.DiseaseDetection;
import com.farmer.crop.repository.DiseaseDetectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DiseaseDetectionStorageService.
 */
@ExtendWith(MockitoExtension.class)
class DiseaseDetectionStorageServiceTest {

    @Mock
    private DiseaseDetectionRepository diseaseDetectionRepository;

    @InjectMocks
    private DiseaseDetectionStorageService storageService;

    private DiseaseDetectionStorageDto storageDto;
    private DiseaseDetection diseaseDetection;

    @BeforeEach
    void setUp() {
        storageDto = DiseaseDetectionStorageDto.builder()
                .userId(1L)
                .cropId(100L)
                .imagePath("/uploads/disease_detection/1/image.jpg")
                .diseaseName("blast")
                .diseaseNameLocal("धान का ब्लास्ट")
                .confidenceScore(85.50)
                .severityLevel("HIGH")
                .affectedAreaPercent(25.00)
                .treatmentRecommendations("{\"options\": []}")
                .detectionTimestamp(LocalDateTime.now())
                .modelVersion("1.0.0")
                .build();

        diseaseDetection = DiseaseDetection.builder()
                .id(1L)
                .userId(1L)
                .cropId(100L)
                .imagePath("/uploads/disease_detection/1/image.jpg")
                .diseaseName("blast")
                .diseaseNameLocal("धान का ब्लास्ट")
                .confidenceScore(85.50)
                .severityLevel(DiseaseDetection.SeverityLevel.HIGH)
                .affectedAreaPercent(25.00)
                .treatmentRecommendations("{\"options\": []}")
                .detectionTimestamp(LocalDateTime.now())
                .modelVersion("1.0.0")
                .build();
    }

    @Test
    void testStoreDiseaseDetection_Success() {
        when(diseaseDetectionRepository.save(any(DiseaseDetection.class)))
                .thenReturn(diseaseDetection);

        DiseaseDetection result = storageService.storeDiseaseDetection(storageDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("blast", result.getDiseaseName());
        assertEquals(85.50, result.getConfidenceScore());
        assertEquals(DiseaseDetection.SeverityLevel.HIGH, result.getSeverityLevel());
        verify(diseaseDetectionRepository, times(1)).save(any(DiseaseDetection.class));
    }

    @Test
    void testStoreDiseaseDetection_WithNullCropId() {
        storageDto.setCropId(null);
        when(diseaseDetectionRepository.save(any(DiseaseDetection.class)))
                .thenReturn(diseaseDetection);

        DiseaseDetection result = storageService.storeDiseaseDetection(storageDto);

        assertNotNull(result);
        verify(diseaseDetectionRepository, times(1)).save(any(DiseaseDetection.class));
    }

    @Test
    void testGetDiseaseDetectionsForUser() {
        List<DiseaseDetection> detections = Arrays.asList(diseaseDetection);
        when(diseaseDetectionRepository.findByUserId(1L))
                .thenReturn(detections);

        List<DiseaseDetection> result = storageService.getDiseaseDetectionsForUser(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("blast", result.get(0).getDiseaseName());
        verify(diseaseDetectionRepository, times(1)).findByUserId(1L);
    }

    @Test
    void testGetDiseaseDetectionsForUser_Empty() {
        when(diseaseDetectionRepository.findByUserId(999L))
                .thenReturn(Arrays.asList());

        List<DiseaseDetection> result = storageService.getDiseaseDetectionsForUser(999L);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(diseaseDetectionRepository, times(1)).findByUserId(999L);
    }

    @Test
    void testGetDiseaseDetectionsForCrop() {
        List<DiseaseDetection> detections = Arrays.asList(diseaseDetection);
        when(diseaseDetectionRepository.findByCropId(100L))
                .thenReturn(detections);

        List<DiseaseDetection> result = storageService.getDiseaseDetectionsForCrop(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getCropId());
        verify(diseaseDetectionRepository, times(1)).findByCropId(100L);
    }

    @Test
    void testGetDiseaseDetectionById() {
        when(diseaseDetectionRepository.findById(1L))
                .thenReturn(Optional.of(diseaseDetection));

        Optional<DiseaseDetection> result = storageService.getDiseaseDetectionById(1L);

        assertTrue(result.isPresent());
        assertEquals("blast", result.get().getDiseaseName());
        verify(diseaseDetectionRepository, times(1)).findById(1L);
    }

    @Test
    void testGetDiseaseDetectionById_NotFound() {
        when(diseaseDetectionRepository.findById(999L))
                .thenReturn(Optional.empty());

        Optional<DiseaseDetection> result = storageService.getDiseaseDetectionById(999L);

        assertFalse(result.isPresent());
        verify(diseaseDetectionRepository, times(1)).findById(999L);
    }

    @Test
    void testGetMostRecentDetectionForCrop() {
        when(diseaseDetectionRepository.findMostRecentByCropId(100L))
                .thenReturn(Optional.of(diseaseDetection));

        Optional<DiseaseDetection> result = storageService.getMostRecentDetectionForCrop(100L);

        assertTrue(result.isPresent());
        assertEquals("blast", result.get().getDiseaseName());
        verify(diseaseDetectionRepository, times(1)).findMostRecentByCropId(100L);
    }

    @Test
    void testCountDetectionsForUser() {
        when(diseaseDetectionRepository.countByUserId(1L))
                .thenReturn(5L);

        long count = storageService.countDetectionsForUser(1L);

        assertEquals(5L, count);
        verify(diseaseDetectionRepository, times(1)).countByUserId(1L);
    }

    @Test
    void testCountDetectionsForCrop() {
        when(diseaseDetectionRepository.countByCropId(100L))
                .thenReturn(3L);

        long count = storageService.countDetectionsForCrop(100L);

        assertEquals(3L, count);
        verify(diseaseDetectionRepository, times(1)).countByCropId(100L);
    }

    @Test
    void testDeleteDiseaseDetection() {
        storageService.deleteDiseaseDetection(1L);

        verify(diseaseDetectionRepository, times(1)).deleteById(1L);
    }

    @Test
    void testStoreDiseaseDetection_AllSeverityLevels() {
        for (DiseaseDetection.SeverityLevel severity : DiseaseDetection.SeverityLevel.values()) {
            storageDto.setSeverityLevel(severity.toString());
            when(diseaseDetectionRepository.save(any(DiseaseDetection.class)))
                    .thenReturn(diseaseDetection);

            DiseaseDetection result = storageService.storeDiseaseDetection(storageDto);

            assertNotNull(result);
            verify(diseaseDetectionRepository, atLeastOnce()).save(any(DiseaseDetection.class));
        }
    }

    @Test
    void testStoreDiseaseDetection_HighConfidenceScore() {
        storageDto.setConfidenceScore(99.99);
        when(diseaseDetectionRepository.save(any(DiseaseDetection.class)))
                .thenReturn(diseaseDetection);

        DiseaseDetection result = storageService.storeDiseaseDetection(storageDto);

        assertNotNull(result);
        verify(diseaseDetectionRepository, times(1)).save(any(DiseaseDetection.class));
    }

    @Test
    void testStoreDiseaseDetection_LowConfidenceScore() {
        storageDto.setConfidenceScore(15.50);
        when(diseaseDetectionRepository.save(any(DiseaseDetection.class)))
                .thenReturn(diseaseDetection);

        DiseaseDetection result = storageService.storeDiseaseDetection(storageDto);

        assertNotNull(result);
        verify(diseaseDetectionRepository, times(1)).save(any(DiseaseDetection.class));
    }
}


