"""FastAPI application for AI/ML Service."""
import time
from contextlib import asynccontextmanager
from datetime import datetime
from typing import Optional

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.disease_detection_service import disease_detection_service
from app.embedding_service import embedding_service
from app.image_validation_service import image_validation_service
from app.logging_config import logger
from app.models import (
    BatchEmbeddingRequest,
    BatchEmbeddingResponse,
    DiseaseDetectionRequest,
    DiseaseDetectionResponse,
    DocumentCreate,
    EmbeddingRequest,
    EmbeddingResponse,
    HealthResponse,
    ImageValidationRequest,
    ImageValidationResponse,
    SearchRequest,
    SearchResponse,
)
from app.search_service import semantic_search_service
from app.vector_store import vector_store
from app.voice_models import (
    ASRResponse,
    AudioFormat,
    AudioQuality,
    DisambiguationResponse,
    OCRRequest,
    OCRResponse,
    SupportedLanguage,
    TTSRequest,
    TTSResponse,
    TranslationRequest,
    TranslationResponse,
    VoiceProcessingRequest,
    VoiceProcessingResponse,
)
from app.voice_pipeline import get_voice_pipeline, VoiceProcessingPipeline
from app.bhashini_client import get_bhashini_client, BhashiniClient


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager for startup and shutdown events."""
    # Startup
    logger.info("Starting AI/ML Service...")
    
    try:
        # Connect to MongoDB
        await vector_store.connect()
        logger.info("MongoDB connection established")
        
        # Load embedding model
        await embedding_service.load_model()
        logger.info("Embedding model loaded")
        
        # Load disease detection model
        await disease_detection_service.load_model()
        logger.info("Disease detection model loaded")
        
        logger.info("AI/ML Service started successfully")
        
        yield
        
    finally:
        # Shutdown
        logger.info("Shutting down AI/ML Service...")
        
        # Unload models
        await embedding_service.unload_model()
        await disease_detection_service.unload_model()
        
        # Disconnect from MongoDB
        await vector_store.disconnect()
        
        logger.info("AI/ML Service shut down successfully")


# Create FastAPI application
app = FastAPI(
    title="Indian Farmer Assistance AI Service",
    description="AI/ML service for document embeddings and semantic search",
    version="1.0.0",
    lifespan=lifespan
)

# Configure CORS for cross-origin requests from Spring Boot services
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Exception handlers
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Handle uncaught exceptions."""
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "detail": str(exc) if settings.debug else None,
            "timestamp": datetime.utcnow().isoformat()
        }
    )


# Health check endpoint
@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """
    Health check endpoint for monitoring.
    
    Returns the service status, MongoDB connection status, and embedding model status.
    """
    mongodb_connected = await vector_store.is_connected()
    model_loaded = embedding_service.is_model_loaded()
    
    status = "healthy" if (mongodb_connected and model_loaded) else "unhealthy"
    
    return HealthResponse(
        status=status,
        mongodb_connected=mongodb_connected,
        embedding_model_loaded=model_loaded,
        timestamp=datetime.utcnow()
    )


# Embedding generation endpoints
@app.post("/api/v1/ai/embeddings/generate", response_model=EmbeddingResponse, tags=["Embeddings"])
async def generate_embedding(request: EmbeddingRequest):
    """
    Generate a 768-dimensional embedding for the given text.
    
    - **text**: The text to generate an embedding for
    """
    if not request.text or not request.text.strip():
        raise HTTPException(status_code=400, detail="Text cannot be empty")
    
    try:
        embedding = await embedding_service.generate_embedding(request.text)
        
        return EmbeddingResponse(
            text=request.text,
            embedding=embedding,
            dimension=len(embedding)
        )
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/api/v1/ai/embeddings/generate/batch", response_model=BatchEmbeddingResponse, tags=["Embeddings"])
async def generate_batch_embeddings(request: BatchEmbeddingRequest):
    """
    Generate embeddings for multiple texts in batch for efficiency.
    
    - **texts**: List of texts to generate embeddings for
    """
    if not request.texts:
        raise HTTPException(status_code=400, detail="Texts list cannot be empty")
    
    if len(request.texts) > 100:
        raise HTTPException(status_code=400, detail="Maximum 100 texts per batch")
    
    try:
        embeddings = await embedding_service.generate_embeddings_batch(request.texts)
        
        return BatchEmbeddingResponse(
            embeddings=embeddings,
            dimension=embedding_service.get_embedding_dimension(),
            count=len(embeddings)
        )
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))


# Semantic search endpoints
@app.post("/api/v1/ai/search/semantic", response_model=SearchResponse, tags=["Search"])
async def semantic_search(request: SearchRequest):
    """
    Perform semantic search using vector similarity.
    
    - **query**: The search query text
    - **filters**: Optional filters for category, state, tags
    - **limit**: Maximum number of results (default: 10, max: 100)
    """
    if not request.query or not request.query.strip():
        raise HTTPException(status_code=400, detail="Query cannot be empty")
    
    try:
        response = await semantic_search_service.search(
            query=request.query,
            filters=request.filters,
            limit=request.limit
        )
        
        return response
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))


@app.post("/api/v1/ai/search/semantic/advanced", response_model=SearchResponse, tags=["Search"])
async def advanced_semantic_search(request: SearchRequest):
    """
    Perform semantic search with pre-computed embedding or advanced options.
    
    - **query**: The search query text (optional if query_embedding provided)
    - **query_embedding**: Pre-computed embedding vector (optional if query provided)
    - **filters**: Optional filters for category, state, tags
    - **limit**: Maximum number of results
    """
    if not request.query and not request.query_embedding:
        raise HTTPException(status_code=400, detail="Either query or query_embedding must be provided")
    
    try:
        if request.query_embedding:
            # Use pre-computed embedding
            response = await semantic_search_service.search_with_embedding(
                query_embedding=request.query_embedding,
                filters=request.filters,
                limit=request.limit
            )
        else:
            # Generate embedding from query text
            response = await semantic_search_service.search(
                query=request.query,
                filters=request.filters,
                limit=request.limit
            )
        
        return response
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))


# Document management endpoints (for admin service integration)
@app.post("/api/v1/ai/documents/index", tags=["Documents"])
async def index_document(document: DocumentCreate):
    """
    Index a document for semantic search.
    
    - **document_id**: Unique identifier for the document
    - **title**: Document title
    - **category**: Document category (schemes, guidelines, crop_info, disease_mgmt, market_intel)
    - **content**: Document content text
    - **metadata**: Optional metadata (source, state, tags, etc.)
    """
    if not document.document_id or not document.title or not document.content:
        raise HTTPException(status_code=400, detail="document_id, title, and content are required")
    
    try:
        doc_id = await semantic_search_service.index_document(document)
        return {"document_id": doc_id, "status": "indexed"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/v1/ai/documents/{document_id}", tags=["Documents"])
async def delete_indexed_document(document_id: str):
    """
    Soft-delete a document from the search index.
    """
    deleted = await semantic_search_service.delete_document(document_id)
    
    if not deleted:
        raise HTTPException(status_code=404, detail="Document not found")
    
    return {"document_id": document_id, "status": "deleted"}


# =============================================================================
# Disease Detection Endpoints
# =============================================================================

@app.post("/api/v1/ai/disease/validate", response_model=ImageValidationResponse, tags=["Disease Detection"])
async def validate_image(request: ImageValidationRequest):
    """
    Validate an image for disease detection.
    
    - **filename**: Original filename
    - **file_size**: File size in bytes
    - **content_type**: MIME type (image/jpeg, image/png)
    
    Returns validation status and any warnings.
    """
    response = image_validation_service.validate(request)
    return response


@app.post("/api/v1/ai/disease/detect", response_model=DiseaseDetectionResponse, tags=["Disease Detection"])
async def detect_disease(request: DiseaseDetectionRequest):
    """
    Detect crop diseases from an uploaded image.
    
    - **user_id**: User ID for the farmer
    - **crop_id**: Optional crop ID
    - **image_data**: Base64 encoded image data
    - **filename**: Original filename
    - **content_type**: MIME type (image/jpeg, image/png)
    
    Returns disease detection results with treatment recommendations.
    """
    # Validate image first
    validation_response = image_validation_service.validate_with_content(
        image_data=request.image_data,
        filename=request.filename,
        content_type=request.content_type
    )
    
    if not validation_response.is_valid:
        raise HTTPException(
            status_code=400, 
            detail={
                "error": "Image validation failed",
                "errors": validation_response.errors,
                "warnings": validation_response.warnings
            }
        )
    
    # Check if model is loaded
    if not disease_detection_service.is_model_loaded():
        raise HTTPException(status_code=503, detail="Disease detection model not loaded")
    
    try:
        # Run disease detection
        response = await disease_detection_service.detect(request)
        
        # Add validation warnings to response if any
        if validation_response.warnings:
            response.message = (response.message or "") + " Note: " + " ".join(validation_response.warnings)
        
        return response
        
    except Exception as e:
        logger.error(f"Disease detection failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Disease detection failed: {str(e)}")


@app.get("/api/v1/ai/disease/treatment/{disease_name}", tags=["Disease Detection"])
async def get_treatment_recommendation(disease_name: str):
    """
    Get treatment recommendations for a specific disease.
    
    - **disease_name**: Name of the disease (e.g., blast, bacterial_blight, powdery_mildew)
    
    Returns treatment options, preventive measures, and KVK contact information.
    """
    treatment = disease_detection_service.get_treatment_for_disease(disease_name)
    
    if not treatment:
        raise HTTPException(
            status_code=404, 
            detail=f"Treatment recommendations not found for disease: {disease_name}"
        )
    
    return treatment


@app.get("/api/v1/ai/disease/supported", tags=["Disease Detection"])
async def get_supported_diseases_and_crops():
    """
    Get list of supported diseases and crops for disease detection.
    
    Returns lists of diseases and crops that can be analyzed.
    """
    return {
        "diseases": disease_detection_service.get_supported_diseases(),
        "crops": disease_detection_service.get_supported_crops()
    }


@app.get("/api/v1/ai/disease/model-info", tags=["Disease Detection"])
async def get_disease_detection_model_info():
    """
    Get information about the disease detection model.
    
    Returns model version, device (GPU/CPU), cache status, and memory usage.
    """
    return disease_detection_service.get_model_info()


# =============================================================================
# Voice Agent Endpoints
# =============================================================================

@app.post("/api/v1/ai/voice/process", response_model=VoiceProcessingResponse, tags=["Voice"])
async def process_voice_request(request: VoiceProcessingRequest):
    """
    Process a complete voice query through the full pipeline.
    
    This endpoint handles:
    - Voice Activity Detection (VAD)
    - Automatic Speech Recognition (ASR)
    - Neural Machine Translation (NMT) to English
    - Intent recognition and entity extraction
    - Response generation
    - NMT translation back to user's language
    - Text-to-Speech (TTS) synthesis
    
    - **audio_data**: Base64 encoded audio data
    - **audio_format**: Audio format (wav, mp3, etc.)
    - **source_language**: Farmer's language
    - **target_language**: Response language (usually same as source)
    - **session_id**: Optional session ID for conversation continuity
    - **enable_fallback**: Enable fallback to text if voice fails
    """
    if not request.audio_data:
        raise HTTPException(status_code=400, detail="Audio data is required")
    
    try:
        pipeline = get_voice_pipeline()
        response = await pipeline.process_voice(request)
        return response
    except Exception as e:
        logger.error(f"Voice processing error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/ai/voice/asr", response_model=ASRResponse, tags=["Voice"])
async def transcribe_audio(
    audio_data: str,
    language: SupportedLanguage = SupportedLanguage.HINDI,
    audio_format: AudioFormat = AudioFormat.WAV
):
    """
    Transcribe audio to text using Bhashini ASR.
    
    - **audio_data**: Base64 encoded audio data
    - **language**: Source language for transcription
    - **audio_format**: Audio format (wav, mp3, ogg, webm, flac)
    """
    if not audio_data:
        raise HTTPException(status_code=400, detail="Audio data is required")
    
    try:
        bhashini = get_bhashini_client()
        response = await bhashini.asr.transcribe(
            audio_data=audio_data,
            language=language,
            audio_format=audio_format
        )
        return response
    except Exception as e:
        logger.error(f"ASR error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/ai/voice/tts", response_model=TTSResponse, tags=["Voice"])
async def synthesize_speech(request: TTSRequest):
    """
    Convert text to speech using Bhashini TTS.
    
    - **text**: Text to convert to speech
    - **language**: Target language for synthesis
    - **voice**: Voice type (male, female)
    - **speed**: Speech speed (0.5 to 2.0)
    - **quality**: Audio quality (high, medium, low)
    """
    if not request.text or not request.text.strip():
        raise HTTPException(status_code=400, detail="Text cannot be empty")
    
    try:
        bhashini = get_bhashini_client()
        response = await bhashini.tts.synthesize(
            text=request.text,
            language=request.language,
            voice=request.voice,
            speed=request.speed,
            quality=request.quality
        )
        return response
    except Exception as e:
        logger.error(f"TTS error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/ai/voice/translate", response_model=TranslationResponse, tags=["Voice"])
async def translate_text(request: TranslationRequest):
    """
    Translate text between languages using Bhashini NMT.
    
    - **text**: Text to translate
    - **source_language**: Source language code
    - **target_language**: Target language code
    """
    if not request.text or not request.text.strip():
        raise HTTPException(status_code=400, detail="Text cannot be empty")
    
    try:
        bhashini = get_bhashini_client()
        response = await bhashini.nmt.translate(
            text=request.text,
            source_language=request.source_language,
            target_language=request.target_language
        )
        return response
    except Exception as e:
        logger.error(f"Translation error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/ai/ocr/extract", response_model=OCRResponse, tags=["OCR"])
async def extract_text_from_image(request: OCRRequest):
    """
    Extract text from images using Bhashini OCR.
    
    Supports all Indian scripts and can read documents like:
    - Soil Health Cards
    - Land Records
    - Government Documents
    
    - **image_data**: Base64 encoded image data
    - **language**: Language of text in the image
    """
    if not request.image_data:
        raise HTTPException(status_code=400, detail="Image data is required")
    
    try:
        bhashini = get_bhashini_client()
        response = await bhashini.ocr.extract_text(
            image_data=request.image_data,
            language=request.language
        )
        return response
    except Exception as e:
        logger.error(f"OCR error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/ai/voice/language/configure", tags=["Voice"])
async def configure_voice_language(
    session_id: str,
    language: SupportedLanguage = SupportedLanguage.HINDI
):
    """
    Configure Bhashini API for a specific language.
    
    This should be called when a farmer selects their preferred language.
    All subsequent voice interactions will use this language configuration.
    
    - **session_id**: User session ID
    - **language**: Selected language for voice interactions
    """
    try:
        pipeline = get_voice_pipeline()
        config = pipeline.configure_language(session_id, language)
        return {
            "session_id": session_id,
            "language": language.value,
            "asr_model": config.asr_model,
            "nmt_model": config.nmt_model,
            "tts_voice": config.tts_voice,
            "tts_speed": config.tts_speed
        }
    except Exception as e:
        logger.error(f"Language configuration error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/v1/ai/voice/disambiguation/resolve", response_model=DisambiguationResponse, tags=["Voice"])
async def resolve_disambiguation(
    confirmation_id: str,
    selected_option: str
):
    """
    Resolve a disambiguation request.
    
    Called when the user selects an option from a disambiguation prompt.
    
    - **confirmation_id**: Confirmation ID from the disambiguation response
    - **selected_option**: User's selected option
    """
    try:
        pipeline = get_voice_pipeline()
        response = pipeline.disambiguation_handler.resolve_disambiguation(
            confirmation_id=confirmation_id,
            selected_option=selected_option
        )
        
        if not response:
            raise HTTPException(status_code=404, detail="Disambiguation request not found")
        
        return response
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Disambiguation resolution error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


# Root endpoint
@app.get("/", tags=["Root"])
async def root():
    """Root endpoint with API information."""
    return {
        "service": "Indian Farmer Assistance AI Service",
        "version": "1.0.0",
        "endpoints": {
            "health": "/health",
            "embeddings": {
                "generate": "POST /api/v1/ai/embeddings/generate",
                "batch": "POST /api/v1/ai/embeddings/generate/batch"
            },
            "search": {
                "semantic": "POST /api/v1/ai/search/semantic",
                "advanced": "POST /api/v1/ai/search/semantic/advanced"
            },
            "documents": {
                "index": "POST /api/v1/ai/documents/index",
                "delete": "DELETE /api/v1/ai/documents/{document_id}"
            },
            "disease_detection": {
                "validate": "POST /api/v1/ai/disease/validate",
                "detect": "POST /api/v1/ai/disease/detect",
                "treatment": "GET /api/v1/ai/disease/treatment/{disease_name}",
                "supported": "GET /api/v1/ai/disease/supported"
            }
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.app_host,
        port=settings.app_port,
        reload=settings.debug
    )