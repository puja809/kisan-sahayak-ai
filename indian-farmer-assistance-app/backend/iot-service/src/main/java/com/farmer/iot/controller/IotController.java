package com.farmer.iot.controller;

import com.farmer.iot.dto.*;
import com.farmer.iot.entity.IotDevice;
import com.farmer.iot.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for IoT device management.
 * Provides endpoints for device provisioning, sensor data, alerts, and configuration.
 * Validates: Requirements 10.1, 10.3, 10.4, 10.5, 10.6, 10.7
 */
@RestController
@RequestMapping("/api/v1/iot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "IoT Device Management", description = "IoT device provisioning and sensor data endpoints")
public class IotController {

    private final IotDeviceService deviceService;
    private final SensorReadingService readingService;
    private final AlertService alertService;
    private final EncryptionService encryptionService;

    /**
     * Get all devices for a farmer.
     * GET /api/v1/iot/devices/{farmerId}
     * Validates: Requirement 10.1
     */
    @GetMapping("/devices/{farmerId}")
    @Operation(summary = "Get farmer devices", description = "Retrieves all IoT devices for a farmer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Devices retrieved successfully",
            content = @Content(schema = @Schema(implementation = DeviceResponse.class)))
    })
    public ResponseEntity<List<DeviceResponse>> getDevices(
            @Parameter(description = "Farmer ID") @PathVariable String farmerId) {
        log.info("Fetching devices for farmer: {}", farmerId);
        List<DeviceResponse> devices = deviceService.getDevicesByFarmerId(farmerId);
        return ResponseEntity.ok(devices);
    }

    /**
     * Provision a new device.
     * POST /api/v1/iot/devices/provision
     * Validates: Requirements 10.1, 10.2, 10.9
     */
    @PostMapping("/devices/provision")
    @Operation(summary = "Provision device", description = "Provisions a new IoT device for a farmer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Device provisioned successfully",
            content = @Content(schema = @Schema(implementation = DeviceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<DeviceResponse> provisionDevice(
            @Parameter(description = "Farmer ID header") @RequestHeader("X-Farmer-Id") String farmerId,
            @Parameter(description = "Device registration request") @Valid @RequestBody DeviceRegistrationRequest request) {
        log.info("Provisioning device for farmer: {}", farmerId);
        DeviceResponse device = deviceService.provisionDevice(farmerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(device);
    }

    /**
     * Update device configuration.
     * PUT /api/v1/iot/devices/{deviceId}/config
     * Validates: Requirement 10.7
     */
    @PutMapping("/devices/{deviceId}/config")
    @Operation(summary = "Update device config", description = "Updates configuration for an IoT device")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration updated successfully",
            content = @Content(schema = @Schema(implementation = DeviceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<DeviceResponse> updateDeviceConfig(
            @Parameter(description = "Device ID") @PathVariable Long deviceId,
            @Parameter(description = "Device configuration request") @Valid @RequestBody DeviceConfigurationRequest request) {
        log.info("Updating configuration for device: {}", deviceId);
        DeviceResponse device = deviceService.updateDeviceConfiguration(deviceId, request);
        return ResponseEntity.ok(device);
    }

    /**
     * Delete a device.
     * DELETE /api/v1/iot/devices/{deviceId}
     * Validates: Requirement 10.1
     */
    @DeleteMapping("/devices/{deviceId}")
    @Operation(summary = "Delete device", description = "Deletes an IoT device")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Device deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<Void> deleteDevice(
            @Parameter(description = "Device ID") @PathVariable Long deviceId) {
        log.info("Deleting device: {}", deviceId);
        deviceService.deleteDevice(deviceId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get latest sensor readings for a device.
     * GET /api/v1/iot/devices/{deviceId}/readings
     * Validates: Requirement 10.3
     */
    @GetMapping("/devices/{deviceId}/readings")
    @Operation(summary = "Get latest readings", description = "Retrieves latest sensor readings for a device")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Readings retrieved successfully",
            content = @Content(schema = @Schema(implementation = SensorReadingResponse.class))),
        @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<SensorReadingResponse> getLatestReadings(
            @Parameter(description = "Device ID") @PathVariable Long deviceId) {
        log.info("Fetching latest readings for device: {}", deviceId);
        SensorReadingResponse reading = readingService.getLatestReading(deviceId);
        return ResponseEntity.ok(reading);
    }

    /**
     * Get historical sensor readings for a device (past 30 days).
     * GET /api/v1/iot/devices/{deviceId}/readings/history
     * Validates: Requirement 10.5
     */
    @GetMapping("/devices/{deviceId}/readings/history")
    @Operation(summary = "Get historical readings", description = "Retrieves historical sensor readings for a device (past 30 days)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Readings retrieved successfully",
            content = @Content(schema = @Schema(implementation = SensorReadingResponse.class))),
        @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<List<SensorReadingResponse>> getHistoricalReadings(
            @Parameter(description = "Device ID") @PathVariable Long deviceId) {
        log.info("Fetching historical readings for device: {}", deviceId);
        List<SensorReadingResponse> readings = readingService.getHistoricalReadings(deviceId);
        return ResponseEntity.ok(readings);
    }

    /**
     * Record a new sensor reading.
     * POST /api/v1/iot/devices/{deviceId}/readings
     * Validates: Requirement 10.3
     */
    @PostMapping("/devices/{deviceId}/readings")
    @Operation(summary = "Record sensor reading", description = "Records a new sensor reading for a device")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Reading recorded successfully",
            content = @Content(schema = @Schema(implementation = SensorReadingResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<SensorReadingResponse> recordReading(
            @Parameter(description = "Device ID") @PathVariable Long deviceId,
            @Parameter(description = "Sensor reading request") @Valid @RequestBody SensorReadingRequest request) {
        log.info("Recording reading for device: {}", deviceId);
        request.setDeviceId(deviceId);
        SensorReadingResponse reading = readingService.recordReading(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reading);
    }

    /**
     * Configure alert thresholds for a device sensor.
     * POST /api/v1/iot/devices/{deviceId}/alerts/config
     * Validates: Requirement 10.4
     */
    @PostMapping("/devices/{deviceId}/alerts/config")
    @Operation(summary = "Configure alert", description = "Configures alert thresholds for a device sensor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Alert configured successfully",
            content = @Content(schema = @Schema(implementation = AlertConfigurationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<AlertConfigurationResponse> configureAlert(
            @Parameter(description = "Farmer ID header") @RequestHeader("X-Farmer-Id") String farmerId,
            @Parameter(description = "Device ID") @PathVariable Long deviceId,
            @Parameter(description = "Alert configuration request") @Valid @RequestBody AlertConfigurationRequest request) {
        log.info("Configuring alert for device: {}, farmer: {}", deviceId, farmerId);
        request.setDeviceId(deviceId);
        AlertConfigurationResponse config = alertService.configureAlert(farmerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }

    /**
     * Get alert configurations for a device.
     * GET /api/v1/iot/devices/{deviceId}/alerts/config
     * Validates: Requirement 10.4
     */
    @GetMapping("/devices/{deviceId}/alerts/config")
    @Operation(summary = "Get alert configurations", description = "Retrieves alert configurations for a device")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configurations retrieved successfully",
            content = @Content(schema = @Schema(implementation = AlertConfigurationResponse.class)))
    })
    public ResponseEntity<List<AlertConfigurationResponse>> getAlertConfigurations(
            @Parameter(description = "Device ID") @PathVariable Long deviceId) {
        log.info("Fetching alert configurations for device: {}", deviceId);
        List<AlertConfigurationResponse> configs = alertService.getAlertConfigurations(deviceId);
        return ResponseEntity.ok(configs);
    }

    /**
     * Get alerts for a device.
     * GET /api/v1/iot/devices/{deviceId}/alerts
     * Validates: Requirement 10.4
     */
    @GetMapping("/devices/{deviceId}/alerts")
    @Operation(summary = "Get device alerts", description = "Retrieves alerts for a specific device")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alerts retrieved successfully",
            content = @Content(schema = @Schema(implementation = AlertResponse.class)))
    })
    public ResponseEntity<List<AlertResponse>> getDeviceAlerts(
            @Parameter(description = "Device ID") @PathVariable Long deviceId) {
        log.info("Fetching alerts for device: {}", deviceId);
        List<AlertResponse> alerts = alertService.getAlertsForDevice(deviceId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alerts for a farmer.
     * GET /api/v1/iot/alerts/{farmerId}
     * Validates: Requirement 10.4
     */
    @GetMapping("/alerts/{farmerId}")
    @Operation(summary = "Get farmer alerts", description = "Retrieves all alerts for a farmer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alerts retrieved successfully",
            content = @Content(schema = @Schema(implementation = AlertResponse.class)))
    })
    public ResponseEntity<List<AlertResponse>> getFarmerAlerts(
            @Parameter(description = "Farmer ID") @PathVariable String farmerId) {
        log.info("Fetching alerts for farmer: {}", farmerId);
        List<AlertResponse> alerts = alertService.getAlertsForFarmer(farmerId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Acknowledge an alert.
     * PUT /api/v1/iot/alerts/{alertId}/acknowledge
     * Validates: Requirement 10.4
     */
    @PutMapping("/alerts/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge alert", description = "Acknowledges an alert")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alert acknowledged successfully",
            content = @Content(schema = @Schema(implementation = AlertResponse.class))),
        @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public ResponseEntity<AlertResponse> acknowledgeAlert(
            @Parameter(description = "Alert ID") @PathVariable Long alertId) {
        log.info("Acknowledging alert: {}", alertId);
        AlertResponse alert = alertService.acknowledgeAlert(alertId);
        return ResponseEntity.ok(alert);
    }

    /**
     * Resolve an alert.
     * PUT /api/v1/iot/alerts/{alertId}/resolve
     * Validates: Requirement 10.4
     */
    @PutMapping("/alerts/{alertId}/resolve")
    @Operation(summary = "Resolve alert", description = "Resolves an alert")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alert resolved successfully",
            content = @Content(schema = @Schema(implementation = AlertResponse.class))),
        @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public ResponseEntity<AlertResponse> resolveAlert(
            @Parameter(description = "Alert ID") @PathVariable Long alertId) {
        log.info("Resolving alert: {}", alertId);
        AlertResponse alert = alertService.resolveAlert(alertId);
        return ResponseEntity.ok(alert);
    }

    /**
     * Discover devices on network/Bluetooth.
     * POST /api/v1/iot/devices/discover
     * Validates: Requirement 10.1
     */
    @PostMapping("/devices/discover")
    @Operation(summary = "Discover devices", description = "Discovers devices on network/Bluetooth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Discovery completed",
            content = @Content(schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<List<String>> discoverDevices() {
        log.info("Starting device discovery");
        List<String> devices = deviceService.discoverDevices();
        return ResponseEntity.ok(devices);
    }

    /**
     * Get data ownership information.
     * GET /api/v1/iot/data-ownership
     * Validates: Requirement 10.10
     */
    @GetMapping("/data-ownership")
    @Operation(summary = "Get data ownership info", description = "Retrieves data ownership information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data ownership info retrieved")
    })
    public ResponseEntity<String> getDataOwnershipInfo() {
        log.info("Fetching data ownership information");
        return ResponseEntity.ok(encryptionService.getDataOwnershipMessage());
    }

    /**
     * Health check endpoint.
     * GET /api/v1/iot/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("IoT Service is healthy");
    }
}