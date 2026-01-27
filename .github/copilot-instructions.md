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
- **Versioning**:
  - `regtech-core`: V2-V9
  - `regtech-iam`: V10-V19
  - `regtech-billing`: V20-V29
  - `regtech-ingestion`: V30-V39
  - `regtech-data-quality`: V40-V49 (Use V49_1, V49_2 for overflow)
  - `regtech-risk-calculation`: V50-V59
  - `regtech-report-generation`: V60-V69
  - `metrics`: V70+
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

### CQRS Pattern (Read vs Write)
- **Commands**: Handle side effects (Create/Update/Delete). Return `Result<Id>` or `Result<Void>`.
- **Queries**: Handle data retrieval. Return DTOs directly (or `Page<Dto>`).
- **Query Handlers**: Encapsulate query logic. Use `JpaSpecificationExecutor` for dynamic filtering.
- **DTO Location**: Application layer (if shared) or Presentation layer (if specific to API). Application layer must NOT depend on Presentation layer.

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
}
```
