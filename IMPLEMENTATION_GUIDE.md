# RegTech Platform - Implementation Guide

> **A comprehensive guide for implementing new requirements following established conventions, best practices, and Clean Architecture with DDD principles.**

---

## Table of Contents

1. [Overview](#overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Module Organization](#module-organization)
5. [Core Concepts](#core-concepts)
6. [Architecture Patterns](#architecture-patterns)
7. [Implementation Best Practices](#implementation-best-practices)
8. [Context Navigation](#context-navigation)
9. [Security & Authorization](#security--authorization)
10. [Event-Driven Communication](#event-driven-communication)
11. [Step-by-Step Implementation Guide](#step-by-step-implementation-guide)
12. [Architecture Decisions](#architecture-decisions)

---

## Overview

The RegTech platform is a modular, microservices-based system designed for regulatory compliance (BCBS 239) with a focus on:

- **Clean Architecture** with Domain-Driven Design (DDD)
- **Layered Architecture** (Domain → Application → Infrastructure → Presentation)
- **Event-Driven Communication** (Internal events, Integration events, Sagas)
- **Fail-Fast Philosophy** (No try-catch in controllers, centralized error handling)
- **Type Safety** with Result/Maybe types
- **Multi-tenancy** and **Role-Based Access Control (RBAC)**

### Key Principles

✅ **Separation of Concerns** - Each layer has a distinct responsibility  
✅ **Dependency Inversion** - Dependencies flow inward (Presentation → Infrastructure → Application → Domain)  
✅ **Fail-Fast** - Validation at boundaries, throw exceptions for unexpected errors  
✅ **Immutability** - Value objects and commands are immutable  
✅ **Event Sourcing Ready** - Domain events capture state changes  
✅ **Testability** - Each layer independently testable

---

## Technology Stack

### Core Technologies
- **Java 25** with preview features enabled
- **Spring Boot 3.5.6** - Framework for microservices
- **Maven** - Multi-module build system
- **PostgreSQL** - Primary database
- **H2** - In-memory database for testing

### Key Libraries
- **Lombok** - Reduce boilerplate code
- **Flyway** - Database migrations
- **Testcontainers** - Integration testing
- **Jackson** - JSON serialization
- **Stripe SDK** - Payment processing (Billing module)
- **Apache POI** - Excel processing (Data Quality module)

### Infrastructure
- **Docker Compose** - Local development environment
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Data access layer
- **Spring Boot Actuator** - Health checks and monitoring

---

## Project Structure

```
regtech/
├── regtech-core/              # Shared kernel (Result, Maybe, Events, Security)
│   ├── domain/                # Core domain primitives
│   │   └── pom.xml
│   ├── application/           # Inbox/Outbox processing
│   │   └── pom.xml
│   ├── infrastructure/        # Event processing, security
│   │   └── pom.xml
│   ├── presentation/          # Base controllers, API responses
│   │   └── pom.xml
│   └── pom.xml                # Core parent POM
│
├── regtech-iam/               # Identity & Access Management
│   ├── domain/                # User aggregates, roles, permissions
│   │   └── pom.xml
│   ├── application/           # User commands, handlers
│   │   └── pom.xml
│   ├── infrastructure/        # Repositories, security
│   │   └── pom.xml
│   ├── presentation/          # User controllers, DTOs
│   │   └── pom.xml
│   └── pom.xml                # IAM parent POM
│
├── regtech-billing/           # Billing & Subscriptions
│   ├── domain/                # Billing aggregates (Invoice, Subscription)
│   │   └── pom.xml
│   ├── application/           # Payment sagas, command handlers
│   │   └── pom.xml
│   ├── infrastructure/        # Stripe integration, repositories
│   │   └── pom.xml
│   ├── presentation/          # Billing controllers
│   │   └── pom.xml
│   └── pom.xml                # Billing parent POM
│
├── regtech-data-quality/      # Data Quality & Validation
│   ├── domain/                # Validation rules, quality metrics
│   │   └── pom.xml
│   ├── application/           # Validation workflows
│   │   └── pom.xml
│   ├── infrastructure/        # File processors, schedulers
│   │   └── pom.xml
│   ├── presentation/          # Validation controllers
│   │   └── pom.xml
│   └── pom.xml                # Data Quality parent POM
│
├── regtech-ingestion/         # Data Ingestion & Processing
│   ├── domain/                # Ingestion aggregates
│   │   └── pom.xml
│   ├── application/           # Ingestion workflows
│   │   └── pom.xml
│   ├── infrastructure/        # File handlers, parsers
│   │   └── pom.xml
│   ├── presentation/          # Ingestion controllers
│   │   └── pom.xml
│   └── pom.xml                # Ingestion parent POM
│
├── regtech-app/               # Application entry point
│   ├── config/                # Global configuration, exception handling
│   └── pom.xml                # App POM (aggregates all modules)
│
└── pom.xml                    # Root parent POM
```

### Layered Architecture Pattern

Each module follows a **strict 4-layer architecture**:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │  ← Controllers, DTOs, API
├─────────────────────────────────────────┤
│       Infrastructure Layer              │  ← Repositories, External Services
├─────────────────────────────────────────┤
│        Application Layer                │  ← Commands, Handlers, Sagas
├─────────────────────────────────────────┤
│          Domain Layer                   │  ← Aggregates, Entities, Value Objects
└─────────────────────────────────────────┘
```

**Dependency Flow**: Presentation → Infrastructure → Application → Domain

---

## Module Organization

### Domain Layer (`<module>-domain`)
**Purpose**: Pure business logic, no frameworks

**Contains**:
- **Aggregates** - Consistency boundaries (e.g., `BillingAccount`, `User`)
- **Entities** - Domain objects with identity
- **Value Objects** - Immutable domain concepts (e.g., `Money`, `Email`)
- **Domain Events** - Business events (e.g., `PaymentProcessed`)
- **Domain Services** - Business operations spanning multiple aggregates
- **Specifications** - Business rules encapsulation

**Dependencies**: Only `regtech-core-domain`, Lombok, JUnit

**Example Structure**:
```
domain/
├── pom.xml                    # Domain layer POM
└── src/main/java/com/bcbs239/regtech/<module>/domain/
    ├── aggregates/
    │   ├── User.java
    │   └── BillingAccount.java
    ├── valueobjects/
    │   ├── Money.java
    │   └── Email.java
    ├── events/
    │   ├── UserRegisteredEvent.java
    │   └── PaymentProcessedEvent.java
    └── services/
        └── PaymentDomainService.java
```

**Example `pom.xml`**:
```xml
<parent>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech-<module></artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>

<artifactId>regtech-<module>-domain</artifactId>
<name>regtech-<module>-domain</name>

<dependencies>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-core-domain</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

### Application Layer (`<module>-application`)
**Purpose**: Orchestrate business workflows

**Contains**:
- **Commands** - Write operations with validation
- **Command Handlers** - Execute commands, update aggregates
- **Queries** - Read operations
- **Query Handlers** - Fetch data for queries
- **Sagas** - Orchestrate multi-step distributed transactions
- **Application Services** - Coordinate workflows

**Organized by Capability** (not by technical type):
```
application/
├── pom.xml                    # Application layer POM
└── src/main/java/com/bcbs239/regtech/<module>/application/
    ├── subscriptions/              # Subscription management capability
    │   ├── CreateSubscriptionCommand.java
    │   ├── CreateSubscriptionCommandHandler.java
    │   ├── CancelSubscriptionCommand.java
    │   └── CancelSubscriptionCommandHandler.java
    ├── payments/                   # Payment processing capability
    │   ├── ProcessPaymentCommand.java
    │   ├── ProcessPaymentCommandHandler.java
    │   └── PaymentVerificationSaga.java
    ├── invoicing/                  # Invoice management capability
    │   ├── GenerateInvoiceCommand.java
    │   └── MonthlyBillingSaga.java
    └── integration/                # Cross-module integration
        └── BillingIntegrationEventHandler.java
```

**Dependencies**: 
- Domain layer
- `regtech-core-domain`
- `regtech-core-application`
- Spring Boot Starter
- Spring Boot Validation

**Example `pom.xml`**:
```xml
<parent>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech-<module></artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>

<artifactId>regtech-<module>-application</artifactId>
<name>regtech-<module>-application</name>

<dependencies>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-<module>-domain</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-core-application</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

---

### Infrastructure Layer (`<module>-infrastructure`)
**Purpose**: Technical implementations, external integrations

**Contains**:
- **Database Entities** - JPA entities (separate from domain)
- **Repositories** - Data access implementations
- **Mappers** - Convert domain ↔ persistence entities
- **External Services** - Third-party integrations (Stripe, AWS)
- **Configuration** - Spring configurations
- **Security** - Authentication, authorization implementations
- **Jobs & Schedulers** - Background tasks
- **Messaging** - Event bus implementations

**Example Structure**:
```
infrastructure/
├── pom.xml                    # Infrastructure layer POM
└── src/main/java/com/bcbs239/regtech/<module>/infrastructure/
    ├── persistence/
    │   ├── entities/
    │   │   └── UserEntity.java
    │   ├── repositories/
    │   │   └── JpaUserRepository.java
    │   └── mappers/
    │       └── UserMapper.java
    ├── external/
    │   └── stripe/
    │       └── StripePaymentService.java
    ├── config/
    │   └── ModuleConfiguration.java
    └── security/
        └── AuthenticationProvider.java
```

**Dependencies**:
- Domain layer ONLY (NOT Application layer)
- `regtech-core-domain`
- `regtech-core-infrastructure`
- Spring Boot Data JPA
- Spring Boot Security
- External SDKs (Stripe, AWS)

**⚠️ CRITICAL**: Infrastructure must NOT depend on Application layer. This is a core Clean Architecture principle.

**Example `pom.xml`**:
```xml
<parent>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech-<module></artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>

<artifactId>regtech-<module>-infrastructure</artifactId>
<name>regtech-<module>-infrastructure</name>

<dependencies>
    <!-- ✅ CORRECT: Depends on Domain -->
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-<module>-domain</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- ❌ WRONG: Should NOT depend on Application -->
    <!-- 
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-<module>-application</artifactId>
        <version>${project.version}</version>
    </dependency>
    -->
    
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-core-domain</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-core-infrastructure</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <!-- External SDKs as needed -->
</dependencies>
```

---

### Presentation Layer (`<module>-presentation`)
**Purpose**: Expose APIs, handle HTTP concerns

**Contains**:
- **Controllers** - REST endpoints using functional routing
- **DTOs** - Request/Response objects
- **Validation** - Input validation annotations
- **API Documentation** - OpenAPI/Swagger annotations

**Example Structure**:
```
presentation/
├── pom.xml                    # Presentation layer POM
└── src/main/java/com/bcbs239/regtech/<module>/presentation/
    ├── users/
    │   ├── UserController.java
    │   ├── RegisterUserRequest.java
    │   └── UserResponse.java
    ├── billing/
    │   ├── BillingController.java
    │   └── InvoiceResponse.java
    └── health/
        └── HealthController.java
```

**Dependencies**:
- Application layer (for commands, handlers)
- Infrastructure layer (for repository implementations - injected via Spring)
- Domain layer
- `regtech-core-presentation`
- Spring Boot Web
- Spring Boot Security
- Spring Boot Validation

**Example `pom.xml`**:
```xml
<parent>
    <groupId>com.bcbs239</groupId>
    <artifactId>regtech-<module></artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>

<artifactId>regtech-<module>-presentation</artifactId>
<name>regtech-<module>-presentation</name>

<dependencies>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-<module>-domain</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-<module>-application</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-<module>-infrastructure</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.bcbs239</groupId>
        <artifactId>regtech-core-presentation</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

---

## Core Concepts

### 1. Result Type (regtech-core)

**Purpose**: Type-safe error handling without exceptions

```java
public record Result<T>(T value, List<ErrorDetail> errors) {
    public static <T> Result<T> success(T value);
    public static <T> Result<T> failure(ErrorDetail error);
    public boolean isSuccess();
    public boolean isFailure();
    public Optional<T> getValue();
}
```

**Usage**:
```java
// In Command validation
public static Result<CreateUserCommand> create(String email, String password) {
    if (email == null || email.isBlank()) {
        return Result.failure(ErrorDetail.of(
            "INVALID_EMAIL",
            ErrorType.VALIDATION,
            "Email is required",
            "user.email.required"
        ));
    }
    return Result.success(new CreateUserCommand(email, password));
}

// In Command Handler
public Result<RegisterUserResponse> handle(RegisterUserCommand command) {
    // Business logic
    if (userExists) {
        return Result.failure(ErrorDetail.of(
            "USER_EXISTS",
            ErrorType.BUSINESS,
            "User already exists",
            "user.already.exists"
        ));
    }
    
    User user = User.create(command.email(), command.password());
    userRepository.save(user);
    
    return Result.success(new RegisterUserResponse(user.getId()));
}
```

---

### 2. Maybe Type (regtech-core)

**Purpose**: Represent optional values functionally

```java
public sealed interface Maybe<T> {
    static <T> Maybe<T> some(T value);
    static <T> Maybe<T> none();
    boolean isPresent();
    T getValue();
    T orElse(T defaultValue);
}
```

**Usage for Optional Fields**:
```java
// In Command
public record CreateUserCommand(
    String email,
    String password,
    Maybe<String> middleName,  // Optional field
    Maybe<String> phone        // Optional field
) {
    public static Result<CreateUserCommand> create(
        String email,
        String password,
        String middleName,
        String phone
    ) {
        return Result.success(new CreateUserCommand(
            email,
            password,
            middleName != null ? Maybe.some(middleName) : Maybe.none(),
            phone != null ? Maybe.some(phone) : Maybe.none()
        ));
    }
}

// In Handler
public Result<UserResponse> handle(CreateUserCommand command) {
    User user = User.create(
        command.email(),
        command.password(),
        command.middleName().orElse(null)  // Extract optional value
    );
    // ...
}
```

**When to Use Maybe vs Optional**:
- **Maybe**: Domain layer, commands, value objects (functional approach)
- **Optional**: Infrastructure layer, Java interop (Java standard)

---

### 3. Domain Events

**Base Types**:
```java
// Base for all events
public abstract class DomainEvent {
    private final String eventId;
    private final String correlationId;
    private final Maybe<String> causationId;
    private final Instant timestamp;
    private final String eventType;
}

// For events within a bounded context
public abstract class InternalEvent extends DomainEvent {}

// For events across bounded contexts
public abstract class IntegrationEvent extends DomainEvent {}
```

**Event Types**:

| Event Type | Scope | Publisher | Consumer | Example |
|------------|-------|-----------|----------|---------|
| **Domain Event** | Aggregate | Domain | Application | `UserCreatedEvent` |
| **Internal Event** | Module | Application | Application (same module) | `PaymentProcessedEvent` |
| **Integration Event** | Cross-Module | Application | Application (other modules) | `BillingAccountActivatedEvent` |

**Example Domain Event**:
```java
public class UserRegisteredEvent extends DomainEvent {
    private final String userId;
    private final String email;
    private final Instant registeredAt;

    public UserRegisteredEvent(String correlationId, String userId, String email) {
        super(correlationId, "UserRegistered");
        this.userId = userId;
        this.email = email;
        this.registeredAt = Instant.now();
    }

    @Override
    public String eventType() {
        return "UserRegistered";
    }
}
```

**Example Integration Event**:
```java
public class BillingAccountActivatedEvent extends IntegrationEvent {
    private final String userId;
    private final String billingAccountId;
    private final String stripeCustomerId;

    public BillingAccountActivatedEvent(
        String correlationId,
        Maybe<String> causationId,
        String userId,
        String billingAccountId,
        String stripeCustomerId
    ) {
        super(correlationId, causationId, "BillingAccountActivated");
        this.userId = userId;
        this.billingAccountId = billingAccountId;
        this.stripeCustomerId = stripeCustomerId;
    }

    @Override
    public String eventType() {
        return "BillingAccountActivated";
    }
}
```

---

### 4. BaseUnitOfWork (Transactional Outbox Pattern)

**Purpose**: Ensure reliable event publishing with transactional guarantees

`BaseUnitOfWork` implements the **Transactional Outbox Pattern** to guarantee that domain events are published reliably, even in case of failures.

**Key Features**:
- ✅ Events saved in same transaction as domain changes
- ✅ No lost events (atomic with database transaction)
- ✅ Automatic serialization to JSON
- ✅ Background processor handles actual publishing
- ✅ Retries on failure
- ✅ Works for both internal and integration events

**Class Structure**:
```java
@Component
@Scope("prototype")  // New instance per transaction
public class BaseUnitOfWork {
    private final IOutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    // Register entity whose domain events should be collected
    public void registerEntity(Entity entity) {
        collectDomainEvents(entity);
    }
    
    // Save changes: persist collected events to outbox
    @Transactional
    public void saveChanges() {
        // Serialize events to JSON
        // Save to outbox table
        // Clear collected events
    }
}
```

**Usage Pattern**:
```java
@Service
public class SomeCommandHandler {
    private final BaseUnitOfWork unitOfWork;
    private final SomeRepository repository;
    
    @Transactional
    public Result<Response> handle(SomeCommand command) {
        // 1. Execute business logic
        SomeAggregate aggregate = SomeAggregate.create(...);
        // Aggregate internally calls: this.addDomainEvent(new SomeEvent(...))
        
        // 2. Save to repository
        repository.save(aggregate);
        
        // 3. Register with unit of work
        unitOfWork.registerEntity(aggregate);
        
        // 4. Save changes (events persisted to outbox)
        unitOfWork.saveChanges();
        
        return Result.success(new Response(...));
    }
}
```

**Why This Pattern?**

❌ **Without Outbox** (Direct Publishing):
```java
repository.save(entity);        // ✅ Committed
eventBus.publish(event);        // ❌ Fails - event lost forever!
```

✅ **With Outbox** (Transactional):
```java
repository.save(entity);        // ✅ Committed
unitOfWork.saveChanges();       // ✅ Events saved in same transaction
// Background processor publishes from outbox (retries if fails)
```

**Event Flow**:
```
1. Handler → unitOfWork.registerEntity(aggregate)
2. Handler → unitOfWork.saveChanges()
3. Events serialized to JSON → saved to outbox table
4. Transaction commits (entity + events saved atomically)
5. OutboxProcessor (background job) → reads unprocessed events
6. OutboxProcessor → publishes to event bus
7. OutboxProcessor → marks event as processed
8. If publish fails → retry later (event still in outbox)
```

---

### 5. Inbox/Outbox Pattern

**Purpose**: Ensure reliable event delivery in distributed systems

**Outbox Pattern** (Publishing Events):
```
1. Handler saves domain changes + event to database (same transaction)
2. Outbox processor reads events from outbox table
3. Publishes events to message bus
4. Marks events as processed
```

**Inbox Pattern** (Consuming Events):
```
1. Event arrives from message bus
2. Saves to inbox table (idempotent)
3. Inbox processor reads events
4. Processes event with handler
5. Marks as processed
```

**Configuration** (in application layer):
```java
@Configuration
public class ModuleEventConfiguration {
    
    @Bean
    public OutboxProcessingConfiguration outboxConfig() {
        return OutboxProcessingConfiguration.builder()
            .enabled(true)
            .processingIntervalSeconds(5)
            .batchSize(50)
            .build();
    }
    
    @Bean
    public InboxConfiguration inboxConfig() {
        return InboxConfiguration.builder()
            .enabled(true)
            .processingIntervalSeconds(5)
            .maxRetries(3)
            .build();
    }
}
```

---

### 6. Sagas (Distributed Transactions)

**Purpose**: Orchestrate multi-step workflows across services with automatic compensation

**Key Saga Components**:
```java
public class PaymentVerificationSaga extends AbstractSaga<PaymentVerificationSagaData> {
    
    private final ApplicationEventPublisher eventPublisher;
    private final IIntegrationEventBus integrationEventBus;
    
    // Constructor with dependencies
    public PaymentVerificationSaga(
        SagaId id, 
        PaymentVerificationSagaData data, 
        TimeoutScheduler timeoutScheduler, 
        ILogger logger, 
        ApplicationEventPublisher eventPublisher,
        IIntegrationEventBus integrationEventBus
    ) {
        super(id, "PaymentVerificationSaga", data, timeoutScheduler, logger);
        this.eventPublisher = eventPublisher;
        this.integrationEventBus = integrationEventBus;
        registerHandlers();  // Register event handlers
    }
    
    // Register event handlers
    private void registerHandlers() {
        onEvent(SagaStartedEvent.class, this::handleSagaStarted);
        onEvent(StripeCustomerCreatedEvent.class, this::handleStripeCustomerCreated);
        onEvent(StripePaymentSucceededEvent.class, this::handleStripePaymentSucceeded);
        onEvent(StripePaymentFailedEvent.class, this::handleStripePaymentFailed);
    }
    
    // Event handler
    private void handleStripeCustomerCreated(StripeCustomerCreatedEvent event) {
        data.setStripeCustomerId(event.getStripeCustomerId());
        data.setBillingAccountId(event.getBillingAccountId());
        
        // Dispatch next command
        dispatchCommand(new CreateStripeSubscriptionCommand(...));
        
        updateStatus();
    }
    
    // Status management
    @Override
    protected void updateStatus() {
        if (data.getFailureReason() != null) {
            setStatus(SagaStatus.FAILED);
            setCompletedAt(Instant.now());
        } else if (allStepsCompleted()) {
            setStatus(SagaStatus.COMPLETED);
            setCompletedAt(Instant.now());
            publishCompletionEvent();
        }
    }
    
    // Compensation (automatic rollback on failure)
    @Override
    protected void compensate() {
        String failureReason = data.getFailureReason();
        
        // Publish compensation events for async handling
        if (data.getStripePaymentIntentId() != null) {
            eventPublisher.publishEvent(new RefundPaymentEvent(...));
        }
        
        if (data.getStripeInvoiceId() != null) {
            eventPublisher.publishEvent(new VoidInvoiceEvent(...));
        }
        
        if (data.getStripeSubscriptionId() != null) {
            eventPublisher.publishEvent(new CancelSubscriptionEvent(...));
        }
    }
}
```

**When to Use Sagas**:
- Multi-step workflows across services
- Long-running processes
- Need automatic rollback on failure
- External API calls (Stripe, AWS)

**Example**: `PaymentVerificationSaga` - Creates Stripe customer, subscription, invoice, processes payment, activates billing account

For detailed saga implementation, see: [`SAGA_IMPLEMENTATION_GUIDE.md`](./SAGA_IMPLEMENTATION_GUIDE.md)

---

## Architecture Patterns

### Clean Architecture with DDD

**Dependency Rules**:
```
┌─────────────────────────────────────────────────────┐
│  Presentation (Controllers, DTOs)                   │
│  ↓ depends on Application                           │
│  ↓ depends on Infrastructure                        │
│                                                      │
│  Application (Commands, Handlers, Sagas)            │
│  ↓ depends on Domain                                │
│  ↑ implements interfaces defined in Domain          │
│                                                      │
│  Infrastructure (Repos, External Services)          │
│  ↓ depends on Domain                                │
│  ↑ implements interfaces defined in Domain          │
│                                                      │
│  Domain (Aggregates, Entities, Value Objects)       │
│  ← No dependencies, pure business logic             │
│  ← Defines repository interfaces                    │
└─────────────────────────────────────────────────────┘
```

**Key Principles**:

1. **Domain Independence**: Domain layer has NO framework dependencies and defines all interfaces
2. **Dependency Inversion**: Application and Infrastructure both depend on Domain, not on each other
3. **Infrastructure Isolation**: Infrastructure implements domain interfaces (repositories, external services)
4. **Application Orchestration**: Application layer orchestrates domain objects and uses domain interfaces
5. **Presentation Thin**: Controllers depend on Application and Infrastructure, just routing and validation

**⚠️ IMPORTANT**: Infrastructure should NEVER depend on Application layer. If you see this dependency in the project, it's a violation of Clean Architecture principles and should be refactored.

---

### CQRS (Command Query Responsibility Segregation)

**Commands** (Write Operations):
```java
public record CreateSubscriptionCommand(
    String userId,
    String planId,
    String paymentMethodId
) {
    public static Result<CreateSubscriptionCommand> create(...) {
        // Validation logic
    }
}

@Component
public class CreateSubscriptionCommandHandler {
    public Result<SubscriptionResponse> handle(CreateSubscriptionCommand command) {
        // Execute command, return result
    }
}
```

**Queries** (Read Operations):
```java
public record GetUserQuery(String userId) {}

@Component
public class GetUserQueryHandler {
    public Result<UserResponse> handle(GetUserQuery query) {
        // Fetch data, return result
    }
}
```

**Separation Benefits**:
- Optimized read/write models
- Scalability (different databases)
- Clear intent (command vs query)

---

### Event-Driven Architecture

**Event Flow**:
```
1. Domain Event → Raised by aggregate
2. Application Event → Published internally
3. Outbox → Saved with transaction
4. Event Bus → Published to other modules
5. Inbox → Received by consumer
6. Event Handler → Processes event
```

**Example Flow**:
```java
// 1. Raise Domain Event (in Aggregate)
public class User {
    public void register() {
        // Business logic
        this.addDomainEvent(new UserRegisteredEvent(this.id, this.email));
    }
}

// 2. Publish Application Event (in Handler)
@Component
public class RegisterUserCommandHandler {
    public Result<UserResponse> handle(RegisterUserCommand command) {
        User user = User.register(...);
        userRepository.save(user);  // Triggers domain events
        
        // Publish integration event to other modules
        eventPublisher.publish(new UserCreatedIntegrationEvent(user.getId()));
        
        return Result.success(new UserResponse(user.getId()));
    }
}

// 3. Consume Event (in Other Module)
@Component
public class UserEventHandler {
    @EventHandler
    public void handleUserCreated(UserCreatedIntegrationEvent event) {
        // React to user creation
    }
}
```

---

## Implementation Best Practices

### 1. Fail-Fast Philosophy

**Principle**: Validate early, throw exceptions for unexpected errors, no try-catch in controllers

**Controller Pattern**:
```java
@Configuration
public class UserController extends BaseController {
    
    private ServerResponse registerUserHandler(ServerRequest request) {
        // 1. Parse request
        RegisterUserRequest req = request.body(RegisterUserRequest.class);
        
        // 2. Validate & create command
        Result<RegisterUserCommand> commandResult = RegisterUserCommand.create(
            req.email(),
            req.password(),
            req.firstName(),
            req.lastName()
        );
        
        // 3. Handle validation errors
        if (commandResult.isFailure()) {
            ErrorDetail error = commandResult.getError().get();
            ResponseEntity<?> response = handleValidationError(
                error.getFieldErrors(),
                error.getMessage()
            );
            return ServerResponse.status(response.getStatusCode())
                .body(response.getBody());
        }
        
        // 4. Execute command
        RegisterUserCommand command = commandResult.getValue().get();
        Result<RegisterUserResponse> result = commandHandler.handle(command);
        
        // 5. Handle result
        ResponseEntity<?> response = handleResult(
            result,
            "User registered successfully",
            "user.register.success"
        );
        
        return ServerResponse.status(response.getStatusCode())
            .body(response.getBody());
    }
}
```

**Global Exception Handler** (Catches Unexpected Errors):
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex, HttpServletRequest request) {
        logError("UNHANDLED_EXCEPTION", ex, request, null);
        return ResponseEntity.internalServerError().body(
            ResponseUtils.systemError(ex.getMessage())
        );
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(
        DataIntegrityViolationException ex,
        HttpServletRequest request
    ) {
        logError("DATA_INTEGRITY_VIOLATION", ex, request, null);
        return ResponseEntity.internalServerError().body(
            ResponseUtils.systemError(ex.getMessage())
        );
    }
}
```

**When to Throw Exceptions**:
- ✅ Unexpected technical errors (database down, external API failure)
- ✅ Programming errors (null pointer, illegal state)
- ❌ Business validation errors (use `Result.failure()`)
- ❌ Expected error conditions (use `Result.failure()`)

---

### 2. Command Validation

**Always Validate in Command Factory Method**:
```java
public record CreateSubscriptionCommand(
    String userId,
    String planId,
    String paymentMethodId,
    Maybe<String> promoCode  // Optional field
) {
    public static Result<CreateSubscriptionCommand> create(
        String userId,
        String planId,
        String paymentMethodId,
        String promoCode
    ) {
        List<ErrorDetail> errors = new ArrayList<>();
        
        // Required field validation
        if (userId == null || userId.isBlank()) {
            errors.add(ErrorDetail.fieldError(
                "userId",
                "REQUIRED",
                "User ID is required",
                "subscription.userId.required"
            ));
        }
        
        if (planId == null || planId.isBlank()) {
            errors.add(ErrorDetail.fieldError(
                "planId",
                "REQUIRED",
                "Plan ID is required",
                "subscription.planId.required"
            ));
        }
        
        // Business validation
        if (!PlanValidator.isValidPlan(planId)) {
            errors.add(ErrorDetail.fieldError(
                "planId",
                "INVALID_PLAN",
                "Invalid plan selected",
                "subscription.planId.invalid"
            ));
        }
        
        if (!errors.isEmpty()) {
            return Result.failure(errors);
        }
        
        // Create command with Maybe for optional fields
        return Result.success(new CreateSubscriptionCommand(
            userId,
            planId,
            paymentMethodId,
            promoCode != null ? Maybe.some(promoCode) : Maybe.none()
        ));
    }
}
```

---

### 3. Repository Pattern

**Domain Interface** (in domain layer):
```java
public interface IUserRepository {
    Result<User> findById(String userId);
    Result<User> save(User user);
    Result<List<User>> findByRole(Bcbs239Role role);
}
```

**Infrastructure Implementation** (in infrastructure layer):
```java
@Repository
public class JpaUserRepository implements IUserRepository {
    
    private final UserEntityRepository jpaRepository;
    private final UserMapper mapper;
    
    @Override
    public Result<User> findById(String userId) {
        return jpaRepository.findById(userId)
            .map(mapper::toDomain)
            .map(Result::success)
            .orElse(Result.failure(ErrorDetail.of(
                "USER_NOT_FOUND",
                ErrorType.NOT_FOUND,
                "User not found",
                "user.not.found"
            )));
    }
    
    @Override
    public Result<User> save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return Result.success(mapper.toDomain(saved));
    }
}
```

**Mapper** (separate domain from persistence):
```java
@Component
public class UserMapper {
    public User toDomain(UserEntity entity) {
        return User.reconstitute(
            entity.getId(),
            entity.getEmail(),
            entity.getPasswordHash(),
            entity.getRole()
        );
    }
    
    public UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRole(user.getRole());
        return entity;
    }
}
```

---

### 4. Functional Routing (Controllers)

**Pattern**: Use Spring WebFlux functional routing instead of `@RestController`

```java
@Configuration
public class UserController extends BaseController {
    
    private final RegisterUserCommandHandler registerUserHandler;
    private final GetUserQueryHandler getUserHandler;
    
    @Bean
    public RouterFunction<ServerResponse> userRoutes() {
        // Public routes
        RouterFunction<ServerResponse> publicRoutes = RouterFunctions
            .route(POST("/api/v1/users/register").and(accept(APPLICATION_JSON)),
                this::registerUserHandler)
            .andRoute(POST("/api/v1/users/login").and(accept(APPLICATION_JSON)),
                this::loginHandler);
        
        // Protected routes with permissions
        RouterFunction<ServerResponse> protectedRoutes = RouterFunctions
            .route(GET("/api/v1/users/profile").and(accept(APPLICATION_JSON)),
                this::getUserProfileHandler)
            .andRoute(PUT("/api/v1/users/profile").and(accept(APPLICATION_JSON)),
                this::updateProfileHandler);
        
        // Apply security attributes
        return RouterAttributes.asPublic(publicRoutes)
            .and(RouterAttributes.withPermissions(protectedRoutes, "user:read"));
    }
    
    private ServerResponse registerUserHandler(ServerRequest request) {
        // Implementation
    }
}
```

**Benefits**:
- Type-safe routing
- Composable routes
- Easier testing
- Clear security boundaries

---

### 5. Security & Authorization

**Role-Based Access Control**:

**Roles** (defined in `Bcbs239Role.java`):
```java
public enum Bcbs239Role {
    VIEWER(1, Set.of(
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_VIEW_VIOLATIONS
    )),
    
    DATA_ANALYST(2, Set.of(
        Permission.BCBS239_UPLOAD_FILES,
        Permission.BCBS239_DOWNLOAD_FILES,
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_GENERATE_REPORTS,
        Permission.BCBS239_VALIDATE_DATA
    )),
    
    RISK_MANAGER(3, Set.of(
        Permission.BCBS239_MANAGE_VIOLATIONS,
        Permission.BCBS239_APPROVE_DATA,
        Permission.BCBS239_REJECT_DATA
        // ... more permissions
    )),
    
    SYSTEM_ADMIN(5, Set.of(
        Permission.SYSTEM_ADMIN,
        Permission.BCBS239_MANAGE_SYSTEM_CONFIG
        // ... all permissions
    ));
}
```

**Permissions** (defined in `Permission.java`):
```java
public final class Permission {
    // User Management
    public static final String USER_CREATE = "user:create";
    public static final String USER_READ = "user:read";
    
    // Billing
    public static final String BILLING_READ = "billing:read";
    public static final String BILLING_PROCESS_PAYMENT = "billing:process-payment";
    
    // BCBS239 Specific
    public static final String BCBS239_UPLOAD_FILES = "bcbs239:upload-files";
    public static final String BCBS239_GENERATE_REPORTS = "bcbs239:generate-reports";
    public static final String BCBS239_MANAGE_VIOLATIONS = "bcbs239:manage-violations";
}
```

**Applying Security to Routes**:
```java
// Public route (no authentication)
RouterAttributes.asPublic(registerRoute)

// Protected route (requires authentication)
RouterAttributes.withPermissions(profileRoute, "user:read")

// Protected route (requires multiple permissions)
RouterAttributes.withPermissions(
    adminRoute,
    "user:admin",
    "system:admin"
)
```

**Accessing Security Context**:
```java
@Component
public class GetUserProfileHandler {
    public Result<UserProfileResponse> handle() {
        // Get current user from security context
        SecurityContext securityContext = SecurityContextHolder.getContext();
        String currentUserId = securityContext.getUserId();
        Bcbs239Role role = securityContext.getRole();
        
        // Business logic
        return Result.success(new UserProfileResponse(currentUserId, role));
    }
}
```

---

### 6. Logging (ILogger with Async Structured Logging)

**Always use ILogger interface with async structured logging**

The platform uses `ILogger` from `regtech-core-domain` for all logging operations. This ensures:
- ✅ Asynchronous, non-blocking logging
- ✅ Structured data (queryable in log aggregation tools)
- ✅ Domain layer independence (no framework coupling)
- ✅ Consistent logging format across all modules

**ILogger Interface**:
```java
public interface ILogger {
    void asyncStructuredLog(String message, Map<String, Object> details);
    void asyncStructuredErrorLog(String message, Throwable throwable, Map<String, Object> details);
}
```

**Inject ILogger** (not SLF4J Logger):
```java
@Component
public class CreateInvoiceCommandHandler {
    private final ILogger asyncLogger;  // ✅ Inject ILogger
    
    public CreateInvoiceCommandHandler(ILogger asyncLogger) {
        this.asyncLogger = asyncLogger;
    }
}
```

**Success/Info Logging**:
```java
// Log successful operations with context
asyncLogger.asyncStructuredLog("INVOICE_CREATED_SUCCESSFULLY", Map.of(
    "sagaId", String.valueOf(command.sagaId()),
    "invoiceId", String.valueOf(invoiceId.value()),
    "stripeInvoiceId", String.valueOf(stripeInvoice.invoiceId()),
    "amount", String.valueOf(invoice.getAmount()),
    "currency", invoice.getCurrency()
));

asyncLogger.asyncStructuredLog("SUBSCRIPTION_ACTIVATED", Map.of(
    "sagaId", String.valueOf(command.sagaId()),
    "subscriptionId", String.valueOf(subscriptionId),
    "userId", String.valueOf(userId),
    "tier", subscriptionTier.name()
));
```

**Error Logging (without Exception)**:
```java
// Log business/validation errors from Result
Result<Invoice> invoiceCreateResult = invoiceService.create(...);

if (invoiceCreateResult.isFailure()) {
    asyncLogger.asyncStructuredErrorLog("INVOICE_CREATION_FAILED", null, Map.of(
        "sagaId", String.valueOf(command.sagaId()),
        "userId", String.valueOf(command.getUserId()),
        "error", String.valueOf(invoiceCreateResult.getError())
    ));
    return Result.failure(invoiceCreateResult.errors());
}
```

**Error Logging (with Exception)**:
```java
// Log technical errors with exception
try {
    stripeService.createCustomer(...);
} catch (StripeException e) {
    asyncLogger.asyncStructuredErrorLog("STRIPE_API_CALL_FAILED", e, Map.of(
        "sagaId", String.valueOf(command.sagaId()),
        "operation", "createCustomer",
        "userId", String.valueOf(command.getUserId()),
        "stripeErrorCode", e.getCode()
    ));
    throw e;
}
```

**Log Message Naming Conventions**:
- ✅ Use **UPPER_SNAKE_CASE** for all event names
- ✅ Be specific and descriptive
- ✅ Include the operation and outcome

**Good Examples**:
```
"CREATE_STRIPE_SUBSCRIPTION_COMMAND_RECEIVED"
"BILLING_ACCOUNT_NOT_FOUND"
"STRIPE_CUSTOMER_CREATED_SUCCESSFULLY"
"SUBSCRIPTION_ACTIVATION_FAILED"
"INVOICE_SAVED"
"PAYMENT_PROCESSED"
"SAGA_COMPLETED"
```

**Bad Examples**:
```
❌ "error" (too generic)
❌ "Customer created" (not snake case)
❌ "process" (not descriptive)
❌ "success" (which operation?)
```

**Context Map Guidelines**:

1. **Always include saga/correlation ID** (for tracing):
```java
Map.of("sagaId", String.valueOf(command.sagaId()))
```

2. **Include relevant entity IDs**:
```java
Map.of(
    "userId", String.valueOf(userId),
    "subscriptionId", String.valueOf(subscriptionId),
    "invoiceId", String.valueOf(invoiceId)
)
```

3. **Include error details from Result**:
```java
Map.of(
    "error", String.valueOf(result.getError()),
    "errorCode", result.getError().get().getCode(),
    "errorMessage", result.getError().get().getMessage()
)
```

4. **Use String.valueOf() for all values** (prevents null pointer):
```java
Map.of(
    "sagaId", String.valueOf(command.sagaId()),          // ✅ Safe
    "invoiceId", String.valueOf(invoiceId.value()),      // ✅ Safe
    "stripeInvoiceId", String.valueOf(stripeInvoice.invoiceId()) // ✅ Safe
)
```

**Complete Example**:
```java
@Component
public class CreateStripeInvoiceCommandHandler {
    private final ILogger asyncLogger;
    private final PaymentService paymentService;
    
    @Transactional
    public void handle(CreateStripeInvoiceCommand command) {
        // Log command received
        asyncLogger.asyncStructuredLog("CREATE_STRIPE_INVOICE_COMMAND_RECEIVED", Map.of(
            "sagaId", String.valueOf(command.sagaId()),
            "subscriptionId", String.valueOf(command.getSubscriptionId()),
            "userId", String.valueOf(command.getUserId())
        ));
        
        // Business logic
        Result<Invoice> invoiceResult = paymentService.createInvoice(...);
        
        if (invoiceResult.isFailure()) {
            // Log failure
            asyncLogger.asyncStructuredErrorLog("INVOICE_CREATION_FAILED", null, Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "subscriptionId", String.valueOf(command.getSubscriptionId()),
                "error", String.valueOf(invoiceResult.getError())
            ));
            return;
        }
        
        Invoice invoice = invoiceResult.getValue().get();
        
        // Log success
        asyncLogger.asyncStructuredLog("INVOICE_CREATED_SUCCESSFULLY", Map.of(
            "sagaId", String.valueOf(command.sagaId()),
            "invoiceId", String.valueOf(invoice.getId()),
            "stripeInvoiceId", String.valueOf(invoice.getStripeInvoiceId()),
            "amount", String.valueOf(invoice.getAmount())
        ));
    }
}
```

**Don't use standard SLF4J Logger**:
```java
// ❌ WRONG - Don't do this
private static final Logger logger = LoggerFactory.getLogger(SomeClass.class);

logger.info("Invoice created: {}", invoiceId);  // Not structured, not async
logger.error("Failed to create invoice", exception);  // Not structured
```

**Benefits of ILogger Pattern**:
- Asynchronous (doesn't block request threads)
- Structured (easy to query in Elasticsearch, CloudWatch, etc.)
- Consistent format across all modules
- Domain-layer compatible (no framework dependencies)
- Correlation ID automatically tracked

---

### 7. Error Handling

**Error Types**:
```java
public enum ErrorType {
    VALIDATION,      // Input validation errors
    BUSINESS,        // Business rule violations
    NOT_FOUND,       // Resource not found
    CONFLICT,        // Duplicate or conflict
    UNAUTHORIZED,    // Authentication failed
    FORBIDDEN,       // Authorization failed
    SYSTEM           // Technical/infrastructure errors
}
```

**Error Detail**:
```java
public record ErrorDetail(
    String code,
    ErrorType type,
    String message,
    String messageKey,
    Maybe<Map<String, String>> fieldErrors
) {
    public static ErrorDetail of(String code, ErrorType type, String message, String messageKey) {
        return new ErrorDetail(code, type, message, messageKey, Maybe.none());
    }
    
    public static ErrorDetail fieldError(String field, String code, String message, String messageKey) {
        return new ErrorDetail(
            code,
            ErrorType.VALIDATION,
            message,
            messageKey,
            Maybe.some(Map.of(field, message))
        );
    }
}
```

**API Response**:
```java
public record ApiResponse<T>(
    boolean success,
    Maybe<T> data,
    Maybe<String> message,
    Maybe<String> messageKey,
    Maybe<List<ErrorDetail>> errors
) {
    public static <T> ApiResponse<T> success(T data, String message, String messageKey) {
        return new ApiResponse<>(true, Maybe.some(data), Maybe.some(message), Maybe.some(messageKey), Maybe.none());
    }
    
    public static <T> ApiResponse<T> failure(List<ErrorDetail> errors) {
        return new ApiResponse<>(false, Maybe.none(), Maybe.none(), Maybe.none(), Maybe.some(errors));
    }
}
```

---

## Context Navigation

### Finding Your Way Around

**1. Start with Domain Layer**:
```
regtech-<module>/domain/src/main/java/com/bcbs239/regtech/<module>/domain/
```
- Understand core business concepts
- Review aggregates and value objects
- Check domain events

**2. Understand Capabilities (Application Layer)**:
```
regtech-<module>/application/src/main/java/com/bcbs239/regtech/<module>/application/
├── capability1/     # e.g., subscriptions
├── capability2/     # e.g., payments
└── capability3/     # e.g., invoicing
```
- Each capability = business feature
- Commands, handlers, sagas grouped by capability

**3. Check Infrastructure Implementations**:
```
regtech-<module>/infrastructure/src/main/java/com/bcbs239/regtech/<module>/infrastructure/
├── persistence/     # Repositories, entities
├── external/        # External service integrations
└── config/          # Spring configurations
```

**4. Review API Contracts (Presentation Layer)**:
```
regtech-<module>/presentation/src/main/java/com/bcbs239/regtech/<module>/presentation/
```
- Controllers and routes
- Request/Response DTOs

**5. Global Configuration**:
```
regtech-app/src/main/java/com/bcbs239/regtech/app/config/
├── GlobalExceptionHandler.java    # Centralized error handling
├── SecurityConfiguration.java      # Security setup
└── ModuleConfiguration.java        # Module orchestration
```

---

### Module Dependencies

**Dependency Graph** (Correct Architecture):
```
regtech-app (Composition Root)
    ├── depends on → regtech-iam
    ├── depends on → regtech-billing
    ├── depends on → regtech-data-quality
    └── depends on → regtech-ingestion

regtech-iam
    └── depends on → regtech-core ONLY

regtech-billing
    └── depends on → regtech-core ONLY

regtech-data-quality
    └── depends on → regtech-core ONLY

regtech-ingestion
    └── depends on → regtech-core ONLY

regtech-core
    └── No dependencies (shared kernel)
```

**⚠️ CRITICAL PRINCIPLE: Module Independence**

Business modules (IAM, Billing, Data Quality, Ingestion) must be **completely independent** of each other:
- ✅ Each module depends ONLY on `regtech-core`
- ✅ Modules communicate through **Integration Events** only
- ❌ Modules must NEVER have direct dependencies on other business modules
- ❌ No module-to-module imports (e.g., `regtech-billing` cannot import `regtech-iam`)

**Why Module Independence?**
1. **Scalability**: Each module can be extracted to a separate microservice
2. **Maintainability**: Changes in one module don't affect others
3. **Team Autonomy**: Different teams can work on different modules
4. **Testability**: Each module can be tested in isolation
5. **Deployment**: Modules can be deployed independently (future microservices)

**Cross-Module Communication**:
- Use **Integration Events** published through event bus
- Events are defined in each module's domain layer
- No direct method calls between modules
- No shared domain objects between modules
- Only `regtech-app` (composition root) orchestrates all modules

**Example of Correct Communication**:
```java
// ❌ WRONG - Direct dependency
// In regtech-billing
import com.bcbs239.regtech.iam.domain.User;  // DON'T DO THIS!

// ✅ CORRECT - Integration Event
// In regtech-iam
public class UserRegisteredEvent extends IntegrationEvent {
    private final String userId;
    private final String email;
    // ...
}

// In regtech-billing (event handler)
@EventHandler
public void handleUserRegistered(UserRegisteredEvent event) {
    // Create billing account for new user
    // Only uses event data, no direct IAM dependency
}
```

**⚠️ If You See Module-to-Module Dependencies**: This is an architecture violation and should be refactored. Replace direct dependencies with integration events.

---

## Security & Authorization

### Authentication Flow

1. User submits credentials (email/password)
2. `AuthenticationService` validates credentials
3. JWT token generated with user context
4. Token returned to client
5. Client includes token in `Authorization: Bearer <token>` header
6. Spring Security validates token
7. `SecurityContext` populated with user details

### Authorization Pattern

**Route-Level Security**:
```java
@Bean
public RouterFunction<ServerResponse> billingRoutes() {
    // Requires "billing:read" permission
    RouterFunction<ServerResponse> viewInvoices = RouterFunctions
        .route(GET("/api/v1/billing/invoices"), this::getInvoicesHandler);
    
    // Requires "billing:process-payment" permission
    RouterFunction<ServerResponse> processPayment = RouterFunctions
        .route(POST("/api/v1/billing/payments"), this::processPaymentHandler);
    
    return RouterAttributes.withPermissions(viewInvoices, "billing:read")
        .and(RouterAttributes.withPermissions(processPayment, "billing:process-payment"));
}
```

**Handler-Level Security** (business logic checks):
```java
@Component
public class ProcessPaymentHandler {
    public Result<PaymentResponse> handle(ProcessPaymentCommand command) {
        SecurityContext context = SecurityContextHolder.getContext();
        
        // Check if user owns the billing account
        if (!command.userId().equals(context.getUserId())) {
            return Result.failure(ErrorDetail.of(
                "FORBIDDEN",
                ErrorType.FORBIDDEN,
                "Cannot process payment for another user",
                "payment.forbidden"
            ));
        }
        
        // Business logic
        return Result.success(new PaymentResponse(...));
    }
}
```

---

## Event-Driven Communication

### Publishing Events

**Using BaseUnitOfWork** (Transactional Outbox Pattern):

The correct pattern for publishing both domain events and integration events is to use `BaseUnitOfWork`, which ensures:
- Events are saved in the same transaction as domain changes
- Reliable event delivery via outbox pattern
- Automatic serialization and persistence to outbox table
- Background processor handles actual event publishing

```java
@Service
public class RegisterUserCommandHandler {
    
    private final UserRepository userRepository;
    private final BaseUnitOfWork unitOfWork;  // ✅ Inject BaseUnitOfWork
    
    @Transactional
    public Result<RegisterUserResponse> handle(RegisterUserCommand command) {
        // 1. Validate and create domain object
        Result<Email> emailResult = Email.create(command.getEmail());
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError().get());
        }
        
        // 2. Create user (domain events raised in aggregate)
        User newUser = User.createWithBank(
            email,
            password,
            firstName,
            lastName,
            command.getBankId(),
            command.getPaymentMethodId()
        );
        
        // 3. Save user to repository
        Result<UserId> saveResult = userRepository.save(newUser);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }
        
        // 4. Register entity with unit of work (collects domain events)
        unitOfWork.registerEntity(newUser);
        
        // 5. Save changes (persists events to outbox in same transaction)
        unitOfWork.saveChanges();
        
        return Result.success(new RegisterUserResponse(userId));
    }
}
```

**How It Works**:
1. Domain aggregate raises events: `this.addDomainEvent(new UserRegisteredEvent(...))`
2. Handler registers entity: `unitOfWork.registerEntity(entity)`
3. `saveChanges()` collects events from entity and saves to outbox table
4. Background `OutboxProcessor` reads outbox and publishes events
5. Events delivered reliably (survives crashes, retries on failure)

**Domain Events vs Integration Events**:
- Both use the same `BaseUnitOfWork` pattern
- Difference is in event type (extends `DomainEvent` or `IntegrationEvent`)
- `OutboxProcessor` handles routing based on event type

**Example - Integration Event in Aggregate**:
```java
public class BillingAccount extends Entity {
    
    public void activate() {
        this.status = BillingAccountStatus.ACTIVE;
        this.activatedAt = Instant.now();
        
        // Raise integration event (will be published cross-module)
        this.addDomainEvent(new BillingAccountActivatedEvent(
            this.userId,
            this.id,
            this.subscriptionTier,
            this.activatedAt,
            CorrelationContext.correlationId()
        ));
    }
}
```

### Consuming Events

**Event Handler**:
```java
@Component
public class UserEventHandler {
    
    @EventHandler  // From Spring or custom annotation
    @Transactional
    public void handleBillingAccountActivated(BillingAccountActivatedEvent event) {
        // Save to inbox (idempotency)
        inboxRepository.save(InboxMessage.from(event));
        
        // Update user with billing info
        Result<User> userResult = userRepository.findById(event.getUserId());
        if (userResult.isSuccess()) {
            User user = userResult.getValue().get();
            user.activateBillingAccount(event.getBillingAccountId());
            userRepository.save(user);
        }
    }
}
```

**Idempotent Processing** (via Inbox):
```java
@Component
public class InboxProcessor {
    
    @Scheduled(fixedDelay = 5000)  // Every 5 seconds
    public void processInbox() {
        List<InboxMessage> messages = inboxRepository.findUnprocessed();
        
        for (InboxMessage message : messages) {
            try {
                // Deserialize event
                IntegrationEvent event = deserialize(message.getPayload(), message.getEventType());
                
                // Process event
                eventHandler.handle(event);
                
                // Mark as processed
                message.markProcessed();
                inboxRepository.save(message);
                
            } catch (Exception e) {
                message.incrementRetryCount();
                inboxRepository.save(message);
            }
        }
    }
}
```

---

## Step-by-Step Implementation Guide

### Implementing a New Feature

**Example**: Add "Cancel Subscription" feature to Billing module

---

#### Step 1: Domain Layer

**1.1. Add Domain Method to Aggregate**:
```java
// regtech-billing/domain/src/main/java/com/bcbs239/regtech/billing/domain/aggregates/Subscription.java

public class Subscription {
    private SubscriptionId id;
    private UserId userId;
    private SubscriptionStatus status;
    private Instant cancelledAt;
    
    /**
     * Cancel subscription
     */
    public Result<Void> cancel(String reason) {
        // Business rules
        if (status == SubscriptionStatus.CANCELLED) {
            return Result.failure(ErrorDetail.of(
                "ALREADY_CANCELLED",
                ErrorType.BUSINESS,
                "Subscription already cancelled",
                "subscription.already.cancelled"
            ));
        }
        
        if (status == SubscriptionStatus.SUSPENDED) {
            return Result.failure(ErrorDetail.of(
                "CANNOT_CANCEL_SUSPENDED",
                ErrorType.BUSINESS,
                "Cannot cancel suspended subscription",
                "subscription.cannot.cancel.suspended"
            ));
        }
        
        // Apply change
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        
        // Raise domain event
        this.addDomainEvent(new SubscriptionCancelledEvent(
            this.id.getValue(),
            this.userId.getValue(),
            reason,
            Instant.now()
        ));
        
        return Result.success();
    }
}
```

**1.2. Create Domain Event**:
```java
// regtech-billing/domain/src/main/java/com/bcbs239/regtech/billing/domain/events/SubscriptionCancelledEvent.java

public class SubscriptionCancelledEvent extends DomainEvent {
    private final String subscriptionId;
    private final String userId;
    private final String reason;
    private final Instant cancelledAt;
    
    public SubscriptionCancelledEvent(
        String subscriptionId,
        String userId,
        String reason,
        Instant cancelledAt
    ) {
        super(UUID.randomUUID().toString(), "SubscriptionCancelled");
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.reason = reason;
        this.cancelledAt = cancelledAt;
    }
    
    @Override
    public String eventType() {
        return "SubscriptionCancelled";
    }
}
```

---

#### Step 2: Application Layer

**2.1. Create Command**:
```java
// regtech-billing/application/src/main/java/com/bcbs239/regtech/billing/application/subscriptions/CancelSubscriptionCommand.java

public record CancelSubscriptionCommand(
    String subscriptionId,
    String userId,
    String reason
) {
    public static Result<CancelSubscriptionCommand> create(
        String subscriptionId,
        String userId,
        String reason
    ) {
        List<ErrorDetail> errors = new ArrayList<>();
        
        if (subscriptionId == null || subscriptionId.isBlank()) {
            errors.add(ErrorDetail.fieldError(
                "subscriptionId",
                "REQUIRED",
                "Subscription ID is required",
                "subscription.id.required"
            ));
        }
        
        if (userId == null || userId.isBlank()) {
            errors.add(ErrorDetail.fieldError(
                "userId",
                "REQUIRED",
                "User ID is required",
                "subscription.userId.required"
            ));
        }
        
        if (!errors.isEmpty()) {
            return Result.failure(errors);
        }
        
        return Result.success(new CancelSubscriptionCommand(
            subscriptionId,
            userId,
            reason
        ));
    }
}
```

**2.2. Create Command Handler**:
```java
// regtech-billing/application/src/main/java/com/bcbs239/regtech/billing/application/subscriptions/CancelSubscriptionCommandHandler.java

@Component
public class CancelSubscriptionCommandHandler {
    
    private final ISubscriptionRepository subscriptionRepository;
    private final BaseUnitOfWork unitOfWork;  // ✅ Use BaseUnitOfWork
    
    public CancelSubscriptionCommandHandler(
        ISubscriptionRepository subscriptionRepository,
        BaseUnitOfWork unitOfWork
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.unitOfWork = unitOfWork;
    }
    
    @Transactional
    public Result<Void> handle(CancelSubscriptionCommand command) {
        // 1. Retrieve subscription
        Result<Subscription> subscriptionResult = subscriptionRepository.findById(
            new SubscriptionId(command.subscriptionId())
        );
        
        if (subscriptionResult.isFailure()) {
            return Result.failure(subscriptionResult.errors());
        }
        
        Subscription subscription = subscriptionResult.getValue().get();
        
        // 2. Verify ownership
        if (!subscription.getUserId().getValue().equals(command.userId())) {
            return Result.failure(ErrorDetail.of(
                "FORBIDDEN",
                ErrorType.FORBIDDEN,
                "Cannot cancel another user's subscription",
                "subscription.cancel.forbidden"
            ));
        }
        
        // 3. Cancel subscription (business logic in domain)
        // This will raise SubscriptionCancelledEvent in the aggregate
        Result<Void> cancelResult = subscription.cancel(command.reason());
        if (cancelResult.isFailure()) {
            return cancelResult;
        }
        
        // 4. Save to repository
        subscriptionRepository.save(subscription);
        
        // 5. Register with unit of work (collects domain events)
        unitOfWork.registerEntity(subscription);
        
        // 6. Save changes (persists events to outbox)
        unitOfWork.saveChanges();
        
        return Result.success();
    }
}
```

---

#### Step 3: Infrastructure Layer

**3.1. Update Repository Implementation** (if needed):
```java
// regtech-billing/infrastructure/src/main/java/com/bcbs239/regtech/billing/infrastructure/persistence/repositories/JpaSubscriptionRepository.java

@Repository
public class JpaSubscriptionRepository implements ISubscriptionRepository {
    
    private final SubscriptionEntityRepository jpaRepository;
    private final SubscriptionMapper mapper;
    
    @Override
    public Result<Subscription> findById(SubscriptionId id) {
        return jpaRepository.findById(id.getValue())
            .map(mapper::toDomain)
            .map(Result::success)
            .orElse(Result.failure(ErrorDetail.of(
                "SUBSCRIPTION_NOT_FOUND",
                ErrorType.NOT_FOUND,
                "Subscription not found",
                "subscription.not.found"
            )));
    }
    
    @Override
    public Result<Subscription> save(Subscription subscription) {
        SubscriptionEntity entity = mapper.toEntity(subscription);
        SubscriptionEntity saved = jpaRepository.save(entity);
        return Result.success(mapper.toDomain(saved));
    }
}
```

**3.2. Add External Service Call** (if needed):
```java
// regtech-billing/infrastructure/src/main/java/com/bcbs239/regtech/billing/infrastructure/external/stripe/StripeSubscriptionService.java

@Service
public class StripeSubscriptionService {
    
    private final StripeClient stripeClient;
    
    public Result<Void> cancelStripeSubscription(String stripeSubscriptionId) {
        try {
            com.stripe.model.Subscription stripeSubscription = 
                com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
            
            stripeSubscription.cancel();
            
            return Result.success();
            
        } catch (StripeException e) {
            return Result.failure(ErrorDetail.of(
                "STRIPE_ERROR",
                ErrorType.SYSTEM,
                "Failed to cancel Stripe subscription: " + e.getMessage(),
                "stripe.cancel.failed"
            ));
        }
    }
}
```

**3.3. Update Handler with External Call**:
```java
@Component
public class CancelSubscriptionCommandHandler {
    
    private final ISubscriptionRepository subscriptionRepository;
    private final StripeSubscriptionService stripeService;
    private final BaseUnitOfWork unitOfWork;
    
    @Transactional
    public Result<Void> handle(CancelSubscriptionCommand command) {
        // ... retrieve and verify subscription ...
        
        // Cancel in Stripe first (external call)
        if (subscription.getStripeSubscriptionId() != null) {
            Result<Void> stripeCancelResult = stripeService.cancelStripeSubscription(
                subscription.getStripeSubscriptionId()
            );
            
            if (stripeCancelResult.isFailure()) {
                return stripeCancelResult;  // Fail fast
            }
        }
        
        // Cancel subscription (raises domain event)
        Result<Void> cancelResult = subscription.cancel(command.reason());
        if (cancelResult.isFailure()) {
            return cancelResult;
        }
        
        // Save and publish events
        subscriptionRepository.save(subscription);
        unitOfWork.registerEntity(subscription);
        unitOfWork.saveChanges();
        
        return Result.success();
    }
}
```

---

#### Step 4: Presentation Layer

**4.1. Create Request DTO**:
```java
// regtech-billing/presentation/src/main/java/com/bcbs239/regtech/billing/presentation/subscriptions/CancelSubscriptionRequest.java

public record CancelSubscriptionRequest(
    String subscriptionId,
    String reason
) {}
```

**4.2. Add Controller Route**:
```java
// regtech-billing/presentation/src/main/java/com/bcbs239/regtech/billing/presentation/subscriptions/SubscriptionController.java

@Configuration
public class SubscriptionController extends BaseController {
    
    private final CancelSubscriptionCommandHandler cancelHandler;
    
    @Bean
    public RouterFunction<ServerResponse> subscriptionRoutes() {
        // Cancel subscription route
        RouterFunction<ServerResponse> cancelRoute = RouterFunctions
            .route(
                POST("/api/v1/subscriptions/cancel").and(accept(APPLICATION_JSON)),
                this::cancelSubscriptionHandler
            );
        
        // Apply permission
        return RouterAttributes.withPermissions(
            cancelRoute,
            Permission.BILLING_MANAGE_SUBSCRIPTIONS
        );
    }
    
    private ServerResponse cancelSubscriptionHandler(ServerRequest request) {
        // 1. Parse request
        CancelSubscriptionRequest req = request.body(CancelSubscriptionRequest.class);
        
        // 2. Get current user
        SecurityContext securityContext = SecurityContextHolder.getContext();
        String userId = securityContext.getUserId();
        
        // 3. Create command
        Result<CancelSubscriptionCommand> commandResult = CancelSubscriptionCommand.create(
            req.subscriptionId(),
            userId,
            req.reason()
        );
        
        if (commandResult.isFailure()) {
            ErrorDetail error = commandResult.getError().get();
            ResponseEntity<?> response = handleValidationError(
                error.getFieldErrors(),
                error.getMessage()
            );
            return ServerResponse.status(response.getStatusCode())
                .body(response.getBody());
        }
        
        // 4. Execute command
        CancelSubscriptionCommand command = commandResult.getValue().get();
        Result<Void> result = cancelHandler.handle(command);
        
        // 5. Handle result
        ResponseEntity<?> response = handleResult(
            result,
            "Subscription cancelled successfully",
            "subscription.cancel.success"
        );
        
        return ServerResponse.status(response.getStatusCode())
            .body(response.getBody());
    }
}
```

---

#### Step 5: Testing

**5.1. Unit Test - Domain**:
```java
@Test
void shouldCancelActiveSubscription() {
    // Given
    Subscription subscription = Subscription.create(...);
    
    // When
    Result<Void> result = subscription.cancel("User request");
    
    // Then
    assertTrue(result.isSuccess());
    assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
    assertNotNull(subscription.getCancelledAt());
}

@Test
void shouldNotCancelAlreadyCancelledSubscription() {
    // Given
    Subscription subscription = Subscription.create(...);
    subscription.cancel("First cancellation");
    
    // When
    Result<Void> result = subscription.cancel("Second cancellation");
    
    // Then
    assertTrue(result.isFailure());
    assertEquals("ALREADY_CANCELLED", result.getError().get().getCode());
}
```

**5.2. Integration Test - Handler**:
```java
@SpringBootTest
@Transactional
class CancelSubscriptionCommandHandlerTest {
    
    @Autowired
    private CancelSubscriptionCommandHandler handler;
    
    @Autowired
    private ISubscriptionRepository repository;
    
    @Test
    void shouldCancelSubscription() {
        // Given
        Subscription subscription = createTestSubscription();
        repository.save(subscription);
        
        CancelSubscriptionCommand command = CancelSubscriptionCommand.create(
            subscription.getId().getValue(),
            subscription.getUserId().getValue(),
            "Test cancellation"
        ).getValue().get();
        
        // When
        Result<Void> result = handler.handle(command);
        
        // Then
        assertTrue(result.isSuccess());
        
        Result<Subscription> updatedResult = repository.findById(subscription.getId());
        Subscription updated = updatedResult.getValue().get();
        assertEquals(SubscriptionStatus.CANCELLED, updated.getStatus());
    }
}
```

---

### Summary Checklist

- ✅ **Domain**: Business logic in aggregate, domain event created
- ✅ **Application**: Command, command handler, validation
- ✅ **Infrastructure**: Repository implementation, external service integration
- ✅ **Presentation**: Request DTO, controller route, permission applied
- ✅ **Testing**: Unit tests (domain), integration tests (handler)
- ✅ **Documentation**: Update API docs, add to capability README

---

## Architecture Decisions

### Why Clean Architecture?

**Benefits**:
- Business logic isolated from frameworks
- Testable without infrastructure
- Technology-agnostic core
- Easy to migrate frameworks

**Trade-offs**:
- More initial boilerplate
- Requires discipline to maintain boundaries
- Steeper learning curve

---

### Why DDD?

**Benefits**:
- Ubiquitous language (business speaks code)
- Clear bounded contexts
- Rich domain models
- Event-driven insights

**Trade-offs**:
- Requires domain expertise
- More complex than CRUD
- Overhead for simple use cases

---

### Why Event-Driven?

**Benefits**:
- Decoupled modules
- Audit trail (event sourcing ready)
- Reactive to business changes
- Scalable (async processing)

**Trade-offs**:
- Eventual consistency
- Complex debugging
- Requires monitoring

---

### Why Fail-Fast?

**Benefits**:
- Clear error boundaries
- No hidden failures
- Easier debugging
- Centralized error handling

**Trade-offs**:
- Verbose validation code
- Must handle all error paths

---

### Why Functional Routing?

**Benefits**:
- Type-safe routes
- Composable
- Explicit security
- Easier testing

**Trade-offs**:
- Less familiar than `@RestController`
- More verbose

---

### Why Multi-Module Maven?

**Benefits**:
- Clear layer boundaries
- Independent compilation
- Dependency enforcement
- Team-friendly (parallel development)

**Trade-offs**:
- Complex build configuration
- Slower initial setup

---

## Getting Started

### 1. Clone Repository
```bash
git clone <repository-url>
cd regtech
```

### 2. Build Project
```bash
mvn clean install -DskipTests
```

### 3. Run Docker Services
```bash
docker-compose up -d
```

### 4. Run Application
```bash
mvn -pl regtech-app spring-boot:run
```

### 5. Access Application
- API: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`

---

## Additional Resources

- **SAGA_IMPLEMENTATION_GUIDE.md** - Detailed saga pattern implementation
- **LAYERED_ARCHITECTURE_REORGANIZATION.md** - Billing module restructuring
- **API_ENDPOINTS.md** - API documentation
- **README-DEV.md** - Development setup

---

## Questions & Support

For questions or issues:
1. Review this guide and module-specific READMEs
2. Check architecture decision records (ADRs)
3. Review existing implementations
4. Consult team lead or architect

---

**Happy Coding! 🚀**
