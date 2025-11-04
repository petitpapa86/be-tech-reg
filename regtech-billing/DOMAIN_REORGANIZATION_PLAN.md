# Billing Domain Reorganization Plan

## Current Issues
1. Poor capability organization - everything mixed together
2. Duplicate classes in multiple packages
3. Missing core aggregates (BillingAccount)
4. Events scattered across packages
5. No clear bounded context boundaries
6. Value objects mixed with entities

## Proposed Capability-Based Structure

```
regtech-billing/domain/src/main/java/com/bcbs239/regtech/billing/domain/
├── accounts/                    # Account Management Capability
│   ├── BillingAccount.java     # Aggregate Root
│   ├── BillingAccountId.java   # Value Object
│   ├── BillingAccountStatus.java
│   ├── BillingAccountRepository.java
│   └── events/
│       ├── BillingAccountCreatedEvent.java
│       ├── BillingAccountStatusChangedEvent.java
│       └── BillingAccountConfigurationFailedEvent.java
├── subscriptions/              # Subscription Management Capability
│   ├── Subscription.java      # Aggregate Root
│   ├── SubscriptionId.java    # Value Object
│   ├── SubscriptionStatus.java
│   ├── SubscriptionTier.java
│   ├── StripeSubscriptionId.java
│   ├── SubscriptionRepository.java
│   └── events/
│       ├── SubscriptionCreatedEvent.java
│       ├── SubscriptionCancelledEvent.java
│       └── StripeSubscriptionCreatedEvent.java
├── invoicing/                  # Invoice Management Capability
│   ├── Invoice.java           # Aggregate Root
│   ├── InvoiceId.java        # Value Object
│   ├── InvoiceNumber.java
│   ├── InvoiceStatus.java
│   ├── InvoiceLineItem.java
│   ├── InvoiceLineItemId.java
│   ├── StripeInvoiceId.java
│   ├── InvoiceRepository.java
│   └── events/
│       ├── InvoiceGeneratedEvent.java
│       ├── InvoicePaymentSucceededEvent.java
│       ├── InvoicePaymentFailedEvent.java
│       └── StripeInvoiceCreatedEvent.java
├── payments/                   # Payment Processing Capability
│   ├── PaymentService.java    # Domain Service
│   ├── PaymentMethodId.java   # Value Object
│   ├── StripeCustomerId.java
│   └── events/
│       ├── PaymentVerifiedEvent.java
│       ├── StripePaymentSucceededEvent.java
│       ├── StripePaymentFailedEvent.java
│       ├── StripeCustomerCreatedEvent.java
│       └── StripeCustomerCreationFailedEvent.java
├── dunning/                    # Dunning Management Capability
│   ├── DunningCase.java       # Aggregate Root
│   ├── DunningCaseId.java     # Value Object
│   ├── DunningCaseStatus.java
│   ├── DunningAction.java     # Entity
│   ├── DunningActionId.java
│   ├── DunningActionType.java
│   └── DunningStep.java
├── shared/                     # Shared Domain Concepts
│   ├── valueobjects/
│   │   ├── Money.java
│   │   ├── BillingPeriod.java
│   │   └── ProcessedWebhookEvent.java
│   ├── events/
│   │   ├── WebhookEvent.java
│   │   └── SagaNotFoundEvent.java
│   └── validation/
│       └── BillingValidationUtils.java
└── integration/                # Integration Events
    └── events/
        ├── PaymentMethodAttachmentFailedEvent.java
        ├── PaymentMethodDefaultFailedEvent.java
        └── StripeSubscriptionWebhookReceivedEvent.java
```

## Benefits of This Structure
1. **Clear capability boundaries**: Each package represents a distinct business capability
2. **Aggregate-centric**: Each capability is organized around its aggregate root
3. **Event locality**: Events are co-located with their originating capability
4. **Reduced coupling**: Clear separation of concerns
5. **Better discoverability**: Easy to find related concepts
6. **DDD alignment**: Follows Domain-Driven Design principles

## Migration Steps
1. Create new capability-based package structure
2. Move and reorganize existing classes
3. Remove duplicates and consolidate
4. Update import statements across the codebase
5. Verify compilation and tests