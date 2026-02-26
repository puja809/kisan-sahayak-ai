package com.farmer.bandwidth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for detecting network bandwidth levels and classifying them.
 * Validates: Requirements 13.1
 */
@Slf4j
@Service
public class BandwidthDetectionService {

    @Value("${bandwidth.low-threshold-kbps:256}")
    private long lowBandwidthThreshold;

    @Value("${bandwidth.medium-threshold-kbps:1024}")
    private long mediumBandwidthThreshold;

    /**
     * Bandwidth classification levels
     */
    public enum BandwidthLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    /**
     * Detects and classifies network bandwidth level.
     * 
     * @param bandwidthKbps bandwidth in kilobits per second
     * @return BandwidthLevel classification
     */
    public BandwidthLevel detectBandwidth(long bandwidthKbps) {
        if (bandwidthKbps < lowBandwidthThreshold) {
            return BandwidthLevel.LOW;
        } else if (bandwidthKbps < mediumBandwidthThreshold) {
            return BandwidthLevel.MEDIUM;
        } else {
            return BandwidthLevel.HIGH;
        }
    }

    /**
     * Checks if bandwidth is low.
     * 
     * @param bandwidthKbps bandwidth in kilobits per second
     * @return true if bandwidth is below low threshold
     */
    public boolean isLowBandwidth(long bandwidthKbps) {
        return detectBandwidth(bandwidthKbps) == BandwidthLevel.LOW;
    }

    /**
     * Gets the low bandwidth threshold in Kbps.
     * 
     * @return low bandwidth threshold
     */
    public long getLowBandwidthThreshold() {
        return lowBandwidthThreshold;
    }

    /**
     * Gets the medium bandwidth threshold in Kbps.
     * 
     * @return medium bandwidth threshold
     */
    public long getMediumBandwidthThreshold() {
        return mediumBandwidthThreshold;
    }
}
