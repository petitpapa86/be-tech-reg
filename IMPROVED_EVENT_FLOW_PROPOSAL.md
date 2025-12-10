# Event Flow Architecture - Best Practices Guide

## Architecture Principle (CORRECT ✅)

**Maintain in each module:**
- **Internal events** for domain logic within the module
- **Integration events** for cross-module communication only

This follows Domain-Driven Design and Bounded Context best practices.

## Current Architecture (CORRECT)

```
Ingestion Module:
  - Internal: BatchCompletedEvent (domain event)
  - External: BatchCompletedIntegrationEvent (for cross-module)

Risk Calculation Module:
  - External: BatchCompletedIntegrationEvent (receives from ingestion)
  - Internal: BatchIngestedEvent (domain event for risk-calc)
  - Processing: Command/Handler pattern

Data Quality Module:
  - External: BatchCompletedIntegrationEvent (receives from ingestion)
  - Internal: BatchIngestedEvent (domain event for data-quality)
  - Processing: Command/Handler pattern
```

**Why This Design is Good:**
- ✅ **Bounded Context Isolation**: Each module maintains its own domain language
- ✅ **Translation Layer**: Adapters convert external contracts to internal events
- ✅ **Flexibility**: Internal events can change without affecting other modules
- ✅ **Testability**: Can test internal event handlers independently
- ✅ **No Direct Dependencies**: Modules don't depend on each other's domains

## The Real Issue (Now Fixed)

The architecture was correct. The problem was the **Adapter missing inbox replay check**:

### Before Fix (Had Duplicate Events ❌)

```java
// BatchCompletedIntegrationAdapter (BEFORE - Missing check)
@EventListener
public void onBatchCompletedIntegrationEvent(BatchCompletedIntegrationEvent integrationEvent) {
    // ❌ NO CHECK FOR INBOX REPLAY!
    // This runs TWICE: once on initial publish, once on inbox replay
    
    BatchIngestedEvent internalEvent = new BatchIngestedEvent(...);
    domainEventBus.publishAsReplay(internalEvent);
}
```

**Result:** Two `BatchIngestedEvent` instances created → Race condition → Duplicate key error

### After Fix (Correct ✅)

```java
// BatchCompletedIntegrationAdapter (AFTER - With check)
@EventListener
public void onBatchCompletedIntegrationEvent(BatchCompletedIntegrationEvent integrationEvent) {
    // ✅ CHECK FOR INBOX REPLAY
    if (CorrelationContext.isInboxReplay()) {
        log.info("Skipping inbox replay for batch: {}", integrationEvent.getBatchId());
        return;  // Don't process twice!
    }
    
    // Convert external integration event → internal domain event
    BatchIngestedEvent internalEvent = new BatchIngestedEvent(...);
    domainEventBus.publishAsReplay(internalEvent);
}
```

**Result:** Only one `BatchIngestedEvent` created → No race condition → No duplicates

## Complete Fixed Flow

```
┌──────────────────────────────────────────────────────────┐
│ Ingestion Module                                         │
├──────────────────────────────────────────────────────────┤
│ BatchCompletedEvent (domain)                             │
│   ↓ @TransactionalEventListener                          │
│ BatchCompletedIntegrationEvent → Outbox → CrossModule    │
└──────────────────────────────────────────────────────────┘
                    ↓
        ┌───────────┴───────────┐
        ↓ (initial)             ↓ (replay @ 10s)
┌──────────────────────────────────────────────────────────┐
│ Risk Calculation Module                                  │
├──────────────────────────────────────────────────────────┤
│ IntegrationEventReceiver → Inbox                         │
│   ↓                                                       │
│ BatchCompletedIntegrationAdapter                         │
│   ├─ if (isInboxReplay) return; ✅ SKIP REPLAY          │
│   └─ else: convert to BatchIngestedEvent                 │
│        ↓                                                  │
│ BatchIngestedEventListener                               │
│   ├─ if (isInboxReplay) return; ✅ DOUBLE CHECK         │
│   ├─ if (alreadyProcessed) return; ✅ IDEMPOTENCY        │
│   └─ else: process                                       │
│        ↓                                                  │
│ CalculateRiskMetricsCommandHandler                       │
│   ├─ if (batchExists) return; ✅ COMMAND IDEMPOTENCY    │
│   └─ else: create batch + calculate                      │
│        ↓                                                  │
│ BatchRepository                                          │
│   └─ catch DuplicateKeyException → success ✅ LAST LINE │
└──────────────────────────────────────────────────────────┘
```

## Fixes Applied (Multi-Layer Defense)

### Layer 1: Adapter - Skip Inbox Replay ✅
**File:** `BatchCompletedIntegrationAdapter.java`

```java
@EventListener
public void onBatchCompletedIntegrationEvent(BatchCompletedIntegrationEvent integrationEvent) {
    // ✅ PRIMARY FIX - Skip inbox replay
    if (CorrelationContext.isInboxReplay()) {
        log.info("Skipping inbox replay for batch: {}", integrationEvent.getBatchId());
        return;
    }
    
    // Convert integration event → internal domain event
    BatchIngestedEvent internalEvent = new BatchIngestedEvent(...);
    domainEventBus.publishAsReplay(internalEvent);
}
```

### Layer 2: Command Handler - Check Before Processing ✅
**File:** `CalculateRiskMetricsCommandHandler.java`

```java
@Transactional
public Result<Void> handle(CalculateRiskMetricsCommand command) {
    String batchId = command.getBatchId();
    
    // ✅ IDEMPOTENCY CHECK - Skip if batch already exists
    Maybe<Batch> existingBatch = batchRepository.findById(BatchId.of(batchId));
    if (existingBatch.isPresent()) {
        log.info("Batch {} already exists, skipping duplicate calculation", batchId);
        return Result.success();
    }
    
    // Continue with processing...
}
```

### Layer 3: Repository - Handle Database Constraint ✅
**File:** `JpaBatchRepository.java`

```java
@Transactional
public Result<Void> save(Batch batch) {
    try {
        // Find or create entity
        BatchEntity entity = springDataRepository.findById(batch.getId().value())
            .orElseGet(() -> new BatchEntity());
        
        batch.populateEntity(entity);
        springDataRepository.save(entity);
        
        return Result.success();
        
    } catch (DataIntegrityViolationException e) {
        // ✅ GRACEFUL HANDLING - Treat duplicate key as success (idempotent)
        if (e.getMessage() != null && e.getMessage().contains("batches_pkey")) {
            log.warn("Batch {} already exists (duplicate key), treating as idempotent success", 
                     batch.getId().value());
            return Result.success();
        }
        throw e;
    }
}
```

## Why This Maintains Your Architecture Principle

**Your Principle:** Each module should have internal events + integration events

**Fixed Implementation Preserves This:**

```
✅ CORRECT: Maintains Bounded Context

Ingestion Module:
  - Internal: BatchCompletedEvent
  - External: BatchCompletedIntegrationEvent

Risk Calculation Module:
  - Adapter converts: BatchCompletedIntegrationEvent → BatchIngestedEvent
  - Internal: BatchIngestedEvent (risk-calc domain)
  - Listener processes: BatchIngestedEvent
  - No direct dependency on ingestion domain

Translation happens at boundary (Adapter)
Each module speaks its own language
```

**What Would VIOLATE Your Principle:**

```
❌ WRONG: Breaks Bounded Context

Risk Calculation Module:
  - Listener processes: BatchCompletedIntegrationEvent (external event)
  - No internal event
  - Direct dependency on ingestion's integration contract
  
No translation layer
Module couples to external events
```

## Benefits of Current Architecture (With Fixes)

### Bounded Context Isolation
- ✅ **Each module owns its domain**: Risk-calc has `BatchIngestedEvent`, not coupled to ingestion
- ✅ **Translation at boundaries**: Adapter converts external → internal events
- ✅ **Independent evolution**: Can change internal events without affecting other modules

### Reliability
- ✅ **Inbox/Outbox pattern**: Guaranteed delivery and processing
- ✅ **Multi-layer idempotency**: Prevents duplicates at adapter, command, and repository levels
- ✅ **Graceful failure handling**: Database constraints act as last line of defense

### Maintainability
- ✅ **Clear separation of concerns**: Adapter, Listener, Command, Repository each have one job
- ✅ **Testability**: Can test internal event handlers without external dependencies
- ✅ **Domain-driven design**: Follows DDD patterns and bounded context principles

## Future Optimization Options (Optional)

### Option 1: Smarter Inbox Replay (Selective)

Only replay events that actually failed:

```java
@Entity
@Table(name = "inbox_messages")
public class InboxMessageEntity {
    
    @Column(name = "replay_required", nullable = false)
    private boolean replayRequired = false;  // Default: no replay needed
    
    // Set to true ONLY if initial processing failed
}

// ProcessInboxJob
@Scheduled(fixedDelayString = "#{@inboxOptions.getPollInterval().toMillis()}")
public void processInboxMessages() {
    // Only replay events marked as needing retry
    List<InboxMessage> toRetry = inboxRepository
        .findByProcessingStatusAndReplayRequired(InboxMessageStatus.PENDING, true);
    
    // Don't replay events that succeeded on first try
}
```

**Benefit:** Reduces unnecessary inbox processing overhead

### Option 2: Mark as Processed Immediately

Update inbox status as soon as initial processing succeeds:

```java
@Component
public class IntegrationEventReceiver {
    
    @EventListener
    @Transactional
    public void onIntegrationEvent(IntegrationEvent event) {
        if (CorrelationContext.isInboxReplay()) {
            return;
        }
        
        InboxMessage entry = InboxMessage.fromIntegrationEvent(event, mapper);
        entry.markAsProcessing(); // Optimistic: assume will succeed
        inboxRepository.save(entry);
        
        // After successful handler execution:
        entry.markAsProcessed();
        inboxRepository.save(entry);
    }
}
```

**Benefit:** Reduces inbox table size, faster queries

## Checklist: Apply to Other Modules

The same pattern should be applied to all modules:

- [x] **Risk Calculation Module** - Fixed ✅
  - `BatchCompletedIntegrationAdapter` - Added inbox replay check
  - `CalculateRiskMetricsCommandHandler` - Added idempotency check
  - `JpaBatchRepository` - Handles duplicate key gracefully

- [ ] **Data Quality Module** - TODO
  - `BatchCompletedIntegrationAdapter` - Add inbox replay check
  - Command handlers - Add idempotency checks
  - Repositories - Handle duplicate keys

- [ ] **Report Generation Module** - TODO
  - Integration adapters - Add inbox replay check
  - Command handlers - Add idempotency checks
  - Repositories - Handle duplicate keys

- [ ] **Billing Module** - TODO
  - Integration adapters - Add inbox replay check
  - Command handlers - Add idempotency checks
  - Repositories - Handle duplicate keys

## Summary

**Your Architecture Principle is CORRECT ✅**

"Maintain in each module only internal events, and integration events only for module-to-module communication"

**The Fix Preserves Your Architecture**

The issue wasn't the design - it was just a missing inbox replay check in one place. The fixes maintain your bounded context isolation while preventing duplicate processing.

**What Was Fixed:**
1. ✅ Adapter checks for inbox replay before converting events
2. ✅ Command handler checks for existing batch before processing
3. ✅ Repository gracefully handles duplicate key constraints

**What Was NOT Changed:**
- ✅ Still using adapter pattern (external → internal event conversion)
- ✅ Still using internal domain events per module
- ✅ Still using inbox/outbox for reliability
- ✅ Still maintaining bounded context isolation

**Result:** Clean architecture + No duplicates