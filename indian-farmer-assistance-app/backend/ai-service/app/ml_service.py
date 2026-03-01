"""
FastAPI service for ML model predictions
Serves crop recommendation and rotation models
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import os
import logging
import asyncio
from crop_recommendation_model import CropRecommendationModel
from crop_rotation_model import CropRotationModel
from fertilizer_recommendation_model import FertilizerRecommendationModel
from crop_name_mapper import map_crop_name
from aws_voice_assistant_client import ask_question
from dotenv import load_dotenv

# Load environment variables from .env file (for local development)
try:
    # Try to find .env in project root
    env_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../../.env'))
    if os.path.exists(env_path):
        load_dotenv(env_path)
    else:
        # Fallback to current directory
        load_dotenv()
except Exception as e:
    logger.warning(f"Could not load .env file: {e}")

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="ML Crop Prediction Service", version="1.0.0")

# Global model instances
crop_reco_model = None
crop_rotation_model = None
fertilizer_model = None

class CropRecommendationRequest(BaseModel):
    latitude: float
    longitude: float
    temperature: float
    humidity: float
    rainfall: float
    soilPH: float
    nitrogen: float
    phosphorus: float
    potassium: float

class CropRotationRequest(BaseModel):
    previousCrop: str
    soilPH: float
    soilType: str
    temperature: float
    humidity: float
    rainfall: float
    season: str

class FertilizerRecommendationRequest(BaseModel):
    crop: str
    soilType: str
    soilPH: float
    temperature: float
    humidity: float
    rainfall: float
    season: str

class VoiceAssistantRequest(BaseModel):
    question: str

class PredictionResponse(BaseModel):
    prediction: str
    confidence: float
    probabilities: dict
    modelVersion: str = "1.0.0"

@app.on_event("startup")
async def startup_event():
    """Load models and register with Eureka on startup"""
    global crop_reco_model, crop_rotation_model, fertilizer_model
    try:
        base_path = os.path.dirname(os.path.abspath(__file__))
        models_dir = os.path.join(base_path, 'models')
        
        logger.info("Loading crop recommendation model...")
        crop_reco_model = CropRecommendationModel()
        crop_reco_model.load(os.path.join(models_dir, 'crop_recommendation_model.pkl'))
        logger.info("✓ Crop recommendation model loaded")
        
        logger.info("Loading crop rotation model...")
        crop_rotation_model = CropRotationModel()
        crop_rotation_model.load(os.path.join(models_dir, 'crop_rotation_model.pkl'))
        logger.info("✓ Crop rotation model loaded")
        
        logger.info("Loading fertilizer recommendation model...")
        fertilizer_model = FertilizerRecommendationModel()
        fertilizer_model.load(os.path.join(models_dir, 'fertilizer_recommendation_model.pkl'))
        logger.info("✓ Fertilizer recommendation model loaded")
        
    except Exception as e:
        logger.error(f"Error loading models: {e}")
        raise

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "crop_reco_model": crop_reco_model is not None,
        "crop_rotation_model": crop_rotation_model is not None,
        "fertilizer_model": fertilizer_model is not None
    }

@app.post("/api/ml/predict-crop", response_model=PredictionResponse)
async def predict_crop(request: CropRecommendationRequest):
    """Predict crop based on soil and weather parameters"""
    try:
        if crop_reco_model is None:
            raise HTTPException(status_code=503, detail="Crop recommendation model not loaded")
        
        result = crop_reco_model.predict(
            N=request.nitrogen,
            P=request.phosphorus,
            K=request.potassium,
            temperature=request.temperature,
            humidity=request.humidity,
            pH=request.soilPH,
            rainfall=request.rainfall
        )
        
        return PredictionResponse(
            prediction=result['crop'],
            confidence=result['confidence'],
            probabilities=result['probabilities']
        )
    except Exception as e:
        logger.error(f"Error in crop prediction: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ml/predict-rotation", response_model=PredictionResponse)
async def predict_rotation(request: CropRotationRequest):
    """Predict next crop for rotation"""
    try:
        if crop_rotation_model is None:
            raise HTTPException(status_code=503, detail="Crop rotation model not loaded")
        
        result = crop_rotation_model.predict(
            previous_crop=request.previousCrop,
            soil_pH=request.soilPH,
            soil_type=request.soilType,
            temperature=request.temperature,
            humidity=request.humidity,
            rainfall=request.rainfall,
            season=request.season
        )
        
        return PredictionResponse(
            prediction=result['recommended_next_crop'],
            confidence=result['confidence'],
            probabilities=result['probabilities']
        )
    except Exception as e:
        logger.error(f"Error in crop rotation prediction: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ml/predict-fertilizer")
async def predict_fertilizer(request: FertilizerRecommendationRequest):
    """Predict fertilizer dosages (N, P, K)"""
    try:
        if fertilizer_model is None:
            raise HTTPException(status_code=503, detail="Fertilizer recommendation model not loaded")
        
        # Map crop name to ensure it's in the fertilizer model's training data
        mapped_crop = map_crop_name(request.crop)
        logger.info(f"Mapped crop '{request.crop}' to '{mapped_crop}' for fertilizer prediction")
        
        result = fertilizer_model.predict(
            crop=mapped_crop,
            soil_type=request.soilType,
            soil_pH=request.soilPH,
            temperature=request.temperature,
            humidity=request.humidity,
            rainfall=request.rainfall,
            season=request.season
        )
        
        return {
            "N_dosage": result['N_dosage'],
            "P_dosage": result['P_dosage'],
            "K_dosage": result['K_dosage'],
            "total_dosage": result['total_dosage'],
            "modelVersion": "1.0.0"
        }
    except Exception as e:
        logger.error(f"Error in fertilizer prediction: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ml/ask-question")
async def ask_voice_question(request: VoiceAssistantRequest):
    """
    Ask a question to the AWS Voice Assistant API
    Proxies requests to the AWS Lambda-based question answering service
    """
    try:
        if not request.question or not request.question.strip():
            raise HTTPException(status_code=400, detail="Question cannot be empty")
        
        logger.info(f"Forwarding question to AWS Voice Assistant: {request.question[:50]}...")
        
        # Call AWS Voice Assistant API
        result = ask_question(request.question)
        
        if result.get('success'):
            return {
                "success": True,
                "answer": result.get('answer'),
                "status_code": result.get('status_code')
            }
        else:
            logger.error(f"AWS API error: {result.get('error')}")
            raise HTTPException(
                status_code=result.get('status_code', 500),
                detail=result.get('error', 'Failed to get answer from AWS API')
            )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error in voice question endpoint: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
