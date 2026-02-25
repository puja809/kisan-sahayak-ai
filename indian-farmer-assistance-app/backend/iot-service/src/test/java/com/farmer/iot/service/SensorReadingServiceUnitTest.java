package com.farmer.iot.service;

import com.farmer.iot.dto.SensorReadingRequest;
import com.farmer.iot.dto.SensorReadingResponse;
import com.farmer.iot.entity.IotDevice;
import com.farmer.iot.entity.SensorReading;
import com.farmer.iot.repository.IotDeviceRepository;
import com.farmer.iot.repository.SensorReadingRepository;
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
 * Unit tests for sensor data collection and management.
 * Validates: Requirements 10.3, 10.5
 */
@ExtendWith(MockitoExtension.class)
class SensorReadingServiceUnitTest {

    @Mock
    private SensorReadingRepository readingRepository;

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private SensorReadingService readingService;

    private IotDevice testDevice;
    private SensorReading testReading;
    private SensorReadingRequest readingRequest;

    @BeforeEach
    void setUp() {
        testDevice = IotDevice.builder()
                .id(1L)
                .deviceId("IOT-ABC12345")
                .deviceName("Soil Sensor 1")
                .status(IotDevice.DeviceStatus.ACTIVE)
                .ownerFarmerId("FARMER-001")
                .build();

        testReading = SensorReading.builder()
                .id(1L)
                .deviceId(1L)
                .readingTimestamp(LocalDateTime.now())
                .soilMoisturePercent(45.5)
                .temperatureCelsius(28.5)
                .humidityPercent(65.0)
                .phLevel(6.5)
                .isEncrypted(true)
                .createdAt(LocalDateTime.now())
                .build();

        readingRequest = SensorReadingRequest.builder()
                .deviceId(1L)
                .readingTimestamp(LocalDateTime.now())
                .soilMoisturePercent(45.5)
                .temperatureCelsius(28.5)
                .humidityPercent(65.0)
                .phLevel(6.5)
                .build();
    }

    @Test
    void recordReading_shouldSaveReadingAndUpdateDevice() {
        // Given
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(testDevice);
        when(readingRepository.save(any(SensorReading.class))).thenAnswer(invocation -> {
            SensorReading reading = invocation.getArgument(0);
            reading.setId(1L);
            return reading;
        });
        doNothing().when(alertService).checkThresholdsAndGenerateAlerts(any(), any());

        // When
        SensorReadingResponse response = readingService.recordReading(readingRequest);

        // Then
        assertNotNull(response);
        assertEquals(45.5, response.getSoilMoisturePercent());
        assertEquals(28.5, response.getTemperatureCelsius());
        assertEquals(65.0, response.getHumidityPercent());
        assertEquals(6.5, response.getPhLevel());
        assertTrue(response.getIsEncrypted());

        verify(deviceRepository, times(1)).save(any(IotDevice.class));
        verify(readingRepository, times(1)).save(any(SensorReading.class));
        verify(alertService, times(1)).checkThresholdsAndGenerateAlerts(any(), any());
    }

    @Test
    void recordReading_shouldThrowExceptionWhenDeviceNotFound() {
        // Given
        readingRequest.setDeviceId(999L);
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> readingService.recordReading(readingRequest));
    }

    @Test
    void getLatestReading_shouldReturnLatestReading() {
        // Given
        Long deviceId = 1L;
        when(readingRepository.findLatestReadingByDeviceId(deviceId)).thenReturn(Optional.of(testReading));

        // When
        SensorReadingResponse response = readingService.getLatestReading(deviceId);

        // Then
        assertNotNull(response);
        assertEquals(deviceId, response.getDeviceId());
        assertEquals(45.5, response.getSoilMoisturePercent());
    }

    @Test
    void getLatestReading_shouldThrowExceptionWhenNoReadings() {
        // Given
        Long deviceId = 1L;
        when(readingRepository.findLatestReadingByDeviceId(deviceId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> readingService.getLatestReading(deviceId));
    }

    @Test
    void getReadings_shouldReturnReadingsInTimeRange() {
        // Given
        Long deviceId = 1L;
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(readingRepository.findByDeviceIdAndReadingTimestampBetween(deviceId, start, end))
                .thenReturn(Arrays.asList(testReading));

        // When
        List<SensorReadingResponse> readings = readingService.getReadings(deviceId, start, end);

        // Then
        assertNotNull(readings);
        assertEquals(1, readings.size());
    }

    @Test
    void getHistoricalReadings_shouldReturn30DayHistory() {
        // Given
        Long deviceId = 1L;
        when(readingRepository.findHistoricalReadings(eq(deviceId), any(), any()))
                .thenReturn(Arrays.asList(testReading));

        // When
        List<SensorReadingResponse> readings = readingService.getHistoricalReadings(deviceId);

        // Then
        assertNotNull(readings);
        assertEquals(1, readings.size());
        verify(readingRepository, times(1)).findHistoricalReadings(eq(deviceId), any(), any());
    }

    @Test
    void getAverageReadings_shouldReturnAverages() {
        // Given
        Long deviceId = 1L;
        int hoursBack = 24;
        when(readingRepository.getAverageSoilMoisture(deviceId, any())).thenReturn(45.0);
        when(readingRepository.getAverageTemperature(deviceId, any())).thenReturn(28.0);
        when(readingRepository.getAverageHumidity(deviceId, any())).thenReturn(65.0);
        when(readingRepository.getAveragePhLevel(deviceId, any())).thenReturn(6.5);

        // When
        SensorReadingResponse response = readingService.getAverageReadings(deviceId, hoursBack);

        // Then
        assertNotNull(response);
        assertEquals(45.0, response.getSoilMoisturePercent());
        assertEquals(28.0, response.getTemperatureCelsius());
        assertEquals(65.0, response.getHumidityPercent());
        assertEquals(6.5, response.getPhLevel());
    }

    @Test
    void recordReading_shouldUpdateDeviceStatusToActive() {
        // Given
        testDevice.setStatus(IotDevice.DeviceStatus.OFFLINE);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(testDevice);
        when(readingRepository.save(any(SensorReading.class))).thenReturn(testReading);
        doNothing().when(alertService).checkThresholdsAndGenerateAlerts(any(), any());

        // When
        readingService.recordReading(readingRequest);

        // Then
        verify(deviceRepository, times(1)).save(argThat(device ->
            device.getStatus() == IotDevice.DeviceStatus.ACTIVE
        ));
    }
}