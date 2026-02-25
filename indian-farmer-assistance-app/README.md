# Indian Farmer Assistance Application

A comprehensive, mobile-first platform for Indian farmers built on a microservices architecture. The application integrates with government Digital Public Infrastructure (DPI) services including IMD (India Meteorological Department), AGMARKNET, AgriStack, and Bhashini to provide farmers with real-time agricultural intelligence.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend Layer                            │
│  ┌─────────────────┐    ┌─────────────────────────────────┐    │
│  │  Angular Web    │    │  Progressive Web App (PWA)      │    │
│  │  Application    │    │  with offline support           │    │
│  └────────┬────────┘    └─────────────────────────────────┘    │
└───────────┼─────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API Gateway Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐     │
│  │  Spring     │  │  Auth &     │  │  Rate Limiter       │     │
│  │  Cloud      │  │  Security   │  │  & Throttling       │     │
│  │  Gateway    │  │             │  │                     │     │
│  └─────────────┘  └─────────────┘  └─────────────────────┘     │
└───────────┬─────────────────────────────────────────────────────┘
            │
    ┌───────┼───────┬───────┬───────┬───────┬───────┬───────┐
    │       │       │       │       │       │       │       │
    ▼       ▼       ▼       ▼       ▼       ▼       ▼       ▼
┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
│ User │ │Weather│ │ Crop │ │Scheme│ │ Mandi│ │  IoT │ │Admin │
│ Svc  │ │ Svc  │ │ Svc  │ │ Svc  │ │ Svc  │ │ Svc  │ │ Svc  │
└──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘
    │       │       │       │       │       │       │
    └───────┴───────┴───────┴───────┴───────┴───────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Data Layer                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐     │
│  │   MySQL     │  │  MongoDB    │  │      Redis          │     │
│  │  (Primary)  │  │  (Vectors)  │  │      (Cache)        │     │
│  └─────────────┘  └─────────────┘  └─────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

## Technology Stack

### Frontend
- **Framework**: Angular 17+ with standalone components
- **State Management**: RxJS
- **PWA**: Service Worker for offline support
- **Styling**: SCSS with responsive design

### Backend Services (Java/Spring Boot)
- **Framework**: Spring Boot 3.2
- **API Gateway**: Spring Cloud Gateway
- **Service Discovery**: Spring Cloud Netflix Eureka
- **Circuit Breaker**: Resilience4j
- **Database**: MySQL 8.0 with JPA/Hibernate
- **Caching**: Redis

### AI/ML Service (Python)
- **Framework**: FastAPI
- **ML Libraries**: PyTorch, Transformers, Sentence-Transformers
- **Vector Database**: MongoDB with vector search
- **Voice Processing**: Bhashini API integration

### Infrastructure
- **Containerization**: Docker
- **Orchestration**: Docker Compose (local), Kubernetes (production)
- **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Monitoring**: Prometheus, Grafana

## Project Structure

```
indian-farmer-assistance-app/
├── backend/
│   ├── pom.xml                          # Parent Maven POM
│   ├── common/                          # Shared utilities
│   │   └── src/main/java/com/farmer/common/
│   ├── api-gateway/                     # API Gateway service
│   │   ├── src/main/java/com/farmer/apigateway/
│   │   └── src/main/resources/
│   │       ├── application.yml
│   │       ├── logback-spring.xml
│   │       └── config/
│   │           ├── application-dev.yml
│   │           ├── application-staging.yml
│   │           └── application-prod.yml
│   ├── user-service/                    # User management
│   ├── weather-service/                 # IMD API integration
│   ├── crop-service/                    # Crop recommendations
│   ├── scheme-service/                  # Government schemes
│   ├── mandi-service/                   # AGMARKNET integration
│   ├── iot-service/                     # IoT device management
│   └── admin-service/                   # Admin functionality
│
├── frontend/
│   ├── package.json                     # Angular dependencies
│   ├── angular.json                     # Angular configuration
│   ├── tsconfig.json                    # TypeScript config
│   └── src/
│       ├── main.ts                      # Application entry
│       ├── index.html                   # HTML template
│       ├── styles.scss                  # Global styles
│       ├── app/
│       │   ├── app.component.ts         # Root component
│       │   ├── app.config.ts            # App configuration
│       │   ├── app.routes.ts            # Route definitions
│       │   └── pages/                   # Page components
│       ├── environments/                # Environment configs
│       └── manifest.webmanifest         # PWA manifest
│
├── ai-service/
│   ├── requirements.txt                 # Python dependencies
│   ├── config.yaml                      # Service configuration
│   ├── Dockerfile                       # Container image
│   └── app/
│       ├── main.py                      # FastAPI application
│       ├── routers/                     # API endpoints
│       ├── services/                    # Business logic
│       └── models/                      # Data models
│
└── infrastructure/
    ├── docker-compose.yml               # Local development
    ├── redis/
    │   └── redis.conf                   # Redis configuration
    ├── mongodb/
    │   └── mongod.conf                  # MongoDB configuration
    └── mysql/
        └── init-scripts/               # Database initialization
```

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- Python 3.11+
- Docker & Docker Compose
- Maven 3.8+

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd indian-farmer-assistance-app
   ```

2. **Start infrastructure services**
   ```bash
   cd infrastructure
   docker-compose up -d
   ```

3. **Build and run backend services**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run -pl api-gateway
   # Run other services in separate terminals
   ```

4. **Build and run frontend**
   ```bash
   cd frontend
   npm install
   npm start
   ```

5. **Build and run AI service**
   ```bash
   cd ai-service
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   pip install -r requirements.txt
   python -m app.main
   ```

### Environment Variables

Create `.env` files based on the examples:

```bash
# Backend
export MYSQL_PASSWORD=your_password
export REDIS_PASSWORD=your_password
export JWT_SECRET=your_jwt_secret

# AI Service
export MONGODB_URI=mongodb://localhost:27017/farmer_dev
export BHASHINI_API_KEY=your_api_key
```

## API Documentation

- **API Gateway**: http://localhost:8080/actuator/gateway/routes
- **Swagger UI**: Available on each service (dev profile)
- **API Docs**: http://localhost:8080/docs

## Testing

```bash
# Backend unit tests
cd backend
mvn test

# Frontend tests
cd frontend
npm test

# AI service tests
cd ai-service
pytest tests/
```

## Deployment

### Docker Production Build

```bash
# Build all services
docker-compose -f docker-compose.prod.yml build

# Deploy
docker-compose -f docker-compose.prod.yml up -d
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, please contact the development team or create an issue in the repository.