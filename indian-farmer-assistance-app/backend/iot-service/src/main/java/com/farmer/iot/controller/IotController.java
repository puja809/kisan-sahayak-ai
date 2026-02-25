package com.farmer.iot.controller;

import com.farmer.iot.dto.*;
import com.farmer.iot.entity.IotDevice;
import com.farmer.iot.service.*;
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
    public ResponseEntity<List<DeviceResponse>> getDevices(@PathVariable String farmerId) {
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
    public ResponseEntity<DeviceResponse> provisionDevice(
            @RequestHeader("X-Farmer-Id") String farmerId,
            @Valid @RequestBody DeviceRegistrationRequest request) {
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
    public ResponseEntity<DeviceResponse> updateDeviceConfig(
            @PathVariable Long deviceId,
            @Valid @RequestBody DeviceConfigurationRequest request) {
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
    public ResponseEntity<Void> deleteDevice(@PathVariable Long deviceId) {
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
    public ResponseEntity<SensorReadingResponse> getLatestReadings(@PathVariable Long deviceId) {
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
    public ResponseEntity<List<SensorReadingResponse>> getHistoricalReadings(@PathVariable Long deviceId) {
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
    public ResponseEntity<SensorReadingResponse> recordReading(
            @PathVariable Long deviceId,
            @Valid @RequestBody SensorReadingRequest request) {
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
    public ResponseEntity<AlertConfigurationResponse> configureAlert(
            @RequestHeader("X-Farmer-Id") String farmerId,
            @PathVariable Long deviceId,
            @Valid @RequestBody AlertConfigurationRequest request) {
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
    public ResponseEntity<List<AlertConfigurationResponse>> getAlertConfigurations(@PathVariable Long deviceId) {
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
    public ResponseEntity<List<AlertResponse>> getDeviceAlerts(@PathVariable Long deviceId) {
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
    public ResponseEntity<List<AlertResponse>> getFarmerAlerts(@PathVariable String farmerId) {
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
    public ResponseEntity<AlertResponse> acknowledgeAlert(@PathVariable Long alertId) {
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
    public ResponseEntity<AlertResponse> resolveAlert(@PathVariable Long alertId) {
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