package com.farmer.admin.service;

import com.farmer.admin.dto.DocumentResponse;
import com.farmer.admin.exception.DocumentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for document management using AWS S3 directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name:krishi-sahayak-docs}")
    private String bucketName;

    @Value("${aws.s3.documents-path:documents/}")
    private String documentsPath;

    @Value("${app.document.max-size-mb:50}")
    private int maxSizeMb;

    private static final List<String> ALLOWED_FORMATS = Arrays.asList("PDF", "DOCX", "TXT");
    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024; // 50MB

    public String uploadDocument(MultipartFile file, String title, String adminId) {
        log.info("Uploading document: {} by admin: {}", title, adminId);

        validateDocument(file);
        String documentId = UUID.randomUUID().toString();
        String s3Key = documentsPath + documentId + "/" + file.getOriginalFilename();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .metadata(Map.of(
                            "title", title,
                            "uploadedBy", adminId,
                            "uploadDate", LocalDateTime.now().toString()))
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return s3Key;
        } catch (IOException e) {
            throw new DocumentValidationException("Failed to read file for S3 upload", e);
        }
    }

    public void validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentValidationException("File is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new DocumentValidationException("File size exceeds 50MB limit");
        }
        String extension = getFileExtension(file.getOriginalFilename()).toUpperCase();
        if (!ALLOWED_FORMATS.contains(extension)) {
            throw new DocumentValidationException("Invalid file format. Allowed: " + ALLOWED_FORMATS);
        }
    }

    public String getPresignedUrl(String s3Key, int expirationMinutes) {
        try (S3Presigner presigner = S3Presigner.builder().region(s3Client.serviceClientConfiguration().region())
                .build()) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return presigner.presignGetObject(getObjectPresignRequest).url().toString();
        }
    }

    public List<DocumentResponse> getAllDocuments() {
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(documentsPath)
                .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

        return listResponse.contents().stream()
                .filter(s3Object -> !s3Object.key().endsWith("/")) // Filter out folders
                .map(s3Object -> {
                    HeadObjectRequest headRequest = HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Object.key())
                            .build();
                    HeadObjectResponse headResponse = s3Client.headObject(headRequest);

                    String title = headResponse.metadata().getOrDefault("title", "Untitled");
                    String uploadedBy = headResponse.metadata().getOrDefault("uploadedBy", "unknown");

                    return DocumentResponse.builder()
                            .id(s3Object.key())
                            .title(title)
                            .createdAt(s3Object.lastModified().atZone(ZoneId.systemDefault()).toLocalDateTime())
                            .metadata(DocumentResponse.DocumentMetadataDto.builder()
                                    .s3Key(s3Object.key())
                                    .fileSizeBytes(s3Object.size())
                                    .uploadedBy(uploadedBy)
                                    .fileFormat(getFileExtension(s3Object.key()))
                                    .build())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public void deleteDocument(String s3Key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    private String getFileExtension(String filename) {
        if (filename == null)
            return "";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1) : "";
    }
}