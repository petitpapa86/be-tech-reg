# Requirements Document

## Introduction

The RegTech application consists of multiple bounded contexts (modules) with configuration scattered across various YAML files. Currently, the main `application.yml` contains configuration for all modules, while some modules have their own module-specific configuration files. This creates maintenance challenges, duplication, and makes it difficult to understand which configuration applies to which module. This feature aims to reorganize the configuration into a clear, maintainable structure following Spring Boot best practices.

## Glossary

- **RegTech Application**: The main Spring Boot application that orchestrates all modules
- **Bounded Context**: A DDD concept representing a module with clear boundaries (e.g., ingestion, risk-calculation, report-generation)
- **Configuration Profile**: Spring Boot profiles (development, production) that allow environment-specific configuration
- **S3 Storage**: Amazon S3 cloud storage service used by multiple modules for file storage
- **Module Configuration**: YAML configuration specific to a bounded context/module
- **Shared Configuration**: Configuration that applies across multiple modules (database, logging, metrics)

## Requirements

### Requirement 1: Configuration File Structure

**User Story:** As a developer, I want a clear configuration file structure, so that I can easily find and modify configuration for specific modules and environments.

#### Acceptance Criteria

1. THE RegTech Application SHALL organize configuration files into a hierarchical structure with application.yml as the root configuration file
2. WHEN a module has module-specific configuration THEN the RegTech Application SHALL store it in a separate application-{module-name}.yml file in that module's infrastructure resources directory
3. THE RegTech Application SHALL maintain the following modules with separate configuration files: ingestion, data-quality, risk-calculation, report-generation, billing, iam
4. THE RegTech Application SHALL keep shared configuration (database, logging, metrics, event processing) in the root application.yml file
5. THE RegTech Application SHALL use Spring profile sections (---) to separate environment-specific configuration within each file

### Requirement 2: S3 Storage Configuration Consolidation

**User Story:** As a DevOps engineer, I want S3 storage configuration consolidated and standardized, so that I can manage cloud storage settings consistently across all modules.

#### Acceptance Criteria

1. THE RegTech Application SHALL identify all modules that use S3 storage (ingestion, risk-calculation, report-generation, data-quality)
2. WHEN multiple modules use S3 storage THEN the RegTech Application SHALL define a shared S3 configuration structure with common properties (bucket, region, access-key, secret-key, endpoint, encryption)
3. THE RegTech Application SHALL allow each module to specify module-specific S3 properties (prefix, bucket-name-suffix) while inheriting common properties
4. THE RegTech Application SHALL support both S3 and local filesystem storage through a storage.type property
5. THE RegTech Application SHALL use environment variables for sensitive S3 credentials (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)

### Requirement 3: Database Configuration Organization

**User Story:** As a database administrator, I want database configuration centralized, so that I can manage database connections and Flyway migrations from a single location.

#### Acceptance Criteria

1. THE RegTech Application SHALL maintain database configuration (datasource, JPA, Flyway) in the root application.yml file
2. WHEN modules require module-specific database schemas THEN the RegTech Application SHALL configure Flyway migration locations per module
3. THE RegTech Application SHALL support profile-specific database configuration for development and production environments
4. THE RegTech Application SHALL use environment variables for database credentials in production
5. THE RegTech Application SHALL configure connection pooling and JPA properties in the shared configuration

### Requirement 4: Logging Configuration Standardization

**User Story:** As a system operator, I want logging configuration standardized across all modules, so that I can consistently collect and analyze logs in different environments.

#### Acceptance Criteria

1. THE RegTech Application SHALL maintain logging configuration in the root application.yml file
2. WHEN running in production environment THEN the RegTech Application SHALL support structured JSON logging as an option
3. THE RegTech Application SHALL define log levels per module (ingestion, data-quality, risk-calculation, report-generation, billing, iam)
4. THE RegTech Application SHALL support different logging patterns for console and file output
5. THE RegTech Application SHALL reference logback-spring.xml for advanced logging configuration

### Requirement 5: Metrics and Monitoring Configuration

**User Story:** As a site reliability engineer, I want metrics and monitoring configuration centralized, so that I can ensure consistent observability across all modules.

#### Acceptance Criteria

1. THE RegTech Application SHALL maintain Spring Actuator configuration in the root application.yml file
2. THE RegTech Application SHALL expose health, info, and metrics endpoints for all modules
3. WHEN modules implement custom health checks THEN the RegTech Application SHALL configure health check details visibility
4. THE RegTech Application SHALL configure Resilience4j circuit breakers and retry policies per module
5. THE RegTech Application SHALL support profile-specific monitoring configuration for development and production

### Requirement 6: Module-Specific Configuration Isolation

**User Story:** As a module developer, I want my module's configuration isolated in its own file, so that I can modify module settings without affecting other modules.

#### Acceptance Criteria

1. WHEN a module has configuration properties THEN the RegTech Application SHALL store them in application-{module-name}.yml in the module's infrastructure/src/main/resources directory
2. THE RegTech Application SHALL use Spring's @ConfigurationProperties to bind module configuration to Java classes
3. THE RegTech Application SHALL support profile-specific overrides within each module configuration file
4. THE RegTech Application SHALL document all configuration properties with comments explaining their purpose and valid values
5. THE RegTech Application SHALL validate required configuration properties at application startup

### Requirement 7: Environment-Specific Configuration

**User Story:** As a DevOps engineer, I want clear separation of environment-specific configuration, so that I can deploy the application to different environments without code changes.

#### Acceptance Criteria

1. THE RegTech Application SHALL support development and production profiles
2. WHEN running in development profile THEN the RegTech Application SHALL use local filesystem storage and reduced thread pools
3. WHEN running in production profile THEN the RegTech Application SHALL use S3 storage and optimized performance settings
4. THE RegTech Application SHALL support optional structured JSON logging for production deployments
5. THE RegTech Application SHALL use environment variables for all environment-specific secrets and endpoints

### Requirement 8: Configuration Documentation

**User Story:** As a new developer, I want configuration files well-documented, so that I can understand what each property does and how to configure the application.

#### Acceptance Criteria

1. THE RegTech Application SHALL include inline comments in YAML files explaining each configuration section
2. THE RegTech Application SHALL document valid values and units for numeric properties (e.g., "500MB in bytes", "5 minutes")
3. THE RegTech Application SHALL reference requirement numbers in comments where configuration relates to specific requirements
4. THE RegTech Application SHALL provide example values for optional properties
5. THE RegTech Application SHALL maintain a configuration reference document listing all properties by module

### Requirement 9: Configuration Migration Strategy

**User Story:** As a technical lead, I want a safe migration strategy for reorganizing configuration, so that we can refactor without breaking existing functionality.

#### Acceptance Criteria

1. THE RegTech Application SHALL create new module-specific configuration files without removing existing configuration
2. WHEN new configuration files are created THEN the RegTech Application SHALL verify they are loaded correctly through integration tests
3. THE RegTech Application SHALL remove duplicate configuration from application.yml only after verifying module files are loaded
4. THE RegTech Application SHALL update all @ConfigurationProperties classes to reference the new property paths
5. THE RegTech Application SHALL document the migration in a CONFIGURATION_MIGRATION.md file

### Requirement 10: Configuration Validation

**User Story:** As a quality assurance engineer, I want configuration validated at startup, so that misconfigurations are caught early before causing runtime errors.

#### Acceptance Criteria

1. THE RegTech Application SHALL validate required configuration properties are present at startup
2. WHEN configuration values are invalid THEN the RegTech Application SHALL fail fast with clear error messages
3. THE RegTech Application SHALL validate numeric ranges for properties like thread-pool-size and timeout values
4. THE RegTech Application SHALL validate file paths and S3 bucket names follow naming conventions
5. THE RegTech Application SHALL provide configuration validation tests that run in CI/CD pipeline

### Requirement 11: Security Configuration Consolidation

**User Story:** As a security engineer, I want security configuration consolidated and consistently applied, so that authentication and authorization work uniformly across all modules.

#### Acceptance Criteria

1. THE RegTech Application SHALL maintain security configuration in the root application.yml file under an iam.security section
2. THE RegTech Application SHALL define public paths (paths that do not require authentication) in a centralized configuration list
3. WHEN a module endpoint requires authentication THEN the RegTech Application SHALL use the SecurityFilter from regtech-iam infrastructure
4. WHEN a module endpoint requires specific permissions THEN the RegTech Application SHALL declare permissions using RouterAttributes in the route definition
5. THE RegTech Application SHALL document all public paths with comments explaining why they are public

### Requirement 12: Endpoint Security Consistency

**User Story:** As an API developer, I want a consistent pattern for securing endpoints, so that I can easily understand and apply security to new endpoints.

#### Acceptance Criteria

1. THE RegTech Application SHALL use functional routing (RouterFunction) for all module endpoints
2. WHEN defining a route THEN the RegTech Application SHALL use RouterAttributes.withAttributes to declare required permissions
3. WHEN an endpoint requires no authentication THEN the RegTech Application SHALL add its path to the public paths configuration
4. WHEN an endpoint requires authentication but no specific permissions THEN the RegTech Application SHALL pass null for permissions in RouterAttributes
5. THE RegTech Application SHALL register all module RouterFunction beans through the RouterConfig in regtech-app

### Requirement 13: Thread Pool and Async Configuration

**User Story:** As a performance engineer, I want thread pool configuration organized per module, so that I can tune async processing independently for each bounded context.

#### Acceptance Criteria

1. WHEN a module uses async processing THEN the RegTech Application SHALL define thread pool configuration in that module's application-{module}.yml file
2. THE RegTech Application SHALL use consistent property names for thread pool configuration (core-pool-size, max-pool-size, queue-capacity, thread-name-prefix)
3. THE RegTech Application SHALL configure profile-specific thread pool sizes (smaller for development, larger for production)
4. THE RegTech Application SHALL document the purpose of each thread pool and what operations it handles
5. THE RegTech Application SHALL validate thread pool sizes are positive integers at startup
