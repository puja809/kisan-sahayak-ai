package com.farmer.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO for semantic search results.
 * 
 * Represents a document search result with similarity score.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {

    private Long id;
    private String documentId;
    private String title;
    
    // Category
    public enum DocumentCategory {
        SCHEMES, GUIDELINES, CROP_INFO, DISEASE_MGMT, MARKET_INTEL
    }
    
    private DocumentCategory category;
    
    // Content
    private String content;
    private String snippet;
    private String contentLanguage;
    
    // Similarity score (0-1)
    private Double similarityScore;
    
    // Metadata
    private Map<String, Object> metadata;
    private String source;
    private LocalDate uploadDate;
    private String uploadedBy;
    private String state;
    private List<String> applicableCrops;
    private List<String> tags;
    
    // Additional information
    private String documentUrl;
    private String documentType;
    private Integer version;
}