package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MLPredictionResponseDto {
    private String prediction;
    private Double confidence;
    private Map<String, Double> probabilities;
    private String modelVersion;
}
