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
S3_BUCKET = "krishi-sahayak-docs"
VOICE_FOLDER = "kisan-voice-temp/"
TABLE_NAME = "KisanChatMemory"

# ===============================
# AWS CLIENTS
# ===============================
bedrock = boto3.client("bedrock-agent-runtime")
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
        "body": json.dumps(body)
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
    for msg in messages[-5:]:  # last 5 exchanges
        role = msg["role"]
        content = msg["content"]
        context += f"{role}: {content}\n"
    return context


# ===============================
# MAIN HANDLER
# ===============================
def lambda_handler(event, context):
    try:
        body = json.loads(event.get("body", "{}"))
        session_id = body.get("session_id", "default-session")

        # Load previous memory
        chat_history = get_chat_history(session_id)

        # ======================
        # TEXT MODE
        # ======================
        if "question" in body:
            user_input = body["question"]

            answer_text = process_question_with_memory(user_input, chat_history)

            # Update memory
            chat_history.append({"role": "user", "content": user_input})
            chat_history.append({"role": "assistant", "content": answer_text})
            save_chat_history(session_id, chat_history)

            return success_response({
                "text": answer_text
            })

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
            IdentifyLanguage=True,
            LanguageOptions=["hi-IN", "kn-IN", "bn-IN", "en-IN"]
        )

        while True:
            status = transcribe.get_transcription_job(TranscriptionJobName=job_name)
            state = status["TranscriptionJob"]["TranscriptionJobStatus"]

            if state in ["COMPLETED", "FAILED"]:
                break

            time.sleep(2)

        if state == "FAILED":
            return success_response({"error": "Transcription failed"})

        transcription_data = status["TranscriptionJob"]

        detected_language = transcription_data.get("LanguageCode") or "en-IN"

        transcript_uri = transcription_data["Transcript"]["TranscriptFileUri"]

        transcript_json = json.loads(
            urllib.request.urlopen(transcript_uri).read()
        )

        transcripts = transcript_json.get("results", {}).get("transcripts", [])

        if not transcripts:
            return success_response({"error": "No transcript found"})

        transcript_text = transcripts[0].get("transcript", "")

        answer_text = process_question_with_memory(transcript_text, chat_history)

        # Update memory
        chat_history.append({"role": "user", "content": transcript_text})
        chat_history.append({"role": "assistant", "content": answer_text})
        save_chat_history(session_id, chat_history)

        # Voice mapping
        lang = detected_language.lower()

        if lang.startswith("hi"):
            polly_voice = "Aditi"
        elif lang.startswith("kn"):
            polly_voice = "Sravani"
        elif lang.startswith("bn"):
            polly_voice = "Raveena"
        else:
            polly_voice = "Aditi"

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
            "language": detected_language
        })

    except Exception as e:
        return success_response({"error": str(e)})


# ===============================
# BEDROCK + MEMORY
# ===============================
def process_question_with_memory(question, chat_history):

    memory_context = build_memory_context(chat_history)

    prompt = (
        "You are an agricultural assistant helping Indian farmers.\n"
        "Respond clearly and practically.\n"
        "Respond strictly in the SAME language as the user's question.\n\n"
        f"Previous conversation:\n{memory_context}\n"
        f"Current question:\n{question}"
    )

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
