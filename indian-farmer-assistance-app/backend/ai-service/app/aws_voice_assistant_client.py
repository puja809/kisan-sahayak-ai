"""
AWS Voice Assistant API Client
Calls the AWS Lambda-based question answering API
"""
import requests
import json
import logging
from typing import Dict, Optional
from dotenv import load_dotenv
import os

# Load environment variables
load_dotenv()

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# AWS API Configuration
AWS_API_ENDPOINT = "https://5m2acu2lea.execute-api.us-east-1.amazonaws.com/prod/ask"
REQUEST_TIMEOUT = 30  # seconds


def ask_question(question: str) -> Dict:
    """
    Send a question to the AWS Voice Assistant API and get an answer.
    
    Args:
        question (str): The question to ask the voice assistant
        
    Returns:
        Dict: Response containing the answer or error details
        
    Raises:
        requests.exceptions.RequestException: For network-related errors
        json.JSONDecodeError: For JSON parsing errors
    """
    
    if not question or not isinstance(question, str):
        logger.error("Invalid question: must be a non-empty string")
        return {
            "success": False,
            "error": "Invalid question: must be a non-empty string",
            "status_code": 400
        }
    
    # Prepare request
    headers = {
        "Content-Type": "application/json"
    }
    
    payload = {
        "question": question.strip()
    }
    
    try:
        logger.info(f"Sending question to AWS API: {question[:50]}...")
        
        # Make POST request
        response = requests.post(
            AWS_API_ENDPOINT,
            headers=headers,
            json=payload,
            timeout=REQUEST_TIMEOUT
        )
        
        # Log status code
        logger.info(f"HTTP Status Code: {response.status_code}")
        
        # Handle non-200 responses
        if response.status_code != 200:
            logger.warning(f"Non-200 response: {response.status_code}")
            try:
                error_data = response.json()
                return {
                    "success": False,
                    "error": error_data.get("message", "Unknown error"),
                    "status_code": response.status_code,
                    "response": error_data
                }
            except json.JSONDecodeError:
                return {
                    "success": False,
                    "error": f"HTTP {response.status_code}: {response.text}",
                    "status_code": response.status_code
                }
        
        # Parse JSON response
        try:
            response_data = response.json()
            logger.info("Successfully parsed JSON response")
            
            # Validate response has answer field
            if "text" not in response_data:
                logger.warning("Response missing 'answer' field")
                return {
                    "success": False,
                    "error": "Response missing 'answer' field",
                    "status_code": 200,
                    "response": response_data
                }
            
            return {
                "success": True,
                "answer": response_data["text"],
                "status_code": response.status_code,
                "response": response_data
            }
            
        except json.JSONDecodeError as e:
            logger.error(f"JSON parsing error: {e}")
            return {
                "success": False,
                "error": f"JSON parsing error: {str(e)}",
                "status_code": response.status_code,
                "raw_response": response.text
            }
    
    except requests.exceptions.Timeout:
        logger.error(f"Request timeout after {REQUEST_TIMEOUT} seconds")
        return {
            "success": False,
            "error": f"Request timeout after {REQUEST_TIMEOUT} seconds",
            "status_code": None
        }
    
    except requests.exceptions.ConnectionError as e:
        logger.error(f"Connection error: {e}")
        return {
            "success": False,
            "error": f"Connection error: {str(e)}",
            "status_code": None
        }
    
    except requests.exceptions.RequestException as e:
        logger.error(f"Request error: {e}")
        return {
            "success": False,
            "error": f"Request error: {str(e)}",
            "status_code": None
        }
    
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return {
            "success": False,
            "error": f"Unexpected error: {str(e)}",
            "status_code": None
        }


def main():
    """Test the AWS Voice Assistant API client"""
    
    print("=" * 60)
    print("AWS Voice Assistant API Client Test")
    print("=" * 60)
    
    # Test questions
    test_questions = [
        "What is AIF scheme?",
        "Tell me about agricultural subsidies",
        "How do I apply for farming loans?"
    ]
    
    for question in test_questions:
        print(f"\n📝 Question: {question}")
        print("-" * 60)
        
        result = ask_question(question)
        
        print(f"✓ Status Code: {result.get('status_code')}")
        print(f"✓ Success: {result.get('success')}")
        
        if result.get('success'):
            print(f"✓ Answer: {result.get('answer')}")
        else:
            print(f"✗ Error: {result.get('error')}")
        
        print()


if __name__ == "__main__":
    main()
