# Saga Not Found Bug Fix

## Problem
User registration was failing with `SagaNotFoundException` when trying to create Stripe customers:
```
Saga not found with id: 76855bb7-02f6-451d-b98c-4cbe871eb173
```

## Root Cause
The `UserRegisteredEventHandler` was calling `CreateStripeCustomerCommandHandler` with a randomly generated `SagaId`, but **never actually started the saga** using `sagaManager.startSaga()`. 

The command handler then tried to:
1. Process events via `sagaManager.processEvent()` 
2. Load saga data via `sagaRepository.load(sagaId)`

Both operations failed because the saga didn't exist.

## Solution
Removed the premature Stripe customer creation from user registration flow. The corrected flow is:

1. **User Registration** → Creates billing account only
2. **Payment Method Added** → Starts payment verification saga → Creates Stripe customer

This is the correct approach because:
- Stripe customers require a payment method
- The saga pattern is only needed for the complex payment verification flow
- User registration is a simple event-driven operation

## Changes Made
- **File**: `regtech-billing/application/src/main/java/com/bcbs239/regtech/billing/application/integration/UserRegisteredEventHandler.java`
- **Change**: Removed call to `CreateStripeCustomerCommandHandler` 
- **Result**: User registration now only creates the billing account, deferring Stripe customer creation to when payment method is added

## Testing
After this fix:
1. User registration should complete successfully
2. Billing account should be created
3. No saga errors should occur
4. Stripe customer creation happens later in the payment flow (when saga is properly initialized)
