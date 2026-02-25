from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field

class SupportedLanguage(str, Enum):
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
    WAV = "wav"
    MP3 = "mp3"
    OGG = "ogg"
    WEBM = "webm"
    FLAC = "flac"

class AudioQuality(str, Enum):
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"

class LanguageConfig(BaseModel):
    source_language: SupportedLanguage
    target_language: SupportedLanguage = SupportedLanguage.ENGLISH
    asr_model: str = "default"
    nmt_model: str = "agriculture"
    tts_voice: str = "female"
    tts_speed: float = 1.0

class ASRResponse(BaseModel):
    text: str
    confidence: float
    language: SupportedLanguage
    is_final: bool = True
    processing_time_ms: float

class TranslationResponse(BaseModel):
    original_text: str
    translated_text: str
    source_language: SupportedLanguage
    target_language: SupportedLanguage
    confidence: float
    processing_time_ms: float

class TTSResponse(BaseModel):
    text: str
    audio_data: str
    audio_format: AudioFormat
    duration_seconds: float
    processing_time_ms: float

class OCRResponse(BaseModel):
    extracted_text: str
    confidence: float
    language: SupportedLanguage
    processing_time_ms: float

class VoiceProcessingRequest(BaseModel):
    audio_data: str
    audio_format: AudioFormat = AudioFormat.WAV
    source_language: SupportedLanguage = SupportedLanguage.HINDI
    target_language: SupportedLanguage = SupportedLanguage.HINDI
    session_id: Optional[str] = None
    enable_fallback: bool = True

class VoiceProcessingResponse(BaseModel):
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
    VOICE = "voice"
    TEXT = "text"
    CACHED = "cached"

class VoiceActivityDetectionResult(BaseModel):
    is_speech: bool
    confidence: float
    start_time: Optional[float] = None
    end_time: Optional[float] = None
    speech_duration_ms: Optional[float] = None
