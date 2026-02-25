package com.farmer.admin;

import com.farmer.admin.entity.Document;
import com.farmer.admin.exception.DocumentValidationException;
import com.farmer.admin.service.DocumentService;
import net.jqwik.api.*;
import net.jqwik.junit5.JqwikTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for document format validation.
 * Validates: Requirements 21.2
 * 
 * Property 38: Document Format Validation
 * For any document uploaded by an admin, the system should validate that 
 * the format is PDF, DOCX, or TXT and the size is ≤ 50MB before processing, 
 * and reject any document that fails these criteria.
 */
@ExtendWith(MockitoExtension.class)
@JqwikTest
class DocumentFormatValidationPropertyTest {

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
            null, null, null, null);
        ReflectionTestUtils.setField(documentService, "maxSizeMb", 50);
        ReflectionTestUtils.setField(documentService, "retentionDays", 30);
    }

    // ==================== GENERATORS ====================

    /**
     * Generator for valid file extensions.
     */
    @Provide
    Arbitrary<String> validExtensions() {
        return Arbitraries.of("PDF", "DOCX", "TXT");
    }

    /**
     * Generator for invalid file extensions.
     */
    @Provide
    Arbitrary<String> invalidExtensions() {
        return Arbitraries.of("JPG", "PNG", "GIF", "MP4", "EXE", "ZIP", "HTML", "XML");
    }

    /**
     * Generator for valid file sizes (≤ 50MB).
     */
    @Provide
    Arbitrary<Long> validFileSizes() {
        // Generate sizes from 1 byte to 50MB
        return Arbitraries.longs().between(1, 50L * 1024 * 1024);
    }

    /**
     * Generator for invalid file sizes (> 50MB).
     */
    @Provide
    Arbitrary<Long> invalidFileSizes() {
        // Generate sizes from 50MB + 1 byte to 100MB
        return Arbitraries.longs().between(50L * 1024 * 1024 + 1, 100L * 1024 * 1024);
    }

    /**
     * Generator for valid filenames.
     */
    @Provide
    Arbitrary<String> validFilenames() {
        return validExtensions().flatMap(ext -> 
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                .map(name -> name + "." + ext));
    }

    /**
     * Generator for invalid filenames.
     */
    @Provide
    Arbitrary<String> invalidFilenames() {
        return invalidExtensions().flatMap(ext -> 
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                .map(name -> name + "." + ext));
    }

    /**
     * Generator for empty filenames.
     */
    @Provide
    Arbitrary<String> emptyFilenames() {
        return Arbitraries.of("", "   ", "noextension");
    }

    // ==================== PROPERTY TESTS ====================

    /**
     * Property 38.1: Valid documents should pass validation.
     * 
     * For any document with a valid extension (PDF, DOCX, TXT) and size ≤ 50MB,
     * the validation should pass without throwing an exception.
     * 
     * Validates: Requirements 21.2
     */
    @Property
    void validDocumentsShouldPassValidation(
            @ForAll("validExtensions") String extension,
            @ForAll("validFileSizes") long fileSize) {
        // Arrange
        String filename = "test." + extension;
        byte[] content = new byte[(int) Math.min(fileSize, Integer.MAX_VALUE)];
        MockMultipartFile file = new MockMultipartFile("file", filename, 
            "application/octet-stream", content);

        // Act & Then
        assertDoesNotThrow(() -> documentService.validateDocument(file),
            "Valid document should pass validation");
    }

    /**
     * Property 38.2: Invalid format documents should be rejected.
     * 
     * For any document with an invalid extension (not PDF, DOCX, or TXT),
     * the validation should throw a DocumentValidationException.
     * 
     * Validates: Requirements 21.2
     */
    @Property
    void invalidFormatDocumentsShouldBeRejected(
            @ForAll("invalidExtensions") String extension) {
        // Arrange
        String filename = "test." + extension;
        byte[] content = new byte[1000];
        MockMultipartFile file = new MockMultipartFile("file", filename, 
            "application/octet-stream", content);

        // Act & Then
        DocumentValidationException exception = assertThrows(
            DocumentValidationException.class,
            () -> documentService.validateDocument(file),
            "Invalid format document should be rejected"
        );
        
        assertTrue(exception.getMessage().contains("Invalid file format"),
            "Exception message should mention invalid file format");
    }

    /**
     * Property 38.3: Oversized documents should be rejected.
     * 
     * For any document with size > 50MB, the validation should throw
     * a DocumentValidationException.
     * 
     * Validates: Requirements 21.2
     */
    @Property
    void oversizedDocumentsShouldBeRejected(
            @ForAll("invalidFileSizes") long fileSize) {
        // Arrange
        // Create a file with the specified size (capped at Integer.MAX_VALUE for byte array)
        int contentSize = (int) Math.min(fileSize, Integer.MAX_VALUE);
        byte[] content = new byte[contentSize];
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", 
            "application/octet-stream", content);

        // Act & Then
        DocumentValidationException exception = assertThrows(
            DocumentValidationException.class,
            () -> documentService.validateDocument(file),
            "Oversized document should be rejected"
        );
        
        assertTrue(exception.getMessage().contains("exceeds maximum allowed size"),
            "Exception message should mention size limit");
    }

    /**
     * Property 38.4: Empty files should be rejected.
     * 
     * For any empty file, the validation should throw a DocumentValidationException.
     * 
     * Validates: Requirements 21.2
     */
    @Property
    void emptyFilesShouldBeRejected(@ForAll("validExtensions") String extension) {
        // Arrange
        String filename = "test." + extension;
        MockMultipartFile file = new MockMultipartFile("file", filename, 
            "application/octet-stream", new byte[0]);

        // Act & Then
        DocumentValidationException exception = assertThrows(
            DocumentValidationException.class,
            () -> documentService.validateDocument(file),
            "Empty file should be rejected"
        );
        
        assertTrue(exception.getMessage().contains("File is required"),
            "Exception message should mention file is required");
    }

    /**
     * Property 38.5: Null files should be rejected.
     * 
     * For a null file, the validation should throw a DocumentValidationException.
     * 
     * Validates: Requirements 21.2
     */
    @Test
    void nullFilesShouldBeRejected() {
        // Act & Then
        DocumentValidationException exception = assertThrows(
            DocumentValidationException.class,
            () -> documentService.validateDocument(null),
            "Null file should be rejected"
        );
        
        assertTrue(exception.getMessage().contains("File is required"),
            "Exception message should mention file is required");
    }

    /**
     * Property 38.6: Empty filenames should be rejected.
     * 
     * For any file with an empty or missing filename, the validation should
     * throw a DocumentValidationException.
     * 
     * Validates: Requirements 21.2
     */
    @Property
    void emptyFilenamesShouldBeRejected(@ForAll("emptyFilenames") String filename) {
        // Arrange
        byte[] content = new byte[1000];
        MockMultipartFile file = new MockMultipartFile("file", filename, 
            "application/octet-stream", content);

        // Act & Then
        DocumentValidationException exception = assertThrows(
            DocumentValidationException.class,
            () -> documentService.validateDocument(file),
            "File with empty filename should be rejected"
        );
        
        assertTrue(exception.getMessage().contains("Filename is required"),
            "Exception message should mention filename is required");
    }

    /**
     * Property 38.7: Validation is deterministic.
     * 
     * For any valid document, calling validation multiple times with the same
     * inputs should produce the same result (no exceptions).
     * 
     * Validates: Requirements 21.2
     */
    @Property
    void validationIsDeterministic(
            @ForAll("validExtensions") String extension,
            @ForAll("validFileSizes") long fileSize) {
        // Arrange
        String filename = "test." + extension;
        byte[] content = new byte[(int) Math.min(fileSize, Integer.MAX_VALUE)];
        MockMultipartFile file = new MockMultipartFile("file", filename, 
            "application/octet-stream", content);

        // Act - Validate multiple times
        assertDoesNotThrow(() -> documentService.validateDocument(file), "First validation should pass");
        assertDoesNotThrow(() -> documentService.validateDocument(file), "Second validation should pass");
        assertDoesNotThrow(() -> documentService.validateDocument(file), "Third validation should pass");
    }

    /**
     * Property 38.8: All valid formats should be accepted.
     * 
     * For each valid format (PDF, DOCX, TXT), documents with that format
     * should pass validation.
     * 
     * Validates: Requirements 21.2
     */
    @Property
    void allValidFormatsShouldBeAccepted(@ForAll("validFileSizes") long fileSize) {
        // Arrange
        List<String> validFormats = Arrays.asList("PDF", "DOCX", "TXT");
        byte[] content = new byte[(int) Math.min(fileSize, Integer.MAX_VALUE)];

        // Act & Then
        for (String format : validFormats) {
            String filename = "test." + format;
            MockMultipartFile file = new MockMultipartFile("file", filename, 
                "application/octet-stream", content);
            
            assertDoesNotThrow(() -> documentService.validateDocument(file),
                format + " document should pass validation");
        }
    }

    /**
     * Property 38.9: File size boundary condition at exactly 50MB.
     * 
     * A file with exactly 50MB should pass validation.
     * 
     * Validates: Requirements 21.2
     */
    @Test
    void exactly50MBFileShouldPassValidation() {
        // Arrange - exactly 50MB
        long fileSize = 50L * 1024 * 1024;
        byte[] content = new byte[(int) Math.min(fileSize, Integer.MAX_VALUE)];
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", 
            "application/octet-stream", content);

        // Act & Then
        assertDoesNotThrow(() -> documentService.validateDocument(file),
            "Exactly 50MB file should pass validation");
    }

    /**
     * Property 38.10: File size boundary condition just over 50MB.
     * 
     * A file with just over 50MB should fail validation.
     * 
     * Validates: Requirements 21.2
     */
    @Test
    void justOver50MBFileShouldFailValidation() {
        // Arrange - just over 50MB
        long fileSize = 50L * 1024 * 1024 + 1;
        byte[] content = new byte[(int) Math.min(fileSize, Integer.MAX_VALUE)];
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", 
            "application/octet-stream", content);

        // Act & Then
        assertThrows(DocumentValidationException.class,
            () -> documentService.validateDocument(file),
            "Just over 50MB file should fail validation");
    }
}