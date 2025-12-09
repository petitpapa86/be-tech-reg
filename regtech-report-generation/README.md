# Report Generation Module

## Overview

The Report Generation Module generates comprehensive HTML and XBRL-XML reports combining Large Exposures (Grandi Esposizioni) analysis with Data Quality validation results. The module is compliant with CRR Regulation (EU) No. 575/2013 and EBA ITS standards.

### Key Features

- **Dual Event Coordination**: Waits for both risk calculation and quality validation events before generating reports
- **Comprehensive Reports**: Single report combining Large Exposures analysis and Data Quality metrics
- **Dynamic Recommendations**: Contextual quality recommendations based on actual error patterns
- **Parallel Generation**: HTML and XBRL reports generated concurrently for optimal performance
- **Regulatory Compliance**: XBRL output conforming to EBA COREP Framework (LE1 and LE2 templates)
- **Resilient Architecture**: Circuit breaker pattern, automatic retry, and graceful degradation

## Architecture

### Module Structure

The module follows Domain-Driven Design principles with a 4-layer architecture:

```
regtech-report-generation/
├── domain/              # Core business logic and entities
├── application/         # Use cases and orchestration
├── infrastructure/      # Technical implementations (DB, S3, templates)
└── presentation/        # Health checks and metrics
```

### Event Flow

```
┌─────────────────┐    ┌─────────────────┐
│ Risk Calculation│    │ Data Quality    │
│ Module          │    │ Module          │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          ▼                      ▼
┌─────────────────┐    ┌─────────────────┐
│BatchCalculation │    │BatchQuality     │
│CompletedEvent   │    │CompletedEvent   │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          └──────────┬───────────┘
                     ▼
          ┌─────────────────┐
          │ Report Event    │
          │ Listener        │
          └─────────┬───────┘
                    │
                    ▼
          ┌─────────────────┐
          │ Batch Event     │
          │ Tracker         │
          │ (Wait for BOTH) │
          └─────────┬───────┘
                    │
                    ▼ (Both Present)
          ┌─────────────────┐
          │ Comprehensive   │
          │ Report          │
          │ Orchestrator    │
          └─────────┬───────┘
                    │
    ┌───────────────┴───────────────┐
    │                               │
    ▼                               ▼
┌─────────────────┐    ┌─────────────────┐
│ HTML Generation │    │ XBRL Generation │
│ (Thymeleaf)     │    │ (EBA COREP)     │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          └──────────┬───────────┘
                     ▼
          ┌─────────────────┐
          │ Upload to S3    │
          │ Save Metadata   │
          │ Publish Event   │
          └─────────────────┘
```

## Event Coordination

### Dual Event Pattern

The module implements a sophisticated event coordination mechanism:

1. **Event Arrival**: Listens for `BatchCalculationCompletedEvent` and `BatchQualityCompletedEvent`
2. **Event Tracking**: `BatchEventTracker` maintains in-memory state of pending events per batch
3. **Coordination**: `ReportCoordinator` triggers generation only when BOTH events are present
4. **Cleanup**: Automatic cleanup of events older than 24 hours

### Event Validation

Each incoming event is validated for:
- Event freshness (not older than 24 hours)
- Idempotency (no duplicate processing)
- Required fields (batch ID, S3 URIs)

## Report Generation

### HTML Report

Professional HTML report with:
- **Header**: Bank information, reporting date, quality badge
- **Large Exposures Section**: 
  - Executive summary cards (Tier 1 Capital, exposure count, limit breaches)
  - Interactive charts (sector distribution, top exposures)
  - Detailed exposure table with compliance status
- **Data Quality Section**:
  - Overall quality score with grade (A-F)
  - Dimension scores (Completeness, Accuracy, Consistency, Timeliness, Uniqueness, Validity)
  - Error distribution analysis
  - Dynamic contextual recommendations
- **BCBS 239 Compliance**: Compliance status for Principles 3, 4, 5, 6

**Technology**: Thymeleaf template engine, Tailwind CSS, Chart.js

### XBRL Report

Regulatory XBRL-XML file conforming to:
- EBA COREP Framework
- Templates LE1 (counterparty details) and LE2 (exposure amounts)
- Full namespace and schema references
- Context dimensions (CP, CT, SC)
- XSD validation against EBA taxonomy

### Quality Recommendations

Dynamic recommendations generated based on actual data issues:

- **Critical Situation** (score < 60%): Immediate action items
- **Dimension-Specific**: Targeted guidance for dimensions below thresholds
- **Error Pattern Analysis**: Recommendations for top 3 most common errors
- **Positive Aspects**: Highlights excellent dimensions (≥95%)
- **Action Plan**: Short-term, medium-term, and long-term actions

## Configuration

### Application Properties

Key configuration in `application-report-generation.yml`:

```yaml
report-generation:
  s3:
    bucket-name: risk-analysis-production
    html-path: reports/html/
    xbrl-path: reports/xbrl/
  
  file-paths:
    calculation-pattern: calculated/calc_batch_{batchId}.json
    quality-pattern: quality/quality_batch_{batchId}.json
  
  async:
    core-pool-size: 4
    max-pool-size: 8
    queue-capacity: 100
  
  circuit-breaker:
    failure-threshold: 10
    wait-duration: 5m
    permitted-calls-half-open: 1
```

### Environment Variables

Required environment variables:

- `AWS_ACCESS_KEY_ID`: AWS credentials for S3 access
- `AWS_SECRET_ACCESS_KEY`: AWS secret key
- `AWS_REGION`: AWS region (default: eu-central-1)
- `SPRING_PROFILES_ACTIVE`: Active profile (dev, prod)

### Development vs Production

**Development Mode** (`spring.profiles.active=dev`):
- Reads files from local filesystem (`/data/calculated/`, `/data/quality/`)
- Saves reports to local filesystem
- Reduced logging

**Production Mode** (`spring.profiles.active=prod`):
- Reads files from S3
- Uploads reports to S3 with encryption
- Full logging and metrics

## Monitoring and Alerting

### Metrics

The module emits comprehensive metrics:

**Timers** (with percentiles p50, p75, p90, p95, p99):
- `report.generation.comprehensive.duration` - End-to-end generation time
- `report.generation.html.duration` - HTML generation time
- `report.generation.xbrl.duration` - XBRL generation time
- `report.data.fetch.duration` - Data retrieval time
- `report.s3.upload.duration` - S3 upload time

**Counters**:
- `report.generation.comprehensive.success` - Successful generations
- `report.generation.comprehensive.failure` - Failed generations (with failure_reason tag)
- `report.generation.partial` - Partial successes
- `report.generation.duplicate` - Duplicate event detections
- `report.s3.circuit.breaker.open` - Circuit breaker openings
- `report.s3.circuit.breaker.closed` - Circuit breaker closings

**Gauges**:
- `report.database.pool.active` - Active DB connections
- `report.database.pool.idle` - Idle DB connections
- `report.async.executor.queue.size` - Async task queue size
- `report.async.executor.active.threads` - Active async threads
- `report.deferred.uploads.count` - Pending deferred uploads
- `report.circuit.breaker.state` - Circuit breaker state (0=closed, 1=open, 2=half-open)

### Health Checks

Health check endpoints:

- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`

Health indicators check:
- Database connectivity
- S3 accessibility
- Event tracker state (pending events)
- Async executor queue capacity

**Status Levels**:
- `UP`: All components healthy
- `WARN`: Degraded performance (queue 50-80% full, slow S3 response)
- `DOWN`: Critical failures (DB timeout, S3 inaccessible, queue full)

### Alerting Rules

**CRITICAL Alerts**:
- Failure rate > 10% over 5 minutes
- S3 consecutive failures > 10
- Database connection pool exhausted
- Permission denied errors (403)

**HIGH Alerts**:
- Event timeout rate > 20%
- Deferred uploads accumulating (>50)
- XBRL validation failure spike (>30% of reports)

**MEDIUM Alerts**:
- P95 duration > 10 seconds
- Partial report rate > 10%
- Outbox events accumulating (>100 pending)

## Error Handling

### Error Categories

**Transient Errors** (Automatic Retry):
- Network timeouts
- Temporary S3 unavailability
- Database connection timeouts

**Permanent Errors** (No Retry):
- S3 permission denied (403)
- S3 bucket not found (404)
- Invalid credentials

**Data Quality Errors**:
- Malformed JSON
- Missing required fields
- Invalid checksums

**Partial Failures**:
- HTML generated but XBRL validation fails
- One file uploaded but other fails

### Recovery Mechanisms

1. **EventRetryProcessor**: Automatic retry with exponential backoff (1s, 2s, 4s, 8s, 16s)
2. **Circuit Breaker**: Prevents cascading failures during S3 outages
3. **Local Fallback**: Saves files locally when S3 unavailable
4. **Partial Success**: Marks reports as PARTIAL when one format succeeds
5. **Compensating Transactions**: Fallback metadata table for DB failures

## Performance

### Target Metrics

- **End-to-End Duration**: 5-7 seconds
- **Data Fetching**: ~800ms (both files)
- **HTML Generation**: ~2.0 seconds (including recommendations)
- **XBRL Generation**: ~800ms (including validation)
- **S3 Upload**: ~800ms (both files)

### Optimization Strategies

1. **Parallel Generation**: HTML and XBRL generated concurrently
2. **Async Processing**: Event processing in dedicated thread pool
3. **Connection Pooling**: Reused database and S3 connections
4. **Template Caching**: Thymeleaf templates cached in production
5. **Streaming**: Large files processed with streaming APIs

## Testing

### Unit Tests

- **Coverage Target**: ≥85% line coverage, ≥75% branch coverage
- **Framework**: JUnit 5 with Mockito
- **Focus**: Business logic, domain entities, value objects

### Property-Based Tests

- **Framework**: jqwik
- **Iterations**: Minimum 100 per property
- **Key Properties**:
  - Dual event coordination
  - Quality recommendations contextuality
  - Idempotency guarantees
  - Quality score thresholds

### Integration Tests

- **Framework**: Spring Boot Test with Testcontainers
- **Containers**: PostgreSQL, LocalStack (S3)
- **Scenarios**: Happy path, reverse event order, failures, partial generation

## Dependencies

### Core Dependencies

- Spring Boot 3.x
- Spring Data JPA
- Thymeleaf
- AWS SDK for Java 2.x
- Resilience4j
- Micrometer (metrics)
- jqwik (property-based testing)

### Shared Modules

- `regtech-core-domain`: Base aggregate root, domain events
- `regtech-core-application`: Event retry processor, unit of work
- `regtech-core-infrastructure`: S3 service, file storage abstractions

## Development

### Building the Module

```bash
# Build all modules
mvn clean install

# Build report generation module only
cd regtech-report-generation
mvn clean install

# Skip tests
mvn clean install -DskipTests
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ComprehensiveReportOrchestratorTest

# Run integration tests only
mvn verify -P integration-tests
```

### Local Development

1. Start required services:
```bash
# PostgreSQL
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:15

# LocalStack (S3 emulation)
docker run -d -p 4566:4566 localstack/localstack
```

2. Set environment variables:
```bash
export SPRING_PROFILES_ACTIVE=dev
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_REGION=eu-central-1
```

3. Run the application:
```bash
mvn spring-boot:run
```

## Troubleshooting

### Common Issues

**Issue**: Reports not generating
- Check both events are being published by upstream modules
- Verify event tracker state via health endpoint
- Check logs for event validation failures

**Issue**: S3 upload failures
- Verify AWS credentials are correct
- Check S3 bucket exists and has correct permissions
- Review circuit breaker state in metrics

**Issue**: XBRL validation failures
- Check EBA schema file is present in classpath
- Verify LEI codes are valid (20 characters)
- Review validation errors in logs

**Issue**: Slow report generation
- Check database connection pool size
- Verify S3 network latency
- Review async executor queue size

### Debug Logging

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    com.bcbs239.regtech.reportgeneration: DEBUG
    com.bcbs239.regtech.core: DEBUG
```

## Support

For issues, questions, or contributions:
- Internal Wiki: [Report Generation Module Documentation]
- Slack Channel: #regtech-report-generation
- Email: regtech-support@example.com

## License

Internal proprietary software. All rights reserved.
