# RegTech Platform - AI Coding Agent Instructions

## Architecture Overview

This is a **modular monolith** implementing **Clean Architecture + DDD** with event-driven communication between bounded contexts. Each module follows strict layering: `domain → application → infrastructure → presentation`.

### Module Structure
```
regtech-{module}/
├── domain/         # Pure business logic, zero framework dependencies
├── application/    # Use case handlers (@Component/@Service, never @Transactional)
├── infrastructure/ # JPA repositories, external APIs, ALL logging/metrics
└── presentation/   # REST controllers, DTOs, input validation
```

**Critical**: Domain layer MUST NOT contain Spring annotations, logging, or JPA. See [CLEAN_ARCH_GUIDE.md](../CLEAN_ARCH_GUIDE.md) for enforcement rules.

### Bounded Contexts
- **regtech-iam**: Multi-tenant user management, JWT auth, role/permission system
- **regtech-billing**: Stripe integration with saga-based payment verification
- **regtech-ingestion**: Async file processing (JSON/Excel), S3 storage, bank enrichment
- **regtech-data-quality**: Rules engine, validation reports, data processing, S3/local storage
- **regtech-risk-calculation**: Credit/market/operational risk assessment
- **regtech-report-generation**: Report building ONLY - reads processed data from data-quality
- **regtech-core**: Shared kernel (outbox/inbox, sagas, Result pattern, events)

### Data Flow Architecture
```
Ingestion → Data Quality (validate + process + store to S3/local) → Report Generation (read + build reports)
```

**Critical**: Data Quality owns all data processing and storage. Report Generation is a read-only consumer that focuses solely on report building/formatting. Never move data processing logic to Report Generation.

## Critical Developer Workflows

### Running the Application
```powershell
# All modules together (requires PostgreSQL running)
.\mvnw clean install -DskipTests
.\mvnw spring-boot:run -pl regtech-app

# Profiles: development (default), production, observability
# Main app: regtech-app (aggregates all modules)
```

### Database Migrations (Flyway)
- **Structure**: `regtech-app/src/main/resources/db/migration/{module}/`
- **Versioning**: V1 (schemas), V2-V9 (common), V10-V19 (iam), V20-V29 (billing), etc.
- **Commands**:
  ```powershell
  .\mvnw flyway:migrate -pl regtech-app      # Apply migrations
  .\mvnw flyway:info -pl regtech-app         # Check status
  .\mvnw flyway:validate -pl regtech-app     # Verify checksums
  ```
- **Rules**: Migrations are immutable. Never edit existing files. See [DATABASE_MIGRATIONS.md](../DATABASE_MIGRATIONS.md).

### Testing
```powershell
# Unit tests only (fast)
.\mvnw test -pl regtech-{module}/domain

# Integration tests (requires TestContainers)
.\mvnw verify -pl regtech-{module}/infrastructure

# Full build with all tests
.\mvnw clean verify
```

## Project-Specific Patterns

### Result Pattern (Error Handling)
ALL application handlers return `Result<T>` instead of throwing exceptions:
```java
// ✅ CORRECT - Domain validation returns Result
public static Result<Email> of(String value) {
    if (value == null || value.isBlank()) {
        return Result.failure(ErrorDetail.of("Email cannot be blank"));
    }
    return Result.success(new Email(value));
}

// ✅ CORRECT - Application layer propagates failures
public Result<User> handle(CreateUserCommand cmd) {
    Result<Email> emailResult = Email.of(cmd.email());
    if (emailResult.isFailure()) {
        return Result.failure(emailResult.getError());
    }
    // ... continue with success path
}

// ❌ WRONG - Never throw in domain/application
throw new IllegalArgumentException("Invalid email");
```

### Event-Driven Communication

**Internal Events** (within module, Spring `@EventListener`):
```java
// Domain publishes
this.domainEvents.add(new UserRegistered(userId, email));

// Application handles
@EventListener
public void on(UserRegistered event) { ... }
```

**Integration Events** (cross-module, via outbox pattern):
```java
// Publishing module
integrationEventBus.publish(new BillingAccountActivated(userId, accountId));

// Receiving module uses @Component with inbox processing
@Component("iamBillingAccountActivatedEventHandler")
public class BillingAccountActivatedEventHandler implements IIntegrationEventHandler<BillingAccountActivated> {
    @Override
    public void handle(BillingAccountActivated event) { ... }
}
```

**Never** directly inject services from another module. Use integration events + outbox/inbox.

### Repository Pattern (Port/Adapter)
```java
// ✅ Domain defines interface (port) - NO annotations
package com.bcbs239.regtech.iam.domain.users;
public interface UserRepository {
    Optional<User> findById(UserId id);
    void save(User user);
}

// ✅ Infrastructure implements (adapter) - JPA here
@Repository
public class JpaUserRepository implements UserRepository {
    private final JpaUserRepositorySpring springRepo; // JpaRepository
    // ... implementation with @Transactional
}
```

### Saga Pattern (Complex Transactions)
For multi-step processes spanning modules (e.g., payment verification):
```java
@Component
public class PaymentVerificationSaga extends AbstractSaga<PaymentSagaData> {
    public PaymentVerificationSaga(SagaId sagaId, PaymentSagaData data, 
                                   TimeoutScheduler timeoutScheduler,
                                   ApplicationEventPublisher eventPublisher,
                                   IIntegrationEventBus integrationEventBus) {
        super(sagaId, data, timeoutScheduler, eventPublisher, integrationEventBus);
    }
    
    @Override
    protected void defineSagaSteps() {
        step("createStripeCustomer")
            .invokeCommand(this::createStripeCustomer)
            .onSuccess(this::onStripeCustomerCreated)
            .withCompensation(this::deleteStripeCustomer);
        // ... more steps
    }
}
```
See [SAGA_IMPLEMENTATION_GUIDE.md](../SAGA_IMPLEMENTATION_GUIDE.md) for compensation patterns.

### Observability (OpenTelemetry + Micrometer)
- **Never** manually log in domain layer
- **Application layer**: Use `@Observed` annotation on handlers
- **Infrastructure**: Logging allowed, use SLF4J
- **Presentation**: `@Observed` on controllers for tracing
```java
@Component
public class CalculateRiskMetricsCommandHandler {
    @Observed(name = "calculate-risk-metrics", contextualName = "Calculate Risk Metrics")
    public Result<Void> handle(CalculateRiskMetricsCommand command) { ... }
}
```
Configuration: [application-observability.yml](../regtech-app/src/main/resources/application-observability.yml)

## Authentication & Authorization

### JWT Token Structure
```json
{
  "userId": "uuid",
  "email": "user@bank.com",
  "banks": [
    {"bankId": "uuid", "role": "DATA_ANALYST", "permissions": ["BCBS239_UPLOAD_FILES", ...]}
  ]
}
```

### Role Hierarchy (highest to lowest)
1. SYSTEM_ADMIN - Full system access
2. HOLDING_COMPANY_ADMIN - Multi-bank management
3. BANK_ADMIN - Single bank management
4. COMPLIANCE_OFFICER - Audit and compliance
5. DATA_ANALYST - Risk calculations and reports
6. VIEWER - Read-only access

### Securing Endpoints
```java
@GetMapping("/api/v1/reports/{reportId}")
@RequiresPermission("BCBS239_VIEW_REPORTS")
public ResponseEntity<?> getReport(@PathVariable String reportId) { ... }
```
See [AUTHENTICATION_GUIDE.md](../AUTHENTICATION_GUIDE.md) for permission mappings.

## Common Pitfalls & Solutions

### ❌ Don't: Mix layers
```java
// WRONG - Domain calling infrastructure
public class User {
    private void notifyUser() {
        emailService.send(...); // Infrastructure dependency in domain!
    }
}
```
✅ **Do**: Publish domain event, handle in application/infrastructure

### ❌ Don't: Use @Transactional in application layer
```java
// WRONG - Transaction in application
@Service
public class CreateUserHandler {
    @Transactional // Application should not control transactions
    public Result<User> handle(CreateUserCommand cmd) { ... }
}
```
✅ **Do**: Use `@Transactional` only in infrastructure repositories

### ❌ Don't: Throw exceptions for business validation
```java
// WRONG
if (email.isBlank()) throw new ValidationException("Invalid email");
```
✅ **Do**: Return `Result.failure(ErrorDetail.of("Invalid email"))`

### ❌ Don't: Reuse DTOs across layers
```java
// WRONG - Using presentation DTO in domain
public User(UserDto dto) { ... }
```
✅ **Do**: Map DTOs to commands/queries in presentation, use value objects in domain

## Technology Stack Notes

- **Java 25** with preview features enabled
- **Spring Boot 4.0.0** (latest patterns, no deprecated APIs)
- **PostgreSQL** with schema-per-module isolation
- **Maven multi-module** build (reactor pattern)
- **Flyway** for migrations (versioned, immutable)
- **OpenTelemetry + Micrometer** for observability (no custom monitoring packages)
- **TestContainers** for integration tests
- **JSpecify** annotations for nullability (`@Nullable`, `@NonNull`)

## Key Files for Reference

- Module configuration: `regtech-{module}/infrastructure/src/main/resources/application-{module}.yml`
- Shared config: [regtech-app/src/main/resources/application.yml](../regtech-app/src/main/resources/application.yml)
- API endpoints: [API_ENDPOINTS.md](../API_ENDPOINTS.md)
- Migration guide: [MIGRATION_GUIDE.md](../MIGRATION_GUIDE.md)
- Clean arch rules: [CLEAN_ARCH_GUIDE.md](../CLEAN_ARCH_GUIDE.md)

## Module Responsibilities

### Data Quality vs Report Generation
- **Data Quality**: Validates data, processes it, stores results (S3/local), generates quality reports
- **Report Generation**: ONLY reads processed data and builds BCBS 239 reports (no data processing)
- **Data Flow**: Ingestion → Data Quality (store) → Report Generation (read)

If you're adding data processing logic, it belongs in **data-quality**, not **report-generation**.

## When Adding New Features

1. **Start with domain**: Create aggregates, value objects, domain events
2. **Define ports**: Add repository/service interfaces to domain
3. **Implement use cases**: Create command/query handlers in application
4. **Add adapters**: Implement repositories in infrastructure
5. **Expose endpoints**: Create controllers in presentation
6. **Add migrations**: Create versioned SQL in correct version range
7. **Write tests**: Domain (unit), Infrastructure (integration), Presentation (API)

Always follow the dependency rule: **outer layers depend on inner layers, never the reverse**.
