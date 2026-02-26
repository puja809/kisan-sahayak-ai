package com.farmer.admin;

import com.farmer.admin.dto.DocumentResponse;
import com.farmer.admin.dto.DocumentUpdateRequest;
import com.farmer.admin.dto.DocumentUploadRequest;
import com.farmer.admin.entity.Document;
import com.farmer.admin.entity.DocumentVersion;
import com.farmer.admin.exception.DocumentNotFoundException;
import com.farmer.admin.exception.DocumentValidationException;
import com.farmer.admin.repository.DocumentRepository;
import com.farmer.admin.repository.DocumentVersionRepository;
import com.farmer.admin.service.AuditService;
import com.farmer.admin.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for admin service.
 * Requirements: 21.2, 21.6, 21.7, 21.9, 21.11
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceUnitTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private AuditService auditService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
            documentRepository, 
            documentVersionRepository, 
            null, 
            auditService);
        ReflectionTestUtils.setField(documentService, "maxSizeMb", 50);
        ReflectionTestUtils.setField(documentService, "retentionDays", 30);
    }

    // ==================== Document Upload Tests ====================

    @Test
    void shouldUploadValidDocument() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", "Test content".getBytes());
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .title("Test Document")
                .category("schemes")
                .description("Test description")
                .source("Government")
                .build();

        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId("test-mongo-id");
            return doc;
        });
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Document result = documentService.uploadDocument(file, request, "admin1");

        // Then
        assertNotNull(result, "Document should be created");
        assertEquals("Test Document", result.getTitle(), "Title should match");
        assertEquals("schemes", result.getCategory(), "Category should match");
        assertNotNull(result.getDocumentId(), "Document ID should be generated");
        assertTrue(result.getIsActive(), "Document should be active");
        assertFalse(result.getIsDeleted(), "Document should not be deleted");
        verify(documentRepository).save(any(Document.class));
        verify(documentVersionRepository).save(any(DocumentVersion.class));
        verify(auditService).logDocumentAction(eq("DOCUMENT_UPLOAD"), any(), any(), any(), eq("admin1"));
    }

    @Test
    void shouldRejectInvalidFileFormat() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "Test content".getBytes());
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .title("Test Document")
                .category("schemes")
                .build();

        // Act & Then
        DocumentValidationException exception = assertThrows(
            DocumentValidationException.class,
            () -> documentService.uploadDocument(file, request, "admin1")
        );
        
        assertTrue(exception.getMessage().contains("Invalid file format"),
            "Exception should mention invalid file format");
        assertTrue(exception.getMessage().contains("JPG"),
            "Exception should mention the invalid format");
    }

    @Test
    void shouldRejectOversizedFile() {
        // Arrange - Create a file larger than 50MB
        byte[] largeContent = new byte[51 * 1024 * 1024]; // 51MB
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", largeContent);
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .title("Test Document")
                .category("schemes")
                .build();

        // Act & Then
        DocumentValidationException exception = assertThrows(
            DocumentValidationException.class,
            () -> documentService.uploadDocument(file, request, "admin1")
        );
        
        assertTrue(exception.getMessage().contains("exceeds maximum allowed size"),
            "Exception should mention size limit");
    }

    @Test
    void shouldRejectEmptyFile() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", new byte[0]);
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .title("Test Document")
                .category("schemes")
                .build();

        // Act & Then
        DocumentValidationException exception = assertThrows(
            DocumentValidationException.class,
            () -> documentService.uploadDocument(file, request, "admin1")
        );
        
        assertTrue(exception.getMessage().contains("File is required"),
            "Exception should mention file is required");
    }

    @Test
    void shouldExtractTextFromPdf() {
        // Arrange
        String pdfContent = "This is a test PDF document with some content.";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", pdfContent.getBytes());
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .title("Test PDF")
                .category("guidelines")
                .build();

        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId("test-mongo-id");
            return doc;
        });
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Document result = documentService.uploadDocument(file, request, "admin1");

        // Then
        assertNotNull(result, "Document should be created");
        assertEquals(pdfContent, result.getContent(), "PDF content should be extracted");
    }

    @Test
    void shouldExtractTextFromDocx() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 
            "Test DOCX content".getBytes());
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .title("Test DOCX")
                .category("guidelines")
                .build();

        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId("test-mongo-id");
            return doc;
        });
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Document result = documentService.uploadDocument(file, request, "admin1");

        // Then
        assertNotNull(result, "Document should be created");
        assertNotNull(result.getContent(), "Content should be extracted");
    }

    @Test
    void shouldExtractTextFromTxt() {
        // Arrange
        String txtContent = "This is a plain text document.";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain", txtContent.getBytes());
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .title("Test TXT")
                .category("guidelines")
                .build();

        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId("test-mongo-id");
            return doc;
        });
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Document result = documentService.uploadDocument(file, request, "admin1");

        // Then
        assertNotNull(result, "Document should be created");
        assertEquals(txtContent, result.getContent(), "TXT content should be extracted");
    }

    // ==================== Document Versioning Tests ====================

    @Test
    void shouldUpdateDocumentAndIncrementVersion() {
        // Arrange
        String documentId = "test-doc-id";
        Document existingDoc = createTestDocument(documentId, 1);
        
        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(existingDoc));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentUpdateRequest updateRequest = DocumentUpdateRequest.builder()
                .title("Updated Title")
                .changeReason("Updated content")
                .build();

        // Act
        Document result = documentService.updateDocument(documentId, updateRequest, "admin1");

        // Then
        assertNotNull(result, "Document should be updated");
        assertEquals("Updated Title", result.getTitle(), "Title should be updated");
        assertEquals(2, result.getMetadata().getVersion(), "Version should be incremented");
        verify(documentVersionRepository).save(any(DocumentVersion.class));
        verify(auditService).logDocumentAction(eq("DOCUMENT_UPDATE"), any(), any(), any(), eq("admin1"));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentDocument() {
        // Arrange
        String documentId = "non-existent-id";
        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        DocumentUpdateRequest updateRequest = DocumentUpdateRequest.builder()
                .title("Updated Title")
                .build();

        // Act & Then
        DocumentNotFoundException exception = assertThrows(
            DocumentNotFoundException.class,
            () -> documentService.updateDocument(documentId, updateRequest, "admin1")
        );
        
        assertTrue(exception.getMessage().contains(documentId),
            "Exception should mention the document ID");
    }

    // ==================== Soft Delete Tests ====================

    @Test
    void shouldSoftDeleteDocument() {
        // Arrange
        String documentId = "test-doc-id";
        Document existingDoc = createTestDocument(documentId, 1);
        
        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(existingDoc));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        documentService.deleteDocument(documentId, "admin1");

        // Then
        verify(documentRepository).save(argThat(doc -> 
            doc.getIsDeleted() && !doc.getIsActive() && doc.getDeletedAt() != null));
        verify(documentVersionRepository).save(any(DocumentVersion.class));
        verify(auditService).logDocumentAction(eq("DOCUMENT_DELETE"), any(), any(), any(), eq("admin1"));
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentDocument() {
        // Arrange
        String documentId = "non-existent-id";
        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        // Act & Then
        DocumentNotFoundException exception = assertThrows(
            DocumentNotFoundException.class,
            () -> documentService.deleteDocument(documentId, "admin1")
        );
        
        assertTrue(exception.getMessage().contains(documentId),
            "Exception should mention the document ID");
    }

    // ==================== Document Restore Tests ====================

    @Test
    void shouldRestoreSoftDeletedDocument() {
        // Arrange
        String documentId = "test-doc-id";
        Document deletedDoc = createTestDocument(documentId, 1);
        deletedDoc.setIsDeleted(true);
        deletedDoc.setDeletedAt(LocalDateTime.now());
        deletedDoc.setIsActive(false);
        
        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(deletedDoc));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Document result = documentService.restoreDocument(documentId, "admin1");

        // Then
        assertNotNull(result, "Document should be restored");
        assertFalse(result.getIsDeleted(), "Document should not be deleted");
        assertTrue(result.getIsActive(), "Document should be active");
        assertNull(result.getDeletedAt(), "Deleted at should be null");
        verify(documentVersionRepository).save(any(DocumentVersion.class));
        verify(auditService).logDocumentAction(eq("DOCUMENT_RESTORE"), any(), any(), any(), eq("admin1"));
    }

    @Test
    void shouldThrowExceptionWhenRestoringNonDeletedDocument() {
        // Arrange
        String documentId = "test-doc-id";
        Document activeDoc = createTestDocument(documentId, 1);
        
        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(activeDoc));

        // Act & Then
        DocumentValidationException exception = assertThrows(
            DocumentValidationException.class,
            () -> documentService.restoreDocument(documentId, "admin1")
        );
        
        assertTrue(exception.getMessage().contains("not deleted"),
            "Exception should mention document is not deleted");
    }

    // ==================== Document Retrieval Tests ====================

    @Test
    void shouldGetAllActiveDocuments() {
        // Arrange
        List<Document> documents = Arrays.asList(
            createTestDocument("doc1", 1),
            createTestDocument("doc2", 1)
        );
        
        when(documentRepository.findByIsActiveTrue()).thenReturn(documents);

        // Act
        List<Document> result = documentService.getAllDocuments();

        // Then
        assertEquals(2, result.size(), "Should return 2 documents");
    }

    @Test
    void shouldGetDocumentsByCategory() {
        // Arrange
        String category = "schemes";
        List<Document> documents = Arrays.asList(
            createTestDocument("doc1", 1),
            createTestDocument("doc2", 1)
        );
        documents.forEach(doc -> doc.setCategory(category));
        
        when(documentRepository.findByCategoryAndIsActiveTrue(category)).thenReturn(documents);

        // Act
        List<Document> result = documentService.getDocumentsByCategory(category);

        // Then
        assertEquals(2, result.size(), "Should return 2 documents");
        assertTrue(result.stream().allMatch(doc -> category.equals(doc.getCategory())),
            "All documents should have the correct category");
    }

    @Test
    void shouldGetDocumentById() {
        // Arrange
        String documentId = "test-doc-id";
        Document document = createTestDocument(documentId, 1);
        
        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(document));

        // Act
        Document result = documentService.getDocument(documentId);

        // Then
        assertNotNull(result, "Document should be found");
        assertEquals(documentId, result.getDocumentId(), "Document ID should match");
    }

    @Test
    void shouldThrowExceptionWhenDocumentNotFound() {
        // Arrange
        String documentId = "non-existent-id";
        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        // Act & Then
        DocumentNotFoundException exception = assertThrows(
            DocumentNotFoundException.class,
            () -> documentService.getDocument(documentId)
        );
        
        assertTrue(exception.getMessage().contains(documentId),
            "Exception should mention the document ID");
    }

    // ==================== Version History Tests ====================

    @Test
    void shouldGetDocumentVersionHistory() {
        // Arrange
        String documentId = "test-doc-id";
        List<DocumentVersion> versions = Arrays.asList(
            createTestVersion(documentId, 2),
            createTestVersion(documentId, 1)
        );
        
        when(documentVersionRepository.findByDocumentIdOrderByVersionDesc(documentId))
            .thenReturn(versions);

        // Act
        List<DocumentVersion> result = documentService.getDocumentVersionHistory(documentId);

        // Then
        assertEquals(2, result.size(), "Should return 2 versions");
        assertEquals(2, result.get(0).getVersion(), "First version should be the latest");
    }

    // ==================== Helper Methods ====================

    private Document createTestDocument(String documentId, int version) {
        return Document.builder()
                .id("mongo-" + documentId)
                .documentId(documentId)
                .title("Test Document")
                .category("schemes")
                .content("Test content")
                .contentLanguage("en")
                .isActive(true)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private DocumentVersion createTestVersion(String documentId, int version) {
        return DocumentVersion.builder()
                .id((long) version)
                .documentId(documentId)
                .version(version)
                .title("Test Document v" + version)
                .content("Content v" + version)
                .changeType(version == 1 ? "CREATED" : "UPDATED")
                .changedBy("admin1")
                .createdAt(LocalDateTime.now())
                .build();
    }
}