"""Semantic search service for document retrieval."""
import time
from typing import List, Optional

from app.config import settings
from app.embedding_service import embedding_service
from app.logging_config import logger
from app.models import Document, DocumentCreate, DocumentMetadata, SearchFilters, SearchRequest, SearchResponse, SearchResult
from app.vector_store import vector_store


class SemanticSearchService:
    """Service for semantic document search using vector embeddings."""

    def __init__(self):
        """Initialize the semantic search service."""
        self._embedding_service = embedding_service
        self._vector_store = vector_store

    async def search(
        self,
        query: str,
        filters: Optional[SearchFilters] = None,
        limit: int = 10
    ) -> SearchResponse:
        """
        Perform semantic search for documents matching the query.

        Args:
            query: The search query text
            filters: Optional filters for category, state, tags, etc.
            limit: Maximum number of results to return

        Returns:
            SearchResponse with ranked results
        """
        start_time = time.time()
        
        # Generate embedding for the query
        query_embedding = await self._embedding_service.generate_embedding(query)
        
        # Perform vector similarity search
        results = await self._vector_store.search_by_similarity(
            query_embedding=query_embedding,
            limit=limit,
            filters=filters
        )
        
        # Sort results by similarity score in descending order
        results.sort(key=lambda x: x.similarity_score, reverse=True)
        
        # Calculate processing time
        processing_time_ms = (time.time() - start_time) * 1000
        
        logger.info(f"Semantic search for '{query[:50]}...' returned {len(results)} results in {processing_time_ms:.2f}ms")
        
        return SearchResponse(
            query=query,
            results=results,
            total_results=len(results),
            processing_time_ms=processing_time_ms
        )

    async def search_with_embedding(
        self,
        query_embedding: List[float],
        filters: Optional[SearchFilters] = None,
        limit: int = 10
    ) -> SearchResponse:
        """
        Perform semantic search using a pre-computed query embedding.

        Args:
            query_embedding: Pre-computed embedding vector for the query
            filters: Optional filters for category, state, tags, etc.
            limit: Maximum number of results to return

        Returns:
            SearchResponse with ranked results
        """
        start_time = time.time()
        
        # Perform vector similarity search
        results = await self._vector_store.search_by_similarity(
            query_embedding=query_embedding,
            limit=limit,
            filters=filters
        )
        
        # Sort results by similarity score in descending order
        results.sort(key=lambda x: x.similarity_score, reverse=True)
        
        # Calculate processing time
        processing_time_ms = (time.time() - start_time) * 1000
        
        return SearchResponse(
            query="[pre-computed embedding]",
            results=results,
            total_results=len(results),
            processing_time_ms=processing_time_ms
        )

    async def index_document(self, document: DocumentCreate) -> str:
        """
        Index a document for semantic search.

        Args:
            document: The document to index

        Returns:
            The document ID
        """
        # Generate embedding for the document content
        embedding = await self._embedding_service.generate_embedding(document.content)
        
        # Create document with metadata
        doc = Document(
            document_id=document.document_id,
            title=document.title,
            category=document.category,
            content=document.content,
            content_language=document.content_language,
            embedding=embedding,
            metadata=document.metadata or DocumentMetadata()
        )
        
        # Store in vector database
        doc_id = await self._vector_store.store_document(doc, embedding)
        
        return doc_id

    async def delete_document(self, document_id: str) -> bool:
        """Soft-delete a document from the index."""
        return await self._vector_store.delete_document(document_id)

    async def get_document(self, document_id: str) -> Optional[Document]:
        """Get a document by ID."""
        return await self._vector_store.get_document(document_id)

    async def list_documents(
        self,
        category: Optional[str] = None,
        state: Optional[str] = None,
        limit: int = 100,
        offset: int = 0
    ) -> List[Document]:
        """List indexed documents with optional filtering."""
        return await self._vector_store.list_documents(
            category=category,
            state=state,
            limit=limit,
            offset=offset
        )


# Global semantic search service instance
semantic_search_service = SemanticSearchService()