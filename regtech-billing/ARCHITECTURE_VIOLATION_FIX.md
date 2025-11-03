# Billing Module - Architecture Violation Fix

## Problem
The billing application layer was directly importing and using infrastructure repository implementations (`JpaBillingAccountRepository`, `JpaSubscriptionRepository`, `JpaInvoiceRepository`), which violates clean architecture principles.

## Root Cause
The billing module was missing domain repository interfaces, causing the application layer to depend directly on infrastructure implementations.

## Solution Applied

### 1. Created Domain Repository Interfaces

**BillingAccountRepository.java**:
```java
public interface BillingAccountRepository {
    Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder();
    Function<UserId, Maybe<BillingAccount>> billingAccountByUserFinder();
    Function<BillingAccount, Result<BillingAccountId>> billingAccountSaver();
    Function<BillingAccount, Result<BillingAccountId>> billingAccountUpdater();
}
```

**SubscriptionRepository.java**:
```java
public interface SubscriptionRepository {
    Function<SubscriptionId, Maybe<Subscription>> subscriptionFinder();
    Function<SubscriptionId, Maybe<Subscription>> activeSubscriptionFinder();
    Function<Subscription, Result<SubscriptionId>> subscriptionSaver();
}
```

**InvoiceRepository.java**:
```java
public interface InvoiceRepository {
    Function<StripeInvoiceId, Maybe<Invoice>> invoiceByStripeIdFinder();
    Function<Invoice, Result<InvoiceId>> invoiceSaver();
}
```

### 2. Updated Infrastructure Implementations

Updated JPA repositories to implement domain interfaces:
- `JpaBillingAccountRepository implements BillingAccountRepository`
- `JpaSubscriptionRepository implements SubscriptionRepository`
- `JpaInvoiceRepository implements InvoiceRepository`

### 3. Application Layer Updates Required

The following application classes need to be updated to use domain interfaces:

**Files to Update**:
- `FinalizeBillingAccountCommandHandler.java` ✅ DONE
- `CreateSubscriptionCommandHandler.java`
- `CreateStripeCustomerCommandHandler.java`
- `ProcessPaymentCommandHandler.java`
- `GenerateInvoiceCommandHandler.java`
- `UserRegisteredEventHandler.java`
- `CancelSubscriptionCommandHandler.java`
- `GetSubscriptionCommandHandler.java`
- `CreateStripeInvoiceCommandHandler.java`
- `ProcessWebhookCommandHandler.java`

**Changes Needed**:
1. Replace infrastructure imports with domain interface imports
2. Update constructor parameters to use domain interfaces
3. Update field declarations to use domain interfaces

### 4. Benefits

After this fix:
- ✅ Application layer depends only on domain interfaces
- ✅ Infrastructure layer implements domain interfaces
- ✅ Clean architecture principles followed
- ✅ Testability improved (can mock domain interfaces)
- ✅ Flexibility increased (can swap implementations)

## Status

- ✅ Domain interfaces created
- ✅ Infrastructure implementations updated
- 🔄 Application layer updates in progress
- ⏳ Build verification pending

This fix resolves the architecture violation and establishes proper dependency inversion in the billing module.