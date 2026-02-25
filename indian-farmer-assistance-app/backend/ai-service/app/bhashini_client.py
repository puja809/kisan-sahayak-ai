"""Bhashini API client for ASR, NMT, TTS, and OCR services."""
import asyncio
import base64
import json
import time
from abc import ABC, abstractmethod
from typing import Any, AsyncGenerator, Dict, List, Optional

import aiohttp
from aiohttp import WSMsgType

from app.config import settings
from app.logging_config import logger
from app.voice_models import (
    ASRResponse,
    AudioFormat,
    AudioQuality,
    OCRResponse,
    SupportedLanguage,
    TTSRequest,
    TTSResponse,
    TranslationRequest,
    TranslationResponse,
)


class BhashiniAPIError(Exception):
    """Exception raised for Bhashini API errors."""
    
    def __init__(self, message: str, error_code: str = "BHASHINI_ERROR", details: Optional[Dict] = None):
        self.message = message
        self.error_code = error_code
        self.details = details or {}
        super().__init__(self.message)


class BaseBhashiniClient(ABC):
    """Base class for Bhashini API clients."""
    
    def __init__(self, api_key: Optional[str] = None, base_url: Optional[str] = None):
        self.api_key = api_key or settings.bhashini_api_key
        self.base_url = base_url or settings.bhashini_api_url
        self.timeout = aiohttp.ClientTimeout(total=30, connect=10)
    
    def _get_headers(self) -> Dict[str, str]:
        """Get common headers for API requests."""
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        return headers
    
    async def _make_request(
        self,
        method: str,
        endpoint: str,
        data: Optional[Dict] = None,
        retry_count: int = 3
    ) -> Dict[str, Any]:
        """Make an API request with retry logic."""
        url = f"{self.base_url}{endpoint}"
        headers = self._get_headers()
        
        last_exception = None
        for attempt in range(retry_count):
            try:
                async with aiohttp.ClientSession(timeout=self.timeout) as session:
                    async with session.request(method, url, json=data, headers=headers) as response:
                        if response.status == 200:
                            return await response.json()
                        elif response.status == 429:  # Rate limited
                            wait_time = (attempt + 1) * 2
                            logger.warning(f"Rate limited, waiting {wait_time}s before retry")
                            await asyncio.sleep(wait_time)
                            continue
                        else:
                            error_text = await response.text()
                            logger.error(f"Bhashini API error: {response.status} - {error_text}")
                            raise BhashiniAPIError(
                                f"API request failed with status {response.status}",
                                error_code=f"API_ERROR_{response.status}",
                                details={"response": error_text}
                            )
            except asyncio.TimeoutError as e:
                last_exception = e
                logger.warning(f"Request timeout (attempt {attempt + 1}/{retry_count})")
                await asyncio.sleep(2 ** attempt)  # Exponential backoff
            except aiohttp.ClientError as e:
                last_exception = e
                logger.warning(f"Request failed (attempt {attempt + 1}/{retry_count}): {e}")
                await asyncio.sleep(2 ** attempt)
        
        raise BhashiniAPIError(
            f"Request failed after {retry_count} attempts",
            error_code="MAX_RETRIES_EXCEEDED",
            details={"last_error": str(last_exception)}
        )


class ASRClient(BaseBhashiniClient):
    """Client for Bhashini Automatic Speech Recognition API."""
    
    def __init__(self, api_key: Optional[str] = None, base_url: Optional[str] = None):
        super().__init__(api_key, base_url)
        self.base_url = base_url or settings.bhashini_asr_url
    
    async def transcribe(
        self,
        audio_data: str,
        language: SupportedLanguage,
        audio_format: AudioFormat = AudioFormat.WAV,
        enable_streaming: bool = False
    ) -> ASRResponse:
        """
        Transcribe audio to text using Bhashini ASR.
        
        Args:
            audio_data: Base64 encoded audio data
            language: Source language for transcription
            audio_format: Audio format (wav, mp3, etc.)
            enable_streaming: Whether to use streaming transcription
        
        Returns:
            ASRResponse with transcribed text and confidence
        """
        start_time = time.time()
        
        # Decode base64 audio for API
        audio_bytes = base64.b64decode(audio_data)
        
        # Prepare request payload
        payload = {
            "audio": {
                "content": audio_data,
                "format": audio_format.value,
                "encoding": "base64"
            },
            "language": {
                "sourceLanguage": language.value
            },
            "config": {
                "enableStreaming": enable_streaming,
                "audioDuration": len(audio_bytes) / 32000  # Approximate duration
            }
        }
        
        try:
            response = await self._make_request("POST", "/asr/v1/transcribe", payload)
            
            # Parse response
            result = response.get("result", {})
            transcription = result.get("transcription", "")
            confidence = result.get("confidence", 0.0)
            
            processing_time_ms = (time.time() - start_time) * 1000
            
            return ASRResponse(
                text=transcription,
                confidence=confidence,
                language=language,
                is_final=True,
                processing_time_ms=processing_time_ms
            )
        except BhashiniAPIError as e:
            logger.error(f"ASR transcription failed: {e}")
            # Return a fallback response with low confidence
            return ASRResponse(
                text="",
                confidence=0.0,
                language=language,
                is_final=False,
                processing_time_ms=(time.time() - start_time) * 1000
            )
    
    async def transcribe_streaming(
        self,
        audio_chunks: AsyncGenerator[bytes, None],
        language: SupportedLanguage
    ) -> AsyncGenerator[ASRResponse, None]:
        """
        Transcribe audio stream in real-time using WebSocket.
        
        Args:
            audio_chunks: Async generator of audio chunks
            language: Source language for transcription
        
        Yields:
            ASRResponse for each chunk
        """
        ws_url = f"{self.base_url.replace('http', 'ws')}/asr/v1/stream"
        
        async with aiohttp.ClientSession() as session:
            async with session.ws_connect(ws_url) as ws:
                # Send language configuration
                await ws.send_json({
                    "language": {"sourceLanguage": language.value},
                    "config": {"enableStreaming": True}
                })
                
                # Send audio chunks
                async for chunk in audio_chunks:
                    await ws.send_bytes(chunk)
                
                # Receive transcriptions
                async for msg in ws:
                    if msg.type == WSMsgType.TEXT:
                        data = json.loads(msg.data)
                        result = data.get("result", {})
                        
                        yield ASRResponse(
                            text=result.get("transcription", ""),
                            confidence=result.get("confidence", 0.0),
                            language=language,
                            is_final=result.get("isFinal", False),
                            processing_time_ms=0
                        )
                    elif msg.type == WSMsgType.ERROR:
                        logger.error(f"WebSocket error: {msg.data}")
                        break


class NMTClient(BaseBhashiniClient):
    """Client for Bhashini Neural Machine Translation API."""
    
    def __init__(self, api_key: Optional[str] = None, base_url: Optional[str] = None):
        super().__init__(api_key, base_url)
        self.base_url = base_url or settings.bhashini_nmt_url
    
    async def translate(
        self,
        text: str,
        source_language: SupportedLanguage,
        target_language: SupportedLanguage
    ) -> TranslationResponse:
        """
        Translate text from source language to target language.
        
        Args:
            text: Text to translate
            source_language: Source language code
            target_language: Target language code
        
        Returns:
            TranslationResponse with translated text
        """
        start_time = time.time()
        
        # Skip translation if source and target are the same
        if source_language == target_language:
            return TranslationResponse(
                original_text=text,
                translated_text=text,
                source_language=source_language,
                target_language=target_language,
                confidence=1.0,
                processing_time_ms=0
            )
        
        payload = {
            "input": [{
                "source": text
            }],
            "config": {
                "sourceLanguage": source_language.value,
                "targetLanguage": target_language.value,
                "domain": "agriculture"  # Use agriculture domain for better accuracy
            }
        }
        
        try:
            response = await self._make_request("POST", "/nmt/v1/translate", payload)
            
            # Parse response
            translations = response.get("output", {}).get("translations", [])
            if translations:
                translated_text = translations[0].get("target", "")
            else:
                translated_text = ""
            
            # Calculate confidence based on response
            confidence = response.get("output", {}).get("confidence", 0.9)
            
            processing_time_ms = (time.time() - start_time) * 1000
            
            return TranslationResponse(
                original_text=text,
                translated_text=translated_text,
                source_language=source_language,
                target_language=target_language,
                confidence=confidence,
                processing_time_ms=processing_time_ms
            )
        except BhashiniAPIError as e:
            logger.error(f"Translation failed: {e}")
            # Return original text as fallback
            return TranslationResponse(
                original_text=text,
                translated_text=text,
                source_language=source_language,
                target_language=target_language,
                confidence=0.0,
                processing_time_ms=(time.time() - start_time) * 1000
            )
    
    async def translate_batch(
        self,
        texts: List[str],
        source_language: SupportedLanguage,
        target_language: SupportedLanguage
    ) -> List[TranslationResponse]:
        """
        Translate multiple texts in batch.
        
        Args:
            texts: List of texts to translate
            source_language: Source language code
            target_language: Target language code
        
        Returns:
            List of TranslationResponse
        """
        if source_language == target_language:
            return [
                TranslationResponse(
                    original_text=text,
                    translated_text=text,
                    source_language=source_language,
                    target_language=target_language,
                    confidence=1.0,
                    processing_time_ms=0
                )
                for text in texts
            ]
        
        payload = {
            "input": [{"source": text} for text in texts],
            "config": {
                "sourceLanguage": source_language.value,
                "targetLanguage": target_language.value,
                "domain": "agriculture"
            }
        }
        
        try:
            response = await self._make_request("POST", "/nmt/v1/translate", payload)
            
            translations = response.get("output", {}).get("translations", [])
            
            return [
                TranslationResponse(
                    original_text=text,
                    translated_text=t.get("target", text),
                    source_language=source_language,
                    target_language=target_language,
                    confidence=t.get("confidence", 0.9),
                    processing_time_ms=0
                )
                for text, t in zip(texts, translations)
            ]
        except BhashiniAPIError as e:
            logger.error(f"Batch translation failed: {e}")
            # Return original texts as fallback
            return [
                TranslationResponse(
                    original_text=text,
                    translated_text=text,
                    source_language=source_language,
                    target_language=target_language,
                    confidence=0.0,
                    processing_time_ms=0
                )
                for text in texts
            ]


class TTSClient(BaseBhashiniClient):
    """Client for Bhashini Text-to-Speech API."""
    
    def __init__(self, api_key: Optional[str] = None, base_url: Optional[str] = None):
        super().__init__(api_key, base_url)
        self.base_url = base_url or settings.bhashini_tts_url
    
    async def synthesize(
        self,
        text: str,
        language: SupportedLanguage,
        voice: str = "female",
        speed: float = 1.0,
        quality: AudioQuality = AudioQuality.MEDIUM
    ) -> TTSResponse:
        """
        Synthesize speech from text using Bhashini TTS.
        
        Args:
            text: Text to convert to speech
            language: Target language for synthesis
            voice: Voice type (male, female)
            speed: Speech speed (0.5 to 2.0)
            quality: Audio quality level
        
        Returns:
            TTSResponse with base64 encoded audio
        """
        start_time = time.time()
        
        # Map quality to sample rate and bitrate
        quality_config = {
            AudioQuality.HIGH: {"sampleRate": 44100, "bitrate": 320000},
            AudioQuality.MEDIUM: {"sampleRate": 22050, "bitrate": 128000},
            AudioQuality.LOW: {"sampleRate": 16000, "bitrate": 64000}
        }
        
        q = quality_config.get(quality, quality_config[AudioQuality.MEDIUM])
        
        payload = {
            "input": [{
                "source": text
            }],
            "config": {
                "language": {
                    "sourceLanguage": language.value
                },
                "audio": {
                    "voice": voice,
                    "speed": speed,
                    "samplingRate": q["sampleRate"],
                    "bitrate": q["bitrate"],
                    "format": "mp3"
                }
            }
        }
        
        try:
            response = await self._make_request("POST", "/tts/v1/synthesize", payload)
            
            # Parse response
            audio_data = response.get("output", {}).get("audio", {}).get("content", "")
            
            # Calculate approximate duration
            audio_bytes = base64.b64decode(audio_data)
            duration_seconds = len(audio_bytes) / (q["sampleRate"] * 4)  # Approximate for 16-bit audio
            
            processing_time_ms = (time.time() - start_time) * 1000
            
            return TTSResponse(
                text=text,
                audio_data=audio_data,
                audio_format=AudioFormat.MP3,
                duration_seconds=duration_seconds,
                processing_time_ms=processing_time_ms
            )
        except BhashiniAPIError as e:
            logger.error(f"TTS synthesis failed: {e}")
            # Return empty audio as fallback
            return TTSResponse(
                text=text,
                audio_data="",
                audio_format=AudioFormat.MP3,
                duration_seconds=0.0,
                processing_time_ms=(time.time() - start_time) * 1000
            )
    
    async def synthesize_streaming(
        self,
        text: str,
        language: SupportedLanguage,
        voice: str = "female",
        speed: float = 1.0
    ) -> AsyncGenerator[bytes, None]:
        """
        Synthesize speech with streaming output.
        
        Args:
            text: Text to convert to speech
            language: Target language for synthesis
            voice: Voice type (male, female)
            speed: Speech speed (0.5 to 2.0)
        
        Yields:
            Audio chunks as bytes
        """
        ws_url = f"{self.base_url.replace('http', 'ws')}/tts/v1/stream"
        
        async with aiohttp.ClientSession() as session:
            async with session.ws_connect(ws_url) as ws:
                # Send TTS configuration
                await ws.send_json({
                    "language": {"sourceLanguage": language.value},
                    "audio": {"voice": voice, "speed": speed}
                })
                
                # Send text
                await ws.send_json({"input": [{"source": text}]})
                
                # Receive audio chunks
                async for msg in ws:
                    if msg.type == WSMsgType.BINARY:
                        yield msg.data
                    elif msg.type == WSMsgType.ERROR:
                        logger.error(f"WebSocket error: {msg.data}")
                        break


class OCRClient(BaseBhashiniClient):
    """Client for Bhashini OCR API."""
    
    def __init__(self, api_key: Optional[str] = None, base_url: Optional[str] = None):
        super().__init__(api_key, base_url)
        self.base_url = base_url or settings.bhashini_ocr_url
    
    async def extract_text(
        self,
        image_data: str,
        language: SupportedLanguage
    ) -> OCRResponse:
        """
        Extract text from image using Bhashini OCR.
        
        Args:
            image_data: Base64 encoded image data
            language: Language of text in image
        
        Returns:
            OCRResponse with extracted text
        """
        start_time = time.time()
        
        payload = {
            "image": {
                "content": image_data,
                "encoding": "base64"
            },
            "language": {
                "sourceLanguage": language.value
            },
            "config": {
                "detectOrientation": True,
                "textDetection": True,
                "wordLevelConfidence": True
            }
        }
        
        try:
            response = await self._make_request("POST", "/ocr/v1/extract", payload)
            
            # Parse response
            result = response.get("result", {})
            extracted_text = result.get("text", "")
            confidence = result.get("confidence", 0.0)
            
            processing_time_ms = (time.time() - start_time) * 1000
            
            return OCRResponse(
                extracted_text=extracted_text,
                confidence=confidence,
                language=language,
                processing_time_ms=processing_time_ms
            )
        except BhashiniAPIError as e:
            logger.error(f"OCR extraction failed: {e}")
            # Return empty result as fallback
            return OCRResponse(
                extracted_text="",
                confidence=0.0,
                language=language,
                processing_time_ms=(time.time() - start_time) * 1000
            )
    
    async def extract_text_batch(
        self,
        images: List[str],
        language: SupportedLanguage
    ) -> List[OCRResponse]:
        """
        Extract text from multiple images in batch.
        
        Args:
            images: List of base64 encoded images
            language: Language of text in images
        
        Returns:
            List of OCRResponse
        """
        payload = {
            "images": [
                {"content": img, "encoding": "base64"} for img in images
            ],
            "language": {
                "sourceLanguage": language.value
            }
        }
        
        try:
            response = await self._make_request("POST", "/ocr/v1/extract/batch", payload)
            
            results = response.get("results", [])
            
            return [
                OCRResponse(
                    extracted_text=r.get("text", ""),
                    confidence=r.get("confidence", 0.0),
                    language=language,
                    processing_time_ms=0
                )
                for r in results
            ]
        except BhashiniAPIError as e:
            logger.error(f"Batch OCR extraction failed: {e}")
            # Return empty results as fallback
            return [
                OCRResponse(
                    extracted_text="",
                    confidence=0.0,
                    language=language,
                    processing_time_ms=0
                )
                for _ in images
            ]


class BhashiniClient:
    """Unified Bhashini API client combining all services."""
    
    def __init__(self, api_key: Optional[str] = None):
        self.asr = ASRClient(api_key)
        self.nmt = NMTClient(api_key)
        self.tts = TTSClient(api_key)
        self.ocr = OCRClient(api_key)
    
    async def close(self):
        """Close any open connections."""
        pass  # aiohttp handles connection cleanup


# Singleton instance
_bhashini_client: Optional[BhashiniClient] = None


def get_bhashini_client() -> BhashiniClient:
    """Get or create the Bhashini client singleton."""
    global _bhashini_client
    if _bhashini_client is None:
        _bhashini_client = BhashiniClient()
    return _bhashini_client