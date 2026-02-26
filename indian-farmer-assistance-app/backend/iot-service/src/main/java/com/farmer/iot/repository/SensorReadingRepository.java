package com.farmer.iot.repository;

import com.farmer.iot.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for sensor reading operations.
 */
@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findByDeviceIdOrderByReadingTimestampDesc(Long deviceId);

    List<SensorReading> findByDeviceIdAndReadingTimestampBetween(
            Long deviceId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s FROM SensorReading s WHERE s.deviceId = :deviceId " +
           "ORDER BY s.readingTimestamp DESC LIMIT 1")
    Optional<SensorReading> findLatestReadingByDeviceId(@Param("deviceId") Long deviceId);

    @Query("SELECT s FROM SensorReading s WHERE s.deviceId = :deviceId " +
           "AND s.readingTimestamp >= :since ORDER BY s.readingTimestamp ASC")
    List<SensorReading> findReadingsSince(@Param("deviceId") Long deviceId,
                                          @Param("since") LocalDateTime since);

    @Query("SELECT s FROM SensorReading s WHERE s.deviceId = :deviceId " +
           "AND s.readingTimestamp BETWEEN :start AND :end " +
           "ORDER BY s.readingTimestamp ASC")
    List<SensorReading> findHistoricalReadings(@Param("deviceId") Long deviceId,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Query("SELECT AVG(s.soilMoisturePercent) FROM SensorReading s " +
           "WHERE s.deviceId = :deviceId AND s.readingTimestamp >= :since")
    Double getAverageSoilMoisture(@Param("deviceId") Long deviceId,
                                  @Param("since") LocalDateTime since);

    @Query("SELECT AVG(s.temperatureCelsius) FROM SensorReading s " +
           "WHERE s.deviceId = :deviceId AND s.readingTimestamp >= :since")
    Double getAverageTemperature(@Param("deviceId") Long deviceId,
                                 @Param("since") LocalDateTime since);

    @Query("SELECT AVG(s.humidityPercent) FROM SensorReading s " +
           "WHERE s.deviceId = :deviceId AND s.readingTimestamp >= :since")
    Double getAverageHumidity(@Param("deviceId") Long deviceId,
                              @Param("since") LocalDateTime since);

    @Query("SELECT AVG(s.phLevel) FROM SensorReading s " +
           "WHERE s.deviceId = :deviceId AND s.readingTimestamp >= :since")
    Double getAveragePhLevel(@Param("deviceId") Long deviceId,
                             @Param("since") LocalDateTime since);

    void deleteByDeviceIdAndReadingTimestampBefore(Long deviceId, LocalDateTime threshold);

    void deleteByReadingTimestampBefore(LocalDateTime threshold);
}