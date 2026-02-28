package com.farmer.mandi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FertilizerRecommendationRequest {
    private String crop;
    private String soilType;
    private Double soilPH;
    private Double temperature;
    private Double humidity;
    private Double rainfall;
    private String season;
}
