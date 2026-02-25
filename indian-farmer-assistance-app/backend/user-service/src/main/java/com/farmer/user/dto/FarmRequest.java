package com.farmer.user.dto;

import com.farmer.user.entity.Farm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * DTO for creating or updating a farm.
 * Requirements: 11A.2, 11A.3, 11A.10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmRequest {

    private String parcelNumber;

    @NotNull(message = "Total area is required")
    @Positive(message = "Total area must be positive")
    private Double totalAreaAcres;

    private String soilType;

    private Farm.IrrigationType irrigationType;

    private String agroEcologicalZone;

    private String surveyNumber;

    private Double gpsLatitude;

    private Double gpsLongitude;

    private String village;

    private String district;

    private String state;

    private String pinCode;
}