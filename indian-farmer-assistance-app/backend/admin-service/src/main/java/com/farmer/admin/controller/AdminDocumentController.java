package com.farmer.admin.controller;

import com.farmer.admin.dto.DocumentResponse;
import com.farmer.admin.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * REST controller for admin document management operations using S3.
 */
@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("POST /api/v1/admin/documents/upload - Title: {}", title);
        String s3Key = documentService.uploadDocument(file, title, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(s3Key);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        log.debug("GET /api/v1/admin/documents - Fetching all documents from S3");
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @GetMapping("/url")
    @Operation(summary = "Get document download URL")
    public ResponseEntity<String> getDocumentDownloadUrl(
            @RequestParam("s3Key") String s3Key,
            @RequestParam(defaultValue = "60") int expirationMinutes) {

        log.info("Document URL requested for S3 Key: {}", s3Key);
        String url = documentService.getPresignedUrl(s3Key, expirationMinutes);
        return ResponseEntity.ok(url);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteDocument(@RequestParam("s3Key") String s3Key) {
        log.info("DELETE /api/v1/admin/documents - Deleting document from S3: {}", s3Key);
        documentService.deleteDocument(s3Key);
        return ResponseEntity.noContent().build();
    }
}