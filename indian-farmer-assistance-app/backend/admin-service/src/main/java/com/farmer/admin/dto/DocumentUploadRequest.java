package com.farmer.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

/**
 * DTO for document upload request.
 * Requirements: 21.2
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Category is required")
    private String category;

    private String description;

    private String contentLanguage;

    private String state;

    private List<String> applicableCrops;

    private List<String> tags;

    private String source;

    private String changeReason;
}