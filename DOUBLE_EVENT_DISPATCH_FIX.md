# Double Event Dispatch Fix

## Problem
The data quality module was processing integration events twice, causing duplicate processing. This was happening because:

1. **Missing Inbox Replay Check**: Event listeners were not checking the `CorrelationContext.isInboxReplay()` scoped value
2. **Duplicate Event Listeners**: The data quality module had two separate event listeners processing the same logical events

## Root Cause Analysis

### Integration Event Flow
1. **Outbox → Integration Event Bus**: Events are published from outbox to integration event bus
2. **IntegrationEventReceiver**: Saves events to inbox (unless `isInboxReplay()` is true)
3. **Inbox Processing Job**: Replays events from inbox with `INBOX_REPLAY` scoped value set to `true`
4. **Module Event Listeners**: Should check `isInboxReplay()` to avoid duplicate processing

### The Issue
Event listeners in modules were not checking the `CorrelationContext.isInboxReplay()` flag, causing:
- Events to be processed both during initial dispatch AND during inbox replay
- Double processing of the same logical event
- Data inconsistencies and duplicate records

## Generic Solution

### 1. Add Inbox Replay Check to Event Listeners

All integration event listeners should check the inbox replay flag and skip processing entirely:

```java
@EventListener
public void handleIntegrationEvent(SomeIntegrationEvent event) {
    // Skip processing entirely if this is an inbox replay
    // Events are processed once during initial dispatch, inbox replay is for reliability only
    if (CorrelationContext.isInboxReplay()) {
        log.info("Skipping inbox replay for event: {}", event.getEventId());
        return;
    }
    
    // Normal event processing with idempotency checks...
    if (alreadyProcessed(event)) {
        log.info("Event already processed: {}", event.getEventId());
        return;
    }
    
    // Process event...
}
```

### 2. Implement Proper Idempotency Checks

Each module should implement idempotency checks appropriate to their domain:

**Risk Calculation Module:**
```java
// Check if portfolio analysis already exists for this batch
BatchId batchId = BatchId.of(event.getBatchId());
if (portfolioAnalysisRepository.findByBatchId(batchId.value()).isPresent()) {
    log.info("Batch {} already processed, skipping", event.getBatchId());
    return;
}
```

**Data Quality Module:**
```java
// Check if quality report already exists for this batch
BatchId batchId = new BatchId(event.getBatchId());
if (qualityReportRepository.existsByBatchId(batchId)) {
    log.info("Quality report already exists for batch: {}", event.getBatchId());
    return;
}
```

### 3. Remove Duplicate Event Listeners

Ensure each module has only one event listener per integration event type.

## Implementation Applied

### Data Quality Module
1. **Removed duplicate `QualityEventListener`** - This was causing the double processing
2. **Enhanced `BatchIngestedEventListener`** - Added proper inbox replay checking
3. **Verified idempotency checks** - The existing `existsByBatchId()` check is sufficient

### Risk Calculation Module
1. **Verified `BatchIngestedEventListener`** - Already has proper idempotency checks
2. **No changes needed** - This module was working correctly

## Verification

### Test the Fix
1. **Single Event Processing**: Each integration event should be processed exactly once
2. **Inbox Replay Handling**: Events replayed from inbox should respect idempotency
3. **No Duplicate Records**: Database should not contain duplicate processing results

### Monitoring
- Check logs for "Skipping inbox replay" messages
- Monitor database for duplicate records
- Verify event processing metrics show single processing per event

## Best Practices for Future Event Listeners

1. **Always check inbox replay first**: `if (CorrelationContext.isInboxReplay()) return;` - Skip entirely, no repository calls needed
2. **Implement idempotency for duplicates**: Check if the event has already been processed (for non-replay duplicates)
3. **Use unique component names**: Avoid bean name conflicts with `@Component("uniqueName")`
4. **Log replay skips**: Help with debugging and monitoring
5. **Test with inbox replay**: Ensure handlers work correctly during replay scenarios
6. **Optimize performance**: Scoped value check is faster than repository calls

## Related Files
- `regtech-core/domain/src/main/java/com/bcbs239/regtech/core/domain/context/CorrelationContext.java`
- `regtech-core/application/src/main/java/com/bcbs239/regtech/core/application/eventprocessing/IntegrationEventReceiver.java`
- `regtech-data-quality/application/src/main/java/com/bcbs239/regtech/dataquality/application/integration/BatchIngestedEventListener.java`
- `regtech-risk-calculation/application/src/main/java/com/bcbs239/regtech/riskcalculation/application/integration/BatchIngestedEventListener.java`

## Status
✅ **FIXED** - Removed duplicate event listener and verified proper inbox replay handling in both modules.