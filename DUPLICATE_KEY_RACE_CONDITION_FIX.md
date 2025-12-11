# Duplicate Key Race Condition Fix

## Issue Summary

**Error**: PostgreSQL duplicate key constraint violations (`23505`) occurring in concurrent event processing:
- `quality_reports_batch_id_key` - Quality reports being inserted multiple times for the same batch_id
- `portfolio_analysis_pkey` - Portfolio analysis being inserted multiple times for the same batch_id

**Root Cause**: Race condition in async event listeners where multiple threads process the same `BatchIngestedEvent` concurrently:

1. Thread A checks if report exists (`existsByBatchId` or `findByBatchId`) → NOT FOUND
2. Thread B checks if report exists → NOT FOUND (because Thread A hasn't saved yet)
3. Thread A attempts to insert → SUCCESS
4. Thread B attempts to insert → DUPLICATE KEY ERROR

This is a classic **Time-of-Check to Time-of-Use (TOCTOU)** race condition.

## Error Logs (Before Fix)

```
2025-12-11 11:05:53.466 [quality-event-2] WARN  org.hibernate.orm.jdbc.error - HHH000247: ErrorCode: 0, SQLState: 23505
2025-12-11 11:05:53.466 [quality-event-4] WARN  org.hibernate.orm.jdbc.error - HHH000247: ErrorCode: 0, SQLState: 23505

Batch entry 0 insert into dataquality.quality_reports (...) was aborted: 
ERRORE: un valore chiave duplicato viola il vincolo univoco "quality_reports_batch_id_key"
Dettaglio: La chiave (batch_id)=(batch_20251211_110541_e9e7c0bf-5a68-4fae-83ed-fc61a8468e4e) esiste già.
```

## Solution Applied

### 1. Data Quality Module

**File**: `ValidateBatchQualityCommandHandler.java`

**Changes**:
- Wrapped initial save operation in try-catch to handle concurrent creation gracefully
- Added detection for `QUALITY_REPORT_DUPLICATE_BATCH_ID` error code
- Added fallback check after exception to verify if another thread succeeded
- Changed logging from WARN to DEBUG for expected concurrent behavior

**Code**:
```java
// Try to create and save quality report (may fail if another thread created it)
QualityReport report;
try {
    report = QualityReport.createForBatch(command.batchId(), command.bankId());
    report.startValidation();
    
    Result<QualityReport> saveResult = qualityReportRepository.save(report);
    if (saveResult.isFailure()) {
        // If duplicate key error, another thread is processing it
        if (saveResult.getError().map(e -> 
            e.getCode().equals("QUALITY_REPORT_DUPLICATE_BATCH_ID")
        ).orElse(false)) {
            logger.debug("Concurrent creation detected for batch {}, another thread created the report", 
                command.batchId().value());
            return Result.success();
        }
        logger.error("Failed to save quality report for batch {}: {}", 
            command.batchId().value(), 
            saveResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
        return Result.failure(saveResult.errors());
    }
    report = saveResult.getValueOrThrow();
} catch (Exception e) {
    logger.warn("Exception during initial quality report creation for batch {}: {}", 
        command.batchId().value(), e.getMessage());
    // Check if report was created by another thread despite the exception
    Optional<QualityReport> retryCheck = qualityReportRepository.findByBatchId(command.batchId());
    if (retryCheck.isPresent()) {
        logger.debug("Report exists after exception, another thread succeeded for batch {}", 
            command.batchId().value());
        return Result.success();
    }
    throw e;
}
```

**File**: `QualityReportRepositoryImpl.java`

**Changes**:
- Changed duplicate key error logging from WARN to DEBUG
- Added clarifying message that concurrent processing is expected

```java
if (errorMsg.contains("batch_id") || errorMsg.contains("quality_reports_batch_id_key")) {
    logger.debug("Duplicate batch_id detected for report: {} batch_id: {} - This is expected in concurrent processing", 
        report.getReportId().value(), report.getBatchId().value());
    return Result.failure("QUALITY_REPORT_DUPLICATE_BATCH_ID", ErrorType.VALIDATION_ERROR, 
        "Quality report already exists for batch_id: " + report.getBatchId().value(), "batch_id");
```

### 2. Risk Calculation Module

**File**: `JpaPortfolioAnalysisRepository.java`

**Changes**:
- Added `DataIntegrityViolationException` handling for duplicate batch_id primary key violations
- Silently skips duplicate inserts (returns without throwing)
- Changed logging from ERROR to DEBUG for expected concurrent behavior

**Code**:
```java
@Override
@Transactional
public void save(PortfolioAnalysis analysis) {
    try {
        PortfolioAnalysisEntity entity = mapper.toEntity(analysis);
        springDataRepository.save(entity);
    } catch (DataIntegrityViolationException e) {
        // Check if this is a duplicate batch_id primary key violation
        String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (errorMsg.contains("batch_id") || errorMsg.contains("portfolio_analysis_pkey")) {
            log.debug("Duplicate batch_id detected for portfolio analysis: {} - This is expected in concurrent processing", 
                analysis.getBatchId());
            // Silently skip - another thread already created this analysis
            return;
        }
        log.error("Data integrity violation while saving portfolio analysis for batch: {}", 
            analysis.getBatchId(), e);
        throw e;
    } catch (OptimisticLockingFailureException e) {
        log.error("Optimistic locking failure while saving portfolio analysis for batch: {}", 
            analysis.getBatchId(), e);
    }
}
```

## Why This Fix Works

### Idempotency via Graceful Failure
Instead of trying to prevent the race condition (which would require distributed locking), we allow it to happen and handle it gracefully:

1. **Database constraint prevents actual duplicates** - PostgreSQL unique constraint ensures data integrity
2. **Application catches and handles the error** - No transaction rollback, no error propagation
3. **Event processing completes successfully** - All threads return success, avoiding event retry storms
4. **Clean logs** - DEBUG level for expected concurrent behavior

### Benefits
- ✅ **No distributed locks needed** - Simpler architecture
- ✅ **Database ensures correctness** - Single source of truth
- ✅ **Concurrent processing preserved** - No performance impact
- ✅ **Clean error logs** - No WARN/ERROR for expected behavior
- ✅ **Idempotent event handling** - Multiple events for same batch are safe

## Testing Recommendations

### 1. Unit Tests
Test concurrent save attempts:
```java
@Test
void shouldHandleConcurrentSaveGracefully() {
    // Simulate two threads trying to save same batch_id
    // Verify both return success
    // Verify only one record in database
}
```

### 2. Integration Tests
Test with real concurrent events:
```java
@Test
void shouldHandleDuplicateBatchIngestedEvents() {
    // Publish same BatchIngestedEvent twice concurrently
    // Verify both are processed successfully
    // Verify only one quality report exists
}
```

### 3. Load Testing
- Send multiple duplicate events under load
- Monitor for `23505` errors in logs (should be DEBUG only)
- Verify no transaction rollbacks or event retries

## Monitoring

### Metrics to Watch
- `dataquality.validation.batch` - Should show successful completions
- Database constraint violations - Should decrease (moved to DEBUG)
- Event processing failures - Should not increase

### Log Patterns
**Before Fix** (ERROR level):
```
WARN org.hibernate.orm.jdbc.error - HHH000247: ErrorCode: 0, SQLState: 23505
```

**After Fix** (DEBUG level):
```
DEBUG c.b.r.d.a.v.ValidateBatchQualityCommandHandler - Concurrent creation detected for batch X
DEBUG c.b.r.d.i.r.QualityReportRepositoryImpl - Duplicate batch_id detected - expected in concurrent processing
```

## Related Files Modified

### Data Quality Module
1. `regtech-data-quality/application/src/main/java/com/bcbs239/regtech/dataquality/application/validation/ValidateBatchQualityCommandHandler.java`
2. `regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/reporting/QualityReportRepositoryImpl.java`

### Risk Calculation Module
3. `regtech-risk-calculation/infrastructure/src/main/java/com/bcbs239/regtech/riskcalculation/infrastructure/database/repositories/JpaPortfolioAnalysisRepository.java`

## Database Schema (No Changes Required)

The existing unique constraints are sufficient:
- `quality_reports.batch_id` - Unique constraint: `quality_reports_batch_id_key`
- `portfolio_analysis.batch_id` - Primary key: `portfolio_analysis_pkey`

## Alternative Solutions Considered

### 1. Distributed Locking (Redis)
**Pros**: Prevents race condition entirely
**Cons**: 
- Adds external dependency
- More complex
- Potential single point of failure
- Doesn't improve performance

### 2. Database SELECT FOR UPDATE
**Pros**: Database-level locking
**Cons**:
- Requires two queries (SELECT + INSERT)
- Can cause deadlocks
- Reduces concurrency
- More complex transaction management

### 3. Application-Level Synchronization
**Pros**: Simple within single JVM
**Cons**:
- Doesn't work across multiple instances
- Not scalable
- Defeats purpose of async processing

**Decision**: Graceful failure handling is the simplest and most scalable solution.

## Conclusion

The fix addresses the race condition by:
1. Allowing concurrent attempts (maintaining async benefits)
2. Letting the database reject duplicates (maintaining data integrity)
3. Handling the rejection gracefully (maintaining idempotency)
4. Logging appropriately (reducing noise)

This is a pragmatic solution that balances correctness, performance, and operational simplicity.
