# IAM Bounded Context Consolidation

## Overview

The IAM bounded context has been consolidated following proper DDD patterns with closure-based repositories, proper domain/persistence separation, and the established architecture guidelines.

## Directory Structure

```
regtech-iam/
├── src/main/java/com/bcbs239/regtech/iam/
│   ├── IamModule.java                           # Spring Boot module configuration
│   ├── api/                                     # REST API layer
│   │   └── users/
│   │       └── UserController.java              # User management endpoints
│   ├── application/                             # Application services (CQRS)
│   │   ├── createuser/                          # User registration use case
│   │   │   ├── RegisterUserCommand.java
│   │   │   ├── RegisterUserCommandHandler.java
│   │   │   └── RegisterUserResponse.java
│   │   ├── authenticate/                        # Authentication use case
│   │   │   ├── AuthenticationCommand.java
│   │   │   ├── AuthenticateUserCommandHandler.java
│   │   │   └── AuthenticationResult.java
│   │   └── events/                              # Cross-context event handlers
│   │       ├── PaymentVerificationEventHandler.java
│   │       └── BillingAccountEventHandler.java
│   ├── domain/                                  # Domain model (pure business logic)
│   │   └── users/                               # User aggregate
│   │       ├── User.java                        # Aggregate root
│   │       ├── UserId.java                      # Value object
│   │       ├── Email.java                       # Value object
│   │       ├── Password.java                    # Value object
│   │       ├── UserStatus.java                  # Enum
│   │       ├── UserRole.java                    # Entity
│   │       └── BankId.java                      # Value object
│   └── infrastructure/                          # Infrastructure concerns
│       ├── database/                            # Persistence layer
│       │   ├── entities/                        # JPA entities
│       │   │   ├── UserEntity.java
│       │   │   ├── UserRoleEntity.java
│       │   │   └── UserBankAssignmentEntity.java
│       │   └── repositories/                    # Closure-based repositories
│       │       └── JpaUserRepository.java       # Consolidated repository
│       ├── security/                            # Security configuration
│       │   ├── IamSecurityConfiguration.java
│       │   └── IamAuthorizationService.java
│       ├── events/                              # Event infrastructure
│       │   ├── IamEventPublisher.java
│       │   └── IamOutboxEventProcessor.java
│       └── health/                              # Health checks
│           └── IamModuleHealthIndicator.java
├── src/main/resources/
│   ├── db/migration/
│   │   └── V1__Create_iam_schema.sql           # Database schema
│   └── application-iam.yml                     # Module configuration
└── src/test/                                   # Tests (mirrors main structure)
```

## Key Consolidation Changes

### 1. **Repository Consolidation**
- **Before**: Separate `UserRepository` and `UserRoleRepository` in wrong packages
- **After**: Single `JpaUserRepository` with all user-related operations using closures

### 2. **Proper Domain/Persistence Separation**
- **Before**: Domain `User` class used directly as JPA entity
- **After**: Separate `UserEntity` for persistence with conversion methods

### 3. **Closure-Based Repository Pattern**
```java
// Example closure usage
public Function<Email, Maybe<User>> emailLookup() {
    return email -> {
        // Implementation with EntityManager
    };
}

// Usage in command handler
Result<RegisterUserResponse> result = registerUser(
    command,
    userRepository.emailLookup(),      // Closure injection
    userRepository.userSaver(),        // Closure injection
    this::publishEvent                 // Event publisher
);
```

### 4. **Consolidated Authorization**
- **Before**: Separate `UserRoleRepository` interface
- **After**: Role operations integrated into `JpaUserRepository` with closures

### 5. **Proper Database Schema**
- **Before**: Mixed table structures
- **After**: Clean schema with proper indexes and constraints

## Repository Closures Available

### User Operations
- `emailLookup()` - Find user by email
- `userLoader()` - Load user by ID
- `userSaver()` - Save user
- `saveOAuthUser()` - OAuth user creation/update

### Role Operations
- `userRolesFinder()` - Get all roles for user
- `userOrgRolesFinder()` - Get roles for user in organization
- `userRoleSaver()` - Save user role
- `userRoleChecker()` - Check if user has specific role

## Domain Model Features

### User Aggregate Root
- Factory methods for creation (`create`, `createWithBank`, `createOAuth`)
- Business methods (`activate`, `suspend`, `changePassword`)
- Proper encapsulation with value objects
- Bank assignment management

### Value Objects
- `UserId` - Strongly typed user identifier
- `Email` - Email with validation
- `Password` - Password with strength requirements and hashing
- `BankId` - Bank identifier

### Authorization Integration
- Multi-tenant support with organization-based roles
- Permission-based access control
- Cross-module authorization service

## Testing Strategy

### Unit Tests (Pure Functions)
```java
@Test
void shouldRegisterUserSuccessfully() {
    // Given - mock closures
    Function<Email, Maybe<User>> emailLookup = email -> Maybe.none();
    Function<User, Result<UserId>> userSaver = user -> Result.success(user.getId());
    
    // When - call pure function
    Result<RegisterUserResponse> result = RegisterUserCommandHandler.registerUser(
        command, emailLookup, userSaver, eventPublisher
    );
    
    // Then - assert result
    assertThat(result.isSuccess()).isTrue();
}
```

### Integration Tests
- Database operations with test containers
- Cross-module event publishing
- Security configuration testing

## Configuration

### Database Configuration
```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_schema: iam
  flyway:
    schemas: iam
```

### Security Configuration
```yaml
iam:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400
    password:
      min-length: 12
      require-special-chars: true
```

## Migration Guide

### From Old Structure
1. **Update imports** - Change from old repository interfaces to `JpaUserRepository`
2. **Update injection** - Inject `JpaUserRepository` instead of separate repositories
3. **Use closures** - Replace direct method calls with closure functions
4. **Update tests** - Use closure mocking instead of interface mocking

### Example Migration
```java
// Before
@Autowired
private UserRepository userRepository;
@Autowired 
private UserRoleRepository userRoleRepository;

Maybe<User> user = userRepository.findByEmail(email);
List<UserRole> roles = userRoleRepository.findByUserId(userId);

// After
@Autowired
private JpaUserRepository userRepository;

Maybe<User> user = userRepository.emailLookup().apply(email);
List<UserRole> roles = userRepository.userRolesFinder().apply(userId);
```

## Benefits of Consolidation

1. **Single Source of Truth** - All user-related operations in one place
2. **Better Testability** - Closure-based functions are easily mockable
3. **Proper Separation** - Clear domain/persistence boundaries
4. **Performance** - Optimized queries with proper indexing
5. **Maintainability** - Consistent patterns across the codebase
6. **Scalability** - Multi-tenant ready with organization support

## Next Steps

1. **Update other modules** - Apply same patterns to billing and future modules
2. **Add caching** - Implement permission caching for performance
3. **Add monitoring** - Metrics and health checks for authorization
4. **Security hardening** - Implement proper password hashing with BCrypt
5. **OAuth integration** - Complete Google/Facebook OAuth implementation

This consolidation provides a solid foundation for the IAM bounded context that follows established DDD patterns and supports the modular monolith architecture.