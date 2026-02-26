package com.farmer.bandwidth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for automatically adjusting content quality when network improves.
 * Validates: Requirements 13.6
 */
@Slf4j
@Service
public class AutomaticQualityAdjustmentService {

    private final BandwidthDetectionService bandwidthDetectionService;
    private final AdaptiveQualityService adaptiveQualityService;

    public AutomaticQualityAdjustmentService(
            BandwidthDetectionService bandwidthDetectionService,
            AdaptiveQualityService adaptiveQualityService) {
        this.bandwidthDetectionService = bandwidthDetectionService;
        this.adaptiveQualityService = adaptiveQualityService;
    }

    /**
     * Tracks quality adjustment state for a session.
     */
    public static class QualityAdjustmentState {
        private BandwidthDetectionService.BandwidthLevel currentLevel;
        private long lastAdjustmentTime;
        private int adjustmentCount;

        public QualityAdjustmentState(BandwidthDetectionService.BandwidthLevel initialLevel) {
            this.currentLevel = initialLevel;
            this.lastAdjustmentTime = System.currentTimeMillis();
            this.adjustmentCount = 0;
        }

        public BandwidthDetectionService.BandwidthLevel getCurrentLevel() {
            return currentLevel;
        }

        public void setCurrentLevel(BandwidthDetectionService.BandwidthLevel level) {
            this.currentLevel = level;
            this.lastAdjustmentTime = System.currentTimeMillis();
            this.adjustmentCount++;
        }

        public long getLastAdjustmentTime() {
            return lastAdjustmentTime;
        }

        public int getAdjustmentCount() {
            return adjustmentCount;
        }

        public boolean hasImproved(BandwidthDetectionService.BandwidthLevel newLevel) {
            return newLevel.ordinal() > currentLevel.ordinal();
        }

        public boolean hasWorsened(BandwidthDetectionService.BandwidthLevel newLevel) {
            return newLevel.ordinal() < currentLevel.ordinal();
        }
    }

    /**
     * Creates a new quality adjustment state tracker.
     * 
     * @param initialBandwidthKbps initial bandwidth in Kbps
     * @return QualityAdjustmentState instance
     */
    public QualityAdjustmentState createAdjustmentState(long initialBandwidthKbps) {
        BandwidthDetectionService.BandwidthLevel level = bandwidthDetectionService.detectBandwidth(initialBandwidthKbps);
        return new QualityAdjustmentState(level);
    }

    /**
     * Checks if quality should be adjusted based on bandwidth change.
     * 
     * @param state current adjustment state
     * @param newBandwidthKbps new bandwidth measurement in Kbps
     * @return true if adjustment is needed
     */
    public boolean shouldAdjustQuality(QualityAdjustmentState state, long newBandwidthKbps) {
        BandwidthDetectionService.BandwidthLevel newLevel = bandwidthDetectionService.detectBandwidth(newBandwidthKbps);
        return !newLevel.equals(state.getCurrentLevel());
    }

    /**
     * Adjusts quality based on new bandwidth measurement.
     * 
     * @param state current adjustment state
     * @param newBandwidthKbps new bandwidth measurement in Kbps
     * @return true if quality was improved
     */
    public boolean adjustQuality(QualityAdjustmentState state, long newBandwidthKbps) {
        BandwidthDetectionService.BandwidthLevel newLevel = bandwidthDetectionService.detectBandwidth(newBandwidthKbps);
        
        if (newLevel.equals(state.getCurrentLevel())) {
            return false;
        }

        boolean improved = state.hasImproved(newLevel);
        state.setCurrentLevel(newLevel);
        
        if (improved) {
            log.info("Network quality improved to {}, increasing content quality", newLevel);
        } else {
            log.info("Network quality degraded to {}, reducing content quality", newLevel);
        }
        
        return improved;
    }

    /**
     * Gets the new quality level after adjustment.
     * 
     * @param state current adjustment state
     * @param newBandwidthKbps new bandwidth measurement in Kbps
     * @return new quality level (0-100)
     */
    public int getAdjustedQualityLevel(QualityAdjustmentState state, long newBandwidthKbps) {
        return adaptiveQualityService.getImageQualityLevel(newBandwidthKbps);
    }

    /**
     * Determines if content should be refreshed after quality improvement.
     * 
     * @param state current adjustment state
     * @param newBandwidthKbps new bandwidth measurement in Kbps
     * @return true if content should be refreshed
     */
    public boolean shouldRefreshContent(QualityAdjustmentState state, long newBandwidthKbps) {
        BandwidthDetectionService.BandwidthLevel newLevel = bandwidthDetectionService.detectBandwidth(newBandwidthKbps);
        return state.hasImproved(newLevel);
    }

    /**
     * Gets minimum time between adjustments to avoid thrashing (in milliseconds).
     * 
     * @return minimum adjustment interval
     */
    public long getMinimumAdjustmentInterval() {
        return 5000; // 5 seconds
    }

    /**
     * Checks if enough time has passed since last adjustment.
     * 
     * @param state current adjustment state
     * @return true if adjustment interval has passed
     */
    public boolean isAdjustmentIntervalPassed(QualityAdjustmentState state) {
        long timeSinceLastAdjustment = System.currentTimeMillis() - state.getLastAdjustmentTime();
        return timeSinceLastAdjustment >= getMinimumAdjustmentInterval();
    }
}
