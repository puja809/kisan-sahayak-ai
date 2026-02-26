package com.farmer.bandwidth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdaptiveQualityService.
 * Validates: Requirements 13.1, 13.2
 */
class AdaptiveQualityServiceTest {

    private AdaptiveQualityService service;
    private BandwidthDetectionService bandwidthDetectionService;

    @BeforeEach
    void setUp() {
        bandwidthDetectionService = new BandwidthDetectionService();
        ReflectionTestUtils.setField(bandwidthDetectionService, "lowBandwidthThreshold", 256L);
        ReflectionTestUtils.setField(bandwidthDetectionService, "mediumBandwidthThreshold", 1024L);

        service = new AdaptiveQualityService(bandwidthDetectionService);
        ReflectionTestUtils.setField(service, "lowBandwidthCompressionRatio", 0.3);
        ReflectionTestUtils.setField(service, "mediumBandwidthCompressionRatio", 0.6);
        ReflectionTestUtils.setField(service, "highBandwidthCompressionRatio", 1.0);
    }

    @Test
    void testGetQualityCompressionRatioLowBandwidth() {
        double ratio = service.getQualityCompressionRatio(100);
        assertEquals(0.3, ratio);
    }

    @Test
    void testGetQualityCompressionRatioMediumBandwidth() {
        double ratio = service.getQualityCompressionRatio(512);
        assertEquals(0.6, ratio);
    }

    @Test
    void testGetQualityCompressionRatioHighBandwidth() {
        double ratio = service.getQualityCompressionRatio(2048);
        assertEquals(1.0, ratio);
    }

    @Test
    void testShouldDeprioritizeImagesLowBandwidth() {
        assertTrue(service.shouldDeprioritizeImages(100));
    }

    @Test
    void testShouldDeprioritizeImagesMediumBandwidth() {
        assertFalse(service.shouldDeprioritizeImages(512));
    }

    @Test
    void testGetImageQualityLevelLowBandwidth() {
        int quality = service.getImageQualityLevel(100);
        assertEquals(30, quality);
    }

    @Test
    void testGetImageQualityLevelMediumBandwidth() {
        int quality = service.getImageQualityLevel(512);
        assertEquals(60, quality);
    }

    @Test
    void testGetImageQualityLevelHighBandwidth() {
        int quality = service.getImageQualityLevel(2048);
        assertEquals(100, quality);
    }

    @Test
    void testGetEstimatedCompressedSizeLowBandwidth() {
        long originalSize = 1000000; // 1MB
        long compressedSize = service.getEstimatedCompressedSize(originalSize, 100);
        assertEquals(300000, compressedSize);
    }

    @Test
    void testGetEstimatedCompressedSizeMediumBandwidth() {
        long originalSize = 1000000; // 1MB
        long compressedSize = service.getEstimatedCompressedSize(originalSize, 512);
        assertEquals(600000, compressedSize);
    }

    @Test
    void testGetEstimatedCompressedSizeHighBandwidth() {
        long originalSize = 1000000; // 1MB
        long compressedSize = service.getEstimatedCompressedSize(originalSize, 2048);
        assertEquals(1000000, compressedSize);
    }

    @Test
    void testShouldUseTextOnlyLowBandwidth() {
        assertTrue(service.shouldUseTextOnly(100));
    }

    @Test
    void testShouldUseTextOnlyMediumBandwidth() {
        assertFalse(service.shouldUseTextOnly(512));
    }
}
