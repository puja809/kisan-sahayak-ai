package com.farmer.mandi.dto;

import lombok.*;

/**
 * DTO for State entity
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateDto {
    private Long id;
    private String stateCode;
    private String stateName;
    private Long population;
    private Double areaSqKm;
    private Double literacyRate;
    private Boolean isActive;
}
