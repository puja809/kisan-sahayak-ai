package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for agro-ecological zone information.
 * 
 * Validates: Requirement 2.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgroEcologicalZoneDto {

    private Long id;
    private String zoneCode;
    private String zoneName;
    private String description;
    private String climateType;
    private String rainfallRange;
    private String temperatureRange;
    private String soilTypes;
    private String suitableCrops;
    private String kharifSuitability;
    private String rabiSuitability;
    private String zaidSuitability;
    private String latitudeRange;
    private String longitudeRange;
    private String statesCovered;
}