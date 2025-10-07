# Architecture Refactoring Design

## 📋 **MANDATORY Architecture Standards**
**ALL refactoring MUST follow the established patterns in:**
📋 **[Functional Bounded Context Architecture Guide](../../docs/architecture/functional-bounded-context-architecture-guide.md)**

## Overview

This design document outlines the refactoring approach to transform the current BCBS239 compliance platform from technical-concern-based organization to capability-based organization within each bounded context. The refactoring will implement proper DDD vertical slices while maintaining the modular monolith structure.

**CRITICAL**: All implementations must strictly adhere to the Architecture Guide patterns to ensure consistency and prevent future refactoring cycles.

## Architecture

### 🚫 **CRITICAL Anti-Patterns to Avoid**

**❌ FORBIDDEN - Direct Repository Injection**
```java
@Component
public class BadHandler {
    private final ConcreteRepositoryImpl repository; // ❌ VIOLATES functional pattern
    
    public BadHandler(ConcreteRepositoryImpl repository) {
        this.repository = repository;
    }
    
    public Result<Response, Error> handle(Command command) {
        return repository.save(entity); // ❌ No functional composition
    }
}
```

**✅ REQUIRED - Functional Repository Interface**
```java
@Component
public class GoodHandler {
    private final CapabilityRepositoryFunctions repositoryFunctions; // ✅ REQUIRED
    private final Function<Command, Result<Response, Error>> operation;
    
    public GoodHandler(CapabilityRepositoryFunctions repositoryFunctions) {
        this.repositoryFunctions = repositoryFunctions;
        this.operation = this::operationImpl;
    }
    
    public Result<Response, Error> handle(Command command) {
        return operation.apply(command);
    }
    
    private Result<Response, Error> operationImpl(Command command) {
        return repositoryFunctions.saveEntity().apply(command.data());
    }
}
```

### Current State Analysis

The current architecture organizes code by technical concerns:
```
src/main/java/com/bcbs239/compliance/
├── iam/
│   ├── domain/
│   │   ├── model/           # All domain models mixed together
│   │   ├── services/        # All domain services mixed together
│   │   ├── repositories/    # All repository interfaces mixed together
│   │   └── events/          # All events mixed together
│   └── infrastructure/
│       ├── persistence/     # All persistence mixed together
│       ├── oauth2/          # OAuth2 specific infrastructure
│       ├── web/             # All web controllers mixed together
│       └── metrics/         # All metrics mixed together
```

### Target State Architecture

The target architecture will organize code by business capabilities:
```
src/main/java/com/bcbs239/compliance/
├── iam/
│   ├── domain/
│   │   ├── users/           # User Management capability
│   │   │   ├── User.java
│   │   │   ├── UserRepository.java
│   │   │   ├── UserRegisteredEvent.java
│   │   │   └── UserManagementService.java
│   │   ├── authentication/ # Authentication capability
│   │   │   ├── AuthenticationService.java
│   │   │   ├── OAuth2AuthenticationEvent.java
│   │   │   └── OAuth2Provider.java
│   │   ├── sessions/       # Session Management capability
│   │   │   ├── UserSession.java
│   │   │   ├── SessionRepository.java
│   │   │   └── SessionService.java
│   │   ├── authorization/  # Authorization capability
│   │   │   ├── BankRoleAssignment.java
│   │   │   ├── BankRoleRepository.java
│   │   │   └── AuthorizationService.java
│   │   └── security/       # Security capability
│   │       ├── SecurityEvent.java
│   │       └── SecurityService.java
│   ├── application/
│   │   ├── users/          # User Management use cases
│   │   │   ├── RegisterUserHandler.java
│   │   │   ├── UpdateUserProfileHandler.java
│   │   │   └── DeactivateUserHandler.java
│   │   ├── authentication/ # Authentication use cases
│   │   │   ├── AuthenticateUserHandler.java
│   │   │   ├── OAuth2LoginHandler.java
│   │   │   └── PasswordResetHandler.java
│   │   ├── sessions/       # Session Management use cases
│   │   │   ├── CreateSessionHandler.java
│   │   │   ├── ValidateSessionHandler.java
│   │   │   └── TerminateSessionHandler.java
│   │   ├── authorization/  # Authorization use cases
│   │   │   ├── AssignBankRoleHandler.java
│   │   │   ├── CheckPermissionHandler.java
│   │   │   └── RevokeBankAccessHandler.java
│   │   └── security/       # Security use cases
│   │       ├── LogSecurityEventHandler.java
│   │       └── DetectSuspiciousActivityHandler.java
│   ├── infrastructure/
│   │   ├── users/          # User Management infrastructure
│   │   │   ├── UserRepositoryImpl.java
│   │   │   ├── UserEntity.java
│   │   │   └── UserMetricsCollector.java
│   │   ├── authentication/ # Authentication infrastructure
│   │   │   ├── OAuth2Providers/
│   │   │   ├── AuthenticationMetrics.java
│   │   │   └── AuthenticationHealthCheck.java
│   │   ├── sessions/       # Session Management infrastructure
│   │   │   ├── SessionRepositoryImpl.java
│   │   │   ├── SessionEntity.java
│   │   │   └── SessionMetrics.java
│   │   ├── authorization/  # Authorization infrastructure
│   │   │   ├── BankRoleRepositoryImpl.java
│   │   │   ├── BankRoleEntity.java
│   │   │   └── AuthorizationMetrics.java
│   │   └── security/       # Security infrastructure
│   │       ├── SecurityEventStore.java
│   │       └── SecurityMetrics.java
│   └── api/
│       ├── users/          # User Management endpoints
│       │   └── UserController.java
│       ├── authentication/ # Authentication endpoints
│       │   └── AuthController.java
│       ├── sessions/       # Session Management endpoints
│       │   └── SessionController.java
│       └── authorization/  # Authorization endpoints
│           └── AuthorizationController.java
```

## Components and Interfaces

### Capability Structure Template

Each capability will follow this consistent structure:

#### Domain Layer (`/domain/{capability}/`)
- **Aggregate Root**: Main entity representing the capability
- **Value Objects**: Supporting value objects for the capability
- **Domain Events**: Events published by the capability
- **Repository Interface**: Data access contract for the capability
- **Domain Services**: Business logic services for the capability

#### Application Layer (`/application/{capability}/`)
- **Command Handlers**: Handle commands for the capability
- **Query Handlers**: Handle queries for the capability
- **Use Case Services**: Orchestrate domain services for complex workflows
- **DTOs**: Data transfer objects for the capability

#### Infrastructure Layer (`/infrastructure/{capability}/`)
- **Repository Implementation**: Concrete data access implementation
- **JPA Entities**: Database entities for the capability
- **External Adapters**: Third-party service integrations
- **Metrics Collectors**: Capability-specific metrics
- **Health Indicators**: Capability-specific health checks

#### Presentation Layer (`/api/{capability}/`)
- **Controllers**: REST endpoints for the capability
- **Request/Response DTOs**: API-specific data structures
- **Validation**: Input validation for the capability

### Database Schema Organization

Each capability will have its own schema namespace:

```sql
-- IAM Capabilities
CREATE SCHEMA iam_users;
CREATE SCHEMA iam_authentication;
CREATE SCHEMA iam_sessions;
CREATE SCHEMA iam_authorization;
CREATE SCHEMA iam_security;

-- Billing Capabilities
CREATE SCHEMA billing_accounts;
CREATE SCHEMA billing_usage;
CREATE SCHEMA billing_invoices;
CREATE SCHEMA billing_payments;
CREATE SCHEMA billing_subscriptions;
CREATE SCHEMA billing_tax;
CREATE SCHEMA billing_dunning;
```

### Cross-Capability Communication

Capabilities will communicate through:

1. **Domain Events**: Asynchronous communication via event bus containing aggregate IDs
2. **Application Services**: Synchronous communication through well-defined interfaces using aggregate IDs
3. **Shared Kernel**: Common value objects and utilities in core module
4. **ID-Only References**: Aggregates reference each other only by ID to maintain loose coupling

### Aggregate Reference Patterns

**Correct Pattern - ID-Only References:**
```java
// User Management Capability
public class User {
    private UserId id;
    private BillingAccountId billingAccountId; // Reference by ID only
    // Never: private BillingAccount billingAccount;
}

// Authorization Capability  
public class BankRoleAssignment {
    private AssignmentId id;
    private UserId userId; // Reference by ID only
    private BankId bankId; // Reference by ID only
    // Never: private User user;
    // Never: private Bank bank;
}
```

**Application Service Orchestration:**
```java
@ApplicationService
public class UserRegistrationHandler {
    
    public Result<UserId, Error> handle(RegisterUserCommand command) {
        // Load aggregates separately by ID
        var user = userRepository.findById(command.userId());
        var billingAccount = billingAccountRepository.findById(command.billingAccountId());
        
        // Orchestrate business logic
        user.activate();
        billingAccount.linkToUser(command.userId());
        
        // Save aggregates separately
        userRepository.save(user);
        billingAccountRepository.save(billingAccount);
        
        return Result.success(user.getId());
    }
}

## Data Models

### Capability Aggregate Mapping

#### IAM Bounded Context

**User Management Capability**
- Aggregate Root: `User`
- Entities: `UserProfile`, `UserPreferences`
- Value Objects: `UserId`, `Email`, `FullName`, `Address`
- External References: `BillingAccountId` (ID-only reference to Billing context)
- Events: `UserRegisteredEvent`, `UserActivatedEvent`, `UserDeactivatedEvent`

**Authentication Capability**
- Aggregate Root: `AuthenticationAttempt`
- Entities: `OAuth2Identity`, `PasswordCredential`
- Value Objects: `AuthenticationMethod`, `OAuth2Provider`, `JwtToken`
- External References: `UserId` (ID-only reference to User Management capability)
- Events: `UserAuthenticatedEvent`, `OAuth2AuthenticationEvent`, `AuthenticationFailedEvent`

**Session Management Capability**
- Aggregate Root: `UserSession`
- Value Objects: `SessionId`, `SessionStatus`, `AuthTokens`
- External References: `UserId` (ID-only reference to User Management capability)
- Events: `SessionCreatedEvent`, `SessionExpiredEvent`, `SessionTerminatedEvent`

**Authorization Capability**
- Aggregate Root: `BankRoleAssignment`
- Value Objects: `BankId`, `Role`, `Permission`
- External References: `UserId` (ID-only reference to User Management capability)
- Events: `RoleAssignedEvent`, `RoleRevokedEvent`, `PermissionGrantedEvent`

**Security Capability**
- Aggregate Root: `SecurityEvent`
- Value Objects: `EventType`, `Severity`, `ThreatLevel`
- External References: `UserId` (ID-only reference to User Management capability)
- Events: `SuspiciousActivityDetectedEvent`, `SecurityViolationEvent`

#### Billing Bounded Context

**Account Management Capability**
- Aggregate Root: `BillingAccount`
- Value Objects: `BillingAccountId`, `AccountStatus`, `BillingAddress`
- External References: `UserId` (ID-only reference to IAM User Management capability)
- Events: `AccountCreatedEvent`, `AccountSuspendedEvent`, `AccountReactivatedEvent`

**Usage Tracking Capability**
- Aggregate Root: `UsageMetrics`
- Entities: `UsageEvent`
- Value Objects: `UsageType`, `UsageAmount`, `BillingPeriod`
- External References: `BillingAccountId` (ID-only reference to Account Management capability)
- Events: `UsageRecordedEvent`, `UsageLimitExceededEvent`

**Invoice Management Capability**
- Aggregate Root: `Invoice`
- Entities: `InvoiceLineItem`
- Value Objects: `InvoiceId`, `InvoiceNumber`, `InvoiceStatus`
- External References: `BillingAccountId`, `UsageMetricsId` (ID-only references)
- Events: `InvoiceGeneratedEvent`, `InvoicePaidEvent`, `InvoiceOverdueEvent`

## Error Handling

### Capability-Specific Error Handling

Each capability will define its own error types and handling:

```java
// User Management Capability
public enum UserManagementErrorType {
    USER_NOT_FOUND,
    EMAIL_ALREADY_EXISTS,
    INVALID_USER_STATUS,
    USER_DEACTIVATION_FAILED
}

// Authentication Capability  
public enum AuthenticationErrorType {
    INVALID_CREDENTIALS,
    OAUTH2_PROVIDER_ERROR,
    TOKEN_EXPIRED,
    AUTHENTICATION_RATE_LIMITED
}
```

### Cross-Capability Error Propagation

Errors will be propagated across capabilities through:
1. **Result Types**: Using `Result<T, Error>` for synchronous operations
2. **Domain Events**: Publishing error events for asynchronous handling
3. **Application Exceptions**: Well-defined exceptions for application layer

## Testing Strategy

### Capability-Level Testing

Each capability will have comprehensive testing at all layers:

1. **Domain Tests**: Unit tests for domain logic within each capability
2. **Application Tests**: Integration tests for use case handlers
3. **Infrastructure Tests**: Repository and adapter integration tests
4. **API Tests**: Controller and endpoint tests

### Cross-Capability Integration Testing

Integration tests will verify:
1. **Event Publishing**: Capability events are properly published
2. **Event Handling**: Capabilities properly handle events from other capabilities
3. **Data Consistency**: Cross-capability data remains consistent
4. **Performance**: Cross-capability operations meet performance requirements

### Testing Structure

```
src/test/java/com/bcbs239/compliance/
├── iam/
│   ├── domain/
│   │   ├── users/UserTest.java
│   │   ├── authentication/AuthenticationServiceTest.java
│   │   └── sessions/SessionServiceTest.java
│   ├── application/
│   │   ├── users/RegisterUserHandlerTest.java
│   │   └── authentication/AuthenticateUserHandlerTest.java
│   └── infrastructure/
│       ├── users/UserRepositoryImplTest.java
│       └── authentication/OAuth2ProviderTest.java
└── integration/
    ├── IamCapabilityIntegrationTest.java
    └── CrossCapabilityEventTest.java
```