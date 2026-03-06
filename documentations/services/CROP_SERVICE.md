# Crop Service Documentation

**Port:** 8093  
**Language:** Java (Spring Boot)  
**Database:** PostgreSQL  
**Key Integration:** ML Service (Python), Weather API, Soil API

## Overview

The Crop Service provides crop recommendations, crop rotation planning, and fertilizer recommendations by integrating with the Python ML Service. It acts as the bridge between the frontend and machine learning models.

## Key Responsibilities

- Crop recommendation based on soil and weather conditions
- Crop rotation planning for sustainable farming
- Fertilizer dosage recommendations
- Integration with ML models
- Weather data fetching
- Soil data integration
- MCP tools exposure for AI agents

## API Endpoints

### Crop Information (`/api/v1/crops`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get all available crops |
| GET | `/{cropId}` | Get crop details |
| GET | `/commodity/{commodityId}` | Get commodity details |

### Crop Recommendations (`/api/v1/crops/recommendations`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Get crop recommendation |
| GET | `/{recommendationId}` | Get recommendation details |
| GET | `/user/{userId}` | Get user's recommendations |

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
  "potassium": 30,
  "season": "KHARIF"
}
```

### Crop Recommendation Response
```json
{
  "recommendationId": "uuid",
  "recommendedCrop": "Rice",
  "confidence": 0.92,
  "alternativeCrops": ["Wheat", "Maize"],
  "reasoning": "Optimal conditions for rice cultivation",
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
  "rotationId": "uuid",
  "recommendedCrop": "Wheat",
  "benefits": ["Nitrogen fixation", "Pest control"],
  "soilImpact": "Improved soil health",
  "timestamp": "2024-03-06T10:30:00Z"
}
```

## Core Services

### CropRecommendationDashboardService
- Orchestrates crop recommendation workflow
- Calls ML Service for predictions
- Fetches weather and soil data
- Combines multiple data sources
- Caches recommendations

### MLServiceClient
- REST client for Python ML Service
- Calls `/api/ml/predict-crop` endpoint
- Calls `/api/ml/predict-rotation` endpoint
- Calls `/api/ml/predict-fertilizer` endpoint
- Handles timeouts and retries

### WeatherApiClient
- Fetches current weather data
- Gets weather forecasts
- Integrates with weatherapi.com
- Caches weather data

### KaegroCropSoilApiClient
- Fetches soil data by coordinates
- Gets soil composition details
- Provides soil recommendations

## MCP Tools

### CropMcpTools
Exposes crop-related functions for AI agents:
- `get_crop_recommendation` - Get crop recommendation
- `get_crop_rotation` - Get rotation suggestion
- `get_fertilizer_recommendation` - Get fertilizer dosage
- `list_crops` - List available crops

## Data Models

### Crop Entity
```
- cropId (PK)
- cropName
- cropType (CEREAL, PULSE, OILSEED, etc.)
- season (KHARIF, RABI, SUMMER)
- waterRequirement
- temperatureRange
- soilType
- description
```

### CropRecommendation Entity
```
- recommendationId (PK)
- userId (FK)
- latitude
- longitude
- recommendedCrop
- confidence
- alternativeCrops (JSON)
- reasoning
- createdAt
```

### Commodity Entity
```
- commodityId (PK)
- commodityName
- unit
- description
- season
```

## External Integrations

### ML Service (Python FastAPI)
- **Base URL**: `http://localhost:8001`
- **Endpoints**:
  - POST `/api/ml/predict-crop` - Crop recommendation
  - POST `/api/ml/predict-rotation` - Crop rotation
  - POST `/api/ml/predict-fertilizer` - Fertilizer dosage

### Weather API
- **Provider**: weatherapi.com
- **Endpoints**:
  - Current weather by coordinates
  - 7-day forecast
  - Historical weather data

### Soil Data API
- **Provider**: Kaegro API
- **Data**: Soil composition, pH, nutrients

## Configuration

### Environment Variables
```
CROP_SERVICE_PORT=8093
CROP_DB_URL=jdbc:postgresql://localhost:5432/crop_service
CROP_DB_USERNAME=postgres
CROP_DB_PASSWORD=postgres
ML_SERVICE_URL=http://localhost:8001
WEATHER_API_KEY=<api-key>
WEATHER_API_URL=https://api.weatherapi.com/v1
```

### ML Service Configuration
- **Timeout**: 30 seconds
- **Retry**: 3 attempts
- **Cache**: Redis with 1-hour TTL

## Dependencies

- Spring Boot 3.5.8
- Spring Data JPA
- Spring WebFlux (reactive)
- PostgreSQL Driver
- Redis Client
- Spring AI MCP
- RestTemplate/WebClient
- Lombok
- SpringDoc OpenAPI 2.0.4

## Integration Points

### Inbound
- **API Gateway**: Receives requests
- **Frontend**: Angular application
- **Other Services**: Service-to-service calls

### Outbound
- **ML Service**: Crop predictions
- **Weather API**: Weather data
- **Soil API**: Soil data
- **Eureka Server**: Service registration

## Error Handling

- ML Service unavailable: 503 Service Unavailable
- Invalid coordinates: 400 Bad Request
- Weather API error: 502 Bad Gateway
- Database error: 500 Internal Server Error
- Invalid crop data: 400 Bad Request

## Monitoring & Observability

- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`
- **Swagger/OpenAPI**: `/swagger-ui.html`, `/v3/api-docs`
- **Logging Level**: DEBUG for `com.farmer.cropservice`
- **Performance Metrics**: Request latency, ML Service response time

## Deployment

- **Docker**: Dockerfile available
- **Environment**: Supports dev, staging, production
- **Database Migration**: Hibernate auto-update enabled
- **Health Checks**: Spring Boot Actuator health endpoint

## Common Use Cases

1. **Get Crop Recommendation**
   - POST `/api/v1/crops/recommendations/` with soil/weather data
   - Returns recommended crop with confidence score

2. **Get Crop Rotation Suggestion**
   - POST `/api/v1/crops/recommendations/` with previous crop
   - Returns next crop for rotation

3. **Get Fertilizer Recommendation**
   - POST `/api/v1/crops/recommendations/` with crop and soil data
   - Returns N, P, K dosages

4. **List Available Crops**
   - GET `/api/v1/crops/`
   - Returns all crops with details

5. **Get Crop Details**
   - GET `/api/v1/crops/{cropId}`
   - Returns crop specifications

## Performance Considerations

- Caching of crop data and recommendations
- Async calls to external APIs using WebFlux
- Connection pooling for database
- Redis caching for frequently accessed data
- Timeout handling for external API calls

## ML Model Integration

### Crop Recommendation Model
- **Input**: Latitude, longitude, temperature, humidity, rainfall, soil pH, N, P, K
- **Output**: Recommended crop, confidence score, alternatives
- **Accuracy**: ~92% on test data

### Crop Rotation Model
- **Input**: Previous crop, soil type, pH, temperature, humidity, rainfall, season
- **Output**: Recommended next crop, benefits, soil impact
- **Purpose**: Sustainable farming practices

### Fertilizer Recommendation Model
- **Input**: Crop, soil type, pH, temperature
- **Output**: N, P, K dosages in kg/hectare
- **Purpose**: Optimal nutrient management

## Future Enhancements

- Real-time soil sensor integration
- Drone-based crop monitoring
- Pest and disease prediction
- Yield forecasting
- Climate change impact analysis
- Precision agriculture recommendations
