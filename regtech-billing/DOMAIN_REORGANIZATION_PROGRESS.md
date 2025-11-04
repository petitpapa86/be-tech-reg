# Billing Domain Reorganization Progress

## ✅ Completed

### Accounts Capability
- ✅ BillingAccount.java (aggregate root)
- ✅ BillingAccountId.java (value object)
- ✅ BillingAccountStatus.java (enum)
- ✅ BillingAccountRepository.java (repository interface)

### Payments Capability
- ✅ PaymentMethodId.java (value object)
- ✅ StripeCustomerId.java (value object)
- ✅ PaymentService.java (domain service)

### Shared Capability
- ✅ Money.java (value object)
- ✅ BillingPeriod.java (value object)

## 🔄 In Progress / Next Steps

### Invoicing Capability
- [ ] Move Invoice.java from invoices/ to invoicing/
- [ ] Move InvoiceId.java, InvoiceNumber.java, InvoiceStatus.java
- [ ] Move InvoiceLineItem.java, InvoiceLineItemId.java
- [ ] Move StripeInvoiceId.java
- [ ] Move InvoiceRepository.java
- [ ] Create invoicing/events/ for invoice-related events

### Subscriptions Capability
- [ ] Update Subscription.java imports
- [ ] Move SubscriptionId.java, SubscriptionStatus.java, SubscriptionTier.java
- [ ] Move StripeSubscriptionId.java
- [ ] Move SubscriptionRepository.java
- [ ] Create subscriptions/events/ for subscription-related events

### Dunning Capability
- [ ] Organize existing dunning classes
- [ ] Remove duplicates from valueobjects/

### Events Organization
- [ ] Move account-related events to accounts/events/
- [ ] Move payment-related events to payments/events/
- [ ] Move invoice-related events to invoicing/events/
- [ ] Move subscription-related events to subscriptions/events/
- [ ] Move shared events to shared/events/

### Validation
- [ ] Move BillingValidationUtils to shared/validation/

## 🎯 Benefits Achieved So Far

1. **Clear Capability Boundaries**: Accounts and Payments capabilities are well-defined
2. **Aggregate Co-location**: BillingAccount aggregate and its value objects are together
3. **Shared Concepts**: Common value objects like Money and BillingPeriod are in shared/
4. **Reduced Coupling**: Payment concepts are separated from account concepts

## 🔧 Next Actions

1. Complete the invoicing capability reorganization
2. Update all import statements across the codebase
3. Verify compilation after each capability is moved
4. Update infrastructure and application layers to use new package structure