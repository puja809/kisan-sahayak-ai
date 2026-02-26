package com.farmer.crop.controller;

import com.farmer.crop.dto.DiseaseDetectionResultDto;
import com.farmer.crop.dto.DiseaseDetectionStorageDto;
import com.farmer.crop.entity.DiseaseDetection;
import com.farmer.crop.service.DiseaseDetectionService;
import com.farmer.crop.service.DiseaseDetectionStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for DiseaseDetectionController.
 */
@ExtendWith(MockitoExtension.class)
class DiseaseDetectionControllerTest {

    @Mock
    private DiseaseDetectionStorageService storageService;

    @Mock
    private DiseaseDetectionService detectionService;

    @InjectMocks
    private DiseaseDetectionController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private DiseaseDetectionStorageDto storageDto;
    private DiseaseDetection diseaseDetection;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

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
    void testStoreDiseaseDetection_Success() throws Exception {
        when(storageService.storeDiseaseDetection(any(DiseaseDetectionStorageDto.class)))
                .thenReturn(diseaseDetection);

        mockMvc.perform(post("/api/v1/crops/disease/store")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(storageDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.diseaseName").value("blast"))
                .andExpect(jsonPath("$.confidenceScore").value(85.50));

        verify(storageService, times(1)).storeDiseaseDetection(any(DiseaseDetectionStorageDto.class));
    }

    @Test
    void testGetUserDetections() throws Exception {
        List<DiseaseDetection> detections = Arrays.asList(diseaseDetection);
        when(storageService.getDiseaseDetectionsForUser(1L))
                .thenReturn(detections);

        mockMvc.perform(get("/api/v1/crops/disease/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].diseaseName").value("blast"));

        verify(storageService, times(1)).getDiseaseDetectionsForUser(1L);
    }

    @Test
    void testGetUserDetections_Empty() throws Exception {
        when(storageService.getDiseaseDetectionsForUser(999L))
                .thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/v1/crops/disease/user/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(storageService, times(1)).getDiseaseDetectionsForUser(999L);
    }

    @Test
    void testGetCropDetections() throws Exception {
        List<DiseaseDetection> detections = Arrays.asList(diseaseDetection);
        when(storageService.getDiseaseDetectionsForCrop(100L))
                .thenReturn(detections);

        mockMvc.perform(get("/api/v1/crops/disease/crop/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cropId").value(100L))
                .andExpect(jsonPath("$[0].diseaseName").value("blast"));

        verify(storageService, times(1)).getDiseaseDetectionsForCrop(100L);
    }

    @Test
    void testGetDetection() throws Exception {
        when(storageService.getDiseaseDetectionById(1L))
                .thenReturn(Optional.of(diseaseDetection));

        mockMvc.perform(get("/api/v1/crops/disease/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.diseaseName").value("blast"));

        verify(storageService, times(1)).getDiseaseDetectionById(1L);
    }

    @Test
    void testGetDetection_NotFound() throws Exception {
        when(storageService.getDiseaseDetectionById(999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/crops/disease/999"))
                .andExpect(status().isNotFound());

        verify(storageService, times(1)).getDiseaseDetectionById(999L);
    }

    @Test
    void testGetLatestDetection() throws Exception {
        when(storageService.getMostRecentDetectionForCrop(100L))
                .thenReturn(Optional.of(diseaseDetection));

        mockMvc.perform(get("/api/v1/crops/disease/crop/100/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.diseaseName").value("blast"));

        verify(storageService, times(1)).getMostRecentDetectionForCrop(100L);
    }

    @Test
    void testDeleteDetection() throws Exception {
        mockMvc.perform(delete("/api/v1/crops/disease/1"))
                .andExpect(status().isNoContent());

        verify(storageService, times(1)).deleteDiseaseDetection(1L);
    }

    @Test
    void testRankByConfidence() throws Exception {
        DiseaseDetectionResultDto dto1 = DiseaseDetectionResultDto.builder()
                .diseaseName("blast")
                .confidenceScore(85.50)
                .severityLevel(DiseaseDetectionResultDto.SeverityLevel.HIGH)
                .build();

        DiseaseDetectionResultDto dto2 = DiseaseDetectionResultDto.builder()
                .diseaseName("rust")
                .confidenceScore(60.00)
                .severityLevel(DiseaseDetectionResultDto.SeverityLevel.MEDIUM)
                .build();

        List<DiseaseDetectionResultDto> detections = Arrays.asList(dto2, dto1);
        List<DiseaseDetectionResultDto> ranked = Arrays.asList(dto1, dto2);

        when(detectionService.rankByConfidence(detections))
                .thenReturn(ranked);

        mockMvc.perform(post("/api/v1/crops/disease/rank/confidence")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(detections)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diseaseName").value("blast"))
                .andExpect(jsonPath("$[1].diseaseName").value("rust"));

        verify(detectionService, times(1)).rankByConfidence(detections);
    }

    @Test
    void testRankBySeverity() throws Exception {
        DiseaseDetectionResultDto dto1 = DiseaseDetectionResultDto.builder()
                .diseaseName("blast")
                .confidenceScore(85.50)
                .severityLevel(DiseaseDetectionResultDto.SeverityLevel.CRITICAL)
                .build();

        DiseaseDetectionResultDto dto2 = DiseaseDetectionResultDto.builder()
                .diseaseName("rust")
                .confidenceScore(60.00)
                .severityLevel(DiseaseDetectionResultDto.SeverityLevel.LOW)
                .build();

        List<DiseaseDetectionResultDto> detections = Arrays.asList(dto2, dto1);
        List<DiseaseDetectionResultDto> ranked = Arrays.asList(dto1, dto2);

        when(detectionService.rankBySeverity(detections))
                .thenReturn(ranked);

        mockMvc.perform(post("/api/v1/crops/disease/rank/severity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(detections)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].severityLevel").value("CRITICAL"))
                .andExpect(jsonPath("$[1].severityLevel").value("LOW"));

        verify(detectionService, times(1)).rankBySeverity(detections);
    }
}

