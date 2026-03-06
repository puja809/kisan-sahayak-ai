# Indian Farmer Assistance App - Complete Documentation Index

## 📚 Documentation Overview

This comprehensive documentation covers all aspects of the Indian Farmer Assistance App, including architecture, services, deployment, and system flows.

---

## 🏗️ Architecture & Design

### [ARCHITECTURE.md](./ARCHITECTURE.md)
Complete system architecture including:
- Architecture overview and diagrams
- System components breakdown
- Data flow patterns
- Service communication
- Technology stack
- Deployment architecture
- Security architecture
- Scalability and performance considerations

### [DEPLOYMENT_ECS.md](./DEPLOYMENT_ECS.md)
AWS ECS deployment guide including:
- ECS cluster configuration
- Fargate task definitions
- Load balancing setup
- Service discovery with Cloud Map
- Security configuration
- Environment variables
- Logging and monitoring
- Auto-scaling policies
- Cost optimization
- Disaster recovery

---

## 🔄 System Flows

### [SYSTEM_FLOWS.md](./SYSTEM_FLOWS.md)
Detailed flow diagrams and step-by-step processes for:
- User registration and authentication
- Crop recommendation workflow
- Market price search
- Weather advisory generation
- Disease detection from images
- Voice assistant interaction
- Yield calculation
- Government scheme search

---

## 🛠️ Service Documentation

### Backend Services

#### [USER_SERVICE.md](./services/USER_SERVICE.md)
**Port: 8099** - Authentication and user management
- User registration and login
- JWT token management
- Profile management
- Role-based access control
- API endpoints and models
- Security features

#### [CROP_SERVICE.md](./services/CROP_SERVICE.md)
**Port: 8093** - Crop recommendations and planning
- Crop recommendation engine
- Crop rotation planning
- Fertilizer recommendations
- ML model integration
- Weather and soil data integration
- MCP tools for AI agents

#### [MANDI_SERVICE.md](./services/MANDI_SERVICE.md)
**Port: 8096** - Agricultural market data
- Market price information
- Commodity data management
- Fertilizer supplier locator
- AGMARKNET API integration
- data.gov.in API integration
- Price alerts and notifications

#### [WEATHER_SERVICE.md](./services/WEATHER_SERVICE.md)
**Port: 8100** - Weather forecasts and advisories
- Real-time weather data
- 7-day forecasts
- Agromet advisories
- Weather alerts
- IMD API integration
- weatherapi.com integration

#### [MANDI_SCHEME_LOCATION_YIELD_SERVICES.md](./services/MANDI_SCHEME_LOCATION_YIELD_SERVICES.md)
Multiple services documentation:
- **Scheme Service (8097)**: Government schemes and eligibility
- **Location Service (8095)**: Government body locator
- **Yield Service (8094)**: Yield prediction and revenue calculation
- **Admin Service (8091)**: Document management and analytics

#### [API_GATEWAY_EUREKA.md](./services/API_GATEWAY_EUREKA.md)
Infrastructure services:
- **API Gateway (8080)**: Request routing and authentication
- **Eureka Server (8761)**: Service discovery and registration

### AI/ML Services

#### [AI_SERVICE.md](./services/AI_SERVICE.md)
**Port: 8001** - Python FastAPI ML service
- Crop recommendation model
- Crop rotation model
- Fertilizer recommendation model
- Voice assistant integration
- Disease detection
- AWS Bedrock integration
- Multilingual support

#### [LAMBDA_RAG_SERVICE.md](./services/LAMBDA_RAG_SERVICE.md)
**AWS Lambda** - Retrieval-Augmented Generation
- RAG implementation with Bedrock Knowledge Base
- Voice processing (transcription and synthesis)
- Disease detection from images
- Chat memory management
- Multilingual support (10+ languages)
- Vision model integration

---

## 📋 Service Inventory

| Service | Port | Language | Purpose |
|---------|------|----------|---------|
| API Gateway | 8080 | Java | Request routing, authentication |
| User Service | 8099 | Java | Authentication, profiles |
| Crop Service | 8093 | Java | Crop recommendations |
| Mandi Service | 8096 | Java | Market prices |
| Weather Service | 8100 | Java | Weather forecasts |
| Scheme Service | 8097 | Java | Government schemes |
| Location Service | 8095 | Java | Government bodies |
| Yield Service | 8094 | Java | Yield prediction |
| Admin Service | 8091 | Java | Document management |
| Eureka Server | 8761 | Java | Service discovery |
| AI Service | 8001 | Python | ML models |
| Lambda RAG | - | Python | Voice & disease detection |

---

## 🔐 Security

### Authentication & Authorization
- JWT-based authentication (24-hour tokens)
- Role-based access control (FARMER, ADMIN, SUPER_ADMIN)
- Refresh token mechanism (7-day validity)
- Password hashing with BCrypt

### Data Security
- AES-256 encryption at rest
- TLS 1.3 for data in transit
- API key management via environment variables
- Secrets stored in AWS Secrets Manager

### API Security
- Rate limiting (100 requests/minute per user)
- CORS configuration
- CSRF protection
- Input validation
- SQL injection prevention

---

## 📊 Data Models

### Core Entities
- **User**: Farmer/admin accounts with roles
- **Crop**: User's crops with cultivation details
- **Recommendation**: Crop, rotation, and fertilizer recommendations
- **MandiPrice**: Market price data
- **Weather**: Current and forecast weather data
- **Scheme**: Government scheme information
- **GovernmentBody**: KVK and agricultural office locations
- **YieldCalculator**: Yield prediction parameters

### External Data
- AGMARKNET: Commodity prices
- data.gov.in: Mandi and fertilizer data
- IMD: Weather forecasts
- weatherapi.com: Backup weather data
- Bhashini: Multilingual translation

---

## 🚀 Deployment

### Local Development
```bash
docker-compose up
# Services available at:
# - Frontend: http://localhost:4200
# - API Gateway: http://localhost:8080
# - Eureka: http://localhost:8761
```

### AWS ECS Deployment
```bash
aws cloudformation create-stack \
  --stack-name farmer-ecs-stack \
  --template-body file://aws-ecs-template.yml \
  --parameters ParameterKey=VpcId,ParameterValue=vpc-xxxxx
```

### Key Infrastructure
- **Compute**: AWS ECS Fargate (serverless containers)
- **Database**: Amazon RDS PostgreSQL
- **Cache**: Amazon ElastiCache Redis
- **Storage**: Amazon S3
- **AI/ML**: AWS Bedrock, Lambda
- **Load Balancing**: Application Load Balancer
- **Service Discovery**: AWS Cloud Map
- **Logging**: CloudWatch Logs

---

## 📈 Performance & Scalability

### Caching Strategy
- Redis for session management
- 1-hour TTL for recommendations
- 24-hour TTL for reference data
- Event-based cache invalidation

### Database Optimization
- Connection pooling (HikariCP)
- Indexed queries
- Read replicas for scaling
- Automated backups

### API Performance
- Async processing with WebFlux
- Pagination for large result sets
- Response compression
- CDN for static assets

### Auto-Scaling
- Target tracking on CPU utilization
- Min/max capacity per service
- Scale-out cooldown: 60 seconds
- Scale-in cooldown: 300 seconds

---

## 🔍 Monitoring & Observability

### Metrics
- Request latency
- Error rate
- CPU/memory utilization
- Database connection pool
- Cache hit rate
- API throughput

### Logging
- Structured logging with timestamps
- CloudWatch Logs integration
- 30-day retention
- Log levels: DEBUG, INFO, WARN, ERROR

### Alerting
- High CPU utilization (> 80%)
- High memory utilization (> 80%)
- Task failures
- Service unhealthy
- Database errors

---

## 🌐 External Integrations

### Government APIs
- **AGMARKNET**: Agricultural commodity prices
- **data.gov.in**: Mandi prices and fertilizer suppliers
- **IMD**: India Meteorological Department weather data

### Third-party Services
- **weatherapi.com**: Backup weather data
- **Bhashini**: Multilingual translation
- **AWS Bedrock**: LLM for voice assistant
- **AWS Lambda**: Disease detection
- **AWS Transcribe**: Speech-to-text
- **AWS Polly**: Text-to-speech

---

## 📱 Frontend

### Technology Stack
- Angular 17+
- TypeScript
- RxJS
- Angular Material
- Bootstrap 5

### Key Features
- Responsive design
- Real-time updates
- Offline support
- Progressive Web App (PWA)
- Multilingual UI

### Pages
- Dashboard
- Crop Management
- Recommendations
- Market Prices
- Weather
- Schemes
- Government Bodies
- Yield Calculator
- Disease Detection
- Voice Assistant
- Admin Dashboard

---

## 🔧 Development

### Prerequisites
- Java 17+
- Python 3.11+
- Node.js 18+
- Docker & Docker Compose
- AWS CLI
- Maven 3.8+

### Build & Run

**Backend Services**:
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

**AI Service**:
```bash
cd backend/ai-service
pip install -r requirements.txt
uvicorn app.ml_service:app --reload
```

**Frontend**:
```bash
cd frontend
npm install
ng serve
```

---

## 📚 Additional Resources

### Configuration Files
- `.env`: Environment variables
- `docker-compose.yml`: Local development setup
- `aws-ecs-template.yml`: AWS ECS deployment
- `pom.xml`: Maven dependencies
- `requirements.txt`: Python dependencies

### Documentation Files
- `README.md`: Project overview
- `DEPLOYMENT_GUIDE.md`: Deployment instructions
- Individual service README files

---

## 🎯 Quick Links

### Getting Started
1. Read [ARCHITECTURE.md](./ARCHITECTURE.md) for system overview
2. Review [SYSTEM_FLOWS.md](./SYSTEM_FLOWS.md) for key workflows
3. Check service documentation for specific services
4. Follow [DEPLOYMENT_ECS.md](./DEPLOYMENT_ECS.md) for deployment

### For Developers
1. Start with [USER_SERVICE.md](./services/USER_SERVICE.md) for authentication
2. Review [CROP_SERVICE.md](./services/CROP_SERVICE.md) for ML integration
3. Check [AI_SERVICE.md](./services/AI_SERVICE.md) for ML models
4. Review [LAMBDA_RAG_SERVICE.md](./services/LAMBDA_RAG_SERVICE.md) for RAG

### For DevOps
1. Read [DEPLOYMENT_ECS.md](./DEPLOYMENT_ECS.md) for infrastructure
2. Review security configuration in [ARCHITECTURE.md](./ARCHITECTURE.md)
3. Check monitoring setup in [DEPLOYMENT_ECS.md](./DEPLOYMENT_ECS.md)
4. Review auto-scaling policies

### For Product Managers
1. Review [SYSTEM_FLOWS.md](./SYSTEM_FLOWS.md) for user journeys
2. Check service capabilities in individual service docs
3. Review [ARCHITECTURE.md](./ARCHITECTURE.md) for system capabilities

---

## 📞 Support & Troubleshooting

### Common Issues

**Service not starting**:
- Check environment variables
- Verify database connectivity
- Review CloudWatch logs

**High latency**:
- Check database performance
- Review cache hit rates
- Monitor API response times

**Memory issues**:
- Review task memory allocation
- Check for memory leaks
- Monitor garbage collection

**Network errors**:
- Verify security group rules
- Check service discovery
- Review DNS resolution

---

## 📝 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2024-03-06 | Initial documentation |

---

## 📄 License

This documentation is part of the Indian Farmer Assistance App project.

---

## 🤝 Contributing

To contribute to this documentation:
1. Update relevant markdown files
2. Maintain consistent formatting
3. Update this index if adding new documents
4. Submit pull request for review

---

**Last Updated**: March 6, 2024  
**Maintained By**: Development Team  
**Status**: Active
