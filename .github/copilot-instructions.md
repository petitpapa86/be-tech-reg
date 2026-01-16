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

### Error Handling Strategy: Exceptions vs Result Pattern

**Critical Principle**: We distinguish between **expected errors** (validation failures) and **unexpected exceptions** (system failures).

#### Exceptions vs Errors

**Exceptions** = Unexpected system failures (let them propagate):
- Database connection failures
- Network timeouts
- File system errors
- External API failures
- NullPointerException, IllegalStateException, etc.

**Errors** = Expected validation/business rule failures (return Result.failure):
- Invalid email format
- Missing required field
- Business rule violation (e.g., duplicate user)
- Authorization failure

#### Rule: Never Catch Exceptions (Except at Boundaries)

```java
// ❌ WRONG - Don't catch exceptions in domain/application/infrastructure
public Result<StorageResult> uploadToS3(String content, StorageUri uri) {
    try {
        String bucket = uri.getBucket();
        String key = uri.getKey();
        coreS3Service.putString(bucket, key, content, "application/json", metadata, null);
        return Result.success(buildStorageResult(uri, content.length()));
    } catch (Exception e) {  // ❌ Don't catch!
        log.error("S3 upload failed: {}", e.getMessage(), e);
        return Result.failure(ErrorDetail.of(...));
    }
}

// ✅ CORRECT - Let exceptions propagate to GlobalExceptionHandler
public Result<StorageResult> uploadToS3(String content, StorageUri uri) {
    // Validate inputs (expected errors)
    if (uri.getBucket() == null || uri.getKey() == null) {
        return Result.failure(
            ErrorDetail.of("INVALID_S3_URI", ErrorType.VALIDATION_ERROR, 
                "Invalid S3 URI: " + uri, "storage.invalid_s3_uri"));
    }
    
    // Let exceptions propagate (unexpected system errors)
    coreS3Service.putString(uri.getBucket(), uri.getKey(), content, 
        "application/json", metadata, null);
    
    return Result.success(buildStorageResult(uri, content.length()));
}
```

**Why?** GlobalExceptionHandler (`regtech-app/src/main/java/com/bcbs239/regtech/app/config/GlobalExceptionHandler.java`) handles ALL exceptions at the controller boundary with proper logging, metrics, and error responses.

#### Result Pattern Usage

**When to use Result.failure()**: Only for **expected validation errors**

```java
// ✅ CORRECT - Domain validation returns Result
public static Result<Email> of(String value) {
    if (value == null || value.isBlank()) {
        return Result.failure(
            ErrorDetail.of("EMAIL_REQUIRED", ErrorType.VALIDATION_ERROR, 
                "Email is required", "validation.email_required"));
    }
    if (!value.matches(EMAIL_PATTERN)) {
        return Result.failure(
            ErrorDetail.of("EMAIL_INVALID_FORMAT", ErrorType.VALIDATION_ERROR, 
                "Invalid email format", "validation.email_invalid_format"));
    }
    return Result.success(new Email(value));
}

// ✅ CORRECT - Application layer propagates validation failures
public Result<User> handle(CreateUserCommand cmd) {
    Result<Email> emailResult = Email.of(cmd.email());
    if (emailResult.isFailure()) {
        return Result.failure(emailResult.getError());
    }
    
    // Let exceptions from repository propagate (DB errors)
    userRepository.save(user);  // IOException, SQLException, etc. propagate
    
    return Result.success(user);
}
```

**ErrorDetail Structure**: Always use all 4 parameters

```java
ErrorDetail.of(
    "ERROR_CODE",              // Unique code for this error
    ErrorType.VALIDATION_ERROR, // VALIDATION_ERROR, BUSINESS_RULE_ERROR, SYSTEM_ERROR, etc.
    "Human readable message",   // Display to user
    "i18n.message.key"         // Translation key
)
```

**Available ErrorTypes** (from `ErrorType` enum):
- `VALIDATION_ERROR` - Input validation failures
- `BUSINESS_RULE_ERROR` - Business logic violations
- `SYSTEM_ERROR` - System/infrastructure failures
- `AUTHENTICATION_ERROR` - Auth failures
- `NOT_FOUND_ERROR` - Resource not found

### Value Objects (Domain Validation)

Use **value objects** to ensure type safety and validation at construction:

```java
// ✅ CORRECT - Value object with static factory method
public record Email(String value) {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    
    // Static factory method that validates
    public static Result<Email> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(
                ErrorDetail.of("EMAIL_REQUIRED", ErrorType.VALIDATION_ERROR, 
                    "Email is required", "validation.email_required"));
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            return Result.failure(
                ErrorDetail.of("EMAIL_INVALID_FORMAT", ErrorType.VALIDATION_ERROR, 
                    "Invalid email format", "validation.email_invalid_format"));
        }
        return Result.success(new Email(value));
    }
    
    // Compact constructor for internal use only (private would be ideal)
    public Email {
        // No validation here - already validated in of()
    }
}

// ✅ Usage in application layer
public Result<User> createUser(CreateUserCommand cmd) {
    Result<Email> emailResult = Email.of(cmd.email());
    if (emailResult.isFailure()) {
        return Result.failure(emailResult.getError());
    }
    
    Email email = emailResult.getValueOrThrow(); // Safe - already checked
    User user = User.create(email, cmd.firstName(), cmd.lastName());
    
    userRepository.save(user);  // Exceptions propagate
    return Result.success(user);
}
```

**Value Object Naming Conventions**:
- `of()` - Validates and constructs from primitive types
- `from()` - Converts from another type (e.g., `UserId.from(UUID)`)
- `parse()` - Parses from string representation

**Value Object Constants**: Use value objects for default constants, not primitives

```java
// ❌ WRONG - Raw primitive constants
private static final double DEFAULT_COMPLETENESS = 0.95;
private static final double DEFAULT_ACCURACY = 0.05;

// ✅ CORRECT - Value object constants
private static final CompletenessThreshold DEFAULT_COMPLETENESS = 
    CompletenessThreshold.of(0.95).getValueOrThrow();
private static final AccuracyThreshold DEFAULT_ACCURACY = 
    AccuracyThreshold.of(0.05).getValueOrThrow();

// Usage
return new QualityThreshold(
    bankId,
    DEFAULT_COMPLETENESS.value(),  // Extract validated value
    DEFAULT_ACCURACY.value()
);
```

**Benefits**: Constants are validated at class-load time, providing compile-time safety.

### Value Object Usage Across Layers

Value objects should flow through all layers to maintain type safety and validation:

#### 1. Domain Layer: Define Value Objects
```java
// ✅ Domain value object with validation
public record BankId(String value) {
    
    public static Result<BankId> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(
                ErrorDetail.of("BANK_ID_REQUIRED", ErrorType.VALIDATION_ERROR,
                    "Bank ID is required", "validation.bank_id_required"));
        }
        return Result.success(new BankId(value));
    }
}

public record CompletenessThreshold(double value) {
    
    public static Result<CompletenessThreshold> of(double value) {
        if (value < 0.0 || value > 1.0) {
            return Result.failure(
                ErrorDetail.of("INVALID_COMPLETENESS_THRESHOLD", ErrorType.VALIDATION_ERROR,
                    "Completeness threshold must be between 0.0 and 1.0",
                    "validation.completeness_threshold.out_of_range"));
        }
        return Result.success(new CompletenessThreshold(value));
    }
    
    public double asPercentage() {
        return value * 100.0;
    }
}
```

#### 2. Application Layer: Use Value Objects in Commands/Queries
```java
// ✅ Command with value object
public record UpdateConfigurationCommand(
    BankId bankId,  // Value object, not String
    ConfigurationDto configuration
) {}

// ✅ Handler validates and uses value objects
@Component
public class UpdateConfigurationCommandHandler {
    
    public Result<ConfigurationDto> handle(UpdateConfigurationCommand command) {
        // Value object already validated in command creation
        String bankIdStr = command.bankId().value(); // Extract primitive for repository
        
        // Validate threshold value objects
        Result<CompletenessThreshold> completenessResult = 
            CompletenessThreshold.of(command.configuration().thresholds().completeness());
        
        if (completenessResult.isFailure()) {
            return Result.failure(completenessResult.getError().orElseThrow());
        }
        
        CompletenessThreshold completeness = completenessResult.getValueOrThrow();
        
        // Use validated value
        QualityThreshold threshold = new QualityThreshold(
            bankIdStr,
            completeness.value(),  // Extract primitive
            // ... other thresholds
        );
        
        thresholdRepository.save(threshold);
        return Result.success(config);
    }
}
```

#### 3. Presentation Layer: Validate at Boundary
```java
// ✅ Controller validates value objects from request parameters
@Component
public class DataQualityConfigController {
    
    public ServerResponse getConfiguration(ServerRequest request) {
        // Extract String from request parameter
        String bankIdStr = request.param("bankId").orElse("default-bank");
        
        // Validate and create value object
        Result<BankId> bankIdResult = BankId.of(bankIdStr);
        if (bankIdResult.isFailure()) {
            return responseHandler.handleErrorResponse(
                bankIdResult.getError().orElseThrow()
            );
        }
        
        // Pass value object to command/query
        GetConfigurationQuery query = new GetConfigurationQuery(
            bankIdResult.getValueOrThrow()  // Value object
        );
        
        Result<ConfigurationDto> result = queryHandler.handle(query);
        return responseHandler.handleSuccessResult(result, "Success", "config.retrieved");
    }
}
```

#### 4. Infrastructure Layer: Extract Primitives for Repositories
```java
// ✅ Handler extracts primitive values for repository calls
@Component
public class GetConfigurationQueryHandler {
    
    public Result<ConfigurationDto> handle(GetConfigurationQuery query) {
        // Extract primitive from value object for repository
        String bankIdStr = query.bankId().value();
        
        QualityThreshold threshold = thresholdRepository
            .findByBankId(bankIdStr)  // Repository uses String
            .orElseGet(() -> getDefaultThresholds(query.bankId()));
        
        return Result.success(config);
    }
    
    private QualityThreshold getDefaultThresholds(BankId bankId) {
        return new QualityThreshold(
            bankId.value(),  // Extract String
            DEFAULT_COMPLETENESS.value(),  // Extract double
            DEFAULT_ACCURACY.value()
        );
    }
}
```

#### Value Object Flow Summary
```
HTTP Request (String) 
  → Controller validates → Result<BankId>
    → Command/Query (BankId value object)
      → Handler uses value object → Extract .value() for repository
        → Repository (String primitive)
          → Database
```

**Key Rules**:
1. **Create value objects at boundaries** (controller, from external input)
2. **Pass value objects through layers** (commands, queries, handlers)
3. **Extract primitives only when needed** (repository calls, database persistence)
4. **Never skip validation** - always check `isFailure()` before `getValueOrThrow()`
5. **Use value object constants** for defaults, not raw primitives

### Clean Boundaries Pattern

**Controllers** validate input, then **internal layers work with clean data**:

```java
// ✅ CORRECT - Controller validates at boundary
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        // 1. Controller validates HTTP input (Spring validation)
        // 2. Convert to command (clean DTO)
        CreateUserCommand command = CreateUserCommand.from(request);
        
        // 3. Application layer receives clean data
        Result<User> result = createUserUseCase.execute(command);
        
        // 4. Convert Result to HTTP response
        return result.isSuccess()
            ? ResponseEntity.ok(UserResponse.from(result.getValueOrThrow()))
            : toErrorResponse(result);
    }
    
    private ResponseEntity<?> toErrorResponse(Result<?> result) {
        ErrorDetail error = result.getError().orElseThrow();
        HttpStatus status = switch (error.getErrorType()) {
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case BUSINESS_RULE_ERROR -> HttpStatus.CONFLICT;
            case NOT_FOUND_ERROR -> HttpStatus.NOT_FOUND;
            case AUTHENTICATION_ERROR -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(ErrorResponse.from(error));
    }
}

// ✅ Application layer works with validated value objects
@Component
public class CreateUserUseCase {
    
    public Result<User> execute(CreateUserCommand command) {
        // Data is already validated at boundary
        // Use value objects for additional type safety
        Result<Email> emailResult = Email.of(command.email());
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError());
        }
        
        Email email = emailResult.getValueOrThrow();
        // No try-catch needed - exceptions propagate to GlobalExceptionHandler
        User user = userRepository.save(User.create(email, ...));
        
        return Result.success(user);
    }
}
```

### Summary: Error Handling Checklist

✅ **DO**:
- Return `Result.failure()` for **expected validation errors**
- Use value objects with `of()/from()` static factory methods
- Let exceptions propagate to GlobalExceptionHandler
- Validate at boundaries (controllers, domain constructors)
- Use all 4 parameters in `ErrorDetail.of()`

❌ **DON'T**:
- Catch exceptions in domain/application/infrastructure layers
- Throw exceptions for validation failures (use Result.failure instead)
- Use Result.failure for unexpected system errors (let exceptions propagate)
- Skip validation in value object constructors
- Create value objects with `new` - always use `of()/from()`

### Logging Strategy

#### 🧭 Why log mainly in the command handler (and not everywhere)?

You want clear, non-duplicated logs that tell the business story:

**"User registration started → validated email → saved user → added outbox event → success."**

If every internal class also logs, you'll get:
- **Redundant noise** ("Email validation succeeded" repeated twice)
- **Mixed layers** (domain logs in business flow)
- **Harder debugging** (too much chatter obscures the root cause)

Your handler already logs:
- Start/end of process
- Each validation or save step
- Failures with ErrorDetail
- Structured context (correlationId, email, bankId)

That's already ideal granularity for the application layer.

#### 🧩 When to also log in lower layers

You **do** want logging in these cases:

| Layer | When to log | Example |
|-------|------------|---------|
| **Repository / Infrastructure** | On I/O failure, retries, SQL exceptions, or latency warnings | `logger.error("Failed to save User", e)` |
| **External API clients** | Request/response metadata, timeouts, retries | `"POST /payment-service took 2.3s — status 500"` |
| **Event publishers / outbox processors** | When publishing fails or retries happen | `"Failed to dispatch UserRegisteredEvent to Kafka"` |
| **Domain layer** | Only if debugging domain logic in rare cases; otherwise **avoid** | None (ideally pure logic) |

So your repositories (e.g., `JpaUserRepository`, `OutboxEventRepository`) might log **only** around persistence:

```java
try {
    // Save user
} catch (DataAccessException e) {
    logger.error("Database error while saving user", e);
    throw e;
}
```

That's it — no need to log inside every getter/setter or domain factory.

#### 🧠 Practical guideline

Think of it like layers of concern:

| Layer | Logging Focus |
|-------|--------------|
| **Controller / CommandHandler (application)** | Business flow, structured audit trail |
| **Infrastructure** | Technical I/O, performance, error details |
| **Domain** | **No logging** — focus on correctness & immutability |

#### ✅ Summary recommendation

Your command handlers (e.g., `RegisterUserCommandHandler`) already do exactly the right amount of logging for the application layer.

**You should:**
1. **Keep this logging here** — it provides business flow visibility
2. **Add minimal technical logs** inside repository or outbox persistence methods (only errors or performance issues)
3. **Avoid logs inside domain entities** (`User`, `Email`, etc.) — they should stay pure

**Example of good logging distribution:**
```java
// ✅ Application layer (CommandHandler)
logger.info("Starting user registration | correlationId={} email={}", correlationId, email);
// ... business steps with structured logs

// ✅ Infrastructure layer (Repository)
catch (DataAccessException e) {
    logger.error("Failed to persist user | userId={}", userId, e);
    throw e;
}

// ❌ Domain layer (Entity)
// NO logging here - keep it pure
public static Result<Email> of(String value) {
    // Just validation logic, no logs
}
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

## Common API Gotchas

### ErrorDetail API (Lombok @Getter)
```java
// ❌ WRONG - Record accessor syntax
result.getError().orElseThrow().message()
result.getError().orElseThrow().messageKey()

// ✅ CORRECT - Lombok getter syntax
result.getError().orElseThrow().getMessage()
result.getError().orElseThrow().getMessageKey()
```

**Why**: `ErrorDetail` uses Lombok `@Getter` annotation, which generates `getMessage()` not `message()`.

### QualityResponseHandler Methods
```java
// ❌ WRONG - These methods don't exist
responseHandler.ok(data)
responseHandler.badRequest(message)
responseHandler.internalError(message)

// ✅ CORRECT - Use these methods
responseHandler.handleSuccessResult(result, "Success message", "message.key")
responseHandler.handleErrorResponse(errorDetail)
responseHandler.handleSystemErrorResponse(exception)
```

### Result Pattern with Value Objects
```java
// ✅ CORRECT - Check failure before using value
Result<BankId> bankIdResult = BankId.of(bankIdStr);
if (bankIdResult.isFailure()) {
    return Result.failure(bankIdResult.getError().orElseThrow());
}
BankId bankId = bankIdResult.getValueOrThrow(); // Safe - already checked

// ❌ WRONG - Don't skip validation check
BankId bankId = BankId.of(bankIdStr).getValueOrThrow(); // May throw!
```

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
