package com.farmer.user.dto;

import com.farmer.user.entity.Crop;
import lombok.*;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for crop response.
 * Requirements: 11A.4, 11A.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropResponse {

    private Long id;
    private Long farmId;
    private String farmParcelNumber;
    private String cropName;
    private String cropVariety;
    private LocalDate sowingDate;
    private LocalDate expectedHarvestDate;
    private LocalDate actualHarvestDate;
    private Double areaAcres;
    private String season;
    private String status;
    private Double seedCost;
    private Double fertilizerCost;
    private Double pesticideCost;
    private Double laborCost;
    private Double otherCost;
    private Double totalInputCost;
    private Double totalYieldQuintals;
    private String qualityGrade;
    private Double sellingPricePerQuintal;
    private String mandiName;
    private Double totalRevenue;
    private Double profitLoss;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FertilizerApplicationResponse> fertilizerApplications;

    /**
     * Convert Crop entity to CropResponse DTO.
     */
    public static CropResponse fromEntity(Crop crop) {
        CropResponseBuilder builder = CropResponse.builder()
                .id(crop.getId())
                .farmId(crop.getFarm().getId())
                .farmParcelNumber(crop.getFarm().getParcelNumber())
                .cropName(crop.getCropName())
                .cropVariety(crop.getCropVariety())
                .sowingDate(crop.getSowingDate())
                .expectedHarvestDate(crop.getExpectedHarvestDate())
                .actualHarvestDate(crop.getActualHarvestDate())
                .areaAcres(crop.getAreaAcres())
                .season(crop.getSeason() != null ? crop.getSeason().name() : null)
                .status(crop.getStatus() != null ? crop.getStatus().name() : null)
                .seedCost(crop.getSeedCost())
                .fertilizerCost(crop.getFertilizerCost())
                .pesticideCost(crop.getPesticideCost())
                .laborCost(crop.getLaborCost())
                .otherCost(crop.getOtherCost())
                .totalInputCost(crop.getTotalInputCost())
                .totalYieldQuintals(crop.getTotalYieldQuintals())
                .qualityGrade(crop.getQualityGrade())
                .sellingPricePerQuintal(crop.getSellingPricePerQuintal())
                .mandiName(crop.getMandiName())
                .totalRevenue(crop.getTotalRevenue())
                .profitLoss(crop.getProfitLoss())
                .notes(crop.getNotes())
                .createdAt(crop.getCreatedAt())
                .updatedAt(crop.getUpdatedAt());

        if (crop.getFertilizerApplications() != null) {
            builder.fertilizerApplications(crop.getFertilizerApplications().stream()
                    .map(FertilizerApplicationResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}