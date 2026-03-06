# Crop Service

The Crop Service provides AI-powered crop recommendations, crop rotation planning, and yield prediction for farmers.

## 🚀 Overview

- **Port:** 8093
- **Technology Stack:** Java (Spring Boot), PostgreSQL, Redis
- **Primary Purpose:** Provide intelligent crop suggestions and planning tools based on location, soil, and climate data.

## 🛠️ Key Features

- **Crop Recommendations:** Suggests the best crops for a given land based on NPK values, temperature, humidity, pH, and rainfall.
- **Crop Rotation Planning:** Optimizes nutrient cycling by suggesting subsequent crops based on the previous harvest and soil health.
- **Yield Prediction:** Estimates expected crop yield using ML models integration.
- **Fertilizer Suggestions:** Recommends appropriate fertilizers based on soil deficiency and crop needs.

## 📋 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/crops/recommend` | Get crop recommendations |
| POST | `/api/v1/crops/rotation` | Plan crop rotation |
| GET | `/api/v1/crops/yield/historical` | Get historical yield data |

## ⚙️ Configuration

The service requires the following environment variables:
- `CROP_DB_URL`: PostgreSQL connection URL
- `CROP_REDIS_URL`: Redis connection URL for caching metadata
- `AI_SERVICE_URL`: URL to the Python AI service for ML predictions

## 📚 Further Reading

For more detailed technical documentation, please refer to:
- [Crop Service Documentation](../../documentations/services/CROP_SERVICE.md)
