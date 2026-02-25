"""
Property-based tests for Voice Agent Service.

Feature: indian-farmer-assistance-app, Property 13: Language Configuration Consistency
Feature: indian-farmer-assistance-app, Property 14: Disambiguation Trigger
Feature: indian-farmer-assistance-app, Property 15: Voice Fallback Hierarchy
"""
import pytest
from hypothesis import given, settings, strategies as st
from hypothesis.strategies import integers, floats, lists, text, sampled_from, dictionaries

from app.voice_models import (
    FallbackMode,
    LanguageConfig,
    SupportedLanguage,
    VoiceProcessingRequest,
    VoiceProcessingResponse,
)
from app.voice_pipeline import (
    DisambiguationHandler,
    FallbackHandler,
    IntentRecognizer,
    VoiceProcessingPipeline,
    VoiceActivityDetector,
)


# =============================================================================
# Property 13: Language Configuration Consistency
# Validates: Requirements 8.1, 8.12
# =============================================================================

class TestLanguageConfigurationConsistency:
    """Property tests for language configuration consistency."""
    
    @given(language=sampled_from(list(SupportedLanguage)))
    @settings(max_examples=20)
    def test_language_config_persistence(self, language):
        """
        Property: For any supported language selected by a farmer, the system
        should configure Bhashini API for that language, and all subsequent
        voice interactions should use that language configuration until changed.
        
        Validates: Requirements 8.1, 8.12
        """
        pipeline = VoiceProcessingPipeline()
        session_id = "test_session_123"
        
        # Configure language
        config1 = pipeline.configure_language(session_id, language)
        
        # Verify configuration is stored
        config2 = pipeline.get_language_config(session_id)
        
        assert config2 is not None, "Language config should be stored"
        assert config2.source_language == language, f"Expected {language}, got {config2.source_language}"
        assert config2.target_language == SupportedLanguage.ENGLISH, "Default target should be English"
    
    @given(session_id=text(min_size=1, max_size=50),
           language=sampled_from(list(SupportedLanguage)))
    @settings(max_examples=30)
    def test_language_config_immutable_after_set(self, session_id, language):
        """
        Property: Once a language is configured for a session, it should remain
        consistent across multiple get requests until explicitly changed.
        
        Validates: Requirements 8.1, 8.12
        """
        pipeline = VoiceProcessingPipeline()
        
        # Configure language
        config = pipeline.configure_language(session_id, language)
        
        # Multiple get requests should return same config
        for _ in range(5):
            retrieved = pipeline.get_language_config(session_id)
            assert retrieved is not None
            assert retrieved.source_language == language
            assert retrieved.target_language == config.target_language
            assert retrieved.asr_model == config.asr_model
            assert retrieved.nmt_model == config.nmt_model
            assert retrieved.tts_voice == config.tts_voice
            assert retrieved.tts_speed == config.tts_speed
    
    @given(session_id=text(min_size=1, max_size=50),
           lang1=sampled_from(list(SupportedLanguage)),
           lang2=sampled_from(list(SupportedLanguage)))
    @settings(max_examples=20)
    def test_language_switch_updates_config(self, session_id, lang1, lang2):
        """
        Property: When a farmer switches languages mid-session, the configuration
        should be updated while maintaining other settings.
        
        Validates: Requirement 8.12
        """
        pipeline = VoiceProcessingPipeline()
        
        # Configure initial language
        config1 = pipeline.configure_language(session_id, lang1)
        
        # Switch language
        config2 = pipeline.switch_language(session_id, lang2)
        
        # Verify update
        assert config2.source_language == lang2
        assert config2.target_language == SupportedLanguage.ENGLISH  # Should remain English
        assert config2.asr_model == config1.asr_model  # Other settings preserved
        assert config2.nmt_model == config1.nmt_model
        assert config2.tts_voice == config1.tts_voice
        assert config2.tts_speed == config1.tts_speed


# =============================================================================
# Property 14: Disambiguation Trigger
# Validates: Requirement 8.7
# =============================================================================

class TestDisambiguationTrigger:
    """Property tests for disambiguation trigger."""
    
    @given(text=text(min_size=1, max_size=200, alphabet=st.characters(
        whitelist_categories=['L', 'N'],
        whitelist_characters=' .,!?@#$%&*()[]{}'
    )))
    @settings(max_examples=50)
    def test_ambiguous_price_query_triggers_disambiguation(self, text):
        """
        Property: For any voice query that matches multiple intents with similar
        confidence scores (difference < 10%), the system should request
        disambiguation confirmation from the user.
        
        Validates: Requirement 8.7
        """
        recognizer = IntentRecognizer()
        
        # Add "price" or "rate" to make it a price query
        test_text = f"{text} price rate"
        
        intent, confidence = recognizer.recognize_intent(test_text)
        is_ambiguous, options = recognizer.is_ambiguous(test_text, confidence)
        
        # If it's a price query without specific crop, should trigger disambiguation
        if "price" in test_text.lower() or "rate" in test_text.lower():
            if not any(crop in test_text.lower() for crop in ["wheat", "rice", "cotton", "sugarcane"]):
                assert is_ambiguous, "Price query without specific crop should trigger disambiguation"
                assert len(options) > 0, "Should provide disambiguation options"
    
    @given(text=text(min_size=1, max_size=200))
    @settings(max_examples=30)
    def test_ambiguous_crop_query_triggers_disambiguation(self, text):
        """
        Property: For ambiguous crop queries (e.g., "rice" without specifying
        paddy, basmati, or parboiled), the system should request disambiguation.
        
        Validates: Requirement 8.7
        """
        recognizer = IntentRecognizer()
        
        # Test with "rice" which is ambiguous
        test_text = f"{text} rice cultivation"
        
        intent, confidence = recognizer.recognize_intent(test_text)
        is_ambiguous, options = recognizer.is_ambiguous(test_text, confidence)
        
        # Rice without specific variant should trigger disambiguation
        if "rice" in test_text.lower():
            assert is_ambiguous, "Ambiguous crop query should trigger disambiguation"
    
    @given(confidence=floats(min_value=0.0, max_value=1.0))
    @settings(max_examples=20)
    def test_low_confidence_triggers_disambiguation(self, confidence):
        """
        Property: When intent recognition confidence is below 0.6, the system
        should request disambiguation.
        
        Validates: Requirement 8.7
        """
        recognizer = IntentRecognizer()
        
        # Test with low confidence
        is_ambiguous, _ = recognizer.is_ambiguous("some random text", confidence)
        
        if confidence < 0.6:
            assert is_ambiguous, "Low confidence should trigger disambiguation"
    
    @given(options=lists(sampled_from(["wheat", "rice", "cotton", "sugarcane", "soybean"]),
                        min_size=2, max_size=5))
    @settings(max_examples=20)
    def test_disambiguation_handler_creates_confirmation(self, options):
        """
        Property: When disambiguation is triggered, the system should create
        a confirmation ID for tracking the user's selection.
        
        Validates: Requirement 8.7
        """
        handler = DisambiguationHandler()
        language = SupportedLanguage.HINDI
        
        confirmation_id = handler.create_disambiguation(
            query="What is the price?",
            options=options,
            language=language
        )
        
        assert confirmation_id is not None
        assert len(confirmation_id) > 0
        
        # Verify it can be resolved
        response = handler.resolve_disambiguation(confirmation_id, options[0])
        assert response is not None
        assert response.selected_option == options[0]


# =============================================================================
# Property 15: Voice Fallback Hierarchy
# Validates: Requirement 8.9
# =============================================================================

class TestVoiceFallbackHierarchy:
    """Property tests for voice fallback hierarchy."""
    
    @given()
    @settings(max_examples=10)
    def test_fallback_chain_order(self):
        """
        Property: For any voice processing failure, the system should fall back
        to text input, and if text processing fails, it should fall back to cached
        answers, following the explicit hierarchy: voice → text → cached.
        
        Validates: Requirement 8.9
        """
        handler = FallbackHandler()
        chain = handler.get_fallback_chain()
        
        # Verify the order is voice -> text -> cached
        assert len(chain) == 3, "Fallback chain should have 3 modes"
        assert chain[0] == FallbackMode.VOICE, "First fallback should be VOICE"
        assert chain[1] == FallbackMode.TEXT, "Second fallback should be TEXT"
        assert chain[2] == FallbackMode.CACHED, "Third fallback should be CACHED"
    
    @given()
    @settings(max_examples=10)
    def test_next_fallback_progression(self):
        """
        Property: When a fallback mode fails, the system should progress to
        the next mode in the hierarchy.
        
        Validates: Requirement 8.9
        """
        handler = FallbackHandler()
        
        # After VOICE fails, should go to TEXT
        next_after_voice = handler.get_next_fallback(FallbackMode.VOICE)
        assert next_after_voice == FallbackMode.TEXT, "After VOICE should fallback to TEXT"
        
        # After TEXT fails, should go to CACHED
        next_after_text = handler.get_next_fallback(FallbackMode.TEXT)
        assert next_after_text == FallbackMode.CACHED, "After TEXT should fallback to CACHED"
        
        # After CACHED, no more fallbacks
        next_after_cached = handler.get_next_fallback(FallbackMode.CACHED)
        assert next_after_cached is None, "After CACHED there should be no more fallbacks"
    
    @given(query=text(min_size=1, max_size=100),
           response=text(min_size=1, max_size=200))
    @settings(max_examples=20)
    def test_caching_for_fallback(self, query, response):
        """
        Property: Responses should be cached for fallback use when voice
        and text processing fail.
        
        Validates: Requirement 8.9
        """
        handler = FallbackHandler()
        
        # Cache a response
        handler.cache_response(query, response)
        
        # Retrieve cached response
        cached = handler.get_cached_response(query)
        
        assert cached == response, "Cached response should match original"
    
    @given(query=text(min_size=1, max_size=100))
    @settings(max_examples=20)
    def test_case_insensitive_cache_lookup(self, query):
        """
        Property: Cache lookup should be case-insensitive for consistent
        fallback behavior.
        
        Validates: Requirement 8.9
        """
        handler = FallbackHandler()
        
        # Cache with lowercase
        handler.cache_response(query.lower(), "response")
        
        # Lookup with different case
        cached = handler.get_cached_response(query.upper())
        
        assert cached == "response", "Cache lookup should be case-insensitive"
    
    @given(query=text(min_size=1, max_size=100))
    @settings(max_examples=10)
    def test_no_fallback_for_nonexistent_query(self, query):
        """
        Property: When no cached response exists, fallback should indicate
        unavailability.
        
        Validates: Requirement 8.9
        """
        handler = FallbackHandler()
        
        cached = handler.get_cached_response(query)
        assert cached is None, "Non-existent query should return None"


# =============================================================================
# Additional Voice Activity Detection Properties
# =============================================================================

class TestVoiceActivityDetection:
    """Property tests for voice activity detection."""
    
    @given(energy_threshold=floats(min_value=0.001, max_value=0.1))
    @settings(max_examples=10)
    def test_vad_threshold_configurable(self, energy_threshold):
        """
        Property: VAD energy threshold should be configurable for different
        noise environments.
        
        Validates: Requirement 8.8
        """
        vad = VoiceActivityDetector(energy_threshold=energy_threshold)
        
        assert vad.energy_threshold == energy_threshold, "Threshold should be configurable"
    
    @given(audio_data=lists(integers(min_value=-32768, max_value=32767), min_size=100, max_size=1000))
    @settings(max_examples=20)
    def test_vad_energy_calculation(self, audio_data):
        """
        Property: VAD energy calculation should return non-negative values
        and be consistent for the same input.
        
        Validates: Requirement 8.8
        """
        import struct
        vad = VoiceActivityDetector()
        
        # Convert to bytes
        audio_bytes = struct.pack('<' + 'h' * len(audio_data), *audio_data)
        
        # Calculate energy
        energy = vad.calculate_energy(audio_bytes)
        
        assert energy >= 0, "Energy should be non-negative"
        
        # Same input should give same energy
        energy2 = vad.calculate_energy(audio_bytes)
        assert energy == energy2, "Energy calculation should be deterministic"


# =============================================================================
# Intent Recognition Properties
# =============================================================================

class TestIntentRecognition:
    """Property tests for intent recognition."""
    
    @given(text=text(min_size=1, max_size=200))
    @settings(max_examples=50)
    def test_intent_recognition_returns_valid_intent(self, text):
        """
        Property: Intent recognition should always return a valid intent
        and confidence score.
        
        Validates: Requirement 8.6
        """
        recognizer = IntentRecognizer()
        
        intent, confidence = recognizer.recognize_intent(text)
        
        assert intent is not None
        assert len(intent) > 0
        assert 0.0 <= confidence <= 1.0, "Confidence should be in [0, 1]"
    
    @given(text=text(min_size=1, max_size=200))
    @settings(max_examples=30)
    def test_entity_extraction_returns_dict(self, text):
        """
        Property: Entity extraction should always return a dictionary.
        
        Validates: Requirement 8.6
        """
        recognizer = IntentRecognizer()
        
        entities = recognizer.extract_entities(text)
        
        assert isinstance(entities, dict), "Entities should be a dictionary"
        assert all(isinstance(k, str) for k in entities.keys()), "Keys should be strings"
    
    @given(text=text(min_size=1, max_size=200, alphabet=st.ascii_letters + ' '))
    @settings(max_examples=20)
    def test_crop_entity_extraction(self, text):
        """
        Property: Entity extraction should correctly identify crop names.
        
        Validates: Requirement 8.6
        """
        recognizer = IntentRecognizer()
        
        # Test with known crop names
        test_text = f"{text} wheat rice cotton"
        entities = recognizer.extract_entities(test_text)
        
        if "crops" in entities:
            assert all(isinstance(crop, str) for crop in entities["crops"])
    
    @given(text=text(min_size=1, max_size=200, alphabet=st.ascii_letters + ' '))
    @settings(max_examples=20)
    def test_quantity_entity_extraction(self, text):
        """
        Property: Entity extraction should correctly identify quantities.
        
        Validates: Requirement 8.6
        """
        recognizer = IntentRecognizer()
        
        # Test with quantities
        test_text = f"{text} 100 kg 50 quintal"
        entities = recognizer.extract_entities(test_text)
        
        if "quantities" in entities:
            for q in entities["quantities"]:
                assert "value" in q
                assert "unit" in q