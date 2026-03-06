# Scheme, Location, Yield, and Admin Services Documentation

## Table of Contents
1. [Scheme Service](#scheme-service)
2. [Location Service](#location-service)
3. [Yield Service](#yield-service)
4. [Admin Service](#admin-service)

---

## Scheme Service

**Port:** 8097  
**Language:** Java (Spring Boot)  
**Database:** PostgreSQL  
**Purpose:** Government agricultural schemes and eligibility assessment

### Overview

The Scheme Service provides information about government agricultural schemes, subsidies, and eligibility criteria. It helps farmers discover and understand available government support programs.

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/schemes` | Get all schemes |
| GET | `/api/v1/schemes/{schemeId}` | Get scheme details |
| POST | `/api/v1/schemes/search` | Search schemes by criteria |
| GET | `/api/v1/schemes/state/{state}` | Get schemes by state |
| GET | `/api/v1/schemes/commodity/{commodity}` | Get schemes by commodity |

### Request/Response Models

#### Scheme Search Request
```json
{
  "state": "Maharashtra",
  "commodity": "Rice",
  "farmerType": "MARGINAL",
  "landHolding": 1.5,
  "income": 100000
}
```

#### Scheme Response
```json
{
  "schemeId": "uuid",
  "schemeName": "PM-KISAN",
  "description": "Pradhan Mantri Kisan Samman Nidhi",
  "ministry": "Ministry of Agriculture",
  "state": "All India",
  "commodities": ["All"],
  "eligibility": {
    "farmerTypes": ["MARGINAL", "SMALL", "MEDIUM"],
    "minLandHolding": 0.1,
    "maxLandHolding": 2.0,
    "maxIncome": 500000
  },
  "benefits": {
    "amount": 6000,
    "frequency": "ANNUAL",
    "installments": 3
  },
  "applicationDeadline": "2024-12-31",
  "applicationUrl": "https://pmkisan.gov.in",
  "contactNumber": "1800-123-4567"
}
```

### Core Services

- **SchemeService**: Scheme search and filtering
- **SchemeDataLoader**: CSV-based scheme data initialization
- **EligibilityChecker**: Determines farmer eligibility

### Data Models

#### Scheme Entity
```
- schemeId (PK)
- schemeName
- description
- ministry
- state
- commodities (JSON array)
- eligibility (JSON)
- benefits (JSON)
- applicationDeadline
- applicationUrl
- contactNumber
- createdAt
- updatedAt
```

### MCP Tools

- `search_schemes` - Search schemes by criteria
- `get_scheme_details` - Get scheme information
- `check_eligibility` - Check farmer eligibility

### External Data Source

- CSV file with scheme information
- Government ministry websites
- PMKISAN portal data

### Configuration

```
SCHEME_SERVICE_PORT=8097
SCHEME_DB_URL=jdbc:postgresql://localhost:5432/scheme_service
SCHEME_DB_USERNAME=postgres
SCHEME_DB_PASSWORD=password
```

### Common Use Cases

1. **Search Schemes by State and Commodity**
   - POST `/api/v1/schemes/search` with filters
   - Returns matching schemes

2. **Get Scheme Details**
   - GET `/api/v1/schemes/{schemeId}`
   - Returns full scheme information

3. **Check Eligibility**
   - POST `/api/v1/schemes/check-eligibility` with farmer data
   - Returns eligible schemes

---

## Location Service

**Port:** 8095  
**Language:** Java (Spring Boot)  
**Database:** PostgreSQL  
**Purpose:** Government body locator, reverse geocoding, KVK/district officer contacts

### Overview

The Location Service helps farmers find government agricultural offices, KVKs (Krishi Vigyan Kendras), and district agricultural officers. It provides location-based services and contact information.

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/government-bodies` | Get all government bodies |
| GET | `/api/v1/government-bodies/{bodyId}` | Get body details |
| POST | `/api/v1/government-bodies/search` | Search by location |
| GET | `/api/v1/government-bodies/state/{state}` | Get bodies by state |
| GET | `/api/v1/government-bodies/district/{district}` | Get bodies by district |
| GET | `/api/v1/government-bodies/nearby` | Get nearby bodies (geolocation) |

### Request/Response Models

#### Government Body Search Request
```json
{
  "latitude": 28.7041,
  "longitude": 77.1025,
  "radius": 10,
  "bodyType": "KVK"
}
```

#### Government Body Response
```json
{
  "bodyId": "uuid",
  "name": "KVK Pune",
  "type": "KVK",
  "state": "Maharashtra",
  "district": "Pune",
  "address": "123 Agricultural Road",
  "latitude": 18.5204,
  "longitude": 73.8567,
  "phoneNumber": "+91-9876543210",
  "email": "kvk.pune@gov.in",
  "officerName": "Dr. Sharma",
  "officerDesignation": "Senior Scientist",
  "workingHours": "9:00 AM - 5:00 PM",
  "services": ["Crop Advisory", "Training", "Soil Testing"],
  "distance": 2.5
}
```

### Core Services

- **GovernmentBodyService**: Body management and search
- **GovernmentBodyDataLoader**: CSV-based data initialization
- **GeolocationService**: Reverse geocoding and nearby search

### Data Models

#### GovernmentBody Entity
```
- bodyId (PK)
- name
- type (KVK, DISTRICT_OFFICE, STATE_OFFICE, etc.)
- state
- district
- address
- latitude
- longitude
- phoneNumber
- email
- officerName
- officerDesignation
- workingHours
- services (JSON array)
```

### MCP Tools

- `get_government_bodies` - Get bodies by state/district
- `find_nearby_bodies` - Find nearby government offices
- `get_body_details` - Get office information

### Admin Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/admin/government-bodies` | Create body |
| PUT | `/api/v1/admin/government-bodies/{bodyId}` | Update body |
| DELETE | `/api/v1/admin/government-bodies/{bodyId}` | Delete body |

### Configuration

```
LOCATION_SERVICE_PORT=8095
LOCATION_DB_URL=jdbc:postgresql://localhost:5432/location_service
LOCATION_DB_USERNAME=postgres
LOCATION_DB_PASSWORD=password
```

### Common Use Cases

1. **Find Nearby KVK**
   - GET `/api/v1/government-bodies/nearby?latitude=28.7041&longitude=77.1025&radius=10`
   - Returns nearby KVKs

2. **Get District Officer Contact**
   - GET `/api/v1/government-bodies/district/Pune?type=DISTRICT_OFFICE`
   - Returns district office details

3. **Search by State**
   - GET `/api/v1/government-bodies/state/Maharashtra`
   - Returns all government bodies in state

---

## Yield Service

**Port:** 8094  
**Language:** Java (Spring Boot)  
**Database:** PostgreSQL  
**Purpose:** Yield prediction, revenue calculation, profit estimation

### Overview

The Yield Service provides yield predictions and revenue calculations based on crop, soil, and weather conditions. It helps farmers estimate expected production and income.

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/crops/yield/commodities` | Get all commodities |
| POST | `/api/v1/crops/yield/calculate` | Calculate yield |
| GET | `/api/v1/crops/yield/historical` | Get historical yield data |
| POST | `/api/v1/crops/yield/revenue` | Calculate revenue |

### Request/Response Models

#### Yield Calculation Request
```json
{
  "commodity": "Rice",
  "area": 1.5,
  "soilType": "Loamy",
  "soilPH": 6.8,
  "temperature": 25.5,
  "humidity": 65,
  "rainfall": 800,
  "nitrogenUsed": 45,
  "phosphorusUsed": 20,
  "potassiumUsed": 30,
  "irrigationMethod": "DRIP"
}
```

#### Yield Response
```json
{
  "yieldId": "uuid",
  "commodity": "Rice",
  "area": 1.5,
  "estimatedYield": 6.75,
  "yieldUnit": "Ton",
  "yieldPerHectare": 4.5,
  "confidence": 0.88,
  "factors": {
    "soilQuality": "Good",
    "weatherConditions": "Favorable",
    "nutrientManagement": "Optimal"
  },
  "recommendations": [
    "Maintain water level at 5cm",
    "Apply potassium fertilizer at flowering"
  ],
  "timestamp": "2024-03-06T10:30:00Z"
}
```

#### Revenue Calculation Request
```json
{
  "commodity": "Rice",
  "estimatedYield": 6.75,
  "marketPrice": 2650,
  "productionCost": 45000,
  "laborCost": 15000
}
```

#### Revenue Response
```json
{
  "revenueId": "uuid",
  "commodity": "Rice",
  "estimatedYield": 6.75,
  "marketPrice": 2650,
  "grossRevenue": 178875,
  "productionCost": 45000,
  "laborCost": 15000,
  "totalCost": 60000,
  "netProfit": 118875,
  "profitMargin": 66.4,
  "breakEvenPrice": 888.89,
  "timestamp": "2024-03-06T10:30:00Z"
}
```

### Core Services

- **YieldCalculatorService**: Yield prediction and calculation
- **RevenueCalculatorService**: Revenue and profit calculation
- **YieldCalculatorDataLoader**: CSV-based yield parameters

### Data Models

#### YieldCalculator Entity
```
- yieldId (PK)
- commodity
- soilType
- baseYield
- yieldPerHectare
- factors (JSON)
- recommendations (JSON)
```

#### Commodity Entity
```
- commodityId (PK)
- commodityName
- unit
- marketPrice
- productionCost
- laborCost
```

### MCP Tools

- `calculate_yield` - Calculate crop yield
- `calculate_revenue` - Calculate revenue
- `get_commodities` - List commodities

### Configuration

```
YIELD_SERVICE_PORT=8094
YIELD_DB_URL=jdbc:postgresql://localhost:5432/yield_service
YIELD_DB_USERNAME=postgres
YIELD_DB_PASSWORD=password
```

### Common Use Cases

1. **Calculate Yield**
   - POST `/api/v1/crops/yield/calculate` with crop and soil data
   - Returns estimated yield

2. **Calculate Revenue**
   - POST `/api/v1/crops/yield/revenue` with yield and market data
   - Returns profit estimation

3. **Get Commodities**
   - GET `/api/v1/crops/yield/commodities`
   - Returns all supported commodities

---

## Admin Service

**Port:** 8091  
**Language:** Java (Spring Boot)  
**Database:** PostgreSQL  
**AWS Integration:** S3 for document storage  
**Purpose:** Document management, analytics, system administration

### Overview

The Admin Service provides administrative functions including document management, analytics, audit logs, and system configuration. It integrates with AWS S3 for secure document storage.

### API Endpoints

#### Document Management (`/api/v1/admin/documents`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Upload document |
| GET | `/` | List documents |
| GET | `/{documentId}` | Get document details |
| DELETE | `/{documentId}` | Delete document |
| GET | `/{documentId}/download` | Download document |

#### Analytics (`/api/v1/admin/analytics`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dashboard` | Get analytics dashboard |
| GET | `/users` | Get user statistics |
| GET | `/crops` | Get crop statistics |
| GET | `/recommendations` | Get recommendation statistics |

#### Audit Logs (`/api/v1/admin/audit-logs`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get audit logs |
| GET | `/user/{userId}` | Get user audit logs |
| GET | `/action/{action}` | Get logs by action |

### Request/Response Models

#### Document Upload Request
```
Content-Type: multipart/form-data
- file: Binary file
- documentType: "TRAINING_MATERIAL", "ADVISORY", "RESEARCH"
- title: "Document Title"
- description: "Document Description"
```

#### Document Response
```json
{
  "documentId": "uuid",
  "title": "Crop Rotation Guide",
  "description": "Guide for sustainable crop rotation",
  "documentType": "ADVISORY",
  "fileName": "crop_rotation_guide.pdf",
  "fileSize": 2048576,
  "uploadedBy": "admin@example.com",
  "uploadedAt": "2024-03-06T10:30:00Z",
  "s3Url": "https://s3.amazonaws.com/bucket/documents/...",
  "tags": ["crop", "rotation", "sustainability"]
}
```

#### Analytics Dashboard Response
```json
{
  "dashboardId": "uuid",
  "totalUsers": 5000,
  "activeUsers": 3500,
  "totalRecommendations": 15000,
  "averageRecommendationAccuracy": 0.88,
  "topCrops": ["Rice", "Wheat", "Maize"],
  "topStates": ["Maharashtra", "Punjab", "Haryana"],
  "systemHealth": {
    "uptime": 99.9,
    "responseTime": 250,
    "errorRate": 0.1
  },
  "generatedAt": "2024-03-06T10:30:00Z"
}
```

### Core Services

- **DocumentService**: Document upload, storage, retrieval
- **AnalyticsService**: System analytics and reporting
- **AuditLogService**: Audit trail management
- **AwsS3Config**: AWS S3 integration

### Data Models

#### Document Entity
```
- documentId (PK)
- title
- description
- documentType
- fileName
- fileSize
- uploadedBy (FK)
- uploadedAt
- s3Url
- tags (JSON array)
- isPublic
```

#### AuditLog Entity
```
- logId (PK)
- userId (FK)
- action
- resource
- details (JSON)
- timestamp
- ipAddress
- userAgent
```

#### Analytics Entity
```
- analyticsId (PK)
- metricName
- metricValue
- timestamp
- dimension (JSON)
```

### AWS S3 Integration

- **Bucket**: `indian-farmer-assistance-documents`
- **Folder Structure**: `/documents/{documentType}/{year}/{month}/`
- **File Naming**: `{documentId}_{timestamp}_{originalFileName}`
- **Access Control**: Private with signed URLs for download
- **Encryption**: Server-side encryption enabled

### Security

- **Authentication**: JWT-based, admin role required
- **Authorization**: Role-based access control
- **File Validation**: Type, size, and content validation
- **Virus Scanning**: Optional integration with antivirus service

### Configuration

```
ADMIN_SERVICE_PORT=8091
ADMIN_DB_URL=jdbc:postgresql://localhost:5432/admin_service
ADMIN_DB_USERNAME=postgres
ADMIN_DB_PASSWORD=password
AWS_S3_BUCKET_NAME=indian-farmer-assistance-documents
AWS_S3_REGION=ap-south-1
AWS_ACCESS_KEY_ID=<access-key>
AWS_SECRET_ACCESS_KEY=<secret-key>
AWS_S3_ENDPOINT=https://s3.amazonaws.com
```

### Common Use Cases

1. **Upload Document**
   - POST `/api/v1/admin/documents` with file and metadata
   - Returns document details with S3 URL

2. **Get Analytics Dashboard**
   - GET `/api/v1/admin/analytics/dashboard`
   - Returns system analytics

3. **Get Audit Logs**
   - GET `/api/v1/admin/audit-logs?limit=100&offset=0`
   - Returns audit trail

4. **Download Document**
   - GET `/api/v1/admin/documents/{documentId}/download`
   - Returns signed S3 URL

### Performance Considerations

- Async document upload to S3
- Pagination for large result sets
- Caching of analytics data
- Indexed queries on userId, action, timestamp

### Future Enhancements

- Document versioning
- Full-text search for documents
- Advanced analytics and reporting
- Real-time system monitoring
- Automated backup and recovery
- Document approval workflow
