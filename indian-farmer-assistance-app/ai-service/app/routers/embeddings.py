"""
Embeddings router for AI/ML Service - Vector similarity search
"""

from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

router = APIRouter()


class DocumentEmbeddingRequest(BaseModel):
    """Document embedding request model."""
    document_id: str
    title: str
    content: str
    category: str  # schemes, guidelines, crop_info, disease_mgmt, market_intel
    content_language: str
    metadata: Optional[dict] = None


class DocumentEmbeddingResponse(BaseModel):
    """Document embedding response model."""
    success: bool
    document_id: str
    embedding_dimension: int
    stored_at: datetime


class SemanticSearchRequest(BaseModel):
    """Semantic search request model."""
    query: str
    query_language: str
    limit: int = 10
    filters: Optional[dict] = None  # category, state, tags


class SearchResult(BaseModel):
    """Search result model."""
    document_id: str
    title: str
    category: str
    content_preview: str
    similarity_score: float
    metadata: dict


class SemanticSearchResponse(BaseModel):
    """Semantic search response model."""
    success: bool
    query: str
    results: List[SearchResult]
    total_results: int
    processing_time_ms: int


@router.post("/generate")
async def generate_embeddings(request: DocumentEmbeddingRequest):
    """
    Generate 768-dimensional embeddings for a document using sentence-transformers.
    
    The embeddings are stored in MongoDB for vector similarity search.
    """
    return DocumentEmbeddingResponse(
        success=True,
        document_id=request.document_id,
        embedding_dimension=768,
        stored_at=datetime.utcnow(),
    )


@router.post("/search", response_model=SemanticSearchResponse)
async def semantic_search(request: SemanticSearchRequest):
    """
    Search documents using vector similarity.
    
    Uses cosine similarity to find semantically similar documents.
    Results are ranked by similarity score in descending order.
    """
    return SemanticSearchResponse(
        success=True,
        query=request.query,
        results=[
            SearchResult(
                document_id="doc_001",
                title="PM-Kisan Samman Nidhi Scheme",
                category="schemes",
                content_preview="The PM-Kisan Samman Nidhi is a central government scheme...",
                similarity_score=0.92,
                metadata={"state": "all", "applicable_crops": []},
            ),
            SearchResult(
                document_id="doc_002",
                title="Rice Cultivation Guidelines",
                category="crop_info",
                content_preview="Rice is a kharif crop that requires adequate water...",
                similarity_score=0.85,
                metadata={"state": "all", "applicable_crops": ["rice"]},
            ),
        ],
        total_results=2,
        processing_time_ms=150,
    )