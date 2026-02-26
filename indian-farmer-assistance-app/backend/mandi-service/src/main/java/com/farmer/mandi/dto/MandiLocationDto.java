package com.farmer.mandi.dto;

import lombok.*;


/**
 * DTO for mandi location response.
 * 
 * Requirements:
 * - 6.4: Sort mandis by distance from farmer's location
 * - 6.9: Display contact information, operating hours, mandi comparison
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MandiLocationDto {

    private String mandiCode;
    private String mandiName;
    private String state;
    private String district;
    private String address;
    private Double latitude;
    private Double longitude;
    private String contactNumber;
    private String operatingHours;
    private Double distanceKm; // Distance from farmer's location
    private Boolean isActive;
}