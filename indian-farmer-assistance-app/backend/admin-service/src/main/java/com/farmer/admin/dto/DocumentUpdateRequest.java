package com.farmer.admin.dto;

import lombok.*;
import java.util.List;

/**
 * DTO for document update request.
 * Requirements: 21.6
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUpdateRequest {

    private String title;
    private String category;
    private String content;
    private String contentLanguage;
    private String description;
    private String state;
    private List<String> applicableCrops;
    private List<String> tags;
    private String changeReason;
}