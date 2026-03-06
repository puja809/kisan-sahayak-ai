# Yield Service

The Yield Service provides yield predictions, revenue calculations, and profit estimation for farmers based on crop, soil, and weather conditions.

## 🚀 Overview

- **Port:** 8094
- **Technology Stack:** Java (Spring Boot), PostgreSQL
- **Primary Purpose:** Help farmers estimate expected production and income to make better financial decisions.

## 🛠️ Key Features

- **Yield Calculation:** Predicts expected crop yield (e.g., in Tons per hectare) based on soil pH, nutrients, weather, and irrigation methods.
- **Revenue Estimation:** Calculates gross revenue based on estimated yield and current market prices.
- **Profit Analysis:** Estimates net profit by subtracting production and labor costs from the gross revenue.
- **Historical Data:** Access to historical yield data for various commodities.

## 📋 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/crops/yield/calculate` | Predict crop yield |
| POST | `/api/v1/crops/yield/revenue` | Calculate estimated revenue/profit |
| GET | `/api/v1/crops/yield/commodities` | List supported commodities |

## ⚙️ Configuration

The service requires the following environment variables:
- `YIELD_DB_URL`: PostgreSQL connection URL

## 📚 Further Reading

For more detailed technical documentation, please refer to:
- [Scheme, Location & Yield Services Documentation](../../documentations/services/MANDI_SCHEME_LOCATION_YIELD_SERVICES.md#yield-service)
