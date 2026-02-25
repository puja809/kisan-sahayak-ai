package com.farmer.admin.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Document entity for storing agricultural documents in MongoDB.
 * Supports vector embeddings for semantic search.
 * 
 * Requirements: 21.3, 21.4, 21.5, 21.6, 21.7, 21.11
 */
@Document(collection = "sess_c05a946fe_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    private String id;

    @Field("document_id")
    private String documentId;

    @Field("title")
    private String title;

    @Field("category")
    @Indexed
    private String category; // "schemes", "guidelines", "crop_info", "disease_mgmt", "market_intel"

    @Field("content")
    private String content;

    @Field("content_language")
    private String contentLanguage;

    @Field("embedding")
    private List<Float> embedding; // 768-dimensional vector

    @Field("metadata")
    private DocumentMetadataInfo metadata;

    @Field("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Field("is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Field("deleted_at")
    private LocalDateTime deletedAt;

    @Field("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Document categories for classification.
     * Requirements: 21.5
     */
    public enum DocumentCategory {
        SCHEMES("schemes"),
        GUIDELINES("guidelines"),
        CROP_INFO("crop_info"),
        DISEASE_MGMT("disease_mgmt"),
        MARKET_INTEL("market_intel");

        private final String value;

        DocumentCategory(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static DocumentCategory fromValue(String value) {
            for (DocumentCategory category : values()) {
                if (category.value.equalsIgnoreCase(value)) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Unknown category: " + value);
        }
    }
}