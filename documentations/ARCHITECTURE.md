# Indian Farmer Assistance App - System Architecture

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [System Components](#system-components)
3. [Data Flow](#data-flow)
4. [Service Communication](#service-communication)
5. [Technology Stack](#technology-stack)
6. [Deployment Architecture](#deployment-architecture)
7. [Security Architecture](#security-architecture)
8. [Scalability & Performance](#scalability--performance)

---

## Architecture Overview

The Indian Farmer Assistance App uses a **microservices architecture** with the following characteristics:

- **11 Java Spring Boot microservices** + **1 Python AI service**
- **API Gateway** for request routing and authentication
- **Eureka Server** for service discovery
- **PostgreSQL** for persistent data storage
- **Redis** for caching and session management
- **AWS Services** for ML and document storage
- **Government APIs** for real-time data

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend Layer                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Angular Web Application (Port 4200)                     │   │
│  │  - Responsive UI for farmers and admins                  │   │
│  │  - Real-time updates via WebSocket                       │   │
│  │  - Offline support with service workers                  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway Layer                             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Spring Cloud Gateway (Port 8080)                        │   │
│  │  - Request routing                                       │   │
│  │  - JWT authentication                                    │   │
│  │  - Rate limiting                                         │   │
│  │  - Request logging                                       │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  Microservices Layer                             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  User Service (8099)      │  Crop Service (8093)         │   │
│  │  - Authentication         │  - Recommendations           │   │
│  │  - Profile Management     │  - Rotation Planning         │   │
│  │  - Role Management        │  - ML Integration            │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │  Mandi Service (8096)     │  Weather Service (8100)      │   │
│  │  - Market Prices          │  - Forecasts                 │   │
│  │  - Commodities            │  - Agromet Advisories        │   │
│  │  - Fertilizer Suppliers   │  - Alerts                    │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │  Scheme Service (8097)    │  Location Service (8095)     │   │
│  │  - Government Schemes     │  - Government Bodies         │   │
│  │  - Eligibility Check      │  - Geolocation              │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │  Yield Service (8094)     │  Admin Service (8091)        │   │
│  │  - Yield Prediction       │  - Document Management       │   │
│  │  - Revenue Calculation    │  - Analytics                 │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    AI/ML Layer                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  AI Service (Python FastAPI, Port 8001)                  │   │
│  │  - Crop Recommendation Model                             │   │
│  │  - Crop Rotation Model                                   │   │
│  │  - Fertilizer Recommendation Model                       │   │
│  │  - Voice Assistant (AWS Bedrock)                         │   │
│  │  - Disease Detection (AWS Lambda)                        │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  Data & Infrastructure Layer                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  PostgreSQL (Primary Database)                           │   │
│  │  - User data, profiles, crops                            │   │
│  │  - Market data, commodities                              │   │
│  │  - Schemes, government bodies                            │   │
│  │  - Recommendations, analytics                            │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │  Redis (Cache & Session Store)                           │   │
│  │  - Session management                                    │   │
│  │  - Recommendation caching                                │   │
│  │  - Rate limiting counters                                │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │  AWS Services                                            │   │
│  │  - S3: Document storage                                  │   │
│  │  - Bedrock: LLM for voice assistant                      │   │
│  │  - Lambda: Disease detection                             │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │  External APIs                                           │   │
│  │  - IMD: Weather forecasts                                │   │
│  │  - AGMARKNET: Market prices                              │   │
│  │  - data.gov.in: Mandi & fertilizer data                  │   │
│  │  - weatherapi.com: Backup weather                        │   │
│  │  - Bhashini: Multilingual support                        │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  Service Discovery Layer                         │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Eureka Server (Port 8761)                               │   │
│  │  - Service registration                                  │   │
│  │  - Service discovery                                     │   │
│  │  - Health monitoring                                     │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## System Components

### Frontend Layer

**Angular Web Application**
- **Port**: 4200
- **Framework**: Angular 17+
- **Features**:
  - Responsive design for desktop and mobile
  - Real-time updates via WebSocket
  - Offline support with service workers
  - Progressive Web App (PWA) capabilities
  - Multi-language support

**Key Pages**:
- Dashboard: Overview of recommendations and alerts
- Crop Management: Add/manage crops
- Recommendations: View crop, rotation, and fertilizer recommendations
- Market Prices: Search and view market data
- Weather: Current weather and forecasts
- Schemes: Government schemes and eligibility
- Government Bodies: Find KVKs and agricultural offices
- Yield Calculator: Estimate yield and revenue
- Disease Detection: Upload crop images for disease detection
- Voice Assistant: Ask questions in local languages
- Admin Dashboard: System administration and analytics

### API Gateway Layer

**Spring Cloud Gateway**
- **Port**: 8080
- **Responsibilities**:
  - Route requests to appropriate microservices
  - Validate JWT tokens
  - Apply rate limiting
  - Log all requests
  - Aggregate API documentation

### Microservices Layer

**11 Java Spring Boot Services**:
1. User Service (8099) - Authentication and profiles
2. Crop Service (8093) - Crop recommendations
3. Mandi Service (8096) - Market data
4. Weather Service (8100) - Weather forecasts
5. Scheme Service (8097) - Government schemes
6. Location Service (8095) - Government bodies
7. Yield Service (8094) - Yield predictions
8. Admin Service (8091) - Document management
9. API Gateway (8080) - Request routing
10. Eureka Server (8761) - Service discovery
11. Common Module - Shared utilities

### AI/ML Layer

**Python FastAPI Service**
- **Port**: 8001
- **Models**:
  - Crop Recommendation (Random Forest)
  - Crop Rotation (Decision Tree)
  - Fertilizer Recommendation (Linear Regression)
- **Features**:
  - Voice Assistant (AWS Bedrock LLM)
  - Disease Detection (AWS Lambda)
  - Multilingual Support (Bhashini API)

### Data Layer

**PostgreSQL**
- Primary relational database
- Separate databases per service (database per service pattern)
- Connection pooling with HikariCP

**Redis**
- Session management
- Caching layer
- Rate limiting counters

**AWS Services**
- S3: Document storage
- Bedrock: LLM for voice assistant
- Lambda: Disease detection

**External APIs**
- IMD: Weather data
- AGMARKNET: Market prices
- data.gov.in: Mandi and fertilizer data
- weatherapi.com: Backup weather
- Bhashini: Multilingual translation

### Service Discovery Layer

**Eureka Server**
- Central service registry
- Health monitoring
- Service instance management

---

## Data Flow

### 1. User Registration and Authentication Flow

```
User (Frontend)
    ↓
POST /api/v1/auth/register
    ↓
API Gateway (8080)
    ↓
User Service (8099)
    ├─ Validate input
    ├─ Hash password
    ├─ Store in PostgreSQL
    └─ Generate JWT token
    ↓
Return JWT token to Frontend
    ↓
Frontend stores token in localStorage
```

### 2. Crop Recommendation Flow

```
User (Frontend)
    ↓
POST /api/v1/crops/recommendations
(with soil/weather data)
    ↓
API Gateway (8080)
    ├─ Validate JWT token
    └─ Route to Crop Service
    ↓
Crop Service (8093)
    ├─ Fetch weather data from Weather API
    ├─ Fetch soil data from Kaegro API
    └─ Call ML Service
    ↓
AI Service (8001)
    ├─ Load Crop Recommendation Model
    ├─ Predict best crop
    └─ Return recommendation
    ↓
Crop Service
    ├─ Cache result in Redis
    ├─ Store in PostgreSQL
    └─ Return to Frontend
    ↓
Frontend displays recommendation
```

### 3. Market Price Search Flow

```
User (Frontend)
    ↓
POST /api/v1/mandi/filter/search
(with state, district, commodity)
    ↓
API Gateway (8080)
    ├─ Validate JWT token
    └─ Route to Mandi Service
    ↓
Mandi Service (8096)
    ├─ Check Redis cache
    ├─ If not cached:
    │  ├─ Query PostgreSQL
    │  ├─ Call AGMARKNET API
    │  ├─ Call data.gov.in API
    │  └─ Cache in Redis
    └─ Return results
    ↓
Frontend displays market prices
```

### 4. Voice Assistant Flow

```
User (Frontend)
    ↓
POST /api/ml/ask-question
(with question in local language)
    ↓
API Gateway (8080)
    ├─ Validate JWT token
    └─ Route to AI Service
    ↓
AI Service (8001)
    ├─ Translate question to English (Bhashini)
    ├─ Call AWS Bedrock LLM
    ├─ Get answer from Claude 3 Sonnet
    ├─ Translate answer back to local language
    └─ Return answer
    ↓
Frontend displays answer
```

### 5. Disease Detection Flow

```
User (Frontend)
    ↓
POST /api/ml/disease-detect
(with crop image)
    ↓
API Gateway (8080)
    ├─ Validate JWT token
    └─ Route to AI Service
    ↓
AI Service (8001)
    ├─ Validate image
    ├─ Call AWS Lambda
    ├─ Lambda processes image
    ├─ Deep learning model classifies disease
    └─ Return disease name and treatment
    ↓
Frontend displays disease info and treatment
```

---

## Service Communication

### Synchronous Communication (REST)

Services communicate synchronously using REST APIs:

```
Crop Service → ML Service
Crop Service → Weather API
Mandi Service → AGMARKNET API
Mandi Service → data.gov.in API
```

### Asynchronous Communication (Events)

For future scalability, event-driven communication can be added:

```
Service A → Event Bus (Kafka/RabbitMQ) → Service B
```

### Service-to-Service Authentication

Services use JWT tokens for inter-service communication:

```
Service A
    ↓
Generate JWT token with service credentials
    ↓
Service B
    ↓
Validate JWT token
    ↓
Process request
```

---

## Technology Stack

### Backend

**Java Stack**:
- Spring Boot 3.5.8
- Spring Cloud 2025.0.0
- Spring Data JPA
- Spring Security
- Spring WebFlux (reactive)
- Spring AI MCP

**Database**:
- PostgreSQL 12+
- Redis 6+
- MongoDB 5+ (optional, for embeddings)

**Libraries**:
- JJWT 0.11.5 (JWT)
- Lombok
- SpringDoc OpenAPI 2.0.4
- HikariCP (connection pooling)

**Build Tool**:
- Maven 3.8+

### Frontend

**Angular Stack**:
- Angular 17+
- TypeScript 5+
- RxJS
- Angular Material
- Bootstrap 5

**Build Tool**:
- npm/yarn

### AI/ML

**Python Stack**:
- FastAPI 0.109.0
- Uvicorn 0.27.0
- scikit-learn 1.3.2
- pandas 2.1.3
- numpy 1.x
- LangChain
- boto3 (AWS SDK)
- MCP (Model Context Protocol)

**ML Models**:
- Random Forest (Crop Recommendation)
- Decision Tree (Crop Rotation)
- Linear Regression (Fertilizer Recommendation)

### Infrastructure

**Containerization**:
- Docker
- Docker Compose

**Orchestration**:
- Kubernetes (optional)
- AWS ECS (optional)

**Cloud Services**:
- AWS S3 (document storage)
- AWS Bedrock (LLM)
- AWS Lambda (disease detection)

---

## Deployment Architecture

### Local Development

```
docker-compose up
├─ PostgreSQL (5432)
├─ Redis (6379)
├─ Eureka Server (8761)
├─ API Gateway (8080)
├─ User Service (8099)
├─ Crop Service (8093)
├─ Mandi Service (8096)
├─ Weather Service (8100)
├─ Scheme Service (8097)
├─ Location Service (8095)
├─ Yield Service (8094)
├─ Admin Service (8091)
├─ AI Service (8001)
└─ Angular Frontend (4200)
```

### Docker Compose Structure

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    ports:
      - "5432:5432"
  
  redis:
    image: redis:7
    ports:
      - "6379:6379"
  
  eureka-server:
    build: ./backend/eureka-server
    ports:
      - "8761:8761"
  
  api-gateway:
    build: ./backend/api-gateway
    ports:
      - "8080:8080"
    depends_on:
      - eureka-server
  
  user-service:
    build: ./backend/user-service
    ports:
      - "8099:8099"
    depends_on:
      - postgres
      - eureka-server
  
  # ... other services
  
  ai-service:
    build: ./backend/ai-service
    ports:
      - "8001:8001"
  
  frontend:
    build: ./frontend
    ports:
      - "4200:4200"
```

### Production Deployment

**AWS ECS**:
```
Load Balancer (ALB)
    ↓
ECS Cluster
├─ API Gateway Task
├─ User Service Task
├─ Crop Service Task
├─ Mandi Service Task
├─ Weather Service Task
├─ Scheme Service Task
├─ Location Service Task
├─ Yield Service Task
├─ Admin Service Task
├─ Eureka Server Task
└─ AI Service Task
    ↓
RDS PostgreSQL
    ↓
ElastiCache Redis
    ↓
S3 (Documents)
```

**Kubernetes**:
```
Ingress (API Gateway)
    ↓
Services
├─ user-service
├─ crop-service
├─ mandi-service
├─ weather-service
├─ scheme-service
├─ location-service
├─ yield-service
├─ admin-service
├─ eureka-server
└─ ai-service
    ↓
Deployments (Pods)
    ↓
Persistent Volumes
├─ PostgreSQL
└─ Redis
```

---

## Security Architecture

### Authentication

- **JWT-based authentication**
- **Token expiration**: 24 hours
- **Refresh token**: 7 days
- **Algorithm**: HS256

### Authorization

- **Role-based access control (RBAC)**
- **Roles**: FARMER, ADMIN, SUPER_ADMIN
- **Permissions**: Defined per endpoint

### Data Security

- **Password hashing**: BCrypt with salt
- **Data encryption**: AES-256 at rest
- **TLS 1.3**: For data in transit
- **API key management**: Environment variables

### API Security

- **Rate limiting**: 100 requests/minute per user
- **CORS**: Configured for frontend domain
- **CSRF protection**: Token-based
- **Input validation**: All inputs validated
- **SQL injection prevention**: Parameterized queries

### Infrastructure Security

- **Network isolation**: VPC with security groups
- **Secrets management**: AWS Secrets Manager
- **Audit logging**: All actions logged
- **Compliance**: DPDP Act compliance (right to be forgotten)

---

## Scalability & Performance

### Horizontal Scaling

- **Stateless services**: Can be scaled horizontally
- **Load balancing**: API Gateway distributes requests
- **Service discovery**: Eureka manages service instances
- **Database replication**: PostgreSQL read replicas

### Caching Strategy

- **Redis caching**: Frequently accessed data
- **Cache TTL**: 1 hour for recommendations, 24 hours for reference data
- **Cache invalidation**: Event-based invalidation

### Performance Optimization

- **Connection pooling**: HikariCP with 10 connections
- **Async processing**: WebFlux for non-blocking I/O
- **Pagination**: Large result sets paginated
- **Indexing**: Database indexes on frequently queried columns
- **CDN**: Static assets served from CDN

### Monitoring & Observability

- **Metrics**: Prometheus metrics exposed
- **Logging**: Structured logging with ELK stack
- **Tracing**: Distributed tracing with Jaeger
- **Alerting**: CloudWatch alarms for critical metrics

---

## Future Enhancements

1. **Event-driven architecture**: Kafka/RabbitMQ for async communication
2. **Microservices mesh**: Istio for service-to-service communication
3. **GraphQL API**: Alternative to REST API
4. **Real-time updates**: WebSocket for live data
5. **Machine learning pipeline**: Automated model training and deployment
6. **Advanced analytics**: Data warehouse and BI tools
7. **Mobile app**: Native iOS and Android apps
8. **Blockchain integration**: For supply chain transparency
