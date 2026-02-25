"""
Voice Assistant for Krishi Sahayak RAG backend.

This module provides voice interaction capabilities using:
- OpenAI Whisper for speech-to-text
- pyttsx3 for text-to-speech
- sounddevice for audio recording
"""

import os
import wave
import numpy as np
import sounddevice as sd
import whisper
import pyttsx3
from pathlib import Path

from app.rag_pipeline import query_rag_pipeline


# Audio configuration
SAMPLE_RATE = 16000  # 16kHz for Whisper
DURATION = 5  # seconds
CHANNELS = 1  # mono
TEMP_AUDIO_FILE = "input.wav"

# Set default input device to 0
sd.default.device = (0, None)


def record_audio(duration: int = DURATION) -> np.ndarray:
    """
    Record audio from microphone.
    
    Args:
        duration: Recording duration in seconds.
        
    Returns:
        numpy array containing audio data (float32, normalized).
    """
    print(f"\n🎤 Recording for {duration} seconds... Speak now!")
    
    # Record audio with float32 dtype
    audio_data = sd.rec(
        int(duration * SAMPLE_RATE),
        samplerate=SAMPLE_RATE,
        channels=CHANNELS,
        dtype='float32'
    )
    sd.wait()  # Wait until recording is finished
    
    print("✓ Recording complete!")
    
    # Print max volume for debugging
    max_volume = np.abs(audio_data).max()
    print(f"📊 Max volume: {max_volume:.4f}")
    
    # Normalize audio if needed
    if max_volume > 0:
        audio_data = audio_data / max_volume
        print(f"✓ Audio normalized (peak: {max_volume:.4f})")
    else:
        print("⚠️  Warning: No audio detected (max volume is 0)")
    
    return audio_data


def save_audio(audio_data: np.ndarray, filename: str = TEMP_AUDIO_FILE):
    """
    Save audio data to WAV file.
    
    Args:
        audio_data: Audio data as numpy array (float32, normalized).
        filename: Output filename.
    """
    # Convert float32 normalized audio to int16 for WAV file
    audio_int16 = (audio_data * 32767).astype(np.int16)
    
    with wave.open(filename, 'wb') as wf:
        wf.setnchannels(CHANNELS)
        wf.setsampwidth(2)  # 16-bit audio
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(audio_int16.tobytes())


def transcribe_audio(audio_file: str, model) -> str:
    """
    Transcribe audio file to text using Whisper.
    
    Args:
        audio_file: Path to audio file.
        model: Loaded Whisper model.
        
    Returns:
        Transcribed text.
    """
    print("🔄 Transcribing audio...")
    
    result = model.transcribe(audio_file)
    text = result["text"].strip()
    
    print(f"📝 You said: '{text}'")
    
    return text


def speak_text(text: str):
    """
    Convert text to speech and play it.
    
    Args:
        text: Text to speak.
    """
    print(f"\n🔊 Speaking: {text[:100]}...")
    
    try:
        engine = pyttsx3.init()
        
        # Configure voice properties
        engine.setProperty('rate', 150)  # Speed of speech
        engine.setProperty('volume', 0.9)  # Volume (0.0 to 1.0)
        
        # Speak the text
        engine.say(text)
        engine.runAndWait()
        
    except Exception as e:
        print(f"✗ Error in text-to-speech: {e}")
        print(f"Text: {text}")


def cleanup_temp_files():
    """Remove temporary audio files."""
    if os.path.exists(TEMP_AUDIO_FILE):
        os.remove(TEMP_AUDIO_FILE)


def start_voice_assistant():
    """
    Start the voice assistant loop.
    
    Continuously listens for voice input, transcribes it, queries the RAG system,
    and speaks the answer back to the user.
    """
    print("\n" + "="*60)
    print("🎙️  Krishi Sahayak Voice Assistant")
    print("="*60)
    print("\nInitializing...")
    
    try:
        # Load Whisper model
        print("📥 Loading Whisper model (base)...")
        whisper_model = whisper.load_model("base")
        print("✓ Whisper model loaded successfully!")
        
        # Initialize text-to-speech engine
        print("🔊 Initializing text-to-speech engine...")
        tts_engine = pyttsx3.init()
        print("✓ Text-to-speech engine ready!")
        
        print("\n" + "="*60)
        print("✅ Voice Assistant is ready!")
        print("="*60)
        print("\nInstructions:")
        print("- Speak your question when prompted")
        print("- Say 'exit' to quit the assistant")
        print("- Each recording is 5 seconds long")
        print("\n" + "="*60 + "\n")
        
        # Main voice assistant loop
        while True:
            try:
                # Record audio
                audio_data = record_audio(DURATION)
                
                # Save audio to file
                save_audio(audio_data, TEMP_AUDIO_FILE)
                
                # Transcribe audio to text
                question = transcribe_audio(TEMP_AUDIO_FILE, whisper_model)
                
                # Check for exit command
                if question.lower() in ["exit", "quit", "stop", "bye"]:
                    print("\n👋 Goodbye! Exiting voice assistant...")
                    speak_text("Goodbye!")
                    break
                
                # Skip if no meaningful input
                if not question or len(question.strip()) < 3:
                    print("⚠️  No clear input detected. Please try again.")
                    continue
                
                # Query RAG pipeline
                print("\n🔍 Searching for answer...")
                result = query_rag_pipeline(question)
                
                # Extract answer from result
                if result.get("success"):
                    answer = result.get("answer", "I could not find an answer.")
                    sections = result.get("sections", [])
                    
                    print(f"\n✅ Answer found!")
                    if sections:
                        print(f"📚 Sources: {', '.join(sections)}")
                    
                    # Speak the answer
                    speak_text(answer)
                else:
                    error_msg = "I encountered an error while processing your question."
                    print(f"\n✗ Error: {result.get('error', 'Unknown error')}")
                    speak_text(error_msg)
                
                print("\n" + "-"*60 + "\n")
                
            except KeyboardInterrupt:
                print("\n\n⚠️  Interrupted by user. Exiting...")
                break
                
            except Exception as e:
                print(f"\n✗ Error during interaction: {e}")
                print("Continuing to next interaction...\n")
                continue
        
    except Exception as e:
        print(f"\n✗ Fatal error initializing voice assistant: {e}")
        print("Please ensure all dependencies are installed:")
        print("  pip install openai-whisper sounddevice scipy pyttsx3")
        
    finally:
        # Cleanup
        print("\n🧹 Cleaning up...")
        cleanup_temp_files()
        print("✓ Cleanup complete!")
        print("\n" + "="*60)
        print("Voice Assistant stopped.")
        print("="*60 + "\n")


if __name__ == "__main__":
    start_voice_assistant()
