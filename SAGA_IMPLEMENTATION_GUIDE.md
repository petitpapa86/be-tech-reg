# Payment Verification Saga - Implementation Guide

## Table of Contents
1. [Overview](#overview)
2. [Saga Architecture](#saga-architecture)
3. [How the Saga Works](#how-the-saga-works)
4. [Problems Encountered & Solutions](#problems-encountered--solutions)
5. [Compensation Strategy](#compensation-strategy)
6. [Cross-Module Integration](#cross-module-integration)
7. [Best Practices & Lessons Learned](#best-practices--lessons-learned)

---

## Overview

The **PaymentVerificationSaga** orchestrates the complex process of setting up a user's billing subscription, involving multiple steps across Stripe integration, billing account creation, and subscription management. It implements the Saga pattern to ensure data consistency and automatic rollback in case of failures.

### Business Process
```
User Registers → Create Stripe Customer → Create Subscription → 
Create Invoice → Process Payment → Finalize Billing Account → 
Notify IAM Module → Complete
```

### Key Characteristics
- **Long-Running Transaction**: Spans multiple microservices and external APIs (Stripe)
- **Eventual Consistency**: Uses event-driven coordination
- **Automatic Compensation**: Rolls back on failure with automated cleanup
- **Cross-Module Communication**: Publishes integration events to IAM module

---

## Saga Architecture

### Components

```
┌─────────────────────────────────────────────────────────────────┐
│                   PaymentVerificationSaga                        │
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌─────────────────┐  │
│  │  Saga State  │    │   Commands   │    │     Events      │  │
│  │   (Data)     │    │  Dispatcher  │    │   Processor     │  │
│  └──────────────┘    └──────────────┘    └─────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            Compensation Handlers                          │  │
│  │  (RefundPayment, VoidInvoice, CancelSubscription, etc.)  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
         │                    │                    │
         ↓                    ↓                    ↓
   Stripe API        Billing Database      IAM Module
```

### Dependencies Injected
1. **SagaId**: Unique identifier for saga instance
2. **PaymentVerificationSagaData**: Saga state (mutable)
3. **TimeoutScheduler**: Handles timeout scenarios
4. **ILogger**: Structured logging
5. **ApplicationEventPublisher**: Internal event publishing (compensation)
6. **IIntegrationEventBus**: Cross-module event publishing (IAM notification)

---

## How the Saga Works

### Step-by-Step Execution Flow

#### 1. Saga Initialization (SagaStartedEvent)
```java
handleSagaStarted(SagaStartedEvent event)
```
**Actions**:
- Dispatches `CreateStripeCustomerCommand`
- Schedules payment timeout (SLA-based)

**State Updated**: None yet

---

#### 2. Customer Created (StripeCustomerCreatedEvent)
```java
handleStripeCustomerCreated(StripeCustomerCreatedEvent event)
```
**Actions**:
- Stores `stripeCustomerId` and `billingAccountId` in saga data
- Dispatches `CreateStripeSubscriptionCommand`

**State Updated**:
```java
data.setStripeCustomerId(event.getStripeCustomerId());
data.setBillingAccountId(event.getBillingAccountId());
```

**Status Check**: Calls `updateStatus()` → Still in progress

---

#### 3. Subscription Created (StripeSubscriptionCreatedEvent)
```java
handleStripeSubscriptionCreated(StripeSubscriptionCreatedEvent event)
```
**Actions**:
- Stores `stripeSubscriptionId` and `stripeInvoiceId`
- Checks if subscription entity exists locally
- If exists: Dispatches `CreateStripeInvoiceCommand`
- If not: Re-dispatches `CreateStripeSubscriptionCommand` to create local entity

**State Updated**:
```java
data.setStripeSubscriptionId(event.getStripeSubscriptionId());
data.setStripeInvoiceId(event.getStripeInvoiceId());
data.setSubscriptionId(event.getSubscriptionId());
```

**Status Check**: Calls `updateStatus()` → Still in progress

---

#### 4. Invoice Created (StripeInvoiceCreatedEvent)
```java
handleStripeInvoiceCreated(StripeInvoiceCreatedEvent event)
```
**Actions**:
- Stores `stripeInvoiceId`
- Dispatches `FinalizeBillingAccountCommand`
- **Does NOT call updateStatus()** - waits for payment webhook

**State Updated**:
```java
data.setStripeInvoiceId(event.getStripeInvoiceId());
```

**Why no updateStatus()?**
To prevent race condition where saga completes before finalization command executes.

---

#### 5. Payment Succeeded (StripePaymentSucceededEvent)
```java
handleStripePaymentSucceeded(StripePaymentSucceededEvent event)
```
**Actions**:
- Stores `stripePaymentIntentId`
- Cancels payment timeout
- Dispatches `FinalizeBillingAccountCommand` (redundant safety)
- **Calls updateStatus()** - triggers completion

**State Updated**:
```java
data.setStripePaymentIntentId(event.getStripePaymentIntentId());
```

**Status Check**: Calls `updateStatus()` → **COMPLETED** (all IDs present)

---

#### 6. Saga Completion (updateStatus)
```java
protected void updateStatus()
```
**Completion Criteria** (ALL must be true):
```java
data.getUserId() != null &&
data.getStripeCustomerId() != null &&
data.getStripeSubscriptionId() != null &&
data.getStripeInvoiceId() != null &&
data.getStripePaymentIntentId() != null
```

**Actions on Completion**:
1. Sets status to `COMPLETED`
2. Sets completion timestamp
3. **Publishes BillingAccountActivatedEvent** → Notifies IAM module

---

### Failure Paths

#### 6a. Customer Creation Failed (StripeCustomerCreationFailedEvent)
```java
handleStripeCustomerCreationFailed(StripeCustomerCreationFailedEvent event)
```
**Actions**:
- Stores failure reason
- Cancels payment timeout
- Sets status to `FAILED`
- **Triggers compensation** (cleanup)

#### 6b. Payment Failed (StripePaymentFailedEvent)
```java
handleStripePaymentFailed(StripePaymentFailedEvent event)
```
**Actions**:
- Stores failure reason
- Sets status to `FAILED`
- **Triggers compensation** (rollback)

#### 6c. Payment Timeout (handlePaymentTimeout)
```java
handlePaymentTimeout()
```
**Actions**:
- Calls `fail()` with timeout reason
- `fail()` automatically triggers `compensate()`

---

## Problems Encountered & Solutions

### Problem 1: Race Condition Between Saga Completion and Finalization

#### Issue
```
Timeline:
1. Saga receives StripePaymentSucceededEvent
2. Saga calls updateStatus() → marks COMPLETED
3. Saga completes and persists to database
4. FinalizeBillingAccountCommand tries to execute
5. ❌ Saga not found - already completed!
```

**Symptom**: `SagaNotFoundException: Could not find active saga with id: ...`

#### Root Cause
The saga was completing **before** the `FinalizeBillingAccountCommand` had time to execute. The saga lifecycle was:
1. Dispatch command
2. Mark as completed immediately
3. Save to database (saga state = COMPLETED)
4. Command handler tries to find saga
5. Saga already completed → Not found

#### Solution
**Delayed Completion**: Don't call `updateStatus()` in `handleStripeInvoiceCreated()`

```java
private void handleStripeInvoiceCreated(StripeInvoiceCreatedEvent event) {
    data.setStripeInvoiceId(event.getStripeInvoiceId());
    
    dispatchCommand(new FinalizeBillingAccountCommand(...));
    
    // ❌ OLD: updateStatus(); // Would complete saga too early
    // ✅ NEW: Wait for payment webhook to complete saga
}
```

Wait for **StripePaymentSucceededEvent** webhook to arrive, which ensures:
1. Invoice finalization command has time to execute
2. Payment is confirmed by Stripe
3. Saga completes only after full payment verification

---

### Problem 2: Compensation Not Triggering on Failure

#### Issue
When saga failed (timeout, payment error, etc.), the `compensate()` method was never called automatically.

**Symptom**: Failed sagas left orphaned resources (Stripe subscriptions, customers) without cleanup.

#### Root Cause
The base `AbstractSaga.fail(String reason)` method only set the status to FAILED but didn't trigger compensation:

```java
// OLD Implementation
protected void fail(String reason) {
    setStatus(SagaStatus.FAILED);
    setCompletedAt(Instant.now());
    // Missing: compensate() call!
}
```

#### Solution
**Automatic Compensation Trigger**: Update `AbstractSaga.fail()` to call `compensate()`

```java
// NEW Implementation
protected void fail(String reason) {
    setStatus(SagaStatus.FAILED);
    setCompletedAt(Instant.now());
    compensate(); // ✅ Automatically trigger compensation
}
```

Both failure methods now trigger compensation:
- `fail(String reason)` → calls `compensate()`
- `completeWithFailure(SagaError error)` → calls `compensate()`

---

### Problem 3: Manual Compensation Implementation

#### Issue
Original compensation was **procedural and manual**:
```java
// OLD Approach (Anti-pattern)
protected void compensate() {
    if (data.getStripePaymentIntentId() != null) {
        // Manual Stripe API call
        stripeService.refundPayment(data.getStripePaymentIntentId());
    }
    if (data.getStripeSubscriptionId() != null) {
        stripeService.cancelSubscription(...);
    }
    // ... more manual cleanup
}
```

**Problems**:
- Tight coupling to infrastructure (Stripe service)
- Blocking/synchronous operations
- Poor testability
- No retry mechanism
- No idempotency

#### Solution
**Event-Driven Compensation**: Publish compensation events, let handlers execute asynchronously

```java
// NEW Approach (Event-Driven)
protected void compensate() {
    // 1. Refund payment if paid
    if (data.getStripePaymentIntentId() != null) {
        RefundPaymentEvent event = new RefundPaymentEvent(
            getId().id(),
            data.getStripePaymentIntentId(),
            data.getUserId(),
            failureReason
        );
        eventPublisher.publishEvent(event); // ✅ Async handling
    }
    
    // 2. Void invoice if created
    if (data.getStripeInvoiceId() != null) {
        VoidInvoiceEvent event = new VoidInvoiceEvent(...);
        eventPublisher.publishEvent(event);
    }
    
    // 3. Cancel subscription
    // 4. Suspend billing account
    // 5. Notify user
}
```

**Benefits**:
- ✅ **Decoupled**: Saga doesn't know about Stripe implementation
- ✅ **Asynchronous**: Non-blocking compensation
- ✅ **Testable**: Easy to mock event publisher
- ✅ **Scalable**: Can add new compensation handlers without changing saga
- ✅ **Resilient**: Handlers can implement retry logic independently

---

### Problem 4: SagaManager Doesn't Support 6-Parameter Constructor

#### Issue
After adding `IIntegrationEventBus` as 6th parameter to `PaymentVerificationSaga`, the application failed to compile:

```
NoSuchMethodException: Could not find saga constructor with expected signature
```

#### Root Cause
`SagaManager.createSagaInstance()` only supported:
- 4-param constructor: (SagaId, Data, TimeoutScheduler, ILogger)
- 5-param constructor: + ApplicationEventPublisher

But `PaymentVerificationSaga` now requires:
- 6-param constructor: + IIntegrationEventBus

#### Solution
**Extended Constructor Discovery**: Update SagaManager to try 6-param first

```java
// In SagaManager
private <T> AbstractSaga<T> createSagaInstance(...) {
    try {
        // ✅ Try 6-param first (latest)
        Constructor<?> constructor = sagaClass.getDeclaredConstructor(
            SagaId.class, data.getClass(), TimeoutScheduler.class, 
            ILogger.class, ApplicationEventPublisher.class, IIntegrationEventBus.class
        );
        return (AbstractSaga<T>) constructor.newInstance(
            sagaId, data, timeoutScheduler, logger, 
            eventPublisher, integrationEventBus
        );
    } catch (NoSuchMethodException e) {
        // Fallback to 5-param
        try {
            Constructor<?> constructor = sagaClass.getDeclaredConstructor(...);
            return (AbstractSaga<T>) constructor.newInstance(...);
        } catch (NoSuchMethodException e2) {
            // Fallback to 4-param
            Constructor<?> constructor = sagaClass.getDeclaredConstructor(...);
            return (AbstractSaga<T>) constructor.newInstance(...);
        }
    }
}
```

**Updates Required**:
1. Add `IIntegrationEventBus` field to SagaManager
2. Update `createSagaInstance()` - 6/5/4 param fallback
3. Update `reconstructSaga()` - 6/5/4 param fallback
4. Update `discoverSagaDataClass()` - recognize 6-param pattern

---

## Compensation Strategy

### Compensation Order
Compensation follows the **reverse order of creation** to safely clean up resources:

```
Creation Order:           Compensation Order:
1. Stripe Customer    →   5. (Keep customer for retry)
2. Stripe Subscription→   4. Cancel Subscription
3. Stripe Invoice     →   3. Void Invoice (if unpaid)
4. Payment Intent     →   2. Refund Payment (if paid)
5. Billing Account    →   1. Suspend Billing Account
```

### Compensation Events

#### 1. RefundPaymentEvent
**When**: Payment was successfully processed
**Handler**: `RefundPaymentEventHandler`
**Action**: 
- Calls `PaymentService.refundPayment()`
- Stripe API: `refund.create(payment_intent_id)`
- Logs refund result

#### 2. VoidInvoiceEvent
**When**: Invoice created but not paid
**Handler**: `VoidInvoiceEventHandler`
**Action**:
- Calls `PaymentService.voidInvoice()`
- Stripe API: `invoice.voidInvoice(invoice_id)`
- Logs void result

#### 3. CancelSubscriptionEvent
**When**: Subscription was created
**Handler**: `CancelSubscriptionEventHandler`
**Action**:
- Calls `SubscriptionService.cancelSubscription()`
- Stripe API: `subscription.cancel(subscription_id)`
- Updates local subscription status

#### 4. SuspendBillingAccountEvent
**When**: Billing account was created
**Handler**: `SuspendBillingAccountEventHandler`
**Action**:
- Calls `BillingAccountService.suspend()`
- Sets account status to SUSPENDED
- Prevents future billing

#### 5. NotifyUserEvent
**When**: Always (user notification)
**Handler**: `NotifyUserEventHandler`
**Action**:
- Builds user-friendly message
- Sends email/notification
- Includes saga ID for support reference

### Compensation Message Example

```
Subject: Subscription Setup Issue

We encountered an issue setting up your subscription.

Your payment has been fully refunded and should appear in your 
account within 5-10 business days.

You can try again by visiting your account settings.

If you continue to experience issues, please contact our support 
team with reference: saga-12345-abc-def

Technical details: Payment timeout after 30 minutes
```

---

## Cross-Module Integration

### Integration Event: BillingAccountActivatedEvent

When billing completes successfully, the saga publishes an integration event to notify the IAM module:

```java
private void publishBillingActivatedEvent() {
    try {
        // Reconstruct value objects from saga data
        UserId userId = new UserId(UUID.fromString(data.getUserId()));
        BillingAccountId billingAccountId = 
            new BillingAccountId(data.getBillingAccountId());
        SubscriptionTier subscriptionTier = SubscriptionTier.STARTER;
        
        BillingAccountActivatedEvent event = new BillingAccountActivatedEvent(
            userId,
            billingAccountId,
            subscriptionTier,
            Instant.now(),
            getId().id() // correlation ID
        );
        
        integrationEventBus.publish(event);
        
        logger.asyncStructuredLog("BILLING_ACTIVATED_EVENT_PUBLISHED", ...);
    } catch (Exception e) {
        logger.asyncStructuredErrorLog("BILLING_ACTIVATED_EVENT_PUBLICATION_FAILED", ...);
    }
}
```

### Cross-Module Flow

```
┌─────────────────────────────────────────────────────────┐
│                  Billing Module                          │
│                                                          │
│  PaymentVerificationSaga                                 │
│    → updateStatus() → COMPLETED                         │
│    → publishBillingActivatedEvent()                     │
│    → integrationEventBus.publish(event)                 │
└────────────────────┬─────────────────────────────────────┘
                     │
                     │ IIntegrationEventBus
                     │ (CrossModuleEventBus)
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│                   IAM Module                             │
│                                                          │
│  @EventListener(BillingAccountActivatedEvent.class)     │
│    → BillingAccountActivatedEventHandler                │
│    → Find User by userId                                │
│    → user.activateAccount()                             │
│    → userRepository.save(user)                          │
│    → Log activation                                     │
└─────────────────────────────────────────────────────────┘
```

### Why Integration Events?

**Domain Events** (internal):
- Published via `ApplicationEventPublisher`
- Consumed within same bounded context (Billing module)
- Examples: `StripeCustomerCreatedEvent`, `StripePaymentSucceededEvent`

**Integration Events** (cross-module):
- Published via `IIntegrationEventBus`
- Consumed across bounded contexts (Billing → IAM)
- Examples: `BillingAccountActivatedEvent`
- Ensures loose coupling between modules

---

## Best Practices & Lessons Learned

### 1. Saga State Management

✅ **DO**: Store minimal state in saga data
```java
// Only store IDs and essential fields
data.setStripeCustomerId("cus_123");
data.setStripeSubscriptionId("sub_456");
data.setStripePaymentIntentId("pi_789");
```

❌ **DON'T**: Store entire domain entities
```java
// Anti-pattern: storing full objects
data.setCustomer(customer); // Too much data
data.setSubscription(subscription); // Serialization issues
```

### 2. Event Ordering

✅ **DO**: Design for eventual consistency
- Events may arrive out of order
- Use idempotent handlers
- Check for null values before proceeding

❌ **DON'T**: Assume strict event ordering
```java
// Anti-pattern: assuming invoice event comes after subscription
handleStripeInvoiceCreated() {
    // ❌ Assumes subscription already exists
    var subscription = data.getSubscription();
}
```

### 3. Compensation Idempotency

✅ **DO**: Make compensation operations idempotent
```java
// Handlers check if operation already completed
if (payment.status == PaymentStatus.REFUNDED) {
    logger.info("Payment already refunded, skipping");
    return;
}
```

❌ **DON'T**: Execute compensation multiple times
```java
// Anti-pattern: no idempotency check
stripeService.refundPayment(paymentIntentId); // May fail on retry
```

### 4. Logging Strategy

✅ **DO**: Use structured logging with context
```java
logger.asyncStructuredLog("SAGA_STEP_COMPLETED", Map.of(
    "sagaId", getId().id(),
    "step", "StripeCustomerCreated",
    "customerId", data.getStripeCustomerId(),
    "userId", data.getUserId()
));
```

❌ **DON'T**: Use plain string logging
```java
// Anti-pattern: hard to query/analyze
logger.info("Customer created: " + customerId);
```

### 5. Timeout Handling

✅ **DO**: Set reasonable timeouts and clean up
```java
// Schedule timeout
timeoutScheduler.schedule(
    getId().id() + "-payment-timeout",
    PAYMENT_TIMEOUT_SLA.toMillis(),
    this::handlePaymentTimeout
);

// Cancel on success
timeoutScheduler.cancel(getId().id() + "-payment-timeout");
```

❌ **DON'T**: Let sagas hang indefinitely
```java
// Anti-pattern: no timeout handling
// Saga waits forever for payment webhook
```

### 6. Error Handling

✅ **DO**: Wrap risky operations in try-catch
```java
private void publishBillingActivatedEvent() {
    try {
        integrationEventBus.publish(event);
        logger.asyncStructuredLog("EVENT_PUBLISHED", ...);
    } catch (Exception e) {
        logger.asyncStructuredErrorLog("EVENT_PUBLICATION_FAILED", e, ...);
        // Don't re-throw - event publication is non-critical
    }
}
```

❌ **DON'T**: Let exceptions bubble up unhandled
```java
// Anti-pattern: unhandled exception kills saga
integrationEventBus.publish(event); // May throw
```

### 7. Testing Strategy

**Unit Tests**:
- Test individual event handlers
- Mock command dispatcher and event publisher
- Verify state transitions

**Integration Tests**:
- Test full saga flow end-to-end
- Use test containers for database
- Mock Stripe API calls

**Compensation Tests**:
- Trigger failures at each step
- Verify compensation events published
- Check cleanup handlers execute correctly

---

## Saga Execution Timeline (Happy Path)

```
Time  │ Event                              │ Saga State
──────┼────────────────────────────────────┼─────────────────────────────
0ms   │ SagaStartedEvent                   │ STARTED
      │   → CreateStripeCustomerCommand    │
      │   → Schedule timeout (30min)       │
──────┼────────────────────────────────────┼─────────────────────────────
500ms │ StripeCustomerCreatedEvent         │ IN_PROGRESS
      │   → Store customerId               │ customerId: cus_123
      │   → CreateStripeSubscriptionCmd    │ billingAccountId: acc_456
──────┼────────────────────────────────────┼─────────────────────────────
1.2s  │ StripeSubscriptionCreatedEvent     │ IN_PROGRESS
      │   → Store subscriptionId           │ subscriptionId: sub_789
      │   → CreateStripeInvoiceCommand     │
──────┼────────────────────────────────────┼─────────────────────────────
1.8s  │ StripeInvoiceCreatedEvent          │ IN_PROGRESS
      │   → Store invoiceId                │ invoiceId: inv_101
      │   → FinalizeBillingAccountCmd      │
      │   → NO updateStatus() call         │ (Wait for payment)
──────┼────────────────────────────────────┼─────────────────────────────
5.3s  │ StripePaymentSucceededEvent        │ IN_PROGRESS
      │   → Store paymentIntentId          │ paymentIntentId: pi_202
      │   → Cancel timeout                 │
      │   → updateStatus() called          │
      │   → All IDs present = COMPLETED    │ ✅ COMPLETED
      │   → publishBillingActivatedEvent() │
      │   → IIntegrationEventBus.publish() │
──────┼────────────────────────────────────┼─────────────────────────────
5.4s  │ BillingAccountActivatedEvent       │ COMPLETED
      │   → Received by IAM module         │ (Cross-module)
      │   → User account activated         │
──────┴────────────────────────────────────┴─────────────────────────────
```

---

## Summary

### Key Achievements

✅ **Saga Pattern Implementation**: Complex multi-step transaction coordination  
✅ **Automatic Compensation**: Event-driven rollback on failure  
✅ **Race Condition Fix**: Delayed completion prevents command execution issues  
✅ **Cross-Module Integration**: Billing → IAM communication via integration events  
✅ **Stripe Integration**: Full payment lifecycle management  
✅ **Timeout Handling**: SLA-based payment timeout with cleanup  
✅ **Structured Logging**: Complete audit trail for debugging  
✅ **Testability**: Decoupled design enables easy testing  

### Architecture Principles Applied

1. **Event-Driven Architecture**: Events for coordination and compensation
2. **Eventual Consistency**: Distributed transaction management
3. **Bounded Context Isolation**: Billing and IAM remain separate
4. **Dependency Inversion**: Sagas depend on interfaces, not implementations
5. **Single Responsibility**: Each handler focuses on one compensation action
6. **Open/Closed Principle**: Easy to add new compensation handlers

---

**Document Version**: 1.0  
**Last Updated**: 2025-11-11  
**Authors**: Backend Team  
**Status**: Production Ready ✅
