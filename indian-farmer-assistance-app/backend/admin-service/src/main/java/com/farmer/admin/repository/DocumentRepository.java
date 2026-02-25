package com.farmer.admin.repository;

import com.farmer.admin.entity.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for Document entity.
 * Requirements: 21.3, 21.4, 21.5
 */
@Repository
public interface DocumentRepository extends MongoRepository<Document, String> {

    /**
     * Find document by document ID.
     */
    Optional<Document> findByDocumentId(String documentId);

    /**
     * Find all active documents by category.
     */
    List<Document> findByCategoryAndIsActiveTrue(String category);

    /**
     * Find all active documents.
     */
    List<Document> findByIsActiveTrue();

    /**
     * Find documents by category and state.
     */
    List<Document> findByCategoryAndMetadataStateAndIsActiveTrue(String category, String state);

    /**
     * Find documents by uploader.
     */
    List<Document> findByMetadataUploadedByAndIsActiveTrue(String uploadedBy);

    /**
     * Find soft-deleted documents pending permanent deletion.
     */
    @Query("{ 'is_deleted': true, 'deleted_at': { $lte: ?0 } }")
    List<Document> findDocumentsForPermanentDeletion(LocalDateTime cutoffDate);

    /**
     * Find documents by title containing search term (case-insensitive).
     */
    List<Document> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title);

    /**
     * Count documents by category.
     */
    long countByCategoryAndIsActiveTrue(String category);

    /**
     * Check if document exists by document ID.
     */
    boolean existsByDocumentId(String documentId);
}