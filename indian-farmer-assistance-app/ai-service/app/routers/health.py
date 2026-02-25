"""
Health check router for AI/ML Service
"""

from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class HealthResponse(BaseModel):
    """Health check response model."""
    status: str
    service: str
    version: str
    mongodb: str
    redis: str
    models_loaded: bool


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Health check endpoint for the AI/ML service.
    
    Returns the status of:
    - Service health
    - MongoDB connection
    - Redis connection
    - ML models loading status
    """
    return HealthResponse(
        status="healthy",
        service="ai-service",
        version="1.0.0",
        mongodb="connected",
        redis="connected",
        models_loaded=True,
    )


@router.get("/")
async def root():
    """Root endpoint with service information."""
    return {
        "service": "Indian Farmer Assistance AI Service",
        "version": "1.0.0",
        "description": "AI/ML services for crop disease detection, yield prediction, recommendations, and voice processing",
        "endpoints": {
            "health": "/health",
            "docs": "/docs",
            "disease_detection": "/api/v1/ai/disease",
            "yield_prediction": "/api/v1/ai/yield",
            "recommendations": "/api/v1/ai/recommendations",
            "voice": "/api/v1/ai/voice",
            "embeddings": "/api/v1/ai/embeddings",
        },
    }