# Billing Context Requirements Document

## Introduction

The Billing Context is responsible for managing subscription tiers, payment processing through Stripe integration, invoice generation, usage tracking, and account status management. This context handles the complete billing lifecycle from payment information collection to invoice generation and dunning processes.

## Requirements

### Requirement 1: Payment Information Processing with Stripe

**User Story:** As a new user completing registration, I want to securely provide payment information, so that my subscription is activated and I can access the platform.

#### Acceptance Criteria

1. WHEN payment information is submitted THEN the system SHALL process it using the ProcessPaymentCommandHandler with signature:
   ```java
   Result<ProcessPaymentResponse> handle(ProcessPaymentCommand command, 
                                       StripeService stripeService, 
                                       BillingAccountRepository repository)
   ```

2. WHEN creating a Stripe customer THEN the system SHALL:
   - Extract user data from correlationId via saga lookup
   - Create Stripe customer with user email and name
   - Attach the provided payment method ID to the customer
   - Set the payment method as the default payment method
   - Store the Stripe customer ID in the billing account

3. WHEN creating a subscription THEN the system SHALL:
   - Use STARTER tier pricing (€500.00/month)
   - Set billing anchor to the first day of the next month
   - Create Stripe subscription with the customer and pricing
   - Store the Stripe subscription ID in the subscription record

4. WHEN generating the first invoice THEN the system SHALL:
   - Calculate pro-rated amount for the current month
   - Set due date to 14 days from issue date
   - Store invoice with PENDING status
   - Link to the Stripe invoice ID

5. WHEN all payment operations succeed THEN the system SHALL:
   - Mark BillingAccount status as ACTIVE
   - Publish PaymentVerifiedEvent to IAM context
   - Return success response with invoice details

### Requirement 2: Billing Account Management

**User Story:** As a system administrator, I want billing accounts to be properly managed with status tracking, so that subscription states are accurately maintained.

#### Acceptance Criteria

1. WHEN a new user registers THEN the system SHALL create a billing account with PENDING_VERIFICATION status
2. WHEN payment is verified THEN the system SHALL update billing account status to ACTIVE
3. WHEN payment fails THEN the system SHALL update billing account status to PAST_DUE
4. WHEN account is suspended THEN the system SHALL update billing account status to SUSPENDED
5. WHEN subscription is cancelled THEN the system SHALL update billing account status to CANCELLED

### Requirement 3: Subscription Tier Management

**User Story:** As a user, I want to have a STARTER subscription tier, so that I can access basic platform features with defined limits.

#### Acceptance Criteria

1. WHEN creating a subscription THEN the system SHALL use STARTER tier by default
2. WHEN STARTER tier is active THEN the system SHALL set monthly price to €500.00
3. WHEN subscription is created THEN the system SHALL set status to ACTIVE
4. WHEN subscription billing fails THEN the system SHALL set status to PAST_DUE
5. WHEN subscription is cancelled THEN the system SHALL set status to CANCELLED

### Requirement 4: Invoice Generation and Management

**User Story:** As a billing administrator, I want invoices to be automatically generated with proper line items and amounts, so that customers receive accurate billing statements.

#### Acceptance Criteria

1. WHEN billing period starts THEN the system SHALL generate invoice with unique invoice number
2. WHEN calculating invoice amounts THEN the system SHALL include subscription amount and overage charges
3. WHEN invoice is created THEN the system SHALL set status to PENDING
4. WHEN invoice is paid THEN the system SHALL update status to PAID and record paid_at timestamp
5. WHEN invoice payment fails THEN the system SHALL update status to FAILED
6. WHEN invoice is overdue THEN the system SHALL update status to OVERDUE

### Requirement 5: Monthly Billing Saga Management

**User Story:** As a system, I want to manage billing cycles through sagas, so that billing processes are reliable and can handle failures with compensation.

#### Acceptance Criteria

1. WHEN billing period starts THEN the system SHALL create a MonthlyBillingSaga for each active subscription
2. WHEN saga is created THEN the system SHALL use correlationId format: {userId}-{billingPeriod} (e.g., "user-123-2024-01")
3. WHEN saga executes THEN the system SHALL:
   - Query ingestion context for usage metrics
   - Calculate subscription and overage charges
   - Generate invoice through Stripe
   - Update billing account status
4. WHEN saga completes successfully THEN the system SHALL mark invoice as issued and saga as completed
5. WHEN saga fails THEN the system SHALL execute compensation logic to reverse partial operations

### Requirement 6: Usage Tracking and Overage Calculation

**User Story:** As a system, I want to track usage and calculate overage charges, so that customers are billed accurately for usage beyond their tier limits.

#### Acceptance Criteria

1. WHEN billing period ends THEN the system SHALL query ingestion context for total exposures uploaded
2. WHEN usage exceeds tier limits THEN the system SHALL calculate overage charges
3. WHEN overage charges exist THEN the system SHALL add them to the invoice as separate line items
4. WHEN calculating pro-rated amounts THEN the system SHALL use daily rates based on billing period
5. WHEN usage is within limits THEN the system SHALL only charge the base subscription amount

### Requirement 7: Dunning Process Management

**User Story:** As a billing administrator, I want automated dunning processes for overdue payments, so that payment collection is systematic and compliant.

#### Acceptance Criteria

1. WHEN invoice becomes overdue THEN the system SHALL create a dunning case with status IN_PROGRESS
2. WHEN dunning case is created THEN the system SHALL start with STEP_1_REMINDER
3. WHEN dunning step is executed THEN the system SHALL record the action with execution timestamp
4. WHEN payment is received THEN the system SHALL mark dunning case as RESOLVED
5. WHEN final dunning step fails THEN the system SHALL suspend the billing account

### Requirement 8: Webhook Event Processing

**User Story:** As a system, I want to process Stripe webhook events idempotently, so that billing state remains consistent with Stripe's records.

#### Acceptance Criteria

1. WHEN webhook event is received THEN the system SHALL check if event_id was already processed
2. WHEN event is new THEN the system SHALL process it and record the event_id with result
3. WHEN event was already processed THEN the system SHALL return success without reprocessing
4. WHEN webhook processing fails THEN the system SHALL record FAILURE result and log error details
5. WHEN webhook processing succeeds THEN the system SHALL record SUCCESS result

### Requirement 9: Domain Event Publishing

**User Story:** As a system, I want to publish domain events for billing state changes, so that other contexts can react to billing events.

#### Acceptance Criteria

1. WHEN billing account status changes THEN the system SHALL publish BillingAccountStatusChangedEvent
2. WHEN payment is verified THEN the system SHALL publish PaymentVerifiedEvent to IAM context
3. WHEN invoice is generated THEN the system SHALL publish InvoiceGeneratedEvent
4. WHEN subscription is cancelled THEN the system SHALL publish SubscriptionCancelledEvent
5. WHEN domain events are published THEN the system SHALL store them in the outbox table for reliable delivery

### Requirement 10: Error Handling and Functional Programming

**User Story:** As a developer, I want all billing operations to use functional error handling patterns, so that errors are handled consistently without exceptions.

#### Acceptance Criteria

1. WHEN any billing operation is performed THEN the system SHALL return Result<T> instead of throwing exceptions
2. WHEN validation fails THEN the system SHALL return Result.failure with structured ErrorDetail
3. WHEN Stripe operations fail THEN the system SHALL wrap Stripe exceptions in Result.failure
4. WHEN repository operations fail THEN the system SHALL return Result.failure with appropriate error codes
5. WHEN using Maybe<T> for optional values THEN the system SHALL avoid null pointer exceptions

### Requirement 11: Audit Trail and Compliance

**User Story:** As a compliance officer, I want complete audit trails of billing calculations and state changes, so that billing processes are transparent and auditable.

#### Acceptance Criteria

1. WHEN saga state changes THEN the system SHALL record event in saga_audit_log with complete state snapshot
2. WHEN billing calculations are performed THEN the system SHALL log calculation details and source data
3. WHEN invoice amounts are determined THEN the system SHALL record the calculation methodology
4. WHEN billing account status changes THEN the system SHALL record the reason and timestamp
5. WHEN saga completes THEN the system SHALL maintain audit log for compliance reporting

### Requirement 12: Scheduled Billing Operations

**User Story:** As a system, I want billing operations to be triggered automatically on schedule, so that billing cycles run without manual intervention.

#### Acceptance Criteria

1. WHEN first day of month arrives THEN the system SHALL trigger monthly billing job at 00:00:00
2. WHEN billing job runs THEN the system SHALL create sagas for all active subscriptions
3. WHEN dunning reminders are due THEN the system SHALL execute scheduled dunning actions
4. WHEN billing operations are scheduled THEN the system SHALL handle timezone considerations properly
5. WHEN scheduled jobs fail THEN the system SHALL log errors and attempt retry with exponential backoff