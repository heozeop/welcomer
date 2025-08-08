# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.4 application written in Kotlin 1.9.25 with Java 21. The application uses:
- JOOQ for database access layer
- Flyway for database migrations
- gRPC for service communication
- Resilience4j for circuit breaker patterns
- MySQL as the database

## Common Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Clean build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "com.welcomer.welcome.WelcomeApplicationTests"

# Run tests with more output
./gradlew test --info
```

### Development
```bash
# Check for dependency updates
./gradlew dependencyUpdates

# Generate JOOQ classes (if configured)
./gradlew generateJooq

# Run Flyway migrations
./gradlew flywayMigrate

# View Flyway migration info
./gradlew flywayInfo
```

## Architecture

### Package Structure
- `com.welcomer.welcome` - Main application package containing the Spring Boot application class
- Database migrations located in `src/main/resources/db/migration/`
- Application configuration in `src/main/resources/application.properties`

### Key Dependencies
- **Data Access**: JOOQ 3.20.0 and Exposed 1.0.0-beta-5 for different database access patterns
- **Database Migration**: Flyway for versioned database schema management
- **RPC Communication**: gRPC 1.69.0 with Spring Boot integration via grpc-spring-boot-starter
- **Resilience**: Spring Cloud Circuit Breaker with Resilience4j implementation

### Database Configuration
The application uses MySQL. Ensure MySQL is running and configured before starting the application. Database migrations are managed by Flyway and located in `src/main/resources/db/migration/`.

### gRPC Services
The project includes gRPC dependencies suggesting service-to-service communication. Protocol buffer definitions should be added as needed for service contracts.