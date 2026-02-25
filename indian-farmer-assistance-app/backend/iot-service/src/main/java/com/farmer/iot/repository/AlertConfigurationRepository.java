package com.farmer.iot.repository;

import com.farmer.iot.entity.AlertConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for alert configuration operations.
 */
@Repository
public interface AlertConfigurationRepository extends JpaRepository<AlertConfiguration, Long> {

    List<AlertConfiguration> findByDeviceId(Long deviceId);

    List<AlertConfiguration> findByDeviceIdAndAlertEnabled(Long deviceId, Boolean enabled);

    Optional<AlertConfiguration> findByDeviceIdAndSensorType(Long deviceId, String sensorType);

    List<AlertConfiguration> findByFarmerId(String farmerId);

    List<AlertConfiguration> findByFarmerIdAndAlertEnabled(String farmerId, Boolean enabled);

    void deleteByDeviceId(Long deviceId);
}