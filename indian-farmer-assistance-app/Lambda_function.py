# Original Code of Puja

import json
import boto3
import base64
import uuid
import time
import urllib.request

# ===============================
# CONFIG
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
# AWS CLIENTS
# ===============================
bedrock = boto3.client("bedrock-agent-runtime")  # RAG
bedrock_runtime = boto3.client("bedrock-runtime")  # Vision

transcribe = boto3.client("transcribe")
polly = boto3.client("polly")
s3 = boto3.client("s3")
dynamodb = boto3.resource("dynamodb")
table = dynamodb.Table(TABLE_NAME)

# ===============================
# RESPONSE FORMAT
# ===============================
def success_response(body):
    return {
        "statusCode": 200,
        "headers": {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Headers": "Content-Type",
            "Access-Control-Allow-Methods": "OPTIONS,POST"
        },
        "body": json.dumps(body, ensure_ascii=False)
    }

# ===============================
# MEMORY FUNCTIONS
# ===============================
def get_chat_history(session_id):
    response = table.get_item(Key={"session_id": session_id})
    return response.get("Item", {}).get("messages", [])

def save_chat_history(session_id, messages):
    table.put_item(Item={
        "session_id": session_id,
        "messages": messages
    })

def build_memory_context(messages):
    context = ""
    for msg in messages[-5:]:
        context += f"{msg['role']}: {msg['content']}\n"
    return context

# ===============================
# LANGUAGE ROUTING
# ===============================
def get_transcribe_language(lang_code):
    mapping = {
        "Hindi": "hi-IN", "hi": "hi-IN",
        "Bengali": "bn-IN", "bn": "bn-IN",
        "Telugu": "te-IN", "te": "te-IN",
        "Marathi": "mr-IN", "mr": "mr-IN",
        "Tamil": "ta-IN", "ta": "ta-IN",
        "Gujarati": "gu-IN", "gu": "gu-IN",
        "Punjabi": "pa-IN", "pa": "pa-IN",
        "Kannada": "kn-IN", "ka": "kn-IN",
        "Malayalam": "ml-IN", "ml": "ml-IN",
        "English": "en-IN", "en": "en-IN"
    }
    return mapping.get(lang_code, "en-IN")

def get_polly_voice(lang_code):
    mapping = {
        "Hindi": "Aditi", "hi": "Aditi",
        "Bengali": "Kajal", "bn": "Kajal",
        "Telugu": "Shruti", "te": "Shruti",
        "Marathi": "Arpita", "mr": "Arpita",
        "Tamil": "Priyadarshani", "ta": "Priyadarshani",
        "Gujarati": "Dhvani", "gu": "Dhvani",
        # Fallbacks for languages not natively supported by Polly yet
        "Punjabi": "Aditi", "pa": "Aditi",
        "Kannada": "Aditi", "ka": "Aditi",
        "Malayalam": "Aditi", "ml": "Aditi",
        "English": "En-GB-Sonia", "en": "En-GB-Sonia"
    }
    return mapping.get(lang_code, "Aditi")

# ===============================
# MAIN HANDLER
# ===============================
def lambda_handler(event, context):
    try:
        body = json.loads(event.get("body", "{}"))
        session_id = body.get("session_id", "default-session")
        user_language = body.get("language", "en")

        chat_history = get_chat_history(session_id)

        # ======================
        # 🖼 IMAGE DISEASE MODE (NEW)
        # ======================
        if "image" in body:
            return handle_disease_detection(body, session_id, user_language)

        # ======================
        # TEXT MODE
        # ======================
        if "question" in body:
            user_input = body["question"]

            answer_text = process_question_with_memory(
                user_input,
                chat_history,
                user_language
            )

            chat_history.append({"role": "user", "content": user_input})
            chat_history.append({"role": "assistant", "content": answer_text})
            save_chat_history(session_id, chat_history)

            return success_response({"text": answer_text})

        # ======================
        # VOICE MODE
        # ======================
        if "audio" not in body:
            return success_response({"error": "No audio provided"})

        audio_bytes = base64.b64decode(body["audio"])
        audio_key = VOICE_FOLDER + str(uuid.uuid4()) + ".wav"

        s3.put_object(Bucket=S3_BUCKET, Key=audio_key, Body=audio_bytes)

        job_name = "job-" + str(uuid.uuid4())

        transcribe.start_transcription_job(
            TranscriptionJobName=job_name,
            Media={"MediaFileUri": f"s3://{S3_BUCKET}/{audio_key}"},
            MediaFormat="wav",
            LanguageCode=get_transcribe_language(user_language)
        )

        while True:
            status = transcribe.get_transcription_job(
                TranscriptionJobName=job_name
            )
            state = status["TranscriptionJob"]["TranscriptionJobStatus"]

            if state in ["COMPLETED", "FAILED"]:
                break

            time.sleep(2)

        if state == "FAILED":
            return success_response({"error": "Transcription failed"})

        transcript_uri = status["TranscriptionJob"]["Transcript"]["TranscriptFileUri"]

        transcript_json = json.loads(
            urllib.request.urlopen(transcript_uri).read()
        )

        transcript_text = transcript_json["results"]["transcripts"][0]["transcript"]

        answer_text = process_question_with_memory(
            transcript_text,
            chat_history,
            user_language
        )

        chat_history.append({"role": "user", "content": transcript_text})
        chat_history.append({"role": "assistant", "content": answer_text})
        save_chat_history(session_id, chat_history)

        polly_voice = get_polly_voice(user_language)

        polly_response = polly.synthesize_speech(
            Text=answer_text,
            OutputFormat="mp3",
            VoiceId=polly_voice
        )

        audio_stream = polly_response["AudioStream"].read()
        audio_base64_response = base64.b64encode(audio_stream).decode("utf-8")

        return success_response({
            "text": answer_text,
            "audio": audio_base64_response,
            "language": user_language
        })

    except Exception as e:
        print("Error:", str(e))
        return success_response({"error": str(e)})

# ===============================
# 🖼 IMAGE DISEASE FUNCTION
# ===============================
def handle_disease_detection(body, session_id, user_language):

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
    # ✅ CALL LLAMA 4 SCOUT VISION
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
Return strictly in the following JSON format. Ensure the JSON keys are exactly as shown (in English), but the values for "crop", "disease", "symptoms", "treatment", and "prevention" should be written in {user_language}.

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
    # ✅ EXTRACT RESPONSE CORRECTLY
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
# BEDROCK + MEMORY (UNCHANGED)
# ===============================
def process_question_with_memory(question, chat_history, user_language):

    memory_context = build_memory_context(chat_history)

    prompt = f"""
You are "Kisan Sahayak", an AI assistant helping Indian farmers.

IMPORTANT RULES:
1. Answer ONLY agriculture-related questions.
2. Use the provided context from Knowledge Base to answer.
3. If the answer is NOT found in the context, say:
   "I am sorry, I do not have information about this in my agricultural knowledge base."
4. Do NOT guess or hallucinate.
5. Do NOT answer unrelated topics.

LANGUAGE RULES:
- The user has requested the response in: {user_language}.
- ALWAYS respond in the `{user_language}` language.
- Use the appropriate script (e.g., Devanagari for Hindi, Bengali script for Bengali, etc.).
- If `{user_language}` is 'English', respond in English.
- If `{user_language}` is 'Auto' or not clear, respond in the same language as the USER QUESTION.

CONTEXT FROM KNOWLEDGE BASE:
{memory_context}

USER QUESTION:
{question}

Answer clearly, practically, and in a farmer-friendly tone in {user_language}.
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