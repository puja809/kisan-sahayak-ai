package com.farmer.admin.controller;

import com.farmer.admin.dto.DocumentResponse;
import com.farmer.admin.dto.DocumentUpdateRequest;
import com.farmer.admin.dto.DocumentUploadRequest;
import com.farmer.admin.entity.Document;
import com.farmer.admin.entity.DocumentMetadataInfo;
import com.farmer.admin.entity.DocumentVersion;
import com.farmer.admin.service.AuditService;
import com.farmer.admin.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for admin document management operations.
 * Requirements: 21.2, 21.5, 21.6, 21.7, 21.11
 */
@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
@Slf4j
// @PreAuthorize("hasRole('ADMIN')") // Temporarily disabled until JWT filter is
// implemented
public class AdminDocumentController {

    private final DocumentService documentService;
    private final AuditService auditService;

    /**
     * Upload a new document.
     * POST /api/v1/admin/documents/upload
     * Requirements: 21.2
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "contentLanguage", required = false) String contentLanguage,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "applicableCrops", required = false) String applicableCrops,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "changeReason", required = false) String changeReason,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("POST /api/v1/admin/documents/upload - Title: {}, Category: {}", title, category);

        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .title(title)
                .category(category)
                .description(description)
                .contentLanguage(contentLanguage)
                .state(state)
                .applicableCrops(applicableCrops != null ? List.of(applicableCrops.split(",")) : null)
                .tags(tags != null ? List.of(tags.split(",")) : null)
                .source(source)
                .changeReason(changeReason)
                .build();

        Document document = documentService.uploadDocument(file, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(document));
    }

    /**
     * Get all documents.
     * GET /api/v1/admin/documents
     * Requirements: 21.5
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        log.debug("GET /api/v1/admin/documents - Fetching all documents");
        List<Document> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(documents.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /**
     * Get document by ID.
     * GET /api/v1/admin/documents/{id}
     * Requirements: 21.5
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable String id) {
        log.debug("GET /api/v1/admin/documents/{} - Fetching document", id);
        Document document = documentService.getDocument(id);
        return ResponseEntity.ok(toResponse(document));
    }

    /**
     * Get documents by category.
     * GET /api/v1/admin/documents/category/{category}
     * Requirements: 21.5
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByCategory(@PathVariable String category) {
        log.debug("GET /api/v1/admin/documents/category/{} - Fetching documents by category", category);
        List<Document> documents = documentService.getDocumentsByCategory(category);
        return ResponseEntity.ok(documents.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /**
     * Update a document.
     * PUT /api/v1/admin/documents/{id}
     * Requirements: 21.6
     */
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable String id,
            @RequestBody DocumentUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("PUT /api/v1/admin/documents/{} - Updating document", id);
        Document document = documentService.updateDocument(id, request, userDetails.getUsername());
        return ResponseEntity.ok(toResponse(document));
    }

    /**
     * Soft delete a document.
     * DELETE /api/v1/admin/documents/{id}
     * Requirements: 21.7
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("DELETE /api/v1/admin/documents/{} - Deleting document", id);
        documentService.deleteDocument(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Restore a soft-deleted document.
     * POST /api/v1/admin/documents/{id}/restore
     * Requirements: 21.6
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<DocumentResponse> restoreDocument(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("POST /api/v1/admin/documents/{}/restore - Restoring document", id);
        Document document = documentService.restoreDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(toResponse(document));
    }

    /**
     * Get document version history.
     * GET /api/v1/admin/documents/{id}/versions
     * Requirements: 21.6
     */
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<DocumentVersion>> getDocumentVersions(@PathVariable String id) {
        log.debug("GET /api/v1/admin/documents/{}/versions - Fetching version history", id);
        List<DocumentVersion> versions = documentService.getDocumentVersionHistory(id);
        return ResponseEntity.ok(versions);
    }

    /**
     * Convert Document entity to DocumentResponse DTO.
     */
    private DocumentResponse toResponse(Document document) {
        DocumentResponse.DocumentMetadataDto metadataDto = null;
        if (document.getMetadata() != null) {
            DocumentMetadataInfo metadata = document.getMetadata();
            metadataDto = DocumentResponse.DocumentMetadataDto.builder()
                    .source(metadata.getSource())
                    .uploadDate(metadata.getUploadDate())
                    .uploadedBy(metadata.getUploadedBy())
                    .version(metadata.getVersion())
                    .state(metadata.getState())
                    .applicableCrops(metadata.getApplicableCrops())
                    .tags(metadata.getTags())
                    .fileFormat(metadata.getFileFormat())
                    .fileSizeBytes(metadata.getFileSizeBytes())
                    .originalFilename(metadata.getOriginalFilename())
                    .build();
        }

        return DocumentResponse.builder()
                .id(document.getId())
                .documentId(document.getDocumentId())
                .title(document.getTitle())
                .category(document.getCategory())
                .content(document.getContent())
                .contentLanguage(document.getContentLanguage())
                .metadata(metadataDto)
                .isActive(document.getIsActive())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}