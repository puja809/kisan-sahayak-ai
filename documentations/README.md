# Indian Farmer Assistance App - Complete Documentation

Welcome to the comprehensive documentation for the Indian Farmer Assistance App. This documentation covers all aspects of the application including architecture, services, deployment, and system flows.

## 📖 Documentation Structure

```
documentations/
├── README.md (this file)
├── INDEX.md (complete index of all documentation)
├── ARCHITECTURE.md (system architecture and design)
├── DEPLOYMENT_ECS.md (AWS ECS deployment guide)
├── SYSTEM_FLOWS.md (detailed workflow diagrams)
├── SERVICES_DOCUMENTATION.md (services overview)
└── services/
    ├── USER_SERVICE.md
    ├── CROP_SERVICE.md
    ├── MANDI_SERVICE.md
    ├── WEATHER_SERVICE.md
    ├── MANDI_SCHEME_LOCATION_YIELD_SERVICES.md
    ├── AI_SERVICE.md
    ├── LAMBDA_RAG_SERVICE.md
    └── API_GATEWAY_EUREKA.md
```

## 🚀 Quick Start

### For New Developers
1. Start with [ARCHITECTURE.md](./ARCHITECTURE.md) to understand the system
2. Review [SYSTEM_FLOWS.md](./SYSTEM_FLOWS.md) to see how components interact
3. Read the specific service documentation for the service you'll be working on
4. Check [DEPLOYMENT_ECS.md](./DEPLOYMENT_ECS.md) for deployment details

### For DevOps Engineers
1. Read [DEPLOYMENT_ECS.md](./DEPLOYMENT_ECS.md) for infrastructure setup
2. Review security configuration in [ARCHITECTURE.md](./ARCHITECTURE.md)
3. Check monitoring and logging setup
4. Review auto-scaling and disaster recovery procedures

### For Product Managers
1. Review [SYSTEM_FLOWS.md](./SYSTEM_FLOWS.md) for user journeys
2. Check [ARCHITECTURE.md](./ARCHITECTURE.md) for system capabilities
3. Review individual service documentation for feature details

## 🏗️ System Overview

The Indian Farmer Assistance App is a comprehensive microservices-based platform designed to help Indian farmers with:

- **Crop Recommendations**: AI-powered crop suggestions based on soil and weather
- **Market Information**: Real-time agricultural market prices
- **Weather Forecasts**: Accurate weather predictions and agromet advisories
- **Government Schemes**: Information about agricultural subsidies and schemes
- **Yield Prediction**: Estimate crop yield and revenue
- **Disease Detection**: Identify crop diseases from images
- **Voice Assistant**: Ask questions in local languages
- **Government Services**: Locate KVKs and agricultural offices

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.8
- **Language**: Java 17
- **Database**: PostgreSQL
- **Cache**: Redis
- **Service Discovery**: Eureka
- **API Gateway**: Spring Cloud Gateway

### Frontend
- **Framework**: Angular 17+
- **Language**: TypeScript
- **UI Library**: Angular Material, Bootstrap 5

### AI/ML
- **Framework**: FastAPI
- **Smart Agent**: **Bedrock MCP Agent** (LangGraph + Tool Calling)
- **Language**: Python 3.11+
- **ML Libraries**: scikit-learn, pandas, numpy
- **LLM**: AWS Bedrock (Llama 3 / Nova Pro)
- **RAG**: Bedrock Knowledge Base

### Infrastructure
- **Containerization**: Docker
- **Orchestration**: AWS ECS Fargate
- **Load Balancing**: Application Load Balancer
- **Service Discovery**: AWS Cloud Map
- **Logging**: CloudWatch Logs
- **Storage**: Amazon S3

## 📊 Services

### Core Services (11 Java Services)

| Service | Port | Purpose |
|---------|------|---------|
| API Gateway | 8080 | Request routing and authentication |
| User Service | 8099 | User management and authentication |
| Crop Service | 8093 | Crop recommendations and planning |
| Mandi Service | 8096 | Market prices and commodities |
| Weather Service | 8100 | Weather forecasts and advisories |
| Scheme Service | 8097 | Government schemes |
| Location Service | 8095 | Government body locator |
| Yield Service | 8094 | Yield prediction |
| Admin Service | 8091 | Document management |
| Eureka Server | 8761 | Service discovery |

### AI/ML Services

| Service | Port | Purpose |
|---------|------|---------|
| AI Service | 8001 | ML models & Bedrock MCP Agent |
| Lambda RAG | - | Multi-region Voice & Disease Detection |

## 🔐 Security Features

- **Authentication**: JWT-based with 24-hour tokens
- **Authorization**: Role-based access control (FARMER, ADMIN, SUPER_ADMIN)
- **Encryption**: AES-256 at rest, TLS 1.3 in transit
- **Rate Limiting**: 100 requests/minute per user
- **Input Validation**: All inputs validated
- **SQL Injection Prevention**: Parameterized queries

## 📈 Key Metrics

- **Services**: 11 Java + 1 Python + 1 Lambda
- **Yield Prediction**: 5+ regional models
- **Supported Languages**: 10 major Indian languages (High-fidelity Script & Voice)
- **API Endpoints**: 110+
- **Database Tables**: 50+
- **ML Models**: 4 (Crop Recommendation, Rotation, Fertilizer, Disease)
- **External APIs**: 6+ (AGMARKNET, data.gov.in, IMD, Nominatim, AWS Bedrock)

## 🚀 Deployment

### Local Development
```bash
docker-compose up
```

### AWS ECS
```bash
aws cloudformation create-stack \
  --stack-name farmer-ecs-stack \
  --template-body file://aws-ecs-template.yml
```

## 📚 Documentation Files

### Architecture & Design
- **[ARCHITECTURE.md](./ARCHITECTURE.md)**: Complete system architecture
- **[DEPLOYMENT_ECS.md](./DEPLOYMENT_ECS.md)**: AWS ECS deployment guide

### Workflows
- **[SYSTEM_FLOWS.md](./SYSTEM_FLOWS.md)**: Detailed system flows and diagrams

### Services
- **[USER_SERVICE.md](./services/USER_SERVICE.md)**: Authentication and user management
- **[CROP_SERVICE.md](./services/CROP_SERVICE.md)**: Crop recommendations
- **[MANDI_SERVICE.md](./services/MANDI_SERVICE.md)**: Market data
- **[WEATHER_SERVICE.md](./services/WEATHER_SERVICE.md)**: Weather forecasts
- **[MANDI_SCHEME_LOCATION_YIELD_SERVICES.md](./services/MANDI_SCHEME_LOCATION_YIELD_SERVICES.md)**: Multiple services
- **[AI_SERVICE.md](./services/AI_SERVICE.md)**: ML models
- **[LAMBDA_RAG_SERVICE.md](./services/LAMBDA_RAG_SERVICE.md)**: Voice assistant and disease detection
- **[API_GATEWAY_EUREKA.md](./services/API_GATEWAY_EUREKA.md)**: Infrastructure services

### Index
- **[INDEX.md](./INDEX.md)**: Complete documentation index

## 🔄 Key Workflows

### 1. User Registration
```
User → Register → User Service → JWT Token → Dashboard
```

### 2. Crop Recommendation
```
User → Input Data → Crop Service → ML Service → Recommendation
```

### 3. Market Price Search
```
User → Search Criteria → Mandi Service → AGMARKNET/data.gov.in → Prices
```

### 4. Voice Assistant
```
User → Voice/Text → AI Service (Bedrock MCP) → Tools (Weather/Mandi/etc) → Smart Response
```

### 5. Multi-Language RAG
```
User Question → Lambda RAG → Bedrock Knowledge Base → Language-Specific Response
```

## 🎯 Features

### For Farmers
- ✅ Crop recommendations based on soil and weather
- ✅ Real-time market prices
- ✅ Weather forecasts and alerts
- ✅ Government scheme information
- ✅ Yield and revenue predictions
- ✅ Crop disease detection
- ✅ Voice assistant in local languages
- ✅ Government office locator

### For Administrators
- ✅ Document management
- ✅ System analytics
- ✅ User management
- ✅ Audit logs
- ✅ System configuration

## 📊 Data Flow

```
Frontend (Angular)
    ↓
API Gateway (8080)
    ↓
Microservices (8091-8100)
    ↓
PostgreSQL + Redis
    ↓
External APIs + AWS Services
```

## 🔧 Configuration

### Environment Variables
All services use environment variables for configuration:
- Database credentials
- API keys
- JWT secrets
- Service URLs
- AWS credentials

### Secrets Management
Sensitive data is stored in AWS Secrets Manager:
- Database passwords
- JWT secrets
- API keys
- AWS credentials

## 📈 Performance

### Caching
- Redis for session management
- 1-hour TTL for recommendations
- 24-hour TTL for reference data

### Database
- Connection pooling (HikariCP)
- Indexed queries
- Read replicas for scaling

### API
- Async processing with WebFlux
- Pagination for large result sets
- Response compression

## 🔍 Monitoring

### Metrics
- Request latency
- Error rate
- CPU/memory utilization
- Database performance
- Cache hit rate

### Logging
- CloudWatch Logs
- Structured logging
- 30-day retention

### Alerting
- High CPU/memory
- Service failures
- Database errors
- API errors

## 🌐 External Integrations

### Government APIs
- AGMARKNET (commodity prices)
- data.gov.in (mandi prices, fertilizer suppliers)
- IMD (weather forecasts)

### Third-party Services
- weatherapi.com (backup weather)
- Bhashini (multilingual translation)
- AWS Bedrock (LLM)
- AWS Lambda (disease detection)
- AWS Transcribe (speech-to-text)
- AWS Polly (text-to-speech)

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Python 3.11+
- Node.js 18+
- Docker & Docker Compose
- AWS CLI

### Local Setup
```bash
# Clone repository
git clone <repository-url>
cd indian-farmer-assistance-app

# Start services
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

## 📞 Support

For issues or questions:
1. Check the relevant service documentation
2. Review [SYSTEM_FLOWS.md](./SYSTEM_FLOWS.md) for workflow details
3. Check [DEPLOYMENT_ECS.md](./DEPLOYMENT_ECS.md) for deployment issues
4. Review CloudWatch logs for error details

## 📝 Contributing

To contribute to this documentation:
1. Update relevant markdown files
2. Maintain consistent formatting
3. Update [INDEX.md](./INDEX.md) if adding new documents
4. Submit pull request for review

## 📄 License

This documentation is part of the Indian Farmer Assistance App project.

## 🎓 Learning Path

### Beginner
1. Read [ARCHITECTURE.md](./ARCHITECTURE.md)
2. Review [SYSTEM_FLOWS.md](./SYSTEM_FLOWS.md)
3. Explore individual service documentation

### Intermediate
1. Study [DEPLOYMENT_ECS.md](./DEPLOYMENT_ECS.md)
2. Review security configuration
3. Understand data models

### Advanced
1. Study ML models in [AI_SERVICE.md](./services/AI_SERVICE.md)
2. Review RAG implementation in [LAMBDA_RAG_SERVICE.md](./services/LAMBDA_RAG_SERVICE.md)
3. Understand auto-scaling and disaster recovery

## 🔗 Quick Links

- [Complete Index](./INDEX.md)
- [Architecture](./ARCHITECTURE.md)
- [Deployment Guide](./DEPLOYMENT_ECS.md)
- [System Flows](./SYSTEM_FLOWS.md)
- [Services Overview](./SERVICES_DOCUMENTATION.md)

---

**Last Updated**: March 6, 2026  
**Version**: 1.1.0  
**Status**: Active (Enhanced AI & Multi-Language)

For the latest updates, check the [INDEX.md](./INDEX.md) file.
