# DDD Aggregate Refactoring Plan for Risk Calculation Module

## Problem Statement

The current `CalculateRiskMetricsCommandHandler` implementation violates DDD principles:

1. **No Aggregate Root**: The handler directly manipulates repositories without proper aggregates
2. **No Domain Events**: Events are published directly by the application layer using `RiskCalculationEventPublisher`
3. **No Outbox Pattern**: Events are not persisted to the outbox table for reliable delivery
4. **Anemic Domain Model**: Business logic is in the application layer instead of the domain
5. **PortfolioAnalysis Not an Aggregate**: `PortfolioAnalysis` doesn't extend `Entity` and can't raise domain events

## Current Architecture Issues

### Current Flow
```
CalculateRiskMetricsCommandHandler
  ├─> batchRepository.createBatch() // Direct repository call
  ├─> batchRepository.markAsCompleted() // Direct repository call
  └─> eventPublisher.publishBatchCalculationCompleted() // Direct event publishing
```

### Problems
- Events are published synchronously, not through outbox
- No transactional consistency between batch state and events
- Business logic scattered across application layer
- Cannot replay events or implement event sourcing later

## Proposed Solution

### Target Architecture (Following IAM Pattern)

```
CalculateRiskMetricsCommandHandler
  ├─> Batch.create() // Factory method creates aggregate
  ├─> batch.startCalculation() // Domain method
  ├─> batch.completeCalculation(results) // Domain method raises event
  ├─> batchRepository.save(batch) // Save aggregate
  ├─> unitOfWork.registerEntity(batch) // Register for event collection
  └─> unitOfWork.saveChanges() // Persist events to outbox
```

## Implementation Steps

### Step 1: Update PortfolioAnalysis to Extend Entity

**Location**: `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/analysis/PortfolioAnalysis.java`

`PortfolioAnalysis` already exists but needs to extend `Entity` to raise domain events:

```java
package com.bcbs239.regtech.riskcalculation.domain.analysis;

import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.riskcalculation.domain.analysis.events.*;
// ... other imports

/**
 * Portfolio Analysis Aggregate Root
 * Represents the complete analysis of a portfolio
 */
public class PortfolioAnalysis extends Entity {  // ADD: extends Entity
    
    private final String batchId;
    private final EurAmount totalPortfolio;
    private final Breakdown geographicBreakdown;
    private final Breakdown sectorBreakdown;
    private final HHI geographicHHI;
    private final HHI sectorHHI;
    private final Instant analyzedAt;
    
    // ... existing code ...
    
    /**
     * Factory method to analyze a portfolio
     */
    public static PortfolioAnalysis analyze(String batchId, List<ClassifiedExposure> exposures) {
        // ... existing calculation logic ...
        
        PortfolioAnalysis analysis = new PortfolioAnalysis(
            batchId,
            totalPortfolio,
            geoBreakdown,
            sectorBreakdown,
            geoHHI,
            sectorHHI,
            Instant.now()
        );
        
        // ADD: Raise domain event
        analysis.addDomainEvent(new PortfolioAnalysisCompletedEvent(
            batchId,
            totalPortfolio.value(),
            geoHHI.value(),
            sectorHHI.value(),
            Instant.now()
        ));
        
        return analysis;
    }
    
    // ... rest of existing code ...
}
```

### Step 2: Create Batch Aggregate Root

**Location**: `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/calculation/Batch.java`

```java
package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.*;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.*;
import java.time.Instant;
import java.util.List;

/**
 * Batch Aggregate Root
 * Represents a batch of exposures being processed for risk calculation
 */
public class Batch extends Entity {
    
    private BatchId id;
    private BankInfo bankInfo;
    private BatchStatus status;
    private int totalExposures;
    private FileStorageUri sourceDataUri;
    private FileStorageUri calculationResultsUri;
    private ProcessingTimestamps timestamps;
    private String failureReason;
    
    // Private constructor - use factory methods
    private Batch() {}
    
    /**
     * Factory method to create a new batch
     */
    public static Batch create(
            String batchId,
            BankInfo bankInfo,
            int totalExposures,
            String sourceDataUri) {
        
        Batch batch = new Batch();
        batch.id = BatchId.of(batchId);
        batch.bankInfo = bankInfo;
        batch.totalExposures = totalExposures;
        batch.sourceDataUri = FileStorageUri.of(sourceDataUri);
        batch.status = BatchStatus.PROCESSING;
        batch.timestamps = ProcessingTimestamps.started(Instant.now());
        
        // Raise domain event
        batch.addDomainEvent(new BatchCalculationStartedEvent(
            batchId,
            bankInfo.abiCode(),
            totalExposures,
            Instant.now()
        ));
        
        return batch;
    }
    
    /**
     * Mark calculation as completed
     */
    public void completeCalculation(String resultsUri, int processedExposures) {
        if (this.status != BatchStatus.PROCESSING) {
            throw new IllegalStateException("Cannot complete batch that is not in PROCESSING state");
        }
        
        this.status = BatchStatus.COMPLETED;
        this.calculationResultsUri = FileStorageUri.of(resultsUri);
        this.timestamps = this.timestamps.withCompleted(Instant.now());
        
        // Raise domain event
        this.addDomainEvent(new BatchCalculationCompletedEvent(
            this.id.value(),
            this.bankInfo.abiCode(),
            processedExposures,
            resultsUri,
            this.timestamps.completedAt().orElseThrow()
        ));
    }
    
    /**
     * Mark calculation as failed
     */
    public void failCalculation(String reason) {
        if (this.status != BatchStatus.PROCESSING) {
            throw new IllegalStateException("Cannot fail batch that is not in PROCESSING state");
        }
        
        this.status = BatchStatus.FAILED;
        this.failureReason = reason;
        this.timestamps = this.timestamps.withFailed(Instant.now());
        
        // Raise domain event
        this.addDomainEvent(new BatchCalculationFailedEvent(
            this.id.value(),
            this.bankInfo.abiCode(),
            reason,
            this.timestamps.failedAt().orElseThrow()
        ));
    }
    
    /**
     * Reconstitute from persistence
     */
    public static Batch reconstitute(
            BatchId id,
            BankInfo bankInfo,
            BatchStatus status,
            int totalExposures,
            FileStorageUri sourceDataUri,
            FileStorageUri calculationResultsUri,
            ProcessingTimestamps timestamps,
            String failureReason) {
        
        Batch batch = new Batch();
        batch.id = id;
        batch.bankInfo = bankInfo;
        batch.status = status;
        batch.totalExposures = totalExposures;
        batch.sourceDataUri = sourceDataUri;
        batch.calculationResultsUri = calculationResultsUri;
        batch.timestamps = timestamps;
        batch.failureReason = failureReason;
        
        return batch;
    }
    
    // Getters
    public BatchId getId() { return id; }
    public BankInfo getBankInfo() { return bankInfo; }
    public BatchStatus getStatus() { return status; }
    public int getTotalExposures() { return totalExposures; }
    public FileStorageUri getSourceDataUri() { return sourceDataUri; }
    public FileStorageUri getCalculationResultsUri() { return calculationResultsUri; }
    public ProcessingTimestamps getTimestamps() { return timestamps; }
    public String getFailureReason() { return failureReason; }
}
```

### Step 3: Create Domain Events

**Location**: `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/calculation/events/` and `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/analysis/events/`

```java
// BatchCalculationStartedEvent.java
public record BatchCalculationStartedEvent(
    String batchId,
    String bankId,
    int totalExposures,
    Instant startedAt
) implements DomainEvent {
    @Override
    public String getEventType() {
        return "BatchCalculationStarted";
    }
}

// BatchCalculationCompletedEvent.java (already exists, move to domain)
public record BatchCalculationCompletedEvent(
    String batchId,
    String bankId,
    int processedExposures,
    String calculationResultsUri,
    Instant completedAt
) implements DomainEvent {
    @Override
    public String getEventType() {
        return "BatchCalculationCompleted";
    }
}

// BatchCalculationFailedEvent.java (already exists, move to domain)
public record BatchCalculationFailedEvent(
    String batchId,
    String bankId,
    String reason,
    Instant failedAt
) implements DomainEvent {
    @Override
    public String getEventType() {
        return "BatchCalculationFailed";
    }
}

// PortfolioAnalysisCompletedEvent.java (NEW)
public record PortfolioAnalysisCompletedEvent(
    String batchId,
    BigDecimal totalPortfolioEur,
    BigDecimal geographicHHI,
    BigDecimal sectorHHI,
    Instant completedAt
) implements DomainEvent {
    @Override
    public String getEventType() {
        return "PortfolioAnalysisCompleted";
    }
}
```

### Step 4: Create BatchStatus Value Object

**Location**: `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/calculation/BatchStatus.java`

```java
public enum BatchStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
```

### Step 5: Update BatchRepository

**Location**: `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/persistence/BatchRepository.java`

```java
public interface BatchRepository {
    
    /**
     * Save or update a batch aggregate
     */
    Result<Void> save(Batch batch);
    
    /**
     * Find a batch by ID
     */
    Maybe<Batch> findById(BatchId batchId);
    
    /**
     * Find batches by status
     */
    List<Batch> findByStatus(BatchStatus status);
}
```

### Step 6: Refactor Command Handler

**Location**: `regtech-risk-calculation/application/src/main/java/com/bcbs239/regtech/riskcalculation/application/calculation/CalculateRiskMetricsCommandHandler.java`

**Key Changes**:
1. Add `BaseUnitOfWork` dependency
2. Remove `RiskCalculationEventPublisher` dependency
3. Create Batch aggregate instead of calling repository methods directly
4. Call domain methods on aggregate (which raise events)
5. Register aggregate with UnitOfWork
6. Call `unitOfWork.saveChanges()` to persist events to outbox
7. **Simplify error handling**: Replace multiple nested try-catch blocks with one top-level try-catch and use Result pattern throughout

**Simplified Example** (see full implementation in Step 6 details below):

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CalculateRiskMetricsCommandHandler {

    private final BatchRepository batchRepository;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final IFileStorageService fileStorageService;
    private final ICalculationResultsStorageService calculationResultsStorageService;
    private final ExchangeRateProvider exchangeRateProvider;
    private final BaseUnitOfWork unitOfWork; // ADD THIS
    // REMOVE: private final RiskCalculationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final PerformanceMetrics performanceMetrics;

    @Transactional
    public Result<Void> handle(CalculateRiskMetricsCommand command) {
        String batchId = command.getBatchId();
        performanceMetrics.recordBatchStart(batchId);
        
        try {
            // Download file - returns Result
            Result<String> downloadResult = fileStorageService.retrieveFile(command.getS3Uri());
            if (downloadResult.isFailure()) {
                return handleFailure(batchId, command.getBankId(), "File download failed");
            }
            
            // Parse JSON - returns Result
            Result<List<ExposureRecording>> parseResult = parseExposures(downloadResult.getValue().get());
            if (parseResult.isFailure()) {
                return handleFailure(batchId, command.getBankId(), "Parse failed");
            }
            
            List<ExposureRecording> exposures = parseResult.getValue().get();
            
            // CREATE BATCH AGGREGATE (instead of batchRepository.createBatch)
            Batch batch = Batch.create(
                command.getBatchId(),
                bankInfo,
                exposures.size(),
                command.getS3Uri()
            );
            
            // ... existing calculation logic ...
            
            // Store results - returns Result
            Result<String> storageResult = calculationResultsStorageService.storeCalculationResults(calculationResult);
            if (storageResult.isFailure()) {
                return handleFailure(batchId, command.getBankId(), "Storage failed");
            }
            
            // COMPLETE BATCH (raises domain event)
            batch.completeCalculation(storageResult.getValue().get(), protectedExposures.size());
            
            // SAVE AGGREGATE
            batchRepository.save(batch);
            
            // REGISTER WITH UNIT OF WORK AND PERSIST EVENTS
            unitOfWork.registerEntity(batch);
            unitOfWork.saveChanges(); // Persists events to outbox
            
            performanceMetrics.recordBatchSuccess(batchId, protectedExposures.size());
            return Result.success();
            
        } catch (Exception e) {
            // ONE catch block for all unexpected errors
            log.error("Risk calculation failed for batch: {}", batchId, e);
            return handleFailure(batchId, command.getBankId(), e.getMessage());
        }
    }
    
    private Result<Void> handleFailure(String batchId, String bankId, String reason) {
        try {
            Maybe<Batch> maybeBatch = batchRepository.findById(BatchId.of(batchId));
            Batch batch = maybeBatch.orElse(() -> 
                Batch.create(batchId, BankInfo.of("Unknown", bankId, "UNKNOWN"), 0, "")
            );
            
            batch.failCalculation(reason);
            batchRepository.save(batch);
            unitOfWork.registerEntity(batch);
            unitOfWork.saveChanges();
            
            performanceMetrics.recordBatchFailure(batchId, reason);
        } catch (Exception e) {
            log.error("Failed to record batch failure: {}", batchId, e);
        }
        
        return Result.failure(ErrorDetail.of(
            "CALCULATION_FAILED",
            ErrorType.SYSTEM_ERROR,
            reason,
            "calculation.failed"
        ));
    }
}
```

**Error Handling Improvement**:

**Before** (current implementation):
- Multiple nested try-catch blocks
- Specific catches for FileNotFoundException, JsonProcessingException, CalculationResultsSerializationException, etc.
- Hard to follow the flow
- Duplicated error handling logic

**After** (refactored):
- ONE top-level try-catch for unexpected errors
- Use Result pattern for expected failures (file not found, parse errors, storage errors)
- Check Result.isFailure() and handle gracefully
- Cleaner, more maintainable code
- All failures go through handleFailure() method

### Step 7: Remove RiskCalculationEventPublisher

**Location**: `regtech-risk-calculation/application/src/main/java/com/bcbs239/regtech/riskcalculation/application/integration/RiskCalculationEventPublisher.java`

The `RiskCalculationEventPublisher` in the application layer is no longer needed. Events are now:
1. Raised by the Batch aggregate
2. Collected by BaseUnitOfWork
3. Persisted to outbox table
4. Published by OutboxProcessor background job

**Action**: Delete this file or mark as deprecated.

### Step 8: Update Batch Aggregate with Persistence Methods

**Location**: `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/calculation/Batch.java`

Following the principle "Tell, don't ask" - instead of creating a mapper that extracts data from the aggregate, we ask the aggregate to provide what we need for persistence.

Add these methods to the Batch aggregate:

```java
/**
 * Populate a persistence entity with this aggregate's data.
 * The aggregate knows how to represent itself for persistence.
 */
public void populateEntity(BatchEntity entity) {
    entity.setBatchId(this.id.value());
    entity.setBankName(this.bankInfo.bankName());
    entity.setAbiCode(this.bankInfo.abiCode());
    entity.setLeiCode(this.bankInfo.leiCode());
    entity.setStatus(this.status.name());
    entity.setTotalExposures(this.totalExposures);
    entity.setSourceDataUri(this.sourceDataUri.value());
    
    if (this.calculationResultsUri != null) {
        entity.setCalculationResultsUri(this.calculationResultsUri.value());
    }
    
    entity.setStartedAt(this.timestamps.startedAt());
    this.timestamps.completedAt().ifPresent(entity::setCompletedAt);
    this.timestamps.failedAt().ifPresent(entity::setFailedAt);
    
    if (this.failureReason != null) {
        entity.setFailureReason(this.failureReason);
    }
}
```

**Why this is better**:
- The aggregate encapsulates its own persistence logic
- No need for a separate mapper class
- Follows "Tell, don't ask" principle
- The aggregate decides what data to expose and how

### Step 9: Update Infrastructure Layer - JpaBatchRepository

**Location**: `regtech-risk-calculation/infrastructure/src/main/java/com/bcbs239/regtech/riskcalculation/infrastructure/database/repositories/JpaBatchRepository.java`

Update `JpaBatchRepository` to use the aggregate's `populateEntity` method:

```java
@Repository
@RequiredArgsConstructor
public class JpaBatchRepository implements BatchRepository {
    
    private final SpringDataBatchRepository springDataRepository;
    
    @Override
    public Result<Void> save(Batch batch) {
        try {
            // Ask the aggregate to populate the entity
            BatchEntity entity = new BatchEntity();
            batch.populateEntity(entity);
            springDataRepository.save(entity);
            return Result.success();
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of(
                "BATCH_SAVE_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to save batch: " + e.getMessage(),
                "batch.save.failed"
            ));
        }
    }
    
    @Override
    public Maybe<Batch> findById(BatchId batchId) {
        return springDataRepository.findById(batchId.value())
            .map(entity -> Batch.reconstitute(
                BatchId.of(entity.getBatchId()),
                BankInfo.of(entity.getBankName(), entity.getAbiCode(), entity.getLeiCode()),
                BatchStatus.valueOf(entity.getStatus()),
                entity.getTotalExposures(),
                FileStorageUri.of(entity.getSourceDataUri()),
                entity.getCalculationResultsUri() != null 
                    ? FileStorageUri.of(entity.getCalculationResultsUri()) 
                    : null,
                ProcessingTimestamps.reconstitute(
                    entity.getStartedAt(),
                    Optional.ofNullable(entity.getCompletedAt()),
                    Optional.ofNullable(entity.getFailedAt())
                ),
                entity.getFailureReason()
            ))
            .map(Maybe::of)
            .orElse(Maybe.empty());
    }
    
    @Override
    public List<Batch> findByStatus(BatchStatus status) {
        return springDataRepository.findByStatus(status.name())
            .stream()
            .map(entity -> Batch.reconstitute(
                BatchId.of(entity.getBatchId()),
                BankInfo.of(entity.getBankName(), entity.getAbiCode(), entity.getLeiCode()),
                BatchStatus.valueOf(entity.getStatus()),
                entity.getTotalExposures(),
                FileStorageUri.of(entity.getSourceDataUri()),
                entity.getCalculationResultsUri() != null 
                    ? FileStorageUri.of(entity.getCalculationResultsUri()) 
                    : null,
                ProcessingTimestamps.reconstitute(
                    entity.getStartedAt(),
                    Optional.ofNullable(entity.getCompletedAt()),
                    Optional.ofNullable(entity.getFailedAt())
                ),
                entity.getFailureReason()
            ))
            .toList();
    }
}
```

**Key Principle**: "Tell, don't ask"
- We tell the aggregate: "populate this entity"
- We don't ask the aggregate: "give me your data so I can build an entity"
- The aggregate controls its own representation

### Step 10: Update Tests

## Benefits of This Refactoring

1. **Proper DDD**: Batch is now a true aggregate root with business logic
2. **Reliable Events**: Outbox pattern ensures events are never lost
3. **Transactional Consistency**: Batch state and events are saved in same transaction
4. **Testability**: Can test aggregate behavior in isolation
5. **Event Sourcing Ready**: Can later implement event sourcing if needed
6. **Audit Trail**: All events are persisted and can be replayed
7. **Decoupling**: Application layer doesn't know about event publishing

## Migration Strategy

1. Create new Batch aggregate and domain events
2. Update BatchRepository interface
3. Implement new repository methods
4. Refactor command handler to use aggregate
5. Update tests to verify events are raised
6. Remove old RiskCalculationEventPublisher
7. Verify outbox processor picks up events
8. Deploy and monitor

### Step 10: Update Tests

**Location**: `regtech-risk-calculation/application/src/test/java/com/bcbs239/regtech/riskcalculation/application/calculation/CalculateRiskMetricsCommandHandlerTest.java`

Update existing tests to:
1. Mock `BaseUnitOfWork` instead of `RiskCalculationEventPublisher`
2. Verify `unitOfWork.registerEntity()` is called with batch
3. Verify `unitOfWork.saveChanges()` is called
4. Verify batch aggregate methods are called (create, completeCalculation, failCalculation)

**Example Test Update**:

```java
@Test
void shouldCompleteCalculationAndPersistEventsToOutbox() {
    // Arrange
    when(fileStorageService.retrieveFile(any())).thenReturn(Result.success(testJsonContent));
    when(calculationResultsStorageService.storeCalculationResults(any()))
        .thenReturn(Result.success("s3://results/batch_123.json"));
    when(batchRepository.save(any())).thenReturn(Result.success());
    
    // Act
    Result<Void> result = handler.handle(command);
    
    // Assert
    assertTrue(result.isSuccess());
    verify(unitOfWork).registerEntity(any(Batch.class)); // Verify aggregate registered
    verify(unitOfWork).saveChanges(); // Verify events persisted to outbox
    verify(batchRepository).save(any(Batch.class)); // Verify aggregate saved
    // REMOVE: verify(eventPublisher).publishBatchCalculationCompleted(...)
}
```

## Testing Strategy

### Unit Tests
- Test Batch aggregate methods raise correct events
- Test state transitions (PROCESSING → COMPLETED/FAILED)
- Test invariants (can't complete already completed batch)
- Test PortfolioAnalysis extends Entity and can raise events

### Integration Tests
- Verify events are persisted to outbox table
- Verify OutboxProcessor publishes events
- Verify transactional consistency
- Verify command handler uses aggregates correctly

## Implementation Checklist

- [ ] Step 1: Update PortfolioAnalysis to extend Entity
- [ ] Step 2: Create Batch aggregate root
- [ ] Step 3: Create domain events (BatchCalculationStartedEvent, update existing events)
- [ ] Step 4: Create BatchStatus value object
- [ ] Step 5: Update BatchRepository interface
- [ ] Step 6: Refactor CalculateRiskMetricsCommandHandler
- [ ] Step 7: Remove RiskCalculationEventPublisher
- [ ] Step 8: Add populateEntity method to Batch aggregate
- [ ] Step 9: Update JpaBatchRepository (no mapper needed!)
- [ ] Step 10: Update tests
- [ ] Verify OutboxProcessor picks up events
- [ ] Verify transactional consistency
- [ ] Deploy and monitor

## Summary

This refactoring transforms the risk calculation module from an anemic domain model to a proper DDD implementation:

**Before**:
- Application layer directly manipulates repositories
- Events published synchronously by application layer
- No transactional consistency between state and events
- Business logic scattered across layers

**After**:
- Batch and PortfolioAnalysis are proper aggregates extending Entity
- Domain events raised by aggregates
- Outbox pattern ensures reliable event delivery
- Transactional consistency between aggregate state and events
- Business logic encapsulated in domain layer
- Application layer orchestrates, domain layer decides

This follows the exact pattern used in the IAM module's `RegisterUserCommandHandler` and provides a solid foundation for future enhancements like event sourcing.

## References

- IAM Module: `regtech-iam/application/src/main/java/com/bcbs239/regtech/iam/application/users/RegisterUserCommandHandler.java`
- User Aggregate: `regtech-iam/domain/src/main/java/com/bcbs239/regtech/iam/domain/users/User.java`
- BaseUnitOfWork: `regtech-core/application/src/main/java/com/bcbs239/regtech/core/application/BaseUnitOfWork.java`
- Entity Base Class: `regtech-core/domain/src/main/java/com/bcbs239/regtech/core/domain/shared/Entity.java`
