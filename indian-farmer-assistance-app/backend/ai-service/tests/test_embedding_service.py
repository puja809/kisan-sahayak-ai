"""Unit tests for embedding service."""
import math
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.embedding_service import EmbeddingService


class TestEmbeddingService:
    """Tests for the EmbeddingService class."""

    @pytest.fixture
    def embedding_service(self):
        """Create an embedding service instance."""
        return EmbeddingService()

    @pytest.fixture
    def mock_model(self):
        """Create a mock sentence transformer model."""
        mock = MagicMock()
        # Return a 768-dimensional embedding
        mock.encode.return_value = [[0.1] * 768]
        return mock

    def test_service_initialization(self, embedding_service):
        """Test that the service initializes correctly."""
        assert embedding_service._model is None
        assert embedding_service._model_loaded is False

    def test_is_model_loaded_false_initially(self, embedding_service):
        """Test that is_model_loaded returns False initially."""
        assert embedding_service.is_model_loaded() is False

    def test_get_embedding_dimension(self, embedding_service):
        """Test that get_embedding_dimension returns the configured dimension."""
        dimension = embedding_service.get_embedding_dimension()
        assert dimension == 768

    @pytest.mark.asyncio
    async def test_generate_embedding_empty_text(self, embedding_service):
        """Test that generate_embedding raises ValueError for empty text."""
        with pytest.raises(ValueError, match="Text cannot be empty"):
            await embedding_service.generate_embedding("")

    @pytest.mark.asyncio
    async def test_generate_embedding_whitespace_only(self, embedding_service):
        """Test that generate_embedding raises ValueError for whitespace-only text."""
        with pytest.raises(ValueError, match="Text cannot be empty"):
            await embedding_service.generate_embedding("   ")

    @pytest.mark.asyncio
    async def test_generate_embedding_model_not_loaded(self, embedding_service):
        """Test that generate_embedding raises RuntimeError when model not loaded."""
        with pytest.raises(RuntimeError, match="Embedding model not loaded"):
            await embedding_service.generate_embedding("test text")

    @pytest.mark.asyncio
    async def test_generate_embedding_success(self, embedding_service, mock_model):
        """Test successful embedding generation."""
        embedding_service._model = mock_model
        embedding_service._model_loaded = True

        embedding = await embedding_service.generate_embedding("test text")

        assert len(embedding) == 768
        assert all(isinstance(x, float) for x in embedding)
        mock_model.encode.assert_called_once()

    @pytest.mark.asyncio
    async def test_generate_embeddings_batch_empty_list(self, embedding_service):
        """Test batch embedding with empty list returns empty list."""
        embeddings = await embedding_service.generate_embeddings_batch([])
        assert embeddings == []

    @pytest.mark.asyncio
    async def test_generate_embeddings_batch_model_not_loaded(self, embedding_service):
        """Test batch embedding raises RuntimeError when model not loaded."""
        with pytest.raises(RuntimeError, match="Embedding model not loaded"):
            await embedding_service.generate_embeddings_batch(["text1", "text2"])

    @pytest.mark.asyncio
    async def test_generate_embeddings_batch_success(self, embedding_service, mock_model):
        """Test successful batch embedding generation."""
        embedding_service._model = mock_model
        embedding_service._model_loaded = True

        # Mock returns 2 embeddings for 2 texts
        mock_model.encode.return_value = [[0.1] * 768, [0.2] * 768]

        embeddings = await embedding_service.generate_embeddings_batch(["text1", "text2"])

        assert len(embeddings) == 2
        assert len(embeddings[0]) == 768
        assert len(embeddings[1]) == 768

    @pytest.mark.asyncio
    async def test_generate_embeddings_batch_filters_empty(self, embedding_service, mock_model):
        """Test batch embedding filters out empty texts."""
        embedding_service._model = mock_model
        embedding_service._model_loaded = True

        mock_model.encode.return_value = [[0.1] * 768]

        embeddings = await embedding_service.generate_embeddings_batch(["text1", "", "   ", "text2"])

        # Only 2 valid texts should be processed
        mock_model.encode.assert_called_once()

    def test_normalize_embedding_empty(self, embedding_service):
        """Test normalizing an empty embedding returns empty list."""
        result = embedding_service.normalize_embedding([])
        assert result == []

    def test_normalize_embedding_zeros(self, embedding_service):
        """Test normalizing a zero vector returns the same vector."""
        embedding = [0.0] * 768
        result = embedding_service.normalize_embedding(embedding)
        assert result == embedding

    def test_normalize_embedding_success(self, embedding_service):
        """Test successful normalization."""
        embedding = [1.0] * 768
        result = embedding_service.normalize_embedding(embedding)

        # Result should be normalized to unit length
        norm = math.sqrt(sum(x * x for x in result))
        assert abs(norm - 1.0) < 0.0001

    def test_cosine_similarity_empty_vectors(self, embedding_service):
        """Test cosine similarity with empty vectors returns 0."""
        similarity = embedding_service.cosine_similarity([], [])
        assert similarity == 0.0

    def test_cosine_similarity_one_empty_vector(self, embedding_service):
        """Test cosine similarity with one empty vector returns 0."""
        similarity = embedding_service.cosine_similarity([0.1] * 768, [])
        assert similarity == 0.0

    def test_cosine_similarity_different_dimensions(self, embedding_service):
        """Test cosine similarity with different dimension vectors raises error."""
        with pytest.raises(ValueError, match="Vectors must have the same dimension"):
            embedding_service.cosine_similarity([0.1] * 768, [0.1] * 384)

    def test_cosine_similarity_identical_vectors(self, embedding_service):
        """Test cosine similarity of identical vectors is 1."""
        embedding = [0.1] * 768
        similarity = embedding_service.cosine_similarity(embedding, embedding)
        assert abs(similarity - 1.0) < 0.0001

    def test_cosine_similarity_opposite_vectors(self, embedding_service):
        """Test cosine similarity of opposite vectors is -1."""
        embedding1 = [0.1] * 768
        embedding2 = [-0.1] * 768
        similarity = embedding_service.cosine_similarity(embedding1, embedding2)
        assert abs(similarity - (-1.0)) < 0.0001

    def test_cosine_similarity_orthogonal_vectors(self, embedding_service):
        """Test cosine similarity of orthogonal vectors is 0."""
        # Create orthogonal vectors
        embedding1 = [1.0] + [0.0] * 767
        embedding2 = [0.0] * 768
        embedding2[1] = 1.0  # Orthogonal to embedding1
        similarity = embedding_service.cosine_similarity(embedding1, embedding2)
        assert abs(similarity) < 0.0001

    @pytest.mark.asyncio
    async def test_unload_model(self, embedding_service, mock_model):
        """Test unloading the model."""
        embedding_service._model = mock_model
        embedding_service._model_loaded = True

        await embedding_service.unload_model()

        assert embedding_service._model is None
        assert embedding_service._model_loaded is False