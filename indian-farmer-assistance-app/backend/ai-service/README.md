# AI Service - Crop Prediction & Smart Assistant

This service provides ML-based crop recommendations, rotation predictions, and a **Bedrock MCP Agent** that integrates with all microservices to provide intelligent agricultural assistance.

## Structure

```
ai-service/
├── app/
│   ├── crop_recommendation_model.py    # Crop recommendation model class
│   ├── crop_rotation_model.py          # Crop rotation model class
│   ├── fertilizer_recommendation_model.py # Fertilizer model class
│   ├── ml_service.py                   # FastAPI service (main entry)
│   ├── bedrock_mcp_agent.py            # Bedrock LangGraph MCP Agent
│   ├── aws_voice_assistant_client.py   # Client for Lambda Voice API
│   ├── disease_detection_client.py     # Client for Lambda Disease API
│   ├── models/                         # Serialized ML models
│       ├── crop_recommendation_model.pkl
│       ├── crop_rotation_model.pkl
│       └── fertilizer_recommendation_model.pkl
├── tests/
├── requirements.txt
├── Dockerfile.ml
├── docker-compose.ml.yml
└── ML_SETUP.md
```

## Quick Start

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Train Models
```bash
cd app
python train_models.py
```

### 3. Start ML Service
```bash
python ml_service.py
```

Service runs on `http://localhost:8001`

### 4. Test Models
```bash
python test_models.py
```

## API Endpoints

### Health Check
```
GET /health
```

### Crop Recommendation
```
POST /api/ml/predict-crop
Content-Type: application/json

{
  "latitude": 12.97,
  "longitude": 77.59,
  "temperature": 26.5,
  "humidity": 78,
  "rainfall": 210,
  "soilPH": 6.5,
  "nitrogen": 100,
  "phosphorus": 50,
  "potassium": 50
}
```

### Smart Assistant (Bedrock MCP)
```
POST /api/ml/ask-question
Content-Type: application/json

{
  "question": "What is the current price of Wheat in Bhopal?",
  "language": "Hindi",
  "latitude": 23.2599,
  "longitude": 77.4126
}
```

### Disease Detection (Proxy to Lambda)
```
POST /api/ml/disease-detect?language=Hindi&session_id=123
Content-Type: multipart/form-data

image: [binary_data]
```

### Supported Languages
```
GET /api/ml/languages
```

## Models

### Crop Recommendation Model
- **Algorithm:** Random Forest (100 trees, max_depth=15)
- **Features:** N, P, K, temperature, humidity, pH, rainfall
- **Output:** Crop type with confidence score

### Crop Rotation Model
- **Algorithm:** Random Forest (100 trees, max_depth=15)
- **Features:** soil_pH, temperature, humidity, rainfall, previous_crop, soil_type, season
- **Output:** Recommended next crop with confidence score

## Docker

### Build Image
```bash
docker build -f Dockerfile.ml -t farmer-ml-service:latest .
```

### Run with Docker Compose
```bash
docker-compose -f docker-compose.ml.yml up -d
```

## Configuration

Set environment variables:
```bash
export PYTHONUNBUFFERED=1
```

## Documentation

See `ML_SETUP.md` for detailed setup and configuration instructions.
