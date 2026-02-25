package com.farmer.admin.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for document response.
 * Requirements: 21.2, 21.5, 21.11
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private String id;
    private String documentId;
    private String title;
    private String category;
    private String content;
    private String contentLanguage;
    private String description;
    private DocumentMetadataDto metadata;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentMetadataDto {
        private String source;
        private LocalDateTime uploadDate;
        private String uploadedBy;
        private Integer version;
        private String state;
        private List<String> applicableCrops;
        private List<String> tags;
        private String fileFormat;
        private Long fileSizeBytes;
        private String originalFilename;
    }
}