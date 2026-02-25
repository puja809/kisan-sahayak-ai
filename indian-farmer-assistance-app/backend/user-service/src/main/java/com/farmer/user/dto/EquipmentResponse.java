package com.farmer.user.dto;

import com.farmer.user.entity.Equipment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for equipment response.
 * Requirements: 11A.12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentResponse {

    private Long id;
    private Long userId;
    private String equipmentType;
    private String equipmentName;
    private String manufacturer;
    private String model;
    private String serialNumber;
    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private String ownershipType;
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private BigDecimal currentValue;
    private String status;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Equipment entity to EquipmentResponse DTO.
     */
    public static EquipmentResponse fromEntity(Equipment equipment) {
        return EquipmentResponse.builder()
                .id(equipment.getId())
                .userId(equipment.getUser().getId())
                .equipmentType(equipment.getEquipmentType() != null ? equipment.getEquipmentType().name() : null)
                .equipmentName(equipment.getEquipmentName())
                .manufacturer(equipment.getManufacturer())
                .model(equipment.getModel())
                .serialNumber(equipment.getSerialNumber())
                .purchaseDate(equipment.getPurchaseDate())
                .purchaseCost(equipment.getPurchaseCost())
                .ownershipType(equipment.getOwnershipType() != null ? equipment.getOwnershipType().name() : null)
                .lastMaintenanceDate(equipment.getLastMaintenanceDate())
                .nextMaintenanceDate(equipment.getNextMaintenanceDate())
                .currentValue(equipment.getCurrentValue())
                .status(equipment.getStatus())
                .notes(equipment.getNotes())
                .isActive(equipment.getIsActive())
                .createdAt(equipment.getCreatedAt())
                .updatedAt(equipment.getUpdatedAt())
                .build();
    }
}