package com.farmer.user.dto;

import com.farmer.user.entity.Farm;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for farm response.
 * Requirements: 11A.2, 11A.3, 11A.10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmResponse {

    private Long id;
    private String parcelNumber;
    private Double totalAreaAcres;
    private String soilType;
    private String irrigationType;
    private String agroEcologicalZone;
    private String surveyNumber;
    private Double gpsLatitude;
    private Double gpsLongitude;
    private String village;
    private String district;
    private String state;
    private String pinCode;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CropResponse> crops;

    /**
     * Convert Farm entity to FarmResponse DTO.
     */
    public static FarmResponse fromEntity(Farm farm) {
        FarmResponseBuilder builder = FarmResponse.builder()
                .id(farm.getId())
                .parcelNumber(farm.getParcelNumber())
                .totalAreaAcres(farm.getTotalAreaAcres())
                .soilType(farm.getSoilType())
                .irrigationType(farm.getIrrigationType() != null ? farm.getIrrigationType().name() : null)
                .agroEcologicalZone(farm.getAgroEcologicalZone())
                .surveyNumber(farm.getSurveyNumber())
                .gpsLatitude(farm.getGpsLatitude())
                .gpsLongitude(farm.getGpsLongitude())
                .village(farm.getVillage())
                .district(farm.getDistrict())
                .state(farm.getState())
                .pinCode(farm.getPinCode())
                .isActive(farm.getIsActive())
                .createdAt(farm.getCreatedAt())
                .updatedAt(farm.getUpdatedAt());

        if (farm.getCrops() != null) {
            builder.crops(farm.getCrops().stream()
                    .map(CropResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}