# Risk Calculation Application Layer Refactoring - Design

## Overview

This design document outlines the systematic refactoring approach for the Risk Calculation module's application layer. The refactoring aligns the application layer with the updated domain model while maintaining clean architecture principles and ensuring all compilation errors are resolved.

## Architecture Context

### Current State
- Domain layer: Successfully implemented with proper value objects, domain services, and repository interfaces
- Application layer: Contains compilation errors due to API mismatches and outdated method calls
- Infrastructure layer: Properly implements domain interfaces

### Target State
- Zero compilation errors in application layer
- Consistent error handling using ErrorDetail API
- Proper integration with domain services and repositories
- Correct usage of Java record accessors
- Performance metrics tracking integrated into command handlers

## Component Refactoring Strategy

### 1. CalculateRiskMetricsCommandHandler

**Current Issues:**
- Missing PerformanceMetrics integration
- Some Result.failure() calls may need ErrorDetail updates
- Potential record accessor issues in result handling

**Design Solution:**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CalculateRiskMetricsCommandHandler {
    private final ExposureRepository exposureRepository;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final IFileStorageService fileStorageService;
    private final ExchangeRateProvider exchangeRateProvider;
    private final RiskCalculationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final PerformanceMetrics performanceMetrics;  // ADD THIS
    
    @Transactional
    public Result<Void> handle(CalculateRiskMetricsCommand command) {
        String batchId = command.getBatchId();
        
        // Record batch start
        performanceMetrics.recordBatchStart(batchId);
        
        try {
            // ... existing calculation logic ...
            
            // Record success with exposure count
            performanceMetrics.recordBatchSuccess(batchId, classifiedExposures.size());
            
            return Result.success();
            
        } catch (Exception e) {
            // Record failure with error message
            performanceMetrics.recordBatchFailure(batchId, e.getMessage());
            
            return Result.failure(ErrorDetail.of(
                "CALCULATION_FAILED", 
                ErrorType.SYSTEM_ERROR,
                "Risk calculation failed: " + e.getMessage(), 
                "calculation.failed"
            ));
        }
    }
}
```

**Key Changes:**
1. Add `PerformanceMetrics` as constructor dependency
2. Call `recordBatchStart()` at the beginning of handle method
3. Call `recordBatchSuccess()` on successful completion
4. Call `recordBatchFailure()` in catch block
5. Ensure all Result.failure() calls use ErrorDetail

### 2. CalculationResultsJsonSerializer

**Current Issues:**
- Using getter methods on RiskCalculationResult record (e.g., `getBatchId()`)
- Should use record accessor methods (e.g., `batchId()`)
- Domain object method calls may be incorrect

**Design Solution:**

```java
// BEFORE (incorrect)
String batchId = result.getBatchId();
BankInfo bankInfo = result.getBankInfo();
List<CalculatedExposure> exposures = result.getCalculatedExposures();
PortfolioAnalysis analysis = result.getPortfolioAnalysis();

// AFTER (correct)
String batchId = result.batchId();
BankInfo bankInfo = result.bankInfo();
int totalExposures = result.totalExposures();
PortfolioAnalysis analysis = result.analysis();
Instant ingestedAt = result.ingestedAt();
```

**Record Accessor Pattern:**
- Java records automatically generate accessor methods without the `get` prefix
- Method name matches field name exactly
- For field `batchId`, accessor is `batchId()`, not `getBatchId()`

**Domain Object Access:**
For domain objects within the result, check if they are records or classes:
- Records: Use `field()` accessor
- Classes: Use `getField()` method

Example for Share domain object:
```java
// If Share is a record
double amount = share.amount();
double percentage = share.percentage();

// If Share is a class
double amount = share.getAmount();
double percentage = share.getPercentage();
```

### 3. BatchIngestedEventListener

**Current Issues:**
- Repository method names don't match interface
- Using `existsByBatchId()` instead of `existsById()`

**Design Solution:**

```java
// BEFORE (incorrect)
boolean exists = batchSummaryRepository.existsByBatchId(batchId);

// AFTER (correct)
BatchId batchIdObj = BatchId.of(batchId);
boolean exists = batchSummaryRepository.existsById(batchIdObj);
```

**Repository Interface Pattern:**
- Repository methods use domain value objects, not primitives
- `existsById(BatchId)` instead of `existsByBatchId(String)`
- Wrap primitive IDs in value objects before calling repository methods

### 4. RiskCalculationEventPublisher

**Current Issues:**
- Method signatures may not match expected parameter types
- Domain event creation may have incorrect field mapping

**Design Solution:**

The event publisher should have overloaded methods:

```java
@Component
public class RiskCalculationEventPublisher {
    
    // Simple method for backward compatibility
    public void publishBatchCalculationCompleted(
        String batchId, 
        String bankId, 
        int totalExposures
    ) {
        // Create domain event
        BatchCalculationCompletedEvent domainEvent = new BatchCalculationCompletedEvent(
            batchId,
            bankId,
            totalExposures,
            0.0,  // totalAmountEur - calculate if needed
            ""    // resultFileUri - provide if available
        );
        
        // Delegate to domain event handler
        publishBatchCalculationCompleted(domainEvent);
    }
    
    // Domain event handler
    @TransactionalEventListener
    public Result<Void> publishBatchCalculationCompleted(
        BatchCalculationCompletedEvent domainEvent
    ) {
        // Transform to integration event
        BatchCalculationCompletedIntegrationEvent integrationEvent = 
            new BatchCalculationCompletedIntegrationEvent(
                domainEvent.getBatchId(),
                domainEvent.getBankId(),
                domainEvent.getTotalExposures(),
                Instant.now(),
                UUID.randomUUID().toString(),
                Map.of()
            );
        
        // Publish integration event
        applicationEventPublisher.publishEvent(integrationEvent);
        
        return Result.success();
    }
}
```

### 5. PerformanceMonitoringScheduler

**Current Issues:**
- May have Result.failure() calls needing ErrorDetail updates

**Design Solution:**

```java
// Ensure all error handling uses ErrorDetail
return Result.failure(ErrorDetail.of(
    "METRICS_COLLECTION_FAILED",
    ErrorType.SYSTEM_ERROR,
    "Failed to collect performance metrics: " + e.getMessage(),
    "monitoring.metrics.collection.failed"
));
```

## Error Handling Strategy

### ErrorDetail Usage Pattern

All Result.failure() calls must follow this pattern:

```java
Result.failure(ErrorDetail.of(
    "ERROR_CODE",           // Unique error identifier (UPPERCASE_SNAKE_CASE)
    ErrorType.CATEGORY,     // Error category enum
    "Human readable message", // User-friendly description
    "error.context.key"     // Localization key (dot.separated.lowercase)
))
```

### Error Code Conventions

| Error Type | Usage | Example Code |
|------------|-------|--------------|
| VALIDATION_ERROR | Input validation failures | INVALID_BATCH_ID |
| BUSINESS_RULE_ERROR | Domain rule violations | NO_EXPOSURES |
| SYSTEM_ERROR | Technical/infrastructure failures | CALCULATION_FAILED |
| NOT_FOUND_ERROR | Resource not found | BATCH_NOT_FOUND |

### Error Context Key Pattern

Follow pattern: `module.operation.error.type`

Examples:
- `calculation.file.download.failed`
- `calculation.validation.batch.id`
- `calculation.processing.failed`
- `calculation.no.exposures`

## Record Accessor Compliance

### Identification Strategy

1. Identify all record classes in the codebase
2. Search for getter-style method calls on records
3. Replace with record accessor methods

### Common Record Classes

| Record Class | Incorrect | Correct |
|--------------|-----------|---------|
| RiskCalculationResult | `result.getBatchId()` | `result.batchId()` |
| RiskCalculationResult | `result.getBankInfo()` | `result.bankInfo()` |
| RiskCalculationResult | `result.getAnalysis()` | `result.analysis()` |
| Share | `share.getAmount()` | `share.amount()` |
| Share | `share.getPercentage()` | `share.percentage()` |
| Breakdown | `breakdown.getShares()` | `breakdown.shares()` |

### Verification Approach

1. Check class definition: `public record ClassName(...)`
2. If record, use field name as accessor: `field()`
3. If class, use getter method: `getField()`

## Domain Service Integration

### IFileStorageService

**Correct Method Usage:**
```java
// Correct method name
Result<String> result = fileStorageService.retrieveFile(fileUri);

// Correct parameter type
FileStorageUri uri = FileStorageUri.of(command.getS3Uri());
Result<String> result = fileStorageService.retrieveFile(uri);
```

### Repository Interfaces

**Pattern:**
- Always use domain value objects as parameters
- Wrap primitive IDs before calling repository methods

```java
// Correct usage
BatchId batchId = BatchId.of(batchIdString);
boolean exists = batchSummaryRepository.existsById(batchId);

Optional<BatchSummary> summary = batchSummaryRepository.findById(batchId);
```

## Performance Metrics Integration

### Integration Points

1. **Command Handler Start:**
   - Call `performanceMetrics.recordBatchStart(batchId)` at method entry
   - Increments active calculations counter
   - Records start timestamp

2. **Command Handler Success:**
   - Call `performanceMetrics.recordBatchSuccess(batchId, exposureCount)` before returning success
   - Calculates processing duration
   - Updates success counters and throughput metrics

3. **Command Handler Failure:**
   - Call `performanceMetrics.recordBatchFailure(batchId, errorMessage)` in catch block
   - Updates failure counters
   - Logs failure details

### Metrics Tracked

- Total batches processed (success count)
- Total batches failed (failure count)
- Total exposures processed
- Average processing time per batch
- Error rate percentage
- Active calculations count
- Throughput per hour
- Average exposures per batch

## Testing Strategy

### Unit Test Updates

1. **Mock Updates:**
   - Add PerformanceMetrics mock to command handler tests
   - Verify performance metrics methods are called correctly
   - Update mock method signatures to match refactored code

2. **Record Accessor Tests:**
   - Verify tests use correct accessor methods
   - Update assertions to use record accessors

3. **Error Handling Tests:**
   - Verify ErrorDetail is used in all failure cases
   - Check error codes, types, and context keys

### Integration Test Updates

1. **End-to-End Flow:**
   - Verify performance metrics are recorded during actual execution
   - Check that metrics snapshot reflects actual processing

2. **Repository Integration:**
   - Verify value objects are correctly used with repositories
   - Test repository method calls with domain objects

## Migration Path

### Phase 1: Critical Fixes (Compilation)
1. Add PerformanceMetrics dependency to CalculateRiskMetricsCommandHandler
2. Fix record accessor usage in CalculationResultsJsonSerializer
3. Fix repository method calls in BatchIngestedEventListener
4. Update all Result.failure() calls to use ErrorDetail

### Phase 2: Integration
1. Integrate PerformanceMetrics calls in command handler
2. Verify event publisher method signatures
3. Update domain event creation and publishing

### Phase 3: Testing
1. Update unit tests with new mocks and assertions
2. Fix integration tests
3. Verify end-to-end functionality

## Quality Gates

### Compilation Gate
- Zero compilation errors in application layer
- All imports resolved
- Maven build succeeds: `mvn clean compile`

### Functionality Gate
- All unit tests pass
- Integration tests demonstrate proper module interaction
- Performance metrics are correctly recorded

### Architecture Gate
- Clean architecture boundaries maintained
- Domain logic remains in domain layer
- Application layer only orchestrates
- No business logic in application services

## Success Criteria

1. ✅ Zero compilation errors in application layer
2. ✅ All Result.failure() calls use ErrorDetail API
3. ✅ All record accessor methods used correctly
4. ✅ Repository methods called with correct value objects
5. ✅ PerformanceMetrics integrated in CalculateRiskMetricsCommandHandler
6. ✅ Event publishers have correct method signatures
7. ✅ All unit tests pass
8. ✅ Integration tests demonstrate proper functionality
9. ✅ Maven build completes successfully in under 2 minutes
10. ✅ Code follows established architectural patterns
