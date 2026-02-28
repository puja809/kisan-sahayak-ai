# Scheme Service Dockerfile
FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app
COPY pom.xml .
COPY common ./common
COPY scheme-service ./scheme-service
RUN mvn -f pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1000 app && adduser -u 1000 -G app -s /bin/sh -D app

# Copy the built artifact
COPY --from=builder /app/scheme-service/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /var/log/farmer-app && chown -R app:app /var/log/farmer-app

# Switch to non-root user
USER app

# Expose port
EXPOSE 8097

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8097/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
