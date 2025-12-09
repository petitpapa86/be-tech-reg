# DDD Aggregate Refactoring - Implementation Progress

## Completed Steps ✓

### Step 1: Update PortfolioAnalysis to Extend Entity ✓
- **File**: `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/analysis/PortfolioAnalysis.java`
- **Changes**:
  - Extended `Entity` base class
  - Updated `analyze()` method to raise `PortfolioAnalysisCompletedEvent`
  - Added import for `Entity` and event

### Step 2: Create Batch Aggregate Root ✓
- **File**: `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/calculation/Batch.java`
- **Created**: Complete aggregate root with:
  - Factory method `create()` that raises `BatchCalculationStartedEvent`
  - Business method `completeCalculation()` that raises `BatchCalculationCompletedEvent`
  - Business method `failCalculation()` that raises `BatchCalculationFailedEvent`
  - Reconstitution method for persistence
  - Proper encapsulation and invariant enforcement

### Step 3: Create/Update Domain Events ✓
- **Created**: `BatchCalculationStartedEvent.java` - New event for batch start
- **Updated**: `BatchCalculationCompletedEvent.java` - Simplified to match plan
- **Updated**: `BatchCalculationFailedEvent.java` - Simplified to match plan
- **Created**: `PortfolioAnalysisCompletedEvent.java` - New event for portfolio analysis

### Step 4: Create BatchStatus Value Object ✓
- **File**: `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/calculation/BatchStatus.java`
- **Created**: Enum with PROCESSING, COMPLETED, FAILED states

### Additional Updates ✓
- **Updated**: `ProcessingTimestamps.java` - Added methods for reconstitution and Optional returns

## Remaining Steps

### Step 5: Update BatchRepository Interface
- **File**: `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/persistence/BatchRepository.java`
- **Action**: Update interface to work with Batch aggregate
- **Methods needed**:
  - `Result<Void> save(Batch batch)`
  - `Maybe<Batch> findById(BatchId batchId)`
  - `List<Batch> findByStatus(BatchStatus status)`

### Step 6: Refactor CalculateRiskMetricsCommandHandler
- **File**: `regtech-risk-calculation/application/src/main/java/com/bcbs239/regtech/riskcalculation/application/calculation/CalculateRiskMetricsCommandHandler.java`
- **Major changes**:
  - Add `BaseUnitOfWork` dependency
  - Remove `RiskCalculationEventPublisher` dependency
  - Use `Batch.create()` instead of repository methods
  - Call `batch.completeCalculation()` or `batch.failCalculation()`
  - Register aggregate with UnitOfWork
  - Call `unitOfWork.saveChanges()` to persist events
  - Simplify error handling (one try-catch, use Result pattern)

### Step 7: Remove RiskCalculationEventPublisher
- **File**: `regtech-risk-calculation/application/src/main/java/com/bcbs239/regtech/riskcalculation/application/integration/RiskCalculationEventPublisher.java`
- **Action**: Delete or deprecate (events now go through outbox)

### Step 8: Add populateEntity Method to Batch
- **File**: `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/calculation/Batch.java`
- **Action**: Add method for aggregate to populate persistence entity
- **Principle**: "Tell, don't ask" - aggregate controls its own persistence representation

### Step 9: Update JpaBatchRepository
- **File**: `regtech-risk-calculation/infrastructure/src/main/java/com/bcbs239/regtech/riskcalculation/infrastructure/database/repositories/JpaBatchRepository.java`
- **Action**: Implement new repository interface using aggregate's `populateEntity` method
- **No mapper needed**: Aggregate handles its own persistence

### Step 10: Update Tests
- **File**: `regtech-risk-calculation/application/src/test/java/com/bcbs239/regtech/riskcalculation/application/calculation/CalculateRiskMetricsCommandHandlerTest.java`
- **Action**: Update tests to:
  - Mock `BaseUnitOfWork` instead of `RiskCalculationEventPublisher`
  - Verify `unitOfWork.registerEntity()` called
  - Verify `unitOfWork.saveChanges()` called
  - Verify aggregate methods called

## Key Architectural Changes

**Before (Anemic Domain)**:
```
CommandHandler → Repository (direct manipulation)
CommandHandler → EventPublisher (synchronous)
```

**After (Rich Domain)**:
```
CommandHandler → Batch.create() → raises event
CommandHandler → batch.completeCalculation() → raises event
CommandHandler → batchRepository.save(batch)
CommandHandler → unitOfWork.registerEntity(batch)
CommandHandler → unitOfWork.saveChanges() → persists to outbox
OutboxProcessor → publishes events asynchronously
```

## Benefits Achieved So Far

1. ✓ **Proper Aggregates**: Batch and PortfolioAnalysis are now true aggregate roots
2. ✓ **Domain Events**: Events are raised by aggregates, not application layer
3. ✓ **Business Logic Encapsulation**: Logic is in domain, not scattered
4. ✓ **Invariant Enforcement**: Aggregates enforce their own rules
5. ⏳ **Reliable Events**: Will be achieved when outbox pattern is wired up
6. ⏳ **Transactional Consistency**: Will be achieved with UnitOfWork integration

## Next Actions

1. Continue with Step 5 (Update BatchRepository interface)
2. Proceed through Steps 6-10 sequentially
3. Run tests after each step
4. Verify outbox processor integration
5. Deploy and monitor

## Files Created/Modified

### Created:
- `Batch.java` - Aggregate root
- `BatchStatus.java` - Value object
- `BatchCalculationStartedEvent.java` - Domain event
- `PortfolioAnalysisCompletedEvent.java` - Domain event

### Modified:
- `PortfolioAnalysis.java` - Now extends Entity
- `BatchCalculationCompletedEvent.java` - Simplified
- `BatchCalculationFailedEvent.java` - Simplified
- `ProcessingTimestamps.java` - Added reconstitution methods

## Reference Implementation

Following the pattern from:
- `regtech-iam/application/src/main/java/com/bcbs239/regtech/iam/application/users/RegisterUserCommandHandler.java`
- `regtech-iam/domain/src/main/java/com/bcbs239/regtech/iam/domain/users/User.java`
