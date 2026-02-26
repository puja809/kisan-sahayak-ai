package com.farmer.crop.dto;

import com.farmer.crop.enums.CropFamily;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO representing a single crop history entry.
 * 
 * Requirements: 3.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropHistoryEntryDto {

    private Long cropId;
    private String cropName;
    private String cropVariety;
    private LocalDate sowingDate;
    private LocalDate expectedHarvestDate;
    private LocalDate actualHarvestDate;
    private Double areaAcres;
    private String season; // KHARIF, RABI, ZAID
    private String status;
    private Double totalYieldQuintals;
    
    // Derived fields for analysis
    private CropFamily cropFamily;
    private CropFamily.RootDepth rootDepth;
    private Integer seasonOrder; // 1 = most recent, 2 = second most recent, etc.
    private LocalDateTime harvestedAt;
}