package com.farmer.location.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovernmentBodyCreateRequest {
    
    @NotBlank(message = "State is required")
    private String state;
    
    @NotBlank(message = "District is required")
    private String district;
    
    @NotBlank(message = "District officer name is required")
    private String districtOfficer;
    
    @NotBlank(message = "District phone is required")
    private String districtPhone;
    
    @NotBlank(message = "Email is required")
    private String email;
    
    @NotBlank(message = "KVK phone is required")
    private String kvkPhone;
    
    private String sampleVillage;
}
