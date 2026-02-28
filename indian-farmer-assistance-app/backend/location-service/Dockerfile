# location-service Dockerfile
FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

COPY pom.xml .
COPY common ./common
COPY api-gateway ./api-gateway
COPY user-service ./user-service
COPY crop-service ./crop-service
COPY location-service ./location-service
COPY mandi-service ./mandi-service
COPY scheme-service ./scheme-service
COPY admin-service ./admin-service
COPY iot-service ./iot-service
COPY weather-service ./weather-service
COPY yield-service ./yield-service
COPY eureka-server ./eureka-server

# Support BuildKit caching for .m2
RUN \
    --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml clean package -DskipTests -pl location-service -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup app && adduser -u 1000 -G app -s /bin/sh -D app

# Copy the built artifact
COPY --from=builder /app/location-service/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /var/log/farmer-app && chown -R app:app /var/log/farmer-app

# Switch to non-root user
RUN mkdir -p /app/logs && chown -R app:app /app/logs
USER app

# Expose port
EXPOSE 8095

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8095/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

