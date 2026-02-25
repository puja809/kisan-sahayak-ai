"""Data models for AI Service."""
from datetime import datetime
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class DocumentMetadata(BaseModel):
    """Metadata for a document."""
    source: Optional[str] = None
    upload_date: Optional[datetime] = None
    uploaded_by: Optional[str] = None
    version: int = 1
    state: Optional[str] = None
    applicable_crops: List[str] = Field(default_factory=list)
    tags: List[str] = Field(default_factory=list)


class Document(BaseModel):
    """Document model for vector storage."""
    document_id: str
    title: str
    category: str  # "schemes", "guidelines", "crop_info", "disease_mgmt", "market_intel"
    content: str
    content_language: str = "en"
    embedding: Optional[List[float]] = None
    metadata: DocumentMetadata = Field(default_factory=DocumentMetadata)
    is_active: bool = True
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)


class DocumentCreate(BaseModel):
    """Request model for creating a document."""
    document_id: str
    title: str
    category: str
    content: str
    content_language: str = "en"
    metadata: Optional[DocumentMetadata] = None


class DocumentUpdate(BaseModel):
    """Request model for updating a document."""
    title: Optional[str] = None
    category: Optional[str] = None
    content: Optional[str] = None
    content_language: Optional[str] = None
    metadata: Optional[DocumentMetadata] = None
    is_active: Optional[bool] = None


class SearchFilters(BaseModel):
    """Filters for semantic search."""
    category: Optional[str] = None
    state: Optional[str] = None
    tags: Optional[List[str]] = None
    applicable_crops: Optional[List[str]] = None


class SearchRequest(BaseModel):
    """Request model for semantic search."""
    query: str
    query_embedding: Optional[List[float]] = None
    filters: Optional[SearchFilters] = None
    limit: int = Field(default=10, ge=1, le=100)


class SearchResult(BaseModel):
    """Result model for semantic search."""
    document_id: str
    title: str
    category: str
    content: str
    similarity_score: float
    metadata: DocumentMetadata


class SearchResponse(BaseModel):
    """Response model for semantic search."""
    query: str
    results: List[SearchResult]
    total_results: int
    processing_time_ms: float


class EmbeddingRequest(BaseModel):
    """Request model for generating embeddings."""
    text: str
    batch_texts: Optional[List[str]] = None


class EmbeddingResponse(BaseModel):
    """Response model for embeddings."""
    text: str
    embedding: List[float]
    dimension: int


class BatchEmbeddingRequest(BaseModel):
    """Request model for batch embedding generation."""
    texts: List[str]


class BatchEmbeddingResponse(BaseModel):
    """Response model for batch embeddings."""
    embeddings: List[List[float]]
    dimension: int
    count: int


class HealthResponse(BaseModel):
    """Response model for health check."""
    status: str
    mongodb_connected: bool
    embedding_model_loaded: bool
    timestamp: datetime


class ErrorResponse(BaseModel):
    """Response model for errors."""
    error: str
    detail: Optional[str] = None
    timestamp: datetime = Field(default_factory=datetime.utcnow)


# =============================================================================
# Disease Detection Models
# =============================================================================

class ImageValidationRequest(BaseModel):
    """Request model for image validation."""
    filename: str
    file_size: int  # in bytes
    content_type: str


class ImageValidationResponse(BaseModel):
    """Response model for image validation."""
    is_valid: bool
    errors: List[str] = Field(default_factory=list)
    warnings: List[str] = Field(default_factory=list)


class BoundingBox(BaseModel):
    """Bounding box for affected region in image."""
    x: float
    y: float
    width: float
    height: float
    confidence: float


class DiseaseDetectionResult(BaseModel):
    """Result of a single disease detection."""
    disease_name: str
    disease_name_local: Optional[str] = None
    confidence_score: float  # 0-100
    severity_level: str  # LOW, MEDIUM, HIGH, CRITICAL
    affected_area_percent: float
    bounding_boxes: List[BoundingBox] = Field(default_factory=list)
    is_healthy: bool = False


class TreatmentOption(BaseModel):
    """Treatment option for a disease."""
    type: str  # ORGANIC, CHEMICAL
    name: str
    description: str
    dosage: str
    application_timing: str
    safety_precautions: List[str] = Field(default_factory=list)
    estimated_cost: float  # in INR per acre
    unit: str = "per acre"


class PreventiveMeasure(BaseModel):
    """Preventive measure for a disease."""
    measure: str
    timing: str
    notes: Optional[str] = None


class TreatmentRecommendation(BaseModel):
    """Treatment recommendations for a detected disease."""
    disease_name: str
    disease_name_local: Optional[str] = None
    treatment_options: List[TreatmentOption] = Field(default_factory=list)
    preventive_measures: List[PreventiveMeasure] = Field(default_factory=list)
    estimated_total_cost: float  # in INR
    kvk_expert_contact: Optional[str] = None


class DiseaseDetectionRequest(BaseModel):
    """Request model for disease detection."""
    user_id: str
    crop_id: Optional[str] = None
    image_data: str  # Base64 encoded image
    filename: str
    content_type: str


class DiseaseDetectionResponse(BaseModel):
    """Response model for disease detection."""
    detection_id: str
    user_id: str
    crop_id: Optional[str] = None
    detections: List[DiseaseDetectionResult] = Field(default_factory=list)
    treatment_recommendations: List[TreatmentRecommendation] = Field(default_factory=list)
    image_path: str
    model_version: str
    processing_time_ms: float
    is_healthy: bool = False
    message: Optional[str] = None
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class DiseaseDetectionHistoryItem(BaseModel):
    """Item in disease detection history."""
    detection_id: str
    timestamp: datetime
    primary_disease: Optional[str] = None
    severity: Optional[str] = None
    confidence: Optional[float] = None
    image_path: str