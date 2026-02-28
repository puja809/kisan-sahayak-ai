# Frontend Dockerfile - Multi-stage build
FROM node:18-alpine as builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the Angular application
RUN npm run build

# Production stage
FROM node:18-alpine

WORKDIR /app

# Install serve to run the application
RUN npm install -g serve

# Create non-root user for security
RUN addgroup -g 1000 app && adduser -u 1000 -G app -s /bin/sh -D app

# Copy built application from builder
COPY --from=builder /app/dist ./dist

# Change ownership
RUN chown -R app:app /app

# Switch to non-root user
USER app

# Expose port
EXPOSE 4200

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:4200/ || exit 1

# Run the application
CMD ["serve", "-s", "dist", "-l", "4200"]
