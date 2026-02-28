package com.farmer.cropservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FertilizerRecommendationResponse {
    private Double N_dosage;
    private Double P_dosage;
    private Double K_dosage;
    private Double total_dosage;
    private String modelVersion;
}
