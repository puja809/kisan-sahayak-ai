package com.farmer.iot.service;

import com.farmer.iot.client.DeviceDiscoveryClient;
import com.farmer.iot.dto.*;
import com.farmer.iot.entity.IotDevice;
import com.farmer.iot.repository.IotDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing IoT devices.
 * Handles device provisioning, discovery, and status tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IotDeviceService {

    private final IotDeviceRepository deviceRepository;
    private final DeviceDiscoveryClient discoveryClient;
    private final EncryptionService encryptionService;

    /**
     * Provision a new IoT device for a farmer.
     */
    @Transactional
    public DeviceResponse provisionDevice(String farmerId, DeviceRegistrationRequest request) {
        log.info("Provisioning device for farmer: {}", farmerId);

        // Generate unique device ID
        String deviceId = generateDeviceId();

        // Discover device capabilities
        String capabilities = discoveryClient.discoverDeviceCapabilities(deviceId);

        // Create device entity
        IotDevice device = IotDevice.builder()
                .deviceId(deviceId)
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .manufacturer(request.getManufacturer())
                .model(request.getModel())
                .status(IotDevice.DeviceStatus.ACTIVE)
                .lastSeen(LocalDateTime.now())
                .ownerFarmerId(farmerId)
                .farmId(request.getFarmId())
                .capabilities(capabilities)
                .dataOwnershipConfirmed(request.getDataOwnershipConfirmed() != null ?
                        request.getDataOwnershipConfirmed() : false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        IotDevice savedDevice = deviceRepository.save(device);
        log.info("Device provisioned successfully: {}", deviceId);

        return DeviceResponse.fromEntity(savedDevice);
    }

    /**
     * Get all devices for a farmer.
     */
    public List<DeviceResponse> getDevicesByFarmerId(String farmerId) {
        log.debug("Fetching devices for farmer: {}", farmerId);
        return deviceRepository.findByOwnerFarmerId(farmerId)
                .stream()
                .map(DeviceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get a device by ID.
     */
    public DeviceResponse getDeviceById(Long deviceId) {
        log.debug("Fetching device: {}", deviceId);
        IotDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));
        return DeviceResponse.fromEntity(device);
    }

    /**
     * Update device configuration.
     */
    @Transactional
    public DeviceResponse updateDeviceConfiguration(Long deviceId, DeviceConfigurationRequest request) {
        log.info("Updating configuration for device: {}", deviceId);

        IotDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

        if (request.getDeviceName() != null) {
            device.setDeviceName(request.getDeviceName());
        }
        if (request.getConfiguration() != null) {
            device.setConfiguration(request.getConfiguration());
        }
        if (request.getStatus() != null) {
            device.setStatus(IotDevice.DeviceStatus.valueOf(request.getStatus()));
        }

        device.setUpdatedAt(LocalDateTime.now());
        IotDevice savedDevice = deviceRepository.save(device);

        return DeviceResponse.fromEntity(savedDevice);
    }

    /**
     * Delete a device.
     */
    @Transactional
    public void deleteDevice(Long deviceId) {
        log.info("Deleting device: {}", deviceId);
        if (!deviceRepository.existsById(deviceId)) {
            throw new RuntimeException("Device not found: " + deviceId);
        }
        deviceRepository.deleteById(deviceId);
    }

    /**
     * Update device status (called when device sends heartbeat).
     */
    @Transactional
    public void updateDeviceStatus(Long deviceId, IotDevice.DeviceStatus status) {
        log.debug("Updating device {} status to {}", deviceId, status);
        IotDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

        device.setStatus(status);
        device.setLastSeen(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        deviceRepository.save(device);
    }

    /**
     * Get active device count for a farmer.
     */
    public long getActiveDeviceCount(String farmerId) {
        return deviceRepository.countActiveDevicesByFarmerId(farmerId);
    }

    /**
     * Discover devices on local network or via Bluetooth.
     */
    public List<String> discoverDevices() {
        log.info("Starting device discovery");
        return discoveryClient.discoverDevices();
    }

    /**
     * Establish secure connection with device.
     */
    public boolean establishSecureConnection(String deviceId) {
        log.info("Establishing secure connection with device: {}", deviceId);
        return discoveryClient.establishSecureConnection(deviceId);
    }

    /**
     * Generate a unique device ID.
     */
    private String generateDeviceId() {
        return "IOT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}