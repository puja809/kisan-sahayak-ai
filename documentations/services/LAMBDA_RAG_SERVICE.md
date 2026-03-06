# AWS Lambda RAG Service Documentation

**Service Type:** AWS Lambda Function  
**Language:** Python 3.11+  
**Trigger:** API Gateway  
**Purpose:** Retrieval-Augmented Generation (RAG) for intelligent question answering and disease detection

## Overview

The AWS Lambda RAG Service is a serverless function that powers the voice assistant and disease detection capabilities. It uses AWS Bedrock for LLM inference, Knowledge Base for RAG, and integrates with multiple AWS services for a complete AI solution.

## Key Responsibilities

- Question answering with Retrieval-Augmented Generation (RAG)
- Voice input processing (transcription and synthesis)
- Crop disease detection from images
- Chat memory management
- Multilingual support (10+ Indian languages)
- Image format auto-detection and processing

## Architecture

```
API Gateway
    ↓
Lambda Function (Lambda_function.py)
    ├─→ Text Mode (Question Answering)
    │   ├─ Bedrock Knowledge Base (RAG)
    │   ├─ Bedrock LLM (Llama 3 8B)
    │   └─ DynamoDB (Chat Memory)
    │
    ├─→ Voice Mode (Audio Processing)
    │   ├─ S3 (Audio Storage)
    │   ├─ Transcribe (Speech-to-Text)
    │   ├─ Bedrock LLM (Answer Generation)
    │   ├─ Polly (Text-to-Speech)
    │   └─ DynamoDB (Chat Memory)
    │
    └─→ Image Mode (Disease Detection)
        ├─ S3 (Image Storage)
        ├─ Bedrock Vision Model (Llama 4 Scout)
        └─ DynamoDB (Chat Memory)
```

## Configuration

### AWS Resources Required

```
1. Bedrock Knowledge Base
   - ID: 4382GUKREH
   - Model: meta.llama3-8b-instruct-v1:0
   - Purpose: RAG for agricultural knowledge

2. Bedrock Vision Model
   - ARN: arn:aws:bedrock:us-east-1:211125690509:application-inference-profile/tejkhkadyemh
   - Model: Llama 4 Scout (Vision)
   - Purpose: Disease detection from images

3. S3 Bucket
   - Name: krishi-sahayak-docs
   - Folders:
     - kisan-voice-temp/ (Audio files)
     - disease-images/ (Disease detection images)

4. DynamoDB Table
   - Name: KisanChatMemory
   - Primary Key: session_id
   - Attributes: messages (list)

5. AWS Services
   - Transcribe: Speech-to-text conversion
   - Polly: Text-to-speech synthesis
   - Bedrock Runtime: LLM inference
   - Bedrock Agent Runtime: RAG queries
```

### Environment Variables

```
KNOWLEDGE_BASE_ID=4382GUKREH
MODEL_ARN=arn:aws:bedrock:us-east-1::foundation-model/meta.llama3-8b-instruct-v1:0
VISION_MODEL_ID=arn:aws:bedrock:us-east-1:211125690509:application-inference-profile/tejkhkadyemh
S3_BUCKET=krishi-sahayak-docs
VOICE_FOLDER=kisan-voice-temp/
DISEASE_FOLDER=disease-images/
TABLE_NAME=KisanChatMemory
AWS_REGION=us-east-1
```

## API Endpoints

### Text Question Answering

**Endpoint**: `POST /ask`

**Request**:
```json
{
  "question": "What is the best time to plant rice?",
  "language": "hi",
  "session_id": "user-123-session"
}
```

**Response**:
```json
{
  "statusCode": 200,
  "body": {
    "text": "धान रोपण का सर्वोत्तम समय मई-जून है..."
  }
}
```

### Voice Question Answering

**Endpoint**: `POST /ask-voice`

**Request**:
```json
{
  "audio": "base64_encoded_audio",
  "language": "hi",
  "session_id": "user-123-session"
}
```

**Response**:
```json
{
  "statusCode": 200,
  "body": {
    "text": "धान रोपण का सर्वोत्तम समय मई-जून है...",
    "audio": "base64_encoded_mp3_response",
    "language": "hi"
  }
}
```

### Disease Detection

**Endpoint**: `POST /detect-disease`

**Request**:
```json
{
  "image": "base64_encoded_image",
  "language": "hi",
  "session_id": "user-123-session"
}
```

**Response**:
```json
{
  "statusCode": 200,
  "body": {
    "disease_analysis": "फसल: गेहूं\nरोग: पत्ती धब्बा\nलक्षण: पत्तियों पर भूरे धब्बे...\nउपचार: कवकनाशी का छिड़काव करें...\nरोकथाम: स्वस्थ बीज का उपयोग करें..."
  }
}
```

## Core Functions

### 1. Text Mode: Question Answering with RAG

```python
def process_question_with_memory(question, chat_history, user_language):
    """
    Process user question using Bedrock RAG
    
    Steps:
    1. Build memory context from chat history
    2. Create prompt with knowledge base context
    3. Call Bedrock retrieve_and_generate
    4. Return answer in user's language
    """
    memory_context = build_memory_context(chat_history)
    
    prompt = f"""
    You are "Kisan Sahayak", an AI assistant helping Indian farmers.
    
    RULES:
    1. Answer ONLY agriculture-related questions.
    2. Use the provided context from Knowledge Base.
    3. If not found, say "I don't have this information".
    4. Respond in {user_language}. Use the appropriate script (e.g., Devanagari for Hindi).
    
    CONTEXT: {memory_context}
    QUESTION: {question}
    
    Answer clearly and helpfully in {user_language}.
    """
    
    response = bedrock.retrieve_and_generate(
        input={"text": prompt},
        retrieveAndGenerateConfiguration={
            "type": "KNOWLEDGE_BASE",
            "knowledgeBaseConfiguration": {
                "knowledgeBaseId": KNOWLEDGE_BASE_ID,
                "modelArn": MODEL_ARN
            }
        }
    )
    
    return response["output"]["text"]
```

### 2. Voice Mode: Audio Processing

```python
def handle_voice_input(audio_bytes, user_language, session_id):
    """
    Process voice input:
    1. Upload audio to S3
    2. Transcribe using AWS Transcribe
    3. Process transcribed text with RAG
    4. Synthesize response using Polly
    5. Return audio response
    """
    # Upload to S3
    audio_key = VOICE_FOLDER + str(uuid.uuid4()) + ".wav"
    s3.put_object(Bucket=S3_BUCKET, Key=audio_key, Body=audio_bytes)
    
    # Transcribe
    job_name = "job-" + str(uuid.uuid4())
    transcribe.start_transcription_job(
        TranscriptionJobName=job_name,
        Media={"MediaFileUri": f"s3://{S3_BUCKET}/{audio_key}"},
        MediaFormat="wav",
        LanguageCode=get_transcribe_language(user_language)
    )
    
    # Wait for transcription
    while True:
        status = transcribe.get_transcription_job(TranscriptionJobName=job_name)
        if status["TranscriptionJob"]["TranscriptionJobStatus"] in ["COMPLETED", "FAILED"]:
            break
        time.sleep(2)
    
    # Get transcript
    transcript_uri = status["TranscriptionJob"]["Transcript"]["TranscriptFileUri"]
    transcript_text = json.loads(urllib.request.urlopen(transcript_uri).read())
    
    # Process with RAG
    answer_text = process_question_with_memory(transcript_text, chat_history, user_language)
    
    # Synthesize speech
    polly_response = polly.synthesize_speech(
        Text=answer_text,
        OutputFormat="mp3",
        VoiceId=get_polly_voice(user_language)
    )
    
    return answer_text, polly_response["AudioStream"].read()
```

### 3. Image Mode: Disease Detection

```python
def handle_disease_detection(body, session_id, user_language):
    """
    Detect crop disease from image:
    1. Auto-detect image format
    2. Upload to S3
    3. Call Bedrock Vision Model (Llama 4 Scout)
    4. Parse response
    5. Save to chat memory
    """
    base64_image = body["image"]
    
    # Auto-detect format
    if base64_image.startswith("iVBOR"):
        image_format = "png"
    elif base64_image.startswith("/9j/"):
        image_format = "jpeg"
    elif base64_image.startswith("UklG"):
        image_format = "webp"
    else:
        image_format = "jpeg"
    
    image_bytes = base64.b64decode(base64_image)
    
    # Upload to S3
    image_key = DISEASE_FOLDER + str(uuid.uuid4()) + f".{image_format}"
    s3.put_object(Bucket=S3_BUCKET, Key=image_key, Body=image_bytes)
    
    # Call Vision Model
    response = bedrock_runtime.converse(
        modelId=VISION_MODEL_ID,
        messages=[
            {
                "role": "user",
                "content": [
                    {
                        "text": f"""
                        Identify crop and disease from image.
                        Provide: Crop Name, Disease Name, Symptoms, Treatment, Prevention
                        Respond in {user_language}
                        """
                    },
                    {
                        "image": {
                            "format": image_format,
                            "source": {"bytes": image_bytes}
                        }
                    }
                ]
            }
        ]
    )
    
    answer_text = response["output"]["message"]["content"][0]["text"]
    
    # Save to memory
    chat_history = get_chat_history(session_id)
    chat_history.append({"role": "user", "content": "[Uploaded crop image]"})
    chat_history.append({"role": "assistant", "content": answer_text})
    save_chat_history(session_id, chat_history)
    
    return answer_text
```

## Chat Memory Management

### DynamoDB Schema

```
Table: KisanChatMemory
├─ Partition Key: session_id (String)
└─ Attributes:
   └─ messages (List)
      ├─ role (String): "user" or "assistant"
      └─ content (String): Message text
```

### Memory Functions

```python
def get_chat_history(session_id):
    """Retrieve chat history for session"""
    response = table.get_item(Key={"session_id": session_id})
    return response.get("Item", {}).get("messages", [])

def save_chat_history(session_id, messages):
    """Save chat history to DynamoDB"""
    table.put_item(Item={
        "session_id": session_id,
        "messages": messages
    })

def build_memory_context(messages):
    """Build context from last 5 messages"""
    context = ""
    for msg in messages[-5:]:
        context += f"{msg['role']}: {msg['content']}\n"
    return context
```

## Multilingual Support

### Supported Languages

| Language | Code | Transcribe | Polly | Bedrock |
|----------|------|-----------|-------|---------|
| Hindi    | hi   | hi-IN     | Aditi | ✓ (Devanagari) |
| Bengali  | bn   | bn-IN     | Kajal | ✓ (Bengali) |
| Telugu   | te   | te-IN     | Shruti| ✓ (Telugu) |
| Marathi  | mr   | mr-IN     | Arpita| ✓ (Devanagari) |
| Tamil    | ta   | ta-IN     | Priyadarshani | ✓ (Tamil) |
| Gujarati | gu   | gu-IN     | Dhvani| ✓ (Gujarati) |
| Punjabi  | pa   | pa-IN     | Aditi*| ✓ (Gurmukhi) |
| Kannada  | ka   | kn-IN     | Aditi*| ✓ (Kannada) |
| Malayalam| ml   | ml-IN     | Aditi*| ✓ (Malayalam) |
| English  | en   | en-IN     | Sonia | ✓ (Latin) |

*Fallback to Aditi for languages not natively supported by Polly

### Language Mapping Functions

```python
def get_transcribe_language(lang_code):
    """Map language code to AWS Transcribe format"""
    mapping = {
        "Hindi": "hi-IN", "hi": "hi-IN",
        "Bengali": "bn-IN", "bn": "bn-IN",
        # ... more mappings
    }
    return mapping.get(lang_code, "en-IN")

def get_polly_voice(lang_code):
    """Map language code to Polly voice"""
    mapping = {
        "Hindi": "Aditi", "hi": "Aditi",
        "Bengali": "Kajal", "bn": "Kajal",
        # ... more mappings
    }
    return mapping.get(lang_code, "Aditi")
```

## Bedrock Knowledge Base (RAG)

### Knowledge Base Configuration

- **ID**: 4382GUKREH
- **Model**: meta.llama3-8b-instruct-v1:0
- **Purpose**: Retrieve agricultural knowledge for RAG

### RAG Process

```
User Question
    ↓
Bedrock Knowledge Base
    ├─ Search relevant documents
    ├─ Retrieve top-k results
    └─ Return context
    ↓
Bedrock LLM (Llama 3 8B)
    ├─ Combine context with question
    ├─ Generate answer
    └─ Return response
    ↓
Response to User
```

### Knowledge Base Documents

The knowledge base contains:
- Crop cultivation guides
- Pest and disease management
- Fertilizer recommendations
- Weather-based advisories
- Government schemes
- Market information
- Soil management practices

## Vision Model (Disease Detection)

### Model Details

- **Model**: Llama 4 Scout (Vision)
- **ARN**: arn:aws:bedrock:us-east-1:211125690509:application-inference-profile/tejkhkadyemh
- **Capabilities**:
  - Image classification
  - Object detection
  - Text extraction
  - Multilingual responses

### Disease Detection Process

```
Crop Image
    ↓
Auto-detect Format (PNG/JPEG/WebP)
    ↓
Upload to S3
    ↓
Bedrock Vision Model
    ├─ Analyze image
    ├─ Identify crop
    ├─ Detect disease
    ├─ Extract symptoms
    ├─ Generate treatment
    └─ Generate prevention
    ↓
Response in User's Language
```

## Error Handling

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| Transcription failed | Audio quality issue | Retry with clearer audio |
| Knowledge Base timeout | Large query | Simplify question |
| Image format error | Unsupported format | Use PNG/JPEG/WebP |
| Language not supported | Invalid language code | Use supported language |
| DynamoDB error | Table not found | Check table name and permissions |

### Error Response Format

```json
{
  "statusCode": 500,
  "body": {
    "error": "Error message describing the issue"
  }
}
```

## Performance Considerations

### Latency

- **Text Mode**: 2-5 seconds (RAG + LLM)
- **Voice Mode**: 10-15 seconds (Transcribe + RAG + Polly)
- **Image Mode**: 5-10 seconds (Vision Model)

### Concurrency

- Lambda concurrent executions: 1000 (default)
- Bedrock concurrent requests: 100 (default)
- Transcribe concurrent jobs: 100 (default)

### Cost Optimization

- Use Lambda reserved concurrency for predictable workloads
- Cache frequently asked questions
- Optimize Knowledge Base queries
- Use S3 lifecycle policies for temporary files

## Deployment

### Lambda Configuration

```
Runtime: Python 3.11
Memory: 3008 MB (max for better performance)
Timeout: 900 seconds (15 minutes for voice processing)
Ephemeral Storage: 10240 MB
Environment Variables: See Configuration section
```

### IAM Permissions Required

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel",
        "bedrock:InvokeModelWithResponseStream",
        "bedrock-agent-runtime:RetrieveAndGenerate"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::krishi-sahayak-docs/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "transcribe:StartTranscriptionJob",
        "transcribe:GetTranscriptionJob"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "polly:SynthesizeSpeech"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem"
      ],
      "Resource": "arn:aws:dynamodb:*:*:table/KisanChatMemory"
    }
  ]
}
```

### API Gateway Integration

```
Trigger: API Gateway
Methods: POST
Paths:
  - /ask (Text)
  - /ask-voice (Voice)
  - /detect-disease (Image)
  - /ask/stream (Streaming text)
  - /ask-voice/stream (Streaming voice)
```

## Monitoring & Logging

### CloudWatch Logs

- Log Group: `/aws/lambda/kisan-sahayak`
- Log Stream: Per invocation
- Retention: 30 days

### Metrics

- Invocations
- Duration
- Errors
- Throttles
- Concurrent Executions

### Alarms

- Error rate > 5%
- Duration > 30 seconds
- Throttles > 0

## Future Enhancements

**Last Updated**: March 6, 2026  
**Version**: 1.1.0  
**Status**: Active (Enhanced Multi-Language & Prompt Tuning)
 **Multi-turn Conversations**: Better context management
3. **Image Caching**: Cache disease detection results
4. **Fine-tuned Models**: Custom models for specific crops
5. **Feedback Loop**: Improve answers based on user feedback
6. **Analytics**: Track popular questions and diseases
7. **Offline Support**: Cache common answers locally
8. **Integration with IoT**: Real-time sensor data analysis
```
