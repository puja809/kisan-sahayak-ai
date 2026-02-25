"""Unit tests for Voice Agent Service."""
import base64
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from app.voice_models import (
    AudioFormat,
    AudioQuality,
    FallbackMode,
    SupportedLanguage,
    TTSResponse,
    TranslationResponse,
    VoiceActivityDetectionResult,
    VoiceProcessingRequest,
    VoiceProcessingResponse,
)
from app.voice_pipeline import (
    DisambiguationHandler,
    FallbackHandler,
    IntentRecognizer,
    VoiceActivityDetector,
    VoiceProcessingPipeline,
)


class TestVoiceActivityDetector:
    def test_vad_detects_speech_above_threshold(self):
        vad = VoiceActivityDetector(energy_threshold=0.02)
        audio_bytes = b'\x00' * 100 + b'\xff' * 100 + b'\x00' * 100
        result = vad.detect(audio_bytes)
        assert isinstance(result, VoiceActivityDetectionResult)
        assert result.confidence >= 0.0
        assert result.confidence <= 1.0

    def test_vad_empty_audio(self):
        vad = VoiceActivityDetector()
        result = vad.detect(b'')
        assert isinstance(result, VoiceActivityDetectionResult)
        assert result.confidence == 0.0
        assert result.is_speech == False


class TestIntentRecognizer:
    def test_recognize_weather_query(self):
        recognizer = IntentRecognizer()
        intent, confidence = recognizer.recognize_intent('What is the weather forecast?')
        assert intent == "weather_query"
        assert confidence > 0.0

    def test_recognize_price_query(self):
        recognizer = IntentRecognizer()
        intent, confidence = recognizer.recognize_intent('What is the price of wheat?')
        assert intent == "price_query"
        assert confidence > 0.0

    def test_recognize_general_query(self):
        recognizer = IntentRecognizer()
        intent, confidence = recognizer.recognize_intent('Random text')
        assert intent == "general_query"
        assert confidence > 0.0

    def test_extract_crop_entities(self):
        recognizer = IntentRecognizer()
        entities = recognizer.extract_entities('Price of wheat and rice')
        assert 'crops' in entities
        assert 'wheat' in entities['crops']
        assert 'rice' in entities['crops']


class TestFallbackHandler:
    def test_fallback_chain_order(self):
        handler = FallbackHandler()
        chain = handler.get_fallback_chain()
        assert chain[0] == FallbackMode.VOICE
        assert chain[1] == FallbackMode.TEXT
        assert chain[2] == FallbackMode.CACHED

    def test_cache_response(self):
        handler = FallbackHandler()
        handler.cache_response('weather', 'Sunny')
        cached = handler.get_cached_response('weather')
        assert cached == 'Sunny'


class TestLanguageConfiguration:
    def test_configure_language(self):
        pipeline = VoiceProcessingPipeline()
        config = pipeline.configure_language('session123', SupportedLanguage.HINDI)
        assert config.source_language == SupportedLanguage.HINDI
        assert config.target_language == SupportedLanguage.ENGLISH

    def test_switch_language(self):
        pipeline = VoiceProcessingPipeline()
        pipeline.configure_language('session123', SupportedLanguage.HINDI)
        config2 = pipeline.switch_language('session123', SupportedLanguage.TAMIL)
        assert config2.source_language == SupportedLanguage.TAMIL


class TestVoiceModels:
    def test_supported_languages(self):
        assert SupportedLanguage.HINDI == "hi"
        assert SupportedLanguage.TAMIL == "ta"
        assert SupportedLanguage.ENGLISH == "en"

    def test_audio_formats(self):
        assert AudioFormat.WAV == "wav"
        assert AudioFormat.MP3 == "mp3"

    def test_fallback_modes(self):
        assert FallbackMode.VOICE == "voice"
        assert FallbackMode.TEXT == "text"
        assert FallbackMode.CACHED == "cached"
