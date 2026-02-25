package com.farmer.iot.service;

import com.farmer.iot.dto.AlertConfigurationRequest;
import com.farmer.iot.dto.AlertConfigurationResponse;
import com.farmer.iot.dto.AlertResponse;
import com.farmer.iot.entity.*;
import com.farmer.iot.repository.*;
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
 * Unit tests for threshold monitoring and alert generation.
 * Validates: Requirement 10.4
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceUnitTest {

    @Mock
    private AlertConfigurationRepository configRepository;

    @Mock
    private IotAlertRepository alertRepository;

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AlertService alertService;

    private IotDevice testDevice;
    private AlertConfiguration testConfig;
    private IotAlert testAlert;
    private AlertConfigurationRequest configRequest;

    @BeforeEach
    void setUp() {
        testDevice = IotDevice.builder()
                .id(1L)
                .deviceId("IOT-ABC12345")
                .deviceName("Soil Sensor 1")
                .status(IotDevice.DeviceStatus.ACTIVE)
                .ownerFarmerId("FARMER-001")
                .build();

        testConfig = AlertConfiguration.builder()
                .id(1L)
                .deviceId(1L)
                .farmerId("FARMER-001")
                .sensorType("SOIL_MOISTURE")
                .minThreshold(20.0)
                .maxThreshold(80.0)
                .thresholdUnit("percent")
                .alertEnabled(true)
                .notificationMethod("PUSH")
                .alertSeverity(AlertConfiguration.AlertSeverity.MEDIUM)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testAlert = IotAlert.builder()
                .id(1L)
                .deviceId(1L)
                .farmerId("FARMER-001")
                .sensorType("SOIL_MOISTURE")
                .readingValue(15.0)
                .thresholdType("MIN")
                .thresholdValue(20.0)
                .alertMessage("Alert: SOIL_MOISTURE reading (15.00 percent) below threshold (20.00 percent)")
                .alertSeverity(IotAlert.AlertSeverity.MEDIUM)
                .notificationMethod("PUSH")
                .status(IotAlert.AlertStatus.NEW)
                .alertTimestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        configRequest = AlertConfigurationRequest.builder()
                .deviceId(1L)
                .sensorType("SOIL_MOISTURE")
                .minThreshold(20.0)
                .maxThreshold(80.0)
                .thresholdUnit("percent")
                .alertEnabled(true)
                .notificationMethod("PUSH")
                .alertSeverity("MEDIUM")
                .build();
    }

    @Test
    void configureAlert_shouldCreateNewConfiguration() {
        // Given
        String farmerId = "FARMER-001";
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(configRepository.findByDeviceIdAndSensorType(1L, "SOIL_MOISTURE"))
                .thenReturn(Optional.empty());
        when(configRepository.save(any(AlertConfiguration.class))).thenAnswer(invocation -> {
            AlertConfiguration config = invocation.getArgument(0);
            config.setId(1L);
            return config;
        });

        // When
        AlertConfigurationResponse response = alertService.configureAlert(farmerId, configRequest);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getDeviceId());
        assertEquals("SOIL_MOISTURE", response.getSensorType());
        assertEquals(20.0, response.getMinThreshold());
        assertEquals(80.0, response.getMaxThreshold());
        assertTrue(response.getAlertEnabled());
        verify(configRepository, times(1)).save(any(AlertConfiguration.class));
    }

    @Test
    void configureAlert_shouldUpdateExistingConfiguration() {
        // Given
        String farmerId = "FARMER-001";
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(configRepository.findByDeviceIdAndSensorType(1L, "SOIL_MOISTURE"))
                .thenReturn(Optional.of(testConfig));
        when(configRepository.save(any(AlertConfiguration.class))).thenReturn(testConfig);

        // When
        AlertConfigurationResponse response = alertService.configureAlert(farmerId, configRequest);

        // Then
        assertNotNull(response);
        verify(configRepository, times(1)).save(any(AlertConfiguration.class));
    }

    @Test
    void configureAlert_shouldThrowExceptionWhenDeviceNotFound() {
        // Given
        String farmerId = "FARMER-001";
        configRequest.setDeviceId(999L);
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> alertService.configureAlert(farmerId, configRequest));
    }

    @Test
    void configureAlert_shouldThrowExceptionWhenDeviceNotOwnedByFarmer() {
        // Given
        String farmerId = "FARMER-999";
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

        // When/Then
        assertThrows(RuntimeException.class, () -> alertService.configureAlert(farmerId, configRequest));
    }

    @Test
    void getAlertConfigurations_shouldReturnConfigurations() {
        // Given
        Long deviceId = 1L;
        when(configRepository.findByDeviceId(deviceId)).thenReturn(Arrays.asList(testConfig));

        // When
        List<AlertConfigurationResponse> configs = alertService.getAlertConfigurations(deviceId);

        // Then
        assertNotNull(configs);
        assertEquals(1, configs.size());
        assertEquals("SOIL_MOISTURE", configs.get(0).getSensorType());
    }

    @Test
    void checkThresholdsAndGenerateAlerts_shouldGenerateAlertWhenBelowMin() {
        // Given
        SensorReading reading = SensorReading.builder()
                .id(1L)
                .deviceId(1L)
                .readingTimestamp(LocalDateTime.now())
                .soilMoisturePercent(15.0) // Below min threshold of 20.0
                .build();

        when(configRepository.findByDeviceIdAndAlertEnabled(1L, true))
                .thenReturn(Arrays.asList(testConfig));
        when(alertRepository.save(any(IotAlert.class))).thenAnswer(invocation -> {
            IotAlert alert = invocation.getArgument(0);
            alert.setId(1L);
            return alert;
        });
        doNothing().when(notificationService).sendAlertNotification(any());

        // When
        alertService.checkThresholdsAndGenerateAlerts(reading, testDevice);

        // Then
        verify(alertRepository, times(1)).save(any(IotAlert.class));
        verify(notificationService, times(1)).sendAlertNotification(any());
    }

    @Test
    void checkThresholdsAndGenerateAlerts_shouldGenerateAlertWhenAboveMax() {
        // Given
        SensorReading reading = SensorReading.builder()
                .id(1L)
                .deviceId(1L)
                .readingTimestamp(LocalDateTime.now())
                .soilMoisturePercent(85.0) // Above max threshold of 80.0
                .build();

        when(configRepository.findByDeviceIdAndAlertEnabled(1L, true))
                .thenReturn(Arrays.asList(testConfig));
        when(alertRepository.save(any(IotAlert.class))).thenAnswer(invocation -> {
            IotAlert alert = invocation.getArgument(0);
            alert.setId(1L);
            return alert;
        });
        doNothing().when(notificationService).sendAlertNotification(any());

        // When
        alertService.checkThresholdsAndGenerateAlerts(reading, testDevice);

        // Then
        verify(alertRepository, times(1)).save(any(IotAlert.class));
        verify(notificationService, times(1)).sendAlertNotification(any());
    }

    @Test
    void checkThresholdsAndGenerateAlerts_shouldNotGenerateAlertWhenWithinRange() {
        // Given
        SensorReading reading = SensorReading.builder()
                .id(1L)
                .deviceId(1L)
                .readingTimestamp(LocalDateTime.now())
                .soilMoisturePercent(50.0) // Within range 20-80
                .build();

        when(configRepository.findByDeviceIdAndAlertEnabled(1L, true))
                .thenReturn(Arrays.asList(testConfig));

        // When
        alertService.checkThresholdsAndGenerateAlerts(reading, testDevice);

        // Then
        verify(alertRepository, never()).save(any(IotAlert.class));
        verify(notificationService, never()).sendAlertNotification(any());
    }

    @Test
    void getAlertsForDevice_shouldReturnAlerts() {
        // Given
        Long deviceId = 1L;
        when(alertRepository.findByDeviceIdOrderByAlertTimestampDesc(deviceId))
                .thenReturn(Arrays.asList(testAlert));

        // When
        List<AlertResponse> alerts = alertService.getAlertsForDevice(deviceId);

        // Then
        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        assertEquals("SOIL_MOISTURE", alerts.get(0).getSensorType());
        assertEquals("NEW", alerts.get(0).getStatus());
    }

    @Test
    void getAlertsForFarmer_shouldReturnAlerts() {
        // Given
        String farmerId = "FARMER-001";
        when(alertRepository.findByFarmerIdOrderByAlertTimestampDesc(farmerId))
                .thenReturn(Arrays.asList(testAlert));

        // When
        List<AlertResponse> alerts = alertService.getAlertsForFarmer(farmerId);

        // Then
        assertNotNull(alerts);
        assertEquals(1, alerts.size());
    }

    @Test
    void acknowledgeAlert_shouldUpdateStatus() {
        // Given
        Long alertId = 1L;
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(IotAlert.class))).thenReturn(testAlert);

        // When
        AlertResponse response = alertService.acknowledgeAlert(alertId);

        // Then
        assertNotNull(response);
        verify(alertRepository, times(1)).save(argThat(alert ->
            alert.getStatus() == IotAlert.AlertStatus.ACKNOWLEDGED &&
            alert.getAcknowledgedAt() != null
        ));
    }

    @Test
    void resolveAlert_shouldUpdateStatus() {
        // Given
        Long alertId = 1L;
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(IotAlert.class))).thenReturn(testAlert);

        // When
        AlertResponse response = alertService.resolveAlert(alertId);

        // Then
        assertNotNull(response);
        verify(alertRepository, times(1)).save(argThat(alert ->
            alert.getStatus() == IotAlert.AlertStatus.RESOLVED &&
            alert.getResolvedAt() != null
        ));
    }

    @Test
    void getNewAlertCount_shouldReturnCount() {
        // Given
        String farmerId = "FARMER-001";
        when(alertRepository.countNewAlertsByFarmerId(farmerId)).thenReturn(3L);

        // When
        long count = alertService.getNewAlertCount(farmerId);

        // Then
        assertEquals(3L, count);
    }
}