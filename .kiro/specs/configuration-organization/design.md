# Configuration Organization Design Document

## Overview

This design document outlines the reorganization of the RegTech application's configuration structure. The current architecture has configuration scattered across multiple files with significant duplication and unclear boundaries. This design establishes a clear, maintainable configuration hierarchy that separates shared infrastructure concerns from module-specific settings while supporting multiple deployment environments.

The design follows Spring Boot configuration best practices, using a layered approach where:
1. Root `application.yml` contains shared infrastructure (database, logging, metrics, event processing)
2. Module-specific `application-{module}.yml` files contain bounded context configuration
3. Profile sections within each file handle environment-specific overrides
4. Configuration properties classes provide type-safe access to settings

## Architecture

### Configuration File Hierarchy

```
regtech-app/src/main/resources/
├── application.yml                          # Root configuration (shared infrastructure)
├── logback-spring.xml                       # Logging configuration
└── schema.sql                               # Database schema

regtech-ingestion/infrastructure/src/main/resources/
└── application-ingestion.yml                # Ingestion module configuration

regtech-data-quality/infrastructure/src/main/resources/
└── application-data-quality.yml             # Data quality module configuration

regtech-risk-calculation/infrastructure/src/main/resources/
└── application-risk-calculation.yml         # Risk calculation module configuration

regtech-report-generation/infrastructure/src/main/resources/
└── application-report-generation.yml        # Report generation module configuration

regtech-billing/infrastructure/src/main/resources/
└── application-billing.yml                  # Billing module configuration

regtech-iam/infrastructure/src/main/resources/
└── application-iam.yml                      # IAM module configuration
```

### Configuration Loading Order

Spring Boot loads configuration in the following order (later sources override earlier ones):
1. `application.yml` (root)
2. `application-{module}.yml` (module-specific)
3. Profile-specific sections within each file
4. Environment variables
5. Command-line arguments

### Shared vs Module-Specific Configuration

**Shared Configuration (application.yml):**
- Spring application settings
- Database configuration (datasource, JPA, Flyway)
- Logging configuration
- Metrics and monitoring (Spring Actuator)
- Event processing (inbox/outbox)
- Security configuration (JWT, OAuth2, public paths)
- Cross-cutting concerns (CORS, error handling)

**Module-Specific Configuration (application-{module}.yml):**
- Module business logic settings
- Module-specific storage configuration
- Module-specific processing parameters
- Module-specific integrations
- Module-specific performance tuning

## Components and Interfaces

### 1. Configuration Properties Classes

Each module will have a `@ConfigurationProperties` class to bind YAML configuration to Java objects:

```java
@ConfigurationProperties(prefix = "ingestion")
public class IngestionProperties {
    private FileProperties file;
    private S3Properties s3;
    private ProcessingProperties processing;
    private PerformanceProperties performance;
    // getters and setters
}
```

### 2. S3 Configuration Structure

Shared S3 configuration structure used across modules:

```yaml
# Shared S3 properties structure
{module}.s3:
  bucket: string              # S3 bucket name
  region: string              # AWS region
  prefix: string              # Object key prefix
  access-key: string          # AWS access key (from env var)
  secret-key: string          # AWS secret key (from env var)
  endpoint: string            # S3 endpoint (for LocalStack/MinIO)
  encryption: string          # Encryption type (AES256, aws:kms)
  kms-key-id: string          # KMS key ID (optional)
```

### 3. Storage Abstraction

Modules that support both S3 and local storage will use a storage type selector:

```yaml
{module}.storage:
  type: s3 | local            # Storage backend selection
  s3:
    # S3 configuration
  local:
    base-path: string         # Local filesystem base path
    create-directories: boolean
```

### 4. Profile-Specific Overrides

Each configuration file uses Spring profile sections:

```yaml
# Base configuration
{module}:
  property: default-value

---
# Development profile
spring:
  config:
    activate:
      on-profile: development

{module}:
  property: dev-value

---
# Production profile
spring:
  config:
    activate:
      on-profile: production

{module}:
  property: prod-value
```

## Data Models

### Root Application Configuration Structure

```yaml
spring:
  application:
    name: regtech
  profiles:
    active: development
  datasource:
    url: jdbc:postgresql://...
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
  flyway:
    enabled: true
    locations:
      - classpath:db/migration/common
      - classpath:db/migration/ingestion
      - classpath:db/migration/data-quality
      - classpath:db/migration/risk-calculation
      - classpath:db/migration/report-generation
      - classpath:db/migration/billing
      - classpath:db/migration/iam

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized

logging:
  config: classpath:logback-spring.xml
  level:
    com.bcbs239.regtech: INFO
    com.bcbs239.regtech.ingestion: INFO
    com.bcbs239.regtech.dataquality: INFO
    com.bcbs239.regtech.riskcalculation: INFO
    com.bcbs239.regtech.reportgeneration: INFO
    com.bcbs239.regtech.billing: INFO
    com.bcbs239.regtech.iam: INFO

# IAM and Security Configuration
iam:
  security:
    # JWT configuration
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400  # 24 hours in seconds
    
    # Password policy
    password:
      min-length: 12
      require-uppercase: true
      require-lowercase: true
      require-digits: true
      require-special-chars: true
    
    # OAuth2 providers
    oauth2:
      google:
        client-id: ${GOOGLE_CLIENT_ID:}
        client-secret: ${GOOGLE_CLIENT_SECRET:}
      facebook:
        client-id: ${FACEBOOK_CLIENT_ID:}
        client-secret: ${FACEBOOK_CLIENT_SECRET:}
    
    # Public paths (no authentication required)
    public-paths:
      - /api/public/**
      - /api/health
      - /api/auth/login
      - /api/auth/register
      - /api/auth/forgot-password
      - /api/v1/users/register
      - /actuator/health
      - /swagger-ui/**
      - /v3/api-docs/**
      # Module-specific public paths
      - /api/v1/ingestion/health
      - /api/v1/data-quality/health
      - /api/v1/data-quality/health/**
      - /api/v1/risk-calculation/health
      - /api/v1/risk-calculation/health/**
      - /api/v1/report-generation/health
      - /api/v1/report-generation/health/**
  
  authorization:
    cache:
      enabled: true
      ttl: 300  # 5 minutes
    multi-tenant:
      enabled: true
      default-organization: "default-org"
    permissions:
      strict-mode: true
      audit-enabled: true

# Event processing configuration
regtech:
  outbox:
    enabled: true
    processing-interval: 30000
    retry-interval: 60000
    max-retries: 3
  inbox:
    enabled: true
    processing-interval: 10000
    batch-size: 20
```

### Module Configuration Structure (Example: Ingestion)

```yaml
# Ingestion Module Configuration
ingestion:
  enabled: true
  
  # File upload settings
  file:
    max-size: 524288000  # 500MB
    supported-types:
      - application/json
      - application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
  
  # Storage configuration
  storage:
    type: s3  # s3 or local
    s3:
      bucket: regtech-ingestion
      region: eu-central-1
      prefix: raw/
      access-key: ${AWS_ACCESS_KEY_ID}
      secret-key: ${AWS_SECRET_ACCESS_KEY}
      endpoint: ${AWS_S3_ENDPOINT:}
      encryption: AES256
    local:
      base-path: ./data/ingestion
      create-directories: true
  
  # Async processing configuration
  # Purpose: Handles async file upload and processing operations
  async:
    enabled: true
    core-pool-size: 5
    max-pool-size: 10
    queue-capacity: 100
    thread-name-prefix: ingestion-async-
    await-termination-seconds: 60
  
  # Performance settings
  performance:
    max-concurrent-files: 4
    chunk-size: 10000

---
# Development profile overrides
spring:
  config:
    activate:
      on-profile: development

ingestion:
  storage:
    type: local
  async:
    core-pool-size: 2
    max-pool-size: 4

---
# Production profile overrides
spring:
  config:
    activate:
      on-profile: production

ingestion:
  storage:
    type: s3
  async:
    core-pool-size: 10
    max-pool-size: 20
```

### S3-Enabled Modules Configuration

The following modules use S3 storage and will have similar storage configuration:

1. **Ingestion Module** (`application-ingestion.yml`)
   - Bucket: `regtech-ingestion`
   - Prefix: `raw/`
   - Purpose: Store uploaded raw data files

2. **Data Quality Module** (`application-data-quality.yml`)
   - Bucket: `regtech-data-quality`
   - Prefix: `quality/`
   - Purpose: Store quality assessment results

3. **Risk Calculation Module** (`application-risk-calculation.yml`)
   - Bucket: `regtech-risk-calculations`
   - Prefix: `calculations/`
   - Purpose: Store calculated risk metrics

4. **Report Generation Module** (`application-report-generation.yml`)
   - Bucket: `regtech-reports`
   - Prefix: `reports/`
   - Purpose: Store generated HTML and XBRL reports

### Security Configuration Structure

Security is centralized in the root `application.yml` under the `iam.security` section:

**JWT Configuration:**
- Secret key (from environment variable)
- Token expiration time
- Token validation settings

**Password Policy:**
- Minimum length requirements
- Character type requirements (uppercase, lowercase, digits, special chars)

**OAuth2 Providers:**
- Google OAuth2 credentials
- Facebook OAuth2 credentials
- Additional providers as needed

**Public Paths:**
- Centralized list of paths that don't require authentication
- Includes health check endpoints from all modules
- Includes authentication endpoints (login, register)
- Includes API documentation endpoints

**Authorization Settings:**
- Permission caching configuration
- Multi-tenant support
- Audit logging for permission checks

### Endpoint Security Pattern

All module endpoints follow a consistent security pattern:

1. **Define Routes with RouterAttributes:**
```java
@Component
public class ModuleRoutes {
    public RouterFunction<ServerResponse> createRoutes() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/module/endpoint"), controller::handleRequest),
            new String[]{"module:action:permission"},  // Required permissions
            new String[]{Tags.MODULE, Tags.FEATURE},
            "Endpoint description"
        );
    }
}
```

2. **Public Endpoints (No Authentication):**
   - Add path to `iam.security.public-paths` in application.yml
   - SecurityFilter will skip authentication for these paths

3. **Authenticated Endpoints (No Specific Permissions):**
   - Use `null` for permissions parameter in RouterAttributes
   - SecurityFilter will require valid JWT but won't check permissions

4. **Authorized Endpoints (Specific Permissions Required):**
   - Specify permissions array in RouterAttributes
   - SecurityFilter will validate JWT and check permissions

### Thread Pool Configuration Pattern

Each module that uses async processing defines its own thread pool configuration:

**Standard Thread Pool Properties:**
```yaml
{module}.async:
  enabled: boolean                    # Enable/disable async processing
  core-pool-size: integer            # Core number of threads
  max-pool-size: integer             # Maximum number of threads
  queue-capacity: integer            # Task queue capacity
  thread-name-prefix: string         # Thread naming pattern
  await-termination-seconds: integer # Graceful shutdown timeout
```

**Module-Specific Thread Pools:**

1. **Ingestion Module** - File upload and processing
   - Dev: core=2, max=4
   - Prod: core=10, max=20
   - Purpose: Async file upload and batch processing

2. **Data Quality Module** - Validation and quality checks
   - Dev: core=2, max=4
   - Prod: core=5, max=10
   - Purpose: Async quality validation operations

3. **Risk Calculation Module** - Risk metric calculations
   - Dev: core=2, max=4
   - Prod: core=5, max=10
   - Purpose: Async risk calculation and aggregation

4. **Report Generation Module** - Report generation
   - Dev: core=2, max=3
   - Prod: core=5, max=10
   - Purpose: Async HTML and XBRL report generation

**Thread Pool Configuration Class:**
```java
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "{module}.async")
public class ModuleAsyncConfiguration {
    private boolean enabled = true;
    private int corePoolSize = 5;
    private int maxPoolSize = 10;
    private int queueCapacity = 100;
    private String threadNamePrefix = "{module}-async-";
    private int awaitTerminationSeconds = 60;
    
    @Bean(name = "{module}TaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        return executor;
    }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Configuration Loading Completeness
*For any* module with a configuration file, loading the Spring application context should successfully bind all configuration properties without errors
**Validates: Requirements 1.2, 6.1, 6.5**

### Property 2: Profile Override Consistency
*For any* configuration property with profile-specific overrides, activating that profile should result in the profile-specific value being used instead of the default value
**Validates: Requirements 7.2, 7.3, 7.4**

### Property 3: Environment Variable Substitution
*For any* configuration property using environment variable syntax (${VAR_NAME}), the property should resolve to the environment variable value when present, or the default value when absent
**Validates: Requirements 2.5, 3.4, 7.5**

### Property 4: Module Configuration Isolation
*For any* two different modules, changing configuration in one module's file should not affect the configuration values loaded by the other module
**Validates: Requirements 6.1, 6.2**

### Property 5: Required Property Validation
*For any* configuration property marked as required, starting the application without that property should fail with a clear error message
**Validates: Requirements 6.5, 10.1, 10.2**

### Property 6: S3 Configuration Consistency
*For any* module using S3 storage, the S3 configuration structure (bucket, region, prefix, access-key, secret-key, endpoint, encryption) should be consistent across all modules
**Validates: Requirements 2.2, 2.3**

### Property 7: Storage Type Selection
*For any* module supporting multiple storage backends, setting storage.type to "s3" should use S3 configuration, and setting it to "local" should use local filesystem configuration
**Validates: Requirements 2.4**

### Property 8: Numeric Range Validation
*For any* numeric configuration property with defined valid ranges (e.g., thread-pool-size > 0), providing a value outside that range should fail validation at startup
**Validates: Requirements 10.3**

### Property 9: Public Path Configuration
*For any* path defined in iam.security.public-paths, requests to that path should not require authentication and should be accessible without a JWT token
**Validates: Requirements 11.2, 11.5**

### Property 10: Endpoint Security Consistency
*For any* module endpoint defined using RouterAttributes, if permissions are specified, then authentication should be required; if permissions are null, then authentication should be required but no specific permissions checked
**Validates: Requirements 12.2, 12.4**

### Property 11: Thread Pool Configuration Consistency
*For any* module with async processing enabled, the thread pool configuration should have consistent property names (core-pool-size, max-pool-size, queue-capacity, thread-name-prefix) and core-pool-size should be less than or equal to max-pool-size
**Validates: Requirements 13.2, 13.5**

## Error Handling

### Configuration Loading Errors

1. **Missing Required Properties**
   - Error: `ConfigurationPropertiesBindException`
   - Message: "Property '{module}.{property}' is required but not found"
   - Action: Application fails to start with clear error message

2. **Invalid Property Values**
   - Error: `BindException`
   - Message: "Property '{module}.{property}' has invalid value '{value}'. Expected: {expected}"
   - Action: Application fails to start with validation details

3. **Profile Not Found**
   - Error: `ProfileNotFoundException`
   - Message: "Profile '{profile}' specified but no configuration found"
   - Action: Log warning and continue with default configuration

4. **Environment Variable Not Set**
   - Error: `PropertySourceException`
   - Message: "Environment variable '{VAR_NAME}' required but not set"
   - Action: Application fails to start if no default provided

### S3 Configuration Errors

1. **Invalid Bucket Name**
   - Error: `IllegalArgumentException`
   - Message: "S3 bucket name '{bucket}' is invalid. Must follow AWS naming rules"
   - Action: Fail fast at startup

2. **Missing Credentials**
   - Error: `SdkClientException`
   - Message: "AWS credentials not found. Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY"
   - Action: Fail fast at startup when storage.type=s3

3. **Invalid Region**
   - Error: `IllegalArgumentException`
   - Message: "AWS region '{region}' is not valid"
   - Action: Fail fast at startup

### Migration Errors

1. **Duplicate Configuration**
   - Error: `DuplicatePropertyException`
   - Message: "Property '{property}' defined in both application.yml and application-{module}.yml"
   - Action: Log warning, use module-specific value

2. **Missing Module Configuration**
   - Error: `FileNotFoundException`
   - Message: "Expected configuration file application-{module}.yml not found"
   - Action: Log warning, use defaults from application.yml

## Testing Strategy

### Unit Testing

1. **Configuration Properties Binding Tests**
   - Test that each `@ConfigurationProperties` class correctly binds YAML values
   - Test default values when properties are not specified
   - Test validation annotations (@NotNull, @Min, @Max, etc.)

2. **Profile Override Tests**
   - Test that development profile overrides default values
   - Test that production profile overrides default values
   - Test that profile-specific values take precedence

3. **Environment Variable Substitution Tests**
   - Test that ${VAR_NAME} syntax resolves correctly
   - Test that ${VAR_NAME:default} provides fallback values
   - Test that missing required environment variables cause startup failure

### Property-Based Testing

1. **Property 1: Configuration Loading Completeness**
   ```java
   @Property
   void allModuleConfigurationsLoadSuccessfully(@ForAll("moduleNames") String moduleName) {
       // Given a module name
       // When loading Spring context with that module's configuration
       // Then no binding exceptions should occur
   }
   ```

2. **Property 2: Profile Override Consistency**
   ```java
   @Property
   void profileOverridesApplyCorrectly(
       @ForAll("profiles") String profile,
       @ForAll("configProperties") String property) {
       // Given a profile and a property with profile-specific override
       // When loading context with that profile active
       // Then the property value should match the profile-specific value
   }
   ```

3. **Property 4: Module Configuration Isolation**
   ```java
   @Property
   void moduleConfigurationsAreIsolated(
       @ForAll("moduleNames") String module1,
       @ForAll("moduleNames") String module2) {
       // Given two different modules
       // When loading their configurations
       // Then changes to module1 config should not affect module2 config
   }
   ```

4. **Property 6: S3 Configuration Consistency**
   ```java
   @Property
   void s3ConfigurationStructureIsConsistent(@ForAll("s3Modules") String moduleName) {
       // Given a module that uses S3
       // When loading its S3 configuration
       // Then it should have all required S3 properties (bucket, region, etc.)
   }
   ```

5. **Property 7: Storage Type Selection**
   ```java
   @Property
   void storageTypeSelectionWorks(
       @ForAll("storageTypes") String storageType,
       @ForAll("s3Modules") String moduleName) {
       // Given a storage type (s3 or local) and a module
       // When setting storage.type to that value
       // Then the correct storage implementation should be used
   }
   ```

6. **Property 9: Public Path Configuration**
   ```java
   @Property
   void publicPathsDoNotRequireAuthentication(@ForAll("publicPaths") String path) {
       // Given a path in iam.security.public-paths
       // When making a request to that path without authentication
       // Then the request should succeed (not return 401)
   }
   ```

7. **Property 10: Endpoint Security Consistency**
   ```java
   @Property
   void endpointSecurityIsConsistent(@ForAll("endpoints") Endpoint endpoint) {
       // Given an endpoint with RouterAttributes
       // When permissions are specified
       // Then authentication should be required
       // And when permissions are null
       // Then authentication should be required but no permission check
   }
   ```

### Integration Testing

1. **Full Application Context Loading**
   - Test that application starts successfully with all modules
   - Test that all configuration files are found and loaded
   - Test that all @ConfigurationProperties beans are created

2. **Profile-Specific Context Loading**
   - Test application startup with development profile
   - Test application startup with production profile
   - Verify correct configuration values for each profile

3. **S3 Configuration Integration**
   - Test S3 client creation with configuration from each module
   - Test that S3 operations use correct bucket/prefix
   - Test fallback to local storage when S3 is unavailable

4. **Configuration Migration Verification**
   - Test that old configuration paths still work (backward compatibility)
   - Test that new configuration paths work correctly
   - Test that duplicate configuration is handled gracefully

5. **Security Configuration Integration**
   - Test that SecurityFilter loads public paths from configuration
   - Test that public paths are accessible without authentication
   - Test that protected paths require authentication
   - Test that RouterAttributes permissions are enforced correctly

6. **Endpoint Security Integration**
   - Test that health endpoints from all modules are public
   - Test that module endpoints with permissions require authentication
   - Test that module endpoints with null permissions require authentication but no permission check
   - Test that unauthorized requests return 401 with proper error message

### Configuration Validation Tests

1. **Required Property Tests**
   - Test startup failure when required properties are missing
   - Test clear error messages for missing properties
   - Test that optional properties don't cause startup failure

2. **Numeric Range Tests**
   - Test validation of thread-pool-size (must be > 0)
   - Test validation of timeout values (must be positive)
   - Test validation of file size limits

3. **Format Validation Tests**
   - Test S3 bucket name validation
   - Test AWS region validation
   - Test file path validation

## Implementation Notes

### Migration Strategy

1. **Phase 1: Create Module Configuration Files**
   - Create `application-{module}.yml` for each module
   - Copy module-specific configuration from root `application.yml`
   - Keep original configuration in place (no deletion yet)

2. **Phase 2: Update Configuration Properties Classes**
   - Update `@ConfigurationProperties` prefix to match new paths
   - Add validation annotations
   - Create integration tests to verify loading

3. **Phase 3: Verify and Test**
   - Run all integration tests
   - Verify application starts with both old and new configuration
   - Test profile-specific overrides

4. **Phase 4: Remove Duplicates**
   - Remove module-specific configuration from root `application.yml`
   - Keep only shared infrastructure configuration
   - Update documentation

5. **Phase 5: Validation and Cleanup**
   - Add configuration validation
   - Remove deprecated configuration paths
   - Update deployment documentation

### Backward Compatibility

During migration, support both old and new configuration paths:
- If property exists in new location, use it
- If property exists only in old location, use it with deprecation warning
- If property exists in both locations, use new location and log warning

### Documentation Requirements

1. **Configuration Reference Document**
   - List all configuration properties by module
   - Document valid values and defaults
   - Provide examples for common scenarios

2. **Migration Guide**
   - Step-by-step instructions for updating configuration
   - Mapping of old paths to new paths
   - Troubleshooting common issues

3. **Deployment Guide**
   - Environment-specific configuration instructions
   - Required environment variables
   - Profile selection guidance

### Performance Considerations

1. **Configuration Caching**
   - Spring Boot caches configuration after initial load
   - No performance impact from multiple configuration files
   - Profile-specific overrides resolved at startup

2. **Environment Variable Resolution**
   - Resolved once at startup
   - No runtime overhead
   - Consider using Spring Cloud Config for dynamic updates

3. **Validation Overhead**
   - Validation occurs only at startup
   - Minimal impact on application startup time
   - Benefits outweigh costs (fail-fast behavior)
