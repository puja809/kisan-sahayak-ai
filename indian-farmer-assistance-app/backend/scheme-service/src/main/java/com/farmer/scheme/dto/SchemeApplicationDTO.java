package com.farmer.scheme.dto;

import com.farmer.scheme.entity.SchemeApplication;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO representing a scheme application for display purposes.
 * 
 * Requirements: 11D.10
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeApplicationDTO {
    
    private Long applicationId;
    private Long userId;
    private Long schemeId;
    private String schemeName;
    private String schemeCode;
    private LocalDate applicationDate;
    private String applicationNumber;
    private SchemeApplication.ApplicationStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime disbursedAt;
    private String remarks;
    private String reviewerNotes;
    private Object documents;
    
    /**
     * Create DTO from entity.
     */
    public static SchemeApplicationDTO fromEntity(SchemeApplication entity) {
        if (entity == null) return null;
        
        return SchemeApplicationDTO.builder()
                .applicationId(entity.getId())
                .userId(entity.getUserId())
                .schemeId(entity.getScheme().getId())
                .schemeName(entity.getScheme().getSchemeName())
                .schemeCode(entity.getScheme().getSchemeCode())
                .applicationDate(entity.getApplicationDate())
                .applicationNumber(entity.getApplicationNumber())
                .status(entity.getStatus())
                .submittedAt(entity.getSubmittedAt())
                .reviewedAt(entity.getReviewedAt())
                .disbursedAt(entity.getDisbursedAt())
                .remarks(entity.getRemarks())
                .reviewerNotes(entity.getReviewerNotes())
                .build();
    }
}