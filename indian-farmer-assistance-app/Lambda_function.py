"""
Krishi Sahayak AWS Lambda Backend.

Provides agricultural assistance to Indian farmers using AWS Bedrock and 
Amazon Transcribe/Polly for voice integration.

Supports both standard REST JSON responses and Server-Sent Events (SSE) streaming.
"""

import base64
import json
import logging
import time
import urllib.request
import uuid
from typing import Any, Dict, Generator, List, Optional

import boto3
from botocore.exceptions import ClientError

try:
    import awslambda
    HAS_AWSLAMBDA = True
except ImportError:
    HAS_AWSLAMBDA = False

# ===============================
# CONFIGURATION
# ===============================
KNOWLEDGE_BASE_ID = "4382GUKREH"
MODEL_ARN = "arn:aws:bedrock:us-east-1::foundation-model/meta.llama3-8b-instruct-v1:0"

# 🔥 NEW VISION MODEL
VISION_MODEL_ID = "arn:aws:bedrock:us-east-1:211125690509:application-inference-profile/tejkhkadyemh"

S3_BUCKET = "krishi-sahayak-docs"
VOICE_FOLDER = "kisan-voice-temp/"
DISEASE_FOLDER = "disease-images/"
TABLE_NAME = "KisanChatMemory"

# ===============================
# LOGGING
# ===============================
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# ===============================
# AWS CLIENTS
# ===============================
bedrock = boto3.client("bedrock-agent-runtime")
bedrock_runtime = boto3.client("bedrock-runtime")  # Vision client
transcribe = boto3.client("transcribe")
polly = boto3.client("polly")
s3 = boto3.client("s3")
dynamodb = boto3.resource("dynamodb")
table = dynamodb.Table(TABLE_NAME)


# ===============================
# HELPER FUNCTIONS
# ===============================
def success_response(body: Dict[str, Any]) -> Dict[str, Any]:
    """
    Format a standard API Gateway success response.

    Args:
        body: The dictionary payload to return.

    Returns:
        A dictionary containing statusCode, headers, and stringified body.
    """
    return {
        "statusCode": 200,
        "headers": {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Headers": "Content-Type",
            "Access-Control-Allow-Methods": "OPTIONS,POST",
        },
        "body": json.dumps(body),
    }


def get_chat_history(session_id: str) -> List[Dict[str, str]]:
    """
    Retrieve previous conversation history from DynamoDB.

    Args:
        session_id: The unique identifier for the user's session.

    Returns:
        A list of message dictionaries (role, content).
    """
    try:
        response = table.get_item(Key={"session_id": session_id})
        messages = response.get("Item", {}).get("messages", [])
        # Ensure we return a strict List[Dict[str, str]]
        return messages if isinstance(messages, list) else []
    except ClientError as e:
        logger.error("Failed to retrieve chat history: %s", e)
        return []


def save_chat_history(session_id: str, messages: List[Dict[str, str]]) -> None:
    """
    Save conversation history back to DynamoDB.

    Args:
        session_id: The unique identifier for the user's session.
        messages: A list of message dictionaries (role, content).
    """
    try:
        table.put_item(
            Item={
                "session_id": session_id,
                "messages": messages,
            }
        )
    except ClientError as e:
        logger.error("Failed to save chat history: %s", e)


def build_memory_context(messages: List[Dict[str, str]]) -> str:
    """
    Build a continuous string representation of the last N chat messages.

    Args:
        messages: A list of message dictionaries (role, content).

    Returns:
        A formatted string of the recent conversation history.
    """
    context = ""
    for msg in messages[-5:]:  # limit to last 5 exchanges
        role = msg.get("role", "unknown")
        content = msg.get("content", "")
        context += f"{role}: {content}\n"
    return context


# ===============================
# LANGUAGE ROUTING
# ===============================
def get_transcribe_language(lang_code: str) -> str:
    if lang_code == "hi":
        return "hi-IN"
    elif lang_code == "bn":
        return "bn-IN"
    else:
        return "en-IN"


def get_polly_voice(lang_code: str) -> str:
    if lang_code == "hi":
        return "Aditi"
    elif lang_code == "bn":
        return "Raveena"
    else:
        return "Aditi"


# ===============================
# 🖼 IMAGE DISEASE FUNCTION
# ===============================
def handle_disease_detection(
    body: Dict[str, Any], session_id: str, user_language: str
) -> Dict[str, Any]:
    base64_image = body["image"]

    # -------------------------------
    # ✅ AUTO DETECT IMAGE FORMAT
    # -------------------------------
    if base64_image.startswith("iVBOR"):
        image_format = "png"
        content_type = "image/png"
        extension = ".png"
    elif base64_image.startswith("/9j/"):
        image_format = "jpeg"
        content_type = "image/jpeg"
        extension = ".jpg"
    elif base64_image.startswith("UklG"):
        image_format = "webp"
        content_type = "image/webp"
        extension = ".webp"
    else:
        image_format = "jpeg"
        content_type = "image/jpeg"
        extension = ".jpg"

    image_bytes = base64.b64decode(base64_image)
    image_key = DISEASE_FOLDER + str(uuid.uuid4()) + extension

    # -------------------------------
    # ✅ STORE IMAGE IN S3
    # -------------------------------
    s3.put_object(
        Bucket=S3_BUCKET,
        Key=image_key,
        Body=image_bytes,
        ContentType=content_type
    )

    # -------------------------------
    # ✅ CALL VISION MODEL 
    # -------------------------------
    response = bedrock_runtime.converse(
        modelId=VISION_MODEL_ID,
        messages=[
            {
                "role": "user",
                "content": [
                    {
                        "text": f"""
Identify the crop and disease from this image.
Provide:
- Crop Name
- Disease Name
- Symptoms
- Treatment
- Prevention 
Return strictly in the following JSON format:

{{
    "crop": "",
    "disease": "",
    "symptoms": "",
    "treatment": "",
    "prevention": "",
    "confidence": "",
    "modelVersion": "1.0.0",
    "raw_analysis": ""
}}

Respond clearly in {user_language}.
"""
                    },
                    {
                        "image": {
                            "format": image_format,
                            "source": {
                                "bytes": image_bytes
                            }
                        }
                    }
                ]
            }
        ],
        inferenceConfig={
            "maxTokens": 512,
            "temperature": 0.2
        }
    )

    # -------------------------------
    # ✅ EXTRACT RESPONSE
    # -------------------------------
    answer_text = response["output"]["message"]["content"][0]["text"]

    # -------------------------------
    # ✅ SAVE TO MEMORY
    # -------------------------------
    chat_history = get_chat_history(session_id)
    chat_history.append({"role": "user", "content": "[Uploaded crop image]"})
    chat_history.append({"role": "assistant", "content": answer_text})
    save_chat_history(session_id, chat_history)

    return success_response({
        "disease_analysis": answer_text
    })


# ===============================
# BEDROCK GENERATION
# ===============================
def process_question_with_memory_stream(
    question: str, chat_history: List[Dict[str, str]], user_language: str
) -> Generator[str, None, None]:
    """
    Call AWS Bedrock Knowledge Base and yield generated response chunks.

    Args:
        question: The user's query.
        chat_history: The previous conversation context.
        user_language: The requested language for the response.

    Yields:
        Chunks of text as they are streamed from Bedrock.
    """
    memory_context = build_memory_context(chat_history)

    prompt = f"""
You are "Kisan Sahayak", an AI assistant helping Indian farmers.

IMPORTANT RULES:
1. Answer ONLY agriculture-related questions.
2. Use the provided context from Knowledge Base to answer.
3. If the answer is NOT found in the context, say:
   "I am sorry, I do not have information about this in my agricultural knowledge base."
4. Do NOT guess.
5. Do NOT hallucinate.
6. Do NOT answer unrelated topics (politics, sports, movies, etc.).

LANGUAGE RULES:
- Detect the language of the user's question automatically.
- If the question is in Hindi (even romanized), respond in Hindi script.
- If the question is in Bengali (even romanized), respond in Bengali script.
- If the question is in English, respond in English.
- Do NOT default to Hindi or Bengali.

CONTEXT FROM KNOWLEDGE BASE:
{memory_context}

USER QUESTION:
{question}

ANSWER LANGUAGE:
{user_language}

Answer clearly, practically, and in a farmer-friendly tone.
"""

    response = bedrock.retrieve_and_generate_stream(
        input={"text": prompt},
        retrieveAndGenerateConfiguration={
            "type": "KNOWLEDGE_BASE",
            "knowledgeBaseConfiguration": {
                "knowledgeBaseId": KNOWLEDGE_BASE_ID,
                "modelArn": MODEL_ARN,
            },
        },
    )

    for event in response.get("stream", []):
        if "chunk" in event and "bytes" in event["chunk"]:
            yield event["chunk"]["bytes"].decode("utf-8")


# ===============================
# CORE LOGIC
# ===============================
def core_handler(
    event: Dict[str, Any], context: Any, response_stream: Optional[Any] = None
) -> Optional[Dict[str, Any]]:
    """
    Main business logic for processing queries via text or audio.
    Supports optional streaming of SSE (Server-Sent Events) down to the client.

    Args:
        event: The AWS API Gateway event dictionary.
        context: The AWS Lambda context object.
        response_stream: The writable stream object (provided by awslambda.streamifyResponse).

    Returns:
        If strictly REST, returns an API Gateway proxy response.
        If streaming, writes to the stream and returns None.
    """
    try:
        # 1. Parse Input
        body_raw = event.get("body", "{}")
        body = json.loads(body_raw) if isinstance(body_raw, str) else body_raw

        # Fallback if body is not stringified JSON or is passed directly
        if not body and "question" in event:
            body = event
        if not body and "audio" in event:
            body = event

        session_id = body.get("session_id", "default-session")
        user_language = body.get("language", "en")

        # 2. Setup SSE Helpers
        def write_sse(data: Dict[str, Any]) -> None:
            """Write a JSON-serializable dictionary as an SSE data block."""
            if response_stream:
                try:
                    sse_payload = f"data: {json.dumps(data)}\n\n"
                    response_stream.write(sse_payload.encode("utf-8"))
                except Exception as stream_err:
                    logger.error("Failed to write to stream: %s", stream_err)

        if response_stream and hasattr(response_stream, "setContentType"):
            response_stream.setContentType("text/event-stream")

        chat_history = get_chat_history(session_id)

        # ======================
        # 🖼 IMAGE DISEASE MODE (NEW)
        # ======================
        if "image" in body:
            # We don't stream the vision model yet, just return standard response
            return handle_disease_detection(body, session_id, user_language)

        # ======================
        # TEXT MODE
        # ======================
        if "question" in body:
            user_input = body["question"]
            logger.info("Processing text query for session %s", session_id)

            if response_stream:
                write_sse({"type": "start"})

            answer_text = ""
            for chunk in process_question_with_memory_stream(user_input, chat_history, user_language):
                answer_text += chunk
                if response_stream:
                    write_sse({"type": "chunk", "chunk": chunk})

            # Update memory
            chat_history.append({"role": "user", "content": user_input})
            chat_history.append({"role": "assistant", "content": answer_text})
            save_chat_history(session_id, chat_history)

            if response_stream:
                write_sse({"type": "done", "text": answer_text})
                response_stream.end()
                return None

            return success_response({"text": answer_text})

        # ======================
        # VOICE MODE
        # ======================
        if "audio" not in body:
            error_msg = "No question or audio provided in payload"
            if response_stream:
                write_sse({"error": error_msg})
                response_stream.end()
                return None
            return success_response({"error": error_msg})

        logger.info("Processing audio query for session %s", session_id)
        if response_stream:
            write_sse({"type": "status", "message": "Processing audio..."})

        # 3. Handle Audio Upload
        audio_bytes = base64.b64decode(body["audio"])
        audio_key = f"{VOICE_FOLDER}{uuid.uuid4()}.wav"

        s3.put_object(Bucket=S3_BUCKET, Key=audio_key, Body=audio_bytes)

        # 4. Transcribe Audio
        job_name = f"job-{uuid.uuid4()}"
        transcribe.start_transcription_job(
            TranscriptionJobName=job_name,
            Media={"MediaFileUri": f"s3://{S3_BUCKET}/{audio_key}"},
            MediaFormat="wav",
            LanguageCode=get_transcribe_language(user_language)
        )

        state = "IN_PROGRESS"
        while True:
            status = transcribe.get_transcription_job(TranscriptionJobName=job_name)
            state = status["TranscriptionJob"]["TranscriptionJobStatus"]

            if state in ["COMPLETED", "FAILED"]:
                break
            time.sleep(2)

        if state == "FAILED":
            if response_stream:
                write_sse({"error": "Transcription failed"})
                response_stream.end()
                return None
            return success_response({"error": "Transcription failed"})

        # 5. Download Transcription
        transcription_data = status["TranscriptionJob"]
        transcript_uri = transcription_data["Transcript"]["TranscriptFileUri"]

        transcript_resp = urllib.request.urlopen(transcript_uri).read()
        transcript_json = json.loads(transcript_resp)

        transcripts = transcript_json.get("results", {}).get("transcripts", [])
        if not transcripts:
            if response_stream:
                write_sse({"error": "No transcript found"})
                response_stream.end()
                return None
            return success_response({"error": "No transcript found"})

        transcript_text = transcripts[0].get("transcript", "")
        logger.info("Transcription success: %s", transcript_text)
        
        if response_stream:
            write_sse({"type": "transcript", "transcript": transcript_text})

        # 6. Generate AI Response
        answer_text = ""
        for chunk in process_question_with_memory_stream(transcript_text, chat_history, user_language):
            answer_text += chunk
            if response_stream:
                write_sse({"type": "chunk", "chunk": chunk})

        chat_history.append({"role": "user", "content": transcript_text})
        chat_history.append({"role": "assistant", "content": answer_text})
        save_chat_history(session_id, chat_history)

        if response_stream:
            write_sse({"type": "status", "message": "Generating audio response..."})

        # 7. Synthesize Speech Return
        polly_voice = get_polly_voice(user_language)

        polly_response = polly.synthesize_speech(
            Text=answer_text, OutputFormat="mp3", VoiceId=polly_voice
        )

        audio_stream = polly_response["AudioStream"].read()
        audio_base64_response = base64.b64encode(audio_stream).decode("utf-8")

        if response_stream:
            write_sse(
                {
                    "type": "audio",
                    "audio": audio_base64_response,
                    "language": user_language,
                    "done": True,
                }
            )
            response_stream.end()
            return None

        # Standard Return
        return success_response(
            {
                "text": answer_text,
                "audio": audio_base64_response,
                "language": user_language,
            }
        )

    except Exception as e:
        logger.exception("An error occurred during Lambda execution.")
        if response_stream:
            try:
                write_sse({"error": str(e)})
                response_stream.end()
            except Exception:
                pass
            return None
        return success_response({"error": str(e)})


# ===============================
# EXPORTED HANDLERS
# ===============================

def lambda_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """
    Standard native REST endpoint handler.
    Does NOT stream. Buffers the entire response and returns JSON.
    Use this handler for HTTP API or native REST without HTTP Response Streaming.
    """
    return core_handler(event, context, response_stream=None)


if HAS_AWSLAMBDA:
    @awslambda.streamifyResponse
    def stream_handler(event: Dict[str, Any], response_stream: Any, context: Any) -> None:
        """
        Streaming endpoint handler.
        Uses HTTP Response Streaming to send chunks context back to the client.
        Configure API Gateway REST APIs for InvokeWithResponseStream pointing here.
        """
        core_handler(event, context, response_stream=response_stream)
else:
    # Graceful fallback for local developer environments
    def stream_handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
        """
        Fallback when awslambda isn't installed.
        """
        logger.warning("awslambda library not found. Falling back to non-streaming response.")
        return core_handler(event, context, response_stream=None)
