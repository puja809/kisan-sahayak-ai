package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CropRecommendationMLRequestDto {
    private Double latitude;
    private Double longitude;
    private Double temperature;
    private Double humidity;
    private Double rainfall;
    private Double soilPH;
    private Double nitrogen;
    private Double phosphorus;
    private Double potassium;
}
