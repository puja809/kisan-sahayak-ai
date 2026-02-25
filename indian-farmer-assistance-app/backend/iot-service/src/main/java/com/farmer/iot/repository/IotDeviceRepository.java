package com.farmer.iot.repository;

import com.farmer.iot.entity.IotDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for IoT device operations.
 */
@Repository
public interface IotDeviceRepository extends JpaRepository<IotDevice, Long> {

    Optional<IotDevice> findByDeviceId(String deviceId);

    List<IotDevice> findByOwnerFarmerId(String farmerId);

    List<IotDevice> findByOwnerFarmerIdAndStatus(String farmerId, IotDevice.DeviceStatus status);

    List<IotDevice> findByFarmId(Long farmId);

    @Query("SELECT d FROM IotDevice d WHERE d.ownerFarmerId = :farmerId ORDER BY d.createdAt DESC")
    List<IotDevice> findAllDevicesByFarmerIdOrderByCreatedAtDesc(@Param("farmerId") String farmerId);

    @Query("SELECT d FROM IotDevice d WHERE d.status = :status AND d.lastSeen < :threshold")
    List<IotDevice> findDevicesOfflineSince(@Param("status") IotDevice.DeviceStatus status,
                                            @Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(d) FROM IotDevice d WHERE d.ownerFarmerId = :farmerId AND d.status = 'ACTIVE'")
    long countActiveDevicesByFarmerId(@Param("farmerId") String farmerId);

    boolean existsByDeviceId(String deviceId);
}