package com.farmer.iot.dto;

import com.farmer.iot.entity.SensorReading;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for sensor reading response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReadingResponse {

    private Long id;
    private Long deviceId;
    private LocalDateTime readingTimestamp;
    private Double soilMoisturePercent;
    private Double temperatureCelsius;
    private Double humidityPercent;
    private Double phLevel;
    private Double ecValue;
    private Double npkNitrogen;
    private Double npkPhosphorus;
    private Double npkPotassium;
    private Boolean isEncrypted;
    private LocalDateTime createdAt;

    public static SensorReadingResponse fromEntity(SensorReading reading) {
        return SensorReadingResponse.builder()
                .id(reading.getId())
                .deviceId(reading.getDeviceId())
                .readingTimestamp(reading.getReadingTimestamp())
                .soilMoisturePercent(reading.getSoilMoisturePercent())
                .temperatureCelsius(reading.getTemperatureCelsius())
                .humidityPercent(reading.getHumidityPercent())
                .phLevel(reading.getPhLevel())
                .ecValue(reading.getEcValue())
                .npkNitrogen(reading.getNpkNitrogen())
                .npkPhosphorus(reading.getNpkPhosphorus())
                .npkPotassium(reading.getNpkPotassium())
                .isEncrypted(reading.getIsEncrypted())
                .createdAt(reading.getCreatedAt())
                .build();
    }
}