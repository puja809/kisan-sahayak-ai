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

    @Column(name = "soil_moisture_percent")
    private Double soilMoisturePercent;

    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    @Column(name = "humidity_percent")
    private Double humidityPercent;

    @Column(name = "ph_level")
    private Double phLevel;

    @Column(name = "ec_value")
    private Double ecValue;

    @Column(name = "npk_nitrogen")
    private Double npkNitrogen;

    @Column(name = "npk_phosphorus")
    private Double npkPhosphorus;

    @Column(name = "npk_potassium")
    private Double npkPotassium;

    @Column(name = "is_encrypted")
    @Builder.Default
    private Boolean isEncrypted = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}