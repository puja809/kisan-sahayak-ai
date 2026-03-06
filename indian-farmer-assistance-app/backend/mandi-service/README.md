# Mandi Service

The Mandi Service provides real-time AGMARKNET commodity prices, market trends, and information about fertilizer suppliers.

## 🚀 Overview

- **Port:** 8096
- **Technology Stack:** Java (Spring Boot), PostgreSQL
- **Primary Purpose:** Empower farmers with market intelligence to get better prices for their produce.

## 🛠️ Key Features

- **Real-time Market Data:** Integration with AGMARKNET API to provide current prices for commodities across various mandis in India.
- **Price Trends:** Historical data and trends analysis to help farmers decide when to sell.
- **Fertilizer Suppliers:** Locator for nearby fertilizer and seed suppliers.
- **Commodity Search:** Filter market data by state, district, and commodity.

## 📋 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/mandi/prices` | Get current commodity prices |
| GET | `/api/v1/mandi/prices/search` | Search prices by district/commodity |
| GET | `/api/v1/mandi/suppliers` | Find nearby fertilizer suppliers |

## ⚙️ Configuration

The service requires the following environment variables:
- `MANDI_DB_URL`: PostgreSQL connection URL

## 📚 Further Reading

For more detailed technical documentation, please refer to:
- [Mandi Service Documentation](../../documentations/services/MANDI_SERVICE.md)
