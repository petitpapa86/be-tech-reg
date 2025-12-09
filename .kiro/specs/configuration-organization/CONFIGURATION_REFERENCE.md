# Configuration Reference

## Overview

This document provides a comprehensive reference for all configuration properties in the RegTech application. The configuration is organized into a hierarchical structure with shared infrastructure configuration in the root `application.yml` and module-specific configuration in separate files.

## Configuration File Structure

```
regtech-app/src/main/resources/
├── application.yml                          # Root configuration (shared infrastructure)
└── logback-spring.xml                       # Logging configuration

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

## Environment Variables

The following environment variables are used across the application:

### Database
- `DB_URL`: Database connection URL (production only)
- `DB_USERNAME`: Database username (default: `postgres` in development)
- `DB_PASSWORD`: Database password (default: `dracons86` in development)

### AWS/S3
- `AWS_ACCESS_KEY_ID`: AWS access key for S3 operations
- `AWS_SECRET_ACCESS_KEY`: AWS secret key for S3 operations
- `AWS_S3_ENDPOINT`: S3 endpoint URL (for LocalStack in development)

### Security
- `JWT_SECRET`: Secret key for JWT token signing (default provided for development)
- `GOOGLE_CLIENT_ID`: Google OAuth2 client ID (optional)
- `GOOGLE_CLIENT_SECRET`: Google OAuth2 client secret (optional)
- `FACEBOOK_CLIENT_ID`: Facebook OAuth2 client ID (optional)
- `FACEBOOK_CLIENT_SECRET`: Facebook OAuth2 client secret (optional)

### Billing
- `STRIPE_API_KEY`: Stripe API key (development/test mode)
- `STRIPE_WEBHOOK_SECRET`: Stripe webhook signing secret
- `STRIPE_TEST_API_KEY`: Stripe test mode API key (development)
- `STRIPE_TEST_WEBHOOK_SECRET`: Stripe test mode webhook secret (development)
- `STRIPE_LIVE_API_KEY`: Stripe live mode API key (production)
- `STRIPE_LIVE_WEBHOOK_SECRET`: Stripe live mode webhook secret (production)

### External Services
- `BANK_REGISTRY_URL`: Bank registry service URL (default: `http://localhost:8081`)


---

## Root Configuration (application.yml)

### Spring Application Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.application.name` | String | `regtech` | Application name |
| `spring.profiles.active` | String | `development` | Active Spring profile |
| `spring.main.allow-bean-definition-overriding` | Boolean | `true` | Allow bean overriding |

### Database Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.datasource.url` | String | `jdbc:postgresql://localhost:5432/regtech` | Database connection URL |
| `spring.datasource.driver-class-name` | String | `org.postgresql.Driver` | JDBC driver class |
| `spring.datasource.username` | String | `${DB_USERNAME:postgres}` | Database username |
| `spring.datasource.password` | String | `${DB_PASSWORD:dracons86}` | Database password |
| `spring.jpa.hibernate.ddl-auto` | String | `update` | Hibernate DDL mode |
| `spring.jpa.show-sql` | Boolean | `false` | Show SQL statements in logs |
| `spring.jpa.open-in-view` | Boolean | `false` | Enable Open Session in View |

### Flyway Migration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.flyway.enabled` | Boolean | `false` | Enable Flyway migrations |
| `spring.flyway.locations` | List | See config | Migration script locations |

**Migration Locations:**
- `classpath:db/migration/common`
- `classpath:db/migration/ingestion`
- `classpath:db/migration/data-quality`
- `classpath:db/migration/risk-calculation`
- `classpath:db/migration/report-generation`
- `classpath:db/migration/billing`
- `classpath:db/migration/iam`

### Logging Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `logging.config` | String | `classpath:logback-spring.xml` | Logback configuration file |
| `logging.level.com.bcbs239.regtech` | String | `INFO` | Root package log level |
| `logging.level.com.bcbs239.regtech.core.events` | String | `INFO` | Event processing log level |
| `logging.level.com.bcbs239.regtech.core.inbox` | String | `INFO` | Inbox pattern log level |
| `logging.level.com.bcbs239.regtech.core.outbox` | String | `INFO` | Outbox pattern log level |
| `logging.level.com.bcbs239.regtech.modules.ingestion` | String | `INFO` | Ingestion module log level |
| `logging.level.com.bcbs239.regtech.dataquality` | String | `INFO` | Data quality module log level |
| `logging.level.com.bcbs239.regtech.riskcalculation` | String | `INFO` | Risk calculation module log level |
| `logging.level.com.bcbs239.regtech.reportgeneration` | String | `INFO` | Report generation module log level |
| `logging.level.com.bcbs239.regtech.billing` | String | `INFO` | Billing module log level |
| `logging.level.com.bcbs239.regtech.iam` | String | `INFO` | IAM module log level |

**Profile Overrides:**
- **Development**: All module log levels set to `DEBUG`
- **Production**: All module log levels remain at `INFO`

### Metrics and Monitoring

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `management.endpoints.web.exposure.include` | List | `health,info,metrics` | Exposed actuator endpoints |
| `management.endpoint.health.show-details` | String | `when-authorized` | Health detail visibility |
| `management.health.diskspace.enabled` | Boolean | `true` | Enable disk space health check |


### Security Configuration (iam.security)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `iam.security.jwt.secret` | String | `${JWT_SECRET:...}` | JWT signing secret |
| `iam.security.jwt.expiration` | Integer | `86400` | JWT expiration in seconds (24 hours) |
| `iam.security.password.min-length` | Integer | `12` | Minimum password length |
| `iam.security.password.require-uppercase` | Boolean | `true` | Require uppercase characters |
| `iam.security.password.require-lowercase` | Boolean | `true` | Require lowercase characters |
| `iam.security.password.require-digits` | Boolean | `true` | Require digit characters |
| `iam.security.password.require-special-chars` | Boolean | `true` | Require special characters |
| `iam.security.oauth2.google.client-id` | String | `${GOOGLE_CLIENT_ID:}` | Google OAuth2 client ID |
| `iam.security.oauth2.google.client-secret` | String | `${GOOGLE_CLIENT_SECRET:}` | Google OAuth2 client secret |
| `iam.security.oauth2.facebook.client-id` | String | `${FACEBOOK_CLIENT_ID:}` | Facebook OAuth2 client ID |
| `iam.security.oauth2.facebook.client-secret` | String | `${FACEBOOK_CLIENT_SECRET:}` | Facebook OAuth2 client secret |
| `iam.security.public-paths` | List | See config | Paths that don't require authentication |

**Public Paths (No Authentication Required):**
- `/api/public/**` - Public API endpoints
- `/api/auth/login` - Login endpoint
- `/api/auth/register` - Registration endpoint
- `/api/auth/forgot-password` - Password reset endpoint
- `/api/v1/users/register` - User registration endpoint
- `/api/health` - Health check endpoint
- `/actuator/health` - Actuator health endpoint
- `/api/v1/ingestion/health` - Ingestion health endpoint
- `/api/v1/data-quality/health` - Data quality health endpoint
- `/api/v1/data-quality/health/**` - Data quality health sub-paths
- `/api/v1/risk-calculation/health` - Risk calculation health endpoint
- `/api/v1/risk-calculation/health/**` - Risk calculation health sub-paths
- `/api/v1/report-generation/health` - Report generation health endpoint
- `/api/v1/report-generation/health/**` - Report generation health sub-paths
- `/swagger-ui/**` - Swagger UI
- `/v3/api-docs/**` - OpenAPI documentation

### Authorization Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `iam.authorization.cache.enabled` | Boolean | `true` | Enable authorization caching |
| `iam.authorization.cache.ttl` | Integer | `300` | Cache TTL in seconds (5 minutes) |
| `iam.authorization.multi-tenant.enabled` | Boolean | `true` | Enable multi-tenancy |
| `iam.authorization.multi-tenant.default-organization` | String | `default-org` | Default organization ID |
| `iam.authorization.permissions.strict-mode` | Boolean | `true` | Enable strict permission checking |
| `iam.authorization.permissions.audit-enabled` | Boolean | `true` | Enable permission audit logging |

### Event Processing Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regtech.outbox.enabled` | Boolean | `true` | Enable outbox pattern |
| `regtech.outbox.processing-interval` | Integer | `30000` | Processing interval in ms (30 seconds) |
| `regtech.outbox.retry-interval` | Integer | `60000` | Retry interval in ms (1 minute) |
| `regtech.outbox.max-retries` | Integer | `3` | Maximum retry attempts |
| `regtech.outbox.cleanup-interval` | Integer | `86400000` | Cleanup interval in ms (24 hours) |
| `regtech.outbox.cleanup-retention-days` | Integer | `30` | Retention period in days |
| `regtech.inbox.enabled` | Boolean | `true` | Enable inbox pattern |
| `regtech.inbox.processing-interval` | Integer | `10000` | Processing interval in ms (10 seconds) |
| `regtech.inbox.batch-size` | Integer | `20` | Batch size for processing |
| `regtech.inbox.parallel-processing-enabled` | Boolean | `false` | Enable parallel processing |
| `inbox.poll-interval` | Duration | `5s` | Inbox poll interval |
| `inbox.batch-size` | Integer | `10` | Inbox batch size |
| `outbox.poll-interval` | Duration | `30s` | Outbox poll interval |
| `outbox.batch-size` | Integer | `10` | Outbox batch size |

### Event Retry Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regtech.event.retry.interval` | Integer | `60000` | Retry check interval in ms (60 seconds) |
| `regtech.event.retry.batch-size` | Integer | `10` | Events to process per batch |
| `regtech.event.retry.max-retries` | Integer | `5` | Maximum retry attempts |
| `regtech.event.retry.enabled` | Boolean | `true` | Enable retry processing |

### Bank Registry Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `regtech.bank-registry.base-url` | String | `${BANK_REGISTRY_URL:http://localhost:8081}` | Bank registry service URL |
| `regtech.bank-registry.timeout` | Integer | `30000` | Request timeout in ms (30 seconds) |
| `regtech.bank-registry.retry-attempts` | Integer | `3` | Retry attempts for failed requests |


---

## Ingestion Module Configuration (application-ingestion.yml)

### Module Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ingestion.enabled` | Boolean | `true` | Enable ingestion module |

### File Upload Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ingestion.file.max-size` | Integer | `524288000` | Maximum file size in bytes (500MB) |
| `ingestion.file.supported-types` | List | `application/json`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | Supported MIME types |

### Storage Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ingestion.storage.type` | String | `s3` | Storage type: `s3` or `local` |
| `ingestion.storage.s3.bucket` | String | `regtech-ingestion` | S3 bucket name |
| `ingestion.storage.s3.region` | String | `us-east-1` | AWS region |
| `ingestion.storage.s3.prefix` | String | `raw/` | S3 object key prefix |
| `ingestion.storage.s3.access-key` | String | `${AWS_ACCESS_KEY_ID:}` | AWS access key |
| `ingestion.storage.s3.secret-key` | String | `${AWS_SECRET_ACCESS_KEY:}` | AWS secret key |
| `ingestion.storage.s3.endpoint` | String | `${AWS_S3_ENDPOINT:}` | S3 endpoint (for LocalStack) |
| `ingestion.storage.s3.encryption` | String | `AES256` | S3 encryption type |
| `ingestion.storage.local.base-path` | String | `./data/ingestion` | Local filesystem base path |
| `ingestion.storage.local.create-directories` | Boolean | `true` | Auto-create directories |

**Profile Overrides:**
- **Development**: `storage.type` = `local`
- **Production**: `storage.type` = `s3`

### Async Thread Pool Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ingestion.async.enabled` | Boolean | `true` | Enable async processing |
| `ingestion.async.core-pool-size` | Integer | `5` | Core thread pool size |
| `ingestion.async.max-pool-size` | Integer | `10` | Maximum thread pool size |
| `ingestion.async.queue-capacity` | Integer | `100` | Task queue capacity |
| `ingestion.async.thread-name-prefix` | String | `ingestion-async-` | Thread name prefix |
| `ingestion.async.await-termination-seconds` | Integer | `60` | Graceful shutdown timeout |

**Profile Overrides:**
- **Development**: `core-pool-size` = `2`, `max-pool-size` = `4`
- **Production**: `core-pool-size` = `10`, `max-pool-size` = `20`

### Processing Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ingestion.processing.async-enabled` | Boolean | `true` | Enable async processing |
| `ingestion.processing.thread-pool-size` | Integer | `10` | Thread pool size |
| `ingestion.processing.queue-capacity` | Integer | `100` | Queue capacity |

### Performance Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ingestion.performance.max-concurrent-files` | Integer | `4` | Maximum concurrent file processing |
| `ingestion.performance.chunk-size` | Integer | `10000` | Chunk size for large files |

### Parser Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ingestion.parser.default-max-records` | Integer | `10000` | Maximum records per file |


---

## Data Quality Module Configuration (application-data-quality.yml)

### Module Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `data-quality.enabled` | Boolean | `true` | Enable data quality module |

### Storage Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `data-quality.storage.type` | String | `s3` | Storage type: `s3` or `local` |
| `data-quality.storage.s3.bucket` | String | `regtech-data-quality` | S3 bucket name |
| `data-quality.storage.s3.region` | String | `us-east-1` | AWS region |
| `data-quality.storage.s3.prefix` | String | `quality/` | S3 object key prefix |
| `data-quality.storage.s3.access-key` | String | `${AWS_ACCESS_KEY_ID:}` | AWS access key |
| `data-quality.storage.s3.secret-key` | String | `${AWS_SECRET_ACCESS_KEY:}` | AWS secret key |
| `data-quality.storage.s3.endpoint` | String | `${AWS_S3_ENDPOINT:}` | S3 endpoint (for LocalStack) |
| `data-quality.storage.s3.encryption` | String | `AES256` | S3 encryption type |
| `data-quality.storage.local.base-path` | String | `./data/quality` | Local filesystem base path |
| `data-quality.storage.local.create-directories` | Boolean | `true` | Auto-create directories |

**Profile Overrides:**
- **Development**: `storage.type` = `local`
- **Production**: `storage.type` = `s3`

### Async Thread Pool Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `data-quality.async.enabled` | Boolean | `true` | Enable async processing |
| `data-quality.async.core-pool-size` | Integer | `5` | Core thread pool size |
| `data-quality.async.max-pool-size` | Integer | `10` | Maximum thread pool size |
| `data-quality.async.queue-capacity` | Integer | `100` | Task queue capacity |
| `data-quality.async.thread-name-prefix` | String | `data-quality-async-` | Thread name prefix |
| `data-quality.async.await-termination-seconds` | Integer | `60` | Graceful shutdown timeout |

**Profile Overrides:**
- **Development**: `core-pool-size` = `2`, `max-pool-size` = `4`
- **Production**: `core-pool-size` = `10`, `max-pool-size` = `20`

### Rules Engine Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `data-quality.rules-engine.enabled` | Boolean | `true` | Enable database-driven rules engine |
| `data-quality.rules-engine.cache-enabled` | Boolean | `true` | Enable rule caching |
| `data-quality.rules-engine.cache-ttl` | Integer | `300` | Cache TTL in seconds (5 minutes) |
| `data-quality.rules-engine.parallel-execution` | Boolean | `false` | Execute rules in parallel |

### Rules Migration Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `data-quality.rules-migration.enabled` | Boolean | `true` | Run initial rules population |


---

## Risk Calculation Module Configuration (application-risk-calculation.yml)

### Module Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `risk-calculation.enabled` | Boolean | `true` | Enable risk calculation module |

### Storage Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `risk-calculation.storage.type` | String | `s3` | Storage type: `s3` or `local` |
| `risk-calculation.storage.s3.bucket` | String | `regtech-risk-calculations` | S3 bucket name |
| `risk-calculation.storage.s3.region` | String | `us-east-1` | AWS region |
| `risk-calculation.storage.s3.prefix` | String | `calculations/` | S3 object key prefix |
| `risk-calculation.storage.s3.access-key` | String | `${AWS_ACCESS_KEY_ID:}` | AWS access key |
| `risk-calculation.storage.s3.secret-key` | String | `${AWS_SECRET_ACCESS_KEY:}` | AWS secret key |
| `risk-calculation.storage.s3.endpoint` | String | `${AWS_S3_ENDPOINT:}` | S3 endpoint (for LocalStack) |
| `risk-calculation.storage.s3.encryption` | String | `AES256` | S3 encryption type |
| `risk-calculation.storage.local.base-path` | String | `./data/risk-calculations` | Local filesystem base path |
| `risk-calculation.storage.local.create-directories` | Boolean | `true` | Auto-create directories |

**Profile Overrides:**
- **Development**: `storage.type` = `local`
- **Production**: `storage.type` = `s3`

### Async Thread Pool Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `risk-calculation.async.enabled` | Boolean | `true` | Enable async processing |
| `risk-calculation.async.core-pool-size` | Integer | `5` | Core thread pool size |
| `risk-calculation.async.max-pool-size` | Integer | `10` | Maximum thread pool size |
| `risk-calculation.async.queue-capacity` | Integer | `50` | Task queue capacity |
| `risk-calculation.async.thread-name-prefix` | String | `risk-calc-async-` | Thread name prefix |
| `risk-calculation.async.await-termination-seconds` | Integer | `60` | Graceful shutdown timeout |

**Profile Overrides:**
- **Development**: `core-pool-size` = `2`, `max-pool-size` = `4`
- **Production**: `core-pool-size` = `10`, `max-pool-size` = `20`

### Processing Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `risk-calculation.processing.async-enabled` | Boolean | `true` | Enable async processing |
| `risk-calculation.processing.thread-pool-size` | Integer | `5` | Thread pool size |
| `risk-calculation.processing.queue-capacity` | Integer | `50` | Queue capacity |
| `risk-calculation.processing.timeout-seconds` | Integer | `300` | Processing timeout (5 minutes) |

**Profile Overrides:**
- **Development**: `thread-pool-size` = `2`
- **Production**: `thread-pool-size` = `10`

### Currency Conversion Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `risk-calculation.currency.base-currency` | String | `EUR` | Base currency for conversions |
| `risk-calculation.currency.cache-enabled` | Boolean | `true` | Enable exchange rate caching |
| `risk-calculation.currency.cache-ttl` | Integer | `3600` | Cache TTL in seconds (1 hour) |
| `risk-calculation.currency.provider.timeout` | Integer | `30000` | Provider timeout in ms (30 seconds) |
| `risk-calculation.currency.provider.retry-attempts` | Integer | `3` | Retry attempts for failed requests |
| `risk-calculation.currency.provider.mock-enabled` | Boolean | `false` | Use mock provider |

**Profile Overrides:**
- **Development**: `provider.mock-enabled` = `true`
- **Production**: `provider.mock-enabled` = `false`

### Performance Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `risk-calculation.performance.max-concurrent-calculations` | Integer | `3` | Maximum concurrent calculations |
| `risk-calculation.performance.streaming-parser-enabled` | Boolean | `true` | Enable streaming JSON parser |
| `risk-calculation.performance.memory-threshold-mb` | Integer | `512` | Memory threshold for streaming |

### Geographic Classification Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `risk-calculation.geographic.home-country` | String | `IT` | Home country code |
| `risk-calculation.geographic.eu-countries` | List | See config | EU country codes |

**EU Countries:** AT, BE, BG, HR, CY, CZ, DK, EE, FI, FR, DE, GR, HU, IE, IT, LV, LT, LU, MT, NL, PL, PT, RO, SK, SI, ES, SE

### Concentration Thresholds

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `risk-calculation.concentration.high-threshold` | Double | `0.25` | High concentration threshold (HHI > 0.25) |
| `risk-calculation.concentration.moderate-threshold` | Double | `0.15` | Moderate concentration threshold (HHI 0.15-0.25) |
| `risk-calculation.concentration.precision` | Integer | `4` | Decimal places for HHI calculation |


---

## Report Generation Module Configuration (application-report-generation.yml)

### Module Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `report-generation.enabled` | Boolean | `true` | Enable report generation module |

### S3 Storage Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `report-generation.s3.bucket` | String | `risk-analysis` | S3 bucket name |
| `report-generation.s3.html-prefix` | String | `reports/html/` | HTML report prefix |
| `report-generation.s3.xbrl-prefix` | String | `reports/xbrl/` | XBRL report prefix |
| `report-generation.s3.encryption` | String | `AES256` | S3 encryption type |
| `report-generation.s3.presigned-url-expiration-hours` | Integer | `1` | Presigned URL expiration (1 hour) |

**Profile Overrides:**
- **Development**: `bucket` = `risk-analysis-dev`
- **Production**: `bucket` = `risk-analysis-production`

### File Path Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `report-generation.file-paths.calculation-pattern` | String | `calculated/calc_batch_{batchId}.json` | Calculation file pattern |
| `report-generation.file-paths.quality-pattern` | String | `quality/quality_batch_{batchId}.json` | Quality file pattern |
| `report-generation.file-paths.local-base-path` | String | `/data` | Local filesystem base path |
| `report-generation.file-paths.malformed-json-path` | String | `/tmp/malformed-json/` | Malformed JSON storage path |

### Fallback Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `report-generation.fallback.local-path` | String | `/tmp/deferred-reports/` | Local fallback path |
| `report-generation.fallback.retry-interval-minutes` | Integer | `30` | Retry interval (30 minutes) |
| `report-generation.fallback.max-retry-attempts` | Integer | `5` | Maximum retry attempts |

### Async Thread Pool Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `report-generation.async.core-pool-size` | Integer | `2` | Core thread pool size |
| `report-generation.async.max-pool-size` | Integer | `5` | Maximum thread pool size |
| `report-generation.async.queue-capacity` | Integer | `100` | Task queue capacity |
| `report-generation.async.thread-name-prefix` | String | `report-gen-` | Thread name prefix |
| `report-generation.async.await-termination-seconds` | Integer | `60` | Graceful shutdown timeout |

**Profile Overrides:**
- **Development**: `core-pool-size` = `2`, `max-pool-size` = `3`, `queue-capacity` = `50`
- **Production**: `core-pool-size` = `5`, `max-pool-size` = `10`, `queue-capacity` = `200`

### Event Coordination Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `report-generation.coordination.event-expiration-hours` | Integer | `24` | Event expiration (24 hours) |
| `report-generation.coordination.cleanup-interval-minutes` | Integer | `30` | Cleanup interval (30 minutes) |

### Performance Targets (milliseconds)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `report-generation.performance.data-fetch-target` | Integer | `800` | Data fetch target time |
| `report-generation.performance.html-generation-target` | Integer | `2000` | HTML generation target time |
| `report-generation.performance.xbrl-generation-target` | Integer | `800` | XBRL generation target time |
| `report-generation.performance.s3-upload-target` | Integer | `800` | S3 upload target time |
| `report-generation.performance.total-generation-target` | Integer | `7000` | Total generation target time |
| `report-generation.performance.file-download-timeout` | Integer | `30000` | File download timeout (30 seconds) |

### Retry Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `report-generation.retry.max-retries` | Integer | `3` | Maximum retry attempts |
| `report-generation.retry.backoff-intervals-seconds` | List | `[1, 2, 4, 8, 16]` | Backoff intervals |
| `report-generation.retry.retryable-exceptions` | List | See config | Exceptions that trigger retry |
| `report-generation.retry.non-retryable-exceptions` | List | See config | Exceptions that don't trigger retry |

**Retryable Exceptions:**
- `software.amazon.awssdk.services.s3.model.S3Exception`
- `java.io.IOException`
- `java.net.SocketTimeoutException`

**Non-Retryable Exceptions:**
- `software.amazon.awssdk.services.s3.model.NoSuchBucketException`
- `software.amazon.awssdk.core.exception.SdkClientException`

### AWS Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `aws.region` | String | `eu-west-1` | AWS region |
| `aws.s3.endpoint` | String | `${AWS_S3_ENDPOINT:}` | S3 endpoint (for LocalStack) |
| `aws.access-key-id` | String | `${AWS_ACCESS_KEY_ID:}` | AWS access key |
| `aws.secret-access-key` | String | `${AWS_SECRET_ACCESS_KEY:}` | AWS secret key |

**Profile Overrides:**
- **Development**: `aws.s3.endpoint` = `http://localhost:4566`


### Resilience4j Circuit Breaker Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `resilience4j.circuitbreaker.configs.default.registerHealthIndicator` | Boolean | `true` | Register health indicator |
| `resilience4j.circuitbreaker.configs.default.slidingWindowSize` | Integer | `10` | Sliding window size |
| `resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls` | Integer | `5` | Minimum calls before evaluation |
| `resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState` | Integer | `1` | Test calls in half-open state |
| `resilience4j.circuitbreaker.configs.default.automaticTransitionFromOpenToHalfOpenEnabled` | Boolean | `true` | Auto transition to half-open |
| `resilience4j.circuitbreaker.configs.default.waitDurationInOpenState` | Duration | `5m` | Wait duration in open state |
| `resilience4j.circuitbreaker.configs.default.failureRateThreshold` | Integer | `50` | Failure rate threshold (%) |
| `resilience4j.circuitbreaker.instances.s3-upload.slidingWindowType` | String | `COUNT_BASED` | Sliding window type |
| `resilience4j.circuitbreaker.instances.s3-upload.slidingWindowSize` | Integer | `10` | Sliding window size |
| `resilience4j.circuitbreaker.instances.s3-upload.minimumNumberOfCalls` | Integer | `10` | Minimum calls before evaluation |
| `resilience4j.circuitbreaker.instances.s3-upload.failureRateThreshold` | Integer | `50` | Failure rate threshold (%) |
| `resilience4j.circuitbreaker.instances.s3-upload.slowCallRateThreshold` | Integer | `50` | Slow call rate threshold (%) |
| `resilience4j.circuitbreaker.instances.s3-upload.slowCallDurationThreshold` | Duration | `10s` | Slow call duration threshold |
| `resilience4j.circuitbreaker.instances.s3-upload.waitDurationInOpenState` | Duration | `5m` | Wait duration in open state |

**Profile Overrides:**
- **Development**: `failureRateThreshold` = `80`, `waitDurationInOpenState` = `1m`, `minimumNumberOfCalls` = `5`
- **Production**: `failureRateThreshold` = `50`, `waitDurationInOpenState` = `5m`, `minimumNumberOfCalls` = `10`

### Resilience4j Retry Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `resilience4j.retry.configs.default.maxAttempts` | Integer | `3` | Maximum retry attempts |
| `resilience4j.retry.configs.default.waitDuration` | Duration | `1s` | Wait duration between retries |
| `resilience4j.retry.configs.default.enableExponentialBackoff` | Boolean | `true` | Enable exponential backoff |
| `resilience4j.retry.configs.default.exponentialBackoffMultiplier` | Integer | `2` | Backoff multiplier |
| `resilience4j.retry.instances.s3-operations.maxAttempts` | Integer | `3` | Maximum retry attempts |
| `resilience4j.retry.instances.s3-operations.waitDuration` | Duration | `1s` | Wait duration between retries |

### Thymeleaf Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.thymeleaf.prefix` | String | `classpath:/templates/reports/` | Template prefix |
| `spring.thymeleaf.suffix` | String | `.html` | Template suffix |
| `spring.thymeleaf.mode` | String | `HTML` | Template mode |
| `spring.thymeleaf.encoding` | String | `UTF-8` | Template encoding |
| `spring.thymeleaf.cache` | Boolean | `true` | Enable template caching |
| `spring.thymeleaf.check-template-location` | Boolean | `true` | Check template location |

**Profile Overrides:**
- **Development**: `cache` = `false`
- **Production**: `cache` = `true`


---

## Billing Module Configuration (application-billing.yml)

### Module Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `billing.enabled` | Boolean | `true` | Enable billing module |

### Stripe Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `billing.stripe.api-key` | String | `${STRIPE_API_KEY:}` | Stripe API key |
| `billing.stripe.webhook-secret` | String | `${STRIPE_WEBHOOK_SECRET:}` | Stripe webhook signing secret |
| `stripe.api.key` | String | `${STRIPE_API_KEY:}` | Stripe SDK API key |
| `stripe.webhook.secret` | String | `${STRIPE_WEBHOOK_SECRET:}` | Stripe SDK webhook secret |

**Profile Overrides:**
- **Development**: Uses `STRIPE_TEST_API_KEY` and `STRIPE_TEST_WEBHOOK_SECRET`
- **Production**: Uses `STRIPE_LIVE_API_KEY` and `STRIPE_LIVE_WEBHOOK_SECRET`

### Subscription Tier Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `billing.tiers.starter.monthly-price` | Double | `299.99` | Starter tier monthly price |
| `billing.tiers.starter.currency` | String | `USD` | Starter tier currency |
| `billing.tiers.starter.exposure-limit` | Integer | `10000` | Starter tier exposure limit |

### Dunning Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `billing.dunning.reminder-intervals.step1` | Integer | `7` | Days after first failure for step 1 |
| `billing.dunning.reminder-intervals.step2` | Integer | `14` | Days after first failure for step 2 |
| `billing.dunning.reminder-intervals.step3` | Integer | `21` | Days after first failure for step 3 |
| `billing.dunning.final-action-delay` | Integer | `30` | Days before final action |

**Profile Overrides:**
- **Development**: `step1` = `1`, `step2` = `2`, `step3` = `3`, `final-action-delay` = `5`

### Invoice Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `billing.invoices.due-days` | Integer | `30` | Payment due within days |
| `billing.invoices.currency` | String | `USD` | Invoice currency |

### Billing Cycle Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `billing.billing-cycle.timezone` | String | `America/New_York` | Billing timezone |
| `billing.billing-cycle.billing-day` | Integer | `1` | Day of month to bill |

### Outbox Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `billing.outbox.enabled` | Boolean | `true` | Enable outbox pattern |
| `billing.outbox.processing-interval` | Integer | `30000` | Processing interval in ms (30 seconds) |
| `billing.outbox.retry-interval` | Integer | `60000` | Retry interval in ms (1 minute) |
| `billing.outbox.max-retries` | Integer | `3` | Maximum retry attempts |
| `billing.outbox.cleanup-interval` | Integer | `86400000` | Cleanup interval in ms (24 hours) |
| `billing.outbox.cleanup-retention-days` | Integer | `30` | Retention period in days |

**Profile Overrides:**
- **Development**: `processing-interval` = `10000` (10 seconds)

### Scheduling Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `billing.scheduling.monthly-billing.enabled` | Boolean | `true` | Enable monthly billing job |
| `billing.scheduling.monthly-billing.cron` | String | `0 0 1 * *` | Cron expression (midnight on 1st) |
| `billing.scheduling.monthly-billing.timezone` | String | `America/New_York` | Job timezone |
| `billing.scheduling.dunning-process.enabled` | Boolean | `true` | Enable dunning process job |
| `billing.scheduling.dunning-process.cron` | String | `0 0 2 * *` | Cron expression (2 AM daily) |
| `billing.scheduling.dunning-process.timezone` | String | `America/New_York` | Job timezone |
| `billing.scheduling.dunning-process.thread-pool-size` | Integer | `5` | Thread pool size |

**Profile Overrides:**
- **Development**: Both jobs `enabled` = `false`
- **Production**: Both jobs `enabled` = `true`, `dunning-process.thread-pool-size` = `10`

### Notification Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `billing.notifications.email.enabled` | Boolean | `true` | Enable email notifications |
| `billing.notifications.email.templates.payment-reminder` | String | `payment-reminder.html` | Payment reminder template |
| `billing.notifications.email.templates.payment-failed` | String | `payment-failed.html` | Payment failed template |
| `billing.notifications.email.templates.subscription-cancelled` | String | `subscription-cancelled.html` | Subscription cancelled template |
| `billing.notifications.sms.enabled` | Boolean | `false` | Enable SMS notifications |
| `billing.notifications.push.enabled` | Boolean | `false` | Enable push notifications |


---

## IAM Module Configuration (application-iam.yml)

### Module Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `iam.enabled` | Boolean | `true` | Enable IAM module |

**Note:** Most security configuration (JWT, OAuth2, public paths) is in the root `application.yml` under `iam.security` as it is a shared concern across all modules.

### User Management Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `iam.user-management.password-reset-token-expiration` | Integer | `3600` | Password reset token expiration in seconds (1 hour) |
| `iam.user-management.email-verification-token-expiration` | Integer | `86400` | Email verification token expiration in seconds (24 hours) |
| `iam.user-management.lockout.enabled` | Boolean | `true` | Enable account lockout |
| `iam.user-management.lockout.max-failed-attempts` | Integer | `5` | Maximum failed login attempts |
| `iam.user-management.lockout.lockout-duration` | Integer | `1800` | Lockout duration in seconds (30 minutes) |

**Profile Overrides:**
- **Development**: `password-reset-token-expiration` = `7200` (2 hours), `lockout.enabled` = `false`
- **Production**: `password-reset-token-expiration` = `1800` (30 minutes), `lockout.enabled` = `true`, `lockout.max-failed-attempts` = `3`, `lockout.lockout-duration` = `3600` (1 hour)

### Session Management

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `iam.session.max-concurrent-sessions` | Integer | `3` | Maximum concurrent sessions per user |
| `iam.session.timeout` | Integer | `3600` | Session timeout in seconds (1 hour) |
| `iam.session.remember-me-validity` | Integer | `2592000` | Remember-me token validity in seconds (30 days) |

**Profile Overrides:**
- **Development**: `max-concurrent-sessions` = `10`, `timeout` = `7200` (2 hours)
- **Production**: `max-concurrent-sessions` = `2`, `timeout` = `1800` (30 minutes)

---

## Common Configuration Patterns

### Profile Selection

Activate a profile using one of these methods:

1. **Application property:**
   ```yaml
   spring:
     profiles:
       active: development
   ```

2. **Environment variable:**
   ```bash
   export SPRING_PROFILES_ACTIVE=production
   ```

3. **Command-line argument:**
   ```bash
   java -jar app.jar --spring.profiles.active=production
   ```

4. **JVM system property:**
   ```bash
   java -Dspring.profiles.active=production -jar app.jar
   ```

### Storage Type Selection

All modules that support storage use a consistent pattern:

```yaml
{module}.storage.type: s3  # or 'local'
```

- **Development**: Typically uses `local` for easier testing
- **Production**: Uses `s3` for scalability and durability

### Thread Pool Sizing Guidelines

Thread pool sizes should be tuned based on:
- Available CPU cores
- Expected workload
- Memory constraints
- I/O vs CPU-bound operations

**General Guidelines:**
- **Development**: Small pools (2-4 threads) to conserve resources
- **Production**: Larger pools (5-20 threads) based on load testing
- **Core Pool Size**: Should be ≤ Max Pool Size
- **Queue Capacity**: Buffer for burst traffic

### Environment Variable Best Practices

1. **Never commit secrets** to source control
2. **Use defaults for development** to simplify local setup
3. **Require explicit values in production** using `${VAR_NAME}` without defaults
4. **Document all required variables** in deployment guides
5. **Use consistent naming** (e.g., `AWS_*`, `DB_*`, `STRIPE_*`)


---

## Common Configuration Scenarios

### Scenario 1: Local Development Setup

**Goal**: Run the application locally with minimal external dependencies.

**Configuration:**
1. Use `development` profile
2. Use local filesystem storage
3. Use in-memory or local PostgreSQL
4. Disable scheduled jobs
5. Use mock external services

**Example:**
```yaml
spring:
  profiles:
    active: development

# All modules automatically use local storage in development profile
# No AWS credentials needed
# Reduced thread pools for resource conservation
```

### Scenario 2: Production Deployment

**Goal**: Deploy to production with full S3 integration and optimized performance.

**Configuration:**
1. Use `production` profile
2. Use S3 storage for all modules
3. Use production database with connection pooling
4. Enable all scheduled jobs
5. Use real external services

**Required Environment Variables:**
```bash
export SPRING_PROFILES_ACTIVE=production
export DB_URL=jdbc:postgresql://prod-db:5432/regtech
export DB_USERNAME=regtech_prod
export DB_PASSWORD=<secure-password>
export AWS_ACCESS_KEY_ID=<aws-key>
export AWS_SECRET_ACCESS_KEY=<aws-secret>
export JWT_SECRET=<secure-jwt-secret>
export STRIPE_LIVE_API_KEY=<stripe-key>
export STRIPE_LIVE_WEBHOOK_SECRET=<stripe-webhook-secret>
```

### Scenario 3: Testing with LocalStack

**Goal**: Test S3 integration locally using LocalStack.

**Configuration:**
1. Use `development` profile
2. Override storage type to `s3`
3. Set S3 endpoint to LocalStack

**Example:**
```yaml
spring:
  profiles:
    active: development

# Override in application-ingestion.yml
ingestion:
  storage:
    type: s3
    s3:
      endpoint: http://localhost:4566

# Repeat for other modules
```

### Scenario 4: GCP Deployment with JSON Logging

**Goal**: Deploy to Google Cloud Platform with structured JSON logging.

**Configuration:**
1. Use `production,gcp` profiles (comma-separated)
2. Enable structured logging
3. Use GCP-specific configurations

**Example:**
```bash
export SPRING_PROFILES_ACTIVE=production,gcp
```

This enables:
- Production settings (S3, optimized pools, etc.)
- JSON structured logging for Google Cloud Logging
- Increased verbosity for event processing

### Scenario 5: Module-Specific Configuration Override

**Goal**: Override specific module configuration without changing the module file.

**Method**: Use environment variables or command-line arguments.

**Examples:**

Override ingestion thread pool:
```bash
export INGESTION_ASYNC_CORE_POOL_SIZE=20
export INGESTION_ASYNC_MAX_POOL_SIZE=40
```

Override risk calculation currency provider:
```bash
export RISK_CALCULATION_CURRENCY_PROVIDER_MOCK_ENABLED=false
```

Override report generation S3 bucket:
```bash
export REPORT_GENERATION_S3_BUCKET=custom-bucket-name
```

**Note**: Spring Boot automatically maps environment variables to properties:
- `INGESTION_ASYNC_CORE_POOL_SIZE` → `ingestion.async.core-pool-size`
- Underscores become dots
- All uppercase becomes lowercase with hyphens


---

## Troubleshooting

### Configuration Not Loading

**Symptom**: Module configuration properties are null or using defaults.

**Possible Causes:**
1. Configuration file not in correct location
2. File naming doesn't match Spring Boot conventions
3. `@ConfigurationProperties` class not registered
4. Profile not activated

**Solutions:**
1. Verify file location: `{module}/infrastructure/src/main/resources/application-{module}.yml`
2. Verify file naming: Must be `application-{module}.yml`
3. Ensure `@ConfigurationProperties` class has `@Configuration` or is registered via `@EnableConfigurationProperties`
4. Check active profile: `spring.profiles.active`

### Environment Variables Not Resolving

**Symptom**: Properties show `${VAR_NAME}` instead of actual values.

**Possible Causes:**
1. Environment variable not set
2. Incorrect variable name
3. No default value provided

**Solutions:**
1. Set environment variable: `export VAR_NAME=value`
2. Check variable name matches exactly (case-sensitive)
3. Add default value: `${VAR_NAME:default-value}`

### Profile-Specific Configuration Not Applied

**Symptom**: Profile-specific values not overriding defaults.

**Possible Causes:**
1. Profile not activated
2. Profile section syntax incorrect
3. Property path doesn't match

**Solutions:**
1. Verify active profile: Check logs for "The following profiles are active: ..."
2. Verify profile section syntax:
   ```yaml
   ---
   spring:
     config:
       activate:
         on-profile: development
   ```
3. Ensure property paths match exactly between default and profile sections

### Thread Pool Not Created

**Symptom**: Async operations not executing or using wrong thread pool.

**Possible Causes:**
1. `@EnableAsync` not present on configuration class
2. Thread pool bean name doesn't match `@Async` annotation
3. Async configuration disabled

**Solutions:**
1. Add `@EnableAsync` to async configuration class
2. Verify bean name matches: `@Bean(name = "{module}TaskExecutor")`
3. Check `{module}.async.enabled` is `true`

### S3 Connection Failures

**Symptom**: S3 operations fail with authentication or connection errors.

**Possible Causes:**
1. AWS credentials not set
2. Incorrect S3 endpoint
3. Bucket doesn't exist
4. Network connectivity issues

**Solutions:**
1. Set `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
2. For LocalStack: Set `AWS_S3_ENDPOINT=http://localhost:4566`
3. Create bucket or verify bucket name
4. Check network connectivity and firewall rules

### Database Connection Failures

**Symptom**: Application fails to start with database connection errors.

**Possible Causes:**
1. Database not running
2. Incorrect connection URL
3. Invalid credentials
4. Database doesn't exist

**Solutions:**
1. Start PostgreSQL: `docker-compose up -d postgres`
2. Verify `spring.datasource.url`
3. Verify `DB_USERNAME` and `DB_PASSWORD`
4. Create database: `createdb regtech`

### Validation Errors on Startup

**Symptom**: Application fails to start with validation errors.

**Possible Causes:**
1. Required property missing
2. Invalid property value
3. Constraint violation (e.g., core-pool-size > max-pool-size)

**Solutions:**
1. Check error message for missing property name
2. Verify property value matches expected type and format
3. Review validation constraints in `@ConfigurationProperties` class


---

## Configuration Validation

### Startup Validation

The application validates configuration at startup using `@ConfigurationProperties` validation annotations:

**Common Validation Annotations:**
- `@NotNull`: Property must be present
- `@NotEmpty`: String/collection must not be empty
- `@Min(value)`: Numeric value must be >= value
- `@Max(value)`: Numeric value must be <= value
- `@Pattern(regexp)`: String must match regex pattern
- `@Positive`: Numeric value must be > 0
- `@PositiveOrZero`: Numeric value must be >= 0

**Example Validation:**
```java
@ConfigurationProperties(prefix = "ingestion")
@Validated
public class IngestionProperties {
    @NotNull
    private Boolean enabled;
    
    @Positive
    @Max(1073741824) // 1GB max
    private Long fileMaxSize;
    
    @Valid
    private AsyncProperties async;
    
    public static class AsyncProperties {
        @Positive
        private Integer corePoolSize;
        
        @Positive
        private Integer maxPoolSize;
        
        // Validation: corePoolSize <= maxPoolSize
        @AssertTrue(message = "core-pool-size must be <= max-pool-size")
        public boolean isValidPoolSizes() {
            return corePoolSize <= maxPoolSize;
        }
    }
}
```

### Testing Configuration

**Unit Tests:**
```java
@SpringBootTest
@TestPropertySource(properties = {
    "ingestion.enabled=true",
    "ingestion.async.core-pool-size=5"
})
class ConfigurationPropertiesTest {
    @Autowired
    private IngestionProperties properties;
    
    @Test
    void shouldLoadConfiguration() {
        assertThat(properties.getEnabled()).isTrue();
        assertThat(properties.getAsync().getCorePoolSize()).isEqualTo(5);
    }
}
```

**Integration Tests:**
```java
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextLoadingTest {
    @Test
    void contextLoads() {
        // If context loads, configuration is valid
    }
}
```

---

## Migration from Old Configuration

If you're migrating from an older configuration structure, follow these steps:

### Step 1: Identify Module-Specific Configuration

Review the root `application.yml` and identify properties that belong to specific modules:
- Properties with module-specific prefixes (e.g., `ingestion.*`, `risk-calculation.*`)
- Properties only used by one module
- Module-specific storage, processing, or business logic settings

### Step 2: Create Module Configuration Files

Create `application-{module}.yml` files in each module's infrastructure resources directory:
```
{module}/infrastructure/src/main/resources/application-{module}.yml
```

### Step 3: Move Configuration

Copy module-specific configuration from root `application.yml` to the module file:

**Before (in root application.yml):**
```yaml
ingestion:
  file:
    max-size: 524288000
  storage:
    type: s3
```

**After (in application-ingestion.yml):**
```yaml
ingestion:
  file:
    max-size: 524288000
  storage:
    type: s3
```

### Step 4: Update Configuration Properties Classes

Update `@ConfigurationProperties` classes to use the new prefix:

**Before:**
```java
@ConfigurationProperties(prefix = "app.ingestion")
```

**After:**
```java
@ConfigurationProperties(prefix = "ingestion")
```

### Step 5: Test Configuration Loading

1. Run integration tests to verify configuration loads correctly
2. Start application and check logs for configuration values
3. Verify module functionality works as expected

### Step 6: Remove Duplicates

Once verified, remove the duplicate configuration from root `application.yml`:
- Keep only shared infrastructure configuration
- Remove module-specific properties

### Step 7: Update Documentation

Update any deployment documentation, runbooks, or configuration guides to reflect the new structure.

---

## Additional Resources

- [Spring Boot Configuration Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html)
- [Spring Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Maintained By**: RegTech Development Team
