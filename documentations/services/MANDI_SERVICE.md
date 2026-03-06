# Mandi Service Documentation

**Port:** 8096  
**Language:** Java (Spring Boot)  
**Database:** PostgreSQL  
**Key Integrations:** AGMARKNET API, data.gov.in API, Weather API

## Overview

The Mandi Service provides agricultural market data, commodity prices, market trends, and fertilizer supplier information. It integrates with government APIs to provide real-time market information to farmers.

## Key Responsibilities

- Agricultural market price data
- Commodity information and varieties
- Market location and mandi details
- Fertilizer supplier locator
- State and district population data
- Price alerts and notifications
- MCP tools for AI agents

## API Endpoints

### Market Filtering (`/api/v1/mandi/filter`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/states` | Get all states |
| GET | `/districts/{stateId}` | Get districts by state |
| GET | `/mandis/{districtId}` | Get mandis by district |
| GET | `/commodities` | Get all commodities |
| GET | `/varieties/{commodityId}` | Get varieties by commodity |
| GET | `/grades/{varietyId}` | Get grades by variety |
| POST | `/search` | Search market data with filters |

### State and District Data (`/api/v1/mandi/states-districts`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/states` | Get all states with population |
| GET | `/districts/{stateId}` | Get districts with population |
| GET | `/population-stats` | Get population statistics |

### Fertilizer Suppliers (`/api/v1/fertilizer-suppliers`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get all fertilizer suppliers |
| GET | `/{supplierId}` | Get supplier details |
| POST | `/search` | Search suppliers by location |
| GET | `/nearby` | Get nearby suppliers (geolocation) |

## Request/Response Models

### Market Search Request
```json
{
  "state": "Maharashtra",
  "district": "Pune",
  "commodity": "Rice",
  "variety": "Basmati",
  "grade": "A1",
  "dateRange": {
    "from": "2024-03-01",
    "to": "2024-03-06"
  }
}
```

### Market Price Response
```json
{
  "priceId": "uuid",
  "mandi": "Pune Mandi",
  "state": "Maharashtra",
  "district": "Pune",
  "commodity": "Rice",
  "variety": "Basmati",
  "grade": "A1",
  "minPrice": 2500,
  "maxPrice": 2800,
  "modalPrice": 2650,
  "unit": "Quintal",
  "date": "2024-03-06",
  "volume": 1500
}
```

### Fertilizer Supplier Response
```json
{
  "supplierId": "uuid",
  "name": "ABC Fertilizer Store",
  "address": "123 Market Street",
  "state": "Maharashtra",
  "district": "Pune",
  "latitude": 18.5204,
  "longitude": 73.8567,
  "phoneNumber": "+91-9876543210",
  "email": "contact@abcfertilizer.com",
  "products": ["Urea", "DAP", "NPK"],
  "rating": 4.5,
  "reviews": 120
}
```

## Core Services

### MandiLocationService
- Manages mandi location data
- Stores mandi details and contact information
- Handles mandi-commodity relationships

### MandiFilterService
- Filters market data by state, district, commodity
- Searches for specific varieties and grades
- Provides filtered results for UI

### StateDistrictPopulationService
- Manages state and district data
- Provides population statistics
- Used for demographic analysis

### MandiDataImportService
- Imports CSV data into database
- Handles data validation
- Manages data updates

### FertilizerSupplierService
- Manages fertilizer supplier information
- Handles supplier search and filtering
- Provides geolocation-based search

### NotificationService
- Sends price alerts to users
- Manages notification preferences
- Handles alert scheduling

## MCP Tools

### MandiMcpTools
Exposes mandi-related functions for AI agents:
- `search_mandi_prices` - Search market prices
- `get_commodity_list` - List commodities
- `get_fertilizer_suppliers` - Find suppliers
- `get_price_trends` - Get price trends

## Data Models

### MandiLocation Entity
```
- mandiId (PK)
- mandiName
- state (FK)
- district (FK)
- address
- latitude
- longitude
- phoneNumber
- email
- establishedYear
```

### State Entity
```
- stateId (PK)
- stateName (unique)
- population
- region
```

### District Entity
```
- districtId (PK)
- districtName
- stateId (FK)
- population
- area
```

### Commodity Entity
```
- commodityId (PK)
- commodityName
- commodityType (CEREAL, PULSE, OILSEED, etc.)
- unit (Quintal, Kg, Liter)
- description
```

### Variety Entity
```
- varietyId (PK)
- commodityId (FK)
- varietyName
- description
```

### Grade Entity
```
- gradeId (PK)
- varietyId (FK)
- gradeName (A1, A, B, etc.)
- description
```

### MandiPrice Entity
```
- priceId (PK)
- mandiId (FK)
- commodityId (FK)
- varietyId (FK)
- gradeId (FK)
- minPrice
- maxPrice
- modalPrice
- unit
- date
- volume
- source (AGMARKNET, data.gov.in)
```

### FertilizerSupplier Entity
```
- supplierId (PK)
- supplierName
- address
- state
- district
- latitude
- longitude
- phoneNumber
- email
- products (JSON array)
- rating
- reviewCount
```

## External Integrations

### AGMARKNET API
- **Base URL**: https://agmarknet.gov.in
- **Purpose**: Agricultural commodity prices
- **Data**: Mandi prices, commodities, varieties
- **Timeout**: 30 seconds
- **Retry**: 3 attempts

### data.gov.in API
- **Mandi Prices Endpoint**: `/resource/9ef84268-d588-465a-a308-a864a43d0070`
- **Fertilizer Suppliers Endpoint**: `/resource/56f40018-fd03-4010-94a3-f34ca7b43f7c`
- **Authentication**: API key required
- **Timeout**: 10 seconds
- **Retry**: 3 attempts

### Weather API
- **Provider**: weatherapi.com
- **Purpose**: Weather data for market analysis
- **Integration**: Optional for trend analysis

## Configuration

### Environment Variables
```
MANDI_SERVICE_PORT=8096
MANDI_DB_URL=jdbc:postgresql://localhost:5432/farmer_assistance
MANDI_DB_USERNAME=postgres
MANDI_DB_PASSWORD=password
AGMARKNET_API_BASE_URL=https://agmarknet.gov.in
AGMARKNET_CONNECTION_TIMEOUT=30
AGMARKNET_READ_TIMEOUT=60
AGMARKNET_MAX_RETRIES=3
MANDI_DATAGOV_PRICE_API_KEY=<api-key>
MANDI_DATAGOV_FERTILIZER_API_KEY=<api-key>
DATAGOV_TIMEOUT=10000
DATAGOV_RETRY_MAX_ATTEMPTS=3
ML_SERVICE_URL=http://localhost:8001
WEATHER_API_KEY=<api-key>
WEATHER_API_URL=https://api.weatherapi.com/v1
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
- **AGMARKNET API**: Market price data
- **data.gov.in API**: Mandi and fertilizer data
- **Weather API**: Weather information
- **Eureka Server**: Service registration

## Error Handling

- API timeout: 502 Bad Gateway
- Invalid API key: 401 Unauthorized
- Data not found: 404 Not Found
- Database error: 500 Internal Server Error
- Invalid filter parameters: 400 Bad Request

## Monitoring & Observability

- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`
- **Swagger/OpenAPI**: `/swagger-ui.html`, `/v3/api-docs`
- **Logging Level**: DEBUG for `com.farmer.mandi`
- **Performance Metrics**: API response time, data import duration

## Deployment

- **Docker**: Dockerfile available
- **Environment**: Supports dev, staging, production
- **Database Migration**: Hibernate auto-update enabled
- **Health Checks**: Spring Boot Actuator health endpoint

## Common Use Cases

1. **Search Market Prices**
   - POST `/api/v1/mandi/filter/search` with filters
   - Returns matching market prices

2. **Get Fertilizer Suppliers**
   - GET `/api/v1/fertilizer-suppliers/nearby` with coordinates
   - Returns nearby suppliers

3. **Get State List**
   - GET `/api/v1/mandi/filter/states`
   - Returns all states

4. **Get Districts by State**
   - GET `/api/v1/mandi/filter/districts/{stateId}`
   - Returns districts in state

5. **Get Commodity Varieties**
   - GET `/api/v1/mandi/filter/varieties/{commodityId}`
   - Returns varieties for commodity

## Performance Considerations

- Caching of state/district/commodity data
- Indexed queries on state, district, commodity
- Async API calls using WebFlux
- Connection pooling for database
- Redis caching for frequently accessed data
- Pagination for large result sets

## Data Refresh Strategy

- **Mandi Prices**: Updated daily from AGMARKNET
- **Fertilizer Suppliers**: Updated weekly from data.gov.in
- **State/District Data**: Updated monthly
- **Commodity Data**: Updated quarterly

## Future Enhancements

- Real-time price updates via WebSocket
- Price prediction using ML models
- Market trend analysis
- Farmer-to-farmer direct trading platform
- Auction price tracking
- Seasonal price forecasting
- Mobile app integration
