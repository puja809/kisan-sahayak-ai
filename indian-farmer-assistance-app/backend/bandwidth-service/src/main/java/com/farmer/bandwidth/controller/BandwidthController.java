package com.farmer.bandwidth.controller;

import com.farmer.bandwidth.service.AdaptiveQualityService;
import com.farmer.bandwidth.service.AutomaticQualityAdjustmentService;
import com.farmer.bandwidth.service.BandwidthDetectionService;
import com.farmer.bandwidth.service.DataUsageMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for bandwidth optimization endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bandwidth")
@Tag(name = "Bandwidth Management", description = "Bandwidth detection and optimization endpoints")
public class BandwidthController {

    private final BandwidthDetectionService bandwidthDetectionService;
    private final AdaptiveQualityService adaptiveQualityService;
    private final DataUsageMonitoringService dataUsageMonitoringService;
    private final AutomaticQualityAdjustmentService automaticQualityAdjustmentService;

    public BandwidthController(
            BandwidthDetectionService bandwidthDetectionService,
            AdaptiveQualityService adaptiveQualityService,
            DataUsageMonitoringService dataUsageMonitoringService,
            AutomaticQualityAdjustmentService automaticQualityAdjustmentService) {
        this.bandwidthDetectionService = bandwidthDetectionService;
        this.adaptiveQualityService = adaptiveQualityService;
        this.dataUsageMonitoringService = dataUsageMonitoringService;
        this.automaticQualityAdjustmentService = automaticQualityAdjustmentService;
    }

    /**
     * Detects and classifies bandwidth level.
     * 
     * @param bandwidthKbps bandwidth in kilobits per second
     * @return bandwidth classification response
     */
    @PostMapping("/detect")
    @Operation(summary = "Detect bandwidth level", description = "Detects and classifies bandwidth level based on speed")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bandwidth detected successfully",
            content = @Content(schema = @Schema(implementation = Map.class)))
    })
    public ResponseEntity<Map<String, Object>> detectBandwidth(
            @Parameter(description = "Bandwidth in kilobits per second") @RequestParam long bandwidthKbps) {
        BandwidthDetectionService.BandwidthLevel level = bandwidthDetectionService.detectBandwidth(bandwidthKbps);
        
        Map<String, Object> response = new HashMap<>();
        response.put("bandwidthKbps", bandwidthKbps);
        response.put("level", level);
        response.put("isLow", bandwidthDetectionService.isLowBandwidth(bandwidthKbps));
        response.put("lowThresholdKbps", bandwidthDetectionService.getLowBandwidthThreshold());
        response.put("mediumThresholdKbps", bandwidthDetectionService.getMediumBandwidthThreshold());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets adaptive quality settings based on bandwidth.
     * 
     * @param bandwidthKbps bandwidth in kilobits per second
     * @return quality settings response
     */
    @PostMapping("/quality")
    @Operation(summary = "Get quality settings", description = "Retrieves adaptive quality settings based on bandwidth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quality settings retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class)))
    })
    public ResponseEntity<Map<String, Object>> getQualitySettings(
            @Parameter(description = "Bandwidth in kilobits per second") @RequestParam long bandwidthKbps) {
        double compressionRatio = adaptiveQualityService.getQualityCompressionRatio(bandwidthKbps);
        int qualityLevel = adaptiveQualityService.getImageQualityLevel(bandwidthKbps);
        
        Map<String, Object> response = new HashMap<>();
        response.put("bandwidthKbps", bandwidthKbps);
        response.put("compressionRatio", compressionRatio);
        response.put("qualityLevel", qualityLevel);
        response.put("deprioritizeImages", adaptiveQualityService.shouldDeprioritizeImages(bandwidthKbps));
        response.put("textOnly", adaptiveQualityService.shouldUseTextOnly(bandwidthKbps));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Calculates estimated compressed size.
     * 
     * @param originalSizeBytes original size in bytes
     * @param bandwidthKbps bandwidth in kilobits per second
     * @return compression estimate response
     */
    @PostMapping("/estimate-compression")
    @Operation(summary = "Estimate compression", description = "Calculates estimated compressed size based on bandwidth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Compression estimate calculated successfully",
            content = @Content(schema = @Schema(implementation = Map.class)))
    })
    public ResponseEntity<Map<String, Object>> estimateCompression(
            @Parameter(description = "Original size in bytes") @RequestParam long originalSizeBytes,
            @Parameter(description = "Bandwidth in kilobits per second") @RequestParam long bandwidthKbps) {
        long compressedSize = adaptiveQualityService.getEstimatedCompressedSize(originalSizeBytes, bandwidthKbps);
        double savingsPercent = ((double) (originalSizeBytes - compressedSize) / originalSizeBytes) * 100;
        
        Map<String, Object> response = new HashMap<>();
        response.put("originalSizeBytes", originalSizeBytes);
        response.put("compressedSizeBytes", compressedSize);
        response.put("savingsBytes", originalSizeBytes - compressedSize);
        response.put("savingsPercent", savingsPercent);
        response.put("bandwidthKbps", bandwidthKbps);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets data usage information.
     * 
     * @param usedBytes bytes used so far
     * @param limitMb data limit in MB
     * @return data usage response
     */
    @PostMapping("/data-usage")
    @Operation(summary = "Get data usage info", description = "Retrieves data usage information and suggestions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data usage info retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class)))
    })
    public ResponseEntity<Map<String, Object>> getDataUsageInfo(
            @Parameter(description = "Bytes used so far") @RequestParam long usedBytes,
            @Parameter(description = "Data limit in MB") @RequestParam(defaultValue = "100") long limitMb) {
        DataUsageMonitoringService.DataUsageTracker tracker = dataUsageMonitoringService.createTracker(limitMb);
        tracker.addUsage(usedBytes);
        
        Map<String, Object> response = new HashMap<>();
        response.put("usedBytes", tracker.getTotalUsedBytes());
        response.put("limitBytes", tracker.getLimitBytes());
        response.put("usagePercentage", tracker.getUsagePercentage());
        response.put("remainingBytes", tracker.getRemainingBytes());
        response.put("hasExceeded", tracker.hasExceededLimit());
        response.put("isNearLimit", tracker.isNearLimit(dataUsageMonitoringService.getWarningThresholdPercent()));
        response.put("suggestions", dataUsageMonitoringService.getDataSavingSuggestions(tracker));
        
        return ResponseEntity.ok(response);
    }
}
