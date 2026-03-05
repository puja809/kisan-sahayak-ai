"""
FastAPI service for ML model predictions
Serves crop recommendation and rotation models
"""
from fastapi import FastAPI, HTTPException, File, UploadFile, Query, Form
from contextlib import asynccontextmanager
from pydantic import BaseModel
from typing import Optional
import os
import logging
import asyncio
from crop_recommendation_model import CropRecommendationModel
from crop_rotation_model import CropRotationModel
from fertilizer_recommendation_model import FertilizerRecommendationModel
from crop_name_mapper import map_crop_name
from aws_voice_assistant_client import ask_question_text, ask_question_audio, ask_question_text_stream, ask_question_audio_stream
from fastapi.responses import StreamingResponse
import json
from disease_detection_client import detect_disease as detect_disease_from_aws
from bedrock_mcp_agent import bedrock_mcp_agent # Import the new Bedrock MCP agent
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

@asynccontextmanager
async def lifespan(app: FastAPI):
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
    
    yield
    # Cleanup logic can go here during shutdown

app = FastAPI(title="ML Crop Prediction Service", version="1.0.0", lifespan=lifespan)

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
    language: str = "en"
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    city_name: Optional[str] = None

class PredictionResponse(BaseModel):
    prediction: str
    confidence: float
    probabilities: dict
    modelVersion: str = "1.0.0"

class DiseaseDetectionResponse(BaseModel):
    crop: str
    disease: str
    symptoms: str
    treatment: str
    prevention: str
    confidence: float = 0.0
    modelVersion: str = "1.0.0"
    raw_analysis: str = ""


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "crop_reco_model": crop_reco_model is not None,
        "crop_rotation_model": crop_rotation_model is not None,
        "fertilizer_model": fertilizer_model is not None
    }

# Supported languages for all AI features (Voice, Text, Disease Detection)
SUPPORTED_LANGUAGES = [
    {"code": "en", "name": "English",    "nativeName": "English"},
    {"code": "hi", "name": "Hindi",      "nativeName": "हिंदी"},
    {"code": "bn", "name": "Bengali",    "nativeName": "বাংলা"},
    {"code": "te", "name": "Telugu",     "nativeName": "తెలుగు"},
    {"code": "mr", "name": "Marathi",    "nativeName": "मराठी"},
    {"code": "ta", "name": "Tamil",      "nativeName": "தமிழ்"},
    {"code": "gu", "name": "Gujarati",   "nativeName": "ગુજરાતી"},
    {"code": "pa", "name": "Punjabi",    "nativeName": "ਪੰਜਾਬੀ"},
    {"code": "ka", "name": "Kannada",    "nativeName": "ಕನ್ನಡ"},
    {"code": "ml", "name": "Malayalam",  "nativeName": "മലയാളം"},
]

@app.get("/api/ml/languages")
async def get_supported_languages():
    """
    Returns the list of languages supported by all AI features:
    Voice Assistant, Text Assistant, and Disease Detection.
    Each language includes code, English name, and native script name.
    """
    return {
        "languages": SUPPORTED_LANGUAGES,
        "default": "en"
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

@app.get("/api/ml/fertilizer-crops")
async def get_fertilizer_crops():
    """Returns the list of crops supported by the fertilizer recommendation model"""
    from crop_name_mapper import CROP_NAME_MAPPING
    # Extract unique crop names from both keys and values to provide a comprehensive list
    supported_crops = set(CROP_NAME_MAPPING.keys()).union(set(CROP_NAME_MAPPING.values()))
    sorted_crops = sorted(list(supported_crops))
    return {"crops": sorted_crops}

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
    Ask a question to the Krishi Sahayak Assistant.
    This routes the text through the Bedrock LangGraph MCP Agent which connects
    to all local Spring Boot MCP servers.
    """
    try:
        if not request.question or not request.question.strip():
            raise HTTPException(status_code=400, detail="Question cannot be empty")
        
        logger.info(f"Received request on /api/ml/ask-question:")
        logger.info(f" - Question (truncated): {request.question[:50]}...")
        logger.info(f" - Language: {request.language}")
        logger.info(f" - Location: lat={request.latitude}, lng={request.longitude}, city={request.city_name}")
        
        # Use Bedrock MCP agent
        answer = await bedrock_mcp_agent.invoke(request.question, latitude=request.latitude, longitude=request.longitude, city_name=request.city_name)
        
        return {
            "success": True,
            "answer": answer,
            "status_code": 200
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error in ask-question endpoint (Bedrock MCP): {e}")
        # Fallback to AWS Voice Assistant API if bedrock MCP fails
        try:
            logger.info("Falling back to AWS Voice Assistant API...")
            result = ask_question_text(request.question)
            if result.get('success'):
                return {
                    "success": True,
                    "answer": result.get('answer'),
                    "status_code": result.get('status_code')
                }
            raise Exception(result.get('error', 'Fallback API failed'))
        except Exception as fallback_err:
            logger.error(f"Fallback API also failed: {fallback_err}")
            raise HTTPException(status_code=500, detail=f"Error generating answer: {str(e)}")

@app.post("/api/ml/ask-question/stream")
async def ask_voice_question_stream(request: VoiceAssistantRequest):
    """
    Stream a question to the Krishi Sahayak Assistant.
    """
    try:
        if not request.question or not request.question.strip():
            raise HTTPException(status_code=400, detail="Question cannot be empty")
            
        logger.info(f"Received request on /api/ml/ask-question/stream:")
        logger.info(f" - Question (truncated): {request.question[:50]}...")
        logger.info(f" - Language: {request.language}")
        logger.info(f" - Location: lat={request.latitude}, lng={request.longitude}, city={request.city_name}")

        # Route directly to the AWS Lambda which is now streaming
        return StreamingResponse(ask_question_text_stream(request.question, request.language), media_type="text/event-stream")
    except Exception as e:
        logger.error(f"Error in ask-question stream endpoint: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ml/disease-detect", response_model=DiseaseDetectionResponse)
async def detect_disease_endpoint(
    image: UploadFile = File(...),
    language: str = Query(default="en", description="Language code: en, hi, bn"),
    session_id: str = Query(default="default-session", description="Session ID for chat memory")
):
    """
    Detect crop disease from image and return disease details.
    Proxies to AWS Lambda disease-detect endpoint.
    Supports language preference (en/hi/bn) — Lambda responds in the requested language.
    Returns: crop name, disease, symptoms, treatment, prevention, raw_analysis
    """
    try:
        image_bytes = await image.read()
        if not image_bytes or len(image_bytes) == 0:
            raise HTTPException(status_code=400, detail="Image data is required")
        
        logger.info(f"Processing disease detection image: {len(image_bytes)} bytes, language={language}")
        
        # Call disease detection client — passes language & session_id to Lambda
        result = detect_disease_from_aws(image_bytes, language=language, session_id=session_id)
        
        return DiseaseDetectionResponse(
            crop=result["crop"],
            disease=result["disease"],
            symptoms=result["symptoms"],
            treatment=result["treatment"],
            prevention=result["prevention"],
            confidence=result["confidence"],
            raw_analysis=result.get("raw_analysis", "")
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error in disease detection: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ml/ask-question-audio")
async def ask_question_with_audio(audio: bytes = File(...)):
    """
    Process audio input and get response from AWS Voice Assistant
    1. Receives audio from UI
    2. Sends to AWS /ask-voice endpoint
    3. AWS returns: transcribed_text, text (answer), audio (MP3 base64)
    4. Returns all three to UI for display and playback
    """
    try:
        if not audio or len(audio) == 0:
            raise HTTPException(status_code=400, detail="Audio data is required")
        
        logger.info(f"Processing audio input: {len(audio)} bytes")
        
        # Convert audio to base64 for AWS API
        import base64
        base64_audio = base64.b64encode(audio).decode('utf-8')
        
        # Call AWS Voice Assistant API with audio
        result = ask_question_audio(base64_audio)
        
        if result.get('success'):
            # Extract response from AWS
            answer = result.get('answer', '')
            transcribed_text = result.get('transcribed_text', '')
            audio_response = result.get('audio')
            language = result.get('language', 'en')
            
            logger.info(f"AWS Response - Transcribed: {transcribed_text[:50]}...")
            logger.info(f"AWS Response - Answer: {answer[:50]}...")
            logger.info(f"AWS Response - Audio present: {audio_response is not None}")
            
            return {
                "success": True,
                "transcribedText": transcribed_text,
                "answer": answer,
                "audio": audio_response,
                "language": language,
                "status_code": 200
            }
        else:
            logger.error(f"AWS API error: {result.get('error')}")
            status_code = result.get('status_code', 500)
            raise HTTPException(
                status_code=status_code,
                detail=result.get('error', 'Failed to process audio')
            )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error in audio question endpoint: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ml/ask-question-audio/stream")
async def ask_question_with_audio_stream(
    audio: bytes = File(...),
    language: str = Form("en")
):
    """
    Process audio input and stream response from AWS Voice Assistant
    """
    try:
        if not audio or len(audio) == 0:
            raise HTTPException(status_code=400, detail="Audio data is required")
        
        # Convert audio to base64 for AWS API
        import base64
        base64_audio = base64.b64encode(audio).decode('utf-8')
        
        return StreamingResponse(ask_question_audio_stream(base64_audio, language), media_type="text/event-stream")
    except Exception as e:
        logger.error(f"Error in audio question stream endpoint: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
