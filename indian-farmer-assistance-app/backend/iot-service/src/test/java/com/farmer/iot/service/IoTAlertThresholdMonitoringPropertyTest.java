package com.farmer.iot.service;

import com.farmer.iot.entity.AlertConfiguration;
import com.farmer.iot.entity.IotDevice;
import com.farmer.iot.entity.SensorReading;
import com.farmer.iot.repository.AlertConfigurationRepository;
import com.farmer.iot.repository.IotAlertRepository;
import com.farmer.iot.repository.IotDeviceRepository;
import net.jqwik.api.*;
import net.jqwik.junit5.JqwikTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for IoT Alert Threshold Monitoring.
 * Feature: indian-farmer-assistance-app, Property 18: IoT Alert Threshold Monitoring
 * Validates: Requirements 10.4
 *
 * For any sensor reading where a measured value exceeds the configured threshold
 * for that sensor type, the system should generate an alert, and no alert should
 * be generated when all values are within their respective thresholds.
 */
@ExtendWith(MockitoExtension.class)
@JqwikTest
class IoTAlertThresholdMonitoringPropertyTest {

    @Mock
    private AlertConfigurationRepository configRepository;

    @Mock
    private IotAlertRepository alertRepository;

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private NotificationService notificationService;

    private AlertService alertService;
    private IotDevice testDevice;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(
                configRepository, alertRepository, deviceRepository, notificationService);

        testDevice = IotDevice.builder()
                .id(1L)
                .deviceId("IOT-TEST123")
                .deviceName("Test Sensor")
                .status(IotDevice.DeviceStatus.ACTIVE)
                .ownerFarmerId("FARMER-TEST")
                .build();
    }

    /**
     * Property: Alert generation when sensor value is below minimum threshold.
     * For any sensor reading where value < minThreshold, an alert should be generated.
     */
    @Property
    void alertGeneratedWhenValueBelowMinThreshold(
            @ForAll("sensorTypes") String sensorType,
            @ForAll("validSensorValues") Double readingValue,
            @ForAll("minThresholds") Double minThreshold) {
        // Ensure reading is below minimum threshold
        Assume.that(readingValue < minThreshold);

        // Setup configuration
        AlertConfiguration config = createConfig(sensorType, minThreshold, null);
        when(configRepository.findByDeviceIdAndAlertEnabled(1L, true))
                .thenReturn(List.of(config));
        when(alertRepository.save(any())).thenAnswer(invocation -> {
            IotAlert alert = invocation.getArgument(0);
            alert.setId(1L);
            return alert;
        });
        doNothing().when(notificationService).sendAlertNotification(any());

        // Create reading
        SensorReading reading = createReading(sensorType, readingValue);

        // Execute
        alertService.checkThresholdsAndGenerateAlerts(reading, testDevice);

        // Verify alert was generated
        verify(alertRepository, times(1)).save(argThat(alert ->
            alert.getThresholdType().equals("MIN") &&
            alert.getReadingValue().equals(readingValue) &&
            alert.getThresholdValue().equals(minThreshold)
        ));
    }

    /**
     * Property: Alert generation when sensor value is above maximum threshold.
     * For any sensor reading where value > maxThreshold, an alert should be generated.
     */
    @Property
    void alertGeneratedWhenValueAboveMaxThreshold(
            @ForAll("sensorTypes") String sensorType,
            @ForAll("validSensorValues") Double readingValue,
            @ForAll("maxThresholds") Double maxThreshold) {
        // Ensure reading is above maximum threshold
        Assume.that(readingValue > maxThreshold);

        // Setup configuration
        AlertConfiguration config = createConfig(sensorType, null, maxThreshold);
        when(configRepository.findByDeviceIdAndAlertEnabled(1L, true))
                .thenReturn(List.of(config));
        when(alertRepository.save(any())).thenAnswer(invocation -> {
            IotAlert alert = invocation.getArgument(0);
            alert.setId(1L);
            return alert;
        });
        doNothing().when(notificationService).sendAlertNotification(any());

        // Create reading
        SensorReading reading = createReading(sensorType, readingValue);

        // Execute
        alertService.checkThresholdsAndGenerateAlerts(reading, testDevice);

        // Verify alert was generated
        verify(alertRepository, times(1)).save(argThat(alert ->
            alert.getThresholdType().equals("MAX") &&
            alert.getReadingValue().equals(readingValue) &&
            alert.getThresholdValue().equals(maxThreshold)
        ));
    }

    /**
     * Property: No alert when sensor value is within threshold range.
     * For any sensor reading where minThreshold <= value <= maxThreshold,
     * no alert should be generated.
     */
    @Property
    void noAlertWhenValueWithinThresholds(
            @ForAll("sensorTypes") String sensorType,
            @ForAll("validSensorValues") Double readingValue,
            @ForAll("minThresholds") Double minThreshold,
            @ForAll("maxThresholds") Double maxThreshold) {
        // Ensure min < max and reading is within range
        Assume.that(minThreshold < maxThreshold);
        Assume.that(readingValue >= minThreshold && readingValue <= maxThreshold);

        // Setup configuration
        AlertConfiguration config = createConfig(sensorType, minThreshold, maxThreshold);
        when(configRepository.findByDeviceIdAndAlertEnabled(1L, true))
                .thenReturn(List.of(config));

        // Create reading
        SensorReading reading = createReading(sensorType, readingValue);

        // Execute
        alertService.checkThresholdsAndGenerateAlerts(reading, testDevice);

        // Verify no alert was generated
        verify(alertRepository, never()).save(any());
    }

    /**
     * Property: No alert when no configurations exist.
     * For any sensor reading with no alert configurations, no alert should be generated.
     */
    @Property
    void noAlertWhenNoConfigurations(@ForAll("validSensorValues") Double readingValue) {
        // Setup - no configurations
        when(configRepository.findByDeviceIdAndAlertEnabled(1L, true))
                .thenReturn(Collections.emptyList());

        // Create reading
        SensorReading reading = SensorReading.builder()
                .id(1L)
                .deviceId(1L)
                .readingTimestamp(LocalDateTime.now())
                .soilMoisturePercent(readingValue)
                .build();

        // Execute
        alertService.checkThresholdsAndGenerateAlerts(reading, testDevice);

        // Verify no alert was generated
        verify(alertRepository, never()).save(any());
    }

    /**
     * Property: Alert severity is set correctly based on threshold deviation.
     * The alert severity should be determined by the configuration.
     */
    @Property
    void alertSeverityMatchesConfiguration(
            @ForAll("sensorTypes") String sensorType,
            @ForAll("validSensorValues") Double readingValue,
            @ForAll("minThresholds") Double minThreshold,
            @ForAll("alertSeverities") AlertConfiguration.AlertSeverity severity) {
        // Ensure reading is below minimum threshold
        Assume.that(readingValue < minThreshold);

        // Setup configuration with specific severity
        AlertConfiguration config = createConfigWithSeverity(sensorType, minThreshold, null, severity);
        when(configRepository.findByDeviceIdAndAlertEnabled(1L, true))
                .thenReturn(List.of(config));
        when(alertRepository.save(any())).thenAnswer(invocation -> {
            IotAlert alert = invocation.getArgument(0);
            alert.setId(1L);
            return alert;
        });
        doNothing().when(notificationService).sendAlertNotification(any());

        // Create reading
        SensorReading reading = createReading(sensorType, readingValue);

        // Execute
        alertService.checkThresholdsAndGenerateAlerts(reading, testDevice);

        // Verify alert severity matches
        verify(alertRepository, times(1)).save(argThat(alert ->
            alert.getAlertSeverity() == severity
        ));
    }

    // Helper methods for generating test data

    private AlertConfiguration createConfig(String sensorType, Double min, Double max) {
        return AlertConfiguration.builder()
                .id(1L)
                .deviceId(1L)
                .farmerId("FARMER-TEST")
                .sensorType(sensorType)
                .minThreshold(min)
                .maxThreshold(max)
                .thresholdUnit("percent")
                .alertEnabled(true)
                .notificationMethod("PUSH")
                .alertSeverity(AlertConfiguration.AlertSeverity.MEDIUM)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AlertConfiguration createConfigWithSeverity(String sensorType, Double min, Double max,
                                                         AlertConfiguration.AlertSeverity severity) {
        return AlertConfiguration.builder()
                .id(1L)
                .deviceId(1L)
                .farmerId("FARMER-TEST")
                .sensorType(sensorType)
                .minThreshold(min)
                .maxThreshold(max)
                .thresholdUnit("percent")
                .alertEnabled(true)
                .notificationMethod("PUSH")
                .alertSeverity(severity)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private SensorReading createReading(String sensorType, Double value) {
        SensorReading.SensorReadingBuilder builder = SensorReading.builder()
                .id(1L)
                .deviceId(1L)
                .readingTimestamp(LocalDateTime.now());

        switch (sensorType.toUpperCase()) {
            case "SOIL_MOISTURE" -> builder.soilMoisturePercent(value);
            case "TEMPERATURE" -> builder.temperatureCelsius(value);
            case "HUMIDITY" -> builder.humidityPercent(value);
            case "PH" -> builder.phLevel(value);
            case "EC" -> builder.ecValue(value);
            case "NPK_NITROGEN" -> builder.npkNitrogen(value);
            case "NPK_PHOSPHORUS" -> builder.npkPhosphorus(value);
            case "NPK_POTASSIUM" -> builder.npkPotassium(value);
        }

        return builder.build();
    }

    // Generators for property-based testing

    @Provide
    static Arbitrary<String> sensorTypes() {
        return Arbitraries.of("SOIL_MOISTURE", "TEMPERATURE", "HUMIDITY", "PH", "EC",
                "NPK_NITROGEN", "NPK_PHOSPHORUS", "NPK_POTASSIUM");
    }

    @Provide
    static Arbitrary<Double> validSensorValues() {
        return Arbitraries.doubles()
                .between(0.0, 100.0)
                .withScale(2);
    }

    @Provide
    static Arbitrary<Double> minThresholds() {
        return Arbitraries.doubles()
                .between(10.0, 40.0)
                .withScale(2);
    }

    @Provide
    static Arbitrary<Double> maxThresholds() {
        return Arbitraries.doubles()
                .between(60.0, 90.0)
                .withScale(2);
    }

    @Provide
    static Arbitrary<AlertConfiguration.AlertSeverity> alertSeverities() {
        return Arbitraries.of(AlertConfiguration.AlertSeverity.values());
    }
}