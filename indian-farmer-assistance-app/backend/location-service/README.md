# Location Service

The Location Service provides GPS-based services, government body locator (KVKs, district offices), and reverse geocoding for the Indian Farmer Assistance Application.

## 🚀 Overview

- **Port:** 8095
- **Technology Stack:** Java (Spring Boot), PostgreSQL
- **Primary Purpose:** Help farmers find nearby agricultural resources and provide location-aware information.

## 🛠️ Key Features

- **Government Body Locator:** Find Krishi Vigyan Kendras (KVKs), district agricultural offices, and state-level bodies.
- **Nearby Search:** Geolocation-based search to find the nearest agricultural assistance centers.
- **District/State Filtering:** Browse government bodies by state or district.
- **Contact Management:** Provides contact names, phone numbers, and emails for agricultural officers and KVK scientists.

## 📋 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/government-bodies/nearby` | Find nearby government offices (by lat/long) |
| GET | `/api/v1/government-bodies/district/{district}` | Get bodies by district |
| GET | `/api/v1/government-bodies/state/{state}` | Get bodies by state |

## 🧪 Configuration

The service requires the following environment variables:
- `LOCATION_DB_URL`: PostgreSQL connection URL

## 📚 Further Reading

For more detailed technical documentation, please refer to:
- [Scheme, Location & Yield Services Documentation](../../documentations/services/MANDI_SCHEME_LOCATION_YIELD_SERVICES.md#location-service)
