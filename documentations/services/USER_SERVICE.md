# User Service Documentation

**Port:** 8099  
**Language:** Java (Spring Boot)  
**Database:** PostgreSQL  
**Authentication:** JWT-based

## Overview

The User Service handles authentication, user profile management, role management, and AgriStack integration. It's the core authentication service for the entire application.

## Key Responsibilities

- User registration and login
- JWT token generation and validation
- User profile management
- Farmer crop management
- Admin role management and auditing
- AgriStack integration for government services

## API Endpoints

### Authentication Endpoints (`/api/v1/auth`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register new farmer user |
| POST | `/login` | Farmer login with email/password |
| POST | `/admin-login` | Admin login with special credentials |
| POST | `/refresh-token` | Refresh expired JWT token |
| POST | `/logout` | Logout user |

### User Management Endpoints (`/api/v1/users`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/{userId}` | Get user profile details |
| PUT | `/{userId}` | Update user profile |
| DELETE | `/{userId}` | Delete user account |
| GET | `/profile/crops` | Get user's crops |

### Profile Management Endpoints (`/api/v1/profile`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/crops` | Add crop to user profile |
| PUT | `/crops/{cropId}` | Update crop details |
| DELETE | `/crops/{cropId}` | Remove crop from profile |
| GET | `/crops` | List all user crops |

### Admin Role Management (`/api/v1/admin/roles`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/create-admin` | Create new admin user |
| PUT | `/modify-role` | Modify user role |
| GET | `/audit-log` | Get role modification audit trail |

## Core Services

### AuthController
- Handles user registration and login
- Manages JWT token lifecycle
- Validates credentials against database
- Returns authentication tokens

### UserController
- CRUD operations for user profiles
- Account deletion with data cleanup
- User information retrieval

### ProfileService
- Manages farmer profile data
- Handles crop management (add/update/delete)
- Maintains crop-user relationships

### RoleManagementService
- Admin user creation
- Role modification with audit trail
- Permission management

### JwtService
- JWT token generation
- Token validation and expiration checks
- Claim extraction and verification

## Data Models

### User Entity
```
- userId (PK)
- email (unique)
- password (hashed)
- firstName
- lastName
- phoneNumber
- address
- state
- district
- role (FARMER, ADMIN, SUPER_ADMIN)
- createdAt
- updatedAt
- isActive
```

### Crop Entity
```
- cropId (PK)
- userId (FK)
- cropName
- cropType
- areaUnderCultivation
- sowingDate
- expectedHarvestDate
- soilType
- createdAt
- updatedAt
```

### RoleModificationAudit Entity
```
- auditId (PK)
- userId (FK)
- oldRole
- newRole
- modifiedBy
- modifiedAt
- reason
```

## Security Features

- **Password Hashing**: BCrypt with salt
- **JWT Tokens**: HS256 algorithm, configurable expiration
- **Refresh Tokens**: Extended validity for token refresh
- **Role-Based Access Control**: FARMER, ADMIN, SUPER_ADMIN roles
- **Token Validation**: Signature and expiration verification

## Configuration

### Environment Variables
```
USER_SERVICE_PORT=8099
USER_DB_URL=jdbc:postgresql://localhost:5432/indian_farmer_db
USER_DB_USERNAME=postgres
USER_DB_PASSWORD=password
JWT_SECRET=<256-bit-secret-key>
JWT_EXPIRATION=86400000 (24 hours)
JWT_REFRESH_EXPIRATION=604800000 (7 days)
JWT_ISSUER=indian-farmer-assistance
SUPER_ADMIN_TOKEN=<super-admin-secret>
```

### Database Configuration
- **Driver**: PostgreSQL
- **Connection Pool**: HikariCP
  - Max Pool Size: 10
  - Min Idle: 5
  - Connection Timeout: 30s
  - Idle Timeout: 10m
  - Max Lifetime: 30m

## Dependencies

- Spring Boot 3.5.8
- Spring Security
- Spring Data JPA
- PostgreSQL Driver
- JJWT 0.11.5 (JWT library)
- Lombok
- SpringDoc OpenAPI 2.0.4

## Integration Points

### Outbound
- **Eureka Server**: Service registration and discovery
- **API Gateway**: Receives requests through gateway

### Inbound
- **Frontend**: Angular application
- **Other Services**: Service-to-service authentication validation

## Error Handling

- Invalid credentials: 401 Unauthorized
- User not found: 404 Not Found
- Duplicate email: 409 Conflict
- Invalid token: 401 Unauthorized
- Expired token: 401 Unauthorized
- Insufficient permissions: 403 Forbidden

## Monitoring & Observability

- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`
- **Swagger/OpenAPI**: `/swagger-ui.html`, `/v3/api-docs`
- **Logging Level**: DEBUG for `com.farmer`, INFO for others
- **Security Logging**: DEBUG level for Spring Security

## Deployment

- **Docker**: Dockerfile available
- **Environment**: Supports dev, staging, production
- **Database Migration**: Hibernate auto-update enabled
- **Health Checks**: Spring Boot Actuator health endpoint

## Common Use Cases

1. **User Registration**
   - POST `/api/v1/auth/register` with email, password, name
   - Returns JWT token and user details

2. **User Login**
   - POST `/api/v1/auth/login` with email, password
   - Returns JWT token and refresh token

3. **Add Crop to Profile**
   - POST `/api/v1/profile/crops` with crop details
   - Links crop to authenticated user

4. **Token Refresh**
   - POST `/api/v1/auth/refresh-token` with refresh token
   - Returns new JWT token

5. **Admin Creation**
   - POST `/api/v1/admin/roles/create-admin` with admin details
   - Creates new admin user with audit trail

## Performance Considerations

- JWT validation on every request
- Database connection pooling for efficiency
- Indexed queries on email and userId
- Cached user roles for authorization checks

## Future Enhancements

- OAuth2/OpenID Connect integration
- Multi-factor authentication (MFA)
- Social login integration
- AgriStack integration for government services
- Session management improvements
