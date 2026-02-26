# Swagger/OpenAPI Setup Guide

## ✅ Swagger Configuration Status

All backend services have been configured with Swagger/OpenAPI documentation. Here's the complete setup:

## 📋 Services with Swagger Enabled

| Service | Port | Swagger URL | API Docs |
|---------|------|-------------|----------|
| API Gateway | 8080 | http://localhost:8080/swagger-ui.html | http://localhost:8080/v3/api-docs |
| User Service | 8099 | http://localhost:8099/swagger-ui.html | http://localhost:8099/v3/api-docs |
| Weather Service | 8100 | http://localhost:8100/swagger-ui.html | http://localhost:8100/v3/api-docs |
| Crop Service | 8093 | http://localhost:8093/swagger-ui.html | http://localhost:8093/v3/api-docs |
| Scheme Service | 8097 | http://localhost:8097/swagger-ui.html | http://localhost:8097/v3/api-docs |
| Mandi Service | 8096 | http://localhost:8096/swagger-ui.html | http://localhost:8096/v3/api-docs |
| Location Service | 8095 | http://localhost:8095/swagger-ui.html | http://localhost:8095/v3/api-docs |
| Admin Service | 8091 | http://localhost:8091/swagger-ui.html | http://localhost:8091/v3/api-docs |
| IoT Service | 8094 | http://localhost:8094/swagger-ui.html | http://localhost:8094/v3/api-docs |

## 🔧 Configuration Details

### 1. Dependencies Added

All services have the following dependency in their `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

**Version**: 2.3.0 (defined in parent pom.xml)

### 2. Application Configuration

Each service has Swagger configuration in `application.yml`:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operations-sorter: method
    tags-sorter: alpha
    display-request-duration: true
  show-actuator: true
  use-fqn: true
```

### 3. OpenAPI Annotations

Each service has OpenAPI annotations in the main application class:

**Example (UserServiceApplication.java)**:
```java
@OpenAPIDefinition(
    info = @Info(
        title = "User Service API",
        version = "1.0.0",
        description = "User authentication and profile management service",
        contact = @Contact(name = "Farmer Assistance Team", email = "support@farmer-assistance.in")
    )
)
```

### 4. API Gateway Configuration

The API Gateway has a centralized OpenAPI configuration:

**File**: `backend/api-gateway/src/main/java/com/farmer/apigateway/config/OpenApiConfig.java`

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Indian Farmer Assistance Application API")
                        .version("1.0.0")
                        .description("Comprehensive API documentation...")
                        .contact(new Contact()
                                .name("Indian Farmer Assistance Team")
                                .email("support@farmerassistance.gov.in"))
                        .license(new License()
                                .name("Government of India License")))
                .servers(Arrays.asList(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.farmerassistance.gov.in").description("Production"),
                        new Server().url("https://staging-api.farmerassistance.gov.in").description("Staging")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```

## 🚀 How to Access Swagger Documentation

### Option 1: Direct Service Access

Access Swagger UI directly from each service:

```bash
# User Service
curl http://localhost:8099/swagger-ui.html

# Weather Service
curl http://localhost:8100/swagger-ui.html

# Crop Service
curl http://localhost:8093/swagger-ui.html

# And so on for other services...
```

### Option 2: API Gateway Access

Access through the API Gateway (once routing is configured):

```bash
curl http://localhost:8080/swagger-ui.html
```

### Option 3: Frontend Component

Access through the Angular frontend component:

**File**: `frontend/src/app/pages/api-docs/api-docs.component.ts`

- Navigate to: http://localhost:4200/api-docs
- Select service from tabs
- View Swagger UI embedded in the component

## 📝 API Documentation Features

### Available in Swagger UI

1. **Interactive API Testing**
   - Try out endpoints directly
   - Send requests with parameters
   - View responses

2. **Request/Response Models**
   - See data structures
   - View required fields
   - Check data types

3. **Authentication**
   - JWT Bearer token support
   - Authorize button to add token
   - Secure endpoints marked

4. **API Organization**
   - Endpoints sorted by HTTP method
   - Tags sorted alphabetically
   - Request duration displayed

5. **Documentation**
   - Endpoint descriptions
   - Parameter documentation
   - Response examples

## 🔐 Security Configuration

### JWT Bearer Token

All services support JWT Bearer token authentication:

```bash
# Example: Add token to request
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8099/api/v1/users/profile
```

### In Swagger UI

1. Click "Authorize" button
2. Enter: `Bearer YOUR_JWT_TOKEN`
3. Click "Authorize"
4. All subsequent requests will include the token

## 🛠️ Troubleshooting

### Swagger UI Not Showing

**Problem**: Swagger UI returns 404

**Solution**:
1. Verify service is running: `curl http://localhost:8099/actuator/health`
2. Check springdoc dependency is in pom.xml
3. Verify springdoc configuration in application.yml
4. Check service logs for errors

### API Docs Not Loading

**Problem**: `/v3/api-docs` returns empty or error

**Solution**:
1. Ensure controllers have proper annotations
2. Check for compilation errors
3. Verify Spring Boot version compatibility
4. Check application logs

### Endpoints Not Showing

**Problem**: Endpoints don't appear in Swagger UI

**Solution**:
1. Add `@RestController` or `@Controller` to controller classes
2. Add `@RequestMapping` or `@GetMapping`, `@PostMapping`, etc.
3. Add `@Operation` annotations for documentation
4. Rebuild and restart service

## 📚 Adding Documentation to Endpoints

### Example: Documenting an Endpoint

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping("/profile")
    @Operation(
        summary = "Get user profile",
        description = "Retrieve the authenticated user's profile information"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Profile retrieved successfully"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - token required"
    )
    public ResponseEntity<UserProfileDto> getProfile() {
        // Implementation
    }
}
```

### Annotations Used

- `@Operation`: Describes the endpoint
- `@ApiResponse`: Documents possible responses
- `@Parameter`: Documents request parameters
- `@RequestBody`: Documents request body
- `@Schema`: Documents data models

## 🔄 Updating Swagger Documentation

### When to Update

1. **New Endpoints**: Add `@Operation` and `@ApiResponse` annotations
2. **Changed Parameters**: Update `@Parameter` annotations
3. **New Models**: Add `@Schema` annotations to DTOs
4. **Security Changes**: Update `@SecurityRequirement` annotations

### Rebuild and Restart

```bash
# Rebuild service
mvn clean install -f backend/user-service/pom.xml

# Restart service
java -jar backend/user-service/target/user-service-1.0.0-SNAPSHOT.jar

# Access updated documentation
curl http://localhost:8099/swagger-ui.html
```

## 📊 Swagger Configuration Summary

| Component | Status | Details |
|-----------|--------|---------|
| springdoc-openapi dependency | ✅ | Version 2.3.0 |
| Swagger UI | ✅ | Enabled on all services |
| API Docs endpoint | ✅ | `/v3/api-docs` |
| OpenAPI annotations | ✅ | Added to all services |
| JWT security scheme | ✅ | Configured in API Gateway |
| Service documentation | ✅ | Added to all services |
| Frontend component | ✅ | Created and integrated |

## 🎯 Next Steps

1. **Start Services**: Run all backend services
2. **Access Swagger UI**: Navigate to service Swagger URLs
3. **Test Endpoints**: Use "Try it out" feature
4. **Add Authentication**: Use "Authorize" button with JWT token
5. **Document APIs**: Add annotations to new endpoints

## 📞 Support

For issues with Swagger documentation:

1. Check service logs: `tail -f backend/logs/application.log`
2. Verify springdoc configuration
3. Ensure controllers have proper annotations
4. Check Spring Boot version compatibility
5. Review OpenAPI specification: https://swagger.io/specification/

---

**Status**: ✅ **SWAGGER FULLY CONFIGURED AND READY**

**Last Updated**: February 26, 2026
