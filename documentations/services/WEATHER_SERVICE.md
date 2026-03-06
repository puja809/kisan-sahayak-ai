# Weather Service Documentation

**Port:** 8100  
**Language:** Java (Spring Boot)  
**Database:** PostgreSQL  
**Key Integrations:** IMD API, weatherapi.com

## Overview

The Weather Service provides real-time weather forecasts, agromet advisories, and weather alerts. It integrates with India Meteorological Department (IMD) and weatherapi.com to provide accurate weather information for agricultural planning.

## Key Responsibilities

- Real-time weather forecasts
- 7-day weather predictions
- Agromet advisories for farmers
- Weather alerts and warnings
- Nowcast (short-term forecasts)
- MCP tools for AI agents

## API Endpoints

### Weather Information (`/api/v1/weather`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/current` | Get current weather |
| GET | `/forecast` | Get 7-day forecast |
| GET | `/nowcast` | Get nowcast (short-term) |
| GET | `/agromet-advisory` | Get agromet advisory |
| GET | `/alerts` | Get weather alerts |
| POST | `/subscribe-alerts` | Subscribe to alerts |

## Request/Response Models

### Current Weather Request
```json
{
  "latitude": 28.7041,
  "longitude": 77.1025,
  "unit": "metric"
}
```

### Current Weather Response
```json
{
  "weatherId": "uuid",
  "location": "Delhi",
  "latitude": 28.7041,
  "longitude": 77.1025,
  "temperature": 25.5,
  "feelsLike": 24.8,
  "humidity": 65,
  "windSpeed": 12.5,
  "windDirection": "NE",
  "pressure": 1013.25,
  "visibility": 10,
  "uvIndex": 6,
  "condition": "Partly Cloudy",
  "precipitation": 0,
  "cloudCover": 40,
  "timestamp": "2024-03-06T10:30:00Z"
}
```

### 7-Day Forecast Response
```json
{
  "forecastId": "uuid",
  "location": "Delhi",
  "latitude": 28.7041,
  "longitude": 77.1025,
  "forecasts": [
    {
      "date": "2024-03-07",
      "maxTemp": 28.5,
      "minTemp": 18.2,
      "condition": "Sunny",
      "precipitation": 0,
      "humidity": 60,
      "windSpeed": 15,
      "uvIndex": 7
    }
  ],
  "generatedAt": "2024-03-06T10:30:00Z"
}
```

### Agromet Advisory Response
```json
{
  "advisoryId": "uuid",
  "state": "Maharashtra",
  "district": "Pune",
  "crop": "Rice",
  "advisory": "Favorable conditions for transplanting",
  "recommendations": [
    "Maintain water level at 5cm",
    "Apply nitrogen fertilizer",
    "Monitor for pests"
  ],
  "riskLevel": "LOW",
  "validFrom": "2024-03-06",
  "validTo": "2024-03-13"
}
```

### Weather Alert Response
```json
{
  "alertId": "uuid",
  "state": "Maharashtra",
  "district": "Pune",
  "alertType": "HEAVY_RAIN",
  "severity": "HIGH",
  "message": "Heavy rainfall expected on March 8-9",
  "recommendations": [
    "Avoid field operations",
    "Ensure drainage",
    "Protect crops from waterlogging"
  ],
  "issuedAt": "2024-03-06T10:30:00Z",
  "validUntil": "2024-03-09T18:00:00Z"
}
```

## Core Services

### WeatherService
- Orchestrates weather data retrieval
- Combines data from multiple sources
- Generates agromet advisories
- Manages weather alerts

### ImdApiClient
- Integrates with IMD API
- Fetches official weather forecasts
- Gets agromet advisories
- Retrieves weather alerts

### WeatherApiClient
- Integrates with weatherapi.com
- Provides backup weather data
- Gets current weather conditions
- Fetches historical weather data

## MCP Tools

### WeatherMcpTools
Exposes weather-related functions for AI agents:
- `get_current_weather` - Get current weather
- `get_forecast` - Get weather forecast
- `get_agromet_advisory` - Get farming advisory
- `get_weather_alerts` - Get active alerts

## Data Models

### Weather Entity
```
- weatherId (PK)
- latitude
- longitude
- location
- temperature
- feelsLike
- humidity
- windSpeed
- windDirection
- pressure
- visibility
- uvIndex
- condition
- precipitation
- cloudCover
- timestamp
- source (IMD, weatherapi.com)
```

### Forecast Entity
```
- forecastId (PK)
- latitude
- longitude
- location
- forecastDate
- maxTemp
- minTemp
- condition
- precipitation
- humidity
- windSpeed
- uvIndex
- generatedAt
```

### AgrometAdvisory Entity
```
- advisoryId (PK)
- state
- district
- crop
- advisory
- recommendations (JSON)
- riskLevel (LOW, MEDIUM, HIGH)
- validFrom
- validTo
- createdAt
```

### WeatherAlert Entity
```
- alertId (PK)
- state
- district
- alertType (HEAVY_RAIN, FROST, HAIL, DROUGHT, etc.)
- severity (LOW, MEDIUM, HIGH, CRITICAL)
- message
- recommendations (JSON)
- issuedAt
- validUntil
- isActive
```

### AlertSubscription Entity
```
- subscriptionId (PK)
- userId (FK)
- state
- district
- alertTypes (JSON array)
- notificationMethod (EMAIL, SMS, PUSH)
- isActive
- createdAt
```

## External Integrations

### IMD API (India Meteorological Department)
- **Purpose**: Official weather forecasts and agromet advisories
- **Data**: Temperature, humidity, rainfall, wind, alerts
- **Update Frequency**: Every 6 hours
- **Coverage**: All Indian states and districts

### weatherapi.com API
- **Purpose**: Backup weather data and current conditions
- **Data**: Real-time weather, forecasts, historical data
- **Update Frequency**: Every 15 minutes
- **Coverage**: Global coverage

## Configuration

### Environment Variables
```
WEATHER_SERVICE_PORT=8100
WEATHER_DB_URL=jdbc:postgresql://localhost:5432/weather_service
WEATHER_DB_USERNAME=postgres
WEATHER_DB_PASSWORD=password
IMD_API_BASE_URL=https://imdpune.gov.in/api
IMD_API_KEY=<api-key>
WEATHER_API_KEY=<api-key>
WEATHER_API_URL=https://api.weatherapi.com/v1
WEATHER_API_TIMEOUT=30000
WEATHER_API_RETRY_ATTEMPTS=3
```

### Database Configuration
- **Driver**: PostgreSQL
- **Connection Pool**: HikariCP
- **Max Pool Size**: 10
- **Min Idle**: 5

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
- **IMD API**: Weather forecasts and advisories
- **weatherapi.com**: Current weather and backup data
- **Eureka Server**: Service registration

## Error Handling

- API timeout: 502 Bad Gateway
- Invalid coordinates: 400 Bad Request
- Weather data not available: 404 Not Found
- Database error: 500 Internal Server Error
- Invalid location: 400 Bad Request

## Monitoring & Observability

- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`
- **Swagger/OpenAPI**: `/swagger-ui.html`, `/v3/api-docs`
- **Logging Level**: DEBUG for `com.farmer.weather`
- **Performance Metrics**: API response time, data freshness

## Deployment

- **Docker**: Dockerfile available
- **Environment**: Supports dev, staging, production
- **Database Migration**: Hibernate auto-update enabled
- **Health Checks**: Spring Boot Actuator health endpoint

## Common Use Cases

1. **Get Current Weather**
   - GET `/api/v1/weather/current?latitude=28.7041&longitude=77.1025`
   - Returns current weather conditions

2. **Get 7-Day Forecast**
   - GET `/api/v1/weather/forecast?latitude=28.7041&longitude=77.1025`
   - Returns 7-day weather forecast

3. **Get Agromet Advisory**
   - GET `/api/v1/weather/agromet-advisory?state=Maharashtra&district=Pune&crop=Rice`
   - Returns farming advisory

4. **Get Weather Alerts**
   - GET `/api/v1/weather/alerts?state=Maharashtra&district=Pune`
   - Returns active weather alerts

5. **Subscribe to Alerts**
   - POST `/api/v1/weather/subscribe-alerts` with preferences
   - Subscribes user to weather alerts

## Performance Considerations

- Caching of weather data (15-minute TTL)
- Async API calls using WebFlux
- Connection pooling for database
- Redis caching for frequently accessed data
- Pagination for large result sets

## Data Refresh Strategy

- **Current Weather**: Updated every 15 minutes
- **Forecasts**: Updated every 6 hours
- **Agromet Advisories**: Updated daily
- **Weather Alerts**: Updated in real-time

## Alert Types

- **HEAVY_RAIN**: Rainfall > 100mm in 24 hours
- **FROST**: Temperature < 0°C
- **HAIL**: Hail storm warning
- **DROUGHT**: Prolonged dry period
- **HEAT_WAVE**: Temperature > 40°C
- **COLD_WAVE**: Temperature < 5°C
- **WIND_STORM**: Wind speed > 50 km/h
- **THUNDERSTORM**: Lightning and heavy rain

## Future Enhancements

- Real-time weather updates via WebSocket
- Hyperlocal weather forecasts
- Satellite imagery integration
- Pest and disease prediction based on weather
- Irrigation scheduling recommendations
- Crop-specific weather advisories
- Mobile app push notifications
- Weather-based insurance recommendations
