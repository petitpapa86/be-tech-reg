# Architecture Refactoring Implementation Plan

## �  **IMPLEMENTATION STATUS OVERVIEW**

### ✅ **COMPLETED PHASES**
- **Analysis and Planning Phase (1.x)** - 100% Complete
- **IAM User Management Capability (2.1.x)** - 100% Complete  
- **IAM Authentication Capability (2.2.x)** - 100% Complete
- **IAM Session Management Capability (2.3.x)** - 100% Complete
- **IAM Authorization Capability (2.4.x)** - 100% Complete
- **IAM Security Capability (2.5.x)** - 100% Complete

### ⚠️ **PARTIALLY COMPLETED PHASES**
- None - All IAM capabilities are now complete!

### ✅ **COMPLETED PHASES**
- **Analysis and Planning Phase (1.x)** - 100% Complete
- **IAM User Management Capability (2.1.x)** - 100% Complete  
- **IAM Authentication Capability (2.2.x)** - 100% Complete
- **IAM Session Management Capability (2.3.x)** - 100% Complete
- **IAM Authorization Capability (2.4.x)** - 100% Complete
- **IAM Security Capability (2.5.x)** - 100% Complete
- **Billing Account Management Capability (3.1.x)** - 100% Complete

### ⚠️ **PARTIALLY COMPLETED PHASES**
- None - All IAM capabilities and first Billing capability are now complete!

### ❌ **NOT STARTED PHASES**
- **Billing Usage Tracking Capability (3.2.x)** - 0% Complete
- **Billing Invoice Management Capability (3.3.x)** - 0% Complete
- **Cross-Capability Integration Testing** - 0% Complete
- **Performance Optimization** - 0% Complete

### 🎯 **NEXT PRIORITY TASKS**
1. Begin Billing Usage Tracking capability (3.2.1)
2. Complete remaining Billing bounded context capabilities (3.2.x, 3.3.x)
3. Cross-capability integration testing and optimization
4. Performance optimization

## 🚨 **MANDATORY ARCHITECTURE COMPLIANCE**

**ALL refactoring work MUST strictly follow the patterns defined in:**
📋 **[Functional Bounded Context Architecture Guide](../../docs/architecture/functional-bounded-context-architecture-guide.md)**

### **🔥 CRITICAL PRE-REFACTORING REQUIREMENTS**
**BEFORE starting ANY refactoring task, you MUST:**

1. [ ] **MANDATORY**: Read and understand the complete [Architecture Guide](../../docs/architecture/functional-bounded-context-architecture-guide.md)
2. [ ] **MANDATORY**: Understand the exact handler pattern with functional composition
3. [ ] **MANDATORY**: Understand the direct repository access pattern (eliminates pass-through methods)  
4. [ ] **MANDATORY**: Understand immutable record patterns for commands/queries/responses
5. [ ] **MANDATORY**: Understand the Result pattern for error handling
6. [ ] **MANDATORY**: Understand capability-based package organization standards
7. [ ] **MANDATORY**: Understand logging, metrics, and health check patterns
8. [ ] **MANDATORY**: Review all forbidden anti-patterns

### **⚠️ ARCHITECTURE COMPLIANCE CHECKPOINTS**
Every refactored component MUST pass these validation checks:

#### **Handler Compliance**
- [ ] ✅ Uses `{Capability}RepositoryFunctions` (NOT direct repository injection)
- [ ] ✅ Has functional composition with `Function<Command, Result<Response, Error>>`
- [ ] ✅ Uses private implementation method (e.g., `operationImpl`)
- [ ] ✅ Includes proper logging with correlation IDs
- [ ] ✅ Uses Result pattern for error handling
- [ ] ✅ NO imperative repository calls in handler methods

#### **Repository Functions Compliance**
- [ ] ✅ Uses direct repository access pattern (NOT pass-through methods)
- [ ] ✅ Provides simple accessor methods to repository implementations
- [ ] ✅ Repository implementations return `Function<Input, Result<Output, Error>>`
- [ ] ✅ Usage follows explicit call chain: `repositoryFunctions.entityRepository().operation().apply(input)`
- [ ] ✅ NO unnecessary wrapper methods that just delegate

#### **Command/Query/Response Compliance**
- [x] ✅ Uses immutable records for all commands/queries/responses



- [x] ✅ Includes proper validation annotations



- [ ] ✅ Has factory methods for common use cases
- [ ] ✅ Includes correlation ID support
- [ ] ✅ NO mutable state or setter methods

#### **Error Handling Compliance**
- [ ] ✅ All operations return `Result<T, {BoundedContext}Error>`
- [ ] ✅ Uses proper error types from `{BoundedContext}ErrorType` enum
- [ ] ✅ Includes correlation ID in error context
- [ ] ✅ Handles exceptions and converts to domain errors

### **🚫 STRICTLY FORBIDDEN PATTERNS**
The following patterns will result in **IMMEDIATE REJECTION**:

#### ❌ **Direct Repository Injection**
```java
// ❌ FORBIDDEN - This will be REJECTED
@Component
public class BadHandler {
    private final ConcreteRepositoryImpl repository; // ❌ VIOLATES ARCHITECTURE
    public BadHandler(ConcreteRepositoryImpl repository) {
        this.repository = repository;
    }
}
```

#### ❌ **Imperative Handler Logic**
```java
// ❌ FORBIDDEN - Imperative style will be REJECTED
public Result<Response, Error> handle(Command command) {
    Entity entity = repository.findById(command.id()); // ❌ Direct calls
    if (entity == null) {
        return Result.failure(Error.notFound());
    }
    entity.update(command.data());
    Entity saved = repository.save(entity); // ❌ Imperative flow
    return Result.success(Response.from(saved));
}
```

#### ❌ **Mutable Command Objects**
```java
// ❌ FORBIDDEN - Mutable commands will be REJECTED
public class BadCommand {
    private String field; // ❌ Mutable state
    public void setField(String field) { // ❌ Setter methods
        this.field = field;
    }
}
```

### **✅ REQUIRED PATTERNS**
Every refactored component MUST follow these exact patterns:

#### ✅ **Correct Handler Pattern**
```java
// ✅ REQUIRED - This is the ONLY acceptable pattern
@Component
public class {Operation}Handler {
    private static final Logger logger = LoggerFactory.getLogger({Operation}Handler.class);
    private final {Capability}RepositoryFunctions repositoryFunctions; // ✅ REQUIRED
    private final Function<{Operation}Command, Result<{Operation}Response, {BoundedContext}Error>> {operation};
    
    public {Operation}Handler({Capability}RepositoryFunctions repositoryFunctions) {
        this.repositoryFunctions = repositoryFunctions;
        this.{operation} = this::{operation}Impl;
    }
    
    public Result<{Operation}Response, {BoundedContext}Error> handle({Operation}Command command) {
        logger.info("Processing {operation} for {}", command.identifier());
        return {operation}.apply(command);
    }
    
    private Result<{Operation}Response, {BoundedContext}Error> {operation}Impl({Operation}Command command) {
        if (command == null) {
            return Result.failure({BoundedContext}Error.validationError("Command cannot be null"));
        }
        return repositoryFunctions
            .save{Entity}()
            .apply(command.data())
            .map(this::createResponse);
    }
}
```

#### ✅ **Correct Repository Functions Pattern (Direct Access)**
```java
// ✅ REQUIRED - Direct Repository Access Pattern
@Component
public class {Capability}RepositoryFunctions {
    
    private final {Entity}RepositoryImpl {entity}Repository;
    private final {RelatedEntity}RepositoryImpl {relatedEntity}Repository;
    
    public {Capability}RepositoryFunctions(
            {Entity}RepositoryImpl {entity}Repository,
            {RelatedEntity}RepositoryImpl {relatedEntity}Repository) {
        this.{entity}Repository = {entity}Repository;
        this.{relatedEntity}Repository = {relatedEntity}Repository;
    }
    
    /**
     * Get {entity} repository for direct function access.
     * Usage: repositoryFunctions.{entity}Repository().save{Entity}().apply(entity)
     */
    public {Entity}RepositoryImpl {entity}Repository() {
        return {entity}Repository;
    }
    
    /**
     * Get {relatedEntity} repository for direct function access.
     * Usage: repositoryFunctions.{relatedEntity}Repository().findBy{Criteria}().apply(criteria)
     */
    public {RelatedEntity}RepositoryImpl {relatedEntity}Repository() {
        return {relatedEntity}Repository;
    }
}
```

**Benefits of Direct Access Pattern:**
- **Eliminates Pass-through Calls**: No more unnecessary wrapper methods that just delegate to the actual repository
- **More Explicit**: The call chain clearly shows you're accessing the payment repository and then calling its function
- **Better Functional Composition**: Follows functional programming principles more closely
- **Reduced Code Duplication**: No need to duplicate method signatures in the wrapper class
- **Cleaner Architecture**: The RepositoryFunctions becomes a simple aggregator rather than a proxy

**Usage Examples:**
```java
// Save payment
repositoryFunctions.paymentRepository().savePayment().apply(payment);

// Find payment attempts
repositoryFunctions.paymentAttemptRepository().findAttemptsByPaymentId().apply(paymentId);

// Find retryable payments
repositoryFunctions.paymentRepository().findRetryableFailedPayments().apply(3);

// Complex functional composition
return repositoryFunctions
    .paymentRepository()
    .findById()
    .apply(paymentId)
    .flatMap(payment -> 
        repositoryFunctions
            .paymentAttemptRepository()
            .findAttemptsByPaymentId()
            .apply(payment.getId())
            .map(attempts -> PaymentWithAttempts.of(payment, attempts))
    );
```

#### ✅ **Correct Command/Query Pattern**
```java
// ✅ REQUIRED - Immutable record with validation
public record {Operation}Command(
    @NotNull(message = "Required field cannot be null")
    RequiredField requiredField,
    OptionalField optionalField,
    CorrelationId correlationId
) {
    public {Operation}Command {
        Objects.requireNonNull(requiredField, "Required field cannot be null");
        if (correlationId == null) {
            correlationId = CorrelationId.generate();
        }
    }
    
    public static {Operation}Command of(RequiredField field, OptionalField optional) {
        return new {Operation}Command(field, optional, CorrelationId.generate());
    }
}
```

---

**Current State**: The codebase has been significantly refactored from technical concerns to capability-based organization. Major progress has been made on IAM bounded context.

**Key Achievements**:
- ✅ **IAM BOUNDED CONTEXT 100% COMPLETE** - All capabilities fully implemented
- ✅ IAM User Management capability fully implemented (domain, application, infrastructure, API, database)
- ✅ IAM Authentication capability fully implemented (domain, application, infrastructure, API, database)  
- ✅ IAM Session Management capability fully implemented (domain, application, infrastructure, API, database)
- ✅ IAM Authorization capability fully implemented (domain, application, infrastructure, API, database)
- ✅ IAM Security capability fully implemented (domain, application, infrastructure, database)
- ✅ Architecture compliance patterns implemented and validated across all capabilities
- ✅ Functional handler patterns with repository functions implemented throughout
- ✅ Result pattern, structured logging, and validation patterns implemented
- ✅ Comprehensive health monitoring and metrics collection implemented

**Remaining Work**:
- ❌ Billing bounded context capabilities not yet started (all 3.x tasks)
- ❌ Cross-capability integration testing and performance optimization

## 1. Analysis and Planning Phase

- [x] 1.1 Analyze current codebase structure and identify all capabilities
  - Map existing domain models to business capabilities
  - Identify aggregate boundaries and consistency requirements
  - Document current cross-module dependencies
  - _Requirements: 1.1, 1.4_

- [x] 1.2 Define capability boundaries and interfaces
  - Create capability mapping document for each bounded context
  - Define aggregate roots and entity relationships within capabilities
  - Specify cross-capability communication patterns using ID-only references
  - Document aggregate reference patterns and anti-patterns
  - _Requirements: 1.1, 1.4, 5.4, 7.1, 7.2_

- [x] 1.3 Extract embedded repository interfaces in billing domain
  - Extract repository interfaces from BillingService, UsageService, PaymentService, and other domain services
  - Create separate repository interface files in billing/domain/repositories/
  - Update domain services to use the extracted repository interfaces
  - _Requirements: 1.1, 1.3_

- [x] 1.4 Design database schema migration strategy
  - Create capability-specific schema migration scripts for IAM capabilities (iam_users, iam_authentication, iam_sessions, iam_authorization, iam_security)
  - Create capability-specific schema migration scripts for Billing capabilities (billing_accounts, billing_usage, billing_invoices, billing_payments, billing_subscriptions, billing_tax, billing_dunning)
  - Plan data migration from existing tables to new capability-specific schemas
  - Define cross-capability data access patterns using ID-only references
  - _Requirements: 5.1, 5.2, 5.3_

## 2. IAM Bounded Context Refactoring

### 🚨 **IAM ARCHITECTURE COMPLIANCE REQUIREMENTS**
**EVERY IAM capability MUST follow the [Architecture Guide](../../docs/architecture/functional-bounded-context-architecture-guide.md) patterns:**

#### **Package Structure Compliance**
```
com.bcbs239.compliance.iam/
├── api/{capability}/           # REST controllers and DTOs
├── application/{capability}/   # Use case handlers (organized by operation)
├── domain/{capability}/        # Domain entities, value objects, services  
└── infrastructure/{capability}/ # Technical implementations
```

#### **Handler Pattern Compliance**
- ✅ ALL handlers MUST use `{Capability}RepositoryFunctions`
- ✅ ALL handlers MUST use functional composition with `Function<Command, Result<Response, IamError>>`
- ✅ ALL handlers MUST have private implementation methods
- ✅ ALL handlers MUST include structured logging with correlation IDs
- ✅ NO direct repository injection allowed

#### **Repository Functions Compliance**
- ✅ ALL repository access MUST use direct access pattern (NOT pass-through methods)
- ✅ ALL repository implementations MUST return `Function<Input, Result<Output, IamError>>`
- ✅ ALL usage MUST follow explicit call chain: `repositoryFunctions.entityRepository().operation().apply(input)`
- ✅ NO unnecessary wrapper methods that just delegate to repository functions

#### **Command/Query/Response Compliance**
- ✅ ALL commands/queries/responses MUST be immutable records
- ✅ ALL MUST include proper validation annotations
- ✅ ALL MUST include correlation ID support
- ✅ ALL MUST have factory methods

#### **Error Handling Compliance**
- ✅ ALL operations MUST return `Result<T, IamError>`
- ✅ MUST use `IamErrorType` enum for error classification
- ✅ MUST include correlation IDs in error context

### 2.1 User Management Capability

- [x] 2.1.1 Create User Management domain structure
  - Create `/iam/domain/users/` package structure
  - Move User aggregate and related value objects (User, UserId, Email, FullName, Address, UserStatus) from existing `/iam/domain/model/` to `/iam/domain/users/`
  - Move UserRepository interface from `/iam/domain/repositories/` to `/iam/domain/users/`
  - Move UserRegistrationService from `/iam/domain/services/` to `/iam/domain/users/`
  - Move user-related events (UserRegisteredEvent) from `/iam/domain/events/` to `/iam/domain/users/`
  - Refactor User aggregate to reference BillingAccount by ID only (remove any direct object references)
  - _Requirements: 1.1, 1.3, 7.1, 7.2_

- [x] 2.1.2 Create User Management application layer
  - Create `/iam/application/users/` package structure following Architecture Guide
  - Move and refactor UserRegistrationService to RegisterUserHandler following functional handler pattern
  - Create UpdateUserProfileHandler, DeactivateUserHandler following Architecture Guide patterns
  - Create user management command and query DTOs as immutable records
  - **🚨 ARCHITECTURE VALIDATION**: 
    - [ ] ✅ All handlers use `UserRepositoryFunctions` (NOT direct repository injection)
    - [ ] ✅ All handlers use `Function<Command, Result<Response, IamError>>` pattern
    - [ ] ✅ All commands/queries/responses are immutable records
    - [ ] ✅ All operations return `Result<T, IamError>`
    - [ ] ✅ All handlers include structured logging with correlation IDs
  - _Requirements: 3.1, 3.2_

- [x] 2.1.3 Create User Management infrastructure layer
  - Create `/iam/infrastructure/users/` package structure
  - Move UserRepositoryImpl, UserEntity, and JpaUserRepository from existing `/iam/infrastructure/persistence/` to `/iam/infrastructure/users/`
  - Create user-specific metrics collector and health indicator
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 2.1.4 Create User Management presentation layer
  - Create `/iam/api/users/` package structure (currently no API layer exists for IAM)
  - Create UserController with user-related endpoints
  - Create user-specific request/response DTOs
  - _Requirements: 4.1, 4.2_

- [x] 2.1.5 Create User Management database schema
  - Create `iam_users` schema and migrate existing user tables from `iam` schema
  - Update UserRepositoryImpl to use new `iam_users` schema
  - Create user management specific migration scripts
  - _Requirements: 5.1, 5.2, 6.2_

### 2.2 Authentication Capability

- [x] 2.2.1 Create Authentication domain structure
  - Create `/iam/domain/authentication/` package structure
  - Move AuthenticationService, OAuth2AuthenticationService from `/iam/domain/services/` to `/iam/domain/authentication/`
  - Move OAuth2 related models (OAuth2Provider, OAuth2UserInfo, OAuth2TokenResponse, OAuth2IdentityProvider, OAuth2UserMapper) to `/iam/domain/authentication/`
  - Move authentication events (OAuth2AuthenticationEvent, UserAuthenticatedEvent) to `/iam/domain/authentication/`
  - Move JwtToken, AuthTokens models to `/iam/domain/authentication/`
  - _Requirements: 1.1, 1.3_

- [x] 2.2.2 Create Authentication application layer
  - Create `/iam/application/authentication/` package structure
  - Create AuthenticateUserHandler, OAuth2LoginHandler application services
  - Move PasswordResetService to PasswordResetHandler in application layer
  - Create authentication command and query DTOs
  - _Requirements: 3.1, 3.2_

- [x] 2.2.3 Create Authentication infrastructure layer
  - Create `/iam/infrastructure/authentication/` package structure
  - Move OAuth2 providers (GoogleOAuth2Provider, FacebookOAuth2Provider) from `/iam/infrastructure/oauth2/` to `/iam/infrastructure/authentication/`
  - Move OAuth2 handlers (OAuth2AuthenticationSuccessHandler, OAuth2AuthenticationFailureHandler, OAuth2UserServiceImpl) to `/iam/infrastructure/authentication/`
  - Create authentication-specific metrics and health checks
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 2.2.4 Create Authentication presentation layer
  - Create `/iam/api/authentication/` package structure
  - Move OAuth2Controller from `/iam/infrastructure/web/` to `/iam/api/authentication/`
  - Create authentication-specific request/response DTOs
  - _Requirements: 4.1, 4.2_

- [x] 2.2.5 Create Authentication database schema
  - Create `iam_authentication` schema for OAuth2 and credential data
  - Migrate OAuth2-related columns from existing user tables to new schema
  - Update authentication-related repository implementations
  - Create authentication specific migration scripts
  - _Requirements: 5.1, 5.2, 6.2_

### 2.3 Session Management Capability

- [x] 2.3.1 Create Session Management domain structure
  - Create `/iam/domain/sessions/` package structure
  - Move UserSession aggregate and related value objects (SessionId, SessionStatus, AuthTokens) from `/iam/domain/model/` to `/iam/domain/sessions/`
  - Move UserSessionRepository interface from `/iam/domain/repositories/` to `/iam/domain/sessions/`
  - Move UserSessionService from `/iam/domain/services/` to `/iam/domain/sessions/`
  - Move JwtTokenService from `/iam/domain/services/` to `/iam/domain/sessions/` (as it's session-related)
  - _Requirements: 1.1, 1.3_

- [x] 2.3.2 Create Session Management application layer
  - Create `/iam/application/sessions/` package structure
  - Create CreateSessionHandler, ValidateSessionHandler, TerminateSessionHandler application services
  - Move session-related logic from UserSessionService to application handlers
  - Create session management command and query DTOs
  - _Requirements: 3.1, 3.2_

- [x] 2.3.3 Create Session Management infrastructure layer
  - Create `/iam/infrastructure/sessions/` package structure
  - Move UserSessionRepositoryImpl, UserSessionEntity, JpaUserSessionRepository from `/iam/infrastructure/persistence/` to `/iam/infrastructure/sessions/`
  - Create session-specific metrics collector and health indicators
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 2.3.4 Create Session Management presentation layer
  - Create `/iam/api/sessions/` package structure
  - Create SessionController with session-related endpoints
  - Create session-specific request/response DTOs
  - _Requirements: 4.1, 4.2_

- [x] 2.3.5 Create Session Management database schema
  - Create `iam_sessions` schema and migrate existing user_sessions table from `iam` schema
  - Update UserSessionRepositoryImpl to use new `iam_sessions` schema
  - Create session management specific migration scripts
  - _Requirements: 5.1, 5.2, 6.2_

### 2.4 Authorization Capability

- [x] 2.4.1 Create Authorization domain structure
  - Create `/iam/domain/authorization/` package structure
  - Move BankRoleAssignment aggregate and related models (AssignmentId, BankId, UserRole) from `/iam/domain/model/` to `/iam/domain/authorization/`
  - Move BankRoleAssignmentRepository interface from `/iam/domain/repositories/` to `/iam/domain/authorization/`
  - Move BankRoleService from `/iam/domain/services/` to `/iam/domain/authorization/`
  - Refactor BankRoleAssignment to reference User and Bank by ID only (remove any direct object references)
  - _Requirements: 1.1, 1.3, 7.1, 7.2_

- [x] 2.4.2 Create Authorization application layer
  - ✅ Created `/iam/application/authorization/` package structure
  - ✅ Created AssignBankRoleHandler, CheckPermissionHandler, RevokeBankAccessHandler application services
  - ✅ Moved authorization logic from BankRoleService to application handlers
  - ✅ Created authorization command and query DTOs
  - _Requirements: 3.1, 3.2_

- [x] 2.4.3 Create Authorization infrastructure layer
  - ✅ Created `/iam/infrastructure/authorization/` package structure
  - ✅ Moved BankRoleAssignmentRepositoryImpl, BankRoleAssignmentEntity, JpaBankRoleAssignmentRepository from `/iam/infrastructure/persistence/` to `/iam/infrastructure/authorization/`
  - ✅ Created authorization-specific metrics collector and health indicators
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 2.4.4 Create Authorization presentation layer
  - ✅ Created `/iam/api/authorization/` package structure
  - ✅ Created AuthorizationController with authorization-related endpoints
  - ✅ Created authorization-specific request/response DTOs
  - _Requirements: 4.1, 4.2_

- [x] 2.4.5 Create Authorization database schema
  - ✅ Created `iam_authorization` schema and migrated existing bank_role_assignments table from `iam` schema
  - ✅ Updated BankRoleAssignmentRepositoryImpl to use new `iam_authorization` schema
  - ✅ Created authorization specific migration scripts (V9__Create_Authorization_Capability_Schema.sql)
  - _Requirements: 5.1, 5.2, 6.2_

### 2.5 Security Capability

- [x] 2.5.1 Create Security domain structure
  - Create `/iam/domain/security/` package structure
  - Move security-related models and events from existing structure
  - Move TenantContext from `/iam/domain/model/` to `/iam/domain/security/` (as it's security-related)
  - Create security-specific value objects and events for audit logging
  - _Requirements: 1.1, 1.3_

- [x] 2.5.2 Create Security application layer
  - Create `/iam/application/security/` package structure
  - Create LogSecurityEventHandler, DetectSuspiciousActivityHandler application services
  - Create security command and query DTOs
  - _Requirements: 3.1, 3.2_

- [x] 2.5.3 Create Security infrastructure layer
  - ✅ Created `/iam/infrastructure/security/` package structure
  - ✅ Created SecurityEventRepository and AuditLogRepository domain interfaces
  - ✅ Created JPA entities (SecurityEventEntity, AuditLogEntity) with domain conversion
  - ✅ Created JPA repositories with comprehensive query methods
  - ✅ Created repository implementations with Result pattern and error handling
  - ✅ Created SecurityRepositoryFunctions with functional composition patterns
  - ✅ Created SecurityHealthIndicator for monitoring and alerting
  - ✅ Updated application handlers to use functional repository patterns
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 2.5.4 Create Security database schema
  - ✅ Created `iam_security` schema with security_events and audit_logs tables
  - ✅ Added comprehensive indexes for performance optimization
  - ✅ Added proper constraints, triggers, and JSONB support
  - ✅ Created V10__Create_Security_Capability_Schema.sql migration
  - _Requirements: 5.1, 5.2, 6.2_
  - Create `iam_security` schema for security events and audit logs
  - Create security event tables for audit logging
  - Create security specific migration scripts
  - _Requirements: 5.1, 5.2, 6.2_

## 3. Billing Bounded Context Refactoring

### 🚨 **BILLING ARCHITECTURE COMPLIANCE REQUIREMENTS**
**EVERY Billing capability MUST follow the [Architecture Guide](../../docs/architecture/functional-bounded-context-architecture-guide.md) patterns:**

#### **Package Structure Compliance**
```
com.bcbs239.compliance.billing/
├── api/{capability}/           # REST controllers and DTOs
├── application/{capability}/   # Use case handlers (organized by operation)
├── domain/{capability}/        # Domain entities, value objects, services  
└── infrastructure/{capability}/ # Technical implementations
```

#### **Handler Pattern Compliance**
- ✅ ALL handlers MUST use `{Capability}RepositoryFunctions`
- ✅ ALL handlers MUST use functional composition with `Function<Command, Result<Response, BillingError>>`
- ✅ ALL handlers MUST have private implementation methods
- ✅ ALL handlers MUST include structured logging with correlation IDs
- ✅ NO direct repository injection allowed

#### **Repository Functions Compliance**
- ✅ ALL repository access MUST use direct access pattern (NOT pass-through methods)
- ✅ ALL repository implementations MUST return `Function<Input, Result<Output, BillingError>>`
- ✅ ALL usage MUST follow explicit call chain: `repositoryFunctions.entityRepository().operation().apply(input)`
- ✅ NO unnecessary wrapper methods that just delegate to repository functions

#### **Command/Query/Response Compliance**
- ✅ ALL commands/queries/responses MUST be immutable records
- ✅ ALL MUST include proper validation annotations
- ✅ ALL MUST include correlation ID support
- ✅ ALL MUST have factory methods

#### **Error Handling Compliance**
- ✅ ALL operations MUST return `Result<T, BillingError>`
- ✅ MUST use `BillingErrorType` enum for error classification
- ✅ MUST include correlation IDs in error context

#### **Metrics and Observability Compliance**
- ✅ ALL capabilities MUST have dedicated metrics collectors
- ✅ ALL capabilities MUST have health indicators
- ✅ ALL operations MUST be instrumented with timers and counters

**Note**: Billing domain currently has repository interfaces embedded within service classes. These need to be extracted first (task 1.3) before capability refactoring can begin.

### 3.1 Account Management Capability

- [x] 3.1.1 Create Account Management domain structure
  - ✅ Created `/billing/domain/accountmanagement/` package structure (already exists and well-organized)
  - ✅ BillingAccount aggregate and related value objects (BillingAccountId, AccountStatus, BillingAddress, BankId, etc.) properly located in `/billing/domain/accountmanagement/`
  - ✅ BillingAccountRepository interface properly located in `/billing/domain/accountmanagement/`
  - ✅ Account-related domain services (TierManagementService, ServiceAccessControlService, etc.) properly located in `/billing/domain/accountmanagement/services/`
  - ✅ All domain models follow immutable patterns and proper encapsulation
  - _Requirements: 1.1, 1.3_

- [x] 3.1.2 Create Account Management application layer
  - ✅ Created `/billing/application/accountmanagement/` package structure with operation-based organization
  - ✅ Created CreateBillingAccountHandler, SuspendAccountHandler, ReactivateAccountHandler, GetAccountHandler, UpdateAccountHandler application services
  - ✅ All handlers follow functional architecture pattern with `Function<Command, Result<Response, ErrorDetail>>`
  - ✅ All handlers use `AccountManagementRepositoryFunctions` (NOT direct repository injection)
  - ✅ All handlers include structured logging with correlation IDs
  - ✅ Created immutable record command and query DTOs (CreateBillingAccountCommand, GetAccountQuery, etc.)
  - ✅ All operations return `Result<T, ErrorDetail>` for proper error handling
  - **🚨 ARCHITECTURE VALIDATION**: 
    - [x] ✅ All handlers use `AccountManagementRepositoryFunctions` (NOT direct repository injection)
    - [x] ✅ All handlers use `Function<Command, Result<Response, ErrorDetail>>` pattern
    - [x] ✅ All commands/queries/responses are immutable records
    - [x] ✅ All operations return `Result<T, ErrorDetail>`
    - [x] ✅ All handlers include structured logging with correlation IDs
  - _Requirements: 3.1, 3.2_

- [x] 3.1.3 Create Account Management infrastructure layer
  - ✅ Created `/billing/infrastructure/accountmanagement/` package structure
  - ✅ BillingAccountRepositoryImpl, BillingAccountEntity, and JpaBillingAccountRepository properly located in `/billing/infrastructure/accountmanagement/`
  - ✅ Created AccountManagementRepositoryFunctions with functional composition patterns
  - ✅ All repository functions return `Function<Input, Result<Output, BillingError>>`
  - ✅ All repository functions use method references and functional patterns
  - ✅ Created AccountManagementMetricsCollector with comprehensive metrics for account operations
  - ✅ Created AccountManagementHealthIndicator for monitoring account management service health
  - ✅ All infrastructure components follow architecture compliance patterns
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 3.1.4 Create Account Management presentation layer
  - ✅ Created `/billing/api/accountmanagement/` package structure
  - ✅ Created AccountController with comprehensive REST endpoints for account lifecycle management
  - ✅ Created account-specific request DTOs (CreateAccountRequestDto, UpdateAccountRequestDto, SuspendAccountRequestDto)
  - ✅ Created account-specific response DTOs (CreateAccountResponseDto, AccountResponseDto, ErrorResponseDto)
  - ✅ Implemented proper HTTP status mapping and error handling
  - ✅ All DTOs follow immutable record patterns with validation annotations
  - ✅ Proper mapping between API DTOs and application commands/responses
  - _Requirements: 4.1, 4.2_

- [x] 3.1.5 Create Account Management database schema
  - ✅ Created `billing_accounts` schema with comprehensive table structure (V5__Create_Billing_Capability_Schemas.sql)
  - ✅ Created billing_accounts.billing_accounts table with all required fields
  - ✅ Created billing_accounts.account_configurations table for account settings
  - ✅ Created billing_accounts.account_events table for audit logging
  - ✅ Data migration implemented in V6__Migrate_Data_To_Capability_Schemas.sql
  - ✅ BillingAccountRepositoryImpl uses new `billing_accounts` schema
  - ✅ Account management specific migration scripts created and functional
  - _Requirements: 5.1, 5.2, 6.2_

### 3.2 Usage Tracking Capability

- [x] 3.2.1 Create Usage Tracking domain structure
  - Create `/billing/domain/usage/` package structure
  - Move UsageMetrics aggregate and UsageEvent entities from `/billing/domain/model/` to `/billing/domain/usage/`
  - Move usage-related models (UsageMetricsId, UsageEventId, BillingPeriod, UsageSummary, UsageBreakdown, UsageAnalytics) to `/billing/domain/usage/`
  - Move UsageService and UsageTrackingEngine from `/billing/domain/services/` to `/billing/domain/usage/`
  - Move usage repository interfaces to `/billing/domain/usage/`
  - _Requirements: 1.1, 1.3_

- [x] 3.2.2 Create Usage Tracking application layer
  - Create `/billing/application/usage/` package structure
  - Create RecordUsageHandler, CalculateUsageHandler, GenerateUsageReportHandler application services
  - Move usage tracking logic from existing services to application handlers
  - Create usage tracking command and query DTOs
  - _Requirements: 3.1, 3.2_

- [x] 3.2.3 Create Usage Tracking infrastructure layer
  - Create `/billing/infrastructure/usage/` package structure
  - Move UsageMetricsRepositoryImpl, UsageMetricsEntity, UsageEventEntity from `/billing/infrastructure/persistence/` to `/billing/infrastructure/usage/`
  - Create usage-specific metrics collector and health indicators
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 3.2.4 Create Usage Tracking presentation layer
  - Create `/billing/api/usage/` package structure
  - Move usage-related endpoints from UsageController to new capability-specific controller
  - Create usage-specific request/response DTOs
  - _Requirements: 4.1, 4.2_

- [x] 3.2.5 Create Usage Tracking database schema
  - Create `billing_usage` schema and migrate existing usage_metrics and usage_events tables
  - Update usage repository implementations to use new `billing_usage` schema
  - Create usage tracking specific migration scripts
  - _Requirements: 5.1, 5.2, 6.2_

### 3.3 Invoice Management Capability

- [ ] 3.3.1 Create Invoice Management domain structure
  - Create `/billing/domain/invoices/` package structure
  - Move Invoice aggregate and InvoiceLineItem entities from `/billing/domain/model/` to `/billing/domain/invoices/`
  - Move invoice-related models (InvoiceId, InvoiceNumber, InvoiceStatus, InvoiceLineItemId, TaxBreakdown) to `/billing/domain/invoices/`
  - Move InvoiceGenerationEngine and InvoiceDistributionService from `/billing/domain/services/` to `/billing/domain/invoices/`
  - Move invoice repository interfaces to `/billing/domain/invoices/`
  - _Requirements: 1.1, 1.3_

- [x] 3.3.2 Create Invoice Management application layer
  - Create `/billing/application/invoices/` package structure
  - Create GenerateInvoiceHandler, SendInvoiceHandler application services
  - Move invoice generation logic from existing services to application handlers
  - Create invoice management command and query DTOs
  - _Requirements: 3.1, 3.2_

- [x] 3.3.3 Create Invoice Management infrastructure layer
  - Create `/billing/infrastructure/invoices/` package structure
  - Move InvoiceRepositoryImpl, InvoiceEntity, InvoiceLineItemEntity from `/billing/infrastructure/persistence/` to `/billing/infrastructure/invoices/`
  - Create invoice-specific metrics collector and health indicators
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 3.3.4 Create Invoice Management presentation layer
  - Create `/billing/api/invoices/` package structure
  - Move invoice-related endpoints from InvoiceController to new capability-specific controller
  - Create invoice-specific request/response DTOs
  - _Requirements: 4.1, 4.2_

- [x] 3.3.5 Create Invoice Management database schema
  - Create `billing_invoices` schema and migrate existing invoices and invoice_line_items tables
  - Update invoice repository implementations to use new `billing_invoices` schema
  - Create invoice management specific migration scripts
  - _Requirements: 5.1, 5.2, 6.2_

### 3.4 Payment Processing Capability

- [x] 3.4.1 Create Payment Processing domain structure
  - Create `/billing/domain/payments/` package structure
  - Move Payment aggregate and PaymentAttempt entities from `/billing/domain/model/` to `/billing/domain/payments/`
  - Move payment-related models (PaymentId, PaymentAttemptId, PaymentStatus, PaymentMethod, PaymentFailureReason, PaymentRetryStrategy) to `/billing/domain/payments/`
  - Move PaymentService, StripePaymentProcessingService, PaymentRetryService from `/billing/domain/services/` to `/billing/domain/payments/`
  - Move Stripe-related models and services to `/billing/domain/payments/`
  - Move payment repository interfaces to `/billing/domain/payments/`
  - _Requirements: 1.1, 1.3_

- [-] 3.4.2 Create Payment Processing application layer
  - Create `/billing/application/payments/` package structure
  - Create ProcessPaymentHandler, RetryPaymentHandler, RefundPaymentHandler application services
  - Move payment processing logic from existing services to application handlers
  - Create payment processing command and query DTOs
  - _Requirements: 3.1, 3.2_

- [x] 3.4.3 Create Payment Processing infrastructure layer
  - Create `/billing/infrastructure/payments/` package structure
  - Move PaymentRepositoryImpl, PaymentEntity, PaymentAttemptEntity from `/billing/infrastructure/persistence/` to `/billing/infrastructure/payments/`
  - Move StripeHttpClient from `/billing/infrastructure/` to `/billing/infrastructure/payments/`
  - Create payment-specific metrics collector and health indicators
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 3.4.4 Create Payment Processing presentation layer
  - Create `/billing/api/payments/` package structure
  - Move payment-related endpoints from PaymentController to new capability-specific controller
  - Create payment-specific request/response DTOs
  - _Requirements: 4.1, 4.2_

- [x] 3.4.5 Create Payment Processing database schema
  - Create `billing_payments` schema and migrate existing payments and payment_attempts tables
  - Update payment repository implementations to use new `billing_payments` schema
  - Create payment processing specific migration scripts
  - _Requirements: 5.1, 5.2, 6.2_

### 3.5 Subscription Management Capability

- [x] 3.5.1 Create Subscription Management domain structure
  - Create `/billing/domain/subscriptions/` package structure
  - Move SubscriptionTier and tier-related models from `/billing/domain/model/` to `/billing/domain/subscriptions/`
  - Move tier management models (TierOption, TierRecommendation, TierUpgradeValidation, TierDowngradeResult) to `/billing/domain/subscriptions/`
  - Move TierManagementService, SubscriptionTierOrchestrator, TierUpgradeRecommendationService from `/billing/domain/services/` to `/billing/domain/subscriptions/`
  - Move StripeSubscriptionManagementService to `/billing/domain/subscriptions/`
  - Move subscription repository interfaces to `/billing/domain/subscriptions/`
  - _Requirements: 1.1, 1.3_

- [x] 3.5.2 Create Subscription Management application layer
  - Create `/billing/application/subscriptions/` package structure
  - Create CreateSubscriptionHandler, UpgradeTierHandler, CancelSubscriptionHandler application services
  - Move subscription management logic from existing services to application handlers
  - Create subscription management command and query DTOs
  - _Requirements: 3.1, 3.2_

- [x] 3.5.3 Create Subscription Management infrastructure layer
  - Create `/billing/infrastructure/subscriptions/` package structure
  - Move subscription-related repository implementations from `/billing/infrastructure/persistence/` to `/billing/infrastructure/subscriptions/`
  - Create subscription-specific metrics collector and health indicators
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 3.5.4 Create Subscription Management presentation layer
  - Create `/billing/api/subscriptions/` package structure
  - Move subscription-related endpoints from TierManagementController to new capability-specific controller
  - Create subscription-specific request/response DTOs
  - _Requirements: 4.1, 4.2_

- [x] 3.5.5 Create Subscription Management database schema
  - Create `billing_subscriptions` schema and migrate existing subscription_tiers table
  - Update subscription repository implementations to use new `billing_subscriptions` schema
  - Create subscription management specific migration scripts
  - _Requirements: 5.1, 5.2, 6.2_

### 3.6 Tax Compliance Capability

- [x] 3.6.1 Create Tax Compliance domain structure
  - Create `/billing/domain/tax/` package structure
  - Move tax-related aggregates and compliance models from `/billing/domain/model/` to `/billing/domain/tax/`
  - Move tax models (TaxRate, TaxRateId, TaxType, TaxCalculationResult, TaxLineItem, TaxComplianceReport, TaxInformation) to `/billing/domain/tax/`
  - Move TaxCalculationEngine, TaxCalculationService, TaxComplianceService, TaxRateManagementService from `/billing/domain/services/` to `/billing/domain/tax/`
  - Move ExchangeRate, Currency, CurrencyConversion models and ExchangeRateService to `/billing/domain/tax/`
  - Move tax repository interfaces to `/billing/domain/tax/`
  - _Requirements: 1.1, 1.3_

- [x] 3.6.2 Create Tax Compliance application layer
  - Create `/billing/application/tax/` package structure
  - Create CalculateTaxHandler, GenerateTaxReportHandler, ValidateComplianceHandler application services
  - Move tax calculation logic from existing services to application handlers
  - Create tax compliance command and query DTOs
  - _Requirements: 3.1, 3.2_

- [x] 3.6.3 Create Tax Compliance infrastructure layer
  - Create `/billing/infrastructure/tax/` package structure
  - Move TaxRateRepositoryImpl, TaxComplianceRepositoryImpl, ExchangeRateRepositoryImpl from `/billing/infrastructure/persistence/` to `/billing/infrastructure/tax/`
  - Move related entities (TaxRateEntity, TaxComplianceReportEntity, ExchangeRateEntity) to `/billing/infrastructure/tax/`
  - Create tax-specific metrics collector and health indicators
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 3.6.4 Create Tax Compliance database schema
  - Create `billing_tax` schema and migrate existing tax_rates, tax_compliance_reports, exchange_rates tables
  - Update tax repository implementations to use new `billing_tax` schema
  - Create tax compliance specific migration scripts
  - _Requirements: 5.1, 5.2, 6.2_

### 3.7 Dunning Management Capability

- [x] 3.7.1 Create Dunning Management domain structure
  - Create `/billing/domain/dunning/` package structure
  - Move dunning-related aggregates and service restriction models from `/billing/domain/model/` to `/billing/domain/dunning/`
  - Move dunning models (DunningAction, DunningSequence, DunningLevel, ServiceRestriction, RestrictionLevel) to `/billing/domain/dunning/`
  - Move DunningManagementEngine, PaymentRecoveryService, ServiceRestrictionService, DunningNotificationService from `/billing/domain/services/` to `/billing/domain/dunning/`
  - Move dunning repository interfaces to `/billing/domain/dunning/`
  - _Requirements: 1.1, 1.3_

- [x] 3.7.2 Create Dunning Management application layer
  - Create `/billing/application/dunning/` package structure
  - Create InitiateDunningHandler, EscalateDunningHandler, RestrictServiceHandler application services
  - Move dunning management logic from existing services to application handlers
  - Create dunning management command and query DTOs
  - _Requirements: 3.1, 3.2_

- [x] 3.7.3 Create Dunning Management infrastructure layer
  - Create `/billing/infrastructure/dunning/` package structure
  - Move dunning-related repository implementations from `/billing/infrastructure/persistence/` to `/billing/infrastructure/dunning/`
  - Create dunning-specific metrics collector and health indicators
  - _Requirements: 2.1, 2.2, 2.4, 6.2_

- [x] 3.7.4 Create Dunning Management database schema
  - Create `billing_dunning` schema for dunning-related tables (no existing tables to migrate)
  - Create new dunning tables for DunningAction, DunningSequence, ServiceRestriction entities
  - Create dunning management specific migration scripts
  - _Requirements: 5.1, 5.2, 6.2_

## 🚨 **ARCHITECTURE COMPLIANCE QUALITY GATES**

### **Pre-Implementation Quality Gate**
**BEFORE implementing ANY capability, validate:**
- [ ] ✅ Architecture Guide has been read and understood
- [ ] ✅ Package structure follows capability-based organization
- [ ] ✅ Handler patterns are understood and will be implemented correctly
- [ ] ✅ Repository functions patterns are understood
- [ ] ✅ Command/Query/Response patterns are understood
- [ ] ✅ Error handling patterns are understood

### **Implementation Quality Gate**
**DURING implementation, EVERY component MUST:**
- [ ] ✅ Follow exact handler pattern from Architecture Guide
- [ ] ✅ Use `{Capability}RepositoryFunctions` (NO direct repository injection)
- [ ] ✅ Use immutable records for commands/queries/responses
- [ ] ✅ Return `Result<T, {BoundedContext}Error>` from all operations
- [ ] ✅ Include structured logging with correlation IDs
- [ ] ✅ Include proper validation annotations
- [ ] ✅ Have factory methods for common use cases

### **Post-Implementation Quality Gate**
**AFTER implementation, validate:**
- [ ] ✅ NO anti-patterns detected (direct repository injection, mutable commands, imperative logic)
- [x] ✅ ALL handlers pass functional pattern validation















































- [x] ✅ ALL repository functions use direct access pattern (no pass-through methods)










- [-] ✅ ALL commands/queries/responses are immutable records


- [x] ✅ ALL operations return Result pattern





- [x] ✅ ALL components have proper logging and metrics







- [ ] ✅ ALL components have health checks
- [ ] ✅ Integration tests pass
- [x] ✅ Architecture compliance tests pass






### **Code Review Quality Gate**
**Code review MUST verify:**
- [ ] ✅ Architecture Guide compliance
- [x] ✅ NO forbidden patterns present




- [x] ✅ Functional composition implemented correctly



- [ ] ✅ Error handling follows Result pattern
- [ ] ✅ Logging includes correlation IDs
- [ ] ✅ Metrics and health checks implemented
- [ ] ✅ Tests cover success and failure scenarios

### **Architecture Compliance Validation**
**Automated checks MUST verify:**
- [ ] ✅ Package structure follows standards
- [ ] ✅ Handler pattern compliance
- [ ] ✅ Repository functions pattern compliance
- [ ] ✅ Command/Query/Response pattern compliance
- [ ] ✅ Error handling pattern compliance
- [ ] ✅ NO anti-patterns detected

## 4. Cross-Capability Integration

- [x] 4.1 Update cross-capability event handling
  - Update existing IamEventPublisher and BillingEventPublisher to work with new capability structure
  - Update CrossModuleEventBus to route events between capabilities properly
  - Update event handlers in each capability (move from reactors to capability-specific handlers)
  - Refactor existing events to contain aggregate IDs instead of full objects
  - Update IamCrossModuleEvents and billing events to follow new patterns
  - _Requirements: 5.4, 7.3_

- [x] 4.2 Update cross-capability service dependencies
  - Refactor application services to use capability-specific interfaces instead of direct service calls
  - Update Spring dependency injection configuration for new capability structure
  - Update BillingIntegrationService to use capability-specific interfaces
  - Implement application service orchestration using aggregate IDs (remove direct object passing)
  - Update existing orchestrators (PaymentProcessingOrchestrator) to follow new patterns
  - _Requirements: 3.3, 5.4, 7.4_

- [x] 4.3 Update observability integration
  - Update existing IamMetricsCollector and BillingMetricsCollector to aggregate capability-specific metrics
  - Update IamModuleHealthIndicator and BillingModuleHealthIndicator to aggregate capability health status
  - Update BillingObservabilityService to work with new capability structure
  - Ensure comprehensive observability coverage for all capabilities
  - _Requirements: 2.4, 6.1_

## 5. Testing and Validation

- [x] 5.1 Update existing tests for capability structure
  - Update existing IAM domain tests (IamPackageStructureTest, domain model tests) to work with new capability package structure
  - Update existing billing domain tests (SubscriptionAnalyticsReactorTest, BillingDashboardComposerTest, ServiceComposerReactorsTest) to work with new capability package structure
  - Update existing infrastructure tests (IamRepositoryIntegrationTest, BillingRepositoryIntegrationTest) for new structure
  - Update existing observability tests (OAuth2ObservabilityIntegrationTest, BillingObservabilityTest) for capability structure
  - Update import statements and package references in all affected test files
  - _Requirements: 6.1, 6.4_

- [x] 5.2 Create capability integration tests
  - Create integration tests for each capability's full stack (domain + application + infrastructure)
  - Update existing cross-module tests (CrossModuleEventPublishingTest, DomainServiceEventPublishingIntegrationTest) for capability interactions
  - Create integration tests for database schema isolation between capabilities
  - _Requirements: 6.3, 6.5_

- [x] 5.3 Create end-to-end capability tests
  - Create end-to-end tests for complete business workflows spanning multiple capabilities
  - Create performance tests for capability operations
  - Create tests for capability failure scenarios and resilience
  - _Requirements: 6.6_

- [ ] 5.4 Validate architecture compliance
  - Update existing ModularArchitectureValidationTest to enforce capability boundaries
  - Create architecture tests to validate that capabilities only access their own schemas
  - Validate that cross-capability communication follows defined patterns (ID-only references)
  - Create tests to ensure aggregates only reference each other by ID (no direct object references)
  - Test eventual consistency patterns for cross-aggregate queries
  - _Requirements: 6.7, 7.1, 7.2, 7.6_

## 6. Documentation and Migration

- [ ] 6.1 Update architecture documentation
  - Update existing docs/iam/README.md and docs/billing/README.md to reflect capability-based architecture
  - Update existing API documentation (docs/iam/api/openapi-specification.yml, docs/billing/api/openapi-specification.yml) for new endpoint organization
  - Update docs/modular-architecture-implementation-summary.md with capability structure
  - Create capability interaction diagrams
  - _Requirements: 6.1, 6.4_

- [ ] 6.2 Create migration guides
  - Update existing troubleshooting guides (docs/iam/operations/iam-troubleshooting-runbook.md, docs/billing/operations/billing-troubleshooting-runbook.md) for new structure
  - Create developer migration guide for new capability structure
  - Create deployment migration guide for database schema changes
  - Update existing user guides for new API structure
  - _Requirements: 6.2, 6.6_

- [ ] 6.3 Update build and deployment configuration
  - Update existing Maven pom.xml if needed for new structure
  - Update Spring configuration files (ComplianceApplication.java, ModularJpaConfiguration.java, BillingPersistenceConfig.java, BillingAutoConfiguration.java) for capability-based package structure
  - Update @ComponentScan, @EnableJpaRepositories, and @EntityScan annotations to reference new capability packages
  - Update existing application.yml and test configurations for new schema structure
  - Update CI/CD pipelines for new test structure
  - _Requirements: 6.5_

- [ ] 6.4 Clean up legacy structure
  - Remove old technical-concern-based package structure after migration
  - Clean up unused imports and dependencies from moved classes
  - Update existing configuration files (BillingAutoConfiguration, etc.) for new structure
  - Remove duplicate or obsolete classes after capability reorganization
  - _Requirements: 6.4_
##
 7. Future Modules Architecture Compliance

### 🚨 **MANDATORY ARCHITECTURE COMPLIANCE FOR ALL FUTURE MODULES**

**ALL future bounded contexts (Bank Registry, Data Quality, Risk Calculation, Report Generation, Exposure Ingestion) MUST strictly follow the [Functional Bounded Context Architecture Guide](../../docs/architecture/functional-bounded-context-architecture-guide.md)**

### **📋 PRE-DEVELOPMENT ARCHITECTURE CHECKLIST**
**BEFORE starting ANY future module development:**

#### **Architecture Guide Comprehension**
- [ ] **MANDATORY**: Read complete [Architecture Guide](../../docs/architecture/functional-bounded-context-architecture-guide.md)
- [ ] **MANDATORY**: Understand functional-first design principles
- [ ] **MANDATORY**: Understand clean architecture layers
- [ ] **MANDATORY**: Understand CQRS pattern implementation
- [ ] **MANDATORY**: Understand capability-based organization

#### **Pattern Understanding Validation**
- [ ] **MANDATORY**: Understand exact handler pattern with functional composition
- [ ] **MANDATORY**: Understand direct repository access pattern (eliminates pass-through methods)
- [ ] **MANDATORY**: Understand immutable record patterns for commands/queries/responses
- [ ] **MANDATORY**: Understand Result pattern for error handling
- [ ] **MANDATORY**: Understand domain entity and value object patterns
- [ ] **MANDATORY**: Understand API layer patterns
- [ ] **MANDATORY**: Understand infrastructure layer patterns
- [ ] **MANDATORY**: Understand testing patterns
- [ ] **MANDATORY**: Understand logging, metrics, and health check standards

### **🏗️ ARCHITECTURE IMPLEMENTATION STANDARDS**

#### **Package Structure Compliance**
**EVERY future module MUST follow this exact structure:**
```
com.bcbs239.compliance.{boundedcontext}/
├── api/{capability}/           # REST controllers and DTOs
├── application/{capability}/   # Use case handlers (organized by operation)
├── domain/{capability}/        # Domain entities, value objects, services  
└── infrastructure/{capability}/ # Technical implementations
```

#### **Handler Pattern Compliance**
**EVERY handler MUST follow this exact pattern:**
```java
@Component
public class {Operation}Handler {
    private static final Logger logger = LoggerFactory.getLogger({Operation}Handler.class);
    private final {Capability}RepositoryFunctions repositoryFunctions; // ✅ REQUIRED
    private final Function<{Operation}Command, Result<{Operation}Response, {BoundedContext}Error>> {operation};
    
    public {Operation}Handler({Capability}RepositoryFunctions repositoryFunctions) {
        this.repositoryFunctions = repositoryFunctions;
        this.{operation} = this::{operation}Impl;
    }
    
    public Result<{Operation}Response, {BoundedContext}Error> handle({Operation}Command command) {
        logger.info("Processing {operation} for {}", command.identifier());
        return {operation}.apply(command);
    }
    
    private Result<{Operation}Response, {BoundedContext}Error> {operation}Impl({Operation}Command command) {
        if (command == null) {
            return Result.failure({BoundedContext}Error.validationError("Command cannot be null"));
        }
        return repositoryFunctions
            .save{Entity}()
            .apply(command.data())
            .map(this::createResponse);
    }
}
```

#### **Repository Functions Compliance**
**EVERY repository functions class MUST follow this exact pattern:**
```java
@Component
public class {Capability}RepositoryFunctions {
    private final {Capability}RepositoryImpl repository;
    
    public {Capability}RepositoryFunctions({Capability}RepositoryImpl repository) {
        this.repository = repository;
    }
    
    public Function<{Entity}, Result<{Entity}, {BoundedContext}Error>> save{Entity}() {
        return repository::save; // ✅ Method reference REQUIRED
    }
    
    public Function<{Entity}Id, Result<Optional<{Entity}>, {BoundedContext}Error>> find{Entity}ById() {
        return repository::findById; // ✅ Method reference REQUIRED
    }
}
```

#### **Command/Query/Response Compliance**
**ALL commands/queries/responses MUST be immutable records:**
```java
public record {Operation}Command(
    @NotNull(message = "Required field cannot be null")
    RequiredField requiredField,
    OptionalField optionalField,
    CorrelationId correlationId
) {
    public {Operation}Command {
        Objects.requireNonNull(requiredField, "Required field cannot be null");
        if (correlationId == null) {
            correlationId = CorrelationId.generate();
        }
    }
    
    public static {Operation}Command of(RequiredField field, OptionalField optional) {
        return new {Operation}Command(field, optional, CorrelationId.generate());
    }
}
```

### **🚫 STRICTLY FORBIDDEN PATTERNS**
**The following patterns are ABSOLUTELY FORBIDDEN and will result in IMMEDIATE REJECTION:**

#### ❌ **Direct Repository Injection**
```java
// ❌ FORBIDDEN - Will be REJECTED
@Component
public class BadHandler {
    private final ConcreteRepositoryImpl repository; // ❌ VIOLATES ARCHITECTURE
}
```

#### ❌ **Imperative Handler Logic**
```java
// ❌ FORBIDDEN - Will be REJECTED
public Result<Response, Error> handle(Command command) {
    Entity entity = repository.findById(command.id()); // ❌ Direct calls
    entity.update(command.data()); // ❌ Imperative flow
    return Result.success(Response.from(entity));
}
```

#### ❌ **Mutable Command Objects**
```java
// ❌ FORBIDDEN - Will be REJECTED
public class BadCommand {
    private String field; // ❌ Mutable state
    public void setField(String field) { this.field = field; } // ❌ Setters
}
```

### **✅ MANDATORY QUALITY GATES FOR FUTURE MODULES**

#### **Pre-Implementation Quality Gate**
- [x] ✅ Architecture Guide comprehension validated





- [ ] ✅ Package structure design approved
- [x] ✅ Handler patterns understood and validated





- [x] ✅ Repository functions patterns understood





- [x] ✅ Command/Query/Response patterns understood







- [x] ✅ Error handling patterns understood



- [x] ✅ Domain modeling approach validated





#### **Implementation Quality Gate**
- [x] ✅ ALL handlers use `{Capability}RepositoryFunctions` (NO direct repository injection)





- [x] ✅ ALL handlers use functional composition with `Function<Command, Result<Response, Error>>`



- [x] ✅ ALL handlers have private implementation methods




- [ ] ✅ ALL commands/queries/responses are immutable records
- [x] ✅ ALL operations return `Result<T, {BoundedContext}Error>`





- [x] ✅ ALL components include structured logging with correlation IDs





- [x] ✅ ALL components include proper validation annotations







- [x] ✅ ALL components have factory methods for common use cases






#### **Post-Implementation Quality Gate**
- [x] ✅ NO anti-patterns detected in code review




- [ ] ✅ ALL handlers pass functional pattern validation
- [ ] ✅ ALL repository functions use direct access pattern (no pass-through methods)
- [ ] ✅ ALL commands/queries/responses are immutable records
- [ ] ✅ ALL operations return Result pattern
- [ ] ✅ ALL components have proper logging and metrics
- [ ] ✅ ALL components have health checks
- [ ] ✅ Integration tests pass
- [ ] ✅ Architecture compliance tests pass

#### **Code Review Quality Gate**
- [ ] ✅ Architecture Guide compliance verified
- [ ] ✅ NO forbidden patterns present
- [ ] ✅ Functional composition implemented correctly
- [ ] ✅ Error handling follows Result pattern
- [ ] ✅ Logging includes correlation IDs
- [ ] ✅ Metrics and health checks implemented
- [ ] ✅ Tests cover success and failure scenarios
- [x] ✅ Documentation updated



### **🔍 ARCHITECTURE COMPLIANCE VALIDATION**

#### **Automated Architecture Tests**
**EVERY future module MUST have automated tests that verify:**
- [ ] ✅ Package structure follows standards
- [ ] ✅ Handler pattern compliance (no direct repository injection)
- [ ] ✅ Repository functions pattern compliance (direct access, no pass-through methods)
- [ ] ✅ Command/Query/Response pattern compliance (immutable records)
- [ ] ✅ Error handling pattern compliance (Result pattern)
- [ ] ✅ NO anti-patterns detected

#### **Manual Code Review Checklist**
**EVERY code review MUST verify:**
- [ ] ✅ Architecture Guide patterns followed exactly
- [ ] ✅ Functional composition implemented correctly
- [ ] ✅ NO imperative logic in handlers
- [ ] ✅ NO mutable state in commands/queries/responses
- [ ] ✅ Proper error handling with correlation IDs
- [ ] ✅ Comprehensive logging and metrics
- [ ] ✅ Health checks implemented
- [ ] ✅ Tests cover all scenarios

### **📚 FUTURE MODULE SPECIFIC REQUIREMENTS**

#### **7.1 Bank Registry Module**
**BEFORE starting Bank Registry development:**
- [ ] **MANDATORY**: Complete Architecture Guide review
- [ ] Identify capabilities: `registration/`, `validation/`, `lookup/`, `compliance/`
- [ ] Design package organization following Architecture Guide
- [ ] Create `BankRegistryError` and `BankRegistryErrorType` following patterns
- [ ] Plan `BankRegistryRepositoryFunctions` for each capability
- [ ] **VALIDATION**: All handlers will use functional patterns
- [ ] **VALIDATION**: All repository access will use functional interfaces
- [ ] **VALIDATION**: All commands/queries/responses will be immutable records

#### **7.2 Data Quality Module**
**BEFORE starting Data Quality development:**
- [ ] **MANDATORY**: Complete Architecture Guide review
- [ ] Identify capabilities: `validation/`, `cleansing/`, `profiling/`, `monitoring/`
- [ ] Design package organization following Architecture Guide
- [ ] Create `DataQualityError` and `DataQualityErrorType` following patterns
- [ ] Plan `DataQualityRepositoryFunctions` for each capability
- [ ] **VALIDATION**: All handlers will use functional patterns
- [ ] **VALIDATION**: All repository access will use functional interfaces
- [ ] **VALIDATION**: All commands/queries/responses will be immutable records

#### **7.3 Risk Calculation Module**
**BEFORE starting Risk Calculation development:**
- [ ] **MANDATORY**: Complete Architecture Guide review
- [ ] Identify capabilities: `calculation/`, `modeling/`, `scenarios/`, `reporting/`
- [ ] Design package organization following Architecture Guide
- [ ] Create `RiskCalculationError` and `RiskCalculationErrorType` following patterns
- [ ] Plan `RiskCalculationRepositoryFunctions` for each capability
- [ ] **VALIDATION**: All handlers will use functional patterns
- [ ] **VALIDATION**: All repository access will use functional interfaces
- [ ] **VALIDATION**: All commands/queries/responses will be immutable records

#### **7.4 Report Generation Module**
**BEFORE starting Report Generation development:**
- [ ] **MANDATORY**: Complete Architecture Guide review
- [ ] Identify capabilities: `generation/`, `templates/`, `distribution/`, `scheduling/`
- [ ] Design package organization following Architecture Guide
- [ ] Create `ReportGenerationError` and `ReportGenerationErrorType` following patterns
- [ ] Plan `ReportGenerationRepositoryFunctions` for each capability
- [ ] **VALIDATION**: All handlers will use functional patterns
- [ ] **VALIDATION**: All repository access will use functional interfaces
- [ ] **VALIDATION**: All commands/queries/responses will be immutable records

#### **7.5 Exposure Ingestion Module**
**BEFORE starting Exposure Ingestion development:**
- [ ] **MANDATORY**: Complete Architecture Guide review
- [ ] Identify capabilities: `ingestion/`, `validation/`, `transformation/`, `storage/`
- [ ] Design package organization following Architecture Guide
- [ ] Create `ExposureIngestionError` and `ExposureIngestionErrorType` following patterns
- [ ] Plan `ExposureIngestionRepositoryFunctions` for each capability
- [ ] **VALIDATION**: All handlers will use functional patterns
- [ ] **VALIDATION**: All repository access will use functional interfaces
- [ ] **VALIDATION**: All commands/queries/responses will be immutable records

### **🎯 MODULE COMPLETION CRITERIA**
**Each future module is considered complete ONLY when:**
- [ ] ✅ ALL handlers follow Architecture Guide patterns exactly
- [ ] ✅ ALL repository functions use direct access pattern (no pass-through methods)
- [ ] ✅ ALL commands/queries/responses are immutable records
- [ ] ✅ ALL error handling uses Result pattern with proper error types
- [ ] ✅ ALL logging includes correlation IDs and structured format
- [ ] ✅ ALL components have metrics collection and health checks
- [ ] ✅ ALL tests pass including architecture compliance tests
- [ ] ✅ Code review approval with zero anti-pattern violations
- [ ] ✅ Architecture compliance verification completed
- [ ] ✅ Documentation updated to reflect new patterns
- [ ] ✅ Integration with existing modules follows established patterns

### **🚨 ZERO TOLERANCE POLICY**
**There is ZERO TOLERANCE for:**
- Direct repository injection in handlers
- Imperative logic in application layer
- Mutable commands, queries, or responses
- Missing error handling or correlation IDs
- Missing logging, metrics, or health checks
- Anti-patterns from the forbidden list
- Deviation from Architecture Guide patterns

**Any violation will result in immediate rejection and requirement to refactor according to the Architecture Guide.**