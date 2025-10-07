# RegTech Application Architecture Documentation

## Overview

The RegTech application is built using a **Modular Monolithic Architecture** with **Domain-Driven Design (DDD)** principles, **Functional Programming** patterns, and **Saga-based distributed transactions**. The system emphasizes **testability**, **maintainability**, and **scalability** through the use of **closures**, **Result types**, and **event-driven communication**.

## 1. Code Organization

### Project Structure
```
regtech/
├── pom.xml                           # Parent POM with modules
├── regtech-core/                     # Shared core module
├── regtech-iam/                      # Identity & Access Management
├── regtech-billing/                  # Billing module
└── docs/                             # Documentation
```

### Module Organization (DDD Layers)
Each module follows **Domain-Driven Design** with clear architectural layers:

```
regtech-[module]/
├── [Module]Module.java              # Module configuration
├── api/                             # REST API controllers
├── application/                     # Use cases & command handlers
│   └── [usecase]/                   # CQRS command/query handlers
├── domain/                          # Domain model & business logic
│   ├── [aggregate]/                 # Domain aggregates & entities
│   └── [aggregate]Repository.java   # Repository interfaces/contracts
└── infrastructure/                  # External concerns
    ├── persistence/                 # JPA entities & repositories
    ├── security/                    # Security configurations
    ├── oauth2/                      # OAuth2 integrations
    └── health/                      # Health indicators
```

### Core Module Structure
```
regtech-core/
├── shared/                          # Shared functional types
│   ├── Result.java                  # Railway-oriented programming
│   ├── Maybe.java                   # Functional optional type
│   ├── ErrorDetail.java             # Structured error handling
│   └── CorrelationId.java           # Distributed tracing
├── saga/                            # Saga orchestration framework
│   ├── Saga.java                    # Saga interface
│   ├── SagaOrchestrator.java        # Saga execution engine
│   ├── SagaClosures.java            # Functional saga operations
│   └── MessageBus.java              # Inter-saga communication
├── events/                          # Cross-module events
│   ├── CrossModuleEventBus.java     # Event publishing
│   └── BaseEvent.java               # Event base classes
├── config/                          # Shared configurations
│   ├── ModularJpaConfiguration.java # Multi-module JPA
│   ├── SharedTransactionConfiguration.java
│   └── LoggingConfiguration.java
├── health/                          # Health monitoring
└── web/                             # Web utilities
```

## 2. Functional Programming with Closures

### Core Principles
The application uses **closures** extensively for:
- **Dependency Injection**: All external dependencies are injected as functions
- **Testability**: Pure functions with mockable closures
- **Immutability**: Functional data structures and operations
- **Composition**: Function composition for complex operations

### Key Functional Types

#### Result<T> - Railway-Oriented Programming
```java
public class Result<T> {
    // Success/Failure pattern without exceptions
    public static <T> Result<T> success(T value) { ... }
    public static <T> Result<T> failure(ErrorDetail error) { ... }

    // Functional operations
    public <U> Result<U> map(Function<T, U> mapper) { ... }
    public <U> Result<U> flatMap(Function<T, Result<U>> mapper) { ... }
}
```

#### Maybe<T> - Functional Optional
```java
public sealed interface Maybe<T> {
    record Some<T>(T value) implements Maybe<T> { ... }
    record None<T>() implements Maybe<T> { ... }
}
```

#### Repository Closures
```java
@Repository
public class UserRepository {
    // Functional repository operations
    public Function<Email, Maybe<User>> emailLookup() {
        return email -> { /* JPA query logic */ };
    }

    public Function<User, Result<UserId>> userSaver() {
        return user -> { /* Persistence logic */ };
    }
}
```

### Command Handler with Closures
```java
@Component
public class RegisterUserCommandHandler {

    public Result<RegisterUserResponse> handle(RegisterUserCommand command) {
        // Inject repository operations as closures
        return registerUser(command,
            userRepository.emailLookup(),
            userRepository.userSaver());
    }

    // Pure function with injected dependencies
    static Result<RegisterUserResponse> registerUser(
            RegisterUserCommand command,
            Function<Email, Maybe<User>> emailLookup,
            Function<User, Result<UserId>> userSaver) {
        // Pure business logic with no side effects
    }
}
```

## 3. Modular Architecture

### Module Isolation
- **Package-based boundaries**: Each module in its own package namespace
- **Controlled dependencies**: Modules only depend on core shared components
- **Independent deployment**: Modules can be developed and tested separately

### Shared Infrastructure
```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.bcbs239.regtech.core",    // Shared components
    "com.bcbs239.regtech.iam",     // IAM module
    "com.bcbs239.regtech.billing"  // Billing module
})
public class RegtechApplication { ... }
```

### Cross-Module Communication
```java
@Component
public class CrossModuleEventBus {
    @Async
    public void publishEvent(Object event) {
        // Asynchronous event publishing
        eventPublisher.publishEvent(event);
    }
}
```

### JPA Configuration Across Modules
```java
@Configuration
@EntityScan(basePackages = {
    "com.bcbs239.regtech.iam.domain",
    "com.bcbs239.regtech.billing.domain"
})
@EnableJpaRepositories(basePackages = {
    "com.bcbs239.regtech.iam.infrastructure.repository",
    "com.bcbs239.regtech.billing.infrastructure.repository"
})
public class ModularJpaConfiguration { }
```

## 4. Component Connections

### Application Flow
```
REST API → Controller → Command Handler → Domain Service → Repository → Database
     ↓           ↓            ↓            ↓           ↓           ↓
   Result     Result       Result       Maybe      Result       void
```

### Saga Integration
```
Command Handler → Saga Orchestrator → Message Bus → Other Bounded Contexts
      ↓               ↓                    ↓               ↓
   CorrelationId   SagaResult         SagaMessage     Event
```

### Event Flow
```
Domain Event → Event Bus → Event Handlers → Side Effects
     ↓            ↓            ↓            ↓
  Immutable    Async       Transaction    Database
```

## 5. Core Module Deep Dive

### Shared Types Architecture

#### ErrorDetail - Structured Error Handling
```java
public class ErrorDetail {
    private final String code;        // Machine-readable error code
    private final String message;     // Human-readable message
    private final String messageKey;  // I18N key
    private final List<FieldError> fieldErrors; // Validation errors
}
```

#### CorrelationId - Distributed Tracing
```java
public class CorrelationId {
    private final String id;

    public static CorrelationId generate() {
        return new CorrelationId(UUID.randomUUID().toString());
    }
}
```

### Saga Framework Architecture

#### SagaClosures - Functional Saga Operations
```java
public interface SagaClosures {
    // IO Operations (mockable)
    @FunctionalInterface interface MessagePublisher { void publish(SagaMessage msg); }
    @FunctionalInterface interface SagaDataSaver { void save(SagaData data); }

    // Business Operations (mockable)
    @FunctionalInterface interface SagaExecutor<T extends SagaData> {
        SagaResult execute(T sagaData);
    }

    // System Operations (mockable)
    @FunctionalInterface interface TimeoutScheduler {
        String schedule(String sagaId, String type, long delay, Runnable callback);
    }
}
```

#### Saga Orchestrator - Execution Engine
```java
@Component
public class SagaOrchestrator {

    public <T extends SagaData> CompletableFuture<SagaResult> startSaga(
            Saga<T> saga, T sagaData) {
        // 1. Initialize saga state
        // 2. Execute saga logic asynchronously
        // 3. Handle success/compensation
        // 4. Monitor and log
    }

    public void handleMessage(SagaMessage message) {
        // Handle cross-context messages
    }
}
```

### Configuration Architecture

#### Modular JPA Configuration
- **Entity Scanning**: Discovers entities across all modules
- **Repository Scanning**: Enables repositories in infrastructure layers
- **Schema Management**: Flyway migrations per module

#### Shared Transaction Management
- **Single Transaction Manager**: All modules share transaction context
- **Cross-Module Transactions**: Distributed operations stay atomic
- **Rollback Coordination**: Consistent error handling

## 6. Saga Pattern Implementation

### Saga Fundamentals
**Sagas** coordinate distributed transactions across bounded contexts using:
- **Eventual Consistency**: No two-phase commit
- **Compensation**: Rollback through compensating actions
- **Timeouts**: Business-level timeout handling
- **Monitoring**: Comprehensive saga lifecycle tracking

### Saga Lifecycle
```
STARTED → EXECUTING → COMPLETED
    ↓         ↓
FAILED → COMPENSATING → COMPENSATED
```

### Saga Implementation Example
```java
public interface Saga<T extends SagaData> {
    SagaResult execute(T sagaData);
    SagaResult handleMessage(T sagaData, SagaMessage message);
    SagaResult compensate(T sagaData);
}

public class UserRegistrationSaga implements Saga<UserRegistrationData> {
    @Override
    public SagaResult execute(UserRegistrationData data) {
        // 1. Create user in IAM
        // 2. Send welcome email (async)
        // 3. Create billing account
        // 4. Send confirmation
    }

    @Override
    public SagaResult compensate(UserRegistrationData data) {
        // Rollback: delete user, cancel email, remove billing account
    }
}
```

### Saga Orchestrator Responsibilities
- **Execution Management**: Async saga execution with virtual threads
- **Message Handling**: Cross-context communication
- **Timeout Management**: Business timeout scheduling/cancellation
- **Compensation Coordination**: Failure recovery orchestration
- **Monitoring Integration**: Lifecycle event recording

### Saga Data Management
```java
public abstract class SagaData {
    protected String sagaId;
    protected SagaStatus status;
    protected Instant startedAt;
    protected Instant completedAt;
    protected Map<String, String> metadata;

    public enum SagaStatus {
        STARTED, EXECUTING, COMPLETED,
        COMPENSATING, COMPENSATED, COMPENSATION_FAILED
    }
}
```

### Message Bus Integration
```java
public interface MessageBus {
    void publish(SagaMessage message);
    void subscribe(String messageType, Consumer<SagaMessage> handler);
}

public class SagaMessage {
    private final String sagaId;
    private final String type;
    private final Object payload;
    private final String source;
    private final String target;
}
```

## Architecture Benefits

### Testability
- **Pure Functions**: Business logic isolated from side effects
- **Mockable Closures**: All dependencies injectable as functions
- **Functional Assertions**: Railway-oriented result testing

### Maintainability
- **Clear Boundaries**: Module isolation prevents coupling
- **DDD Structure**: Business logic organized by domain
- **Functional Patterns**: Immutable operations, no side effects

### Scalability
- **Modular Design**: Independent module development/deployment
- **Event-Driven**: Asynchronous cross-module communication
- **Saga Coordination**: Distributed transaction management

### Reliability
- **Result Pattern**: Explicit error handling without exceptions
- **Saga Compensation**: Reliable failure recovery
- **Correlation Tracking**: End-to-end request tracing

## Development Workflow

### Adding New Modules
1. Create `regtech-[name]` module
2. Add to parent POM modules
3. Add to main application `@ComponentScan`
4. Configure JPA scanning
5. Implement domain, application, infrastructure layers

### Adding New Sagas
1. Implement `Saga<T>` interface
2. Define `SagaData` subclass
3. Register with `SagaOrchestrator`
4. Define compensating actions
5. Add monitoring integration

### Testing Strategy
- **Unit Tests**: Pure functions with mocked closures
- **Integration Tests**: Full stack with real dependencies
- **Saga Tests**: End-to-end saga execution testing
- **Contract Tests**: Cross-module interface verification

This architecture provides a solid foundation for building complex, distributed business applications with strong guarantees around reliability, testability, and maintainability.</content>
<parameter name="filePath">c:\Users\alseny\Desktop\react projects\regtech\ARCHITECTURE.md