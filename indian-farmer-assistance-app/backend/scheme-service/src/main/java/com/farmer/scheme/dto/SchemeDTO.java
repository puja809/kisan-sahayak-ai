package com.farmer.scheme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemeDTO {
    private Long id;
    private String schemeName;
    private String centerStateName;
    private String schemeDetails;
    private String websiteLink;
    private String phone;
    private String email;
    private String commodityName;
    private String responsibleMinistry;
    private String implementingOffice;
    private String officeAddress;
    private String addressSource;
}
