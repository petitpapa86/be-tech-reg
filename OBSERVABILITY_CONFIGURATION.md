# Observability Configuration - Spring Boot 4 Migration

## Overview

This document describes the observability configuration implemented for the Spring Boot 4 migration. The configuration includes Micrometer 2 for metrics collection, OpenTelemetry for distributed tracing, and enhanced health check endpoints for SSL certificate monitoring.

**Requirements Addressed:**
- 10.1: Micrometer 2 metrics collection
- 10.2: OpenTelemetry integration for distributed tracing
- 10.3: Trace context propagation across all modules
- 10.4: SSL certificate expiration reporting in expiringChains entry
- 10.5: Report expiring certificates as VALID instead of WILL_EXPIRE_SOON

## Components

### 1. Application Configuration (`application.yml`)

The main configuration file includes comprehensive observability settings:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,httptrace
  
  endpoint:
    health:
      show-details: when-authorized
      show-components: always
  
  health:
    diskspace:
      enabled: true
    ssl:
      enabled: true
  
  metrics:
    enable:
      jvm: true
      process: true
      system: true
      http: true
      logback: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
  
  tracing:
    enabled: true
    sampling:
      probability: 1.0
    propagation:
      type: w3c
  
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
      protocol: http/protobuf
    metrics:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/metrics}
      protocol: http/protobuf
```

### 2. Dependencies (`regtech-app/pom.xml`)

Added observability dependencies:

```xml
<!-- Spring Boot Actuator for metrics and health checks -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus registry for metrics export -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Micrometer Tracing Bridge for OpenTelemetry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- OpenTelemetry Exporter for OTLP -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

### 3. SSL Certificate Health Indicator

**File:** `regtech-app/src/main/java/com/bcbs239/regtech/app/monitoring/SslCertificateHealthIndicator.java`

Implements Spring Boot Actuator's `HealthIndicator` interface to monitor SSL certificates:

- Reports expiring certificates in the `expiringChains` entry (Requirement 10.4)
- Reports expiring certificates with status `VALID` instead of `WILL_EXPIRE_SOON` (Requirement 10.5)
- Configurable warning threshold (default: 30 days)
- Provides detailed certificate information including subject, issuer, and expiration dates

**Example Health Response:**
```json
{
  "status": "UP",
  "details": {
    "expiringChains": [
      {
        "subject": "CN=example.com",
        "issuer": "CN=Example CA",
        "notBefore": "2024-01-01T00:00:00Z",
        "notAfter": "2025-01-15T00:00:00Z",
        "daysUntilExpiration": 25,
        "expiringSoon": true,
        "expired": false,
        "status": "VALID"
      }
    ],
    "totalCertificates": 5,
    "expiringCount": 1,
    "validCount": 4,
    "warningThresholdDays": 30
  }
}
```

### 4. Observability Configuration

**File:** `regtech-app/src/main/java/com/bcbs239/regtech/app/monitoring/ObservabilityConfiguration.java`

Configures Micrometer 2 and OpenTelemetry integration:

- **ObservedAspect**: Enables `@Observed` annotation support for automatic method observation
- **ObservationRegistryCustomizer**: Adds application-specific tags to all observations
- **TraceContextLogger**: Helper for including trace IDs and span IDs in log statements

**Key Features:**
- Automatic observation of annotated methods
- Common tags propagated with all metrics and traces
- Trace context available for logging correlation

### 5. Application Metrics Collector

**File:** `regtech-app/src/main/java/com/bcbs239/regtech/app/monitoring/ApplicationMetricsCollector.java`

Provides custom metrics using Micrometer 2:

**Built-in Metrics:**
- `regtech.application.starts`: Counter for application start events
- `regtech.application.startup.time`: Timer for application startup duration
- `regtech.module.initializations`: Counter for module initialization events
- `regtech.module.initialization.time`: Timer for module initialization duration

**API Methods:**
- `recordApplicationStart()`: Records an application start event
- `recordApplicationStartupTime(long)`: Records startup time in milliseconds
- `recordModuleInitialization(String)`: Records module initialization
- `recordModuleInitializationTime(String, long)`: Records module initialization time
- `createCounter(String, String, String...)`: Creates custom counters
- `createTimer(String, String, String...)`: Creates custom timers

### 6. Application Startup Integration

**File:** `regtech-app/src/main/java/com/bcbs239/regtech/app/RegtechApplication.java`

The main application class now records startup metrics:

```java
@Bean
public ApplicationListener<ApplicationReadyEvent> applicationReadyListener(
        ApplicationMetricsCollector metricsCollector) {
    return event -> {
        long startupTime = System.currentTimeMillis() - startTime;
        metricsCollector.recordApplicationStart();
        metricsCollector.recordApplicationStartupTime(startupTime);
        logger.info("Application started successfully in {}ms", startupTime);
    };
}
```

## Endpoints

### Health Check Endpoint

**URL:** `/actuator/health`

Returns overall application health including SSL certificate status.

### Metrics Endpoint

**URL:** `/actuator/metrics`

Lists all available metrics.

**URL:** `/actuator/metrics/{metric.name}`

Returns detailed information about a specific metric.

### Prometheus Endpoint

**URL:** `/actuator/prometheus`

Exports metrics in Prometheus format for scraping.

## Trace Context Propagation

Trace context is automatically propagated across all modules using the W3C Trace Context format. This ensures that:

1. All HTTP requests include trace headers (`traceparent`, `tracestate`)
2. Trace IDs and span IDs are available in log statements
3. Distributed traces can be correlated across module boundaries
4. OpenTelemetry collectors can aggregate traces from all modules

**Configuration:**
```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # Sample 100% of traces
    propagation:
      type: w3c  # W3C Trace Context format
```

## OpenTelemetry Integration

The application exports traces and metrics to an OpenTelemetry collector using the OTLP protocol:

**Default Endpoints:**
- Traces: `http://localhost:4318/v1/traces`
- Metrics: `http://localhost:4318/v1/metrics`

**Configuration via Environment Variables:**
```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://your-collector:4318
```

## Testing

### Unit Tests

**ObservabilityConfigurationTest:**
- Verifies metrics collector instantiation
- Tests custom counter creation
- Tests custom timer creation
- Validates application startup metrics
- Validates module initialization metrics

**SslCertificateHealthIndicatorTest:**
- Verifies health check returns valid Health object
- Tests required details inclusion
- Validates VALID status for expiring certificates
- Tests error handling

### Running Tests

```bash
mvn test -pl regtech-app -Dtest=ObservabilityConfigurationTest,SslCertificateHealthIndicatorTest
```

## Usage Examples

### Recording Custom Metrics

```java
@Component
public class MyService {
    
    private final ApplicationMetricsCollector metricsCollector;
    
    public MyService(ApplicationMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }
    
    public void processData() {
        // Create a custom counter
        Counter counter = metricsCollector.createCounter(
            "myservice.data.processed",
            "Number of data items processed",
            "type", "batch"
        );
        
        // Process data...
        counter.increment();
    }
    
    public void performOperation() {
        // Create a custom timer
        Timer timer = metricsCollector.createTimer(
            "myservice.operation.duration",
            "Time taken for operation",
            "operation", "process"
        );
        
        // Time the operation
        timer.record(() -> {
            // Perform operation...
        });
    }
}
```

### Using @Observed Annotation

```java
@Service
public class MyService {
    
    @Observed(name = "myservice.operation", contextualName = "process-data")
    public void processData(String data) {
        // This method will be automatically observed
        // Metrics and traces will be generated
    }
}
```

### Accessing Trace Context in Logs

```java
@Component
public class MyService {
    
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);
    private final TraceContextLogger traceLogger;
    
    public MyService(TraceContextLogger traceLogger) {
        this.traceLogger = traceLogger;
    }
    
    public void processData() {
        String traceId = traceLogger.getCurrentTraceId();
        String spanId = traceLogger.getCurrentSpanId();
        
        logger.info("Processing data [traceId={}, spanId={}]", traceId, spanId);
    }
}
```

## Migration Notes

### Changes from Spring Boot 3.x

1. **Micrometer 2.x**: Updated from Micrometer 1.x with improved observation API
2. **OpenTelemetry Native Support**: Spring Boot 4 includes native OpenTelemetry support
3. **Health Indicator Changes**: SSL certificate health reporting follows new Spring Boot 4 conventions
4. **Trace Context Format**: Uses W3C Trace Context standard by default

### Breaking Changes

- `management.metrics.export.prometheus.enabled` is now always enabled when Prometheus dependency is present
- Health indicator responses may have different structure
- Some Micrometer 1.x APIs are deprecated in favor of Observation API

## Troubleshooting

### Metrics Not Appearing

1. Verify actuator endpoints are exposed:
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info,metrics,prometheus
   ```

2. Check that Micrometer dependencies are present in POM

3. Verify MeterRegistry bean is available in application context

### Traces Not Being Exported

1. Verify OpenTelemetry collector is running and accessible
2. Check OTLP endpoint configuration
3. Verify tracing is enabled:
   ```yaml
   management:
     tracing:
       enabled: true
   ```

4. Check sampling probability (set to 1.0 for 100% sampling during testing)

### SSL Health Check Failing

1. Verify SSL context is properly configured
2. Check certificate trust store configuration
3. Review application logs for SSL-related errors

## References

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/4.0.0/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/instrumentation/java/)
- [W3C Trace Context Specification](https://www.w3.org/TR/trace-context/)
