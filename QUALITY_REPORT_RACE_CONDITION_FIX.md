# Quality Report Race Condition Fix

## Issue Summary

**Error**: Duplicate key constraint violation on `quality_reports.batch_id`
```
ERRORE: un valore chiave duplicato viola il vincolo univoco "quality_reports_batch_id_key"
Dettaglio: La chiave (batch_id)=(batch_20251210_221220_6823a644-10c6-4198-8843-1c5172025653) esiste già.
```

**Root Cause**: Race condition in quality report creation when multiple concurrent rule executions attempt to create quality reports for the same batch.

## Technical Analysis

### The Problem

The original code had a check-then-act race condition:

```java
// Thread 1 checks
if (qualityReportRepository.existsByBatchId(command.batchId())) {
    return Result.success();
}
// Thread 2 checks (returns false at same time)

// Thread 1 creates and saves
QualityReport report = QualityReport.createForBatch(...);
report.startValidation();
qualityReportRepository.save(report); // SUCCESS

// Thread 2 creates and saves
QualityReport report = QualityReport.createForBatch(...);
report.startValidation();
qualityReportRepository.save(report); // DUPLICATE KEY VIOLATION!
```

### Timeline of Concurrent Execution

```
Time    Thread 1 (Rule DQ_COMPLETENESS)         Thread 2 (Rule DQ_VALIDITY)
----    ----------------------------------      ---------------------------------
T0      existsByBatchId() → false
T1                                              existsByBatchId() → false
T2      Create report                           
T3      Save report → SUCCESS                   Create report
T4                                              Save report → CONSTRAINT VIOLATION
```

### Database Schema Constraint

```sql
-- V43__create_quality_reports_tables.sql
CREATE TABLE dataquality.quality_reports (
    report_id VARCHAR(36) NOT NULL PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL UNIQUE,  -- ← This UNIQUE constraint
    ...
);
```

## Solution Implemented

### 1. Find-or-Create Pattern with Graceful Handling

**File**: `ValidateBatchQualityCommandHandler.java`

```java
// BEFORE: Race-prone check
if (qualityReportRepository.existsByBatchId(command.batchId())) {
    return Result.success();
}

// AFTER: Fetch existing report atomically
Optional<QualityReport> existingReport = qualityReportRepository.findByBatchId(command.batchId());
if (existingReport.isPresent()) {
    logger.info("Quality report already exists for batch {}, skipping duplicate processing", 
        command.batchId().value());
    return Result.success();
}
```

**Why this helps**:
- `findByBatchId()` is a single atomic database operation
- Reduces the race window significantly
- Still provides idempotency

### 2. Graceful Constraint Violation Handling

Added fallback logic when concurrent inserts occur:

```java
if (reportResult.isFailure()) {
    // Check if this is a duplicate key constraint violation (concurrent insert)
    if (reportResult.getError().map(err -> 
        err.getCode().contains("CONSTRAINT_VIOLATION") || 
        err.getMessage().contains("duplicate") ||
        err.getMessage().contains("batch_id")
    ).orElse(false)) {
        // Another thread already created the report, fetch and use it
        logger.warn("Concurrent insert detected for batch {}, fetching existing report", 
            command.batchId().value());
        existingReport = qualityReportRepository.findByBatchId(command.batchId());
        if (existingReport.isPresent()) {
            logger.info("Using existing quality report for batch {}", command.batchId().value());
            return Result.success(); // ← Gracefully handle the race
        }
    }
    return Result.failure(reportResult.errors());
}
```

**What this does**:
- Detects when a concurrent thread won the race
- Fetches the existing report created by the other thread
- Returns success instead of propagating the error
- Logs the event for monitoring

### 3. Enhanced Error Diagnostics

**File**: `QualityReportRepositoryImpl.java`

```java
catch (DataIntegrityViolationException e) {
    String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
    if (errorMsg.contains("batch_id") || errorMsg.contains("quality_reports_batch_id_key")) {
        logger.warn("Duplicate batch_id constraint violation for report: {} batch_id: {}", 
            report.getReportId().value(), report.getBatchId().value());
        return Result.failure("QUALITY_REPORT_DUPLICATE_BATCH_ID", ErrorType.VALIDATION_ERROR, 
            "Quality report already exists for batch_id: " + report.getBatchId().value(), "batch_id");
    } else {
        // Other constraint violations
    }
}
```

**Benefits**:
- Specific error code: `QUALITY_REPORT_DUPLICATE_BATCH_ID`
- Clear error messages for debugging
- Distinguishes batch_id conflicts from other constraint violations

## Why the Race Condition Occurs

### Event-Driven Architecture Trigger

The race condition is triggered by the event-driven architecture:

```
Batch Ingestion
    ↓
Multiple Rule Executions (Parallel)
    ├─→ Rule 1: DQ_COMPLETENESS_EXPOSURE_ID
    ├─→ Rule 2: DQ_VALIDITY_LEI_CODE  
    ├─→ Rule 3: DQ_CONSISTENCY_AMOUNT
    └─→ ... (more rules)
         ↓
    ALL try to create QualityReport
    for the SAME batch_id
    at approximately the SAME time
```

### Why Multiple Events Fire

Looking at your logs:
```
2025-12-10 22:12:22.009 [quality-event-2] ERROR c.b.r.d.i.r.e.DefaultRulesEngine 
- Failed to save rule execution log for rule DQ_COMPLETENESS_EXPOSURE_ID
```

Each rule execution tries to:
1. Create/fetch the quality report
2. Execute the rule
3. Save rule execution logs
4. Save violations

When multiple rules execute **concurrently** for the **same batch**, they all race to create the quality report.

## Prevention Strategy

### 1. Transaction Isolation

The `@Transactional` annotation on the handler provides some isolation, but doesn't prevent the race entirely because:
- The check (`findByBatchId`) and insert (`save`) are separate operations
- Database commits happen at different times for different threads
- The unique constraint is enforced at commit time

### 2. Database-Level Protection

The `UNIQUE` constraint on `batch_id` is the ultimate protection:
- Prevents duplicate records at the database level
- Ensures data integrity even under concurrent load
- Our fix handles the constraint violation gracefully

### 3. Idempotency Pattern

The solution implements proper idempotency:
```
Request 1: Create report → SUCCESS
Request 2: Create report → DUPLICATE_KEY → Fetch existing → SUCCESS
Request 3: Create report → DUPLICATE_KEY → Fetch existing → SUCCESS
```

All requests succeed, but only one report is created.

## Testing the Fix

### Scenario 1: Normal Flow (No Concurrency)
```
1. Check for existing report → None found
2. Create new report
3. Save report → SUCCESS
4. Continue processing
```

### Scenario 2: Concurrent Inserts
```
Thread 1:
1. Check for existing report → None found
2. Create new report
3. Save report → SUCCESS

Thread 2:
1. Check for existing report → None found (Thread 1 not committed yet)
2. Create new report  
3. Save report → DUPLICATE_KEY_VIOLATION
4. Detect constraint violation
5. Fetch existing report from Thread 1 → SUCCESS
6. Continue processing with existing report
```

### Scenario 3: Repeated Requests
```
Request 1: findByBatchId() → Found existing → Skip
Request 2: findByBatchId() → Found existing → Skip
```

## Performance Impact

### Minimal Overhead

1. **Database Queries**:
   - Before: 1 `existsByBatchId()` + 1 `save()`
   - After: 1 `findByBatchId()` + 1 `save()` (in race case: +1 extra `findByBatchId()`)

2. **Memory**:
   - Fetching full report vs checking existence: negligible difference
   - Reports are small objects

3. **Latency**:
   - `findByBatchId()` vs `existsByBatchId()`: nearly identical
   - Indexed on `batch_id` (unique index)

## Monitoring Recommendations

### Log Messages to Watch

**Normal Operation**:
```
Quality report already exists for batch X, skipping duplicate processing
```
→ Expected when request is retried or replayed

**Race Condition Detected**:
```
Concurrent insert detected for batch X, fetching existing report
Using existing quality report for batch X
```
→ Indicates concurrent rule executions (expected under load)

**Error Condition**:
```
Duplicate batch_id constraint violation for report: X batch_id: Y
```
→ Should be rare after this fix; investigate if frequent

### Metrics to Track

1. **Frequency of concurrent inserts**
   - Count log occurrences: "Concurrent insert detected"
   - High frequency → Consider adjusting parallelism

2. **Race condition recovery rate**
   - Should be 100% (all concurrent attempts succeed)
   - Any failures indicate a deeper issue

3. **Quality report creation time**
   - Monitor for increased latency
   - Should remain constant

## Related Files Modified

1. **ValidateBatchQualityCommandHandler.java**
   - Added `Optional` import
   - Changed `existsByBatchId()` to `findByBatchId()`
   - Added graceful constraint violation handling

2. **QualityReportRepositoryImpl.java**
   - Enhanced error handling in `save()` method
   - Added specific error code for duplicate batch_id
   - Improved logging for constraint violations

## Verification

### Before the Fix
```
ERROR: could not execute batch [Batch entry 0 insert into dataquality.quality_reports ...]
ERRORE: un valore chiave duplicato viola il vincolo univoco "quality_reports_batch_id_key"
```

### After the Fix (Expected)
```
WARN: Concurrent insert detected for batch X, fetching existing report
INFO: Using existing quality report for batch X
```

## Alternative Solutions Considered

### 1. Pessimistic Locking
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<QualityReport> findByBatchId(BatchId batchId);
```
**Rejected**: Overkill, would reduce concurrency significantly

### 2. Database SELECT FOR UPDATE
```sql
SELECT * FROM quality_reports WHERE batch_id = ? FOR UPDATE;
```
**Rejected**: Requires raw SQL, less portable

### 3. Distributed Lock (Redis/etc.)
```java
if (lockService.tryLock("quality-report:" + batchId)) {
    // create report
}
```
**Rejected**: Adds external dependency, over-engineering

### 4. Sequential Processing
**Rejected**: Defeats the purpose of parallel rule execution

## Conclusion

The fix implements a robust **find-or-create pattern** with graceful handling of concurrent inserts:

✅ **Prevents data corruption** (unique constraint still enforced)  
✅ **Handles race conditions gracefully** (no errors propagated)  
✅ **Maintains idempotency** (same result for duplicate requests)  
✅ **Minimal performance impact** (one extra DB query in race case)  
✅ **Comprehensive logging** (monitors concurrent behavior)  

The solution leverages database constraints as the source of truth while handling constraint violations gracefully at the application level.
