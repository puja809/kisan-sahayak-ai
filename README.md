# Indian Farmer Assistance Application

A comprehensive, multilingual, mobile-first platform designed to empower farmers across India with real-time agricultural intelligence, government scheme access, market information, and AI-powered assistance.

## Overview

The Indian Farmer Assistance Application integrates with multiple government Digital Public Infrastructure (DPI) services to provide farmers with:

- **Real-time Weather Forecasts** - 7-day forecasts, nowcasts, and district-level alerts from IMD
- **Location-Based Crop Recommendations** - Personalized crop suggestions based on agro-ecological zones and soil health
- **Crop Rotation Planning** - Nutrient cycling optimization and soil health management
- **Government Scheme Access** - Central and state-specific schemes with eligibility assessment
- **Market Intelligence** - Real-time mandi prices and market trends from AGMARKNET
- **Disease Detection** - AI-powered crop disease identification with treatment recommendations
- **IoT Device Management** - Farm sensor monitoring and environmental tracking
- **Multilingual Voice Agent** - Voice-based interaction in 22+ Indian languages via Bhashini
- **Farmer Profile Management** - Comprehensive farm and crop record tracking
- **Yield Prediction** - AI-driven yield estimation with historical analysis

## Key Features

### 1. Weather Intelligence
- 7-day weather forecasts with temperature, humidity, rainfall, and wind data
- Real-time nowcast alerts (0-3 hours) for thunderstorms and localized precipitation
- District-level weather warnings with severity indicators
- Agromet advisories with heat stress indices and evapotranspiration (ET₀) data
- Offline weather caching for rural connectivity

### 2. Crop Management
- Crop recommendations ranked by suitability score
- Crop rotation planning with nutrient cycling optimization
- Yield estimation with min/expected/max ranges
- Fertilizer calculator with split application schedules
- 5-year crop history tracking
- Harvest recording with quality grades and input cost tracking

### 3. Government Schemes
- **Central Schemes**: PM-Kisan, PMFBY, PM-Kisan Maan Dhan, KCC
- **State-Specific Schemes**: Karnataka (FRUITS), Maharashtra (MahaDBT), Telangana (Rythu Bandhu), Andhra Pradesh (YSR Rythu Bharosa), Haryana, Uttar Pradesh, Punjab
- Eligibility assessment with confidence indicators
- Application tracking and deadline notifications
- Direct links to online application portals

### 4. Market Intelligence
- Real-time mandi prices from AGMARKNET
- Price trends with 30-day historical analysis
- MSP (Minimum Support Price) comparison
- Mandi locator with distance-based sorting
- Price alerts for peak trading periods

### 5. Disease Detection
- AI-powered crop disease identification from images
- Severity level assessment (low, medium, high, critical)
- Treatment recommendations (organic and chemical options)
- Links to KVK experts for consultation
- Visual overlays highlighting affected regions

### 6. IoT Integration
- Device provisioning and management
- Real-time sensor readings (soil moisture, temperature, humidity, pH)
- Threshold-based alerts
- 30-day historical trend visualization
- Vendor-neutral device support

### 7. Multilingual Voice Agent
- Voice interaction in 22+ Indian languages
- Automatic Speech Recognition (ASR) with Voice Activity Detection
- Neural Machine Translation (NMT) for language processing
- Text-to-Speech (TTS) with natural-sounding voices
- Fallback to text-based interaction
- OCR support for document extraction

### 8. AgriStack Integration
- Farmer authentication via UFSI (Unified Farmer Service Interface)
- Access to Farmer Registry with unique Farmer IDs
- Geo-Referenced Village Map Registry for plot-level precision
- Crop Sown Registry with Digital Crop Survey data
- Paper-free credit approval with verified land and crop details

## Architecture

### Technology Stack

**Frontend**
- Angular web application
- Progressive Web App (PWA) for offline support
- Mobile-first responsive design
- Optimized for 2GB RAM devices with 3-second launch target

**Backend Services**
- Spring Boot microservices (Java)
- API Gateway with authentication and rate limiting
- User, Weather, Crop, Scheme, Mandi, IoT, and Admin services

**AI/ML Services**
- Python-based AI service
- Disease detection model
- Yield prediction model
- Recommendation engine
- Voice agent orchestration

**Data Layer**
- MySQL for relational data
- MongoDB for vector embeddings and document storage
- Redis for caching
- Vector database for semantic search

**External Integrations**
- IMD Weather API
- AGMARKNET API
- AgriStack UFSI
- Bhashini API (ASR, NMT, TTS, OCR)

### Microservices

1. **API Gateway** - Request routing, authentication, rate limiting
2. **User Service** - Registration, profile management, AgriStack integration
3. **Weather Service** - IMD integration, weather caching, alerts
4. **Crop Service** - Recommendations, rotation planning, yield estimation
5. **Scheme Service** - Scheme catalog, eligibility assessment, application tracking
6. **Mandi Service** - AGMARKNET integration, price trends, alerts
7. **IoT Service** - Device provisioning, sensor data collection, alerts
8. **Admin Service** - Document management, scheme administration, analytics
9. **AI/ML Service** - Disease detection, yield prediction, recommendations, voice processing

## Non-Functional Requirements

### Availability & Reliability
- 99.5% monthly uptime (excluding planned maintenance)
- Graceful degradation when external APIs are unavailable
- Offline functionality for critical features

### Performance
- Support for 1M+ concurrent users nationally
- Weather/price queries: <2 seconds (95th percentile)
- Voice interactions: <4 seconds round-trip (95th percentile)
- Home screen launch: <3 seconds on 2GB RAM devices

### Data Freshness
- IMD weather data: Updated within 30 minutes
- AGMARKNET prices: Updated within 1 hour
- Government schemes: Updated within 24 hours

### Security & Compliance
- TLS 1.3 encryption for all data transmissions
- AES-256 encryption for data at rest
- DPDP Act 2023 compliance
- Data localization within Indian territory
- Role-based access control (RBAC)
- Complete audit trails for all administrative actions

## Getting Started

### Prerequisites
- Java 11+ (for backend services)
- Node.js 16+ (for Angular frontend)
- Python 3.8+ (for AI/ML services)
- MySQL 8.0+
- MongoDB 4.4+
- Redis 6.0+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/indian-farmer-assistance-app.git
   cd indian-farmer-assistance-app
   ```

2. **Backend Setup**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

3. **Frontend Setup**
   ```bash
   cd frontend
   npm install
   npm start
   ```

4. **AI/ML Service Setup**
   ```bash
   cd ai-service
   pip install -r requirements.txt
   python app.py
   ```

### Configuration

Configure external API integrations in `application.properties`:

```properties
# IMD Weather API
imd.api.url=https://api.imd.gov.in
imd.api.key=${IMD_API_KEY}

# AGMARKNET API
agmarknet.api.url=https://agmarknet.gov.in/api
agmarknet.api.key=${AGMARKNET_API_KEY}

# AgriStack UFSI
agristack.ufsi.url=https://ufsi.agristack.gov.in
agristack.client.id=${AGRISTACK_CLIENT_ID}
agristack.client.secret=${AGRISTACK_CLIENT_SECRET}

# Bhashini API
bhashini.api.url=https://api.bhashini.gov.in
bhashini.api.key=${BHASHINI_API_KEY}

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/farmer_app
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/farmer_app_vectors

# Redis
spring.redis.host=localhost
spring.redis.port=6379
```

## API Documentation

### Weather Service
```
GET    /api/v1/weather/forecast/{district}
GET    /api/v1/weather/current/{district}
GET    /api/v1/weather/nowcast/{district}
GET    /api/v1/weather/alerts/{district}
```

### Crop Service
```
GET    /api/v1/crops/recommendations
POST   /api/v1/crops/recommendations/calculate
GET    /api/v1/crops/rotation/{farmerId}
POST   /api/v1/crops/yield/estimate
```

### Scheme Service
```
GET    /api/v1/schemes
GET    /api/v1/schemes/{id}
POST   /api/v1/schemes/eligibility/check
GET    /api/v1/schemes/personalized/{farmerId}
```

### Mandi Service
```
GET    /api/v1/mandi/prices/{commodity}
GET    /api/v1/mandi/prices/trends/{commodity}
GET    /api/v1/mandi/locations/nearby
```

### Disease Detection
```
POST   /api/v1/ai/disease/detect
```

### Voice Agent
```
POST   /api/v1/ai/voice/process
POST   /api/v1/ai/voice/asr
POST   /api/v1/ai/voice/tts
```

## Data Models

The application uses a comprehensive data model including:

- **Users** - Farmer profiles with AgriStack integration
- **Farms** - Land parcel information with GPS coordinates
- **Crops** - Crop records with sowing/harvest dates and input costs
- **Fertilizer Applications** - Nutrient tracking and application history
- **Livestock** - Farm animal records
- **Equipment** - Farm machinery tracking
- **Yield Predictions** - AI-driven yield estimates with confidence intervals
- **Schemes** - Government scheme catalog with eligibility criteria
- **Mandi Prices** - Real-time commodity prices from regulated markets
- **IoT Devices** - Connected farm sensors and readings
- **Disease Detections** - AI-identified crop diseases with treatment recommendations
- **Weather Cache** - Offline weather data storage
- **Audit Logs** - Complete data provenance and compliance tracking

## Correctness Properties

The application implements formal correctness properties to ensure reliability through property-based testing:

1. **API Parameter Correctness** - Weather API calls use correct district parameters
2. **Weather Data Completeness** - All required weather fields present for 7-day forecasts
3. **Alert Display Consistency** - All warnings displayed with severity indicators
4. **Cached Data Timestamps** - Offline data includes accurate fetch timestamps
5. **Location to Zone Mapping** - Consistent agro-ecological zone mapping
6. **Descending Ranking Order** - Recommendations ranked by score in descending order
7. **Crop Recommendation Completeness** - All required fields in recommendations
8. **Nutrient Depletion Detection** - Risk flagged for consecutive same-family crops
9. **Eligibility Assessment Consistency** - Same inputs produce same eligibility results
10. **State-Based Scheme Filtering** - Only applicable schemes displayed
11. **Distance-Based Sorting** - Locations sorted by distance in ascending order
12. **Price Data Constraints** - min_price ≤ modal_price ≤ max_price
13. **Language Configuration Consistency** - Selected language persists across sessions
14. **Disambiguation Trigger** - Ambiguous queries request user confirmation
15. **Voice Fallback Hierarchy** - Explicit fallback order: voice → text → cached
16. **Image Validation** - JPEG/PNG format and ≤10MB size enforced
17. **Disease Detection Confidence Threshold** - Below 70% confidence shows healthy status
18. **IoT Alert Threshold Monitoring** - Alerts generated only when thresholds exceeded
19. **End-to-End Encryption** - TLS 1.3 in transit, AES-256 at rest
20. **AgriStack Profile Retrieval** - All three registries retrieved on authentication
21. **Profile Data Persistence Round Trip** - All fields persisted and retrievable
22. **Data Retention Policy** - Crops retained for 5 years or 10 cycles
23. **Version History Maintenance** - All updates versioned with timestamps
24. **Yield Estimate Range Validity** - min ≤ expected ≤ max with confidence intervals
25. **Prediction Variance Calculation** - Variance stored for model improvement
26. **Fertilizer Recommendation Completeness** - All required fields in recommendations
27. **Nutrient Calculation Accuracy** - Total NPK equals sum of individual applications
28. **Scheme Deadline Notifications** - Push notifications at 7 days and 1 day before
29. **Offline Mode Activation** - Automatic enable within 2 seconds of disconnection
30. **Sync Queue FIFO Processing** - Queued requests processed in order
31. **Adaptive Quality Reduction** - Image quality reduced on low bandwidth
32. **Reverse Geocoding Accuracy** - GPS coordinates map to correct district/state
33. **Conflict Resolution by Timestamp** - Most recent timestamp wins
34. **Localization Completeness** - All UI text translated to selected language
35. **Error Message User-Friendliness** - No technical stack traces in messages
36. **Retry Logic Bounds** - Exactly 3 retries with exponential backoff
37. **PII Exclusion from Analytics** - No PII without explicit consent
38. **Document Format Validation** - PDF/DOCX/TXT format and ≤50MB size
39. **Vector Embedding Generation** - 768-dimensional embeddings generated
40. **Semantic Search Ranking** - Documents ranked by cosine similarity
41. **Role-Based Access Control** - 403 Forbidden for unauthorized admin access
42. **Audit Log Completeness** - All admin actions logged with full metadata

## Testing

The application includes comprehensive test coverage:

- **Unit Tests** - Individual component testing
- **Property-Based Tests** - Formal correctness property validation
- **Integration Tests** - API and service integration testing
- **Performance Tests** - Load and stress testing

Run tests with:
```bash
# Backend tests
mvn test

# Frontend tests
npm test

# AI/ML tests
pytest
```

## Deployment

### Docker Deployment
```bash
docker-compose up -d
```

### Kubernetes Deployment
```bash
kubectl apply -f k8s/
```

### Cloud Deployment
- AWS ECS/EKS
- Google Cloud Run
- Azure Container Instances

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, contact:
- **Email**: support@farmerapp.gov.in
- **Phone**: 1800-FARMER-1 (1800-327-6371)
- **Website**: https://farmerapp.gov.in

## Acknowledgments

- India Meteorological Department (IMD)
- AGMARKNET
- AgriStack
- Bhashini
- Ministry of Agriculture & Farmers Welfare
- State Agriculture Departments
- Krishi Vigyan Kendras (KVKs)

## Implementation Status

### Completed (Spec Ready)
- Comprehensive requirements and design documentation
- 42 correctness properties defined for property-based testing
- Microservices architecture with clear boundaries
- Complete database schemas (MySQL and MongoDB)
- API specifications for all services
- Error handling and testing strategies

### Next Steps
- Generate implementation tasks from design
- Set up development environment
- Implement core services (Weather, Crop, Scheme)
- Implement AI/ML services (Disease detection, Yield prediction)
- Implement voice agent with Bhashini integration
- Implement IoT device management
- Implement admin document management
- Property-based testing implementation
- End-to-end testing and validation

---

**Last Updated**: February 2026
**Version**: 1.0.0
**Spec Status**: Requirements and Design Complete
#   k i s a n - s a h a y a k - a i 
 
 