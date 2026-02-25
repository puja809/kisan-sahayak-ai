package com.farmer.user.dto;

import com.farmer.user.entity.Livestock;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for livestock response.
 * Requirements: 11A.11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivestockResponse {

    private Long id;
    private Long userId;
    private Long farmId;
    private String farmParcelNumber;
    private String livestockType;
    private String breed;
    private Integer quantity;
    private String purpose;
    private LocalDate acquisitionDate;
    private LocalDate dateOfBirth;
    private String gender;
    private String tagNumber;
    private String healthStatus;
    private String vaccinationStatus;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Livestock entity to LivestockResponse DTO.
     */
    public static LivestockResponse fromEntity(Livestock livestock) {
        LivestockResponseBuilder builder = LivestockResponse.builder()
                .id(livestock.getId())
                .userId(livestock.getUser().getId())
                .livestockType(livestock.getLivestockType() != null ? livestock.getLivestockType().name() : null)
                .breed(livestock.getBreed())
                .quantity(livestock.getQuantity())
                .purpose(livestock.getPurpose() != null ? livestock.getPurpose().name() : null)
                .acquisitionDate(livestock.getAcquisitionDate())
                .dateOfBirth(livestock.getDateOfBirth())
                .gender(livestock.getGender())
                .tagNumber(livestock.getTagNumber())
                .healthStatus(livestock.getHealthStatus())
                .vaccinationStatus(livestock.getVaccinationStatus())
                .notes(livestock.getNotes())
                .isActive(livestock.getIsActive())
                .createdAt(livestock.getCreatedAt())
                .updatedAt(livestock.getUpdatedAt());

        if (livestock.getFarm() != null) {
            builder.farmId(livestock.getFarm().getId())
                   .farmParcelNumber(livestock.getFarm().getParcelNumber());
        }

        return builder.build();
    }
}