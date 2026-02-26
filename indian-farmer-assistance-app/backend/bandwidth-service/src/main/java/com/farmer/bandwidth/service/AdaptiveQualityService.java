package com.farmer.bandwidth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for adaptive quality reduction based on bandwidth levels.
 * Validates: Requirements 13.1, 13.2
 */
@Slf4j
@Service
public class AdaptiveQualityService {

    @Value("${quality.low-bandwidth-compression-ratio:0.3}")
    private double lowBandwidthCompressionRatio;

    @Value("${quality.medium-bandwidth-compression-ratio:0.6}")
    private double mediumBandwidthCompressionRatio;

    @Value("${quality.high-bandwidth-compression-ratio:1.0}")
    private double highBandwidthCompressionRatio;

    private final BandwidthDetectionService bandwidthDetectionService;

    public AdaptiveQualityService(BandwidthDetectionService bandwidthDetectionService) {
        this.bandwidthDetectionService = bandwidthDetectionService;
    }

    /**
     * Gets the quality compression ratio based on bandwidth level.
     * Lower ratio means more compression (lower quality).
     * 
     * @param bandwidthKbps bandwidth in kilobits per second
     * @return compression ratio (0.0 to 1.0)
     */
    public double getQualityCompressionRatio(long bandwidthKbps) {
        BandwidthDetectionService.BandwidthLevel level = bandwidthDetectionService.detectBandwidth(bandwidthKbps);
        
        switch (level) {
            case LOW:
                return lowBandwidthCompressionRatio;
            case MEDIUM:
                return mediumBandwidthCompressionRatio;
            case HIGH:
                return highBandwidthCompressionRatio;
            default:
                return highBandwidthCompressionRatio;
        }
    }

    /**
     * Determines if images should be prioritized or deprioritized based on bandwidth.
     * 
     * @param bandwidthKbps bandwidth in kilobits per second
     * @return true if images should be deprioritized (low bandwidth)
     */
    public boolean shouldDeprioritizeImages(long bandwidthKbps) {
        return bandwidthDetectionService.isLowBandwidth(bandwidthKbps);
    }

    /**
     * Calculates reduced image quality level (0-100) based on bandwidth.
     * 
     * @param bandwidthKbps bandwidth in kilobits per second
     * @return quality level (0-100)
     */
    public int getImageQualityLevel(long bandwidthKbps) {
        double ratio = getQualityCompressionRatio(bandwidthKbps);
        return (int) (ratio * 100);
    }

    /**
     * Calculates estimated data size after compression.
     * 
     * @param originalSizeBytes original data size in bytes
     * @param bandwidthKbps bandwidth in kilobits per second
     * @return estimated compressed size in bytes
     */
    public long getEstimatedCompressedSize(long originalSizeBytes, long bandwidthKbps) {
        double ratio = getQualityCompressionRatio(bandwidthKbps);
        return (long) (originalSizeBytes * ratio);
    }

    /**
     * Determines if content should be text-only based on bandwidth.
     * 
     * @param bandwidthKbps bandwidth in kilobits per second
     * @return true if content should be text-only
     */
    public boolean shouldUseTextOnly(long bandwidthKbps) {
        return bandwidthDetectionService.isLowBandwidth(bandwidthKbps);
    }
}
