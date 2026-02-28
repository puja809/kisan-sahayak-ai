package com.farmer.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovernmentBodyDto {
    private Long id;
    private String state;
    private String district;
    private String districtOfficer;
    private String districtPhone;
    private String email;
    private String kvkPhone;
    private String sampleVillage;
}
