"""
AI/ML Service - Main Application Entry Point

This service handles:
- Crop disease detection using computer vision
- Yield prediction using machine learning
- Recommendation engine for crops and fertilizers
- Voice agent orchestration with Bhashini integration
- Vector similarity search for document embeddings
"""

import logging
import sys
from contextlib import asynccontextmanager
from pathlib import Path

import yaml
from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from loguru import logger
from pythonjsonlogger import jsonlogger

from app.routers import disease, embeddings, health, recommendations, voice, yield_prediction


def setup_logging(environment: str = "dev"):
    """Configure structured JSON logging for production environments."""
    
    # Remove default handler
    logger.remove()
    
    # Configure log format based on environment
    if environment == "prod":
        log_format = (
            "{time:YYYY-MM-DDTHH:mm:ss.SSSZ} | {level: <8} | "
            "{name}:{function}:{line} | {message} | "
            "service=ai-service | environment={extra[environment]} | "
            "trace_id={extra[trace_id]} | span_id={extra[span_id]}"
        )
    else:
        log_format = (
            "<green>{time:YYYY-MM-DD HH:mm:ss}</green> | "
            "<level>{level: <8}</level> | "
            "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> | "
            "<level>{message}</level>"
        )
    
    # Add console sink
    logger.add(
        sys.stdout,
        format=log_format,
        level=logging.DEBUG if environment == "dev" else logging.INFO,
        colorize=environment != "prod",
    )
    
    # Add file sink for production
    if environment == "prod":
        log_path = Path("/var/log/farmer-app/ai-service.log")
        log_path.parent.mkdir(parents=True, exist_ok=True)
        
        logger.add(
            str(log_path),
            format=log_format,
            level=logging.INFO,
            rotation="100 MB",
            retention="30 days",
            serialize=True,  # JSON format for file
        )


def load_config(config_path: str = "config.yaml") -> dict:
    """Load application configuration from YAML file."""
    config_file = Path(config_path)
    if config_file.exists():
        with open(config_file, "r") as f:
            return yaml.safe_load(f)
    return {}


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager for startup and shutdown events."""
    # Startup
    logger.info("AI/ML Service starting up...")
    
    # Initialize connections, load models, etc.
    logger.info("Initializing MongoDB connection...")
    logger.info("Initializing Redis connection...")
    logger.info("Loading ML models...")
    
    yield
    
    # Shutdown
    logger.info("AI/ML Service shutting down...")
    logger.info("Closing connections...")
    logger.info("Cleanup complete.")


def create_app(config: dict = None) -> FastAPI:
    """Create and configure the FastAPI application."""
    
    environment = config.get("environment", "dev") if config else "dev"
    setup_logging(environment)
    
    app = FastAPI(
        title="Indian Farmer Assistance AI Service",
        description="AI/ML services for crop disease detection, yield prediction, recommendations, and voice processing",
        version="1.0.0",
        lifespan=lifespan,
        docs_url="/docs" if environment == "dev" else None,
        redoc_url="/redoc" if environment == "dev" else None,
    )
    
    # CORS configuration
    app.add_middleware(
        CORSMiddleware,
        allow_origins=config.get("cors_origins", ["http://localhost:4200"]) if config else ["http://localhost:4200"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    
    # Exception handlers
    @app.exception_handler(Exception)
    async def global_exception_handler(request: Request, exc: Exception):
        logger.exception(f"Unhandled exception: {exc}")
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={"error": "Internal server error", "message": str(exc)},
        )
    
    # Include routers
    app.include_router(health.router, prefix="/health", tags=["Health"])
    app.include_router(disease.router, prefix="/api/v1/ai/disease", tags=["Disease Detection"])
    app.include_router(yield_prediction.router, prefix="/api/v1/ai/yield", tags=["Yield Prediction"])
    app.include_router(recommendations.router, prefix="/api/v1/ai/recommendations", tags=["Recommendations"])
    app.include_router(voice.router, prefix="/api/v1/ai/voice", tags=["Voice Agent"])
    app.include_router(embeddings.router, prefix="/api/v1/ai/embeddings", tags=["Embeddings"])
    
    logger.info(f"AI/ML Service initialized in {environment} mode")
    
    return app


# Create application instance
config = load_config()
app = create_app(config)


if __name__ == "__main__":
    import uvicorn
    
    host = config.get("host", "0.0.0.0") if config else "0.0.0.0"
    port = int(config.get("port", 8000)) if config else 8000
    
    uvicorn.run(
        "main:app",
        host=host,
        port=port,
        reload=config.get("reload", True) if config else True,
        log_level="info",
    )