package com.farmer.user.dto;

import com.farmer.user.entity.FertilizerApplication;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for fertilizer application response.
 * Requirements: 11A.4, 11C.6, 11C.7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerApplicationResponse {

    private Long id;
    private Long cropId;
    private String cropName;
    private String fertilizerType;
    private String fertilizerCategory;
    private BigDecimal quantityKg;
    private LocalDate applicationDate;
    private String applicationStage;
    private BigDecimal cost;
    private BigDecimal nitrogenContentPercent;
    private BigDecimal phosphorusContentPercent;
    private BigDecimal potassiumContentPercent;
    private BigDecimal nitrogenKg;
    private BigDecimal phosphorusKg;
    private BigDecimal potassiumKg;
    private String notes;
    private LocalDateTime createdAt;

    /**
     * Convert FertilizerApplication entity to FertilizerApplicationResponse DTO.
     */
    public static FertilizerApplicationResponse fromEntity(FertilizerApplication application) {
        return FertilizerApplicationResponse.builder()
                .id(application.getId())
                .cropId(application.getCrop().getId())
                .cropName(application.getCrop().getCropName())
                .fertilizerType(application.getFertilizerType())
                .fertilizerCategory(application.getFertilizerCategory() != null ? application.getFertilizerCategory().name() : null)
                .quantityKg(application.getQuantityKg())
                .applicationDate(application.getApplicationDate())
                .applicationStage(application.getApplicationStage())
                .cost(application.getCost())
                .nitrogenContentPercent(application.getNitrogenContentPercent())
                .phosphorusContentPercent(application.getPhosphorusContentPercent())
                .potassiumContentPercent(application.getPotassiumContentPercent())
                .nitrogenKg(application.getNitrogenKg())
                .phosphorusKg(application.getPhosphorusKg())
                .potassiumKg(application.getPotassiumKg())
                .notes(application.getNotes())
                .createdAt(application.getCreatedAt())
                .build();
    }
}