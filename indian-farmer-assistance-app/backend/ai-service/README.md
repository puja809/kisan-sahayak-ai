# AI Service - Crop Prediction Models

This service provides ML-based crop recommendation and rotation predictions using trained Random Forest models.

## Structure

```
ai-service/
├── app/
│   ├── crop_recommendation_model.py    # Crop recommendation model class
│   ├── crop_rotation_model.py          # Crop rotation model class
│   ├── ml_service.py                   # FastAPI service
│   ├── train_models.py                 # Training script
│   ├── test_models.py                  # Testing script
│   ├── __init__.py
│   └── models/
│       ├── crop_recommendation_model.pkl
│       └── crop_rotation_model.pkl
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

### Crop Rotation
```
POST /api/ml/predict-rotation
Content-Type: application/json

{
  "previousCrop": "Wheat",
  "soilPH": 7.2,
  "soilType": "loamy",
  "temperature": 25.3,
  "humidity": 82.58,
  "rainfall": 118.95,
  "season": "Kharif"
}
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
