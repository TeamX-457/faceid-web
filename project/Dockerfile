# syntax=docker/dockerfile:1

# Build stage
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline -Dmaven.test.skip=true

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -Dmaven.test.skip=true

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/target/faceid-campus-backend-*.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads

# Expose port (informational - actual bind port comes from $PORT at runtime, e.g. on Render)
EXPOSE 8080

# Health check (shell form so $PORT expands; defaults to 8080 for local/Docker runs)
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]