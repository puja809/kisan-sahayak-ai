"""Voice processing pipeline for Bhashini voice agent."""
import base64
import json
import re
import time
import uuid
from typing import Any, Dict, List, Optional, Tuple

from app.bhashini_client import BhashiniClient, BhashiniAPIError
from app.config import settings
from app.logging_config import logger
from app.voice_models import (
    AudioFormat,
    AudioQuality,
    ConversationTurn,
    DisambiguationRequest,
    DisambiguationResponse,
    FallbackMode,
    FallbackResponse,
    LanguageConfig,
    OCRResponse,
    SupportedLanguage,
    TranslationResponse,
    VoiceActivityDetectionResult,
    VoiceConversation,
    VoiceProcessingRequest,
    VoiceProcessingResponse,
)


class VoiceActivityDetector:
    """Voice Activity Detection for audio stream processing."""
    
    # Energy threshold for speech detection (adjustable)
    ENERGY_THRESHOLD = 0.02
    # Minimum speech duration in milliseconds
    MIN_SPEECH_DURATION_MS = 300
    # Maximum silence duration in milliseconds before ending utterance
    MAX_SILENCE_DURATION_MS = 1000
    
    def __init__(self, energy_threshold: float = None):
        self.energy_threshold = energy_threshold or self.ENERGY_THRESHOLD
    
    def calculate_energy(self, audio_chunk: bytes) -> float:
        """Calculate energy of an audio chunk."""
        import struct
        # Convert bytes to samples (16-bit PCM)
        try:
            samples = struct.unpack('<' + 'h' * (len(audio_chunk) // 2), audio_chunk)
            # Calculate RMS energy
            energy = sum(s * s for s in samples) / len(samples)
            return energy
        except struct.error:
            return 0.0
    
    def detect(self, audio_chunk: bytes) -> VoiceActivityDetectionResult:
        """
        Detect if audio chunk contains speech.
        
        Args:
            audio_chunk: Raw audio bytes
        
        Returns:
            VoiceActivityDetectionResult with detection result
        """
        energy = self.calculate_energy(audio_chunk)
        is_speech = energy > self.energy_threshold
        
        return VoiceActivityDetectionResult(
            is_speech=is_speech,
            confidence=min(energy / self.energy_threshold, 1.0) if is_speech else 0.0
        )


class IntentRecognizer:
    """Simple intent recognition for agricultural queries."""
    
    # Intent patterns for agricultural queries
    INTENT_PATTERNS = {
        "weather_query": [
            r"weather",
            r"forecast",
            r"rain",
            r"temperature",
            r"monsoon"
        ],
        "price_query": [
            r"price",
            r"mandi",
            r"rate",
            r"cost",
            r"market"
        ],
        "scheme_query": [
            r"scheme",
            r"subsidy",
            r"government",
            r"loan",
            r"insurance"
        ],
        "crop_query": [
            r"crop",
            r"cultivation",
            r"plant",
            r"grow",
            r"sow"
        ],
        "disease_query": [
            r"disease",
            r"pest",
            r"infection",
            r"treatment",
            r"cure"
        ],
        "fertilizer_query": [
            r"fertilizer",
            r"nutrient",
            r"urea",
            r"dap",
            r"manure"
        ]
    }
    
    def __init__(self):
        self.intent_patterns = self.INTENT_PATTERNS
    
    def extract_entities(self, text: str) -> Dict[str, Any]:
        """
        Extract named entities from text.
        
        Args:
            text: Input text
        
        Returns:
            Dictionary of extracted entities
        """
        entities = {}
        
        # Extract crop names
        crop_names = ["wheat", "rice", "paddy", "cotton", "sugarcane", "corn", "maize",
                      "soybean", "groundnut", "mustard", "potato", "tomato", "onion"]
        found_crops = [crop for crop in crop_names if crop.lower() in text.lower()]
        if found_crops:
            entities["crops"] = found_crops
        
        # Extract locations
        indian_states = ["maharashtra", "punjab", "haryana", "uttar pradesh", "karnataka",
                         "andhra pradesh", "telangana", "tamil nadu", "west bengal", "gujarat"]
        found_states = [state for state in indian_states if state.lower() in text.lower()]
        if found_states:
            entities["state"] = found_states[0]
        
        # Extract quantities
        quantity_pattern = r'(\d+)\s*(kg|quintal|ton|acre|hectare|liter|ml)'
        matches = re.findall(quantity_pattern, text.lower())
        if matches:
            entities["quantities"] = [{"value": int(m[0]), "unit": m[1]} for m in matches]
        
        # Extract time expressions
        time_patterns = [
            (r"today", "today"),
            (r"tomorrow", "tomorrow"),
            (r"this week", "this_week"),
            (r"next month", "next_month"),
            (r"kharif", "kharif"),
            (r"rabi", "rabi")
        ]
        for pattern, time_expr in time_patterns:
            if re.search(pattern, text.lower()):
                entities["time"] = time_expr
                break
        
        return entities
    
    def recognize_intent(self, text: str) -> Tuple[str, float]:
        """
        Recognize intent from text.
        
        Args:
            text: Input text
        
        Returns:
            Tuple of (intent, confidence)
        """
        text_lower = text.lower()
        best_intent = "general_query"
        best_confidence = 0.0
        
        for intent, patterns in self.intent_patterns.items():
            for pattern in patterns:
                if re.search(pattern, text_lower):
                    # Calculate confidence based on pattern match
                    match_count = len(re.findall(pattern, text_lower))
                    confidence = min(0.5 + (match_count * 0.1), 0.95)
                    
                    if confidence > best_confidence:
                        best_intent = intent
                        best_confidence = confidence
        
        return best_intent, best_confidence
    
    def is_ambiguous(self, text: str, confidence: float) -> Tuple[bool, List[str]]:
        """
        Check if query is ambiguous and needs disambiguation.
        
        Args:
            text: Input text
            confidence: Intent recognition confidence
        
        Returns:
            Tuple of (is_ambiguous, options)
        """
        text_lower = text.lower()
        options = []
        
        # Check for ambiguous crop names
        ambiguous_crops = {
            "rice": ["paddy", "basmati", "parboiled rice"],
            "millet": ["bajra", "jowar", "ragi"],
            "oilseed": ["mustard", "sunflower", "groundnut"]
        }
        
        for crop, variants in ambiguous_crops.items():
            if crop in text_lower:
                # Check if specific variant is mentioned
                mentioned_variants = [v for v in variants if v in text_lower]
                if not mentioned_variants:
                    options = variants
                    return True, options
        
        # Check for ambiguous price queries
        if "price" in text_lower or "rate" in text_lower:
            if not any(word in text_lower for word in ["wheat", "rice", "cotton", "sugarcane"]):
                options = ["wheat", "rice", "cotton", "sugarcane", "soybean"]
                return True, options
        
        # Check confidence threshold
        if confidence < 0.6:
            return True, []
        
        return False, []


class DisambiguationHandler:
    """Handle disambiguation for ambiguous queries."""
    
    def __init__(self):
        self.pending_disambiguations: Dict[str, DisambiguationRequest] = {}
    
    def create_disambiguation(
        self,
        query: str,
        options: List[str],
        language: SupportedLanguage
    ) -> str:
        """
        Create a disambiguation request and return a confirmation ID.
        
        Args:
            query: Original query
            options: Disambiguation options
            language: User's language
        
        Returns:
            Confirmation ID for tracking
        """
        confirmation_id = str(uuid.uuid4())
        self.pending_disambiguations[confirmation_id] = DisambiguationRequest(
            query=query,
            options=options,
            language=language
        )
        return confirmation_id
    
    def resolve_disambiguation(
        self,
        confirmation_id: str,
        selected_option: str
    ) -> Optional[DisambiguationResponse]:
        """
        Resolve a disambiguation request.
        
        Args:
            confirmation_id: Confirmation ID from create_disambiguation
            selected_option: User's selected option
        
        Returns:
            DisambiguationResponse or None if not found
        """
        request = self.pending_disambiguations.get(confirmation_id)
        if not request:
            return None
        
        # Remove from pending
        del self.pending_disambiguations[confirmation_id]
        
        # Calculate confidence based on selection
        if selected_option in request.options:
            confidence = 1.0 / len(request.options)
        else:
            confidence = 0.5
        
        return DisambiguationResponse(
            selected_option=selected_option,
            confidence=confidence
        )
    
    def generate_disambiguation_message(
        self,
        query: str,
        options: List[str],
        language: SupportedLanguage
    ) -> str:
        """
        Generate a disambiguation message in the user's language.
        
        Args:
            query: Original query
            options: Disambiguation options
            language: User's language
        
        Returns:
            Disambiguation message
        """
        # Simple message templates
        messages = {
            SupportedLanguage.HINDI: f"आपका प्रश्न अस्पष्ट है। क्या आपका मतलब है: {', '.join(options)}?",
            SupportedLanguage.ENGLISH: f"Your query is ambiguous. Did you mean: {', '.join(options)}?",
            SupportedLanguage.TAMIL: f"உங்கள் கேள்வி தெளிவற்றது. நீங்கள் குறிப்பிட்டது: {', '.join(options)}?",
            SupportedLanguage.TELUGU: f"మీ ప్రశ్న అస్పష్టం. మీరు అర్థం చేసుకున్నది: {', '.join(options)}?",
        }
        
        return messages.get(language, messages[SupportedLanguage.ENGLISH])


class FallbackHandler:
    """Handle fallback from voice to text to cached answers."""
    
    def __init__(self):
        self.fallback_chain = [FallbackMode.VOICE, FallbackMode.TEXT, FallbackMode.CACHED]
        self.cache: Dict[str, str] = {}  # Simple cache for responses
    
    def get_next_fallback(self, failed_mode: FallbackMode) -> Optional[FallbackMode]:
        """
        Get the next fallback mode after a failure.
        
        Args:
            failed_mode: The mode that failed
        
        Returns:
            Next fallback mode or None if no more fallbacks
        """
        try:
            failed_index = self.fallback_chain.index(failed_mode)
            if failed_index < len(self.fallback_chain) - 1:
                return self.fallback_chain[failed_index + 1]
        except ValueError:
            pass
        return None
    
    def get_fallback_chain(self) -> List[FallbackMode]:
        """Get the full fallback chain."""
        return self.fallback_chain.copy()
    
    def cache_response(self, query: str, response: str) -> None:
        """
        Cache a response for future fallback use.
        
        Args:
            query: Query text
            response: Response text
        """
        # Simple cache with query as key
        self.cache[query.lower().strip()] = response
    
    def get_cached_response(self, query: str) -> Optional[str]:
        """
        Get a cached response for a query.
        
        Args:
            query: Query text
        
        Returns:
            Cached response or None
        """
        return self.cache.get(query.lower().strip())


class VoiceProcessingPipeline:
    """
    Complete voice processing pipeline for Bhashini voice agent.
    
    Pipeline steps:
    1. Audio ingestion with VAD
    2. ASR for speech-to-text conversion
    3. NMT for translation to English
    4. Conversational intelligence (intent recognition, entity extraction)
    5. NMT for translation back to farmer's language
    6. TTS for audio synthesis with streaming
    """
    
    def __init__(self, bhashini_client: Optional[BhashiniClient] = None):
        self.bhashini = bhashini_client or BhashiniClient()
        self.vad = VoiceActivityDetector()
        self.intent_recognizer = IntentRecognizer()
        self.disambiguation_handler = DisambiguationHandler()
        self.fallback_handler = FallbackHandler()
        self.language_configs: Dict[str, LanguageConfig] = {}
    
    def configure_language(
        self,
        session_id: str,
        language: SupportedLanguage
    ) -> LanguageConfig:
        """
        Configure Bhashini API for a specific language.
        
        Args:
            session_id: User session ID
            language: Selected language
        
        Returns:
            LanguageConfig for the session
        """
        config = LanguageConfig(
            source_language=language,
            target_language=SupportedLanguage.ENGLISH
        )
        self.language_configs[session_id] = config
        return config
    
    def get_language_config(self, session_id: str) -> Optional[LanguageConfig]:
        """Get language configuration for a session."""
        return self.language_configs.get(session_id)
    
    async def process_voice(
        self,
        request: VoiceProcessingRequest
    ) -> VoiceProcessingResponse:
        """
        Process a voice query through the full pipeline.
        
        Args:
            request: VoiceProcessingRequest with audio data
        
        Returns:
            VoiceProcessingResponse with text and audio response
        """
        start_time = time.time()
        session_id = request.session_id or str(uuid.uuid4())
        
        # Configure language if not already configured
        if session_id not in self.language_configs:
            self.configure_language(session_id, request.source_language)
        
        lang_config = self.language_configs[session_id]
        
        try:
            # Step 1: Voice Activity Detection
            audio_bytes = base64.b64decode(request.audio_data)
            vad_result = self.vad.detect(audio_bytes)
            
            if not vad_result.is_speech:
                return VoiceProcessingResponse(
                    session_id=session_id,
                    user_text="",
                    user_text_english="",
                    system_response="Could not detect speech. Please speak clearly.",
                    confidence=0.0,
                    disambiguation_required=False,
                    fallback_used=False,
                    fallback_reason="No speech detected",
                    processing_time_ms=(time.time() - start_time) * 1000
                )
            
            # Step 2: ASR - Speech to Text
            asr_response = await self.bhashini.asr.transcribe(
                audio_data=request.audio_data,
                language=request.source_language,
                audio_format=request.audio_format
            )
            
            if not asr_response.text:
                # Fallback to text input
                return await self._fallback_to_text(session_id, request, "ASR failed")
            
            user_text = asr_response.text
            
            # Step 3: NMT - Translate to English
            translation_response = await self.bhashini.nmt.translate(
                text=user_text,
                source_language=request.source_language,
                target_language=SupportedLanguage.ENGLISH
            )
            
            user_text_english = translation_response.translated_text
            
            # Step 4: Intent Recognition and Entity Extraction
            intent, intent_confidence = self.intent_recognizer.recognize_intent(user_text_english)
            entities = self.intent_recognizer.extract_entities(user_text_english)
            
            # Step 5: Check for disambiguation
            is_ambiguous, disambiguation_options = self.intent_recognizer.is_ambiguous(
                user_text_english, intent_confidence
            )
            
            if is_ambiguous and disambiguation_options:
                disambiguation_message = self.disambiguation_handler.generate_disambiguation_message(
                    user_text_english, disambiguation_options, request.source_language
                )
                
                return VoiceProcessingResponse(
                    session_id=session_id,
                    user_text=user_text,
                    user_text_english=user_text_english,
                    system_response=disambiguation_message,
                    intent=intent,
                    entities=entities,
                    confidence=intent_confidence,
                    disambiguation_required=True,
                    disambiguation_options=disambiguation_options,
                    fallback_used=False,
                    processing_time_ms=(time.time() - start_time) * 1000
                )
            
            # Step 6: Generate system response (placeholder - would invoke external APIs)
            system_response = await self._generate_response(intent, entities, user_text_english)
            system_response_english = system_response
            
            # Step 7: NMT - Translate back to user's language
            if request.source_language != SupportedLanguage.ENGLISH:
                translation_back = await self.bhashini.nmt.translate(
                    text=system_response,
                    source_language=SupportedLanguage.ENGLISH,
                    target_language=request.source_language
                )
                system_response = translation_back.translated_text
            
            # Step 8: TTS - Generate audio response
            tts_response = await self.bhashini.tts.synthesize(
                text=system_response,
                language=request.source_language,
                voice=lang_config.tts_voice,
                speed=lang_config.tts_speed,
                quality=request.audio_format  # Use requested quality
            )
            
            processing_time_ms = (time.time() - start_time) * 1000
            
            return VoiceProcessingResponse(
                session_id=session_id,
                user_text=user_text,
                user_text_english=user_text_english,
                system_response=system_response,
                system_response_audio=tts_response.audio_data if tts_response.audio_data else None,
                intent=intent,
                entities=entities,
                confidence=intent_confidence,
                disambiguation_required=False,
                fallback_used=False,
                processing_time_ms=processing_time_ms
            )
            
        except BhashiniAPIError as e:
            logger.error(f"Bhashini API error in voice pipeline: {e}")
            return await self._fallback_to_text(session_id, request, str(e))
        
        except Exception as e:
            logger.error(f"Error in voice pipeline: {e}", exc_info=True)
            return await self._fallback_to_text(session_id, request, str(e))
    
    async def _generate_response(
        self,
        intent: str,
        entities: Dict[str, Any],
        query_english: str
    ) -> str:
        """
        Generate a system response based on intent and entities.
        
        Args:
            intent: Recognized intent
            entities: Extracted entities
            query_english: Query in English
        
        Returns:
            System response text
        """
        # Placeholder response generation
        # In production, this would invoke external APIs (Agmarknet, IMD, etc.)
        
        response_templates = {
            "weather_query": "The weather forecast for your area shows clear skies with temperatures between 25-35°C. No rainfall is expected in the next 7 days.",
            "price_query": f"Current market prices for {entities.get('crops', ['crops'])[0] if entities.get('crops') else 'agricultural commodities'} are available. Please check the mandi prices section for details.",
            "scheme_query": "There are several government schemes available for farmers. You can view them in the schemes section of the app.",
            "crop_query": f"For {entities.get('crops', ['crops'])[0] if entities.get('crops') else 'agricultural'} cultivation, recommended practices include proper sowing time, adequate irrigation, and timely fertilizer application.",
            "disease_query": "For crop disease diagnosis, please upload a clear image of the affected plant part in the disease detection section.",
            "fertilizer_query": "Fertilizer recommendations depend on soil test results and crop requirements. Please check the fertilizer recommendation section.",
            "general_query": "I can help you with weather forecasts, mandi prices, government schemes, crop recommendations, and disease detection. What would you like to know about?"
        }
        
        return response_templates.get(intent, response_templates["general_query"])
    
    async def _fallback_to_text(
        self,
        session_id: str,
        request: VoiceProcessingRequest,
        error_message: str
    ) -> VoiceProcessingResponse:
        """
        Fallback to text-based interaction when voice processing fails.
        
        Args:
            session_id: User session ID
            request: Original request
            error_message: Error message
        
        Returns:
            VoiceProcessingResponse with text fallback
        """
        if not request.enable_fallback:
            return VoiceProcessingResponse(
                session_id=session_id,
                user_text="",
                user_text_english="",
                system_response="Voice processing failed. Please try again or use text input.",
                confidence=0.0,
                disambiguation_required=False,
                fallback_used=True,
                fallback_reason=error_message,
                processing_time_ms=0
            )
        
        # Try cached response
        cached_response = self.fallback_handler.get_cached_response("")
        if cached_response:
            return VoiceProcessingResponse(
                session_id=session_id,
                user_text="",
                user_text_english="",
                system_response=cached_response,
                confidence=0.0,
                disambiguation_required=False,
                fallback_used=True,
                fallback_reason=f"Voice failed: {error_message}",
                processing_time_ms=0
            )
        
        # Return text fallback message
        return VoiceProcessingResponse(
            session_id=session_id,
            user_text="",
            user_text_english="",
            system_response="Voice recognition failed. Please repeat your query or type it instead.",
            confidence=0.0,
            disambiguation_required=False,
            fallback_used=True,
            fallback_reason=f"Voice failed: {error_message}",
            processing_time_ms=0
        )
    
    async def switch_language(
        self,
        session_id: str,
        new_language: SupportedLanguage
    ) -> LanguageConfig:
        """
        Switch language mid-session without losing conversation context.
        
        Args:
            session_id: User session ID
            new_language: New language to switch to
        
        Returns:
            Updated LanguageConfig
        """
        # Reconfigure Bhashini for new language
        config = self.configure_language(session_id, new_language)
        
        logger.info(f"Language switched for session {session_id} to {new_language.value}")
        
        return config
    
    def store_conversation(
        self,
        conversation: VoiceConversation
    ) -> None:
        """
        Store conversation in MongoDB Voice_Conversations collection.
        
        Args:
            conversation: VoiceConversation to store
        """
        # This would be implemented with actual MongoDB storage
        logger.info(f"Storing conversation {conversation.session_id} with {len(conversation.conversations)} turns")
    
    def get_conversation(self, session_id: str) -> Optional[VoiceConversation]:
        """
        Retrieve conversation from MongoDB.
        
        Args:
            session_id: Session ID to retrieve
        
        Returns:
            VoiceConversation or None
        """
        # This would be implemented with actual MongoDB retrieval
        return None


# Singleton instance
_voice_pipeline: Optional[VoiceProcessingPipeline] = None


def get_voice_pipeline() -> VoiceProcessingPipeline:
    """Get or create the voice processing pipeline singleton."""
    global _voice_pipeline
    if _voice_pipeline is None:
        _voice_pipeline = VoiceProcessingPipeline()
    return _voice_pipeline