package com.farmer.iot.repository;

import com.farmer.iot.entity.IotAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for IoT alert operations.
 */
@Repository
public interface IotAlertRepository extends JpaRepository<IotAlert, Long> {

    List<IotAlert> findByDeviceIdOrderByAlertTimestampDesc(Long deviceId);

    List<IotAlert> findByFarmerIdOrderByAlertTimestampDesc(String farmerId);

    List<IotAlert> findByFarmerIdAndStatus(String farmerId, IotAlert.AlertStatus status);

    List<IotAlert> findByDeviceIdAndStatus(Long deviceId, IotAlert.AlertStatus status);

    @Query("SELECT a FROM IotAlert a WHERE a.farmerId = :farmerId " +
           "AND a.status = 'NEW' ORDER BY a.alertTimestamp DESC")
    List<IotAlert> findNewAlertsByFarmerId(@Param("farmerId") String farmerId);

    @Query("SELECT a FROM IotAlert a WHERE a.deviceId = :deviceId " +
           "AND a.alertTimestamp >= :since ORDER BY a.alertTimestamp DESC")
    List<IotAlert> findRecentAlerts(@Param("deviceId") Long deviceId,
                                    @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM IotAlert a WHERE a.farmerId = :farmerId AND a.status = 'NEW'")
    long countNewAlertsByFarmerId(@Param("farmerId") String farmerId);

    void deleteByDeviceId(Long deviceId);

    void deleteByAlertTimestampBefore(LocalDateTime threshold);
}