package com.farmer.cropservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CropDTO {
    private Long id;
    private String commodity;
    private String category;
    private String season;
    private String durationDays;
    private String seedRateKgPerAcre;
    private String spacingCm;
    private String fertilizerNpkKgPerAcre;
    private String irrigationNumber;
    private String keyOperations;
    private String harvestSigns;
    private String yieldKgPerAcre;
}
