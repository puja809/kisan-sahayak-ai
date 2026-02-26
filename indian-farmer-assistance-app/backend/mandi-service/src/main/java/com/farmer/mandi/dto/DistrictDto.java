package com.farmer.mandi.dto;

import lombok.*;

/**
 * DTO for District entity
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistrictDto {
    private Long id;
    private String districtCode;
    private String districtName;
    private Long stateId;
    private String stateName;
    private Long population;
    private Double areaSqKm;
    private Double literacyRate;
    private Boolean isActive;
}
