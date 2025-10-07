# Identity & Access Management Context - Design Document

## Overview

The Identity & Access Management (IAM) context serves as the central authentication and authorization hub for the BCBS 239 SaaS platform. It implements a complete user lifecycle from registration through multi-bank access management, using functional programming patterns, pure business functions, and closure-based dependency injection.

The design emphasizes value objects as Java records, explicit error handling through `Result<T, ErrorDetail>` types, and framework-independent business logic that integrates seamlessly with the Service Composer pattern for cross-context coordination.

## Architecture

### Core Architecture Principles

1. **Functional Programming Patterns**: Pure functions, immutable value objects, and closure-based dependency injection
2. **Explicit Error Handling**: All operations return `Result<T, ErrorDetail>` with no exceptions for business logic
3. **Value Object Design**: Domain concepts implemented as Java records with factory methods and validation
4. **Service Composer Integration**: Provides TenantContext for cross-context authorization
5. **Multi-Tenant Security**: Bank-scoped access control with subscription tier enforcement

### High-Level Architecture Diagram

```mermaid
graph TB
    subgraph "Registration Flow"
        RF[Registration Form]
        ST[Stripe Payment]
        BC[Bank Configuration]
    end
    
    subgraph "IAM Core Domain"
        UA[User Aggregate]
        BRA[BankRoleAssignment Aggregate]
        US[UserSession Aggregate]
        TC[TenantContext]
    end
    
    subgraph "Service Composer Integration"
        AR[AuthorizationReactor]
        RR[RegistrationReactor]
        BCR[BankConfigReactor]
    end
    
    subgraph "External Systems"
        STRIPE[Stripe API]
        BR[Bank Registry Context]
        BILL[Billing Context]
    end
    
    subgraph "Pure Functions"
        CF[createUser()]
        AF[authenticateUser()]
        VF[validateSession()]
        PF[assignBankRole()]
    end
    
    RF --> RR
    ST --> STRIPE
    BC --> BCR
    
    RR --> CF
    AR --> AF
    AR --> VF
    BCR --> PF
    
    CF --> UA
    AF --> US
    VF --> TC
    PF --> BRA
    
    AR --> TC
    TC --> BR
    TC --> BILL
    
    STRIPE -.-> BILL
    BCR -.-> BR
```

## Components and Interfaces

### 1. Domain Models (Value Objects as Records)

#### User Aggregate
The central user entity with immutable value objects.

```java
public record User(
    UserId id,
    Email email,
    FullName name,
    Address address,
    PasswordHash passwordHash,
    UserRole defaultRole,
    Instant createdAt,
    boolean isActive
) {
    public static Result<User, ErrorDetail> create(
        Email email,
        FullName name,
        Address address,
        String plainPassword
    ) {
        return PasswordHash.create(plainPassword)
            .map(hash -> new User(
                UserId.generate(),
                email,
                name,
                address,
                hash,
                UserRole.ADMIN, // Auto-assigned for registration
                Instant.now(),
                true
            ));
    }
    
    public Result<User, ErrorDetail> assignToBank(BankId bankId, UserRole role) {
        // Business logic for bank assignment
        return Result.success(this);
    }
}
```

#### Value Objects with Factory Methods

```java
public record UserId(String value) {
    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }
    
    public static Result<UserId, ErrorDetail> create(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Result.failure(ErrorDetail.validation("UserId cannot be empty"));
        }
        return Result.success(new UserId(value));
    }
}

public record Email(String value) {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    
    public static Result<Email, ErrorDetail> create(String value) {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            return Result.failure(ErrorDetail.validation("Invalid email format"));
        }
        return Result.success(new Email(value.toLowerCase()));
    }
}

public record FullName(String firstName, String lastName) {
    public static Result<FullName, ErrorDetail> create(String firstName, String lastName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            return Result.failure(ErrorDetail.validation("First name is required"));
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            return Result.failure(ErrorDetail.validation("Last name is required"));
        }
        return Result.success(new FullName(firstName.trim(), lastName.trim()));
    }
    
    public String displayName() {
        return firstName + " " + lastName;
    }
}

public record Address(
    String street,
    String city,
    String postalCode,
    String country
) {
    public static Result<Address, ErrorDetail> create(
        String street, String city, String postalCode, String country
    ) {
        List<String> errors = new ArrayList<>();
        
        if (street == null || street.trim().isEmpty()) {
            errors.add("Street is required");
        }
        if (city == null || city.trim().isEmpty()) {
            errors.add("City is required");
        }
        if (postalCode == null || postalCode.trim().isEmpty()) {
            errors.add("Postal code is required");
        }
        if (country == null || country.trim().isEmpty()) {
            errors.add("Country is required");
        }
        
        if (!errors.isEmpty()) {
            return Result.failure(ErrorDetail.validation("Address validation failed", 
                Map.of("errors", errors)));
        }
        
        return Result.success(new Address(
            street.trim(), city.trim(), postalCode.trim(), country.trim()
        ));
    }
}
```

#### BankRoleAssignment Aggregate

```java
public record BankRoleAssignment(
    AssignmentId id,
    UserId userId,
    BankId bankId,
    UserRole role,
    UserId assignedBy,
    Instant assignedAt,
    Maybe<Instant> revokedAt,
    boolean isActive
) {
    public static Result<BankRoleAssignment, ErrorDetail> create(
        UserId userId,
        BankId bankId,
        UserRole role,
        UserId assignedBy
    ) {
        return Result.success(new BankRoleAssignment(
            AssignmentId.generate(),
            userId,
            bankId,
            role,
            assignedBy,
            Instant.now(),
            Maybe.none(),
            true
        ));
    }
    
    public BankRoleAssignment revoke(UserId revokedBy) {
        return new BankRoleAssignment(
            id, userId, bankId, role, assignedBy, assignedAt,
            Maybe.some(Instant.now()), false
        );
    }
    
    public boolean canManageBank() {
        return isActive && (role == UserRole.BANK_ADMIN || role == UserRole.SYSTEM_ADMIN);
    }
}
```

### 2. Pure Business Functions

#### User Registration Function

```java
public class UserRegistrationService {
    
    public static Result<RegistrationResponse, ErrorDetail> registerUser(
        RegisterUserCommand command,
        Function<Email, Result<Maybe<User>, ErrorDetail>> userLookup,
        Function<User, Result<UserId, ErrorDetail>> userSave,
        Function<SubscriptionTier, Result<StripeSubscription, ErrorDetail>> createSubscription
    ) {
        return Email.create(command.email())
            .flatMap(email -> FullName.create(command.firstName(), command.lastName())
                .flatMap(name -> Address.create(command.street(), command.city(), 
                    command.postalCode(), command.country())
                    .flatMap(address -> checkUserNotExists(email, userLookup)
                        .flatMap(__ -> User.create(email, name, address, command.password())
                            .flatMap(user -> userSave.apply(user)
                                .flatMap(userId -> createSubscription.apply(command.subscriptionTier())
                                    .map(subscription -> new RegistrationResponse(
                                        userId,
                                        email,
                                        subscription.id(),
                                        "CONFIGURE_BANK",
                                        "Set up your first bank to start using the platform"
                                    ))
                                )
                            )
                        )
                    )
                )
            );
    }
    
    private static Result<Void, ErrorDetail> checkUserNotExists(
        Email email,
        Function<Email, Result<Maybe<User>, ErrorDetail>> userLookup
    ) {
        return userLookup.apply(email)
            .flatMap(maybeUser -> maybeUser.isPresent() 
                ? Result.failure(ErrorDetail.businessRule("USER_EXISTS", "User already exists"))
                : Result.success(null)
            );
    }
}
```

#### Authentication Function

```java
public class AuthenticationService {
    
    public static Result<AuthenticationResult, ErrorDetail> authenticateUser(
        AuthenticationCommand command,
        Function<Email, Result<Maybe<User>, ErrorDetail>> userLookup,
        Function<String, String, Boolean> passwordVerifier,
        Function<User, Result<JwtToken, ErrorDetail>> tokenGenerator
    ) {
        return Email.create(command.email())
            .flatMap(email -> userLookup.apply(email)
                .flatMap(maybeUser -> maybeUser.map(user -> 
                    authenticateWithPassword(user, command.password(), passwordVerifier, tokenGenerator)
                ).orElse(Result.failure(ErrorDetail.businessRule("INVALID_CREDENTIALS", "Invalid credentials")))
                )
            );
    }
    
    private static Result<AuthenticationResult, ErrorDetail> authenticateWithPassword(
        User user,
        String password,
        Function<String, String, Boolean> passwordVerifier,
        Function<User, Result<JwtToken, ErrorDetail>> tokenGenerator
    ) {
        if (!user.isActive()) {
            return Result.failure(ErrorDetail.businessRule("USER_INACTIVE", "User account is inactive"));
        }
        
        if (!passwordVerifier.apply(password, user.passwordHash().value())) {
            return Result.failure(ErrorDetail.businessRule("INVALID_CREDENTIALS", "Invalid credentials"));
        }
        
        return tokenGenerator.apply(user)
            .map(token -> new AuthenticationResult(
                user.id(),
                user.email(),
                token,
                user.defaultRole()
            ));
    }
}
```

#### Bank Role Assignment Function

```java
public class BankRoleService {
    
    public static Result<BankRoleAssignment, ErrorDetail> assignUserToBank(
        AssignBankRoleCommand command,
        Function<UserId, Result<Maybe<User>, ErrorDetail>> userLookup,
        Function<BankId, Result<Maybe<Bank>, ErrorDetail>> bankLookup,
        Function<UserId, Result<List<BankRoleAssignment>, ErrorDetail>> getUserBankCount,
        Function<UserId, Result<SubscriptionTier, ErrorDetail>> getSubscriptionTier,
        Function<BankRoleAssignment, Result<AssignmentId, ErrorDetail>> saveAssignment
    ) {
        return validateUserExists(command.userId(), userLookup)
            .flatMap(user -> validateBankExists(command.bankId(), bankLookup)
                .flatMap(bank -> validateSubscriptionLimits(user.id(), command.bankId(), 
                    getUserBankCount, getSubscriptionTier)
                    .flatMap(__ -> BankRoleAssignment.create(
                        user.id(), bank.id(), command.role(), command.assignedBy())
                        .flatMap(assignment -> saveAssignment.apply(assignment)
                            .map(__ -> assignment)
                        )
                    )
                )
            );
    }
    
    private static Result<Void, ErrorDetail> validateSubscriptionLimits(
        UserId userId,
        BankId bankId,
        Function<UserId, Result<List<BankRoleAssignment>, ErrorDetail>> getUserBankCount,
        Function<UserId, Result<SubscriptionTier, ErrorDetail>> getSubscriptionTier
    ) {
        return getUserBankCount.apply(userId)
            .flatMap(assignments -> getSubscriptionTier.apply(userId)
                .flatMap(tier -> {
                    int currentBankCount = assignments.size();
                    int maxBanks = tier.maxBanks();
                    
                    if (maxBanks != -1 && currentBankCount >= maxBanks) {
                        return Result.failure(ErrorDetail.businessRule("BANK_LIMIT_EXCEEDED", 
                            "Subscription tier allows maximum " + maxBanks + " banks"));
                    }
                    
                    return Result.success(null);
                })
            );
    }
}
```

### 3. Service Composer Integration

#### AuthorizationReactor
Provides TenantContext for all composition handlers.

```java
@Component
@CompositionHandler(route = "/*", order = 0)
public class AuthorizationReactor implements PostCompositionHandler, GetCompositionHandler {
    
    private final Function<JwtToken, Result<TenantContext, ErrorDetail>> validateSession;
    
    public AuthorizationReactor(
        Function<JwtToken, Result<TenantContext, ErrorDetail>> validateSession
    ) {
        this.validateSession = validateSession;
    }
    
    @Override
    public Result<Void, ErrorDetail> handleGet(
        HttpServletRequest request, 
        CompositionContext context,
        Map<String, Object> model
    ) {
        return extractAndValidateToken(request)
            .map(tenantContext -> {
                model.put("tenantContext", tenantContext);
                return null;
            })
            .mapError(error -> {
                model.put("authorizationError", error);
                model.put("processingPhase", "FAILED");
                return error;
            });
    }
    
    @Override
    public Result<Void, ErrorDetail> onInitialized(
        HttpServletRequest request, 
        Map<String, Object> body, 
        CompositionContext context
    ) {
        return extractAndValidateToken(request)
            .map(tenantContext -> {
                context.putData("tenantContext", tenantContext);
                return null;
            })
            .mapError(error -> {
                context.putData("authorizationError", error);
                context.putData("processingPhase", "FAILED");
                return error;
            });
    }
    
    private Result<TenantContext, ErrorDetail> extractAndValidateToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.failure(ErrorDetail.businessRule("MISSING_TOKEN", "Authorization token required"));
        }
        
        String tokenValue = authHeader.substring(7);
        return JwtToken.create(tokenValue)
            .flatMap(validateSession);
    }
}
```

#### RegistrationReactor
Handles user registration flow coordination.

```java
@Component
@CompositionHandler(route = "/auth/register", order = 1)
public class RegistrationReactor implements PostCompositionHandler {
    
    private final Function<RegisterUserCommand, Result<RegistrationResponse, ErrorDetail>> registerUser;
    
    @Override
    public Result<Void, ErrorDetail> onInitialized(
        HttpServletRequest request, 
        Map<String, Object> body, 
        CompositionContext context
    ) {
        return extractRegistrationCommand(body)
            .flatMap(registerUser)
            .map(response -> {
                context.putData("registrationResponse", response);
                context.putData("processingPhase", "USER_CREATED");
                return null;
            });
    }
    
    @Override
    public Result<Void, ErrorDetail> onUpdated(
        HttpServletRequest request, 
        Map<String, Object> body, 
        CompositionContext context
    ) {
        // Wait for billing context to confirm subscription
        return context.getData("billingConfirmed", Boolean.class)
            .map(confirmed -> {
                if (confirmed) {
                    context.putData("processingPhase", "REGISTRATION_COMPLETE");
                }
                return null;
            })
            .orElse(Result.success(null));
    }
    
    private Result<RegisterUserCommand, ErrorDetail> extractRegistrationCommand(Map<String, Object> body) {
        try {
            return Result.success(new RegisterUserCommand(
                (String) body.get("email"),
                (String) body.get("firstName"),
                (String) body.get("lastName"),
                (String) body.get("street"),
                (String) body.get("city"),
                (String) body.get("postalCode"),
                (String) body.get("country"),
                (String) body.get("password"),
                SubscriptionTier.valueOf((String) body.get("subscriptionTier"))
            ));
        } catch (Exception e) {
            return Result.failure(ErrorDetail.validation("Invalid registration data"));
        }
    }
}
```

### 4. TenantContext for Multi-Bank Access

```java
public record TenantContext(
    UserId userId,
    Email email,
    Set<BankAccess> bankAccess,
    boolean isSystemAdmin,
    Instant sessionExpiry
) {
    public boolean canAccessBank(BankId bankId) {
        return isSystemAdmin || bankAccess.stream()
            .anyMatch(access -> access.bankId().equals(bankId));
    }
    
    public boolean canManageBank(BankId bankId) {
        return isSystemAdmin || bankAccess.stream()
            .anyMatch(access -> access.bankId().equals(bankId) && access.canManage());
    }
    
    public Maybe<UserRole> getRoleForBank(BankId bankId) {
        return bankAccess.stream()
            .filter(access -> access.bankId().equals(bankId))
            .findFirst()
            .map(access -> Maybe.some(access.role()))
            .orElse(Maybe.none());
    }
    
    public Set<BankId> getAccessibleBanks() {
        return bankAccess.stream()
            .map(BankAccess::bankId)
            .collect(Collectors.toSet());
    }
}

public record BankAccess(
    BankId bankId,
    UserRole role,
    Instant assignedAt
) {
    public boolean canManage() {
        return role == UserRole.BANK_ADMIN || role == UserRole.SYSTEM_ADMIN;
    }
    
    public boolean canViewCompliance() {
        return role.ordinal() >= UserRole.COMPLIANCE_OFFICER.ordinal();
    }
    
    public boolean canAnalyze() {
        return role.ordinal() >= UserRole.ANALYST.ordinal();
    }
}
```

## Data Models

### Command Objects

```java
public record RegisterUserCommand(
    String email,
    String firstName,
    String lastName,
    String street,
    String city,
    String postalCode,
    String country,
    String password,
    SubscriptionTier subscriptionTier
) {}

public record AuthenticationCommand(
    String email,
    String password
) {}

public record AssignBankRoleCommand(
    UserId userId,
    BankId bankId,
    UserRole role,
    UserId assignedBy
) {}
```

### Response Objects

```java
public record RegistrationResponse(
    UserId userId,
    Email email,
    SubscriptionId subscriptionId,
    String nextStep,
    String message
) {}

public record AuthenticationResult(
    UserId userId,
    Email email,
    JwtToken token,
    UserRole defaultRole
) {}
```

### Enums and Constants

```java
public enum UserRole {
    VIEWER(1),
    ANALYST(2),
    COMPLIANCE_OFFICER(3),
    BANK_ADMIN(4),
    SYSTEM_ADMIN(5);
    
    private final int level;
    
    UserRole(int level) {
        this.level = level;
    }
    
    public boolean canAccess(UserRole requiredRole) {
        return this.level >= requiredRole.level;
    }
}

public enum SubscriptionTier {
    STARTER(1, 1000, 5, 500.00),
    PROFESSIONAL(5, 10000, 50, 2000.00),
    ENTERPRISE(-1, -1, -1, 5000.00);
    
    private final int maxBanks;
    private final int maxExposures;
    private final int maxReports;
    private final double monthlyPrice;
    
    SubscriptionTier(int maxBanks, int maxExposures, int maxReports, double monthlyPrice) {
        this.maxBanks = maxBanks;
        this.maxExposures = maxExposures;
        this.maxReports = maxReports;
        this.monthlyPrice = monthlyPrice;
    }
    
    public int maxBanks() { return maxBanks; }
    public boolean allowsUnlimitedBanks() { return maxBanks == -1; }
}
```

## Error Handling

### Domain-Specific Error Types

```java
public class IamErrorCodes {
    public static final String USER_EXISTS = "USER_EXISTS";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String USER_INACTIVE = "USER_INACTIVE";
    public static final String BANK_LIMIT_EXCEEDED = "BANK_LIMIT_EXCEEDED";
    public static final String INSUFFICIENT_PERMISSIONS = "INSUFFICIENT_PERMISSIONS";
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
    public static final String MISSING_TOKEN = "MISSING_TOKEN";
}
```

### Error Handling Patterns

```java
public class IamErrorHandler {
    
    public static Result<TenantContext, ErrorDetail> handleAuthenticationError(
        Result<TenantContext, ErrorDetail> result,
        CompositionContext context
    ) {
        return result.mapError(error -> {
            context.addError(error);
            
            // Log security events
            if (IamErrorCodes.INVALID_CREDENTIALS.equals(error.code())) {
                logSecurityEvent("FAILED_LOGIN", error.context());
            }
            
            return error;
        });
    }
    
    private static void logSecurityEvent(String eventType, String context) {
        // Security event logging implementation
    }
}
```

## Testing Strategy

### Pure Function Testing

```java
class UserRegistrationServiceTest {
    
    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
            "test@example.com", "John", "Doe", 
            "123 Main St", "City", "12345", "Country",
            "password123", SubscriptionTier.PROFESSIONAL
        );
        
        Function<Email, Result<Maybe<User>, ErrorDetail>> userLookup = 
            email -> Result.success(Maybe.none());
        Function<User, Result<UserId, ErrorDetail>> userSave = 
            user -> Result.success(user.id());
        Function<SubscriptionTier, Result<StripeSubscription, ErrorDetail>> createSubscription = 
            tier -> Result.success(new StripeSubscription("sub_123"));
        
        // When
        Result<RegistrationResponse, ErrorDetail> result = 
            UserRegistrationService.registerUser(command, userLookup, userSave, createSubscription);
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals("CONFIGURE_BANK", result.getValue().nextStep());
    }
    
    @Test
    void shouldFailWhenUserAlreadyExists() {
        // Given
        RegisterUserCommand command = createValidCommand();
        Function<Email, Result<Maybe<User>, ErrorDetail>> userLookup = 
            email -> Result.success(Maybe.some(createExistingUser()));
        
        // When
        Result<RegistrationResponse, ErrorDetail> result = 
            UserRegistrationService.registerUser(command, userLookup, null, null);
        
        // Then
        assertTrue(result.isFailure());
        assertEquals(IamErrorCodes.USER_EXISTS, result.getError().code());
    }
}
```

### Service Composer Integration Testing

```java
@SpringBootTest
class AuthorizationReactorIntegrationTest {
    
    @Test
    void shouldCreateTenantContextForValidToken() {
        // Given
        CompositionTestFramework
            .forRoute("/dashboard")
            .withHandler(new AuthorizationReactor(mockValidateSession))
            .withMockData("validToken", "Bearer jwt_token_123")
            .execute()
            .assertSuccess()
            .assertModelContains("tenantContext")
            .assertModelDoesNotContain("authorizationError");
    }
}
```

This design provides a comprehensive foundation for user registration, authentication, and multi-bank access control while maintaining functional programming principles and Service Composer integration.