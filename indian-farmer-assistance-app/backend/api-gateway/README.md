# API Gateway

The API Gateway is the single entry point for all requests to the Indian Farmer Assistance Application microservices. It handles request routing, authentication, and security.

## 🚀 Overview

- **Port:** 8080
- **Technology Stack:** Java (Spring Boot), Spring Cloud Gateway, JWT (AgriStack integration)
- **Primary Purpose:** Route client requests to appropriate microservices and enforce security policies.

## 🛠️ Key Features

- **Dynamic Routing:** Routes traffic to backend services (User, Weather, Crop, Mandi, etc.) using service discovery (Eureka).
- **Authentication & Security:** Validates JWT tokens and handles AgriStack identity integration.
- **Rate Limiting:** Protects the system from excessive requests.
- **CORS Configuration:** Manages Cross-Origin Resource Sharing for the Angular frontend.
- **Swagger/OpenAPI Aggregation:** Provides a centralized Swagger UI for all microservices at `http://localhost:8080/swagger-ui.html`.

## 📋 Configuration

The service requires the following environment variables:
- `EUREKA_SERVER_URL`: URL of the discovery server
- `GATEWAY_JWT_SECRET`: Secret key for JWT validation

## 📚 Further Reading

For more detailed technical documentation, please refer to:
- [API Gateway & Eureka Documentation](../../documentations/services/API_GATEWAY_EUREKA.md)
