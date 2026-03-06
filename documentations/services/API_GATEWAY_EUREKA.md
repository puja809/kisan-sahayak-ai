# API Gateway and Eureka Server Documentation

## Table of Contents
1. [API Gateway](#api-gateway)
2. [Eureka Server](#eureka-server)

---

## API Gateway

**Port:** 8080  
**Language:** Java (Spring Boot)  
**Framework:** Spring Cloud Gateway  
**Purpose:** Request routing, authentication, rate limiting, API documentation aggregation

## Overview

The API Gateway serves as the single entry point for all client requests. It handles request routing to appropriate microservices, enforces authentication, applies rate limiting, and aggregates API documentation.

## Key Responsibilities

- Request routing to microservices
- JWT authentication and validation
- Rate limiting and throttling
- Request/response transformation
- API documentation aggregation
- Load balancing
- Circuit breaking
- Request logging and monitoring

## Architecture

```
Client (Frontend/Mobile)
    ↓
API Gateway (8080)
    ├─→ User Service (8099)
    ├─→ Crop Service (8093)
    ├─→ Mandi Service (8096)
    ├─→ Weather Service (8100)
    ├─→ Scheme Service (8097)
    ├─→ Location Service (8095)
    ├─→ Yield Service (8094)
    ├─→ Admin Service (8091)
    └─→ AI Service (8001)
```

## API Routes

### Service Discovery Routes

The API Gateway uses Eureka service discovery to dynamically route requests:

| Service | Route | Port |
|---------|-------|------|
| User Service | `/user-service/**` | 8099 |
| Crop Service | `/crop-service/**` | 8093 |
| Mandi Service | `/mandi-service/**` | 8096 |
| Weather Service | `/weather-service/**` | 8100 |
| Scheme Service | `/scheme-service/**` | 8097 |
| Location Service | `/location-service/**` | 8095 |
| Yield Service | `/yield-service/**` | 8094 |
| Admin Service | `/admin-service/**` | 8091 |
| AI Service | `/ai-service/**` | 8001 |

### Public Routes (No Authentication Required)

```
POST   /user-service/api/v1/auth/register
POST   /user-service/api/v1/auth/login
POST   /user-service/api/v1/auth/admin-login
GET    /api-docs
GET    /swagger-ui.html
```

### Protected Routes (Authentication Required)

All other routes require valid JWT token in `Authorization` header:

```
Authorization: Bearer <jwt-token>
```

## Request Flow

```
1. Client sends request to API Gateway
   ↓
2. Gateway checks if route is public
   ├─ If public → Forward to service
   └─ If protected → Validate JWT token
   ↓
3. Token validation
   ├─ Valid → Extract user info, forward request
   └─ Invalid/Expired → Return 401 Unauthorized
   ↓
4. Rate limiting check
   ├─ Within limit → Forward to service
   └─ Exceeded → Return 429 Too Many Requests
   ↓
5. Route to appropriate service using Eureka
   ↓
6. Service processes request
   ↓
7. Gateway returns response to client
```

## Configuration

### Environment Variables

```
GATEWAY_SERVICE_PORT=8080
SPRING_PROFILES_ACTIVE=prod
EUREKA_SERVER_URL=http://localhost:8761/eureka
JWT_SECRET=<256-bit-secret-key>
RATE_LIMIT_REQUESTS=100
RATE_LIMIT_WINDOW=60000
```

### Gateway Configuration (application.yml)

```yaml
server:
  port: ${GATEWAY_SERVICE_PORT:8080}

spring:
  application:
    name: api-gateway
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:prod}
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        # Routes are auto-discovered from Eureka
        # Format: /{service-name}/**
```

## Core Components

### GatewayFilter

Implements custom filtering logic:

```java
public class JwtAuthenticationFilter implements GatewayFilter {
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Extract JWT token from Authorization header
        // Validate token signature and expiration
        // Extract user claims
        // Add user info to request headers
        // Forward to service
    }
}
```

### RateLimitingFilter

Implements rate limiting:

```java
public class RateLimitingFilter implements GatewayFilter {
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Check request count for user/IP
        // If exceeded limit → Return 429
        // Otherwise → Forward request
    }
}
```

### RequestLoggingFilter

Logs all requests:

```java
public class RequestLoggingFilter implements GatewayFilter {
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Log request method, path, headers
        // Log response status, latency
        // Store in audit log
    }
}
```

## Authentication Flow

```
1. Client sends credentials to /user-service/api/v1/auth/login
   ↓
2. User Service validates credentials
   ↓
3. User Service generates JWT token
   ↓
4. Client receives JWT token
   ↓
5. Client includes token in Authorization header for subsequent requests
   ↓
6. API Gateway validates token
   ↓
7. If valid → Forward request with user info
   If invalid → Return 401 Unauthorized
```

## Rate Limiting

- **Default Limit**: 100 requests per minute per user
- **Burst Limit**: 200 requests per minute
- **Window**: 60 seconds
- **Response**: 429 Too Many Requests when exceeded

## Error Handling

| Status | Meaning | Response |
|--------|---------|----------|
| 400 | Bad Request | Invalid request format |
| 401 | Unauthorized | Missing/invalid JWT token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Service/endpoint not found |
| 429 | Too Many Requests | Rate limit exceeded |
| 502 | Bad Gateway | Service unavailable |
| 503 | Service Unavailable | Gateway unavailable |

## Monitoring & Observability

- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`
- **Request Logging**: All requests logged with timestamp, method, path, status
- **Performance Metrics**: Request latency, throughput, error rate
- **Distributed Tracing**: Optional integration with Jaeger/Zipkin

## Dependencies

- Spring Boot 3.5.8
- Spring Cloud Gateway
- Spring Cloud Eureka Client
- Spring Security
- JJWT 0.11.5
- Lombok
- SpringDoc OpenAPI 2.0.4

## Deployment

- **Docker**: Dockerfile available
- **Port**: 8080
- **Health Check**: `/actuator/health`
- **Startup**: Waits for Eureka Server to be available

## Common Use Cases

1. **User Registration**
   - POST `http://localhost:8080/user-service/api/v1/auth/register`
   - No authentication required

2. **User Login**
   - POST `http://localhost:8080/user-service/api/v1/auth/login`
   - Returns JWT token

3. **Get Crop Recommendation**
   - POST `http://localhost:8080/crop-service/api/v1/crops/recommendations`
   - Requires JWT token in Authorization header

4. **Search Market Prices**
   - POST `http://localhost:8080/mandi-service/api/v1/mandi/filter/search`
   - Requires JWT token

## Performance Considerations

- Async request processing using WebFlux
- Connection pooling for service communication
- Caching of service discovery information
- Load balancing across service instances

## Future Enhancements

- API versioning support
- Request/response compression
- GraphQL gateway
- WebSocket support
- Advanced analytics and monitoring
- API key authentication
- OAuth2/OpenID Connect integration

---

## Eureka Server

**Port:** 8761  
**Language:** Java (Spring Boot)  
**Framework:** Spring Cloud Eureka Server  
**Purpose:** Service discovery and registration

## Overview

The Eureka Server is the central service registry for the microservices architecture. All services register themselves with Eureka, and the API Gateway uses Eureka to discover and route requests to services.

## Key Responsibilities

- Service registration and deregistration
- Service discovery
- Health monitoring
- Load balancing information
- Service instance metadata management

## Architecture

```
Microservices
    ↓
Eureka Server (8761)
    ↓
API Gateway
    ↓
Clients
```

## Service Registration

### Registration Process

```
1. Service starts up
   ↓
2. Service sends registration request to Eureka
   - Service name
   - Instance ID
   - IP address
   - Port
   - Health check URL
   - Metadata
   ↓
3. Eureka stores service information
   ↓
4. Service sends heartbeat every 30 seconds
   ↓
5. Eureka marks service as UP
```

### Service Information

Each service registers with:

```json
{
  "serviceName": "user-service",
  "instanceId": "user-service:8099",
  "ipAddress": "192.168.1.100",
  "port": 8099,
  "healthCheckUrl": "http://192.168.1.100:8099/actuator/health",
  "statusPageUrl": "http://192.168.1.100:8099/actuator/info",
  "homePageUrl": "http://192.168.1.100:8099/",
  "metadata": {
    "version": "1.0.0",
    "environment": "production"
  }
}
```

## Service Discovery

### Discovery Process

```
1. API Gateway requests service list from Eureka
   ↓
2. Eureka returns list of available instances
   ↓
3. API Gateway caches service information
   ↓
4. API Gateway routes requests to available instances
   ↓
5. If instance becomes unavailable:
   - Eureka detects missed heartbeat
   - Marks instance as DOWN
   - Removes from service list
   - API Gateway stops routing to instance
```

## Health Monitoring

### Heartbeat Mechanism

- **Interval**: Every 30 seconds
- **Timeout**: 90 seconds (3 missed heartbeats)
- **Action**: Mark instance as DOWN if timeout

### Health Check

```
GET /actuator/health
Response: {"status": "UP"}
```

## Dashboard

### Eureka Dashboard

Access at: `http://localhost:8761/`

**Features**:
- List of all registered services
- Instance status (UP, DOWN, OUT_OF_SERVICE)
- Instance metadata
- Service instance count
- Replication status

### Dashboard Information

```
Instances currently registered with Eureka
├─ user-service (1 instance)
│  └─ user-service:8099 (UP)
├─ crop-service (1 instance)
│  └─ crop-service:8093 (UP)
├─ mandi-service (1 instance)
│  └─ mandi-service:8096 (UP)
├─ weather-service (1 instance)
│  └─ weather-service:8100 (UP)
├─ scheme-service (1 instance)
│  └─ scheme-service:8097 (UP)
├─ location-service (1 instance)
│  └─ location-service:8095 (UP)
├─ yield-service (1 instance)
│  └─ yield-service:8094 (UP)
├─ admin-service (1 instance)
│  └─ admin-service:8091 (UP)
└─ api-gateway (1 instance)
   └─ api-gateway:8080 (UP)
```

## Configuration

### Environment Variables

```
EUREKA_SERVER_PORT=8761
EUREKA_HOSTNAME=localhost
EUREKA_INSTANCE_PREFER_IP_ADDRESS=true
EUREKA_CLIENT_REGISTER_WITH_EUREKA=false
EUREKA_CLIENT_FETCH_REGISTRY=false
```

### Eureka Server Configuration (application.yml)

```yaml
server:
  port: ${EUREKA_SERVER_PORT:8761}

eureka:
  instance:
    hostname: ${EUREKA_HOSTNAME:localhost}
    prefer-ip-address: ${EUREKA_INSTANCE_PREFER_IP_ADDRESS:true}
  
  client:
    register-with-eureka: ${EUREKA_CLIENT_REGISTER_WITH_EUREKA:false}
    fetch-registry: ${EUREKA_CLIENT_FETCH_REGISTRY:false}
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  
  server:
    enable-self-preservation: true
    eviction-interval-timer-in-ms: 60000
```

## Service Configuration

### Client-Side Configuration (Each Service)

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
    registry-fetch-interval-seconds: 30
  
  instance:
    instance-id: ${spring.application.name}:${server.port}
    prefer-ip-address: true
    health-check-url-path: /actuator/health
    status-page-url-path: /actuator/info
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90
```

## Dependencies

- Spring Boot 3.5.8
- Spring Cloud Eureka Server
- Lombok
- SpringDoc OpenAPI 2.0.4

## Deployment

- **Docker**: Dockerfile available
- **Port**: 8761
- **Health Check**: `/actuator/health`
- **Dashboard**: `http://localhost:8761/`

## Common Use Cases

1. **View All Services**
   - Visit `http://localhost:8761/`
   - See all registered services and instances

2. **Check Service Status**
   - Look at dashboard
   - See if service is UP or DOWN

3. **Deregister Service**
   - Service sends deregistration request
   - Eureka removes service from registry

4. **Service Failover**
   - If instance goes down
   - Eureka detects missed heartbeat
   - Marks instance as DOWN
   - API Gateway stops routing to instance

## Monitoring & Observability

- **Dashboard**: Visual representation of services
- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`
- **Logging**: Service registration/deregistration events
- **Metrics**: Service count, instance count, registration rate

## Performance Considerations

- **Caching**: Service information cached by clients
- **Replication**: Eureka can be replicated for high availability
- **Scalability**: Supports hundreds of service instances

## High Availability Setup

For production, Eureka can be configured in a cluster:

```
Eureka Server 1 (8761)
    ↔ Replication ↔
Eureka Server 2 (8762)
    ↔ Replication ↔
Eureka Server 3 (8763)
```

Each service registers with all Eureka instances.

## Future Enhancements

- Eureka cluster setup for high availability
- Advanced monitoring and alerting
- Service mesh integration (Istio)
- Kubernetes integration
- Custom health check strategies
