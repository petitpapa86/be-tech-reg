# RegTech App Module

This module serves as the main application orchestrator for the RegTech modular monolith.

## Purpose

- **Application Entry Point**: Contains the main `RegtechApplication` class
- **Centralized Configuration**: Houses all environment-specific configurations
- **Module Orchestration**: Coordinates all domain modules (IAM, Billing, Core)
- **Dependency Management**: Manages dependencies between modules

## Structure

```
regtech-app/
├── src/main/java/com/bcbs239/regtech/app/
│   └── RegtechApplication.java          # Main Spring Boot application class
├── src/main/resources/
│   └── application.yml                  # Centralized application configuration
└── pom.xml                              # Module dependencies and build configuration
```

## Running the Application

### Development Mode (H2 Database)
```bash
# From project root
./mvnw clean install -DskipTests
cd regtech-app
../mvnw spring-boot:run -Dspring-boot.run.profiles=development
```

### Production Mode (PostgreSQL)
```bash
# Start PostgreSQL first
docker-compose -f compose.yaml up -d

# Run with production profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=production
```

## Configuration

All application configuration is centralized in `application.yml`:
- Database connections (H2 for dev, PostgreSQL for prod)
- Security settings
- Logging configuration
- Module-specific properties

## Module Dependencies

This module depends on:
- `regtech-core`: Shared infrastructure and security
- `regtech-iam`: Identity and Access Management
- `regtech-billing`: Payment and subscription management

## Access Points

- **Application**: http://localhost:8080
- **H2 Console**: http://localhost:8080/h2-console (development only)
- **Health Check**: http://localhost:8080/actuator/health
- **API Docs**: http://localhost:8080/swagger-ui.html (if configured)