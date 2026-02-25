package com.farmer.admin.service;

import com.farmer.admin.dto.DocumentUploadRequest;
import com.farmer.admin.dto.DocumentUpdateRequest;
import com.farmer.admin.entity.Document;
import com.farmer.admin.entity.DocumentVersion;
import com.farmer.admin.exception.DocumentNotFoundException;
import com.farmer.admin.exception.DocumentValidationException;
import com.farmer.admin.repository.DocumentRepository;
import com.farmer.admin.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for document management including upload, validation, embedding, and storage.
 * Requirements: 21.2, 21.3, 21.4, 21.5, 21.6, 21.7
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditService auditService;

    @Value("${app.document.max-size-mb:50}")
    private int maxSizeMb;

    @Value("${app.document.retention-days:30}")
    private int retentionDays;

    private static final List<String> ALLOWED_FORMATS = Arrays.asList("PDF", "DOCX", "TXT");
    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024; // 50MB

    @PostConstruct
    public void initIndexes() {
        try {
            mongoTemplate.indexOps(Document.class)
                    .ensureIndex(new Index().on("category", org.springframework.data.domain.Sort.Direction.ASC));
            mongoTemplate.indexOps(Document.class)
                    .ensureIndex(new Index().on("metadata.uploadedBy", org.springframework.data.domain.Sort.Direction.ASC));
            mongoTemplate.indexOps(Document.class)
                    .ensureIndex(new Index().on("isActive", org.springframework.data.domain.Sort.Direction.ASC));
            log.info("MongoDB indexes created for Document collection");
        } catch (Exception e) {
            log.warn("Could not create MongoDB indexes: {}", e.getMessage());
        }
    }

    /**
     * Upload and validate a document.
     * Requirements: 21.2
     */
    public Document uploadDocument(MultipartFile file, DocumentUploadRequest request, String adminId) {
        log.info("Uploading document: {} by admin: {}", request.getTitle(), adminId);

        // Validate file
        validateDocument(file);

        // Extract text content
        String content = extractTextContent(file);

        // Generate document ID
        String documentId = UUID.randomUUID().toString();

        // Create document
        Document document = Document.builder()
                .documentId(documentId)
                .title(request.getTitle())
                .category(request.getCategory())
                .content(content)
                .contentLanguage(request.getContentLanguage() != null ? request.getContentLanguage() : "en")
                .metadata(Document.DocumentMetadata.builder()
                        .source(request.getSource())
                        .uploadDate(LocalDateTime.now())
                        .uploadedBy(adminId)
                        .version(1)
                        .state(request.getState())
                        .applicableCrops(request.getApplicableCrops())
                        .tags(request.getTags())
                        .fileFormat(getFileExtension(file.getOriginalFilename()))
                        .fileSizeBytes(file.getSize())
                        .originalFilename(file.getOriginalFilename())
                        .build())
                .isActive(true)
                .isDeleted(false)
                .build();

        // Save document
        Document savedDocument = documentRepository.save(document);

        // Create version history
        createVersionHistory(savedDocument, "CREATED", adminId, request.getChangeReason());

        // Audit log
        auditService.logDocumentAction("DOCUMENT_UPLOAD", savedDocument.getId(), null, savedDocument, adminId);

        log.info("Document uploaded successfully: {}", documentId);
        return savedDocument;
    }

    /**
     * Validate document format and size.
     * Requirements: 21.2
     */
    public void validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentValidationException("File is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new DocumentValidationException("Filename is required");
        }

        // Check file size
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new DocumentValidationException(
                String.format("File size %d bytes exceeds maximum allowed size of %d bytes (50MB)", 
                    file.getSize(), MAX_SIZE_BYTES));
        }

        // Check file format
        String extension = getFileExtension(filename).toUpperCase();
        if (!ALLOWED_FORMATS.contains(extension)) {
            throw new DocumentValidationException(
                String.format("Invalid file format '%s'. Allowed formats: %s", extension, ALLOWED_FORMATS));
        }

        log.debug("Document validation passed for: {}", filename);
    }

    /**
     * Extract text content from document.
     * Requirements: 21.2
     */
    public String extractTextContent(MultipartFile file) {
        String extension = getFileExtension(file.getOriginalFilename()).toUpperCase();
        
        try (InputStream inputStream = file.getInputStream()) {
            switch (extension) {
                case "PDF":
                    return extractPdfText(inputStream);
                case "DOCX":
                    return extractDocxText(inputStream);
                case "TXT":
                    return extractTextFromInputStream(inputStream);
                default:
                    throw new DocumentValidationException("Unsupported file format: " + extension);
            }
        } catch (IOException e) {
            log.error("Error extracting text content: {}", e.getMessage());
            throw new DocumentValidationException("Failed to extract text content: " + e.getMessage(), e);
        }
    }

    private String extractPdfText(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocxText(InputStream inputStream) throws IOException {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                text.append(paragraph.getText()).append("\n");
            }
        }
        return text.toString();
    }

    private String extractTextFromInputStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Get document by ID.
     * Requirements: 21.5
     */
    public Document getDocument(String documentId) {
        return documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    /**
     * Get all active documents.
     * Requirements: 21.5
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findByIsActiveTrue();
    }

    /**
     * Get documents by category.
     * Requirements: 21.5
     */
    public List<Document> getDocumentsByCategory(String category) {
        return documentRepository.findByCategoryAndIsActiveTrue(category);
    }

    /**
     * Update document.
     * Requirements: 21.6
     */
    public Document updateDocument(String documentId, DocumentUpdateRequest request, String adminId) {
        log.info("Updating document: {} by admin: {}", documentId, adminId);

        Document document = getDocument(documentId);
        Document oldDocument = cloneDocument(document);

        // Update fields
        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }
        if (request.getCategory() != null) {
            document.setCategory(request.getCategory());
        }
        if (request.getContent() != null) {
            document.setContent(request.getContent());
        }
        if (request.getContentLanguage() != null) {
            document.setContentLanguage(request.getContentLanguage());
        }
        
        // Update metadata
        Document.DocumentMetadata metadata = document.getMetadata();
        if (metadata == null) {
            metadata = new Document.DocumentMetadata();
            document.setMetadata(metadata);
        }
        if (request.getState() != null) {
            metadata.setState(request.getState());
        }
        if (request.getApplicableCrops() != null) {
            metadata.setApplicableCrops(request.getApplicableCrops());
        }
        if (request.getTags() != null) {
            metadata.setTags(request.getTags());
        }
        metadata.setVersion(metadata.getVersion() + 1);

        document.setUpdatedAt(LocalDateTime.now());
        Document savedDocument = documentRepository.save(document);

        // Create version history
        createVersionHistory(savedDocument, "UPDATED", adminId, request.getChangeReason());

        // Audit log
        auditService.logDocumentAction("DOCUMENT_UPDATE", savedDocument.getId(), oldDocument, savedDocument, adminId);

        log.info("Document updated successfully: {}", documentId);
        return savedDocument;
    }

    /**
     * Soft delete document with 30-day retention.
     * Requirements: 21.7
     */
    public void deleteDocument(String documentId, String adminId) {
        log.info("Soft deleting document: {} by admin: {}", documentId, adminId);

        Document document = getDocument(documentId);
        Document oldDocument = cloneDocument(document);

        document.setIsDeleted(true);
        document.setDeletedAt(LocalDateTime.now());
        document.setIsActive(false);
        document.setUpdatedAt(LocalDateTime.now());

        documentRepository.save(document);

        // Create version history
        createVersionHistory(document, "DELETED", adminId, "Soft delete with 30-day retention");

        // Audit log
        auditService.logDocumentAction("DOCUMENT_DELETE", document.getId(), oldDocument, document, adminId);

        log.info("Document soft deleted: {}", documentId);
    }

    /**
     * Permanently delete documents past retention period.
     * Requirements: 21.7
     */
    public void permanentDeleteOldDocuments() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<Document> oldDocuments = documentRepository.findDocumentsForPermanentDeletion(cutoffDate);
        
        for (Document document : oldDocuments) {
            documentRepository.delete(document);
            log.info("Permanently deleted document: {}", document.getDocumentId());
        }
        
        log.info("Permanent deletion completed. Deleted {} documents", oldDocuments.size());
    }

    /**
     * Restore soft-deleted document.
     * Requirements: 21.6
     */
    public Document restoreDocument(String documentId, String adminId) {
        log.info("Restoring document: {} by admin: {}", documentId, adminId);

        Document document = getDocument(documentId);
        Document oldDocument = cloneDocument(document);

        if (!document.getIsDeleted()) {
            throw new DocumentValidationException("Document is not deleted");
        }

        document.setIsDeleted(false);
        document.setDeletedAt(null);
        document.setIsActive(true);
        document.setUpdatedAt(LocalDateTime.now());

        Document savedDocument = documentRepository.save(document);

        // Create version history
        createVersionHistory(savedDocument, "RESTORED", adminId, "Restored from soft delete");

        // Audit log
        auditService.logDocumentAction("DOCUMENT_RESTORE", savedDocument.getId(), oldDocument, savedDocument, adminId);

        log.info("Document restored successfully: {}", documentId);
        return savedDocument;
    }

    /**
     * Get document version history.
     * Requirements: 21.6
     */
    public List<DocumentVersion> getDocumentVersionHistory(String documentId) {
        return documentVersionRepository.findByDocumentIdOrderByVersionDesc(documentId);
    }

    /**
     * Create version history entry.
     */
    private void createVersionHistory(Document document, String changeType, String changedBy, String changeReason) {
        DocumentVersion version = DocumentVersion.builder()
                .documentId(document.getDocumentId())
                .version(document.getMetadata().getVersion())
                .title(document.getTitle())
                .content(document.getContent())
                .contentHash(hashContent(document.getContent()))
                .category(document.getCategory())
                .metadata(serializeMetadata(document.getMetadata()))
                .changeType(changeType)
                .changedBy(changedBy)
                .changeReason(changeReason)
                .build();
        
        documentVersionRepository.save(version);
    }

    private Document cloneDocument(Document document) {
        return Document.builder()
                .id(document.getId())
                .documentId(document.getDocumentId())
                .title(document.getTitle())
                .category(document.getCategory())
                .content(document.getContent())
                .contentLanguage(document.getContentLanguage())
                .metadata(document.getMetadata())
                .isActive(document.getIsActive())
                .isDeleted(document.getIsDeleted())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    private String hashContent(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private String serializeMetadata(Document.DocumentMetadata metadata) {
        if (metadata == null) return "{}";
        return String.format(
            "{\"source\":\"%s\",\"state\":\"%s\",\"version\":%d}",
            metadata.getSource() != null ? metadata.getSource() : "",
            metadata.getState() != null ? metadata.getState() : "",
            metadata.getVersion() != null ? metadata.getVersion() : 1
        );
    }
}