# Implementation Plan

- [x] 1. Set up billing module structure and core infrastructure





  - Create regtech-billing module with DDD layer structure (api, application, domain, infrastructure)
  - Configure Maven dependencies for Stripe SDK, JPA, and Spring Boot
  - Add billing module to parent POM and main application component scan
  - Create BillingModule configuration class
  - _Requirements: 1.1, 2.1_

- [x] 2. Implement core domain value objects and enums






  - [x] 2.1 Create Money value object with currency support and arithmetic operations

    - Implement Money record with BigDecimal amount and Currency
    - Add factory methods (of, zero) and arithmetic operations (add, multiply)
    - Include validation for currency matching in operations
    - _Requirements: 1.3, 4.2_

  - [x] 2.2 Create billing-specific value objects and enums


    - Implement BillingAccountId, SubscriptionId, InvoiceId as typed identifiers
    - Create SubscriptionTier enum with STARTER tier (€500.00/month, 10000 exposure limit)
    - Implement BillingPeriod record with pro-ration calculation methods
    - Create status enums (BillingAccountStatus, SubscriptionStatus, InvoiceStatus)
    - _Requirements: 2.2, 3.1, 3.2_



  - [x] 2.3 Create Stripe integration value objects





    - Implement StripeCustomerId, StripeSubscriptionId, StripeInvoiceId as typed wrappers
    - Create PaymentMethodId value object
    - Add InvoiceNumber value object with generation logic
    - _Requirements: 1.2_

- [-] 3. Implement domain aggregate roots with business logic



  - [x] 3.1 Create BillingAccount aggregate root


    - Implement BillingAccount with factory method create()
    - Add business methods: activate(), suspend(), canCreateSubscription()
    - Include status transition validation using Result<T> pattern
    - Add version field for optimistic locking
    - _Requirements: 2.1, 2.2, 2.4_

  - [x] 3.2 Create Subscription aggregate root


    - Implement Subscription with factory method create()
    - Add business methods: cancel(), getMonthlyAmount(), isActive()
    - Include tier-based pricing logic
    - Add start/end date management
    - _Requirements: 3.1, 3.2, 3.5_

  - [x] 3.3 Create Invoice aggregate root






    - Implement Invoice with factory method create()
    - Add business methods: markAsPaid(), markAsOverdue(), isOverdue()
    - Include automatic line item generation for subscription and overage
    - Add billing period and due date calculation
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ] 3.4 Create DunningCase aggregate root
    - Implement DunningCase with step progression logic
    - Add methods for step execution and case resolution
    - Include dunning action tracking
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 4. Implement repository interfaces with closure-based patterns
  - [ ] 4.1 Create BillingAccountRepository with functional operations
    - Implement billingAccountFinder() returning Function<BillingAccountId, Maybe<BillingAccount>>
    - Add billingAccountByUserFinder() returning Function<UserId, Maybe<BillingAccount>>
    - Create billingAccountSaver() returning Function<BillingAccount, Result<BillingAccountId>>
    - _Requirements: 2.1, 10.1, 10.2_

  - [ ] 4.2 Create SubscriptionRepository with functional operations
    - Implement activeSubscriptionFinder() returning Function<BillingAccountId, Maybe<Subscription>>
    - Add subscriptionSaver() returning Function<Subscription, Result<SubscriptionId>>
    - Create subscriptionsByStatusFinder() for dunning processes
    - _Requirements: 3.1, 10.1, 10.2_

  - [ ] 4.3 Create InvoiceRepository with functional operations
    - Implement invoiceFinder() returning Function<InvoiceId, Maybe<Invoice>>
    - Add overdueInvoicesFinder() for dunning process
    - Create invoiceSaver() returning Function<Invoice, Result<InvoiceId>>
    - _Requirements: 4.1, 10.1, 10.2_

- [ ] 5. Implement Stripe service integration
  - [ ] 5.1 Create StripeService with customer management
    - Implement createCustomer() method returning Result<StripeCustomer>
    - Add attachPaymentMethod() and setDefaultPaymentMethod() methods
    - Include proper error handling wrapping StripeException in Result<T>
    - _Requirements: 1.2, 10.3_

  - [ ] 5.2 Add subscription management to StripeService
    - Implement createSubscription() with billing anchor to next month start
    - Add pro-ration behavior configuration
    - Include subscription cancellation methods
    - _Requirements: 1.3, 10.3_

  - [ ] 5.3 Add invoice management to StripeService
    - Implement invoice creation and retrieval methods
    - Add webhook signature verification
    - Include invoice status synchronization
    - _Requirements: 1.4, 8.1, 8.2, 8.3_

- [ ] 6. Create application layer command handlers
  - [ ] 6.1 Implement ProcessPaymentCommandHandler
    - Create ProcessPaymentCommand record with validation
    - Implement pure function processPayment() with closure dependencies
    - Add user data extraction from saga correlation ID
    - Include Stripe customer creation and subscription setup
    - Generate pro-rated first invoice
    - Publish PaymentVerifiedEvent to IAM context
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 9.2_

  - [ ] 6.2 Implement CreateSubscriptionCommandHandler
    - Create command and response DTOs with validation
    - Add subscription creation logic with tier validation
    - Include billing account status verification
    - _Requirements: 3.1, 3.2_

  - [ ] 6.3 Implement GenerateInvoiceCommandHandler
    - Create command for manual invoice generation
    - Add usage metrics querying from ingestion context
    - Include overage calculation logic
    - _Requirements: 4.1, 4.2, 6.1, 6.2_

  - [ ] 6.4 Implement ProcessWebhookCommandHandler
    - Create webhook processing command with event validation
    - Add idempotency checking using processed_webhook_events table
    - Include event type routing and processing
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 7. Implement Monthly Billing Saga
  - [ ] 7.1 Create MonthlyBillingSagaData
    - Implement saga data with billing period tracking
    - Add usage metrics and charge calculation fields
    - Include billing step enumeration and progression
    - _Requirements: 5.1, 5.2, 11.1_

  - [ ] 7.2 Implement MonthlyBillingSaga with closure dependencies
    - Create saga with step-based execution (gather metrics, calculate charges, generate invoice, finalize)
    - Add usage metrics querying from ingestion context
    - Include overage calculation based on tier limits
    - Implement invoice generation through Stripe integration
    - Add compensation logic for partial operation reversal
    - _Requirements: 5.3, 5.4, 5.5, 6.1, 6.2, 6.3_

  - [ ] 7.3 Create saga orchestration and scheduling
    - Implement scheduled job to start monthly billing sagas
    - Add saga correlation ID generation (userId-billingPeriod format)
    - Include active subscription querying for saga creation
    - _Requirements: 5.1, 5.2, 12.1, 12.2_

- [ ] 8. Create database schema and JPA entities
  - [ ] 8.1 Create Flyway migration scripts
    - Create billing schema with all required tables
    - Add indexes for performance optimization
    - Include foreign key constraints and cascading rules
    - _Requirements: 2.1, 3.1, 4.1, 7.1, 8.1, 9.1, 11.1_

  - [ ] 8.2 Implement JPA entity mappings
    - Create BillingAccountEntity with proper annotations
    - Add SubscriptionEntity with relationship mappings
    - Implement InvoiceEntity with embedded line items
    - Create DunningCaseEntity with action tracking
    - Add ProcessedWebhookEventEntity for idempotency
    - _Requirements: 2.1, 3.1, 4.1, 7.1, 8.1_

  - [ ] 8.3 Implement JPA repository implementations
    - Create concrete repository classes extending closure-based interfaces
    - Add EntityManager integration with proper transaction handling
    - Include query implementations for complex finders
    - _Requirements: 10.1, 10.2_

- [ ] 9. Implement API controllers and endpoints
  - [ ] 9.1 Create BillingController for payment processing
    - Implement POST /api/v1/billing/process-payment endpoint
    - Add request/response DTOs with validation
    - Include proper error handling with ApiResponse<T> envelope
    - _Requirements: 1.1, 10.1, 10.2_

  - [ ] 9.2 Create SubscriptionController for subscription management
    - Implement GET /api/v1/subscriptions/{id} endpoint
    - Add POST /api/v1/subscriptions/{id}/cancel endpoint
    - Include subscription status and tier information
    - _Requirements: 3.1, 3.5_

  - [ ] 9.3 Create WebhookController for Stripe webhooks
    - Implement POST /api/v1/billing/webhooks/stripe endpoint
    - Add webhook signature verification
    - Include event processing and idempotency handling
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 10. Implement event publishing and domain events
  - [ ] 10.1 Create billing domain events
    - Implement PaymentVerifiedEvent for IAM context integration
    - Add InvoiceGeneratedEvent for notification systems
    - Create BillingAccountStatusChangedEvent for status tracking
    - Add SubscriptionCancelledEvent for cleanup processes
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [ ] 10.2 Implement event outbox pattern
    - Create BillingDomainEventEntity for reliable event delivery
    - Add event publishing service with outbox processing
    - Include event serialization and deserialization
    - _Requirements: 9.5_

- [ ] 11. Add monitoring and health checks
  - [ ] 11.1 Create BillingModuleHealthIndicator
    - Implement health checks for database connectivity
    - Add Stripe API connectivity verification
    - Include billing account status monitoring
    - _Requirements: 2.1_

  - [ ] 11.2 Add saga monitoring and audit logging
    - Implement saga audit log recording for compliance
    - Add billing calculation audit trails
    - Include performance metrics for billing operations
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [ ] 12. Implement scheduled jobs and automation
  - [ ] 12.1 Create monthly billing scheduler
    - Implement @Scheduled job for first day of month execution
    - Add active subscription querying and saga creation
    - Include error handling and retry logic
    - _Requirements: 12.1, 12.2, 12.5_

  - [ ] 12.2 Create dunning process scheduler
    - Implement scheduled dunning reminder execution
    - Add overdue invoice detection and processing
    - Include dunning step progression automation
    - _Requirements: 7.1, 7.2, 7.3, 12.3_

- [ ]* 13. Write comprehensive tests
  - [ ]* 13.1 Create unit tests for domain aggregates
    - Test BillingAccount business logic and status transitions
    - Add Subscription tier management and cancellation tests
    - Test Invoice generation and payment processing
    - _Requirements: 2.1, 3.1, 4.1_

  - [ ]* 13.2 Create command handler tests with closures
    - Test ProcessPaymentCommandHandler with mock closures
    - Add saga testing with functional dependencies
    - Test webhook processing with idempotency scenarios
    - _Requirements: 10.4_

  - [ ]* 13.3 Create integration tests
    - Test end-to-end payment processing flow
    - Add Stripe integration testing with test API keys
    - Test database operations and transaction handling
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 14. Configure security and validation
  - [ ] 14.1 Add billing-specific security configuration
    - Configure endpoint security for billing operations
    - Add API key validation for webhook endpoints
    - Include rate limiting for payment processing
    - _Requirements: 8.1_

  - [ ] 14.2 Implement input validation and sanitization
    - Add validation annotations to command DTOs
    - Include payment amount and currency validation
    - Add Stripe webhook payload validation
    - _Requirements: 10.1, 10.2_

- [ ] 15. Add configuration and environment setup
  - [ ] 15.1 Create billing configuration properties
    - Add Stripe API key configuration
    - Include billing tier pricing configuration
    - Add dunning process timing configuration
    - _Requirements: 3.1, 7.1_

  - [ ] 15.2 Configure development and production profiles
    - Set up H2 database for development testing
    - Add PostgreSQL configuration for production
    - Include Stripe test/live key switching
    - _Requirements: 12.4_