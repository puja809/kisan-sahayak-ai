"""Embedding service for generating document embeddings."""
import math
import time
from typing import List, Optional

from sentence_transformers import SentenceTransformer

from app.config import settings
from app.logging_config import logger


class EmbeddingService:
    """Service for generating text embeddings using sentence-transformers."""

    def __init__(self):
        """Initialize the embedding service."""
        self._model: Optional[SentenceTransformer] = None
        self._model_loaded = False

    async def load_model(self) -> None:
        """Load the sentence transformer model."""
        try:
            # Use a lightweight, efficient model for embeddings
            # all-MiniLM-L6-v2 produces 384-dimensional embeddings
            # For 768 dimensions, we can use 'all-mpnet-base-v2'
            self._model = SentenceTransformer("all-mpnet-base-v2")
            self._model_loaded = True
            logger.info("Embedding model loaded successfully")
        except Exception as e:
            logger.error(f"Failed to load embedding model: {e}")
            raise

    async def unload_model(self) -> None:
        """Unload the model to free resources."""
        if self._model:
            del self._model
            self._model = None
            self._model_loaded = False
            logger.info("Embedding model unloaded")

    def is_model_loaded(self) -> bool:
        """Check if the model is loaded."""
        return self._model_loaded

    def get_embedding_dimension(self) -> int:
        """Get the embedding dimension."""
        # all-mpnet-base-v2 produces 768-dimensional embeddings
        return settings.vector_dimension

    async def generate_embedding(self, text: str) -> List[float]:
        """Generate a single embedding for the given text."""
        if not self._model:
            raise RuntimeError("Embedding model not loaded")
        
        if not text or not text.strip():
            raise ValueError("Text cannot be empty")
        
        # Generate embedding
        embedding = self._model.encode(text, convert_to_numpy=True)
        
        # Convert to list of floats
        return embedding.tolist()

    async def generate_embeddings_batch(self, texts: List[str], batch_size: int = 32) -> List[List[float]]:
        """Generate embeddings for multiple texts in batches."""
        if not self._model:
            raise RuntimeError("Embedding model not loaded")
        
        if not texts:
            return []
        
        # Filter out empty texts
        valid_texts = [t for t in texts if t and t.strip()]
        
        if not valid_texts:
            return []
        
        # Generate embeddings in batches for efficiency
        embeddings = self._model.encode(
            valid_texts,
            convert_to_numpy=True,
            batch_size=batch_size,
            show_progress_bar=False
        )
        
        return embeddings.tolist()

    def normalize_embedding(self, embedding: List[float]) -> List[float]:
        """Normalize an embedding vector to unit length."""
        if not embedding:
            return embedding
        
        norm = math.sqrt(sum(x * x for x in embedding))
        
        if norm == 0:
            return embedding
        
        return [x / norm for x in embedding]

    def cosine_similarity(self, vec1: List[float], vec2: List[float]) -> float:
        """Compute cosine similarity between two vectors."""
        if not vec1 or not vec2:
            return 0.0
        
        if len(vec1) != len(vec2):
            raise ValueError("Vectors must have the same dimension")
        
        dot_product = sum(a * b for a, b in zip(vec1, vec2))
        norm1 = math.sqrt(sum(a * a for a in vec1))
        norm2 = math.sqrt(sum(b * b for b in vec2))
        
        if norm1 == 0 or norm2 == 0:
            return 0.0
        
        return dot_product / (norm1 * norm2)


# Global embedding service instance
embedding_service = EmbeddingService()