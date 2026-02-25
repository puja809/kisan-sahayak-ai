package com.farmer.weather.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for caching weather data from IMD API.
 * Stores weather data to support offline functionality and reduce API calls.
 */
@Entity
@Table(name = "weather_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String district;

    @Column(nullable = false, length = 50)
    private String state;

    @Column(name = "forecast_type", nullable = false, length = 20)
    private String forecastType;

    @Column(name = "cached_data", columnDefinition = "TEXT")
    private String cachedData;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(length = 50)
    private String source;

    @PrePersist
    protected void onCreate() {
        if (fetchedAt == null) {
            fetchedAt = LocalDateTime.now();
        }
        if (source == null) {
            source = "IMD";
        }
    }
}