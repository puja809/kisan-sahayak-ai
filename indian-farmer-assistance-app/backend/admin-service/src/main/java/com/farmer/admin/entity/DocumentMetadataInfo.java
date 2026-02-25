package com.farmer.admin.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Document metadata for provenance and audit.
 * Requirements: 21.11
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMetadataInfo {
    @Field("source")
    private String source;

    @Field("upload_date")
    private LocalDateTime uploadDate;

    @Field("uploaded_by")
    private String uploadedBy;

    @Field("version")
    @Builder.Default
    private Integer version = 1;

    @Field("state")
    private String state;

    @Field("applicable_crops")
    private List<String> applicableCrops;

    @Field("tags")
    private List<String> tags;

    @Field("file_format")
    private String fileFormat;

    @Field("file_size_bytes")
    private Long fileSizeBytes;

    @Field("original_filename")
    private String originalFilename;
}