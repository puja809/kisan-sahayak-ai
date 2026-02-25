package com.farmer.iot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing sensor readings from IoT devices.
 * Stores time-series data for soil moisture, temperature, humidity, pH, etc.
 */
@Entity
@Table(name = "sensor_readings", indexes = {
    @Index(name = "idx_device_timestamp", columnList = "device_id, reading_timestamp"),
    @Index(name = "idx_timestamp", columnList = "reading_timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "reading_timestamp", nullable = false)
    private LocalDateTime readingTimestamp;

    @Column(name = "soil_moisture_percent", precision = 5, scale = 2)
    private Double soilMoisturePercent;

    @Column(name = "temperature_celsius", precision = 5, scale = 2)
    private Double temperatureCelsius;

    @Column(name = "humidity_percent", precision = 5, scale = 2)
    private Double humidityPercent;

    @Column(name = "ph_level", precision = 4, scale = 2)
    private Double phLevel;

    @Column(name = "ec_value", precision = 6, scale = 2)
    private Double ecValue;

    @Column(name = "npk_nitrogen", precision = 6, scale = 2)
    private Double npkNitrogen;

    @Column(name = "npk_phosphorus", precision = 6, scale = 2)
    private Double npkPhosphorus;

    @Column(name = "npk_potassium", precision = 6, scale = 2)
    private Double npkPotassium;

    @Column(name = "is_encrypted")
    @Builder.Default
    private Boolean isEncrypted = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}