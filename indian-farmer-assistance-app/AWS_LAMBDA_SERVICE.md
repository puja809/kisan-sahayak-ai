# AWS Lambda Service (Kisan Sahayak)

The AWS Lambda service is a serverless component of the Indian Farmer Assistance Application that provides voice assistance, RAG-based (Retrieval-Augmented Generation) text answering, and image-based disease detection.

## 🚀 Overview

- **Runtime:** Python 3.x
- **Core File:** `Lambda_function.py`
- **Primary Purpose:** Provide high-performance, scalable AI capabilities using AWS Bedrock, Transcribe, Polly, and DynamoDB.

## 🛠️ Key Features

### 1. Voice Assistance (Multilingual)
- **Transcription:** Uses **AWS Transcribe** to convert farmer's voice (WAV format) into text in multiple Indian languages (Hindi, Bengali, Telugu, etc.).
- **Text-to-Speech:** Uses **AWS Polly** (Voices: Aditi, Kajal, Shruti, etc.) to convert AI responses back into speech for the farmer.
- **Support:** Handles 10+ Indian languages with automatic routing to the correct language models.

### 2. Intelligent RAG (Retrieval-Augmented Generation)
- **Engine:** Powered by **AWS Bedrock** (Meta Llama 3).
- **Knowledge Base:** Uses a curated agricultural knowledge base to provide factual, farmer-friendly answers.
- **Memory:** Integrated with **Amazon DynamoDB** (`KisanChatMemory`) to maintain conversation history for context-aware responses.

### 3. Image-Based Disease Detection (NEW)
- **Model:** Uses **AWS Bedrock Vision** (Inference Profile).
- **Capability:** Identifies crops and diseases from uploaded images (PNG, JPEG, WebP).
- **Output:** Provides crop name, disease name, symptoms, treatment, and prevention in the user's preferred language.

## 📋 API Functionality

The Lambda handles three main modes based on user input:
- **Text Mode:** Processes a `question` and returns a text `answer`.
- **Voice Mode:** Processes an `audio` base64 string, transcribes it, gets an answer, and returns both `text` and `audio` response.
- **Image Mode:** Processes an `image` base64 string and returns a detailed `disease_analysis`.

## ⚙️ Configuration

The service relies on the following AWS resources and constants:
- `KNOWLEDGE_BASE_ID`: Bedrock Knowledge Base identifier.
- `MODEL_ARN`: Bedrock LLM ARN.
- `VISION_MODEL_ID`: Bedrock Vision Model inference profile ARN.
- `S3_BUCKET`: `krishi-sahayak-docs` for temporary audio and image storage.
- `TABLE_NAME`: `KisanChatMemory` (DynamoDB table).

## 📚 Further Reading

For more detailed technical documentation, please refer to:
- [Lambda RAG Service Documentation](../../documentations/services/LAMBDA_RAG_SERVICE.md)
