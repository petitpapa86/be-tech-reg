# Ingestion Configuration Integration

## Problem Identified

The ingestion module had disconnected configuration components:

1. **FileProcessingPerformanceOptimizer** - Used hardcoded `@Value` annotations
2. **InfrastructureFileParsingService** - Used `@Value` for parser settings
3. **IngestionProperties** - Existed but wasn't connected to the services

This created configuration inconsistency and made it difficult to:
- Override settings via application.yml
- Test with different configurations
- Understand the complete configuration surface

## Solution Implemented

### 1. Enhanced IngestionProperties

Added comprehensive configuration structure:

```java
@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(
    FileProperties file,
    ProcessingProperties processing,
    PerformanceProperties performance,  // NEW
    ParserProperties parser              // NEW
)
```

**New Configuration Sections:**

- `PerformanceProperties`: Controls file processing optimization
  - `maxConcurrentFiles`: Maximum parallel file processing (default: 4)
  - `chunkSize`: Records per processing chunk (default: 10000)

- `ParserProperties`: Controls file parsing behavior
  - `defaultMaxRecords`: Maximum records to parse (default: 10000)

### 2. Connected FileProcessingPerformanceOptimizer

**Before:**
```java
public FileProcessingPerformanceOptimizer(
    @Value("${ingestion.performance.max-concurrent-files:4}") int maxConcurrentFiles,
    @Value("${ingestion.performance.chunk-size:10000}") int chunkSize)
```

**After:**
```java
public FileProcessingPerformanceOptimizer(IngestionProperties ingestionProperties) {
    this.maxConcurrentFiles = ingestionProperties.performance().maxConcurrentFiles();
    this.chunkSize = ingestionProperties.performance().chunkSize();
    // ...
}
```

### 3. Connected InfrastructureFileParsingService

**Before:**
```java
@Value("${ingestion.parser.default-max-records:10000}")
private int defaultMaxRecords;

public InfrastructureFileParsingService(
    FileToLoanExposureParser parser,
    FileProcessingPerformanceOptimizer optimizer)
```

**After:**
```java
private final int defaultMaxRecords;

public InfrastructureFileParsingService(
    FileToLoanExposureParser parser,
    FileProcessingPerformanceOptimizer optimizer,
    IngestionProperties ingestionProperties) {
    this.defaultMaxRecords = ingestionProperties.parser().defaultMaxRecords();
}
```

## Configuration Flow

```
application.yml / application-ingestion.yml
    ↓
IngestionProperties (loaded by Spring)
    ↓
├─→ FileProcessingPerformanceOptimizer
│   - Uses performance.maxConcurrentFiles
│   - Uses performance.chunkSize
│
├─→ InfrastructureFileParsingService
│   - Uses parser.defaultMaxRecords
│   - Uses optimizer (which uses performance settings)
│
└─→ ProcessBatchCommandHandler
    - Uses fileParsingService (InfrastructureFileParsingService)
```

## Benefits

1. **Centralized Configuration**: All ingestion settings in one place
2. **Type Safety**: Record-based configuration with compile-time checking
3. **Testability**: Easy to create test configurations
4. **Discoverability**: Clear configuration structure via IDE autocomplete
5. **Documentation**: Self-documenting via record structure
6. **Consistency**: All services use the same configuration source

## Configuration Example

```yaml
ingestion:
  file:
    max-size: 524288000  # 500MB
    supported-types:
      - application/json
      - application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
  
  processing:
    async-enabled: true
    thread-pool-size: 10
    queue-capacity: 100
  
  performance:
    max-concurrent-files: 4      # Parallel file processing limit
    chunk-size: 10000           # Records per chunk
  
  parser:
    default-max-records: 10000  # Max records per file
```

## Performance Tuning Guidelines

### High-Throughput Environment
```yaml
performance:
  max-concurrent-files: 8
  chunk-size: 50000
parser:
  default-max-records: 50000
```

### Memory-Constrained Environment
```yaml
performance:
  max-concurrent-files: 2
  chunk-size: 5000
parser:
  default-max-records: 5000
```

### Development/Testing
```yaml
performance:
  max-concurrent-files: 2
  chunk-size: 1000
parser:
  default-max-records: 1000
```

## Integration Points

### Services Using Configuration

1. **FileProcessingPerformanceOptimizer**
   - Thread pool sizing
   - Chunk size for splitting logic
   - Concurrent file limits

2. **InfrastructureFileParsingService**
   - Max records to parse
   - Uses optimizer's capacity decisions

3. **ProcessBatchCommandHandler**
   - Indirectly via fileParsingService

### Configuration Loading

- `IngestionAutoConfiguration`: Enables properties via `@EnableConfigurationProperties`
- Spring Boot auto-configuration loads properties from:
  - `application.yml`
  - `application-ingestion.yml` (profile-specific)
  - Environment variables
  - System properties

## Testing

### Create Test Configuration

```java
@TestConfiguration
public class TestIngestionConfiguration {
    
    @Bean
    public IngestionProperties testIngestionProperties() {
        return new IngestionProperties(
            new FileProperties(1048576L, List.of("application/json")),
            new ProcessingProperties(false, 2, 10),
            new PerformanceProperties(2, 100),
            new ParserProperties(100)
        );
    }
}
```

## Migration Notes

- All existing `@Value` annotations have been removed
- Default values are now in `IngestionProperties` record
- Configuration keys remain the same (backward compatible)
- Services now require `IngestionProperties` dependency

## Next Steps

1. Add validation annotations to properties if needed
2. Create configuration profiles for different environments
3. Document property ranges and constraints
4. Add metrics/monitoring for configuration effectiveness
