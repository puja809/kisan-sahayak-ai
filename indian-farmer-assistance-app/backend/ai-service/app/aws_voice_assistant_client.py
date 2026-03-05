"""
AWS Voice Assistant API Client
Calls the AWS Lambda-based question answering API
"""
import requests
import json
import logging
from typing import Dict
from dotenv import load_dotenv
import os

# Load environment variables
load_dotenv()

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# AWS API Configuration
AWS_API_TEXT_ENDPOINT = os.getenv("AWS_API_TEXT_ENDPOINT", "https://5m2acu2lea.execute-api.us-east-1.amazonaws.com/prod/ask")
AWS_API_VOICE_ENDPOINT = os.getenv("AWS_API_VOICE_ENDPOINT", "https://5m2acu2lea.execute-api.us-east-1.amazonaws.com/prod/ask-voice")
AWS_API_STREAM_TEXT_ENDPOINT = os.getenv("AWS_API_STREAM_TEXT_ENDPOINT", "https://5m2acu2lea.execute-api.us-east-1.amazonaws.com/prod/ask/stream")
AWS_API_STREAM_VOICE_ENDPOINT = os.getenv("AWS_API_STREAM_VOICE_ENDPOINT", "https://5m2acu2lea.execute-api.us-east-1.amazonaws.com/prod/ask-voice/stream")
REQUEST_TIMEOUT = 30  # seconds


def ask_question_text(question: str) -> Dict:
    """
    Send a text question to the AWS Voice Assistant API and get an answer.
    
    Args:
        question (str): The question to ask the voice assistant
        
    Returns:
        Dict: Response containing the answer or error details
    """
    
    if not question or not isinstance(question, str):
        logger.error("Invalid question: must be a non-empty string")
        return {
            "success": False,
            "error": "Invalid question: must be a non-empty string",
            "status_code": 400
        }
    
    headers = {"Content-Type": "application/json"}
    payload = {"question": question.strip()}
    
    try:
        logger.info(f"Sending text question to AWS API: {question[:50]}...")
        
        response = requests.post(AWS_API_TEXT_ENDPOINT, headers=headers, json=payload, timeout=REQUEST_TIMEOUT)
        logger.info(f"HTTP Status Code: {response.status_code}")
        
        if response.status_code != 200:
            try:
                error_data = response.json()
                return {
                    "success": False,
                    "error": error_data.get("message", "Unknown error"),
                    "status_code": response.status_code
                }
            except json.JSONDecodeError:
                return {
                    "success": False,
                    "error": f"HTTP {response.status_code}: {response.text}",
                    "status_code": response.status_code
                }
        
        # Parse response
        response_data = response.json()
        logger.info(f"Response keys: {list(response_data.keys()) if isinstance(response_data, dict) else 'not a dict'}")
        
        # Handle API Gateway wrapped response
        if "body" in response_data:
            body_data = json.loads(response_data["body"])
            answer_text = body_data.get("text") or body_data.get("answer")
            if answer_text:
                return {"success": True, "answer": answer_text, "status_code": response.status_code}
            if body_data.get("error"):
                return {"success": False, "error": body_data.get("error"), "status_code": 200}
            return {"success": False, "error": "Response missing 'text' field", "status_code": 200}
        
        # Direct response
        answer_text = response_data.get("text") or response_data.get("answer")
        if answer_text:
            return {"success": True, "answer": answer_text, "status_code": response.status_code}
        
        return {"success": False, "error": "Response missing 'text' field", "status_code": 200}
    
    except requests.exceptions.Timeout:
        logger.error(f"Request timeout after {REQUEST_TIMEOUT} seconds")
        return {"success": False, "error": f"Request timeout after {REQUEST_TIMEOUT} seconds", "status_code": None}
    
    except requests.exceptions.RequestException as e:
        logger.error(f"Request error: {e}")
        return {"success": False, "error": f"Request error: {str(e)}", "status_code": None}
    
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return {"success": False, "error": f"Unexpected error: {str(e)}", "status_code": None}

def ask_question_text_stream(question: str, language: str = "en"):
    """
    Send a text question to the AWS Voice Assistant API and yield an event stream.
    """
    if not question or not isinstance(question, str):
        yield f"data: {json.dumps({'error': 'Invalid question'})}\n\n"
        return
        
    headers = {"Content-Type": "application/json"}
    payload = {"question": question.strip(), "language": language}
    
    try:
        logger.info(f"Sending stream text question to AWS API: {question[:50]}...")
        with requests.post(AWS_API_STREAM_TEXT_ENDPOINT, headers=headers, json=payload, timeout=60, stream=True) as response:
            if response.status_code != 200:
                yield f"data: {json.dumps({'error': f'HTTP {response.status_code} from AWS API'})}\n\n"
                return
            
            for line in response.iter_lines():
                if line:
                    decoded = line.decode('utf-8')
                    # AWS streamifyResponse might output plain JSON chunks or proper SSE based on the client setup
                    if not decoded.startswith('data:'):
                         yield f"data: {decoded}\n\n"
                    else:
                         yield f"{decoded}\n\n"
    except Exception as e:
        logger.error(f"Unexpected streaming error: {e}")
        yield f"data: {json.dumps({'error': str(e)})}\n\n"


def ask_question_audio(audio_base64: str) -> Dict:
    """
    Send audio to the AWS Voice Assistant API and get transcribed answer with audio response.
    AWS returns: transcribed_text, text (answer), audio (MP3 base64), language
    
    Args:
        audio_base64 (str): Base64 encoded audio data
        
    Returns:
        Dict: Response containing transcribed text, answer, and audio response
    """
    
    if not audio_base64 or not isinstance(audio_base64, str):
        logger.error("Invalid audio: must be a non-empty base64 string")
        return {
            "success": False,
            "error": "Invalid audio: must be a non-empty base64 string",
            "status_code": 400
        }
    
    headers = {"Content-Type": "application/json"}
    payload = {"audio": audio_base64}
    
    try:
        logger.info(f"Sending audio to AWS Voice API ({len(audio_base64)} base64 chars)...")
        
        response = requests.post(AWS_API_VOICE_ENDPOINT, headers=headers, json=payload, timeout=REQUEST_TIMEOUT)
        logger.info(f"HTTP Status Code: {response.status_code}")
        
        if response.status_code != 200:
            try:
                error_data = response.json()
                return {
                    "success": False,
                    "error": error_data.get("message", "Unknown error"),
                    "status_code": response.status_code
                }
            except json.JSONDecodeError:
                return {
                    "success": False,
                    "error": f"HTTP {response.status_code}: {response.text}",
                    "status_code": response.status_code
                }
        
        # Parse response
        response_data = response.json()
        logger.info(f"Response keys: {list(response_data.keys()) if isinstance(response_data, dict) else 'not a dict'}")
        
        # Handle API Gateway wrapped response
        if "body" in response_data:
            body_data = json.loads(response_data["body"])
            logger.info(f"Body keys: {list(body_data.keys()) if isinstance(body_data, dict) else 'not a dict'}")
            
            # Extract all fields from AWS response
            answer_text = body_data.get("text") or body_data.get("answer")
            transcribed_text = body_data.get("transcribed_text") or body_data.get("transcript", "")
            audio_response = body_data.get("audio")
            language = body_data.get("language", "en")
            
            if answer_text:
                return {
                    "success": True,
                    "answer": answer_text,
                    "transcribed_text": transcribed_text,
                    "audio": audio_response,
                    "language": language,
                    "status_code": response.status_code
                }
            if body_data.get("error"):
                return {"success": False, "error": body_data.get("error"), "status_code": 200}
            return {"success": False, "error": "Response missing 'text' field", "status_code": 200}
        
        # Direct response (not wrapped)
        answer_text = response_data.get("text") or response_data.get("answer")
        transcribed_text = response_data.get("transcribed_text") or response_data.get("transcript", "")
        audio_response = response_data.get("audio")
        language = response_data.get("language", "en")
        
        if answer_text:
            return {
                "success": True,
                "answer": answer_text,
                "transcribed_text": transcribed_text,
                "audio": audio_response,
                "language": language,
                "status_code": response.status_code
            }
        
        return {"success": False, "error": "Response missing 'text' field", "status_code": 200}
    
    except requests.exceptions.Timeout:
        logger.error(f"Request timeout after {REQUEST_TIMEOUT} seconds")
        return {"success": False, "error": f"Request timeout after {REQUEST_TIMEOUT} seconds", "status_code": None}
    
    except requests.exceptions.RequestException as e:
        logger.error(f"Request error: {e}")
        return {"success": False, "error": f"Request error: {str(e)}", "status_code": None}
    
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return {"success": False, "error": f"Unexpected error: {str(e)}", "status_code": None}


def ask_question_audio_stream(audio_base64: str, language: str = "en"):
    """
    Send audio to the AWS Voice Assistant API and yield an event stream.
    """
    if not audio_base64 or not isinstance(audio_base64, str):
        yield f"data: {json.dumps({'error': 'Invalid audio base64'})}\n\n"
        return
        
    headers = {"Content-Type": "application/json"}
    payload = {"audio": audio_base64, "language": language}
    
    try:
        logger.info(f"Sending stream voice question to AWS API ({len(audio_base64)} chars)...")
        with requests.post(AWS_API_STREAM_VOICE_ENDPOINT, headers=headers, json=payload, timeout=60, stream=True) as response:
            if response.status_code != 200:
                yield f"data: {json.dumps({'error': f'HTTP {response.status_code} from AWS API'})}\n\n"
                return
            
            for line in response.iter_lines():
                if line:
                    decoded = line.decode('utf-8')
                    if not decoded.startswith('data:'):
                         yield f"data: {decoded}\n\n"
                    else:
                         yield f"{decoded}\n\n"
    except Exception as e:
        logger.error(f"Unexpected streaming voice error: {e}")
        yield f"data: {json.dumps({'error': str(e)})}\n\n"


# Keep backward compatible alias
def ask_question(question: str) -> Dict:
    """Backward compatible wrapper - delegates to ask_question_text"""
    return ask_question_text(question)


def main():
    """Test the AWS Voice Assistant API client"""
    
    print("=" * 60)
    print("AWS Voice Assistant API Client Test")
    print("=" * 60)
    
    # Test text question
    test_question = "What is AIF scheme?"
    print(f"\n📝 Testing text question: {test_question}")
    print("-" * 60)
    
    result = ask_question_text(test_question)
    print(f"✓ Status Code: {result.get('status_code')}")
    print(f"✓ Success: {result.get('success')}")
    
    if result.get('success'):
        print(f"✓ Answer: {result.get('answer')}")
    else:
        print(f"✗ Error: {result.get('error')}")


if __name__ == "__main__":
    main()