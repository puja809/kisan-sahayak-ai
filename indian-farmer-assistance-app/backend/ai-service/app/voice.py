import sounddevice as sd
import numpy as np
import requests
import base64
import scipy.io.wavfile as wav
import tempfile
import time
import json
import os
from pydub import AudioSegment
import simpleaudio as sa

# -------------------------------
# CONFIG
# -------------------------------
API_URL = "https://5m2acu2lea.execute-api.us-east-1.amazonaws.com/prod/ask-voice"
DURATION = 5  # seconds
SAMPLE_RATE = 16000


# -------------------------------
# RECORD AUDIO
# -------------------------------
def record_audio():
    print("🎤 Recording... Speak now")

    recording = sd.rec(
        int(DURATION * SAMPLE_RATE),
        samplerate=SAMPLE_RATE,
        channels=1,
        dtype="int16"
    )
    sd.wait()

    print("✅ Recording finished")

    temp_wav = tempfile.NamedTemporaryFile(delete=False, suffix=".wav")
    wav.write(temp_wav.name, SAMPLE_RATE, recording)

    return temp_wav.name


# -------------------------------
# CONVERT TO BASE64
# -------------------------------
def convert_to_base64(file_path):
    with open(file_path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")


# -------------------------------
# SEND TO AWS
# -------------------------------
def send_audio(base64_audio):
    print("📤 Sending to AI...")

    try:
        response = requests.post(
            API_URL,
            json={"audio": base64_audio},
            timeout=180
        )

        response.raise_for_status()

        data = response.json()

        # IMPORTANT: API Gateway wraps Lambda response in "body"
        if "body" in data:
            return json.loads(data["body"])

        return data

    except requests.exceptions.RequestException as e:
        print("❌ Network Error:", e)
        return None
    except Exception as e:
        print("❌ Parsing Error:", e)
        return None


# -------------------------------
# PLAY RESPONSE AUDIO
# -------------------------------
def play_response(base64_audio):
    print("🔊 Playing response...")

    try:
        audio_bytes = base64.b64decode(base64_audio)

        # Save MP3 temp file
        temp_mp3 = tempfile.NamedTemporaryFile(delete=False, suffix=".mp3")
        with open(temp_mp3.name, "wb") as f:
            f.write(audio_bytes)

        # Convert MP3 → WAV (simpleaudio needs WAV)
        sound = AudioSegment.from_mp3(temp_mp3.name)
        wav_path = temp_mp3.name.replace(".mp3", ".wav")
        sound.export(wav_path, format="wav")

        # Play WAV
        wave_obj = sa.WaveObject.from_wave_file(wav_path)
        play_obj = wave_obj.play()
        play_obj.wait_done()

        # Cleanup
        os.remove(temp_mp3.name)
        os.remove(wav_path)

    except Exception as e:
        print("❌ Audio Playback Error:", e)


# -------------------------------
# MAIN FLOW
# -------------------------------
def main():
    try:
        audio_file = record_audio()
        base64_audio = convert_to_base64(audio_file)

        result = send_audio(base64_audio)

        if not result:
            print("⚠ No response from server.")
            return

        print("\n🌍 Detected Language:", result.get("language"))
        print("📝 AI says:", result.get("text"))

        time.sleep(1)

        if "audio" in result:
            play_response(result["audio"])
        else:
            print("⚠ No audio returned from API.")

    except Exception as e:
        print("❌ Unexpected Error:", e)


if __name__ == "__main__":
    main()