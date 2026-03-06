# AI Service Documentation

**Port:** 8001  
**Language:** Python (FastAPI)  
**Framework:** FastAPI with Uvicorn  
**Key Technologies:** scikit-learn, LangChain, AWS Bedrock, MCP

## Overview

The AI Service is a Python-based FastAPI service that provides machine learning model predictions, voice assistant capabilities, and disease detection. It serves as the ML backbone for the entire application, handling crop recommendations, crop rotation, fertilizer dosages, and AI-powered question answering.

## Key Responsibilities

- Crop recommendation predictions
- Crop rotation planning
- Fertilizer dosage recommendations
- Voice assistant (text and audio)
- Disease detection from images
- High-fidelity multilingual support (10 major Indian languages)
- **Bedrock MCP Agent**: LangGraph-based agent with multi-service tool calling
- Sourcing location data via reverse geocoding

## API Endpoints

### ML Predictions

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ml/predict-crop` | Get crop recommendation |
| POST | `/api/ml/predict-rotation` | Get crop rotation suggestion |
| POST | `/api/ml/predict-fertilizer` | Get fertilizer dosage |

### Voice Assistant

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ml/ask-question` | Ask question (text) |
| POST | `/api/ml/ask-question-audio` | Ask question (audio) |
| POST | `/api/ml/ask-question-stream` | Stream response (text) |
| POST | `/api/ml/ask-question-audio-stream` | Stream response (audio) |

### Disease Detection

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ml/disease-detect` | Detect crop disease from image |
| GET | `/api/ml/languages` | Get supported languages |

### Health & Info

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Service health check |
| GET | `/docs` | API documentation (Swagger) |

## Request/Response Models

### Crop Recommendation Request
```json
{
  "latitude": 28.7041,
  "longitude": 77.1025,
  "temperature": 25.5,
  "humidity": 65,
  "rainfall": 800,
  "soilPH": 6.8,
  "nitrogen": 45,
  "phosphorus": 20,
  "potassium": 30
}
```

### Crop Recommendation Response
```json
{
  "recommendedCrop": "Rice",
  "confidence": 0.92,
  "alternativeCrops": ["Wheat", "Maize"],
  "reasoning": "Optimal conditions for rice cultivation based on soil and weather parameters",
  "modelVersion": "1.0.0",
  "timestamp": "2024-03-06T10:30:00Z"
}
```

### Crop Rotation Request
```json
{
  "previousCrop": "Rice",
  "soilPH": 6.8,
  "soilType": "Loamy",
  "temperature": 25.5,
  "humidity": 65,
  "rainfall": 800,
  "season": "RABI"
}
```

### Crop Rotation Response
```json
{
  "recommendedCrop": "Wheat",
  "benefits": [
    "Nitrogen fixation",
    "Pest control",
    "Soil structure improvement"
  ],
  "soilImpact": "Improved soil health and fertility",
  "yieldExpectation": "High",
  "timestamp": "2024-03-06T10:30:00Z"
}
```

### Fertilizer Recommendation Request
```json
{
  "crop": "Rice",
  "soilType": "Loamy",
  "soilPH": 6.8,
  "temperature": 25.5
}
```

### Fertilizer Recommendation Response
```json
{
  "crop": "Rice",
  "nitrogen": 120,
  "phosphorus": 60,
  "potassium": 40,
  "unit": "kg/hectare",
  "applicationSchedule": [
    {
      "stage": "Basal",
      "nitrogen": 60,
      "phosphorus": 60,
      "potassium": 40
    },
    {
      "stage": "Tillering",
      "nitrogen": 40,
      "phosphorus": 0,
      "potassium": 0
    },
    {
      "stage": "Panicle Initiation",
      "nitrogen": 20,
      "phosphorus": 0,
      "potassium": 0
    }
  ],
  "timestamp": "2024-03-06T10:30:00Z"
}
```

### Voice Assistant Request (Text)
```json
{
  "question": "What is the best time to plant rice?",
  "language": "hi",
  "context": {
    "state": "Maharashtra",
    "district": "Pune",
    "crop": "Rice"
  }
}
```

### Voice Assistant Response
```json
{
  "answer": "Rice should be planted during the monsoon season...",
  "language": "hi",
  "confidence": 0.95,
  "sources": ["IMD", "ICAR"],
  "timestamp": "2024-03-06T10:30:00Z"
}
```

### Disease Detection Request
```
Content-Type: multipart/form-data
- image: Binary image file (JPG, PNG)
- crop: "Rice"
- language: "hi"
```

### Disease Detection Response
```json
{
  "disease": "Leaf Blast",
  "confidence": 0.89,
  "severity": "MODERATE",
  "description": "Fungal disease affecting rice leaves",
  "treatment": [
    "Apply fungicide spray",
    "Improve drainage",
    "Remove infected leaves"
  ],
  "preventiveMeasures": [
    "Use resistant varieties",
    "Maintain proper spacing",
    "Avoid excessive nitrogen"
  ],
  "timestamp": "2024-03-06T10:30:00Z"
}
```

## ML Models

### 1. Crop Recommendation Model

**Purpose**: Predicts the best crop based on soil and weather conditions

**Input Features**:
- Latitude, Longitude
- Temperature (°C)
- Humidity (%)
- Rainfall (mm)
- Soil pH
- Nitrogen (kg/hectare)
- Phosphorus (kg/hectare)
- Potassium (kg/hectare)

**Output**:
- Recommended crop
- Confidence score (0-1)
- Alternative crops
- Reasoning

**Model Details**:
- **Algorithm**: Random Forest Classifier
- **Training Data**: 2,200+ samples
- **Accuracy**: ~92% on test data
- **File**: `models/crop_recommendation_model.pkl`

**Training Process**:
```python
# Features: 8 (lat, lon, temp, humidity, rainfall, pH, N, P, K)
# Target: Crop name (100+ varieties)
# Cross-validation: 5-fold
# Hyperparameters: n_estimators=100, max_depth=15
```

### 2. Crop Rotation Model

**Purpose**: Recommends next crop for sustainable rotation

**Input Features**:
- Previous crop
- Soil type
- Soil pH
- Temperature
- Humidity
- Rainfall
- Season

**Output**:
- Recommended crop
- Benefits (nitrogen fixation, pest control, etc.)
- Soil impact
- Yield expectation

**Model Details**:
- **Algorithm**: Decision Tree with custom rules
- **Training Data**: 1,500+ rotation patterns
- **Accuracy**: ~88% on test data
- **File**: `models/crop_rotation_model.pkl`

### 3. Fertilizer Recommendation Model

**Purpose**: Calculates optimal N, P, K dosages

**Input Features**:
- Crop type
- Soil type
- Soil pH
- Temperature

**Output**:
- Nitrogen dosage (kg/hectare)
- Phosphorus dosage (kg/hectare)
- Potassium dosage (kg/hectare)
- Application schedule

**Model Details**:
- **Algorithm**: Linear Regression with crop-specific rules
- **Training Data**: 800+ fertilizer trials
- **Accuracy**: ~85% on test data
- **File**: `models/fertilizer_recommendation_model.pkl`

## Core Components

### CropRecommendationModel
```python
class CropRecommendationModel:
    def load(self, model_path)
    def predict(self, features) -> dict
    def get_alternatives(self, top_n=3) -> list
```

### CropRotationModel
```python
class CropRotationModel:
    def load(self, model_path)
    def predict(self, features) -> dict
    def get_benefits(self, crop) -> list
```

### FertilizerRecommendationModel
```python
class FertilizerRecommendationModel:
    def load(self, model_path)
    def predict(self, features) -> dict
    def get_schedule(self, crop) -> list
```

### AWS Voice Assistant Client
```python
class AwsVoiceAssistantClient:
    def ask_question_text(self, question, language) -> str
    def ask_question_audio(self, audio_file, language) -> str
    def ask_question_text_stream(self, question) -> Iterator[str]
    def ask_question_audio_stream(self, audio_file) -> Iterator[str]
```

**Integration**:
- Uses AWS Bedrock LLM (Llama 3 / Nova Pro)
- **LangGraph** for robust tool-use and conversation state
- **MCP (Model Context Protocol)** for dynamic integration with Spring Boot microservices
- Supports streaming responses and session-based chat history

### Disease Detection Client
```python
class DiseaseDetectionClient:
    def detect_disease(self, image_file, crop) -> dict
    def get_treatment(self, disease) -> list
    def get_preventive_measures(self, disease) -> list
```

**Integration**:
- AWS Lambda function for image processing
- Deep learning model for disease classification
- Supports 50+ crop diseases

### Crop Name Mapper
```python
class CropNameMapper:
    def map_crop_name(self, input_name, language) -> str
    def get_all_crops() -> list
    def get_crop_aliases(self, crop_name) -> list
```

**Purpose**: Maps crop names across languages and aliases

## External Integrations

### AWS Bedrock
- **Model**: Llama-3-8B / Nova Pro
- **Purpose**: LLM-based intelligent assistance & tool calling
- **Features**: Streaming, location awareness, multilingual script/voice enforcement
- **Integration**: Via LangGraph & LangChain AWS

### AWS Lambda
- **Function**: Disease detection
- **Purpose**: Image processing and classification
- **Trigger**: HTTP API call
- **Response**: Disease name, confidence, treatment

### Bhashini API
- **Purpose**: Multilingual coordination
- **Languages**: Hindi, Bengali, Telugu, Marathi, Tamil, Gujarati, Punjabi, Kannada, Malayalam, English
- **Features**: Script-correct output, localized Polly voices, automatic translation

### MCP (Model Context Protocol)
- **Purpose**: Tool integration with AI agents
- **Tools Exposed**:
  - `predict_crop` - Crop recommendation
  - `predict_rotation` - Crop rotation
  - `predict_fertilizer` - Fertilizer dosage
  - `detect_disease` - Disease detection
  - `ask_question` - Voice assistant

## Configuration

### Environment Variables
```
AI_SERVICE_PORT=8001
AI_SERVICE_HOST=0.0.0.0
LOG_LEVEL=INFO

# AWS Configuration
AWS_REGION=ap-south-1
AWS_ACCESS_KEY_ID=<access-key>
AWS_SECRET_ACCESS_KEY=<secret-key>
AWS_BEDROCK_MODEL_ID=anthropic.claude-3-sonnet-20240229-v1:0

# Bhashini Configuration
BHASHINI_API_KEY=<api-key>
BHASHINI_USER_ID=<user-id>

# Model Configuration
MODELS_DIR=./models
MODEL_CACHE_SIZE=3
```

### Model Loading
```python
# On startup, models are loaded into memory
# Lifespan context manager handles initialization
@asynccontextmanager
async def lifespan(app: FastAPI):
    # Load models
    crop_reco_model = CropRecommendationModel()
    crop_reco_model.load('./models/crop_recommendation_model.pkl')
    
    crop_rotation_model = CropRotationModel()
    crop_rotation_model.load('./models/crop_rotation_model.pkl')
    
    fertilizer_model = FertilizerRecommendationModel()
    fertilizer_model.load('./models/fertilizer_recommendation_model.pkl')
    
    yield
    # Cleanup
```

## Dependencies

```
fastapi>=0.109.0
uvicorn>=0.27.0
numpy<2.0.0
python-dotenv>=1.0.0
pydantic>=2.5.3
pydantic-settings>=2.1.0
pytest>=7.4.4
pytest-asyncio>=0.23.3
httpx>=0.26.0
scikit-learn==1.3.2
joblib>=1.3.2
pandas>=2.1.3
requests>=2.31.0
python-multipart>=0.0.6
langchain
langchain-community
langchain-core
langchain-aws
langchain-mcp-adapters
langgraph
langgraph-prebuilt
mcp
boto3
scipy==1.11.4
```

## Performance Considerations

- **Model Loading**: Loaded once at startup, cached in memory
- **Prediction Latency**: ~100-200ms per prediction
- **Concurrent Requests**: Handled by Uvicorn worker pool
- **Memory Usage**: ~500MB for all models
- **Caching**: Redis integration for prediction caching

## Error Handling

- Invalid input: 400 Bad Request
- Model not loaded: 503 Service Unavailable
- AWS service error: 502 Bad Gateway
- File upload error: 400 Bad Request
- Unsupported language: 400 Bad Request

## Monitoring & Observability

- **Health Endpoint**: `/health`
- **Logging**: Structured logging with timestamps
- **Metrics**: Request count, latency, error rate
- **Tracing**: Optional integration with AWS X-Ray

## Deployment

- **Docker**: Dockerfile available
- **Container**: Python 3.11 slim base image
- **Port**: 8001
- **Workers**: Uvicorn with multiple workers
- **Health Checks**: `/health` endpoint

## Common Use Cases

1. **Get Crop Recommendation**
   - POST `/api/ml/predict-crop` with soil/weather data
   - Returns recommended crop with confidence

2. **Ask Question in Local Language**
   - POST `/api/ml/ask-question` with question in Hindi
   - Returns answer in Hindi via AWS Bedrock

3. **Detect Crop Disease**
   - POST `/api/ml/disease-detect` with image
   - Returns disease name and treatment

4. **Get Fertilizer Dosage**
   - POST `/api/ml/predict-fertilizer` with crop and soil data
   - Returns N, P, K dosages with schedule

## Testing

```bash
# Run tests
pytest tests/

# Run with coverage
pytest --cov=app tests/

# Run specific test
pytest tests/test_models.py::test_crop_recommendation
```

## Future Enhancements

- Real-time model updates
- A/B testing framework
- Model versioning and rollback
- Advanced analytics and monitoring
- GPU acceleration for inference
- Ensemble models for better accuracy
- Federated learning for privacy
- Edge deployment support
