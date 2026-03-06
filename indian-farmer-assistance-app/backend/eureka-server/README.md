# Eureka Server

The Eureka Server serves as the service discovery mechanism for the Indian Farmer Assistance Application, allowing microservices to find and communicate with each other.

## 🚀 Overview

- **Port:** 8761
- **Technology Stack:** Java (Spring Boot), Spring Cloud Netflix Eureka
- **Primary Purpose:** Service registration and discovery for maintaining a dynamic microservices ecosystem.

## 🛠️ Key Features

- **Service Registration:** All backend services (User, Crop, Weather, etc.) register themselves with Eureka on startup.
- **Service Discovery:** Services use Eureka to locate other services without hardcoding IP addresses.
- **Health Monitoring:** Monitors the status of registered service instances.
- **Dashboard:** Provides a web-based UI at `http://localhost:8761` to view all active service instances.

## 📋 Configuration

The service can be configured via `application.yml`. In a local environment, it typically runs as a standalone server.

## 📚 Further Reading

For more detailed technical documentation, please refer to:
- [API Gateway & Eureka Documentation](../../documentations/services/API_GATEWAY_EUREKA.md)
