# Scheme Service

The Scheme Service provides information about government agricultural schemes, subsidies, and eligibility criteria to help farmers discover available support programs.

## 🚀 Overview

- **Port:** 8097
- **Technology Stack:** Java (Spring Boot), PostgreSQL
- **Primary Purpose:** Facilitate access to government schemes and assess farmer eligibility.

## 🛠️ Key Features

- **Scheme Discovery:** Browse national and state-level agricultural schemes.
- **Eligibility Assessment:** Automated checking to determine if a farmer qualifies for a specific scheme based on land holding, income, and crop type.
- **Search & Filtering:** Search for schemes by state, ministry, and commodity.
- **Benefit Analysis:** Clear description of subsidies and financial benefits offered by various programs.

## 📋 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/schemes` | List all government schemes |
| POST | `/api/v1/schemes/search` | Search schemes by criteria |
| POST | `/api/v1/schemes/check-eligibility` | Verify eligibility for schemes |

## ⚙️ Configuration

The service requires the following environment variables:
- `SCHEME_DB_URL`: PostgreSQL connection URL

## 📚 Further Reading

For more detailed technical documentation, please refer to:
- [Scheme, Location & Yield Services Documentation](../../documentations/services/MANDI_SCHEME_LOCATION_YIELD_SERVICES.md#scheme-service)
