package com.farmer.mandi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CropRotationRequest {
    private String previousCrop;
    private Double soilPH;
    private String soilType;
    private Double temperature;
    private Double humidity;
    private Double rainfall;
    private String season;
}
