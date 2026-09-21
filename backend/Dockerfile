# Stage 1: Build the Spring Boot application using Gradle
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and definitions first for efficient layer caching
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Make wrapper executable and download dependencies
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# Copy application source code
COPY src ./src

# Build production jar without running test suite in docker build (tests run in CI/pre-commit)
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Minimal, secure JRE runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy executable jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Cloud Run defaults to port 8080 and provides the PORT env variable
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=${PORT}", "-jar", "app.jar"]
