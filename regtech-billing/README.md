# RegTech Billing Module

## Overview

The Billing Module provides comprehensive billing capabilities for the RegTech platform, including payment processing, subscription management, invoice generation, and dunning processes.

## Architecture

This module follows Domain-Driven Design (DDD) principles with a layered architecture:

### Layer Structure

```
regtech-billing/
├── src/main/java/com/bcbs239/regtech/billing/
│   ├── api/                    # API Layer - REST controllers and DTOs
│   │   ├── billing/           # Payment processing endpoints
│   │   ├── subscriptions/     # Subscription management endpoints
│   │   └── webhooks/          # Stripe webhook endpoints
│   ├── application/           # Application Layer - Command handlers and sagas
│   │   ├── commands/          # Command handlers for business operations
│   │   └── sagas/             # Saga orchestration (MonthlyBillingSaga)
│   ├── domain/                # Domain Layer - Business logic and rules
│   │   ├── billing/           # BillingAccount aggregate root
│   │   ├── subscriptions/     # Subscription aggregate root
│   │   ├── invoices/          # Invoice aggregate root
│   │   ├── dunning/           # DunningCase aggregate root
│   │   └── shared/            # Shared value objects and enums
│   └── infrastructure/        # Infrastructure Layer - External integrations
│       ├── stripe/            # Stripe payment service integration
│       ├── persistence/       # JPA entities and repositories
│       ├── events/            # Event publishing and outbox pattern
│       ├── health/            # Health checks and monitoring
│       └── scheduling/        # Scheduled jobs and automation
└── src/main/resources/
    ├── application-billing.yml # Billing module configuration
    └── db/migration/          # Flyway database migration scripts
```

## Key Features

- **Payment Processing**: Stripe integration for secure payment handling
- **Subscription Management**: STARTER tier with €500.00/month pricing
- **Invoice Generation**: Automated monthly billing with overage calculations
- **Dunning Process**: Systematic overdue payment collection
- **Saga Orchestration**: Reliable distributed transaction management
- **Event-Driven Architecture**: Domain events for cross-module communication
- **Functional Programming**: Result<T> and Maybe<T> for error handling
- **Closure-Based DI**: Testable dependency injection pattern

## Dependencies

- **Stripe Java SDK**: Payment processing integration
- **Spring Boot JPA**: Data persistence and repository pattern
- **Spring Boot Web**: REST API endpoints
- **Spring Boot Validation**: Input validation and sanitization
- **Spring Boot Actuator**: Health checks and monitoring

## Configuration

The module uses `application-billing.yml` for configuration including:
- Stripe API keys and webhook secrets
- Subscription tier pricing and limits
- Dunning process timing and intervals
- Invoice and billing cycle settings

## Testing

Test configuration is provided in `src/test/resources/application-test.yml` with:
- H2 in-memory database for testing
- Stripe test API keys
- Debug logging configuration

## Getting Started

1. Configure Stripe API keys in environment variables:
   ```bash
   export STRIPE_API_KEY=sk_test_your_key_here
   export STRIPE_WEBHOOK_SECRET=whsec_your_secret_here
   ```

2. The module is automatically included in the main application component scan

3. Database migrations will be applied automatically via Flyway

## Next Steps

This is the initial module structure. Implementation will proceed according to the tasks defined in `.kiro/specs/billing-context/tasks.md`.