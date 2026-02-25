"""
Disease detection router for AI/ML Service
"""

from fastapi import APIRouter, File, UploadFile, HTTPException
from pydantic import BaseModel
from typing import List, Optional

router = APIRouter()


class DiseaseDetectionResult(BaseModel):
    """Disease detection result model."""
    disease_name: str
    disease_name_local: str
    confidence_score: float
    severity_level: str  # LOW, MEDIUM, HIGH, CRITICAL
    affected_area_percent: float
    treatment_recommendations: dict


class DiseaseDetectionResponse(BaseModel):
    """Disease detection response model."""
    success: bool
    results: List[DiseaseDetectionResult]
    model_version: str
    processing_time_ms: int


@router.post("/detect", response_model=DiseaseDetectionResponse)
async def detect_disease(image: UploadFile = File(...)):
    """
    Detect crop diseases from an uploaded image.
    
    - Validates image format (JPEG, PNG) and size (max 10MB)
    - Analyzes image using disease detection model
    - Returns disease name, severity, confidence, and treatment recommendations
    """
    # Validate file type
    allowed_types = ["image/jpeg", "image/png"]
    if image.content_type not in allowed_types:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid image format. Allowed: {', '.join(allowed_types)}"
        )
    
    # Validate file size (10MB max)
    MAX_SIZE = 10 * 1024 * 1024  # 10MB
    content = await image.read()
    if len(content) > MAX_SIZE:
        raise HTTPException(
            status_code=400,
            detail="Image size exceeds 10MB limit"
        )
    
    # Placeholder for actual disease detection logic
    # In production, this would call the ML model
    return DiseaseDetectionResponse(
        success=True,
        results=[
            DiseaseDetectionResult(
                disease_name="Bacterial Leaf Blight",
                disease_name_local="Bacterial Leaf Blight",
                confidence_score=0.92,
                severity_level="HIGH",
                affected_area_percent=25.5,
                treatment_recommendations={
                    "organic": ["Remove infected leaves", "Apply neem oil spray"],
                    "chemical": ["Apply copper-based fungicide", "Use streptomycin if severe"],
                    "preventive": ["Use disease-free seeds", "Maintain proper spacing"],
                },
            )
        ],
        model_version="v1.0.0",
        processing_time_ms=150,
    )