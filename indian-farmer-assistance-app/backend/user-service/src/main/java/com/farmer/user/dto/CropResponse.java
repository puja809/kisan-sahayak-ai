package com.farmer.user.dto;

import com.farmer.user.entity.Crop;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropResponse {

    private Long id;
    private Long userId;
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
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CropResponse fromEntity(Crop crop) {
        return CropResponse.builder()
                .id(crop.getId())
                .userId(crop.getUser().getId())
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
                .notes(crop.getNotes())
                .createdAt(crop.getCreatedAt())
                .updatedAt(crop.getUpdatedAt())
                .build();
    }
}