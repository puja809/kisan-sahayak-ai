package com.farmer.iot.service;

import com.farmer.iot.dto.SensorReadingRequest;
import com.farmer.iot.dto.SensorReadingResponse;
import com.farmer.iot.entity.IotDevice;
import com.farmer.iot.entity.SensorReading;
import com.farmer.iot.repository.IotDeviceRepository;
import com.farmer.iot.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing sensor readings.
 * Handles data collection, storage, and retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorReadingService {

    private final SensorReadingRepository readingRepository;
    private final IotDeviceRepository deviceRepository;
    private final EncryptionService encryptionService;
    private final AlertService alertService;

    private static final int DATA_RETENTION_DAYS = 30;

    /**
     * Record a new sensor reading.
     */
    @Transactional
    public SensorReadingResponse recordReading(SensorReadingRequest request) {
        log.debug("Recording sensor reading for device: {}", request.getDeviceId());

        // Verify device exists
        IotDevice device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found: " + request.getDeviceId()));

        // Update device last seen timestamp
        device.setLastSeen(LocalDateTime.now());
        device.setStatus(IotDevice.DeviceStatus.ACTIVE);
        deviceRepository.save(device);

        // Create reading entity
        SensorReading reading = SensorReading.builder()
                .deviceId(request.getDeviceId())
                .readingTimestamp(request.getReadingTimestamp() != null ?
                        request.getReadingTimestamp() : LocalDateTime.now())
                .soilMoisturePercent(request.getSoilMoisturePercent())
                .temperatureCelsius(request.getTemperatureCelsius())
                .humidityPercent(request.getHumidityPercent())
                .phLevel(request.getPhLevel())
                .ecValue(request.getEcValue())
                .npkNitrogen(request.getNpkNitrogen())
                .npkPhosphorus(request.getNpkPhosphorus())
                .npkPotassium(request.getNpkPotassium())
                .isEncrypted(true)
                .createdAt(LocalDateTime.now())
                .build();

        SensorReading savedReading = readingRepository.save(reading);
        log.info("Sensor reading recorded for device: {}", request.getDeviceId());

        // Check thresholds and generate alerts
        alertService.checkThresholdsAndGenerateAlerts(savedReading, device);

        return SensorReadingResponse.fromEntity(savedReading);
    }

    /**
     * Get latest reading for a device.
     */
    public SensorReadingResponse getLatestReading(Long deviceId) {
        log.debug("Fetching latest reading for device: {}", deviceId);
        SensorReading reading = readingRepository.findLatestReadingByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("No readings found for device: " + deviceId));
        return SensorReadingResponse.fromEntity(reading);
    }

    /**
     * Get readings for a device within a time range.
     */
    public List<SensorReadingResponse> getReadings(Long deviceId, LocalDateTime start, LocalDateTime end) {
        log.debug("Fetching readings for device: {} between {} and {}", deviceId, start, end);
        return readingRepository.findByDeviceIdAndReadingTimestampBetween(deviceId, start, end)
                .stream()
                .map(SensorReadingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get historical readings for the past 30 days.
     */
    public List<SensorReadingResponse> getHistoricalReadings(Long deviceId) {
        log.debug("Fetching 30-day historical readings for device: {}", deviceId);
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(DATA_RETENTION_DAYS);
        return readingRepository.findHistoricalReadings(deviceId, start, end)
                .stream()
                .map(SensorReadingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get average readings for a device since a given time.
     */
    public SensorReadingResponse getAverageReadings(Long deviceId, int hoursBack) {
        log.debug("Fetching average readings for device: {} for past {} hours", deviceId, hoursBack);
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);

        Double avgSoilMoisture = readingRepository.getAverageSoilMoisture(deviceId, since);
        Double avgTemperature = readingRepository.getAverageTemperature(deviceId, since);
        Double avgHumidity = readingRepository.getAverageHumidity(deviceId, since);
        Double avgPh = readingRepository.getAveragePhLevel(deviceId, since);

        return SensorReadingResponse.builder()
                .deviceId(deviceId)
                .readingTimestamp(LocalDateTime.now())
                .soilMoisturePercent(avgSoilMoisture)
                .temperatureCelsius(avgTemperature)
                .humidityPercent(avgHumidity)
                .phLevel(avgPh)
                .isEncrypted(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Clean up old sensor data (30 days retention).
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void cleanupOldData() {
        log.info("Cleaning up sensor readings older than {} days", DATA_RETENTION_DAYS);
        LocalDateTime threshold = LocalDateTime.now().minusDays(DATA_RETENTION_DAYS);
        readingRepository.deleteByReadingTimestampBefore(threshold);
        log.info("Old sensor data cleanup completed");
    }
}