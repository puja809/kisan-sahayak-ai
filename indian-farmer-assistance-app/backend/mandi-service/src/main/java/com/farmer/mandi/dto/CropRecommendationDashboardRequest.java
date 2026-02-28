package com.farmer.mandi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CropRecommendationDashboardRequest {
    private Double latitude;
    private Double longitude;
    private String season;
    private String previousCrop;
}
