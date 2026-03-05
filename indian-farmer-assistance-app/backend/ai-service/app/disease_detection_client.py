"""Disease detection client for AWS Lambda API integration"""
import os
import re
import json
import logging
import base64
import requests

logger = logging.getLogger(__name__)

AWS_API_DETECT_DISEASE_ENDPOINT = os.getenv("AWS_API_DETECT_DISEASE_ENDPOINT", "https://5m2acu2lea.execute-api.us-east-1.amazonaws.com/prod/detect-disease")


def _parse_disease_analysis(text: str) -> dict:
    """
    Parse the disease_analysis string returned by the Lambda.

    The model may return either:
      1. A JSON string with keys: crop, disease, symptoms, treatment, prevention
      2. Markdown free-text with bold section headers like **Crop Name**: ...

    Tries JSON first, falls back to regex line-by-line parsing.
    """
    result = {
        "crop": "",
        "disease": "",
        "symptoms": "",
        "treatment": "",
        "prevention": "",
    }

    text = text.strip()

    # ── Strategy 1: JSON string ──────────────────────────────────────────────
    # Strip surrounding markdown code fences if present (```json ... ```)
    json_text = re.sub(r"^```(?:json)?\s*", "", text, flags=re.IGNORECASE)
    json_text = re.sub(r"\s*```$", "", json_text).strip()
    try:
        parsed = json.loads(json_text)
        if isinstance(parsed, dict):
            result["crop"]       = str(parsed.get("crop",       "") or "")
            result["disease"]    = str(parsed.get("disease",    "") or "")
            result["symptoms"]   = str(parsed.get("symptoms",   "") or "")
            result["treatment"]  = str(parsed.get("treatment",  "") or "")
            result["prevention"] = str(parsed.get("prevention", "") or "")
            return result
    except (json.JSONDecodeError, ValueError):
        pass  # not JSON — fall through to regex

    # ── Strategy 2: Markdown / free-text regex ───────────────────────────────
    # Matches patterns like:
    #   - **Crop Name**: Maize          (inline value)
    #   **Symptoms**:                   (standalone section header)
    #   Crop Name: Maize                (plain label: value)
    inline_patterns = {
        "crop":       re.compile(r"\*{0,2}crop[\s_]*name\*{0,2}\s*[:\-–]\s*(.+)", re.IGNORECASE),
        "disease":    re.compile(r"\*{0,2}disease[\s_]*name\*{0,2}\s*[:\-–]\s*(.+)", re.IGNORECASE),
        "symptoms":   re.compile(r"\*{0,2}symptoms?\*{0,2}\s*[:\-–]\s*(.*)",        re.IGNORECASE),
        "treatment":  re.compile(r"\*{0,2}treatment\*{0,2}\s*[:\-–]\s*(.*)",         re.IGNORECASE),
        "prevention": re.compile(r"\*{0,2}prevention\*{0,2}\s*[:\-–]\s*(.*)",        re.IGNORECASE),
    }

    lines = text.splitlines()
    current_field = None
    buffer: list[str] = []

    def flush():
        if current_field and buffer:
            result[current_field] = "\n".join(buffer).strip()

    for raw_line in lines:
        line = raw_line.strip()
        if not line:
            if current_field:
                buffer.append("")  # preserve blank lines within a section
            continue

        matched = False
        for field, pat in inline_patterns.items():
            m = pat.match(line)
            if m:
                flush()
                buffer = []
                current_field = field
                inline_val = m.group(1).strip().lstrip("*").strip()
                if inline_val:
                    buffer.append(inline_val)
                matched = True
                break

        if not matched and current_field:
            # strip leading list markers and trailing bold markers
            clean = re.sub(r"^[-*•]\s*", "", line)
            clean = re.sub(r"\*{1,2}([^*]+)\*{1,2}", r"\1", clean)  # un-bold
            if clean:
                buffer.append(clean)

    flush()

    # Fallback: dump everything into symptoms if nothing was parsed
    if not any(result.values()):
        result["symptoms"] = text

    return result



def detect_disease(image_bytes: bytes, language: str = "en", session_id: str = "default-session") -> dict:
    """
    Detect crop disease from image using the AWS Lambda API.

    Args:
        image_bytes:  Raw image bytes
        language:     Language code for response (e.g. "en", "hi", "bn").
                      The Lambda reads this and instructs the LLM to respond in that language.
        session_id:   Session ID for chat memory in the Lambda.

    Returns:
        dict with keys: crop, disease, symptoms, treatment, prevention, confidence
    """
    if not image_bytes or len(image_bytes) == 0:
        raise ValueError("Image data is required")

    if not AWS_API_DETECT_DISEASE_ENDPOINT:
        raise ValueError("AWS_API_DETECT_DISEASE_ENDPOINT not configured")

    logger.info(f"Calling AWS disease detection API (language={language}): {AWS_API_DETECT_DISEASE_ENDPOINT}")

    # Convert image to base64
    base64_image = base64.b64encode(image_bytes).decode("utf-8")

    # Build payload — Lambda accepts image + language + session_id
    payload = {
        "image": base64_image,
        "language": language,
        "session_id": session_id,
    }

    response = requests.post(
        AWS_API_DETECT_DISEASE_ENDPOINT,
        json=payload,
        timeout=60,
    )

    if response.status_code != 200:
        logger.error(f"AWS API error: {response.status_code} - {response.text}")
        raise RuntimeError(f"Failed to detect disease: {response.status_code}")

    result = response.json()

    # Lambda wraps the actual body as a JSON string inside "body" when invoked via API Gateway
    if isinstance(result.get("body"), str):
        import json as _json
        result = _json.loads(result["body"])

    # Check for error
    if "error" in result:
        logger.error(f"AWS API returned error: {result['error']}")
        raise RuntimeError(f"AWS API error: {result['error']}")

    # Lambda returns: { "disease_analysis": "<full text>" }
    disease_analysis_text = result.get("disease_analysis", "")
    logger.info(f"Raw disease_analysis from Lambda:\n{disease_analysis_text[:300]}")

    # Parse the free-text response into structured fields
    parsed = _parse_disease_analysis(disease_analysis_text)

    return {
        "crop":        parsed["crop"] or "Unknown",
        "disease":     parsed["disease"] or "Unknown",
        "symptoms":    parsed["symptoms"],
        "treatment":   parsed["treatment"],
        "prevention":  parsed["prevention"],
        # Lambda model doesn't provide a numeric confidence score
        "confidence":  result.get("confidence", 0.0),
        # Pass the raw full analysis text too so UI can display it as fallback
        "raw_analysis": disease_analysis_text,
    }
