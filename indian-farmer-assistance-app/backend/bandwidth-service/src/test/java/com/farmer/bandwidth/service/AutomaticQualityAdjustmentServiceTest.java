package com.farmer.bandwidth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AutomaticQualityAdjustmentService.
 * Validates: Requirements 13.6
 */
class AutomaticQualityAdjustmentServiceTest {

    private AutomaticQualityAdjustmentService service;
    private BandwidthDetectionService bandwidthDetectionService;
    private AdaptiveQualityService adaptiveQualityService;

    @BeforeEach
    void setUp() {
        bandwidthDetectionService = new BandwidthDetectionService();
        ReflectionTestUtils.setField(bandwidthDetectionService, "lowBandwidthThreshold", 256L);
        ReflectionTestUtils.setField(bandwidthDetectionService, "mediumBandwidthThreshold", 1024L);

        adaptiveQualityService = new AdaptiveQualityService(bandwidthDetectionService);
        ReflectionTestUtils.setField(adaptiveQualityService, "lowBandwidthCompressionRatio", 0.3);
        ReflectionTestUtils.setField(adaptiveQualityService, "mediumBandwidthCompressionRatio", 0.6);
        ReflectionTestUtils.setField(adaptiveQualityService, "highBandwidthCompressionRatio", 1.0);

        service = new AutomaticQualityAdjustmentService(bandwidthDetectionService, adaptiveQualityService);
    }

    @Test
    void testCreateAdjustmentState() {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(100);
        assertEquals(BandwidthDetectionService.BandwidthLevel.LOW, state.getCurrentLevel());
        assertEquals(0, state.getAdjustmentCount());
    }

    @Test
    void testShouldAdjustQualityWhenLevelChanges() {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(100);
        assertTrue(service.shouldAdjustQuality(state, 512));
    }

    @Test
    void testShouldNotAdjustQualityWhenLevelSame() {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(100);
        assertFalse(service.shouldAdjustQuality(state, 150));
    }

    @Test
    void testAdjustQualityImprovement() {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(100);
        boolean improved = service.adjustQuality(state, 512);
        assertTrue(improved);
        assertEquals(BandwidthDetectionService.BandwidthLevel.MEDIUM, state.getCurrentLevel());
        assertEquals(1, state.getAdjustmentCount());
    }

    @Test
    void testAdjustQualityDegradation() {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(2048);
        boolean improved = service.adjustQuality(state, 512);
        assertFalse(improved);
        assertEquals(BandwidthDetectionService.BandwidthLevel.MEDIUM, state.getCurrentLevel());
    }

    @Test
    void testGetAdjustedQualityLevel() {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(100);
        int qualityLevel = service.getAdjustedQualityLevel(state, 512);
        assertEquals(60, qualityLevel);
    }

    @Test
    void testShouldRefreshContentAfterImprovement() {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(100);
        assertTrue(service.shouldRefreshContent(state, 512));
    }

    @Test
    void testShouldNotRefreshContentAfterDegradation() {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(2048);
        assertFalse(service.shouldRefreshContent(state, 512));
    }

    @Test
    void testGetMinimumAdjustmentInterval() {
        long interval = service.getMinimumAdjustmentInterval();
        assertEquals(5000, interval);
    }

    @Test
    void testIsAdjustmentIntervalPassed() throws InterruptedException {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(100);
        assertFalse(service.isAdjustmentIntervalPassed(state));
        
        Thread.sleep(5100);
        assertTrue(service.isAdjustmentIntervalPassed(state));
    }

    @Test
    void testMultipleAdjustments() {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(100);
        
        service.adjustQuality(state, 512);
        assertEquals(1, state.getAdjustmentCount());
        
        service.adjustQuality(state, 2048);
        assertEquals(2, state.getAdjustmentCount());
    }

    @Test
    void testHasImprovedMethod() {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(100);
        assertTrue(state.hasImproved(BandwidthDetectionService.BandwidthLevel.MEDIUM));
        assertTrue(state.hasImproved(BandwidthDetectionService.BandwidthLevel.HIGH));
        assertFalse(state.hasImproved(BandwidthDetectionService.BandwidthLevel.LOW));
    }

    @Test
    void testHasWorsenedMethod() {
        AutomaticQualityAdjustmentService.QualityAdjustmentState state = service.createAdjustmentState(2048);
        assertTrue(state.hasWorsened(BandwidthDetectionService.BandwidthLevel.MEDIUM));
        assertTrue(state.hasWorsened(BandwidthDetectionService.BandwidthLevel.LOW));
        assertFalse(state.hasWorsened(BandwidthDetectionService.BandwidthLevel.HIGH));
    }
}
