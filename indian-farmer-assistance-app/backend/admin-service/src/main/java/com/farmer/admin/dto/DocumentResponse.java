package com.farmer.admin.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for document response from S3.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private String id; // S3 Key
    private String title;
    private DocumentMetadataDto metadata;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentMetadataDto {
        private String uploadedBy;
        private String fileFormat;
        private Long fileSizeBytes;
        private String s3Key;
    }
}