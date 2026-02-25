"""Configuration management for AI Service."""
import os
from functools import lru_cache
from typing import Optional

from pydantic import Field
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # MongoDB Configuration
    mongodb_uri: str = Field(default="mongodb://localhost:27017", description="MongoDB connection URI")
    mongodb_database: str = Field(default="indian_farmer_app", description="MongoDB database name")
    mongodb_collection_documents: str = Field(default="documents", description="Documents collection name")
    mongodb_collection_voice_conversations: str = Field(default="voice_conversations", description="Voice conversations collection name")

    # Application Configuration
    app_host: str = Field(default="0.0.0.0", description="Application host")
    app_port: int = Field(default=8000, description="Application port")
    debug: bool = Field(default=True, description="Debug mode")

    # Vector Database Configuration
    vector_dimension: int = Field(default=768, description="Vector embedding dimension")
    vector_similarity_threshold: float = Field(default=0.7, description="Minimum similarity threshold for search")
    max_search_results: int = Field(default=10, description="Maximum number of search results")

    # Bhashini API Configuration
    bhashini_api_key: Optional[str] = Field(default=None, description="Bhashini API key")
    bhashini_api_url: str = Field(default="https://api.bhashini.gov.in", description="Bhashini API base URL")
    bhashini_asr_url: str = Field(default="https://asr-api.bhashini.gov.in", description="Bhashini ASR API URL")
    bhashini_nmt_url: str = Field(default="https://nmt-api.bhashini.gov.in", description="Bhashini NMT API URL")
    bhashini_tts_url: str = Field(default="https://tts-api.bhashini.gov.in", description="Bhashini TTS API URL")
    bhashini_ocr_url: str = Field(default="https://ocr-api.bhashini.gov.in", description="Bhashini OCR API URL")

    # Voice Agent Configuration
    voice_fallback_enabled: bool = Field(default=True, description="Enable voice fallback to text")
    voice_disambiguation_enabled: bool = Field(default=True, description="Enable disambiguation for ambiguous queries")
    voice_vad_threshold: float = Field(default=0.02, description="Voice Activity Detection energy threshold")
    voice_max_retries: int = Field(default=3, description="Maximum retries for voice API calls")
    voice_session_timeout_minutes: int = Field(default=30, description="Voice session timeout in minutes")

    # Low-bandwidth Optimization
    audio_quality_default: str = Field(default="medium", description="Default audio quality (high, medium, low)")
    audio_compression_enabled: bool = Field(default=True, description="Enable audio compression")
    audio_max_duration_seconds: int = Field(default=60, description="Maximum audio duration in seconds")

    # Logging Configuration
    log_level: str = Field(default="INFO", description="Logging level")

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = True


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()


settings = get_settings()