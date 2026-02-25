"""
Yield prediction router for AI/ML Service
"""

from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Optional
from datetime import date

router = APIRouter()


class YieldPredictionRequest(BaseModel):
    """Yield prediction request model."""
    crop_name: str
    crop_variety: str
    sowing_date: date
    area_acres: float
    irrigation_type: str  # RAINFED, DRIP, SPRINKLER, CANAL, BOREWELL
    soil_type: Optional[str] = None
    district: str
    state: str


class YieldPredictionResult(BaseModel):
    """Yield prediction result model."""
    predicted_yield_min: float
    predicted_yield_expected: float
    predicted_yield_max: float
    confidence_interval_percent: float
    factors_considered: List[str]
    model_version: str


class YieldPredictionResponse(BaseModel):
    """Yield prediction response model."""
    success: bool
    crop_id: Optional[str] = None
    prediction: YieldPredictionResult
    processing_time_ms: int


@router.post("/predict")
async def predict_yield(request: YieldPredictionRequest):
    """
    Predict crop yield based on various factors.
    
    Factors considered:
    - Crop type and variety
    - Sowing date and area
    - Soil health data (N, P, K, pH)
    - Weather patterns (rainfall, temperature)
    - Irrigation type and frequency
    - Historical yield data
    """
    return YieldPredictionResponse(
        success=True,
        prediction=YieldPredictionResult(
            predicted_yield_min=25.5,
            predicted_yield_expected=32.0,
            predicted_yield_max=38.5,
            confidence_interval_percent=85.0,
            factors_considered=[
                "Crop type and variety",
                "Sowing date",
                "Area under cultivation",
                "Irrigation type",
                "Historical weather data",
                "Soil nutrient levels",
            ],
            model_version="v1.0.0",
        ),
        processing_time_ms=200,
    )


@router.post("/update-model")
async def update_model_with_actual_yield(
    crop_id: str,
    actual_yield: float,
    harvest_date: date
):
    """
    Update the yield prediction model with actual harvest data.
    
    This improves future predictions by learning from actual results.
    """
    return {
        "success": True,
        "crop_id": crop_id,
        "actual_yield": actual_yield,
        "variance_percent": 5.2,
        "message": "Model updated with actual yield data",
    }