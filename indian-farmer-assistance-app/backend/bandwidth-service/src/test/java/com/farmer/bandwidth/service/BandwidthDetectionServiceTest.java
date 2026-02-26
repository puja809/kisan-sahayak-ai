package com.farmer.bandwidth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BandwidthDetectionService.
 * Validates: Requirements 13.1
 */
class BandwidthDetectionServiceTest {

    private BandwidthDetectionService service;

    @BeforeEach
    void setUp() {
        service = new BandwidthDetectionService();
        ReflectionTestUtils.setField(service, "lowBandwidthThreshold", 256L);
        ReflectionTestUtils.setField(service, "mediumBandwidthThreshold", 1024L);
    }

    @Test
    void testDetectLowBandwidth() {
        BandwidthDetectionService.BandwidthLevel level = service.detectBandwidth(100);
        assertEquals(BandwidthDetectionService.BandwidthLevel.LOW, level);
    }

    @Test
    void testDetectMediumBandwidth() {
        BandwidthDetectionService.BandwidthLevel level = service.detectBandwidth(512);
        assertEquals(BandwidthDetectionService.BandwidthLevel.MEDIUM, level);
    }

    @Test
    void testDetectHighBandwidth() {
        BandwidthDetectionService.BandwidthLevel level = service.detectBandwidth(2048);
        assertEquals(BandwidthDetectionService.BandwidthLevel.HIGH, level);
    }

    @Test
    void testDetectBandwidthAtLowThreshold() {
        BandwidthDetectionService.BandwidthLevel level = service.detectBandwidth(256);
        assertEquals(BandwidthDetectionService.BandwidthLevel.MEDIUM, level);
    }

    @Test
    void testDetectBandwidthAtMediumThreshold() {
        BandwidthDetectionService.BandwidthLevel level = service.detectBandwidth(1024);
        assertEquals(BandwidthDetectionService.BandwidthLevel.HIGH, level);
    }

    @Test
    void testIsLowBandwidth() {
        assertTrue(service.isLowBandwidth(100));
        assertFalse(service.isLowBandwidth(256));
        assertFalse(service.isLowBandwidth(1024));
    }

    @Test
    void testGetLowBandwidthThreshold() {
        assertEquals(256L, service.getLowBandwidthThreshold());
    }

    @Test
    void testGetMediumBandwidthThreshold() {
        assertEquals(1024L, service.getMediumBandwidthThreshold());
    }
}
