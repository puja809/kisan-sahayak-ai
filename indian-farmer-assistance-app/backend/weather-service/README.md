# Weather Service

The Weather Service provides real-time IMD weather forecasts, agromet advisories, and weather alerts for farmers.

## 🚀 Overview

- **Port:** 8100
- **Technology Stack:** Java (Spring Boot), PostgreSQL, Redis
- **Primary Purpose:** Provide accurate, location-based weather information to support farming decisions.

## 🛠️ Key Features

- **IMD Integration:** Real-time data fetching from India Meteorological Department (IMD) APIs.
- **Weather Forecasts:** 7-day weather predictions including temperature, rainfall, and wind speed.
- **Agromet Advisories:** Location-specific agricultural advice based on current and predicted weather.
- **Weather Alerts:** Proactive notifications for extreme weather conditions like heatwaves, heavy rain, or frost.
- **Weather Caching:** Redis-based caching to ensure high performance and reduce API load.

## 📋 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/weather/current` | Get current weather for a location |
| GET | `/api/v1/weather/forecast` | Get 7-day weather forecast |
| GET | `/api/v1/weather/advisory` | Get agromet advisory |

## ⚙️ Configuration

The service requires the following environment variables:
- `WEATHER_DB_URL`: PostgreSQL connection URL
- `WEATHER_REDIS_URL`: Redis connection URL for caching

## 📚 Further Reading

For more detailed technical documentation, please refer to:
- [Weather Service Documentation](../../documentations/services/WEATHER_SERVICE.md)
