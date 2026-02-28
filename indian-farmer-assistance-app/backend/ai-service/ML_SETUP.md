# ML Crop Prediction Setup Guide

This guide explains how to set up and use the ML-based crop recommendation and rotation models.

## Overview

The system includes two trained ML models:
1. **Crop Recommendation Model** - Predicts suitable crops based on soil and weather parameters
2. **Crop Rotation Model** - Recommends next crop for rotation based on previous crop and conditions

## Prerequisites

- Python 3.8+
- pip or conda
- The datasets:
  - `documents/crop_reco_weatherapi_kaegro.csv`
  - `documents/enhanced_crop_rotation_dataset.csv`

## Installation

### 1. Install Dependencies

```bash
cd indian-farmer-assistance-app/backend/ai-service
pip install -r requirements.txt
```

### 2. Train Models

Run the training script to generate the model files:

```bash
cd app
python train_models.py
```

This will create:
- `app/models/crop_recommendation_model.pkl`
- `app/models/crop_rotation_model.pkl`

## Running the ML Service

### Start the FastAPI Server

```bash
cd app
python ml_service.py
```

The service will start on `http://localhost:8001`

### Health Check

```bash
curl http://localhost:8001/health
```

Expected response:
```json
{
  "status": "healthy",
  "crop_reco_model": true,
  "crop_rotation_model": true
}
```

## API Endpoints

### 1. Crop Recommendation

**Endpoint:** `POST /api/ml/predict-crop`

**Request:**
```json
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

**Response:**
```json
{
  "prediction": "Rice",
  "confidence": 0.92,
  "probabilities": {
    "Rice": 0.92,
    "Wheat": 0.05,
    "Maize": 0.03
  },
  "modelVersion": "1.0.0"
}
```

### 2. Crop Rotation Recommendation

**Endpoint:** `POST /api/ml/predict-rotation`

**Request:**
```json
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

**Response:**
```json
{
  "prediction": "Cotton",
  "confidence": 0.88,
  "probabilities": {
    "Cotton": 0.88,
    "Maize": 0.08,
    "Gram": 0.04
  },
  "modelVersion": "1.0.0"
}
```

## Integration with Crop Service

The Java crop-service calls the ML service through `MLModelClient`:

### Configuration

Add to `crop-service/src/main/resources/application.properties`:

```properties
ml.service.url=http://localhost:8001
weatherapi.api-key=YOUR_WEATHERAPI_KEY
```

### Usage in Java

```java
// Inject the service
@Autowired
private MLCropPredictionService mlCropPredictionService;

// Get crop recommendation
MLPredictionResponseDto response = mlCropPredictionService.recommendCropByLocation(
    latitude, longitude, nitrogen, phosphorus, potassium
);

// Get crop rotation recommendation
MLPredictionResponseDto rotationResponse = mlCropPredictionService.recommendCropRotation(
    previousCrop, latitude, longitude, season
);
```

## Frontend Integration

The Angular frontend uses `MLCropPredictionService`:

```typescript
// Inject the service
constructor(private mlPredictionService: MLCropPredictionService) {}

// Get crop recommendation
this.mlPredictionService.recommendCrop({
  latitude: 12.97,
  longitude: 77.59,
  nitrogen: 100,
  phosphorus: 50,
  potassium: 50
}).subscribe(prediction => {
  console.log('Recommended crop:', prediction.prediction);
  console.log('Confidence:', prediction.confidence);
});

// Get crop rotation
this.mlPredictionService.recommendCropRotation({
  previousCrop: 'Wheat',
  latitude: 12.97,
  longitude: 77.59,
  season: 'Kharif'
}).subscribe(prediction => {
  console.log('Next crop:', prediction.prediction);
});
```

## Data Flow

```
Frontend (Angular)
    ↓
Crop Service (Java)
    ↓
ML Service (FastAPI)
    ↓
Trained Models (scikit-learn)
    ↓
Predictions
```

## Model Details

### Crop Recommendation Model
- **Algorithm:** Random Forest Classifier
- **Features:** N, P, K, temperature, humidity, pH, rainfall
- **Target:** Crop type
- **Training Data:** crop_reco_weatherapi_kaegro.csv

### Crop Rotation Model
- **Algorithm:** Random Forest Classifier
- **Features:** soil_pH, temperature, humidity, rainfall, previous_crop, soil_type, season
- **Target:** Recommended next crop
- **Training Data:** enhanced_crop_rotation_dataset.csv

## Troubleshooting

### Models not loading
- Ensure `train_models.py` has been run
- Check that model files exist in `app/models/`
- Verify file permissions

### API connection errors
- Ensure ML service is running on port 8001
- Check firewall settings
- Verify `ml.service.url` configuration in crop-service

### Low prediction confidence
- Models may need retraining with more data
- Check input data validity
- Ensure weather/soil data is accurate

## Retraining Models

To retrain with updated datasets:

```bash
cd app
python train_models.py
```

The new models will overwrite existing ones. Restart the ML service to load them.

## Performance Notes

- Model loading: ~2-3 seconds on startup
- Prediction latency: ~50-100ms per request
- Memory usage: ~200MB for both models loaded

## Future Improvements

- Add model versioning
- Implement model monitoring and metrics
- Add confidence thresholds
- Support for ensemble predictions
- A/B testing framework
