"""Data models for Voice Agent Service."""
from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class SupportedLanguage(str, Enum):
    """Supported Indian languages for Bhashini API."""
    HINDI = "hi"
    BENGALI = "bn"
    TAMIL = "ta"
    TELUGU = "te"
    MARATHI = "mr"
    GUJARATI = "gu"
    KANNADA = "kn"
    MALAYALAM = "ml"
    PUNJABI = "pa"
    ODIA = "or"
    ASSAMESE = "as"
    ENGLISH = "en"


class AudioFormat(str, Enum):
    """Supported audio formats."""
    WAV = "wav"
    MP3 = "mp3"
    OGG = "ogg"
    WEBM = "webm"
    FLAC = "flac"


class AudioQuality(str, Enum):
    """Audio quality levels for low-bandwidth optimization."""
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"


class LanguageConfig(BaseModel):
    """Language configuration for Bhashini API."""
    source_language: SupportedLanguage
    target_language: SupportedLanguage = SupportedLanguage.ENGLISH
    asr_model: str = "default"
    nmt_model: str = "agriculture"
    tts_voice: str = "female"
    tts_speed: float = 1.0


class ASRResponse(BaseModel):
    """Response model for Automatic Speech Recognition."""
    text: str
    confidence: float
    language: SupportedLanguage
    is_final: bool = True
    processing_time_ms: float


class TranslationResponse(BaseModel):
    """Response model for translation."""
    original_text: str
    translated_text: str
    source_language: SupportedLanguage
    target_language: SupportedLanguage
    confidence: float
    processing_time_ms: float


class TTSResponse(BaseModel):
    """Response model for Text-to-Speech."""
    text: str
    audio_data: str
    audio_format: AudioFormat
    duration_seconds: float
    processing_time_ms: float


class OCRResponse(BaseModel):
    """Response model for OCR text extraction."""
    extracted_text: str
    confidence: float
    language: SupportedLanguage
    processing_time_ms: float


class VoiceProcessingRequest(BaseModel):
    """Request model for full voice processing pipeline."""
    audio_data: str
    audio_format: AudioFormat = AudioFormat.WAV
    source_language: SupportedLanguage = SupportedLanguage.HINDI
    target_language: SupportedLanguage = SupportedLanguage.HINDI
    session_id: Optional[str] = None
    enable_fallback: bool = True


class VoiceProcessingResponse(BaseModel):
    """Response model for full voice processing pipeline."""
    session_id: str
    user_text: str
    user_text_english: str
    system_response: str
    system_response_audio: Optional[str] = None
    intent: Optional[str] = None
    entities: Dict[str, Any] = None
    confidence: float
    disambiguation_required: bool = False
    disambiguation_options: List[str] = None
    fallback_used: bool = False
    fallback_reason: Optional[str] = None
    processing_time_ms: float


class FallbackMode(str, Enum):
    """Fallback modes for voice processing."""
    VOICE = "voice"
    TEXT = "text"
    CACHED = "cached"


class VoiceActivityDetectionResult(BaseModel):
    """Result from Voice Activity Detection."""
    is_speech: bool
    confidence: float
    start_time: Optional[float] = None
    end_time: Optional[float] = None
    speech_duration_ms: Optional[float] = None


class ConversationTurn(BaseModel):
    """A single turn in a voice conversation."""
    timestamp: datetime
    user_audio_path: Optional[str] = None
    user_text: str
    user_text_translated: Optional[str] = None
    system_response: str
    system_response_translated: Optional[str] = None
    system_audio_path: Optional[str] = None
    intent: Optional[str] = None
    entities: Dict[str, Any] = Field(default_factory=dict)
    confidence: float


class VoiceConversation(BaseModel):
    """Complete voice conversation session."""
    session_id: str
    user_id: str
    language: SupportedLanguage
    conversations: List[ConversationTurn] = Field(default_factory=list)
    created_at: datetime
    updated_at: datetime


class DisambiguationRequest(BaseModel):
    """Request for disambiguation of ambiguous query."""
    query: str
    options: List[str]
    language: SupportedLanguage


class DisambiguationResponse(BaseModel):
    """Response to disambiguation request."""
    selected_option: str
    confidence: float


class FallbackResponse(BaseModel):
    """Response when fallback is used."""
    fallback_mode: FallbackMode
    reason: str
    response: str


class TTSRequest(BaseModel):
    """Request model for Text-to-Speech."""
    text: str
    language: SupportedLanguage = SupportedLanguage.HINDI
    voice: str = "female"
    speed: float = 1.0
    quality: AudioQuality = AudioQuality.MEDIUM


class TranslationRequest(BaseModel):
    """Request model for translation."""
    text: str
    source_language: SupportedLanguage
    target_language: SupportedLanguage


class OCRRequest(BaseModel):
    """Request model for OCR text extraction."""
    image_data: str
    language: SupportedLanguage = SupportedLanguage.HINDI