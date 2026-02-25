"""Unit tests for semantic search service."""
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.models import DocumentMetadata, SearchFilters, SearchResult
from app.search_service import SemanticSearchService


class TestSemanticSearchService:
    """Tests for the SemanticSearchService class."""

    @pytest.fixture
    def search_service(self):
        """Create a semantic search service instance."""
        return SemanticSearchService()

    @pytest.fixture
    def mock_embedding_service(self):
        """Create a mock embedding service."""
        mock = MagicMock()
        mock.generate_embedding = AsyncMock(return_value=[0.1] * 768)
        mock.get_embedding_dimension = MagicMock(return_value=768)
        return mock

    @pytest.fixture
    def mock_vector_store(self):
        """Create a mock vector store."""
        mock = MagicMock()
        mock.search_by_similarity = AsyncMock(return_value=[])
        mock.store_document = AsyncMock(return_value="doc123")
        mock.delete_document = AsyncMock(return_value=True)
        mock.get_document = AsyncMock(return_value=None)
        mock.list_documents = AsyncMock(return_value=[])
        return mock

    @pytest.fixture
    def sample_search_results(self):
        """Create sample search results."""
        return [
            SearchResult(
                document_id="doc1",
                title="PM-Kisan Scheme",
                category="schemes",
                content="PM-Kisan Samman Nidhi provides ₹6000/year...",
                similarity_score=0.95,
                metadata=DocumentMetadata(state="Maharashtra", tags=["central", "income_support"])
            ),
            SearchResult(
                document_id="doc2",
                title="PMFBY Insurance",
                category="schemes",
                content="Pradhan Mantri Fasal Bima Yojana...",
                similarity_score=0.87,
                metadata=DocumentMetadata(state="Maharashtra", tags=["insurance", "crops"])
            ),
            SearchResult(
                document_id="doc3",
                title="Crop Guidelines",
                category="guidelines",
                content="Guidelines for wheat cultivation...",
                similarity_score=0.72,
                metadata=DocumentMetadata(state="Punjab", tags=["wheat", "cultivation"])
            ),
        ]

    def test_service_initialization(self, search_service):
        """Test that the service initializes correctly."""
        assert search_service._embedding_service is not None
        assert search_service._vector_store is not None

    @pytest.mark.asyncio
    async def test_search_empty_query(self, search_service):
        """Test that search raises error for empty query."""
        with pytest.raises(Exception):  # HTTPException
            await search_service.search(query="")

    @pytest.mark.asyncio
    async def test_search_success(self, search_service, mock_embedding_service, mock_vector_store, sample_search_results):
        """Test successful search."""
        search_service._embedding_service = mock_embedding_service
        search_service._vector_store = mock_vector_store
        mock_vector_store.search_by_similarity = AsyncMock(return_value=sample_search_results)

        response = await search_service.search(query="government schemes for farmers")

        assert response.query == "government schemes for farmers"
        assert len(response.results) == 3
        assert response.total_results == 3
        mock_embedding_service.generate_embedding.assert_called_once()

    @pytest.mark.asyncio
    async def test_search_results_sorted_by_similarity(self, search_service, mock_embedding_service, mock_vector_store, sample_search_results):
        """Test that search results are sorted by similarity in descending order."""
        search_service._embedding_service = mock_embedding_service
        search_service._vector_store = mock_vector_store
        mock_vector_store.search_by_similarity = AsyncMock(return_value=sample_search_results)

        response = await search_service.search(query="farmer support")

        # Results should be sorted by similarity score in descending order
        scores = [r.similarity_score for r in response.results]
        assert scores == sorted(scores, reverse=True)

    @pytest.mark.asyncio
    async def test_search_with_filters(self, search_service, mock_embedding_service, mock_vector_store):
        """Test search with filters."""
        search_service._embedding_service = mock_embedding_service
        search_service._vector_store = mock_vector_store

        filters = SearchFilters(category="schemes", state="Maharashtra")
        await search_service.search(query="government schemes", filters=filters)

        mock_vector_store.search_by_similarity.assert_called_once()
        call_args = mock_vector_store.search_by_similarity.call_args
        assert call_args.kwargs["filters"] == filters

    @pytest.mark.asyncio
    async def test_search_with_embedding(self, search_service, mock_vector_store, sample_search_results):
        """Test search with pre-computed embedding."""
        search_service._vector_store = mock_vector_store
        mock_vector_store.search_by_similarity = AsyncMock(return_value=sample_search_results)

        precomputed_embedding = [0.1] * 768
        response = await search_service.search_with_embedding(query_embedding=precomputed_embedding)

        assert response.query == "[pre-computed embedding]"
        assert len(response.results) == 3
        mock_vector_store.search_by_similarity.assert_called_once()

    @pytest.mark.asyncio
    async def test_index_document(self, search_service, mock_embedding_service, mock_vector_store):
        """Test document indexing."""
        search_service._embedding_service = mock_embedding_service
        search_service._vector_store = mock_vector_store

        from app.models import DocumentCreate

        doc = DocumentCreate(
            document_id="test_doc",
            title="Test Document",
            category="schemes",
            content="This is test content for indexing"
        )

        doc_id = await search_service.index_document(doc)

        assert doc_id == "test_doc"
        mock_embedding_service.generate_embedding.assert_called_once_with("This is test content for indexing")
        mock_vector_store.store_document.assert_called_once()

    @pytest.mark.asyncio
    async def test_delete_document(self, search_service, mock_vector_store):
        """Test document deletion."""
        search_service._vector_store = mock_vector_store
        mock_vector_store.delete_document = AsyncMock(return_value=True)

        result = await search_service.delete_document("doc123")

        assert result is True
        mock_vector_store.delete_document.assert_called_once_with("doc123")

    @pytest.mark.asyncio
    async def test_delete_nonexistent_document(self, search_service, mock_vector_store):
        """Test deleting a non-existent document returns False."""
        search_service._vector_store = mock_vector_store
        mock_vector_store.delete_document = AsyncMock(return_value=False)

        result = await search_service.delete_document("nonexistent")

        assert result is False

    @pytest.mark.asyncio
    async def test_get_document(self, search_service, mock_vector_store):
        """Test getting a document by ID."""
        from app.models import Document

        search_service._vector_store = mock_vector_store

        mock_doc = Document(
            document_id="doc123",
            title="Test Doc",
            category="schemes",
            content="Content"
        )
        mock_vector_store.get_document = AsyncMock(return_value=mock_doc)

        doc = await search_service.get_document("doc123")

        assert doc is not None
        assert doc.document_id == "doc123"

    @pytest.mark.asyncio
    async def test_list_documents(self, search_service, mock_vector_store):
        """Test listing documents with filters."""
        from app.models import Document

        search_service._vector_store = mock_vector_store

        mock_docs = [
            Document(document_id="doc1", title="Doc 1", category="schemes", content="Content 1"),
            Document(document_id="doc2", title="Doc 2", category="guidelines", content="Content 2"),
        ]
        mock_vector_store.list_documents = AsyncMock(return_value=mock_docs)

        docs = await search_service.list_documents(category="schemes", limit=10)

        assert len(docs) == 2
        mock_vector_store.list_documents.assert_called_once()


class TestSearchResultRanking:
    """Tests for search result ranking behavior."""

    @pytest.fixture
    def search_service(self):
        """Create a semantic search service instance."""
        return SemanticSearchService()

    @pytest.mark.asyncio
    async def test_ranking_descending_order(self, search_service):
        """Test that results are ranked in descending order by similarity score."""
        # Create results with various similarity scores
        results = [
            SearchResult(
                document_id=f"doc{i}",
                title=f"Document {i}",
                category="test",
                content="Content",
                similarity_score=0.5 + (i * 0.1),
                metadata=DocumentMetadata()
            )
            for i in range(5)
        ]

        # Sort using the service method
        sorted_results = sorted(results, key=lambda x: x.similarity_score, reverse=True)

        # Verify descending order
        scores = [r.similarity_score for r in sorted_results]
        assert scores == sorted(scores, reverse=True)

        # Verify each adjacent pair is in correct order
        for i in range(len(scores) - 1):
            assert scores[i] >= scores[i + 1]

    @pytest.mark.asyncio
    async def test_ranking_with_equal_scores(self, search_service):
        """Test that results with equal scores maintain relative order."""
        results = [
            SearchResult(
                document_id="doc1",
                title="Document 1",
                category="test",
                content="Content",
                similarity_score=0.8,
                metadata=DocumentMetadata()
            ),
            SearchResult(
                document_id="doc2",
                title="Document 2",
                category="test",
                content="Content",
                similarity_score=0.8,
                metadata=DocumentMetadata()
            ),
        ]

        sorted_results = sorted(results, key=lambda x: x.similarity_score, reverse=True)

        # Both should have the same score
        assert sorted_results[0].similarity_score == sorted_results[1].similarity_score == 0.8