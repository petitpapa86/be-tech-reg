# Complete Billing Module Refactoring Plan

## Current Progress ✅

### Completed:
1. **Domain Interfaces Created**:
   - `BillingAccountRepository`
   - `SubscriptionRepository` 
   - `InvoiceRepository`
   - `PaymentService`

2. **Infrastructure Implementations Updated**:
   - `JpaBillingAccountRepository implements BillingAccountRepository`
   - `JpaSubscriptionRepository implements SubscriptionRepository`
   - `JpaInvoiceRepository implements InvoiceRepository`
   - `StripePaymentService implements PaymentService`

3. **Application Files Partially Updated**:
   - ✅ `CreateSubscriptionCommandHandler` - Complete
   - ✅ `FinalizeBillingAccountCommandHandler` - Complete
   - ✅ `UserRegisteredEventHandler` - Complete
   - ✅ `CancelSubscriptionCommandHandler` - Complete
   - ✅ `GetSubscriptionCommandHandler` - Complete
   - ✅ `CreateStripeInvoiceCommandHandler` - Imports updated
   - ✅ `ProcessPaymentCommandHandler` - Imports updated

## Remaining Work 📋

### Files Needing Method Signature Updates:

1. **ProcessPaymentCommandHandler** - Complex (uses StripeCustomer, StripeSubscription)
2. **CreateStripeInvoiceCommandHandler** - Medium complexity
3. **GenerateInvoiceCommandHandler** - Medium complexity
4. **ProcessWebhookCommandHandler** - Complex (uses Stripe Event types)
5. **CreateStripeCustomerCommandHandler** - Medium complexity

### Files Needing Import Updates Only:

6. **MonthlyBillingSaga** - Simple
7. **PaymentVerificationSaga** - Simple

### Files Needing Validation Abstraction:

8. **ProcessWebhookCommand** - Uses infrastructure validation
9. **GenerateInvoiceCommand** - Uses infrastructure validation
10. **ProcessPaymentCommand** - Uses infrastructure validation

## Systematic Completion Strategy 🎯

### Phase 1: Complete Simple Import Updates (15 minutes)
Update remaining files that only need import changes:
- MonthlyBillingSaga
- PaymentVerificationSaga
- Command classes with validation imports

### Phase 2: Update Method Signatures (45 minutes)
For files using Stripe types, update method signatures to use PaymentService domain types:
- Replace `StripeCustomer` with `PaymentService.CustomerCreationResult`
- Replace `StripeSubscription` with `PaymentService.SubscriptionCreationResult`
- Update method calls to use `paymentService` instead of `stripeService`

### Phase 3: Handle Complex Cases (30 minutes)
- ProcessWebhookCommandHandler: Abstract Stripe Event handling
- Validation classes: Create domain validation interfaces

### Phase 4: Test and Verify (15 minutes)
- Run build to verify all compilation errors are resolved
- Test basic functionality

## Implementation Commands

### Quick Import Updates:
```bash
# Update MonthlyBillingSaga
sed -i 's/import.*infrastructure\.external\.stripe\.StripeService/import com.bcbs239.regtech.billing.domain.services.PaymentService/' regtech-billing/application/src/main/java/com/bcbs239/regtech/billing/application/invoicing/MonthlyBillingSaga.java

# Update PaymentVerificationSaga  
sed -i 's/import.*infrastructure\.database\.repositories/import com.bcbs239.regtech.billing.domain.repositories/' regtech-billing/application/src/main/java/com/bcbs239/regtech/billing/application/payments/PaymentVerificationSaga.java
```

### Method Signature Pattern:
```java
// Before:
Function<UserDataAndPaymentMethod, Result<StripeCustomer>> stripeCustomerCreator

// After:
Function<UserDataAndPaymentMethod, Result<PaymentService.CustomerCreationResult>> customerCreator
```

## Estimated Completion Time: 1.5 hours

## Alternative: Temporary Fix Approach

If immediate build success is needed:

1. **Add infrastructure dependency** to billing application pom.xml:
```xml
<dependency>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech-billing-infrastructure</artifactId>
</dependency>
```

2. **Schedule proper refactoring** for dedicated time slot

3. **Benefits of temporary fix**:
   - ✅ Immediate build success
   - ✅ Preserves architectural work done
   - ✅ Allows testing of other modules
   - ✅ Can complete refactoring incrementally

## Recommendation

Given the extensive nature of the remaining work, I recommend:

1. **Apply temporary fix now** (2 minutes)
2. **Test full project build** (10 minutes)  
3. **Schedule dedicated refactoring session** (1.5 hours)

This approach maximizes immediate progress while ensuring the architectural improvements are completed properly.