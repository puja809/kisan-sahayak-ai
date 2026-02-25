package com.farmer.iot.service;

import com.farmer.iot.client.DeviceDiscoveryClient;
import com.farmer.iot.dto.DeviceRegistrationRequest;
import com.farmer.iot.dto.DeviceResponse;
import com.farmer.iot.entity.IotDevice;
import com.farmer.iot.repository.IotDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IoT device provisioning and management.
 * Validates: Requirements 10.1, 10.2, 10.6, 10.9
 */
@ExtendWith(MockitoExtension.class)
class IotDeviceServiceUnitTest {

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private DeviceDiscoveryClient discoveryClient;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private IotDeviceService deviceService;

    private IotDevice testDevice;
    private DeviceRegistrationRequest registrationRequest;

    @BeforeEach
    void setUp() {
        testDevice = IotDevice.builder()
                .id(1L)
                .deviceId("IOT-ABC12345")
                .deviceName("Soil Sensor 1")
                .deviceType("MULTI_SENSOR")
                .manufacturer("AgriTech")
                .model("AT-100")
                .status(IotDevice.DeviceStatus.ACTIVE)
                .ownerFarmerId("FARMER-001")
                .farmId(1L)
                .capabilities("{\"sensors\":[\"soil_moisture\",\"temperature\"]}")
                .dataOwnershipConfirmed(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        registrationRequest = DeviceRegistrationRequest.builder()
                .deviceName("Soil Sensor 1")
                .deviceType("MULTI_SENSOR")
                .manufacturer("AgriTech")
                .model("AT-100")
                .farmId(1L)
                .dataOwnershipConfirmed(true)
                .build();
    }

    @Test
    void provisionDevice_shouldCreateNewDevice() {
        // Given
        String farmerId = "FARMER-001";
        when(discoveryClient.discoverDeviceCapabilities(any())).thenReturn("{\"sensors\":[\"soil_moisture\"]}");
        when(deviceRepository.save(any(IotDevice.class))).thenAnswer(invocation -> {
            IotDevice device = invocation.getArgument(0);
            device.setId(1L);
            return device;
        });

        // When
        DeviceResponse response = deviceService.provisionDevice(farmerId, registrationRequest);

        // Then
        assertNotNull(response);
        assertEquals("Soil Sensor 1", response.getDeviceName());
        assertEquals("MULTI_SENSOR", response.getDeviceType());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(farmerId, response.getOwnerFarmerId());
        verify(discoveryClient, times(1)).discoverDeviceCapabilities(any());
        verify(deviceRepository, times(1)).save(any(IotDevice.class));
    }

    @Test
    void getDevicesByFarmerId_shouldReturnDevices() {
        // Given
        String farmerId = "FARMER-001";
        when(deviceRepository.findByOwnerFarmerId(farmerId)).thenReturn(Arrays.asList(testDevice));

        // When
        List<DeviceResponse> devices = deviceService.getDevicesByFarmerId(farmerId);

        // Then
        assertNotNull(devices);
        assertEquals(1, devices.size());
        assertEquals("IOT-ABC12345", devices.get(0).getDeviceId());
    }

    @Test
    void getDeviceById_shouldReturnDevice() {
        // Given
        Long deviceId = 1L;
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(testDevice));

        // When
        DeviceResponse response = deviceService.getDeviceById(deviceId);

        // Then
        assertNotNull(response);
        assertEquals(deviceId, response.getId());
        assertEquals("Soil Sensor 1", response.getDeviceName());
    }

    @Test
    void getDeviceById_shouldThrowExceptionWhenNotFound() {
        // Given
        Long deviceId = 999L;
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> deviceService.getDeviceById(deviceId));
    }

    @Test
    void updateDeviceConfiguration_shouldUpdateDevice() {
        // Given
        Long deviceId = 1L;
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(testDevice);

        // When
        DeviceResponse response = deviceService.updateDeviceConfiguration(deviceId,
                com.farmer.iot.dto.DeviceConfigurationRequest.builder()
                        .deviceName("Updated Sensor Name")
                        .build());

        // Then
        assertNotNull(response);
        verify(deviceRepository, times(1)).save(any(IotDevice.class));
    }

    @Test
    void deleteDevice_shouldDeleteDevice() {
        // Given
        Long deviceId = 1L;
        when(deviceRepository.existsById(deviceId)).thenReturn(true);
        doNothing().when(deviceRepository).deleteById(deviceId);

        // When
        deviceService.deleteDevice(deviceId);

        // Then
        verify(deviceRepository, times(1)).deleteById(deviceId);
    }

    @Test
    void deleteDevice_shouldThrowExceptionWhenNotFound() {
        // Given
        Long deviceId = 999L;
        when(deviceRepository.existsById(deviceId)).thenReturn(false);

        // When/Then
        assertThrows(RuntimeException.class, () -> deviceService.deleteDevice(deviceId));
    }

    @Test
    void updateDeviceStatus_shouldUpdateStatusAndLastSeen() {
        // Given
        Long deviceId = 1L;
        IotDevice.DeviceStatus newStatus = IotDevice.DeviceStatus.OFFLINE;
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(testDevice);

        // When
        deviceService.updateDeviceStatus(deviceId, newStatus);

        // Then
        verify(deviceRepository, times(1)).save(argThat(device ->
            device.getStatus() == newStatus && device.getLastSeen() != null
        ));
    }

    @Test
    void getActiveDeviceCount_shouldReturnCount() {
        // Given
        String farmerId = "FARMER-001";
        when(deviceRepository.countActiveDevicesByFarmerId(farmerId)).thenReturn(5L);

        // When
        long count = deviceService.getActiveDeviceCount(farmerId);

        // Then
        assertEquals(5L, count);
    }

    @Test
    void discoverDevices_shouldReturnDiscoveredDevices() {
        // Given
        when(discoveryClient.discoverDevices()).thenReturn(Arrays.asList("DEVICE-1", "DEVICE-2"));

        // When
        List<String> devices = deviceService.discoverDevices();

        // Then
        assertNotNull(devices);
        assertEquals(2, devices.size());
        verify(discoveryClient, times(1)).discoverDevices();
    }

    @Test
    void establishSecureConnection_shouldReturnTrue() {
        // Given
        String deviceId = "IOT-ABC12345";
        when(discoveryClient.discoverDeviceCapabilities(deviceId)).thenReturn("{\"sensors\":[]}");
        when(discoveryClient.establishSecureConnection(deviceId)).thenReturn(true);

        // When
        boolean result = deviceService.establishSecureConnection(deviceId);

        // Then
        assertTrue(result);
        verify(discoveryClient, times(1)).establishSecureConnection(deviceId);
    }
}