# Double Event Dispatch Fix

## Problem

When a user registration occurred, the system was creating **duplicate billing accounts** and **duplicate sagas**, causing:
- Two `PaymentVerificationSaga` instances for the same user
- Two billing accounts with different IDs
- Two subscriptions
- Duplicate invoice number errors (`INV-20251204-0001`)
- One saga succeeding, one failing

## Root Cause

The `UserRegisteredIntegrationEvent` was being processed **twice** due to two issues:

### Issue 1: Handlers Processing Outbox Replay Events
Events from outbox replay were being processed immediately by handlers instead of waiting for inbox replay.

1. **First processing** (virtual-77 thread): Direct event propagation from outbox replay
   - OutboxProcessor replays event from outbox with `isOutboxReplay=true`
   - Event published to CrossModuleEventBus
   - IntegrationEventReceiver saves to inbox
   - **Event immediately propagates to UserRegisteredEventHandler** ❌
   - Handler should skip but doesn't check the flag
   - Handler creates billing account, subscription, and starts saga

2. **Second processing** (billing-scheduler-1 thread): Inbox replay
   - ProcessInboxJob reads from inbox
   - Publishes event with `isInboxReplay=true`
   - **Event propagates to UserRegisteredEventHandler again** ❌
   - Handler creates another billing account, subscription, and starts another saga

### Issue 2: ScopedValue Context Not Cleared
The `OUTBOX_REPLAY` scoped value was not being explicitly cleared during inbox replay, causing `isOutboxReplay()` to return `true` even during inbox replay. This prevented handlers from processing events during inbox replay.

## Solution

Integration event handlers must **skip processing** when events come from outbox replay. Events should only be processed during inbox replay to ensure exactly-once semantics.

To avoid repetitive boilerplate code in every handler, we created a **base class** that implements the template method pattern to automatically handle the outbox replay check.

### Changes Made

#### 1. DomainEventBus - Clear Outbox Replay Flag
**File**: `regtech-core/infrastructure/src/main/java/com/bcbs239/regtech/core/infrastructure/eventprocessing/DomainEventBus.java`

Explicitly set `OUTBOX_REPLAY=false` when publishing from inbox to prevent context pollution:

```java
@Override
public void publishFromInbox(DomainEvent event) {
    java.lang.ScopedValue.where(CorrelationContext.INBOX_REPLAY, Boolean.TRUE)
           .where(CorrelationContext.OUTBOX_REPLAY, Boolean.FALSE) // Explicitly clear outbox replay flag
           .where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
           .where(CorrelationContext.CAUSATION_ID, event.getEventId())
           .run(() -> delegate.publishEvent(event));
}
```

#### 2. IntegrationEventReceiver Enhancement
**File**: `regtech-core/application/src/main/java/com/bcbs239/regtech/core/application/eventprocessing/IntegrationEventReceiver.java`

Added logging for `isOutboxReplay` flag to help diagnose the flow:

```java
boolean isOutboxReplay = CorrelationContext.isOutboxReplay();

logger.info("📨 IntegrationEventReceiver: event={}, isInboxReplay={}, isOutboxReplay={}", 
        event.getClass().getSimpleName(), isInboxReplay, isOutboxReplay);
```

#### 3. IntegrationEventHandler Base Class (NEW)
**File**: `regtech-core/application/src/main/java/com/bcbs239/regtech/core/application/eventprocessing/IntegrationEventHandler.java`

Created a base class that implements the template method pattern to automatically handle outbox replay checks:

```java
public abstract class IntegrationEventHandler<T extends IntegrationEvent> {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected void handleIntegrationEvent(T event, java.util.function.Consumer<T> handler) {
        boolean isOutboxReplay = CorrelationContext.isOutboxReplay();
        boolean isInboxReplay = CorrelationContext.isInboxReplay();
        
        logger.info("Received {} (isOutboxReplay={}, isInboxReplay={})", 
                event.getClass().getSimpleName(), isOutboxReplay, isInboxReplay);
        
        // Skip processing if from outbox replay - will be processed via inbox
        if (isOutboxReplay && !isInboxReplay) {
            logger.info("Skipping processing - event from outbox replay will be processed via inbox: {}", 
                    event.getClass().getSimpleName());
            return;
        }
        
        // Process the event
        handler.accept(event);
    }
}
```

#### 4. UserRegisteredEventHandler Refactoring
**File**: `regtech-billing/application/src/main/java/com/bcbs239/regtech/billing/application/integration/UserRegisteredEventHandler.java`

Refactored to extend the base class - no more manual checks:

```java
@Component("billingUserRegisteredEventHandler")
public class UserRegisteredEventHandler extends IntegrationEventHandler<BillingUserRegisteredEvent> {
    
    @EventListener
    public void handle(BillingUserRegisteredEvent event) {
        handleIntegrationEvent(event, this::processEvent);
    }
    
    private void processEvent(BillingUserRegisteredEvent event) {
        // Business logic here - no need to check isOutboxReplay
        // ...
    }
}
```

#### 5. BillingAccountActivatedEventHandler Refactoring
**File**: `regtech-iam/application/src/main/java/com/bcbs239/regtech/iam/application/integration/BillingAccountActivatedEventHandler.java`

Refactored to extend the base class:

```java
@Component("iamBillingAccountActivatedEventHandler")
public class BillingAccountActivatedEventHandler extends IntegrationEventHandler<BillingAccountActivatedEvent> {
    
    @EventListener
    public void handle(BillingAccountActivatedEvent event) {
        handleIntegrationEvent(event, this::processEvent);
    }
    
    private void processEvent(BillingAccountActivatedEvent event) {
        // Business logic here - no need to check isOutboxReplay
        // ...
    }
}
```

## Event Flow After Fix

### Correct Flow (Exactly-Once Processing)

1. **Outbox Replay**:
   - OutboxProcessor replays `UserRegisteredEvent` from outbox
   - Event published to CrossModuleEventBus with `isOutboxReplay=true`
   - IntegrationEventReceiver saves to inbox
   - UserRegisteredEventHandler **skips processing** ✅

2. **Inbox Replay** (later):
   - ProcessInboxJob reads from inbox
   - Publishes event with `isInboxReplay=true`
   - IntegrationEventReceiver **skips saving** (already in inbox)
   - UserRegisteredEventHandler **processes event** ✅
   - Creates billing account, subscription, starts saga **once**

## Testing

After applying this fix:

1. **Clear existing data**:
   ```sql
   DELETE FROM billing.billing_accounts WHERE user_id = '<test-user-id>';
   DELETE FROM billing.subscriptions WHERE billing_account_id IN (
       SELECT id FROM billing.billing_accounts WHERE user_id = '<test-user-id>'
   );
   DELETE FROM core.sagas WHERE saga_type = 'PaymentVerificationSaga';
   ```

2. **Register a new user** via POST `/api/v1/users/register`

3. **Verify logs show**:
   - Only ONE "BILLING_ACCOUNT_CREATED" message
   - Only ONE "SAGA_START_SUCCESS" message
   - "OUTBOX_REPLAY_SKIPPED" message during outbox replay
   - No duplicate invoice number errors

4. **Verify database**:
   ```sql
   SELECT COUNT(*) FROM billing.billing_accounts WHERE user_id = '<test-user-id>';
   -- Should return 1
   
   SELECT COUNT(*) FROM core.sagas WHERE saga_type = 'PaymentVerificationSaga' 
       AND saga_data::text LIKE '%<test-user-id>%';
   -- Should return 1
   ```

## Pattern for Other Handlers

All integration event handlers that process cross-module events should extend the `IntegrationEventHandler` base class:

```java
@Component
public class SomeEventHandler extends IntegrationEventHandler<SomeIntegrationEvent> {
    
    @EventListener
    public void handle(SomeIntegrationEvent event) {
        handleIntegrationEvent(event, this::processEvent);
    }
    
    private void processEvent(SomeIntegrationEvent event) {
        // Your business logic here
        // No need to check isOutboxReplay - handled by base class
    }
}
```

### Benefits of Base Class Approach

1. **No Repetitive Code**: Outbox replay check is centralized in one place
2. **Consistent Behavior**: All handlers follow the same pattern
3. **Better Logging**: Base class provides consistent logging for all events
4. **Easier to Maintain**: Changes to the replay logic only need to be made in one place
5. **Type Safety**: Generic type parameter ensures type-safe event handling

## Related Files

- `regtech-core/application/src/main/java/com/bcbs239/regtech/core/application/outbox/OutboxProcessor.java`
- `regtech-core/application/src/main/java/com/bcbs239/regtech/core/application/inbox/ProcessInboxJob.java`
- `regtech-core/domain/src/main/java/com/bcbs239/regtech/core/domain/context/CorrelationContext.java`

## Date
December 4, 2025
