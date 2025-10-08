# RegTech Billing Saga Lifecycle Documentation

## Overview

This document provides comprehensive documentation of the billing saga lifecycle in the RegTech system, covering the complete billing cycle from user onboarding through monthly billing, dunning processes, and payment resolution.

## Saga Types

### 1. Monthly Billing Saga
- **Purpose**: Processes monthly billing for all active subscriptions
- **Trigger**: Scheduled execution on the 1st of each month
- **Duration**: Typically completes within minutes per subscription
- **Compensation**: Reverses partial operations on failure

### 2. Payment Processing Saga (Initial Setup)
- **Purpose**: Handles initial payment setup and subscription creation
- **Trigger**: User payment submission via API
- **Duration**: Real-time processing (seconds)
- **Compensation**: Reverses Stripe operations and account creation

## Complete Billing Cycle Overview

The billing system operates on a monthly cycle with the following key phases:

1. **User Onboarding & Payment Setup** (Real-time)
2. **Monthly Usage Tracking** (Continuous)
3. **Monthly Billing Execution** (1st of each month)
4. **Invoice Processing & Payment** (Throughout month)
5. **Dunning Process** (For overdue invoices)
6. **Account Management** (Ongoing)

## Detailed Sequence Diagrams
##
# 1. User Onboarding & Initial Payment Setup

```mermaid
sequenceDiagram
    participant User
    participant BillingAPI as Billing API
    participant PaymentSaga as Payment Processing Saga
    participant StripeService as Stripe Service
    participant BillingRepo as Billing Repository
    participant IAMContext as IAM Context
    participant AuditService as Audit Service

    User->>BillingAPI: POST /api/v1/billing/process-payment
    BillingAPI->>PaymentSaga: Start Payment Processing Saga
    
    Note over PaymentSaga: Step 1: Extract User Data
    PaymentSaga->>PaymentSaga: Extract user data from correlation ID
    PaymentSaga->>AuditService: Log saga state change (INITIALIZED → PROCESSING)
    
    Note over PaymentSaga: Step 2: Create Stripe Customer
    PaymentSaga->>StripeService: Create customer with payment method
    StripeService->>PaymentSaga: Return Stripe customer ID
    PaymentSaga->>AuditService: Log Stripe customer creation
    
    Note over PaymentSaga: Step 3: Create Billing Account
    PaymentSaga->>BillingRepo: Create and save billing account
    BillingRepo->>PaymentSaga: Return billing account ID
    PaymentSaga->>AuditService: Log billing account creation
    
    Note over PaymentSaga: Step 4: Create Subscription
    PaymentSaga->>StripeService: Create Stripe subscription
    StripeService->>PaymentSaga: Return subscription details
    PaymentSaga->>BillingRepo: Save subscription entity
    PaymentSaga->>AuditService: Log subscription creation
    
    Note over PaymentSaga: Step 5: Generate Pro-rated Invoice
    PaymentSaga->>StripeService: Create pro-rated invoice
    StripeService->>PaymentSaga: Return invoice details
    PaymentSaga->>BillingRepo: Save invoice entity
    PaymentSaga->>AuditService: Log invoice generation
    
    Note over PaymentSaga: Step 6: Publish Events
    PaymentSaga->>IAMContext: Publish PaymentVerifiedEvent
    PaymentSaga->>BillingAPI: Publish InvoiceGeneratedEvent
    PaymentSaga->>AuditService: Log saga completion (SUCCESS)
    
    PaymentSaga->>BillingAPI: Return success response
    BillingAPI->>User: Return payment confirmation
```### 2
. Monthly Billing Cycle Execution

```mermaid
sequenceDiagram
    participant Scheduler as Monthly Billing Scheduler
    participant SagaOrchestrator as Saga Orchestrator
    participant MonthlyBillingSaga as Monthly Billing Saga
    participant IngestionContext as Ingestion Context
    participant BillingRepo as Billing Repository
    participant StripeService as Stripe Service
    participant AuditService as Audit Service
    participant IAMContext as IAM Context

    Note over Scheduler: Triggered on 1st of each month at 00:00 UTC
    Scheduler->>BillingRepo: Find all active subscriptions
    BillingRepo->>Scheduler: Return active subscriptions list
    
    loop For each active subscription
        Scheduler->>BillingRepo: Get billing account for subscription
        BillingRepo->>Scheduler: Return billing account with user ID
        
        Scheduler->>SagaOrchestrator: Start Monthly Billing Saga
        Note over Scheduler: Correlation ID: userId-billingPeriod
        
        SagaOrchestrator->>MonthlyBillingSaga: Execute saga
        MonthlyBillingSaga->>AuditService: Log saga start
        
        Note over MonthlyBillingSaga: Step 1: Gather Usage Metrics
        MonthlyBillingSaga->>IngestionContext: Query usage metrics for billing period
        IngestionContext->>MonthlyBillingSaga: Return usage data (exposures, documents, volume)
        MonthlyBillingSaga->>AuditService: Log usage metrics gathered
        
        Note over MonthlyBillingSaga: Step 2: Calculate Charges
        MonthlyBillingSaga->>MonthlyBillingSaga: Calculate subscription charges (€500 base)
        MonthlyBillingSaga->>MonthlyBillingSaga: Calculate overage charges (€0.05 per exposure over 10k)
        MonthlyBillingSaga->>AuditService: Log billing calculation details
        
        Note over MonthlyBillingSaga: Step 3: Generate Invoice
        MonthlyBillingSaga->>BillingRepo: Get billing account details
        MonthlyBillingSaga->>StripeService: Create Stripe invoice
        StripeService->>MonthlyBillingSaga: Return Stripe invoice ID
        MonthlyBillingSaga->>BillingRepo: Create and save domain invoice
        MonthlyBillingSaga->>AuditService: Log invoice amount determination
        
        Note over MonthlyBillingSaga: Step 4: Finalize Billing
        MonthlyBillingSaga->>IAMContext: Publish InvoiceGeneratedEvent
        MonthlyBillingSaga->>IAMContext: Publish billing completion message
        MonthlyBillingSaga->>AuditService: Log saga completion
        
        MonthlyBillingSaga->>SagaOrchestrator: Return success
        SagaOrchestrator->>Scheduler: Saga completed
    end
    
    Scheduler->>Scheduler: Generate billing summary report
```#
## 3. Dunning Process Lifecycle

```mermaid
sequenceDiagram
    participant DunningScheduler as Dunning Process Scheduler
    participant InvoiceRepo as Invoice Repository
    participant DunningRepo as Dunning Case Repository
    participant DunningExecutor as Dunning Action Executor
    participant NotificationService as Notification Service
    participant BillingRepo as Billing Repository
    participant AuditService as Audit Service

    Note over DunningScheduler: Triggered daily at 09:00 UTC
    DunningScheduler->>InvoiceRepo: Find overdue invoices without dunning cases
    InvoiceRepo->>DunningScheduler: Return overdue invoices list
    
    loop For each overdue invoice
        DunningScheduler->>DunningRepo: Check if dunning case exists
        DunningRepo->>DunningScheduler: Return none (new case needed)
        
        DunningScheduler->>DunningScheduler: Create new dunning case
        Note over DunningScheduler: Status: IN_PROGRESS, Step: STEP_1_REMINDER
        DunningScheduler->>DunningRepo: Save dunning case
        DunningScheduler->>AuditService: Log dunning case creation
    end
    
    DunningScheduler->>DunningRepo: Find dunning cases ready for action
    DunningRepo->>DunningScheduler: Return ready cases list
    
    loop For each ready dunning case
        par Async execution
            DunningScheduler->>DunningExecutor: Execute dunning action
            
            alt Step 1: First Reminder
                DunningExecutor->>InvoiceRepo: Get invoice details
                DunningExecutor->>BillingRepo: Get billing account details
                DunningExecutor->>NotificationService: Send first reminder email
                NotificationService->>DunningExecutor: Return email sent status
                DunningExecutor->>DunningScheduler: Return EMAIL_SENT result
                
            else Step 2: Second Reminder
                DunningExecutor->>NotificationService: Send urgent reminder email
                NotificationService->>DunningExecutor: Return email sent status
                DunningExecutor->>DunningScheduler: Return EMAIL_SENT result
                
            else Step 3: Final Notice
                DunningExecutor->>NotificationService: Send final notice email
                NotificationService->>DunningExecutor: Return email sent status
                DunningExecutor->>DunningScheduler: Return EMAIL_SENT result
                
            else Step 4: Account Suspension
                DunningExecutor->>NotificationService: Send suspension notice email
                DunningExecutor->>BillingRepo: Suspend billing account
                BillingRepo->>DunningExecutor: Return suspension status
                DunningExecutor->>DunningScheduler: Return ACCOUNT_SUSPENDED result
            end
            
            DunningScheduler->>DunningRepo: Update dunning case with action result
            DunningScheduler->>AuditService: Log dunning action execution
        end
    end
```### 4. Pa
yment Resolution & Dunning Case Closure

```mermaid
sequenceDiagram
    participant StripeWebhook as Stripe Webhook
    participant WebhookController as Webhook Controller
    participant WebhookHandler as Webhook Command Handler
    participant DunningScheduler as Dunning Scheduler
    participant DunningRepo as Dunning Case Repository
    participant InvoiceRepo as Invoice Repository
    participant AuditService as Audit Service
    participant IAMContext as IAM Context

    StripeWebhook->>WebhookController: POST /api/v1/billing/webhooks/stripe
    Note over StripeWebhook: Event: invoice.payment_succeeded
    
    WebhookController->>WebhookHandler: Process webhook event
    WebhookHandler->>WebhookHandler: Verify webhook signature
    WebhookHandler->>WebhookHandler: Check event idempotency
    
    WebhookHandler->>InvoiceRepo: Find invoice by Stripe invoice ID
    InvoiceRepo->>WebhookHandler: Return invoice entity
    
    WebhookHandler->>InvoiceRepo: Mark invoice as paid
    InvoiceRepo->>WebhookHandler: Return updated invoice
    
    WebhookHandler->>DunningScheduler: Resolve dunning cases for invoice
    DunningScheduler->>DunningRepo: Find dunning case by invoice ID
    DunningRepo->>DunningScheduler: Return active dunning case
    
    alt Dunning case exists and is active
        DunningScheduler->>DunningScheduler: Resolve dunning case
        Note over DunningScheduler: Status: IN_PROGRESS → RESOLVED
        DunningScheduler->>DunningRepo: Save resolved dunning case
        DunningScheduler->>AuditService: Log dunning case resolution
    end
    
    WebhookHandler->>IAMContext: Publish PaymentReceivedEvent
    WebhookHandler->>AuditService: Log webhook processing completion
    WebhookHandler->>WebhookController: Return success response
    WebhookController->>StripeWebhook: Return 200 OK
```### 5. Compl
ete Monthly Billing Cycle Timeline

```mermaid
gantt
    title Monthly Billing Cycle Timeline
    dateFormat  YYYY-MM-DD
    section Month N-1
    Usage Tracking           :active, usage, 2024-01-01, 2024-01-31
    section Month N
    Monthly Billing Execution :milestone, billing, 2024-02-01, 1d
    Invoice Generation       :invoice, 2024-02-01, 1d
    Invoice Due Period       :due, 2024-02-01, 14d
    section Dunning Process
    First Reminder          :reminder1, after due, 1d
    Second Reminder         :reminder2, after reminder1, 7d
    Final Notice           :final, after reminder2, 14d
    Account Suspension     :suspend, after final, 7d
    section Resolution
    Payment Processing     :payment, 2024-02-01, 30d
    Case Resolution        :resolve, 2024-02-01, 30d
```## Saga 
State Management

### Monthly Billing Saga States

```mermaid
stateDiagram-v2
    [*] --> INITIALIZED
    INITIALIZED --> GATHER_METRICS : Start execution
    GATHER_METRICS --> CALCULATE_CHARGES : Usage data retrieved
    CALCULATE_CHARGES --> GENERATE_INVOICE : Charges calculated
    GENERATE_INVOICE --> FINALIZE_BILLING : Invoice created
    FINALIZE_BILLING --> COMPLETED : Events published
    
    GATHER_METRICS --> FAILED : Usage query failed
    CALCULATE_CHARGES --> FAILED : Calculation error
    GENERATE_INVOICE --> FAILED : Invoice creation failed
    FINALIZE_BILLING --> FAILED : Event publishing failed
    
    FAILED --> COMPENSATING : Start compensation
    COMPENSATING --> COMPENSATED : Compensation complete
    COMPENSATED --> [*]
    COMPLETED --> [*]
```

### Dunning Case States

```mermaid
stateDiagram-v2
    [*] --> IN_PROGRESS
    IN_PROGRESS --> IN_PROGRESS : Execute step (success)
    IN_PROGRESS --> RESOLVED : Payment received
    IN_PROGRESS --> CANCELLED : Manual cancellation
    IN_PROGRESS --> FAILED : All steps exhausted
    
    RESOLVED --> [*]
    CANCELLED --> [*]
    FAILED --> [*]
    
    note right of IN_PROGRESS
        Steps: STEP_1_REMINDER →
        STEP_2_REMINDER →
        STEP_3_FINAL_NOTICE →
        STEP_4_SUSPENSION
    end note
```## 
Data Flow Architecture

### Cross-Context Communication

```mermaid
graph TB
    subgraph "IAM Context"
        UserService[User Service]
        UserEvents[User Events]
    end
    
    subgraph "Ingestion Context"
        UsageTracking[Usage Tracking]
        MetricsAPI[Metrics API]
    end
    
    subgraph "Billing Context"
        BillingSaga[Monthly Billing Saga]
        DunningProcess[Dunning Process]
        PaymentSaga[Payment Processing Saga]
        BillingEvents[Billing Events]
    end
    
    subgraph "External Services"
        Stripe[Stripe API]
        EmailService[Email Service]
    end
    
    UserService -->|User Data| PaymentSaga
    PaymentSaga -->|PaymentVerifiedEvent| UserEvents
    UsageTracking -->|Usage Metrics| BillingSaga
    BillingSaga -->|InvoiceGeneratedEvent| BillingEvents
    BillingEvents -->|Account Status| UserEvents
    DunningProcess -->|Notifications| EmailService
    PaymentSaga -->|Customer/Subscription| Stripe
    BillingSaga -->|Invoice Creation| Stripe
```

### Event Flow Patterns

```mermaid
sequenceDiagram
    participant BillingSaga as Billing Saga
    participant EventOutbox as Event Outbox
    participant EventProcessor as Outbox Processor
    participant IAMContext as IAM Context
    participant NotificationService as Notification Service

    BillingSaga->>EventOutbox: Store InvoiceGeneratedEvent
    BillingSaga->>EventOutbox: Store BillingCompletedEvent
    
    Note over EventOutbox: Transactional storage with business data
    
    EventProcessor->>EventOutbox: Poll for pending events
    EventOutbox->>EventProcessor: Return pending events
    
    loop For each pending event
        EventProcessor->>IAMContext: Publish event
        alt Success
            EventProcessor->>EventOutbox: Mark event as processed
        else Failure
            EventProcessor->>EventOutbox: Mark event for retry
        end
    end
    
    IAMContext->>NotificationService: Trigger user notifications
```## Mon
itoring and Observability

### Saga Monitoring Flow

```mermaid
sequenceDiagram
    participant Saga as Any Saga
    participant MonitoredWrapper as Monitored Saga Wrapper
    participant AuditService as Audit Service
    participant MetricsService as Metrics Service
    participant MonitoringAPI as Monitoring API

    Saga->>MonitoredWrapper: Execute saga step
    MonitoredWrapper->>MetricsService: Start operation timer
    MonitoredWrapper->>AuditService: Log state change
    
    MonitoredWrapper->>Saga: Execute actual step
    Saga->>MonitoredWrapper: Return result
    
    MonitoredWrapper->>MetricsService: Record execution time
    MonitoredWrapper->>AuditService: Log step completion
    MonitoredWrapper->>AuditService: Log billing calculations (if applicable)
    
    alt Success
        MonitoredWrapper->>MetricsService: Increment success counter
        MonitoredWrapper->>AuditService: Log successful completion
    else Failure
        MonitoredWrapper->>MetricsService: Increment failure counter
        MonitoredWrapper->>AuditService: Log failure details
    end
    
    MonitoringAPI->>AuditService: Query audit trail
    MonitoringAPI->>MetricsService: Query performance metrics
```

### Compliance and Audit Trail

```mermaid
graph LR
    subgraph "Audit Events"
        StateChange[Saga State Changes]
        BillingCalc[Billing Calculations]
        InvoiceAmount[Invoice Amount Determination]
        AccountStatus[Account Status Changes]
        SagaCompletion[Saga Completion]
    end
    
    subgraph "Audit Storage"
        AuditDB[(Audit Database)]
        ComplianceReports[Compliance Reports]
    end
    
    subgraph "Monitoring Dashboard"
        Metrics[Performance Metrics]
        HealthStatus[Health Status]
        Statistics[Saga Statistics]
    end
    
    StateChange --> AuditDB
    BillingCalc --> AuditDB
    InvoiceAmount --> AuditDB
    AccountStatus --> AuditDB
    SagaCompletion --> AuditDB
    
    AuditDB --> ComplianceReports
    AuditDB --> Metrics
    AuditDB --> HealthStatus
    AuditDB --> Statistics
```## Error
 Handling and Compensation

### Saga Compensation Patterns

```mermaid
sequenceDiagram
    participant SagaOrchestrator as Saga Orchestrator
    participant MonthlyBillingSaga as Monthly Billing Saga
    participant StripeService as Stripe Service
    participant BillingRepo as Billing Repository
    participant AuditService as Audit Service

    SagaOrchestrator->>MonthlyBillingSaga: Execute saga
    MonthlyBillingSaga->>StripeService: Create invoice
    StripeService->>MonthlyBillingSaga: Return invoice ID
    MonthlyBillingSaga->>BillingRepo: Save invoice
    BillingRepo->>MonthlyBillingSaga: Database error
    
    Note over MonthlyBillingSaga: Saga execution failed
    
    SagaOrchestrator->>MonthlyBillingSaga: Compensate
    MonthlyBillingSaga->>AuditService: Log compensation start
    
    alt Invoice was created in Stripe
        MonthlyBillingSaga->>StripeService: Void/cancel invoice
        StripeService->>MonthlyBillingSaga: Invoice voided
        MonthlyBillingSaga->>AuditService: Log invoice cancellation
    end
    
    alt Charges were calculated
        MonthlyBillingSaga->>MonthlyBillingSaga: Clear calculated charges
        MonthlyBillingSaga->>AuditService: Log charge clearing
    end
    
    alt Usage metrics were gathered
        MonthlyBillingSaga->>MonthlyBillingSaga: Clear usage metrics
        MonthlyBillingSaga->>AuditService: Log metrics clearing
    end
    
    MonthlyBillingSaga->>AuditService: Log compensation completion
    MonthlyBillingSaga->>SagaOrchestrator: Compensation successful
```

### Error Recovery Strategies

| Error Type | Recovery Strategy | Compensation Action |
|------------|------------------|-------------------|
| **Usage Query Failure** | Retry with exponential backoff | Clear gathered metrics |
| **Stripe API Failure** | Retry with circuit breaker | Void created resources |
| **Database Failure** | Transaction rollback | Revert entity changes |
| **Event Publishing Failure** | Outbox pattern retry | Mark events as failed |
| **Network Timeout** | Retry with timeout increase | Cancel in-flight operations |

## Performance Characteristics

### Typical Execution Times

| Operation | Expected Duration | SLA |
|-----------|------------------|-----|
| **Payment Processing Saga** | 2-5 seconds | < 10 seconds |
| **Monthly Billing Saga** | 30-60 seconds per subscription | < 2 minutes |
| **Dunning Action Execution** | 1-3 seconds | < 5 seconds |
| **Invoice Generation** | 5-10 seconds | < 30 seconds |
| **Event Processing** | < 1 second | < 3 seconds |

### Scalability Metrics

- **Concurrent Sagas**: Up to 100 monthly billing sagas simultaneously
- **Throughput**: 1000+ subscriptions processed per hour
- **Event Processing**: 10,000+ events per minute
- **Dunning Actions**: 500+ actions per day
- **Audit Records**: 1M+ records per month

## Operational Procedures

### Manual Interventions

1. **Trigger Monthly Billing**:
   ```bash
   POST /api/v1/billing/scheduling/monthly-billing/trigger-previous
   ```

2. **Resolve Dunning Case**:
   ```bash
   POST /api/v1/billing/scheduling/dunning-process/resolve/{invoiceId}
   ```

3. **Check Saga Status**:
   ```bash
   GET /api/v1/billing/monitoring/audit/saga/{sagaId}
   ```

4. **View Performance Metrics**:
   ```bash
   GET /api/v1/billing/monitoring/metrics/summary
   ```

### Troubleshooting Guide

| Issue | Symptoms | Resolution |
|-------|----------|------------|
| **Saga Stuck** | No progress for > 1 hour | Check logs, manual compensation |
| **High Failure Rate** | > 5% saga failures | Check external service health |
| **Slow Processing** | Execution time > SLA | Scale resources, check database |
| **Missing Events** | Events not published | Check outbox processor status |
| **Dunning Not Executing** | No dunning actions | Check scheduler configuration |

## Configuration Reference

### Key Configuration Properties

```yaml
regtech:
  billing:
    scheduling:
      monthly-billing:
        enabled: true
        cron: "0 0 0 1 * ?"
        timezone: "UTC"
      dunning-process:
        enabled: true
        cron: "0 0 9 * * ?"
        thread-pool-size: 5
    
    saga:
      timeout: 300000  # 5 minutes
      retry-attempts: 3
      compensation-timeout: 60000  # 1 minute
    
    monitoring:
      audit-retention-days: 365
      metrics-retention-days: 90
      health-check-interval: 30000
```

This documentation provides a comprehensive view of how sagas orchestrate the entire billing lifecycle, from initial payment setup through monthly billing cycles and dunning processes, with full observability and error handling capabilities.