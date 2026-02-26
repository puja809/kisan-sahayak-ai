package com.farmer.mandi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for fertilizer supplier information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FertilizerSupplierDto {
    private String state;
    private String district;
    private String documentId;
    private Integer slNo;
    private Integer noOfWholesalers;
    private Integer noOfRetailers;
    private String fertilizerType;
    private String supplierName;
    private String contactInfo;
    
    /**
     * Get total number of suppliers (wholesalers + retailers).
     * 
     * @return Total supplier count
     */
    public Integer getTotalSuppliers() {
        return (noOfWholesalers != null ? noOfWholesalers : 0) + 
               (noOfRetailers != null ? noOfRetailers : 0);
    }
}
