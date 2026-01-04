# Observability Architecture - Metrics Context

## Overview

This directory contains the infrastructure-layer observability components for the Metrics bounded context. The design follows Clean Architecture principles by keeping the application layer free of infrastructure concerns like logging and metrics collection.

## Design Philosophy

### Separation of Concerns

- **Application Layer**: Publishes semantic, business-meaningful signals (events) via `ApplicationSignalPublisher`
- **Infrastructure Layer**: Observes these signals and translates them to logs, metrics, and traces

The application layer has ZERO knowledge of:
- SLF4J, Logback, or any logging framework
- Micrometer or metrics collection
- MDC (Mapped Diagnostic Context)
- Infrastructure observability concerns

## Components

### 1. ApplicationSignal System

#### Purpose
A semantic event system that describes business-relevant actions without coupling to infrastructure.

#### Components
- `ApplicationSignal` (application layer): Interface for semantic signals
- `ApplicationSignalPublisher` (application layer): Port for publishing signals
- `SpringEventApplicationSignalPublisher` (infrastructure): Adapter that publishes to Spring Events
- `ApplicationSignalEmittedEvent` (infrastructure): Spring event wrapper with source location tracking
- `ApplicationSignalLoggingListener` (infrastructure): Listens to signals and logs them

#### Example Signals
- `DashboardQueriedSignal`: Dashboard was queried for a bank
- `DashboardMetricsUpdatedSignal`: Dashboard metrics were recalculated
- `ComplianceReportUpsertedSignal`: A compliance report was upserted
- `DashboardMetricsUpdateIgnoredSignal`: Dashboard update was skipped (with reason)

### 2. Use Case Logging Aspects

#### UseCaseLoggingAspect
Automatically logs entry and exit of all use case methods.

**Features:**
- Logs method entry with arguments (non-sensitive)
- Logs successful exit with execution time
- Logs exceptions with full context
- Uses MDC for contextual information (class, method)
- Debug mode for detailed argument inspection

**Pointcut:**
```java
execution(public * com.bcbs239.regtech.metrics.application..*UseCase.*(..))
```

**Example Logs:**
```
INFO  >>> Entering use case: DashboardUseCase.execute
INFO  <<< Exiting use case: DashboardUseCase.execute successfully in 142 ms
ERROR <<< Use case: UpdateDashboardMetricsOnDataQualityCompletedUseCase.process failed after 89 ms with exception: NullPointerException
```

#### UseCasePerformanceAspect
Monitors use case performance and warns about slow operations.

**Features:**
- Tracks execution time for all use cases
- Warns when execution exceeds threshold (default: 5000ms)
- Can be enabled/disabled via configuration

**Configuration:**
```yaml
metrics:
  observability:
    performance-monitoring:
      enabled: true  # default
```

#### UseCaseMetricsAspect
Collects Micrometer metrics for use case executions.

**Metrics Collected:**
- `metrics.usecase.execution.duration`: Timer with tags (usecase, status, error_type)
- `metrics.usecase.execution.total`: Counter with tags (usecase, status, error_type)

**Configuration:**
```yaml
metrics:
  observability:
    metrics-collection:
      enabled: true  # default
```

### 3. Signal Logging Listener

#### ApplicationSignalLoggingListener
Converts application signals to structured logs with rich context.

**Features:**
- Pattern matches on signal types (using Java 21 pattern matching)
- Adds MDC context (bank-id, batch-id, source location)
- Respects signal severity levels (DEBUG, INFO, WARN, ERROR)
- Provides fallback for unknown signal types

**MDC Fields Added:**
- `bank-id`: Bank identifier from signal
- `batch-id`: Batch identifier from signal
- `source-class`: Class that published the signal
- `source-method`: Method that published the signal
- `source-file`: Source file name
- `source-line`: Line number

## Aspect Execution Order

Aspects are executed in the following order (controlled by `@Order`):

1. **UseCasePerformanceAspect** (@Order(1)) - Performance monitoring
2. **UseCaseMetricsAspect** (@Order(2)) - Metrics collection
3. **UseCaseLoggingAspect** (@Order(3), default) - Logging

This order ensures:
- Performance measurements include all processing
- Metrics are collected before detailed logging
- Logs show the complete picture including metrics

## Usage Examples

### Publishing a Signal (Application Layer)

```java
@Component
public class DashboardUseCase {
    private final ApplicationSignalPublisher signalPublisher;
    
    public DashboardResult execute(BankId bankId) {
        // Publish semantic signal - no logging infrastructure!
        signalPublisher.publish(new DashboardQueriedSignal(
            bankId.getValue(), 
            startDate, 
            endDate
        ));
        
        // Business logic...
    }
}
```

### Infrastructure Observes and Logs

The infrastructure automatically:
1. Logs method entry: `>>> Entering use case: DashboardUseCase.execute`
2. Publishes Spring event from signal
3. Logs signal details: `Dashboard requested bankId=BANK001 range=2026-01-01..2026-01-04`
4. Logs method exit: `<<< Exiting use case: DashboardUseCase.execute successfully in 142 ms`
5. Records metrics: `metrics.usecase.execution.total{usecase=DashboardUseCase.execute,status=success} ++`

## Benefits

### For Application Layer
- ✅ No logging dependencies
- ✅ No infrastructure coupling
- ✅ Clean, testable code
- ✅ Business-focused signals
- ✅ Easy unit testing (mock ApplicationSignalPublisher)

### For Infrastructure Layer
- ✅ Centralized observability logic
- ✅ Consistent logging format
- ✅ Rich contextual information (MDC)
- ✅ Automatic metrics collection
- ✅ Performance monitoring
- ✅ Easy to modify logging without touching application code

### For Operations/SRE
- ✅ Structured logs with context
- ✅ Prometheus metrics out of the box
- ✅ Performance monitoring
- ✅ Easy debugging with source location tracking
- ✅ Consistent observability patterns

## Adding New Signals

1. Create signal record in application layer:
```java
package com.bcbs239.regtech.metrics.application.signal;

public record NewBusinessSignal(
    String someData,
    String contextInfo
) implements ApplicationSignal {
    @Override
    public String type() {
        return "metrics.new.business.action";
    }
    
    @Override
    public SignalLevel level() {
        return SignalLevel.INFO; // or WARN, ERROR, DEBUG
    }
}
```

2. Publish from use case:
```java
signalPublisher.publish(new NewBusinessSignal(data, context));
```

3. Add pattern matching in `ApplicationSignalLoggingListener`:
```java
case NewBusinessSignal(String data, String context) -> {
    withContext(null, null, event, () ->
        info(signal, "New business action data={} context={}", data, context)
    );
    return;
}
```

## Testing

### Testing Use Cases (Application Layer)
```java
@Test
void testDashboardUseCase() {
    ApplicationSignalPublisher mockPublisher = mock(ApplicationSignalPublisher.class);
    DashboardUseCase useCase = new DashboardUseCase(..., mockPublisher);
    
    useCase.execute(BankId.of("BANK001"));
    
    verify(mockPublisher).publish(any(DashboardQueriedSignal.class));
}
```

### Testing Signal Logging (Infrastructure)
```java
@Test
void testSignalLogging() {
    // Use Spring's ApplicationEventPublisher test support
    // or Mockito to verify log statements
}
```

## Configuration

All aspects support configuration:

```yaml
metrics:
  observability:
    performance-monitoring:
      enabled: true
      slow-threshold-ms: 5000
    metrics-collection:
      enabled: true
```

## Future Enhancements

Potential additions:
- Distributed tracing integration (OpenTelemetry)
- Automatic correlation ID propagation
- Business metrics from signals (counter per signal type)
- Alert generation from specific signals
- Signal replay for debugging
- Signal-based audit trail

## Related Documentation

- [Clean Architecture Guide](../../CLEAN_ARCH_GUIDE.md)
- [Observability Configuration](../../OBSERVABILITY_CONFIGURATION.md)
- [Grafana Dashboards](../../GRAFANA_DASHBOARDS_IMPLEMENTATION.md)
