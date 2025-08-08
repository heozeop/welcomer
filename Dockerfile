# Multi-stage Dockerfile for Welcomer SNS Feed System
# Build stage
FROM gradle:8.14.3-jdk21 AS build

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle gradle

# Copy source code
COPY src src

# Build the application
RUN gradle clean build -x test --no-daemon

# Runtime stage
FROM openjdk:21-jre-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user
RUN groupadd -r app && useradd -r -g app app

# Create directories
RUN mkdir -p /app/logs && chown -R app:app /app

# Copy the jar from build stage
COPY --from=build /app/build/libs/*.jar /app/app.jar

# Change to app user
USER app

WORKDIR /app

# Expose ports
EXPOSE 8080 5005

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication"

# Run the application
CMD java $JAVA_OPTS -jar app.jar