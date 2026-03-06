# Indian Farmer Assistance App - Complete System Overview

## Executive Summary

The Indian Farmer Assistance App is a comprehensive microservices-based platform designed to empower Indian farmers with AI-driven insights, market information, and government services. The system comprises 11 Java Spring Boot microservices, 1 Python FastAPI service, and AWS Lambda functions for advanced AI capabilities.

---

## System Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────────┐
│                    Frontend Layer                                │
│              Angular Web Application (4200)                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                            │
│         Spring Cloud Gateway + JWT Authentication               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  Microservices Layer                             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  User (8099) │ Crop (8093) │ Mandi (8096)              │   │
│  │  Weather (8100) │ Scheme (8097) │ Location (8095)      │   │
│  │  Yield (8094) │ Admin (8091) │ ML (8001)               │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Data & AI Layer                               │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  PostgreSQL │ Redis │ AWS Bedrock │ Lambda RAG          │   │
│  │  S3 │ Transcribe │ Polly │ Knowledge Base              │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  External Integrations                           │
│  AGMARKNET │ data.gov.in │ IMD │ weatherapi.com │ Bhashini     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Core Components

### 1. Frontend Application
- **Technology**: Angular 17+, TypeScript, RxJS
- **Port**: 4200
- **Features**: Responsive design, offline support, PWA capabilities
- **Pages**: Dashboard, Crops, Recommendations, Market, Weather, Schemes, Disease Detection, Voice Assistant

### 2. API Gateway
- **Technology**: Spring Cloud Gateway
- **Port**: 8080
- **Functions**: Request routing, JWT validation, rate limiting, request logging
- **Load Balancing**: Application Load Balancer (ALB)

### 3. Microservices (11 Java Services)

#### Authentication & User Management
- **User Service (8099)**: Registration, login, profile management, role management

#### Agricultural Intelligence
- **Crop Service (8093)**: Crop recommendations, rotation planning, fertilizer suggestions
- **Mandi Service (8096)**: Market prices, commodities, fertilizer suppliers
- **Weather Service (8100)**: Forecasts, agromet advisories, alerts
- **Yield Service (8094)**: Yield prediction, revenue calculation

#### Government Services
- **Scheme Service (8097)**: Government schemes, eligibility assessment
- **Location Service (8095)**: Government body locator, KVK finder

#### Administration
- **Admin Service (8091)**: Document management, analytics, audit logs

#### Infrastructure
- **Eureka Server (8761)**: Service discovery and registration

### 4. AI/ML Services

#### Python FastAPI Service (8001)
- Crop recommendation model (Random Forest)
- Crop rotation model (Decision Tree)
- Fertilizer recommendation model (Linear Regression)
- Voice assistant integration
- Disease detection

#### AWS Lambda RAG Service
- Retrieval-Augmented Generation (RAG)
- Voice processing (transcription + synthesis)
- Disease detection from images
- Chat memory management
- Multilingual support (10+ languages)

### 5. Data Layer

#### PostgreSQL Database
- Primary relational database
- Separate databases per service
- Connection pooling with HikariCP
- Automated backups and replication

#### Redis Cache
- Session management
- Recommendation caching
- Rate limiting counters
- Real-time data

#### AWS Services
- **S3**: Document storage
- **Bedrock**: LLM for voice assistant
- **Lambda**: Disease detection
- **Transcribe**: Speech-to-text
- **Polly**: Text-to-speech
- **DynamoDB**: Chat memory

---

## Key Features

### For Farmers

#### 1. Crop Recommendations
- AI-powered suggestions based on soil and weather
- Confidence scores and alternatives
- Integration with ML models
- Real-time updates

#### 2. Market Information
- Real-time commodity prices
- Market trends and analysis
- Fertilizer supplier locator
- Price alerts

#### 3. Weather Services
- 7-day forecasts
- Agromet advisories
- Weather alerts
- Nowcast (short-term forecasts)

#### 4. Yield Prediction
- Estimate crop yield
- Revenue calculation
- Profit analysis
- Break-even price

#### 5. Disease Detection
- Image-based disease identification
- Treatment recommendations
- Preventive measures
- Multilingual responses

#### 6. Voice Assistant
- Ask questions in local languages
- AI-powered answers using RAG
- Chat memory for context
- Audio responses

#### 7. Government Services
- Scheme eligibility assessment
- Government office locator
- Contact information
- Application links

### For Administrators

#### 1. Document Management
- Upload and manage documents
- S3 integration for storage
- Document versioning
- Access control

#### 2. Analytics
- User statistics
- Recommendation accuracy
- System health metrics
- Usage patterns

#### 3. Audit Logs
- Track all user actions
- Role modification history
- System events
- Compliance reporting

---

## Data Flow Examples

### Example 1: Crop Recommendation Flow

```
1. User enters soil/weather data
   ↓
2. Frontend sends POST to /api/v1/crops/recommendations
   ↓
3. API Gateway validates JWT and routes to Crop Service
   ↓
4. Crop Service:
   - Checks Redis cache
   - Fetches weather from Weather API
   - Fetches soil data from Kaegro API
   - Calls ML Service with combined data
   ↓
5. ML Service:
   - Loads Crop Recommendation Model
   - Runs prediction
   - Returns recommendation with confidence
   ↓
6. Crop Service:
   - Caches result in Redis
   - Stores in PostgreSQL
   - Returns to frontend
   ↓
7. Frontend displays recommendation
```

### Example 2: Voice Assistant Flow

```
1. User asks question in Hindi
   ↓
2. Frontend sends to Lambda RAG via API Gateway
   ↓
3. Lambda RAG:
   - Translates question to English (Bhashini)
   - Calls Bedrock Knowledge Base (RAG)
   - Retrieves relevant documents
   - Calls Claude 3 Sonnet LLM
   - Translates answer back to Hindi
   ↓
4. Returns answer to frontend
   ↓
5. Frontend displays answer
```

### Example 3: Disease Detection Flow

```
1. User uploads crop image
   ↓
2. Frontend sends to Lambda RAG
   ↓
3. Lambda RAG:
   - Auto-detects image format
   - Uploads to S3
   - Calls Bedrock Vision Model (Llama 4 Scout)
   - Analyzes image
   - Generates disease info in user's language
   ↓
4. Returns disease analysis
   ↓
5. Frontend displays disease info and treatment
```

---

## Technology Stack Summary

### Backend
- **Framework**: Spring Boot 3.5.8
- **Cloud**: Spring Cloud 2025.0.0
- **Language**: Java 17
- **Build**: Maven 3.8+
- **Database**: PostgreSQL 12+
- **Cache**: Redis 6+
- **ORM**: Spring Data JPA
- **Security**: Spring Security, JJWT

### Frontend
- **Framework**: Angular 17+
- **Language**: TypeScript 5+
- **UI**: Angular Material, Bootstrap 5
- **State**: RxJS
- **Build**: npm/yarn

### AI/ML
- **Framework**: FastAPI 0.109.0
- **Language**: Python 3.11+
- **ML**: scikit-learn 1.3.2, pandas 2.1.3
- **LLM**: AWS Bedrock (Claude 3 Sonnet)
- **RAG**: Bedrock Knowledge Base
- **Server**: Uvicorn 0.27.0

### Infrastructure
- **Containerization**: Docker
- **Orchestration**: AWS ECS Fargate
- **Load Balancing**: Application Load Balancer
- **Service Discovery**: AWS Cloud Map
- **Logging**: CloudWatch Logs
- **Monitoring**: CloudWatch Metrics
- **Storage**: Amazon S3
- **Database**: Amazon RDS PostgreSQL
- **Cache**: Amazon ElastiCache Redis

---

## Deployment Architecture

### Local Development
```
Docker Compose
├─ PostgreSQL (5432)
├─ Redis (6379)
├─ Eureka Server (8761)
├─ API Gateway (8080)
├─ All Microservices
├─ ML Service (8001)
└─ Frontend (4200)
```

### AWS ECS Production
```
AWS ECS Fargate Cluster
├─ Application Load Balancer
├─ 11 Microservices (Fargate Tasks)
├─ ML Service (Fargate Task)
├─ Frontend (Fargate Task)
├─ RDS PostgreSQL (Multi-AZ)
├─ ElastiCache Redis
├─ S3 Buckets
├─ Lambda Functions
├─ Cloud Map (Service Discovery)
└─ CloudWatch (Logging & Monitoring)
```

---

## Security Architecture

### Authentication
- JWT-based authentication
- 24-hour token expiration
- 7-day refresh token validity
- HS256 algorithm

### Authorization
- Role-based access control (RBAC)
- Roles: FARMER, ADMIN, SUPER_ADMIN
- Endpoint-level permissions

### Data Security
- AES-256 encryption at rest
- TLS 1.3 for data in transit
- Secrets in AWS Secrets Manager
- API key management

### API Security
- Rate limiting (100 req/min per user)
- CORS configuration
- CSRF protection
- Input validation
- SQL injection prevention

---

## Performance Characteristics

### Latency
- API response: 100-500ms
- ML prediction: 200-500ms
- Voice processing: 10-15 seconds
- Disease detection: 5-10 seconds

### Throughput
- API Gateway: 1000+ requests/second
- Database: 100+ concurrent connections
- Cache: 10,000+ operations/second

### Scalability
- Horizontal scaling via ECS auto-scaling
- Database read replicas
- Redis clustering
- CDN for static assets

---

## Monitoring & Observability

### Metrics
- Request latency (p50, p95, p99)
- Error rate
- CPU/memory utilization
- Database connection pool
- Cache hit rate
- API throughput

### Logging
- CloudWatch Logs
- Structured logging with timestamps
- 30-day retention
- Log levels: DEBUG, INFO, WARN, ERROR

### Alerting
- High CPU (> 80%)
- High memory (> 80%)
- Task failures
- Service unhealthy
- Database errors
- API errors (> 5%)

---

## External Integrations

### Government APIs
- **AGMARKNET**: Commodity prices (updated daily)
- **data.gov.in**: Mandi prices, fertilizer suppliers
- **IMD**: Weather forecasts (updated every 6 hours)

### Third-party Services
- **weatherapi.com**: Backup weather data
- **Bhashini**: Multilingual translation (10+ languages)
- **AWS Bedrock**: LLM inference
- **AWS Lambda**: Disease detection
- **AWS Transcribe**: Speech-to-text
- **AWS Polly**: Text-to-speech

---

## Supported Languages

| Language | Code | Support |
|----------|------|---------|
| Hindi | hi | ✓ Full |
| Bengali | bn | ✓ Full |
| Telugu | te | ✓ Full |
| Marathi | mr | ✓ Full |
| Tamil | ta | ✓ Full |
| Gujarati | gu | ✓ Full |
| Punjabi | pa | ✓ Full |
| Kannada | ka | ✓ Full |
| Malayalam | ml | ✓ Full |
| English | en | ✓ Full |

---

## API Endpoints Summary

### Authentication (User Service)
- POST `/api/v1/auth/register` - Register user
- POST `/api/v1/auth/login` - Login user
- POST `/api/v1/auth/refresh-token` - Refresh JWT

### Crops (Crop Service)
- GET `/api/v1/crops` - List crops
- POST `/api/v1/crops/recommendations` - Get recommendation
- POST `/api/v1/crops/yield/calculate` - Calculate yield

### Market (Mandi Service)
- GET `/api/v1/mandi/filter/states` - List states
- POST `/api/v1/mandi/filter/search` - Search prices
- GET `/api/v1/fertilizer-suppliers` - Find suppliers

### Weather (Weather Service)
- GET `/api/v1/weather/current` - Current weather
- GET `/api/v1/weather/forecast` - 7-day forecast
- GET `/api/v1/weather/agromet-advisory` - Advisory

### Schemes (Scheme Service)
- GET `/api/v1/schemes` - List schemes
- POST `/api/v1/schemes/search` - Search schemes

### Government Bodies (Location Service)
- GET `/api/v1/government-bodies` - List bodies
- GET `/api/v1/government-bodies/nearby` - Nearby bodies

### AI/ML (AI Service)
- POST `/api/ml/predict-crop` - Crop recommendation
- POST `/api/ml/ask-question` - Voice assistant
- POST `/api/ml/disease-detect` - Disease detection

---

## Database Schema Overview

### Core Tables
- `users` - User accounts
- `crops` - User crops
- `crop_recommendations` - Recommendations
- `mandi_prices` - Market prices
- `weather_data` - Weather information
- `schemes` - Government schemes
- `government_bodies` - KVK and offices
- `yield_calculators` - Yield parameters
- `documents` - Admin documents
- `audit_logs` - System audit trail

### Cache Keys (Redis)
- `user:{userId}:profile` - User profile
- `recommendation:{cropId}` - Crop recommendation
- `weather:{lat}:{lon}` - Weather data
- `prices:{commodity}` - Market prices
- `session:{sessionId}` - User session

---

## Cost Estimation (AWS)

### Monthly Costs (Approximate)
- **ECS Fargate**: $350-500 (11 tasks × 512 CPU, 1024 MB)
- **RDS PostgreSQL**: $100-200 (Multi-AZ)
- **ElastiCache Redis**: $50-100
- **S3**: $10-20
- **Lambda**: $5-10
- **CloudWatch**: $20-30
- **Data Transfer**: $50-100
- **Total**: $585-960/month

### Cost Optimization
- Use Fargate Spot (70% discount)
- Reserved capacity for predictable workloads
- Auto-scaling to reduce idle resources
- S3 lifecycle policies

---

## Disaster Recovery

### Backup Strategy
- Daily automated PostgreSQL backups (7-day retention)
- Multi-AZ RDS deployment
- S3 versioning and cross-region replication
- DynamoDB point-in-time recovery

### Recovery Procedures
- Service failure: Auto-replacement by ECS
- Database failure: RDS automatic failover
- Region failure: Restore from backup in different region
- Data loss: Restore from S3 versioning

### RTO/RPO
- **RTO** (Recovery Time Objective): < 5 minutes
- **RPO** (Recovery Point Objective): < 1 hour

---

## Future Roadmap

### Phase 1 (Q2 2024)
- ✓ Core microservices
- ✓ ML models
- ✓ Voice assistant
- ✓ Disease detection

### Phase 2 (Q3 2024)
- Mobile app (iOS/Android)
- Real-time notifications
- Advanced analytics
- Farmer community features

### Phase 3 (Q4 2024)
- IoT sensor integration
- Blockchain for supply chain
- Advanced ML models
- Multi-region deployment

### Phase 4 (2025)
- Marketplace integration
- Insurance recommendations
- Supply chain optimization
- Predictive analytics

---

## Getting Started

### Prerequisites
- Java 17+
- Python 3.11+
- Node.js 18+
- Docker & Docker Compose
- AWS CLI

### Quick Start
```bash
# Clone repository
git clone <repo-url>

# Start local environment
docker-compose up

# Access services
# Frontend: http://localhost:4200
# API Gateway: http://localhost:8080
# Eureka: http://localhost:8761
```

### AWS Deployment
```bash
# Create ECS stack
aws cloudformation create-stack \
  --stack-name farmer-ecs-stack \
  --template-body file://aws-ecs-template.yml \
  --parameters ParameterKey=VpcId,ParameterValue=vpc-xxxxx
```

---

## Documentation Files

| Document | Purpose |
|----------|---------|
| [README.md](./README.md) | Overview and quick start |
| [INDEX.md](./INDEX.md) | Complete documentation index |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | System architecture |
| [DEPLOYMENT_ECS.md](./DEPLOYMENT_ECS.md) | AWS ECS deployment |
| [SYSTEM_FLOWS.md](./SYSTEM_FLOWS.md) | Detailed workflows |
| [USER_SERVICE.md](./services/USER_SERVICE.md) | Authentication service |
| [CROP_SERVICE.md](./services/CROP_SERVICE.md) | Crop recommendations |
| [MANDI_SERVICE.md](./services/MANDI_SERVICE.md) | Market data |
| [WEATHER_SERVICE.md](./services/WEATHER_SERVICE.md) | Weather forecasts |
| [AI_SERVICE.md](./services/AI_SERVICE.md) | ML models |
| [LAMBDA_RAG_SERVICE.md](./services/LAMBDA_RAG_SERVICE.md) | Voice & disease detection |

---

## Support & Contact

For questions or issues:
1. Check relevant documentation
2. Review CloudWatch logs
3. Check service health endpoints
4. Contact development team

---

**Last Updated**: March 6, 2024  
**Version**: 1.0.0  
**Status**: Active  
**Maintained By**: Development Team
