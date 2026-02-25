package com.farmer.user.dto;

import com.farmer.user.entity.Equipment;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating or updating equipment.
 * Requirements: 11A.12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentRequest {

    @NotNull(message = "Equipment type is required")
    private Equipment.EquipmentType equipmentType;

    private String equipmentName;

    private String manufacturer;

    private String model;

    private String serialNumber;

    private LocalDate purchaseDate;

    private BigDecimal purchaseCost;

    private Equipment.OwnershipType ownershipType;

    private LocalDate lastMaintenanceDate;

    private LocalDate nextMaintenanceDate;

    private BigDecimal currentValue;

    private String status;

    private String notes;
}