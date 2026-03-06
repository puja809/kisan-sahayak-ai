# Indian Farmer Assistance App - Services Documentation

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Service Inventory](#service-inventory)
3. [Individual Service Documentation](#individual-service-documentation)
4. [Service Communication](#service-communication)
5. [Data Flow](#data-flow)
6. [Deployment](#deployment)

---

## Architecture Overview

The Indian Farmer Assistance App uses a **microservices architecture** with 11 Java Spring Boot services + 1 Python AI service, coordinated through Eureka service discovery and API Gateway.

### Key Characteristics:
- **Service Discovery**: Eureka Server (Port 8761)
- **API Gateway**: Spring Cloud Gateway (Port 8080)
- **Authentication**: JWT-based with role-based access control
- **Data Persistence**: PostgreSQL (primary), Redis (caching), MongoDB (embeddings)
- **External Integrations**: AWS services, Government APIs, Weather APIs
- **AI/ML**: Python FastAPI service with scikit-learn models and AWS Bedrock LLM

---

## Service Inventory

| Service | Port | Language | Purpose |
|---------|------|----------|---------|
| API Gateway | 8080 | Java | Request routing, authentication, rate limiting |
| User Service | 8099 | Java | Authentication, profiles, role management |
| Weather Service | 8100 | Java | Weather forecasts, agromet advisories |
| Crop Service | 8093 | Java | Crop recommendations, rotation planning |
| Scheme Service | 8097 | Java | Government schemes, eligibility |
| Mandi Service | 8096 | Java | Market prices, commodity data |
| Location Service | 8095 | Java | Government body locator, geocoding |
| Admin Service | 8091 | Java | Document management, analytics |
| Yield Service | 8094 | Java | Yield prediction, revenue calculation |
| Eureka Server | 8761 | Java | Service discovery and registration |
| AI Service | 8001 | Python | ML models, voice assistant, disease detection |

---

## Individual Service Documentation

